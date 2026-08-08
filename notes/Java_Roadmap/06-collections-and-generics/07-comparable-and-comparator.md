# Comparable and Comparator: Sorting Custom Objects in Java

## Table of Contents

1. [Introduction](#1-introduction)
2. [The Sorting Problem with Custom Objects](#2-the-sorting-problem-with-custom-objects)
3. [The `Comparable` Interface](#3-the-comparable-interface)
   - [What Is `Comparable<T>`?](#31-what-is-comparablet)
   - [Implementing `Comparable`](#32-implementing-comparable)
   - [Best Practices & Pitfalls](#33-best-practices--pitfalls)
4. [The `Comparator` Interface](#4-the-comparator-interface)
   - [What Is `Comparator<T>`?](#41-what-is-comparatort)
   - [Creating Comparators](#42-creating-comparators)
   - [Comparator Chaining and Utilities](#43-comparator-chaining-and-utilities)
5. [Putting It All Together — Real-World Case Study](#5-putting-it-all-together--real-world-case-study)
6. [When to Use Which](#6-when-to-use-which)
7. [Common Pitfalls & Interview-Style Questions](#7-common-pitfalls--interview-style-questions)
8. [Exercises](#8-exercises)
9. [Summary](#9-summary)

---

## 1. Introduction

Imagine you are building the payroll system for a company with 10,000 employees. Your boss walks over and asks, "Can you give me a report of everyone sorted by salary?" The next day: "Now sort by name." The week after: "Sort by hire date, and if two people were hired the same day, put the higher-paid person first."

You have a `List<Employee>`, and Java gives you `Collections.sort(...)`. It feels like it should just work. It doesn't.

Here is the core problem: **`Collections.sort` knows how to order numbers, strings, and dates — but it has no idea what an `Employee` is.** Sorting requires a notion of *"before" and "after,"* and for your own classes, only *you* can define that notion. This chapter teaches you the two mechanisms Java provides for exactly this purpose: **`Comparable`** and **`Comparator`**.

By the end of this chapter, you will be able to define a natural order for any class, create multiple ad-hoc orderings for the same class, chain sort criteria together for multi-level sorting, and handle edge cases like `null` values safely. These are everyday skills in real Java codebases — and a favorite topic of interviewers.

---

## 2. The Sorting Problem with Custom Objects

Let's start with the failure. We'll define an `Employee` class that we'll use throughout the entire chapter. Note the fields: `id`, `name`, `salary`, and `hireDate`.

```java
import java.time.LocalDate;

public class Employee {
    private final int id;
    private final String name;
    private final double salary;
    private final LocalDate hireDate;

    public Employee(int id, String name, double salary, LocalDate hireDate) {
        this.id = id;
        this.name = name;
        this.salary = salary;
        this.hireDate = hireDate;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getSalary() { return salary; }
    public LocalDate getHireDate() { return hireDate; }

    @Override
    public String toString() {
        return String.format("Employee{id=%d, name='%s', salary=%.2f, hireDate=%s}",
                id, name, salary, hireDate);
    }
}
```

Now watch what happens when we naively try to sort a list of these objects:

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SortFailDemo {
    public static void main(String[] args) {
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee(1, "Alice", 60000.00, LocalDate.of(2020, 3, 1)));
        employees.add(new Employee(2, "Bob",   50000.00, LocalDate.of(2021, 7, 15)));
        employees.add(new Employee(3, "Carol", 70000.00, LocalDate.of(2019, 1, 10)));

        Collections.sort(employees);  // COMPILE ERROR!
    }
}
```

This code does not compile. Java tells you something like:

```text
error: no suitable method found for sort(List<Employee>)
    method Collections.sort(List<T>) is not applicable
      (inference variable T has incompatible bounds:
         equality constraints: Employee
         lower bounds: Comparable<? super Employee>)
```

The translation: `Collections.sort` requires every element to be **`Comparable`** — that is, to know how it ranks against another object of its own kind. `String`, `Integer`, and `LocalDate` all implement `Comparable`, which is why `List<String>` sorts fine but `List<Employee>` does not.

Conceptually, Java's sorting machinery works like this: while sorting, the algorithm repeatedly asks *"does element A come before element B?"* It gets the answer by calling either a method **on the elements themselves** (`Comparable.compareTo`) or a method **on a separate helper object you supply** (`Comparator.compare`). If neither exists, the question can't be answered, and sorting is impossible. The rest of this chapter is about teaching your objects (or supplying a helper) to answer that question.

---

## 3. The `Comparable` Interface

### 3.1 What Is `Comparable<T>`?

**`Comparable<T>`** is an interface that gives a class a single **natural ordering** — the "default" order in which instances of that class should be sorted. Think of the integers: `5 < 7` is not something we decide per-sort; it's built into the numbers themselves. `Comparable` does the same for your classes: it builds the ordering *into* the class.

The interface declares exactly one method:

```java
public interface Comparable<T> {
    public int compareTo(T o);
}
```

When `a.compareTo(b)` is called, the **sign of the returned `int`** is the entire contract:

| Return value | Meaning |
|---|---|
| **negative** (e.g., `-1`) | `this` object comes **before** the argument `o` |
| **zero** (`0`) | `this` and `o` are considered **equal** for ordering |
| **positive** (e.g., `1`) | `this` object comes **after** the argument `o` |

Think of it as a tug of war: whoever is "less" pulls the answer to the negative side. A single method, three possible verdicts. That's the whole contract — but as we'll see in §3.3, there are important subtleties hiding behind those three verdicts.

### 3.2 Implementing `Comparable`

Let's give `Employee` a natural ordering. The most common choice for an `Employee` is sorting by salary — let's say ascending. We declare the class as `implements Comparable<Employee>` and provide `compareTo`:

```java
import java.time.LocalDate;

public class Employee implements Comparable<Employee> {
    private final int id;
    private final String name;
    private final double salary;
    private final LocalDate hireDate;

    public Employee(int id, String name, double salary, LocalDate hireDate) {
        this.id = id;
        this.name = name;
        this.salary = salary;
        this.hireDate = hireDate;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getSalary() { return salary; }
    public LocalDate getHireDate() { return hireDate; }

    @Override
    public int compareTo(Employee other) {
        return Double.compare(this.salary, other.salary);
    }

    @Override
    public String toString() {
        return String.format("Employee{id=%d, name='%s', salary=%.2f, hireDate=%s}",
                id, name, salary, hireDate);
    }
}
```

Let's walk through the method line by line:

- `@Override` — we are overriding the interface method; the annotation lets the compiler catch typos like `compareTo(Employee other)` where we meant `compareTo(Employee o)`. (Here we chose a descriptive parameter name, `other`.)
- `public int compareTo(Employee other)` — the generic type parameter `<Employee>` means the argument is typed as `Employee`, no casting needed.
- `return Double.compare(this.salary, other.salary);` — this is the heart of it. We delegate to `Double.compare`, a static helper that returns negative/zero/positive exactly as `compareTo` promises. We are *not* writing `(int)(this.salary - other.salary)` — that's a classic bug we'll dissect in §3.3.

Now sorting is a one-liner. Both of these do the same thing:

```java
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SortByComparable {
    public static void main(String[] args) {
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee(3, "Carol", 70000.00, LocalDate.of(2019, 1, 10)));
        employees.add(new Employee(1, "Alice", 60000.00, LocalDate.of(2020, 3, 1)));
        employees.add(new Employee(2, "Bob",   50000.00, LocalDate.of(2021, 7, 15)));

        System.out.println("BEFORE:");
        employees.forEach(System.out::println);

        // Option 1: classic static method
        Collections.sort(employees);
        // Option 2: modern instance method on List
        // employees.sort(null);   // null => use the natural ordering

        System.out.println("\nAFTER (sorted by salary, ascending):");
        employees.forEach(System.out::println);
    }
}
```

Expected output:

```text
BEFORE:
Employee{id=3, name='Carol', salary=70000.00, hireDate=2019-01-10}
Employee{id=1, name='Alice', salary=60000.00, hireDate=2020-03-01}
Employee{id=2, name='Bob', salary=50000.00, hireDate=2021-07-15}

AFTER (sorted by salary, ascending):
Employee{id=2, name='Bob', salary=50000.00, hireDate=2021-07-15}
Employee{id=1, name='Alice', salary=60000.00, hireDate=2020-03-01}
Employee{id=3, name='Carol', salary=70000.00, hireDate=2019-01-10}
```

Note the two idioms: `Collections.sort(list)` is the classic library method, while `list.sort(null)` (added in Java 8) sorts in place using the *natural* order when you pass `null` as the comparator. Both are equivalent here. The key takeaway: **once `Employee` is `Comparable`, every sortable collection in the JDK understands it automatically** — `Arrays.sort`, `TreeSet`, `TreeMap`, `Collections.max`, and so on.

### 3.3 Best Practices & Pitfalls

A `compareTo` that returns the right sign 99% of the time is still a bug if the contract is broken in an edge case. The `Comparable` contract (inherited from the docs) demands three properties for all `a`, `b`, `c`:

- **Consistency with `equals`**: `a.compareTo(b) == 0` should agree with `a.equals(b)`. If they disagree, classes like `TreeSet` (which rely on `compareTo` for both ordering *and* membership) will behave inconsistently with `HashSet` (which relies on `equals`). For example, if two employees have the same salary but different ids, `compareTo` returns `0` but `equals` returns `false` — a `TreeSet` might silently drop one of them.
- **Transitivity**: if `a < b` and `b < c`, then `a < c` must hold. Broken transitivity produces garbage ordering and can even throw `IllegalArgumentException` ("Comparison method violates its general contract!") inside TimSort.
- **Antisymmetry**: `a.compareTo(b)` and `b.compareTo(a)` must return opposite signs (or both zero). Flipping the arguments must flip the verdict.

Now the four classic pitfalls, each with the fix:

**Pitfall 1 — overflow with subtraction.** `a - b` looks clever but overflows for extreme values. `Integer.MAX_VALUE - (-1)` wraps to a negative number, silently reporting the wrong order.

```java
// WRONG — overflow for extreme values
public int compareTo(Employee other) {
    return this.id - other.id;
}

// RIGHT — no overflow
public int compareTo(Employee other) {
    return Integer.compare(this.id, other.id);
}
```

**Pitfall 2 — the same trap with `double`/`float`.** `(int)(a - b)` truncates tiny differences to zero and misbehaves for `NaN`. Always use the boxed-type static helpers: `Double.compare`, `Float.compare`, `Integer.compare`, `Long.compare`, and `Short.compare` / `Byte.compare`.

**Pitfall 3 — `compareTo` must not throw on `null` arguments.** Unlike `equals`, the `compareTo` contract says the argument is never null — but collections can *contain* nulls, and a class like `TreeSet` will happily hand your method a null. Throw a `NullPointerException` yourself, or handle nulls explicitly:

```java
@Override
public int compareTo(Employee other) {
    if (other == null) {
        throw new NullPointerException("Cannot compare Employee to null");
    }
    int nameCmp = this.name.compareTo(other.name);
    if (nameCmp != 0) {
        return nameCmp;
    }
    // nullsFirst/nullsLast (see §4.3) is the modern way to allow nulls.
    return Double.compare(this.salary, other.salary);
}
```

**Pitfall 4 — losing the other fields.** If you compare only by salary, employees with equal salaries become *unorderable* with respect to each other. Break ties by adding a secondary comparison (the field-specific version of `thenComparing`, which we meet properly in §4.3):

```java
@Override
public int compareTo(Employee other) {
    int salaryCmp = Double.compare(this.salary, other.salary);
    if (salaryCmp != 0) {
        return salaryCmp;
    }
    return Integer.compare(this.id, other.id);  // deterministic tie-break
}
```

The mental model: **`compareTo` answers "who wins this duel?"** The answer must be total, deterministic, and consistent — then and only then will every sorting algorithm in the JDK produce trustworthy results.

---

## 4. The `Comparator` Interface

### 4.1 What Is `Comparator<T>`?

`Comparable` works when *you* control the class and *one* ordering is enough. But what if you need to sort the same `Employee` list by name today, by salary tomorrow, and by hire date next week — without touching the `Employee` class? That's the job of **`Comparator<T>`**.

Where `Comparable` is *how an object ranks itself*, **`Comparator` is an external judge** that ranks objects from the outside. The analogy to keep throughout this chapter: with `Comparable`, the contestants score their own matches; with `Comparator`, you bring in an impartial referee — and you may bring in a *different* referee for every contest.

```java
public interface Comparator<T> {
    int compare(T a, T b);
}
```

The contract is identical to `compareTo` in meaning:

| Return value | Meaning |
|---|---|
| **negative** | first argument `a` comes **before** `b` |
| **zero** | `a` and `b` are considered **equal** for ordering |
| **positive** | first argument `a` comes **after** `b` |

The crucial differences from `Comparable`:

- **Zero class modification** — `Employee` stays untouched.
- **Many orderings** — you can define any number of comparators for the same type.
- **Sorting by computed values** — the judge can compare things like `getName().length()` or `salary * 12` without those fields existing on the class.

### 4.2 Creating Comparators

There are four idiomatic ways to create a `Comparator`. All four below produce *the same behavior*: sorting employees by name alphabetically. Let's look at each, then use them.

**Way 1 — a separate named class.** Clear, reusable, and the old-school style. If a comparator is used in many places, giving it a name pays off.

```java
import java.util.Comparator;

public class NameComparator implements Comparator<Employee> {
    @Override
    public int compare(Employee a, Employee b) {
        return a.getName().compareTo(b.getName());
    }
}
```

**Way 2 — an anonymous inner class.** Useful when the comparator is used in exactly one place, and you don't want a separate top-level file.

```java
Comparator<Employee> byName = new Comparator<Employee>() {
    @Override
    public int compare(Employee a, Employee b) {
        return a.getName().compareTo(b.getName());
    }
};
```

**Way 3 — a lambda expression.** Because `Comparator` is a *functional interface* (exactly one abstract method), Java 8 lets you write the comparison logic directly. `Comparator.comparing` is a static factory that builds a comparator from a **key extractor** function.

```java
Comparator<Employee> byName = Comparator.comparing(Employee::getName);
```

**Way 4 — a method reference.** As a further shorthand, if a class already has a method that does exactly the comparison you want, you can reference it directly. For instance, to sort *strings* by their own natural order:

```java
Comparator<String> naturalStringOrder = String::compareTo;
// Which is exactly what Comparator.naturalOrder() returns, by the way.
```

All four are interchangeable at the call site. Here is a complete program demonstrating sorting by name with each of them:

```java
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortByNameDemo {
    public static void main(String[] args) {
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee(1, "Carol", 60000.00, LocalDate.of(2019, 1, 10)));
        employees.add(new Employee(2, "alice", 70000.00, LocalDate.of(2020, 3, 1)));
        employees.add(new Employee(3, "Bob",   50000.00, LocalDate.of(2021, 7, 15)));

        // Way 1: named class
        Collections.sort(employees, new NameComparator());

        // Way 2: anonymous inner class
        // Collections.sort(employees, new Comparator<Employee>() {
        //     @Override
        //     public int compare(Employee a, Employee b) {
        //         return a.getName().compareTo(b.getName());
        //     }
        // });

        // Way 3: lambda via Comparator.comparing  <-- the one we run
        employees.sort(Comparator.comparing(Employee::getName));

        // Way 4: method reference on a String comparator
        // employees.sort(Comparator.comparing(e -> e.getName(), String::compareTo));

        System.out.println("Sorted by name:");
        employees.forEach(System.out::println);
    }
}
```

Expected output:

```text
Sorted by name:
Employee{id=2, name='alice', salary=70000.00, hireDate=2020-03-01}
Employee{id=3, name='Bob', salary=50000.00, hireDate=2021-07-15}
Employee{id=1, name='Carol', salary=60000.00, hireDate=2019-01-10}
```

Notice two things. First, `'alice'` sorts *before* `'Bob'` because `String.compareTo` uses lexicographic order based on the Unicode values of the characters, and lowercase letters have higher code points than uppercase ones. That's why the output looks "wrong" at a glance — if you want case-insensitive sorting, use `String.CASE_INSENSITIVE_ORDER` or `Comparator.comparing(e -> e.getName().toLowerCase())`. Second, all four ways compile to essentially the same comparison logic; prefer the lambda form (Way 3) in modern code because it is short and reads almost like prose: *"sort employees by their name."*

### 4.3 Comparator Chaining and Utilities

The real power of `Comparator` emerges when one criterion isn't enough. Your boss wants employees sorted **by department, then by salary descending, then by name**. This is **multi-level sorting**.

**The naive (wrong) way.** A beginner might try to sort the list several times, once per key:

```java
// WRONG — brittle and order-dependent
employees.sort(Comparator.comparing(Employee::getName));
employees.sort(Comparator.comparingDouble(Employee::getSalary).reversed());
employees.sort(Comparator.comparing(Employee::getDepartment));
```

This is wrong because each sort *destroys* the ordering established by the previous one — the last sort wins outright. The only reason it could ever appear to work is the **stability** of `Collections.sort` (a stable sort preserves the relative order of equal elements), which keeps *some* residual ordering from earlier passes — but that residual behavior is accidental, invisible to readers, and breaks the moment any single sort reorders ties. Never sort repeatedly; **chain** instead.

**The correct way — `thenComparing`.** A comparator chain reads in priority order: first by department, then (among equal departments) by salary descending, then (among equal salaries) by name:

```java
Comparator<Employee> reportOrder =
        Comparator.comparing(Employee::getDepartment)
                  .thenComparing(Comparator.comparingDouble(Employee::getSalary).reversed())
                  .thenComparing(Employee::getName);

employees.sort(reportOrder);
```

Read it aloud: *"compare by department; if tied, compare by salary reversed (highest first); if still tied, compare by name."* Each `thenComparing` only ever decides ties left unresolved by the previous level. This is the single most useful `Comparator` pattern in real code.

For primitive fields there are dedicated helpers that avoid boxing overhead: `thenComparingInt`, `thenComparingDouble`, `thenComparingLong` — and their standalone counterparts `Comparator.comparingInt`, `comparingDouble`, `comparingLong`:

```java
employees.sort(
        Comparator.comparing(Employee::getDepartment)
                  .thenComparingDouble(Employee::getSalary)   // ascending
                  .thenComparing(Employee::getName));
```

**Reversing.** Flip any comparator with `.reversed()`:

```java
employees.sort(Comparator.comparingDouble(Employee::getSalary).reversed()); // highest first
```

And for natural-order comparators, use the static helpers `Comparator.reverseOrder()` (descending natural order) or `Comparator.naturalOrder()` (ascending natural order):

```java
Collections.sort(names, Comparator.reverseOrder()); // equivalent to names.sort(Comparator.reverseOrder())
```

**Handling nulls.** `compareTo` throws on null, but real data has missing values. `nullsFirst(...)` and `nullsLast(...)` wrap any comparator and decide where nulls go:

```java
// Null names sink to the bottom; the rest sort alphabetically.
employees.sort(Comparator.comparing(Employee::getName,
                                    Comparator.nullsLast(String::compareTo)));

// Or, equivalently and more readably:
employees.sort(Comparator.comparing(Employee::getName,
                                    Comparator.nullsLast(Comparator.naturalOrder())));
```

Use `nullsFirst` when nulls represent "missing/unknown" that should lead, `nullsLast` when nulls mean "incomplete" and should trail. Chain them like any other step: `.thenComparing(Comparator.nullsLast(...))`.

**Multi-level with stable sorts.** There is one scenario where *multiple* sorts are legitimate: when each sort is a *single* stable pass and you apply them **in reverse priority order**. The least important criterion is sorted first, the most important last; stability guarantees the earlier orderings survive inside the ties of later ones. This is a classic algorithm-design fact, but it's also exactly the situation `thenComparing` replaces with clearer code — mention the technique for interviews, use chaining in production.

A complete example combining all the utilities:

```java
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChainingDemo {
    public static void main(String[] args) {
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee(1, "Alice", 60000.00, LocalDate.of(2020, 3, 1)));
        employees.add(new Employee(2, "Bob",   50000.00, LocalDate.of(2021, 7, 15)));
        employees.add(new Employee(3, "Carol", 70000.00, LocalDate.of(2019, 1, 10)));
        employees.add(new Employee(4, null,    55000.00, LocalDate.of(2022, 2, 2)));

        employees.sort(
                Comparator.comparing(Employee::getHireDate)                 // 1st: hire date, oldest first
                          .thenComparingDouble(Employee::getSalary).reversed() // 2nd: salary, highest first
                          .thenComparing(Employee::getName,                  // 3rd: name, nulls last
                                         Comparator.nullsLast(String::compareTo)));

        employees.forEach(System.out::println);
    }
}
```

Expected output:

```text
Employee{id=3, name='Carol', salary=70000.00, hireDate=2019-01-10}
Employee{id=1, name='Alice', salary=60000.00, hireDate=2020-03-01}
Employee{id=2, name='Bob', salary=50000.00, hireDate=2021-07-15}
Employee{id=4, name='null', salary=55000.00, hireDate=2022-02-02}
```

(Note: the last line prints `name='null'` because `toString` renders the null name via string formatting; the important part is that the null-named employee was placed *last* by `nullsLast`.)

---

## 5. Putting It All Together — Real-World Case Study

Let's build one self-contained program that exercises everything in this chapter: an **e-commerce product catalog** sorted multiple ways, and an **employee report** using the natural order. This is the exact shape of real production code.

Scenario: a `Product` has a natural order (by price ascending — cheaper first), but the product-listing page sorts by *price, then rating, then name* ad-hoc, while the admin report sorts employees by their natural order (salary).

```java
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// ---------- Domain classes ----------

class Product implements Comparable<Product> {
    private final String name;
    private final double price;
    private final double rating; // 0.0 - 5.0

    public Product(String name, double price, double rating) {
        this.name = name;
        this.price = price;
        this.rating = rating;
    }

    public String getName() { return name; }
    public double getPrice() { return price; }
    public double getRating() { return rating; }

    @Override
    public int compareTo(Product other) {
        // Natural order: ascending price, ties broken by rating, then name.
        int byPrice = Double.compare(this.price, other.price);
        if (byPrice != 0) return byPrice;
        int byRating = Double.compare(this.rating, other.rating);
        if (byRating != 0) return byRating;
        return this.name.compareTo(other.name);
    }

    @Override
    public String toString() {
        return String.format("Product{name='%s', price=%.2f, rating=%.1f}", name, price, rating);
    }
}

// Reuse the Employee class from §3.2 (it implements Comparable<Employee> by salary).

// ---------- Main program ----------

public class CaseStudy {
    public static void main(String[] args) {
        List<Product> catalog = new ArrayList<>();
        catalog.add(new Product("Wireless Mouse",   25.99, 4.2));
        catalog.add(new Product("Mechanical Keyboard", 89.99, 4.8));
        catalog.add(new Product("USB-C Cable",      12.49, 4.5));
        catalog.add(new Product("Laptop Stand",     25.99, 4.6));
        catalog.add(new Product("Mouse Pad",        12.49, 4.1));

        // Product-listing page: price, then rating (highest first), then name.
        Comparator<Product> listingOrder =
                Comparator.comparingDouble(Product::getPrice)
                          .thenComparing(Comparator.comparingDouble(Product::getRating).reversed())
                          .thenComparing(Product::getName);
        catalog.sort(listingOrder);

        System.out.println("=== Catalog (price asc, rating desc, name asc) ===");
        catalog.forEach(System.out::println);

        // Employee report using the natural order defined on Employee.
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee(1, "Alice", 60000.00, LocalDate.of(2020, 3, 1)));
        employees.add(new Employee(2, "Bob",   50000.00, LocalDate.of(2021, 7, 15)));
        employees.add(new Employee(3, "Carol", 70000.00, LocalDate.of(2019, 1, 10)));
        Collections.sort(employees);   // natural order: salary ascending

        System.out.println("\n=== Employee report (natural order: salary asc) ===");
        employees.forEach(System.out::println);
    }
}
```

Expected output:

```text
=== Catalog (price asc, rating desc, name asc) ===
Product{name='Mouse Pad', price=12.49, rating=4.1}
Product{name='USB-C Cable', price=12.49, rating=4.5}
Product{name='Wireless Mouse', price=25.99, rating=4.2}
Product{name='Laptop Stand', price=25.99, rating=4.6}
Product{name='Mechanical Keyboard', price=89.99, rating=4.8}

=== Employee report (natural order: salary asc) ===
Employee{id=2, name='Bob', salary=50000.00, hireDate=2021-07-15}
Employee{id=1, name='Alice', salary=60000.00, hireDate=2020-03-01}
Employee{id=3, name='Carol', salary=70000.00, hireDate=2019-01-10}
```

Study the catalog output carefully — it demonstrates multi-level sorting in action. The two products at \$12.49 are ordered *by rating* (4.5 before 4.1), and the two at \$25.99 likewise (4.6 before 4.2). The two different sort styles coexist in one program: `Comparable` drives the report through `Collections.sort`, while `Comparator` drives the catalog through a chained, ad-hoc listing order. One class, one natural order; an external judge for everything else.

---

## 6. When to Use Which

| Dimension | `Comparable<T>` | `Comparator<T>` |
|---|---|---|
| **Purpose** | Defines the **natural ordering** of a class | Defines an **ad-hoc / external ordering** |
| **Number of orderings** | Exactly **one** per class | **Many** — one per comparator object |
| **Class modification** | **Required** — the class must implement it | **None** — works on any class you can read |
| **Sorting entry point** | `Collections.sort(list)` or `list.sort(null)` | `Collections.sort(list, cmp)` or `list.sort(cmp)` |
| **Method signature** | `int compareTo(T o)` — compares `this` to `o` | `int compare(T a, T b)` — compares two arguments |
| **Typical use** | The "obvious" default order (e.g., ID, price) | Report/view-specific sorts, sort by computed value, descending order, nulls |

**Decision guidance:**

- Use **`Comparable`** when there is exactly **one clear natural order** (a product's price, an employee's ID, a document's timestamp) **and** you control the class source. It makes the class sortable everywhere — lists, `TreeSet`, `TreeMap`, `Collections.max` — with zero extra code.
- Use **`Comparator`** when any of these hold: you **cannot modify** the class (it's from a library), you need **multiple orderings** of the same type, the order depends on **context** (UI sortable columns), you want **descending** order or **null handling**, or you want to sort by a **computed value** that isn't a field.
- A common hybrid: give the class a sensible `Comparable` natural order, and use `Comparator` for everything else.

Many core Java types implement `Comparable`, which is why they "just work" with sorting out of the box: `String` (lexicographic), `Integer`, `Double`, `LocalDate`, `BigDecimal`, and `java.io.File` (path name order). Any time you find yourself writing `if (a.x < b.x) return -1; ...` in user code, stop and ask: should this be a `compareTo` on the class, or a `Comparator.comparing` at the call site?

---

## 7. Common Pitfalls & Interview-Style Questions

**Pitfalls checklist:**

- **Overflow via subtraction** — `a - b` wraps around for `Integer.MIN_VALUE`/`MAX_VALUE`. Use `Integer.compare`, `Double.compare`, etc.
- **`NaN` and floating-point comparisons** — `(int)(a - b)` truncates small differences to `0` and mishandles `NaN`. Always delegate to `Double.compare`/`Float.compare`.
- **`compareTo` inconsistent with `equals`** — classes like `TreeSet` decide *membership* via `compareTo`, so a zero `compareTo` for non-equal objects silently drops elements.
- **Unhandled nulls** — `compareTo` is allowed to throw on null, but be deliberate: wrap with `nullsFirst`/`nullsLast`, or throw a descriptive `NullPointerException` yourself.
- **Mutating the list during sort** — you cannot add/remove elements while a sort is running (`ConcurrentModificationException`); and if elements' sort keys change *between* sorts, previously sorted lists become stale.
- **Off-by-one signs** — returning `1` when you meant "before" inverts the entire order. Test with `a.compareTo(b)` and `b.compareTo(a)` to confirm antisymmetry.
- **Forgetting generics — raw types** — `implements Comparable` (raw) forces an unchecked cast in `compareTo` and defeats type safety. Always write `implements Comparable<Employee>`.
- **Broken transitivity** — e.g., comparing by one field in one branch and a different field in another can violate the contract and crash TimSort with *"Comparison method violates its general contract!"*
- **Repeated sorting instead of chaining** — multiple `sort` calls overwrite each other; chain with `thenComparing` instead.

**Practice questions:**

1. **How do you sort a list of strings by length, then alphabetically?**
   `list.sort(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));` — first a `comparingInt` key, then a `thenComparing` tie-break.

2. **What happens if `compareTo` returns random values?** Sorting becomes nondeterministic: the same list may come out in different orders across runs, violating antisymmetry and transitivity and potentially throwing `IllegalArgumentException` ("violates its general contract") inside the sort algorithm.

3. **`Comparable` vs `Comparator` — which modifies the class?** `Comparable` requires the class itself to implement the interface (modification required, one order). `Comparator` lives outside the class (no modification, unlimited orders).

4. **How do you sort by salary descending using both mechanisms?** With `Comparable`, implement `compareTo` with `Double.compare(other.salary, this.salary)` (swap the operands) — though that bakes "descending" into the natural order, which is a design smell. With `Comparator`: `Comparator.comparingDouble(Employee::getSalary).reversed()`.

5. **Why can't you use `a - b` for comparison?** `int` overflow and floating-point truncation both produce wrong signs. `Integer.MAX_VALUE - (-1)` is negative, so a huge value would sort *before* its smaller sibling.

---

## 8. Exercises

### Exercise 1 (Easy) — Sort Students by Grade
Create a `Student` class with fields `name` and `grade` (`double`). Give it a **natural order** by grade ascending, implement `Comparable<Student>`, and sort a list with `Collections.sort`. Print before/after.

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Student implements Comparable<Student> {
    private final String name;
    private final double grade;

    public Student(String name, double grade) {
        this.name = name;
        this.grade = grade;
    }

    // TODO: implement compareTo by grade ascending, then add a main() that
    // builds a list and sorts it.
}
```

*What your solution should demonstrate:* correct `compareTo` using `Double.compare`, and a working `Collections.sort(list)` on a `Comparable` type.

<details>
<summary>Hint</summary>

`compareTo` should `return Double.compare(this.grade, other.grade);` — never `(int)(this.grade - other.grade)`.
</details>

### Exercise 2 (Medium) — Multi-level Employee Sort
Using the `Employee` class from this chapter, write a `main` that sorts employees with a **single chained comparator**: by hire date descending (newest first), then by salary descending, then by name ascending. Add one employee with a `null` name and ensure it lands **last**.

```java
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Report {
    public static void main(String[] args) {
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee(1, "Alice", 60000.00, LocalDate.of(2020, 3, 1)));
        employees.add(new Employee(2, "Bob",   50000.00, LocalDate.of(2021, 7, 15)));
        employees.add(new Employee(3, "Carol", 70000.00, LocalDate.of(2019, 1, 10)));
        employees.add(new Employee(4, null,    55000.00, LocalDate.of(2022, 2, 2)));
        // TODO: build one comparator and sort employees with it.
    }
}
```

*What your solution should demonstrate:* fluent `thenComparing` chaining, `.reversed()`, and `nullsLast` in a single expression.

<details>
<summary>Hint</summary>

Start with `Comparator.comparing(Employee::getHireDate).reversed()`, then chain `.thenComparingDouble(Employee::getSalary).reversed()` and `.thenComparing(Employee::getName, Comparator.nullsLast(Comparator.naturalOrder()))`.
</details>

### Exercise 3 (Hard) — A Sortable, Stable Event Scheduler
You have a list of `Event` objects (`startTime` as `LocalDateTime`, `title`, and `priority` as `int`, higher = more important). Implement `Comparable<Event>` with a *correct total order*: priority descending, then start time ascending, then title ascending — obeying the transitivity/consistency rules. Then, separately, create a `Comparator` that sorts by start time ascending while keeping events with equal start times in their original list order, and verify that a stable sort preserves that guarantee.

```java
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Event implements Comparable<Event> {
    private final LocalDateTime startTime;
    private final String title;
    private final int priority;

    public Event(LocalDateTime startTime, String title, int priority) {
        this.startTime = startTime;
        this.title = title;
        this.priority = priority;
    }

    // TODO: implement compareTo with the 3-level total order described above.
    // Then write a main() that sorts by start time with a Comparator and
    // demonstrates stability.
}
```

*What your solution should demonstrate:* a multi-field `compareTo` that respects the `Comparable` contract, plus understanding of **sort stability** when using an external `Comparator`.

<details>
<summary>Hint</summary>

For `compareTo`, use `Integer.compare(other.priority, this.priority)` to get descending priority, then `this.startTime.compareTo(other.startTime)`, then `this.title.compareTo(other.title)`. For stability, use `Comparator.comparing(Event::getStartTime)` — `List.sort` is stable, so equal start times keep insertion order.
</details>

---

## 9. Summary

- **`Collections.sort`/`Arrays.sort` only sort objects the JVM knows how to order** — your custom classes need a defined ordering, via `Comparable` or `Comparator`.
- **`Comparable<T>` gives a class one natural ordering**, defined *inside* the class by implementing `int compareTo(T o)`.
- **The sign convention is everything**: negative = *this before other*, zero = *equal*, positive = *this after other*.
- **`Comparator<T>` is an external judge**: `int compare(T a, T b)`, defined *outside* the class, enabling unlimited orderings with no class modification.
- **Create comparators** with a named class, an anonymous class, a lambda, or `Comparator.comparing(Employee::getName)` — prefer the lambda form.
- **Multi-level sorting** is done with `thenComparing` chaining (or `thenComparingInt`, `thenComparingDouble`), never by sorting the list repeatedly.
- **Reverse any comparator** with `.reversed()`; use `Comparator.reverseOrder()` for descending natural order.
- **Handle nulls deliberately** with `nullsFirst(...)` / `nullsLast(...)`.
- **`compareTo` must be consistent with `equals`** and obey transitivity and antisymmetry, or sets and sort algorithms silently misbehave.
- **Never compare with subtraction** (`a - b`); use `Integer.compare`, `Double.compare`, and friends to avoid overflow and truncation.

**Glossary:**

| Term | Definition |
|---|---|
| **`Comparable<T>`** | Interface a class implements to define its single *natural* ordering via `compareTo`. |
| **`Comparator<T>`** | Interface for an external ordering strategy via `compare(a, b)`; no class modification required. |
| **Natural ordering** | The "default" order of a class as defined by its `compareTo` implementation. |
| **Comparator chaining** | Building a multi-key order with `thenComparing`, so each level only breaks ties from the previous level. |
| **Sort stability** | The guarantee that elements considered equal keep their relative order; `List.sort`/`Collections.sort` (TimSort) are stable, which is why chain-comparators and reverse-priority passes work. |
| **Key extractor** | A function passed to `Comparator.comparing` that pulls the field to compare (e.g., `Employee::getSalary`). |