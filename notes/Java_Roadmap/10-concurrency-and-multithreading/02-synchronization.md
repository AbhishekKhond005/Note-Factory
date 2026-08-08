# Java Synchronization: From Races to Robust Concurrency

## Table of Contents
1. [Introduction & Real-World Motivation](#1-introduction--real-world-motivation)
2. [The Problem: Race Conditions](#2-the-problem-race-conditions)
3. [The Core Solution: Mutual Exclusion via `synchronized`](#3-the-core-solution-mutual-exclusion-via-synchronized)
4. [`synchronized` Methods](#4-synchronized-methods)
5. [`synchronized` Blocks](#5-synchronized-blocks)
6. [Explicit Locks (`java.util.concurrent.locks`)](#6-explicit-locks-javautilconcurrentlocks)
7. [Deadlock](#7-deadlock)
8. [Best Practices, Pitfalls & Checklist](#8-best-practices-pitfalls--checklist)
9. [Practice Exercises & Further Reading](#9-practice-exercises--further-reading)

---

### 1. Introduction & Real-World Motivation

Imagine you are building the backend for a popular airline booking system. A flight has **200 seats**. On a busy Friday morning, 250 people try to book that flight within the same minute. Each booking request is handled by a **thread** — a lightweight unit of execution that runs concurrently with others. Every request must check "are there seats left?" and, if so, decrement the seat counter.

Now imagine 250 requests all perform this check *at the same instant*. Without careful coordination, it is entirely possible that two passengers both see "seat 142 available" and both book it. The airline just sold seat 142 twice. Your app is now on the evening news.

The same scenario plays out everywhere:

- **Bank transfers** — a withdrawal and a deposit touching the same account balance simultaneously.
- **Ride-sharing apps** — two drivers accepting the same ride request.
- **Multiplayer games** — two players looting the same chest at the same tick.
- **Inventory systems** — an online store overselling a limited product.
- **Social media** — a "likes" counter on a viral post being incremented by millions of users.

In all of these, the word that matters is **shared mutable state**: a piece of data (a seat counter, a balance, an inventory count) that *multiple threads can read and modify*. Single-threaded programs never face this problem because only one thing happens at a time. The instant you introduce concurrency, you must answer a hard question:

> **How do we allow many things to happen "at once" while guaranteeing that shared data is never corrupted?**

That question is the subject of this chapter. **Synchronization** is the set of mechanisms that coordinate threads' access to shared data.

By the end of this section, you will be able to:

- Explain, in plain language, why concurrent access to shared data is dangerous.
- Write thread-safe Java code using `synchronized` methods and blocks.
- Use explicit `Lock`s (`ReentrantLock`, `ReentrantReadWriteLock`) for finer control.
- Recognize and prevent **deadlock**.
- Avoid the classic pitfalls that turn "thread-safe" code back into "thread-broken" code.

---

### 2. The Problem: Race Conditions

#### 2.1 Definition

A **race condition** is a flaw that occurs when the correctness of a program depends on the *timing* or *interleaving* of operations performed by multiple threads. The result varies depending on *who runs first, and in what order* — not on the logic of the code itself. The same program can produce different answers on different runs, and most of those answers are wrong.

Think of it this way: a race condition is when two or more threads "race" to use the same data, and the outcome is determined by which one wins the race — a random, uncontrollable factor.

#### 2.2 A Concrete Example: The Shared Counter

Here is the classic minimal example. We have a `Counter` with a single `int` field, and we let two threads each call `increment()` 100,000 times. If everything works, the final value should be **200,000**.

```java
public class Counter {
    private int count = 0;

    public void increment() {
        count++;
    }

    public int getCount() {
        return count;
    }
}
```

```java
public class RaceDemo {
    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();

        // Two threads share the SAME Counter object.
        Thread threadA = new Thread(() -> {
            for (int i = 0; i < 100_000; i++) {
                counter.increment();
            }
        });

        Thread threadB = new Thread(() -> {
            for (int i = 0; i < 100_000; i++) {
                counter.increment();
            }
        });

        threadA.start();
        threadB.start();

        // main() waits for both threads to finish.
        threadA.join();
        threadB.join();

        System.out.println("Expected: 200000");
        System.out.println("Actual:   " + counter.getCount());
    }
}
```

**Sample output (varies from run to run):**

```
Expected: 200000
Actual:   153247
```

Run it twenty times and you will get twenty different (wrong) numbers. Sometimes you'll get a value that is *higher* than the expected 200,000 — a sign that the corruption is even more severe than simple lost updates.

#### 2.3 Why Does This Happen? The Interleaving

Here's the crux: `count++` looks like one operation in Java, but the CPU executes it as **three separate steps**:

1. **Read** the current value of `count` from memory.
2. **Add** 1 to it (inside the CPU).
3. **Write** the new value back to memory.

Because these three steps are not atomic (they are not indivisible), two threads can interleave. Suppose `count` is `0` and both threads execute `increment()`:

| Step | Thread A | Thread B | Value of `count` in memory |
|------|----------|----------|----------------------------|
| 1 | Reads `count` → **0** | | 0 |
| 2 | Adds 1 → has **1** (not yet written!) | Reads `count` → **0** | 0 |
| 3 | Writes **1** back | Adds 1 → has **1** | 1 |
| 4 | | Writes **1** back (overwrites A's write) | **1** |

Both threads increment once, but the counter only moved from 0 to **1**. One increment was **lost**. This is called a **lost update**. Multiply this by thousands of interleavings per run, and you get the chaotic output above.

#### 2.4 A Non-Code Analogy

> **Two chefs, one order ticket.** Imagine a busy kitchen where two chefs cook simultaneously. Each time a dish is plated, a chef adds a tally mark to the same order ticket taped to the pass. Chefs never coordinate: Chef A glances at the ticket (reads), walks away to grab a pan, comes back and adds a mark based on the *old* number they saw, while Chef B has meanwhile added their own marks and scribbled over the same space. The ticket ends up with too few tally marks, the kitchen undercounts dishes, and nobody can tell which orders actually shipped. The ticket is the shared state; the uncoordinated chefs are the threads.

#### 2.5 The Correct vs. Incorrect Output

- **Incorrect:** The program reports *anything other than* 200,000 — a lost update, a corrupted count, inconsistent intermediate values, and non-deterministic behavior between runs.
- **Correct:** Every run prints exactly `200000`, because every increment is applied exactly once to the shared counter, regardless of timing.

The fix is not to make the threads "luckier." The fix is to give them a *coordination mechanism* so that the read-modify-write sequence can never be interleaved with another thread's. That mechanism is **mutual exclusion**, and in Java its simplest form is the `synchronized` keyword.

---

### 3. The Core Solution: Mutual Exclusion via `synchronized`

#### 3.1 The Concept: A Monitor / Intrinsic Lock

The central idea in Java synchronization is the **monitor** — often called the **intrinsic lock** or **monitor lock**. Every Java object carries a hidden lock with it. A thread that wants to execute a block of code marked `synchronized` must first **acquire** that object's lock. While a thread holds the lock, no other thread can acquire it; any contender **blocks** (waits) until the lock is released.

> **The single-key restroom.** Picture a restroom with exactly one key hanging on the wall. A person who wants to use it must take the key, lock the door, and do their business while everyone else waits in line. When they finish, they unlock the door and hang the key back. Only one person is ever *inside* at a time — that is mutual exclusion. The key is the lock; the restroom is the critical section; the waiting people are the blocked threads.

#### 3.2 Fixing the Race Condition

We fix the counter by marking `increment()` and `getCount()` as `synchronized`:

```java
public class SafeCounter {
    private int count = 0;

    public synchronized void increment() {
        count++;   // read, add, write — now all inside one lock
    }

    public synchronized int getCount() {
        return count;
    }
}
```

Because `increment()` is `synchronized`, the entire read-modify-write sequence now runs with the lock held. No other thread can interleave a read, add, or write into the middle of it. The lost-update scenario is impossible.

Running the previous demo against `SafeCounter` will now reliably print `200000` every single time. Try it.

#### 3.3 What "Entering a `synchronized` Method/Block" Actually Does

When a thread enters a `synchronized` region, the JVM guarantees this exact sequence:

1. **Acquire the lock.** If no other thread holds it, the acquiring thread takes it. If another thread holds it, the contender blocks until the lock becomes available. The JVM tracks which thread owns the lock.
2. **Execute the body.** The thread runs the code inside the critical section. Because it owns the lock, it has exclusive access to any data protected by that lock.
3. **Release the lock — automatically, no matter what.** The lock is released on *every* exit path: a normal return, a `return` statement, a thrown exception, even a `System.exit`-style abrupt exit of the block. Java's `synchronized` is *exception-safe by construction*; you never have to remember to "unlock" it.

This automatic release on exception is a huge reliability win, and you will see how much boilerplate it saves when we compare against explicit `Lock`s in Section 6.

#### 3.4 Two Benefits, Not One

A common misconception is that `synchronized` only provides **mutual exclusion** (only one thread at a time). It provides a second, equally vital guarantee:

- **Visibility.** When a thread exits a `synchronized` region, its writes are flushed to main memory. When the next thread acquires the same lock, it is guaranteed to see those writes. The Java Memory Model (JLS §17) defines a **happens-before** relationship: *unlocking a monitor happens-before every subsequent locking of that same monitor.*

In other words, `synchronized` solves both "two threads stepping on each other" (atomicity) and "one thread doesn't see another's changes" (visibility). The two are often forgotten separately, and forgetting either one is enough to break your program.

---

### 4. `synchronized` Methods

#### 4.1 Syntax and Semantics

Making a method `synchronized` is the simplest possible way to protect shared state:

```java
public synchronized void methodName() {
    // critical section — the whole method body
}
```

Semantically, a `synchronized` instance method is equivalent to wrapping the entire body in a `synchronized (this) { ... }` block. The question that matters is: **which lock is held?**

| Kind of method | Lock target | Effect |
|----------------|-------------|--------|
| `synchronized` **instance** method | The object `this` on which the method is called | All synchronized instance methods of the same object are mutually exclusive with each other |
| `synchronized` **static** method | The `Class` object (e.g., `Counter.class`) | All synchronized static methods of the same class are mutually exclusive with each other |

Note the subtlety: a synchronized instance method and a synchronized static method **do not** exclude each other — they lock on *different* objects (`this` vs. `Counter.class`).

#### 4.2 Complete Example: A Thread-Safe Bank Account

```java
public class BankAccount {
    private double balance;

    public BankAccount(double initialBalance) {
        this.balance = initialBalance;
    }

    public synchronized void deposit(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        balance += amount;
    }

    public synchronized void withdraw(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (balance < amount) {
            throw new IllegalStateException("Insufficient funds");
        }
        balance -= amount;
    }

    public synchronized double getBalance() {
        return balance;
    }
}
```

**How it works:** every `deposit`, `withdraw`, and `getBalance` locks on the `BankAccount` instance. Two threads calling `deposit` on the *same* account run strictly one after another — no interleaving, no lost updates, and each thread is guaranteed to see the latest committed balance.

**What it does NOT do:** it does *not* protect you if two threads operate on *different* `BankAccount` objects concurrently. Each account has its own lock. That's exactly what you want — accounts should not block each other — but it means you must think carefully about *which objects* share which locks. (This becomes critical in the deadlock section.)

#### 4.3 Reentrancy: Same Thread Can Re-Acquire

Suppose a synchronized method calls another synchronized method on the *same* object:

```java
public class ReentrantDemo {

    public synchronized void outer() {
        System.out.println("outer() — lock held: " + Thread.currentThread().getName());
        inner();   // same thread re-enters a synchronized method on the same object
    }

    public synchronized void inner() {
        System.out.println("inner() — lock re-acquired: " + Thread.currentThread().getName());
    }

    public static void main(String[] args) {
        new ReentrantDemo().outer();
    }
}
```

Output (this works — it does *not* deadlock):

```
outer() — lock held: main
inner() — lock re-acquired: main
```

This works because Java locks are **reentrant**: the JVM tracks how many times the *owning thread* has acquired the lock (a hold counter). Each acquisition increments the count; each exit decrements it; the lock is truly released only when the count reaches zero.

**Why this matters:** without reentrancy, an object's synchronized method could not safely call another synchronized method of the same object — even from the same thread. Reentrancy means you can freely compose synchronized methods (e.g., `transfer` calling `withdraw` and `deposit`) without self-deadlock.

**Pitfall to note now:** reentrancy is per-thread, but it does **not** mean two different threads can hold the same lock. A second thread calling `outer()` while the first is inside it will still block until the first thread fully exits `outer()`.

---

### 5. `synchronized` Blocks

#### 5.1 Syntax

Sometimes locking an entire method is too coarse. A `synchronized` block lets you lock on *any* object and protect *only* the code that needs protection:

```java
synchronized (someObject) {
    // critical section: only this code is protected
}
```

#### 5.2 When a Block Is Better Than a Method

You should prefer a `synchronized` block over a `synchronized` method when:

- **The critical section is small** — e.g., only one field assignment, not 50 lines of logging, I/O, and validation.
- **You need to protect code that spans multiple method calls** — e.g., a read-check-update sequence.
- **You want to lock on a dedicated lock object**, so unrelated data protected by other locks is never blocked by this section.
- **You need to coordinate with other code** that locks on a specific object (e.g., a shared collection's `Collections.synchronizedList` view).

A bigger critical section means more threads wait in line — **coarse locking** hurts throughput. Keep the locked region as small as possible (**fine-grained locking**), but not so small that it no longer protects the invariant you care about.

#### 5.3 Example: Locking on a Dedicated Lock Object

```java
import java.util.ArrayList;
import java.util.List;

public class SynchronizedList<E> {
    // A dedicated, private lock object. Nothing else in the program can touch it.
    private final Object lock = new Object();
    private final List<E> list = new ArrayList<>();

    public void add(E element) {
        synchronized (lock) {
            list.add(element);
        }
    }

    public E get(int index) {
        synchronized (lock) {
            return list.get(index);
        }
    }

    public int size() {
        synchronized (lock) {
            return list.size();
        }
    }
}
```

**Why a dedicated lock object?** Because nothing else in the program can reference `lock`, nobody can accidentally create a lock-order interaction with it, and it stays constant for the object's lifetime. (Contrast: synchronizing on `this` lets *any* code with a reference to the object lock it — which is sometimes useful, sometimes a liability.)

#### 5.4 Example: Locking on the Class Object

When you need to protect **static** shared state, lock on the `Class` object:

```java
public class ConnectionManager {
    private static int activeConnections = 0;
    private static final int MAX_CONNECTIONS = 100;

    public static boolean tryOpenConnection() {
        // Lock on the Class object, because the shared field is static.
        synchronized (ConnectionManager.class) {
            if (activeConnections >= MAX_CONNECTIONS) {
                return false;
            }
            activeConnections++;
            return true;
        }
    }

    public static void closeConnection() {
        synchronized (ConnectionManager.class) {
            if (activeConnections > 0) {
                activeConnections--;
            }
        }
    }

    public static int getActiveConnections() {
        synchronized (ConnectionManager.class) {
            return activeConnections;
        }
    }
}
```

This protects the static counter across *all* instances of `ConnectionManager` — which is the whole point, since there is no `this` in a static context.

#### 5.5 `synchronized` Method vs. `synchronized` Block

| Criterion | `synchronized` method | `synchronized` block |
|-----------|----------------------|----------------------|
| Lock target | Fixed: `this` (instance) or `Class` (static) | Any object you choose, including a dedicated private lock |
| Lock scope | Entire method body | Only the code inside `{ ... }` |
| Granularity | Coarse — often locks more than necessary | Fine — you control exactly what is protected |
| Flexibility | Low (one lock per method) | High (different sections can use different locks) |
| Boilerplate | None | Slightly more (must remember `{ }`) |
| Reentrancy | Yes | Yes |
| Common use cases | Small classes whose whole state is one invariant (counters, accounts) | Locking a single field, guarding compound operations, protecting access to a shared collection |

**Rule of thumb:** start with the simplest thing that is *correct* (a synchronized method). If profiling shows contention, shrink the critical section with blocks. Optimize for correctness first, throughput second.

---

### 6. Explicit Locks (`java.util.concurrent.locks`)

#### 6.1 Why Explicit Locks?

The `synchronized` keyword is simple and exception-safe, but it lacks several capabilities:

- You **cannot interrupt** a thread that is blocked waiting for a `synchronized` lock.
- You **cannot try** to acquire a lock and walk away if it's busy (`tryLock`).
- You **cannot wait for a limited time** (timed acquisition).
- You **cannot choose fair** (FIFO) ordering; `synchronized` is inherently unfair.

The `java.util.concurrent.locks` package solves all of these with the `Lock` interface and its implementations.

#### 6.2 The Core Abstraction: `Lock` and `ReentrantLock`

`Lock` is an interface; `ReentrantLock` is its most important implementation. Like `synchronized`, it is reentrant (the same thread can re-acquire). Unlike `synchronized`, *you* are responsible for releasing it — hence the famous **try/finally** pattern:

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockedCounter {
    private final Lock lock = new ReentrantLock();
    private int count = 0;

    public void increment() {
        lock.lock();          // 1. acquire — blocks if another thread holds it
        try {
            count++;          // 2. critical section
        } finally {
            lock.unlock();    // 3. ALWAYS release, even on exception
        }
    }

    public int getCount() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
```

**Never write a bare `lock.lock()` followed by code without a `finally { lock.unlock(); }`.** If an exception escapes, the lock stays locked forever and every other thread blocks permanently. This is the single most common `Lock` bug.

#### 6.3 `tryLock()` — Non-Blocking and Timed Acquisition

`tryLock()` attempts to acquire the lock without blocking indefinitely:

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SeatReserver {
    private final Lock lock = new ReentrantLock();
    private int seatsLeft = 3;

    // Returns true if the seat was reserved, false if we gave up waiting.
    public boolean reserveSeat() {
        // Try to grab the lock; if busy, return immediately with false.
        if (!lock.tryLock()) {
            System.out.println("Too busy to wait — try again later.");
            return false;
        }
        try {
            if (seatsLeft > 0) {
                seatsLeft--;
                System.out.println("Seat reserved. Remaining: " + seatsLeft);
                return true;
            }
            System.out.println("Sold out.");
            return false;
        } finally {
            lock.unlock();
        }
    }
}
```

With a timeout, the thread waits up to a bounded time and then gives up:

```java
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TimedReserver {
    private final Lock lock = new ReentrantLock();
    private int stock = 1;

    public void reserve() {
        boolean acquired = false;
        try {
            acquired = lock.tryLock(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // restore the interrupt flag
        }

        if (!acquired) {
            System.out.println("Could not reserve within 500 ms.");
            return;
        }

        try {
            if (stock > 0) {
                stock--;
                System.out.println("Reserved one item.");
            }
        } finally {
            lock.unlock();
        }
    }
}
```

Note the good practice in the `catch`: re-setting the interrupted status with `Thread.currentThread().interrupt()`, so the rest of the program can still observe that the thread was interrupted.

#### 6.4 `lockInterruptibly()` — Interruptible Waiting

If a thread is blocked inside `lockInterruptibly()` waiting for the lock, another thread can call `thread.interrupt()` on it, and it will wake up, give up waiting, and throw `InterruptedException`. With plain `synchronized`, that thread would be stuck forever. This is essential for responsive, cancellable operations:

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class InterruptibleWorker {
    private final Lock lock = new ReentrantLock();

    public void doWork() throws InterruptedException {
        // If interrupted while waiting for the lock, we throw and never enter the try.
        lock.lockInterruptibly();
        try {
            System.out.println("Holding the lock. Working...");
            Thread.sleep(10_000);
        } finally {
            lock.unlock();   // safe: if we got here, we hold the lock
        }
    }
}
```

#### 6.5 Read/Write Locks: `ReentrantReadWriteLock`

A `ReadWriteLock` maintains **two locks**: a shared **read lock** and an exclusive **write lock**.

> **The shared textbook.** Many students can *read* the same textbook at the same time — readers don't interfere with each other. But when someone needs to *write* in it, everyone must pause; the writer needs exclusive access until they finish. Multiple reads are safe together; any write must be alone.

This is a huge performance win for **read-heavy** workloads: instead of serializing all readers (which `synchronized` and `ReentrantLock` would do), `ReentrantReadWriteLock` lets unlimited readers proceed in parallel.

**Complete example — a read-heavy cache:**

```java
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteCache<K, V> {
    private final Map<K, V> cache = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // Multiple readers can run this method at the same time.
    public V get(K key) {
        lock.readLock().lock();
        try {
            return cache.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public V put(K key, V value) {
        // Writers are exclusive — no readers and no other writers during this.
        lock.writeLock().lock();
        try {
            return cache.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
```

**Pitfall:** the read lock is *shared*, not free. Two readers can coexist, but a thread holding the read lock cannot upgrade to the write lock without deadlock. And if writers are frequent, readers can starve them (readers always win since reads don't block each other). Modern practice often prefers `ConcurrentHashMap` for caches like this (see Section 8), but `ReadWriteLock` remains the right tool when you need fine control over reads vs. writes.

#### 6.6 Comparison: `synchronized` vs. `Lock`

| Criterion | `synchronized` | `Lock` / `ReentrantLock` |
|-----------|----------------|--------------------------|
| Syntax | Keyword, no boilerplate | `lock()` / `unlock()` with try/finally |
| Auto-release on exception | Yes (built in) | No — you must `unlock()` in `finally` |
| Blocking acquisition | Only `lock()` | `lock()`, `tryLock()`, `tryLock(timeout)`, `lockInterruptibly()` |
| Interruptible wait | No | Yes (via `lockInterruptibly()`) |
| Timed acquisition | No | Yes (via `tryLock(timeout, unit)`) |
| Fairness option | Unfair only | Configurable: `new ReentrantLock(true)` = FIFO fair |
| Read/write separation | Not possible | Yes (`ReentrantReadWriteLock`) |
| Multiple condition variables | No | Yes (`Condition`) |
| Learning curve | Trivial | Moderate; easy to misuse |

**Rule of thumb:** prefer `synchronized` unless you concretely need interruptibility, timed waits, fairness, or read/write separation. `synchronized` is exception-safe, simpler, and heavily optimized by modern JVMs.

---

### 7. Deadlock

#### 7.1 Definition and the Four Necessary Conditions

A **deadlock** is a situation in which two or more threads are each holding a lock and waiting for a lock held by another thread, so none of them can make progress — forever. Deadlock is the second great killer of concurrent programs (after race conditions), and it is uniquely nasty because the program *looks* like it's just stuck: no exception, no error, just a silent freeze.

For deadlock to occur, all **four necessary conditions** must hold simultaneously:

1. **Mutual exclusion** — each resource (lock) can be held by only one thread at a time.
2. **Hold and wait** — a thread holds at least one resource while waiting to acquire another.
3. **No preemption** — a thread holding a resource cannot have it forcibly taken away.
4. **Circular wait** — there is a cycle of threads, each waiting on a resource held by the next.

If any one condition is broken, deadlock becomes impossible.

#### 7.2 A Reliable Deadlock Example

The classic setup: two threads each need two locks, but acquire them in **opposite order**.

```java
public class DeadlockDemo {
    private static final Object RESOURCE_A = new Object();
    private static final Object RESOURCE_B = new Object();

    public static void main(String[] args) {
        Thread threadA = new Thread(() -> {
            synchronized (RESOURCE_A) {
                System.out.println(Thread.currentThread().getName() + " locked A");
                sleep(100);   // widen the race so the deadlock actually happens
                synchronized (RESOURCE_B) {
                    System.out.println(Thread.currentThread().getName() + " locked B");
                }
            }
        }, "Thread-A");

        Thread threadB = new Thread(() -> {
            synchronized (RESOURCE_B) {
                System.out.println(Thread.currentThread().getName() + " locked B");
                sleep(100);
                synchronized (RESOURCE_A) {
                    System.out.println(Thread.currentThread().getName() + " locked A");
                }
            }
        }, "Thread-B");

        threadA.start();
        threadB.start();

        // Wait a while, then observe that the program never finishes.
        try {
            threadA.join();
            threadB.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Both threads finished — no deadlock this run.");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

When run, this program usually *hangs* — the final print never appears. (If you run it many times, occasionally the timing is lucky and it completes; the `sleep(100)` calls make the deadlock reproducible, not guaranteed.)

#### 7.3 Walking Through the Deadlock Step by Step

| Time | Thread A | Thread B | Locks held |
|------|----------|----------|------------|
| t1 | Acquires **A** | Acquires **B** | A: {A}, B: {B} |
| t2 | Sleeps | Sleeps | A: {A}, B: {B} |
| t3 | Wants **B** — held by B → **blocks** | Wants **A** — held by A → **blocks** | A: {A} waiting for B; B: {B} waiting for A |
| t4 | Still blocked | Still blocked | **Circular wait — deadlock** |

At t3 we have the classic **circular wait**: A holds A and wants B; B holds B and wants A. Neither can proceed because neither will give up its current lock. The program freezes forever.

#### 7.4 The Four Conditions and How to Break Each

| Condition | Meaning | How to break it |
|-----------|---------|-----------------|
| **Mutual exclusion** | Only one thread can hold a lock at a time | Avoid locks entirely (use lock-free structures, `AtomicInteger`, immutable data) — not always possible |
| **Hold and wait** | Thread holds a resource while waiting for another | Acquire *all* needed locks atomically (try to grab everything, release all if you can't) |
| **No preemption** | Locks can't be taken away | Use `tryLock(timeout)` — if the second lock isn't available, **give up** the first lock and retry |
| **Circular wait** | A cycle exists in the waits-for graph | Enforce a **global lock ordering** — everyone acquires locks in the same order |

The two most practical, code-level strategies are **lock ordering** and **timeouts with `tryLock`**. A third blunt instrument is **using a single lock** (or avoiding nested locks entirely).

#### 7.5 Prevention Strategy 1: Lock Ordering

If every thread always acquires locks in the same global order, cycles are impossible. For bank transfers, order by a unique, immutable account id:

```java
import java.util.concurrent.atomic.AtomicInteger;

public class BankAccount {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);
    private final int id = ID_GENERATOR.getAndIncrement();   // unique, never changes
    private double balance;

    public BankAccount(double initialBalance) {
        this.balance = initialBalance;
    }

    public synchronized void debit(double amount) {
        balance -= amount;
    }

    public synchronized void credit(double amount) {
        balance += amount;
    }

    public synchronized double getBalance() {
        return balance;
    }

    public int getId() {
        return id;
    }

    /**
     * Deadlock-free transfer: both parties always acquire locks in
     * ascending id order, so no cycle can ever form.
     */
    public static void safeTransfer(BankAccount from, BankAccount to, double amount) {
        BankAccount first = (from.id < to.id) ? from : to;
        BankAccount second = (first == from) ? to : from;

        synchronized (first) {
            synchronized (second) {
                from.debit(amount);
                to.credit(amount);
            }
        }
    }
}
```

**Why this works:** suppose Thread A transfers from account 5 to account 9, and Thread B transfers from 9 to 5. Both threads acquire lock 5 *before* lock 9, so they can never each hold one end of a cycle and wait for the other.

#### 7.6 Prevention Strategy 2: Timeouts with `tryLock`

Instead of blocking forever on the second lock, use `tryLock` and **release everything and retry** if you can't get it. This converts deadlock (permanent) into a retry (transient):

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SafeTransfer {
    private final Lock lock = new ReentrantLock();
    private double balance;

    public SafeTransfer(double initialBalance) {
        this.balance = initialBalance;
    }

    /**
     * Tries a two-lock transfer without deadlocking.
     * Returns true on success, false if it had to give up (caller may retry).
     */
    public boolean tryTransfer(SafeTransfer target, double amount) {
        if (this.lock.tryLock()) {
            try {
                if (target.lock.tryLock()) {
                    try {
                        this.balance -= amount;
                        target.balance += amount;
                        return true;
                    } finally {
                        target.lock.unlock();
                    }
                }
                // Could not get the second lock — give up the first and retry later.
                return false;
            } finally {
                this.lock.unlock();
            }
        }
        return false;
    }
}
```

The key insight: **no preemption** is broken. A thread that can't get the second lock voluntarily releases the first. Deadlock cannot persist because threads *give up* rather than wait forever.

#### 7.7 Prevention Strategy 3: A Single Lock

The simplest possible fix — use **one** lock to guard all resources involved:

```java
public class SingleLockTransfer {
    private static final Object GLOBAL_LOCK = new Object();
    private double balance;

    public void transfer(SingleLockTransfer target, double amount) {
        synchronized (GLOBAL_LOCK) {   // one lock for every transfer
            this.balance -= amount;
            target.balance += amount;
        }
    }
}
```

This can never deadlock because there are never two locks to form a cycle. The price is throughput: every transfer in the entire system serializes. For a demo, perfect; for a high-traffic banking core, you'd want the lock-ordering solution instead.

#### 7.8 Real-World Context and Debugging

- **Databases:** deadlocks are routine under concurrent transactions; databases detect them, abort one victim transaction, and let you retry. (Exactly the "give up and retry" philosophy of `tryLock`.)
- **Distributed systems:** deadlock can also happen across machines when distributed locks are acquired in inconsistent orders — hence distributed coordination services (etcd, ZooKeeper) and consistent lock-ordering conventions.
- **Debugging Java deadlocks:** when a program hangs, obtain a **thread dump**:
  - `jps` to find the process id, then `jstack <pid>`
  - Or press `Ctrl+\` on Unix / `Ctrl+Break` on Windows
  - Or use `jvisualvm`, `jconsole`, or `jcmd <pid> Thread.print`

A thread dump will clearly show the cycle, e.g.:

```
"Thread-A" waiting to lock <0x...B> (held by "Thread-B")
"Thread-B" waiting to lock <0x...A> (held by "Thread-A")
Found one Java-level deadlock.
```

When you see "Found one Java-level deadlock", you have the smoking gun: find where the locks were acquired, then reorder them consistently.

---

### 8. Best Practices, Pitfalls & Checklist

#### 8.1 Common Mistakes

1. **Locking the wrong object.**
   ```java
   // WRONG: the lock field is reassigned, so threads may lock DIFFERENT objects.
   private Object lock = new Object();
   public void update() {
       synchronized (lock) { ... }
   }
   public void reset() { lock = new Object(); }   // breaks mutual exclusion!
   ```
   Make the lock `final` (or, for a field you're protecting, synchronize on `this`).

2. **Synchronizing on `String` literals or boxed types.**
   ```java
   // WRONG: all uses of this literal in the whole JVM may share ONE interned object.
   private static final String LOCK = "config-lock";
   // WRONG: value caching means Integer values -128..127 may be shared across classes.
   private static final Integer LOCK = 42;
   ```
   String literals are interned and small `Integer` values are cached, so unrelated code could be contending on your lock (or worse, another library could hold it). Use a dedicated `new Object()` or a named lock object.

3. **Critical sections too large.** Doing slow I/O, network calls, or long computations inside a lock makes every other thread wait. Move everything not touching shared state *outside* the lock. **Never** call unknown third-party code while holding a lock (it might try to lock something you don't control).

4. **`volatile` misuse.** `volatile` guarantees **visibility** but **not atomicity**. `count++` on a `volatile int` is still a race; `volatile` only helps when the write does not depend on a read-modify-write (e.g., a boolean flag, a safely published reference). If in doubt, use `synchronized` or `AtomicInteger`.

5. **Forgetting `finally` for `Lock.unlock()`.** With explicit locks, a thrown exception before `unlock()` permanently wedges the lock. Always:
   ```java
   lock.lock();
   try { ... } finally { lock.unlock(); }
   ```
   (Forgetting this is impossible with `synchronized` — another reason to prefer it when you don't need `Lock` features.)

6. **Hand-rolling double-checked locking.** The classic lazy-singleton idiom is famously subtle. If you really need it, use a properly declared `volatile` field, or better, use the initialization-on-demand holder idiom or `ConcurrentHashMap.computeIfAbsent`. When in doubt, just mark the method `synchronized` — the performance difference is rarely worth the risk.

7. **Assuming `synchronized` methods on different objects exclude each other.** They don't. Mutual exclusion is per-lock-object, and it is your job to map shared *data* to shared *locks* correctly.

#### 8.2 Summary of Constructs Introduced

| Construct | Lock target | Strengths | Weaknesses |
|-----------|-------------|-----------|------------|
| `synchronized` instance method | `this` | Simple, exception-safe, reentrant | Whole method locked; no timeout/interrupt; unfair |
| `synchronized` static method | `Class` object | Simple protection of static state | Same limits as above |
| `synchronized` block | Any object you choose | Fine-grained control, dedicated locks | Must choose the right object yourself |
| `ReentrantLock` | Explicit `Lock` object | `tryLock`, timeouts, `lockInterruptibly`, fairness | Boilerplate; forgetting `unlock()` wedges threads |
| `ReentrantReadWriteLock` | Read/write lock pair | Concurrent reads, exclusive writes | Writer starvation risk; more complex; no upgrade |
| `AtomicInteger` (see 8.3) | none (hardware CAS) | Lock-free, great for single counters | Only for simple single-variable updates |

#### 8.3 Higher-Level Abstractions: Less Locking, More Safety

These `java.util.concurrent` tools often let you avoid hand-written locking entirely.

- **`ConcurrentHashMap<K,V>`** — a thread-safe map using *internal fine-grained locking* (per-bucket). Reads are lock-free, and it offers atomic compound operations like `putIfAbsent` and `compute`. If you need a shared map, prefer this over `HashMap` + manual synchronization.
- **`AtomicInteger` / `AtomicLong` / `AtomicReference`** — lock-free wrappers using **compare-and-set (CAS)** hardware instructions. Perfect for counters, sequences, and single-value updates:
  ```java
  import java.util.concurrent.atomic.AtomicInteger;

  public class AtomicCounter {
      private final AtomicInteger count = new AtomicInteger(0);

      public void increment() {
          count.incrementAndGet();   // atomic read-modify-write, no locks needed
      }

      public int get() {
          return count.get();
      }
  }
  ```
- **`Executors` / thread pools** — instead of spawning raw `Thread`s, submit tasks to a pool (`Executors.newFixedThreadPool(n)`). Pools bound the number of concurrent threads, reduce context-switch overhead, and make shutdown (and thus resource leaks) manageable. For scheduled or "one big task" problems, pools and `Future`/`CompletableFuture` let you express concurrency at a higher level.

#### 8.4 Checklist for Writing Thread-Safe Code

1. **Identify the shared state.** List every field readable/writable by more than one thread.
2. **Define the invariants.** For each piece of shared state, state the rule that must hold (e.g., `balance == sum of all deposits minus withdrawals`).
3. **Pick the right mechanism.** `synchronized` for simple cases; explicit `Lock` if you need timeouts/interrupts/fairness; `ReadWriteLock` for read-heavy data; `Atomic*` or `ConcurrentHashMap` for single-variable or collection cases.
4. **Map data to locks deliberately.** Every piece of shared data must be guarded by exactly one designated lock — and every access to that data must go through that lock.
5. **Keep critical sections minimal.** Lock only what touches shared state; do I/O and computation outside.
6. **Release every explicit lock in `finally`.** If you write `lock()`, write the try/finally immediately.
7. **Avoid nested locks** unless you control the order — and if you must nest, establish a global lock ordering.
8. **Release resources in `finally` or try-with-resources** for anything else (streams, connections).
9. **Test under concurrency stress**, not just happy-path runs. Run your race demo 1,000 times; use `-Xint` (interpreted mode) and slow machines to surface races; use thread dumps on hangs.
10. **Document the locking protocol** in comments so future maintainers don't break the invariant.

---

### 9. Practice Exercises & Further Reading

#### Exercise 1 — Spot and Fix the Race (Easy)

Given the `Counter` from Section 2, run it until you observe a wrong value. Then:
- Fix it using a `synchronized` method.
- Fix it again using a `synchronized` block on a dedicated lock.
- Fix it a third time using `AtomicInteger`.

**Expected outcome:** all three versions reliably print `200000`. Compare the three solutions' readability and line counts.

#### Exercise 2 — Convert `synchronized` to `Lock` (Easy–Medium)

Take your `SafeCounter` (or `BankAccount`) and rewrite it using `ReentrantLock`, making sure every `lock()` is paired with a `finally { unlock(); }`.

**Expected outcome:** identical behavior. Now add a method that uses `tryLock(1, TimeUnit.SECONDS)` and verify it never blocks forever even if another thread holds the lock.

#### Exercise 3 — Thread-Safe Stack (Medium)

Implement a thread-safe `Stack<E>` (push/pop/peek/empty) where `pop` returns `Optional.empty()` when empty instead of throwing.
- Version 1: `synchronized` methods.
- Version 2: a single `ReentrantLock`.
- Version 3: a `ReentrantReadWriteLock` (reads: peek/empty; writes: push/pop).

**Expected outcome:** run a producer/consumer stress test with several threads pushing and popping; verify no `NoSuchElementException`, no lost elements, and that counts reconcile at the end.

#### Exercise 4 — Deadlock-Fixing (Medium–Hard)

Write the two-thread two-resource deadlock program from Section 7.2 and confirm it hangs. Then:
- Fix it with lock ordering (assign each resource a unique int; always lock ascending).
- Fix it with `tryLock` timeouts and a retry loop.
- Measure that neither version ever hangs, even over 10,000 iterations with random sleep delays.

**Expected outcome:** your fixed versions never deadlock; the retry version may occasionally report "giving up, retrying" but always completes.

#### Challenge — The Dining Philosophers (Hard)

Model five philosophers sitting in a circle; each needs two chopsticks to eat, and each chopstick is shared with a neighbor. Implement it so that:
- Every philosopher eats a few times (loop), then the program terminates cleanly.
- No philosopher ever starves and the program **never deadlocks**.
- Try (and observe) the naive version that deadlocks, then fix it with lock ordering (always pick the lower-numbered chopstick first) *or* with `tryLock`-based backoff.

**Expected outcome:** the naive version eventually hangs; your fixed version always terminates. Add a thread-dump step (`jstack`) to see the cycle in the naive version.

#### Further Reading

- **Oracle's official tutorial, "Lesson: Concurrency"** (part of the *Java Tutorials*) — the definitive gentle introduction; read the sections on synchronization, atomic access, and deadlock.
- **Brian Goetz et al., *Java Concurrency in Practice*** — the industry bible. Chapter 2 (Thread Safety), Chapter 4 (Composing Objects), Chapter 10 (Avoiding Liveness Hazards), and Chapter 13 (Explicit Locks) map directly to this chapter.
- **Joshua Bloch, *Effective Java*** — items on minimizing the scope of `synchronized` and preferring executors/tasks to raw threads.
- **The Java Language Specification, Chapter 17** (*Threads and Locks*) — the authoritative description of the **Java Memory Model**, `happens-before`, and monitor semantics.
- **`java.util.concurrent` API documentation** — package summary, `Lock`, `ReentrantLock`, `ReentrantReadWriteLock`, `Condition`, `AtomicInteger`, and `ConcurrentHashMap`.
- **JVM tooling docs** — `jstack`, `jcmd`, `jvisualvm` for diagnosing real deadlocks and lock contention in running applications.

---

**You now have everything you need to write correct synchronized Java.** The mental model to keep forever: *shared mutable state is dangerous; every access to it must be serialized by a well-chosen lock; nested locks must follow a fixed order; and explicit locks must always be released in `finally`.* Master those four rules and you can build concurrent systems that are correct, understandable, and deadlock-free.