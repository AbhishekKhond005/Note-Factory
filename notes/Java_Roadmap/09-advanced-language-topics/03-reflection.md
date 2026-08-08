# Reflection in Java

## Table of Contents
1. [Introduction](#1-introduction)
2. [The `Class` Object](#2-the-class-object)
3. [Inspecting Fields, Methods, and Constructors](#3-inspecting-fields-methods-and-constructors)
4. [Working with Annotations](#4-working-with-annotations)
5. [Real-World Use Cases](#5-real-world-use-cases)
6. [Risks, Performance, and Best Practices](#6-risks-performance-and-best-practices)
7. [Chapter Summary](#7-chapter-summary)

---

# 1. Introduction

**Imagine this:** You have just joined a company that maintains a large legacy codebase. Hundreds of classes, written over a decade by a dozen developers, live in your `lib` directory. Your boss wants a tool that prints an inventory report of every "business entity" class — its fields, their types, and whether any of them are annotated as "sensitive." You did not write these classes. You have no source for most of them. And you cannot wait for the authors to add an interface for you.

How can a program written *today* understand classes written *years ago*, without ever having compiled against them?

The answer is **reflection**.

## What is reflection?

In plain language, **reflection** is the ability of a running Java program to *examine* and *manipulate itself at runtime*. A Java program can ask the JVM:

- "What class is this object an instance of?"
- "What fields, methods, and constructors does this class declare?"
- "What annotations are attached to this code element?"
- "Can I invoke that method or read that field — even if it's `private`?"

**Analogy.** Think of a movie set. Normally, actors follow the screenplay (the compiled code) and play their roles. Reflection is like the *director* stepping in mid-scene with the script in hand: the director can see every actor on set (all loaded classes), read their names and costumes (fields and annotations), and say "actor #3, deliver your line now" (invoke a method) — even for actors who didn't audition for the scene (classes you never explicitly wrote code against).

The name comes from the idea that the program's behavior is *reflected* in a runtime image of itself. The JVM maintains metadata about every loaded type, and `java.lang.reflect` gives you a well-defined API to read that metadata.

## A common misconception (flag it early)

> **"Reflection is only for framework authors."**

Wrong. While frameworks are the most visible consumers, reflection powers tools *you* use daily: your IDE's autocomplete, your debugger's expression viewer, your profiler's stack sampling, and your build tool's annotation processing. Understanding reflection makes you a better user and a better designer of these tools — and occasionally, a better architect of your own code.

## Chapter roadmap

Here is where this chapter will take you:

1. **§2 — The `Class` Object:** the entry point for all reflection. What it is, how to get one, and how the JVM's class-loading machinery creates it.
2. **§3 — Fields, Methods, Constructors:** the `java.lang.reflect` workhorses — inspecting, reading, and *invoking* members at runtime, including private ones.
3. **§4 — Annotations:** how reflection reads the metadata your code carries on its shoulders.
4. **§5 — Real-World Use Cases:** DI frameworks, ORMs, JSON libraries, test runners, debuggers, and proxy architectures — each mapped back to the APIs in §2–§4.
5. **§6 — Risks, Performance, and Best Practices:** why reflective calls are slow, why they break encapsulation, and how to use reflection responsibly.
6. **§7 — Chapter Summary:** concept-to-API mapping table plus progressive exercises.

---

# 2. The `Class` Object

## 2.1 What is a `Class` object?

Every type that the JVM loads — classes, interfaces, enums, annotations, arrays, even primitives — is represented at runtime by an instance of `java.lang.Class<T>`. There is exactly **one** `Class` object per loaded type per class loader.

Think of a `Class` object as the **blueprint catalog entry** for a type: while instances of a class are the buildings themselves, the `Class` object is the official file in the city planning office that describes the blueprint — its name, who built it, what its rooms (fields) and doors (methods) are.

Crucially, the `Class` object is distinct from the instances it describes. `"hello".getClass()` and `"world".getClass()` return the *same* object: `String.class`.

## 2.2 The three ways to obtain a `Class` reference

### 1. `obj.getClass()`

If you already have an instance, ask it who it belongs to:

```java
Object mystery = "a string, but typed as Object";
Class<?> c = mystery.getClass();
System.out.println(c.getName());   // java.lang.String
```

Note that `getClass()` gives you the **runtime** type of the object — *not* the static type of the variable. This is the most common source of the misconception "`getClass()` returns the superclass": if the variable is typed `Object` but holds a `String`, `getClass()` returns `String.class`.

### 2. `ClassName.class`

If you know the type at compile time, use the class literal:

```java
Class<String> c = String.class;      // type-safe: Class<String>
Class<? super String> sup = String.class.getSuperclass(); // Object.class
```

Class literals are compile-time constants, require **no instance**, and are checked by the compiler — so `String.class` has the generic type `Class<String>`. This is the preferred form when the type is known statically.

### 3. `Class.forName("fully.qualified.Name")`

If you only know the name as a *string* (from configuration, a database, a user input), reflect on the string:

```java
try {
    Class<?> c = Class.forName("java.util.ArrayList");
    Object list = c.getDeclaredConstructor().newInstance();
    System.out.println(c.getName());   // java.util.ArrayList
} catch (ClassNotFoundException e) {
    System.out.println("No such class on the classpath: " + e.getMessage());
}
```

`Class.forName` throws the checked `ClassNotFoundException` — you must handle or declare it. It also has a subtle side effect: by default it **initializes** the class (runs static initializers). That can trigger anything from harmless counters to database connections. The overload `Class.forName(String name, boolean initialize, ClassLoader loader)` lets you suppress initialization:

```java
Class<?> c = Class.forName("com.example.LazyClass", false,
                           ClassLoader.getSystemClassLoader());
```

### Comparison table

| Aspect | `obj.getClass()` | `ClassName.class` | `Class.forName(...)` |
|---|---|---|---|
| **When to use** | You have an instance and want its *runtime* type | You know the type statically and want type safety | You know only a *string* name (config, plugins, load at runtime) |
| **Availability** | Any object at runtime | Always available at compile time | Requires the class to be on the classpath / module path at runtime |
| **Compile-time type safety** | `Class<? extends X>` if variable is `X` | `Class<T>` — fully generic | `Class<?>` — no type info |
| **Checked exception** | None | None | `ClassNotFoundException` |
| **Initialization side effects** | None (already initialized) | Class literal does **not** initialize (just loads/resolves) | **Initializes** the class by default (use 3-arg overload to avoid) |
| **Common pitfall** | Returns runtime type, which may surprise you | Cannot be parameterized dynamically | String typos fail at runtime, not compile time; triggers static initializers |

> **Misconception alert.** "Class literals initialize the class." Not so — `Foo.class` does *not* run static initializers. Only the first *active use* (new instance, static field access, method invocation, `Class.forName` with default flags) initializes a class.

## 2.3 Primitives, arrays, and interfaces

Reflection handles far more than ordinary classes.

```java
// Primitives — yes, even these have Class objects
System.out.println(int.class.isPrimitive());      // true
System.out.println(int.class.getName());          // int

// Arrays — Class object exists per (component type, dimension) pair
Class<?> strArray = String[].class;
System.out.println(strArray.isArray());           // true
System.out.println(strArray.getComponentType());  // class java.lang.String

// Interfaces
Class<?> r = Runnable.class;
System.out.println(r.isInterface());              // true

// Assignability relationships
System.out.println(Integer.class.isAssignableFrom(int.class)); // false!
```

Interesting consequences:

- `int.class` and `Integer.class` are **different** objects. `int.class.isAssignableFrom(Integer.class)` is `false`. Autoboxing is a *compiler* convenience, not a type-system identity.
- `String[].class`, `String[][].class`, and `Object[].class` are all distinct `Class` objects.
- Array classes are created automatically by the JVM when the array type is first used — you cannot "see" them in source.

## 2.4 Class loading: the JVM's role

Reflection is only possible because the JVM keeps rich metadata for every loaded type. The story goes like this:

1. **Loading** — the class loader (bootstrap, platform, or application) reads the `.class` bytecode and produces a `Class` object.
2. **Linking** — verification, preparation, and (optionally) resolution happen.
3. **Initialization** — static initializers run, and `static final` constants are given their final values.

The `Class` object returned by `Class.forName` is a window into step 1 (and, by default, step 3). Every JVM knows the invariant: **one class loader + one class name ⇒ one `Class` object.** Two class loaders loading the same binary name produce *different* `Class` objects, and instances of one are not assignable to the other — a notorious source of `ClassCastException` bugs in application servers and plugin systems.

Modern reflection also interacts with **modules** (Java 9+): `Class.getModule()` tells you which module a type lives in, and module exports govern whether other modules may reflect on it (we return to this in §6).

---

### 📌 Key Takeaways

- Every loaded type is represented by exactly one `Class` object per class loader — the "blueprint catalog entry."
- Three ways to obtain it: `obj.getClass()` (runtime type), `ClassName.class` (compile-time, type-safe), `Class.forName(...)` (by string, throws `ClassNotFoundException`, initializes by default).
- `int.class`, array classes, and interface classes all exist and behave consistently.
- Class literals do **not** initialize the class; active use does.

### ⚡ Quick Check

**Question:** You load the same binary name `"com.example.Gadget"` in two separate class loaders. Can you safely cast an instance created by loader A to the `Gadget` class seen by loader B? Why or why not?

<details>
<summary>Answer</summary>

**No.** Each class loader creates its own `Class` object for the binary name, and the two `Class` objects are unrelated. An object created via loader A's `Gadget` is an instance of *that* `Class`, not of loader B's `Gadget`. An explicit cast `(GadgetB) instanceOfGadgetA` throws `ClassCastException`. This is the classic "class loader leak" / "duplicate class" problem in application servers and plugin systems.

</details>

---

# 3. Inspecting Fields, Methods, and Constructors

The package `java.lang.reflect` provides the three heavy hitters:

- `java.lang.reflect.Field` — a single field (its name, type, modifiers, value).
- `java.lang.reflect.Method` — a single method (parameters, return type, modifiers, invocable).
- `java.lang.reflect.Constructor<?>` — a single constructor (parameters, modifiers, invocable to create instances).

All three extend `java.lang.reflect.AccessibleObject`, which gives them `setAccessible(boolean)` — the switch that lets you poke through `private`.

## 3.1 Fields

The two families of lookup methods mirror each other across fields, methods, and constructors:

| Method | Returns | Visibility | Inherited members? |
|---|---|---|---|
| `getFields()` | `Field[]` | `public` only | **Yes** |
| `getDeclaredFields()` | `Field[]` | all (incl. `private`, `protected`, package) | **No** |
| `getField("name")` | single `Field` | `public` only | Yes |
| `getDeclaredField("name")` | single `Field` | any declared field | No |

```java
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

class Person {
    private String name;          // private — invisible to getFields()
    protected int age;
    public static final long serialVersionUID = 1L;
}

public class FieldDemo {
    public static void main(String[] args) throws Exception {
        System.out.println("--- getFields() (public only) ---");
        for (Field f : Person.class.getFields())
            System.out.println(Modifier.toString(f.getModifiers()) + " " + f.getType().getSimpleName() + " " + f.getName());

        System.out.println("--- getDeclaredFields() (everything declared) ---");
        for (Field f : Person.class.getDeclaredFields())
            System.out.println(Modifier.toString(f.getModifiers()) + " " + f.getType().getSimpleName() + " " + f.getName());
    }
}
```

**Expected output:**

```
--- getFields() (public only) ---
public static final long serialVersionUID
--- getDeclaredFields() (everything declared) ---
private java.lang.String name
protected int age
public static final long serialVersionUID
```

Notice how `getDeclaredFields()` reveals `private` and `protected` members that `getFields()` hides.

### Reading and setting field values

```java
import java.lang.reflect.Field;

public class FieldAccessDemo {
    static class Counter {
        private int count = 0;   // private!
    }

    public static void main(String[] args) throws Exception {
        Counter c = new Counter();
        Field f = Counter.class.getDeclaredField("count");

        f.setAccessible(true);              // punch through private
        int current = (int) f.get(c);       // read: f.get(instance)
        System.out.println("before: " + current);   // 0

        f.setInt(c, 42);                    // write: primitive-aware setter
        System.out.println("after:  " + f.getInt(c)); // 42
    }
}
```

For static fields, pass `null` as the instance argument: `f.get(null)`.

### The `setAccessible(true)` controversy

By default, Java's access control (private/protected/package) is enforced even through reflection: calling `f.get(c)` on a `private` field throws `IllegalAccessException`. Calling `setAccessible(true)` *suppresses* those checks for this object.

This is a **deliberate, dangerous escape hatch**. It breaks encapsulation by design — which is exactly what libraries like Jackson and Hibernate rely on, and exactly what security sandboxes try to prevent. Since Java 9, the **module system** added another layer: even with `setAccessible(true)`, you cannot reflectively access a member of a class in another module **unless that module exports (and, for deep reflection, `opens`) the package to you**. If the module refuses, you get `InaccessibleObjectException` (an unchecked exception):

```java
// In module "app", reflecting on a type in module "java.base" or a
// closed third-party module:
Field s = String.class.getDeclaredField("value");
s.setAccessible(true);   // throws InaccessibleObjectException in a modular app
```

In summary: `setAccessible` answers *"may the JVM ignore language-level visibility?"* while the module system answers *"may you even see this type/member across module boundaries at all?"*

## 3.2 Methods

### Inspecting methods

```java
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

class Calculator {
    public int add(int a, int b) { return a + b; }
    private double mul(double x, double y) { return x * y; }
    public static void describe() { /* ... */ }
}

public class MethodDemo {
    public static void main(String[] args) throws Exception {
        for (Method m : Calculator.class.getDeclaredMethods()) {
            System.out.printf("%s %s %s(%s)%n",
                Modifier.toString(m.getModifiers()),
                m.getReturnType().getSimpleName(),
                m.getName(),
                java.util.Arrays.toString(m.getParameterTypes()));
        }
    }
}
```

### Invoking methods reflectively

`Method.invoke(Object target, Object... args)` is the crux of reflective dispatch:

```java
import java.lang.reflect.Method;

public class InvokeDemo {
    public static void main(String[] args) throws Exception {
        Calculator calc = new Calculator();

        Method add = Calculator.class.getMethod("add", int.class, int.class);
        // getMethod("name", paramTypes...) — parameter types disambiguate overloads!
        Object result = add.invoke(calc, 3, 4);   // autoboxing handled for you
        System.out.println("3 + 4 = " + result);  // 7

        // private method via setAccessible
        Method mul = Calculator.class.getDeclaredMethod("mul", double.class, double.class);
        mul.setAccessible(true);
        System.out.println("2.5 * 4 = " + mul.invoke(calc, 2.5, 4.0)); // 10.0

        // static method: target instance is ignored (pass null)
        Method desc = Calculator.class.getMethod("describe");
        desc.invoke(null);
    }
}
```

**Expected output:**

```
3 + 4 = 7
2.5 * 4 = 10.0
```

### Edge cases that bite

- **Overloads:** `getMethod("add", int.class, int.class)` requires the exact parameter types. Omitting them makes the lookup ambiguous and throws `NoSuchMethodException`.
- **Wrapper vs. primitive types:** `getMethod("add", Integer.class, Integer.class)` will **not** find `add(int,int)`. Reflection does not perform autoboxing during lookup.
- **Varargs:** a method `void log(String... tags)` is reported with parameter type `String[]`. `isVarArgs()` on the `Method` tells you it was declared with varargs. When invoking, you pass a single `String[]` argument — the compiler's varargs array-building is *not* done for you.
- **Exceptions:** if the invoked method throws, `invoke` wraps it in `InvocationTargetException`. Call `getCause()` to see the real one:

```java
try {
    method.invoke(obj);
} catch (InvocationTargetException e) {
    Throwable real = e.getCause();   // the exception your method actually threw
}
```

- **Return types:** primitive results are returned boxed (`int` → `Integer`); `void` methods return `null`.

## 3.3 Constructors

`Class.getConstructors()` and `Class.getDeclaredConstructors()` return constructors; `getConstructor(Class<?>... paramTypes)` / `getDeclaredConstructor(...)` fetch a specific one. To create instances reflectively:

```java
import java.lang.reflect.Constructor;

public class ConstructorDemo {
    static class Point {
        private final int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }
        public String toString() { return "(" + x + ", " + y + ")"; }
    }

    public static void main(String[] args) throws Exception {
        Constructor<Point> ctor = Point.class.getConstructor(int.class, int.class);
        Point p = ctor.newInstance(10, 20);       // == new Point(10, 20)
        System.out.println(p);                    // (10, 20)

        // Even a private constructor can be reached:
        Constructor<Point> privateCtor = Point.class.getDeclaredConstructor();
        // (not shown here because Point has none — illustration only)
    }
}
```

> **API note.** `Class.newInstance()` (no-args) was **deprecated in Java 9** because it swallows the constructor's checked exceptions and only works for the no-arg constructor. Prefer `getDeclaredConstructor().newInstance()`.

## 3.4 Modifiers: the decoder ring

`Modifier.toString(int)` renders a bitmask into text, and `Modifier.isStatic(int)`-style predicates let you query bits. Here is the essential table:

| Constant | Java keyword | Meaning |
|---|---|---|
| `Modifier.PUBLIC` | `public` | Accessible everywhere |
| `Modifier.PROTECTED` | `protected` | Accessible in package + subclasses |
| `Modifier.PRIVATE` | `private` | Accessible only within the declaring class |
| `Modifier.STATIC` | `static` | Belongs to the class, not instances |
| `Modifier.FINAL` | `final` | Not overridable / not reassignable |
| `Modifier.ABSTRACT` | `abstract` | No body; must be implemented by subclass |
| `Modifier.SYNCHRONIZED` | `synchronized` | Locked during execution |
| `Modifier.VOLATILE` | `volatile` | Reads/writes are immediately visible across threads |
| `Modifier.TRANSIENT` | `transient` | Skipped by default serialization |
| `Modifier.NATIVE` | `native` | Implemented in non-Java (e.g., C) code |
| `Modifier.INTERFACE` | — | Is an interface type |
| `Modifier.STRICT` | `strictfp` | FP behavior per strict rules |

## 3.5 Complete runnable example: a reflective inspector

Here is the promised self-contained "inspector" that dumps any class's fields, methods, and constructors:

```java
import java.lang.reflect.*;
import java.util.Arrays;

/**
 * Dumps the reflection-level anatomy of any class whose name is passed
 * as a program argument. Illustrates getFields/getDeclaredFields,
 * getDeclaredMethods, and getDeclaredConstructors together.
 */
public class Inspector {

    public static void main(String[] args) throws Exception {
        Class<?> target = Class.forName(args.length > 0 ? args[0] : "java.util.Date");

        System.out.println("Inspecting: " + target.getName());

        System.out.println("\n== Constructors ==");
        for (Constructor<?> ctor : target.getDeclaredConstructors()) {
            System.out.println("  " + Modifier.toString(ctor.getModifiers())
                    + " " + target.getSimpleName()
                    + Arrays.toString(ctor.getParameterTypes()));
        }

        System.out.println("\n== Fields ==");
        for (Field f : target.getDeclaredFields()) {
            System.out.println("  " + Modifier.toString(f.getModifiers())
                    + " " + f.getType().getSimpleName() + " " + f.getName());
        }

        System.out.println("\n== Methods ==");
        for (Method m : target.getDeclaredMethods()) {
            System.out.println("  " + Modifier.toString(m.getModifiers())
                    + " " + m.getReturnType().getSimpleName()
                    + " " + m.getName() + Arrays.toString(m.getParameterTypes()));
        }
    }
}
```

**Expected output** (when run as `java Inspector java.lang.String` — abbreviated):

```
Inspecting: java.lang.String

== Constructors ==
  public java.lang.String(char[])
  public java.lang.String(char[], int, int)
  ...

== Fields ==
  private final byte[] value
  private final int coder
  private int hash
  ...

== Methods ==
  public boolean equals(java.lang.Object)
  public int hashCode()
  ...
```

Run it against your own classes too: `java Inspector com.your.Class`. This one tool is a miniature version of what IDEs and debuggers do thousands of times per second.

---

### 📌 Key Takeaways

- `get*()` vs `getDeclared*()` is the central distinction: public+inherited vs. everything-declared.
- `Field.get/set`, `Method.invoke`, and `Constructor.newInstance` are the three action primitives.
- `setAccessible(true)` disables language-level visibility checks; the module system may still refuse (`InaccessibleObjectException`).
- Lookup of overloads needs exact parameter types; varargs and autoboxing are *not* handled for you during lookup; invoked exceptions arrive wrapped in `InvocationTargetException`.
- Prefer `getDeclaredConstructor().newInstance()` over the deprecated `Class.newInstance()`.

### ⚡ Quick Check

**Question:** Why does `getMethod("add", int.class, int.class)` throw `NoSuchMethodException` if you instead pass `Integer.class`? Give the one-line reason.

<details>
<summary>Answer</summary>

Reflection performs **exact** parameter-type matching during lookup; `int` and `Integer` are distinct types as far as reflection is concerned, and no autoboxing/unboxing is applied during method lookup (autoboxing is a compile-time convenience only).

</details>

---

# 4. Working with Annotations

Annotations are *metadata attached to code*. But metadata is only useful if something can read it — and at runtime, that something is reflection.

## 4.1 Retention: the visibility policy

An annotation declared in source has three possible fates, controlled by `@Retention`:

| `RetentionPolicy` | Scope | Can reflection see it at runtime? |
|---|---|---|
| `SOURCE` | Discarded after compilation | **No** — gone from bytecode (e.g., `@Override` never appears in `.class` files as runtime data) |
| `CLASS` | Written to `.class` file, but not loaded into the JVM's runtime reflection tables | **No** — invisible at runtime (default) |
| `RUNTIME` | Written to `.class` and retained by the JVM | **Yes** — readable via `getAnnotation()` |

**Misconception alert.** "Annotations are always visible at runtime." Only `@Retention(RUNTIME)` annotations are. This is why you must annotate *your* custom annotations with `@Retention(RetentionPolicy.RUNTIME)` before a reflective validator can see them — a failure mode that causes baffling `null` results.

## 4.2 `@Target` and `@Inherited`

- **`@Target`** restricts where an annotation may be applied: `ElementType.FIELD`, `ElementType.METHOD`, `ElementType.TYPE` (class/interface/enum), `ElementType.PARAMETER`, `ElementType.CONSTRUCTOR`, and more (including `RECORD_COMPONENT` for records, Java 16+).
- **`@Inherited`** marks an annotation so that when it appears on a *superclass*, subclasses report it too — but only for **class-level** annotations, and only via `getAnnotation`/`getAnnotations` (not `getDeclaredAnnotations`). Interface annotations are never inherited this way.

## 4.3 The reading API

All on `AnnotatedElement` (implemented by `Class`, `Method`, `Field`, `Constructor`, packages, and parameters):

| Method | Returns |
|---|---|
| `getAnnotation(Class<A>)` | The annotation instance, or `null` if absent |
| `getAnnotations()` | All annotations present (including inherited, where applicable) |
| `getDeclaredAnnotations()` | Only those directly declared on this element |
| `isAnnotationPresent(Class<A>)` | `true`/`false` — semantically same as `getAnnotation != null` |
| `getAnnotationsByType(Class<A>)` | Supports `@Repeatable` annotations — returns all repeats |

Annotation *members* are read like interface methods on the returned annotation instance:

```java
MyAnn a = element.getAnnotation(MyAnn.class);
String value = a.value();   // reads member "value"
```

## 4.4 Complete runnable example: a `@NotNull` validator

We define a runtime-visible annotation, apply it to fields of a domain class, then write a reflective validator that checks annotated fields for `null`:

```java
import java.lang.annotation.*;
import java.lang.reflect.*;

// 1. Define the annotation — RUNTIME retention is non-negotiable
@Retention(RetentionPolicy.RUNTIME)          // visible to reflection
@Target(ElementType.FIELD)                   // only on fields
public @interface NotNull {
    String message() default "must not be null";
}

// 2. A domain class that uses it
class User {
    @NotNull private String name;            // required
    private String nickname;                 // optional — no annotation

    User(String name, String nickname) {
        this.name = name; this.nickname = nickname;
    }
}

// 3. The reflective validator
public class Validator {
    public static void validate(Object target) throws IllegalAccessException {
        Class<?> clazz = target.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            NotNull nn = field.getAnnotation(NotNull.class);   // read annotation
            if (nn == null) continue;                          // not annotated

            field.setAccessible(true);
            Object value = field.get(target);
            if (value == null) {
                System.out.printf("VALIDATION FAILED on %s.%s: %s%n",
                    clazz.getSimpleName(), field.getName(), nn.message());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        validate(new User("Ada", "countess"));   // OK — name set
        validate(new User(null, "ade"));         // FAIL — name is null
    }
}
```

**Expected output:**

```
VALIDATION FAILED on User.name: must not be null
```

Notice how compact the *generic* logic is: one method validates *any* class, with zero knowledge of `User`. That generality — "write once, apply to everything" — is the superpower that annotation + reflection combinations give frameworks.

## 4.5 Richer example: annotation member values drive serialization

Members aren't just flags — they carry configuration. Here a `@JsonField` annotation carries the JSON key name:

```java
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.LinkedHashMap;
import java.util.Map;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface JsonField {
    String key();                    // JSON name to use
}

class Product {
    @JsonField(key = "product_id") private final int id;
    @JsonField(key = "product_name") private final String name;
    private transient String secret; // ignored by our serializer

    Product(int id, String name) { this.id = id; this.name = name; }
}

public class TinyJson {
    static String toJson(Object target) throws IllegalAccessException {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Field f : target.getClass().getDeclaredFields()) {
            JsonField jf = f.getAnnotation(JsonField.class);
            if (jf == null) continue;          // not annotated -> not serialized
            f.setAccessible(true);
            out.put(jf.key(), f.get(target));  // member value drives the key
        }
        StringBuilder sb = new StringBuilder("{");
        out.forEach((k, v) ->
            sb.append("\"").append(k).append("\":\"").append(v).append("\","));
        sb.setCharAt(sb.length() - 1, '}');    // trim trailing comma
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        System.out.println(TinyJson.toJson(new Product(7, "Espresso Maker")));
    }
}
```

**Expected output:**

```
{"product_id":"7","product_name":"Espresso Maker"}
```

This ~40-line toy is the essence of libraries like Gson and Jackson, and of Hibernate's `@Column(name = "...")` mapping.

## 4.6 Compile-time vs. runtime processing

There are two eras in which annotations are consumed:

- **Compile-time (annotation processors).** Tools like `javac`'s `-processor` flag and libraries such as **Dagger**, **AutoService**, and **Project Lombok** run *while compiling*. They can *generate code* (`javax.annotation.processing.AbstractProcessor` + `javax.lang.model`). They see `SOURCE` and `CLASS` retention annotations — because they read the source/class files directly, before the JVM runs. This is how Dagger generates DI wiring at build time, giving type safety and near-zero runtime overhead that pure runtime reflection cannot match.
- **Runtime (reflection).** This chapter's focus: annotations survive into the running JVM only with `RUNTIME` retention, and are read through `java.lang.reflect`.

A rule of thumb used by modern engineering: **prefer compile-time processing when you can; fall back to runtime reflection when you must read metadata about arbitrary, dynamically loaded types.**

---

### 📌 Key Takeaways

- Retention decides everything: only `RetentionPolicy.RUNTIME` annotations are visible to reflection.
- `@Target` restricts placement; `@Inherited` propagates class-level annotations to subclasses (interfaces never inherit).
- Read annotations with `getAnnotation`, `getAnnotations`, `getDeclaredAnnotations`, `isAnnotationPresent`; read members like interface methods.
- Annotation + reflection = "write validation/serialization logic once, apply to any class."
- Compile-time annotation processors (Dagger, Lombok) are a separate, more efficient alternative to runtime reflection.

### ⚡ Quick Check

**Question:** You forget `@Retention(RetentionPolicy.RUNTIME)` on your custom annotation. `getAnnotation` returns `null` even though you clearly annotated a field. Why, and how would you diagnose it?

<details>
<summary>Answer</summary>

Without `RUNTIME` retention, the annotation defaults to `CLASS` retention: it is written into the `.class` file but never installed into the JVM's runtime reflection data, so `getAnnotation` has nothing to find. Diagnosis: check the annotation's declaration for `@Retention`; if absent or `SOURCE`/`CLASS`, either add `@Retention(RetentionPolicy.RUNTIME)` (for runtime reflection) or switch to a compile-time annotation processor.

</details>

---

# 5. Real-World Use Cases

Frameworks don't use reflection for fun — they use it because it is the only way to provide generic behavior over types they have never compiled against. Here are six canonical uses, each tied back to the APIs from §2–§4.

## 5.1 Dependency injection (Spring, Guice)

**The problem:** a framework must instantiate classes it has never seen, wire their dependencies (often discovered by type), and inject values — all from configuration.

**How it works:** Spring's `Class.forName`-style lookups (or component scanning over packages) obtain `Class` objects; constructors or fields are located via `getDeclaredConstructor` / `getDeclaredField`; `setAccessible(true)` + `set(...)` inject `@Autowired` fields; and singleton scope is cached per class.

**Minimal sketch:**

```java
// Framework side (simplified)
for (Class<?> bean : scannedClasses) {
    for (Field f : bean.getDeclaredFields()) {
        if (f.isAnnotationPresent(Autowired.class)) {
            f.setAccessible(true);
            f.set(instance, container.lookup(f.getType()));   // inject by type
        }
    }
}
```

**Reflection features used:** `Class.forName`/scanning (§2), `Field`, `Constructor` (§3), `@Autowired` via `getAnnotation` (§4).

## 5.2 Object-relational mapping (Hibernate / JPA)

**The problem:** map Java classes to database tables without hand-written SQL per class.

**How it works:** Hibernate reads `@Entity`, `@Table`, `@Column`, and `@Id` annotations to derive the table/column names, then reflectively instantiates entity objects (via no-arg constructors), sets fields from result-set rows (`field.set(...)`), and reads field values when building `INSERT`/`UPDATE` statements. Lazy-loading proxies are produced via dynamic proxies or bytecode enhancement.

**Minimal sketch:**

```java
Column col = field.getAnnotation(Column.class);
String columnName = (col != null && !col.name().isEmpty())
        ? col.name() : field.getName();               // annotation-driven mapping
resultSet.getObject(i);
field.setAccessible(true);
field.set(entity, resultSet.getObject(i));            // row -> object
```

**Reflection features used:** field inspection + `setAccessible` (§3), `@Column`/`@Id`/`@Entity` reading (§4), no-arg `Constructor` (§3).

## 5.3 Serialization / JSON libraries (Jackson, Gson)

**The problem:** convert arbitrary objects to JSON and back, including private fields and classes with no default constructor.

**How it works:** Jackson walks `getDeclaredFields()` and bean-style getters, reading `@JsonProperty`/`@JsonIgnore` annotations to control naming and inclusion. Deserialization invokes constructors (`getDeclaredConstructor().newInstance()`) and then `field.set(...)`. Our `TinyJson` in §4.5 is a working miniature.

**Reflection features used:** `Field`, `Constructor`, `Method.invoke` for getters (§3), `@JsonProperty` style annotations (§4).

## 5.4 Testing frameworks (JUnit)

**The problem:** a test runner must discover `@Test` methods, invoke them, and report pass/fail — for any test class written by any developer.

**How it works:** JUnit scans classes for methods annotated `@Test`, instantiates the class via its no-arg constructor, invokes `@BeforeEach`/`@Test`/`@AfterEach` in order via `Method.invoke`, and wraps failures in `InvocationTargetException` to extract the underlying assertion error.

**Minimal sketch:**

```java
for (Method m : testClass.getDeclaredMethods()) {
    if (m.isAnnotationPresent(Test.class)) {
        Object instance = testClass.getDeclaredConstructor().newInstance();
        try {
            m.invoke(instance);                    // run the test
            System.out.println("PASS " + m.getName());
        } catch (InvocationTargetException e) {
            System.out.println("FAIL " + m.getName() + ": " + e.getCause());
        }
    }
}
```

**Reflection features used:** `Class` acquisition (§2), `Method.invoke` + `InvocationTargetException` handling (§3), `@Test` reading (§4).

## 5.5 Debuggers, profilers, and IDE tooling

**The problem:** IDEs and debuggers must display arbitrary objects, evaluate expressions, and show stack traces for classes they never imported.

**How it works:** The debugger uses the JVM Tool Interface (JVMTI) plus reflection-style metadata to render object graphs (the "Variables" pane in IntelliJ/Eclipse literally walks fields via reflection), while profilers sample stack frames and inspect instances. IDE autocomplete is built on compiled class metadata of the same kind reflection exposes at runtime. `Object.toString` default implementations and debugging aids in frameworks also use `getClass().getSimpleName()` (§2).

**Reflection features used:** `getClass()`/class literals (§2), `Field.get` on arbitrary objects (§3).

## 5.6 Dynamic proxies and plug-in / service discovery

**The problem (proxies):** intercept every method call on an interface — for logging, transactions, or remoting — without writing an implementation per interface.

**How it works:** `java.lang.reflect.Proxy` synthesizes, at runtime, a class implementing the given interfaces; every method call is forwarded to your `InvocationHandler`. This is how Spring AOP implements `@Transactional` and how Hibernate creates lazy-loading collections.

**Minimal sketch:**

```java
import java.lang.reflect.*;

interface Greeter { String greet(String name); }

public class ProxyDemo {
    public static void main(String[] args) {
        Greeter proxy = (Greeter) Proxy.newProxyInstance(
            Greeter.class.getClassLoader(),
            new Class<?>[] { Greeter.class },
            (Object self, Method m, Object[] a) -> {          // InvocationHandler
                System.out.println("[intercept] " + m.getName());
                return "Hello, " + a[0] + "!";                // fabricate result
            });

        System.out.println(proxy.greet("world"));
    }
}
```

**Expected output:**

```
[intercept] greet
Hello, world!
```

**The problem (service discovery):** load a plugin's implementation purely by name.

**How it works:** `java.util.ServiceLoader` (itself reflection-based under the hood) locates implementations declared in `META-INF/services`, loads them via `Class.forName`-style machinery, and instantiates them — the backbone of JDBC driver discovery and many plug-in architectures.

**Minimal sketch:**

```java
ServiceLoader<Plugin> plugins = ServiceLoader.load(Plugin.class);
for (Plugin p : plugins) p.execute();   // each implementation loaded reflectively
```

**Reflection features used:** `Proxy.newProxyInstance` with `Method.invoke` in the handler (§3), `Class` loading by name (§2), constructor invocation (§3).

---

### 📌 Key Takeaways

- Every framework use case reduces to three verbs: **find a `Class` (§2)**, **inspect/read/invoke members (§3)**, **read annotations (§4)**.
- The value proposition of reflection is *generic code over unknown types* — the ability to write one engine that serves every class.
- `Proxy` + `InvocationHandler` is reflection's cleanest modern extension: intercepting interfaces without implementing them.
- `ServiceLoader` formalizes "discover implementation by name" as a standard platform mechanism.

---

# 6. Risks, Performance, and Best Practices

## 6.1 Performance: why reflective calls are slow

A direct call `obj.add(3, 4)` is a near-free JIT-compiled dispatch. A reflective `method.invoke(obj, 3, 4)` pays several costs:

1. **Dynamic dispatch through an interpreter path:** `Method.invoke` performs access checks, argument boxing/unboxing, and array assembly (`Object[]` allocation per call).
2. **No inlining / constant folding:** the JIT cannot see through the reflectively-chosen target, so classic optimizations (inlining, escape analysis, scalar replacement) are crippled or impossible.
3. **Deoptimization risk:** if the JIT *does* speculate, encountering reflective calls that break assumptions can trigger deoptimization of surrounding code.
4. **Boxing:** primitives are boxed/unboxed on every invocation.

**Measured reality:** reflective invocation is typically **tens to hundreds of times slower** than a direct call for the first calls; warmed-up with the JIT's "reflection inlining" (for known call sites) it improves, but still loses to direct calls by a solid margin, and per-call allocation adds GC pressure.

### Mitigations

- **Cache the `Method`/`Field`/`Constructor` handles.** Resolving metadata is expensive; *invoking* a cached handle is much cheaper. Never call `getDeclaredField` inside a hot loop:

```java
// Bad: resolve every time
//   field = clazz.getDeclaredField("id"); field.get(o);

// Good: resolve once, reuse
private static final Map<String, Field> CACHE = new ConcurrentHashMap<>();
static Field fieldOf(Class<?> c, String name) {
    return CACHE.computeIfAbsent(name, n -> {
        try { Field f = c.getDeclaredField(n); f.setAccessible(true); return f; }
        catch (NoSuchFieldException e) { throw new RuntimeException(e); }
    });
}
```

- **`java.lang.invoke` MethodHandles:** a `MethodHandle` is a typed, compilable, directly-callable reference to a method that the JIT can often inline — significantly faster than `Method.invoke`:

```java
import java.lang.invoke.*;

MethodHandles.Lookup lookup = MethodHandles.lookup();
MethodHandle add = lookup.findVirtual(Calculator.class, "add",
        MethodType.methodType(int.class, int.class, int.class));
int result = (int) add.invokeExact(calc, 3, 4);   // near-direct-call speed
```

- **`invokedynamic`:** the `indy` bytecode instruction (behind lambdas and string concatenation) lets the JVM perform linkage at runtime with full JIT visibility. Libraries like Jackson and Gson leverage generated accessors via `MethodHandles`/`indy` to avoid per-call reflection overhead.
- **Reflection-free alternatives:** compile-time code generation (annotation processors, Dagger), bytecode generation at startup (Hibernate proxies), or `record`-based data carriers (whose accessors are direct).

## 6.2 Security & encapsulation

Reflection is a loaded gun:

- **Breaking encapsulation is the point — and the danger.** `setAccessible(true)` can reach into `java.base` internals. In a modular app, the module system blocks this (`InaccessibleObjectException`) — *deliberately*, to protect the JDK's own internals.
- **Security managers (legacy):** pre-module Java used `SecurityManager` to veto `setAccessible` via `ReflectPermission("suppressAccessChecks")`. Though deprecated for removal in modern JDKs, the principle remains: in untrusted environments, reflective access must be gated.
- **Sandboxing:** reflective calls can bypass language-level checks but still run in-process; they cannot escape the JVM's type system (no memory corruption). The threat model is about *data*: a hostile plugin can read fields you marked `private`.
- **Best-practice stance:** treat `setAccessible(true)` as an *unavoidable library necessity* (Jackson, Hibernate need it), not a routine tool. Prefer public APIs, bean accessors, and interface contracts first; use reflection as the last resort for cross-cutting concerns.

## 6.3 Maintainability: refactoring hazards

- **Stringly-typed references rot silently.** `clazz.getDeclaredField("userName")` compiles today and throws `NoSuchFieldException` next week when someone renames `userName` → `username`. The compiler cannot help you; your test suite must.
- **Mitigations:** centralize reflective names in constants; use `Class`-typed lookups where possible; and prefer *interface contracts* + `Proxy`/method references over name-based lookup.
- **Type erasure is invisible to reflection:** a `List<String>` field reports its type as `List` (raw), not `List<String>`. If you need element types at runtime, you must capture them explicitly (the "type token" pattern — e.g., `TypeReference` in Jackson).
- **`private` fields you expose reflectively are a *de facto* API.** Other code may depend on them; refactoring them breaks more than the compiler tells you.

## 6.4 Do / Don't table

| ✅ Do | ❌ Don't |
|---|---|
| Cache `Field`/`Method`/`Constructor` handles | Call `getDeclared*` inside hot loops |
| Prefer `MethodHandles`/`invokedynamic` for performance-critical paths | Use `Method.invoke` where a direct call or lambda suffices |
| Use `RUNTIME` retention deliberately on annotations you must read at runtime | Assume every annotation is visible to reflection (check retention) |
| Centralize reflective member names as constants | Embed raw string names in scattered code |
| Handle `NoSuchField/MethodException` and `InvocationTargetException` explicitly | Swallow exceptions with `catch (Exception e) { }` |
| Check module access (`getModule().isExported(...)`) and handle `InaccessibleObjectException` | Rely on `setAccessible(true)` in a modular or sandboxed deployment without testing |
| Prefer public APIs / interfaces / `ServiceLoader` for extension points | Reflect into `java.base` or third-party internals in production |
| Use `getDeclaredConstructor().newInstance()` | Use deprecated `Class.newInstance()` |

---

### 📌 Key Takeaways

- Reflective calls are slower (no inlining, boxing, access checks, dynamic dispatch); cache handles and prefer `MethodHandles`/`invokedynamic` when performance matters.
- `setAccessible(true)` breaks encapsulation on purpose; the module system exists precisely to constrain it — respect it and handle `InaccessibleObjectException`.
- Reflective string lookups are refactoring hazards; centralize names, prefer typed contracts, and never treat `private` fields reached reflectively as "not public API."
- The general principle: **reflection is the most flexible tool and the least safe one — use it at the boundaries of your system, not in its core.**

### ⚡ Quick Check

**Question:** You must invoke a `getter()` on 10,000 objects per second, where the class is only known at runtime. Name two techniques to make this fast and safe, and explain why the naive `method.invoke` inside the loop is a poor choice.

<details>
<summary>Answer</summary>

1. **Cache the `Method` (or better, a `MethodHandle`)** resolved once, outside the loop — avoiding repeated metadata lookup; a cached `MethodHandle` is typically the fastest reflective path because the JIT can inline it.
2. **Use `MethodHandles.Lookup.findVirtual` + `invokeExact`** for near-direct-call performance, or even **generate a lambda/accessor** at startup (as Jackson/Hibernate do) to eliminate reflection entirely.

The naive approach is poor because each `Method.invoke` performs access checks, argument boxing/allocation of the `Object[]` varargs array, and prevents inlining — leading to order-of-magnitude slowdowns plus GC churn, repeated on every one of the 10,000 iterations.

</details>

---

# 7. Chapter Summary

Reflection is the JVM's answer to one profound question: *can a program learn about and manipulate types it was never compiled against?* The answer is yes, through three layers:

1. **`Class` objects** (§2) — the runtime representation of every type, obtained via `getClass()`, `.class`, or `Class.forName(...)`, enabled by the JVM's class-loading machinery.
2. **`java.lang.reflect`** (§3) — `Field`, `Method`, `Constructor`, and `AccessibleObject` give you inspection, value access, invocation, and (with `setAccessible(true)`) access to private members.
3. **Annotations** (§4) — with `RUNTIME` retention, reflection reads the metadata that turns generic engines into tailored behavior.

Those three layers are the engine behind Spring, Hibernate, Jackson, JUnit, debuggers, proxies, and `ServiceLoader` (§5). They are powerful and costly: slow without caching and method handles, invasive without the module system, and fragile without discipline (§6). The expert's rule: **use reflection to build generic boundaries; never to work around design.**

## Concept-to-API mapping

| Concept | Key classes / methods | One-line description |
|---|---|---|
| Runtime type of an object | `Object.getClass()` | Returns the `Class` of the actual runtime type |
| Compile-time class reference | `ClassName.class` | Type-safe class literal, does not initialize |
| Load a class by name | `Class.forName(String)` | Loads (and by default initializes) a class from a string |
| Generic type metadata | `Class<?>`, `Type`, `GenericType` | Represents a type at runtime; `TypeReference` pattern captures generic args |
| List members | `getFields` / `getDeclaredFields` · `getMethods` / `getDeclaredMethods` · `getConstructors` / `getDeclaredConstructors` | Enumerate public (+inherited) or all-declared members |
| Read/write a field | `Field.get(o)` / `Field.set(o, v)` | Access a field's value (pass `null` for static) |
| Invoke a method | `Method.invoke(target, args)` | Call a method reflectively; wraps thrown exceptions in `InvocationTargetException` |
| Create an instance | `Constructor.newInstance(args)` | Reflectively construct objects (preferred over deprecated `Class.newInstance()`) |
| Bypass visibility | `AccessibleObject.setAccessible(true)` | Disables language-level access checks (module system still applies) |
| Modifiers | `Modifier.toString`, `Modifier.isStatic`, … | Decode/query the modifier bitmask |
| Read annotations | `AnnotatedElement.getAnnotation`, `getAnnotations`, `isAnnotationPresent` | Retrieve runtime-retained annotations from classes/members |
| Control annotation lifetime | `@Retention(RUNTIME)` | The only retention that makes annotations visible to reflection |
| Fast reflective calls | `java.lang.invoke.MethodHandles.Lookup`, `MethodHandle` | Compilable, inlineable method references |
| Intercept interface calls | `Proxy.newProxyInstance` + `InvocationHandler` | Runtime-generated implementations of interfaces |
| Discover implementations | `ServiceLoader.load(...)` | Standard plug-in/service discovery, reflection-backed |

## Progressive exercises

### Exercise 1 — Recall (warm-up)

**Question.** List the three ways to obtain a `Class` object and give one situation in which each is the only reasonable choice. Why is `Class.forName` the odd one out regarding exceptions and initialization?

<details>
<summary>Model answer</summary>

`obj.getClass()` — when you have an instance and need its *runtime* type; `ClassName.class` — when the type is known statically and you want type safety; `Class.forName("...")` — when the name exists only as a string (config/plugin). `forName` is the odd one out because it throws the checked `ClassNotFoundException` (no compile-time guarantee the class exists) and initializes the class by default (side effects), controllable via the 3-arg overload.

</details>

### Exercise 2 — Code writing

**Question.** Write a method `printAllAnnotations(Class<?> c)` that prints every annotation (name and member values) directly declared on `c` and on each of its declared fields and methods. Use `getDeclaredAnnotations()` and `annotationType()`.

<details>
<summary>Model solution (hint)</summary>

```java
import java.lang.reflect.*;
import java.util.Arrays;

public static void printAllAnnotations(Class<?> c) {
    for (Annotation a : c.getDeclaredAnnotations())
        System.out.println(c.getName() + " @" + a.annotationType().getSimpleName());
    for (Field f : c.getDeclaredFields())
        for (Annotation a : f.getDeclaredAnnotations())
            System.out.println(f.getName() + " @" + a.annotationType().getSimpleName());
    for (Method m : c.getDeclaredMethods())
        for (Annotation a : m.getDeclaredAnnotations())
            System.out.println(m.getName() + " @" + a.annotationType().getSimpleName());
}
```

Hint: to print member values, iterate `annotationType().getDeclaredMethods()` and invoke each on `a`.

</details>

### Exercise 3 — Design / critical thinking

**Question.** A colleague wants to replace every `instanceof` check in a large system with `clazz.isAssignableFrom(...)` via reflection. Argue for or against, referencing at least two of the concerns from §6.

<details>
<summary>Discussion points</summary>

Against: (1) **Performance** — `isAssignableFrom` is a runtime metadata walk; the JIT compiles `instanceof` to a few instructions, so the reflective form is slower and defeats compile-time checking. (2) **Maintainability** — stringly or `Class`-typed logic loses compiler-verified relationships, making renames/refactors silently wrong. (3) **Design** — `instanceof` expresses a static type relationship; reflection signals a *runtime* decision, usually masking a missing interface or a `switch`/polymorphism design. Exception where justified: plugin systems where the candidate type is genuinely only known at runtime (then cache the result).

</details>

### Exercise 4 — Build a mini-framework (challenge)

**Challenge.** Build `@Route(path)`, a runtime annotation for methods, plus a tiny router:

- Define `@Route` with `@Retention(RUNTIME)` and `String path()`.
- Scan a package's classes (use `Class.forName` on a fixed list of class names for simplicity) for methods annotated `@Route`.
- For an incoming request string like `"/users/42"`, match a route whose `path()` equals the request and invoke the method on a fresh instance, passing the id extracted from the path.

<details>
<summary>Design hint & model sketch</summary>

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Route { String path(); }

class UserController {
    @Route(path = "/users/{id}")
    public String getUser(String id) { return "user " + id; }
}

public class MiniRouter {
    public static void main(String[] args) throws Exception {
        for (String cn : new String[]{"UserController"}) {
            for (Method m : Class.forName(cn).getDeclaredMethods()) {
                Route r = m.getAnnotation(Route.class);
                if (r != null && r.path().equals("/users/{id}")) {
                    Object controller = Class.forName(cn).getDeclaredConstructor().newInstance();
                    String id = "42";   // extracted from the request
                    System.out.println(m.invoke(controller, id));
                }
            }
        }
    }
}
```

Hints: match patterns by splitting on `/`; keep a `Map<path, Method>` cache; use `MethodHandles` for the invocation to make the router production-ish.

</details>

---

*End of chapter. Reflection rewards careful readers: it grants your code the same power your IDE has — the power to understand software it was never told about. Use it at the boundaries, cache what you resolve, and let the module system do its protective work.*