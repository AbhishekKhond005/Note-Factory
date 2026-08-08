# Generics in Java: Type-Safe Code Reuse

Generics are one of the most important features added to the Java language (Java 5, JSR 14). They let you write classes, interfaces, and methods that operate on **types as parameters**, so a single piece of code can work with `String`, `Integer`, your own `Customer` class, or anything else — all while catching type mistakes at **compile time** rather than letting them explode at run time. In this chapter you will learn how to declare and use generic classes and methods, how to constrain type parameters with bounds, how wildcards express "any type," and — crucially — what the compiler really does with generics through type erasure. By the end, you will understand not just *how* to use generics, but *why* they work the way they do, which is what separates a competent Java programmer from a great one.

---

## 1. Learning Objectives

- **Define** type parameters, type arguments, generic classes, generic methods, and type erasure, and explain how they differ from one another.
- **Implement** generic classes and interfaces with one or more type parameters, using the diamond operator for concise instantiation.
- **Write** generic methods with inferred type arguments and distinguish them from generic classes.
- **Apply** bounded type parameters (`<T extends ...>`) to restrict types and enable safe method calls such as `compareTo` and `doubleValue`.
- **Compare** and **select** among unbounded, upper-bounded, and lower-bounded wildcards using the PECS ("Producer Extends, Consumer Super") rule.
- **Evaluate** the consequences of type erasure, including its restrictions on primitives, arrays, `instanceof`, and generic exceptions.

---

## 2. Prerequisites

Before studying generics, you should be comfortable with:

- **Java classes and objects** — how to declare fields, methods, constructors, and `main`.
- **Inheritance and polymorphism** — subtype relationships, `extends`/`implements`, and dynamic dispatch.
- **Object types and casting** — the fact that every reference type is an `Object`, and what an explicit cast such as `(String) obj` does (and when it fails).
- **The collections framework basics** — `List`, `ArrayList`, and `Map`, at least at an introductory level.
- **`final` and immutability concepts** — helpful for reading clean generic code, though not strictly required.

If you can answer "what does `Object obj = "hello"; String s = (String) obj;` do, and when does it throw `ClassCastException`?", you are ready.

---

## 3. Main Content

### 3.1 Introduction to Generics

#### Definition

**Generics** are a language feature that enables *types* (classes and interfaces) to be **parameters** when defining classes, interfaces, and methods. A generic declaration — like `class Box<T>` — does not fix the type of data it stores; the *user* of the class supplies the actual type when they instantiate it. The result is code that is **type-safe** (the compiler verifies types) and **reusable** (one implementation serves many types), with **no explicit casts** required at the call site.

#### Analogy

Think of a **parking garage** built with numbered spaces but no reserved make or model. It can hold a sedan today and a motorcycle tomorrow; the *shape* of the garage (the "class") is fixed, while the *tenant* (the "type") changes. When generics arrive, the garage becomes a "car garage" or a "truck garage" — the same blueprint, but the gatekeeper now refuses vehicles that do not match the sign at the entrance. The sign is the **type argument**.

#### The Problem: `Object`-Based Containers

Before generics, a reusable container stored `Object`, which forced the caller to cast on the way out and gave the compiler no chance to verify correctness:

```java
public class ObjectBox {
    private Object value;

    public void set(Object value) {
        this.value = value;
    }

    public Object get() {
        return value;
    }

    public static void main(String[] args) {
        ObjectBox box = new ObjectBox();
        box.set("hello");

        // Explicit cast required, and the compiler cannot verify it.
        String text = (String) box.get();
        System.out.println(text);

        // Nothing stops us from storing something unexpected...
        box.set(42);

        // ...which compiles fine but fails at run time:
        String boom = (String) box.get();   // throws ClassCastException!
    }
}
```

**Line-by-line:** `set` accepts any `Object` (so `set(42)` is legal via autoboxing). `get` returns `Object`, forcing the caller to cast. The last three lines show the classic failure mode: the code *compiles* because the compiler cannot know the box holds an `Integer` when we ask for a `String` — the failure only surfaces as a `ClassCastException` at run time.

#### The Solution: A Generic `Box<T>`

```java
public class Box<T> {
    private T value;              // "T" is a placeholder for a real type

    public void set(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public static void main(String[] args) {
        Box<String> stringBox = new Box<>();
        stringBox.set("hello");
        String text = stringBox.get();        // no cast needed!
        System.out.println(text);

        Box<Integer> intBox = new Box<>();
        intBox.set(42);                        // autoboxing: int -> Integer
        int number = intBox.get();             // auto-unboxing: Integer -> int
        System.out.println(number);

        // stringBox.set(42);                  // compile error: incompatible types
    }
}
```

**Line-by-line:** `class Box<T>` declares the **type parameter** `T`. Inside the class, `T` is used exactly like a real type for the field, the `set` parameter, and the `get` return type. `Box<String>` supplies a **type argument**, replacing `T` with `String`. Because `stringBox.get()` is known to return `String`, no cast is needed, and `stringBox.set(42)` is rejected by the compiler. The `<>` is the **diamond operator** (Java 7+), letting the compiler infer the type argument from the variable's declared type.

**Naming convention:** type parameters use a single uppercase letter: `T` (Type), `E` (Element, used in collections), `K` and `V` (Key and Value, used in maps), and `N` (Number) or `R` (Result) where appropriate.

> **Why it matters:** Generics move type errors from *run time* to *compile time*. A program that does not compile with a clear error message is far cheaper to fix than a program that crashes in production with `ClassCastException` days after deployment.

**Common Pitfalls:** Forgetting the type argument and using a **raw type** (`Box box = new Box();`) silently reverts to the old `Object` behavior with compiler warnings; never use raw types in new code. Also remember that **primitives are not allowed** as type arguments — `Box<int>` is illegal; use the wrapper `Box<Integer>`.

---

### 3.2 Generic Classes

#### Definition

A **generic class** is a class that declares one or more type parameters after its name: `class Box<T>`, `interface List<E>`, `class HashMap<K, V>`. The class body may use those parameters wherever a type is expected — fields, method signatures, local variables, and casts that are safe.

#### Analogy

A **thermos** is "a container that keeps beverages hot" — it does not say *which* beverage. The shape (capacity, lid) is fixed; the content (coffee, tea, soup) is chosen by the owner. A generic class is exactly that: the structure is fixed, the payload type is the customer's choice.

#### Instantiation and the Diamond Operator

```java
public class Pair<K, V> {
    private final K key;
    private final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public static void main(String[] args) {
        // Diamond operator: the compiler infers the type arguments.
        Pair<String, Integer> age = new Pair<>("Alice", 30);
        String name = age.getKey();
        int years = age.getValue();

        Pair<Integer, String> pair = new Pair<>(1, "one");
        System.out.println(name + " is " + years);
        System.out.println(pair.getKey() + " -> " + pair.getValue());
    }
}
```

**Line-by-line:** `class Pair<K, V>` demonstrates a class with **two type parameters** — `K` for the key, `V` for the value. Each instance can have *different* type arguments: `Pair<String, Integer>` and `Pair<Integer, String>` are distinct, incompatible types. `new Pair<>("Alice", 30)` uses the diamond: the compiler infers `K = String`, `V = Integer` from the constructor arguments. Thanks to the declared types, `age.getKey()` returns `String` and `age.getValue()` returns `Integer` without any casting.

#### Raw Types

A **raw type** is a generic class used without type arguments, e.g., `Box box = new Box();`. It exists only for backward compatibility with pre-Java-5 code. The compiler permits it but emits unchecked warnings, because the compiler can no longer guarantee type safety:

```java
Box raw = new Box();            // raw type — warning!
raw.set("hello");
raw.set(42);                    // silently accepted
Integer n = (Integer) raw.get(); // ClassCastException at run time
```

**Common Pitfalls:** Never mix raw types with parameterized types — the raw type "poisons" the type safety of everything it touches, and casts downstream can fail at run time. Prefer the diamond operator and always write the type argument when the target type cannot be inferred.

> **Why it matters:** Generic classes are the foundation of the entire Java collections framework. When you write `List<String>`, you are instantiating the generic interface `List<E>` with `E = String`; every method now speaks in terms of `String` and the compiler enforces it at every call site.

---

### 3.3 Generic Methods

#### Definition

A **generic method** declares its own type parameter, placed in angle brackets *before the return type*: `public static <T> T identity(T value)`. Unlike a generic class, the type parameter is scoped to that single method and is chosen fresh on every invocation. A generic method can live inside a generic class, a non-generic class, or a `static` context — even inside a generic class, a static generic method is fine because its type parameter belongs to the method, not the instance.

#### Analogy

A **copy machine** is a single machine that can copy any document handed to it. The machine does not care what kind of document arrives — it simply produces a faithful copy of whatever type it receives. The "type of document" is decided per job, exactly as a generic method's type argument is decided per call.

#### Example: Generic Methods in a Non-Generic Class

```java
public class GenericMethods {

    // Type parameter <T> comes before the return type.
    public static <T> T identity(T value) {
        return value;
    }

    public static <T> void printArray(T[] array) {
        for (T element : array) {
            System.out.print(element + " ");
        }
        System.out.println();
    }

    public static void main(String[] args) {
        // Explicit type argument (rarely needed):
        String s = GenericMethods.<String>identity("hello");

        // Type inference: the compiler derives T from the argument.
        Integer n = identity(42);

        System.out.println(s + " " + n);

        String[] words = {"generics", "are", "type", "safe"};
        Integer[] numbers = {3, 1, 4, 1, 5};
        printArray(words);     // T = String
        printArray(numbers);   // T = Integer
    }
}
```

**Line-by-line:** In `public static <T> T identity(T value)`, the `<T>` immediately before the return type declares the method's type parameter. `identity` accepts a value of type `T` and returns the same type — it can be called with a `String`, an `Integer`, anything, and each call receives its own inferred `T`. `printArray` shows a generic method returning `void`. Because `T[]` is used, the method works for arrays of any reference type. In `main`, type inference means we rarely need to spell out the type argument — `identity(42)` infers `T = Integer` from the argument, and the assignment target confirms it.

#### Generic Methods vs. Generic Classes

- A **generic class** fixes one set of type parameters for the whole instance; all its methods share them.
- A **generic method** has type parameters that exist only for the duration of that call; they are independent of the class and can even appear in static methods.

> **Why it matters:** Generic methods let you write reusable algorithms — sorting, searching, mapping, filtering — that work for *any* element type while remaining fully type-safe. The entire `java.util.Collections` utility class is essentially a collection of generic methods.

**Common Pitfalls:** The most frequent mistake is writing `public static <T> T ...` but forgetting the `<T>` before the return type — then the compiler treats `T` as an unknown class name and the code will not compile. Another is declaring a type parameter in a method where the class already declares one with the same name; the method's parameter *shadows* the class's, which is almost always a bug.

---

### 3.4 Bounded Type Parameters

#### Definition

A **bounded type parameter** restricts the set of type arguments a generic may accept by requiring them to be a subtype of a given type, using the keyword `extends`: `<T extends Number>` or `<T extends Comparable<T>>`. Multiple bounds are combined with `&`: `<T extends A & B>`. The bound's *only* purpose is to guarantee that whatever `T` actually is, it has the methods and fields of the bound type — which lets the body of the generic code call those methods safely.

#### Analogy

A **delivery truck for refrigerators** cannot take arbitrary parcels — the cargo must fit the truck's constraints. By requiring "all cargo fits through the door," the loading dock can safely use the standard dolly for *every* load. The constraint (the bound) enables a capability (loading) that would otherwise be impossible to guarantee.

#### Why Bounds Enable Method Calls

Without a bound, the compiler knows only that `T` is an `Object`, so it cannot call `compareTo` or `doubleValue`. With `<T extends Comparable<T>>`, the compiler knows that whatever `T` is, it has `compareTo`. With `<T extends Number>`, it knows `T` has `intValue()`, `doubleValue()`, and friends.

#### Example: Maximum of a Collection

```java
import java.util.List;

public class CollectionsMax {

    public static <T extends Comparable<T>> T max(List<T> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("empty list");
        }
        T result = list.get(0);
        for (T element : list) {
            if (element.compareTo(result) > 0) {
                result = element;
            }
        }
        return result;
    }

    public static void main(String[] args) {
        List<Integer> numbers = List.of(9, 2, 7, 4);
        System.out.println("max = " + max(numbers));     // max = 9

        List<String> words = List.of("kiwi", "apple", "mango");
        System.out.println("max = " + max(words));       // max = mango
    }
}
```

**Line-by-line:** `<T extends Comparable<T>>` declares that `T` must implement `Comparable<T>` — i.e., `T` can compare itself to other `T`s. Inside the method, `element.compareTo(result)` is legal *only* because of this bound; without it, `compareTo` would not exist on `T`. The same method serves `List<Integer>` and `List<String>` because both `Integer` and `String` implement `Comparable`. This is a simplified version of the real `java.util.Collections.max`.

#### Multiple Bounds

```java
abstract class Shape {
    abstract double area();
}

interface Drawable {
    void draw();
}

class Circle extends Shape implements Drawable {
    private final double radius;

    Circle(double radius) {
        this.radius = radius;
    }

    @Override
    double area() {
        return Math.PI * radius * radius;
    }

    @Override
    public void draw() {
        System.out.println("Drawing a circle");
    }
}

public class MultiBound {

    // T must be a Shape AND a Drawable. Class bound comes first, then interfaces.
    public static <T extends Shape & Drawable> void describe(T shape) {
        shape.draw();             // guaranteed by the Drawable bound
        System.out.printf("Area: %.2f%n", shape.area());  // guaranteed by the Shape bound
    }

    public static void main(String[] args) {
        describe(new Circle(2.0));
    }
}
```

**Line-by-line:** `<T extends Shape & Drawable>` demands that `T` be a subtype of the class `Shape` **and** implement the interface `Drawable`. Only with both bounds can the body call both `shape.draw()` (from `Drawable`) and `shape.area()` (from `Shape`). Note the **ordering rule**: at most *one* bound may be a class, and it must appear first; all additional bounds must be interfaces, joined with `&`. `Circle` satisfies both constraints, so `describe(new Circle(2.0))` compiles.

#### `extends` for Classes vs