# Advanced Concurrency in Java: Explicit Locks, Synchronizers, and Asynchronous Composition

## 1. Title

**Advanced Concurrency in Java: Explicit Locks, Synchronizers, and Asynchronous Composition**

## 2. Learning Objectives

- **Implement** mutual exclusion with `ReentrantLock` and reason precisely about its guarantees versus `synchronized`.
- **Orchestrate** one-shot coordination between threads using `CountDownLatch`, including correct failure-path handling.
- **Bound** concurrent access to scarce resources using `Semaphore`, and avoid permit-leak and over-release bugs.
- **Synchronize** multi-stage parallel computation across repeated rounds with `CyclicBarrier`, including `BrokenBarrierException` recovery.
- **Compose** asynchronous, non-blocking pipelines with `CompletableFuture` and control exception flow across stages.
- **Diagnose** classic failure modes—forgotten unlocks, broken barriers, inflated permits, and blocking calls that defeat asynchrony—and apply the correct fix.
- **Select** the right synchronization tool for a scenario using a decision procedure, and justify the choice.

## 3. Introduction

### Why this topic matters

Multithreading unlocks parallelism, but threads that share memory and resources introduce two families of problems. First, **race conditions**: two threads reading and writing the same variable concurrently produce nondeterministic results. Second, **coordination problems**: threads must often wait for each other, respect resource limits, or synchronize at agreed-upon stages—and the cost of getting this wrong is deadlock, starvation, or corrupted data in production systems. The `java.util.concurrent` package (introduced in **Java 5**, extended substantially in **Java 8** and **Java 9**) exists precisely so that you express these coordination contracts with battle-tested, high-performance primitives instead of hand-rolled loops over `wait`/`notify`.

### Big-picture mental model

Think of every concurrency tool as an **encoding of a coordination contract**: *who* waits, *for what*, and *for how long*. `synchronized` gives you the rawest form of mutual exclusion; the tools in this chapter let you express richer contracts—reentrant ownership, countdown gates, permit pools, reusable rendezvous, and composable asynchronous pipelines. If you know *which contract* your scenario needs, the correct class name falls out almost mechanically.

### Real-world analogy

Imagine a busy **restaurant kitchen**. Chefs (threads) share a finite set of ovens (scarce resources), must coordinate multi-course meals where course two cannot start until every table's course one is plated (stage synchronization), and the head chef needs to know when the entire service is finished before closing (a one-shot "all done" signal). The lock is the single knife at a prep station: only one chef may use it at a time, and the chef who picks it up is responsible for putting it back. Now scale that kitchen to a web server, a database pool, or a microservice orchestrator—every tool in this chapter is one of those kitchen rules made explicit and safe.

### Prerequisites

- Thread creation and the `Runnable` interface; `Thread.start()` and `join()`.
- `synchronized` blocks/methods and the `volatile` keyword; the concept of a monitor.
- `wait()` / `notify()` / `notifyAll()` semantics (even if you have never used them in anger).
- `ExecutorService`, `Executors.newFixedThreadPool`, and `submit`.
- Java 8 lambdas and method references (required by `CompletableFuture`).

---

## 4. The Five Mechanisms

---

## 4.A ReentrantLock

### 4.1 Core Concept

**Definition.** `ReentrantLock` is an explicit mutual-exclusion lock with a richer API than `synchronized`: it supports timed and interruptible acquisition, fairness policies, and multiple *condition* queues. "Reentrant" means the *same thread* that currently holds the lock may acquire it again (nested) without deadlocking against itself—it must simply release it once for every acquisition.

**Fresh analogy.** Picture a **university library study-carrel key**. Only one student may hold the key at a time. If the same student locks the carrel, steps inside, then needs to lock an inner supply closet that uses the same key, they may do so—they already hold it (reentrancy). But every time they lock something, they must unlock it when done; if they walk off with the key still in the door, every other student waits forever (leaked lock). The key does *not* remember who held it last Tuesday—only *who currently holds it*.

**What it is *and* what it is *not*.**
- It **is** a replacement for `synchronized` when you need features `synchronized` cannot offer: non-blocking `tryLock()`, time-bounded acquisition, interruptible acquisition, fairness, and named conditions.
- It is **not** an automatic lock. Unlike `synchronized`, which releases the monitor on scope exit (even on exceptions), `ReentrantLock` releases **only** when you call `unlock()`. Forgetting this is the single most common production bug.
- It is **not** a higher-throughput magic bullet. For simple, low-contention critical sections, `synchronized` (a biased lock) is usually faster and always simpler.

**Comparison to its simpler counterpart.** With `synchronized` you get an implicit, non-fair monitor tied to the object identity, with exactly one wait queue (`wait`/`notify`). With `ReentrantLock` you get an explicit object you can pass around, `tryLock()` for non-blocking probes, a fairness knob, and *multiple* `Condition` queues (e.g., "not empty" and "not full") attached to one lock—something `synchronized` cannot express without hacks.

### 4.2 Java Syntax & API Walkthrough

| Method / Constructor | Signature | Purpose | Notes / Traps |
|---|---|---|---|
| Constructor | `ReentrantLock()` | Non-fair (barging) lock | Default. New threads can cut in front of waiters → higher throughput, possible starvation. |
| Constructor | `ReentrantLock(boolean fair)` | Fair lock (`fair = true` gives longest-waiter-first) | Predictable ordering, lower throughput (~10–30× worse under contention). |
| `lock()` | `void lock()` | Acquire; block if held | Does **not** respond to interrupts. Always pair with `try { … } finally { unlock(); }`. |
| `lockInterruptibly()` | `void lockInterruptibly()` | Acquire, or throw `InterruptedException` if interrupted while waiting | Caller must handle the checked exception; enables responsive cancellation. |
| `tryLock()` | `boolean tryLock()` | Acquire only if immediately available | Non-blocking probe; no checked exceptions. |
| `tryLock(long, TimeUnit)` | `boolean tryLock(long t, TimeUnit u)` | Acquire within a timeout | Throws `InterruptedException`. The standard tool for deadlock avoidance. |
| `unlock()` | `void unlock()` | Release one hold | Must balance every `lock()`/`tryLock()` success. Throws `IllegalMonitorStateException` if called by a non-owner. |
| `getHoldCount()` | `int getHoldCount()` | Reentrancy depth held by current thread | 0 if the current thread does not hold the lock. Useful for debugging. |
| `newCondition()` | `Condition newCondition()` | Create a condition queue bound to this lock | Analogous to `wait`/`notify`, but you can have several per lock. |

**Example 1 — a thread-safe counter (the canonical `finally`-unlock pattern):**

```java
import java.util.concurrent.locks.ReentrantLock;

public final class ThreadSafeCounter {

    // Fair lock: under contention, the longest-waiting thread goes first.
    private final ReentrantLock lock = new ReentrantLock(true);
    private long count = 0;

    public long increment() {
        lock.lock();                      // acquire
        try {
            return ++count;               // critical section
        } finally {
            lock.unlock();                // ALWAYS release, even on exceptions
        }
    }

    public long get() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
```

**Example 2 — composition with `Condition`: a bounded producer–consumer buffer.** A single `ReentrantLock` owns *two* condition queues, something `synchronized` cannot express with a single monitor:

```java
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class BoundedBuffer<T> {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();  // "at least one item"
    private final Condition notFull  = lock.newCondition();  // "at least one free slot"
    private final Deque<T> items = new ArrayDeque<>();
    private final int capacity;

    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
    }

    public void put(T item) throws InterruptedException {
        lock.lockInterruptibly();                 // respond to cancellation
        try {
            while (items.size() == capacity) {    // always re-test after waking
                notFull.await();                  // sleep until a consumer signals
            }
            items.addLast(item);
            notEmpty.signal();                    // wake one waiting consumer
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (items.isEmpty()) {
                notEmpty.await();
            }
            T item = items.removeFirst();
            notFull.signal();                     // wake one waiting producer
            return item;
        } finally {
            lock.unlock();
        }
    }
}
```

Notice the `while` loops, not `if`: after `await()` wakes, the predicate may have changed again (spurious wakeup or another thread consumed the slot). This is the same rule that governs `Object.wait()`.

### 4.3 Common Pitfalls & Best Practices

- **Forgetting `unlock()` outside a `finally` block.** *Symptom:* the program hangs; other threads block forever on `lock()`. *Why:* unlike `synchronized`, nothing releases the lock automatically, and an exception thrown mid-critical-section skips the unlock. *Fix:*
  ```java
  lock.lock();
  try {
      doWork();          // may throw
  } finally {
      lock.unlock();     // guaranteed
  }
  ```
- **Ignoring the `boolean` returned by `tryLock()`.** *Symptom:* a critical section is silently skipped and state is left inconsistent. *Why:* `tryLock()` returns immediately with `false` if the lock is busy; treating it as success skips work. *Fix:* branch on the result, or loop with a timeout:
  ```java
  while (!lock.tryLock(1, TimeUnit.SECONDS)) {
      // log contention, maybe back off, then retry
  }
  try { /* critical section */ } finally { lock.unlock(); }
  ```
- **Calling `unlock()` from a thread that does not own the lock.** *Symptom:* `IllegalMonitorStateException`. *Why:* `ReentrantLock` tracks ownership; handing the lock to another thread and letting it release corrupts the owner bookkeeping. *Fix:* the thread that acquires must release; never pass a lock to a helper thread and ask it to unlock.
- **Assuming `lock()` is interruptible.** *Symptom:* threads cannot be cancelled while waiting to acquire, and shutdown takes forever. *Why:* `lock()` ignores interrupts by design. *Fix:* use `lockInterruptibly()` in cancellation-sensitive code and restore the interrupt flag in `catch (InterruptedException e) { Thread.currentThread().interrupt(); }`.
- **Choosing non-fair when bounded waiting matters.** *Symptom:* under heavy contention, some threads starve indefinitely (barging threads keep cutting in line). *Fix:* `new ReentrantLock(true)` if fairness or a predictable queue matters more than raw throughput.
- **Best practices with justification:** always pair acquisition with `try/finally`; prefer `tryLock(timeout)` for multi-lock scenarios to avoid classic lock-ordering deadlocks; document whether a lock is fair; keep critical sections short (a lock held across network I/O serializes your entire system).

### 4.4 Real-World Use Cases

- **High-contention hot counters / cache statistics in a web server.** The lock's reentrancy lets one request path call `increment()` nested inside a larger locked operation safely. Using `synchronized` would work, but you lose the option of `tryLock`-based contention handling under load.
- **Building a custom concurrent data structure** (a read-mostly cache, a priority queue with two conditions). `Condition` queues let you have separate "readers wait" and "writers wait" lines; with `synchronized`, you would need one `notifyAll` and re-checking, which wakes far more threads than necessary.
- **Deadlock avoidance in a money-transfer service** that must lock two accounts. `lock.tryLock()` with a timeout lets the service back off and retry instead of blocking forever on a lock-ordering bug. Using `synchronized` here gives you **no** escape hatch.
- **What happens with the wrong tool:** if you use `synchronized` everywhere and later need fairness or timed acquisition, you must rewrite the code; if you use a `Semaphore(1)` as a "lock," you lose ownership guarantees (see §4.C) and introduce subtle bugs.

---

## 4.B CountDownLatch

### 4.1 Core Concept

**Definition.** `CountDownLatch` is a **one-shot synchronization gate**: it holds an integer count; threads block in `await()` until the count is decremented to zero by `countDown()` calls from other threads. Once zero, the latch is open forever and all waiting (and future) `await()` calls return immediately.

**Fresh analogy.** A **rocket launch countdown**. The ground crew and engineers each call in "ready" (that's one `countDown()` each). The launch controller blocks in `await()` until *every* station has reported—count reaches zero—and then the launch happens. There is exactly **one** launch: you cannot rewind the countdown and reuse it for a second rocket. The people who report ready are *not* the same people who wait; the latch separates the "producers of readiness" from the "consumers of readiness."

**What it is *and* what it is *not*.**
- It **is** a way to make one or more threads wait until *N other actions* have completed.
- It is **not** a barrier: the threads calling `countDown()` do **not** wait for each other, and there is no "all parties present" rendezvous.
- It is **not** reusable. After the count hits zero, the latch is exhausted; there is no `reset()`.
- The count is decremented by any thread; the waiters do not decrement it.

**Comparison to its simpler counterpart.** The raw `synchronized` world has no direct equivalent—you would hand-roll a `volatile int` + `wait`/`notifyAll` loop with all its pitfalls (missed notifications, lost wakeups). `CountDownLatch` encapsulates the classic "rendezvous-on-count" logic correctly and efficiently.

### 4.2 Java Syntax & API Walkthrough

| Method / Constructor | Signature | Purpose | Notes / Traps |
|---|---|---|---|
| Constructor | `CountDownLatch(int count)` | Create a latch that opens when `count` reaches 0 | `count < 0` throws `IllegalArgumentException`; `count = 0` means "already open." |
| `await()` | `void await()` | Block until count is 0 | Throws `InterruptedException`. Can block forever if the count never reaches 0. |
| `await(long, TimeUnit)` | `boolean await(long t, TimeUnit u)` | Wait up to `t`; returns `false` on timeout | The production-safe choice; never risk an indefinite block. |
| `countDown()` | `void countDown()` | Decrement count by 1 | Thread-safe; calling below zero is a no-op (which silently hides miscounting bugs). |
| `getCount()` | `long getCount()` | Remaining count | Diagnostic only—the value races and can change between calls. |

**Example 1 — service startup gate: the main thread waits until every service reports ready.**

```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ServiceStartup {

    public static void main(String[] args) throws InterruptedException {
        int serviceCount = 3;
        CountDownLatch ready = new CountDownLatch(serviceCount);   // opens after 3 countDowns
        ExecutorService pool = Executors.newFixedThreadPool(serviceCount);

        for (int i = 1; i <= serviceCount; i++) {
            final int id = i;
            pool.submit(() -> {
                try {
                    initializeService(id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Service " + id + " failed to start");
                } finally {
                    ready.countDown();          // MUST run on every path, success or failure
                }
            });
        }

        boolean allReady = ready.await(10, TimeUnit.SECONDS);   // returns false on timeout
        System.out.println(allReady
                ? "All services up — accepting traffic"
                : "TIMEOUT: not all services became ready");
        pool.shutdownNow();
    }

    private static void initializeService(int id) throws InterruptedException {
        Thread.sleep((long) (Math.random() * 1000));   // simulated startup work
        System.out.println("Service " + id + " ready");
    }
}
```

**Example 2 — composition: a start gate + a done latch (the "release all at once, wait for all" pattern).**

```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ParallelRelease {

    private static final int WORKERS = 4;

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);     // holds everyone until released
        CountDownLatch doneGate = new CountDownLatch(WORKERS); // opened when all finish

        ExecutorService pool = Executors.newFixedThreadPool(WORKERS);
        for (int i = 0; i < WORKERS; i++) {
            final int id = i;
            pool.submit(() -> {
                try {
                    startGate.await();          // workers wait, they do NOT count down this latch
                    process(id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneGate.countDown();       // one countDown per worker, exactly once
                }
            });
        }

        Thread.sleep(100);      // any setup the test wants before the simultaneous start
        startGate.countDown();  // release all workers at (nearly) the same instant
        doneGate.await();       // main waits for the last worker to finish
        System.out.println("All workers finished");
        pool.shutdown();
    }

    private static void process(int id) {
        System.out.println("Worker " + id + " running");
    }
}
```

Note how the *same* thread both awaits and counts down in Example 2, but for *different* latches—a common cause of deadlock if you get the roles confused.

### 4.3 Common Pitfalls & Best Practices

- **Assuming `CountDownLatch` is reusable.** *Symptom:* the second time your code uses the same latch, it opens immediately. *Why:* once the count hits zero the latch is permanently open; there is no `reset()`. *Fix:* create a fresh latch per phase (or use a `CyclicBarrier` if you need repeating rounds).
- **`countDown()` missing on the failure path.** *Symptom:* `await()` blocks forever; the program hangs. *Why:* an exception skips the `countDown()` and the count never reaches zero. *Fix:* put `countDown()` in a `finally` block, as in Example 1.
- **Miscounting the initial count.** *Symptom:* the latch opens too early (count too low) or never (count too high). *Why:* `countDown()` below zero is a silent no-op, so an over-large count hangs indefinitely without any error. *Fix:* derive `count` from the actual number of tasks you will `submit` (e.g., `taskList.size()`), and add an `await(timeout)` so a bug is loud rather than a hang.
- **Confusing it with `CyclicBarrier`.** *Symptom:* you try to use a latch to make N threads wait for *each other*, and instead they just run—because `countDown()` does not block anyone. *Fix:* for a rendezvous where all participants must arrive, choose `CyclicBarrier`; for a one-shot "wait until N events happened," choose `CountDownLatch`.
- **Awaiting in a thread that must also count down.** *Symptom:* deadlock—a worker does `latch.await()` before its own `countDown()`, so the count can never reach zero. *Fix:* count down first (typically in a `finally`), then await the *other* latch.
- **Best practices with justification:** always use the timed `await(long, TimeUnit)` in production code so misconfiguration surfaces as a timeout instead of a hang; count down exactly once per responsible thread; name latches by role (`startGate`, `doneGate`, `servicesReady`) so the wait/count-down roles stay obvious.

### 4.4 Real-World Use Cases

- **Distributed microservice bootstrap:** the gateway waits on a `CountDownLatch` until all backend services report ready via health checks; only then does it advertise itself as healthy. If you used a `CyclicBarrier`, one *slow* (but not failed) service would break the entire boot cycle.
- **Parallel test harness:** a `startGate` releases N test workers simultaneously to reproduce a race, and a `doneGate` lets the harness wait for all before asserting results.
- **"Wait for all N background flushes"** before a server's graceful shutdown completes; each worker `countDown()`s after its final flush.
- **What happens with the wrong tool:** using a `Semaphore(0)` with `release()` to signal readiness is possible but invites over-release bugs and lacks a clean "open forever" semantic; using `CyclicBarrier` for a one-shot start gate requires a throwaway barrier that you must `reset()` and handle `BrokenBarrierException` from—all for zero benefit.

---

## 4.C Semaphore

### 4.1 Core Concept

**Definition.** `Semaphore` maintains a fixed number of **permits**. Threads *acquire* permits before entering a resource-limited section and *release* them when done. If no permit is available, `acquire()` blocks. The semantics are like a counter of "free slots" that producers and consumers hand around; crucially, **permits are not owned**—any thread may release one, regardless of who acquired it.

**Fresh analogy.** A **parking garage with 40 spots**. Each entering car takes one spot (acquire); each leaving car frees one spot (release). The garage never cares *which* car left: any driver can free any spot, and there is no identity attached to a permit. When the lot is full, incoming cars wait at the gate (blocking `acquire()`), and some drivers use the "wait at most 10 minutes" option (`tryAcquire(timeout)`) before driving away. If a dishonest driver *releases without having parked*, the garage thinks it has one more free spot than it truly does—over-release.

**What it is *and* what it is *not*.**
- It **is** a limiter: "at most N threads may be inside this section simultaneously."
- It is **not** a mutex/lock. A binary semaphore (1 permit) does *not* give you ownership; thread B can legally release a permit acquired by thread A, breaking mutual-exclusion reasoning. Use `ReentrantLock`/`synchronized` when ownership matters.
- It is **not** a queue or a message buffer by itself (though two semaphores can implement one, as shown below).

**Comparison to its simpler counterpart.** `synchronized` allows exactly one thread inside a critical section—it cannot express "up to five concurrent database connections." A `Semaphore(N)` is the generalization: mutual exclusion is just the special case `Semaphore(1)` (with the ownership caveat above).

### 4.2 Java Syntax & API Walkthrough

| Method / Constructor | Signature | Purpose | Notes / Traps |
|---|---|---|---|
| Constructor | `Semaphore(int permits)` | Non-fair semaphore with N permits | Non-fair: newly arriving threads can jump the queue. |
| Constructor | `Semaphore(int permits, boolean fair)` | Fair semaphore | FIFO permit grant; better latency predictability, lower throughput. |
| `acquire()` | `void acquire()` | Take one permit; block if none available | Throws `InterruptedException`; must be balanced by a `release()`. |
| `acquire(int n)` | `void acquire(int n)` | Take n permits atomically | If `n` exceeds available permits, the thread blocks—a common accidental deadlock. |
| `tryAcquire()` | `boolean tryAcquire()` | Take a permit if one is immediately available | Non-blocking; no checked exceptions. |
| `tryAcquire(long, TimeUnit)` | `boolean tryAcquire(long t, TimeUnit u)` | Wait up to `t` for a permit | Throws `InterruptedException`; the standard rate-limiting call. |
| `release()` | `boolean release()` | Return one permit | **No ownership check** — over-release silently inflates the permit count. |
| `release(int n)` | `void release(int n)` | Return n permits | With over-release, the pool can grow beyond its intended maximum. |
| `availablePermits()` | `int availablePermits()` | Current permit count | Racy—only useful for diagnostics/monitoring. |
| `drainPermits()` | `int drainPermits()` | Take all currently available permits | Handy for a graceful-shutdown signal. |

**Example 1 — a rate-limited downloader (time-bounded admission):**

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public final class RateLimitedDownloader {

    private final Semaphore downloadSlots = new Semaphore(3);   // at most 3 concurrent downloads

    public static void main(String[] args) throws InterruptedException {
        RateLimitedDownloader downloader = new RateLimitedDownloader();
        ExecutorService pool = Executors.newFixedThreadPool(10);
        for (int i = 1; i <= 20; i++) {
            final String url = "https://example.com/files/" + i;
            pool.submit(() -> downloader.download(url));
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);
    }

    public void download(String url) {
        boolean acquired = false;
        try {
            acquired = downloadSlots.tryAcquire(5, TimeUnit.SECONDS);
            if (!acquired) {
                System.out.println("Skipping " + url + ": no slot for 5s");
                return;
            }
            System.out.println("Downloading " + url + " ...");
            Thread.sleep(200);                         // simulated transfer
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();        // restore the flag
        } finally {
            if (acquired) {
                downloadSlots.release();               // always give the slot back
            }
        }
    }
}
```

The `acquired` flag guards the `finally` release so we never release a permit we never took.

**Example 2 — composition: a bounded queue built from *two* semaphores** (`emptySlots` + `filledSlots`), showing that semaphores can also encode "number of items in stock":

```java
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Semaphore;

public final class SemaphoreQueue<T> {

    private final Deque<T> queue = new ArrayDeque<>();
    private final Semaphore emptySlots;   // free capacity (starts full)
    private final Semaphore filledSlots;  // stored items     (starts empty)

    public SemaphoreQueue(int capacity) {
        emptySlots = new Semaphore(capacity);
        filledSlots = new Semaphore(0);
    }

    public void put(T item) throws InterruptedException {
        emptySlots.acquire();             // claim one free slot (block if full)
        synchronized (queue) {
            queue.addLast(item);
        }
        filledSlots.release();            // make one item visible to consumers
    }

    public T take() throws InterruptedException {
        filledSlots.acquire();            // wait until an item exists
        T item;
        synchronized (queue) {
            item = queue.removeFirst();
        }
        emptySlots.release();             // free one slot
        return item;
    }
}
```

### 4.3 Common Pitfalls & Best Practices

- **Over-release.** *Symptom:* more than N threads enter the critical section; the "limit" is silently broken. *Why:* `release()` never checks who acquired—calling it twice for one acquire (or from the wrong branch) inflates the count. *Fix:* track acquisition state explicitly and release exactly once, in a `finally`, as in Example 1's `acquired` flag.
- **Permit leak (forgotten `release()`).** *Symptom:* threads gradually block forever; the program's throughput decays to zero. *Why:* an exception path exits without `release()`. *Fix:* `try { … } finally { release(); }` everywhere a permit is taken.
- **Using a binary semaphore as a lock.** *Symptom:* "impossible" state corruption: thread B releases the permit thread A holds, then thread A also releases, giving the pool *two* permits. *Why:* no ownership. *Fix:* for mutual exclusion with ownership semantics, use `ReentrantLock`.
- **`acquire(n)` when the full batch may never be available.** *Symptom:* deadlock—a thread waits for 3 permits while other threads hold 2 each. *Why:* `acquire(3)` blocks until *all* 3 are free, and the holders may be waiting on the very thread that is blocked. *Fix:* acquire one at a time or restructure so threads never multi-acquire; if multi-acquire is required, guarantee the batch size is feasible.
- **Blocking `acquire()` inside an executor's worker thread without a timeout.** *Symptom:* the pool's threads are all parked in `acquire()`, the queue backs up, and cancellation is impossible. *Fix:* prefer `tryAcquire(long, TimeUnit)` and decide what "gave up" means for your workload.
- **Best practices with justification:** release in `finally`; guard release with an `acquired` flag; prefer fair semaphores when permit starvation is unacceptable (e.g., fairness for DB slots across tenants); expose `availablePermits()` only to monitoring, never to control flow.

### 4.4 Real-World Use Cases

- **Database connection pooling:** a pool wraps real connections with a `Semaphore(N)` where `N` = connection count. This is the *classic* use—it bounds resource usage without tying a thread to a specific connection. If you instead used a `ReentrantLock`, you could never allow more than one query at a time.
- **Rate limiting an upstream API** (e.g., 10 calls/second to a third-party service): a `tryAcquire(timeout)` gate keeps your application under the vendor's quota. Using a `CountDownLatch` here is nonsensical (it's one-shot); using a lock serializes everything.
- **Admission control for a thread pool that must never queue unboundedly:** acquire a permit before submitting each task so the system sheds load gracefully instead of OOM-ing.
- **What happens with the wrong tool:** using `CyclicBarrier` to limit concurrency fails because a barrier *blocks all parties until everyone arrives*—it has no notion of "at most N at once." Using a `ReentrantLock` limits you to exactly 1 concurrent user instead of N.

---

## 4.D CyclicBarrier

### 4.1 Core Concept

**Definition.** `CyclicBarrier` is a **reusable rendezvous point** for a fixed number of *parties*. Each thread calls `await()`, blocks, and when the last party arrives, all are released simultaneously (and an optional `barrierAction` runs first, in the last-arriving thread). Because all threads are released together, the barrier is safe to reuse for the next round—"cyclic."

**Fresh analogy.** A **team of hikers on a trail with named landmarks**. The group agrees: we will not leave landmark A until *every* hiker has arrived (that's one barrier trip). When the last hiker reaches the landmark, everyone regroups, the team leader notes progress (`barrierAction`), and they all continue to landmark B where they repeat the process. If one hiker gets lost or gives up, the others cannot proceed meaningfully—the arrangement is *broken* and must be renegotiated (`reset()`). Hikers do *not* leave early and wait at the next landmark; the whole point is the group moves stage by stage together.

**What it is *and* what it is *not*.**
- It **is** the tool for "all N threads must arrive at this point before *any* proceeds to the next phase," repeated across many phases.
- It is **not** a counter-based gate like `CountDownLatch`: here the participants wait *for each other*, not for external events, and the barrier resets automatically each trip.
- It is **not** a concurrency limiter: it does not say "at most N in the section"; it says "exactly N must show up."
- Once broken (a party times out, is interrupted, or `reset()` is called), all in-flight and future `await()` calls throw `BrokenBarrierException` until a successful trip or an explicit `reset()`.

**Comparison to its simpler counterpart.** The closest relative is `CountDownLatch` (see the contrast table in §6). Where a latch is "wait until a count reaches zero, once," a barrier is "all parties present, then release together, *repeatedly*." There is no `synchronized`-world equivalent; you would hand-roll a counter plus `wait`/`notifyAll` plus a "generation" concept and get it subtly wrong.

### 4.2 Java Syntax & API Walkthrough

| Method / Constructor | Signature | Purpose | Notes / Traps |
|---|---|---|---|
| Constructor | `CyclicBarrier(int parties)` | Barrier for N parties | The barrier trips only when exactly N threads call `await()`. |
| Constructor | `CyclicBarrier(int parties, Runnable action)` | Run `action` when the barrier trips | The action runs in the **last-arriving thread**, before any waiter is released—ideal for aggregation. |
| `await()` | `int await()` | Arrive and block until all parties arrive | Returns the arrival index (0 = last). Throws `BrokenBarrierException`. |
| `await(long, TimeUnit)` | `int await(long t, TimeUnit u)` | Arrive with a timeout | On timeout, the barrier **breaks for everyone** (`TimeoutException` + `BrokenBarrierException` for others). |
| `reset()` | `void reset()` | Restore the barrier to a fresh state | Any threads currently awaiting throw `BrokenBarrierException`. |
| `isBroken()` | `boolean isBroken()` | True if the barrier has been broken | Check it before deciding whether to retry or abort. |
| `getParties()` | `int getParties()` | The configured party count | Fixed at construction. |
| `getNumberWaiting()` | `int getNumberWaiting()` | Parties currently blocked in `await()` | Diagnostic only. |

**Example 1 — iterative parallel computation with per-round aggregation.** Workers write to their own slot, rendezvous, and the `barrierAction` (which runs with full visibility of all slots, thanks to the barrier's happens-before edges) aggregates the round:

```java
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public final class IterativeWorkers {

    private static final int WORKERS = 4;
    private static final int ROUNDS = 3;

    public static void main(String[] args) throws InterruptedException {
        final int[] perWorker = new int[WORKERS];         // slot per worker, written before await
        final AtomicInteger grandTotal = new AtomicInteger();

        CyclicBarrier barrier = new CyclicBarrier(WORKERS, () -> {
            // barrierAction: runs in the last-arriving thread, once all slots are written.
            int roundTotal = 0;
            for (int v : perWorker) {
                roundTotal += v;
            }
            grandTotal.addAndGet(roundTotal);
            System.out.println("Round aggregate = " + roundTotal
                    + " (cumulative " + grandTotal.get() + ")");
        });

        Thread[] threads = new Thread[WORKERS];
        for (int w = 0; w < WORKERS; w++) {
            final int workerId = w;
            threads[w] = new Thread(() -> {
                for (int round = 0; round < ROUNDS; round++) {
                    perWorker[workerId] = (workerId + 1) * (round + 1); // simulated work
                    try {
                        barrier.await();   // wait until all 4 workers have written their slot
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (BrokenBarrierException e) {
                        System.out.println("Worker " + workerId + " saw a broken barrier");
                        return;
                    }
                }
            });
            threads[w].start();
        }

        for (Thread t : threads) {
            t.join();
        }
        System.out.println("Final total = " + grandTotal.get());
    }
}
```

**Example 2 — timeout, breakage, and `reset()` (failure recovery):**

```java
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class BarrierRecovery {

    public static void main(String[] args) throws InterruptedException {
        CyclicBarrier barrier = new CyclicBarrier(3);

        // Only 2 of 3 parties ever arrive: the barrier can never trip.
        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                try {
                    barrier.await(1, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    System.out.println("Timed out. Barrier broken = " + barrier.isBroken());
                } catch (BrokenBarrierException | InterruptedException e) {
                    System.out.println("Observed broken barrier");
                }
            }).start();
        }

        Thread.sleep(2000);            // let both parties time out
        if (barrier.isBroken()) {
            System.out.println("Resetting for the next cycle...");
            barrier.reset();           // make the barrier usable again
        }
        System.out.println("Parties = " + barrier.getParties() + ", broken = " + barrier.isBroken());
    }
}
```

### 4.3 Common Pitfalls & Best Practices

- **A missing party hangs everyone.** *Symptom:* all threads block forever inside `await()`. *Why:* the barrier trips only when *all* N parties arrive; a thread that crashes or forgets to await means the trip never happens. *Fix:* use `await(long, TimeUnit)` (so a missing party surfaces as a `TimeoutException`) and pair it with a failure-flag or watchdog that calls `reset()`.
- **Ignoring `BrokenBarrierException`.** *Symptom:* one thread fails, and *all other* threads start throwing bizarre exceptions or silently abort. *Why:* when any party times out, is interrupted, or `reset()` is called, every other blocked `await()` throws `BrokenBarrierException`; unhandled, this cascades. *Fix:* catch `BrokenBarrierException`, check `isBroken()`, and implement an explicit retry/abort policy:
  ```java
  try {
      barrier.await(timeout, TimeUnit.SECONDS);
  } catch (BrokenBarrierException e) {
      if (barrier.isBroken()) barrier.reset();   // recovery before retry
  }
  ```
- **Party-count mismatch.** *Symptom:* an extra thread hangs forever, or the barrier trips "early." *Why:* if 6 threads await a 5-party barrier, 5 trip together and the 6th waits for a trip that never comes. *Fix:* make `parties` exactly equal to the number of coordinating threads, and never `submit` more/fewer tasks than the configured count.
- **A throwing `barrierAction` breaks the barrier.** *Symptom:* after one successful-looking round, every subsequent `await()` throws. *Why:* an exception in the barrier action marks the barrier broken (the action runs inside the barrier's release protocol). *Fix:* wrap the barrier action body in `try/catch` and handle failures explicitly.
- **Choosing `CountDownLatch` for repeated rounds.** *Symptom:* you recreate a latch every round, losing the synchronization "wave" and letting fast workers race ahead. *Fix:* use `CyclicBarrier` when you need the *same* set of threads to repeatedly sync across rounds.
- **Best practices with justification:** use a `barrierAction` for aggregation (it gives you a safe, serialized place to merge results); always use the timed `await`; design an explicit broken-barrier policy (retry-with-reset vs. abort) because breakage is a *normal* outcome, not an edge case.

### 4.4 Real-World Use Cases

- **Parallel numerical algorithms** (matrix relaxation, solving large systems by iteration): each iteration is a barrier round—all threads compute their slice, the barrier trip lets results propagate, and the next iteration reads the updated values. Using `CountDownLatch` would require a *new* latch per iteration and forces a "release everyone, then re-gather" pattern that the barrier gives you for free.
- **Multi-phase batch jobs**: load phase, transform phase, persist phase, with a barrier between phases so a consistent snapshot of a batch is visible to all workers before the next phase touches it.
- **Load testing with synchronized ramps**: every simulated user must finish action K before action K+1 begins, so the test measures steady-state, not stragglers. A `Semaphore` would instead let fast users run ahead, corrupting the measurement.
- **What happens with the wrong tool:** using `CountDownLatch` for a recurring phase-sync means rebuilding and re-plumbing the gate every round and losing the "broken barrier" detection that tells you a worker died. Using a `Semaphore` as a rendezvous is impossible—it has no "wait for everyone" semantics at all.

---

## 4.E CompletableFuture

### 4.1 Core Concept

**Definition.** `CompletableFuture` (introduced in **Java 8**, JEP 110) is a `Future` that you can **compose**: instead of blocking on `.get()`, you attach callbacks that fire when the value arrives, chain transformations, fan work out and recombine it, and handle errors with functional style. Each stage is itself a `CompletableFuture`, so a whole pipeline is a graph of dependent asynchronous operations.

**Fresh analogy.** A **package-delivery relay** from a warehouse to a customer. Stage 1 picks the package up; stage 2 is customs clearance *done by a different courier* (async); stage 3 is the final leg. You (the customer) do **not** stand on your doorstep all day: each courier automatically triggers the next hand-off (`thenApply`/`thenCompose`). If you order three items from three warehouses, you ask the hub to notify you only when *all three* have arrived (`allOf`), or when the *first* arrives (`anyOf`). If a package is lost in transit, the hub's error path kicks in (`exceptionally`). Crucially, standing at the door and *watching* the trucks (calling `join()`) defeats the whole design.

**What it is *and* what it is *not*.**
- It **is** a composable async value and a small dataflow engine. It *is* a `Future` (you can still block with `get()`/`join()`).
- It is **not** a thread pool. It uses pools behind the scenes—by default `ForkJoinPool.commonPool()` (parallelism = CPU count) unless you pass an `Executor`.
- It is **not** inherently non-blocking: *you* choose. Calling `join()` on the main thread blocks; attaching callbacks does not.
- The `*Async` variants are not "more asynchronous magic"—they merely dispatch the stage to a pool so the *completing thread* does not run the stage inline.

**Comparison to its simpler counterpart.** The plain `Future` returned by `ExecutorService.submit()` is a *one-shot handle*: you can `get()` (block) or `cancel()`, but you cannot chain, combine, or react to completion. `CompletableFuture` adds exactly that missing reactivity. Its mental sibling in the standard library world is the `Future` + manual `wait`/`notify` hand-rolling you'd otherwise have to do.

### 4.2 Java Syntax & API Walkthrough

| Method | Signature | Purpose | Notes / Traps |
|---|---|---|---|
| `runAsync` | `static CompletableFuture<Void> runAsync(Runnable r [, Executor e])` | Fire a task returning no value | Defaults to `ForkJoinPool.commonPool()`. |
| `supplyAsync` | `static <U> CompletableFuture<U> supplyAsync(Supplier<U> s [, Executor e])` | Fire a task returning a value | Same pool default. |
| `thenApply` | `<U> CompletableFuture<U> thenApply(Function<T,U> fn)` | Chain a synchronous transform | Runs in the thread that completed the previous stage. |
| `thenApplyAsync` | `<U> CompletableFuture<U> thenApplyAsync(Function<T,U> fn [, Executor e])` | Chain the transform on a pool thread | Keeps the completing thread free. |
| `thenCompose` | `<U> CompletableFuture<U> thenCompose(Function<T, CompletionStage<U>> fn)` | Flatten a nested future (like `flatMap`) | **Use when `fn` returns a future**; `thenApply` would nest them. |
| `thenCombine` | `<U,V> CompletableFuture<V> thenCombine(CompletionStage<U> o, BiFunction<T,U,V> fn)` | Combine two *independent* futures | Fires when both complete. |
| `thenAccept` | `CompletableFuture<Void> thenAccept(Consumer<T> c)` | Consume the result; returns `Void` | The usual terminal step of a pipeline. |
| `exceptionally` | `CompletableFuture<T> exceptionally(Function<Throwable,? extends T> fn)` | Recover from failure with a fallback value | Runs **only** on exceptional completion. |
| `handle` | `<U> CompletableFuture<U> handle(BiFunction<T,Throwable,U> fn)` | Handle success *and* failure | Always runs; you inspect both arguments. |
| `whenComplete` | `CompletableFuture<T> whenComplete(BiConsumer<T,Throwable> a)` | Observe completion without changing the result | Exceptions in the chain still propagate downstream. |
| `allOf` | `static CompletableFuture<Void> allOf(CompletableFuture<?>... cfs)` | Completes when **all** complete | Returns `Void`—you still `join()` each to read values. |
| `anyOf` | `static CompletableFuture<Object> anyOf(CompletableFuture<?>... cfs)` | Completes when **any** completes | Result is `Object`; cast required. |
| `join` | `T join()` | Block and get the result | Throws unchecked `CompletionException`; **blocks the calling thread**. |
| `get` | `T get()` / `T get(long, TimeUnit)` | Block and get the result | Throws checked `InterruptedException`/`ExecutionException`/`TimeoutException`. |
| `complete` / `completeExceptionally` | `boolean complete(T v)` / `boolean completeExceptionally(Throwable t)` | Manually finish the future | Returns `false` if already completed (completion is final). |
| `orTimeout` / `completeOnTimeout` | — | Complete exceptionally / with a default on timeout | **Java 9+** only; not in Java 8. |

**Example 1 — an asynchronous order-processing pipeline (Java 8+):**

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OrderProcessing {

    public static void main(String[] args) {
        ExecutorService ioPool = Executors.newFixedThreadPool(4);   // dedicated I/O threads

        CompletableFuture<String> receipt = CompletableFuture
            .supplyAsync(() -> fetchUser("alice"), ioPool)      // stage 1: blocking I/O → ioPool
            .thenApply(OrderProcessing::validateUser)           // stage 2: CPU, runs in same thread
            .thenApply(OrderProcessing::buildOrder)             // stage 3
            .thenCompose(order -> placeOrder(order, ioPool))    // stage 4: returns a future → flatten
            .thenApply(OrderProcessing::generateReceipt)        // stage 5
            .exceptionally(err -> {                             // stage 6: recovery path
                System.err.println("Order failed: " + err.getMessage());
                return "no-receipt";
            });

        System.out.println("Main thread is free to do other work...");
        System.out.println("Receipt: " + receipt.join());       // block only ONCE, at the end
        ioPool.shutdownNow();
    }

    private static String fetchUser(String user) {
        sleepQuietly(200);
        return user;
    }

    private static String validateUser(String user) {
        if (user == null || user.trim().isEmpty()) {
            throw new IllegalArgumentException("invalid user"); // will flow to exceptionally
        }
        return user;
    }

    private static String buildOrder(String user) {
        return "order-" + user;
    }

    private static CompletableFuture<String> placeOrder(String order, ExecutorService pool) {
        return CompletableFuture.supplyAsync(() -> {
            sleepQuietly(150);                                  // simulated remote call
            return order + "-placed";
        }, pool);
    }

    private static String generateReceipt(String orderId) {
        return "receipt-" + orderId;
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

Key subtleties: `thenCompose` (not `thenApply`) is required at stage 4 because `placeOrder` returns a `CompletableFuture`; an exception in any stage propagates down the chain to `exceptionally`.

**Example 2 — composition: parallel fan-out, aggregation, and error handling (Java 8+):**

```java
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public final class ParallelAggregation {

    private static final ExecutorService pool = Executors.newFixedThreadPool(8);

    public static void main(String[] args) {
        List<String> symbols = Arrays.asList("AAPL", "MSFT", "GOOG", "AMZN");

        // Kick off one independent fetch per symbol, all in parallel.
        List<CompletableFuture<Double>> quotes = symbols.stream()
            .map(s -> CompletableFuture.supplyAsync(() -> fetchQuote(s), pool))
            .collect(Collectors.toList());

        // Complete when EVERY quote has arrived.
        CompletableFuture<Void> allDone =
            CompletableFuture.allOf(quotes.toArray(new CompletableFuture[0]));

        CompletableFuture<Double> total = allDone.thenApply(v -> {
            // Safe: allOf() guarantees every future is complete, so join() returns instantly.
            return quotes.stream()
                .map(CompletableFuture::join)
                .mapToDouble(Double::doubleValue)
                .sum();
        }).exceptionally(err -> {
            System.err.println("Could not compute total: " + err.getMessage());
            return Double.NaN;
        });

        System.out.println("Total value: " + total.join());
        pool.shutdownNow();
    }

    private static Double fetchQuote(String symbol) {
        sleepQuietly((long) (Math.random() * 500));             // simulated network call
        return Math.random() * 1000;
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 4.3 Common Pitfalls & Best Practices

- **Blocking with `get()`/`join()` in the middle of a chain.** *Symptom:* a thread pool is exhausted, latency explodes, and your "asynchronous" code behaves synchronously. *Why:* every `join()` parks a thread that could otherwise run another stage. *Fix:* attach callbacks instead:
  ```java
  // Anti-pattern:
  // double v = future.get();            // blocks THIS thread
  // return v * 2;

  // Correct:
  CompletableFuture<Double> result = future.thenApply(v -> v * 2);
  ```
  A lone terminal `join()` at the very end (main thread, test assertion) is acceptable; mid-pipeline blocking is a design bug.
- **Using `thenApply` with a function that returns a `CompletableFuture`.** *Symptom:* your type becomes `CompletableFuture<CompletableFuture<X>>` and the second future never completes on its own. *Why:* `thenApply` wraps the returned value; it does not flatten. *Fix:* use `thenCompose` (the "flatMap" of completable futures).
- **Forgetting that unhandled exceptions are *silent*.** *Symptom:* a stage fails, downstream callbacks never run, and nothing logs anything—until someone `join()`s and gets a confusing `CompletionException`. *Why:* an exceptional completion only surfaces when a downstream handler or terminal blocking call observes it. *Fix:* terminate every chain with `exceptionally`, `handle`, or `whenComplete`, and log inside them:
  ```java
  CompletableFuture<Void> guarded = future
      .thenApply(OrderProcessing::validateUser)
      .thenAccept(OrderProcessing::save)
      .exceptionally(err -> { log(err); return null; });
  ```
- **Running blocking I/O on `ForkJoinPool.commonPool()`.** *Symptom:* throughput collapses and other `CompletableFuture`s in the same JVM (unrelated code!) starve. *Why:* the common pool has `parallelism` threads equal to the CPU count, and each blocked thread is one fewer executor. *Fix:* pass a dedicated, sized `Executor` to `supplyAsync`/`thenApplyAsync` for blocking work, as in Example 1's `ioPool`.
- **Assuming `*Async` variants are needed everywhere.** *Symptom:* over-spawning threads; cache-thrashing. *Why:* `thenApply` running on the completing thread is usually *exactly right* for cheap CPU transforms. *Fix:* use `*Async` only when the stage blocks, is long, or must run on a specific executor.
- **Ignoring the Java version.** `orTimeout`, `completeOnTimeout`, and `minimalCompletionStage` are **Java 9+**; on Java 8 you must hand-roll timeouts with `get(timeout)` + `completeExceptionally`. Note the JDK your deployment targets before using them.
- **Best practices with justification:** give every chain an explicit terminal handler so failures are loud; keep `join()`/`get()` to the boundary of your system; document the executor per stage; treat `CompletableFuture` as an *async dataflow*, not "Future with nicer syntax."

### 4.4 Real-World Use Cases

- **Microservice orchestration:** an API gateway calls `fetchUserProfile`, `fetchOrders`, and `fetchRecommendations` in parallel with `supplyAsync` + `allOf`, combines them with `thenCombine`, and degrades gracefully per-call with `exceptionally`. Using raw `Future.get()` here would serialize the three upstream calls on the gateway's request threads.
- **Asynchronous REST service (Servlet 3.1+ / reactive stack):** the endpoint returns a `CompletableFuture<Response>` without ever blocking a request thread, letting a small thread pool serve thousands of concurrent requests. Blocking `join()` would defeat the entire non-blocking I/O model.
- **Parallel I/O aggregation for a reporting job:** fan out one HTTP/SQL call per data source, wait for all with `allOf`, then merge. This is Example 2's shape, used for real dashboards.
- **GUI background work (Swing/JavaFX):** `CompletableFuture.supplyAsync(...).thenAcceptAsync(ui::update, uiExecutor)` keeps long tasks off the UI thread and marshals results back safely.
- **What happens with the wrong tool:** using a `CountDownLatch` + raw `Future` to orchestrate the same pipeline forces manual bookkeeping of every stage, no error propagation, and no composition—you would reimplement `allOf`/`thenCompose` with hand-rolled wait/notify loops and introduce subtle race bugs.

---

## 5. Putting It All Together

### Capstone: a multi-stage, rate-limited batch processing pipeline

This simulated analytics service ingests log batches through three stages—**fetch** (I/O), **transform** (CPU), and **persist** (database). It uses all five mechanisms, each for a precisely defined reason:

- **`CompletableFuture.runAsync`** submits each worker as an asynchronous task on a shared pool.
- **`CyclicBarrier`** guarantees all workers finish transforming batch *N* before anyone persists it, and (second trip) finish persisting before batch *N+1* begins.
- **`Semaphore`** caps concurrent database writes to `DB_SLOTS` (the DB's connection limit).
- **`CountDownLatch`** lets the main thread know when every worker has completed every batch.
- **`ReentrantLock`** guards the shared `roundSummary` list updated by the barrier action.

```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public final class BatchProcessingPipeline {

    private static final int WORKERS = 4;
    private static final int BATCHES = 3;
    private static final int DB_SLOTS = 2;      // database connection limit

    // ---- coordination tools ---------------------------------------------
    private static final ExecutorService pool = Executors.newFixedThreadPool(WORKERS);
    private static final Semaphore dbSlots = new Semaphore(DB_SLOTS, true);
    private static final ReentrantLock summaryLock = new ReentrantLock();
    private static final CountDownLatch pipelineDone = new CountDownLatch(WORKERS);
    private static final AtomicLong totalRecords = new AtomicLong();
    private static final List<String> roundSummary = new ArrayList<>();

    private static CyclicBarrier stageBarrier;   // created in main(), reused for every round

    public static void main(String[] args) throws InterruptedException {
        // barrierAction runs in the last-arriving worker, once per stage boundary.
        stageBarrier = new CyclicBarrier(WORKERS, BatchProcessingPipeline::logRound);

        List<CompletableFuture<Void>> workers = new ArrayList<>();
        for (int w = 0; w < WORKERS; w++) {
            final int id = w;
            workers.add(CompletableFuture.runAsync(() -> runWorker(id), pool));
        }

        pipelineDone.await();   // main thread blocks until ALL workers are truly finished
        pool.shutdown();
        System.out.println("Pipeline complete. Total records = " + totalRecords.get());
        System.out.println("Round log: " + roundSummary);
    }

    private static void runWorker(int workerId) {
        try {
            for (int batch = 0; batch < BATCHES; batch++) {
                List<String> raw = fetchBatch(workerId, batch);      // stage A: network I/O
                List<String> enriched = transform(raw);             // stage B: CPU parsing
                awaitBarrier("transform");                          // gate: all done with stage B
                persist(enriched);                                  // stage C: DB write
                awaitBarrier("persist");                            // gate: all done with stage C
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Worker " + workerId + " interrupted");
        } catch (Exception e) {
            System.err.println("Worker " + workerId + " failed: " + e.getMessage());
        } finally {
            pipelineDone.countDown();   // exactly once per worker, on every path
        }
    }

    private static void awaitBarrier(String stage)
            throws InterruptedException, java.util.concurrent.BrokenBarrierException,
                   java.util.concurrent.TimeoutException {
        // Timeout keeps a dead peer from hanging the whole pipeline.
        stageBarrier.await(60, TimeUnit.SECONDS);
    }

    private static List<String> fetchBatch(int workerId, int batch) throws InterruptedException {
        Thread.sleep(50); // simulated network fetch
        return Arrays.asList("raw-" + workerId + "-" + batch + "-0",
                             "raw-" + workerId + "-" + batch + "-1");
    }

    private static List<String> transform(List<String> raw) {
        List<String> out = new ArrayList<>();
        for (String line : raw) {
            out.add(line.toUpperCase()); // simulated CPU-heavy enrichment
        }
        return out;
    }

    private static void persist(List<String> records) throws InterruptedException {
        boolean granted = dbSlots.tryAcquire(1, TimeUnit.SECONDS);
        if (!granted) {
            System.out.println("DB busy: dropped " + records.size() + " records");
            return;
        }
        try {
            Thread.sleep(100); // simulated database write
            totalRecords.addAndGet(records.size());
        } finally {
            dbSlots.release(); // always return the permit, even on failure
        }
    }

    private static void logRound() {
        // Runs inside the barrier's release protocol; may touch shared state safely.
        summaryLock.lock();
        try {
            roundSummary.add("round " + (roundSummary.size() + 1)
                    + ": cumulative " + totalRecords.get() + " records");
        } finally {
            summaryLock.unlock();
        }
    }
}
```

### How the pieces interact

1. **Launch (CompletableFuture).** `main` submits four workers with `runAsync` on a fixed pool. The async chain means `main` proceeds straight to `pipelineDone.await()`—the futures complete independently and no thread is parked waiting on stage results yet.
2. **Stage gating (CyclicBarrier).** Each worker fetches and transforms batch *N* privately, then hits `awaitBarrier`. The barrier trips only when all four have transformed batch *N*, so the persist phase always sees a consistent per-batch picture. The *second* `awaitBarrier` per batch prevents batch *N+1*'s fetch/transform from racing ahead of batch *N*'s persist.
3. **Resource limiting (Semaphore).** Inside `persist`, `dbSlots.tryAcquire(1, 1s)` enforces the database's two-connection limit across all four workers; `tryAcquire` (rather than blocking `acquire`) keeps the pipeline moving when the DB is saturated, and the `finally` release prevents a permit leak if the write throws.
4. **Aggregation (ReentrantLock + barrier action).** `logRound` runs in the last-arriving worker thread at each barrier trip. It serializes updates to the shared `roundSummary` with the `ReentrantLock` (the lock, not `synchronized`, because it is used inside the barrier's action thread and its ownership semantics are explicit and debuggable).
5. **Completion (CountDownLatch).** The `finally` in `runWorker` counts `pipelineDone` down exactly once per worker. `main`'s `await()` returns only when all four workers have finished all batches—after which reading `totalRecords` and `roundSummary` is safe thanks to the latch's happens-before guarantee.

Each tool was chosen because it encodes one *distinct* contract: futures for fire-and-compose, a barrier for repeated stage rendezvous, a semaphore for admission control, a lock for guarded aggregation, and a latch for "all done" notification.

---

## 6. Key Takeaways

### Contrast table

| Mechanism | Purpose | One-shot / Reusable | Blocking vs Non-blocking | Typical use |
|---|---|---|---|---|
| **ReentrantLock** | Exclusive access with flexible control (fairness, timeouts, conditions) | Reusable (acquire/release repeatedly) | Blocking (`lock`) and non-blocking (`tryLock`) | High-contention shared state, custom structures, deadlock avoidance |
| **CountDownLatch** | Wait until a counter reaches zero | **One-shot** (no reset) | Blocking (`await`) | Service startup gates, "wait for N tasks" |
| **Semaphore** | Bound concurrent users of a resource | Reusable (permits recycled continuously) | Blocking (`acquire`) and non-blocking (`tryAcquire`) | Connection pools, rate limiters, admission control |
| **CyclicBarrier** | N threads rendezvous, repeatedly, between stages | **Reusable** (auto-resets per trip; explicit `reset` after break) | Blocking (`await`) | Multi-stage parallel computation, synchronized rounds |
| **CompletableFuture** | Compose asynchronous operations with callbacks | One-shot per stage (a future completes once) | Non-blocking chain; blocking escape hatch (`join`/`get`) | Async pipelines, microservice orchestration, parallel I/O aggregation |

### Decision flowchart (text)

- **Just need a plain critical section** → `synchronized`. Stop here unless you need more.
- **Need a critical section plus timeouts, interruptibility, fairness, or multiple wait queues** → `ReentrantLock`.
- **Need at most N threads/users in a section at once** → `Semaphore`.
- **Need a one-shot gate that opens after N external events** → `CountDownLatch`.
- **Need the same N threads to sync with each other, again and again, between phases** → `CyclicBarrier`.
- **Need to react to async results, chain stages, fan out/combine, or handle async errors** → `CompletableFuture`.
- **If two mechanisms both seem plausible**, ask *who waits* and *how many times*: if the waiters are the same threads that must rendezvous with each other, and the pattern repeats → `CyclicBarrier`; if it is a one-time "everything done?" signal → `CountDownLatch`.

---

## 7. Practice Exercises

### Easy — Thread-safe counter with ReentrantLock
Implement `ConcurrentAccumulator` with `increment()`/`get()` backed by a fair `ReentrantLock`, then run 8 threads each calling `increment()` 100,000 times.
- **Correctness criteria:** final value is exactly 800,000; no `IllegalMonitorStateException`; the program terminates.
- **Hint:** never let an exception skip `unlock()`; put it in `finally`. Watch your main-thread join/await.

### Easy — One-shot start gate with CountDownLatch
Write a harness where a `CountDownLatch(1)` start gate releases 8 worker threads *simultaneously*, and each worker records the timestamp at which it started; the main thread then prints the spread between the earliest and latest start.
- **Correctness criteria:** all workers begin within a few milliseconds of each other; the latch counts down exactly once; the main thread does not proceed until every worker has finished.
- **Hint:** workers `await()` the start gate but must never `countDown()` it; use a second latch (or `join()`) for completion.

### Medium — Semaphore-bounded connection pool
Implement a `Pool` of size 4 using `Semaphore(4, true)`. Ten client threads each acquire, "use" the resource for a random 10–50 ms, and release. Track a global counter of *currently in use* connections (guarded appropriately) and assert it never exceeds 4.
- **Correctness criteria:** max concurrent usage never exceeds 4; every acquired permit is released exactly once; no deadlock; `availablePermits()` returns 4 at the end.
- **Hint:** wrap acquire/release in `try/finally` and keep an `acquired` flag; test with a deliberately long client to prove the bound.

### Medium — Iterative computation with CyclicBarrier
Reproduce the `IterativeWorkers` pattern: N workers each process their slice, write results to their own slot, and `await()` a barrier; the barrier action merges the round into a running total. Run 5 rounds.
- **Correctness criteria:** the running total equals the sequential sum of all per-round contributions; `await()` succeeds every round (no `BrokenBarrierException`); the barrier is demonstrably reused across rounds (check `getNumberWaiting()` or the round log).
- **Hint:** workers must write *before* `await()` and the barrier action must run *after* all writes—that ordering is what makes the reads safe.

### Hard — Fault-tolerant async pipeline with CompletableFuture
Build a pipeline: `fetch -> validate -> save`, where each stage is a `CompletableFuture` on a dedicated executor. One input in ten is invalid and must fail validation. Use `thenCompose`, `allOf`, and `exceptionally` so that: (a) valid inputs are saved; (b) invalid inputs are logged and skipped without killing the batch; (c) the main thread does **not** block until the very end.
- **Correctness criteria:** every valid input is saved exactly once; every invalid input appears exactly once in the error log; no `ExecutionException` escapes; the whole batch completes in less than the *sequential* time even though 20% of stages sleep 200 ms.
- **Hint:** handle per-item errors with `exceptionally` *per item* (so one failure cannot abort `allOf`), and verify the speedup—if your program is slower than sequential, you are probably calling `join()` mid-pipeline.

---

## 8. Further Reading

- **Oracle Java Platform, Standard Edition API Documentation — `java.util.concurrent` package.** The authoritative reference for every class in this chapter; read the Javadoc *narrative* sections (especially for `Semaphore`, `CyclicBarrier`, and `CompletableFuture`), which explain intended usage and pitfalls in prose.
- **The Java Language Specification, Chapter 17: Threads and Locks.** The precise definition of the Java memory model—happens-before, synchronization order, and why the barrier/latch guarantees you rely on actually hold. Essential background for *why* these tools work, not just *how*.
- **Brian Goetz et al., *Java Concurrency in Practice* (Addison-Wesley, 2006).** The canonical book on the subject. Chapters 13–15 cover explicit locks, atomic variables, and non-blocking algorithms; Chapter 5 covers latches, barriers, and semaphores with the same rigor this chapter borrows.
- **Tomasz Nurkiewicz, "CompletableFuture in Java 8" (blog series, 2014).** A widely respected, example-dense deep dive into `CompletableFuture` composition, error handling, and the async-vs-sync stage trap—directly extends §4.E with many edge-case runnable snippets.
- **Doug Lea, *Concurrent Programming in Java: Design Principles and Patterns* (Addison-Wesley, 2nd ed., 1999).** The design rationale behind `java.util.concurrent` from its principal author; older APIs but unmatched intuition about synchronization contracts, starvation, and fairness trade-offs.

---

*End of chapter — Advanced Concurrency in Java.*