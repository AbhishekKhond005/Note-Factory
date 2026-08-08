# Java, Explained: Five Confusions That Trip Up Every Learner

## Overview / "Why This Confuses People"

Polymorphism is the moment in a Java course when many students suddenly feel like the language is lying to them. You write `Parent p = new Child();`, call `p.greet()`, and something unexpected happens. You annotate a static method with `@Override` and the compiler refuses — yet an *identical-looking* method on the same class compiles fine. You know the object you created is a `Motorcycle`, but you can't call `wheelie()` on it. The root cause of nearly every one of these "gotchas" is a single, deeply-held misunderstanding: most beginners treat **classes** and **objects** as if they were the same thing, and they assume that *what you type* and *what actually happens* are always identical.

The reality is that Java deliberately separates three ideas that beginners tend to blur: **what you wrote** (the source code), **what type the reference variable declares** (the compile-time type), and **what kind of object actually sits in memory** (the runtime type). When these three line up — as they do in the simple examples you see first — the language feels predictable. When they diverge — as they do in inheritance hierarchies — the confusion begins.

By the end of these notes you will be able to: explain, in your own words and at the JVM level, why static methods *hide* instead of *override*; articulate why instance methods cannot exist without an object; predict which methods a variable can *call* versus which implementation will actually *run*; use runtime polymorphism to design extensible systems; and write overrides with covariant return types. More importantly, you will stop memorizing "exceptions to the rules" and start seeing one consistent set of rules underneath.

---

## Section 1: Why Static Methods Are Not Overridden

### The Conceptual Explanation

Here is the single most important sentence in this section: **overriding** is a *runtime* mechanism, and **static methods** are resolved at *compile time*. The two ideas live in different worlds and can never meet.

When you override an instance method, you are saying, "Objects of my class will respond to this message differently from objects of the parent class." The JVM cannot know which object it will be handed until the program runs, so it postpones the decision until runtime. A static method, by contrast, belongs to the *class itself* — it has no object attached to it at all. Because there is no object, there is nothing to postpone; the compiler can look at the reference type on the left side of the dot and decide, right then, exactly which class's method to call. The decision is *frozen into the bytecode* before your program ever executes.

When a child class declares a static method with the same signature as a parent, the child method does not replace the parent method. It **hides** it. The parent method still exists, fully intact, and which one you get depends entirely on the *type of the reference you used to call it*. This is the classic beginner trap, and the code below is the classic beginner surprise.

### The "Wrong" Example: What Beginners Write and Expect

```java
class Parent {
    public static void greet() {
        System.out.println("Hello from Parent");
    }
}

class Child extends Parent {
    // WRONG-EXAMPLE: This looks like an override, but it is a HIDE.
    // The word 'static' makes @Override impossible and resolution compile-time.
    public static void greet() {
        System.out.println("Hello from Child");
    }
}

public class StaticBindingDemo {
    public static void main(String[] args) {
        Parent p = new Child();   // reference type is Parent, object is Child
        Child c = new Child();    // reference type is Child, object is Child

        // Beginners expect BOTH lines to print "Hello from Child"
        // because both objects are actually Child objects.
        p.greet();
        c.greet();
    }
}
```

**Expected Output**

```text
Hello from Parent
Hello from Child
```

The first call, `p.greet()`, is resolved at compile time using the reference type `Parent` — the compiler never even looks at what object `p` points to, because static methods don't care. So you get "Hello from Parent," which shocks most students. The second call uses a `Child` reference, so the compiler binds it to `Child.greet()`. The object identity was irrelevant in both cases; only the reference type on the left of the dot mattered.

> "But I wrote `@Override` and it compiled — doesn't that mean I overrode it?"
>
> No — and here is the gotcha. If you put `@Override` on a *static* method that "matches" a parent static method, the code **will not compile** (`@Override` is only legal on methods that override an instance method). If your `@Override` *did* compile, then one of two things was true: the parent method was actually an *instance* method, or you were annotating a genuinely overridden instance method. The annotation is a compiler check, not a magic spell — it doesn't change what the method is; it only *verifies* what you claimed.

### The "Correct" Example: Understanding Hiding

```java
class Parent {
    public static void identify() {
        System.out.println("I am a static method defined on Parent");
    }

    // A normal instance method, for contrast later in this section.
    public void instanceGreeting() {
        System.out.println("Instance method runs on: " + getClass().getSimpleName());
    }
}

class Child extends Parent {
    // CORRECT-EXAMPLE: We are deliberately HIDING Parent.identify().
    // We say nothing about overriding, because we are not overriding.
    public static void identify() {
        System.out.println("I am a static method defined on Child");
    }

    // This genuinely overrides Parent.instanceGreeting().
    @Override
    public void instanceGreeting() {
        System.out.println("Overridden instance method runs on: " + getClass().getSimpleName());
    }
}

public class HidingVsOverridingDemo {
    public static void main(String[] args) {
        Parent p = new Child();

        // Static: resolved from the reference type (Parent) at compile time.
        p.identify();

        // Instance: resolved from the runtime object (Child) at run time.
        p.instanceGreeting();
    }
}
```

**Expected Output**

```text
I am a static method defined on Parent
Overridden instance method runs on: Child
```

The same reference variable `p` produces two different behaviors in two adjacent lines. `p.identify()` binds statically to `Parent` because the compiler decides static calls immediately. `p.instanceGreeting()` binds dynamically to the *Child* object in memory, because the JVM defers instance-call resolution until runtime. This one example — a static call and an instance call through the *same variable* — is the fastest way to internalize the difference.

### The Analogy: An Org Chart and Employee Badges

Think of a company org chart. The "Sales" department has a name that is written on a sign on the door. That name belongs to the *department*, not to any individual employee. When someone says "go talk to Sales," they walk to the Sales office — even if a specific employee inside belongs to a different team. Now imagine every employee wears a badge that shows *their personal* name.

- The **class** is the department. Its static method name (like `greet`) is the sign on the door.
- The **object** is the employee. Its instance methods are the personal behaviors written on the badge.
- Calling `p.greet()` where `p` has type `Parent` is like someone telling you, "go talk to the department named on this *map label*." You go to `Parent`'s office, because that's what the label says — you never check which actual employee sits there.
- A child class "hiding" a static method is a new department hanging its own sign; the old department's sign still exists on the old door.

> **Memory hook:** *Static = sign on the door. Instance = badge on the employee. The sign is read by anyone walking past; the badge is read only when you actually meet the person.* If you're calling through a `Parent`-typed reference, you read the `Parent` sign. Period.

### What's Happening Under the Hood?

When you compile `Parent p = new Child(); p.greet();`, the Java compiler emits a bytecode instruction called `invokestatic`, and it bakes the *symbolic reference* to `Parent.greet()` directly into the class file. The JVM never performs a lookup based on the object type for that instruction — there is no method table consultation, no vtable walk, nothing. `invokestatic` targets a specific class's method, full stop. Contrast this with the instance call `p.instanceGreeting()`, which compiles to `invokevirtual` — that instruction tells the JVM, "find the method named `instanceGreeting` in the *actual* class of the object on the stack, and run the most specific version." That single bytecode difference, `invokestatic` versus `invokevirtual`, is the entire mechanical explanation for everything in this section.

### Hiding vs. Overriding: The Comparison Table

| Aspect | Method **Hiding** (static) | Method **Overriding** (instance) |
|---|---|---|
| Binding time | Compile time (`invokestatic`) | Runtime (`invokevirtual`) |
| Decided by | The *reference type* on the left of the dot | The *runtime type* of the object |
| `@Override` allowed? | **No** — compiler error | **Yes** (recommended) |
| Parent method status | Still exists; merely shadowed for the child | Effectively replaced for child objects |
| What the child method can reference | Only `static` members | Both instance and static members |
| Keyword in play | `static` | `extends` + override |
| `super` call from child method | Legal (call `Parent.greet()`) | Legal (`super.greet()`) |
| Polymorphic? | **Never** | The core of polymorphism |

### Real-World Use Case: The `Collections` and `Math` Utility Classes

Every Java developer calls static methods daily without thinking about why they're static. `Collections.sort(list)` and `Math.max(a, b)` are static because sorting and maximum-finding are *operations on values handed to them*, not behaviors of the objects themselves. The list is passed **in** as an argument precisely because the method does not "belong to" any single list.

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollectionsDemo {
    public static void main(String[] args) {
        List<String> words = new ArrayList<>(List.of("banana", "apple", "cherry"));

        // sort is a STATIC method on the Collections class.
        // The list is passed as an argument; sort does not run "on" the list.
        Collections.sort(words);

        System.out.println(words);
    }
}
```

**Expected Output**

```text
[apple, banana, cherry]
```

`Collections.sort(words)` compiles to `invokestatic`; the JVM calls the one and only `Collections.sort`. No subclass of `Collections` can ever change how this line behaves, which is exactly the point — utility classes are meant to be final, fixed behavior. This is why the standard library authors made these methods static, and why you should too for your own pure helper functions.

> **Transition:** Now that we know static methods belong to classes and are frozen at compile time, the obvious question is *why* instance methods are the opposite — why they demand an object at all.

---

## Section 2: Why Instance Methods Need Objects

### The Conceptual Explanation

An instance method is, by definition, a method that reads or modifies **object state** — the values stored in the fields of one specific object. Every instance method carries a hidden parameter, named **`this`**, which is a reference to the object the method was called on. That `this` is the entire reason the method needs an object: without an object, there is no state to read, no state to modify, and no meaning to the word "instance" in the method's name.

Compare the two families of methods:

- **Static methods** receive *only* the explicit arguments you pass. They cannot touch `this` (there is no `this`). They are functions attached to a class.
- **Instance methods** receive their explicit arguments *plus* the implicit `this`. Every time you write `alice.deposit(25)`, you are really calling `deposit` with two arguments: the object `alice` (as `this`) and the number `25`.

This is why the compiler rejects an instance method call without an object. `deposit(25)` alone is meaningless — *which* account's balance changes? `size()` alone is meaningless — *which* list? The object isn't a formality; it is the actual subject of the sentence.

### The Working Example: `this` Is the Implicit Argument

```java
public class BankAccount {
    private double balance;

    public BankAccount(double startingBalance) {
        this.balance = startingBalance;   // 'this' is the account under construction
    }

    public void deposit(double amount) {
        // 'this' is the hidden argument: the exact account being changed.
        // Without this.balance we could not know WHICH balance to update.
        this.balance += amount;
    }

    public double getBalance() {
        return this.balance;
    }

    public static void main(String[] args) {
        BankAccount alice = new BankAccount(100.0);
        BankAccount bob = new BankAccount(50.0);

        // Each call passes a different hidden 'this' argument.
        alice.deposit(25.0);
        bob.deposit(10.0);

        System.out.println(alice.getBalance());
        System.out.println(bob.getBalance());
    }
}
```

**Expected Output**

```text
125.0
60.0
```

`alice.deposit(25.0)` executes the *same* method code as `bob.deposit(10.0)` — there is only one copy of `deposit` in memory — but the first call binds `this` to the alice object, and the second binds `this` to the bob object. Alice's balance becomes `125.0`; Bob's becomes `60.0`. The method code is shared; the state it acts upon is not. That is the whole meaning of an instance method.

### Static vs. Instance: A Contrast Table

| Aspect | `Thread.sleep(1000)` (static) | `list.size()` (instance) |
|---|---|---|
| Needs an object? | **No** — sleeps *any* thread, needs no state | **Yes** — must know *which* list |
| Hidden `this` parameter | None | The list itself |
| What state it operates on | None (global/class behavior) | The list's internal elements |
| When resolved | Compile time | Runtime |
| Typical wording | "a class-level operation" | "a behavior of the object" |
| Example of misuse | `alice.deposit(25)` fails without an object | `Thread.sleep()` works with zero objects |

### The Analogy: A Recipe Versus a Chef With Ingredients

A recipe card is a *class*: it contains instructions that describe a dish in general — "whisk eggs, add flour, bake at 180°C." It is useful, but the recipe by itself feeds no one; it has no ingredients and no oven. A **chef** is an *object*: the chef has a specific set of ingredients (field values) in their kitchen, and when they follow the recipe, they apply it to *their* ingredients. Two chefs following the same recipe produce two different dishes if their ingredient supplies differ.

- The **recipe** is the instance method's code — one copy, shared by all.
- The **chef** is the object; the chef's pantry is the object's fields (`this.balance`).
- "Follow the recipe" is the method call: it always needs a chef attached (`alice.deposit`, not just `deposit`).
- A **static** method, by contrast, is a rule posted on the wall ("always wash your hands before cooking") — it applies to everyone and needs no individual chef's pantry to be meaningful.

> **Memory hook:** *The method is the recipe; `this` is the chef. You cannot cook the recipe in the abstract — you need a specific chef with specific ingredients.* Every instance method is secretly a two-argument method: `(this, explicitArgs...)`.

### Common-Student-Voice: Addressing the Fear

> "Why can't I just call `getBalance()` without an object if there's only one account in my program?"
>
> Because Java's type system has no notion of "the one account." The compiler cannot prove that only one object exists, so it forces you to be explicit about *which* object every time. This rule is what makes your code correct when your program grows from one account to a million. The annoyance at the start is the safety net at scale.

> "But `String.format(...)` and `Math.random()` work with no object — so instance methods are pointless, right?"
>
> Not at all. Those methods are *static by design* precisely because they have no object state to act upon. The moment a method needs state — `String.toLowerCase()`, `List.add(...)`, `BankAccount.deposit(...)` — Java requires an object. The distinction isn't arbitrary; it mirrors whether the operation needs `this`.

### What's Happening Under the Hood?

The JVM stores method *code* once, in a shared memory region called the **method area**. There are not a thousand copies of `deposit()` for a thousand `BankAccount` objects — there is one. What changes per call is the **`this` reference**, which the compiler arranges to be pushed onto the Java operand stack as the *first* argument before every `invokevirtual` instruction. The invoked method then reads its first parameter slot as `this` and uses it to locate the object's fields (each field is an offset into the object's heap layout). So the physical reality is: shared code, per-call `this`, per-object fields. That is the entire architecture, and it explains why instance methods are *cheap* — the only per-call work is pushing one reference and doing a table lookup.

### Real-World Use Case: `String` Methods Everywhere

You interact with instance methods constantly in the JDK. `String` is an immutable object, and nearly every operation on a string is an instance method that needs the string as `this`.

```java
public class StringMethodsDemo {
    public static void main(String[] args) {
        String greeting = "hello";

        // toUpperCase() is an INSTANCE method — it operates on the
        // characters of THIS greeting object (this).
        String loud = greeting.toUpperCase();

        // valueOf() is a STATIC method — it converts an int without
        // needing any String object to act upon.
        String number = String.valueOf(42);

        System.out.println(loud);
        System.out.println(number);
    }
}
```

**Expected Output**

```text
HELLO
42
```

`greeting.toUpperCase()` works because there is an object to transform; the JDK authors gave `String` a whole family of instance methods (`toLowerCase`, `substring`, `replace`, `trim`) because each one operates on the string's internal character array. `String.valueOf(42)` needs no object because converting an int to a string is a pure function of the int. Whenever you see an API that offers both forms — `String.format(...)` (static) and `"abc".format(...)`-style helpers (instance) — you're seeing this exact design decision in action.

> **Transition:** If instance methods run on the object, then the *type of the reference* you use to reach that object starts to matter a great deal — which leads directly to the confusion of "two types."

---

## Section 3: Why Reference Type Matters

### The Conceptual Explanation

Every variable that holds an object actually holds **two types** that can disagree:

1. The **declared type** (also called the reference type or compile-time type) — written in the variable declaration, e.g., `Vehicle v`. The compiler uses this type for *all* checks: "Is the method you're calling defined on this type?" and "Is this assignment legal?"
2. The **runtime type** (also called the actual type) — determined by the constructor you called, e.g., `new Motorcycle()`. The JVM uses this type at runtime to decide *which implementation* of an overridden method to run.

Beginners usually think there's only one type — "I made a `Motorcycle`, so everything about it is `Motorcycle`." But the compiler is not clairvoyant. When you write `Vehicle v = m;`, the compiler knows only that `v` can be trusted to *behave like* a `Vehicle`. It will happily let you call every method declared on `Vehicle`. It will refuse, with a compile error, to let you call a method that exists only on `Motorcycle` — even though, at runtime, the object really is a `Motorcycle`. The compiler is protecting you from a promise it cannot verify: a `Vehicle` reference *could* be pointing at a `Car` that has no `wheelie()` method at all.

This "two types" idea is the foundation of every remaining section in these notes. Reference type controls what you *may say*; runtime type controls what *actually happens*.

### The Working Example: Declared vs. Runtime Type

```java
class Vehicle {
    public void startEngine() {
        System.out.println("Vehicle engine starting");
    }

    public void honk() {
        System.out.println("Vehicle honk: beep");
    }
}

class Motorcycle extends Vehicle {
    @Override
    public void honk() {
        System.out.println("Motorcycle honk: beep beep beep");
    }

    // A method that exists ONLY on Motorcycle, not on Vehicle.
    public void wheelie() {
        System.out.println("Motorcycle popping a wheelie");
    }
}

public class ReferenceTypeDemo {
    public static void main(String[] args) {
        Motorcycle m = new Motorcycle();

        // Upcast: the DECLARED type is now Vehicle, but the RUNTIME
        // type of the object in memory is still Motorcycle.
        Vehicle v = m;

        // Compiles: honk() is declared on Vehicle.
        // Runs: Motorcycle.honk(), because the runtime object is a Motorcycle.
        v.honk();

        // Does NOT compile, even though the object really is a Motorcycle:
        // v.wheelie();   // <-- try uncommenting this line

        // Downcast first, then the Motorcycle-only method becomes reachable.
        ((Motorcycle) v).wheelie();
    }
}
```

**Expected Output**

```text
Motorcycle honk: beep beep beep
Motorcycle popping a wheelie
```

The call `v.honk()` compiles because `honk` is declared on `Vehicle`, and it prints the `Motorcycle` version because the object was created with `new Motorcycle()`. The call `v.wheelie()` would *fail to compile* — the compiler sees the declared type `Vehicle`, checks whether `Vehicle` has `wheelie()`, finds it does not, and refuses the program before it ever runs. Only after an explicit downcast `(Motorcycle) v` does the compiler allow the call. Notice the asymmetry: the compiler is strict about the *reference*, while the JVM is faithful to the *object*.

### A Diagram: The Two Types

```text
Reference variable:            Object on the heap:
                               ┌─────────────────────────┐
        Vehicle v;  ──────────►│   Motorcycle object     │
        (declared type)        │                         │
                               │  honk()      → Motorcycle version
                               │  startEngine()→ Vehicle version
                               │  wheelie()   → Motorcycle-only
                               └─────────────────────────┘

   The compiler looks at "Vehicle v"
     → only honk() and startEngine() are callable.
   The JVM looks at "new Motorcycle()"
     → honk() dispatches to the Motorcycle version.
```

### Declared Type vs. Runtime Type: The Table

| Example | Declared (compile-time) type | Runtime (actual) type | What **compiles** | What **runs** |
|---|---|---|---|---|
| `Vehicle v = new Motorcycle();` | `Vehicle` | `Motorcycle` | `v.honk()`, `v.startEngine()` | `Motorcycle.honk()` (overridden), `Vehicle.startEngine()` |
| `v.wheelie()` | `Vehicle` | `Motorcycle` | **Nothing — compile error** | Never runs |
| `((Motorcycle) v).wheelie()` | `Motorcycle` (after cast) | `Motorcycle` | `wheelie()` | `Motorcycle.wheelie()` |
| `Motorcycle m = new Motorcycle();` | `Motorcycle` | `Motorcycle` | `m.honk()`, `m.wheelie()` | `Motorcycle` versions of both |

> **Memory hook:** *The compiler reads the label on the box; the JVM reads the contents of the box.* The label tells you what operations are *legal*; the contents tell you what the operations actually *do*.

### Common-Student-Voice: The Follow-Up Questions

> "But I *know* `v` points to a Motorcycle — why does the compiler punish me?"
>
> Because the compiler can't read your mind, and more importantly, it can't prove that `v` will always point to a `Motorcycle`. The whole point of a `Vehicle` reference is that it might later be reassigned to a `Car` or a `Truck`. Allowing `v.wheelie()` would let you call a method that a `Car` doesn't have. The compiler enforces the *contract*, not the coincidence.

> "So is casting a hack? Should I avoid it?"
>
> Downcasting is not a hack; it's a *verified* narrowing. The JVM inserts a `checkcast` instruction that throws `ClassCastException` if the object isn't actually the cast target. Cast when you have extra information the type system can't express — but if you find yourself casting constantly, it's usually a design smell that signals you should have used polymorphism instead (Section 4).

### What's Happening Under the Hood?

Every local variable and parameter in a Java method has a *declared type* recorded in the method's bytecode attributes (in the `LocalVariableTable` and the method descriptor). When `javac` sees `v.honk()`, it looks up `honk` in `Vehicle`'s set of methods; finding it, it emits `invokevirtual` referencing `Vehicle.honk`. Crucially, the bytecode instruction *does not* contain "Vehicle" as a hard target in the way `invokestatic` does — `invokevirtual` contains the name, the argument types, and the *return type* of the method, and the JVM resolves it by walking the runtime class's method table. When you downcast, the compiler inserts a `checkcast` bytecode instruction; at runtime the JVM verifies the object really is (a subtype of) the cast target, and throws `ClassCastException` if it is not. So even "the compiler refused" and "the cast worked" are mechanical, inspectable steps in the bytecode.

### Real-World Use Case: Programming to Interfaces in the Collections Framework

The single most common place this shows up in professional Java is the idiom `List<String> names = new ArrayList<>();`. The declared type is the interface `List`; the runtime type is `ArrayList` (or later, `LinkedList`). Whole libraries accept `List` parameters and call only `List` methods — which is exactly the safety the compiler enforces.

```java
import java.util.ArrayList;
import java.util.List;

public class ListDemo {
    // Accept a List, not an ArrayList — this is "program to the interface."
    public static void printSizes(List<String> first, List<String> second) {
        // size() and get() are declared on List, so this compiles no matter
        // which concrete list implementation we receive at runtime.
        System.out.println(first.size() + " + " + second.size()
                + " = " + (first.size() + second.size()));
    }

    public static void main(String[] args) {
        List<String> names = new ArrayList<>(List.of("Ada", "Grace"));

        // get(int) compiles because List declares it.
        String first = names.get(0);

        // A method that exists ONLY on ArrayList would NOT compile here:
        // names.ensureCapacity(100);   // <-- ArrayList-only, rejected

        System.out.println(first);
        printSizes(names, List.of("Alan", "Dennis", "Barbara"));
    }
}
```

**Expected Output**

```text
Ada
2 + 3 = 5
```

`names.get(0)` compiles because `List` declares `get`. `names.ensureCapacity(100)` would fail to compile because `ensureCapacity` is an `ArrayList`-only method and the declared type is `List` — the same mechanism that blocked `v.wheelie()` above. This is why framework methods like `Collections.sort(List<T>)` and Spring's injection points declare interfaces: they are safe for *any* implementation, and the compile-time check guarantees it.

> **Transition:** Once you accept that the reference type gates *what you may call* while the runtime type decides *what runs*, the next question is why the language bothers to make the runtime decision at all — that's the reason runtime polymorphism exists.

---

## Section 4: Why Runtime Polymorphism Exists

### The Conceptual Explanation

Runtime polymorphism — also called **dynamic dispatch** — is the answer to a design problem: *how do you write code that works for an entire family of types, even types that haven't been invented yet?* Without dynamic dispatch, you'd need an `if`-chain for every possible kind of object, and every time someone added a new subclass, you'd have to edit that chain — and so would every library that used it.

The mechanism is simple to state: when you call an overridden instance method through a reference, the JVM ignores the reference type and asks the *actual object* which version of the method to run. That's it. But the *consequences* are enormous. It means you can write `for (Shape s : shapes) { s.draw(); }` once, and the loop handles `Circle`, `Rectangle`, `Triangle`, and any future subclass without being changed. The method called is decided by the runtime object, not the reference. This is the **Liskov substitution principle** in action: if `Circle` IS-A `Shape` and behaves properly, any code that expects a `Shape` can safely be handed a `Circle`.

### The Working Example: The Shape Hierarchy

```java
abstract class Shape {
    // The contract: every shape can report its area.
    public abstract double area();

    // A concrete method on Shape that relies on the abstract area().
    // Because area() is virtual, this method behaves differently per subclass.
    public void describe() {
        System.out.println("I am a " + getClass().getSimpleName()
                + " with area " + area());
    }
}

class Circle extends Shape {
    private final double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }
}

class Rectangle extends Shape {
    private final double width;
    private final double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public double area() {
        return width * height;
    }
}

public class PolymorphismDemo {
    public static void main(String[] args) {
        // One array, mixed runtime types, one declared type: Shape.
        Shape[] shapes = { new Circle(1.0), new Rectangle(2.0, 3.0) };

        // The SAME call site serves every shape.
        // draw()-style logic: the object decides what happens.
        for (Shape s : shapes) {
            s.describe();
        }
    }
}
```

**Expected Output**

```text
I am a Circle with area 3.141592653589793
I am a Rectangle with area 6.0
```

The loop contains a single call, `s.describe()`, compiled once against `Shape`. Yet for the first iteration the JVM dispatches to `Circle.describe()` (inherited, but running `Circle.area()`), and for the second to the `Rectangle` path. The magic in `describe()` is even more subtle: `describe()` itself is *not* overridden anywhere — it lives only on `Shape` — but the `area()` call inside it *is* dispatched dynamically, so the same printed sentence reads "Circle... 3.14" and "Rectangle... 6.0". One method, one call site, infinitely many behaviors. That is polymorphism.

### The Analogy: The Traffic Controller

A traffic controller stands at an intersection and gives every approaching vehicle the same signal: a green "go." Each vehicle *reacts differently* to the identical instruction. A car accelerates and shifts gears. A bike pedals forward. A truck rumbles away, its engine roaring. The controller never needs to know how to drive each vehicle — they just give the signal and trust the vehicle to know itself.

- The **traffic controller** is your code (`for (Shape s : shapes) { s.describe(); }`).
- The **"go" signal** is the method call (`s.describe()`).
- The **vehicles** are the objects (`Circle`, `Rectangle`).
- The **reaction** is the overridden method body — chosen by the object, not by the controller.
- A **new type of vehicle** appearing next year (say, a skateboard) is a new subclass; the controller still just says "go." No intersection code needs to change.

> **Memory hook:** *The caller says WHAT; the object decides HOW.* If you ever find yourself writing `if (x instanceof Circle) ... else if (x instanceof Rectangle) ...`, you have stopped being a traffic controller and started trying to drive every vehicle yourself.

### Compile-Time vs. Runtime Decision: The Table

| Aspect | **Static binding** (Section 1) | **Dynamic dispatch** (this section) |
|---|---|---|
| When decided | Compile time | Runtime |
| Based on | Reference type | Runtime (actual) type |
| Which methods | `static`, `private`, `final` | Overridable instance methods |
| Bytecode | `invokestatic`, `invokespecial` | `invokevirtual`, `invokeinterface` |
| Can be changed by subclasses? | No | Yes |
| Cost | Near zero | One vtable lookup per call |
| Typical use | Utility calls, constructors | Frameworks, event systems, plugins |

### What's Happening Under the Hood?

Each class, when loaded, gets a **method table** (vtable) in the JVM's memory: an array of pointers to the actual code of every virtual method it can run, in a fixed order determined by the superclass chain. When a subclass overrides a method, it places its *own* code pointer in the same slot its parent used. `invokevirtual` works in three steps: (1) pop the object reference, (2) read the class of that object, (3) index into that class's vtable using the *fixed slot number* that was computed at compile time, then jump to whatever code pointer is there. Because slots are positional, the same bytecode works for every class in the hierarchy — the only variable is which table the object's class points to. This is why adding a new subclass costs zero changes to existing callers: the new class just builds its own table with the same slot layout.

### Why Does Java Bother? Real-World Context

Dynamic dispatch is the backbone of every major Java ecosystem feature. **GUI frameworks** (JavaFX, Swing): an event handler is registered as a callback, and the framework calls `handle(event)` on whatever listener object you supplied — it has no idea what your listener does. **Collections and sorting**: `Collections.sort(list, comparator)` calls `comparator.compare(a, b)` polymorphically; you can sort the same list in five different orders by passing five different comparators. **Plugin systems and DI frameworks** (Spring): a framework instantiates your classes and calls interface methods on them, often before your code has even met the framework. None of this would be possible with static binding — a framework cannot know your class at the time it was compiled, so it *must* defer the decision to runtime.

### Real-World Use Case: Sorting With a Comparator

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ComparatorDemo {
    public static void main(String[] args) {
        List<String> names = new ArrayList<>(List.of("Alice", "bob", "Carol"));

        // sort() is the "traffic controller": it calls compare() on whatever
        // Comparator object it receives. The exact ordering logic is decided
        // at runtime by the object we hand it.
        Collections.sort(names, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return a.compareToIgnoreCase(b);
            }
        });

        System.out.println(names);

        // The same sort() call, a different runtime Comparator → different order.
        Collections.sort(names, Comparator.reverseOrder());
        System.out.println(names);
    }
}
```

**Expected Output**

```text
[Alice, bob, Carol]
[Carol, bob, Alice]
```

`Collections.sort` is compiled once, against the `Comparator` interface, and its internal `compare` calls dispatch to whatever comparator object is passed. With the case-insensitive comparator, the order is `[Alice, bob, Carol]`; with `reverseOrder()`, the *same* `sort` method produces the reverse. Two behaviors from one method, zero changes to the library — that is why frameworks and libraries are written against interfaces and why your own code should be, too.

> **Transition:** With polymorphism in place, one more piece completes the picture: the return type of an overridden method may itself be refined — which is the surprisingly flexible rule of covariant return types.

---

## Section 5: Covariant Return Types

### The Conceptual Explanation

**Covariant return types** are the rule that allows an overriding method to declare a *more specific* return type than the method it overrides. If `Parent` declares `Animal makePet()`, a `Child` may override it as `Dog makePet()` — because every `Dog` IS-A `Animal`, so anyone expecting an `Animal` is still fully satisfied by a `Dog`.

This might sound like a niche detail, but it was a genuine *feature addition*: it was introduced in **Java 5 (2004)**. Before that, an override had to have the *exact same* return type — the return type was said to be **invariant**. Why did Java bother to relax the rule? Because the pre-Java-5 situation forced ugly, unnecessary casts and made good API design impossible. If your method genuinely promised a more specific type, you had to override with the parent return type anyway, and then cast at every call site. Java 5 removed that friction.

Why is it *safe*? Because of the subtype relationship. The contract of the original method is "callers get an `Animal`." The override returns a `Dog`, which *is* an `Animal`. No caller can tell the difference; no caller's expectation is violated. The return type only became more informative, never less. That directionality — more specific down the hierarchy — is exactly what "covariant" means: the return types vary *together* with the class types.

### The Working Example: An Inheritance Hierarchy

```java
class Animal {
    public void speak() {
        System.out.println("Some animal noise");
    }
}

class Dog extends Animal {
    @Override
    public void speak() {
        System.out.println("Woof!");
    }

    // A method that only dogs have.
    public void fetch() {
        System.out.println("Fetching the stick...");
    }
}

class AnimalShelter {
    // The original contract: every shelter can produce an Animal.
    public Animal adopt() {
        return new Animal();
    }
}

class DogShelter extends AnimalShelter {
    // COVARIANT RETURN: the override may return Dog instead of Animal.
    // This is legal because Dog IS-A Animal.
    @Override
    public Dog adopt() {
        return new Dog();
    }
}

public class CovariantReturnDemo {
    public static void main(String[] args) {
        AnimalShelter shelter = new DogShelter();

        // Even through an AnimalShelter reference, the runtime object is a Dog.
        Animal pet = shelter.adopt();
        pet.speak();

        // No cast needed here: the DECLARED type of the return is Dog,
        // so Dog-only methods are directly callable.
        DogShelter dogShelter = new DogShelter();
        Dog realDog = dogShelter.adopt();
        realDog.fetch();
    }
}
```

**Expected Output**

```text
Woof!
Fetching the stick...
```

First, `shelter.adopt()` — called through the `AnimalShelter` reference — runs `DogShelter.adopt()` (dynamic dispatch) and returns a `Dog` stored in an `Animal` variable; `pet.speak()` prints "Woof!" because the runtime type is `Dog`. Second, through the `DogShelter` reference, the method's declared return type is `Dog`, so `realDog.fetch()` compiles *without any cast*. That second line is the entire payoff of covariant returns: the compiler already knows you got a `Dog`, so it hands you Dog's full API.

### The "Before" Example: The Cast-Filled World of Java ≤ 1.4

```java
// BEFORE (pre-Java-5 style): covariant returns were illegal,
// so the override had to keep the parent return type.
class BeforeDogShelter extends AnimalShelter {
    // WRONG-EXAMPLE for Java 1.4: returning Dog here would NOT compile.
    // You were forced to return Animal and cast at every call site.
    @Override
    public Animal adopt() {
        return new Dog();   // the Dog is disguised as an Animal
    }
}

public class BeforeDemo {
    public static void main(String[] args) {
        BeforeDogShelter shelter = new BeforeDogShelter();

        // Ugly: the call site must downcast even though the shelter
        // "obviously" always returns a Dog.
        Dog dog = (Dog) shelter.adopt();

        // And if you forget the cast, fetch() is unreachable:
        // shelter.adopt().fetch();   // <-- does not compile without the cast
        System.out.println("Got a " + dog.getClass().getSimpleName());
    }
}
```

**Expected Output**

```text
Got a Dog
```

Every call site that wanted a `Dog` was forced to write `(Dog) shelter.adopt()`. If you forgot the cast, you couldn't call `fetch()` at all. If the implementation later changed to return a `Cat`, the cast would throw `ClassCastException` at runtime. Covariant returns eliminate the boilerplate and move the check to compile time — the compiler now *proves* the return is a `Dog`, so no cast and no surprise.

### Return Type Relationships: The Table

| Overridden return type | Override return type | Allowed? | Why |
|---|---|---|---|
| `Animal` | `Animal` | ✅ Yes | Exact match (invariant) |
| `Animal` | `Dog` | ✅ Yes | **Covariant** — `Dog` IS-A `Animal`, safe |
| `Dog` | `Animal` | ❌ **No** | Contravariant — an override could hand callers a non-`Dog`; breaks the contract |
| `Animal` | `String` | ❌ **No** | Unrelated types — no subtype relationship |
| `int` | `long` | ❌ **No** | Primitives are invariant; widening is *not* covariance |

> **Memory hook:** *The override may return a subtype, never a supertype.* Down the class hierarchy, return types may only become **more specific** — the same direction the classes themselves go. That's why it's called *co*-variant: the return types vary *with* the class types.

### Why Is It Safe, and Why Did Java 5 Bother?

Safety comes from the subtype guarantee: a method's return type is a promise to callers, and returning a `Dog` when an `Animal` was promised is a *stronger* promise, never a weaker one. Every caller that accepted `Animal` accepts `Dog` with zero changes. Java 5 introduced the feature because the pre-2004 world was actively hostile to good design: API authors either wrote methods returning the most general useful type (forcing casts everywhere) or duplicated methods. Modern Java APIs like `Object.clone()` (which returns `Object`, and whose overrides can return the concrete type) and the fluent `Stream`/`StringBuilder` styles lean on covariant returns constantly.

### Common-Student-Voice: The Classic Doubt

> "Wait — if the return type can differ, how does the JVM even *know* which method is which?"
>
> Because the return type is part of a method's signature for the *compiler and the JVM*. The JVM method descriptor encodes `(arguments)returntype`, so `adopt()Animal` and `adopt()Dog` are distinguishable in the class file. When you write an override with a covariant return type, `javac` generates a **bridge method**: a synthetic `adopt()Animal` method that simply calls your `adopt()Dog` and casts the result. That bridge keeps the JVM's method-resolution rules intact while giving *you* the nice type. The JVM sees two methods; your source code sees one. It's compiler magic that just works.

### What's Happening Under the Hood?

At the bytecode level, covariant overrides compile to two methods: your real method (with the specific return type) plus a synthetic **bridge method** holding the parent's descriptor, whose body is simply `return (Animal) this.adopt()Dog();`. The bridge is marked with `ACC_BRIDGE | ACC_SYNTHETIC` and exists solely to satisfy the JVM's method-resolution lookup (which matches on name plus descriptor). Without the bridge, a call through a `AnimalShelter` reference would fail to find `DogShelter.adopt()`. The `javac` compiler generates bridges automatically, so you never write them — but if you ever look at decompiled or disassembled code (`javap -c`), this is what you'll see.

### Real-World Use Case: `Object.clone()` and Fluent APIs

`Object.clone()` returns `Object`, so every class that implements `Cloneable` can override it with a covariant return type — and the JDK's own `StringBuffer`, `StringBuilder`, and `Date` classes do exactly that. The same pattern powers the fluent style where chained calls must return the concrete type.

```java
class Message implements Cloneable {
    private final String text;

    public Message(String text) {
        this.text = text;
    }

    // Covariant override: we promise a Message, not just an Object.
    @Override
    public Message clone() {
        try {
            // super.clone() returns Object; we cast once, here, and
            // callers never have to.
            return (Message) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public String getText() {
        return text;
    }

    public static void main(String[] args) {
        Message original = new Message("Hello, world");

        // No cast at the call site — clone() is declared to return Message.
        Message copy = original.clone();

        // Because Message has no mutable state, the clone is "equal"
        // in content to the original.
        System.out.println(copy.getText());
    }
}
```

**Expected Output**

```text
Hello, world
```

`copy` is typed `Message` directly from `original.clone()`, with no downcast — the covariant return type in the override makes that possible. If the JDK had not added this feature, every `clone()` caller in the entire ecosystem would carry a `(Message)` cast. That one-line difference is the whole story of Java 5's improvement: the language got *safer* and *more concise* at the same time.

> **Transition:** Now that all five confusions have been unpacked, let's compress everything into the mistakes to avoid and the rules to keep.

---

## Common Mistakes & Gotchas

- **Putting `@Override` on a static method.** It's a compile error — the annotation only verifies *overrides*, and static methods can only be *hidden*. Correction: if the method is `static`, drop the annotation and expect hiding.
- **Expecting a static method to be polymorphic through a parent reference.** `Parent p = new Child(); p.greet();` calls `Parent.greet()`. Correction: remember `invokestatic` binds to the reference type at compile time.
- **Calling an instance method without an object.** `getBalance()` with no receiver won't compile. Correction: every instance method needs a `this`; use `alice.getBalance()` or make the method `static` if it truly needs no state.
- **Forgetting that the compiler uses the declared type.** `List<String> names = new ArrayList<>(); names.ensureCapacity(100);` fails. Correction: declare the variable as the type whose full API you need, or cast.
- **Believing the runtime type governs *what you may call*.** It governs only *which implementation runs*. The declared type governs legality. Correction: two types, two jobs — keep them separate.
- **Downcasting without a check when you're not certain.** `(Motorcycle) v` throws `ClassCastException` at runtime if the object isn't a `Motorcycle`. Correction: guard with `instanceof`, or redesign to avoid the cast.
- **Writing `if (x instanceof Y) ... else if (x instanceof Z) ...` chains.** You've reimplemented dynamic dispatch by hand. Correction: define the behavior in the subclasses (an overridden method) and let the JVM dispatch.
- **Assuming an override must have the *same* return type.** Since Java 5 it may be a *subtype* — but never a supertype. Correction: covariant returns are legal; contravariant returns are not.
- **Expecting primitive widening to be covariant.** `int` → `long` is *not* a valid covariant return change. Correction: covariance applies to reference types only.
- **Thinking a `final` class's methods can be overridden.** A `final` class cannot be subclassed at all, so no override is possible. Correction: finality is a deliberate "polymorphism off" switch.

---

## Summary / Key Takeaways

- **Static methods are never overridden — they are hidden.** Resolution happens at compile time via `invokestatic`, based on the *reference type*, and `@Override` on a static method is a compile error.
- **Instance methods exist once in memory, but `this` changes per call.** The object is the hidden first argument; without it there is no state to operate on, which is why `list.size()` needs a list but `Math.max(...)` needs nothing.
- **Every object variable has two types: declared and runtime.** The declared type decides *what compiles*; the runtime type decides *what runs*. `Vehicle v = new Motorcycle()` gives you the `Motorcycle` object but only the `Vehicle` API.
- **Dynamic dispatch is why `what runs` differs from `what compiles`.** `invokevirtual` looks up the method in the runtime object's vtable, enabling one call site to serve any subclass — the foundation of frameworks, GUIs, sorting, and dependency injection.
- **Covariant return types let an override return a subtype** of the overridden method's return type, because a subtype IS-A its supertype. Java 5 added this to eliminate pointless casts; the compiler generates a synthetic bridge method under the hood.
- **The whole confusion reduces to three questions:** What did I declare? (compiler) What object did I create? (JVM) Does the method have a `this`? (static vs. instance).
- **When you find yourself casting, checking `instanceof`, or writing `if/else` chains over subclass types, stop** — you're fighting the language instead of using its polymorphism.

---

## Practice Exercises

**1. (Easy) What is the output?**
```java
class A {
    public static void tag() { System.out.println("A"); }
    public void speak() { System.out.println("A speaks"); }
}
class B extends A {
    public static void tag() { System.out.println("B"); }
    @Override public void speak() { System.out.println("B speaks"); }
}
public class Q1 {
    public static void main(String[] args) {
        A ref = new B();
        ref.tag();
        ref.speak();
        B.tag();
    }
}
```

**2. (Easy) What is the output?**
```java
public class Q2 {
    static int counter = 0;
    int value;

    Q2(int v) { value = v; counter++; }

    int bump() { value++; counter++; return value; }

    public static void main(String[] args) {
        Q2 a = new Q2(1);
        Q2 b = new Q2(10);
        System.out.println(a.bump());
        System.out.println(b.bump());
        System.out.println(Q2.counter);
    }
}
```

**3. (Medium) Does this compile? If not, why, and how do you fix it?**
```java
class Fruit { public void peel() {} }
class Apple extends Fruit { public void core() {} }

public class Q3 {
    public static void main(String[] args) {
        Fruit f = new Apple();
        f.core();                // (a)
        ((Apple) f).core();      // (b)
        Fruit g = new Fruit();
        ((Apple) g).core();      // (c)
    }
}
```
For each of the three lines, state whether it compiles and (if it runs) what happens at runtime.

**4. (Medium) Write the code.** Design a `Payment` hierarchy with an abstract method `double amount()`. Add `CashPayment` and `CardPayment` subclasses with a `surcharge` field added in the override for `CardPayment` (via a covariant-return helper `withSurcharge(double)` that returns `CardPayment`). Then in `main`, iterate a `Payment[]` array and print each payment's amount, demonstrating that `amount()` is dispatched dynamically.

**5. (Hard) Explain and fix.** A teammate wrote this and says the output is `"Circle drawn"` for both lines:
```java
class Shape2 { public void draw() { System.out.println("Shape drawn"); } }
class Circle2 extends Shape2 { public void draw() { System.out.println("Circle drawn"); } }
public class Q5 {
    static void drawIt(Shape2 s) { s.draw(); }
    public static void main(String[] args) {
        Shape2 s = new Circle2();
        drawIt(s);
        Shape2[] arr = { new Circle2(), new Shape2() };
        for (Shape2 x : arr) drawIt(x);
    }
}
```
Explain exactly what actually prints and why. Then describe how the vtable makes this work — name the bytecode instruction used for `s.draw()` and explain the three steps the JVM performs.

---

### Answer Key

**1.** Output: `A`, `B speaks`, `B`. `ref.tag()` binds statically to declared type `A`; `ref.speak()` dispatches to the runtime type `B`; `B.tag()` is explicitly the child's static method.

**2.** Output: `2`, `11`, `3`. Each `bump()` uses its own object's `this.value` (1→2, 10→11), while `counter` is shared static state incremented by two constructors plus two `bump()` calls.

**3.** (a) does **not compile** — `core()` is not declared on `Fruit`. (b) compiles and runs — the object really is an `Apple`. (c) compiles but throws `ClassCastException` at runtime — the object is a plain `Fruit`, so the JVM's `checkcast` fails.

**4.** Sample:
```java
abstract class Payment { public abstract double amount(); }
class CashPayment extends Payment {
    private final double cash;
    CashPayment(double c) { cash = c; }
    @Override public double amount() { return cash; }
}
class CardPayment extends Payment {
    private final double charged;
    CardPayment(double base, double surcharge) { charged = base + surcharge; }
    CardPayment withSurcharge(double extra) { return new CardPayment(0, extra); }
    @Override public double amount() { return charged; }
}
```
Key idea: `amount()` is called through a `Payment` reference and resolved by runtime type.

**5.** Actual output is `Circle drawn`, `Circle drawn`, `Shape drawn`. The vtable slots for `draw` differ per class (`Circle2`'s slot points to its own `draw`; `Shape2`'s points to the base), and `invokevirtual` (1) pops the receiver, (2) reads its class, (3) indexes that class's vtable at the compile-time-fixed slot to find the method to run.