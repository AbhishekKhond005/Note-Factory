## 1. Introduction

Imagine a restaurant kitchen on a busy Saturday night. One chef working alone would peel the onions, then chop the carrots, then sear the steak, then plate the dish — one task at a time, in strict order. Customers would wait forever. A real kitchen instead has several chefs working **simultaneously**: while one boils pasta, another grills fish, and a third assembles desserts. Each chef works independently on their own station, but they all share the same kitchen, the same ingredients, and the same goal: getting plates out on time.

A Java **thread** is exactly that chef. A thread is an independent path of execution that runs concurrently with other threads inside a single Java program. The program (the kitchen) has its own memory, resources, and goal, but it can split its work among multiple threads (the chefs) to get things done faster and keep the whole system responsive.

**Why concurrency matters:**

- Modern CPUs have many cores; a single thread uses only one. To exploit the hardware, you must split work across threads.
- I/O operations (reading a file, querying a database, waiting on a network response) block the CPU for milliseconds or longer. While one thread waits, other threads can keep the CPU busy.
- Users expect responsiveness. A web server that handles each request in its own thread can serve thousands of users at once; an app that freezes its UI while "thinking" feels broken.

**Where this chapter fits in your skill set:** "Thread basics" is the foundation of the entire Java concurrency stack. Everything that follows in advanced concurrency — `synchronized`, `volatile`, locks, thread pools, `CompletableFuture`, and reactive programming — builds on your understanding of how threads are created, how they live and die, and how they can be controlled. If you master the basics here, the advanced material becomes an extension of ideas you already know rather than a wall of new concepts.

**Learning objectives.** By the end of this chapter, you will be able to:

- Create threads using the `Thread` class, the `Runnable` interface, and the modern `Callable` + `ExecutorService` approach.
- Explain and observe the six thread lifecycle states (`NEW`, `RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`, `TERMINATED`).
- Understand thread priorities as platform-dependent hints — and know why they should never be used to enforce correctness.
- Use `Thread.sleep()`, `Thread.join()`, and `Thread.yield()` correctly, including proper handling of `InterruptedException`.
- Recognize common real-world use cases and pitfalls, and choose the idiomatic tool for the job.

---

## 2. Creating Threads

Java gives you several ways to start concurrent work. The three main approaches are: extending `Thread`, implementing `Runnable`, and using `Callable` with an `ExecutorService`. All three ultimately run your code on a thread — the difference is in *how* the code is packaged and *how much* control the framework gives you.

### 2.1 The `Thread` Class

The most direct approach is to subclass `Thread` and override its `run()` method. When you call `start()`, the JVM creates a new native thread that executes your overridden `run()`.

```java
public class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Hello from thread: " + getName());
    }

    public static void main(String[] args) {
        MyThread t = new MyThread();
        t.start();          // schedules the new thread to run
        System.out.println("Hello from main: " + Thread.currentThread().getName());
    }
}
```

**Step by step:**

1. `MyThread extends Thread` — we inherit the entire thread machinery, including `start()`, `getName()`, and the lifecycle bookkeeping.
2. We override `run()` — this is where our custom work lives. `run()` is the "recipe" the new thread executes.
3. `t.start()` — this is critical. `start()` creates the new operating-system thread and begins executing `run()` **on that new thread**. It returns immediately.
4. The `main` method continues concurrently on the main thread.

**Expected output** (order is *not* guaranteed; the two threads race):

```
Hello from main: main
Hello from thread: Thread-0
```

> ⚠️ **Why this is rarely preferred.** A Java class can extend only one superclass. Once you make `Thread` your superclass, you forfeit the ability to extend anything else — a heavy price for what is, in essence, just a `run()` method. Extending `Thread` also couples your business logic to the threading infrastructure, which makes your code harder to test and reuse. Extending `Thread` is taught here only because you will encounter it in legacy code; treat it as the approach to *understand*, not to *use*.

### 2.2 The `Runnable` Interface

The cleaner, preferred approach is to separate the *work* from the *worker*. Implement the `Runnable` interface — a single abstract method, `void run()` — and pass the `Runnable` object to a `Thread` constructor. The thread does the threading; your object does the work.

```java
public class RunnableExample {
    public static void main(String[] args) {
        Runnable task = new GreetingTask("Alice");
        Thread t = new Thread(task);
        t.start();

        System.out.println("Main thread is free to do other work: "
                + Thread.currentThread().getName());
    }
}

class GreetingTask implements Runnable {
    private final String name;

    GreetingTask(String name) {
        this.name = name;
    }

    @Override
    public void run() {
        System.out.println("Hello, " + name + " — running on thread: "
                + Thread.currentThread().getName());
    }
}
```

**Step by step:**

1. `GreetingTask implements Runnable` — the class now carries a `run()` method describing the work. Note the class *is not* a thread; it is merely a task.
2. `new Thread(task)` — the `Thread` wrapper is given the `Runnable` to execute when it starts.
3. `t.start()` — the new thread runs `task.run()`.
4. Because the work object is a plain class, it can still extend other classes and be unit-tested without any thread involved (just call `task.run()` directly).

**Expected output** (order not guaranteed):

```
Main thread is free to do other work: main
Hello, Alice — running on thread: Thread-0
```

> 💡 **Why this matters.** Decoupling the *task* from the *executor* is the single most important conceptual step in this chapter. It is exactly this separation that later lets you hand a `Runnable` not just to a `Thread`, but to thread pools, scheduled executors, and GUI event queues — without changing the task at all.

### 2.3 The `Callable<T>` + `ExecutorService` Approach

`Runnable.run()` has a fundamental limitation: it returns `void` and cannot throw checked exceptions. Modern Java threading therefore favors **tasks that return a value** using `Callable<T>`:

```java
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CallableExample {
    public static void main(String[] args) {
        // A thread pool with exactly one worker thread.
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Callable<Integer> task = () -> {
            int result = 6 * 7;
            // Simulate slow computation.
            Thread.sleep(500);
            return result;
        };

        // submit() hands the task to the pool and returns a Future immediately.
        Future<Integer> future = executor.submit(task);

        System.out.println("Task submitted; main thread keeps working...");

        try {
            // get() blocks until the task completes, then retrieves its value.
            Integer answer = future.get();
            System.out.println("Answer from the future: " + answer);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // re-set the interrupt flag
            System.out.println("Interrupted while waiting for the result.");
        } catch (ExecutionException e) {
            System.out.println("The task threw an exception: " + e.getCause());
        } finally {
            executor.shutdown();   // stop accepting new tasks, then exit
        }
    }
}
```

**Step by step:**

1. `ExecutorService executor = Executors.newSingleThreadExecutor();` — creates a managed pool of one worker thread. The pool owns the thread; you never touch raw `Thread` objects again.
2. `Callable<Integer> task = ...` — a task that returns an `Integer`. Because it is a `Callable`, its body may throw checked exceptions too.
3. `executor.submit(task)` — enqueues the task and immediately returns a `Future<Integer>`, a handle to the *eventual* result.
4. `future.get()` — blocks the current thread until the computation finishes, then unwraps the value. If the task threw, `get()` rethrows it wrapped in an `ExecutionException`; if the waiting thread was interrupted, it throws `InterruptedException`.
5. `executor.shutdown()` — an orderly shutdown. A thread pool's threads are **non-daemon**, so forgetting `shutdown()` would keep the JVM alive after `main` ends.

**Expected output:**

```
Task submitted; main thread keeps working...
Answer from the future: 42
```

> 💡 **Why this matters.** In production code, you will almost never create threads manually. The `ExecutorService` machinery gives you pooled threads, value-returning tasks, cancellation, and bounded resource usage. This is the foundation of everything you will use in real systems — and it only makes sense because you first understood `Runnable` and `Future` as building blocks.

### 2.4 Anonymous Class and Lambda Alternatives

Since `Runnable` and `Callable` are **functional interfaces** (exactly one abstract method), they can be expressed concisely.

**Anonymous class** (pre-Java 8 style, still legal):

```java
public class AnonymousExample {
    public static void main(String[] args) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Running via anonymous class");
            }
        });
        t.start();
    }
}
```

**Lambda (modern, preferred):**

```java
public class LambdaExample {
    public static void main(String[] args) {
        // A Runnable is () -> void
        Thread t = new Thread(() -> System.out.println("Running via lambda"));
        t.start();

        // With an executor + lambda
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        executor.execute(() -> System.out.println("Task on a pool thread: "
                + Thread.currentThread().getName()));
        executor.shutdown();
    }
}
```

**Step by step:**

1. The lambda `() -> System.out.println(...)` is a compact `Runnable`: no arguments, `void` return, one expression body.
2. `executor.execute(...)` runs the task on one of the two pool threads. `execute` is the fire-and-forget method for `Runnable` (no result handle); use `submit` when you need a `Future`.

**Expected output:**

```
Running via lambda
Task on a pool thread: pool-1-thread-1
```

> 📌 **Note on style:** use lambdas for short, stateless tasks. If a task needs parameters or mutable state, prefer a named class or a local class — clarity beats brevity.

### Comparison of the Three Approaches

| Approach | Flexibility | Return Values | Checked Exceptions | Recommended Use |
|---|---|---|---|---|
| Extend `Thread` | Low — consumes your single inheritance slot | `void` only | Cannot throw | Legacy code; never for new work |
| Implement `Runnable` + `Thread` | High — class stays a plain class | `void` only | Cannot throw | Simple fire-and-forget tasks, embedded callbacks |
| `Callable<T>` + `ExecutorService` | Highest — decoupled, pooled, cancellable | `T` via `Future<T>` | Allowed inside the task; surfaced as `ExecutionException` | Production code: anything returning a result, anything needing pools/cancellation |

> ⚠️ **Warning.** Never call `t.run()` directly instead of `t.start()`. Calling `run()` executes the task *synchronously on the calling thread* — no new thread is created, and you lose concurrency entirely. The moment you call `run()` directly, you have a sequential program wearing a thread's clothing.

---

## 3. Thread Lifecycle

A thread is not born running, and it does not vanish abruptly. It passes through a well-defined set of states, tracked internally by the JVM and queryable at any time via `Thread.getState()`. Understanding this lifecycle is the key to reasoning about *why* your program behaves the way it does.

```
                    ┌──────────────┐
                    │     NEW      │
                    └──────┬───────┘
                           │ start()
                           ▼
                    ┌──────────────┐      scheduler gives CPU
                    │  RUNNABLE    │◄──────────────────────┐
                    └──────┬───────┘                       │
          ┌───────────────┼────────────────┐               │
          │               │                │               │
   acquires lock    sleep/join/     sleep(ms)/
   fails (synch)    park (no time)  join(ms)/
          │               │                │
          ▼               ▼                ▼
   ┌───────────┐   ┌────────────┐   ┌───────────────┐
   │  BLOCKED  │   │  WAITING   │   │ TIMED_WAITING │
   └─────┬─────┘   └─────┬──────┘   └───────┬───────┘
          │               │                 │
          └───────────────┼─────────────────┘
                          │ lock acquired / timeout /
                          │ notify / interrupt
                          ▼
                    ┌──────────────┐
                    │  RUNNABLE    │
                    └──────┬───────┘
                           │ run() returns / throws
                           ▼
                    ┌──────────────┐
                    │  TERMINATED  │
                    └──────────────┘
```

**The six states, in plain language:**

- **`NEW`** — the thread object is created but `start()` has not been called. Analogy: a chef hired but not yet clocked in; standing in the break room.
- **`RUNNABLE`** — the thread is *ready* to run or *currently running*. The JVM sees it as runnable; whether it has the CPU at any instant is the operating system scheduler's decision. Analogy: the chef is at the station, ready to cook — sometimes actively cooking, sometimes briefly waiting for the stove to free up.
- **`BLOCKED`** — the thread wants a monitor (lock) that another thread currently holds. It sits in the "waiting room" until the lock is released. Analogy: one chef wants the only oven, but another chef is using it.
- **`WAITING`** — the thread waits indefinitely for another thread's explicit signal (e.g., `Object.wait()`, `Thread.join()` without a timeout). Analogy: a chef waiting to be told the sauce is ready — with no time limit.
- **`TIMED_WAITING`** — like `WAITING`, but with a timeout; the thread will wake up on its own (`Thread.sleep(ms)`, `join(ms)`, `wait(ms)`). Analogy: a chef who sets a 10-minute timer and will check back regardless.
- **`TERMINATED`** — `run()` returned (or threw). The thread is finished and can never be restarted. Analogy: the chef clocked out at the end of shift.

**Observing the lifecycle in code.** `Thread.getState()` returns the current state as a `Thread.State` enum. The following complete example prints each state as a thread moves through its lifecycle:

```java
public class LifecycleExample {
    public static void main(String[] args) throws InterruptedException {
        // 1) NEW: object created, start() not yet called.
        Thread t = new Thread(() -> {
            System.out.println("  [inside run] state: " + Thread.currentThread().getState());
            try {
                Thread.sleep(400);   // -> TIMED_WAITING
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("  [inside run] state before finishing: "
                    + Thread.currentThread().getState());
        });

        System.out.println("After creation         : " + t.getState());      // NEW

        t.start();
        System.out.println("After start()          : " + t.getState());      // RUNNABLE (usually)

        Thread.sleep(50);   // let the worker actually begin running
        System.out.println("While sleeping in task : " + t.getState());      // TIMED_WAITING

        t.join();           // main waits for t to finish
        System.out.println("After join returns     : " + t.getState());      // TERMINATED
    }
}
```

**Step by step:**

1. The thread is created but never started — its state is `NEW`.
2. After `start()`, the thread is eligible to run — `RUNNABLE`. (We print it so quickly that it will almost always still be `RUNNABLE`; there is no guarantee the worker has actually executed yet.)
3. `Thread.sleep(50)` in `main` gives the worker time to start and call its own `sleep(400)`, putting it in `TIMED_WAITING`.
4. `t.join()` blocks `main` until the worker completes. Once it has, the worker's state is `TERMINATED` — permanent.

**Expected output:**

```
After creation         : NEW
After start()          : RUNNABLE
  [inside run] state: RUNNABLE
While sleeping in task : TIMED_WAITING
  [inside run] state before finishing: RUNNABLE
After join returns     : TERMINATED
```

> ⚠️ **Note on `RUNNABLE` vs. actually running.** Java does not expose the distinction between "ready" (waiting for a CPU core) and "running" (executing on a core). Both are reported as `RUNNABLE`. So a `RUNNABLE` thread might be making progress — or it might be starving in the scheduler's queue.

**State transition table:**

| State | Triggered By | Transitions To |
|---|---|---|
| `NEW` | `new Thread(...)` | `RUNNABLE` (via `start()`) |
| `RUNNABLE` | `start()`; acquiring a lock; timeout elapsed; `notify()`/`notifyAll()`; interrupt | `BLOCKED`, `WAITING`, `TIMED_WAITING`, or `TERMINATED` (via `run()` returning) |
| `BLOCKED` | Entering a `synchronized` block/`Object.wait()` while another thread holds the lock | `RUNNABLE` (once the lock is acquired) |
| `WAITING` | `Object.wait()` (no timeout), `Thread.join()` (no timeout), `LockSupport.park()` | `RUNNABLE` (via `notify`/`notifyAll`, the joined thread finishing, or `unpark`) |
| `TIMED_WAITING` | `Thread.sleep(ms)`, `join(ms)`, `wait(ms)`, `LockSupport.parkNanos()` | `RUNNABLE` (via timeout, notify, or interrupt) |
| `TERMINATED` | `run()` returns or throws | none — the state is final |

---

## 4. Thread Priorities

The JVM allows you to suggest a scheduling preference for each thread using an integer between `Thread.MIN_PRIORITY` (1) and `Thread.MAX_PRIORITY` (10). The default is `Thread.NORM_PRIORITY` (5).

| Priority Constant | Value | Use Case / Caveat |
|---|---|---|
| `Thread.MIN_PRIORITY` | 1 | Background, non-urgent housekeeping. **Caveat:** on some platforms it is ignored entirely. |
| `Thread.NORM_PRIORITY` | 5 | The default for every thread. Normal application work. **Caveat:** it is exactly the "no special treatment" value. |
| `Thread.MAX_PRIORITY` | 10 | Urgent, short tasks. **Caveat:** an OS may not have 10 priority levels and will map them; a high-priority thread can also *starve* lower ones, which is usually undesirable. |

**A complete example creating threads at different priorities:**

```java
public class PriorityExample {
    public static void main(String[] args) {
        Thread low = new Thread(() -> System.out.println("Low-priority task ran"));
        Thread high = new Thread(() -> System.out.println("High-priority task ran"));

        low.setPriority(Thread.MIN_PRIORITY);   // 1
        high.setPriority(Thread.MAX_PRIORITY);  // 10

        System.out.println("Default main priority : " + Thread.currentThread().getPriority()); // 5
        System.out.println("Low thread priority   : " + low.getPriority());   // 1
        System.out.println("High thread priority  : " + high.getPriority());  // 10

        high.start();
        low.start();
    }
}
```

**Step by step:**

1. `setPriority(...)` stores the requested priority on the thread object.
2. `getPriority()` reads it back — note that this returns what *we* asked for, which may differ from what the OS actually granted.
3. Both threads start. The JVM may map priority 10 to a different OS-level value, and the scheduler may ignore the hint entirely.

**Expected output (order of the two tasks is *not* guaranteed, even with the priorities set):**

```
Default main priority : 5
Low thread priority   : 1
High thread priority  : 10
High-priority task ran
Low-priority task ran
```

> ⚠️ **The big warning.** Priorities are **platform-dependent hints, not guarantees**. Windows has 7 effective levels, most Linux schedulers ignore Java priorities outright, and even where honored, they only bias the scheduler — they never force ordering. **Never use priorities to make your program correct.** A program that works only "because the high-priority thread always runs first" is a program that will fail randomly in production.

> 💡 **Why this matters.** Correctness must come from *coordination mechanisms* — locks, `join()`, `Future.get()`, latches, barriers — never from hoping the scheduler favors a thread. Priorities are at best a coarse performance hint for niche, time-sensitive tasks.

---

## 5. Thread Join and Sleep

### 5.1 `Thread.sleep()`

`sleep(long millis)` pauses the *current* thread for approximately the given number of milliseconds, putting it into `TIMED_WAITING`. It exists so threads can wait out a delay — simulating slow work, pacing a loop, or giving other threads a chance to make progress.

```java
public class SleepExample {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Before sleep: " + System.currentTimeMillis());

        Thread.sleep(1000);   // raw milliseconds

        System.out.println("After raw sleep: " + System.currentTimeMillis());

        // Modern alternative: TimeUnit for readability.
        java.util.concurrent.TimeUnit.SECONDS.sleep(1);

        System.out.println("After TimeUnit sleep: " + System.currentTimeMillis());
    }
}
```

**Key points:**

- `sleep()` throws **`InterruptedException`**, a checked exception. Your code must either declare `throws InterruptedException` (as above) or catch it and handle the interruption — see the pitfall section.
- `sleep(0)` is a legal call that yields the CPU to other threads — a "polite pause."
- **`TimeUnit.SECONDS.sleep(1)`** is preferred over `sleep(1000)` because the unit is self-documenting. `TimeUnit` also offers `MILLISECONDS`, `MICROSECONDS`, `NANOSECONDS`, `MINUTES`, and `HOURS`.

> ⚠️ **Warning.** `sleep()` does *not* release any locks you hold. If you call `sleep()` inside a `synchronized` block, other threads still cannot enter that block — you are holding the lock while napping.

### 5.2 `Thread.join()`

`join()` makes the *calling* thread wait until the thread it joins finishes. Think of it as the main course waiting for the dessert chef: "I will not plate until you are done." This is how you create **deterministic ordering** between threads.

```java
public class JoinExample {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            System.out.println("Worker starting...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Worker finished.");
        });

        worker.start();

        System.out.println("Main waiting for worker to finish...");
        worker.join();                  // block until worker terminates
        System.out.println("Main continues after worker finished.");
    }
}
```

**Step by step:**

1. `worker.start()` launches the worker; `main` proceeds immediately.
2. `worker.join()` blocks `main` (state `WAITING`) until the worker's `run()` returns.
3. After `join()` returns, `main` resumes — and it is now *guaranteed* that the worker finished.

**Expected output (deterministic, in this order):**

```
Main waiting for worker to finish...
Worker starting...
Worker finished.
Main continues after worker finished.
```

**Overloads:**

- `join()` — wait indefinitely for the thread to die.
- `join(long millis)` — wait up to `millis`; return anyway if the thread is still alive. Use this when you cannot afford to wait forever.
- `join(long millis, int nanos)` — more precise timeout.

### 5.3 `Thread.yield()`

`yield()` is a hint to the scheduler: "I am done with my current time slice; you may run another thread of equal priority." It is **not** a guarantee that anything else will run, and overuse makes programs non-deterministic and slow. In modern Java it is effectively obsolete for production code — scheduler behavior, OS-dependent, makes its effect unpredictable.

> 💡 **Why this matters.** Knowing *why* `yield()` is discouraged is part of understanding the scheduler. It can never make your program correct, only (sometimes) marginally fairer. If you find yourself sprinkling `yield()` to fix a race condition, stop and use a real coordination primitive instead.

### Combined `join()` + `sleep()` example with deterministic ordering

Here is a complete program that uses `sleep()` for pacing and `join()` to force a strict sequence — like three chefs who must finish in order because each dish depends on the previous one:

```java
import java.util.concurrent.TimeUnit;

public class JoinSleepExample {
    public static void main(String[] args) throws InterruptedException {
        Thread step1 = new Thread(() -> work("Step 1", 300));
        Thread step2 = new Thread(() -> work("Step 2", 200));
        Thread step3 = new Thread(() -> work("Step 3", 100));

        step1.start();
        step1.join();      // Step 2 cannot begin before Step 1 ends.

        step2.start();
        step2.join();      // Step 3 cannot begin before Step 2 ends.

        step3.start();
        step3.join();

        System.out.println("All steps completed in order.");
    }

    private static void work(String label, long delayMillis) {
        System.out.println(label + " started");
        try {
            TimeUnit.MILLISECONDS.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println(label + " finished");
    }
}
```

**Step by step:**

1. Three threads are created but only *started* one at a time.
2. `step1.join()` blocks `main` until step 1 completes. Only then is `step2.start()` reached, and so on.
3. `TimeUnit.MILLISECONDS.sleep(delayMillis)` in `work(...)` gives each step a distinct, non-trivial duration so the ordering is clearly observable.

**Expected output (fully deterministic):**

```
Step 1 started
Step 1 finished
Step 2 started
Step 2 finished
Step 3 started
Step 3 finished
All steps completed in order.
```

> 💡 **Why this matters.** The pattern above — start a dependent thread only after `join()`ing its prerequisite — is how you express *happens-before* relationships in the simplest form. Real systems replace the manual chain with `Future` chains or `CompletableFuture`, but the mental model is identical: **"wait for X, then do Y."**

**Method reference table:**

| Method | Signature | Behavior | Throws |
|---|---|---|---|
| `Thread.sleep` | `static void sleep(long millis)` | Pause the *current* thread for ~`millis` ms (`TIMED_WAITING`); does not release locks | `InterruptedException` |
| `Thread.sleep` | `static void sleep(long millis, int nanos)` | As above with nanosecond refinement | `InterruptedException` |
| `Thread.join` | `void join()` | Block the *calling* thread until the joined thread terminates | `InterruptedException` |
| `Thread.join` | `void join(long millis)` | Wait at most `millis` ms, then return regardless | `InterruptedException` |
| `Thread.join` | `void join(long millis, int nanos)` | Timeout with nanosecond refinement | `InterruptedException` |
| `Thread.yield` | `static void yield()` | Hint the scheduler it may run another thread; no guarantee | — |
| `TimeUnit.X.sleep` | `void sleep(long value)` | `sleep()` in a self-documenting unit (e.g., `SECONDS.sleep(1)`) | `InterruptedException` |

---

## 6. Real-World Context and Use Cases

**Where thread basics show up in real systems:**

- **Web servers.** Every incoming HTTP request is typically handled on its own thread (or, more precisely, its own *task* on a pooled thread). One slow request must not block every other user. Tomcat, Jetty, and Netty all build on exactly the `Runnable`/`ExecutorService` model from Section 2.
- **UI event loops.** Desktop apps (Swing, JavaFX) run the UI on the dedicated "Event Dispatch Thread." Long-running work must be pushed to background threads so the window stays responsive and the user is never staring at a frozen screen.
- **Background jobs.** Report generation, batch emailing, log flushing, and scheduled cleanups run on separate threads so the main application never waits on them.
- **Parallel data processing.** Large arrays, image filters, or batch calculations split across cores — each chunk processed by its own thread, results combined afterward (the `Future.get()` pattern).

**Common pitfalls and best practices:**

1. **Favor `Runnable`/`Callable` + `ExecutorService` over extending `Thread`.** You keep your inheritance, your task stays testable, and you get pooling and lifecycle management for free.
2. **Never call `Thread.stop()`, `Thread.destroy()`, or `Thread.suspend()`/`resume()`.** These are deprecated (or long removed) because they can leave shared data in a corrupted half-written state. To stop work, use cooperative interruption — set the interrupt flag and let the task check it.
3. **Never mutate shared state without synchronization.** Two threads incrementing the same `int` without `synchronized` (or `AtomicInteger`) will occasionally lose updates. Thread basics gets you concurrency; the *synchronization* tools you meet next keep it safe.
4. **Always handle `InterruptedException` correctly.** Catching it and doing nothing swallows the interruption signal and can leave your program unable to shut down. The two acceptable responses are: re-throw if your method can, or re-interrupt with `Thread.currentThread().interrupt()`.
5. **Prefer higher-level utilities beyond the basics.** Once you need coordination, reach for `Future`, `CompletableFuture`, thread pools, `CountDownLatch`, `Semaphore`, or `ConcurrentHashMap` rather than hand-rolling `wait`/`notify` logic.

> 🚨 **Common Mistakes callout box.**
> - **Mistake:** calling `t.run()` instead of `t.start()` — silently runs on the current thread, no concurrency.
> - **Mistake:** relying on `setPriority()` to enforce ordering — it is a hint, not a guarantee.
> - **Mistake:** swallowing `InterruptedException` in an empty catch block — the thread's interruption signal is lost.
> - **Mistake:** calling `join()` from within the thread that would be joined — `t.join()` inside `t`'s own `run()` deadlocks.
> - **Mistake:** starting a thread twice — `IllegalThreadStateException`.
> - **Mistake:** forgetting `executor.shutdown()` — the JVM may never exit because pool threads are non-daemon.

---

## 7. Chapter Summary

**Key concepts recap:**

- A **thread** is an independent path of execution within one program; the JVM schedules runnable threads against available CPU cores.
- Create work three ways: extend `Thread` (understand, avoid), implement `Runnable` (simple, flexible), or use `Callable<T>` + `ExecutorService` (production default, value-returning, pooled). Use lambdas for short tasks.
- Threads traverse six lifecycle states — `NEW` → `RUNNABLE` → (`BLOCKED`/`WAITING`/`TIMED_WAITING`) → `RUNNABLE` → `TERMINATED` — observable via `Thread.getState()`.
- Priorities (1–10) are scheduler hints with no correctness guarantees; `NORM_PRIORITY` (5) is the default.
- `sleep()` pauses the current thread (unit-aware `TimeUnit` preferred); `join()` forces one thread to wait for another; `yield()` is a rarely useful hint.
- Correctness comes from coordination, not from scheduling luck.

**Key terms glossary:**

| Term | Definition |
|---|---|
| **thread** | An independent, concurrently executing path of program flow within a single process |
| **runnable** | (a) The `Runnable` interface with a single `void run()` method; (b) the lifecycle state meaning ready-or-running |
| **lifecycle** | The set of states a thread passes through from creation to termination |
| **`NEW`** | Created but not yet started |
| **`TERMINATED`** | `run()` has returned; final state |
| **priority** | An integer hint (1–10) suggesting scheduling preference; not a guarantee |
| **join** | Block the calling thread until the target thread dies |
| **sleep** | Pause the current thread for a duration |
| **yield** | Hint the scheduler that another thread may run |
| **`InterruptedException`** | Checked exception thrown when a waiting thread is interrupted |
| **`ExecutorService`** | A managed pool of threads that executes submitted `Runnable`/`Callable` tasks |
| **`Future<T>`** | A handle to a result that may not be ready yet; `get()` blocks for it |
| **daemon thread** | A background thread that does not keep the JVM alive |

---

## 8. Practice Exercises

**Exercise 1 — Lifecycle trace (easy).** Write a program that starts a thread whose `run()` calls `sleep(500)`. In `main`, print the worker's state immediately after `start()`, then repeatedly every 50 ms until it is `TERMINATED`, recording the sequence of observed states. Compare with the expected lifecycle from Section 3. What do you observe about `RUNNABLE` and `TIMED_WAITING` alternating?

**Exercise 2 — Dependent ordering (easy-to-moderate).** Using three `Thread`s and `join()`, print the numbers 1 through 3 strictly in order from three different threads, where the thread printing 3 has the longest `sleep()`. Explain why priorities alone could not achieve this.

**Exercise 3 — Build a mini thread pool (moderate).** Without `ExecutorService`, build a tiny pool: a fixed array of worker `Thread`s, each looping forever, pulling `Runnable`s from a shared `Queue<Runnable>`. Submit five tasks and print the thread name that executed each. (Hint: handle empty-queue waiting without busy-spinning — a simple `synchronized`/`wait`/`notifyAll` pattern is fine.)

**Exercise 4 — Priority misconception (moderate).** Run the Section 4 example 100 times in a loop and count how often the high-priority task prints before the low-priority one. On most platforms you will see both orders. Write a short paragraph explaining why this result is expected and why a correctness-critical program must not depend on priorities.

**Exercise 5 — Interrupted sleep (advanced).** Start a thread that loops calling `TimeUnit.SECONDS.sleep(1)` forever. From `main`, call `t.interrupt()` after 2.5 seconds. Inside the loop, handle `InterruptedException` by re-interrupting and breaking. Print the state of the thread just before the interrupt fires. Verify the thread terminates cleanly.

### Fully solved sample exercise (Exercise 1)

```java
public class LifecycleTrace {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        System.out.println("state after creation : " + worker.getState());   // NEW
        worker.start();
        System.out.println("state after start()  : " + worker.getState());   // RUNNABLE

        // Poll the state every 50 ms until the worker is finished.
        while (worker.getState() != Thread.State.TERMINATED) {
            System.out.println("observed state        : " + worker.getState());
            Thread.sleep(50);
        }
        System.out.println("state at the end     : " + worker.getState());   // TERMINATED
    }
}
```

**Step by step:**

1. After creation the thread is `NEW` — no OS thread exists yet.
2. Immediately after `start()`, the worker is `RUNNABLE` (ready, and very likely already running given how little time has passed).
3. The polling loop samples `getState()` every 50 ms. Once the worker enters its own `sleep(500)`, the observed states switch from `RUNNABLE` to `TIMED_WAITING`. The worker might also be `RUNNABLE` between polls if it has already resumed — sampling can *miss* states, which is an important insight.
4. After about 500 ms the worker's `run()` returns, the loop exits, and the final state is `TERMINATED`.

**Expected output (state order is illustrative; sampling may vary):**

```
state after creation : NEW
state after start()  : RUNNABLE
observed state        : RUNNABLE
observed state        : TIMED_WAITING
observed state        : TIMED_WAITING
observed state        : TIMED_WAITING
observed state        : RUNNABLE
observed state        : RUNNABLE
state at the end     : TERMINATED
```

**Key takeaway from this exercise:** `getState()` is a *snapshot in time*. A thread can pass through `RUNNABLE`, `BLOCKED`, and `WAITING` between two of your polls and you will never see those states — the lifecycle describes real transitions, but observation is sampling, not a film strip.

---

Now you have the complete foundation: you can create threads four idiomatic ways, predict and observe their lifecycle, understand what priorities actually (and don't) do, and control ordering with `join()` and `sleep()`. Every advanced Java concurrency topic you meet next — synchronization, thread pools, futures, and beyond — is built directly on these basics.