# Set Implementations

## 1. Introduction

Think about the most fundamental rule of a collection of people standing in a room: **no two of them are the same person**. The `Set` interface in the Java Collections Framework is built entirely around that idea. A `Set` is a collection that:

- **Guarantees uniqueness** — no element may appear twice (as determined by `equals()`).
- **Makes no promises about order** — `Set` itself is just a `Collection`; the interface deliberately says nothing about the order in which elements come back from an iterator.
- **Has no indexing** — there is no `get(int index)`. You cannot ask "what is the element at position 3?" because a `Set` is fundamentally unordered at the interface level.

The `Set` interface is *specified*, not *implemented*. The JDK ships three concrete workhorses, each with a distinct personality:

| Implementation | Personality |
|---|---|
| `HashSet` | "Fast but unordered" |
| `LinkedHashSet` | "Fast and remembers insertion order" |
| `TreeSet` | "Sorted and navigable" |

Here is the preview table you will internalize by the end of this chapter:

| Implementation | Underlying structure | Ordering | add/remove/contains | Use case |
|---|---|---|---|---|
| `HashSet` | `HashMap` (hash table) | Unordered | O(1) average | Fast membership testing & dedup |
| `LinkedHashSet` | `HashMap` + doubly-linked list | Insertion order | O(1) average | Ordered-unique streams, LRU-ish caches |
| `TreeSet` | Red-black tree | Sorted (Comparable/Comparator) | O(log n) | Range queries, leaderboards, sorted data |

### Learning Objectives

By the end of this chapter, you will be able to:

1. Explain the core contract of the `Set` interface (uniqueness via `equals`, no order guarantees, no indexing).
2. Describe the underlying data structure of each of `HashSet`, `LinkedHashSet`, and `TreeSet`, and justify its time complexity.
3. Apply the `hashCode()`/`equals()` contract correctly and diagnose a bug caused by violating it.
4. Use `Comparator` and `Comparable` to control `TreeSet` ordering, including lambda-based comparators.
5. Perform navigational and range queries with the `NavigableSet` methods (`floor`, `ceiling`, `subSet`, `headSet`, `tailSet`, etc.).
6. Choose the correct `Set` implementation given a concrete performance and ordering requirement.
7. Avoid the classic pitfalls (mutable keys, null handling, tuning, misuse of iteration order) in your own code.

---

## 2. The Set Interface in Java

The `Set<E>` interface extends `Collection<E>`. Its core methods are few but load-bearing:

```java
public interface Set<E> extends Collection<E> {
    boolean add(E e);        // returns true if e was NOT already present
    boolean remove(Object o); // returns true if an element was removed
    boolean contains(Object o);
    int size();
    boolean isEmpty();
    void clear();
    Iterator<E> iterator();
}
```

The uniqueness contract is enforced at the door: `add(e)` returns `true` if the element was added (i.e., it was not already present) and `false` if it was already a member — the set is left unchanged in that case.

### The Contract: Uniqueness Through Equality

A `Set` never holds two elements `a` and `b` such that `a.equals(b)` is `true`. This is the **fundamental contract**. That one sentence has a huge consequence: whatever notion of equality your elements use is exactly what your `Set` uses for uniqueness. Two `Person` objects with the same ID are "the same" if and only if `Person.equals()` says so.

For hash-based sets (`HashSet`, `LinkedHashSet`) there is a second requirement: the **`hashCode()`/`equals()` contract** (covered in depth in Section 3.3). For `TreeSet`, uniqueness is judged not by `equals()` but by the **comparison result** — two elements that compare equal are treated as duplicates.

### What "Ordered" Actually Means

The `Set` interface promises nothing about iteration order. In practice you get one of three flavors:

| Flavor | Meaning | Example |
|---|---|---|
| **Unordered** | No meaningful order; the iterator order is an implementation detail | `HashSet` |
| **Insertion-ordered** | Elements come back in the order they were first added | `LinkedHashSet` |
| **Sorted** | Elements come back in natural (`Comparable`) or comparator order | `TreeSet` |

### The List → Set Deduplication Idiom

The single most common real-world use of `Set` is deduplication: take a `List`, pour it into a `Set`, and the duplicates evaporate.

```java
var words = List.of("apple", "banana", "apple", "cherry", "banana");

Set<String> unique = new HashSet<>(words);   // pour List into Set
System.out.println(unique);                  // [banana, cherry, apple]  -- order NOT guaranteed
System.out.println(words.size());            // 5
System.out.println(unique.size());           // 3
```

> **Analogy:** The `List` is your inbox full of duplicate invitations; the `Set` is the guest list at the door — you only get added to the list once, no matter how many copies of the invitation you wave around.

---

## 3. HashSet

### 3.1 Overview & Underlying Structure

`HashSet` is the workhorse. Behind the curtain it is literally a `HashMap` with the elements stored as *keys* and a shared dummy value (`PRESENT`) as the value:

```java
private transient HashMap<E,Object> map;   // "PRESENT" is the dummy value
```

The power comes from the **hash table**: an array of **buckets**, where each bucket holds the elements that hash to that index.

- A **hash function** (via `hashCode()`) computes a bucket index for an element.
- Two different elements can land in the same bucket — a **collision**. Since Java 8, each bucket is a *linked list* that automatically **upgrades to a red-black tree** once a single bucket exceeds ~8 entries (and the table is large enough), keeping worst-case degradation in check.

> **Analogy:** A `HashSet` is a coat-check room with numbered cubbyholes. The attendant computes your cubbyhole from your name with a formula (your `hashCode()`), hangs your coat, and remembers nothing else. When you return, the same formula leads instantly to your coat. The occasional collision? Two guests whose names map to the same cubbyhole — they hang their coats back-to-back in the same slot, and finding yours among the two is still fast. With the whole room of cubbyholes, "is this coat here?" is nearly instantaneous regardless of how many coats exist.

### 3.2 Performance Characteristics

Because the table spreads elements across buckets and lookups jump straight to one bucket, the average case is beautifully fast:

| Operation | Average case | Worst case |
|---|---|---|
| `add` | O(1) | O(n) (degenerate hashing or tree rebalance) |
| `remove` | O(1) | O(n) |
| `contains` | O(1) | O(n) |
| `size` | O(1) | O(1) |
| iteration | O(n) | O(n) |

The **worst case** is the poison pill: if every element's `hashCode()` returns the same constant (say `42`), every element lands in one bucket, and every operation degenerates to a linear scan — O(n). A terrible `hashCode()` turns your "fast" set into a slow list. The Java 8+ tree-ification of large buckets softens this to O(log n) in the worst case, but you should never rely on it.

### 3.3 The Critical hashCode()/equals() Contract

This is the single most important rule in this chapter:

> **If two objects are `equals()`, they must have the same `hashCode()`. And if you override either `equals()` or `hashCode()`, you must override both.**

Why? Because `HashSet` first finds the *bucket* using `hashCode()`, then scans the bucket using `equals()`. If equal objects hash to different buckets, the set will never find the duplicate — and you will happily add the "same" object twice.

**Incorrect pair** — equal by ID but hashes are ignored:

```java
final class Employee {
    private final String id;
    Employee(String id) { this.id = id; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee e)) return false;
        return id.equals(e.id);     // equality by id...
    }
    // hashCode() NOT overridden -> inherited Object.hashCode() differs per instance!
}

var set = new HashSet<Employee>();
set.add(new Employee("E100"));
System.out.println(set.contains(new Employee("E100"))); // false !!!
System.out.println(set.size());                          // 2 !!!
```

**Correct pair:**

```java
final class Employee {
    private final String id;
    Employee(String id) { this.id = id; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee e)) return false;
        return id.equals(e.id);
    }

    @Override public int hashCode() {
        return id.hashCode();          // equal ids -> equal hash. Done.
    }
}

var set = new HashSet<Employee>();
set.add(new Employee("E100"));
System.out.println(set.contains(new Employee("E100"))); // true
System.out.println(set.size());                          // 1
```

Note the additional requirement: **`hashCode()` must be stable** for as long as the object is in the set (see Pitfall #2 in Section 7).

### 3.4 Code Examples

Creating, testing membership, and iterating — noting that order is *not* guaranteed:

```java
Set<String> fruits = new HashSet<>();
fruits.add("apple");
fruits.add("banana");
fruits.add("apple");      // duplicate -> false, no change
fruits.add("cherry");

System.out.println(fruits.size());             // 3
System.out.println(fruits.contains("banana")); // true
System.out.println(fruits.contains("date"));   // false

fruits.remove("banana");
System.out.println(fruits.contains("banana")); // false

// Iteration order is NOT insertion order and NOT sorted:
for (String f : fruits) {
    System.out.println(f);   // e.g. "cherry", "apple" — unpredictable
}

fruits.clear();
System.out.println(fruits.isEmpty());          // true
```

**Tuning constructors.** You can pre-size the table to avoid the cost of repeated rehashing when you know the data volume:

```java
// loadFactor 0.75 (default): table resizes when 75% full.
Set<Long> ids = new HashSet<>(1_000_000);                  // initial capacity ~1M
Set<Long> ids2 = new HashSet<>(1_000_000, 0.9f);           // trade more collisions for fewer resizes
```

> **When to tune:** If you know you'll insert roughly *N* elements and you care about throughput, size the initial capacity to roughly `N / loadFactor`. For `N = 1_000_000` and load factor `0.75`, use initial capacity ≈ 1,333,333 (the default resize doubling will get you there anyway, but pre-sizing skips the intermediate rehashes).

### 3.5 Strengths, Weaknesses, Use Cases

**Real-world use cases:**
- **Deduplication** — collapsing duplicate rows, repeated API responses, or log lines.
- **Fast membership testing** — blocking spam email addresses: for each incoming message, `blocked.contains(sender)` in O(1).
- **Tracking seen IDs in a stream** — e.g., "has this order ID been processed already?" to make a pipeline idempotent.

| Strengths | Weaknesses |
|---|---|
| O(1) average `add`/`remove`/`contains` | No ordering guarantees at all |
| Simple, ubiquitous API | Memory overhead of the hash table |
| Excellent for membership tests | Degrades to O(n) with a broken `hashCode()` |
| `null` is allowed (one `null` max) | Iteration order is unstable across JVMs/versions |

---

## 4. LinkedHashSet

### 4.1 Overview & Underlying Structure

`LinkedHashSet` is `HashSet` **plus one extra bookkeeping layer**: a doubly-linked list that threads through all entries in **insertion order**. Every `add` is still O(1) — the same hash-table mechanics — but the set also records the chronological chain of entries so that iteration can replay the exact order of first insertion.

> **Analogy:** Back to the party. `HashSet` is the coat-check room — fast to find a coat, but the coats are hung wherever the formula put them. `LinkedHashSet` is a *numbered guest list*: each arriving guest is handed a ticket with the next number. You can still find a guest instantly by name (the hash table), *and* the party planner can read off the list in the exact order everyone arrived.

### 4.2 Ordering Behavior

- **Insertion order** means the order elements were *first* added.
- **Re-adding an existing element does not change its position.** Adding `"apple"` again, when `"apple"` was added first, keeps `"apple"` first.

```java
Set<String> playlist = new LinkedHashSet<>();
playlist.add("Song A");
playlist.add("Song B");
playlist.add("Song A");   // duplicate -> ignored, order unchanged
playlist.add("Song C");

for (String s : playlist) {
    System.out.println(s);   // Song A, Song B, Song C  -- insertion order preserved!
}
```

The output is deterministic: `Song A`, `Song B`, `Song C`. With a plain `HashSet`, the same code could print the songs in any order.

### 4.3 Performance Characteristics

The performance profile is essentially identical to `HashSet`, with one extra cost:

| | `HashSet` | `LinkedHashSet` |
|---|---|---|
| `add` / `remove` / `contains` | O(1) average | O(1) average |
| Iteration | O(n) | O(n) |
| **Memory per element** | table slot + entry | table slot + entry + **next/prev pointers of the linked list** |
| Ordering | none | insertion order |

The memory overhead of the doubly-linked list (two extra references per entry) is the price you pay for order. If you never iterate and never care about order, `HashSet` is strictly cheaper. If you need both speed *and* order, `LinkedHashSet` is the winner.

### 4.4 Use Cases

- **"Recently viewed items"** — keep a bounded, unique, most-recently-seen list (pair with iteration and remove-oldest logic for an LRU-style cache).
- **Unique tokens in first-seen order** — e.g., reading a file and keeping the first occurrence of each word.

```java
// Preserve first-seen order of unique tokens in a stream of text.
String text = "the quick brown fox jumps over the lazy dog and the fox again";

Set<String> seen = new LinkedHashSet<>();
for (String token : text.split(" ")) {
    seen.add(token);            // duplicates silently skipped, order kept
}

System.out.println(seen);
// [the, quick, brown, fox, jumps, over, lazy, dog, and]
// Note: "the" appears first (not last), because LinkedHashSet keeps FIRST insertion.
```

This is the classic answer to "deduplicate but keep original order" — a plain `HashSet` would scramble it, and a `List` would keep duplicates.

> **Real-world context:** When building a "similar products you viewed" widget, you want unique product IDs in the order the user browsed them. `LinkedHashSet` gives you that in one line, with O(1) membership checks to avoid re-adding items the user already saw.

---

## 5. TreeSet

### 5.1 Overview & Underlying Structure

`TreeSet` abandons hashing entirely. Its backbone is a **red-black tree**: a self-balancing binary search tree that keeps elements **sorted at all times**. Because the tree stays balanced (the red-black invariants guarantee height ≈ log₂n), every operation walks at most O(log n) levels.

"Navigable" means you can move around the sorted structure like a cursor: find the *next* element, the *closest higher* one, the elements *between* two bounds. `TreeSet` implements `NavigableSet<E>` (which extends `SortedSet<E>`), so all of these superpowers are part of the API.

> **Analogy:** A `TreeSet` is a printed dictionary or phone book. Finding a word is fast because the book is alphabetized and you can binary-search it (logarithmic, not linear). And because it's a *book*, you can ask questions a coat-check room can't: "What word comes right after 'monkey'?" ("monotony"), "What's the last entry before 'zebra'?" — these are `higher()` and `lower()`. A `HashSet` can only answer one question: "Is it here or not?"

### 5.2 Ordering: Comparable vs Comparator

Elements must be *mutually comparable*. Either:

1. **Natural ordering** — the element type implements `Comparable<T>` (as `String`, `Integer`, etc. do), or
2. **Custom ordering** — you supply a `Comparator<T>` at construction time.

If you throw elements into a `TreeSet` that are neither comparable to each other nor supported by a provided `Comparator`, you get a `ClassCastException` the moment the set tries to compare two of them.

**Natural ordering with `String`:**

```java
Set<String> names = new TreeSet<>();
names.add("Zoe");
names.add("Ada");
names.add("Max");
System.out.println(names);   // [Ada, Max, Zoe]  -- alphabetically sorted, always
```

**Custom ordering with lambdas** — sort `Employee`s by salary descending:

```java
Set<Employee> bySalary = new TreeSet<>(
        (e1, e2) -> Double.compare(e2.salary(), e1.salary())   // descending
);
bySalary.add(new Employee("Ada",   90_000));
bySalary.add(new Employee("Max",  120_000));
bySalary.add(new Employee("Zoe",  110_000));

System.out.println(bySalary);  // Max, Zoe, Ada  -- highest salary first
```

**A subtle trap:** `TreeSet` deduplicates by *comparison*, not by `equals()`. If two `Employee`s are different people with the same salary, the comparator above treats them as the same element and one silently vanishes. When using a `Comparator`, make sure it returns `0` *only* for elements you truly consider duplicates — typically by adding a distinguishing tie-breaker (e.g., compare `id` after comparing salary).

```java
Set<Employee> bySalary = new TreeSet<>(
        (e1, e2) -> {
            int bySal = Double.compare(e2.salary(), e1.salary());
            return bySal != 0 ? bySal : e1.id().compareTo(e2.id());  // tie-break by id
        }
);
```

### 5.3 Performance Characteristics

| Operation | `TreeSet` | `HashSet` |
|---|---|---|
| `add` | O(log n) | O(1) average |
| `remove` | O(log n) | O(1) average |
| `contains` | O(log n) | O(1) average |
| iteration | O(n) sorted | O(n) unsorted |
| navigational queries (`floor`/`ceiling`) | O(log n) | not supported |

The log factor is negligible for modest sizes but becomes visible at millions of elements: inserting into a `TreeSet` of 1 million elements costs ~20 comparisons per insert, versus ~1 hash computation for `HashSet`. You trade raw speed for **sortedness and range queries** — the exact trade you'll make consciously once you know it.

### 5.4 NavigableSet Operations

Here is the full navigational toolkit, demonstrated on a set of exam scores:

```java
NavigableSet<Integer> scores = new TreeSet<>(Set.of(55, 62, 70, 73, 78, 81, 85, 92, 97));

scores.first();                // 55  -- smallest element
scores.last();                 // 97  -- largest element

scores.floor(80);              // 78  -- greatest element <= 80
scores.ceiling(80);            // 81  -- smallest element >= 80
scores.lower(73);              // 70  -- greatest element < 73
scores.higher(73);             // 78  -- smallest element > 73

scores.pollFirst();            // removes & returns 55
scores.pollLast();             // removes & returns 97

// Range query: every score in [70, 90)
NavigableSet<Integer> range = scores.subSet(70, true, 90, false);
System.out.println(range);     // [70, 73, 78, 81, 85]

// Prefix view: everything strictly below 80
System.out.println(scores.headSet(80));      // [62, 70, 73, 78]

// Suffix view: everything >= 85
System.out.println(scores.tailSet(85));      // [85, 92]

// All scores between 70 and 90, inclusive of both ends:
NavigableSet<Integer> passRange = scores.subSet(70, true, 90, true);
System.out.println(passRange);   // [70, 73, 78, 81, 85]
```

Note that `subSet`, `headSet`, and `tailSet` return **views** backed by the original set: changes to the view (like `add`/`remove`) reflect in the original, and adding an element outside the view's bounds throws `IllegalArgumentException`.

### 5.5 Use Cases

- **Range queries** — "all transactions between date X and Y", "all scores in the pass band".
- **Leaderboards** — sorted scores with instant `first()`/`last()`.
- **Sorted unique data** — unique words in alphabetical order, sorted tags.
- **Interval scheduling** — "next available slot": `ceiling()` finds the next free time ≥ a deadline.
- **"Next available" lookups** — allocating the smallest free port ≥ a requested number.

**Real-world problem → solution:**

> **Problem:** A room-booking system holds a set of occupied meeting-room numbers. When someone requests "a room from 400 upward," we need the *smallest free room number ≥ 400*, and we must reserve it.

```java
NavigableSet<Integer> occupied = new TreeSet<>();
occupied.add(402);
occupied.add(405);

int requested = 400;
Integer next = occupied.ceiling(requested);      // 402 -> taken!

int room;
if (next == null) {
    room = requested;                            // no competition
} else {
    // rooms 400 and 401 are free; 402 is taken.
    // Smallest free room >= 400 that is NOT occupied:
    room = (next > requested) ? requested : next + 1;
}
occupied.add(room);
System.out.println("Reserved room " + room);     // Reserved room 400
```

Without `ceiling()`, you'd scan a list — O(n). With `TreeSet`, every such query is O(log n) and the answer is always sorted. This is exactly why `TreeSet` lives at the heart of schedulers, routing tables, and memory allocators.

---

## 6. Comparing the Three Implementations (Decision Guide)

### Comprehensive Comparison Table

| Criterion | `HashSet` | `LinkedHashSet` | `TreeSet` |
|---|---|---|---|
| Underlying structure | `HashMap` (hash table) | `HashMap` + doubly-linked list | Red-black tree |
| Ordering | Unordered | Insertion order | Sorted (natural/Comparator) |
| Duplicate handling | via `equals()`/`hashCode()` | via `equals()`/`hashCode()` | via `compareTo()`/`Comparator` (== 0) |
| `add` | O(1) average | O(1) average | O(log n) |
| `remove` | O(1) average | O(1) average | O(log n) |
| `contains` | O(1) average | O(1) average | O(log n) |
| Memory | table + entries | + linked-list pointers per entry | tree nodes (parent/child refs) |
| `null` allowed? | Yes (one) | Yes (one) | **No** (NPE on compare) |
| Iteration order guarantee | None | Insertion order (guaranteed) | Sorted order (guaranteed) |
| Navigable/range queries | No | No | Yes (`floor`, `ceiling`, `subSet`, …) |
| Thread-safe? | No | No | No |

**On thread-safety:** none of the three is thread-safe. If a `Set` is shared across threads, wrap it:

```java
Set<String> sync = Collections.synchronizedSet(new HashSet<>());
```

or, for read-heavy, rarely-written data, use the concurrent copy-on-write variant:

```java
Set<String> cow = new CopyOnWriteArraySet<>();   // great for listener sets
```

### Decision Flowchart

```
                Do you need elements in some guaranteed order?
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
       Do you need SORTED order         Do you need INSERTION order
       (range queries, floor/ceiling,   (first-seen dedup, ordered caches,
        leaderboards)?                  LRU-ish iteration)?
              │                               │
       ┌──────┴─────┐                  ┌──────┴──────┐
       ▼            ▼                  ▼             ▼
     Yes           No             Yes              No
      │             │               │               │
      ▼             ▼               ▼               ▼
   TreeSet      LinkedHashSet    LinkedHashSet    HashSet
   (sorted)     (insertion-order) (insertion-order) (fastest,
                  │              + fast             no order)
                  └───────────────┘
```

Reading it as a plain-English checklist:

1. **Do you need sorted order or range queries?** → `TreeSet`.
2. **Do you need insertion order?** → `LinkedHashSet`.
3. **Anything else (just fast uniqueness)?** → `HashSet`.

### Quick-Reference Cheat Sheet

| Implementation | One-liner |
|---|---|
| `HashSet` | "Fastest, no order — use for membership tests and dedup." |
| `LinkedHashSet` | "Hash speed with first-seen order — use for ordered unique data." |
| `TreeSet` | "Sorted and navigable — use for ranges and ordered traversal." |

---

## 7. Common Pitfalls & Best Practices

### Pitfall Table

| # | The mistake | Why it happens | The fix |
|---|---|---|---|
| 1 | Overriding `equals()` without `hashCode()` | Equal objects scatter to different buckets; duplicates sneak in | Override both together, using the same fields |
| 2 | Using mutable elements in a `Set` | `hashCode()` changes after insertion; lookups hit the wrong bucket; `TreeSet` ordering corrupts | Prefer immutable elements (`record`, `String`, `Integer`); never mutate elements while stored |
| 3 | Trusting `HashSet` iteration order | Hash iteration depends on capacity, hash codes, and JVM internals | Use `LinkedHashSet` if order matters; sort explicitly otherwise |
| 4 | Adding non-comparable types to `TreeSet` | No natural order exists; comparison throws `ClassCastException` | Implement `Comparable` or supply a `Comparator` |
| 5 | Using `contains` for "closest value" problems | `contains` only answers yes/no, not "how close" | Use `floor`/`ceiling`/`higher`/`lower` for nearest-neighbor semantics |
| 6 | Ignoring initial capacity for large `HashSet`s | Repeated resizing/rehashing wastes time | Pre-size with `new HashSet<>(expectedN / loadFactor)` |
| 7 | Assuming `null` works everywhere | `HashSet`/`LinkedHashSet` allow one `null`; `TreeSet` throws `NullPointerException` | Know your implementation; guard inputs if `null` is possible |
| 8 | Using `equals`-based uniqueness when you need comparison-based uniqueness | `TreeSet` dedups by comparison, so two "different" equal-salary objects collapse | Design the `Comparator` with tie-breakers |

### Code Snippets: Mistake → Fix

**Pitfall 1 — forgetting `hashCode()`:**

```java
// BAD: equals overridden, hashCode NOT
// set.contains(new Item(...)) randomly returns false
class Item { String sku; /* equals by sku, no hashCode() */ }

// FIX
class Item {
    String sku;
    @Override public boolean equals(Object o) { /* ...by sku... */ }
    @Override public int hashCode() { return Objects.hashCode(sku); }
}
```

**Pitfall 2 — mutable keys break the set:**

```java
// BAD: mutate after insertion
var set = new HashSet<StringBuilder>();
var sb = new StringBuilder("abc");
set.add(sb);
sb.append("def");                 // hashCode changes!
System.out.println(set.contains(new StringBuilder("abcdef"))); // unpredictable

// FIX: use immutable keys
var fixed = new HashSet<String>();
fixed.add("abc");                 // String is immutable; safe forever
```

**Pitfall 3 — "sorted-looking" HashSet output is an illusion:**

```java
// BAD: relies on accidental order
Set<Integer> s = new HashSet<>(List.of(5, 3, 1, 4, 2));

// FIX: use TreeSet for guaranteed sorted order
Set<Integer> sorted = new TreeSet<>(List.of(5, 3, 1, 4, 2));
System.out.println(sorted);       // [1, 2, 3, 4, 5]
```

**Pitfall 4 — TreeSet with non-comparable elements:**

```java
// BAD: throws ClassCastException when TreeSet compares two Widgets
Set<Widget> set = new TreeSet<>();   // Widget does not implement Comparable

// FIX: supply a Comparator
Set<Widget> set = new TreeSet<>((a, b) -> a.weight().compareTo(b.weight()));
```

**Pitfall 5 — nearest value ≠ membership test:**

```java
// BAD: "is there a score of exactly 80?" — misses the real question
if (scores.contains(80)) { ... }

// FIX: "what is the best score above 80?"
int nextHigher = scores.ceiling(81);   // O(log n), answers the nearest query
```

**Pitfall 6 — nulls:**

```java
// HashSet: one null allowed (fine)
Set<String> h = new HashSet<>();
h.add(null);                         // ok

// TreeSet: null throws NullPointerException on comparison
Set<String> t = new TreeSet<>();
t.add(null);                         // NullPointerException!
```

### Best Practices

1. **Choose by behavior, not by name.** "I need a Set" is not enough — ask *which order* and *which operations* before picking `HashSet` vs `LinkedHashSet` vs `TreeSet`.
2. **Program to interfaces.** `Set<E> s = new HashSet<>();` — and use `NavigableSet<E>` (not `TreeSet`) when you need navigational methods, so the declaration documents your intent.
3. **Prefer immutable elements.** `record`s, `String`, `Integer`, `LocalDate`. Mutable elements are a latent bug.
4. **Let the JDK do the work.** Use `Set.copyOf(list)` or `new HashSet<>(list)` for dedup; don't hand-roll loops.
5. **Benchmark before "optimizing."** For < ~10k elements, the difference between O(1) and O(log n) is usually noise; clarity beats micro-tuning.

---

## 8. Summary

**Key points:**

- A `Set` guarantees **uniqueness**, promises **no order** at the interface level, and has **no indexing**.
- Uniqueness is driven by `equals()`/`hashCode()` for hash-based sets and by comparison for `TreeSet`.
- `HashSet` — O(1) average operations, unordered, backed by a `HashMap`; the default choice for fast membership and dedup.
- `LinkedHashSet` — same speed plus **insertion order**, at the cost of linked-list memory overhead.
- `TreeSet` — O(log n) operations, always **sorted**, backed by a red-black tree, with full `NavigableSet` range-query power.
- None of the three is thread-safe; synchronize or use concurrent variants when needed.
- The `hashCode()`/`equals()` contract and immutable elements are the two rules that keep hash-based sets correct.

**The one-liner takeaway for each:**

- **HashSet** — "Fast and unordered."
- **LinkedHashSet** — "Fast and remembers insertion order."
- **TreeSet** — "Sorted and navigable."

### Self-Check Exercise

**Short answer:**

1. Why can a `Set` have no `get(int index)` method, and what contract does it enforce instead?
2. What is the worst-case time complexity of `HashSet.contains` and what causes it?
3. Why does re-adding an existing element to a `LinkedHashSet` not change its position?
4. What exception can you get from a `TreeSet` when its elements are not mutually comparable, and when exactly does it surface?
5. Which `NavigableSet` method returns the greatest element strictly less than a given value, and what is its time complexity?

**Code-writing:** Write a method `List<String> uniqueInOrder(List<String> input)` that removes duplicates from the list while preserving the order of first appearance. It must run in O(n) average time.

---

### Answer Key

**1.** The `Set` interface is specified as a mathematical set: unique elements, no meaningful order, no positional access. Adding `get(int index)` would require an order guarantee that the interface deliberately does not make. Instead, `Set` enforces the *uniqueness* contract via `add` returning `false` for duplicates.

**2.** O(n). A degenerate `hashCode()` (e.g., constant for all objects) forces every element into a single bucket, collapsing the hash table into a linear list. Java 8+ mitigates very large buckets by converting them to red-black trees (O(log n) worst case), but a broken hash code is still a bug to fix, not a corner case to rely on.

**3.** `LinkedHashSet` records the order of *first* insertion. Re-adding an existing element is a no-op: the element is already present, so the set is left unchanged — including its position in the linked list.

**4.** `ClassCastException`, thrown at the moment the set first needs to compare two elements (typically during the first `add` when the second element arrives, or during an iteration that compares adjacent elements).

**5.** `lower(E e)` returns the greatest element strictly less than `e`; it runs in O(log n). (`floor` returns the greatest element *less than or equal to* `e`.)

**Sample solution for the code-writing exercise:**

```java
List<String> uniqueInOrder(List<String> input) {
    Set<String> seen = new LinkedHashSet<>(input);
    // Wait -- that's too easy. The honest "dedup preserving order" answer:
    Set<String> seenTokens = new LinkedHashSet<>();
    for (String s : input) {
        seenTokens.add(s);
    }
    return List.copyOf(seenTokens);   // or new ArrayList<>(seenTokens)
}
```

`LinkedHashSet` is the star of this exercise: its O(1) `add` makes the whole loop O(n), and its insertion-order guarantee preserves first-seen ordering automatically.

---

You now have the complete mental model: a `Set` is about uniqueness, and the three implementations are three ways of paying for that uniqueness — with speed, with order, or with both plus navigability. Choose deliberately, and your collections will serve you.