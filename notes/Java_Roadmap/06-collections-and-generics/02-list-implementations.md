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