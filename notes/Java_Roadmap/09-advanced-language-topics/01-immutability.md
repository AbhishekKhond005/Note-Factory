# Chapter 12: Immutability — The Art of Unchanging State

## 1. Chapter Introduction

Every senior developer has lived through the same nightmare. You ship a feature, it passes tests, and then a bug report arrives that makes no sense. Somewhere deep in the system, an object's state is *wrong* — a name has been silently rewritten, a date has shifted by two days, a price has been divided by four. You check your code, and your code is fine. Then you discover the truth: *someone else's code mutated your object*. A `List` you stored in a constructor was modified by a caller who still held a reference to it. A `Date` you trusted was corrupted by a method that called `setTime()`. The object you thought you owned was shared — and sharing without protection is the root of the bug.

This chapter is the antidote. **Immutability** is the design decision that an object's state *cannot change after construction*. It is one of the most powerful, quietly transformative ideas in Java. It turns objects that you must guard and defend into objects you can hand to anyone without fear. It is the reason `String` is safe to use as a `HashMap` key, the reason `java.time.LocalDate` replaced the notoriously mutable `java.util.Date`, and the reason modern Java gives you `record` — immutability baked into the language.

By the end of this chapter, you will understand not just *how* to write immutable classes, but *why* every component of them exists: the `final` keyword, defensive copying, and the internals of `String` itself.

**What you will learn:**
- What "immutable" precisely means, and the crucial difference between an *immutable object* and an *immutable reference*.
- Every use of the `final` keyword — and why `final` alone never guarantees immutability.
- The five rules for designing a fully immutable class, with a step-by-step before/after refactoring.
- How and when to use defensive copying to stop mutation leaks.
- How `String` achieves its immutability guarantees, how the string constant pool works, and the famous `substring` memory bug.
- Modern tools: value objects, `record`, and the builder pattern.

By the end, the bug from the opening paragraph will be structurally impossible in your code.

---

## 2. Core Concepts

### 2.1 What Does "Immutable" Mean?

An object is **immutable** if, after its construction completes, *no operation can change its observable state*. There are no setters, no mutating methods, and no way to reach a mutable field from the outside. Construction is a one-way door: in through the constructor, and out into the world, forever fixed.

> 🧠 **Analogy: a printed book vs. an editable document.** A PDF you print and bind is immutable. You can read it, copy it, share it, annotate it *in the margins* (a separate object) — but you cannot change a word on page 42. A DOCX file is mutable: anyone with the file can edit text, reorder pages, and reformat. Now imagine ten people sharing one DOCX through email — that's a mutable object passed around. Now imagine them all sharing the printed book — that's immutability. *Which would you rather use as the key to a filing cabinet?* You'd want the book, because its content never changes, so a label describing it never goes stale.

Before going further, we must distinguish three closely related ideas that beginners — and even working developers — frequently conflate:

- **Immutable object:** The *object's* state cannot change. `String` is an immutable object; the bytes backing it never change after creation.
- **Immutable reference:** The *variable* holding the reference cannot be reassigned. `final String s = "hi";` means `s` can never point to a different `String` — but the object `s` points to might still be mutable (see §2.2).
- **Effectively immutable:** An object that *could* be mutated but never is in practice, because no code path mutates it. This is a weaker, convention-based guarantee — useful for publication in concurrent code, but it relies on discipline rather than the type system. Java's `List.of(...)` returns an *unmodifiable* list (structural change is impossible), which is a stronger guarantee than "effectively immutable."

Let's contrast mutable and immutable objects across the dimensions that matter:

| Dimension | Mutable object | Immutable object |
|---|---|---|
| State changes | Possible after construction | Impossible after construction |
| Thread safety | Requires synchronization or `volatile`/locking | Safe to share freely across threads |
| Caching & hashing | Hash code can change → breaks `HashMap` | Hash code stable → safe as map key |
| Copying strategy | Copy to avoid aliasing bugs | Reference may be shared freely; copies optional |
| Mental model | "Live entity" whose state evolves | "Snapshot" that is always true |
| Example | `ArrayList`, `StringBuilder`, `java.util.Date` | `String`, `Integer`, `LocalDate`, `BigDecimal` |

The table's row on hashing deserves emphasis: `HashMap` computes an object's bucket from its hash code. If a key's state changes after insertion, its hash code changes, and the map will look for it in the *wrong bucket* — the entry becomes orphaned and effectively lost. Immutable keys make this failure mode impossible.

### 2.2 The `final` Keyword

The `final` keyword is Java's only built-in immutability *primitive* — but it is much weaker than people assume. `final` has four distinct uses, and only one of them relates directly to object immutability.

**1. `final` variables (primitives and references)**

A `final` primitive variable can be assigned exactly once:

```java
public class FinalVariableExamples {

    public static void main(String[] args) {
        final int maxAttempts = 3;
        // maxAttempts = 4; // COMPILE ERROR: cannot assign a value to final variable

        final StringBuilder builder = new StringBuilder("hello");
        builder.append(" world");      // Legal! We mutate the object.
        System.out.println(builder);   // "hello world"
        // builder = new StringBuilder("nope"); // COMPILE ERROR: cannot reassign

        // A final field can be assigned in a constructor or at declaration:
        final Config config = new Config(8080);
        System.out.println(config.port());
    }

    record Config(int port) {}
}
```

> ⚠️ **Pitfall: `final` ≠ immutable.** `final StringBuilder` is *not* an immutable object. `final` freezes the *reference* (the variable), not the *object* it points to. The reference `builder` always points to the same `StringBuilder`, but that `StringBuilder` happily mutates via `append`. This single misconception is responsible for countless bugs.

**2. `final` parameters**

A `final` parameter cannot be reassigned inside the method body. It documents intent and prevents accidental reassignment bugs:

```java
public class FinalParameterExample {

    // The parameter reference cannot be rebound inside the method.
    static void log(final StringBuilder message, final int level) {
        // level = 2;                 // COMPILE ERROR
        // message = new StringBuilder(); // COMPILE ERROR
        message.append(" [level=" + level + "]"); // Mutating the object is still allowed!
        System.out.println(message);
    }

    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder("Request failed");
        log(sb, 1);
    }
}
```

**3. `final` methods**

A `final` method cannot be overridden in a subclass. This locks in behavior that a subclass must not alter:

```java
public class BasePayment {

    protected long amount;

    public BasePayment(long amount) {
        this.amount = amount;
    }

    // Subclasses cannot change how payments are described.
    public final String describe() {
        return "Payment of " + amount + " cents";
    }

    // Not final: subclasses may customize the display currency.
    public String currencySymbol() {
        return "$";
    }
}

class CryptoPayment extends BasePayment {

    public CryptoPayment(long amount) {
        super(amount);
    }

    // @Override describe() -- would not compile: cannot override final method.

    @Override
    public String currencySymbol() {
        return "₿";
    }
}
```

**4. `final` classes**

A `final` class cannot be subclassed at all. This is the strongest structural immutability tool Java gives you, because *inheritance is the classic way immutability leaks*. A subclass can add mutable fields, override methods to expose them, and behave nothing like its parent. That is why `String`, `Integer`, `BigDecimal`, and all the primitive wrappers are declared `final`:

```java
public final class String {
    // ...
}
```

> 💡 **Insight: why `final` class + `final` fields + `private` access = the skeleton of immutability.** `final` on the class prevents a subclass from breaking the invariant. `final` on each field guarantees the reference is set once and never re-pointed. `private` on each field guarantees no code outside the class can touch the field directly. Together they build the wall; defensive copying (Section 4) closes the remaining hole — the *objects inside* the fields.

**Common Pitfalls callout box:**

> ⚠️ **Common Pitfalls with `final`:**
> 1. Believing `final Object` makes the object immutable. It only prevents reassignment of the variable.
> 2. Skipping defensive copies because "the field is `final`." A `final` reference to a mutable `List` still lets the list's contents change.
> 3. Making a class `final` and forgetting to make its fields `private` — subclasses are blocked, but the whole world can still read and (if fields are mutable) mutate public fields.
> 4. Overusing `final` parameters in internal methods: it adds noise without changing behavior in most cases. Use it where reassignment would be a real bug.

---

## 3. Immutable Classes

### 3.1 The Five Rules of Immutable Classes

The canonical recipe for an immutable class comes straight from Joshua Bloch's *Effective Java* (Item 17). There are five rules:

| # | Rule | Why | Example |
|---|---|---|---|
| 1 | Don't provide setters (or any mutator methods) | No public API can change state after construction | `LocalDate.plusDays(...)` returns a *new* `LocalDate` rather than mutating |
| 2 | Make all fields `final` and `private` | Fields are initialized once and inaccessible to outsiders | `private final String name;` |
| 3 | Prevent subclassing — make the class `final` (or provide only private constructors plus a factory) | A subclass could add mutable state or override methods to expose it | `public final class Money` |
| 4 | If a field refers to a mutable object, defensive-copy it in the constructor and in getters | The object behind the reference could be mutated by whoever passed it in or reads it out | `this.notes = new ArrayList<>(incoming);` |
| 5 | Ensure exclusive access to any mutable component | Don't store the caller's reference; don't hand out your own reference | Getters return `new ArrayList<>(items)` instead of `items` |

Rule 4 and Rule 5 are the two that *most* developers miss, because they require thinking about the world *outside* the class. Rules 1–3 are about the class's own shape; rules 4–5 are about defending the class from its environment.

### 3.2 Step-by-Step Construction

Let's build an immutable class incrementally. We'll design a `Person` with a name and a list of phone numbers. Here is the **broken, mutable version** — every line that leaks is annotated:

```java
import java.util.ArrayList;
import java.util.List;

public class PersonMutable {

    private String name;                       // BUG: not final, not private-immutable
    private List<String> phoneNumbers;         // BUG: mutable type, stored by reference

    public PersonMutable(String name, List<String> phoneNumbers) {
        this.name = name;
        this.phoneNumbers = phoneNumbers;      // BUG: aliases the caller's list!
    }

    public void setName(String name) {         // BUG: mutator
        this.name = name;
    }

    public List<String> getPhoneNumbers() {    // BUG: hands out the internal list!
        return phoneNumbers;
    }

    public static void main(String[] args) {
        List<String> numbers = new ArrayList<>(List.of("555-0100"));
        PersonMutable p = new PersonMutable("Ada", numbers);

        // The class's "internal" list is corrupted from outside:
        numbers.add("555-9999");               // Mutates p's state!
        System.out.println(p.getPhoneNumbers().size()); // 2 -- p changed after construction

        p.getPhoneNumbers().clear();           // Also mutates p directly
        p.setName("Grace");                    // Renamed silently
        System.out.println(p);                 // Someone else's data, silently changed
    }

    @Override
    public String toString() {
        return "PersonMutable{name='" + name + "', phoneNumbers=" + phoneNumbers + "}";
    }
}
```

Every bug class that immutability exists to prevent is present here. Now, the **fully immutable refactoring**, with a comment on each fix:

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Person {                       // FIX (Rule 3): no subclassing

    private final String name;                    // FIX (Rule 2): final + private
    private final List<String> phoneNumbers;      // FIX (Rule 2)

    public Person(String name, List<String> phoneNumbers) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must be non-blank");
        }
        this.name = name;
        // FIX (Rule 4): defensive copy IN -- we own our own list now
        this.phoneNumbers = new ArrayList<>(phoneNumbers);
    }

    // FIX (Rule 1): no setters. "Change" returns a NEW Person.
    public Person withName(String newName) {
        return new Person(newName, this.phoneNumbers);
    }

    public String name() {
        return name;
    }

    public List<String> getPhoneNumbers() {
        // FIX (Rule 5): defensive copy OUT -- caller mutates a throwaway copy
        return new ArrayList<>(phoneNumbers);
    }

    // Unmodifiable view is a cheaper alternative for the getter (see 4.4).
    public List<String> phoneNumbersUnmodifiable() {
        return Collections.unmodifiableList(phoneNumbers);
    }

    public static void main(String[] args) {
        List<String> numbers = new ArrayList<>(List.of("555-0100"));
        Person ada = new Person("Ada", numbers);

        numbers.add("555-9999");                  // Caller's list changes...
        System.out.println(ada.getPhoneNumbers()); // ...but ada's does NOT. ["555-0100"]

        Person renamed = ada.withName("Ada Lovelace"); // new object, ada unchanged
        System.out.println(ada.name());           // still "Ada"
        System.out.println(renamed.name());       // "Ada Lovelace"
    }

    @Override
    public String toString() {
        return "Person{name='" + name + "', phoneNumbers=" + phoneNumbers + "}";
    }
}
```

> 💡 **Insight: mutation becomes *transformation*.** Notice `withName`. Immutable classes don't delete the ability to "change" — they change the *mechanism*. Instead of mutating `this`, the class constructs a brand-new object representing the new state. This is the pattern behind `LocalDate.plusDays`, `String.concat`, and `BigDecimal.add`: each returns a new instance, leaving the receiver untouched. This makes state histories auditable and concurrent access safe.

### 3.3 Benefits

Immutability is not a restriction; it is a **superpower**. Here are the benefits, mapped to concrete Java examples:

| Benefit | Concrete Java example |
|---|---|
| Thread safety without synchronization | Ten threads share one `LocalDate` with no locks, no `volatile`, no data races |
| Safe as `HashMap`/`HashSet` keys | `String` and `Integer` keys never change their hash, so lookups never miss |
| Freedom to share, cache, and reuse (flyweight) | `Integer.valueOf(42)` returns the *same* cached object every time |
| Safe publication in concurrent code | An immutable object can be handed to other threads without synchronization |
| Simpler reasoning | `Money m` is either correct or never existed — no "half-mutated" states |
| Natural failure atomicity | If an operation throws, the object is unchanged — no need to roll back |

The flyweight point deserves expansion. With mutable objects, you cannot safely share instances, because one consumer's mutation corrupts everyone else's view. With immutable objects, sharing is free. That's why `Boolean.TRUE` is a single shared instance, why the string pool (§5.2) exists, and why `Optional.of(value)` can cache instances. Immutability converts the question "who owns this object?" into "who cares — we all own it."

---

## 4. Defensive Copying

### 4.1 The Problem: Aliasing and Mutation Leaks

**Aliasing** is the situation where two references point to the same object. Aliasing is not inherently bad — but *mutable aliasing* is dangerous, because a mutation through one alias silently changes what the other alias sees. When a class stores a reference given to it by a caller, and that caller keeps the original, the class's internal state is **leaked**: the caller can mutate it at any time.

Here is the classic leak, in a data-integrity flavor. A `BankStatement` trusts a `Date[]` passed by the caller:

```java
import java.util.Arrays;
import java.util.Date;

public class BankStatementLeaky {

    private final Date[] transactions;   // final! But it does not save us.

    public BankStatementLeaky(Date[] transactions) {
        this.transactions = transactions;   // LEAK: caller still holds this array
    }

    public Date[] getTransactions() {       // LEAK: hands the array back out
        return transactions;
    }

    public static void main(String[] args) {
        Date[] deposits = { new Date(1_700_000_000_000L), new Date(1_700_008_600_000L) };
        BankStatementLeaky statement = new BankStatementLeaky(deposits);

        // An "external auditor" rewrites history -- this is a fraud scenario in real life:
        deposits[0].setTime(0L);            // Mutates the Date object INSIDE statement
        System.out.println(statement.getTransactions()[0]); // printed as 1970-01-01

        // Or outright replaces an entry:
        statement.getTransactions()[1] = new Date(0L);
        System.out.println(Arrays.toString(statement.getTransactions()));
    }
}
```

The field is `final`, yet the statement's state changed. **This is the perfect counterexample to "`final` fields guarantee immutability."** `final` stopped the field from being re-pointed to a different array, but the array itself and the `Date` objects inside it remain mutable, and the class leaked references to both.

### 4.2 Defensive Copies in Constructors and Getters

The fix is to copy mutable data at the class boundary — **copy-in** on the way in (constructor/setter) and **copy-out** on the way out (getter):

```java
import java.util.Arrays;
import java.util.Date;

public final class BankStatement {

    private final Date[] transactions;   // still final; now truly safe

    public BankStatement(Date[] transactions) {
        if (transactions == null) {
            throw new NullPointerException("transactions must not be null");
        }
        // COPY-IN: we now own our own array.
        this.transactions = transactions.clone();   // Date[] clones shallowly
        // Alternatively: Arrays.copyOf(transactions, transactions.length)
    }

    public Date[] getTransactions() {
        // COPY-OUT: caller gets a throwaway copy, never our internal array.
        return transactions.clone();
    }

    public int size() {
        return transactions.length;
    }

    public static void main(String[] args) {
        Date[] deposits = { new Date(1_700_000_000_000L), new Date(1_700_008_600_000L) };
        BankStatement statement = new BankStatement(deposits);

        deposits[0].setTime(0L);                    // Changes the caller's array only.
        System.out.println(statement.getTransactions()[0]); // Statement unchanged.

        statement.getTransactions()[1] = new Date(0L); // Mutates the throwaway copy.
        System.out.println(statement.size());           // Still 2. Safe.
    }
}
```

> ⚠️ **Pitfall: a shallow copy of an array of mutable objects is not fully defensive.** `clone()` on `Date[]` copies the *references*, not the `Date` objects. If a caller does `statement.getTransactions()[0].setTime(0L)`, they still mutate the shared `Date`. For full safety you'd deep-copy each element (see §4.3). In practice, use `java.time.Instant`/`LocalDate` (immutable) instead of `Date`, and the problem disappears entirely.

The defensive-copy idiom for common mutable types:

| Mutable type | Copy-in / copy-out idiom | Notes |
|---|---|---|
| `Date` | `new Date(old.getTime())` | `Date.clone()` also works; copy constructor is clearer |
| Array (any) | `Arrays.copyOf(a, a.length)` or `a.clone()` | Shallow copy of references |
| `List` | `new ArrayList<>(incoming)` | Works for `Collection` too |
| `Map` | `new HashMap<>(incoming)` | Shallow; values are shared |
| `Set` | `new HashSet<>(incoming)` | Shallow; elements are shared |
| `StringBuilder` | `new StringBuilder(sb)` | Copy constructor exists |
| Primitive wrappers, `String`, `BigDecimal`, `LocalDate` | **No copy needed** | They are already immutable — sharing is safe |

**The copy-on-write trade-off.** Every defensive copy costs time and memory. Copying a 100,000-element list on every getter call is a performance disaster. The alternative is to make the getter return an *unmodifiable view* (§4.4), which costs almost nothing but throws if anyone attempts mutation. The general rule: **copy when you don't fully control the object's lifetime; use an unmodifiable view when you can guarantee nobody mutates the underlying store.**

### 4.3 Deep vs. Shallow Copies

A **shallow copy** duplicates the container but shares the contained objects. A **deep copy** duplicates the entire graph, so nothing is shared. Whether you need a deep copy depends entirely on whether the *contained objects themselves* are mutable:

```java
import java.util.ArrayList;
import java.util.List;

public class DeepVsShallow {

    public static void main(String[] args) {
        List<StringBuilder> builders = new ArrayList<>(List.of(
                new StringBuilder("alpha"), new StringBuilder("beta")));

        // Shallow copy: same two StringBuilder objects inside.
        List<StringBuilder> shallow = new ArrayList<>(builders);
        builders.get(0).append("-changed");
        System.out.println(shallow.get(0));   // "alpha-changed" -- SHARED, mutated!

        // Deep copy: brand-new StringBuilder objects inside.
        List<StringBuilder> deep = new ArrayList<>();
        for (StringBuilder sb : builders) {
            deep.add(new StringBuilder(sb));  // new object per element
        }
        builders.get(0).append("-again");
        System.out.println(deep.get(0));      // "alpha-changed" -- unaffected

        // With immutable elements, shallow == deep, so copying is trivial or unnecessary.
    }
}
```

> 🧠 **Analogy: photocopying a photo album.** A shallow copy is a new binder containing the *same photographs* — tear one photo and both albums show the damage. A deep copy reprints every photograph — the albums are now fully independent. If photographs are immutable (prints, not Polaroids-in-development), shallow copying is perfectly fine, because no one can tear the originals. Immutable elements make deep copying unnecessary — one more way immutability saves you work.

**When are deep copies required?** When you store nested mutable objects *and* hand out references to the graph. The pragmatic modern answer: prefer immutable building blocks (`LocalDate`, `BigDecimal`, `String`, `List.of(...)`) so that shallow copies are automatically safe.

### 4.4 Performance and Design Trade-offs

Defensive copying is not free. Each copy consumes CPU and heap; in hot paths it can dominate. Consider these design levers:

- **`Collections.unmodifiableList(...)` as a cheaper alternative.** Instead of copying the list for every getter, store a normal list internally and expose only an unmodifiable view:

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CourseCatalog {

    private final List<String> courses;
    private final List<String> view;   // cached unmodifiable view, built once

    public CourseCatalog(List<String> courses) {
        this.courses = new ArrayList<>(courses);   // one defensive copy in
        this.view = Collections.unmodifiableList(this.courses);
    }

    public List<String> getCourses() {
        return view;                   // zero-copy, but nobody can mutate it
    }
}
```

  The view throws `UnsupportedOperationException` on any mutation attempt. Cost: one `ArrayList` allocation at construction instead of one per getter call. This is the pattern used by `List.of(...)` and `Map.of(...)`, which return immutable collections *without* backing copies.

- **`clone()` pitfalls.** `Object.clone()` is notoriously easy to misuse: it bypasses constructors, it's shallow, and its `Cloneable` contract is broken. For defensive copying of arrays it's fine (`array.clone()` is idiomatic); for objects, prefer copy constructors (`new Person(existing)`) or explicit copy factories. In fact, the JavaDoc for `Cloneable` itself acknowledges the mechanism is problematic; modern code mostly avoids it.

- **When can you skip copies?** When you *know* the caller can't (or won't) mutate — e.g., the object came from a private source, was created internally, or is passed by trusted code. `HashMap` internally copies arrays for its own bookkeeping but trusts you with the keys it hands back... actually it stores *your* references for keys — which is exactly why you must never mutate keys (§3.3). The rule of thumb: **copy at the trust boundary; don't copy inside the fortress walls.**

> ⚠️ **Pitfall: `Collections.unmodifiableList` is not a copy.** It is a *view* over the original list. If you pass a raw list you still own to `unmodifiableList` and keep the original around, mutation through the original still changes the view. Unmodifiable *views* protect readers, not the underlying data; unmodifiable *copies* (via `List.copyOf`) protect everything. Choose deliberately.

---

## 5. String Internals

### 5.1 Why String Is Special

`String` is the single most-used class in Java, and it is immutable — by design, from the very first release. Why is immutability so essential for strings?

- **Thread safety.** `String` objects are shared aggressively across threads (in caches, logs, servlet attributes). Immutability means sharing requires no synchronization.
- **Security.** Class names, file paths, URLs, SQL queries, and network addresses are often `String`s. A mutable class name could let malicious code redirect a lookup — imagine a `String className` that changes after a security check passed. `String`'s immutability makes such attacks structurally impossible.
- **The string constant pool.** Because `String`s never change, the JVM can safely *intern* identical strings — storing one canonical instance and letting every occurrence reference it — saving enormous memory (§5.2).
- **Hash code stability.** `String` caches its hash code (`private int hash;` computed lazily once) precisely because the content never changes. A mutable string could never cache safely.

The `String` class is `final`, its backing character array is `private final byte[] value;` (the actual storage differs by JDK version), and no public method can modify that array.

### 5.2 The String Constant Pool and `intern()`

When you write a string **literal**, the compiler and JVM arrange for it to live in a special area of memory called the **string constant pool** (in modern JVMs, the heap region that holds interned strings). The JVM guarantees that two equal string literals are the *same object*:

```java
public class StringPoolExample {

    public static void main(String[] args) {
        String a = "java";
        String b = "java";              // same literal -> same pooled instance
        System.out.println(a == b);     // true: identical object in the pool

        String c = new String("java");  // explicitly forces a NEW object (heap, not pool)
        System.out.println(a == c);     // false: different objects

        System.out.println(a.equals(c)); // true: value equality still works

        String interned = c.intern();    // returns the canonical pooled instance
        System.out.println(a == interned); // true: intern() found the pooled "java"
    }
}
```

Diagram-style view of the pool:

```
        ┌─────────────────────────────────────────────┐
        │              STRING CONSTANT POOL           │
        │                                             │
        │   "java" ──── a ───────────────────────────┐ │
        │       ▲                                    │ │
        │       │ intern()                           │ │
        │       │                                    │ │
        │       └────── interned (from c)            │ │
        └────────────────────────────────────────────┼─┘
                                                     │ (same object)
        ┌────────────────────────────────────────────┼─┐
        │         HEAP (non-pooled)                  │ │
        │   new String("java") ──── c  ──────────────┘ │
        └─────────────────────────────────────────────┘
```

| Expression | Creates a pooled object? | `==` with literal `"java"`? |
|---|---|---|
| `String s = "java";` | Yes | `true` |
| `String s2 = "java";` (another literal) | Reuses pool entry | `true` |
| `String s3 = new String("java");` | No — new heap object | `false` |
| `s3.intern()` | Returns existing pooled one | `true` |

`intern()` is rarely needed in modern code: the JVM's heap management and `String` deduplication (a G1 GC feature) largely obsolete manual interning. **Risks of over-using `intern()`:** interned strings live forever (they can't be collected), so interning dynamically generated, unbounded strings is a classic **memory leak**. Only intern strings you know are few and long-lived.

> 🧠 **Analogy: the office nameplate.** The pool is like an office directory: one nameplate per person. Everyone who mentions "Ada" points at the single directory card, not their own photocopy. `new String("java")` is like making your own photocopy of the nameplate anyway — it wastes paper and the photocopy is never `==` the original. `intern()` says "throw away your photocopy and just point at the directory card."

### 5.3 Substring Memory Behavior (Java 6 vs. 7+)

Here is a legendary cautionary tale about immutability and memory. In **Java 6 and earlier**, `String` was implemented as three fields: `char[] value`, `int offset`, and `int count`. A `substring(start, end)` did **not copy** the characters. Instead, it created a new `String` object that *shared the same backing array* and simply adjusted `offset` and `count`:

```java
// Java 6 mental model:
//   "abcdefghijklmnopqrstuvwxyz".substring(0, 1)
//   -> new String, value = SAME 26-char array, offset = 0, count = 1
```

Now the bug: if you extracted a small substring from a very large string, the tiny substring *silently retained the entire large backing array*. A common server pattern — parse one large message, then keep only a 2-character token from it — could pin megabytes of memory for the program's lifetime. This was the notorious **substring memory-retention leak**. The fix, in **Java 7** (JEP 227), changed `substring` to copy the requested range into a fresh array, so a small substring holds only a small array.

```java
public class SubstringMemory {

    public static void main(String[] args) {
        String huge = "x".repeat(10_000_000);          // ~10 MB backing array

        // Java 7+: substring copies. Memory retained is just the small result.
        String token = huge.substring(0, 2);

        // Before Java 7, `token` would have pinned the entire 10 MB array.
        // The moral: immutability is about *semantic* state, not about
        // implementation sharing. Implementers may share or copy freely
        // as long as observable behavior is unchanged.
        System.out.println(token);                     // "xx"
    }
}
```

> 💡 **Insight: immutability gives implementers freedom.** Because `String` semantics never change, the JVM is free to *share* backing arrays (old substring), *copy* them (new substring), or *deduplicate* them (G1 GC) without breaking a single program. This is the hidden payoff of immutability: it makes the implementation tractable for optimization.

### 5.4 `String`, `StringBuilder`, and `StringBuffer`

If `String` is immutable, how do you build strings incrementally without creating a new object per concatenation? That's exactly the job of the mutable string classes:

| Feature | `String` | `StringBuilder` | `StringBuffer` |
|---|---|---|---|
| Mutability | Immutable | Mutable | Mutable |
| Thread safety | Safe (immutable) | **Not** thread-safe | Thread-safe (synchronized methods) |
| Performance (concatenation loop) | Slow — new object per `+` | Fast | Slower than `StringBuilder` (lock overhead) |
| Typical use | Representing values, map keys, logs | Building strings in a single thread | Building strings across threads (rare) |

```java
public class StringBuilderExample {

    public static void main(String[] args) {
        // Slow (in a loop): creates a new String each iteration.
        String slow = "";
        for (int i = 0; i < 1000; i++) {
            slow = slow + i;             // 1000 String objects created
        }

        // Fast: one mutable buffer.
        StringBuilder fast = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            fast.append(i);              // mutates the buffer in place
        }

        String result = fast.toString(); // only ONE String allocation at the end

        // StringBuffer is identical but synchronized; use it only if
        // multiple threads build the same buffer (almost never).
        StringBuffer threadSafe = new StringBuffer("a").append("b");

        System.out.println(result.length());
        System.out.println(threadSafe);
    }
}
```

> 💡 **Insight: the compiler is your friend.** When you write `"a" + "b" + "c"` in a single expression, `javac` automatically compiles it to a `StringBuilder` chain. The problem is only concatenation *inside loops*, where each iteration allocates. For single-expression concatenation, `+` is fine and readable; prefer `StringBuilder` in loops.

---

## 6. Practical Patterns & Real-World Usage

### 6.1 Value Objects

A **value object** is an object whose identity is defined entirely by its *value*, not by its memory location. Two `Money` objects of "$5.00" are the same money, regardless of which instance they are. The contract of a value object is: immutable + `equals`/`hashCode` based on fields + `toString`. Here is a complete, runnable example:

```java
import java.math.BigDecimal;
import java.util.Objects;

public final class Money {

    private final BigDecimal amount;
    private final String currency;   // ISO 4217, e.g. "USD"

    public Money(BigDecimal amount, String currency) {
        // BigDecimal is immutable, so no defensive copy is needed.
        this.amount = Objects.requireNonNull(amount, "amount");
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    public BigDecimal amount() {
        return amount;               // safe to hand out: BigDecimal is immutable
    }

    public String currency() {
        return currency;
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("currency mismatch: " +
                    this.currency + " vs " + other.currency);
        }
        // Returns a NEW Money; `this` is untouched.
        return new Money(this.amount.add(other.amount), this.currency);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money that)) return false;   // pattern matching, Java 16+
        return this.amount.equals(that.amount) && this.currency.equals(that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }

    public static void main(String[] args) {
        Money five = new Money(new BigDecimal("5.00"), "USD");
        Money three = new Money(new BigDecimal("3.00"), "USD");
        Money eight = five.add(three);

        System.out.println(five);                       // 5.00 USD (unchanged!)
        System.out.println(eight);                      // 8.00 USD

        System.out.println(five.equals(new Money(new BigDecimal("5.00"), "USD"))); // true
        System.out.println(five.hashCode() == new Money(new BigDecimal("5.00"), "USD").hashCode()); // true
    }
}
```

### 6.2 Records and Modern Java

Since **Java 16** (final), `record` gives you an immutable class *for free*. The compiler generates: the `private final` fields, a canonical constructor, accessors (`name()`), `equals`, `hashCode`, and `toString`. There are no setters, no inheritance, and the fields are `final`.

```java
// Hand-written immutable class (simplified for comparison):
public final class AddressClass {
    private final String street;
    private final String city;

    public AddressClass(String street, String city) {
        this.street = street;
        this.city = city;
    }
    public String street() { return street; }
    public String city() { return city; }
    // equals, hashCode, toString all written by hand...
}

// The same thing as a record:
public record Address(String street, String city) {}

// Records are immutable OUT OF THE BOX and still support compact constructors
// for validation:
public record PersonRec(String name, List<String> phoneNumbers) {
    public PersonRec {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must be non-blank");
        }
        // Note: records do NOT defensively copy by default! For a mutable
        // component like List, write a canonical constructor that does:
        phoneNumbers = List.copyOf(phoneNumbers);  // defensive copy
    }
}
```

> ⚠️ **Pitfall: `record` gives you immutability of the reference, not of the components.** `record Person(List<String> phones)` stores the exact `List` you pass. If you pass a mutable `ArrayList` and keep the original, the record leaks. Defensive-copy mutable components in a compact constructor, exactly as you would in a hand-written class. Records remove *boilerplate*; they do not remove *judgment*.

Records' `equals`/`hashCode` are based on components, making them perfect value objects and map keys.

### 6.3 The Builder Pattern for Immutable Classes

Immutable classes with many fields face a problem: a constructor with 10 parameters is unreadable and error-prone. The **builder pattern** solves this: the builder is mutable, collects configuration step by step, validates once, and then produces a single immutable object.

```java
public final class NutritionFacts {

    private final int servingSize;   // required
    private final int calories;      // optional
    private final int fat;           // optional
    private final int sodium;        // optional

    private NutritionFacts(Builder b) {
        this.servingSize = b.servingSize;
        this.calories = b.calories;
        this.fat = b.fat;
        this.sodium = b.sodium;
    }

    public static Builder builder(int servingSize) {
        return new Builder(servingSize);
    }

    public static final class Builder {
        private final int servingSize;      // required at construction time
        private int calories;
        private int fat;
        private int sodium;

        private Builder(int servingSize) {
            if (servingSize <= 0) throw new IllegalArgumentException("servingSize must be > 0");
            this.servingSize = servingSize;
        }

        public Builder calories(int v) { this.calories = v; return this; }
        public Builder fat(int v)      { this.fat = v;      return this; }
        public Builder sodium(int v)   { this.sodium = v;   return this; }

        public NutritionFacts build() {
            if (calories < 0 || fat < 0 || sodium < 0) {
                throw new IllegalStateException("nutrient values cannot be negative");
            }
            return new NutritionFacts(this);
        }
    }

    @Override
    public String toString() {
        return "NutritionFacts{servingSize=" + servingSize +
                ", calories=" + calories + ", fat=" + fat + ", sodium=" + sodium + "}";
    }

    public static void main(String[] args) {
        NutritionFacts facts = NutritionFacts.builder(240)
                .calories(100)
                .sodium(35)
                .build();
        System.out.println(facts);
        // NutritionFacts{servingSize=240, calories=100, fat=0, sodium=35}
    }
}
```

> 💡 **Insight: the builder is the bridge between immutability and usability.** The mutable `Builder` collects state; `build()` performs cross-field validation (e.g., "negative sodium") that a single constructor cannot express gracefully; and the immutable `NutritionFacts` is *never* exposed in a partially constructed state. This is the same design used by `StringBuilder` (a builder for `String`) and `java.util.stream.Stream` (a builder for result collections).

### 6.4 Real-World Use Cases

- **`BigDecimal`** — financial calculations must be exact *and* unchanged by accidental mutation; a price cannot silently change between invoice lines. Its immutability means every operation returns a new value, keeping arithmetic auditable.
- **`LocalDate` / `java.time`** — the entire `java.time` package replaced the mutable, thread-hostile `java.util.Date`/`Calendar`. `LocalDate.plusDays` returns a new date, so threads can share a single reference without locks.
- **`Optional`** — an `Optional<T>` is immutable; you can share `Optional.empty()` everywhere, and a value's presence never changes under you, eliminating a whole class of null-handling races.
- **Primitive wrappers (`Integer`, `Boolean`, `Character`...)** — safe as `HashMap` keys and freely cached (e.g., `Integer.valueOf` caches −128..127), which is only sound because their value never changes.
- **Configuration objects in Spring/DI frameworks** — a `@ConfigurationProperties` object is bound once from properties and then shared across beans; immutability guarantees every bean sees the *same* config and no component can tamper with another's settings.
- **DTOs in REST APIs** — a Data Transfer Object arrives from JSON, is validated, and is read by many handlers; immutable DTOs prevent one handler from corrupting another's view of the request.
- **Keys in maps / caching** — an immutable key's hash never changes, so `HashMap`/`ConcurrentHashMap` lookups are always correct; frameworks like Guava's `Cache` rely on this.
- **Security-sensitive objects** — user sessions, roles, and permissions should be immutable so that a privilege check performed at one moment cannot be invalidated by later mutation (recall §5.1's security argument for `String`).

In every case, the same theme recurs: **sharing without fear**. Immutability converts an object from a liability that must be guarded into an asset that can be freely distributed.

---

## 7. Summary & Review

### Key Takeaways

- An **immutable object**'s state cannot change after construction; this is distinct from an *immutable reference* (a `final` variable) and *effectively immutable* (mutable in theory, never in practice).
- `final` has four uses (variables, parameters, methods, classes); `final` freezes *references*, never the pointed-to *objects*. **`final` ≠ immutable.**
- The five rules: no setters; all fields `final` and `private`; prevent subclassing; defensive-copy mutable components; never leak mutable references.
- **Defensive copying** (copy-in/copy-out) closes the aliasing hole that `final` cannot; `Collections.unmodifiableList` is a cheaper *view*-based alternative, and immutable components make deep copies unnecessary.
- `String`'s immutability underwrites thread safety, security, the constant pool, and cached hash codes.
- The string constant pool makes literals share one instance; `new String(...)` creates a distinct object; `intern()` returns the pooled canonical one but can leak memory if overused.
- `substring` in Java 6 retained the parent's entire backing array; Java 7+ copies. Immutability gives implementers freedom to share or copy internally.
- Prefer `StringBuilder` (single thread) over `StringBuffer` (synchronized) for loop concatenation; the compiler already uses `StringBuilder` for simple `+` chains.
- Value objects, `record` (Java 16+), and the builder pattern are the modern toolkit for producing immutable classes cleanly.

### Concept Map Table

| Core Concept | Definition | Key API / Keyword | Common Mistake |
|---|---|---|---|
| `final` | Freezes a variable's reference or blocks inheritance/overriding; does not freeze an object's state | `final`, `final class`, `final method` | Believing `final List` makes the list immutable |
| Immutable class rules | No mutators; `final`/`private` fields; no subclassing; defensive copies; no leaked references | `record`, `Collections.unmodifiableList`, `List.copyOf` | Storing a caller's mutable `List`/`Date` directly |
| Defensive copying | Copying mutable data at class boundaries so internal state can't be aliased | `new ArrayList<>(x)`, `Arrays.copyOf`, `new Date(d.getTime())` | Forgetting copy-out in getters, or deep-copying immutable components |
| String pool | Canonical storage of string literals; `intern()` retrieves the canonical instance | `intern()`, literal pool | `new String(...)` everywhere; over-using `intern()` on unbounded data |

### Quiz / Exercises

**Conceptual multiple-choice questions:**

1. Which statement is **true** about `final`?
   a) A `final` field's object can never be mutated.
   b) A `final` variable cannot be reassigned after initialization.
   c) A `final` class can still be subclassed if it has a public constructor.
   d) `final` fields may be modified by reflection-free code in any method.

   *Answer sketch:* **(b).** `final` governs the variable/reference, not the object; `final class` cannot be subclassed (so (c) is false).

2. `record Money(BigDecimal amount) {}` is immutable **except** that:
   a) Its accessor returns the internal `amount` directly.
   b) `BigDecimal` is mutable.
   c) It cannot be used in a `HashMap`.
   d) Records are never thread-safe.

   *Answer sketch:* **(a).** `BigDecimal` is immutable, so no copy is needed; but a `record` with a mutable component type (e.g., `List`) would leak unless defensively copied in the compact constructor.

3. A class holding `private final Date[] events` is *not* immutable because:
   a) The array is mutable.
   b) The `Date` objects are mutable and both the constructor and getter leak references.
   c) `Date` is not `final`.
   d) Both (a) and (b).

   *Answer sketch:* **(d).** `final` protects the field reference only; the array contents and the `Date` objects remain mutable and reachable.

**Code-fix exercises:**

1. **Make this class immutable** — fix all violations:
   ```java
   import java.util.ArrayList;
   import java.util.List;

   public class ShoppingCart {
       private List<String> items = new ArrayList<>();
       public void add(String item) { items.add(item); }
       public List<String> getItems() { return items; }
   }
   ```
   *Answer sketch:* Make the class `final`; make `items` a `private final` field initialized in a constructor with `List.copyOf(items)`; delete `add` (replace with `withItem` returning a new cart, or keep a builder); return `List.copyOf(items)` (or an unmodifiable view) from the getter.

2. **Fix the leaky constructor:**
   ```java
   public final class Profile {
       private final Map<String, String> attributes;
       public Profile(Map<String, String> attributes) {
           this.attributes = attributes;   // leaks!
       }
   }
   ```
   *Answer sketch:* Use `this.attributes = new HashMap<>(attributes);` in the constructor and `return new HashMap<>(attributes);` (or `Collections.unmodifiableMap(attributes)`) in a getter. Explain that `Map.copyOf` is also a valid defensive-copy idiom.

3. **Spot the `String` pool surprise** — predict the output:
   ```java
   String a = "hello";
   String b = new String("hello");
   String c = b.intern();
   System.out.println(a == b);
   System.out.println(a == c);
   ```
   *Answer sketch:* `false` (b is a fresh heap object) then `true` (`intern()` returns the pooled canonical `"hello"`, which is exactly `a`).

**Open-ended design question:**

4. Design a thread-safe `TemperatureReading` for a monitoring system that records sensor values *as history* (a `List` of past readings, each with a timestamp). The readings are produced by multiple threads and consumed by reporting code. Explain your choices: which parts are immutable, where you use defensive copying or unmodifiable views, and how you would add a new reading without breaking concurrent readers.

   *Answer sketch:* Make `TemperatureReading` itself an immutable value object (`record TemperatureReading(LocalDateTime at, BigDecimal celsius)`); make the history store use an immutable collection that is swapped atomically — e.g., hold `private volatile List<TemperatureReading> history` and replace the whole list via `List.copyOf` on each append (copy-on-write), or use `CopyOnWriteArrayList`, or keep a `ConcurrentLinkedDeque` and expose an unmodifiable snapshot. Concurrency safety comes from (a) the immutable elements needing no synchronization and (b) publishing the collection through a `volatile`/atomic reference so readers always see a consistent snapshot. Defensive copies are only needed at the point where an external caller could retain a mutable reference — with immutable elements and copy-on-write snapshots, none is required.

---

*End of Chapter 12.* You now hold the tools to make the opening bug impossible: immutable state, defensively copied at every boundary, `final` where it belongs, and an understanding of how `String` itself embodies these principles. The next time you see an object whose state "changed by itself," you'll know exactly which of the five rules was violated — and how to fix it.