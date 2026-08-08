# Concurrency Foundations in Java
*A textbook-style guide to threads, tasks, synchronization, and atomicity for the intermediate Java developer.*

---

## 1. Introduction

Concurrency is the ability of a program to do **more than one thing at a time**. In the 1980s and 1990s, software got faster simply because CPUs got faster: you waited a year, bought a new machine, and your program just *ran quicker*. That era is over. Since roughly 2005, processor clock speeds have plateaued; instead, CPUs now ship with **more cores** — 4, 8, 16, even 64 of them. The only way your application gets faster on modern hardware is to **use those cores simultaneously**, i.e., to make your program concurrent.

There's a second, equally important reason concurrency matters: **blocking I/O**. Reading from a disk, querying a database, or calling a web API can take milliseconds or even seconds. If your program sits idle waiting for that response before doing anything else, it wastes the entire machine's time. A concurrent program can send the request, switch to doing useful work, and return to handle the response when it arrives.

And finally there is **responsiveness**. Think of a music player's user interface: if the *Play* button handler had to decode an entire file before the click "returns," the UI would freeze for seconds. Concurrent execution lets a background thread do the heavy lifting while the UI thread stays snappy.

### What this chapter covers

This chapter is your introduction to the **concurrency foundations** of the Java platform. You already know Java syntax, classes, and interfaces; by the end you will understand:

1. **Thread basics** — what a thread is, how to create and run one, and its lifecycle.
2. **`Runnable` and `Callable`** — the two standard ways to describe work, and how to execute it with an `ExecutorService`.
3. **Synchronization** — protecting shared data from corrupting races using `synchronized` and explicit locks.
4. **`volatile`** — the visibility keyword that makes simple shared flags safe.
5. **Atomic classes** — lock-free building blocks like `AtomicInteger` for high-performance counters.

A note on Java version: all examples compile with **Java 17 or later**. Where I use a feature newer than 17 (such as `Thread.ofPlatform()`, which arrived in Java 19 and became final in Java 21), I'll say so explicitly.

### A motivating example: doing work serially

Suppose your application must (1) download images, (2) resize them, and (3) upload them to a CDN. Each operation blocks on I/O. Written naively, the program does them one after another:

```java
// Demonstrates serial execution: each blocking task waits for the previous one
public class SerialExecutionDemo {
    public static void main(String[] args) throws InterruptedException {
        long start = System.nanoTime();

        // Simulate three blocking operations done one after another
        doWork("download images", 3000);
        doWork("resize thumbnails", 2000);
        doWork("upload to CDN", 2500);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("Total time: " + elapsedMs + " ms");
    }

    private static void doWork(String task, long sleepMillis) throws InterruptedException {
        System.out.println("Starting: " + task);
        Thread.sleep(sleepMillis); // pretend this is real network/disk I/O
        System.out.println("Finished: " + task);
    }
}
```

Each call to `doWork` blocks the single thread (the `main` thread) for the duration of the fake I/O, so the total is the **sum** of the durations: about **7.5 seconds**.

Now the same three tasks, run concurrently — each on its own thread:

```java
// Demonstrates concurrent execution: three tasks run on separate threads at once
public class ConcurrentPreview {
    public static void main(String[] args) throws InterruptedException {
        long start = System.nanoTime();

        // Start three threads, each running one task
        Thread t1 = new Thread(() -> doWork("download images", 3000));
        Thread t2 = new Thread(() -> doWork("resize thumbnails", 2000));
        Thread t3 = new Thread(() -> doWork("upload to CDN", 2500));

        t1.start();
        t2.start();
        t3.start();

        t1.join(); // wait until t1 finishes
        t2.join(); // wait until t2 finishes
        t3.join(); // wait until t3 finishes

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("Total time: " + elapsedMs + " ms");
    }

    private static void doWork(String task, long sleepMillis) {
        System.out.println("Starting: " + task);
        try {
            Thread.sleep(sleepMillis); // pretend this is real network/disk I/O
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore the interrupt flag
        }
        System.out.println("Finished: " + task);
    }
}
```

Because the three tasks now overlap, the total drops to roughly the **maximum** of the durations: about **3 seconds** — a 2.5× speedup for free. That's the promise of concurrency. The rest of this chapter explains *how* all of this works under the hood, and — critically — how to do it safely.

> **Real-world use:** Nearly every production system relies on this. A web server accepts thousands of simultaneous connections and serves each request on its own thread or task while the others block on I/O. Build tools like Maven/Gradle compile and test modules in parallel. Mobile apps run network calls on background threads so the UI never freezes. Concurrency is not a niche feature — it's how real software stays fast and responsive.

---

## 2. Thread Basics

### 2.1 What Is a Thread?

A **process** is an operating-system container for a running program: it holds the program's code, its memory, and its open files. A **thread** is a single flow of execution *inside* a process — a sequence of instructions that the CPU can run independently of other threads in the same process.

**Analogy:** Think of a process as a **kitchen** (with a shared pantry, fridge, and counter space) and a thread as a **chef** working in that kitchen. Multiple chefs can work in the same kitchen at the same time, sharing the ingredients (memory) but each following their own recipe. Every chef has their own recipe book and prep notes (their stack), but they all reach into the same fridge (the heap) and can even grab each other's onions (shared state) — which is where the trouble starts.

The crucial distinction is *what is shared* and *what is private*:

| Aspect | Thread | Process |
|---|---|---|
| Lives inside | a process | an operating system, managed by the OS kernel |
| Heap / shared memory | **Shared** with all threads of the same process | Private to that process |
| Own stack & program counter | **Yes** — each thread has its own call stack and private local variables | Yes — the process has its own |
| Cost to create | Cheap (thousands are feasible) | Expensive (hundreds are already a lot) |
| Communication | Directly share objects in the heap | Requires IPC (sockets, files, pipes) |
| Failure impact | A thread can crash without killing the process | A crashing process takes everything with it |
| Switching cost | Low (same memory map) | High (page tables, caches must be swapped) |

In Java, every application begins with a single thread called `main`. Whenever you call `new Thread(...).start()`, you spawn a second chef in the same kitchen.

### 2.2 Creating and Starting Threads

There are two classic ways to create a thread, plus a modern factory API. All three produce identical threads — the difference is *where* you put the work.

#### Approach 1: extend `Thread` (shown, then explained)

```java
// Demonstrates Approach 1: extending the Thread class (discouraged in practice)
public class MyWorker extends Thread {
    @Override
    public void run() {
        System.out.println("Working in thread: " + getName());
    }

    public static void main(String[] args) {
        MyWorker worker = new MyWorker();
        worker.start(); // schedules run() to execute on a brand-new thread
    }
}
```

Walking through it: `MyWorker` inherits everything from `Thread` and overrides `run()`, the method the thread executes when started. In `main`, we construct the worker and call `start()`. The JVM creates a real OS thread, which then invokes `run()` on it. The output is a line like `Working in thread: Thread-0`.

**Why this is discouraged:**

1. **Java only allows single inheritance.** Extending `Thread` consumes your one inheritance slot, so your worker class can't extend a more useful base class.
2. **It couples the task with its executor.** The work (`run`) and the thread are the same object, so you can't hand the same task to a thread pool later.
3. **It promotes per-task thread creation** (`new MyWorker().start()` per job), which is wasteful; you should reuse threads via an executor (Section 3.3).
4. **It's harder to test**, because the task logic is buried inside a `Thread` subclass instead of a plain object.

The modern guideline is *"prefer composition over inheritance"*: separate the **work** from the **worker**.

#### Approach 2: pass a `Runnable` (preferred)

```java
// Demonstrates Approach 2: passing a Runnable to a Thread constructor (preferred)
public class RunnableDemo {
    public static void main(String[] args) {
        Runnable task = () -> System.out.println("Hello from "
                + Thread.currentThread().getName());

        Thread thread = new Thread(task);   // the thread, decoupled from the work
        thread.start();
    }
}
```

Now the work is a plain `Runnable` object, and the `Thread` is just an executor. The task can be reused, stored in a list, passed to a pool — whatever you like.

#### Java 8+ lambda syntax

`Runnable` is a *functional interface* (it has exactly one abstract method, `run()`), so a lambda — `() -> ...` — is the idiomatic way to build one. In the example above, `() -> System.out.println(...)` is shorthand for an anonymous class:

```java
Runnable task = () -> System.out.println("Hello");
// is equivalent to:
Runnable task = new Runnable() {
    @Override
    public void run() {
        System.out.println("Hello");
    }
};
```

Lambdas are the standard choice in modern Java, and you'll see them throughout this chapter.

#### `start()` vs. `run()` — the gotcha

Newcomers often call `run()` directly and see... correct-looking output! But the thread never actually gets created.

```java
// Demonstrates the critical difference between start() and run()
public class StartVsRun {
    public static void main(String[] args) {
        Runnable task = () -> System.out.println("Running on: "
                + Thread.currentThread().getName());

        Thread t1 = new Thread(task, "worker-1");
        t1.start(); // schedules run() to execute on a NEW thread named worker-1

        Thread t2 = new Thread(task, "worker-2");
        t2.run();   // executes run() on the CURRENT (main) thread — no new thread!
    }
}
```

Output:

```
Running on: worker-1
Running on: main
```

Line-by-line: `t1.start()` asks the JVM to spin up a real OS thread; the lambda then prints `worker-1`. `t2.run()` never creates a thread — it simply *calls the method* like any ordinary method call, right here on the `main` thread, so it prints `main`. Calling `run()` directly is the single most common beginner mistake in concurrency, because the program *appears* to work — until you need two tasks to run at the same time and they don't.

#### The modern factory (Java 19+, final in Java 21)

Java 19 introduced a builder-style factory that reads a bit nicer:

```java
// Requires Java 19+ (final in Java 21): factory-style thread creation
public class ModernThreadCreation {
    public static void main(String[] args) throws InterruptedException {
        Runnable task = () -> System.out.println("Hello from "
                + Thread.currentThread().getName());

        // ofPlatform() builds a classic OS-backed thread; start() runs it
        Thread t = Thread.ofPlatform().name("modern-worker").start(task);
        t.join();
    }
}
```

On Java 17, use the plain `new Thread(...)` constructor; the factory is a drop-in replacement and is used where noted later in this chapter.

### 2.3 Thread Lifecycle

A thread is not always running. Java defines **six lifecycle states**, accessible at any moment via `thread.getState()`:

| State | Meaning |
|---|---|
| `NEW` | Created with `new Thread(...)` but `start()` hasn't been called yet. |
| `RUNNABLE` | Ready to run and/or currently running on a CPU. (Java does not distinguish "actually executing" from "waiting for a scheduler timeslice.") |
| `BLOCKED` | Waiting to **enter** a `synchronized` block/method while another thread holds the monitor lock. |
| `WAITING` | Waiting indefinitely for another thread to act — e.g., after `Object.wait()`, `join()` with no timeout, or `LockSupport.park()`. |
| `TIMED_WAITING` | Waiting for a fixed amount of time — e.g., during `Thread.sleep(ms)`, `join(ms)`, or `wait(ms)`. |
| `TERMINATED` | `run()` has completed, either normally or by throwing an exception. |

Here is a program that observes a thread passing through several of these states:

```java
// Demonstrates the thread lifecycle states via getState()
public class LifecycleDemo {
    public static void main(String[] args) throws InterruptedException {
        Thread t = new Thread(() -> {
            System.out.println("Inside run():   " + Thread.currentThread().getState());
            try {
                Thread.sleep(500);              // forces TIMED_WAITING
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "lifecycle-demo");

        System.out.println("After creation: " + t.getState());   // NEW
        t.start();
        System.out.println("After start():  " + t.getState());   // RUNNABLE

        Thread.sleep(100);                                       // give t time to fall asleep
        System.out.println("During sleep:   " + t.getState());   // TIMED_WAITING

        t.join();                                                // wait for t to finish
        System.out.println("After join():   " + t.getState());   // TERMINATED
    }
}
```

Output (roughly):

```
After creation: NEW
Inside run():   RUNNABLE
After start():  RUNNABLE
During sleep:   TIMED_WAITING
After join():   TERMINATED
```

Line-by-line: a freshly created thread reports `NEW`. Right after `start()`, it's `RUNNABLE` (scheduled, possibly not yet given CPU time). Inside `run()`, while the worker is in `Thread.sleep(500)`, it's `TIMED_WAITING` — asleep for a fixed duration. Finally, once the worker's `run()` completes and `main` rejoins it, the state is `TERMINATED`. You'll meet `BLOCKED` and `WAITING` in Section 4, where lock contention and `wait()` come into play.

### 2.4 Common Thread Methods

#### `join()` — "wait for this thread to finish"

`main` may outlive the threads it started. `t.join()` blocks the calling thread until thread `t` terminates:

```java
Thread t = new Thread(() -> doExpensiveWork());
t.start();
t.join();   // main pauses here until t is done
```

This is how the intro's `ConcurrentPreview` made sure all three tasks completed before printing the total time.

#### `sleep()` — "pause this thread for a while"

`Thread.sleep(millis)` suspends the *current* thread for the given number of milliseconds. It throws the checked `InterruptedException`, which forces you to handle the "someone is telling me to stop" case (more below).

#### `interrupt()` and the `InterruptedException` reality

`interrupt()` is a polite request for a thread to stop what it's doing. The interrupt is delivered as a flag, and most *blocking* methods (`sleep`, `join`, `wait`, blocking I/O on some channels) respond by throwing `InterruptedException`.

The rules of the road:

1. **You must handle `InterruptedException`** — the compiler forces it, because `sleep` and `join` declare it.
2. **Never swallow it.** Catching it and doing nothing destroys the interruption request. Best practice is to restore the flag with `Thread.currentThread().interrupt()` (so surrounding code still sees the interrupt), or to rethrow if your method allows it.
3. A thread *ignores* an interrupt while running ordinary non-blocking code; the flag just sits there until the thread checks it (e.g., via `Thread.interrupted()` in a loop).

This combined example shows `join`, `sleep`, and `interrupt` working together:

```java
// Demonstrates join(), sleep(), and interrupt() with proper interrupt handling
public class ThreadMethodsDemo {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(2000);              // simulate long work
                System.out.println("Worker completed its work.");
            } catch (InterruptedException e) {
                System.out.println("Worker was interrupted while sleeping.");
                Thread.currentThread().interrupt(); // restore the interrupt flag
            }
        }, "worker");

        worker.start();

        Thread.sleep(500);                       // let the worker start sleeping
        System.out.println("Main is getting impatient, interrupting...");
        worker.interrupt();                      // tell the worker to stop early

        worker.join();                           // wait for the worker to actually finish
        System.out.println("Worker state: " + worker.getState());
    }
}
```

Output:

```
Main is getting impatient, interrupting...
Worker was interrupted while sleeping.
Worker state: TERMINATED
```

Line-by-line: the worker tries to sleep 2 seconds, but after 0.5 s `main` calls `interrupt()`. The worker's `sleep` immediately throws `InterruptedException`; the catch block prints a message and **re-sets the interrupt flag** so the intent isn't lost. `main` then `join()`s, which blocks until the worker truly exits, leaving it `TERMINATED`.

#### `setName`, `getPriority`, and daemon threads

Two small but useful tools:

- **Names.** `new Thread(task, "name")` or `t.setName("name")` make thread dumps (and your log output) readable.
- **Priority.** `t.setPriority(Thread.MAX_PRIORITY)` (10) or `MIN_PRIORITY` (1) hints to the OS which thread is more important. It's only a *hint* — never rely on it for correctness.
- **Daemon threads.** A daemon is a background helper thread that does **not** keep the JVM alive. When every non-daemon thread exits, the JVM shuts down and daemons die mid-step. Mark one with `t.setDaemon(true)` **before** `start()`.

```java
// Demonstrates daemon threads: they do not prevent the JVM from exiting
public class DaemonDemo {
    public static void main(String[] args) throws InterruptedException {
        Thread daemon = new Thread(() -> {
            try {
                while (true) {                       // would run forever...
                    System.out.println("Daemon heartbeat...");
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "heartbeat");
        daemon.setDaemon(true);
        daemon.start();

        Thread.sleep(1500);                          // let it beat a few times
        System.out.println("Main thread exits — the JVM shuts down, "
                + "killing the daemon mid-loop.");
    }
}
```

Because `heartbeat` is a daemon, the JVM exits as soon as `main` returns, and the infinite loop is terminated with it.

#### The cardinal sin: swallowing `InterruptedException`

```java
// Demonstrates the anti-pattern of swallowing InterruptedException
public class SwallowedInterrupt {
    public static void main(String[] args) {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                // BAD: the interruption request is silently destroyed
            }
        });
        t.start();
        // elsewhere: t.interrupt() would have no lasting effect
    }
}
```

Catching `InterruptedException` and doing nothing is like someone tapping you on the shoulder to say "please stop," and you nodding, then continuing as if nothing happened. The caller's `interrupt()` has no effect. Always re-set the flag or propagate.

> **Real-world use:** Every Java application server, batch job scheduler, and UI toolkit leans on these mechanics. A web container interrupts long-running request threads when a client disconnects. Test runners interrupt flaky test threads on timeout. Daemon threads power background housekeeping — cache expiration sweeps, log rotation, health-check beacons — that must silently vanish when the app shuts down. And readable thread names are what make a thread dump from a production outage understandable in the first place.

---

## 3. Runnable and Callable

Threads are the *workers*; `Runnable` and `Callable` describe the *work*. The modern way to run work is not to create a thread per task but to hand tasks to a thread pool (an `ExecutorService`). This section introduces both task interfaces and then the executor that runs them.

### 3.1 The `Runnable` Interface

`Runnable` has been in Java since version 1.0. Its contract is brutally simple:

```java
@FunctionalInterface
public interface Runnable {
    void run();
}
```

One method, no parameters, `void` return, and — crucially — `run()` **may not throw checked exceptions**. A `Runnable`'s `run()` body either handles its own exceptions or wraps them in unchecked ones (like `RuntimeException`).

```java
// Demonstrates the Runnable interface: run() returns void and cannot throw checked exceptions
public class RunnableExample {
    public static void main(String[] args) {
        Runnable task = () -> {
            // run() returns void — a result cannot be returned directly
            int result = 40 + 2;
            System.out.println("Computed " + result + ", but can't return it!");
        };

        Thread thread = new Thread(task);
        thread.start();
    }
}
```

Line-by-line: the lambda implements `run()`, computes `result`, and prints it — but there's no way for the caller to receive that value. And if `result` came from a method that throws `IOException`, we'd be stuck: `run()` declares no checked exceptions, so we'd have to wrap it in a `RuntimeException`. Both limitations were acceptable in 1995, but they make `Runnable` awkward for tasks that *produce* something. That's why `Callable` exists.

### 3.2 The `Callable` Interface

Java 5 added `Callable`, designed exactly for "work that returns a result or may fail":

```java
@FunctionalInterface
public interface Callable<V> {
    V call() throws Exception;
}
```

The generic type parameter `V` is the type of the result, and `call()` **may throw any checked exception**. 

```java
// Demonstrates the Callable interface: returns a value and may throw checked exceptions
import java.util.concurrent.Callable;

public class CallableExample {
    public static void main(String[] args) throws Exception {
        Callable<Integer> task = () -> {
            int result = 40 + 2; // could also throw an Exception here
            return result;       // call() may return a value
        };

        Integer answer = task.call(); // called directly here; normally via ExecutorService
        System.out.println("Answer: " + answer);
    }
}
```

Here the lambda returns `Integer`, and `main` declares `throws Exception` because `call()` can throw anything. In practice you won't call `task.call()` yourself very often — you'll submit it to an `ExecutorService` and get a `Future`, which is how the result comes back to you.

**Runnable vs. Callable:**

| | `Runnable` | `Callable<V>` |
|---|---|---|
| Abstract method | `void run()` | `V call()` |
| Returns a value | **No** (void) | **Yes** (type `V`) |
| Throws checked exceptions | **No** | **Yes** (`throws Exception`) |
| Execution model | `new Thread(r).start()`, or submit to an executor | Must be submitted to an `ExecutorService` (returns `Future<V>`) |
| Result access | None — side effects only | Via `Future.get()` |
| Typical use cases | Fire-and-forget logging, `System.out` work, UI updates | Computations with results: queries, calculations, HTTP responses |

### 3.3 Executing with ExecutorService

Creating a fresh thread for every task is expensive and unmanageable at scale. An **`ExecutorService`** is a thread pool: a fixed set of worker threads that you hand tasks to, and which execute those tasks and reuse the threads. This decouples "what to run" from "how many threads and how long they live."

Two common pool builders:

```java
ExecutorService single = Executors.newSingleThreadExecutor();   // one worker thread
ExecutorService fixed   = Executors.newFixedThreadPool(4);      // four worker threads
ExecutorService cached  = Executors.newCachedThreadPool();      // grows/shrinks on demand
```

You submit work with:

- `pool.execute(runnable)` — fire-and-forget, no result.
- `pool.submit(callable)` — returns a `Future<V>`.
- `pool.submit(runnable)` — returns a `Future<?>`.

#### `Future<T>` and `get()`

A **`Future`** is a promise: a handle that represents work still in progress. The moment you submit a `Callable`, you get back a `Future`; the task runs *asynchronously*. When you need the result, you call `future.get()`, which **blocks** the calling thread until the result is ready (or throws if the task failed).

`get()` declares two checked exceptions you must handle:

- **`InterruptedException`** — the calling thread was interrupted while waiting.
- **`ExecutionException`** — the task itself threw an exception; `e.getCause()` reveals the original one.

#### A full working example

```java
// Demonstrates submitting Callable tasks to an ExecutorService and collecting results via Future
import java.util.concurrent.*;

public class ExecutorServiceDemo {
    public static void main(String[] args) {
        // A fixed pool of 3 threads will run all 5 tasks
        ExecutorService pool = Executors.newFixedThreadPool(3);

        try {
            Future<Integer> f1 = pool.submit(() -> slowSquare(2));
            Future<Integer> f2 = pool.submit(() -> slowSquare(3));
            Future<Integer> f3 = pool.submit(() -> slowSquare(4));
            Future<Integer> f4 = pool.submit(() -> slowSquare(5));
            Future<Integer> f5 = pool.submit(() -> slowSquare(6));

            // get() blocks until each task's result is available
            int sum = f1.get() + f2.get() + f3.get() + f4.get() + f5.get();
            System.out.println("Sum of squares: " + sum);   // 4+9+16+25+36 = 90
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.err.println("Task failed: " + e.getCause());
        } finally {
            pool.shutdown();   // stop accepting new tasks, then shut down when done
        }
    }

    private static int slowSquare(int n) throws InterruptedException {
        Thread.sleep(500); // simulate a slow computation
        return n * n;
    }
}
```

Output:

```
Sum of squares: 90
```

Line-by-line: we submit five `Callable` lambdas (each computing a square after a 0.5 s delay) to a pool of three threads. All five submissions return *immediately* with `Future` handles — nothing is computed yet. `f1.get()` then blocks until that task completes; the JVM schedules the five tasks across the three pool threads, so they overlap. Each `get()` hands back the `Integer` result, and the sum comes out to 90. The `try`/`catch`/`finally` structure matters: `InterruptedException` and `ExecutionException` are both declared by `get()`, and `pool.shutdown()` guarantees we don't leave worker threads hanging around.

#### When to prefer `Runnable` vs. `Callable`

Use **`Callable`** whenever the task *produces a value* (queries, computations, API responses) or might *throw a checked exception* you want to propagate. Use **`Runnable`** for fire-and-forget side effects — writing to a log, sending a notification, flushing a cache — where nobody needs the outcome. When in doubt, `Callable` is the more flexible choice; it can express everything `Runnable` can plus more.

> **Real-world use:** Web applications are the canonical example: an API endpoint submits a batch of backend calls (`Callable`s) to an executor — one to the database, one to the payment provider, one to an email service — then `get()`s each `Future` and assembles the response. Report generators split a month of data across ten `Callable` tasks and sum the `Future` results. Search engines query shards in parallel. The thread pool bounds resource usage (configurable concurrency) and `Future` gives you a clean way to join all the results back together.

---

## 4. Synchronization

### 4.1 The Problem: Data Races

Threads share the process's heap memory. When two or more threads read and write the *same* memory without coordination, you get a **data race** — the outcome depends on the arbitrary interleaving of thread executions, and it is usually wrong.

**Analogy:** two chefs share a kitchen whiteboard that records how many orders are up. Chef A reads "10 orders", walks to the fridge, comes back and writes "11". Meanwhile Chef B also read "10" and also writes "11". Two orders were completed, but the board says 11 — an order went missing. The whiteboard has no rule saying "only one person may read-and-update it at a time."

Here is that bug in Java. Ten threads each add 1 to a shared counter 10,000 times. Correct result: 100,000.

```java
// Demonstrates a data race: unsynchronized read-modify-write produces a wrong total
public class BrokenCounter {
    private static int count = 0;
    private static final int THREADS = 10;
    private static final int INCREMENTS = 10_000;

    public static void main(String[] args) throws InterruptedException {
        Thread[] workers = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            workers[i] = new Thread(() -> {
                for (int j = 0; j < INCREMENTS; j++) {
                    count++;            // read-modify-write: NOT atomic
                }
            });
            workers[i].start();
        }
        for (Thread t : workers) {
            t.join();                   // wait for all workers to finish
        }
        System.out.println("Expected: " + (THREADS * INCREMENTS));
        System.out.println("Actual:   " + count);
    }
}
```

Typical output:

```
Expected: 100000
Actual:   99283
```

(Your exact number will differ — that's the point; every run races differently.)

**The root cause.** `count++` looks like one operation, but the CPU actually performs **three steps**: (1) *read* the current value of `count` into a register, (2) *add 1* to it, (3) *write* the new value back. This is called a **read-modify-write** sequence, and it is **not atomic** — a thread can be preempted between step 1 and step 3. If two threads both read `10`, both compute `11`, and both write `11`, one increment is silently lost. With 100,000 increments spread across ten racing threads, dozens of increments get lost every run.

### 4.2 Synchronized Methods and Blocks

The fix is to make the read-modify-write **atomic**: no other thread may be inside the same critical section while one thread is in it.

**`synchronized`** does exactly that, using a per-object **monitor lock** (also called an *intrinsic lock*). Every Java object has one, for free. When a thread enters a `synchronized` block, it acquires the object's monitor; while it holds it, every other thread trying to enter a `synchronized` region guarded by the *same* monitor **blocks** (its state becomes `BLOCKED`) until the lock is released.

**Analogy:** the monitor is the **key to a restroom**. Whoever holds the key is the only person inside; everyone else queues outside. When the occupant leaves, they hand the key to the next person in line. One person at a time, always.

There are three flavors:

```java
// Demonstrates the three flavors of synchronized: instance, static, and block
public class BankAccount {
    private double balance;
    private static int totalAccounts;         // shared static state

    // 1) Instance method: locks on `this` (this account object)
    public synchronized void deposit(double amount) {
        balance += amount;
    }

    // 2) Static method: locks on the BankAccount.class object
    public static synchronized void registerAccount() {
        totalAccounts++;
    }

    // 3) Block: lock only the critical section, on an arbitrary object
    private final Object lock = new Object();
    public void updateAlias(String alias) {
        // ... non-critical code runs WITHOUT the lock ...
        synchronized (lock) {
            // only this block is protected
        }
    }

    public double getBalance() { return balance; }
}
```

Line-by-line: `deposit` is an **instance** method — its lock is the specific `BankAccount` object (`this`), so two threads depositing into *different* accounts never contend, while two threads hitting the *same* account queue up. `registerAccount` is **static** — its lock is the single `BankAccount.class` object, so *all* threads calling it anywhere are serialized. The **block** form (`synchronized (lock)`) narrows the protected region to exactly the statements that need it, minimizing how long other threads must wait.

Now re-run the counter with `synchronized`:

```java
// Demonstrates synchronization: a monitor lock makes the read-modify-write atomic
public class SafeCounter {
    private static int count = 0;
    private static final int THREADS = 10;
    private static final int INCREMENTS = 10_000;

    public static synchronized void increment() {
        count++;
    }

    public static void main(String[] args) throws InterruptedException {
        Thread[] workers = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            workers[i] = new Thread(() -> {
                for (int j = 0; j < INCREMENTS; j++) {
                    increment();
                }
            });
            workers[i].start();
        }
        for (Thread t : workers) {
            t.join();
        }
        System.out.println("Expected: " + (THREADS * INCREMENTS));
        System.out.println("Actual:   " + count);
    }
}
```

Output (identical every run):

```
Expected: 100000
Actual:   100000
```

Line-by-line: `increment()` is now `static synchronized`, so its lock is the `SafeCounter.class` object. Every one of the ten threads must acquire that lock before executing `count++`, and only one may hold it at a time. The read-modify-write can no longer be interleaved, so no increments are lost. The output is deterministic: 100,000.

Note the (good) trade-off: the counter is now correct but *slower*, because all ten threads queue for one lock. Later sections (5 and 6) show lighter-weight alternatives when full locking is overkill.

### 4.3 Deadlocks and Livelocks

A **deadlock** is when two or more threads are each waiting on a lock held by the other, so none can proceed — the program hangs forever.

**Analogy:** two hungry diners share a plate (fork and spoon — no, the classic is two forks). Actually, the classic: **fork and spoon**. Diner A picks up the fork and waits for the spoon. Diner B picks up the spoon and waits for the fork. Each holds one utensil and refuses to let go until it gets the other. Nobody eats. Forever.

```java
// Demonstrates a classic deadlock: two threads each hold one lock and wait for the other
public class DeadlockDemo {
    private static final Object FORK = new Object();
    private static final Object SPOON = new Object();

    public static void main(String[] args) {
        Thread dinerA = new Thread(() -> eat("A", FORK, SPOON));
        Thread dinerB = new Thread(() -> eat("B", SPOON, FORK)); // locks in OPPOSITE order!

        dinerA.start();
        dinerB.start();
    }

    private static void eat(String name, Object first, Object second) {
        synchronized (first) {
            System.out.println(name + " picked up the first item");
            try { Thread.sleep(50); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            synchronized (second) {   // waits forever if the other thread holds it
                System.out.println(name + " is eating!");
            }
        }
    }
}
```

Typical output (then it hangs):

```
A picked up the first item
B picked up the first item
```

Line-by-line: diner A acquires `FORK`, diner B acquires `SPOON` (note: B takes them in the *opposite* order — `eat("B", SPOON, FORK)`). Each then tries to acquire the other's lock. A waits for `SPOON` while holding `FORK`; B waits for `FORK` while holding `SPOON`. Neither ever releases, so neither ever proceeds. The program never prints "is eating!"

**How to avoid deadlocks:**

1. **Consistent lock ordering.** Always acquire locks in the same global order everywhere in the codebase. If every code path takes `FORK` before `SPOON`, the scenario above can't happen (B would have grabbed `FORK` first, so A and B would simply queue, one eats, releases, then the other).
2. **`tryLock` with timeouts.** The `ReentrantLock` (next section) lets you *attempt* to acquire a lock and give up after a timeout rather than waiting forever — you can then back off and retry instead of deadlocking.

A **livelock** is the frustrating cousin: threads are not blocked, but each keeps responding to the other's actions by undoing its own, so no progress is ever made — think two polite people who both step aside in the same direction, forever mirroring each other. The cure is usually randomness (step aside in a *random* direction) or a timeout/back-off strategy.

### 4.4 Locks from `java.util.concurrent.locks`

The `synchronized` keyword is built into the language, but it's rigid. **`ReentrantLock`** from `java.util.concurrent.locks` is an explicit, programmatic lock that gives you more control:

- `lock()` / `unlock()` — manual acquire/release (must pair them!).
- `tryLock()` / `tryLock(timeout, unit)` — attempt to acquire without blocking indefinitely.
- `lockInterruptibly()` — acquire while honoring interrupts.
- Fairness option — `new ReentrantLock(true)` hands the lock to the longest-waiting thread (FIFO), avoiding starvation.

The pattern is always the same, and it's non-negotiable: **release the lock in a `finally` block**, so the lock is freed even if the protected code throws:

```java
// Demonstrates ReentrantLock with tryLock and the mandatory try/finally pattern
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LockDemo {
    private static final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {
        Runnable task = () -> {
            boolean acquired = false;
            try {
                acquired = lock.tryLock(1, TimeUnit.SECONDS); // give up after 1 s
                if (acquired) {
                    System.out.println(Thread.currentThread().getName()
                            + " acquired the lock");
                    Thread.sleep(100);                        // do protected work
                } else {
                    System.out.println(Thread.currentThread().getName()
                            + " could NOT get the lock in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (acquired) {
                    lock.unlock();                            // MUST release in finally
                }
            }
        };

        Thread t1 = new Thread(task, "thread-1");
        Thread t2 = new Thread(task, "thread-2");
        t1.start();
        t2.start();
    }
}
```

Possible output:

```
thread-1 acquired the lock
thread-2 could NOT get the lock in time
```

Line-by-line: both threads call `tryLock(1, TimeUnit.SECONDS)`. Whichever gets the lock first holds it for 100 ms (the `Thread.sleep` inside the protected region); the other waits up to 1 second, times out, and prints that it gave up. Because the check `if (acquired)` and the `finally { if (acquired) lock.unlock(); }` go together, the lock is released exactly when it was acquired — never leaked, even if the work inside throws. This timeout-and-give-up behavior is exactly what lets `tryLock` prevent the deadlocks from Section 4.3.

**`synchronized` vs. `ReentrantLock`:**

| | `synchronized` | `ReentrantLock` |
|---|---|---|
| Syntax | Keyword; automatic unlock | Explicit `lock()` / `unlock()` |
| Release guarantee | Automatic, even on exceptions | **You** must release in `finally` (easy to forget → deadlock) |
| Try-acquire with timeout | No | Yes — `tryLock(timeout, unit)` |
| Interruptible waiting | No | Yes — `lockInterruptibly()` |
| Fairness control | No | Yes — `new ReentrantLock(true)` |
| Condition variables | Via `wait()`/`notify()` (clunky) | `newCondition()` — richer and safer |
| Performance | Good; optimized since Java 6 | Comparable |
| Verdict | Prefer by default — simplest and safest | Reach for it when you need timeouts, interruptibility, or fairness |

> **Real-world use:** Data races corrupt bank account balances, inventory counts, and seat-booking systems — the canonical case for monitors. Financial clearing systems wrap every balance mutation in `synchronized` or `ReentrantLock` so transfers never lose money to interleaving. Deadlocks plague distributed locking (two services each holding a resource the other needs); real systems avoid them with consistent lock ordering and `tryLock`-based lease timeouts, the same strategies shown here. A health-check endpoint that can't respond because the app is deadlocked is the classic "production hang" — and `jstack`/thread dumps are how you spot it.

---

## 5. volatile

### 5.1 Visibility vs. Atomicity

`volatile` addresses a *different* problem than `synchronized`. Recall the Java Memory Model at a high level: for performance, each thread may keep its own **cached copy** of a field in CPU registers or core-local caches. When thread A writes a field, thread B's cached copy may simply **not be refreshed** — B keeps reading a stale value, possibly forever.

**Analogy:** `volatile` is like **posting a notice on a public bulletin board** in the town square. When you update it, everyone in town sees the new notice immediately — nobody carries around their own private copy of the announcement. But the bulletin board does *not* let two people edit it simultaneously: if two citizens both read "price = 10", then each tries to pin up "price = 11", the second one overwrites the first's change. **Visibility yes; atomicity no.**

Two distinct guarantees:

- **Visibility** — the *latest written value* is seen by all threads. `volatile` provides this: every write is flushed to shared memory; every read fetches from shared memory.
- **Atomicity** — a *sequence of operations* (read-modify-write) can't be interrupted mid-way. `volatile` does **not** provide this.

You need both for things like `count++`. You need only visibility for a simple flag.

### 5.2 Using `volatile`

The classic correct use: a **flag that is written by one thread and read by others** — the "stop the loop" pattern. A background worker checks a `volatile boolean` each iteration; the `main` thread flips it to ask the worker to stop.

```java
// Demonstrates a working volatile stop flag: one thread writes it, another reads it
public class VolatileFlagDemo {
    private static volatile boolean running = true;

    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            long counter = 0;
            while (running) {        // reads the volatile flag each iteration
                counter++;
            }
            System.out.println("Worker stopped after " + counter + " iterations");
        }, "worker");

        worker.start();
        Thread.sleep(100);           // let the worker spin for a while

        running = false;             // main thread flips the flag

        worker.join();
        System.out.println("Main: done");
    }
}
```

Output (always terminates):

```
Worker stopped after <some large number> iterations
Main: done
```

Line-by-line: `running` is `volatile`, so the moment `main` writes `running = false`, the change is visible to the worker thread — its next read of `while (running)` sees `false` and the loop exits. The worker prints how many spin iterations it completed, `main` joins, and the program terminates cleanly.

Now the broken version — remove `volatile` and nothing else changes:

```java
// Demonstrates why volatile is needed: without it the worker may never see the flag change
public class BrokenVolatileDemo {
    private static boolean running = true;   // NOT volatile — the bug

    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            long counter = 0;
            while (running) {                // may read a cached, stale value forever
                counter++;
            }
            System.out.println("Worker stopped after " + counter + " iterations");
        }, "worker");

        worker.start();
        Thread.sleep(100);
        running = false;

        worker.join();                       // may hang forever
        System.out.println("Main: done");
    }
}
```

**Why it may hang forever:** the JIT compiler is allowed to *hoist* the loop condition out of the loop — if `running` isn't `volatile`, it may compile `while (running)` as if it were `while (true)` with `running` read once into a register, because nothing in the loop could change it (as far as the memory model is concerned, the loop body never reads or writes shared memory). The worker then spins forever, the `main` thread sits in `worker.join()`, and the program never terminates. On some machines it *appears* to work — CPU cache coherence sometimes saves you by accident — but it is wrong regardless, and the JVM is fully entitled to produce the infinite loop.

`volatile` doesn't just fix this particular demo; it also orders memory accesses so that any writes a thread performed *before* writing the volatile flag become visible to any thread that reads the flag afterwards. (That ordering property is what makes the classic double-checked-locking idiom — and lazy singletons — correct with `volatile`.)

### 5.3 Limitations

**The loudest warning: `volatile` does NOT make `count++` atomic.**

```java
volatile int count;
count++;   // STILL a read-modify-write: read, add, write
```

Three threads each doing `count++` can still lose updates exactly as in Section 4.1 — `volatile` only guarantees each individual read and write touches shared memory, not that the read-modify-write sequence is indivisible. If the operation is more than a single field write, reach for `synchronized`, a lock, or an atomic class.

**`synchronized` vs. `volatile` vs. atomic classes:**

| | `synchronized` / locks | `volatile` | Atomic classes (`java.util.concurrent.atomic`) |
|---|---|---|---|
| What it guarantees | Mutual exclusion **and** visibility | Visibility **only** | Atomicity of one field operation **and** visibility |
| Protects read-modify-write (`count++`) | Yes | **No** | Yes |
| Blocking | Yes — threads queue for the monitor | No — no blocking | No — lock-free via CAS |
| Lock used | Intrinsic or `ReentrantLock` | None | Hardware CAS instruction |
| Best for | Multi-statement critical sections | Single-flag visibility (stop flags, config switches) | Single-field counters, sequence numbers, references |
| Performance | Slowest under contention (threads park/wake) | Fast for reads/writes | Very fast under contention (no thread parking) |

> **Real-world use:** Every framework with a graceful-shutdown path uses a volatile flag — a background cache warmer or connection-pool reaper checks `volatile boolean shutdownRequested` each loop, and the shutdown hook flips it. Application *feature-flag* systems publish a `volatile` config snapshot that request handlers read on every call. Server-to-server health checks flip volatile liveness bits. Just remember: volatile is for *announcements*, not *edits* — the moment two threads update the same field, you need atomicity too.

---

## 6. Atomic Classes

### 6.1 The Problem with Read-Modify-Write

Section 4.1 showed the core trouble: `count++` is really read → modify → write, and interleaved executions lose updates. `synchronized` fixes it, but for a *single counter*, locking feels heavy — every increment parks and wakes threads, and the operating system has to schedule them. The overhead of locking dwarfs the work being protected. Isn't there a faster way to make one tiny update atomic?

There is: **atomic classes**.

### 6.2 `java.util.concurrent.atomic` Overview

The `java.util.concurrent.atomic` package provides thread-safe wrappers around single fields. They are **lock-free**: instead of blocking, they use a hardware instruction called **compare-and-swap (CAS)** that the CPU executes atomically. Under high contention they out-perform locks because no thread ever parks.

| Class | Holds | Typical use |
|---|---|---|
| `AtomicInteger` | `int` | Counters, sequence numbers, request IDs |
| `AtomicLong` | `long` | Large counters, 64-bit sequence numbers |
| `AtomicBoolean` | `boolean` | Thread-safe on/off flags that need compare-and-set |
| `AtomicReference<V>` | an object reference | Atomically swap object snapshots (e.g., config updates) |
| `AtomicIntegerArray` / `AtomicLongArray` / `AtomicReferenceArray` | arrays of values | Per-slot atomic updates in arrays |
| `LongAdder` / `DoubleAdder` | `long` / `double` | Very high-contention counters (Section 6.4) |
| `AtomicLongFieldUpdater` / etc. | a `volatile` field in an existing class | Update a `volatile` field atomically without wrapping the object |

### 6.3 AtomicInteger in Practice

`AtomicInteger` behaves like an `int` with a thread-safe API. The common operations:

```java
// Demonstrates the common AtomicInteger operations
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerMethods {
    public static void main(String[] args) {
        AtomicInteger counter = new AtomicInteger(10);

        System.out.println("get():                " + counter.get());              // 10
        counter.set(5);
        System.out.println("after set(5):         " + counter.get());              // 5
        System.out.println("getAndIncrement():    " + counter.getAndIncrement());  // 5, value becomes 6
        System.out.println("incrementAndGet():    " + counter.incrementAndGet());  // 7
        System.out.println("addAndGet(3):         " + counter.addAndGet(3));       // 10
        System.out.println("compareAndSet(10,20): " + counter.compareAndSet(10, 20)); // true
        System.out.println("after CAS:            " + counter.get());              // 20

        // Java 8+ functional style: read, transform, write — atomically
        int squared = counter.updateAndGet(n -> n * n);
        System.out.println("updateAndGet(n*n):    " + squared);                    // 400
    }
}
```

Output:

```
get():                10
after set(5):         5
getAndIncrement():    5
incrementAndGet():    6
addAndGet(3):         10
compareAndSet(10,20): true
after CAS:            20
updateAndGet(n*n):    400
```

Line-by-line: `get`/`set` are trivial reads and writes. `getAndIncrement()` returns the *old* value (5) then bumps to 6; `incrementAndGet()` bumps first (7) and returns the *new* value; `addAndGet(3)` adds and returns 10. `compareAndSet(10, 20)` checks the current value: since it *is* 10, it sets 20 and returns `true`. Finally, `updateAndGet(n -> n * n)` atomically reads the current value (20), applies the lambda (400), and writes it back in one indivisible step.

Now the counter example from Section 4, rewritten with `AtomicInteger`:

```java
// Demonstrates AtomicInteger: a lock-free, thread-safe counter
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicCounterDemo {
    private static final AtomicInteger count = new AtomicInteger();
    private static final int THREADS = 10;
    private static final int INCREMENTS = 10_000;

    public static void main(String[] args) throws InterruptedException {
        Thread[] workers = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            workers[i] = new Thread(() -> {
                for (int j = 0; j < INCREMENTS; j++) {
                    count.incrementAndGet();
                }
            });
            workers[i].start();
        }
        for (Thread t : workers) {
            t.join();
        }
        System.out.println("Expected: " + (THREADS * INCREMENTS));
        System.out.println("Actual:   " + count.get());
    }
}
```

Output (identical every run):

```
Expected: 100000
Actual:   100000
```

Line-by-line: each worker calls `count.incrementAndGet()` 10,000 times. There is no `synchronized` anywhere, yet every increment is atomic, so no updates are lost and the result is always exactly 100,000 — with none of the thread parking that locks require.

**How does it stay correct without a lock? CAS.**

**Analogy — CAS:** imagine you're writing your page number in the sign-up sheet, but the rule is: *"check the page number printed at the top of the sheet before you write; if the page number is still yours, write — but if someone else already advanced the page number, don't write; rip out your page, re-read the sheet, and retry."* That's compare-and-swap. In hardware, CAS is one instruction: "if the memory location still equals value *expected*, set it to *new* and report success; otherwise report failure." If it fails because another thread changed the value in between, the atomic class simply **retries** — looping until it wins. No locks, no blocking, just a tight retry loop. `incrementAndGet()` is implemented as: read value *v*; call CAS with expected *v* and new *v+1*; if it fails, re-read and try again. On a heavily contended counter, a few threads spin briefly instead of parking — and spinning is cheaper.

### 6.4 LongAdder for High Contention

When *many* threads frequently increment the *same* counter, even CAS gets slow: all those spinning threads compete for one cache line. `LongAdder` solves this by **sharding the counter**. Instead of one number, it keeps a set of cells; each thread increments *its own* cell (no contention), and `sum()` adds all cells together when you finally need the total.

```java
// Demonstrates LongAdder: optimized for many threads frequently incrementing one counter
import java.util.concurrent.atomic.LongAdder;

public class LongAdderDemo {
    public static void main(String[] args) throws InterruptedException {
        LongAdder hits = new LongAdder();       // not a single field — internally sharded
        int THREADS = 8;
        int INCREMENTS = 100_000;

        Thread[] workers = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            workers[i] = new Thread(() -> {
                for (int j = 0; j < INCREMENTS; j++) {
                    hits.increment();           // cheap even when many threads run at once
                }
            });
            workers[i].start();
        }
        for (Thread t : workers) {
            t.join();
        }
        System.out.println("Total hits: " + hits.sum());   // 800_000
    }
}
```

Output:

```
Total hits: 800000
```

The only meaningful differences from `AtomicLong`: you call `increment()` instead of `incrementAndGet()`, and you read the result with `sum()` (not `get()`). Because each thread bumps its own private cell, contention collapses, and under heavy load `LongAdder` can be several times faster than `AtomicLong`.

**When to prefer which:** use `AtomicLong` when you need the *current value* frequently or need operations like `compareAndSet` (its value is always exact). Use `LongAdder` when the counter is *incremented constantly but read rarely* — telemetry counters, hit counts, request totals — where the sharded sum is computed once per report interval. `LongAdder` trades an occasionally more expensive `sum()` for much faster increments.

> **Real-world use:** Atomic classes are everywhere in production plumbing. Web servers keep request counts and in-flight gauges in `AtomicInteger`s. Distributed ID generators and event-sourcing systems mint monotonically increasing sequence numbers with `AtomicLong`. Configuration hot-swapping uses `AtomicReference` to publish a new settings object atomically. High-traffic metrics pipelines (requests per second, cache hit counters) use `LongAdder` precisely because thousands of threads increment the same counter every millisecond and read it only when a metrics scrape runs.

---

## 7. Putting It All Together

Here is a complete, runnable program that ties the chapter together: a **simulated web request server**. It combines an `ExecutorService`, a `Callable`, an `AtomicInteger`, and a `volatile` stop flag — and every tool is there for a specific reason.

```java
// Combines ExecutorService, Callable, AtomicInteger, and a volatile flag in one program
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RequestSimulator {
    // Shared across all worker threads: the total number of served requests.
    private static final AtomicInteger servedRequests = new AtomicInteger();
    // One thread (main) writes this; worker threads read it to decide when to stop.
    private static volatile boolean shuttingDown = false;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // (1) A fixed pool of 4 worker threads serves the requests.
        ExecutorService pool = Executors.newFixedThreadPool(4);

        // (2) A Callable models one worker's batch: it serves up to 50 requests,
        //     stopping early when the shutdown flag is raised.
        Callable<Integer> serveOneBatch = () -> {
            int served = 0;
            while (!shuttingDown && served < 50) {     // reads the volatile flag
                servedRequests.incrementAndGet();      // atomic, lock-free increment
                Thread.sleep(2);                       // pretend to do I/O
                served++;
            }
            return served;                             // Callable returns a result
        };

        // (3) Submit five batches; each returns a Future immediately.
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futures.add(pool.submit(serveOneBatch));
        }

        Thread.sleep(200);                             // let requests flow for a while
        shuttingDown = true;                           // (4) volatile flag stops new work

        // (5) Collect each batch's result via Future.get().
        int totalBatches = 0;
        for (Future<Integer> f : futures) {
            totalBatches += f.get();
        }

        pool.shutdown();                               // (6) no new tasks, wait for finish
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("Requests served this run: " + servedRequests.get());
        System.out.println("Batches completed:        " + totalBatches);
    }
}
```

**Why each tool was chosen:**

1. **`ExecutorService` (fixed pool of 4)** — we don't create a thread per request (that's wasteful); the pool reuses worker threads and caps resource usage at 4 concurrent workers.
2. **`Callable<Integer>`** — each batch *produces a result* (how many requests it served), which `Runnable` can't return; the `Future` is how that result travels back.
3. **`AtomicInteger servedRequests`** — many threads increment the same counter concurrently. It needs atomicity (read-modify-write) plus visibility, and CAS gives both without the parking overhead of locks.
4. **`volatile boolean shuttingDown`** — written by *one* thread (`main`) and read by *many* (the workers). That's the textbook case for `volatile`: a visibility-only flag, no read-modify-write involved, so atomicity is not required.
5. **`Future.get()`** — blocks `main` until each batch finishes, so results are joined before we report totals.
6. **`shutdown()` + `awaitTermination()`** — the graceful-shutdown ritual for an executor: stop accepting new work, wait up to 5 seconds for in-flight tasks, then move on.

The interesting detail: the sum of the batch sizes is **not** necessarily equal to the atomic counter, because the `while` condition reads the volatile flag *before* the increment — a worker can see `shuttingDown == true`, exit its loop, and the total served is simply whatever happened before the flag took effect. That's exactly the behavior a real server wants: an in-flight request completes; a new one may or may not start.

> **Real-world use:** This is a miniature version of how real request-serving systems are built: a bounded thread pool accepts tasks, each task returns a result via `Future`, an atomic counter tracks global metrics (requests served, active requests, error counts), and a volatile shutdown flag lets operators drain the server gracefully during deployments. The pattern scales from a toy simulator to a production HTTP listener — the concurrency *tools* are identical; only the surrounding framework changes.

---

## 8. Summary & Common Pitfalls

**What you've learned:**

- A **thread** is a flow of execution sharing the process's heap; create work with `Runnable`/`Callable`, start it with `start()`, and wait with `join()`.
- An **`ExecutorService`** manages reusable worker threads; `Future.get()` retrieves a `Callable`'s result.
- **Data races** come from interleaved read-modify-write; `synchronized` and locks make critical sections atomic using monitor locks.
- **Deadlocks** arise from inconsistent lock ordering; prevent with ordering rules and `tryLock` timeouts.
- **`volatile`** gives visibility for flags (write by one thread, read by others) but never atomicity.
- **Atomic classes** (CAS) give lock-free atomicity for single fields; `LongAdder` shards counters under heavy contention.

### Most common beginner mistakes

- **Calling `run()` instead of `start()`.** The code "works" but runs on the calling thread — no concurrency happens.
- **Swallowing `InterruptedException`.** Catch it, do nothing, and the interrupt request vanishes. Restore the flag (`Thread.currentThread().interrupt()`) or rethrow.
- **Using `volatile` for atomicity.** `volatile int count; count++;` still loses updates. `volatile` is visibility only.
- **Forgetting `try/finally` around locks.** If protected code throws, the `unlock()` is skipped and other threads block forever.
- **Never shutting down the pool.** An `ExecutorService` with default threads keeps the JVM alive; call `shutdown()` when done.
- **Sharing mutable state without any protection.** Two threads writing the same `ArrayList`, `HashMap`, or `SimpleDateFormat` instance → corrupted data or exceptions.
- **Believing "it ran fine once."** Race conditions are nondeterministic; a correct program must be *provably* safe, not "usually" safe.
- **Holding a lock while blocking or sleeping.** You're forcing every other thread to wait through your I/O; hold locks only around the critical section.
- **Starting a thread per task for thousands of tasks.** Thread creation is expensive; use a pool.

### If you see X, the likely cause is Y

| Symptom | Likely cause |
|---|---|
| Output order changes on every run | Threads interleaving — normal for concurrency, but a bug if you depended on order |
| Counter ends up *less* than expected (never more) | Lost updates: unprotected read-modify-write (`count++`) |
| Counter is sometimes correct, sometimes not | Same race, in a less-contended spot — still a bug |
| Program hangs with no output | Deadlock (each thread waits on the other's lock) or a missed `unlock()` |
| Worker ignores a stop flag and keeps running | Missing `volatile` — the flag change isn't visible |
| `join()` or `get()` never returns | The thread it waits on is stuck (deadlock), still running, or never `interrupt()`ed |
| Threads spin at 100% CPU under load | Tight CAS retry loop under heavy contention — consider `LongAdder` or back-off |
| An `ExecutionException` whose `getCause()` is `NullPointerException` | The task itself threw NPE; unwrap with `getCause()` |
| Values are corrupted (wrong IDs, torn data) | Shared mutable object (e.g., a `HashMap`) without synchronization |
| The app won't shut down after work finishes | A non-daemon executor/thread still running — `shutdown()` or mark threads daemon |

### The three rules to live by

1. **Never share mutable state without a plan** — use `synchronized`/locks, or make the state thread-safe with atomic classes, or make it immutable.
2. **Match the tool to the problem** — `volatile` for visibility-only flags, atomic classes for single-field counters, `synchronized` for multi-statement critical sections.
3. **Clean up everything** — release locks in `finally`, `shutdown()` your pools, re-set interrupted flags, and `join()` the threads you start.

Concurrency is the difference between a program that idles on eight idle cores and one that uses all of them. Master these foundations — threads, tasks, synchronization, `volatile`, and atomic classes — and you can write Java that is both *fast* and *correct*.