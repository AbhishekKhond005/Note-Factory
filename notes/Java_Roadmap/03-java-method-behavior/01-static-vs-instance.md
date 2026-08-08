# Chapter: `static` vs `instance` Members in Java

## 1. Introduction

Imagine you are writing software for a small school. You need to register every student with their own name, grade, and locker number — but you *also* need to know how many students have enrolled today. The per-student facts (name, grade) are obviously different for every student. The count, however, is a school-wide fact: it is the same number no matter which student you ask. These are two fundamentally different kinds of data, and Java models them with two different kinds of members: **instance members** and **static members**.

This chapter is a deep, beginner-friendly dive into one of the most misunderstood topics in Java. We will cover:

- What classes, objects, and memory actually are;
- **Instance fields** and **instance methods** — data and behavior owned by each object;
- **Static fields** and **static methods** — data and behavior owned by the class itself;
- How `this` interacts with each world;
- A practical decision guide for *when to use which*;
- Real-world examples, common pitfalls, interview questions, and exercises.

"Static vs instance" trips up developers for years because the distinction is about *ownership* and *memory*, not about syntax. The moment you understand *who owns the data*, the whole topic clicks into place. By the end of this chapter, you will be able to look at any Java class and instantly classify every member as static or instance — and, more importantly, explain *why*.

---

## 2. Core Concepts: What Are Classes, Objects, and Memory?

Before we can talk about static vs instance, we need two definitions nailed down.

- A **class** is a *blueprint*. It describes *what kinds of data* exist (fields) and *what behaviors* are possible (methods).
- An **object** (also called an **instance**) is a *concrete realization* built from that blueprint. A class defines the structure; `new` builds actual objects that live in memory.

**Analogy: cookie cutters and cookies.** A cookie cutter is the *class*: it defines the shape, but you cannot eat it. Each cookie pressed out of the cutter is an *object*: it has the same shape as every other cookie, but it is a separate physical thing. One cutter can produce a thousand cookies, each independent. The same is true in Java: `new Student(...)` can produce a thousand `Student` objects, each with its own data.

Here is a minimal class skeleton with **no** static members yet — just a blueprint for a student:

```java
public class Student {
    // Instance fields: each Student object gets its OWN copies of these
    private String name;
    private int grade;

    // Constructor: runs when an object is created with `new`
    public Student(String name, int grade) {
        this.name = name;
        this.grade = grade;
    }

    // Instance method: operates on one particular Student object
    public void display() {
        System.out.println(name + " is in grade " + grade);
    }
}
```

Every `new Student(...)` call allocates a fresh chunk of memory on the **heap** (Java's big object-storage area). The fields `name` and `grade` inside that chunk belong *only* to that one object. This is the heart of the instance world.

---

## 3. Instance Fields

### Definition

An **instance field** is a variable declared inside a class *without* the `static` keyword. It belongs to each **individual object**. Every object created from the class gets its **own copy** of every instance field, with its own value.

### Key Properties

- Declared without `static`: `private String name;`
- Each `new` object receives its own copy — two objects can hold different values simultaneously.
- Accessed through a **reference** to an object (`alice.name`), not through the class name.
- Initialized when the object is constructed (via the constructor, field initializer, or default value).
- Lives inside the object on the **heap**.

**Analogy: everyone's name is their own.** In a classroom, every student has their own name, their own backpack, their own phone number. Alice's phone number is not Bob's phone number — they just happen to have *fields with the same shape*. An instance field is exactly that: a slot for a value that every object possesses, but that holds that object's personal data.

### Code Example

```java
public class Student {
    // Instance fields
    private String name;
    private int grade;

    public Student(String name, int grade) {
        this.name = name;
        this.grade = grade;
    }

    // Instance method: reads THIS object's fields
    public void display() {
        System.out.println(name + " is in grade " + grade);
    }

    public static void main(String[] args) {
        // Two separate objects, each with its OWN copies of name and grade
        Student alice = new Student("Alice", 10);
        Student bob   = new Student("Bob", 11);

        alice.display();   // prints Alice's data
        bob.display();     // prints Bob's data

        // They are independent objects, not the same object
        System.out.println("alice == bob? " + (alice == bob));   // false
    }
}
```

Expected output:

```
Alice is in grade 10
Bob is in grade 11
alice == bob? false
```

### Memory Diagram

On the heap, each object is a self-contained box. Changing `alice.grade` can never affect `bob.grade`, because they live in different boxes:

```
        HEAP (objects live here)                    STACK (references live here)

┌──────────────────────────────────┐
│   Student object #1              │        ┌─────────┐
│   ┌──────────────────────────┐   │        │  alice  │──┐
│   │ name: "Alice"            │   │        └─────────┘  │
│   │ grade: 10                │   │                     └──────▶┐
│   └──────────────────────────┘   │                              │
│                                  │                              ▼
│   Student object #2              │        ┌─────────┐   ┌──────────────────────┐
│   ┌──────────────────────────┐   │        │   bob   │──▶│ name: "Bob"          │
│   │ name: "Bob"              │   │        └─────────┘   │ grade: 11            │
│   │ grade: 11                │   │                     └──────────────────────┘
│   └──────────────────────────┘   │
└──────────────────────────────────┘
```

The variable `alice` does not *contain* the object; it *points to* it. There are two boxes, two copies of `name`, two copies of `grade`. That is the defining trait of instance fields.

---

## 4. Static Fields (Class Variables)

### Definition

A **static field** (or **class variable**) is a field declared with the `static` keyword. It belongs to the **class itself**, not to any object. No matter how many objects you create — zero, one, or a million — there is **exactly one** copy of a static field in the entire program.

### Key Properties

- Declared with `static`: `private static int studentCount;`
- **One copy exists total**, shared by all instances (and usable even when no instance exists).
- Accessible via the class name: `Student.studentCount`.
- Technically also reachable through an instance reference (`someStudent.studentCount`), but this is **discouraged** — it works, yet it misleads readers into thinking the value belongs to the object. Modern IDEs warn about it.
- Initialized when the class is first loaded, and it lives in the class's metadata area (not inside any object).

**Analogy: the classroom whiteboard vs. personal notebooks.** Each student's *notebook* is private — Bob cannot read Alice's notes. But the *whiteboard* at the front of the room is shared: everyone sees the same words, and when one person erases it, it is gone for everyone. A static field is that whiteboard. It belongs to the class (the classroom), not to any single student.

### Code Example

```java
public class Student {
    // Instance fields: one per object
    private String name;
    private int grade;

    // STATIC field: ONE copy for the entire program, shared by all students
    private static int studentCount = 0;

    public Student(String name, int grade) {
        this.name = name;
        this.grade = grade;
        studentCount++;                    // every new student bumps the shared counter
    }

    public void display() {
        System.out.println(name + " is in grade " + grade);
    }

    public static void main(String[] args) {
        Student alice = new Student("Alice", 10);
        Student bob   = new Student("Bob", 11);
        Student carol = new Student("Carol", 9);

        // Access the shared value through the CLASS name (best practice)
        System.out.println("Total students created: " + Student.studentCount);

        // Discouraged but legal — works, yet suggests the value is per-object
        System.out.println("Via an instance: " + alice.studentCount);
    }
}
```

Expected output:

```
Total students created: 3
Via an instance: 3
```

Notice that `alice.studentCount`, `bob.studentCount`, and `Student.studentCount` all print `3` — because they are all the *same single slot of memory*.

### Instance vs Static Fields — Comparison Table

| Aspect | Instance Field | Static Field |
|---|---|---|
| Declared with | `private String name;` | `private static int count;` |
| Owned by | Each individual object | The class itself |
| Number of copies | One per object (`new`) | Exactly one total |
| Memory location | Inside each object on the heap | Class metadata / method area (one shared slot) |
| Access syntax | `object.field` | `ClassName.field` (recommended) |
| Accessible before any object exists? | No | Yes |
| Initialization timing | When each object is constructed | When the class is first loaded |
| Default value | Depends on the object's constructor | Depends on the class's initializer |

---

## 5. Instance Methods

### Definition

An **instance method** is a method declared *without* `static`. It is a behavior that operates on **one specific object's data**. Because it runs on behalf of an object, it can read and write that object's instance fields.

### Key Properties

- Can access everything: the object's instance fields, other instance methods, and also static fields and static methods.
- Implicitly receives a hidden reference called `this`, which points to the object the method was called on.
- Must be invoked on an **object reference**: `alice.getGrade()`. You cannot call it via the class name (`Student.getGrade()` will not compile).
- Each call is *bound* to a specific object — two objects calling the same method use that method with their own data.

**Analogy: the "start engine" button.** Every car has a start button, but pressing it starts *that* car's engine, using *that* car's fuel gauge and ignition state. The *procedure* is the same for all cars (that's the method), but it always acts on the specific car whose button you pressed (that's the object).

### Code Example

```java
public class Student {
    private String name;
    private int grade;

    public Student(String name, int grade) {
        this.name = name;
        this.grade = grade;
    }

    // Instance method: reads THIS object's grade
    public int getGrade() {
        return grade;                    // implicitly: return this.grade;
    }

    // Instance method: modifies THIS object's grade
    public void updateGrade(int newGrade) {
        if (newGrade >= 1 && newGrade <= 12) {
            this.grade = newGrade;       // 'this' makes the assignment unambiguous
        } else {
            System.out.println("Invalid grade: " + newGrade);
        }
    }

    public void display() {
        System.out.println(name + " is in grade " + grade);
    }

    public static void main(String[] args) {
        Student alice = new Student("Alice", 10);
        Student bob   = new Student("Bob", 11);

        System.out.println("Alice's grade: " + alice.getGrade());
        alice.updateGrade(11);           // affects ONLY alice
        alice.display();
        bob.display();                   // bob is untouched
    }
}
```

Expected output:

```
Alice's grade: 10
Alice is in grade 11
Bob is in grade 11
```

`updateGrade(11)` changed Alice's grade from 10 to 11, while Bob's remained 11 (he was already there, by coincidence). The key point: the method operated on *Alice's* copy of the field — never on Bob's.

---

## 6. Static Methods (Class Methods)

### Definition

A **static method** is a method declared with the `static` keyword. It belongs to the **class**, not to any object. It is a piece of behavior that does not need an object to run — it has no `this` reference and cannot touch any object's instance state.

### Key Properties

- Declared with `static`: `public static int max(int a, int b)`.
- Called via the class name: `Math.max(3, 7)`, `Student.printTotalStudents()`.
- **Cannot** access instance fields or call instance methods directly — there is no object, hence no `this`, hence nothing to read instance data from.
- **Can** access static fields and call other static methods.
- Called on an instance (`someStudent.printTotalStudents()`) compiles but is **bad practice** — it implies object-specific behavior where there is none.

**Analogy: a shared utility in the school workshop.** A bench-mounted saw in a workshop is shared by everyone: you walk up to it and use it; it has no idea *which* student you are, and it does not need to know. `Math.max(...)` is exactly that — a tool you use without creating any object. A personal drill, in contrast, is tuned to its owner — that's an instance method, because it depends on the object's state.

### Code Example

```java
public class Student {
    private String name;
    private int grade;
    private static int studentCount = 0;

    public Student(String name, int grade) {
        this.name = name;
        this.grade = grade;
        studentCount++;
    }

    // STATIC method: reports on the class-wide counter, no object needed
    public static void printTotalStudents() {
        System.out.println("Total students: " + studentCount);
    }

    // STATIC utility method: pure function of its arguments
    public static String formatName(String name) {
        if (name == null || name.isBlank()) {
            return "Unknown";
        }
        return name.trim();
    }

    public static void main(String[] args) {
        Student alice = new Student("Alice", 10);
        Student bob   = new Student("Bob", 11);

        // Static methods are called on the CLASS, not on an object
        Student.printTotalStudents();

        String clean = Student.formatName("   Carol   ");
        System.out.println("Formatted: '" + clean + "'");
    }
}
```

Expected output:

```
Total students: 2
Formatted: 'Carol'
```

`printTotalStudents()` reads only the static field `studentCount`; `formatName()` needs no object state at all — it is a pure function of its parameter. Neither requires an object.

### What Will NOT Compile

A static method has no object to work with. Trying to touch an instance field or call an instance method from a static context is a compile-time error:

```java
public class Student {
    private String name;               // instance field
    private static int studentCount = 0;

    // STATIC method attempting to use instance state — DOES NOT COMPILE
    public static void printName() {
        // COMPILE ERROR: "Cannot make a static reference to the
        // non-static field 'name'"
        System.out.println(name);

        // COMPILE ERROR: "Cannot make a static reference to the
        // non-static method 'display()'"
        display();
    }

    // Even calling an instance method through a local variable is not
    // allowed here, because no Student object exists in this scope:
    public static void tryInstanceAccess() {
        Student s = new Student("Alice", 10);
        // This IS allowed — 's' names a real object:
        s.display();
    }

    public void display() {
        System.out.println(name);
    }
}
```

The compiler rejects `name` and `display()` inside `printName()` because at that point there is *no object* — the static method could be called as `Student.printName()` before any `Student` ever exists. The mental model: **static methods stand outside the objects; instance members live inside them.**

---

## 7. `this` and `static` Interaction

Every **instance** method and constructor receives an implicit reference called `this` — a pointer to the object on which the call was made. **Static** contexts have **no `this`**, because a static member is not invoked "on" any object. There is nothing for `this` to point at.

This single fact explains every rule in this chapter:

- Static methods can't read instance fields → no object → no `this`.
- Static methods can't call instance methods → no receiver object.
- Instance methods *can* access static members → `this` is present, and the class's shared data is always reachable.

### Field Shadowing

`this` also resolves naming collisions. When a constructor parameter has the same name as a field, the parameter **shadows** the field, and `this.field` is the only way to reach the field.

### Code Example: Both in Action

```java
public class BankAccount {
    private String owner;      // instance field
    private double balance;    // instance field

    private static int accountsOpened = 0;   // static field

    // Constructor: 'this' distinguishes fields from parameters (shadowing)
    public BankAccount(String owner, double balance) {
        this.owner = owner;
        this.balance = balance;
        accountsOpened++;
    }

    // Instance method: 'this' is implicit but can be written explicitly
    public void deposit(double amount) {
        this.balance += amount;
    }

    public void printOwner() {
        // 'owner' here is shorthand for 'this.owner'
        System.out.println("Account owned by " + owner);
    }

    // Static method: NO 'this' exists here
    public static void printAccountsOpened() {
        System.out.println("Accounts opened: " + accountsOpened);
        // The following would NOT compile — no 'this' in a static context:
        // System.out.println(owner);
        // printOwner();
    }

    public static void main(String[] args) {
        BankAccount a = new BankAccount("Maria", 1000);
        a.deposit(250);        // call instance method ON an object
        a.printOwner();

        BankAccount.printAccountsOpened();   // call static method ON the class

        // In static main(), you cannot say: printOwner();
        // You must name an object: a.printOwner();
    }
}
```

Expected output:

```
Account owned by Maria
Accounts opened: 1
```

**Summary:** `this` is the object's way of referring to itself. Static members don't belong to any object, so they simply have no `this`.

---

## 8. When to Use Which (Decision Guide)

Every design decision about static vs instance reduces to one question: **whose data is this?**

### Decision Table

| Situation | Use Instance | Use Static | Reason |
|---|---|---|---|
| State that varies per object (name, balance, ID) | ✅ Yes | ❌ No | Each object must carry its own value |
| Shared counter / total across all objects | ❌ No | ✅ Yes | One value must be visible to every object |
| Utility/helper functions (`Math.max`, formatting) | ❌ No | ✅ Yes | Pure logic, no object state required |
| Constants identical for every object (`TAX_RATE`) | ❌ No | ✅ Yes | `static final` — one unchangeable copy |
| Factory methods (`Integer.valueOf(...)`) | ❌ No | ✅ Yes | Produces objects but is not an object's behavior |
| A value that must stay the same across all objects | ❌ No | ✅ Yes | Sharing is the whole point |
| Behavior that reads/writes an object's fields | ✅ Yes | ❌ No | Needs `this` to reach the data |
| Behavior independent of any object | ❌ No | ✅ Yes | No state to bind to |

### Rules of Thumb

- **If each object needs its own value → instance field.** A student's name, an order's amount.
- **If the value is shared by all objects or belongs to the class → static field.** A running count of orders, a global tax rate.
- **If the method reads or writes instance state → instance method.** `calculateTotal()`, `updateGrade(...)`.
- **If the method needs no object state → static method.** `formatName(...)`, `Math.max(...)`.

### Why Is `main` Static?

A Java program starts when the JVM runs `main`. At that instant, **no object of your class exists yet** — the JVM cannot call a non-static `main` on an object it hasn't created. So `main` must be static: it is the entry point that runs before (and then constructs) any objects:

```java
public class App {
    public static void main(String[] args) {
        // No App object exists here. That is exactly why main is static.
        System.out.println("Program starting...");
    }
}
```

### Constants: `static final`

Constants that must be identical for every object are written as `static final` fields in **UPPER_SNAKE_CASE**:

```java
public class Order {
    public static final double TAX_RATE = 0.08;   // shared, unchangeable
    public static final int MAX_ITEMS = 50;
}
```

`final` makes the reference unchangeable; `static` makes it a single shared copy. Together: one shared, read-only value for the whole program.

---

## 9. Real-World Context and Use Cases

The static/instance distinction isn't a classroom curiosity — it powers large Java systems every day.

- **`Math.PI` and `Math.max(...)`** — pure static utility. The `Math` class is never instantiated; you cannot create a `Math` object. Everything is `static` because none of it needs per-object state.

- **The Singleton pattern** — a class that allows exactly one instance uses a `static` field to hold that instance plus a private constructor:

```java
public class Config {
    private static Config instance;          // the single shared instance

    private Config() {}                      // nobody can call `new Config()`

    public static Config getInstance() {     // static: reachable without an object
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }
}
```

- **Object counting / session counters in web apps.** Every time a user logs in, the application increments a shared `static int activeSessions`. Each session object itself is instance data (per user), but the *total* is class-wide.

- **Configuration constants shared app-wide.** Database URLs, feature flags, and default timeouts are `public static final` constants so every component reads the same value from one place.

- **Database connection pools / shared caches.** A connection pool holds a shared set of pooled connections in a static (or singleton-managed) structure. Each *connection* is an instance; the *pool* is shared.

### Complete Runnable Case Study: An `Order` System

This program uses all four member types together: a static shared counter, a static constant, instance fields, an instance method, and a static method.

```java
public class Order {

    // ---- STATIC members (owned by the class) ----
    private static int totalOrders = 0;             // shared counter
    public static final double TAX_RATE = 0.08;     // shared constant

    // ---- INSTANCE members (owned by each order) ----
    private final int orderId;
    private double amount;

    public Order(double amount) {
        this.orderId = ++totalOrders;   // unique ID from the shared counter
        this.amount = amount;
    }

    // Instance method: needs this order's data
    public double calculateTotal() {
        return amount + amount * TAX_RATE;
    }

    // Instance method: prints this order
    public void display() {
        System.out.printf("Order #%d: subtotal $%.2f, total $%.2f%n",
                orderId, amount, calculateTotal());
    }

    // Static methods: no object state needed
    public static int getTotalOrders() {
        return totalOrders;
    }

    public static void resetDailyCount() {
        totalOrders = 0;
    }

    public static void main(String[] args) {
        Order o1 = new Order(100.0);
        Order o2 = new Order(250.0);

        o1.display();
        o2.display();

        System.out.println("Total orders so far: " + Order.getTotalOrders());
        System.out.println("Tax rate: " + Order.TAX_RATE);

        Order.resetDailyCount();
        System.out.println("After reset: " + Order.getTotalOrders());
    }
}
```

Expected console output:

```
Order #1: subtotal $100.00, total $108.00
Order #2: subtotal $250.00, total $270.00
Total orders so far: 2
Tax rate: 0.08
After reset: 0
```

Notice how the pieces fit: each `Order` object carries its own `amount` and gets a unique `orderId`; the class tracks the running total in `totalOrders`; `TAX_RATE` is one shared constant; `calculateTotal()` is instance (it reads `this.amount`); `resetDailyCount()` is static (it mutates only the shared counter).

---

## 10. Common Pitfalls and Interview Questions

### Common Mistakes

1. **Accessing an instance member from a static context.** The most common error: `System.out.println(name)` inside a `static` method. The compiler rejects it — there is no object, so there is no `name`.
2. **Calling static methods through an instance reference.** `someOrder.resetDailyCount()` compiles, but it *lies* about what is happening and confuses maintainers. Always call static members via the class name.
3. **Overusing static.** Making everything static destroys encapsulation: global mutable state is hard to test, easy to corrupt, and impossible to isolate per user or per request.
4. **Assuming static fields are thread-safe.** Static fields are *shared*, not *safe*. Two threads mutating `totalOrders++` concurrently can lose updates unless you synchronize or use atomic types like `AtomicInteger`.
5. **Hiding static fields in subclasses.** A subclass can declare a field with the same name as a parent's static field, "hiding" it. The two fields coexist, and behavior becomes surprising.
6. **Treating static as a speed hack.** "Make it static, it's faster" — static methods do avoid some virtual-dispatch overhead, but premature "optimization" that breaks correct design is never worth it.
7. **Using `static final` for values that should be configurable.** A hard-coded constant can't change at runtime; if a "constant" may differ across deployments, it belongs in configuration, not in `static final`.

### Practice Questions

**Q1.** A class `Car` has `private int speed;` and `private static int carsMade;`. If you create three `Car` objects, how many copies of `speed` and how many copies of `carsMade` exist?

<details>
<summary>Answer</summary>
Three copies of `speed` (one per object) and exactly **one** copy of `carsMade` (shared by the class).
</details>

**Q2.** Can a static method call an instance method? Can an instance method call a static method? Explain.

<details>
<summary>Answer</summary>
A static method **cannot** call an instance method directly — it has no object (no `this`). An instance method **can** call a static method — the shared class data is always available to any object.
</details>

**Q3.** Why must `main` be declared `static`?

<details>
<summary>Answer</summary>
The JVM invokes `main` to start the program, before any objects of the class exist. A static method is callable without an object, which is exactly what the entry point requires.
</details>

**Q4.** You need a method `double add(double a, double b)`. Static or instance? Why?

<details>
<summary>Answer</summary>
**Static.** The method uses no object state — it is a pure function of its parameters, like `Math.max`.
</details>

**Q5.** A `BankAccount` needs `double balance`. Static or instance? Why?

<details>
<summary>Answer</summary>
**Instance.** The balance is per-account data; each account object must hold its own value so accounts don't overwrite each other.
</details>

---

## 11. Key Takeaways / Summary

- A **class** is a blueprint; an **object** is a concrete instance built from it with `new`.
- **Instance fields** live inside each object on the heap — one copy per object.
- **Static fields** live in class metadata — exactly one copy shared by everything.
- **Instance methods** operate on a specific object, have implicit `this`, and can touch both instance and static members.
- **Static methods** belong to the class, have no `this`, and can only touch static members.
- Access static members through the **class name**; access instance members through an **object reference**.
- Decision rule: *per-object data → instance; class-wide data → static; behavior on object state → instance; pure logic → static.*
- `main` is static because it runs before any object exists.
- Constants shared everywhere are `public static final` in `UPPER_SNAKE_CASE`.

### Compact Comparison Table

| Feature | Instance Member | Static Member |
|---|---|---|
| Keyword | none | `static` |
| Owner | an object | the class |
| `this` available? | Yes | No |
| Copies | one per object | one total |
| Access | `object.member` | `ClassName.member` |
| Can access instance fields? | Yes | No |
| Can access static fields? | Yes | Yes |
| Exists without any object? | No | Yes |

---

## 12. Exercises

Work through these in order. Each builds on the last.

**Exercise 1 — Concept recognition (easy).** Read the following class and classify every member as *static* or *instance*. For each static member, state whether it is safe and correct as static:

```java
public class Library {
    private String name;
    private static int booksTotal = 0;
    private static final double FINE_RATE = 0.10;

    public Library(String name) { this.name = name; booksTotal++; }

    public void borrow() { /* ... */ }

    public static String openingHours() { return "9 AM - 9 PM"; }
}
```

> **Hint:** Ask two questions of every member: *Does it need an object to run?* and *Does its value differ per object?* If a field's value must differ per object, static is wrong for it.

**Exercise 2 — Implementation (medium).** Write a `Counter` class with:

- an instance field `int count` and instance methods `increment()` and `getCount()`;
- a static field `int totalIncrements` that counts every increment performed by *any* `Counter` object;
- a static method `getTotalIncrements()`.

In `main`, create two counters, call `increment()` several times on each, and print both the per-object counts and the shared total.

> **Hint:** Remember that the shared counter must be incremented inside `increment()` — every call, no matter which object makes it, adds to the class-wide total.

**Exercise 3 — Design (hard).** Here is a badly designed class where everything is static:

```java
public class User {
    public static String username = "";
    public static int level = 1;

    public static void levelUp() { level++; }
    public static void print() { System.out.println(username + " level " + level); }
}
```

Explain what breaks if two users log in, then refactor it to use instance fields and methods for per-user state while keeping genuinely class-wide data (e.g., `totalUsers`) static. Justify every decision.

> **Hint:** When two objects must coexist with different values, static fields cannot represent them — they all share the same slot. List which fields are *inherently per-user* and which are *inherently global* before you write any code.

---

### Closing Thought

Static and instance are not a style choice — they are a statement about **ownership**. Instance members answer "what does *this* object know?" Static members answer "what does the *class* know?" Get the ownership right, and the syntax, the compiler errors, and the design all fall into place.