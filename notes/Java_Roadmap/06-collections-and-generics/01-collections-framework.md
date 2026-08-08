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