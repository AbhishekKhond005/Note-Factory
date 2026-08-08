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