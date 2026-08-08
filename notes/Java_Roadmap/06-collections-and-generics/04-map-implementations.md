# map implementations in Java

> **Note to the reader:** This chapter assumes you are comfortable with Java classes, interfaces, and generics. If you can write `List<String> names = new ArrayList<>();` without hesitation, you are ready. Everything else you need about the `Map` family will be built from the ground up here.

---

## Introduction

Few data structures in Java are as universally useful as the **map**. A map is an object that associates **keys** with **values**: given a key, the map can look up the value bound to it, store a new binding, or remove an existing one. This key→value association appears everywhere in computing:

- A **dictionary** maps words to their definitions.
- A **phone book** maps names to phone numbers.
- A **library catalog** maps ISBNs to the physical location of a book.
- A **configuration store** maps setting names to their values.
- An **in-memory cache** maps requests to computed results.

In Java, this concept is captured by the **`Map<K, V>` interface** in the `java.util` package, where `K` is the type of the keys and `V` is the type of the values. A `Map<K, V>` is a collection of *pairs* — formally called **entries** — with the crucial constraint that **every key is unique**. If you `put` a value under a key that already exists, the old value is replaced. You can think of a map as a function from keys to values: one key always points to at most one value.

Here is what you will learn in this chapter:

- The core contract and methods of the `Map<K, V>` interface.
- The four classic implementations: **`HashMap`**, **`LinkedHashMap`**, **`TreeMap`**, and the legacy **`Hashtable`**.
- How each implementation works internally, why it behaves the way it does, and what it costs in time and memory.
- When each implementation is the right choice — and when it is a mistake.
- The `hashCode`/`equals` contract and why violating it silently breaks maps.
- Thread-safety concerns, common pitfalls, and modern best practices.

By the end, you will not just know *how* to use maps; you will understand *which* map to pick for a given problem and *why*.

---

## The Map Interface Fundamentals

### The Map Interface

The `Map<K, V>` interface declares a contract that every map implementation must honor. The most important methods are:

| Method | Behavior |
| --- | --- |
| `V put(K key, V value)` | Associates `key` with `value`. If the key already exists, the **old value is replaced** and returned; otherwise returns `null`. |
| `V get(Object key)` | Returns the value bound to `key`, or `null` if the key is absent. |
| `V remove(Object key)` | Removes the mapping for `key` and returns its value (or `null` if absent). |
| `boolean containsKey(Object key)` | Returns `true` if a mapping for `key` exists. |
| `boolean containsValue(Object value)` | Returns `true` if at least one key maps to `value`. |
| `Set<K> keySet()` | A **view** of all keys (a `Set`, since keys are unique). |
| `Collection<V> values()` | A view of all values (values *may* repeat). |
| `Set<Map.Entry<K, V>> entrySet()` | A view of all key–value pairs. |
| `int size()` | The number of entries. |
| `boolean isEmpty()` | `true` if there are no entries. |
| `void clear()` | Removes all entries. |

Two subtleties are worth flagging immediately.

**First**, `get` returning `null` is ambiguous: it could mean "the key is absent" *or* "the key is present and its value is `null`." Because `HashMap` and `LinkedHashMap` allow `null` values, use `containsKey` to disambiguate when `null` values are possible.

**Second**, the collections returned by `keySet()`, `values()`, and `entrySet()` are *live views*, not snapshots. If you modify the map, the views change with it; if you modify the views (e.g., via an iterator's `remove` method), the map changes too. This is powerful but also the source of the notorious `ConcurrentModificationException` we will examine in Best Practices.

Since Java 8, the interface also provides useful **default methods** — `getOrDefault`, `putIfAbsent`, `replace`, `remove(key, value)`, `computeIfAbsent`, `computeIfPresent`, `merge`, and `forEach` — that let you express common logic without boilerplate. We will use several of them in the code examples throughout this chapter.

Here is a compact demonstration of the core methods:

```java
import java.util.HashMap;
import java.util.Map;

public class MapBasics {

    public static void main(String[] args) {
        Map<String, Integer> ages = new HashMap<>();

        ages.put("Alice", 30);
        ages.put("Bob", 25);
        ages.put("Charlie", 35);

        System.out.println(ages.get("Bob"));              // 25
        System.out.println(ages.containsKey("Dana"));     // false
        System.out.println(ages.containsValue(35));       // true
        System.out.println(ages.size());                  // 3

        ages.put("Alice", 31);                            // replaces the old value
        System.out.println(ages.get("Alice"));            // 31

        ages.remove("Charlie");
        System.out.println(ages.size());                  // 2

        ages.putIfAbsent("Bob", 999);                     // Bob already exists: no change
        System.out.println(ages.get("Bob"));              // 25

        ages.replace("Bob", 26);
        System.out.println(ages.get("Bob"));              // 26

        System.out.println(ages.isEmpty());               // false
        ages.clear();
        System.out.println(ages.isEmpty());               // true
    }
}
```

```
// Expected output:
25
false
true
3
31
2
25
26
false
true
```

### Map.Entry and Iterating Entries

A map entry is itself a small object that bundles one key with its value. The `Map.Entry<K, V>` interface exposes two critical methods:

- `K getKey()` — the entry's key.
- `V getValue()` — the entry's value.

It also offers `setValue(V value)`, which replaces the value *in the backing map* — a handy way to update values while iterating. You obtain entries through `entrySet()`:

```java
import java.util.HashMap;
import java.util.Map;

public class IterateEntries {

    public static void main(String[] args) {
        Map<String, Integer> scores = new HashMap<>();
        scores.put("Team A", 3);
        scores.put("Team B", 1);
        scores.put("Team C", 2);

        // Classic iteration over entries.
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }

        System.out.println("---");

        // Iterate keys only.
        for (String team : scores.keySet()) {
            System.out.println(team);
        }

        System.out.println("---");

        // Java 8+ idiomatic style.
        scores.forEach((team, points) -> System.out.println(team + " => " + points));
    }
}
```

```
// Expected output (note: the ORDER of a HashMap is not guaranteed):
Team C -> 2
Team A -> 3
Team B -> 1
---
Team C
Team A
Team B
---
Team C => 2
Team A => 3
Team B => 1
```

> **Note:** The printed order of a `HashMap` may differ on your machine, between runs, or after you insert one more element. Never write code that depends on a `HashMap`'s iteration order.

### Ordering Guarantees

The four implementations differ most visibly in *when iteration order is deterministic*:

| Implementation | Iteration order | Null keys | Null values | Thread-safe | Typical get/put |
| --- | --- | --- | --- | --- | --- |
| `HashMap` | **None** (unspecified) | Yes (one key) | Yes | No | O(1) average |
| `LinkedHashMap` | **Insertion order**, or **access order** | Yes (one key) | Yes | No | O(1) average |
| `TreeMap` | **Sorted** by key (natural or `Comparator`) | No (with natural ordering) | Yes | No | O(log n) |
| `Hashtable` | **None** (unspecified) | No | No | Yes (coarse locking) | O(1) average |

We will unpack every row of this table in the sections that follow.

### Common Implementations at a Glance

Java ships with a family of general-purpose map implementations. The modern four you must know are:

- **`HashMap`** — the workhorse. Hash-based, fastest average performance, no ordering.
- **`LinkedHashMap`** — a `HashMap` that remembers order, either the order keys were inserted or the order they were last accessed.
- **`TreeMap`** — a sorted map backed by a balanced binary search tree.
- **`Hashtable`** — a Java 1.0-era synchronized hash table kept for compatibility.

Two close cousins deserve a mention here and will appear again in Real-World Use Cases: **`ConcurrentHashMap`**, the modern thread-safe choice (which we discuss in the `Hashtable` section as its successor), and **`EnumMap`**, a lightning-fast map for enum keys.

> **Analogy:** Think of the `Map` interface as the contract for "a machine that stores labeled boxes and retrieves them by label." `HashMap` is the machine that stashes each box in a numbered locker computed from its label — blazingly fast, but the boxes come out in whatever locker order they landed in. `LinkedHashMap` is the same machine with a chain linking boxes in the order they were shelved. `TreeMap` is a filing system that keeps every label alphabetized at all times. `Hashtable` is a much older machine, slow and heavily guarded, that still works but has been superseded.

---

## HashMap

### What Is a HashMap?

A **`HashMap<K, V>`** is a hash-table-based implementation of `Map`. It offers **O(1) average time** for `put`, `get`, `remove`, and `containsKey`, which makes it the default choice for the overwhelming majority of map use cases. It allows **one `null` key** and **any number of `null` values**, and it makes **no promise whatsoever about iteration order** — the order can change when the map resizes, and it can differ between JVM runs.

It is not synchronized, so if multiple threads share a `HashMap` without external locking, you will get corrupted state or `ConcurrentModificationException`. For concurrent scenarios, reach for `ConcurrentHashMap` instead.

### How It Works Internally

At its heart, a `HashMap` is an **array of buckets**. When you `put(key, value)`:

1. Java calls `key.hashCode()` to obtain a 32-bit integer.
2. The map *spreads* that hash (mixing the high and low bits) and then reduces it to an array index using a bitmask. In the current OpenJDK implementation: `index = (n - 1) & (h ^ (h >>> 16))`, where `n` is the array length (always a power of two).
3. The entry is stored in the bucket at that index.

Here is the mental picture:

```
             The backing array of buckets (length n = 16, a power of two)

  index:   0        1        2        3        4    ...   15
          +----+   +----+   +----+   +----+   +----+    +----+
          |    |   |    |   |    |   |    |   |    |    |    |
          +----+   +----+   +----+   +----+   +----+    +----+
           null     null     |        null     |          null
                             |                |
                          +-------+       +-------+
                          | key=" |       | key=" |
                          | c"    |       | e"    |
                          +-------+       +-------+
                              |               |
                          +-------+       +-------+
                          | key=" |       | key=" |
                          | d"    |  -->  | f"    |
                          +-------+       +-------+

        Two keys land in bucket 2 (a collision) and form a linked
        list (Java 8 and later convert long lists into red-black trees).
```

**Collision handling.** Two different keys can produce the same bucket index — this is a **collision**, and it is inevitable, since there are far more possible keys than buckets. In Java 8 and later, a bucket that accumulates many collisions is stored as a linked list until it reaches **8 entries**; at that point, *if the backing array is at least 64 long*, the bucket is **treeified** into a red-black tree. This caps the worst-case lookup at **O(log n)** instead of O(n). When a treeified bucket shrinks below **6 entries**, it reverts to a linked list. (Before Java 8, every bucket stayed a linked list, so adversarial inputs could degrade a `HashMap` to O(n) — the classic "hash-flooding denial of service" attack on web servers.)

**Load factor and resizing.** The array length is called the **capacity**. The **load factor** (default **0.75**) is the ratio of entries to capacity at which the map decides to grow. When `size > capacity × load factor`, the map **rehashes**: it allocates a new array of roughly **double** the capacity and relocates every entry into it. Rehashing is O(n) and momentarily expensive, which is why maps with a *known* large number of entries should be created with an adequate initial capacity to avoid repeated resizing.

> **Why it works — the 0.75 load factor:** Under the assumption that hash values are uniformly distributed, the number of entries in a given bucket follows a Poisson distribution with mean λ = 0.75 at the moment of resize. The OpenJDK authors' analysis shows that with λ = 0.75, the probability of a bucket containing 8 or more entries is roughly 0.00000006 — so treeification almost never triggers in well-behaved programs. A lower load factor (say 0.5) would reduce collisions further but waste memory on empty buckets; a higher one (say 1.0) would save memory but increase collision likelihood. The value 0.75 is a carefully chosen empirical balance between space and speed.

**The `hashCode`/`equals` contract.** Everything above depends on two methods every key class inherits from `Object`:

- `int hashCode()` — a fast integer "fingerprint."
- `boolean equals(Object other)` — the definition of sameness.

The contract is small but sacred:

1. **Consistency:** `hashCode()` must return the same value for the same object across calls (within one execution).
2. **Equal implies equal hash:** if `a.equals(b)` is `true`, then `a.hashCode() == b.hashCode()` *must* be true.
3. **Unequal objects may share a hash** — hash collisions are legal and merely cost performance.

> **Warning:** Violating rule 2 silently breaks `HashMap`. If two keys are `equals` but have different hash codes, they will be stored in different buckets, `get` will never find one of them, and duplicates will accumulate. This is one of the most common subtle bugs in Java — see Best Practices.

When a bucket is a linked list, `get` walks the list calling `equals` on each entry's key until a match is found. When it is a tree, `equals` (or the key's `Comparator`/`Comparable`) is used for tree comparisons instead.

### When to Use HashMap

Use `HashMap` when:

- You need the **fastest possible lookups** by key.
- You do **not care about iteration order**.
- You want to allow `null` keys or values.
- Your keys are immutable, or at least you promise never to mutate the fields that contribute to `hashCode` after insertion.

Avoid it when you need sorted keys (`TreeMap`), predictable order (`LinkedHashMap`), or thread-safety (`ConcurrentHashMap`).

### Code Examples

**Creation and basic operations.**

```java
import java.util.HashMap;
import java.util.Map;

public class HashMapBasics {

    public static void main(String[] args) {
        // Diamond operator infers HashMap<String, Integer>.
        Map<String, Integer> stock = new HashMap<>();

        // Provide an initial capacity when you know the size up front.
        Map<String, Integer> bigMap = new HashMap<>(10_000);

        stock.put("laptop", 12);
        stock.put("mouse", 55);
        stock.put("keyboard", 30);

        System.out.println(stock.get("mouse"));          // 55
        System.out.println(stock.getOrDefault("monitor", 0)); // 0
        System.out.println(stock.containsKey("laptop")); // true

        // Java 9+ immutable map factory (no nulls, no duplicates allowed).
        Map<String, Integer> immutable = Map.of("A", 1, "B", 2);
        // immutable.put("C", 3);   // UnsupportedOperationException
    }
}
```

```
// Expected output:
55
0
true
```

**Word frequency counter — the classic HashMap exercise.**

```java
import java.util.HashMap;
import java.util.Map;

public class WordFrequency {

    public static void main(String[] args) {
        String text = "the quick brown fox jumps over the lazy dog the fox";
        Map<String, Integer> frequencies = new HashMap<>();

        for (String word : text.split(" ")) {
            // getOrDefault is the pre-Java 8 way of "increment or start at 1".
            frequencies.put(word, frequencies.getOrDefault(word, 0) + 1);
        }

        // Java 8+ merge() does the same in one call:
        for (String word : text.split(" ")) {
            frequencies.merge(word, 1, Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : frequencies.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}
```

```
// Expected output (ORDER IS NOT GUARANTEED — the HashMap decides):
the: 3
lazy: 1
dog: 1
fox: 2
brown: 1
jumps: 1
over: 1
quick: 1
```

**Memoization — using a map as a function cache.**

```java
import java.util.HashMap;
import java.util.Map;

public class Memoization {

    private static final Map<Integer, Long> cache = new HashMap<>();

    // computeIfAbsent: compute the value only if the key is missing.
    static long factorial(int n) {
        if (n <= 1) {
            return 1L;
        }
        return cache.computeIfAbsent(n, Memoization::factorial) * n;
    }

    public static void main(String[] args) {
        System.out.println(factorial(20)); // 2432902008176640000
        System.out.println(cache.size());  // how many results were memoized
    }
}
```

```
// Expected output:
2432902008176640000
19
```

> **Note:** `computeIfAbsent` takes the key and a *mapping function* that is invoked only when the key is absent. This pattern — check, compute if missing, store — is the idiomatic way to build a cache, and it is atomic enough to be safe even under single-threaded use (and per-key atomic under `ConcurrentHashMap`).

---

## LinkedHashMap

### What Is a LinkedHashMap?

A **`LinkedHashMap<K, V>`** is a `HashMap` that additionally maintains a **doubly linked list** running through all of its entries. Because of that extra bookkeeping, it behaves exactly like a `HashMap` for performance (all operations remain O(1) average) but adds one crucial feature: **predictable iteration order**.

By default, entries iterate in **insertion order** — the order in which you first inserted each key. (Re-inserting an existing key does *not* move it; only brand-new keys are appended.) In a special **access-order mode**, entries instead iterate in **least-recently-accessed to most-recently-accessed** order — the basis of an **LRU cache**.

> **Analogy:** A `LinkedHashMap` is like a photo album. `HashMap` is a box of photos you can find instantly by label, but the order is chaotic. The `LinkedHashMap` album stores the same photos in labeled pockets *and* threads a string through them in the order you added them. When you flip through the album (iterate), you always see them in a sensible, known sequence. Switch on access-order mode and the album now re-orders itself: every time you look at a photo, it is moved to the back of the album — so the front always holds the photos you haven't looked at in the longest time.

### How It Works Internally

`LinkedHashMap` **extends `HashMap`** and overrides a few template-method hooks (`newNode`, `afterNodeAccess`, `afterNodeInsertion`, `afterNodeRemoval`) that the base class calls at the right moments. Each node gains two extra pointers — `before` and `after` — and the map keeps `head` and `tail` references. That is the entire difference: every operation still runs at O(1) average, but each insertion, removal, and (in access-order mode) retrieval costs a small amount of pointer maintenance.

The memory cost is real but modest: roughly two extra references per entry plus two head/tail references. If you need `HashMap` speed *and* deterministic order, this is the price — and it is usually worth it.

### Access-Order Mode

The constructor with three arguments enables the interesting behavior:

```java
new LinkedHashMap<K, V>(initialCapacity, loadFactor, boolean accessOrder)
```

When `accessOrder` is `true`, every **access** — a successful `get`, `getOrDefault`, `replace`, or even a `put` that overwrites an existing key — moves the touched entry to the **end** of the linked list. Iteration then yields entries from **least recently accessed** to **most recently accessed**. The map grows a tail that is "newest activity" and a head that is "stale, waiting to be evicted."

That is precisely what an **LRU (Least Recently Used) cache** needs: evict the entry at the *head* when the cache is full. `LinkedHashMap` gives you the hook:

```java
protected boolean removeEldestEntry(Map.Entry<K, V> eldest) { ... }
```

`put` and `putAll` call this method after inserting a new entry. Return `true` and the eldest entry (the head of the list) is automatically removed. The default implementation returns `false`; overriding it to `return size() > capacity;` turns the map into a self-evicting LRU cache with **O(1)** insertions, lookups, and evictions.

> **Note:** `removeEldestEntry` is called *after* the new entry is added, so `size()` inside the override already includes the new entry. That is why comparing against a strict `capacity` works: at `size() == capacity + 1`, the newest entry was just inserted and the eldest is evicted, restoring the size to `capacity`.

### Code Examples

**Insertion-order iteration.**

```java
import java.util.LinkedHashMap;
import java.util.Map;

public class InsertionOrderDemo {

    public static void main(String[] args) {
        Map<String, Integer> order = new LinkedHashMap<>();
        order.put("first", 1);
        order.put("second", 2);
        order.put("third", 3);
        order.put("first", 100); // overwrite: does NOT move the entry

        for (String key : order.keySet()) {
            System.out.println(key);
        }
    }
}
```

```
// Expected output (stable, insertion order; "first" stayed in place):
first
second
third
```

**Access-order demo.**

```java
import java.util.LinkedHashMap;
import java.util.Map;

public class AccessOrderDemo {

    public static void main(String[] args) {
        // accessOrder = true: iteration order follows recency of use.
        Map<String, Integer> access = new LinkedHashMap<>(16, 0.75f, true);

        access.put("a", 1);
        access.put("b", 2);
        access.put("c", 3);

        System.out.println(access.keySet()); // [a, b, c]

        access.get("a");  // accessing "a" moves it to the most-recent end
        access.put("b", 2); // overwriting also counts as an access

        System.out.println(access.keySet()); // [c, a, b]
    }
}
```

```
// Expected output:
[a, b, c]
[c, a, b]
```

> **Warning:** With `accessOrder = true`, simply *iterating* over the map touches every entry and reorders it — so never rely on an iteration you performed mid-logic, and never let background iteration accidentally "refresh" a cache. If you need to iterate without disturbing order, iterate over a copy: `new LinkedHashMap<>(map)` (insertion-order copy) or snapshot the keys first.

**A complete LRU cache via `removeEldestEntry`.**

```java
import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private final int capacity;

    public LRUCache(int capacity) {
        super(capacity, 0.75f, true); // accessOrder = true
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }

    public static void main(String[] args) {
        LRUCache<String, Integer> cache = new LRUCache<>(3);

        cache.put("A", 1);
        cache.put("B", 2);
        cache.put("C", 3);

        cache.get("A");          // A is now most recently used
        cache.put("D", 4);       // cache is full -> evict eldest (B)

        System.out.println(cache.keySet()); // [C, A, D]  -- B was evicted

        cache.put("E", 5);       // evicts C (least recently used)
        System.out.println(cache.keySet()); // [A, D, E]
    }
}
```

```
// Expected output:
[C, A, D]
[A, D, E]
```

> **Note:** `LRUCache` is not thread-safe. For a thread-safe LRU cache you would synchronize access or use a `ConcurrentHashMap`-based design (e.g., wrapping with `Collections.synchronizedMap`, or using a library like Guava's `CacheBuilder`).

---

## TreeMap

### What Is a TreeMap?

A **`TreeMap<K, V>`** is a map whose entries are stored in a **sorted order** by key. Iteration always visits keys in ascending order — either their **natural ordering** (as defined by `Comparable`, e.g., numeric or lexicographic order) or a **custom ordering** supplied through a `Comparator`. Every operation — `put`, `get`, `remove`, `containsKey` — runs in **O(log n)** time, because the underlying structure is a self-balancing binary search tree.

> **Analogy:** A `TreeMap` is a telephone directory that is *always* kept alphabetized. Inserting "Zelda" doesn't append her to the back of the book; the book re-sorts itself so every page remains in order. The cost of maintaining that constant order is that lookups require a tree descent (logarithmic) instead of the direct "open the right page" jump of a hash table (constant). In return, you get everything sorted maps are good at: prefix ranges, "next/previous key" navigation, and live sorted views.

### How It Works Internally

`TreeMap` is backed by a **red-black tree**: a self-balancing binary search tree in which every node carries a color bit (red or black) and the tree maintains several invariants:

1. The root is black.
2. Every leaf (`null` child) is black.
3. No red node has a red child (no two consecutive reds).
4. Every path from a node to its descendant leaves contains the same number of black nodes.

These rules guarantee that the tree's height stays **O(log n)** — balanced within a factor of two — even in the worst case. When an insertion or deletion threatens the invariants, the tree performs **rotations** and **recolorings** to restore balance. This is the key difference from a plain binary search tree, which can degenerate into a linked list if keys arrive in sorted order; a red-black tree never allows that.

Keys are compared with either the map's `Comparator` (if one was supplied) or the keys' natural ordering (`Comparable`). Which keys are considered "equal" is decided by that comparison returning zero — not by `equals()`. This matters: two keys can be `equals()` but distinct to the `Comparator`, or vice versa.

**Two sharp edges:**

- With natural ordering, a **`null` key throws `NullPointerException`** at insertion. (A custom `Comparator` *may* choose to accept `null`, but the natural-ordering map refuses it.)
- **`null` values are fine.** Only the *key* is compared, so any value may be `null`.

### Range and Navigation Operations

Because entries are sorted, `TreeMap` (via its interfaces `NavigableMap` and `SortedMap`) can answer questions a `HashMap` cannot:

| Method | Returns |
| --- | --- |
| `firstKey()` / `lastKey()` | Smallest / largest key. |
| `lowerKey(k)` / `higherKey(k)` | Strictly smaller / strictly larger key than `k`. |
| `floorKey(k)` / `ceilingKey(k)` | Largest key ≤ `k` / smallest key ≥ `k`. |
| `firstEntry()` / `lastEntry()` | The `Map.Entry` for the smallest / largest key. |
| `lowerEntry(k)` / `floorEntry(k)` / `ceilingEntry(k)` / `higherEntry(k)` | The corresponding *entries* (or `null` if none). |
| `pollFirstEntry()` / `pollLastEntry()` | Removes and returns the smallest / largest entry. |
| `headMap(toKey)` | View of keys strictly less than `toKey`. |
| `tailMap(fromKey)` | View of keys greater than or equal to `fromKey`. |
| `subMap(fromKey, toKey)` | View of keys from `fromKey` (inclusive) up to `toKey` (exclusive). |
| `subMap(from, incl, to, incl)` / `headMap(to, incl)` / `tailMap(from, incl)` | Same views with explicit inclusivity flags. |
| `descendingMap()` | A reversed-order view of the whole map. |

All `headMap`/`tailMap`/`subMap` views are **live**: changes to the view write through to the backing `TreeMap`, and vice versa. Attempting to add a key outside the view's range throws `IllegalArgumentException`.

### When to Use TreeMap

Use `TreeMap` when:

- You need keys **always sorted** — for a leaderboard, dashboard, or calendar.
- You need **range queries**: "all prices between $50 and $100," "all meetings in the next hour."
- You need nearest-neighbor lookups: "what's the next available slot after 3 p.m.?"
- Your keys implement `Comparable` and you want to store them in natural order.

Avoid it when you only need plain lookup performance — the O(log n) cost and the per-node memory overhead (left, right, parent, and color fields per entry) make it noticeably heavier than a `HashMap`.

### Code Examples

**Sorted iteration with natural ordering.**

```java
import java.util.Map;
import java.util.TreeMap;

public class SortedIteration {

    public static void main(String[] args) {
        Map<String, Integer> grades = new TreeMap<>();
        grades.put("Bob", 88);
        grades.put("Alice", 92);
        grades.put("Charlie", 75);
        grades.put("Dana", 81);

        grades.forEach((name, score) -> System.out.println(name + ": " + score));
    }
}
```

```
// Expected output (always alphabetical, regardless of insertion order):
Alice: 92
Bob: 88
Charlie: 75
Dana: 81
```

**Custom comparator.**

```java
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class CustomComparator {

    public static void main(String[] args) {
        // Order keys by their length, then by natural order as a tie-breaker.
        Comparator<String> byLength = Comparator.comparingInt(String::length)
                                               .thenComparing(Comparator.naturalOrder());

        Map<String, String> map = new TreeMap<>(byLength);
        map.put("grape", "purple");
        map.put("fig", "green");
        map.put("plum", "purple");
        map.put("kiwi", "brown");
        map.put("date", "brown");

        for (Map.Entry<String, String> e : map.entrySet()) {
            System.out.println(e.getKey() + " -> " + e.getValue());
        }
    }
}
```

```
// Expected output (sorted by key length, ties broken alphabetically):
date -> brown
fig -> green
kiwi -> brown
plum -> purple
grape -> purple
```

> **Note:** If you supply only `Comparator.comparingInt(String::length)` without the `thenComparing` tie-breaker, two keys of equal length would be treated as *equal* and one would silently overwrite the other. This is a classic `TreeMap` bug — always make your comparator's notion of "equal" agree with what you intend to store.

**Range queries: an event schedule.**

```java
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class EventSchedule {

    public static void main(String[] args) {
        // Keys are 24-hour clock hours.
        NavigableMap<Integer, String> events = new TreeMap<>();
        events.put(9, "Standup meeting");
        events.put(10, "Code review");
        events.put(14, "Design sync");
        events.put(16, "Interview");
        events.put(17, "Demo prep");

        System.out.println("First event at: " + events.firstKey());       // 9
        System.out.println("Last event at:  " + events.lastKey());        // 17

        System.out.println("Ceiling of 15: " + events.ceilingKey(15));    // 16
        System.out.println("Floor of 15:   " + events.floorKey(15));      // 14
        System.out.println("Lower than 16: " + events.lowerKey(16));      // 14
        System.out.println("Higher than 16:" + events.higherKey(16));     // 17

        System.out.println("Between 10 and 16 inclusive:");
        Map<Integer, String> afternoon = events.subMap(10, true, 16, true);
        System.out.println(afternoon); // {10=Code review, 14=Design sync, 16=Interview}

        System.out.println("Before 14: " + events.headMap(14));
        // {9=Standup meeting, 10=Code review}

        System.out.println("From 16 on: " + events.tailMap(16));
        // {16=Interview, 17=Demo prep}
    }
}
```

```
// Expected output:
First event at: 9
Last event at:  17
Ceiling of 15: 16
Floor of 15:   14
Lower than 16: 14
Higher than 16:17
Between 10 and 16 inclusive:
{10=Code review, 14=Design sync, 16=Interview}
Before 14: {9=Standup meeting, 10=Code review}
From 16 on: {16=Interview, 17=Demo prep}
```

---

## Hashtable

### What Is a Hashtable?

**`Hashtable<K, V>`** is the oldest map implementation in the JDK — it has existed since **Java 1.0** (note the lowercase *t*, a quirk of its name that predates the Java naming conventions). It is a hash-table map with the same O(1) average behavior as `HashMap`, but with two defining restrictions:

1. **It is synchronized** — every public method is `synchronized` at the method level.
2. **It forbids `null` keys and `null` values** — inserting either throws `NullPointerException`.

It also uses **`Enumeration`** (the `keys()` and `elements()` methods) alongside the standard `Map` iteration APIs, a legacy leftover from before the Collections Framework existed.

### Legacy Status and Synchronization

In 1998, Java 1.2 introduced the Collections Framework, and `Hashtable` was retrofitted to implement the new `Map` interface. Since then, it has been retained purely for **backward compatibility**. New code has no good reason to use it.

The "thread safety" of `Hashtable` is real but crude: every method acquires the same object-wide monitor. That means **all operations serialize**, even unrelated ones, so concurrent throughput is poor — under heavy contention, threads spend most of their time blocked on the lock. And method-level synchronization cannot protect compound operations anyway: `if (!t.containsKey(k)) t.put(k, v);` is still a race unless you wrap it in your own `synchronized(t)` block.

The modern replacement is **`ConcurrentHashMap`**, which provides:

- **Fine-grained synchronization** (CAS-based operations with per-bucket locks in Java 8+), so concurrent readers don't block at all and writers rarely contend with each other.
- **Weakly consistent iterators** that never throw `ConcurrentModificationException` — safe to iterate while another thread mutates the map.
- O(1) average `size()` (since Java 8), O(1) average `get`/`put`, and useful atomic operations like `putIfAbsent`, `computeIfAbsent`, and `merge`.

> **Note:** Like `Hashtable`, `ConcurrentHashMap` **also forbids `null` keys and values.** The designers' reasoning: in a concurrent map, `get` returning `null` must unambiguously mean "absent," because you can't tell the difference between "stored null" and "not present" without extra coordination. If your data genuinely needs `null` values in a concurrent setting, you need a sentinel or a wrapper object instead.

### Key Differences from HashMap

| Aspect | `Hashtable` | `HashMap` |
| --- | --- | --- |
| Introduced | Java 1.0 | Java 1.2 |
| Null keys | Forbidden (`NPE`) | One allowed |
| Null values | Forbidden (`NPE`) | Allowed |
| Synchronization | All methods synchronized | None (not thread-safe) |
| Iteration | `Enumeration` (`keys`, `elements`) plus iterators | Iterators only |
| Default initial capacity | 11 | 16 |
| Collision handling | Linked lists (no treeification) | Treeified bins in Java 8+ (≥ 8 entries, when array ≥ 64) |
| Status | Legacy | The standard general-purpose choice |

Two implementation details are worth knowing. First, `Hashtable`'s default **initial capacity is 11**, and its bucket calculation is a modulo operation on the hash — it does not require a power-of-two capacity the way modern `HashMap` does. Second, because its code predates the Java 8 `HashMap` rewrite, its buckets do **not** treeify; a pathologically colliding key set can degrade it to O(n).

### Code Examples

**A minimal example, including the null prohibition.**

```java
import java.util.Hashtable;

public class LegacyTableDemo {

    public static void main(String[] args) {
        Hashtable<String, Integer> table = new Hashtable<>();
        table.put("one", 1);
        table.put("two", 2);

        System.out.println(table.get("two"));            // 2

        // Legacy Enumeration-style iteration.
        var en = table.keys();
        while (en.hasMoreElements()) {
            System.out.println(en.nextElement());
        }

        try {
            table.put(null, 0);   // Null keys are rejected.
        } catch (NullPointerException e) {
            System.out.println("Null key rejected!");
        }

        try {
            table.put("zero", null);  // Null values are rejected too.
        } catch (NullPointerException e) {
            System.out.println("Null value rejected!");
        }
    }
}
```

```
// Expected output:
2
one
two
Null key rejected!
Null value rejected!
```

> **Note:** Enumeration order is unspecified, exactly like `HashMap` iteration order — and unlike `HashMap`, the `Enumeration` is *not* fail-fast. If you rely on fail-fast behavior to catch concurrent modification bugs, `Hashtable`'s enumerations won't give it to you.

**Migrating from `Hashtable` to `ConcurrentHashMap`.**

```java
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

public class MigrationDemo {

    public static void main(String[] args) {
        // ----- Legacy code (old systems, Java 1.x era) -----
        Hashtable<String, Integer> legacy = new Hashtable<>();
        legacy.put("cpu", 4);
        legacy.put("memory", 16);
        System.out.println("Legacy CPUs: " + legacy.get("cpu"));

        // ----- Modern replacement (concurrent, weakly consistent) -----
        ConcurrentHashMap<String, Integer> modern = new ConcurrentHashMap<>();
        modern.put("cpu", 4);
        modern.put("memory", 16);

        // Atomic compound operation available on the modern map:
        modern.putIfAbsent("cpu", 128);            // no-op: "cpu" already present

        // Safe to iterate while other threads mutate:
        modern.forEach((k, v) -> System.out.println(k + "=" + v));
    }
}
```

```
// Expected output:
Legacy CPUs: 4
cpu=4
memory=16
```

> **Best Practice:** When modernizing code that uses `Hashtable`, replace it with `ConcurrentHashMap` (if the map is genuinely shared across threads) or plain `HashMap` (if it is confined to one thread). Both migrations are drop-in at the call sites, with the caveat that — as with `Hashtable` — neither modern map accepts `null` keys or values, so scan the surrounding code for null usage first.

---

## Comparing the Implementations

Now that each implementation has been examined in depth, here is the complete picture.

| Feature | `HashMap` | `LinkedHashMap` | `TreeMap` | `Hashtable` |
| --- | --- | --- | --- | --- |
| Iteration order | None guaranteed | Insertion order (default) or access order | Sorted by key | None guaranteed |
| Backing structure | Hash table (array of buckets) | Hash table + doubly linked list | Red-black tree | Hash table |
| `get` / `put` — average | O(1) | O(1) | O(log n) | O(1) |
| `get` / `put` — worst case | O(log n) (treeified bins, Java 8+); O(n) for adversarial inputs otherwise | O(log n) (inherits `HashMap` treeification) | O(log n) always | O(n) (no treeification) |
| `containsKey` | O(1) average | O(1) average | O(log n) | O(1) average |
| Memory overhead | Lowest (array + node per entry) | `HashMap` + 2 pointers/entry | Highest (left, right, parent, color per node) | Low (array + node per entry) |
| `null` keys | One allowed | One allowed | Not allowed (natural ordering) | Not allowed |
| `null` values | Allowed | Allowed | Allowed | Not allowed |
| Thread-safe | No | No | No | Yes (crude, method-level) |
| Iteration behavior | Fail-fast | Fail-fast | Fail-fast | Fail-fast (iterators); not fail-fast (enumerations) |
| Introduced | Java 1.2 | Java 1.4 | Java 1.2 | Java 1.0 |
| Typical use | General-purpose lookups | Ordered iteration, LRU caches | Sorted views, range queries | Legacy code only |

### Decision Flowchart

If prose is easier to navigate than a table, follow this decision tree:

| Your need | Choose |
| --- | --- |
| Fastest general-purpose map; order irrelevant; possibly `null` keys/values | `HashMap` |
| `HashMap` speed *plus* deterministic iteration order | `LinkedHashMap` (insertion order) |
| An eviction cache that drops least-recently-used entries | `LinkedHashMap` with `accessOrder=true` + overridden `removeEldestEntry` |
| Keys must always be sorted; range or nearest-neighbor queries | `TreeMap` |
| Thread-safe map shared by many threads, high throughput | `ConcurrentHashMap` |
| A single-threaded map that may become shared later | Start with `HashMap`; switch to `ConcurrentHashMap` when contention appears |
| You are touching a legacy codebase that already uses it | Keep `Hashtable` only where required; migrate new code |
| Keys are an enum | `EnumMap` (specialized, even faster than `HashMap`) |
| A small, fixed, immutable set of mappings (Java 9+) | `Map.of(...)` / `Map.ofEntries(...)` |

The golden rule of map selection: **default to `HashMap`, and let a specific requirement — ordering, range queries, or concurrency — justify the switch.**

---

## Best Practices and Common Pitfalls

**1. Never rely on `HashMap` iteration order.** The order is an implementation detail. It can change when you add elements (resize), between JVM runs, and between JDK versions. Code that "happens to work" because of accidental order is a latent bug. If you need order, use `LinkedHashMap`; if you need sorted order, use `TreeMap`.

**2. Respect the `hashCode`/`equals` contract.** If two keys are `equals`, their hash codes must match. Failing this, `HashMap` stores duplicates and `get` returns `null` for keys that are definitely present. Use your IDE's "Generate equals and hashCode" feature, or Java's `Objects.equals` and `Objects.hash`:

```java
public final class Product {
    private final String id;
    private final int price;

    public Product(String id, int price) {
        this.id = id;
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product p)) return false;
        return price == p.price && Objects.equals(id, p.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, price);
    }
}
```

**3. Never mutate keys after insertion.** If a key's `hashCode()` (or `Comparator` position) changes while it sits in a map, the map becomes inconsistent: `get` will look in the wrong bucket and return `null`, and the stale entry may become unreachable. Prefer **immutable keys** (like `String`, `Integer`, `UUID`, or records in Java 16+). If you must mutate a key, remove the entry *before* mutating and re-insert it *after*.

```java
// BROKEN: mutating a key after insertion
Product key = new Product("a", 10);
Map<Product, String> map = new HashMap<>();
map.put(key, "shelf 1");
key = new Product("a", 99);       // different hashCode!
System.out.println(map.get(key)); // null -- the original is lost
```

**4. Don't forget the generic type parameters.** A raw `Map` silently degrades to `Object` keys/values, forcing unchecked casts and inviting `ClassCastException`. Always write `Map<String, Integer>` — or use the diamond operator `new HashMap<>()` where the compiler can infer the type.

**5. Choose the right implementation — and the right constructor.** A `HashMap` that will hold a million entries should be created with a realistic initial capacity (and, if you know the exact size, use `new HashMap<>(expectedSize)` or `new HashMap<>(expectedSize / 0.75f + 1)` to avoid resizing). Picking `TreeMap` when you only need lookups wastes logarithmic time and memory; picking `HashMap` when you need ranges forces you to re-sort on every query.

**6. Beware `ConcurrentModificationException`.** Iterators are **fail-fast**: if the map is *structurally modified* (an entry added or removed) after the iterator is created — by any thread, or even by the same thread through the map rather than the iterator — the iterator throws `ConcurrentModificationException` on its next step. You may, however, safely `remove()` via the iterator itself:

```java
// SAFE: removing through the iterator
Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
while (it.hasNext()) {
    if (it.next().getValue() < 0) {
        it.remove();
    }
}
```

```java
// BROKEN: ConcurrentModificationException on next iteration
for (Map.Entry<String, Integer> e : map.entrySet()) {
    if (e.getValue() < 0) {
        map.remove(e.getKey()); // structural change mid-iteration!
    }
}
```

**7. The `get`-vs-`containsKey` ambiguity.** With maps that allow `null` values (`HashMap`, `LinkedHashMap`, `TreeMap`), `get(k) == null` does not prove `k` is absent. When correctness depends on presence, call `containsKey`.

**8. Iterating `keySet()` and then calling `get(key)` is wasteful.** Each `get` is another O(1) or O(log n) lookup. Iterate `entrySet()` (or use `forEach`) when you need both key and value:

```java
// Poor: N extra lookups
for (String k : map.keySet()) {
    process(k, map.get(k));
}

// Better
for (Map.Entry<String, Integer> e : map.entrySet()) {
    process(e.getKey(), e.getValue());
}
```

**9. Prefer default methods over manual check-then-act.** `computeIfAbsent`, `merge`, and `putIfAbsent` express intent and — on `ConcurrentHashMap` — are atomic. Hand-rolled `if (!map.containsKey(k)) map.put(...)` sequences race under concurrency.

> **Warning:** `HashMap`, `LinkedHashMap`, and `TreeMap` are **not thread-safe**. A shared, unsynchronized map is a data race in Java's memory model: reads may see stale or torn state, and iteration can throw `ConcurrentModificationException`. Synchronize externally (`Collections.synchronizedMap(map)`), or use `ConcurrentHashMap`.

---

## Real-World Use Cases

### HashMap in industry

`HashMap` is the default engine behind countless production systems:

- **In-memory caches** — memoization tables, API-response caches, and lookup tables where key→value retrieval must be as fast as possible.
- **Object indexes** — mapping user IDs to user objects, order IDs to orders, session tokens to session data.
- **Counting and aggregation** — word-frequency analysis, log-level counting, tag clouds, and streaming counters (typically with `merge`).
- **Graph adjacency lists** — mapping a node to its set of neighbors.
- **Database connection and thread-local registries** — name→resource mappings.

Essentially, any time a service needs "give me the record for this identifier" with no ordering requirements, a `HashMap` (or its concurrent sibling) is the workhorse.

### LinkedHashMap in industry

- **LRU caches** — in browsers (recently visited pages), databases (page/row caches), and ORM tools (first-level caches), where you must evict the least-recently-used item when memory is exhausted.
- **Most-recently-used lists** — "recent files," "recent orders," feed readers showing the latest items first while still allowing O(1) keyed access.
- **Preserving request order** — when you must process or render items in the exact order they were inserted (e.g., configuration loaded in order, ordered user selections) while retaining O(1) lookups by key.
- **Deterministic output** — any system where serializing a map must produce reproducible, stable output (logs, reports, test fixtures), where `HashMap`'s unpredictable order would cause flaky diffs.

### TreeMap in industry

- **Sorted dashboards and leaderboards** — "top scores" tables that must always be sorted, where entries are re-ranked cheaply.
- **Range queries** — pricing engines ("all products between $50 and $100"), calendar and booking systems ("all free slots between 3 p.m. and 5 p.m."), time-series databases holding events keyed by timestamp.
- **Nearest-neighbor lookups** — "the next train after 14:30," "the previous backup before this restore point," implemented with `ceilingKey`/`floorKey`.
- **Intervals and sweeps** — scheduling algorithms and interval overlap detection, where ordered keys plus range views make subrange extraction O(log n + m).
- **Merge and union operations** — because both maps are sorted, merging two `TreeMap`s into a sorted result is a linear merge, exactly like merging sorted lists.

### Hashtable in industry

- **Legacy systems** — code written in the Java 1.0/1.1 era that is still in production. It works, it's synchronized, and the risk of touching it exceeds the benefit, so it stays.
- **Historical learning** — reading old codebases, textbooks, and open-source projects that predate `ConcurrentHashMap` (which arrived with Java 5 in 2004).
- **Its modern successor, `ConcurrentHashMap`** — the actual industrial workhorse for shared maps: cache stores, session registries, distributed-cache client state, rate-limiters, and object pools, all relying on its fine-grained locking and atomic per-key operations.

---

## Summary

| Implementation | Ordering | Avg `get`/`put` | Null keys | Null values | Thread-safe | Best for |
| --- | --- | --- | --- | --- | --- | --- |
| `HashMap` | None | O(1) | One | Yes | No | General-purpose fast lookups |
| `LinkedHashMap` | Insertion or access | O(1) | One | Yes | No | Ordered iteration, LRU caches |
| `TreeMap` | Sorted by key | O(log n) | No* | Yes | No | Sorted views, range queries |
| `Hashtable` | None | O(1) | No | No | Yes (crude) | Legacy code; use `ConcurrentHashMap` instead |

*With natural ordering. A custom `Comparator` may choose to accept `null`.

Key takeaways:

- A `Map<K, V>` associates unique keys with values; the interface contract includes `put`, `get`, `remove`, `containsKey`, and the three live views `keySet()`, `values()`, and `entrySet()`.
- `HashMap` delivers average O(1) operations via hashing into buckets, with treeified bins (Java 8+) keeping worst cases at O(log n). Its iteration order is **never** guaranteed.
- Correct `hashCode` and `equals` are non-negotiable: equal keys must produce equal hash codes, and keys must be immutable (or never mutated) after insertion.
- `LinkedHashMap` is a `HashMap` plus a doubly linked list; with `accessOrder=true` and an overridden `removeEldestEntry`, it becomes a clean O(1) LRU cache.
- `TreeMap` keeps keys sorted in a red-black tree, enabling O(log n) operations plus range views and navigation (`subMap`, `headMap`, `tailMap`, `ceilingKey`, `floorKey`, and friends).
- `Hashtable` is a synchronized, null-hostile legacy map. New code should use `HashMap` (single-threaded) or `ConcurrentHashMap` (multi-threaded), neither of which accepts nulls either.
- Fail-fast iterators throw `ConcurrentModificationException` if you structurally modify a map mid-iteration; remove through the iterator, or use the view collections' own methods.

---

## Practice Exercises

**Beginner**

1. **Phone book.** Build a `Map<String, String>` (name → phone number), add five contacts, look up one by name, remove one, and print the total count. *Hint: `put`, `get`, `remove`, `size`.*

2. **Contains-value without cheating.** Write a method `boolean containsValue(Map<String, Integer> map, int v)` that checks for a value using only iteration over `values()` or `entrySet()` — do not call `containsValue`. *Hint: compare `v` against each value with `equals` or `==` for `int`.*

3. **Order stability test.** Fill a `LinkedHashMap` with five keys, then call `keySet()` twice, printing the order both times. Confirm it never changes. Repeat with a `HashMap` and observe it can differ from the `LinkedHashMap`. *Hint: no trick — this is about observation and confirming the guarantee.*

**Intermediate**

4. **Case-insensitive word counter.** Count word frequencies from a sentence, ignoring case and punctuation (`"Hello, hello world!"` → `hello: 2, world: 1`), using `merge`. *Hint: `String::toLowerCase`, strip non-letters with `replaceAll("[^a-zA-Z]", " ")`, then `merge(word, 1, Integer::sum)`.*

5. **LRU cache with capacity 2.** Build an `LRUCache` from `LinkedHashMap` with `capacity = 2`. Insert A, B, read A, insert C, and verify B was evicted (access order should be `[A, C]`). *Hint: `super(capacity, 0.75f, true)` and `return size() > capacity;` in `removeEldestEntry`.*

6. **Busy-hour report.** Given a `TreeMap<Integer, Integer>` of hour → bookings, print all bookings between 12:00 and 18:00 inclusive using a range view, plus the busiest hour. *Hint: `subMap(12, true, 18, true)`; iterate the view to find the max value.*

**Advanced**

7. **Memoized Fibonacci.** Implement `fib(n)` with a `Map<Integer, Long>` and `computeIfAbsent`, and verify it runs in near-linear time even for `n = 90`. *Hint: `cache.computeIfAbsent(n, k -> fib(k - 1) + fib(k - 2))`; watch the base cases.*

8. **The mutable-key bug.** Create a small mutable key class (e.g., `class Point { int x, y; }` with proper `hashCode`/`equals`), insert it into a `HashMap`, then mutate one field and try `get`. Explain why it fails, then fix it (e.g., with a record/immutable key). *Hint: `get` recomputes the hash of the mutated key, which no longer matches the stored bucket; this is the canonical demonstration of the immutable-keys rule.*

---

## Further Reading

- **Official Java documentation (Java 17/21 LTS):**
  - `Map` — https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Map.html
  - `HashMap` — https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/HashMap.html
  - `LinkedHashMap` — https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/LinkedHashMap.html
  - `TreeMap` and `NavigableMap` — https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/TreeMap.html
  - `Hashtable` — https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Hashtable.html
  - `ConcurrentHashMap` — https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html
- **Joshua Bloch, *Effective Java*, 3rd edition** — items on `equals`/`hashCode` (Items 10–11), `Comparable` (Item 14), and API/implementation guidance (Items 64–65). The definitive authority on the contracts that maps depend on.
- **Cay S. Horstmann, *Core Java, Volume I — Fundamentals* (12th edition)** — clear, example-driven coverage of the Collections Framework and maps.
- **Maurice Naftalin and Philip Wadler, *Java Generics and Collections*** — the classic deep treatment of the collection interfaces and their implementations, including the rationale behind each design decision.
- **Brian Goetz et al., *Java Concurrency in Practice*** — for `ConcurrentHashMap`, `ConcurrentModificationException`, and the memory-model guarantees that make concurrent maps safe.
- **The OpenJDK `HashMap` source code** (`java.util.HashMap` with its extensive class-level comment) — the canonical reference on load factors, Poisson analysis, and treeification thresholds; reading its header comment is a rite of passage.