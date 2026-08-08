# Method Types in Java: Static, Final, Private, Overloaded, and Overridden Methods

## Opening Overview

Every Java program you have written so far is built from methods — the named, reusable blocks of behavior that give objects life. But not all methods are created equal. A `static` method belongs to a class; an *instance* method belongs to an object. A `final` method is locked against future change; a `private` method is hidden from the outside world entirely. Two methods can share a name yet be perfectly legal (overloading), or a child class can replace a parent's behavior entirely (overriding). These five *method types* form the grammar of everyday Java code, and the decisions you make about them ripple outward into every API you design.

Understanding method types is not trivia — it is the difference between code that is rigid and fragile and code that is flexible and maintainable. When you choose `static` over instance, you decide where state lives. When you mark a method `final`, you decide how much future developers (including yourself) may change behavior. When you overload or override, you decide how your classes read at the call site and how polymorphism powers your architecture.

This chapter will teach you:

- **1. Introduction** — the anatomy of a method and how invocations are executed.
- **2. Static methods** — class-level behavior, utility classes, and the `main` method.
- **3. Final methods** — locking behavior against overriding.
- **4. Private methods** — encapsulation and implementation hiding.
- **5. Overloaded methods** — the same name, different parameters.
- **6. Overridden methods** — subclass customization and runtime polymorphism.
- **7–10.** Comparison, pitfalls, practice exercises, and a summary of everything covered.

By the end, you will not just *know* these method types — you will know *when* to use each one, which is the real skill.

---

## 1. Introduction to Methods in Java

Before we distinguish the five method types, we need a shared vocabulary. A **method** is a named block of statements that can be invoked (called) to perform an action or compute a result. Methods let you decompose a problem, reuse logic, and hide complexity.

### 1.1 Quick Recap: Method Signature, Return Type, Parameter List, Access Modifiers

Every method declaration has several mandatory and optional parts. Consider this declaration:

```java
public  double  calculateArea(double radius)  {
    return Math.PI * radius * radius;
}
```

- **Access modifier** — `public` controls *who* can call the method (the method's visibility). Options are `public`, `protected`, default (package-private), and `private`.
- **Return type** — `double` is the type of value the method produces. A method that returns nothing uses the keyword `void`.
- **Method name** — `calculateArea`, a verb-like identifier following camelCase convention.
- **Parameter list** — `(double radius)` declares the inputs the caller must supply, each with a type and a name.
- **Body** — the `{ ... }` block containing the statements executed on each call.

The **method signature** is the method's *identity* used by the compiler: it consists of the **method name and the parameter list** (types and order). Crucially, the *return type and access modifier are not part of the signature*. This distinction matters enormously for overloading (Section 5) and overriding (Section 6).

> **Best Practice:** Name methods with verbs (`getTotal`, `isEmpty`, `sendInvoice`) and keep each method focused on exactly one responsibility. A method that does one thing is easy to name, test, and reuse.

### 1.2 The Method Call Stack and Method Invocation

When your program calls a method, the JVM pushes a new **stack frame** onto the **call stack** — a LIFO (last-in, first-out) structure that tracks which method is currently executing and where execution must resume when it finishes. Each frame holds the method's local variables, parameters, and the return address.

```java
public class CallStackDemo {
    public static void main(String[] args) {
        int result = add(3, 4);     // frame for main() exists; add() is pushed on top
        System.out.println(result); // after add() returns, main() resumes
    }

    static int add(int a, int b) {
        return a + b;               // a and b live only in add()'s frame
    }
}
```

*Listing 1.1 — A minimal stack trace in action. Output: `7`*

When `main` calls `add`, a new frame is created for `add`, and control jumps into it. When `add` returns, its frame is popped off the stack, the return value `7` is handed back to `main`, and `main` continues. If a method calls itself too deeply (runaway recursion), the JVM throws `StackOverflowError` — the stack has run out of space.

Every call site must resolve *which* method body actually executes — and, as you will see in Sections 5 and 6, this resolution follows two different rules: at *compile time* for overloaded methods and at *runtime* for overridden methods.

The table below maps the method types we will study to their sections:

| Method Category | Keyword(s) | Key Property | Section |
|---|---|---|---|
| Static method | `static` | Belongs to the class, not an instance | Section 2 |
| Final method | `final` | Cannot be overridden by subclasses | Section 3 |
| Private method | `private` | Visible only within the declaring class | Section 4 |
| Overloaded method | (none; same name) | Same name, different parameter list | Section 5 |
| Overridden method | (none; in subclass) | Subclass redefines inherited behavior | Section 6 |

*Table 1.1 — The five method types and where each is covered in this chapter.*

---

## 2. Static Methods

Think of a class as an architectural **blueprint** and each object as a **house** built from it. Instance methods are behaviors of the *house* ("turn on the lights," "open the door") — they need a real house to run on. Static methods are behaviors of the *blueprint itself* — they describe rules about houses in general and need no particular house at all.

### 2.1 What Makes a Method Static

A **static method** belongs to the *class*, not to any instance of the class. It can be invoked without creating an object. Because there is no instance, a static method has no `this` reference — there is no "current object" for it to inspect.

```java
public class MathUtils {
    public static int max(int a, int b) {
        return (a > b) ? a : b;   // no instance state needed — pure function of its arguments
    }
}
```

*Listing 2.1 — A static method as a pure function (fragment).*

The method `max` computes entirely from its parameters. It does not touch any instance field, which is exactly what makes it a natural candidate for `static`.

> **Analogy:** Static methods are like instructions printed on a blueprint — "all houses built from this blueprint must have a fire escape." Instance methods are like actions you perform on a finished house — "open the front door." You can follow the blueprint's instructions without any house; you cannot open a door without a house.

### 2.2 Syntax and Declaration Rules

The syntax is straightforward: the `static` modifier sits between the access modifier and the return type.

```java
public class Counter {
    private static int totalCount = 0;   // static FIELD: shared across all instances

    public static void incrementTotal() { // static METHOD
        totalCount++;                     // OK: can access static fields
        System.out.println("Total is now " + totalCount);
    }

    private int instanceCount = 0;

    public static void show() {
        // instanceCount++;  // ERROR: cannot reference instance field from static context
        // this.instanceCount; // ERROR: 'this' is not available in a static context
        System.out.println("Hello from a static method");
    }
}
```

*Listing 2.2 — Static fields and static methods (fragment).*

The declaration rules follow directly from "no instance exists":

- A static method can call **other static members** (static fields and static methods) directly.
- A static method **cannot access instance fields or instance methods** without an object reference.
- A static method **cannot use `this` or `super`** — there is no current instance.
- `static` methods are resolved at **compile time**, not runtime (more in Section 7.3).

### 2.3 Calling Static Methods: Class Name vs. Instance Reference

The correct, preferred way to call a static method is through the **class name**:

```java
Math.max(10, 5);          // Correct — calls static max on the Math class
Counter.incrementTotal(); // Correct — calls static method via the class
```

Java also *permits* calling a static method through an instance reference, but this is a **bad idea** — it is misleading, and the compiler warns against it. The JVM ignores the object entirely and uses only the *declared type* of the reference.

```java
Counter c = new Counter();
c.incrementTotal(); // compiles with a warning: "static method should be accessed in a static way"
```

*Listing 2.3 — Calling a static method via an instance (fragment).*

> **Pitfall:** Calling a static method through an instance reference is legal but confusing. It suggests the call depends on the object's state when it does not. Always call static methods via the class name — readers, and your future self, will thank you.

### 2.4 The `main` Method as the Canonical Example

You have written `main` dozens of times without perhaps realizing you were writing a static method. The JVM must launch your program *before any objects exist*, so the entry point has to be static:

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello, world!"); // System.out is a static field; println is an instance method
    }
}
```

*Listing 2.4 — The canonical static method: the program entry point.*

Three modifiers must be exactly right, or the JVM refuses to launch the program:

- `public` — so the JVM (an external caller) can access it.
- `static` — so it can run without an instance.
- `void` — the entry point returns nothing to the OS.

The parameter `String[] args` receives command-line arguments. Note the subtlety in the body: `System.out` is a *static field* holding a `PrintStream` object, and `println` is an *instance method* invoked on that object — a perfect miniature example of both method types working together.

### 2.5 Utility Classes and Factory Patterns — Real-World Use Cases

Static methods shine when behavior is a pure function of its inputs, or when you need to create objects in a controlled way.

**Utility classes** group related operations that share no state. The Java standard library is full of them: `Math` (`Math.sqrt`, `Math.random`), `Collections` (`Collections.sort`, `Collections.max`), `Arrays` (`Arrays.copyOf`), and `String` (`String.valueOf`, `String.format`). Such classes often declare a `private` constructor to prevent instantiation, signaling "this class is a toolbox, not a mold":

```java
public class TextTools {
    private TextTools() { /* prevent instantiation — utility class */ }

    public static String reverse(String input) {
        return new StringBuilder(input).reverse().toString();
    }

    public static int countVowels(String input) {
        int count = 0;
        for (char ch : input.toLowerCase().toCharArray()) {
            if ("aeiou".indexOf(ch) >= 0) count++;
        }
        return count;
    }
}
```

*Listing 5.5-referenced — a typical utility class pattern (fragment).*

**Factory methods** are static methods that create and return instances, often choosing a subtype or configuring the object. `Integer.valueOf(int)` returns a cached `Integer` where possible; `LocalDate.of(y, m, d)` builds a date safely; `Collections.emptyList()` returns an immutable empty list without you writing `new`. The factory pattern centralizes construction logic and gives it a descriptive name:

```java
public class Book {
    private final String title;
    private Book(String title) { this.title = title; }

    public static Book of(String title) {        // named constructor via static factory
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        return new Book(title);                  // validation happens in ONE place
    }

    public static Book specialEdition(String title) {
        return new Book("Special Edition: " + title);
    }
}
```

*Listing 2.5 — Static factory methods in practice.*

> **Real World:** The `Collections` class cannot be instantiated and offers dozens of static utilities operating on collections. When you write `Collections.sort(list)`, you rely on a static method — no `Collections` object ever exists. This is the utility-class idiom used throughout the JDK and in libraries like Guava.

### 2.6 Common Pitfalls: Accessing Instance Fields and `this`

Because beginners often blur "class" and "instance," the classic error is trying to use instance state inside a static method:

```java
public class Broken {
    private int value = 10;                 // instance field

    public static void printValue() {
        // System.out.println(value);       // COMPILE ERROR: non-static variable value
        //                                 // cannot be referenced from a static context
        // System.out.println(this.value);  // COMPILE ERROR: 'this' cannot be used in a static context
    }
}
```

*Listing 2.6 — The classic static/instance confusion. Both statements are compile errors.*

**Why?** When `printValue` runs, no `Broken` object necessarily exists — so `value`, which lives *inside* each object, may not exist either. The compiler refuses to guess. The fix is either to make `value` static (if it is genuinely class-level state) or to pass an instance in:

```java
public static void printValue(Broken b) {   // accept the instance as a parameter
    System.out.println(b.value);
}
```

Another subtle pitfall: because static methods bind at compile time, calling a "static" version of a method through an instance reference can hide the fact that subclasses cannot override it polymorphically (see Section 7.3).

---

## 3. Final Methods

A class is a contract with the world. Sometimes, you want to say: *this behavior is the behavior — nobody may change it.* That is the job of the `final` modifier.

### 3.1 Purpose of `final` Methods

A **final method** is a method that **cannot be overridden** by a subclass. Once declared `final`, its implementation is fixed for the entire inheritance hierarchy below it. It is a promise: "every subclass, now and forever, will use this exact behavior."

Why would you want that? Overriding is powerful, but it is also a point of fragility. If a subclass replaces a method that holds a critical invariant — a security check, a consistency guarantee — it could break the whole design. Marking the method `final` makes the invariant unbreakable.

> **Analogy:** A `final` method is like a **sealed law**: Parliament writes it and says "no amendment, ever." A non-final method is an ordinary law that later governments may revise. Both are legitimate in a democracy — but for the rules that protect fundamental rights, you want them sealed.

### 3.2 Syntax and Where `final` Can Appear

The `final` keyword appears in the method declaration, after the access modifier:

```java
public class Vehicle {
    private final int maxSpeed;

    public Vehicle(int maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public final int getMaxSpeed() {     // final method: cannot be overridden
        return maxSpeed;
    }

    public void start() {                 // ordinary method: CAN be overridden
        System.out.println("Vehicle starting...");
    }
}
```

*Listing 3.1 — A final accessor (fragment).*

Related forms of `final`:

- **Final class:** `public final class String { ... }` — a final class *cannot be extended at all*, and therefore *every* method of a final class is effectively final (the compiler knows no subclass can ever exist to override them).
- **Final variables:** `final int LIMIT = 100;` — the variable can be assigned only once. (A common beginner confusion: `final` on a *field* prevents reassignment; `final` on a *method* prevents overriding. Same word, different rules.)
- You cannot mark a method both `static` and `final` *and* expect overriding behavior — static methods are never overridden anyway (Section 7.3), but the combination is legal and simply reinforces that the behavior is fixed.

### 3.3 Why and When to Use Them: Template Method Pattern, Security, and Invariants

Three concrete motivations justify the `final` modifier.

**1. The Template Method Pattern.** A base class defines the *skeleton* of an algorithm and allows subclasses to fill in only specific steps. The skeleton itself must not change, so it is declared `final`:

```java
public abstract class DataImporter {
    // TEMPLATE: the algorithm skeleton — fixed for all subclasses
    public final void importFile(String path) {
        String data = readFile(path);
        validate(data);
        save(parse(data));
        log(path);
    }

    protected abstract void validate(String data);  // steps for subclasses to implement
    protected abstract Object parse(String data);
    protected void log(String path) { System.out.println("Imported " + path); }
    private String readFile(String path) { return "raw data"; }
    private void save(Object o) { /* ... */ }
}
```

*Listing 3.2 — The template method pattern protects the algorithm's structure (fragment).*

A subclass can customize `validate` and `parse`, but it *cannot* reorder or remove steps — `importFile` is `final`.

**2. Security and invariants.** In the JDK, `String` is final, and its methods cannot be overridden. This prevents malicious subclasses from faking string behavior, which protects security-sensitive code that trusts `String`. Similarly, if your class's correctness depends on a method always behaving a certain way — a bank transfer's audit step, a cache-invalidation routine — mark it `final`.

**3. Performance.** Historically, the JIT compiler could inline `final` methods more aggressively. Modern JVMs do this regardless, so treat performance as a *side benefit*, not the reason — the real reasons are design and safety.

> **Real World:** The `Object.getClass()` method is `final`. The JVM relies on every object reporting its true runtime class; allowing subclasses to fake it would break reflection, debugging, and the entire type system. Locking such foundational methods is standard practice in frameworks.

### 3.4 Compile-Time Error Demonstration

Attempting to override a final method is a compile-time error. The compiler catches it *before* your program ever runs:

```java
public class Sport {
    public final void playByOfficialRules() {
        System.out.println("Play by official rules");
    }
}

public class Chess extends Sport {
    // @Override
    // public void playByOfficialRules() { ... }
    // COMPILE ERROR: cannot override the final method from Sport
}
```

*Listing 3.3 — Overriding a final method is rejected by the compiler (fragment).*

The exact compiler message resembles:

```
Chess.java:5: error: playByOfficialRules() in Chess cannot override
    playByOfficialRules() in Sport
  overridden method is final
```

The compiler is not being pedantic; it is protecting the contract the base class author established. If you find yourself wanting to override a `final` method, step back: the author *deliberately* locked it. Either the design intends for you to use composition instead of inheritance, or you need a different hook point (often a `protected` non-final method provided for exactly that purpose).

> **Best Practice:** Default to *non-final* methods unless you have a concrete reason to lock behavior. Overuse of `final` makes frameworks impossible to extend; underuse invites fragile overrides. Mark `final` only the methods whose behavior must be invariant.

---

## 4. Private Methods

If `final` controls *change over time*, `private` controls *visibility in space*. A private method is the most restricted member in Java: it is accessible only within the class that declares it.

### 4.1 Visibility Rules — Accessible Only Within the Declaring Class

The access modifiers, from most to least restrictive:

| Modifier | Accessible in same class | Same package | Subclass (other package) | Anywhere |
|---|---|---|---|---|
| `private` | ✅ | ❌ | ❌ | ❌ |
| *default* (none) | ✅ | ✅ | ❌ | ❌ |
| `protected` | ✅ | ✅ | ✅ | ❌ |
| `public` | ✅ | ✅ | ✅ | ✅ |

*Table 4.1 — Access modifier visibility matrix.*

A `private` method can be called by *any* code inside the same class — constructors, other methods, other private methods — but by nothing outside, including subclasses.

### 4.2 Encapsulation and Implementation Hiding

Encapsulation is the principle of hiding internal details behind a stable public interface. Private methods are the workhorses of that principle: they let you break a complex public operation into small, readable, testable steps *without* exposing those steps to the world.

```java
public class OrderService {
    public void placeOrder(Order order) {
        validateOrder(order);              // internal step #1
        double total = computeTotal(order);// internal step #2
        persist(order, total);             // internal step #3
        notifyCustomer(order);             // internal step #4
    }

    private void validateOrder(Order order) { /* checks stock, prices, addresses */ }
    private double computeTotal(Order order) { /* applies discounts and tax */ return 0.0; }
    private void persist(Order order, double total) { /* writes to database */ }
    private void notifyCustomer(Order order) { /* sends email */ }
}
```

*Listing 4.1 — Public facade with private helpers (fragment).*

**Why private?** Today all four steps live in one class. Tomorrow you might reorder them, replace the email service with SMS, or switch databases. Because callers only see `placeOrder`, you can rewrite every helper without breaking a single client. That is the maintainability payoff of implementation hiding.

> **Real World:** Service classes in Spring applications are a perfect example. The public `@Service` methods form the API; the dozen or so `private` methods beneath them hold SQL snippets, validation logic, and notification details. IDEs even let you extract a selection into a private method with one keystroke — a daily habit for working developers.

### 4.3 Private Methods and Overriding — Shadowed, Not Overridden

Here is the subtle part: **private methods cannot be overridden**, because a subclass cannot even *see* them. If a subclass declares a method with the same signature as a private method in its superclass, it is a brand-new method — completely unrelated. We say the subclass's method **hides (shadows)** the parent's.

```java
public class Parent {
    private void greet() { System.out.println("Parent says hi"); }

    public void callGreet() {
        greet();   // always invokes Parent.greet(), never Child.greet()
    }
}

public class Child extends Parent {
    private void greet() { System.out.println("Child says hi"); } // unrelated method

    public void callChildGreet() {
        greet();   // invokes Child.greet() — different method entirely
    }
}

public class PrivateDemo {
    public static void main(String[] args) {
        Parent p = new Child();
        p.callGreet();        // prints "Parent says hi" — Child.greet is invisible to Parent
        // p.greet();         // COMPILE ERROR: greet() has private access in Parent
    }
}
```

*Listing 4.2 — Private methods are hidden, never overridden. Output: `Parent says hi`*

Note that `@Override` on the subclass's `greet()` would be a compile error, precisely because the compiler knows there is nothing to override. This hiding behavior is why `final` and `private` interact so comfortably: a private method is *implicitly* final in the sense that no subclass can ever change how the parent calls it.

> **Pitfall:** Do not write a private method in a subclass expecting "polymorphic" behavior. A reference of the parent type will always invoke the parent's private method. If you want subclasses to customize behavior, the method must be `protected` or `public` and non-private.

### 4.4 Using Private Methods for Code Reuse and Reducing Duplication

Inside one class, duplication breeds bugs: fix a formula in one place and forget the copy elsewhere. Private methods let you centralize repeated logic:

```java
public class ReportGenerator {
    public String customerSummary(Customer c) {
        return header(c) + "Total orders: " + count(c.orders()) + line();
    }

    public String inventorySummary(Product p) {
        return header(p) + "Units left: " + p.unitsLeft() + line();
    }

    private String header(Object o) { return "== " + o.getClass().getSimpleName() + " ==\n"; }
    private String line()           { return "\n" + "-".repeat(30) + "\n"; }

    private int count(java.util.List<?> items) { return items.size(); }
}
```

*Listing 4.3 — Private helpers eliminate duplicated formatting logic (fragment).*

**Why private and not `protected` or `public`?** The helpers `header`, `line`, and `count` are *implementation details* — no external caller needs them, and no subclass should depend on them. Keeping them private gives you the freedom to refactor them freely later.

> **Best Practice:** Ask of every method: *Who should be allowed to call this?* If the honest answer is "only this class," make it `private`. Visibility is not decoration — it is a promise about your API's stability.

---

## 5. Overloaded Methods

Sometimes the same *concept* deserves the same *name*, even with different inputs. Java supports this directly with **method overloading**.

### 5.1 Definition: Same Method Name, Different Parameter Lists

**Overloading** means declaring multiple methods in the *same class* with the **same name** but **different parameter lists** (different types, different count, or both). The compiler treats them as entirely separate methods.

```java
public class Greeter {
    public String greet() {
        return "Hello!";
    }

    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    public String greet(String name, String title) {
        return "Hello, " + title + " " + name + "!";
    }
}
```

*Listing 5.1 — Three overloads of `greet` (fragment).*

### 5.2 Overload Resolution Rules

When the compiler sees a call like `greet("Ada")`, it must pick *one* overload. It uses a strict priority order:

1. **Exact match** — the argument types match a parameter list exactly.
2. **Widening** — promote the argument to a wider primitive type (`int` → `long` → `float` → `double`), or to a supertype for references.
3. **Boxing** — wrap a primitive in its wrapper class (`int` → `Integer`).
4. **Varargs** — pack the arguments into an array (`int...`).

```java
public class ResolutionDemo {
    public static void choose(int n)        { System.out.println("int"); }
    public static void choose(long n)       { System.out.println("long"); }
    public static void choose(Integer n)    { System.out.println("Integer"); }
    public static void choose(int... nums)  { System.out.println("varargs"); }

    public static void main(String[] args) {
        choose(42);      // exact match         -> "int"
        choose(42L);     // exact match         -> "long"
        int x = 5;
        // choose(Integer.valueOf(x)) implicit -> "Integer" (boxing, only if no widening/exact)
        choose(x, 7, 9); // varargs             -> "varargs"
    }
}
```

*Listing 5.2 — Resolution priority in action. Output: `int`, `long`, `varargs`*

The compiler tries steps in order, and the first viable match wins. `choose(5)` never reaches `Integer` because `choose(int)` is an exact match; a call like `choose(5.5)` would fail entirely because `double` cannot widen or box into any of the options.

### 5.3 Valid vs. Invalid Overloads

Overloads must differ in their **parameter lists**. The return type, access modifier, and thrown exceptions are *not* part of the signature and cannot be the sole difference.

**Invalid — same parameters, different return type:**

```java
public class InvalidOverload {
    public int calc(int x) { return x; }
    // public double calc(int x) { return x * 1.0; }
    // COMPILE ERROR: calc(int) is already defined
}
```

*Listing 5.3 — Return type alone cannot differentiate overloads (fragment).*

**Valid — different parameter types or counts:**

```java
public class ValidOverload {
    public double calc(double x) { return x; }      // different parameter TYPE
    public int calc(int x, int y) { return x + y; } // different parameter COUNT
    public int calc(int... xs) { return xs.length; }// varargs counts as a distinct list
}
```

*Listing 5.4 — Valid overloads (fragment).*

**Why can't the return type differentiate?** Because Java allows you to *ignore* a method's return value — `calc(5);` as a statement. If two `calc(int)` methods differed only in return type, the compiler could not tell which one the bare call `calc(5)` meant.

### 5.4 Real-World Examples

**`String.valueOf(...)`** — The JDK overloads `valueOf` for every primitive type and `char[]`, so `String.valueOf(42)` and `String.valueOf(true)` both work:

```java
String a = String.valueOf(42);     // "42"
String b = String.valueOf(3.14);   // "3.14"
String c = String.valueOf(true);   // "true"
```

*Listing 5.5 — Overloads in `String.valueOf` (fragment).*

**`PrintStream.println(...)`** — `System.out.println` is overloaded for every primitive, `String`, `char[]`, `Object`, and the no-argument version. The `Object` overload is the "catch-all": any type without a more specific overload is converted via `String.valueOf(Object)`.

**Constructor overloading** — Constructors are methods too, and overloading them lets clients build objects at different levels of detail:

```java
public class Person {
    private final String name;
    private final int age;
    private final String email;

    public Person(String name) {
        this(name, 0, "");           // delegate to the full constructor
    }

    public Person(String name, int age) {
        this(name, age, "");
    }

    public Person(String name, int age, String email) { // the "real" constructor
        this.name = name;
        this.age = age;
        this.email = email;
    }
}
```

*Listing 5.6 — Constructor overloading with delegation via `this(...)`.*

> **Real World:** Data Transfer Objects (DTOs) and Builder-pattern classes lean heavily on constructor overloading — a DTO may offer a no-arg constructor for serialization frameworks, a minimal constructor for quick tests, and a full constructor for production use. Frameworks like Jackson and Hibernate reflectively call these overloads, so naming and signature design directly affect library compatibility.

### 5.5 Ambiguity Pitfalls

Overloading is compile-time resolution, and sometimes the compiler simply *cannot* decide — or, worse, silently picks a surprising overload.

**The `null` literal.** `null` is compatible with *every* reference type. Passing it to overloads is ambiguous unless exactly one overload is more specific:

```java
public class Ambiguous {
    static void print(String s)    { System.out.println("String"); }
    static void print(Integer i)   { System.out.println("Integer"); }

    public static void main(String[] args) {
        // print(null);
        // COMPILE ERROR: reference to print is ambiguous —
        // both print(String) and print(Integer) match
        print((String) null);   // cast resolves the ambiguity -> "String"
    }
}
```

*Listing 5.7 — The `null` literal is ambiguous between reference-type overloads. Output: `String`*

**Narrowing vs. boxing.** Java never silently *narrows* a primitive (`double` → `int`) to satisfy an overload, and it prefers widening over boxing:

```java
public class NarrowingDemo {
    static void go(int n)  { System.out.println("int"); }
    static void go(long n) { System.out.println("long"); }

    public static void main(String[] args) {
        byte b = 5;
        go(b);   // widening byte -> int  -> "int" (narrowing to byte is NOT considered)
    }
}
```

*Listing 5.8 — Widening beats boxing and narrowing is never automatic. Output: `int`*

> **Pitfall:** Ambiguity errors are the compiler's *protection*. When a call is genuinely ambiguous, do not try to "fix" it with casts unless you are certain; instead reconsider whether the overloads themselves are well-designed. Two overloads accepting unrelated types that both match `null` are a design smell.

---

## 6. Overridden Methods

Where overloading is about *many methods, one name, chosen at compile time*, overriding is about *one method name, redefined by a subclass, chosen at runtime*. This is the heart of polymorphism.

### 6.1 Definition: Subclass Redefines an Inherited Method

**Overriding** occurs when a subclass declares a method with the same signature as a method in its superclass, *replacing* the inherited behavior for objects of the subclass. When you call the method on a subclass instance through a superclass reference, the subclass's version runs.

```java
public class Animal {
    public void speak() { System.out.println("Some animal sound"); }
}

public class Dog extends Animal {
    @Override
    public void speak() { System.out.println("Woof!"); }
}

public class Cat extends Animal {
    @Override
    public void speak() { System.out.println("Meow!"); }
}

public class PolymorphismDemo {
    public static void main(String[] args) {
        Animal pet = new Dog();
        pet.speak();   // prints "Woof!" — Dog's version runs, chosen at RUNTIME
        Animal other = new Cat();
        other.speak(); // prints "Meow!"
    }
}
```

*Listing 6.1 — The essence of overriding. Output: `Woof!`, `Meow!`*

> **Analogy:** Overriding is like a **child learning a family recipe but making it their own** — same dish name, same basic steps, their own twist. A cookbook reference (`Animal pet`) pointing at a `Dog` object always gets the *dog's* version of `speak`, because the dog knows best what a dog does.

### 6.2 Rules: Same Signature, Covariant Return, No Stricter Access, No New Checked Exceptions

The compiler enforces a precise contract on overrides:

1. **Same signature** — same name and parameter list. (Parameter *names* may differ; types must not.)
2. **Covariant return type** — the return type may be the same *or a subtype* of the parent's return type. `Dog speak()` can return `Dog` where the parent returns `Animal`.
3. **No stricter access** — an override may widen access (`protected` → `public`) but must not narrow it (`public` → `private`).
4. **No new checked exceptions** — an override may throw *fewer or narrower* checked exceptions, never *new or broader* ones. (Unchecked exceptions like `RuntimeException` are always allowed.)

```java
public class Shape {
    public Shape copy() throws CloneNotSupportedException { return new Shape(); }
}

public class Circle extends Shape {
    @Override
    public Circle copy() {        // covariant return: Circle IS-A Shape
        return new Circle();      // no checked exception at all — that's allowed
    }
}
```

*Listing 6.2 — Covariant returns and relaxed exceptions (fragment).*

**Why these rules?** All four protect *callers* who hold a superclass reference. If `Shape` is the compile-time type, the caller expects `copy()` to return a `Shape` (a `Circle` is fine) and expects no exceptions beyond those declared. Rules 3 and 4 ensure a reference typed as `Animal` can call `speak()` as safely as it could on an `Animal`.

### 6.3 The `@Override` Annotation and Why It Is Essential

`@Override` is an **annotation** — a hint to the compiler (and to future readers) that the method intends to override a superclass method. The compiler then *verifies* that an override actually exists; if not, it errors:

```java
public class Dog extends Animal {
    @Override
    public void speaks() { System.out.println("Woof!"); }
    // COMPILE ERROR: method does not override or implement a method from a supertype
}
```

*Listing 6.3 — A typo caught by `@Override` (fragment).*

Without `@Override`, the typo `speaks()` would silently create a new method, the real `speak()` would never be customized, and your program would break at *runtime* in the most confusing way possible. With the annotation, the compiler catches the mistake at compile time.

> **Best Practice:** Annotate *every* overriding method with `@Override`. It costs nothing, documents intent, and converts an entire class of silent bugs into compile-time errors. This is one of the cheapest safety measures in all of Java.

### 6.4 Runtime Polymorphism / Dynamic Dispatch — Foundation of `equals`, `toString`, `hashCode`

**Dynamic dispatch** means the JVM chooses which overridden method to run based on the *actual runtime type* of the object, not the declared type of the reference. This is what makes frameworks and collections work.

Every class inherits `toString`, `equals`, and `hashCode` from `Object`. Overriding them correctly is the foundation of correct collections, logging, and object equality:

```java
public final class Point {
    private final int x;
    private final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;                    // same reference: fast path
        if (!(o instanceof Point p)) return false;     // wrong type (pattern matching)
        return p.x == x && p.y == y;                   // compare all significant fields
    }

    @Override
    public int hashCode() {
        return 31 * x + y;                             // consistent with equals
    }

    @Override
    public String toString() {
        return "Point(" + x + ", " + y + ")";
    }
}
```

*Listing 6.4 — The `equals`/`hashCode`/`toString` contract in action (Java 16+ pattern matching).*

Now the `HashMap` machinery, the `Collections.sort` comparators, and your own debug logs all get correct behavior *automatically*, because they call `equals` and friends polymorphically on `Object` references and the JVM dispatches to `Point`'s versions. The rule: **if two objects are `equals`, they must have the same `hashCode`** — otherwise `HashMap` may fail to find them.

> **Real World:** Overriding `equals` and `hashCode` is precisely how `HashMap` keys work in production. Records (Java 16+) give you correct `equals`/`hashCode`/`toString` for free; for ordinary classes you must write them — and getting them wrong is one of the most common sources of mysterious production bugs.

### 6.5 `super.method()` Calls and Calling Overridden Methods from Constructors

**`super.method()`** lets an overriding method invoke the parent's version, typically to extend rather than replace behavior:

```java
public class SavingsAccount extends Account {
    @Override
    public void deposit(double amount) {
        super.deposit(amount);            // run the base logic first
        System.out.println("Savings bonus credited!"); // then add subclass behavior
    }
}
```

*Listing 6.5 — Extending behavior with `super.deposit()` (fragment).*

**The constructor pitfall.** If a constructor calls a *non-final, overridable* method, the subclass's override runs *before the subclass's fields are initialized*:

```java
public class Base {
    public Base() {
        show();   // dispatches to the MOST-DERIVED override — dangerous!
    }
    public void show() { System.out.println("Base show"); }
}

public class Derived extends Base {
    private final String message = "initialized!";

    public Derived() { super(); } // Base constructor runs first

    @Override
    public void show() {
        System.out.println("Derived show, message = " + message);
    }
}
```

*Listing 6.6 — The constructor-override pitfall.*

`new Derived()` prints `Derived show, message = null` — because at the time `Base()` calls `show()`, `Derived`'s fields have not yet been initialized.

> **Pitfall:** Never call an overridable method from a constructor. It will dispatch to the subclass override on an only-partially-constructed object, and the subclass's fields will be `null`/`0`. This is one of the most infamous subtle bugs in Java — the fix is to make the method `final`, make it `private`, or restructure so constructors call only non-polymorphic code.

### 6.6 Real-World Framework Use: Spring/DAO Overrides and Default Methods

**Spring/DAO pattern.** Service and repository interfaces define contracts; implementations override them. The framework calls your methods polymorphically:

```java
public interface UserRepository {
    Optional<User> findById(long id);
}

public class JdbcUserRepository implements UserRepository {
    @Override
    public Optional<User> findById(long id) {
        // JDBC query code...
        return Optional.of(new User(id, "Ada"));
    }
}

public class UserService {
    public String describeUser(UserRepository repo, long id) {
        return repo.findById(id).map(User::toString).orElse("not found");
    }
}
```

*Listing 6.7 — Interface implementation as overriding (fragment).*

**Interface default methods vs. overrides.** A `default` method in an interface provides a fallback implementation. A class may *override* it — and the class's override always wins over the interface default:

```java
public interface Discountable {
    default double applyDiscount(double price) {
        return price * 0.9;      // default 10% off
    }
}

public class PremiumCustomer implements Discountable {
    @Override
    public double applyDiscount(double price) {
        return price * 0.75;     // premium: 25% off — override wins
    }
}
```

*Listing 6.8 — Overriding an interface default method (fragment).*

> **Real World:** The `Comparator` interface ships many default methods (`thenComparing`, `reversed`) precisely so library users can override or compose comparison logic without breaking existing implementations. The whole Spring ecosystem — `@Controller`, `@Service`, repository interfaces — is built on the runtime dispatch that overriding provides.

---

## 7. Comparing the Method Types

You now hold all five tools. Let us line them up side by side.

### 7.1 Comprehensive Comparison Table

| Method Type | Keyword(s) | Scope | Can Be Overridden? | Can Be Overloaded? | Typical Use Case |
|---|---|---|---|---|---|
| Static method | `static` | Class (no instance needed) | No (hidden/compile-time) | Yes | Utility methods (`Math.max`), factories (`LocalDate.of`), `main` |
| Final method | `final` | Instance (of declaring class) | No (compiler forbids it) | Yes | Template method skeleton, security/invariant-critical behavior |
| Private method | `private` | Within the declaring class only | No (invisible to subclasses) | Yes | Internal helpers, encapsulation, DRY inside a class |
| Overloaded method | none (same name, different params) | Same class | Yes | — (this *is* overloading) | `println(...)`, `String.valueOf(...)`, constructors |
| Overridden method | none (`@Override` annotation) | Subclass of the declaring class | — (this *is* overriding) | Yes | Polymorphic behavior, `equals`/`toString`, framework hooks |

*Table 7.1 — Comprehensive comparison of the five method types.*

### 7.2 When to Choose Each Method Type — Decision Guidance

Start with a single question: **does this behavior depend on the object's state?**

- If **no** — the result is a pure function of the arguments, or a class-level operation — prefer **`static`**. Think `Math.max`, `Collections.sort`, factory methods.
- If **yes** — the behavior needs instance fields — use an **instance method**. Then ask two more questions:
  - **Should subclasses be allowed to change it?** If the behavior must be invariant for correctness or security, make it **`final`**. If it is part of an algorithm skeleton others extend, keep it non-final and `protected` for hooks.
  - **Should anyone outside the class call it?** If only the class needs it, make it **`private`**. If only subclasses need it, use `protected`.
- **Overloading** is a readability decision: use the same name for the same *concept* at different parameter shapes. **Overriding** is a design decision: use it to customize inherited behavior polymorphically.

**API design and maintainability.** These five choices are the primary vocabulary of your public API. `static` tells callers "no state, safe to call anywhere"; `final` tells them "this contract is sealed"; `private` tells them "this is not your business — I can change it anytime." Overloads shape how natural your API reads at the call site, and overrides determine how much future behavior you (or framework users) can adapt. In production codebases, a well-chosen method type *is* the documentation: it encodes intent that no comment could express as cheaply. Getting these decisions wrong — making everything `public` by default, sealing methods that must be extensible, hiding hooks that frameworks need — produces code that is hard to evolve and expensive to maintain.

### 7.3 How These Types Interact

**Static + final.** Legal and meaningful: `public static final int MAX = 100;` is a constant; `public static final void util()` fixes a utility's behavior forever (though since static methods are never overridden, `final` mainly documents intent).

**Private + final.** A private method *cannot* be overridden (it is invisible), so `final` is redundant — but harmless. The compiler accepts `private final void helper()`, and many style guides allow it for clarity.

**Static vs. instance overriding.** Static methods **hide**, never override. If a subclass declares a static method with the same signature as the parent's, the method is *hidden*, and which one runs is decided by the *compile-time type of the reference*:

```java
public class SuperStat {
    public static void who() { System.out.println("Super"); }
}
public class SubStat extends SuperStat {
    public static void who() { System.out.println("Sub"); }
}

public class StaticHideDemo {
    public static void main(String[] args) {
        SuperStat.who();              // "Super" — resolved at compile time
        SubStat.who();                // "Sub"
        SuperStat s = new SubStat();
        s.who();                      // "Super"! NOT polymorphic — static methods hide
    }
}
```

*Listing 7.1 — Static methods are hidden, not overridden. Output: `Super`, `Sub`, `Super`*

> **Pitfall:** Because static methods bind at compile time, never call a hidden static method through an instance reference or a superclass variable — you will get the parent's version regardless of the actual object. If you need polymorphism, the methods must be instance (non-static) methods.

---

## 8. Common Mistakes and Best Practices

### 8.1 Common Mistakes and Their Corrections

1. **Calling static methods through instance references.**
   *Mistake:* `counter.incrementTotal();` where `incrementTotal` is static.
   *Correction:* `Counter.incrementTotal();` — call via the class name; it documents that no instance is involved.

2. **Accessing instance fields from a static method.**
   *Mistake:* Reading `this.value` inside a `static` method.
   *Correction:* Make the field static if it is genuinely class-level, or pass an instance as a parameter.

3. **Trying to override a `final` method.**
   *Mistake:* Re-declaring a `final` method in a subclass.
   *Correction:* Use composition or ask the author for a different hook point; a `final` method is a deliberate contract.

4. **Forgetting `@Override`.**
   *Mistake:* A subclass method with a typo — `speaks()` instead of `speak()` — silently not overriding anything.
   *Correction:* Annotate every override; the compiler then catches signature mistakes at compile time.

5. **Narrowing access when overriding.**
   *Mistake:* Overriding a `public` method with a `protected` one.
   *Correction:* Widen (or keep) access; a subclass can never be *less* accessible than its parent.

6. **Breaking the `equals`/`hashCode` contract.**
   *Mistake:* Overriding `equals` but not `hashCode`, or using mutable fields in `hashCode`.
   *Correction:* Override both consistently and use only immutable, significant fields; `equals` objects must share a `hashCode`.

7. **Calling overridable methods from constructors.**
   *Mistake:* A base constructor invoking a non-final method that subclasses override.
   *Correction:* Make such methods `final` or `private`, or refactor to avoid polymorphic calls during construction.

8. **Designing ambiguous overloads.**
   *Mistake:* Overloads like `print(String)` and `print(Integer)` where `print(null)` is ambiguous.
   *Correction:* Avoid overloads with unrelated reference types that both accept `null`; if unavoidable, cast at the call site deliberately.

### 8.2 Best-Practices Checklist

- [ ] Prefer `static` only for behavior independent of instance state; name utility classes with a plural noun and give them a `private` constructor.
- [ ] Use `@Override` on every overridden method — always.
- [ ] Make access as *restrictive as possible*: `private` by default for helpers, `protected` only for subclass hooks, `public` only for the real API.
- [ ] Mark methods `final` only when behavior must be invariant; do not seal everything out of habit.
- [ ] In overrides, never narrow access and never add checked exceptions.
- [ ] Keep the `equals`/`hashCode`/`toString` contract consistent, using immutable fields.
- [ ] Never call overridable methods from constructors.
- [ ] For overloads, keep each overload's behavior semantically consistent — callers expect the same *idea* under one name.
- [ ] Use `this(...)` to delegate between overloaded constructors and avoid duplication.
- [ ] Document intent: a comment like `// static: no state, pure function` near non-obvious choices is cheap and valuable.

---

## 9. Practice Exercises

### 9.1 The Exercises

**Exercise 1 — Static or Instance? (Beginner)**
For each method, decide whether it should be `static` or instance, and justify:
(a) `double computeArea(double radius)` on a `Circle`;
(b) `void setName(String name)` on a `Student`;
(c) `String toUpper(String input)` on a `StringTools` class;
(d) `LocalDate today()` on a `DateFactory`.
*Hint:* Ask: does the method need to read or write fields of the receiving object?

**Exercise 2 — Final Method Error (Beginner)**
Given `class Device { public final void powerOn() { } }`, write a subclass `Phone extends Device` that tries to override `powerOn()`. Compile it, read the error, then fix it by removing the override and instead calling `powerOn()` from a new non-conflicting method.
*Hint:* The compiler message will mention "overridden method is final."

**Exercise 3 — Private Helper Refactor (Intermediate)**
Write a class `LoanCalculator` with a public method `monthlyPayment(principal, rate, years)` and a second public method `totalInterest(principal, rate, years)`. Both need to compute the monthly rate and the number of payments. Extract the shared calculations into `private` helper methods and show that the public methods stay correct.
*Hint:* `monthlyRate = rate / 1200.0` and `months = years * 12`.

**Exercise 4 — Overload Puzzle (Intermediate)**
Given these overloads — `void play(int n)`, `void play(long n)`, `void play(Integer n)`, `void play(int... ns)` — determine the output (or error) of: `play(5)`, `play(5L)`, `play(5, 6)`, and `play((Integer) 5)`. Then write the program and verify.
*Hint:* Apply the resolution order: exact → widening → boxing → varargs.

**Exercise 5 — Polymorphic Override + Contract (Intermediate)**
Create `abstract class Shape` with `abstract double area()` and an overridable `String description()`. Implement `Circle` and `Square`. In `main`, store both in a `Shape[]`, loop and print `description()` and `area()`. Confirm the runtime type determines which `description` runs.
*Hint:* You need exactly one `area()` implementation per subclass — the abstract method *forces* overriding.

**Exercise 6 — The Constructor Trap (Advanced)**
Build a class `Base` whose constructor calls a public non-final method `init()`, and a subclass `Derived` that overrides `init()` and sets a field. Run the program and observe the `null`/default output. Then fix it using one of: making `init()` final, making it private, or moving the call out of the constructor. Explain which fix you chose and why.
*Hint:* Recall Section 6.5 — field initializers of the subclass run *after* the superclass constructor.

**Exercise 7 — Design a small service API (Advanced)**
Design a class `PaymentService` with: a `public` method `process(Payment p)`; a `private` method `validate(Payment p)` reused by `process`; an overloaded `process(Payment p, boolean notify)`; a `final` method `getCurrency()`. Then subclass it to add a `BonusPaymentService` that overrides `process` only if possible — and document why any restriction you hit exists.
*Hint:* Review Section 7.1 before choosing the modifiers; some choices are mutually incompatible with overriding.

### 9.2 Reference Solutions

**Solution 1 — Static or Instance?**

```java
public class Circle {
    private final double radius;
    public Circle(double radius) { this.radius = radius; }

    public double computeArea() {          // INSTANCE: needs the radius field
        return Math.PI * radius * radius;
    }
}

public class Student {
    private String name;
    public void setName(String name) { this.name = name; } // INSTANCE: writes a field
}

public class StringTools {
    private StringTools() { }                               // prevent instantiation
    public static String toUpper(String input) {            // STATIC: pure function
        return input.toUpperCase();
    }
}

public class DateFactory {
    public static java.time.LocalDate today() {             // STATIC: no state needed
        return java.time.LocalDate.now();
    }
}
```

**Solution 2 — Final Method Error**

```java
public class Device {
    public final void powerOn() {
        System.out.println("Device powered on");
    }
}

public class Phone extends Device {
    // @Override
    // public void powerOn() { }   // COMPILE ERROR: cannot override final method

    public void powerOnWithBell() { // new, non-conflicting method
        powerOn();                  // calls the inherited final method
        System.out.println("Ring ring!");
    }

    public static void main(String[] args) {
        new Phone().powerOnWithBell();
    }
}
// Output: Device powered on
//         Ring ring!
```

**Solution 3 — Private Helper Refactor**

```java
public class LoanCalculator {
    public double monthlyPayment(double principal, double annualRate, int years) {
        double r = monthlyRate(annualRate);        // private helper
        int n = months(years);                     // private helper
        if (r == 0) return principal / n;
        double factor = Math.pow(1 + r, n);
        return principal * r * factor / (factor - 1);
    }

    public double totalInterest(double principal, double annualRate, int years) {
        return monthlyPayment(principal, annualRate, years) * months(years) - principal;
    }

    private double monthlyRate(double annualRate) { return annualRate / 1200.0; } // / 100 / 12
    private int months(int years)                   { return years * 12; }

    public static void main(String[] args) {
        LoanCalculator lc = new LoanCalculator();
        System.out.printf("Payment: %.2f%n", lc.monthlyPayment(200_000, 5.0, 30));
        System.out.printf("Interest: %.2f%n", lc.totalInterest(200_000, 5.0, 30));
    }
}
// Output (illustrative):
// Payment: 1073.64
// Interest: 186511.57
```

**Solution 4 — Overload Puzzle**

```java
public class OverloadPuzzle {
    static void play(int n)         { System.out.println("int"); }
    static void play(long n)        { System.out.println("long"); }
    static void play(Integer n)     { System.out.println("Integer"); }
    static void play(int... ns)     { System.out.println("varargs"); }

    public static void main(String[] args) {
        play(5);            // exact match            -> int
        play(5L);           // exact match            -> long
        play(5, 6);         // no 2-int overload; fits varargs -> varargs
        play((Integer) 5);  // exact match            -> Integer
    }
}
// Output: int
//         long
//         varargs
//         Integer
```

**Solution 5 — Polymorphic Override**

```java
public abstract class Shape {
    public abstract double area();                 // forces every subclass to override

    public String description() {                  // overridable, defaults provided
        return "A generic shape";
    }
}

public class Circle extends Shape {
    private final double r;
    public Circle(double r) { this.r = r; }

    @Override public double area() { return Math.PI * r * r; }

    @Override public String description() { return "A circle with radius " + r; }
}

public class Square extends Shape {
    private final double side;
    public Square(double side) { this.side = side; }

    @Override public double area() { return side * side; }

    @Override public String description() { return "A square with side " + side; }
}

public class ShapeDemo {
    public static void main(String[] args) {
        Shape[] shapes = { new Circle(2), new Square(3) };
        for (Shape s : shapes) {
            System.out.println(s.description() + " -> area " + s.area());
        }
    }
}
// Output:
// A circle with radius 2.0 -> area 12.566370614359172
// A square with side 3.0 -> area 9.0
```

**Solution 6 — The Constructor Trap**

```java
public class Base {
    public Base() {
        init();               // dispatches to Derived.init() BEFORE Derived fields exist
    }
    public void init() { System.out.println("Base init"); }
}

public class Derived extends Base {
    private final String tag = "ready";

    public Derived() { }      // implicit super() runs Base's constructor first

    @Override public void init() {
        System.out.println("Derived init, tag = " + tag); // tag is still null here!
    }

    public static void main(String[] args) {
        new Derived();
    }
}
// Output: Derived init, tag = null   <-- the trap

// FIX (chosen: make the hook non-polymorphic by making it private):
public class BaseFixed {
    public BaseFixed() {
        doInit();   // private -> NOT polymorphic -> always BaseFixed.doInit
    }
    private void doInit() { System.out.println("Base init"); }
}

public class DerivedFixed extends BaseFixed {
    private final String tag = "ready";
    private void doInit() { System.out.println("Derived init, tag = " + tag); }
    // This doInit() is an unrelated private method — it will NEVER be called from BaseFixed's constructor.
    public static void main(String[] args) {
        new DerivedFixed(); // Output: Base init
    }
}
```

**Solution 7 — Small Service API**

```java
public class PaymentService {
    public void process(Payment p) {
        validate(p);                 // private helper reused everywhere
        System.out.println("Processing " + p.amount() + " " + getCurrency());
    }

    public void process(Payment p, boolean notify) {   // overloaded version
        process(p);                                    // delegate to the base logic
        if (notify) System.out.println("Customer notified");
    }

    private void validate(Payment p) {                 // private: implementation detail
        if (p.amount() <= 0) throw new IllegalArgumentException("Amount must be positive");
    }

    public final String getCurrency() { return "USD"; } // final: currency is invariant

    public static void main(String[] args) {
        PaymentService svc = new PaymentService();
        svc.process(new Payment(99.99), true);
    }
}

class Payment {
    private final double amount;
    Payment(double amount) { this.amount = amount; }
    double amount() { return amount; }
}
// Output: Processing 99.99 USD
//         Customer notified

// Why subclassing process() works: process is PUBLIC and NON-FINAL, so a subclass may override it.
// Why getCurrency() is safe: FINAL means no subclass can silently switch currencies —
//   a deliberate design choice protecting financial invariants.
// Why validate() stays private: no one outside this class should bypass validation.
```

---

## 10. Key Takeaways and Summary

**Key concepts in one place:**

- **Static methods** belong to the class, run without an instance, and are resolved at *compile time* — perfect for utilities, factories, and the `main` entry point.
- **Final methods** cannot be overridden — they lock down invariants, template skeletons, and security-critical behavior.
- **Private methods** are visible only within their class — they power encapsulation, implementation hiding, and DRY refactoring.
- **Overloaded methods** share a name but differ in parameters; resolution follows *exact → widening → boxing → varargs* and happens at compile time.
- **Overridden methods** let subclasses replace inherited behavior; resolution happens at *runtime* via dynamic dispatch, enabling polymorphism.
- **`@Override`** turns silent override mistakes into compile-time errors — always use it.
- **Static methods hide, never override** — polymorphism requires instance methods.
- **Private methods are shadowed, never overridden** — a subclass's same-signature method is unrelated.
- **Never call overridable methods from constructors** — the subclass override runs on a half-built object.
- The `equals`/`hashCode`/`toString` contract depends entirely on correct overriding and consistent implementations.

**Glossary:**

| Term | Definition |
|---|---|
| Method | A named block of statements that can be invoked to perform an action or produce a result. |
| Method signature | The method's name plus its parameter list — its identity for overloading and overriding. |
| Static method | A method declared with `static` that belongs to the class and needs no instance. |
| Instance method | A method that belongs to an object and may access its instance fields. |
| Final method | A method declared with `final` that cannot be overridden by subclasses. |
| Private method | A method declared with `private`, callable only within the declaring class. |
| Overloading | Declaring multiple methods with the same name but different parameter lists in one class. |
| Overriding | A subclass redefining a method inherited from a superclass, with the same signature. |
| Dynamic dispatch | The runtime selection of an overridden method based on the object's actual type. |
| Covariant return type | An override returning a subtype of the parent method's return type. |
| Encapsulation | Hiding implementation details behind a stable public interface, often via private methods. |
| `@Override` | An annotation that makes the compiler verify a method truly overrides a supertype method. |

*Table 10.1 — Glossary of key terms.*

You have now met the five method types that structure every Java codebase: **static** methods for class-level logic, **final** methods for unbreakable contracts, **private** methods for hidden implementation, **overloaded** methods for readable APIs, and **overridden** methods for polymorphic behavior. Master the *when*, not just the *how*, and every class you design will communicate its intent through its method types alone.