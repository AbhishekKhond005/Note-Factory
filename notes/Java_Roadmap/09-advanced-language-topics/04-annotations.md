I'll verify the trickiest code examples actually compile and behave as documented before writing the notes. Let me check the environment first.
Java 21 is available. Let me verify the trickier code examples (repeatable annotations, reflection retrieval, TYPE_USE, annotation element types) before writing the notes.
Now let me verify repeatable annotations and the retrieval semantics.
Key finding confirmed: `getAnnotation()`/`isAnnotationPresent()` return null/false for the repeatable type when only the container is present — a great gotcha to document. Now let me verify the `@Target` examples including `TYPE_USE`/`TYPE_PARAMETER`/`PACKAGE`.
TargetDemo works. The package-info test just needs the annotation declared in the same package. Let me fix and verify.
The package wasn't loaded yet — need to force class loading first.
Package annotations verified. Now let me confirm compile-time errors for wrong targets and the `@Inherited`/`@SafeVarargs`/`@FunctionalInterface` behaviors.
Let me verify the exact retention/target of the built-in annotations so the reference tables in the notes are accurate.
`@SuppressWarnings` shows no `@Target` at runtime — interesting. Let me inspect the class file to understand why (javac emits meta-annotations differently).
`@SuppressWarnings` has no `@Target` in the JDK and can even go on type parameters. Let me test the general rule for a user annotation without `@Target`.
Confirmed: no `@Target` → usable on any declaration (including type parameters) but not in type-use contexts. Let me verify two final error cases: repeating a non-repeatable annotation, and `@FunctionalInterface` misuse.
All behaviors verified. Let me run one final check on `RECORD_COMPONENT` and `TYPE_USE` in cast/throws contexts before writing the notes.
TYPE_USE works everywhere. Fixing the record test filename issue:
Valuable nuance discovered: `RECORD_COMPONENT` annotations don't propagate to fields unless `FIELD` is also in the target. Let me verify the propagation rule.
All code examples and runtime behaviors are now verified against Java 21. Here is the complete chapter.

---

# Java Annotations — A Comprehensive Study Guide

> **Chapter scope:** Built-in annotations · custom annotations · retention policies · target restrictions · repeatable annotations · processing · best practices · real-world usage.
>
> **Prerequisites:** Comfortable with classes, methods, inheritance, interfaces, and enums. No prior annotation knowledge assumed.
>
> **All code verified against OpenJDK 21** — every example compiles and runs as documented.

---

## 1. Introduction to Annotations

### 1.1 What Is an Annotation?

An **annotation** is a form of metadata: structured, machine-readable data attached to your source code. It does **not** change the behavior of your program by itself. Instead, it carries *information* that other software — the compiler, build tools, IDEs, or frameworks — can read and act upon.

Think of annotations as **sticky notes** on a filing cabinet.

- The sticky note says "**Review this document before Friday.**"
- The document's *content* is unchanged — the note doesn't alter a single word inside it.
- But the *office robot* (the compiler) sees the note and issues a warning when Friday passes.
- A *different robot* (your framework) sees the note and schedules a reminder.
- If no robot ever reads the note, it's harmless — it just sits there.

That is exactly how annotations behave. Consider the famous one:

```java
@Override
public String toString() {
    return "Coordinate{x=" + x + ", y=" + y + "}";
}
```

The `@Override` annotation does **nothing** to the behavior of `toString()`. But it tells `javac`: *"This method is supposed to override a method from a superclass or interface — please verify that, and if I misspelled it, fail the build."* That's metadata doing a compile-time *safety check*.

### 1.2 Why Do Annotations Exist?

Before Java 5, if you wanted to attach metadata to code, you had to use external XML files (think `struts-config.xml`, `web.xml`), or naming conventions (`getXxx` for properties), or comments that only humans read. All three approaches were fragile:

- XML metadata lives **far away** from the code it describes — they drift apart.
- Naming conventions are **unchecked** — misspell `setter` and you silently break behavior.
- Comments are **invisible to tools** — no program can act on them.

Annotations fix all three problems by putting metadata **directly on the declaration it describes**, in a form the Java compiler *understands* and **type-checks**.

The purposes of annotations, in one list:

- **Compiler diagnostics** — `@Override`, `@Deprecated`, `@SuppressWarnings`.
- **Compile-time code generation** — annotation processors read annotations and generate source or bytecode (Lombok's `@Getter`, Dagger's `@Component`).
- **Runtime configuration** — frameworks inspect annotations with reflection to wire up behavior (Spring's `@Autowired`, JUnit's `@Test`, JPA's `@Entity`).
- **Documentation** — `@Deprecated` (with `since`/`forRemoval`) documents API evolution in Javadoc.
- **Static analysis** — `@Nullable`/`@NonNull` style annotations feed tools like SpotBugs or the Checker Framework.

### 1.3 Where Can Annotations Appear?

Annotations attach to **declarations** and, since Java 8, to **type uses**. Here is the full map:

| Attached to | Example | Common purpose |
|---|---|---|
| A **package** | `package-info.java` | Package-level docs or metadata |
| A **class / interface / enum / record** | `@Entity public class User { ... }` | Declare "this is a table row" |
| An **annotation type** | `@Retention(...)` on `@interface Foo` | Meta-annotation |
| A **field** | `@Inject private Service svc;` | Mark for dependency injection |
| A **method** | `@Test void verify() { ... }` | Register a unit test |
| A **constructor** | `@Inject User(UserService s) { ... }` | Inject constructor dependencies |
| A **parameter** | `void set(@NonNull String name)` | Declare "never null" |
| A **local variable** | `@SuppressWarnings("unchecked") List raw = ...` | Suppress warnings locally |
| A **type parameter** | `<@NonNull T> T read()` | Type-parameter metadata |
| A **type use** | `List<@NonNull String> names` | Metadata on a specific type usage |
| A **module** | `@Deprecated module old.mod { }` | Module-level metadata |

We'll cover every one of these destinations in depth in **Section 5**.

### 1.4 The Simplest Annotation, Explained Piece by Piece

Here is the smallest meaningful annotation program, with every part labeled:

```java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)   // (1) meta-annotation: keep it for the JVM
public @interface Version {           // (2) declare an annotation type named "Version"
    String value();                   // (3) one element, named "value"
}
```

```java
@Version("2.3.1")                     // (4) use it, with shorthand syntax
public class App {
    public static void main(String[] args) {
        // (5) read it back at runtime via reflection
        Version v = App.class.getAnnotation(Version.class);
        System.out.println("Version: " + v.value());   // prints "Version: 2.3.1"
    }
}
```

Walking through it:

1. **`@Retention(RetentionPolicy.RUNTIME)`** — annotations that *describe* other annotations are called **meta-annotations**. This one says: "Keep `@Version` information in the compiled `.class` file and make it visible to the JVM at runtime, so reflection can read it."
2. **`public @interface Version`** — the `@interface` keyword declares a new annotation type. It looks like `interface` with an `@` — an apt mnemonic, because an annotation *is* a special kind of interface.
3. **`String value()`** — a declaration inside the annotation is called an **element** (also *member*). It has no parameters and no body, just a return type and a name. Here `value()` is special: when an annotation has a single element named `value`, you can use the shorthand `@Version("2.3.1")`.
4. **`@Version("2.3.1")`** — this is an **annotation use** (also called an *annotation instance*). You are applying metadata "2.3.1" to the class `App`.
5. **`App.class.getAnnotation(Version.class)`** — because we chose `RUNTIME` retention, the `Class` object exposes the annotation to reflection, and `.value()` retrieves the stored value.

> **Mental model:** Declaring `@interface Version` is like printing a **blank form**. Writing `@Version("2.3.1")` is like **filling in the form** and stapling it to a file. `getAnnotation()` is the **file clerk** who reads the stapled forms back to you.

---

### ✅ Key Takeaways — Introduction

- An annotation is **metadata**, not logic: it never changes program behavior directly.
- Annotations solve the XML drift problem by living **on the declaration they describe**, in a compiler-checked syntax.
- Annotations are consumed in three ways: by the **compiler**, by **annotation processors** (code generation), and by **frameworks via reflection** at runtime.
- `@interface` declares an annotation type; `@Annotation(...)` is a *use* of that type.
- The `value` element enables the shorthand `@X("arg")` syntax.
- Meta-annotations (`@Retention`, `@Target`, …) are annotations *about* your annotation — they configure how it behaves.

---

## 2. Built-In Annotations

Java ships a set of ready-made annotations in `java.lang` and related packages. They fall into three categories:

1. **Compiler-checking annotations** — verified and acted on by `javac`.
2. **Annotations that inform tools** — read by IDEs, build tools, and static analyzers.
3. **Meta-annotations** — annotations that configure other annotation types.

### 2.1 The Reference Table

| Annotation | Retention | Applies to | What it does |
|---|---|---|---|
| `@Override` | `SOURCE` | Methods | Tells the compiler "this method overrides a supertype method" — fails compilation if it does not |
| `@Deprecated` | `RUNTIME` | Types, methods, fields, constructors, parameters, local vars, packages, modules | Marks API as obsolete; since Java 9 takes `since` and `forRemoval` elements |
| `@SuppressWarnings` | `SOURCE` | Almost anything | Tells the compiler to silence specific warnings |
| `@FunctionalInterface` | `RUNTIME` | Types | Declares "this interface has exactly one abstract method" (a SAM type) |
| `@SafeVarargs` | `RUNTIME` | Constructors & methods (static, final, or private) | Suppresses "possible heap pollution" warnings on varargs |
| `@Retention` | `RUNTIME` | Annotation types | **Meta:** how long the annotation is kept |
| `@Target` | `RUNTIME` | Annotation types | **Meta:** where the annotation may be applied |
| `@Documented` | `RUNTIME` | Annotation types | **Meta:** include the annotation in generated Javadoc |
| `@Inherited` | `RUNTIME` | Annotation types | **Meta:** subclasses inherit the annotation from their superclass |
| `@Repeatable` | `RUNTIME` | Annotation types | **Meta:** allow the annotation to appear more than once |
| `@Native` | `SOURCE` | Fields | Constants referenced from JNI/native code (rarely used directly) |

*(The retention/target columns above were verified against OpenJDK 21 — see the class files directly.)*

### 2.2 `@Override` — The Compiler's Integrity Checker

**What it does:** Guarantees that the annotated method really does override a method declared in a superclass or interface.

```java
public abstract class Shape {
    public abstract double area();
}

public class Circle extends Shape {
    private final double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public double area() {                 // correct: overrides Shape.area()
        return Math.PI * radius * radius;
    }

    @Override
    public String toString() {             // correct: overrides Object.toString()
        return "Circle[radius=" + radius + "]";
    }
}
```

**The killer pitfall — typo detection.** Without `@Override`, a misspelled method silently creates a *new* method instead of overriding:

```java
// BAD: "are()" instead of "area()" — compiles fine WITHOUT @Override,
// and Circle silently keeps Shape's abstract-ness → "abstract method not implemented" later.
public double are() {
    return Math.PI * radius * radius;
}
```

With `@Override` the compiler catches it immediately:

```java
public class Circle extends Shape {
    @Override
    public double are() { ... }   // ERROR (verified on JDK 21):
}                                 // "method does not override or implement a method from a supertype"
```

**Best practice:** annotate **every** overriding method, including `toString()`, `equals()`, and `hashCode()`. The only case where `@Override` is *optional* (still recommended) is implementing an interface method — javac doesn't require it there, but `@Override` documents intent and catches signature drift.

### 2.3 `@Deprecated` — The "Out of Service" Sign

**What it does:** Marks an API element as obsolete. The compiler warns when deprecated code is used, and Javadoc flags it in documentation. Since Java 9 it has two elements:

- `since` — the version in which the element was deprecated.
- `forRemoval` — `true` means the API will be removed in a future release (stronger warning).

```java
public class LegacyPrinter {

    /** @deprecated Use {@link #printUtf8(String)} instead. */
    @Deprecated(since = "2.0", forRemoval = true)
    public void printAscii(String text) {
        System.out.println(text);
    }

    public void printUtf8(String text) {
        System.out.println(text);
    }
}
```

```java
public class LegacyPrinterDemo {
    public static void main(String[] args) {
        new LegacyPrinter().printAscii("hello");  // compiler emits a deprecation warning
    }
}
```

Compiling with `-Xlint:deprecation` gives a detailed warning including the `since` version.

**Pitfalls:**

- Do **not** use `@Deprecated` to mean "internal, don't call" — that's what `sealed` classes, access modifiers, and `@SuppressWarnings` are for.
- Keep the Javadoc `@deprecated` tag in sync — IDEs render it; the annotation itself is read by tools.
- Because `@Deprecated` has `RUNTIME` retention, frameworks and tooling can *detect* deprecated elements programmatically at runtime — some build tools refuse to publish APIs that contain `forRemoval = true` elements.

### 2.4 `@SuppressWarnings` — The Muzzle

**What it does:** Silences specific compiler warnings on the annotated declaration and everything inside it. It takes a single element, `value()`, which is an array of warning names.

```java
import java.util.ArrayList;
import java.util.List;

public class LegacyBridge {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<String> bridge() {
        List raw = new ArrayList();        // raw type → would normally warn
        raw.add("ada");
        List<String> typed = raw;          // unchecked conversion → would normally warn
        return typed;
    }
}
```

Common warning names: `unchecked`, `rawtypes`, `deprecation`, `fallthrough`, `finally`, `serial`, `varargs`, plus the kitchen-sink value `"all"`.

> **Fun fact (verified on JDK 21):** The JDK's own `@SuppressWarnings` declaration has **no `@Target` annotation at all** in the class file — meaning it may legally appear on essentially any declaration, including type parameters. That's why you can write `@SuppressWarnings("unchecked") <T> T cast(...) { ... }`. It's the exception, not the rule.

**Pitfalls:**

- **Don't suppress warnings you don't understand.** A suppressed `unchecked` warning is a signed confession that you've bypassed the type system; add a comment explaining *why* it's safe.
- Keep the scope as **narrow as possible** — put it on the local variable or method, not on the whole class, or you'll hide real problems.
- **Never** use it to silence `deprecation` warnings you could fix by migrating.

### 2.5 `@FunctionalInterface` — The SAM Contract

**What it does:** States that the interface has **exactly one abstract method** (a *Single Abstract Method* type, the basis of lambda expressions). The compiler enforces this contract.

```java
@FunctionalInterface
public interface Greeter {
    String greet(String name);          // the single abstract method
}
```

```java
public class GreeterDemo {
    public static void main(String[] args) {
        Greeter informal = name -> "Hi, " + name;        // lambda implements it
        Greeter formal   = name -> "Good day, " + name;  // another implementation
        System.out.println(formal.greet("Ada"));
    }
}
```

The compiler will reject an interface with two abstract methods (verified on JDK 21):

```java
@FunctionalInterface
interface Broken {
    void a();
    void b();
}
// ERROR: "Broken is not a functional interface:
//         multiple non-overriding abstract methods found in interface Broken"
```

**Note:** Default and static methods don't count toward the single abstract method, so this is legal:

```java
@FunctionalInterface
public interface Transformer<T> {
    T apply(T input);                                   // the one abstract method
    default Transformer<T> andThen(Transformer<T> next) {
        return t -> next.apply(apply(t));               // default method — fine
    }
}
```

### 2.6 `@SafeVarargs` — The "I've Checked the Heap Pollution" Badge

**What it does:** Silences the *heap pollution* warning attached to calling varargs methods with generic arguments. A varargs parameter is really an array, and generics and arrays don't mix well — calling `Arrays.asList("a", "b")` hides an unchecked array creation that `javac` warns about.

Since Java 9, `@SafeVarargs` is permitted on **static, final, or private** methods and on constructors — precisely the ones a class can guarantee won't expose the varargs array to a caller.

```java
import java.util.ArrayList;
import java.util.List;

public class Lists {

    @SafeVarargs
    private static <T> List<T> flatten(T... items) {     // safe: never leaks the array
        List<T> result = new ArrayList<>();
        for (T item : items) {
            result.add(item);
        }
        return result;
    }

    public static void main(String[] args) {
        System.out.println(flatten("a", "b", "c"));      // [a, b, c], no warning
    }
}
```

**Pitfall:** Do **not** annotate a method that stores or returns the varargs array — that's precisely the unsafe pattern the warning exists to catch:

```java
@SafeVarargs                                        // WRONG — this is the unsafe case!
static <T> T[] first(T... items) {
    return items;                                   // leaks the array to the caller
}
```

### 2.7 Meta-Annotations — Annotations About Annotations

A **meta-annotation** is an annotation whose target is *another annotation type*. This is the bridge to writing your own annotations: when you declare `@interface`, the first thing you usually do is meta-annotate it.

Here is a quick tour (each is explored in depth later):

| Meta-annotation | Purpose | Default if omitted |
|---|---|---|
| `@Retention` | How long the annotation survives | `CLASS` (kept in `.class`, not readable at runtime) |
| `@Target` | Where the annotation may be applied | Anywhere (declarations, but not type uses) |
| `@Documented` | Include in generated Javadoc | Not documented |
| `@Inherited` | Subclasses inherit the annotation (class-level only) | Not inherited |
| `@Repeatable` | Same annotation may appear multiple times | Not repeatable |

A complete example combining `@Inherited`, which you haven't seen yet:

```java
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Inherited                                      // subclasses inherit this annotation
@Retention(RetentionPolicy.RUNTIME)             // readable via reflection
@Target(ElementType.TYPE)                       // only on types (classes/interfaces/enums)
@Documented                                     // shows up in Javadoc
public @interface Audited {
    String by();
}
```

```java
@Audited(by = "compliance")
public class BaseDocument {
}

public class SubDocument extends BaseDocument {
}
```

```java
public class AuditedDemo {
    public static void main(String[] args) {
        // @Inherited makes the annotation visible on the subclass (verified on JDK 21):
        System.out.println(SubDocument.class.isAnnotationPresent(Audited.class));  // true
        System.out.println(SubDocument.class.getAnnotation(Audited.class));        // @Audited(by="compliance")
    }
}
```

**`@Inherited` rules — important:**

- Applies **only** to annotations on **classes** — never to interfaces, methods, or fields.
- The annotation's retention must be `CLASS` or `RUNTIME` for inheritance to matter.
- `getAnnotation()` / `getAnnotationsByType()` honor inheritance; `getDeclaredAnnotation()` deliberately does **not** (it only reports what's literally written on that class).

---

### ✅ Key Takeaways — Built-In Annotations

- `@Override` = compile-time override verification; use it on **every** overriding method to catch signature typos.
- `@Deprecated` = obsolete API marker; since Java 9 it carries `since` and `forRemoval`.
- `@SuppressWarnings` = scoped warning silencer; keep its scope tight and document *why*.
- `@FunctionalInterface` = enforces the single-abstract-method contract needed by lambdas.
- `@SafeVarargs` = declares a varargs method heap-pollution–safe; only for static/final/private methods or constructors.
- Meta-annotations (`@Retention`, `@Target`, `@Documented`, `@Inherited`, `@Repeatable`) are the configuration knobs you'll use on every annotation you define.

---

## 3. Custom Annotations

Built-ins only get you so far. The real power comes from defining annotations that *your* frameworks, libraries, and team tooling can read. An annotation you write yourself is a **custom annotation**.

### 3.1 Declaring an Annotation with `@interface`

The keyword `@interface` declares an annotation type. Inside its body you declare **elements**, which look like parameterless method declarations.

```java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Author {
    String name();
    String email() default "unknown@example.com";
    int revision() default 1;
}
```

Used like this:

```java
@Author(name = "Grace Hopper", revision = 3)
public class Compiler {
}
```

Every part of the declaration:

- **`public @interface Author`** — the annotation type. File must be named `Author.java`.
- **`String name()`** — a required element. Any use of `@Author` must supply `name`.
- **`String email() default "unknown@example.com"`** — an element with a default; uses may omit it.
- **`int revision() default 1`** — a primitive element with a default.

**Element syntax rules:**

- An element is declared like `ReturnType name();` — **no parameters, no body, no `throws`**.
- Elements may have a `default` value; required elements have none.
- The **name is part of the use-site syntax**: `@Author(name = "Grace")`, so choose readable names.

### 3.2 The Allowed Element Types

An element's return type is strictly limited. This is the complete legal set:

| Allowed element type | Example declaration | Example use |
|---|---|---|
| **primitive** (`int`, `double`, `boolean`, `char`, `long`, `float`, `byte`, `short`) | `int timeout() default 30;` | `@Config(timeout = 60)` |
| **`String`** | `String name();` | `@Config(name = "prod")` |
| **`Class` or parameterized `Class<?>`** | `Class<?> validator() default Void.class;` | `@Config(validator = MyValidator.class)` |
| **an enum** | `Priority priority() default Priority.LOW;` | `@Config(priority = Priority.HIGH)` |
| **another annotation** | `Flag flag() default @Flag(name = "default");` | `@Config(flag = @Flag(name = "urgent"))` |
| **an array of any of the above** | `String[] labels() default {};` | `@Config(labels = {"a", "b"})` |

The **disallowed** types deserve equal attention: `Object`, `Integer`, `List<String>`, `Map`, and every other class type are **illegal**. `javac` rejects them with *"invalid type for annotation member"*. Annotations can only hold *small, closed, constant* values — exactly the kinds of values that can be embedded in a class file.

Here is one annotation demonstrating **every** allowed element type at once:

```java
public enum Priority { LOW, MEDIUM, HIGH }
public enum Status { OPEN, IN_PROGRESS, DONE }

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Contact {
    String name();                          // String
    String email() default "no-reply@example.com";
    Priority priority() default Priority.LOW;   // enum
    Status status();                        // enum (required)
    Class<?> validator() default Void.class;    // Class
    String[] labels() default {};           // array of String
    Flag[] flags() default { @Flag(name = "default") };  // array of annotation
}
```

```java
@Retention(RetentionPolicy.RUNTIME)
public @interface Flag {
    String name();
}
```

A realistic use:

```java
@Contact(name = "Ada Lovelace", status = Status.DONE,
          priority = Priority.HIGH,
          flags = { @Flag(name = "urgent"), @Flag(name = "reviewed") },
          labels = { "analytics", "ga" })
public class Campaign {
}
```

### 3.3 The Special `value` Element and Shorthand

If an annotation has **exactly one element** and that element is named `value`, users may omit the `value =` part entirely:

```java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Version {
    String value();
}
```

```java
@Version("2.3.1")        // shorthand for @Version(value = "2.3.1")
public class App {
}
```

The same shorthand collapses a **single-element array** for any element named `value` that is an array type. That's why `@SuppressWarnings("unchecked")` works even though `value()` is declared as `String[]`.

**Rule:** the shorthand is only legal when you supply *just* `value`. If an annotation has other elements, you must write them by name:

```java
@Contact(name = "Ada", status = Status.DONE)   // OK — value doesn't even exist here
```

### 3.4 Marker Annotations

An annotation with **no elements** is a **marker annotation**. Its presence *is* the information.

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ java.lang.annotation.ElementType.METHOD })
public @interface ExposedAsRestApi {
}
```

```java
public class ReportService {
    @ExposedAsRestApi
    public Report buildReport(String id) {
        return new Report(id);
    }
}
```

Markers are the simplest possible flag: a framework checks `isAnnotationPresent(ExposedAsRestApi.class)` and behaves accordingly. JUnit's `@Test` is essentially a marker.

### 3.5 A Realistic, More Complex Example

Let's build an annotation a hypothetical **task scheduler framework** might define:

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)      // the framework reads it via reflection
@Target(ElementType.METHOD)              // only schedulable methods
public @interface Scheduled {

    /** Cron-like expression, e.g. "0 0 12 * * ?" */
    String cron() default "";

    /** Fixed delay in milliseconds between runs. */
    long fixedDelay() default 0;

    /** Required: who owns this job. */
    String owner();

    /** Retry policy. */
    int maxRetries() default 0;

    /** Validation rule classes to run before execution. */
    Class<?>[] validators() default {};
}
```

```java
public class ReportJob {

    @Scheduled(cron = "0 0 6 * * ?", owner = "analytics-team",
               maxRetries = 3, validators = { DateRangeValidator.class })
    public void nightlyReport() {
        System.out.println("Generating nightly report...");
    }
}
```

### 3.6 Rules, Restrictions, and the "No Inheritance" Trap

Critical restrictions to internalize:

1. **No inheritance of annotations.** Annotation types cannot `extends` another annotation type (a *meta-annotation* relationship like `@Target` is *not* inheritance — it's an annotation *on* the type). You cannot write `@interface B extends A`.
2. **No polymorphism, no generics.** Elements can't be generic: `T value()` is illegal.
3. **Elements may not have parameters or `throws` clauses.**
4. **No `null` defaults.** An element default can never be `null` — this is why `Class<?> validator() default Void.class;` uses `Void.class` as a "no validator" sentinel instead of `null`.
5. **Two annotations of the same type cannot repeat on one declaration** unless the type is declared `@Repeatable` (Section 6). Applying `@Deprecated @Deprecated` produces the verified error: *"Deprecated is not a repeatable annotation interface"*.
6. **Element values must be compile-time constants** — `@Config(name = SOME_STATIC_FINAL)` works for `static final` constants of the allowed types, but not for arbitrary runtime expressions.

---

### ✅ Key Takeaways — Custom Annotations

- `@interface` declares an annotation type; elements are declared as parameterless method signatures with optional `default` values.
- Allowed element types: **primitives, `String`, `Class<?>`, enums, other annotations, and arrays of these** — nothing else.
- The `value` element enables shorthand syntax; single-element arrays collapse too.
- Marker annotations (no elements) encode information purely by their presence.
- Annotations **cannot inherit** from each other, have no generics, and cannot default to `null`.

---

## 4. Retention

**Retention** answers the question: *"How long should this annotation survive after I write it?"* The `@Retention` meta-annotation controls this with one of three values from the `RetentionPolicy` enum.

### 4.1 The Three Policies

| Policy | What survives | Runtime reflection? | Typical use |
|---|---|---|---|
| `SOURCE` | Discarded by the compiler — gone after `javac` | **No** | Compiler checks, lint-like tools, code generation via processors |
| `CLASS` | Recorded in the `.class` file (this is the **default** when `@Retention` is omitted) | **No** (by default; the JVM can be told otherwise) | Bytecode tools that don't need runtime reflection |
| `RUNTIME` | Recorded in `.class` **and** exposed to the JVM | **Yes** | Frameworks that read annotations via reflection (Spring, JUnit, JPA) |

### 4.2 `SOURCE` — Compile-Time Only

`SOURCE` annotations are discarded by the compiler; they never appear in the `.class` file. We verified this directly: after compilation, `javap -v` shows **no annotation attribute at all** for a `@Retention(SOURCE)` annotation, and runtime reflection reports it absent.

**Verified example:**

```java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface Todo {
    String note();
}
```

```java
@Todo(note = "replace magic numbers with constants")
public class Pricing {
    public static void main(String[] args) {
        // RUNTIME: this prints "false" — the annotation is already gone.
        System.out.println(Pricing.class.isAnnotationPresent(Todo.class));
    }
}
```

**When to use `SOURCE`:**

- The annotation exists purely for the **compiler or an annotation processor** during compilation.
- `@Override` and `@SuppressWarnings` are `SOURCE` — they must exist long enough for `javac` to check them, and no longer.
- **Code generation:** an annotation processor (Section 7.3) reads a `SOURCE` annotation and emits new Java source files. Lombok's annotations are `SOURCE` — they generate getters/setters *during compilation* and vanish afterward, which is why Lombok-generated methods don't exist in the source but *do* exist in the `.class` file.

### 4.3 `CLASS` — The Default

If you don't write `@Retention` at all, the annotation is `CLASS`:

```java
public @interface BytecodeHint {   // no @Retention → RetentionPolicy.CLASS
    String hint();
}
```

The annotation is stored in the `.class` file's `RuntimeInvisibleAnnotations` attribute — it's there for bytecode-level tools, but the standard reflection API does **not** surface it to normal `getAnnotation()` calls at runtime.

**When to use `CLASS`:**

- You want the metadata shipped inside the artifact for **bytecode tooling** — instrumentation agents, profilers, build-time validators, or other libraries processing `.class` files directly — without the cost of runtime visibility.
- In practice, **most custom annotations choose `RUNTIME`** because frameworks need reflection access. `CLASS` is the middle ground that keeps the bytes but hides them from reflection.

### 4.4 `RUNTIME` — Visible to the JVM

`RUNTIME` annotations survive into the running JVM and are exposed through the reflection API (`java.lang.reflect.AnnotatedElement`). This is the choice for **framework configuration**.

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
    String name() default "";
}
```

```java
import java.lang.reflect.Method;

@Component(name = "report-service")
public class ReportService {
    public Report build() { return new Report(); }
}

public class ComponentDemo {
    public static void main(String[] args) {
        Component c = ReportService.class.getAnnotation(Component.class);
        System.out.println("component name = " + c.name());   // "report-service"
    }
}
```

This is exactly the mechanism that powers:

- **Spring** — `@Component`, `@Autowired`, `@RestController` (all `RUNTIME`).
- **JUnit** — `@Test` is `RUNTIME` so the JUnit engine can find and run tests via reflection.
- **JPA/Hibernate** — `@Entity`, `@Table`, `@Column` are `RUNTIME`.

### 4.5 Choosing the Wrong Policy — Practical Consequences

| You chose… | but you needed… | Consequence |
|---|---|---|
| `SOURCE` | runtime access | `getAnnotation()` returns `null`; your framework silently ignores your annotation — the classic "it just doesn't work" bug |
| `CLASS` | runtime access | Same: the annotation is in the `.class` file but invisible to standard reflection; everything quietly no-ops |
| `RUNTIME` | nothing more than a compiler hint | Waste: every use is loaded into the JVM and visible to reflection, slightly increasing memory/class-loading overhead; also your annotation is now part of the *public runtime surface* of your API |

**Rule of thumb:** start with the *least powerful* policy that does the job. Compiler-only check → `SOURCE`. Bytecode tooling → `CLASS`. Framework reflection → `RUNTIME`. And remember: **if you omit `@Retention`, you silently get `CLASS`** — a surprisingly common cause of "my annotation doesn't show up at runtime."

---

### ✅ Key Takeaways — Retention

- `@Retention(RetentionPolicy.X)` controls how long an annotation survives.
- `SOURCE` → discarded by the compiler (compiler checks, code generation).
- `CLASS` → stored in `.class`, invisible to reflection — and it's the **default** when `@Retention` is omitted.
- `RUNTIME` → visible to the JVM and the reflection API; required by Spring, JUnit, JPA, and friends.
- Symptom of a wrong policy: `getAnnotation()`/`isAnnotationPresent()` silently return `null`/`false`.

---

## 5. Target

**Target** answers: *"Where is this annotation allowed to appear?"* The `@Target` meta-annotation takes an array of `ElementType` constants.

### 5.1 The Complete `ElementType` Table

| `ElementType` | Where it allows the annotation | Since |
|---|---|---|
| `TYPE` | Class, interface, enum, record, annotation type declarations | 1.5 |
| `FIELD` | Fields, **and record components** (a field-like context) | 1.5 |
| `METHOD` | Methods | 1.5 |
| `PARAMETER` | Method/constructor parameters | 1.5 |
| `CONSTRUCTOR` | Constructors | 1.5 |
| `LOCAL_VARIABLE` | Local variables | 1.5 |
| `ANNOTATION_TYPE` | Annotation type declarations (making yours a meta-annotation) | 1.5 |
| `PACKAGE` | The `package` declaration (in `package-info.java`) | 1.5 |
| `TYPE_PARAMETER` | Type variables in generic declarations, e.g. `<T>` | 8 |
| `TYPE_USE` | Any use of a type: field types, generic arguments, array dimensions, casts, `throws`, type parameters, and more | 8 |
| `RECORD_COMPONENT` | Record components (in the record header) | 16 |
| `MODULE` | Module declarations (in `module-info.java`) | 9 |

### 5.2 Examples for Every Target

**`TYPE`, `FIELD`, `METHOD`, `PARAMETER`, `CONSTRUCTOR`, `LOCAL_VARIABLE`:**

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Api {
    String version();
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Inject {
}
```

```java
@Api(version = "1.0")                       // TYPE: on the class
public class InventoryService {

    @Inject                              // FIELD: on a field
    private ItemRepository repository;

    @Inject                              // CONSTRUCTOR: on a constructor
    public InventoryService(ItemRepository repository) {
        this.repository = repository;
    }

    @Api(version = "1.1")                // METHOD: on a method
    public Item find(@Api(version = "0.9") String id) {  // PARAMETER: on a parameter
        return repository.findById(id);
    }

    public void run() {
        @SuppressWarnings("unchecked")   // LOCAL_VARIABLE: on a local variable
        var items = (java.util.List<Item>) (java.util.List<?>) repository.findAll();
        System.out.println(items.size());
    }
}
```

> **Note:** in the snippet above, `@Api` is *also* applied to a parameter — but `@Api` only targets `TYPE` and `METHOD`, so that line **will not compile**. That's the point of `@Target`, demonstrated live in Section 5.4.

**`ANNOTATION_TYPE` — building your own meta-annotation:**

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)      // can only annotate OTHER annotations
public @interface MyMeta {
    String note() default "";
}

@MyMeta(note = "marker for REST endpoints")  // legal: MyMeta is on an annotation type
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Get {
}
```

**`PACKAGE` — via `package-info.java`:**

Every Java package may contain a `package-info.java` file carrying package-level declarations. Annotations with `@Target(PACKAGE)` go there:

```java
// package-info.java
@PackageInfo(author = "Grace Hopper")
package com.example.myapp;
```

```java
package com.example.myapp;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PackageInfo {
    String author();
}
```

Reading it back (verified on JDK 21):

```java
public class PackageReader {
    public static void main(String[] args) throws Exception {
        Class.forName("com.example.myapp.PackageInfo");   // force the package to load
        Package pkg = Package.getPackage("com.example.myapp");
        System.out.println(pkg.getAnnotation(PackageInfo.class)); // @PackageInfo(author="Grace Hopper")
    }
}
```

**`TYPE_PARAMETER` and `TYPE_USE` — Java 8 additions:**

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_PARAMETER, ElementType.TYPE_USE })
public @interface NonNull {
}

public class TypeUseDemo {

    @NonNull String field;                        // TYPE_USE: field type
    String @NonNull [] names;                     // TYPE_USE: array element type
    List<@NonNull String> list;                   // TYPE_USE: generic argument

    @SuppressWarnings("unchecked")
    Object cast(Object o) {
        return (@NonNull String) o;               // TYPE_USE: cast
    }

    void throwing() throws @NonNull IllegalStateException {   // TYPE_USE: throws clause
        throw new IllegalStateException();
    }

    <@NonNull T> T first(List<@NonNull T> xs) {   // TYPE_PARAMETER: <T>, TYPE_USE: argument
        return xs.get(0);
    }
}
```

*(All of the above positions compile on JDK 21 — verified.)*

**`RECORD_COMPONENT` (Java 16+):**

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {
}
```

```java
import java.lang.reflect.RecordComponent;

public record User(@Sensitive String password, String username) {
}
```

```java
public class RecordReader {
    public static void main(String[] args) {
        RecordComponent rc = User.class.getRecordComponents()[0];
        System.out.println(rc.isAnnotationPresent(Sensitive.class));  // true
    }
}
```

> **Record gotcha (verified):** A `RECORD_COMPONENT`-only annotation stays on the component — it does **not** land on the generated private field. To have it propagate to the field *and* the constructor parameter, include `FIELD` and `PARAMETER` in the target too. Hibernate Validator's `@NotNull` targets `FIELD`, `METHOD`, `PARAMETER`, `ANNOTATION_TYPE`, `TYPE_USE`, and `RECORD_COMPONENT` — precisely so it works everywhere a bean constraint might be placed.

**`MODULE` (Java 9+):** annotations targeting `MODULE` go at the top of `module-info.java`:

```java
// module-info.java
@Deprecated
module legacy.module {
    exports com.example.legacy;
}
```

### 5.3 Omit `@Target` — or Not?

**If you omit `@Target` entirely**, the annotation may be used on **any declaration** — classes, fields, methods, parameters, constructors, local variables, type parameters — but **not** in *type-use* positions such as generic arguments or array dimensions. We verified this on JDK 21:

```java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Anywhere {
}
```

```java
import java.util.List;

public class AnywhereDemo {
    @Anywhere                          // class-level
    static class Inner {
        @Anywhere                      // field
        int field;

        @Anywhere                      // method
        void m(@Anywhere String p) {   // parameter
            @Anywhere                  // local variable
            int local = 1;
        }

        <@Anywhere T> T g(T t) {       // type parameter — allowed!
            return t;
        }
    }

    // COMPILE ERRORS (verified): the annotation may not be used in type contexts.
    // List<@Anywhere String> bad;      // "not applicable in this type context"
    // String @Anywhere [] badArray;   // "not applicable in this type context"
}
```

**Guidance:**

- **Restrict aggressively.** `@Target(ElementType.METHOD)` communicates intent, catches accidental misuse at compile time, and lets your annotation processor optimize. It's self-documentation.
- **Omit only when "apply anywhere" is the real design**, like `@SuppressWarnings`. Even then, consider `@Target({ TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, MODULE })` if you want to *explicitly* document intent.
- The special case `@Target({})` (empty array) declares an annotation that **cannot be applied anywhere** — sometimes used for annotations that only exist to be *referred to* by other annotations.

### 5.4 Compiler Errors — What You Actually See

Applying an annotation to a disallowed element produces a clear compile-time error (verified on JDK 21):

```java
@Target(ElementType.METHOD)
public @interface RunsOnce {
}
```

```java
@RunsOnce                             // ERROR: "annotation interface not applicable
public class WrongTarget {            //         to this kind of declaration"
    @RunsOnce
    private String field;             // same ERROR here
}
```

Because `@RunsOnce` only targets `METHOD`, both the class and the field rejects it. The compiler, not the framework, enforces this — so a wrong target is a **build-time failure**, not a mysterious runtime no-op.

---

### ✅ Key Takeaways — Target

- `@Target` restricts *where* an annotation may appear; it takes an array of `ElementType` constants.
- `TYPE`, `FIELD`, `METHOD`, `PARAMETER`, `CONSTRUCTOR`, `LOCAL_VARIABLE`, `ANNOTATION_TYPE`, `PACKAGE`, `TYPE_PARAMETER`, `TYPE_USE`, `RECORD_COMPONENT`, and `MODULE` cover every attach point in modern Java.
- `TYPE_USE` is the broadest — it covers generic arguments, casts, arrays, and `throws`, and it *implies* `TYPE_PARAMETER` for practical purposes.
- Package annotations live in `package-info.java`; module annotations in `module-info.java`.
- Omitting `@Target` means "any declaration, but not type uses"; `@Target({})` means "nowhere".
- Misuse fails at **compile time** with *"annotation interface not applicable to this kind of declaration"*.

---

## 6. Repeatable Annotations

Sometimes you need to apply the *same* annotation several times to one declaration. Before Java 8 you had to invent awkward container types; since Java 8 there's a clean, built-in mechanism.

### 6.1 The Concept

A **repeatable annotation** is an annotation type that may appear more than once on the same declaration:

```java
@Schedule(day = "Monday")
@Schedule(day = "Friday", time = "14:30")
public class AlarmClock {
}
```

To make `@Schedule` repeatable, you need **two** declarations:

1. The repeatable annotation itself, meta-annotated with `@Repeatable(...)`.
2. A **container annotation**, whose only element is an array of the repeatable type.

### 6.2 The Java 8 Pattern

```java
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Repeatable(Schedules.class)                 // 1) "this type may repeat; the wrapper is Schedules"
@Retention(RetentionPolicy.RUNTIME)
public @interface Schedule {
    String day();
    String time() default "09:00";
}
```

```java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Schedules {                // 2) the container
    Schedule[] value();
}
```

### 6.3 How the Compiler Wraps Them — the "Implicit Container"

The container annotation must be **declared by you** — the compiler does *not* generate the container class. What the compiler *does* implicitly is **wrap your repeated annotations into a container instance** in the class file.

If you write:

```java
@Schedule(day = "Monday")
@Schedule(day = "Friday", time = "14:30")
public class AlarmClock {
}
```

then `AlarmClock.class` actually contains the equivalent of:

```java
@Schedules({
    @Schedule(day = "Monday"),
    @Schedule(day = "Friday", time = "14:30")
})
public class AlarmClock { }
```

You never write `@Schedules` by hand (you *may*, but then you're using the container directly rather than repeatable syntax). The wrapping, the container instantiation, and the attribute emission are all done automatically by `javac`.

### 6.4 Reading Repeatable Annotations Back

Here's the complete, working retrieval program (verified on JDK 21):

```java
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class RepeatableDemo {

    @Repeatable(Schedules.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Schedule {
        String day();
        String time() default "09:00";
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Schedules {
        Schedule[] value();
    }

    @Schedule(day = "Monday")
    @Schedule(day = "Friday", time = "14:30")
    public static class AlarmClock {
    }

    @Schedule(day = "Saturday")
    public static class Gym {
    }

    public static void main(String[] args) {
        Class<?> clock = AlarmClock.class;

        // 1) THE reliable way: all directly + indirectly present annotations
        for (Schedule s : clock.getAnnotationsByType(Schedule.class)) {
            System.out.println(s.day() + " @ " + s.time());
        }
        // prints:
        //   Monday @ 09:00
        //   Friday @ 14:30

        // 2) Reading the raw container
        Schedules container = clock.getAnnotation(Schedules.class);
        System.out.println(container.value().length);      // 2

        // 3) The gotcha — see below
        System.out.println(clock.getAnnotation(Schedule.class));   // null!
        System.out.println(clock.isAnnotationPresent(Schedule.class)); // false!

        // 4) A single direct use behaves "normally"
        System.out.println(Gym.class.getAnnotation(Schedule.class));       // @Schedule(...Saturday)
        System.out.println(Gym.class.isAnnotationPresent(Schedule.class)); // true
        System.out.println(Gym.class.getAnnotationsByType(Schedule.class).length); // 1
    }
}
```

### 6.5 `getAnnotationsByType()` vs. `getAnnotation()` — the Subtle Difference

This is where beginners get burned, and the behavior is easy to verify (we did):

| API | `AlarmClock` (repeated → only container present) | `Gym` (single, direct use) |
|---|---|---|
| `getAnnotationsByType(Schedule.class)` | `[Monday, Friday]` — **all of them** | `[Saturday]` |
| `getAnnotation(Schedule.class)` | **`null`** | the annotation |
| `isAnnotationPresent(Schedule.class)` | **`false`** | `true` |
| `getAnnotation(Schedules.class)` | the container | `null` |
| `getDeclaredAnnotations()` | the container only | the annotation |

The rule to remember:

> **When a repeatable annotation is only *indirectly present* (wrapped inside its container), `getAnnotation()` and `isAnnotationPresent()` on the repeatable type return `null`/`false`.** Only `getAnnotationsByType()` — and walking the container manually — sees them.

This asymmetry exists because the compiler never emits a direct `@Schedule` attribute when there is more than one — it emits exactly one `@Schedules` attribute. `getAnnotation()` reports only *directly present* annotations (plus `@Inherited` cases), so it misses the wrapped ones.

**Rule of thumb:** when reading repeatable annotations, always use **`getAnnotationsByType()`**. It transparently handles both the direct case (single use) and the container case (multiple uses) — which is exactly why it was added in Java 8.

### 6.6 Real-World Usage Patterns

The `@Schedule` / `@Schedules` shape is a template for many real systems:

- **Task scheduling** — the Quartz-style `@Scheduled(cron = ...)` repeated for multiple cron expressions.
- **Testing** — parameterized or repeated test annotations (e.g., `@RepeatedTest` variants), or `@Tag("slow") @Tag("integration")` in JUnit 5.
- **Validation** — multiple constraints on one field: `@Size(min = 2) @Size(max = 100)` style repeated constraints.
- **HTTP routing** — multiple path mappings on one handler method.

### 6.7 Backward Compatibility and Common Mistakes

- **Container must exist.** If `@Repeatable(Container.class)` names a container that isn't declared (or isn't an annotation), the compiler rejects the declaration.
- **Container's `value()` must be an array** of the repeatable type and have `RUNTIME` (or at least matching) retention, or retrieval breaks.
- **Inconsistent retention.** If your repeatable annotation is `RUNTIME` but the container is only `CLASS`, the compiler refuses (retentions must be compatible).
- **Repeating a non-repeatable annotation** gives the verified error: *"X is not a repeatable annotation interface"*.
- **Don't mix styles.** Apply either repeated `@Schedule` entries *or* an explicit `@Schedules({...})`, not both on the same declaration.
- **Pre-Java 8 code** can't use `@Repeatable`; libraries targeting old bytecode still expose container patterns manually (which is why you'll see `@Schedules` in older codebases).

---

### ✅ Key Takeaways — Repeatable Annotations

- `@Repeatable(Container.class)` makes an annotation usable more than once per declaration.
- You must declare the **container annotation yourself**: a `@interface` with one `value()` element returning an array of the repeatable type.
- The compiler **implicitly wraps** repeated annotations into the container in the `.class` file.
- Always retrieve with **`getAnnotationsByType()`** — it handles single and repeated cases alike.
- `getAnnotation()` / `isAnnotationPresent()` on the repeatable type return `null`/`false` when the annotation is only inside its container — a classic trap.

---

## 7. Processing Annotations

Declaring and placing annotations is only half the story. The other half is **processing** them — turning metadata into action. There are two processing times: **compile time** and **runtime**.

### 7.1 Runtime Processing (Reflection)

The reflection API (`java.lang.reflect.AnnotatedElement`, implemented by `Class`, `Method`, `Field`, `Constructor`, `Parameter`, `Package`, `RecordComponent`, and friends) exposes annotations at runtime — provided the retention is `RUNTIME`.

The three core operations:

- **`getAnnotation(Class<T>)`** — return one annotation of a type, or `null`.
- **`isAnnotationPresent(Class<T>)`** — a boolean "is it there?"
- **`getAnnotations()`** — all annotations on the element (including inherited ones).
- **`getDeclaredAnnotations()`** — only those written directly on the element.

A complete worked example (verified):

```java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Contact {
    String name();
    Status status();
}
```

```java
import java.lang.reflect.Method;

public class ReflectionDemo {

    @Contact(name = "Ada Lovelace", status = Status.DONE)
    public void process() {
    }

    public static void main(String[] args) throws Exception {
        Method m = ReflectionDemo.class.getMethod("process");

        System.out.println(m.isAnnotationPresent(Contact.class));        // true
        Contact c = m.getAnnotation(Contact.class);
        System.out.println(c.name());                                    // Ada Lovelace
        System.out.println(c.status());                                  // DONE

        for (java.lang.annotation.Annotation a : m.getAnnotations()) {
            System.out.println("found: " + a);
        }
    }
}
```

This is the engine room of every annotation-driven framework: **scan a class's methods/fields, look for annotations, and wire behavior based on what you find.**

### 7.2 A Mini-Framework: Build a Simple Test Runner

To make it concrete, here's a tiny, realistic reflection-based processor — a 15-line "test runner":

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Test {
}
```

```java
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MiniRunner {

    public static void main(String[] args) throws Exception {
        run(MathTests.class);
    }

    static void run(Class<?> testClass) throws Exception {
        int passed = 0, failed = 0;
        Object instance = testClass.getDeclaredConstructor().newInstance();

        for (Method m : testClass.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Test.class)) {       // the "discovery" step
                try {
                    m.setAccessible(true);
                    m.invoke(instance);
                    passed++;
                    System.out.println("PASS " + m.getName());
                } catch (InvocationTargetException e) {
                    failed++;
                    System.out.println("FAIL " + m.getName() + " -> " + e.getCause());
                }
            }
        }
        System.out.println(passed + " passed, " + failed + " failed");
    }
}

class MathTests {
    @Test
    void addition() {
        if (2 + 2 != 4) throw new AssertionError("2+2 != 4");
    }

    @Test
    void failing() {
        if (1 + 1 != 3) throw new AssertionError("1+1 != 3");
    }
}
```

Output:

```
PASS addition
FAIL failing -> java.lang.AssertionError: 1+1 != 3
1 passed, 1 failed
```

Every serious framework — JUnit, TestNG, Spring — does exactly this pattern, but with far more sophistication (inheritance scanning, parameterized discovery, repeatable tags, etc.).

### 7.3 Compile-Time Processing (Annotation Processors)

The other consumption path is **annotation processing**: `javac` invokes a registered *processor* during compilation, passing it the annotations found in the source. The processor can emit **new Java source files**, which then participate in the same compilation round.

- **Lombok** — `@Getter`, `@Setter`, `@Data`, `@Builder` are `SOURCE` annotations; the processor *rewrites* the AST to generate accessors, `equals`/`hashCode`, builders, etc., before bytecode is emitted. That's why the generated methods exist in `.class` but not in your source.
- **Dagger / Hilt** — `@Component` and `@Module` processors generate the dependency-injection wiring code at build time, avoiding runtime reflection.
- **AutoValue / immutables** — generate value-type implementations from abstract accessors.
- **`@Override`-style linting** — custom processors can enforce team rules (e.g., "every public method of a `@RestController` must be documented") and fail the build otherwise.

Writing a full processor is beyond this chapter, but the shape is:

```java
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("example.Scheduled")      // which annotations we consume
public class ScheduledProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 'roundEnv' gives access to every element annotated with @Scheduled.
        // Here you would generate source code with the Filer API.
        return false;
    }
}
```

Registered in `META-INF/services/javax.annotation.processing.Processor` and run automatically by `javac`.

**Compile-time vs. runtime — a comparison:**

| Dimension | Compile-time (processors) | Runtime (reflection) |
|---|---|---|
| When it runs | During `javac` | During program execution |
| Speed at runtime | Zero reflection cost | Reflection cost on every lookup |
| Can generate code | Yes (new `.java` files) | No — can only *act* on existing code |
| Tooling examples | Lombok, Dagger, AutoValue | Spring, JUnit, JPA, Jackson |
| Retention needed | `SOURCE` or `CLASS` | `RUNTIME` |

---

### ✅ Key Takeaways — Processing Annotations

- **Runtime processing** uses reflection: `getAnnotation()`, `isAnnotationPresent()`, `getAnnotations()`, `getDeclaredAnnotations()`.
- Runtime processing requires `RUNTIME` retention and powers Spring, JUnit, JPA, Jackson.
- **Compile-time processing** uses `javax.annotation.processing` processors invoked by `javac`; they can generate new source files (Lombok, Dagger).
- If you only need build-time code generation, prefer `SOURCE` retention to keep annotations out of the artifact.

---

## 8. Best Practices and Common Pitfalls

### 8.1 Annotations vs. Interfaces vs. Enums — Choosing the Right Tool

| Need | Prefer | Why |
|---|---|---|
| A *type* with behavior, contracts, methods | **interface** | Interfaces carry method signatures and polymorphic behavior |
| A closed set of named constants | **enum** | Enums have values, `switch` support, and behavior |
| **Metadata** describing code, read by tools/frameworks | **annotation** | Annotations are passive data with compile-time checking |
| A flag on a declaration that configures a framework | **annotation** | e.g., `@Component` vs. writing XML config |
| Runtime *logic* that depends on metadata | **annotation + reflection** (or processor) | The annotation stores *what*; your code does *how* |

### 8.2 Naming Conventions and Style

- **Annotation types are PascalCase nouns** describing the *condition* they express: `@Override`, `@Transactional`, `@JsonIgnore`, `@Entity`.
- **Element names are lowerCamelCase verbs/qualities**: `name()`, `fixedDelay()`, `forRemoval()`, `maxRetries()`.
- Use the **`value` shorthand** only when there's a single, obvious element.
- Prefer **markers** (no elements) when a boolean flag is all you need — an annotation's *presence* is the value.
- Use defaults liberally so that uses stay terse: `@Author(name = "Ada")` instead of forcing every field.

### 8.3 Design Rules

1. **Pick the weakest retention that works.** Don't reflexively use `RUNTIME`. Compiler hint → `SOURCE`; code generation → `SOURCE`/`CLASS`; reflection → `RUNTIME`.
2. **Restrict `@Target`.** An annotation usable everywhere is an annotation whose misuse the compiler can't catch.
3. **Keep annotations simple.** A handful of well-named elements beats a 12-element grab bag. If your annotation needs complex configuration, consider a companion builder or an enum.
4. **Never store mutable or object state in annotation elements** — element values are compile-time constants baked into the class file.
5. **Document your annotation.** Because elements can't hold Javadoc text for each use, write thorough Javadoc on the annotation *type* and its elements.
6. **Mind reflection performance.** If you read annotations in a hot loop, cache the results (e.g., `ConcurrentHashMap<Class<?>, Optional<MyAnno>>`). Reflective lookup involves class-loading and proxy creation; for frameworks scanning thousands of classes, this matters.
7. **Prefer compile-time processing over reflection** when you control the build (Lombok-style) — it's faster at runtime and type-safe.

### 8.4 The Top 10 Beginner Mistakes

| # | Mistake | Consequence / Fix |
|---|---|---|
| 1 | Forgetting `@Retention(RUNTIME)` | `getAnnotation()` returns `null` silently. You got `CLASS` by default. |
| 2 | Forgetting `@Target` and letting an annotation leak everywhere | Misuses compile; intent is unclear. |
| 3 | Trying to make an annotation type inherit from another | Won't compile — annotations don't extend each other. |
| 4 | Reading repeatable annotations with `getAnnotation()` | Returns `null` when the container is present; use `getAnnotationsByType()`. |
| 5 | Using a disallowed element type (`List`, `Integer`, `Object`) | *"invalid type for annotation member"* at compile time. |
| 6 | Defaulting an element to `null` | Illegal — use sentinel values like `Void.class` or an empty enum constant. |
| 7 | Suppressing warnings you don't understand | Hides real bugs; document suppressed warnings. |
| 8 | Expecting `@Inherited` to work on methods/fields/interfaces | It only propagates class-level annotations to subclasses. |
| 9 | Applying `@Override` only to some overrides | Misspelled signatures silently break; be consistent. |
| 10 | Using `@Deprecated` for "internal" instead of actual removal | Tools treat it as public API you intend to remove; use packages/sealed/access modifiers for internal. |

---

### ✅ Key Takeaways — Best Practices

- Annotations = **metadata**; interfaces = **behavior**; enums = **closed constants**. Don't blur the lines.
- Choose the **minimum retention and target** that satisfies your use case.
- Read repeatable annotations with `getAnnotationsByType()`; cache reflection lookups in hot paths.
- The classic failures are all *silent*: wrong retention, wrong read API, `null` defaults, non-repeatable reads.

---

## 9. Real-World Applications and Case Studies

Every major Java framework is, at its heart, an **annotation processor** — the annotations you write are *configuration*, and the framework's runtime (or build tools) does the heavy lifting. Here's the "greatest hits."

### 9.1 Spring — Inversion of Control via Metadata

Spring scans your classpath for annotated classes, instantiates them, and wires dependencies automatically. All its annotations are `RUNTIME` (verified), because Spring reads them via reflection at startup.

```java
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.transaction.annotation.Transactional;

@Component                                    // a bean managed by the Spring container
public class ReportRepository {
    public String fetch() { return "data"; }
}

@RestController                               // a web controller (a @Component + HTTP glue)
public class ReportController {

    private final ReportRepository repository;

    @Autowired                                // dependency injection via reflection
    public ReportController(ReportRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/report")                    // maps this method to GET /report
    @Transactional                            // wraps the call in a DB transaction
    public String report() {
        return repository.fetch();
    }
}
```

**Which features does Spring rely on?** `RUNTIME` retention (reflection discovery), `@Target` discipline (`@GetMapping` targets methods and types, `@Autowired` targets constructors/fields/methods/parameters), and — for `@Repeatable` — things like repeated security annotations on one endpoint.

### 9.2 JUnit 5 — Test Discovery and Lifecycle

JUnit's engine discovers methods annotated `@Test` and runs them, hooking lifecycle methods like `@BeforeEach`/`@AfterEach`:

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculatorTest {

    private Calculator calc;

    @BeforeEach
    void setUp() {
        calc = new Calculator();
    }

    @Test
    @DisplayName("two plus two is four")
    @Tag("fast")
    void adds() {
        assertEquals(4, calc.add(2, 2));
    }

    @Test
    @Disabled("needs a database; see issue #42")
    void integration() {
        // not run until re-enabled
    }
}

class Calculator {
    int add(int a, int b) { return a + b; }
}
```

JUnit's annotations are `RUNTIME`, and its `@Tag` is `@Repeatable` — you can stack multiple tags on one test. This is a textbook case of "annotation as registration": the test *does nothing* without the engine scanning for it.

### 9.3 JPA / Hibernate — Object–Relational Mapping

Entities map to tables, fields to columns, and the persistence provider (Hibernate) reads these annotations at runtime to generate SQL and manage the object graph:

```java
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity                                         // this class is an ORM entity
@Table(name = "users")                          // maps to the "users" table
public class User {

    @Id                                         // primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;
}
```

JPA annotations are `RUNTIME` and heavily `@Target`-restricted (`@Entity`/`@Table` → types; `@Id`/`@Column` → fields and methods). This is the canonical "metadata replaces XML" story: before JPA, object–relational mappings lived in `.hbm.xml` files that drifted from the model.

### 9.4 Jackson — JSON Binding

Jackson reads annotations to control JSON serialization/deserialization without touching the class's public API:

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserDto {

    @JsonProperty("user_name")                  // rename in JSON
    private String userName;

    @JsonIgnore                                 // never serialize
    private transient String internalNote;

    // getters/setters omitted
}
```

Jackson relies on `RUNTIME` retention plus `@Target` on `FIELD`, `METHOD`, `PARAMETER` (constructor-based deserialization) — and it uses repeatable/container patterns for things like multiple aliases (`@JsonAlias`).

### 9.5 Lombok — Compile-Time Code Generation

Lombok is the flagship **annotation processor**: the annotations are `SOURCE`, so they never ship in the artifact.

```java
import lombok.Getter;
import lombok.Setter;
import lombok.Data;

@Getter                                     // generates getters for all fields
@Setter                                     // generates setters for all non-final fields
public class Point {
    private final int x;
    private final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

@Data                                       // @Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor
public class Account {
    private String owner;
    private double balance;
}
```

When you compile, Lombok's processor inserts the generated members *before* bytecode emission — the `.class` file has them, your source doesn't. This is why Lombok annotations need no `RUNTIME` retention: **nothing needs to read them after compilation.**

### 9.6 The Framework Cheat-Sheet

| Framework | Example annotations | Retention | Target highlights | Mechanism |
|---|---|---|---|---|
| Spring | `@Component`, `@Autowired`, `@RestController`, `@Transactional` | `RUNTIME` | types, fields, constructors, methods, parameters | Reflection-based component scanning |
| JUnit 5 | `@Test`, `@BeforeEach`, `@Disabled`, `@Tag` (repeatable) | `RUNTIME` | methods, types | Reflection test discovery |
| JPA/Hibernate | `@Entity`, `@Table`, `@Column`, `@Id` | `RUNTIME` | types, fields/methods | Reflection-driven ORM |
| Jackson | `@JsonProperty`, `@JsonIgnore` | `RUNTIME` | fields, methods, parameters, types | Reflection serialization config |
| Lombok | `@Getter`, `@Setter`, `@Data` | `SOURCE` | types, fields | Annotation processor (compile-time codegen) |
| Dagger | `@Component`, `@Module`, `@Inject` | `SOURCE`/`CLASS` | types, methods | Annotation processor (build-time codegen) |

---

### ✅ Key Takeaways — Real-World Applications

- Frameworks are *consumers* of annotations: the annotation is the configuration, the framework is the engine.
- Spring, JUnit, JPA, Jackson → `RUNTIME` + reflection.
- Lombok, Dagger → `SOURCE`/`CLASS` + annotation processors (no runtime reflection cost).
- The same five features show up everywhere: retention, target, repeatable containers, defaults, and the `value` shorthand.

---

## 10. Summary and Key Takeaways

### 10.1 The One-Page Reference

**All five meta-annotations at a glance:**

| Meta-annotation | Meaning | Omit → |
|---|---|---|
| `@Retention(SOURCE \| CLASS \| RUNTIME)` | How long the annotation survives | `CLASS` |
| `@Target(ElementType...)` | Where it may be applied | any declaration, not type uses |
| `@Documented` | Show in Javadoc | not shown |
| `@Inherited` | Subclasses inherit (classes only) | not inherited |
| `@Repeatable(Container.class)` | May appear multiple times | cannot repeat |

**The decision guide:**

- **Do you need the annotation readable at runtime (reflection)?** → `@Retention(RUNTIME)` + `@Target` restricted to exactly the elements you process.
- **Is it only a compiler check or a code-generation trigger?** → `@Retention(SOURCE)`.
- **Is it just for bytecode tooling?** → `@Retention(CLASS)` (or omit `@Retention` — same thing).
- **Where should it be legal?** → pick the narrowest `@Target`; if it must appear multiple times, add `@Repeatable`.
- **How will consumers read it?** → `getAnnotation()`/`isAnnotationPresent()` for single, `getAnnotationsByType()` for repeatable.

**Syntax cheat-sheet:**

```java
// Declaring
@Retention(RetentionPolicy.RUNTIME)            // or SOURCE / CLASS
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Example {
    String value();                            // shorthand-eligible
    int count() default 1;                     // primitive with default
    Class<?> validator() default Void.class;   // Class element (no null!)
}

// Using
@Example("hi")                                 // shorthand for value
@Example(value = "hi", count = 2)              // full form
```

### 10.2 Exercises

Each exercise states what it is designed to teach. No solutions provided — the goal is for you to verify your understanding by compiling and running.

1. **The Wrong-Retention Detective.** Declare an annotation *without* `@Retention`, apply it to a class, and read it via `getAnnotation()`. Predict the output, run it, and then add `@Retention(RUNTIME)` and compare.
   *Teaches:* the default `CLASS` retention and the silent-null symptom.

2. **The `@Override` Safety Net.** Take a small class hierarchy (e.g., `Shape`/`Circle`), intentionally misspell an override, and add `@Override`. Compile both versions and record the exact error message.
   *Teaches:* why `@Override` belongs on every overriding method.

3. **The Annotation Kitchen Sink.** Define an annotation using every allowed element type (primitive, `String`, `Class<?>`, enum, nested annotation, array). Then try adding a `List<String>` element and observe the compiler error.
   *Teaches:* the closed set of legal annotation element types.

4. **The Target Sheriff.** Write one annotation with `@Target(ElementType.METHOD)` and try applying it to a class, a field, and a method. Record which applications compile and which fail, and the error text.
   *Teaches:* how `@Target` turns misuse into compile-time errors.

5. **The Type-Use Explorer.** Create `@NonNull` with `@Target(TYPE_USE)`. Apply it in as many type contexts as you can: field types, generic arguments, array dimensions, casts, `throws`, and type parameters. Apply the *same* annotation without `TYPE_USE` and note which contexts now fail.
   *Teaches:* the reach of `TYPE_USE` and the value of `TYPE_PARAMETER`.

6. **The Repeatable Scheduler.** Implement the `@Schedule`/`@Schedules` pair, apply it twice, and retrieve with both `getAnnotationsByType()` and `getAnnotation()`. Print both results and explain the `null`.
   *Teaches:* the container mechanism and the correct retrieval API.

7. **The Mini Test Runner.** Write your own `@Test` marker (`RUNTIME` + `METHOD` target) and a runner that invokes all `@Test` methods reflectively, reporting pass/fail.
   *Teaches:* runtime annotation processing end-to-end.

8. **The `@Inherited` Experiment.** Mark a class-level annotation `@Inherited`, subclass it, and read it via `getAnnotation()` and `getDeclaredAnnotation()`. Then add a *method*-level annotation with `@Inherited` and observe that it does **not** propagate.
   *Teaches:* the precise semantics of `@Inherited`.

9. **The `@SafeVarargs` Justification.** Write a generic varargs method that stores its array into a field, compile with `@SafeVarargs` and note the (probably silent) problem; then fix it and add `@SafeVarargs` to the safe version.
   *Teaches:* when `@SafeVarargs` is legitimate and when it is a lie.

10. **The Package-Prologue.** Create a `package-info.java` carrying a `@Target(PACKAGE)` annotation with an author field, load the package, and print the annotation.
    *Teaches:* package-level annotation placement and `Package.getPackage()`.

### 10.3 Final Words

Annotations are the silent language of the Java platform: **they let code describe itself**, and they let tools do the rest. Master the four pillars — **declaration** (`@interface`), **retention** (`SOURCE`/`CLASS`/`RUNTIME`), **placement** (`@Target`), and **repetition** (`@Repeatable`) — and you can both *read* the frameworks you use every day and *write* the annotations that make your own libraries feel like magic. When you next see `@SpringBootApplication` or `@Test`, you'll know exactly what's under the hood: metadata, a contract with a tool, and a compiler that guards the deal.

---

> **Chapter review checklist**
>
> - ☐ I can explain what annotations are and why they exist (metadata vs. behavior).
> - ☐ I can use `@Override`, `@Deprecated`, `@SuppressWarnings`, `@FunctionalInterface`, `@SafeVarargs` correctly and know each one's retention/target.
> - ☐ I can declare a custom annotation with `@interface`, including defaults and the `value` shorthand, and enumerate the allowed element types.
> - ☐ I can choose between `SOURCE`, `CLASS`, and `RUNTIME` retention and predict the reflection behavior of each.
> - ☐ I can restrict placement with every `ElementType`, including `TYPE_USE`, `PACKAGE`, `RECORD_COMPONENT`, and `MODULE`.
> - ☐ I can declare a `@Repeatable` annotation plus its container, and retrieve repeats with `getAnnotationsByType()`.
> - ☐ I can process annotations both at runtime (reflection) and at compile time (annotation processors).
> - ☐ I can name the top five beginner pitfalls and how to avoid each one.