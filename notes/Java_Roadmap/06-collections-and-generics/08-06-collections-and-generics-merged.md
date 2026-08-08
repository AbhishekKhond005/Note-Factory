# 06-collections-and-generics

> Merged study notes for **06-collections-and-generics**

---

# The Java Collections Framework

---

## Introduction: Why a Collections Framework?

Imagine you're building an application that tracks every customer who signs up for your service. You need to store their names, add new ones, remove ones who cancel, check whether a name exists, and sort them alphabetically. Now try doing that with a plain array:

```java
String[] customers = new String[10];       // fixed size!
int size = 0;

// Adding requires manual bookkeeping...
customers[size++] = "Ada";
customers[size++] = "Grace";

// ...and resizing requires you to build a whole new array:
String[] bigger = new String[customers.length * 2];
System.arraycopy(customers, 0, bigger, 0, size);
customers = bigger;
```

This works, but it's painful. Arrays have four fundamental problems:

1. **Fixed size** — you must grow them by hand.
2. **No search or sort utilities** — you write every algorithm yourself.
3. **No polymorphism** — you can't write one method that works on "any collection of strings."
4. **No meaningful interface** — `Object[]` tells you nothing about the semantics (is it a list? a set? a queue?).

The **Java Collections Framework (JCF)** solves all of this. It's a unified architecture of interfaces, implementations, and algorithms for storing and manipulating groups of objects.

**Analogy:** A plain array is a pile of loose items on your workshop floor — you can find what you need, but only if you remember exactly where you dropped it. The collections framework is a well-organized toolbox: every drawer is labeled (the *interface* tells you what it does), every tool has a *specific job* (the *implementation* tells you how it works), and the whole thing hangs on a pegboard where you can see how everything fits together (the *hierarchy*).

---

### The Hierarchy: One Family Tree, Three Branches

Every collection in Java belongs to a hierarchy rooted at the `java.util` package. The core idea is **interfaces define *what* you can do; implementations define *how* they do it.**

```
                        Iterable<E>
                             │
                        Collection<E>
                        ┌────┴────┬─────────┐
                      List<E>  Set<E>   Queue<E>
                                 │          │
                              SortedSet   Deque<E>
                        (SortedSet → TreeSet)

              Map<K,V>  ─── (NOT a Collection, its own tree)
              ├── SortedMap<K,V>
              ├── HashMap<K,V> / LinkedHashMap<K,V>
              └── TreeMap<K,V>
```

Two things stand out immediately:

- **`Collection` is the grandfather interface** for everything that holds individual elements: `List`, `Set`, and `Queue` (and `Deque`).
- **`Map` is deliberately *outside* the `Collection` hierarchy.** A `Map` holds *pairs* of things (key → value), not single elements, so it lives on its own branch. This trips up beginners constantly: `Map` is not a `Collection`, and `Collection` is not a `Map`.

Because `Iterable` sits at the top, **every** collection in the framework supports the enhanced `for` loop and streams.

---

### The Common `Collection` Interface

Every `List`, `Set`, and `Queue` shares a common contract. Learn these methods once and they work everywhere:

```java
import java.util.*;

public class CollectionBasics {
    public static void main(String[] args) {
        Collection<String> names = new ArrayList<>();
        names.add("Ada");                 // add an element
        names.add("Grace");
        names.add("Ada");                 // List allows duplicates

        System.out.println(names.size());        // 3
        System.out.println(names.contains("Grace")); // true
        System.out.println(names.isEmpty());     // false

        names.remove("Grace");                   // remove by value
        System.out.println(names);               // [Ada, Ada]

        // Iteration: three equivalent ways
        for (String n : names) System.out.println(n);   // enhanced for
        names.forEach(System.out::println);             // forEach + method ref
        names.stream().map(String::toUpperCase)         // streams
             .forEach(System.out::println);

        names.clear();                                 // empty it
    }
}
```

All of this compiles and runs — and it runs identically whether `names` is an `ArrayList`, a `LinkedList`, a `HashSet`, or a `PriorityQueue`. That's the power of programming to the interface: **you can swap implementations without touching your calling code.**

---

### Generics: `List<String>`, Not `List`

The collection framework is built on generics, which let you say *what kind* of thing the collection holds. This gives you compile-time safety:

```java
List<String> names = new ArrayList<>();   // ✓ the right way
names.add("Ada");                          // fine
// names.add(42);                          // ✗ compile error! Good.

List raw = new ArrayList();                // ✗ raw type — avoid!
raw.add("Ada");
raw.add(42);                               // allowed — everything becomes Object
```

**Pitfall:** Using raw types like `List` (instead of `List<String>`) disables all type checking. You can shove an `Integer` into what you thought was a list of strings, and the problem only explodes at runtime with a `ClassCastException` — far from where the bug was written. The compiler literally warns you about this. Always use parameterized types.

> **When to use:** Whenever your program must store a *group of objects* — names, records, results, events — use the collections framework instead of hand-rolled arrays. Choose the specific interface (`List`/`Set`/`Queue`/`Map`) based on the table below.

---

### The Decision Table

Before we dive into each interface, here's the whole framework in one glance. Come back to this table when you're stuck choosing:

| Your need | Interface | Typical implementation |
|---|---|---|
| Ordered, indexed access; duplicates OK | `List` | `ArrayList` |
| No duplicates; insertion order matters | `Set` | `LinkedHashSet` |
| No duplicates; sorted order matters | `Set` | `TreeSet` |
| No duplicates; order irrelevant | `Set` | `HashSet` |
| First-in, first-out processing | `Queue` | `ArrayDeque` / `LinkedList` |
| Process by priority, not arrival | `Queue` | `PriorityQueue` |
| Stack (last-in, first-out) or double-ended ops | `Deque` | `ArrayDeque` |
| Key → value lookup | `Map` | `HashMap` |
| Key → value, keys sorted | `Map` | `TreeMap` |
| Key → value, insertion order preserved | `Map` | `LinkedHashMap` |

Now let's meet each interface in detail.

---

## List — Ordered, Indexed, Allow Duplicates

A **`List`** is an ordered collection (a *sequence*) where every element has a **position** (index 0, 1, 2, …), you can access elements **by index**, and **duplicates are allowed**. If you add `"Ada"` twice, you genuinely have two copies in two positions.

**Analogy:** A list is a row of numbered lockers. Each locker has a fixed address painted on it (`locker[3]`), you can open any locker directly by its number, and two different lockers may hold identical contents.

**Real-world uses:** a music playlist (songs repeat, and you jump to track 7), a to-do list, a leaderboard that must stay in ranking order, the undo stack of recent actions.

### The Two Workhorses: `ArrayList` and `LinkedList`

**`ArrayList`** — a resizable array under the hood. Elements sit contiguously in memory. When it fills up, it grows by roughly 50% and copies everything over (amortized O(1) append). Index access is *instant* — `get(500)` is just pointer arithmetic.

**`LinkedList`** — a doubly-linked list of nodes, each pointing to its predecessor and successor. There's no indexing hardware: `get(500)` must walk 500 links one at a time.

```java
import java.util.*;

public class ListDemo {
    public static void main(String[] args) {
        // Construction options
        List<String> playlist = new ArrayList<>();          // growable
        List<String> fixed    = Arrays.asList("A", "B");    // fixed-size view
        List<String> immutable = List.of("A", "B");         // immutable, Java 9+

        // Core operations
        playlist.add("Bohemian Rhapsody");      // append
        playlist.add(0, "Intro");               // insert at position 0
        String song = playlist.get(0);          // read by index
        playlist.set(1, "Hotel California");    // replace at index

        // Overloaded remove — WARNING, see pitfalls below
        playlist.remove(0);                     // removes element AT index 0
        playlist.remove("Hotel California");    // removes the OBJECT

        System.out.println(playlist.indexOf("Hotel California")); // index or -1
        System.out.println(playlist.contains("Paranoid"));        // false

        // Sublist is a VIEW — changes reflect in the original
        List<String> head = playlist.subList(0, 2);
        head.set(0, "Changed!");
        System.out.println(playlist);            // first element changed

        // Sorting
        playlist.sort(String::compareTo);        // or Comparator.naturalOrder()

        // Iterating
        for (int i = 0; i < playlist.size(); i++) { }        // indexed
        for (String s : playlist) { }                        // enhanced for
        Iterator<String> it = playlist.iterator();
        while (it.hasNext()) { String s = it.next(); }       // iterator

        // ListIterator walks both directions and can modify safely
        ListIterator<String> li = playlist.listIterator(playlist.size());
        while (li.hasPrevious()) { li.previous(); }

        // Streams
        playlist.stream()
                .filter(s -> s.length() > 5)
                .sorted()
                .forEach(System.out::println);
    }
}
```

### `ArrayList` vs `LinkedList`

| Operation | `ArrayList` | `LinkedList` |
|---|---|---|
| `get(i)` / `set(i, v)` | **O(1)** — direct index | **O(n)** — must traverse links |
| `add(e)` at end | **O(1)** amortized | **O(1)** (keeps tail pointer) |
| `add(i, e)` in middle | **O(n)** — shift elements | **O(n)** to *find* position, then O(1) to link |
| `remove(i)` | **O(n)** — shift elements | **O(n)** to find, then O(1) |
| Memory | Compact array, less overhead | Node per element, ~3× more memory |
| When to choose | **Almost always** | Only when you mostly add/remove at the *ends* |

**The bottom line:** for 99% of real programs, `ArrayList` is the right choice. `LinkedList`'s theoretical advantage at the front of the list is rarely worth its memory overhead and cache-hostile node chasing.

### Common Pitfalls

1. **`remove(int)` vs `remove(Object)` with `Integer`.** `list.remove(1)` removes *by index*; `list.remove(Integer.valueOf(1))` removes *by value*. Pass a bare `int` and Java picks the index overload — often silently deleting the wrong thing.
2. **`ConcurrentModificationException`.** You can't structurally modify a list (add/remove) while iterating it with a `for`-each or `Iterator`, or the iterator detects tampering and throws. Fix: use `iterator.remove()`, a `ListIterator`, or collect items and modify after the loop.
3. **`Arrays.asList` is fixed-size.** It returns a wrapper *view* over the array — you can `set`, but `add`/`remove` throw `UnsupportedOperationException`. For a truly independent list, wrap it: `new ArrayList<>(Arrays.asList(...))`.
4. **`List.of()` is immutable.** No `add`, no `set`, no `null` elements. Perfect for constants, wrong for building data.

### Runnable Example: Playlist Manager

```java
import java.util.*;

public class PlaylistManager {
    public static void main(String[] args) {
        List<String> queue = new ArrayList<>(List.of("Song A", "Song B", "Song C"));

        queue.add("Song D");
        queue.remove("Song B");                    // remove by object
        System.out.println("Now playing: " + queue.get(0));
        System.out.println("Up next: " + queue.get(1));

        queue.sort(Comparator.comparingInt(String::length));
        System.out.println("Queue sorted by length: " + queue);

        // Safe removal during iteration
        queue.removeIf(s -> s.length() < 6);
        System.out.println("After trimming short titles: " + queue);
    }
}
```

> **When to use `List`:** you need to preserve order, access elements by position, or the same element may legitimately appear more than once (playlists, histories, leaderboards, form data). Use `ArrayList` unless you have a concrete reason not to.

---

## Set — No Duplicates, No Defined Order

A **`Set`** is a collection that *rejects duplicates*. Add a value that's already present and the set simply doesn't change. Crucially, a `Set` has **no concept of position** — there's no `get(0)`, no index, no ordering guarantee (unless you choose an implementation that provides one).

**Analogy:** A class roster. Each student appears exactly once, no matter how many times you try to sign them up. The order students are listed in is up to the teacher (implementation), but the rule "every student once" never bends.

**Real-world uses:** deduplicating email addresses, tracking which URLs have been visited, tracking online users, holding valid country codes.

### The `equals`/`hashCode` Contract

A `Set` decides "duplicate?" by asking two questions of every element: **`equals()`** and **`hashCode()`**. For hash-based sets, Java first buckets objects by `hashCode()`; if two objects land in the same bucket, it checks `equals()` to confirm.

The contract (and the single most tested concept in Java interviews):
- If `a.equals(b)` is `true`, then `a.hashCode() == b.hashCode()` **must** be true.
- The reverse is *not* required: two unequal objects may share a hash code (that's a *collision*, handled fine by the framework).
- You should override both together. Override `equals` only, and `HashSet` will happily store two objects that are equal because their hash codes differ.

**Pitfall — mutable objects in a `Set`:** if you insert an object into a `HashSet` and then mutate a field that participates in `hashCode()`/`equals()`, the object's hash changes while it sits in the wrong bucket. The set becomes corrupted: `contains()` returns false for an object that's visibly in the set. **Rule of thumb:** use immutable elements (like `String`, `Integer`, or your own immutable records) in sets.

### Three Implementations

**`HashSet`** — backed by a `HashMap`. Unordered (looks random), average **O(1)** add/remove/contains.

**`LinkedHashSet`** — a `HashSet` that additionally threads a linked list through its entries, preserving **insertion order**. Same O(1) performance, slightly more memory.

**`TreeSet`** — backed by a red-black tree. Keeps elements **sorted** (natural order or by a `Comparator` you supply). All operations are **O(log n)**. Implements `NavigableSet` for range operations like `floor`, `ceiling`, `subSet`.

```java
import java.util.*;

public class SetDemo {
    public static void main(String[] args) {
        // HashSet — unordered, O(1)
        Set<String> visited = new HashSet<>();
        visited.add("https://a.com");
        visited.add("https://b.com");
        visited.add("https://a.com");      // ignored — already present
        System.out.println(visited);       // some order, e.g. [https://b.com, https://a.com]

        // LinkedHashSet — insertion order
        Set<String> recent = new LinkedHashSet<>();
        recent.add("product-1");
        recent.add("product-2");
        recent.add("product-1");           // ignored; "product-1" stays FIRST
        System.out.println(recent);        // [product-1, product-2]

        // TreeSet — sorted
        Set<Integer> ids = new TreeSet<>(Set.of(5, 1, 4, 2, 3));
        System.out.println(ids);           // [1, 2, 3, 4, 5]

        TreeSet<Integer> nav = new TreeSet<>(ids);
        System.out.println(nav.floor(3));   // 3  (≤ 3)
        System.out.println(nav.ceiling(3)); // 3  (≥ 3)
        System.out.println(nav.lower(3));   // 2
        System.out.println(nav.higher(3));  // 4
        System.out.println(nav.subSet(2, true, 4, true)); // [2, 3, 4]
    }
}
```

### Choosing a Set

| Property | `HashSet` | `LinkedHashSet` | `TreeSet` |
|---|---|---|---|
| Order | None (arbitrary) | **Insertion order** | **Sorted** (natural/comparator) |
| Add / Remove / Contains | O(1) average | O(1) average | O(log n) |
| `null` allowed | Yes (one) | Yes (one) | No (needs comparisons) |
| Navigable ops (`floor`, `subSet`) | No | No | Yes |
| When to use | Dedup where order is irrelevant | Preserve arrival order + dedup | Sorted unique data, ranges |

### Set Algebra: Union, Intersection, Difference

The `Set` interface builds on `Collection`'s bulk methods to give you one-line set algebra:

```java
import java.util.*;

public class SetAlgebra {
    public static void main(String[] args) {
        Set<String> teamA = new HashSet<>(List.of("Ada", "Grace", "Alan"));
        Set<String> teamB = new HashSet<>(List.of("Alan", "Linus", "Margaret"));

        Set<String> union = new HashSet<>(teamA);
        union.addAll(teamB);                  // everyone
        System.out.println("Union:        " + union);

        Set<String> intersection = new HashSet<>(teamA);
        intersection.retainAll(teamB);        // people on both teams
        System.out.println("Intersection: " + intersection);

        Set<String> difference = new HashSet<>(teamA);
        difference.removeAll(teamB);          // only on teamA
        System.out.println("Difference:   " + difference);
    }
}
```

### Runnable Example: Deduplicate and Rank

```java
import java.util.*;

public class SetExample {
    public static void main(String[] args) {
        List<String> rawEmails = List.of(
            "ada@x.com", "grace@y.com", "ada@x.com", "ada@x.com", "linus@z.com");

        Set<String> unique = new LinkedHashSet<>(rawEmails);   // dedup, keep order
        System.out.println("Unique signups (in order): " + unique);

        // Sorted view of the same data
        Set<String> sorted = new TreeSet<>(unique);
        System.out.println("Alphabetical:             " + sorted);
    }
}
```

> **When to use `Set`:** the requirement is *uniqueness* — every element must appear exactly once. Pick `HashSet` when order doesn't matter, `LinkedHashSet` when arrival order matters, `TreeSet` when you need sorted, navigable unique data.

---

## Queue — FIFO (First-In, First-Out)

A **`Queue`** processes elements in **FIFO order** — the first element added is the first one removed. Think of it as a line of people at a ticket counter: the person who arrived earliest gets served first.

**Analogy:** A printer queue. You send five documents; the printer works through them in the order they arrived, no matter how short the fifth document is.

**Real-world uses:** task scheduling, message/event processing pipelines, breadth-first search in graphs, order processing, job queues for background workers.

### The Two Families of Methods

The `Queue` interface has two parallel sets of methods that differ only in how they signal failure:

| Operation | Throws exception | Returns special value |
|---|---|---|
| Add to tail | `add(e)` → throws if full | `offer(e)` → `false` if full |
| Remove from head | `remove()` → throws if empty | `poll()` → `null` if empty |
| Peek at head | `element()` → throws if empty | `peek()` → `null` if empty |

The "returns null" family (`offer`/`poll`/`peek`) is almost always what you want for real-world code — it's how you check "is there work to do?" without catching exceptions. `add`/`remove`/`element` exist for when you *require* the operation to succeed.

```java
import java.util.*;

public class QueueBasics {
    public static void main(String[] args) {
        Queue<String> line = new LinkedList<>();

        line.offer("first");      // polite add
        line.offer("second");
        line.add("third");        // throwing variant

        System.out.println(line.peek());    // "first" (don't remove)
        System.out.println(line.element()); // "first"

        while (!line.isEmpty()) {
            String next = line.poll();      // null when empty — safe
            System.out.println("Processing " + next);
        }

        System.out.println(line.poll());    // null
        // System.out.println(line.remove()); // throws NoSuchElementException!
    }
}
```

### `PriorityQueue` — FIFO With an Attitude

Sometimes "first in" shouldn't win. A hospital ER doesn't treat patients by arrival — it treats the *most critical* patient first. That's a **`PriorityQueue`**: elements come out in **priority order** (smallest first by default), not arrival order.

Under the hood it's a **binary min-heap**: a tree where every parent is smaller than its children, giving **O(log n)** insert and remove of the smallest element, and O(1) peek.

```java
import java.util.*;

public class Triage {
    record Patient(String name, int severity) {}

    public static void main(String[] args) {
        // Higher severity number = sicker = treated first (so reverse natural order)
        Queue<Patient> er = new PriorityQueue<>(
            Comparator.comparingInt(Patient::severity).reversed());

        er.offer(new Patient("Alice", 3));
        er.offer(new Patient("Bob",   5));
        er.offer(new Patient("Carol", 1));
        er.offer(new Patient("Dave",  4));

        while (!er.isEmpty()) {
            System.out.println("Treating: " + er.poll().name());
        }
        // Output: Bob (5), Dave (4), Alice (3), Carol (1)
        // NOT arrival order — priority wins.
    }
}
```

Note: `PriorityQueue` orders by **natural ordering** of elements unless you give it a `Comparator` (as above). It is *not* strictly FIFO — equal-priority elements come out in an unspecified order.

### Runnable Example: Print Job Queue + BFS Skeleton

```java
import java.util.*;

public class PrinterQueue {
    public static void main(String[] args) {
        Queue<String> jobs = new LinkedList<>(List.of("report.pdf", "invoice.pdf", "memo.docx"));
        jobs.offer("slides.pptx");

        int id = 1;
        while (!jobs.isEmpty()) {
            System.out.println("Job " + id++ + ": printing " + jobs.poll());
        }
    }
}

class BfsSkeleton {
    // Breadth-first search consumes a queue in FIFO order.
    // "Visit everything one level away before going deeper."
    static void bfs(Map<String, List<String>> graph, String start) {
        Queue<String> frontier = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        frontier.offer(start);
        visited.add(start);

        while (!frontier.isEmpty()) {
            String node = frontier.poll();
            System.out.println("Visiting: " + node);
            for (String neighbor : graph.getOrDefault(node, List.of())) {
                if (visited.add(neighbor)) {      // add() returns true if new
                    frontier.offer(neighbor);
                }
            }
        }
    }
}
```

> **When to use `Queue`:** work must be processed in arrival order (or priority order). Use `LinkedList` or `ArrayDeque` for plain FIFO, `PriorityQueue` when "most important first" beats "first come, first served."

---

## Deque — Double-Ended Queue

A **`Deque`** (pronounced "deck") is a queue where you can add **and** remove from **both ends**. It's a superset of `Queue` and a modern replacement for the legacy `Stack`.

**Analogy:** A deck of cards. You can deal off the top or take from the bottom; you can put a card on top or slip one underneath.

**Real-world uses:** undo/redo history, sliding-window algorithms, browser back/forward navigation, palindrome checking, and any LIFO stack use.

### The Method Family

`Deque` gives you twelve core operations — `First` and `Last` versions of each of the six queue operations, again in both "throws" and "returns null/false" flavors:

| Operation | Throws if empty | Returns null/false |
|---|---|---|
| Add at front | `addFirst(e)` | `offerFirst(e)` |
| Add at back | `addLast(e)` | `offerLast(e)` |
| Remove at front | `removeFirst()` | `pollFirst()` |
| Remove at back | `removeLast()` | `pollLast()` |
| Peek at front | `getFirst()` | `peekFirst()` |
| Peek at back | `getLast()` | `peekLast()` |

```java
import java.util.*;

public class DequeBasics {
    public static void main(String[] args) {
        Deque<String> deck = new ArrayDeque<>();

        deck.addLast("C");                  // [C]
        deck.addLast("D");                  // [C, D]
        deck.addFirst("B");                 // [B, C, D]
        deck.addFirst("A");                 // [A, B, C, D]

        System.out.println(deck.peekFirst()); // A
        System.out.println(deck.peekLast());  // D
        System.out.println(deck.pollFirst()); // A  → removes front
        System.out.println(deck.pollLast());  // D  → removes back
        System.out.println(deck);             // [B, C]
    }
}
```

### Using a `Deque` as a Stack

Here's a nice historical moment: the legacy **`Stack`** class (from Java 1.0) is officially deprecated in modern Java. Its replacement is... a `Deque`. When used with `push`/`pop`/`peek`, a `Deque` behaves as a perfect **LIFO** stack:

```java
Deque<String> stack = new ArrayDeque<>();
stack.push("first");    // addFirst
stack.push("second");   // addFirst
stack.push("third");    // addFirst
System.out.println(stack.peek()); // third
System.out.println(stack.pop());  // third
System.out.println(stack.pop());  // second
```

**Why not `Stack`?** Legacy `Stack` extends `Vector`, which means it inherits random-index methods like `get(i)` and `add(i, e)` that make no sense for a stack — you can insert into the middle of a stack, breaking its contract. `ArrayDeque` has no such leakage and is faster (no synchronization overhead).

### Choosing a Deque Implementation

| Use case | `ArrayDeque` | `LinkedList` | legacy `Stack` |
|---|---|---|---|
| As a stack (LIFO) | **Best** — fastest, no index pollution | Fine | ❌ Avoid (deprecated) |
| As a FIFO queue | **Best** | Fine | — |
| Middle-element access needed | ❌ No `get(i)` | `LinkedList` supports it | — |
| `null` elements | ❌ Rejected | ✅ Allowed | — |
| Memory | Compact circular array | Node overhead | Vector overhead |

`ArrayDeque` is the clear winner for both stacks and queues. Its one quirk: it **rejects `null`** elements (by design, so `poll()` returning `null` unambiguously means "empty").

### Runnable Example: Undo/Redo + Palindrome Checker

```java
import java.util.*;

public class UndoRedo {
    public static void main(String[] args) {
        Deque<String> undo = new ArrayDeque<>();
        Deque<String> redo = new ArrayDeque<>();

        String doc = "";
        void set(String s) { undo.push(doc); doc = s; redo.clear(); }

        set("Hello");
        set("Hello world");
        set("Hello world!");
        System.out.println(doc);               // Hello world!

        undo: doc = undo.pop(); redo.push(doc);
        System.out.println(doc);               // Hello world
        undo: doc = undo.pop(); redo.push(doc);
        System.out.println(doc);               // Hello
        redo: doc = redo.pop(); undo.push(doc);
        System.out.println(doc);               // Hello world
    }
}

class PalindromeChecker {
    static boolean isPalindrome(String input) {
        Deque<Character> d = new ArrayDeque<>();
        for (char c : input.toLowerCase().toCharArray()) {
            if (Character.isLetterOrDigit(c)) d.addLast(c);
        }
        while (d.size() > 1) {
            if (d.pollFirst() != d.pollLast()) return false;
        }
        return true;
    }

    public static void main(String[] args) {
        System.out.println(isPalindrome("racecar"));          // true
        System.out.println(isPalindrome("A man, a plan, a canal: Panama")); // true
        System.out.println(isPalindrome("hello"));             // false
    }
}
```

> **When to use `Deque`:** you need to add or remove at *either* end — undo stacks, sliding windows, back/forward navigation, or any time you'd previously reach for `Stack` or a linked list to fake a queue.

---

## Map — Key-Value Pairs

A **`Map<K, V>`** stores **key-value pairs**. Each **key is unique**; values may repeat freely. A `Map` is **not** a `Collection` — it doesn't hold individual elements, so it sits on its own branch of the hierarchy (though its views — `keySet()`, `values()`, `entrySet()` — *are* collections).

**Analogy:** A dictionary. The word is the key, the definition is the value. You look things up *by word*, not by page number. You can't have two identical entries for the same word, but two different words can share a definition.

**Real-world uses:** caching lookups (config name → value), counting word frequencies, indexes, phone books, in-memory caches.

### Core Operations

```java
import java.util.*;

public class MapBasics {
    public static void main(String[] args) {
        Map<String, Integer> ages = new HashMap<>();
        ages.put("Ada", 36);
        ages.put("Grace", 40);
        ages.put("Ada", 37);                 // overwrites → value 37

        System.out.println(ages.get("Ada"));        // 37
        System.out.println(ages.get("Linus"));      // null (not present)
        System.out.println(ages.getOrDefault("Linus", -1)); // -1

        System.out.println(ages.containsKey("Ada")); // true
        System.out.println(ages.containsValue(40));  // true
        ages.remove("Grace");

        // Views
        Set<String> keys   = ages.keySet();
        Collection<Integer> values = ages.values();
        Set<Map.Entry<String, Integer>> entries = ages.entrySet();

        // Iteration (three equivalent ways)
        for (Map.Entry<String, Integer> e : ages.entrySet()) {
            System.out.println(e.getKey() + " -> " + e.getValue());
        }
        ages.forEach((k, v) -> System.out.println(k + " -> " + v));

        // The modern power tools
        ages.computeIfAbsent("Alan", k -> k.length());          // insert if absent
        ages.merge("Ada", 1, Integer::sum);                     // count/update
        System.out.println(ages);
    }
}
```

### The Three Workhorses

**`HashMap`** — backed by a **hash table**: an array of *buckets*, each bucket holding zero or more entries. `put` and `get` hash the key to find a bucket, then search only that bucket. Average **O(1)** for put/get/remove. Allows **one `null` key** and many `null` values. No ordering guarantee.

**`LinkedHashMap`** — `HashMap` plus a doubly-linked list through entries, preserving **insertion order**. Can be constructed with `accessOrder=true` to reorder on access — which makes building an **LRU (Least Recently Used) cache** nearly trivial.

**`TreeMap`** — a red-black tree keyed by **sorted key order** (natural or via `Comparator`). All ops **O(log n)**. Implements `NavigableMap` for range queries like `subMap`, `floorKey`, `higherKey`.

```java
import java.util.*;

public class MapImplementations {
    public static void main(String[] args) {
        // HashMap: fastest, unordered
        Map<String, Integer> hash = new HashMap<>();
        hash.put("b", 2); hash.put("a", 1); hash.put("c", 3);
        System.out.println(hash);   // arbitrary order

        // LinkedHashMap: insertion order
        Map<String, Integer> linked = new LinkedHashMap<>();
        linked.put("b", 2); linked.put("a", 1); linked.put("c", 3);
        System.out.println(linked); // {b=2, a=1, c=3}

        // TreeMap: sorted by key + range queries
        TreeMap<String, Integer> tree = new TreeMap<>(Map.of(
            "alpha", 1, "beta", 2, "charlie", 3, "delta", 4));
        System.out.println(tree.firstKey());                    // alpha
        System.out.println(tree.floorKey("charlie"));           // charlie
        System.out.println(tree.subMap("beta", true, "delta", false)); // {beta=2, charlie=3}
    }
}
```

### How Hash Tables Work Under the Hood (Briefly)

A `HashMap` doesn't store entries in a list — it stores them in an array of **buckets**. When you `put("Ada", 36)`:

1. Java calls `"Ada".hashCode()`, an `int`.
2. It compresses that hash into a bucket index (`hash % buckets.length`, roughly).
3. It drops the entry into that bucket.

Two different keys landing in the same bucket is a **collision** — perfectly legal; the bucket just holds a small chain (or tree) of entries, and Java checks each with `equals()`. When the map gets too full (beyond the **load factor**, default 0.75), it *resizes*: creates a bigger bucket array and re-hashes everything — an O(n) operation that happens rarely enough to keep `put` at amortized O(1).

The quality of your keys' `hashCode()` directly determines performance. If every object returns `hashCode() == 42`, every key lands in one bucket and your "O(1)" map degrades to O(n).

### Common Pitfalls

1. **Mutable keys.** If a key's `hashCode()` depends on mutable state and you change it after insertion, the entry is lost in the wrong bucket — same disease as mutable set elements. Keys should be immutable (`String`, `Integer`, records).
2. **Relying on `HashMap` iteration order.** It's genuinely arbitrary and can change with Java version and map size. If order matters, use `LinkedHashMap` or `TreeMap`.
3. **`null` differences.** `HashMap` and `LinkedHashMap` allow one `null` key; `TreeMap` throws `NullPointerException` on a `null` key (it must compare keys). `ConcurrentHashMap` (thread-safe) forbids `null` entirely.
4. **`get` returning `null` is ambiguous.** It could mean "absent" *or* "present with value null." Prefer `containsKey` or `getOrDefault` to disambiguate.

### Choosing a Map

| Property | `HashMap` | `LinkedHashMap` | `TreeMap` |
|---|---|---|---|
| Order | None | **Insertion order** | **Sorted by key** |
| Get / Put / Remove | O(1) average | O(1) average | O(log n) |
| `null` key | One allowed | One allowed | Not allowed |
| Navigable ops | No | No | Yes |
| When to use | Fastest lookups, order irrelevant | Config/caching where order matters, LRU caches | Sorted keys, range queries |

### Runnable Example: Word-Frequency Counter + Top-N

```java
import java.util.*;
import java.util.stream.*;

public class WordFrequencies {
    public static void main(String[] args) {
        String text = "the quick brown fox jumps over the lazy dog and the quick fox";

        // Count frequencies with computeIfAbsent
        Map<String, Integer> counts = new HashMap<>();
        for (String w : text.split(" ")) {
            counts.merge(w, 1, Integer::sum);   // or computeIfAbsent + put
        }
        System.out.println(counts);

        // Top-3 by frequency (tie-break alphabetically)
        counts.entrySet().stream()
              .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
              .limit(3)
              .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
        // the: 3, fox: 2, quick: 2
    }
}
```

> **When to use `Map`:** you need to look up a value *by key* quickly. Use `HashMap` by default, `LinkedHashMap` when insertion order matters (caches, configs), `TreeMap` when you need sorted keys or range queries.

---

## Summary: How to Choose

By now you've met every major player. Here's the entire decision process distilled into one flow:

### Decision Flow

| Your requirement | Reach for |
|---|---|
| Indexed access by position (`get(3)`) | `ArrayList` |
| Ordered, duplicates allowed, append-heavy | `ArrayList` |
| Uniqueness only, order irrelevant | `HashSet` |
| Uniqueness + insertion order | `LinkedHashSet` |
| Uniqueness + sorted/navigable | `TreeSet` |
| FIFO processing | `ArrayDeque` (or `LinkedList`) |
| Process by priority | `PriorityQueue` |
| LIFO stack | `ArrayDeque` (with `push`/`pop`) |
| Add/remove from both ends | `ArrayDeque` |
| Key → value lookup | `HashMap` |
| Key → value, insertion order | `LinkedHashMap` |
| Key → value, sorted keys/range queries | `TreeMap` |

### Performance Cheat-Sheet

| Type | Structure | Order | Duplicates | `null` | Add | Get/Contains | Remove |
|---|---|---|---|---|---|---|---|
| `ArrayList` | Resizable array | Indexed | Yes | Yes | O(1) amortized | **O(1)** | O(n) |
| `LinkedList` | Doubly-linked list | Indexed | Yes | Yes | O(1)* | O(n) | O(n) |
| `HashSet` | Hash table | None | No | Yes | O(1) avg | O(1) avg | O(1) avg |
| `LinkedHashSet` | Hash + linked list | Insertion | No | Yes | O(1) avg | O(1) avg | O(1) avg |
| `TreeSet` | Red-black tree | Sorted | No | No | O(log n) | O(log n) | O(log n) |
| `ArrayDeque` | Circular array | Positional | Yes | **No** | O(1) | — | O(1) |
| `PriorityQueue` | Binary heap | Priority | Yes | No | O(log n) | O(1) peek | O(log n) |
| `HashMap` | Hash table | None | Keys unique | Keys: 1 | O(1) avg | O(1) avg | O(1) avg |
| `TreeMap` | Red-black tree | Sorted keys | Keys unique | Keys: No | O(log n) | O(log n) | O(log n) |

\* `LinkedList` add is O(1) at the ends, O(n) to find a middle position first.

### Java 8+ Everywhere

Everything in this chapter is enhanced by modern Java idioms:

- **Streams:** `collection.stream().filter(...).map(...).collect(...)` turns any collection into a pipeline.
- **`forEach`:** `map.forEach((k, v) -> ...)`, `list.forEach(System.out::println)`.
- **Lambdas & `Comparator`:** `list.sort(Comparator.comparingInt(String::length))`, `PriorityQueue` with a custom comparator.
- **Convenience factories:** `List.of(...)`, `Set.of(...)`, `Map.of(...)` for immutable constants.

### Tying It All Together: Customer Support Ticket System

This final program uses **every** major interface — a `List` of ticket history, a `Set` of agent IDs, a `PriorityQueue` for triage, a `Deque` for the work log, and a `Map` for stats:

```java
import java.util.*;

public class TicketSystem {
    record Ticket(int id, int priority, String customer) {}

    public static void main(String[] args) {
        // PriorityQueue: highest-priority ticket served first (lower number = higher priority)
        Queue<Ticket> queue = new PriorityQueue<>(
            Comparator.comparingInt(Ticket::priority));

        queue.offer(new Ticket(1, 3, "Alice"));   // standard
        queue.offer(new Ticket(2, 1, "Bob"));     // urgent
        queue.offer(new Ticket(3, 2, "Carol"));   // high

        // Set: every agent must be unique
        Set<String> agents = new LinkedHashSet<>(List.of("Alex", "Zoe", "Alex"));
        System.out.println("Available agents: " + agents);   // [Alex, Zoe]

        // Deque: audit trail — new entries at the front
        Deque<String> audit = new ArrayDeque<>();

        // List: each customer's service history
        Map<String, List<String>> history = new HashMap<>();

        // Map: per-agent ticket counts (computeIfAbsent pattern)
        Map<String, Integer> workload = new HashMap<>();

        while (!queue.isEmpty()) {
            Ticket t = queue.poll();
            String agent = agents.iterator().next();          // first available
            workload.merge(agent, 1, Integer::sum);
            history.computeIfAbsent(t.customer(), k -> new ArrayList<>())
                   .add("Handled by " + agent);
            audit.addFirst("ticket #" + t.id() + " -> " + agent);
            System.out.println("Assigning ticket #" + t.id()
                + " (priority " + t.priority() + ") from " + t.customer()
                + " to " + agent);
        }

        System.out.println("Workload:  " + workload);
        System.out.println("Audit:     " + audit);
        System.out.println("History:   " + history);
    }
}
```

```
Output:
Assigning ticket #2 (priority 1) from Bob to Alex
Assigning ticket #3 (priority 2) from Carol to Zoe
Assigning ticket #1 (priority 3) from Alice to Alex
Workload:  {Alex=2, Zoe=1}
Audit:     [ticket #1 -> Alex, ticket #3 -> Zoe, ticket #2 -> Alex]
History:   {Bob=[Handled by Alex], Carol=[Handled by Zoe], Alice=[Handled by Alex]}
```

Notice the pattern: each collection was chosen *for its behavior* — the queue guarantees triage order, the set guarantees agent uniqueness, the deque gives a newest-first audit trail, and the map gives instant workload lookup. That's the entire philosophy of the framework in one program: **say what you need, not how to build it.**

---

## Practice Problems

Work through these in order. Each is designed to exercise the decision skills from this chapter.

### 1. Word Frequency Counter (Warm-up)

Count the frequency of every word in a string, ignoring case and punctuation, then print words and counts sorted from most frequent to least.

- **Edge cases:** empty input; the same word in different cases; punctuation attached to words (`"world,"`).
- **Expected output:** `the: 3`, `quick: 2`, ... in descending frequency order.
- **Hint:** A `Map<String, Integer>` with `merge` handles counting; finish with a stream `sorted` on `Map.Entry`. Don't think — this is the exact pattern from Section 6.

### 2. Anagram Groups

Given a list of words, group words that are anagrams of each other. Return the groups.

```java
input:  ["eat", "tea", "tan", "ate", "nat", "bat"]
output: [["eat", "tea", "ate"], ["tan", "nat"], ["bat"]]
```

- **Edge cases:** single-character words; a word alone in its group; duplicates.
- **Hint:** the *signature* of an anagram group is a sorted char array — `"tea"` → `"aet"` → same as `"eat"`. Use a `Map<String, List<String>>` keyed by that sorted signature. This is one of the most common interview problems, and it's an exercise in `computeIfAbsent`.

### 3. LRU Cache

Design a cache that holds at most N key-value pairs, evicting the **least recently used** entry when full.

- **Operations:** `get(key)` (returns value, marks as recently used) and `put(key, value)` (inserts, or updates and marks recently used).
- **Edge cases:** updating an existing key shouldn't count as a new insertion; reads should reorder; capacity 0.
- **Hint:** a `LinkedHashMap` with `accessOrder=true` nearly does this for you — recall the `removeEldestEntry` hook (look it up in the `LinkedHashMap` docs). What's the alternative if you can't use that trick? (A `Deque` + `Map`, or a `Deque` of keys in insertion order.)

### 4. BFS on a Small Grid

Given an `m x n` grid of `0` and `1` cells, find the **minimum number of steps** to travel from the top-left cell to the bottom-right cell, moving only up/down/left/right, and only through `0` cells. If impossible, return `-1`.

```java
grid: {
  {0, 0, 0},
  {1, 1, 0},
  {0, 0, 0}
}
steps: 4   (0,0) → (0,1) → (0,2) → (1,2) → (2,2)
```

- **Edge cases:** impossible grids; a 1×1 grid; starting or ending cell blocked.
- **Hint:** BFS guarantees shortest paths on unweighted graphs — use a `Queue` (plain FIFO) for the frontier and a `Set` (or a `boolean[][]`) for visited cells so you never re-enqueue. Pair each cell with its distance. This is the exact BFS skeleton from Section 4, applied to coordinates instead of a graph.

### 5. Undo/Redo With Versioning

Extend the undo/redo example so the user can perform arbitrary operations (insert text, delete text, replace), undo any number of them, and redo. Clearing the redo stack when a new edit happens after an undo is mandatory.

- **Edge cases:** undo when the stack is empty (should be a no-op or return `false`, not throw); redo after a new edit (must be cleared); very deep undo chains (watch memory).
- **Hint:** model each edit as an object holding enough data to reverse it (`record Edit(String type, String before, String after)`), stored in two `Deque`s. This is the Section 5 pattern promoted to full production shape — and a good exercise in *designing* the "how to undo" logic, not just the data structure.

---

### The One-Paragraph Takeaway

A collection is never chosen in the abstract — it's chosen by *what you promise your users*: "I need to look up by position" → `List`; "every element must be unique" → `Set`; "process in arrival or priority order" → `Queue`; "work from both ends" → `Deque`; "look up by key" → `Map`. Program against the **interfaces**, pick the implementation that matches your ordering and complexity needs, and let the framework's battle-tested algorithms do the rest. Master the decision table in the summary, and you'll never hand-roll a resizing array again.

---

# List Implementations in Java

A Textbook Chapter on `ArrayList`, `LinkedList`, `Vector`, and `Stack`

---

## Learning Objectives

By the end of this chapter, students will be able to:

1. **Explain** the role of the `List<E>` interface within the Java Collections Framework and describe the relationship between an interface and its concrete implementations.
2. **Describe** the internal structure of `ArrayList<E>` (a resizable backing array) and analyze the time complexity of its core operations, including the *amortized* cost of `add`.
3. **Describe** the internal structure of `LinkedList<E>` (a chain of doubly-linked nodes) and explain why its time complexities differ from `ArrayList`'s.
4. **Contrast** the legacy `Vector<E>` class with `ArrayList<E>`, including the historical role, growth policy, and synchronization overhead of `Vector`.
5. **Evaluate** `java.util.Stack<E>` against the modern `ArrayDeque<E>` alternative, articulating the design flaws of the legacy `Stack` class.
6. **Choose** the appropriate list implementation for a given scenario by reasoning from structure → complexity → use case.
7. **Trace** the internal effect of key operations (resizing, node insertion, node removal) on the memory layout of each implementation.

---

## Introduction

Imagine you are building a contact list for a phone book app. Users need to jump instantly to contact #42, add a new contact between existing ones, and delete old entries. Now imagine you are building the undo-history for a text editor, where the newest action is always pushed onto the top and the oldest is removed first. Both problems deal with *ordered sequences of elements*, but they will reward *different* internal storage strategies.

In Java, both problems are expressed against the same interface, `List<E>`, but they are best solved with different *implementations*. The choice of **how** a list stores its elements — in a contiguous block of memory or in a chain of linked nodes — determines what it is fast at and what it is slow at. There is no universally "best" list; there are only lists that are best **for a particular pattern of access**.

This chapter explores the four `List` implementations that ship with the JDK:

- **`ArrayList<E>`** — the workhorse, backed by a resizable array.
- **`LinkedList<E>`** — backed by a doubly-linked chain of nodes.
- **`Vector<E>`** — a legacy synchronized sibling of `ArrayList`.
- **`Stack<E>`** — a LIFO (last-in, first-out) class that inherits from `Vector`.

For each, we will examine the *core idea*, the *internal structure* (with memory diagrams), a precise *time-complexity analysis*, the *common operations*, and the *strengths and limitations*. We will close with side-by-side comparisons, a decision guide for choosing an implementation, common pitfalls, practice exercises, and further reading.

> **Analogy:** Think of this chapter as a study of the *architecture* of warehouses. An `ArrayList` is a single enormous shelf where every box is at a known, numbered position. A `LinkedList` is a set of small boxes scattered around the warehouse, each containing a clue to where the next box is hidden. Both store the same merchandise; they are optimized for completely different kinds of work.

---

## 1. The `List<E>` Interface and Why Implementations Matter

### Where `List` sits in the Java Collections Framework

Every collection class in the JDK descends from a small set of interfaces. The hierarchy relevant to this chapter is:

```
Iterable<E>
   │
   └── Collection<E>
          │
          └── List<E>
                 ├── ArrayList<E>
                 ├── LinkedList<E>
                 ├── Vector<E>
                 └── Stack<E>            (extends Vector<E>)
```

Let's unpack each level:

- **`Iterable<E>`** — declares a single abstract method, `Iterator<E> iterator()`. Anything that is `Iterable` can be used in an enhanced `for` loop (`for (E x : collection)`). It is the root of the entire collections hierarchy.
- **`Collection<E>`** — extends `Iterable<E>` and adds the "bag of elements" contract: `add`, `remove`, `contains`, `size`, `isEmpty`, `clear`, and bulk operations. It says nothing about *order* or *position*.
- **`List<E>`** — extends `Collection<E>` and adds the crucial concept of **position**: elements are arranged in a sequence, each with an integer *index*, and the list permits **duplicate elements**. Its signature methods are:
  - `E get(int index)` and `E set(int index, E element)`
  - `void add(int index, E element)` and `E remove(int index)`
  - `int indexOf(Object o)` and `int lastIndexOf(Object o)`
  - `ListIterator<E> listIterator()`

> **Note:** A `Set<E>` is also a `Collection<E>`, but it *forbids* duplicates and has no notion of positional access. The distinguishing feature of a `List` is precisely this indexed, ordered view of its elements.

### Interface vs. implementation: "program to the interface"

The single most important habit you can develop is to **declare variables with the interface type and construct them with a concrete class**:

```java
import java.util.List;
import java.util.ArrayList;

public class ProgramToInterface {
    public static void main(String[] args) {
        // Declare the variable as List<String>, construct as ArrayList<String>
        List<String> courses = new ArrayList<>();

        courses.add("Data Structures");
        courses.add("Algorithms");
        courses.add("Databases");

        printAll(courses);            // works with ANY List implementation
        System.out.println(courses.get(0)); // "Data Structures"
    }

    public static void printAll(List<String> items) {
        for (String item : items) {
            System.out.println(item);
        }
    }
}
```

Why does this matter?

- **Flexibility:** If `printAll` accepted an `ArrayList` parameter, it could not accept a `LinkedList` or a `Vector`. By accepting `List<String>`, any implementation works.
- **Swappability:** If profiling shows that your code would be faster with a `LinkedList`, you change exactly one line — the constructor call — and the rest of the code is untouched.
- **Clarity:** The interface documents *what* the data structure guarantees, hiding *how* it is achieved.

> **Warning:** The converse habit — declaring everything as the concrete class (`ArrayList<String> x = new ArrayList<>();`) — couples your code to a specific implementation and makes future changes costly. Reserve concrete types for the construction site.

### Why have multiple implementations at all?

Because there is a fundamental trade-off in computer memory: **contiguous storage** gives instant indexed access but expensive middle insertions, while **linked storage** gives cheap insertions and deletions but slow indexed access. No single strategy dominates; each is a different point on the trade-off curve. This chapter is a tour of those trade-offs.

---

## 2. ArrayList<E>

### 2.1 Core Idea

The core idea of `ArrayList` is strikingly simple: **store the elements one after another in a contiguous block of memory — a plain array — and grow that array when it fills up.**

> **Analogy:** An `ArrayList` is a shelf of labeled boxes. Every box has a fixed position (`0`, `1`, `2`, …). If you know the label, you can walk straight to the box — no searching required. But if you want to *insert* a new box in the middle, you must shift every box after it one slot over, which is tiring work. And if the shelf runs out of room, you must buy a bigger shelf and move every box over to it.

### 2.2 Internal Structure (resizable array / backing array)

An `ArrayList` *wraps* an ordinary Java array, called the **backing array** (the JDK field is literally named `elementData`). Two companion integers matter:

- **`size`** — the number of elements the user has actually stored.
- **capacity** — the length of the backing array. The capacity is *not* exposed as a field in modern JDKs, but it is visible through `ensureCapacity(int)` behavior.

The relationship is: `size ≤ capacity`. Elements are packed contiguously from index `0` to `size - 1`; slots at indices `size` through `capacity - 1` are unused (they hold `null` or stale references).

```
Memory diagram of an ArrayList<String> with size = 3, capacity = 10:

        backing array (length 10)
        ┌────┬────┬────┬────┬────┬────┬────┬────┬────┬────┐
        │"Ada"│"Grace"│"Linus"│ null│ null│ null│ null│ null│ null│ null│
        └────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘
        index: 0    1    2    3    4    5    6    7    8    9
               └─────────┘                 └──────────────┘
               size = 3                    unused capacity

The ArrayList object holds a reference to this array plus the int size.
```

Because the backing array is contiguous, the address of element `i` can be computed directly: **base address + `i` × element size**. This is what gives `ArrayList` its O(1) random access: finding element 10,000 takes exactly the same number of steps as finding element 1.

### 2.3 Time Complexity Analysis

The following table summarizes `ArrayList`'s complexities. Here `n` is the number of elements in the list.

| Operation | Complexity | Notes |
|---|---|---|
| `get(i)` / `set(i, e)` | **O(1)** | Direct index into the backing array |
| `add(e)` (append) | **O(1) amortized** | Constant per call on average; occasionally O(n) when resizing |
| `add(i, e)` (insert) | **O(n)** | Shifts up to n elements to the right |
| `remove(i)` | **O(n)** | Shifts up to n elements to the left |
| `remove(e)` (by value) | **O(n)** | Linear search, then O(n) shift |
| `contains(e)` / `indexOf(e)` | **O(n)** | Linear scan (no hashing or ordering) |
| Iteration (enhanced `for`) | **O(n)** | Sequential walk over n elements |

> **Warning:** `contains` on an `ArrayList` is *linear*, not constant. Many students assume "it's a list, so searching is fast." No — search requires scanning every element unless the collection is a hash-based structure. The O(1) superpower of `ArrayList` is **indexed** access, not **search**.

#### The Amortized Complexity of `add(e)`

The append operation deserves special attention because its worst case is O(n) while its typical case is O(1). We summarize this with **amortized analysis**.

**What is amortized complexity?** Amortized analysis looks at the cost of a *sequence* of operations averaged over the whole sequence. It is **not** the same as average-case analysis (which depends on probability); amortized analysis is a *guarantee* about the total cost of any sequence of operations, expressed as a per-operation average.

**What actually happens when the array is full?** When `add(e)` is called and `size == capacity`, the JDK allocates a new, larger backing array and copies every existing element over:

```java
// Conceptual view of what ArrayList.add does internally when full:
Object[] old = elementData;                 // old array, length = capacity
int newCapacity = old.length + (old.length >> 1); // 1.5x growth factor
elementData = Arrays.copyOf(old, newCapacity);    // copy + null tail
elementData[size++] = e;                   // place the new element
```

> **Note on the JDK growth factor:** The JDK grows the array to roughly **1.5× its previous size** (`oldCapacity + (oldCapacity >> 1)`, where `>> 1` is integer division by 2). The initial capacity is the constant `DEFAULT_CAPACITY`, which equals **10**. You can pre-size an `ArrayList` with `new ArrayList<>(initialCapacity)` or with `ensureCapacity(n)` to avoid early resizes.

**The doubling-growth argument (exam-ready):** Suppose the capacity starts at 1 and doubles each time it is exceeded: 1, 2, 4, 8, 16, …. Consider inserting `n` elements. Resizes happen when capacity must jump from 1→2, 2→4, 4→8, … and the *total* work of all copies is:

```
1 + 2 + 4 + 8 + … + n  ≤  2n
```

That is a geometric series summing to less than **2n**. So the total cost of `n` insertions is `n` (the insertions themselves) + at most `2n` (the copies) = **3n**, which averages to **O(1) per insertion**. We say `add(e)` runs in *O(1) amortized* time: occasionally it is expensive, but the total across many calls is linear in the number of operations.

**Before/after resizing — what you'd see in memory:**

```
BEFORE:  size = 4, capacity = 4
         ┌────┬────┬────┬────┐
         │  A │  B │  C │  D │
         └────┴────┴────┴────┘
                 ↓  add(E)
AFTER:   size = 5, capacity = 6   (4 + 4>>1 = 6)
         ┌────┬────┬────┬────┬────┬────┐
         │  A │  B │  C │  D │  E │null│
         └────┴────┴────┴────┴────┴────┘
         (all old elements copied, E appended, one empty slot remains)
```

### 2.4 Common Operations

Here is a compendium of the operations you will use daily, with output annotations:

```java
import java.util.ArrayList;
import java.util.List;

public class ArrayListOps {
    public static void main(String[] args) {
        List<String> names = new ArrayList<>();

        // add — appends to the end, O(1) amortized
        names.add("Ada");       // ["Ada"]
        names.add("Grace");     // ["Ada", "Grace"]
        names.add("Linus");     // ["Ada", "Grace", "Linus"]

        // add at an index — shifts everything right, O(n)
        names.add(1, "Alan");   // ["Ada", "Alan", "Grace", "Linus"]

        // get — O(1) random access
        System.out.println(names.get(0));      // Ada

        // set — replaces in place, O(1)
        names.set(3, "Margaret"); // ["Ada", "Alan", "Grace", "Margaret"]

        // remove by index — shifts everything left, O(n)
        String removed = names.remove(1);      // removes "Alan"
        System.out.println(removed);           // Alan

        // remove by value — search O(n) then shift O(n)
        boolean didRemove = names.remove("Grace");
        System.out.println(didRemove);         // true

        // contains / indexOf — linear search, O(n)
        System.out.println(names.contains("Ada"));    // true
        System.out.println(names.indexOf("Margaret")); // 2

        // iteration — O(n), works because List extends Iterable
        for (String name : names) {
            System.out.println(name);
        }

        // other handy methods
        System.out.println(names.size());      // 2
        System.out.println(names.isEmpty());   // false
    }
}
```

### 2.5 Strengths and Limitations

**Strengths:**
- **O(1) indexed access** — the fastest way to read or overwrite element `i` in any Java list.
- **Cache-friendly** — contiguous memory means the CPU's cache can prefetch elements during iteration, making sequential scans dramatically faster in practice than the big-O suggests.
- **Minimal memory overhead** — only the backing array plus a `size` integer; no per-element storage cost beyond the elements themselves.
- **Tiny constant factors** — no dereferencing chains, no node objects.

**Limitations:**
- **O(n) insertions and removals in the middle** — every operation requires shifting up to n elements.
- **O(n) insertions at the *front*** — even `add(0, e)` is O(n), which is the worst position for an `ArrayList`.
- **O(n) `contains`** — no fast search.
- **Resize hiccups** — occasional O(n) reallocation pauses (mitigated by the amortized guarantee and by pre-sizing).

> **Where You'll Use This:** `ArrayList` dominates real-world Java code. Use it for **maintaining records** (rows from a database, results of a query), **in-memory caches** and buffers where you read frequently by index, **UI list models** (the data behind a listbox or table), and anywhere **fast indexed access dominates**. If your data is mostly *read* and *appended*, `ArrayList` is almost always the right call.

---

## 3. LinkedList<E>

### 3.1 Core Idea

The core idea of `LinkedList` is the opposite of `ArrayList`: do **not** keep elements in contiguous memory. Instead, wrap each element in a **node** that also stores a reference to the next node (and, in a doubly-linked list, the previous one). The list is traversed by following these references.

> **Analogy:** A `LinkedList` is a treasure hunt. You are handed a box containing a clue ("The next clue is hidden at the old oak tree"). You travel to the oak tree, open the next box, and get another clue. Adding a new clue *between* two boxes is easy — you just write two new clues. But finding the 100th box requires walking through all 99 boxes before it: there is no address to jump straight to.

### 3.2 Internal Structure (doubly-linked nodes)

Since Java 6, the JDK's `LinkedList` is a **doubly-linked list**: each node points both *forward* and *backward*, and the list holds references to the **first** node and the **last** node.

Each node stores:
- `item` — the element itself (`E`),
- `next` — a reference to the following node (or `null` at the tail),
- `prev` — a reference to the preceding node (or `null` at the head).

```
Memory diagram of a LinkedList<String> with 3 elements:

   LinkedList object
   ┌──────────────┐
   │ first ───────┼───────────┐
   │ last  ───────┼───────┐   │
   │ size  = 3    │       │   │
   └──────────────┘       │   │
                          ▼   ▼
   ┌─────────┐   ┌─────────┐   ┌─────────┐
   │ "Ada"   │   │ "Grace" │   │ "Linus" │
   │ next ───┼──▶│ next ───┼──▶│ next  ──┼──▶ null
   │ prev ───┼──◀│ prev ───┼──◀│ prev  ──┼──◀┐
   └─────────┘   └─────────┘   └─────────┘    │
        ▲                                      │
        └──────────────────────────────────────┘
        (first's prev = null, last's next = null)
```

Two references to the ends are what make this structure special: inserting or removing at **either end** touches only a constant number of nodes.

**Before/after — inserting a node in the middle:**

```
BEFORE:   A ──▶ B ──▶ C        insert X between A and B
          ◀──   ◀──   ◀──

          A ──▶ X ──▶ B ──▶ C
          ◀──   ◀──   ◀──   ◀──
AFTER:    (new node X created; A.next and B.prev retargeted)
```

Only two links are rewired — nodes `A` and `B` are touched, and nothing needs to move. This is the fundamental advantage of a linked structure.

### 3.3 Time Complexity Analysis

| Operation | Complexity | Notes |
|---|---|---|
| `get(i)` / `set(i, e)` | **O(n)** | Must walk from an end until index `i` is reached |
| `add(e)` (append) | **O(1)** | Tail is known; create node, link after tail |
| `addFirst(e)` / `addLast(e)` | **O(1)** | Constant work at either end |
| `add(i, e)` (insert) | **O(n)** | O(n) to *find* position, then O(1) to link |
| `removeFirst()` / `removeLast()` | **O(1)** | Constant work at the ends |
| `remove(i)` | **O(n)** | O(n) to find the node, then O(1) to unlink |
| `remove(e)` (by value) | **O(n)** | Linear scan for the value |
| `contains(e)` / `indexOf(e)` | **O(n)** | Linear scan |
| Iteration | **O(n)** | Sequential walk following `next` pointers |

> **Exam-critical nuance:** Inserting at the *middle* of a `LinkedList` is **not** O(1). It is O(n) — the O(1) part is only the linking itself, *after* you have found the node, and finding the node costs O(n) because there is no indexed access. Students who answer "O(1) insertion" without qualification lose marks.

### 3.4 Common Operations

```java
import java.util.LinkedList;

public class LinkedListOps {
    public static void main(String[] args) {
        LinkedList<String> queue = new LinkedList<>();

        // Deque-style operations — O(1) at both ends
        queue.addFirst("read email");       // [read email]
        queue.addFirst("commit code");      // [commit code, read email]
        queue.addLast("write report");      // [commit code, read email, write report]

        System.out.println(queue.getFirst()); // commit code
        System.out.println(queue.getLast());  // write report

        String first = queue.removeFirst();   // commit code
        String last = queue.removeLast();     // write report

        // List operations still work — but indexed access is O(n)!
        queue.add("alpha");
        queue.add("beta");
        queue.add("gamma");
        String mid = queue.get(1);            // beta  — but it walked 2 nodes to find it

        // listIterator — the recommended way to traverse and mutate
        var it = queue.listIterator();
        while (it.hasNext()) {
            String s = it.next();
            if (s.equals("beta")) {
                it.set("BETA");               // replace without re-searching
                it.add("beta-copy");          // insert right here, O(1)
            }
        }
        System.out.println(queue);            // [alpha, BETA, beta-copy, gamma]
    }
}
```

> **Note:** `LinkedList` implements both `List<E>` *and* `Deque<E>`, which is why it offers `addFirst`, `addLast`, `removeFirst`, and friends. In modern code, if you only need queue/deque behavior, prefer `ArrayDeque` (see Section 5) — but `LinkedList` remains useful when you also need list semantics.

### 3.5 Strengths and Limitations

**Strengths:**
- **O(1) operations at both ends** — perfect for queues and stacks built on a list.
- **O(1) linking/unlinking once a node is found** — cheap structural edits.
- **No resizing ever** — memory is allocated per-node, on demand; no wasted tail capacity.

**Limitations:**
- **O(n) indexed access** — `get(i)` must walk the chain; this is devastating for "random access" patterns.
- **Poor cache behavior** — nodes are scattered across the heap, so sequential iteration triggers cache misses, often making it *several times slower* than `ArrayList` iteration in practice despite the same big-O.
- **Memory overhead** — every element carries two extra references (`prev`/`next`), roughly 16–24 bytes per element that an `ArrayList` does not pay.

> **Where You'll Use This:** Reach for `LinkedList` when **insertions and removals happen predominantly at the ends**, when traversal is **sequential** (always from head to tail), and when you are building **queue/deque-like structures** or **LRU (least-recently-used) style caches** where you repeatedly move elements from one end to the other. If your code calls `get(i)` in a loop, `LinkedList` is a performance trap.

---

## 4. Vector<E>

### 4.1 Core Idea and Historical Context

`Vector` is the **grandfather of Java collections** — it has existed since JDK 1.0 (1996), before the Collections Framework was designed. Its core idea is identical to `ArrayList`: a resizable array with O(1) indexed access. It is, in fact, structurally so similar that many consider `ArrayList` the "modern replacement for `Vector`."

> **Analogy:** `Vector` and `ArrayList` are like two vintages of the same car model. One (the 1996 edition) came with heavy armor plating welded to the chassis; the other (the modern edition) is the same frame with the plating removed — faster, lighter, and equally capable for most driving.

> **Historical note:** When `Vector` was written, Java had no Collections Framework and no concurrency utilities. The designers made a defensive choice: they **synchronized every method**, so any thread calling `add` or `get` automatically acquires the object's monitor. It seemed prudent at the time; it turned out to be a design liability.

### 4.2 Internal Structure and Capacity/Growth Behavior (`capacityIncrement`)

`Vector` stores elements in a contiguous backing array exactly like `ArrayList`. The one structural difference is the **growth policy**:

- `ArrayList` always grows to **1.5×** its current capacity.
- `Vector` grows according to an optional `capacityIncrement` value:
  - If `capacityIncrement > 0`, the capacity grows by *exactly* that many slots.
  - If `capacityIncrement <= 0` (the default), the capacity **doubles**.

```java
import java.util.Vector;

public class VectorGrowth {
    public static void main(String[] args) {
        // default: capacityIncrement <= 0  →  capacity DOUBLES when full
        Vector<String> v1 = new Vector<>(5);
        for (int i = 0; i < 12; i++) {
            v1.add("item " + i);          // triggers growth at 5, then 10
        }
        System.out.println("v1 capacity: " + v1.capacity()); // 20 (5→10→20)

        // custom: capacityIncrement = 3 → grows by exactly 3 each time
        Vector<String> v2 = new Vector<>(5, 3);
        for (int i = 0; i < 12; i++) {
            v2.add("item " + i);          // grows 5→8→11→14
        }
        System.out.println("v2 capacity: " + v2.capacity()); // 14
    }
}
```

```
Memory diagram of a Vector with size = 5 and capacity = 10:

   Vector object
   ┌──────────────────┐
   │ elementData ─────┼──▶ ┌────┬────┬────┬────┬────┬────┬────┬────┬────┬────┐
   │ elementCount = 5 │     │ a  │ b  │ c  │ d  │ e  │null│null│null│null│null│
   │ capacityIncr = 0 │     └────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘
   └──────────────────┘
```

`Vector` also exposes `capacity()` and `ensureCapacity(int)` publicly — methods `ArrayList` deliberately hides, because most users never need them.

### 4.3 Synchronization and Its Performance Cost

Every mutating and reading method of `Vector` is marked `synchronized`. This means each call acquires the object's intrinsic lock:

```java
public synchronized boolean add(E e) { ... }   // conceptual — Vector's actual code
public synchronized E get(int index) { ... }
```

**Why is this a problem?**

1. **Performance:** Lock acquisition has a real cost (atomic instructions, cache-line contention). A single-threaded program using `Vector` pays this tax on *every* call with zero benefit. Measured overhead of a few percent to tens of percent is typical.
2. **Insufficient for real concurrency anyway:** Synchronizing *individual methods* does **not** make compound operations atomic. Consider the classic check-then-act:
   ```java
   if (!vector.isEmpty()) {          // synchronized, but the lock is released here
       Object x = vector.get(0);     // another thread could remove() in between!
   }
   ```
   Two threads can still interleave between these two calls. Correct multi-threaded use still requires *external* synchronization around the compound operation — at which point the internal locks are redundant.
3. **Legacy baggage:** `Stack` extends `Vector` (Section 5), inheriting this behavior.

> **Warning:** **Do not use `Vector` in new code.** If you need a synchronized list, the modern idioms are `Collections.synchronizedList(new ArrayList<>())` (which wraps a modern list) or, better, the concurrent collections in `java.util.concurrent` (e.g., `CopyOnWriteArrayList`) that provide genuinely better thread-safety guarantees. `Vector` exists so that ancient code still compiles and runs; it should not be your first choice for anything new.

### 4.4 Vector vs. ArrayList

| Characteristic | `Vector<E>` | `ArrayList<E>` |
|---|---|---|
| **Introduced** | JDK 1.0 (1996) | JDK 1.2 (1998), as part of the Collections Framework |
| **Internal storage** | Resizable backing array | Resizable backing array |
| **Indexed access** | O(1) | O(1) |
| **Growth policy** | Doubles by default; or grows by `capacityIncrement` | Grows to 1.5× current size |
| **Thread safety** | All methods `synchronized` (method-level) | Not synchronized |
| **Single-thread performance** | Slower (lock overhead on every call) | Faster |
| **Capacity control** | Public `capacity()`, `setSize()`, `capacityIncrement` | `ensureCapacity()`, private growth |
| **Iteration behavior** | **Fail-fast** `Iterator`; also legacy `Enumeration` | **Fail-fast** `Iterator` only |
| **Modern verdict** | Legacy; for backward compatibility | The standard choice |

> **Note on fail-fast iterators:** Both `Vector` and `ArrayList` throw `ConcurrentModificationException` if the list is structurally modified (by anyone other than the iterator itself) while it is being iterated. This is a *best-effort* safety mechanism, not a guarantee of correctness under concurrency.

---

## 5. Stack<E>

### 5.1 Core Idea (LIFO)

A **stack** is an abstract data type (ADT) whose defining rule is **LIFO — Last In, First Out**. The only element you can inspect or remove is the one most recently added. Stacks appear constantly in computing because the behavior of nested structures — function calls, nested parentheses, backtracking search — is inherently LIFO.

> **Analogy:** A stack of plates in a cafeteria. You *push* clean plates onto the top and *pop* plates from the top. The plate that was placed down last is the first one a customer picks up. Another everyday analogue: your browser's **Back button**, or the **Undo** command in an editor — the most recent action is undone first.

The canonical operations:

- **`push(e)`** — place an element on top.
- **`pop()`** — remove and return the top element.
- **`peek()`** — return the top element *without* removing it.
- **`empty()` / `isEmpty()`** — test whether the stack is empty.

```
Stack of books — pushing and popping:

     push("C")    push("D")       pop()       peek()
        │            │              │            │
   ┌─────────┐  ┌─────────┐   ┌─────────┐   ┌─────────┐
   │    A    │  │    A    │   │    A    │   │    A    │
   │    B    │  │    B    │   │    B    │   │    B    │
   │    C    │  │    C    │   │    C    │   │    C    │
   │         │  │    D    │   │         │   │         │
   └─────────┘  └─────────┘   └─────────┘   └─────────┘
   top = C      top = D      returns D,    returns C,
                             top = C       (D is gone)
```

### 5.2 Legacy `java.util.Stack` vs. `ArrayDeque` (the modern choice)

Java has a `java.util.Stack<E>` class, but it is **legacy and widely considered badly designed**. Its flaws:

1. **It extends `Vector`.** A stack is *not* a list — you should never be able to insert at index 2 or call `get(0)` on a stack — yet `Stack` inherits all of `Vector`'s positional methods. This breaks the LIFO contract through the API surface: you can `add(0, x)` and violate stack discipline.
2. **It inherits synchronization.** All `Vector` methods are synchronized, so `Stack` pays the same unnecessary lock overhead even in single-threaded code.
3. **Its own methods are unhelpfully named** (`empty()` instead of `isEmpty()`, `search()` for a nonstandard operation).

**The modern alternative is `ArrayDeque<E>`**, used as a deque but *viewed* as a stack via `push`, `pop`, and `peek`:

- It implements `Deque<E>`, so the *type* prevents positional list access — the compiler enforces the LIFO discipline.
- It is **not synchronized** — fast in single-threaded code.
- It has **no `null` elements**, a sensible contract for stack-style usage.

| Concern | `java.util.Stack<E>` | `ArrayDeque<E>` (used as stack) |
|---|---|---|
| Underlying storage | Extends `Vector` (array) | Resizable circular array |
| API surface | Stack *and* all List/Vector methods | Only Deque/Stack methods (`push`, `pop`, `peek`, …) |
| Enforces LIFO discipline | No — you can `add(0, x)` | Yes — type system forbids indexed access |
| Synchronized | Yes (inherited from Vector) | No |
| Performance | Slower (lock overhead) | Fast |
| Modern recommendation | Avoid | **Use this** |

> **Warning:** In exam and interview contexts, always be able to say: *"`java.util.Stack` is legacy; prefer `ArrayDeque` used as a stack, because `Stack` extends `Vector`, leaks list operations, and carries obsolete synchronization."* This is a favorite question for a reason.

### 5.3 Common Operations

```java
import java.util.ArrayDeque;
import java.util.Deque;

public class StackModern {
    public static void main(String[] args) {
        // Modern idiom: Deque<E> reference, ArrayDeque backing, used as a stack
        Deque<String> undoStack = new ArrayDeque<>();

        undoStack.push("typing: 'Hello'");
        undoStack.push("typing: ' world'");
        undoStack.push("paste: URL");

        System.out.println(undoStack.peek()); // paste: URL  (top, not removed)

        String undone = undoStack.pop();      // removes and returns top
        System.out.println(undone);           // paste: URL

        System.out.println(undoStack.isEmpty()); // false
        undoStack.pop();
        undoStack.pop();
        System.out.println(undoStack.isEmpty()); // true
        // undoStack.pop();  // would throw NoSuchElementException — guard with isEmpty()
    }
}
```

For completeness, the legacy class works the same way, but note the inherited list methods:

```java
import java.util.Stack;

public class StackLegacy {
    public static void main(String[] args) {
        Stack<String> s = new Stack<>();
        s.push("a");
        s.push("b");
        System.out.println(s.peek());        // b
        System.out.println(s.pop());         // b
        System.out.println(s.empty());       // false

        // The design flaw: you can violate LIFO through inherited list methods!
        s.add(0, "c");                       // silently inserts at the BOTTOM
        System.out.println(s);               // [c, a]
    }
}
```

### 5.4 Real-World Applications of Stacks

- **Expression evaluation** — converting infix to postfix (e.g., `3 + 4 * 2` → `3 4 2 * +`) and evaluating postfix both use stacks of operators and operands (the shunting-yard algorithm).
- **Bracket matching** — a compiler or linter checks `( [ { } ] )` balance by pushing opening brackets and popping on a matching close. If the stack is empty when a closer arrives, or non-empty at the end, the expression is unbalanced.
- **Undo/redo** — every editor keeps an undo stack of actions; "undo" pops the most recent action. Redo is often a second stack that `pop`-fed actions push onto.
- **Backtracking (mazes, recursive search)** — to explore a maze, push each fork; when you hit a dead end, pop back to the last untried fork. This is exactly the *call-stack* behavior of recursion.
- **The call stack itself** — every running program uses a hardware/OS stack of activation frames to track nested function calls. Understanding the stack ADT is understanding how your own code executes.

```java
// A classic: balanced-bracket checking with a stack
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

public class BracketMatcher {
    private static final Map<Character, Character> PAIRS = Map.of(
        ')', '(', ']', '[', '}', '{'
    );

    public static boolean isBalanced(String input) {
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : input.toCharArray()) {
            if (c == '(' || c == '[' || c == '{') {
                stack.push(c);
            } else if (PAIRS.containsKey(c)) {
                if (stack.isEmpty() || stack.pop() != PAIRS.get(c)) {
                    return false;
                }
            }
        }
        return stack.isEmpty();
    }

    public static void main(String[] args) {
        System.out.println(isBalanced("(a + b) * [c - {d}]")); // true
        System.out.println(isBalanced("((a + b)"));            // false
        System.out.println(isBalanced("([)]"));                // false
    }
}
```

> **Where You'll Use This:** Reaching for a stack means you have recognized a **LIFO relationship in your problem**: nested structures, most-recent-first processing, or backtracking. Implement it with `ArrayDeque`, not `java.util.Stack`.

---

## 6. Side-by-Side Comparison

| Operation | `ArrayList<E>` | `LinkedList<E>` | `Vector<E>` | `Stack<E>` (legacy) |
|---|---|---|---|---|
| **`get(i)` / `set(i, e)`** | O(1) | O(n) | O(1) | O(1) (via Vector) |
| **`add(e)` append** | O(1) amortized | O(1) | O(1) amortized | O(1) amortized (as `push`) |
| **`add(i, e)` insert** | O(n) | O(n)* | O(n) | O(n) (inherited) |
| **`remove(i)`** | O(n) | O(n)* | O(n) | O(n) (inherited) |
| **`remove(e)` by value** | O(n) | O(n) | O(n) | O(n) |
| **`contains(e)`** | O(n) | O(n) | O(n) | O(n) |
| **Iteration** | O(n), cache-friendly | O(n), cache-poor | O(n) | O(n) |
| **`addFirst`/`addLast`** | O(n) (no direct API) | O(1) | O(n) (via `add(0,…)`) | O(n) (via `add(0,…)`) |
| **Thread-safe?** | No | No | Yes (per-method) | Yes (inherited) |
| **Random access** | Excellent | Poor | Excellent | Poor (as a stack it's irrelevant) |

\* For `LinkedList`, finding the position is O(n); once found, the structural change is O(1). Because `ArrayList` is a *better* general-purpose list, and because `LinkedList`'s theoretical O(1) middle-insertion is negated by the O(n) search, `LinkedList` is only competitive at the ends.

**Complexities cheat-sheet for the same operation, memorably:**

```
Operation     ArrayList    LinkedList
──────────    ─────────    ──────────
get(i)        O(1)  ★      O(n)
add at end    O(1)*         O(1)
add at front  O(n)          O(1)  ★
add in middle O(n)          O(n) (search dominates)
remove at end O(1)          O(1)
contains      O(n)          O(n)

(* = amortized for ArrayList)
```

---

## 7. Guidelines for Choosing an Implementation

Use this decision guide as a mental checklist. When in doubt, the default answer is `ArrayList`.

| Your dominant pattern of access | Recommended implementation | Reasoning |
|---|---|---|
| Random indexed reads/writes (`get(i)` in a loop) | **`ArrayList`** | O(1) random access; O(n) for `LinkedList` is disqualifying |
| Append-only or append-mostly | **`ArrayList`** | O(1) amortized append, no per-node overhead |
| Insert/remove only at **both ends** (queue/deque) | **`ArrayDeque`** (or `LinkedList`) | O(1) at the ends; `ArrayDeque` is faster and leaner |
| Insert/remove in the **middle**, with an *existing* iterator at the spot | **`LinkedList`** | The O(1) node-link only pays off if you never pay the O(n) search |
| Sequential traversal only; no indexed access | Either; **`ArrayList`** usually faster due to cache | Both are O(n) to iterate, but `ArrayList` wins on locality |
| Multi-threaded access from many threads | **Not** `Vector` — use `CopyOnWriteArrayList`, `ConcurrentLinkedDeque`, or `Collections.synchronizedList(...)` | Method-level locks are both slow and insufficient for compound ops |
| LIFO stack behavior | **`ArrayDeque`** | Enforces the discipline; `java.util.Stack` is legacy |
| Maintain a legacy codebase | `Vector` / `Stack` may be *read*, but do not extend their use | Keep old code working, but never add new uses |
| Very large lists with frequent middle edits *and* you hold node/iterator positions | **`LinkedList`** | Its one genuine niche: structural edits via iterators |

**The one-paragraph decision rule:**

> Start with `ArrayList`. Switch to `LinkedList` *only* when you can prove that (a) you almost never call `get(i)` or `indexOf`, and (b) your insertions and removals cluster at the ends or happen through live iterators. If you need stack or queue semantics, use `ArrayDeque`. Never write new code against `Vector` or `Stack`.

---

## 8. Common Pitfalls and Best Practices

### Pitfalls

1. **`LinkedList` with random access.**
   ```java
   // BAD — O(n²) total: get(i) walks the chain from an end each time
   for (int i = 0; i < list.size(); i++) {
       System.out.println(list.get(i));
   }
   ```
   **Fix:** use an enhanced `for` loop or an iterator, which walk the chain *once*.

2. **Modifying a list while iterating it.**
   ```java
   // BAD — throws ConcurrentModificationException
   for (String s : list) {
       if (s.length() == 0) list.remove(s);
   }
   ```
   **Fix:** use `iterator.remove()` or, for bulk edits, `removeIf`:
   ```java
   list.removeIf(s -> s.isEmpty());            // clean, single pass
   ```

3. **Using `java.util.Stack` and `Vector` in new code.** Their synchronization is slow, their APIs leak positional methods, and the framework has strictly better alternatives. (See Sections 4 and 5.)

4. **Confusing capacity with size.** `size()` is the number of elements *you* stored; `capacity()` (Vector) is the length of the backing array. `new ArrayList<>(1000)` does **not** make the list contain 1000 elements; it only pre-allocates room, and `size()` returns 0.

5. **Calling `pop()` on an empty stack/deque.** `ArrayDeque.pop()` throws `NoSuchElementException` when empty. Guard with `isEmpty()` first.

6. **Assuming `contains` is fast.** On every list in this chapter, `contains` is O(n). If you need membership tests, that's a `HashSet`'s job.

7. **Forgetting that `ArrayList` insert-at-front is O(n).** `add(0, x)` in a loop is O(n²). If you build a list by repeatedly prepending, collect in reverse or use `addLast`/`addFirst` on a deque.

### Best Practices

- **Declare with the interface type**: `List<E> list = new ArrayList<>();`
- **Pre-size when you know the bound**: `new ArrayList<>(expectedCount)` avoids resize churn for large inputs.
- **Use `removeIf`, `replaceAll`, and streams** instead of hand-rolled mutating loops.
- **Prefer `ArrayDeque` for stack/queue needs**, and `LinkedList` only when list semantics are also required.
- **Document the chosen structure's complexity** in method signatures or class comments when the choice is load-bearing.
- **Measure before you optimize.** Big-O tells you the *shape* of the cost; cache effects and constants mean `ArrayList` often wins even where the theory looks even.

> **Exam tip:** The single most-tested distinction in this chapter is the *asymmetric* pairing: `ArrayList` gets O(1) indexed access but pays O(n) for middle insertions; `LinkedList` gets O(1) end insertions but pays O(n) for indexed access. Be prepared to *draw* the backing-array and node-chain diagrams, and to write out the geometric-series argument for amortized `add`.

---

## 9. Chapter Summary

- The **`List<E>` interface** extends `Collection<E>` and `Iterable<E>`, adding positional, index-based access with duplicates allowed. Program to the interface, not the concrete class.
- **`ArrayList`** wraps a **resizable backing array** (default capacity 10, grows by **1.5×**). It gives **O(1)** `get`/`set`, **O(1) amortized** append, and **O(n)** middle insertions/removals and linear search. It is cache-friendly and memory-lean — the default choice.
- **`LinkedList`** wraps each element in a **doubly-linked node** with `prev`/`next` references and holds `first`/`last` pointers. It gives **O(1)** operations at both ends, but **O(n)** indexed access and search, with poor cache locality and per-node memory overhead. Use it at the ends, not for random access.
- **`Vector`** is `ArrayList`'s legacy, synchronized predecessor (JDK 1.0). Its per-method locks cost performance, provide inadequate compound-operation safety, and make it obsolete for new code; its growth policy (doubling or `capacityIncrement`) is its only structural quirk.
- **`Stack`** is LIFO ADT; `java.util.Stack` extends `Vector`, leaking list operations and synchronization. The modern replacement is **`ArrayDeque`** used with `push`/`pop`/`peek`, which enforces the discipline at the type level.
- **Amortized analysis** justifies the O(1) claim for `ArrayList.add`: with doubling growth, `n` insertions cost at most `3n` total work (geometric series ≤ 2n for copies), averaging O(1) per insertion.
- **Choosing** an implementation means tracing structure → complexity → use case. When in doubt: `ArrayList`. For stacks and queues: `ArrayDeque`. Only reach for `LinkedList` when end-dominated edits or iterator-based structural edits dominate.

---

## 10. Practice Exercises

### Conceptual

1. **Interface hierarchy.** Draw the interface hierarchy that connects `List<String>`, `Collection<String>`, and `Iterable<String>`. Which of the three *guarantees* positional access? Which *permits* duplicate elements? Which enables the enhanced `for` loop?
2. **Explain in plain words** why `ArrayList.get(i)` is O(1) while `LinkedList.get(i)` is O(n), tying your answer to their internal structures.

### Code Reading

3. Given the following snippet, trace its output step by step:
   ```java
   List<String> list = new ArrayList<>();
   list.add("a"); list.add("b"); list.add("c");
   list.add(1, "X");
   list.set(3, "Z");
   list.remove(0);
   System.out.println(list);
   ```
4. What is the *amortized* cost of `add(e)` on an `ArrayList`, and what is its worst-case single-call cost? Explain why both statements are simultaneously true, using the doubling-growth argument (write the geometric series).

### Code Writing

5. Write a method `List<Integer> prependValues(int n)` that returns `[n-1, n-2, …, 0]` in **O(n)** time. (Hint: do not call `add(0, x)` in a loop.)
6. Using an `ArrayDeque`, write a `matches(String brackets)` method that returns `true` iff the brackets `()[]{}` are properly balanced and nested (no interleaving like `([)]`).
7. Implement `void reverse(List<String> list)` in place, choosing the implementation-agnostic approach (works for both `ArrayList` and `LinkedList`).

### Complexity Analysis

8. Compare the cost of inserting *n* elements one at a time **at the front** for an `ArrayList` versus a `LinkedList`. Express each as a function of `n`. Which structure wins, and why does the naive comparison of "O(n) vs O(1)" per insertion tell only half the story for `LinkedList`?
9. A program repeatedly does: `list.get(i)` for a random index `i`, `n` times in total. Give the total time for `ArrayList` and for `LinkedList`. Same question if the program instead iterates the whole list from head to tail, `n` times.
10. **Challenge:** Prove that if `ArrayList`'s growth factor were `1 + ε` (any *fixed* positive ε), `add(e)` would still be O(1) amortized. What changes if the growth factor is exactly `1` (i.e., "grow by one slot")?

### Answer Key / Guidance

1. `Iterable` ← `Collection` ← `List`. `List` guarantees positional access; `List` permits duplicates (its distinguishing feature); `Iterable` enables the enhanced `for`.
2. `ArrayList` computes the element address arithmetically from the base address; `LinkedList` must follow `next` pointers, taking up to n hops.
3. Start `[a, b, c]` → add `X` at 1 → `[a, X, b, c]` → set index 3 → `[a, X, b, Z]` → remove index 0 → `[X, b, Z]`.
4. Amortized O(1); worst case single call O(n). Copies 1+2+4+…+n ≤ 2n total over n insertions ⇒ ≤ 3n total work ⇒ O(1) average per operation, while any single resize is O(n).
5. Use `List<Integer> out = new ArrayList<>(n); for (int i = n-1; i >= 0; i--) out.add(i);` — appends are amortized O(1).
6. Mirror the `BracketMatcher` example: push openers, on a closer pop and compare against the expected opener; interleaving like `([)]` fails naturally because the popped value won't match.
7. Swap `list.get(i)` with `list.get(list.size()-1-i)` for `i < size/2` using `set`. Works on any `List`.
8. `ArrayList`: each `add(0, x)` shifts n elements ⇒ total O(n²). `LinkedList`: each `addFirst` is O(1) ⇒ total O(n). But careful: if you used `add(0, x)` on a `LinkedList`, that's `add(index, e)` which *searches* O(n) too — the O(1) win requires the dedicated `addFirst` (or `Deque`/`LinkedList`-typed variable).
9. Random `get`: `ArrayList` O(n) total; `LinkedList` O(n²). Whole-list iteration: both O(n) per pass ⇒ O(n²) total for n passes, but `ArrayList` will be faster in practice due to cache locality.
10. Geometric series `(1+ε)^0 + (1+ε)^1 + … + (1+ε)^k` sums to `((1+ε)^(k+1)-1)/ε` = O(final capacity) = O(n), so the amortized cost stays O(1). If capacity grows by exactly 1 each time, insertions cost 1+2+3+…+n = O(n²) total ⇒ amortized O(n) per `add`.

---

## 11. References / Further Reading

1. **Oracle Java Documentation**
   - `java.util.List` — [https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/List.html](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/List.html)
   - `java.util.ArrayList` — [https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ArrayList.html](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ArrayList.html)
   - `java.util.LinkedList` — [https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/LinkedList.html](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/LinkedList.html)
   - `java.util.Vector` and `java.util.Stack` — legacy pages under `java.util`
   - `java.util.ArrayDeque` and `java.util.Deque` — [https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Deque.html](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Deque.html)
2. **OpenJDK source code** — the implementation details cited here (growth factor `oldCapacity + (oldCapacity >> 1)`, `DEFAULT_CAPACITY = 10`, `LinkedList`'s node class `Node<E>` with `item`/`next`/`prev`, `Vector`'s `capacityIncrement` and synchronized methods) are all in `src/java.base/share/classes/java/util/` of the OpenJDK repository.
3. **Cormen, Leiserson, Rivest, and Stein**, *Introduction to Algorithms* (MIT Press), Chapter on "Amortized Analysis" — the canonical treatment of aggregate, accounting, and potential methods.
4. **Joshua Bloch**, *Effective Java*, 3rd Edition, Item 61 ("Prefer primitive types…") and the general guidance on collections, plus Item 1 discussion of the Collections Framework's design. Bloch also authored the modern Collections Framework that replaced `Vector`/`Stack` idioms.
5. **Robert Sedgewick and Kevin Wayne**, *Algorithms, 4th Edition* — sections on bags, queues, and stacks, including linked-list implementations and cost models.
6. **Java Collections Framework Overview** — the official "Collections Trail" of *The Java Tutorials*: [https://docs.oracle.com/javase/tutorial/collections/](https://docs.oracle.com/javase/tutorial/collections/)

---

*End of chapter: "List Implementations in Java."*

---

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

---

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

---

# Java Study Notes: Iterators

## 1. Learning Objectives

By the end of this section, you will be able to:

- Explain the purpose of the **Iterator pattern** and how it decouples traversal logic from the internal structure of a collection.
- Use the `java.util.Iterator<E>` interface correctly — calling `hasNext()`, `next()`, and the optional `remove()` in the proper sequence — and describe what each method does and when it throws an exception.
- Traverse a `List` in both directions and modify it in place using `java.util.ListIterator<E>` (including `set()` and `add()`).
- Describe **fail-fast** behavior: what triggers a `ConcurrentModificationException`, what "best-effort" means, and why removing through the iterator's own `remove()` is safe while modifying the collection directly during iteration is not.
- Decide, for a given task, whether an explicit `Iterator`, a `ListIterator`, or a for-each loop is the most appropriate tool, and justify your choice.

---

## 2. Introduction and Motivation

When you write software, you constantly need to walk through collections of objects: print every element of a list, find a specific customer in a set, sum the numbers in a queue, or ship every order in a shopping cart. A naive approach would hard-code traversal for each kind of collection: "to go through a `LinkedList`, follow the next pointers; to go through an `ArrayList`, step through the array indices." But then every piece of client code becomes tangled with the internal representation of the collection, and changing the underlying data structure would force changes throughout your entire program.

This is where **iterators** come in. An iterator is a small object whose job is *just* to visit the elements of a collection one at a time. It exposes a uniform, simple contract — "is there another element?" and "give me the next one" — no matter whether the collection behind it is a resizable array, a linked list, a hash table, a tree, or a stream of database rows. This uniformity lets you write one piece of traversal code that works against any collection, and it lets the collection change its internal layout without breaking its consumers.

Iterators are everywhere in real software. The **Java Collections Framework** (`ArrayList`, `HashSet`, `LinkedList`, and friends) is built around them. The **for-each loop** is just syntactic sugar that compiles down to an iterator. Data processing pipelines that read records one at a time — log analyzers, CSV parsers, ORM result sets — use iterator-style interfaces because they allow **lazy** processing: you don't need the entire dataset in memory to process it. UI frameworks use iterators to bind data sources to list views. Even modern **Streams** in Java 8+ are powered underneath by a richer relative of the iterator called a `Spliterator`.

**A simple analogy.** Imagine a music playlist on your phone. You don't need to know whether the songs are stored in a database, in a file, or in a pile of memory chips. You only need a "remote control" with two buttons: *Is there a next song?* and *Play the next song*. That remote control is the iterator. The `ListIterator` is an upgraded remote that also has *previous* and *next index* buttons, plus an *edit this song* button. The collection is the music library; the iterator is the tiny controller that knows how to walk through it, and because the controller is separate from the library, you could swap the library out for a completely different one and still use the same remote.

---

## 3. Core Concepts

### 3.1 The Iterator Pattern

The **Iterator pattern** is one of the twenty-three classic design patterns catalogued in the *Gang of Four* (GoF) book *Design Patterns*. Its stated intent is:

> Provide a way to access the elements of an aggregate object sequentially without exposing its underlying representation.

The pattern involves three roles:

1. **`Iterator`** — an interface declaring traversal operations, most fundamentally "do more elements exist?" and "return the next element."
2. **`Iterable`** — the collection (the "aggregate"). It is responsible for handing out an `Iterator` via a factory method (in Java, `iterator()`).
3. **A concrete `Iterator` implementation** — a class that remembers *where* it currently is inside the collection and knows how to advance.

The crucial architectural win is **decoupling**: the client code talks only to the `Iterator` interface, never to the collection's internals. Consider the two loops below — they are *identical* even though the underlying data structures are radically different:

```java
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PatternDecoupling {
    // This method works for ANY List (array-backed or node-based)
    public static void printAll(List<String> values) {
        for (String value : values) {   // uses an iterator behind the scenes
            System.out.println(value);
        }
    }

    public static void main(String[] args) {
        List<String> arrayList = new ArrayList<>();
        arrayList.add("array-backed");

        List<String> linkedList = new LinkedList<>();
        linkedList.add("node-based");

        printAll(arrayList);    // same code
        printAll(linkedList);   // same code
    }
}
```

Had we written traversal "by hand," the `ArrayList` version would index into a contiguous array while the `LinkedList` version would chase node references — two completely different algorithms behind two different pieces of code. The iterator pattern collapses that to one. Because of this, you can write generic algorithms (searching, filtering, counting) that operate on any `Collection`, and you can even iterate over collections while they grow or shrink, subject to the fail-fast rules discussed in section 3.4.

### 3.2 The `Iterator<E>` Interface

`java.util.Iterator<E>` is a generic interface declared in the `java.util` package. Its full shape (as of Java 8+) is:

```java
public interface Iterator<E> {
    boolean hasNext();                    // does another element remain?
    E next();                             // return the next element and advance
    default void remove() { throw new UnsupportedOperationException(); }
    default void forEachRemaining(Consumer<? super E> action) { ... }
}
```

The generic type parameter `E` is the element type, so `Iterator<String>` yields `String` values from `next()`, and no cast is needed.

**`boolean hasNext()`.** Returns `true` if the iterator has at least one more element to return. This is a **peek**: it does *not* advance the iterator. Because it does not consume anything, it is safe to call as many times as you like. It is what allows you to write safe loops: you only ever call `next()` when you know an element is waiting.

**`E next()`.** Returns the next element and moves the iterator forward one position. This is the method that actually consumes an element. Two critical behaviors:

- If there are **no more elements**, `next()` throws `java.util.NoSuchElementException`. A well-behaved loop guards every call with `hasNext()`.
- `next()` is **irrevocable** with a plain `Iterator`: once you have consumed an element and moved past it, you cannot go back. (The exception is `ListIterator`, section 3.3, which can move backward.)

Think of the iterator's cursor as sitting *between* elements. `next()` moves the cursor forward and returns the element it just crossed. `hasNext()` checks whether there is an element ahead of the cursor. This "cursor-between-elements" model is the key to understanding why `next()` after the last element fails: the cursor has run off the end.

**`void remove()`.** Removes from the collection the last element returned by `next()`. This is the only safe way to remove an element *while iterating* (see section 3.4). Its contract is strict:

- It must be called **immediately after** `next()` and at most **once** per `next()` call. Calling it before any `next()`, or twice in a row, throws `IllegalStateException`.
- Because it is an optional operation, implementations that do not support removal (e.g., iterators over immutable collections or over some read-only views) throw `UnsupportedOperationException`.

**`default void forEachRemaining(Consumer<? super E> action)`.** Added in **Java 8**. It performs the given action on every element that has not yet been consumed, in a single call. It is convenient for one-shot processing when you don't need to interleave other work between elements:

```java
Iterator<String> it = names.iterator();
it.forEachRemaining(name -> System.out.println(name.toUpperCase()));
```

It also allows a native implementation to process the remaining elements in a highly optimized internal loop, since it doesn't have to re-check the cursor position after every element.

**The canonical traversal loop.** Because `hasNext()` and `next()` are safe to use together, the standard idiom is:

```java
Iterator<String> it = collection.iterator();
while (it.hasNext()) {
    String element = it.next();
    // process element
}
```

**Analogy.** A bookstore clerk restocking shelves: `hasNext()` is the clerk asking "are there more books in the box?", and `next()` is pulling one book out of the box. Calling `next()` when the box is empty would have you grabbing thin air (`NoSuchElementException`). The box is the collection; the clerk's hand holding a book is the cursor position.

### 3.3 `ListIterator`

`java.util.ListIterator<E>` extends `Iterator<E>` and adds bidirectional traversal, index access, and in-place modification. It is available **only on `List` implementations** (e.g., `ArrayList`, `LinkedList`, `Vector`) because only a `List` has a well-defined linear order and integer positions. The interface is:

```java
public interface ListIterator<E> extends Iterator<E> {
    boolean hasNext();           // inherited
    E next();                    // inherited
    boolean hasPrevious();       // is there an element before the cursor?
    E previous();                // move cursor backward, returning the element crossed
    int nextIndex();             // index of the element that next() would return
    int previousIndex();         // index of the element that previous() would return
    void remove();               // remove the last element returned by next() or previous()
    void set(E e);               // replace the last element returned by next() or previous()
    void add(E e);               // insert e at the cursor position
}
```

**Bidirectional traversal.** `hasPrevious()` and `previous()` mirror `hasNext()` and `next()` but move the cursor backward. Calling `previous()` when the cursor is before the first element throws `NoSuchElementException`. To walk a list backward from the end, first advance to the end, then call `previous()` in a loop guarded by `hasPrevious()`.

**Index access.** `nextIndex()` returns the index of the element that the *next* call to `next()` would return; `previousIndex()` returns the index of the element that *previous()* would return. In the cursor-between-elements model, if the cursor is between element at index `i` and element at index `i+1`, then `previousIndex()` is `i` and `nextIndex()` is `i+1`. A nice identity: **`nextIndex()` always equals `previousIndex() + 1`**, except at the extremes. When the cursor is before the first element, `previousIndex()` returns `-1`; when it is after the last element, `nextIndex()` returns `list.size()`. These methods are `O(1)` and are useful for tracking position during a traversal.

**Modification: `set()` and `add()`.**

- `set(E e)` **replaces** the last element returned by `next()` or `previous()` with `e`. Like `remove()`, it may be called only after a `next()`/`previous()` call and at most once per call — otherwise `IllegalStateException`. It does not change the structure of the list, so it is always fail-safe in terms of the cursor.
- `add(E e)` **inserts** `e` immediately before the element that would be returned by `next()` (i.e., at the cursor). Unlike `set()`, it does *not* require a preceding `next()` — you may call it at any time. After `add()`, a subsequent `next()` returns the element that was at the cursor before the insertion, and `previous()` returns the newly added element. This method is a structural modification of the list, so it increments the list's structural modification counter, as discussed in section 3.4.

The interplay of `add()` and the cursor is subtle but worth knowing: if the cursor sits between `a` and `b`, calling `add(x)` places `x` between them; `next()` then returns `b` (not `x`), while `previous()` returns `x`.

**Dedicated example: backward traversal.** The program below walks a `List` forward, prints it backward, and uses `set()` to replace elements mid-iteration:

```java
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class ListIteratorDemo {

    public static void main(String[] args) {
        List<String> fruits = new ArrayList<>();
        fruits.add("apple");
        fruits.add("banana");
        fruits.add("cherry");

        ListIterator<String> it = fruits.listIterator();

        // --- Forward traversal, reporting indices ---
        System.out.println("Forward traversal:");
        while (it.hasNext()) {
            int index = it.nextIndex();        // index about to be returned
            String fruit = it.next();
            System.out.println("  index " + index + " -> " + fruit);
        }
        // The cursor is now AFTER "cherry".

        // --- Backward traversal ---
        System.out.println("Backward traversal:");
        while (it.hasPrevious()) {
            int index = it.previousIndex();    // index about to be returned
            String fruit = it.previous();
            System.out.println("  index " + index + " -> " + fruit);
        }
        // The cursor is now BEFORE "apple".

        // --- set(): replace "banana" with "dragonfruit" ---
        while (it.hasNext()) {
            String fruit = it.next();
            if (fruit.equals("banana")) {
                it.set("dragonfruit");         // replace current element
            }
        }

        System.out.println("After set(): " + fruits);

        // --- add(): insert at the cursor position ---
        // Move to the front, then insert "kiwi" before "apple".
        while (it.hasPrevious()) {
            it.previous();
        }
        it.add("kiwi");                        // cursor is at the front -> inserts first
        System.out.println("After add(): " + fruits);
    }
}
```

**Expected output:**

```
Forward traversal:
  index 0 -> apple
  index 1 -> banana
  index 2 -> cherry
Backward traversal:
  index 2 -> cherry
  index 1 -> banana
  index 0 -> apple
After set(): [apple, dragonfruit, cherry]
After add(): [kiwi, apple, dragonfruit, cherry]
```

**Line-by-line explanation.**

- `fruits.listIterator()` returns a `ListIterator` whose cursor starts *before* the first element (position 0).
- Inside the forward loop, `it.nextIndex()` is queried *before* `next()`, so it reports exactly the index of the element about to be returned. After the loop, the cursor sits after `cherry`, so `nextIndex()` would return `3 == fruits.size()`.
- The backward loop reuses the same iterator — no new one is created. `it.previous()` crosses back over `cherry`, `banana`, and `apple`. Note that `previous()` and `next()` are symmetric: calling `next()` then `previous()` returns you to the same element and the same cursor position, with no element skipped.
- The `set()` loop advances again to the front; when it encounters `"banana"`, `set("dragonfruit")` rewrites the element in place. The list's *size* is unchanged, and iteration continues normally with no exception.
- Finally, the cursor is repositioned to the front and `add("kiwi")` inserts before `"apple"`, growing the list.

### 3.4 Fail-Fast Behavior

**Fail-fast** is a defensive design strategy used by the iterators in the `java.util` collections (including `ArrayList`, `LinkedList`, `HashSet`, `HashMap` views, and so on). The iterators of these classes detect — at the earliest safe moment — that the collection has been **structurally modified** since the iterator was created, and they respond by throwing `java.util.ConcurrentModificationException`.

**What is a structural modification?** A change that alters the *size* of the collection or shifts its internal layout in a way that would invalidate a traversal in progress: `add()`, `remove()`, `clear()`, and similar operations. Plainly overwriting the value of an existing element with `set()` is *not* structural (that is exactly why `ListIterator.set()` is legal during iteration). The phrase "since the iterator was created" is important: you may freely modify the collection *before* you create the iterator, or *after* you finish iterating. The protection only applies while an iterator is alive and mid-traversal.

**How is it implemented?** Most `java.util` collections keep a private counter, conventionally named `modCount`, which is incremented on every structural modification. When an iterator is created, it captures the current `modCount`. On every call to `next()` (and `hasNext()` in some implementations), the iterator compares the collection's current `modCount` with the value it captured. If they differ, the iterator concludes that someone changed the collection underneath it and throws `ConcurrentModificationException`.

For example, in an `ArrayList` the implementation's internal `next()` looks, in essence, like:

```java
public E next() {
    checkForComodification();          // if modCount changed -> throw
    ...
}

final void checkForComodification() {
    if (modCount != expectedModCount)
        throw new ConcurrentModificationException();
}
```

**Why does it exist?** It protects you from **undefined behavior**. An iterator's position is a function of the collection's internal structure (array indices, node references, hash-bucket positions). If the structure shifts while you are mid-traversal, the iterator's notion of "where I am" can become meaningless: elements may be skipped, visited twice, or the iterator may read past the end of an internal array. Rather than silently corrupting your results, the collections choose to fail loudly and immediately. This converts a subtle, hard-to-debug logical bug into an exception that fires at the very moment of the invalid modification.

**What does "best-effort" mean?** The Javadoc is deliberately careful. It states that the fail-fast behavior is a *best-effort* guarantee, not a hard one. The reason is that in the presence of unsynchronized concurrent modification, there is no way to *guarantee* that the check happens reliably — two threads could modify the collection in ways that happen to leave `modCount` equal, or the check could race with the modification. The Javadoc for the collections classes says, in essence: "It would be wrong to write a program that depends on this exception for its correctness: *fail-fast behavior should be used only to detect bugs.*" In other words, treat `ConcurrentModificationException` as a bug detector, never as a control-flow mechanism (do not catch it and "fix things up" in the handler).

**When is fail-fast NOT triggered?** Several important cases:

1. **`remove()` via the iterator's own `remove()`.** The iterator increments the collection's `modCount` *and* updates its own cached `expectedModCount` in lockstep, so the internal counter stays consistent. This is why `it.remove()` is safe during iteration while `collection.remove(element)` is not — even though both are structurally modifying the same underlying collection.
2. **Modifications on a *different* iterator over the same collection.** If you create two iterators over one list, and one of them modifies the list, the *other* iterator will throw `ConcurrentModificationException` on its next `next()` — because its cached `modCount` no longer matches. This is one reason sharing a collection across two simultaneous iterations is dangerous.
3. **`add()`/`set()` through a `ListIterator`.** `ListIterator.add()` is a structural modification but is handled internally (it updates both counters), so the *same* iterator can continue safely. `ListIterator.set()` is not structural at all. (Note that even here, a *second* concurrent `ListIterator` on the same list would fail fast.)
4. **Collections that do not track modifications.** Some collections simply do not implement fail-fast. For example, `java.util.concurrent` classes such as `CopyOnWriteArrayList` deliberately use **fail-safe** iterators: they iterate over a snapshot of the data, so modifications during iteration never throw and are simply not seen by the in-flight traversal. An iterator over a `java.util.Collections.emptyList()` has nothing to modify and will never throw. So fail-fast is a property of the *specific* collection class, not a universal law of iterators.
5. **Single-threaded modifications that happen to be invisible to the check.** Because of the "best-effort" nature, there are contrived scenarios where a modification does not increment `modCount` (e.g., directly mutating array internals via reflection), in which case the iterator cannot detect it.

**A short demonstration of the mechanism.** Consider the classic bug:

```java
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

public class FailFastIllustration {
    public static void main(String[] args) {
        List<String> names = new ArrayList<>();
        names.add("A");
        names.add("B");
        names.add("C");

        Iterator<String> it = names.iterator();
        try {
            while (it.hasNext()) {
                String name = it.next();
                System.out.println("Visiting " + name);
                if (name.equals("B")) {
                    names.remove(name);      // structural modification via the COLLECTION
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("ConcurrentModificationException thrown!");
        }
    }
}
```

**Expected output:**

```
Visiting A
Visiting B
ConcurrentModificationException thrown!
```

Note that the exception is not thrown at the moment `names.remove(...)` executes. It is thrown on the *next* call to `next()` (here, the call that would have returned `"C"`), when the iterator re-checks `modCount` and discovers the mismatch. The element `"C"` is never visited. This delayed-detection behavior is normal and worth internalizing: **the exception surfaces on the next iterator operation after the offending modification**, which may be several lines later.

---

## 4. Detailed Code Examples

### Example 1 — Iterating an `ArrayList` and a `HashSet` with `Iterator`

**Purpose.** This example demonstrates the uniformity of the `Iterator` API across two completely different collection implementations, and highlights the crucial fact that **sets are unordered**: iteration over a `HashSet` makes no promise about element order.

```java
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class IteratorBasics {

    public static void main(String[] args) {
        // --- A List: order is preserved ---
        List<String> list = new ArrayList<>();
        list.add("alpha");
        list.add("beta");
        list.add("gamma");

        System.out.println("Iterating over the ArrayList:");
        Iterator<String> listIt = list.iterator();
        while (listIt.hasNext()) {
            String element = listIt.next();
            System.out.println("  " + element);
        }

        // --- A Set: order is NOT guaranteed ---
        Set<String> set = new HashSet<>();
        set.add("alpha");
        set.add("beta");
        set.add("gamma");

        System.out.println("Iterating over the HashSet (order not guaranteed):");
        Iterator<String> setIt = set.iterator();
        while (setIt.hasNext()) {
            String element = setIt.next();
            System.out.println("  " + element);
        }

        // --- Prove the HashSet order is data-dependent ---
        Set<Integer> ints = new HashSet<>();
        ints.add(10);
        ints.add(20);
        ints.add(30);
        System.out.println("HashSet of integers:");
        for (Integer i : ints) {           // for-each is sugar for an iterator
            System.out.println("  " + i);
        }
    }
}
```

**Line-by-line explanation.**

- `list.iterator()` returns an `Iterator<String>` positioned before the first element. The `while (listIt.hasNext())` guard guarantees that `next()` is only called when an element exists, so `NoSuchElementException` can never occur here.
- Each `listIt.next()` returns the next element in *insertion order* because `ArrayList` is index-based and preserves insertion order.
- `set.iterator()` returns an iterator over the `HashSet`. The set stores elements in **hash buckets**, and iteration order depends on the elements' hash codes and the set's current capacity — hence the Javadoc guarantee that no iteration order is promised.
- The third loop iterates a `HashSet<Integer>` using a **for-each loop**, which compiles to exactly the same pattern: the compiler synthesizes a call to `iterator()` and a `hasNext()`/`next()` loop. This demonstrates that for-each is not a separate mechanism — it *is* an iterator in disguise.

**Expected output** (note: the exact order of the `HashSet` elements may differ on your machine/JDK — that is precisely the point):

```
Iterating over the ArrayList:
  alpha
  beta
  gamma
Iterating over the HashSet (order not guaranteed):
  alpha
  gamma
  beta
HashSet of integers:
  20
  10
  30
```

The `ArrayList` output is always `alpha, beta, gamma`. The `HashSet` output may vary between runs and between JDK versions, because hashing is not order-stable. Never rely on `HashSet` iteration order; if you need a predictable order, use a `LinkedHashSet` (insertion order) or a `TreeSet` (sorted order).

### Example 2 — `ListIterator` for forward/backward traversal and in-place `set()`

**Purpose.** This example shows the full power of `ListIterator`: bidirectional traversal of a single iterator, index introspection, and safe replacement of elements during iteration.

```java
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class ListIteratorTraversal {

    public static void main(String[] args) {
        List<String> queue = new ArrayList<>();
        queue.add("ticket-1001");
        queue.add("ticket-1002");
        queue.add("ticket-1003");

        ListIterator<String> lit = queue.listIterator();

        System.out.println("=== Forward pass ===");
        while (lit.hasNext()) {
            int pos = lit.nextIndex();
            String ticket = lit.next();
            System.out.println("next() at index " + pos + " -> " + ticket);
        }

        System.out.println("=== Backward pass (same iterator) ===");
        while (lit.hasPrevious()) {
            int pos = lit.previousIndex();
            String ticket = lit.previous();
            System.out.println("previous() at index " + pos + " -> " + ticket);
        }

        // Rewrite every ticket in place.
        while (lit.hasNext()) {
            lit.next();                     // required before set()
            lit.set("served:" + lit.previousIndex());
        }
        // Careful: previousIndex() above is evaluated AFTER set(), which is
        // safe, but the argument evaluation order deserves attention.
        // To keep things crystal clear, rewrite it explicitly instead:
    }
}
```

The last loop above is unnecessarily clever and hard to read, so the "real" version below replaces it with an explicit variable. Clean code beats clever code:

```java
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class ListIteratorTraversal {

    public static void main(String[] args) {
        List<String> queue = new ArrayList<>();
        queue.add("ticket-1001");
        queue.add("ticket-1002");
        queue.add("ticket-1003");

        ListIterator<String> lit = queue.listIterator();

        System.out.println("=== Forward pass ===");
        while (lit.hasNext()) {
            int pos = lit.nextIndex();
            String ticket = lit.next();
            System.out.println("next() at index " + pos + " -> " + ticket);
        }

        System.out.println("=== Backward pass (same iterator) ===");
        while (lit.hasPrevious()) {
            int pos = lit.previousIndex();
            String ticket = lit.previous();
            System.out.println("previous() at index " + pos + " -> " + ticket);
        }

        // Rewrite every ticket in place.
        while (lit.hasNext()) {
            int pos = lit.nextIndex();
            lit.next();                        // next() must precede set()
            lit.set("served:" + pos);
        }

        System.out.println("=== Updated list ===");
        System.out.println(queue);
    }
}
```

**Line-by-line explanation.**

- `queue.listIterator()` creates a cursor before index `0`. The forward loop prints each element with its index.
- After the forward loop, `lit` has consumed the whole list. The backward loop then uses `hasPrevious()`/`previous()` to retrace the exact same elements in reverse — all with **the same iterator object**. This is impossible with a plain `Iterator`, which only knows how to move forward.
- The final loop calls `set()` after each `next()`. Calling `next()` first is mandatory; `set()` without a preceding `next()`/`previous()` would throw `IllegalStateException`. The index captured by `nextIndex()` before `next()` is the index of the element being replaced, which we bake into the new value.
- Because `set()` is not a structural modification, this whole loop runs without any `ConcurrentModificationException`, even though we are changing the list's contents.

**Expected output:**

```
=== Forward pass ===
next() at index 0 -> ticket-1001
next() at index 1 -> ticket-1002
next() at index 2 -> ticket-1003
=== Backward pass (same iterator) ===
previous() at index 2 -> ticket-1003
previous() at index 1 -> ticket-1002
previous() at index 0 -> ticket-1001
=== Updated list ===
[served:0, served:1, served:2]
```

### Example 3 — Fail-fast: triggering `ConcurrentModificationException` and the safe alternative

**Purpose.** This example deliberately produces a `ConcurrentModificationException` by structurally modifying a collection during iteration, then demonstrates the correct pattern: using the iterator's own `remove()`.

```java
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

public class FailFastDemo {

    public static void main(String[] args) {
        List<String> names = new ArrayList<>();
        names.add("Alice");
        names.add("Bob");
        names.add("Carol");

        // --- WRONG WAY: modify the collection while iterating ---
        Iterator<String> it = names.iterator();
        try {
            while (it.hasNext()) {
                String name = it.next();
                System.out.println("Visiting: " + name);
                if (name.equals("Bob")) {
                    names.remove(name);          // BUG: structural change via the collection
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println(">> ConcurrentModificationException: iteration invalidated");
        }

        // --- RIGHT WAY: use the iterator's own remove() ---
        List<String> more = new ArrayList<>();
        more.add("Alice");
        more.add("Bob");
        more.add("Carol");

        Iterator<String> safeIt = more.iterator();
        while (safeIt.hasNext()) {
            String name = safeIt.next();
            if (name.equals("Bob")) {
                safeIt.remove();                 // safe: iterator updates its own modCount cache
            }
        }

        System.out.println("After safe remove(): " + more);
    }
}
```

**Line-by-line explanation.**

- In the first loop, when `name` is `"Bob"`, the *collection* method `names.remove(name)` executes. This increments the list's internal `modCount` but does **not** update the iterator's cached `expectedModCount`.
- The exception is not thrown immediately at the `remove()` call. It surfaces on the *next* invocation of `it.next()` (which would have returned `"Carol"`). That is why `"Carol"` is never printed.
- In the second loop, `safeIt.remove()` removes the element just returned by `next()`. Because the iterator itself performs the structural modification, it can update both the collection's counter and its own cache, keeping them consistent. The loop completes without exception.
- Notice that we did not re-create the list in the first block by clearing; the first loop's exception did not corrupt the `names` list — the list is still fully intact with all three names, because the failed iteration never actually removed anything. (The remove *did* execute though; so `names` actually contains only `[Alice, Carol]`. The exception aborted the *iteration*, not the *removal*.) If you run this, you may want to rebuild the list between blocks to keep the demo clean; the second block uses a fresh list to avoid confusion.

**Expected output:**

```
Visiting: Alice
Visiting: Bob
>> ConcurrentModificationException: iteration invalidated
After safe remove(): [Alice, Carol]
```

This example crystallizes the single most important fail-fast rule: **to remove while iterating, always go through the iterator, never through the collection.**

---

## 5. Comparison Table

| Feature | `Iterator<E>` | `ListIterator<E>` | for-each loop |
|---|---|---|---|
| **Applicable collections** | Any `Collection` (plus iterable sources such as `Stream.iterator()`); obtained via `iterator()` | Only `List` implementations (`ArrayList`, `LinkedList`, `Vector`, custom `List`s); obtained via `listIterator()` | Any `Iterable` and any Java array |
| **Direction of traversal** | Forward only | Bidirectional (`hasPrevious()`/`previous()` plus forward) | Forward only |
| **Remove during iteration** | Yes, via `remove()` (once per `next()`, `IllegalStateException` otherwise) | Yes, via `remove()` | No — no iterator reference is exposed; removing via the collection throws `ConcurrentModificationException` |
| **Add during iteration** | No | Yes, via `add()` (inserts at the cursor, no preceding `next()` required) | No |
| **Replace element during iteration** | No | Yes, via `set()` (after `next()`/`previous()`) | No |
| **Index access** | No | Yes — `nextIndex()` and `previousIndex()` (`previousIndex()` is `-1` before the first element; `nextIndex()` is `size()` after the last) | No (must manage a counter manually if needed) |
| **Fail-fast behavior** | Yes, in `java.util` collections (best-effort; `ConcurrentModificationException` on structural change detected at the next iterator operation) | Yes, same as `Iterator`, except its own `set()`/`add()`/`remove()` are coordinated with the modification counter | Yes — the underlying synthesized iterator fails fast exactly like an explicit one |
| **Verbosity / control** | More verbose than for-each, but gives you the cursor and `remove()` | Most verbose, most control (position, direction, mutation) | Least verbose; best when you only need to read every element in order |
| **Java version** | Since 1.2; `forEachRemaining` added in Java 8 | Since 1.2 | Since Java 5 (language feature) |

---

## 6. Common Pitfalls and Best Practices

### Pitfall 1 — Calling `next()` without checking `hasNext()`

**Wrong:**

```java
Iterator<String> it = list.iterator();
String first = it.next();          // fine only if the list is non-empty
String second = it.next();         // NoSuchElementException if list.size() < 2
```

If you don't know the collection is non-empty, an unchecked `next()` throws `java.util.NoSuchElementException` at runtime.

**Right:**

```java
if (it.hasNext()) {
    String first = it.next();      // safe, but remember: calling hasNext() doesn't consume
}
```

**Best practice.** Always guard `next()` with `hasNext()` in loops; for single elements, check emptiness first (e.g., `!list.isEmpty()`) or use an `if (it.hasNext())` guard. Never treat `NoSuchElementException` as expected control flow — let the guard prevent it.

### Pitfall 2 — Skipping `next()` before `remove()`

**Wrong:**

```java
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    it.hasNext();                  // pointless peek, consumes nothing
    it.remove();                   // IllegalStateException: no element returned yet
}
```

`remove()` must immediately follow a `next()` (or `previous()` for a `ListIterator`), and only once per call.

**Right:**

```java
while (it.hasNext()) {
    String s = it.next();
    if (shouldRemove(s)) {
        it.remove();               // removes the element just returned
    }
}
```

**Best practice.** Treat `remove()` and `set()` as operations tied to "the element I just consumed." If you need to look ahead, save elements in a local variable; don't attempt to remove an element you haven't consumed.

### Pitfall 3 — Modifying the collection inside a for-each loop

**Wrong:**

```java
for (String s : list) {
    if (s.equals("x")) {
        list.remove(s);            // ConcurrentModificationException on next iteration
    }
}
```

The for-each loop uses an iterator internally; calling the collection's `remove`/`add` invalidates that iterator.

**Right (three valid options):**

```java
// Option A: explicit iterator
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (it.next().equals("x")) it.remove();
}

// Option B: collect-then-remove
List<String> doomed = new ArrayList<>();
for (String s : list) if (s.equals("x")) doomed.add(s);
list.removeAll(doomed);

// Option C (Java 8+): Collection.removeIf
list.removeIf(s -> s.equals("x"));
```

**Best practice.** If the traversal is read-only, for-each is perfect. The moment you need to add or remove elements *while* traversing, either switch to an explicit `Iterator`/`ListIterator` or restructure to "collect then modify after the loop." Since Java 8, `removeIf` covers the most common remove-while-iterating case cleanly.

### Pitfall 4 — Using `ListIterator` on a non-`List` collection

**Wrong:**

```java
Set<String> set = new HashSet<>();
ListIterator<String> lit = set.listIterator();   // does not compile: Set has no listIterator()
```

The compiler rejects this — `listIterator()` is defined on `List`, not on `Collection` or `Set`. This is actually a good thing: it forces you to realize that bidirectional traversal and index semantics make sense only for ordered, indexable collections.

**Right:**

```java
List<String> list = new ArrayList<>(set);        // copy the set into a list if needed
ListIterator<String> lit = list.listIterator();
// ... use the ListIterator over the copy
```

**Best practice.** Reach for `ListIterator` only when you genuinely have a `List`. If you need bidirectional traversal over a `Set`, convert it to a list first (and be aware that `HashSet` order is arbitrary; use a `TreeSet` or `LinkedHashSet` if order matters).

### Pitfall 5 — Relying on `ConcurrentModificationException` as a control-flow mechanism

**Wrong:**

```java
try {
    for (String s : list) {
        if (someCondition) list.remove(s);
    }
} catch (ConcurrentModificationException e) {
    System.out.println("Caught it, continuing...");   // fragile and wrong
}
```

The Javadoc is explicit: fail-fast behavior exists to *detect bugs*, not to *handle* them. Relying on the exception to "skip" a modification is fragile because the exception timing is implementation-defined and because a truly concurrent modification might not be detected at all.

**Right:** Never write code whose correctness depends on the exception being thrown. Write code that avoids the situation entirely (use `iterator.remove()` / `removeIf` / collect-then-modify), and let `ConcurrentModificationException` serve as an alarm that wakes you up during testing and debugging.

---

## 7. Real-World Use Cases

**1. Batch processing of database results.** JDBC's `ResultSet` and many ORMs (e.g., JPA `Query.getResultStream()`) expose iterator-like cursors. Processing a large result set one row at a time avoids loading the entire table into memory — a classic application of **lazy iteration**. The iterator abstraction lets a data-access layer hide whether the "collection" is an in-memory list, a file, or a live database connection.

**2. Pagination and infinite sequences.** Iterators are naturally suited to sources that have no defined end or are too large to materialize. A paginated API client can expose `hasNext()`/`next()` where `next()` fetches the following page from a remote service. Custom iterators are the idiomatic way to model sequences such as Fibonacci numbers, prime-number generators, or token streams from a scanner, because the source can be computed on demand rather than stored.

**3. Implementing custom iterable collections.** If you write your own data structure — a skip list, a trie, a red-black tree, a graph adjacency view — you can implement `Iterable<T>` (or provide an `iterator()` method) and immediately get all the language support: for-each loops, `Stream` conversion, `Collections`-based algorithms. This is exactly how third-party collection libraries (Guava, Eclipse Collections) hook into the platform.

**4. Streaming API internals.** Since Java 8, `Stream` operations use `Spliterator`, a richer sibling of `Iterator` that supports partitioning for parallel execution. Understanding classic iterators makes `Spliterator` comprehensible: it is essentially an iterator that can also split itself into sub-iterators so multiple threads can process different chunks concurrently. Stream pipelines (`.filter().map().collect()`) and `StreamSupport.stream()` exist to let you build streams on top of custom iterable sources.

**5. UI data binding and event streams.** List-based UI components (table models, list adapters in Android/Swing/JavaFX) often iterate over a backing collection to render rows. An iterator-based abstraction lets the UI re-render incrementally as the model changes, and it is the conceptual basis for reactive/event-stream libraries (RxJava, Project Reactor), whose `Observable`/`Flux` types are essentially asynchronous iterators.

**When would you still use an explicit `Iterator` today?** The for-each loop and `Stream` cover most read-only cases, and `removeIf` covers common removal cases. You still want an explicit iterator when you need to: (a) remove or mutate elements mid-traversal, (b) interleave reading from two or more collections, (c) control traversal position manually, or (d) consume a custom/lazy iterable where the stream abstraction doesn't fit. In short — the modern APIs build on the iterator concept; the explicit iterator remains the tool when you need precise, imperative control over traversal.

---

## 8. Exercises and Review Questions

### Comprehension questions

**Q1.** What does `next()` return when the iterator has no remaining elements, and how can a well-written loop guarantee this never happens?

*Hint.* Consider the two method calls of the classic `while` loop pattern and which method *consumes* an element. The exception type is in `java.util`.

**Q2.** Why is it legal to call `ListIterator.add(e)` without first calling `next()`, while `ListIterator.set(e)` throws `IllegalStateException` in that situation?

*Hint.* `add` inserts relative to the *cursor position*; `set` replaces the *last returned element*. Which of the two depends on there being a "last returned element"?

**Q3.** Explain in your own words what "best-effort" means in the Javadoc statement that `ConcurrentModificationException` is a "best-effort" detection. Why can't the collections framework guarantee detection in the face of unsynchronized concurrent modification?

*Hint.* Think about race conditions between two threads updating the same `modCount` field, and about what the API docs say the exception is intended for.

### Applied problems

**P1.** Write a method `public static <E> List<E> removeEveryNth(List<E> list, int n)` that removes every *n*-th element (e.g., positions `n-1`, `2n-1`, …) using a `ListIterator`. Return the resulting list. What happens if `n <= 0`?

*Hint.* Use `list.listIterator()`. Keep a counter; after consuming an element at a multiple of `n`, call `it.remove()`. Guard `n <= 0` up front (treat it as invalid input, e.g., throw `IllegalArgumentException`).

**P2.** Write a method `public static <E> boolean hasRepeated(List<E> list)` that returns `true` if any element equals any other element at a different index, using **two iterators** (a nested loop over `list.listIterator(i+1)` for the inner scan). Do not use `Set` or `contains`.

*Hint.* For each index `i` from 0 to `size()-2`, take `list.listIterator(i+1)` and scan the tail; compare with `==` or `Objects.equals`. Beware `null` elements — use `Objects.equals(a, b)`.

**P3.** Write a method that iterates a `List<String>` and replaces every occurrence of a given string with its uppercase form *in place*, using `ListIterator.set`. Then verify the loop does not throw `ConcurrentModificationException`.

*Hint.* The pattern is: `while (lit.hasNext()) { String s = lit.next(); if (s.equals(target)) lit.set(s.toUpperCase()); }`. `set` is not a structural modification.

### Challenge question

**C1.** Design and implement a custom `Iterator<Integer>` for a binary search tree (BST) that yields the values in **in-order** (ascending) order. You may implement only the `Iterator` interface (no `remove` support — leave it throwing `UnsupportedOperationException`). Your iterator should be **lazy**: it must not traverse the whole tree in advance; it should use an explicit stack so that `hasNext()` and `next()` do `O(1)` amortized work per element. Then wrap your BST in an `Iterable<Integer>` so that a for-each loop prints the values in sorted order.

*Hint.* Push all left descendants of the current node onto a `Deque<Integer>`/`ArrayDeque<Node>` (used as a stack) during construction. `hasNext()` returns `!stack.isEmpty()`. `next()` pops the top node, returns its value, then pushes the node's right child and all of *its* left descendants. This is the classic iterative in-order traversal and runs in `O(n)` total time over the whole traversal. A skeleton:

```java
class TreeIterator implements Iterator<Integer> {
    private final Deque<Node> stack = new ArrayDeque<>();
    // push the leftmost spine of 'root' onto the stack

    public boolean hasNext() { return !stack.isEmpty(); }

    public Integer next() {
        // pop top; process; push right spine
    }
}
```

### Answer key / hints

- **Q1:** `next()` throws `java.util.NoSuchElementException`. The loop pattern `while (it.hasNext()) { it.next(); }` ensures `next()` is only invoked when `hasNext()` returned `true`.
- **Q2:** `set()` must know which element to replace — it replaces the *last element returned by `next()`/`previous()`*, so it requires a preceding call; `add()` only needs the cursor position, which always exists, so it never throws `IllegalStateException`.
- **Q3:** Two threads racing on the collection's `modCount` can interleave in ways that hide a modification (or the iterator may check at a moment that happens to miss it). The framework therefore *cannot* guarantee detection; it guarantees only a best-effort attempt, intended to surface bugs, never to be a correctness mechanism.
- **P1:** Wrap the loop in a counter; call `it.next()`, increment; if the count is a multiple of `n`, call `it.remove()`.
- **P2:** Outer `ListIterator` at index `i`; inner `listIterator(i+1)`; use `Objects.equals`.
- **P3:** `while (lit.hasNext()) { if (lit.next().equals(target)) lit.set(target.toUpperCase()); }`.
- **C1:** Standard stack-based in-order; each node is pushed once and popped once → `O(n)` total; `O(h)` space for the stack.

---

## 9. Summary

- **Iterators decouple traversal from representation.** The GoF Iterator pattern lets one piece of code walk through any collection (`ArrayList`, `LinkedList`, `HashSet`, …) through a uniform `hasNext()`/`next()` contract, without exposing internals.
- **The `Iterator<E>` contract is small but strict.** `hasNext()` peeks, `next()` consumes (throwing `NoSuchElementException` past the end), and `remove()` (optional) must immediately follow `next()` or it throws `IllegalStateException`. Java 8 added `forEachRemaining()`.
- **`ListIterator<E>` adds power for `List`s only.** It traverses both directions, reports indices via `nextIndex()`/`previousIndex()`, replaces the current element with `set()`, and inserts at the cursor with `add()` — enabling safe in-place mutation during traversal.
- **Fail-fast is a bug detector, not a feature.** `java.util` iterators track the collection's structural-modification count (`modCount`) and throw `ConcurrentModificationException` on the next iterator operation if it changed — on a best-effort basis that is not guaranteed under unsynchronized concurrency.
- **Remove through the iterator, never through the collection.** `it.remove()` is safe because the iterator updates its own modification bookkeeping; `collection.remove(x)` mid-iteration is the classic cause of `ConcurrentModificationException`.
- **The for-each loop is sugar over an iterator**, so it inherits fail-fast behavior and is perfect for read-only traversal; explicit iterators are still the right tool when you need removal, bidirectional movement, index awareness, or lazy generation.
- **Modern Java builds on the idea**: `Stream`/`Spliterator` extend the iterator concept to lazy, parallel processing, but the core mental model — a cursor that safely walks a collection one element at a time — remains the foundation of all of it.

---

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

---

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