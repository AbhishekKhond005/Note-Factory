# Executors: Thread Pools, ExecutorService, Futures, and Scheduled Execution

## Learning Objectives

After reading this chapter, you will be able to:

- Explain the costs of the spawn-per-task `new Thread` approach and justify when pooled execution is the correct design.
- Distinguish between `Executor`, `ExecutorService`, and `ScheduledExecutorService`, and choose the right abstraction for a given problem.
- Construct each of the four standard thread pools from `Executors`, describe its work-queue semantics, and justify a choice using the comparison table in this chapter.
- Submit work with `execute`, `submit`, `invokeAll`, and `invokeAny`, and retrieve results through `Future` with correct exception handling.
- Manage the executor lifecycle — `shutdown()`, `shutdownNow()`, and `awaitTermination()` — so that no threads are leaked and no tasks are silently dropped.
- Configure one-shot and periodic tasks with `schedule`, `scheduleAtFixedRate`, and `scheduleWithFixedDelay`, and explain precisely how fixed-rate and fixed-delay timing differ.
- Build a bounded custom `ThreadPoolExecutor` with an explicit saturation policy and tune pool size for CPU-bound versus I/O-bound workloads.

---

## Introduction & Motivation

Concurrency is what lets a program make progress on many things "at the same time." On modern hardware, every server ships with multiple CPU cores, and even a single-core machine is surrounded by slow peripherals — disks, databases, and networks — that idle a thread for milliseconds or seconds at a time. If a program executes its work strictly sequentially, every I/O wait is pure dead time. Asynchronous and concurrent execution exists to fill those gaps: while one task waits for a network response, another can compute, and a third can write to disk.

Consider the simplest possible concurrent program: a server that spawns a new `Thread` per incoming request.

```java
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class NaiveServer {
    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(8080)) {
            while (true) {
                Socket socket = server.accept();            // block until a client connects
                // PROBLEM: a brand-new thread per request — expensive and unbounded
                new Thread(() -> handle(socket)).start();
            }
        }
    }

    private static void handle(Socket socket) {
        try (socket) {                                      // Java 9+: try-with-resources on a parameter
            Thread.sleep(50);                               // simulate I/O-heavy request work
            System.out.println("Handled request on " + Thread.currentThread().getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();             // restore the interrupt flag
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }
}
```

This naive design has three serious flaws:

1. **Creation overhead.** Each `new Thread` allocates a fresh stack (typically 512 KB–1 MB of virtual memory) and requires OS-level bookkeeping. Creating 10,000 threads costs far more than the actual work many of them perform.
2. **Unbounded resource usage.** With no cap on thread count, a traffic spike of 100,000 requests creates 100,000 threads. The machine thrashes, swaps, and eventually throws `OutOfMemoryError` or crashes the process. The server fails exactly when it is needed most.
3. **Lifecycle boilerplate.** Every raw thread forces you to hand-roll interruption handling, result passing, exception capture, and shutdown coordination — code that is easy to get subtly wrong and impossible to test in isolation.

The `java.util.concurrent` framework, and in particular the **executor** family, exists to solve all three problems at once. Instead of spawning a thread per task, you *submit* tasks to an **ExecutorService**, which assigns them to a fixed, reusable set of worker threads and queues any overflow. Compare the naive server above with the same design built on an executor:

```java
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorServer {
    public static void main(String[] args) throws IOException {
        // A fixed pool caps concurrency and reuses its 8 worker threads.
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try (ServerSocket server = new ServerSocket(8080)) {
            while (true) {
                Socket socket = server.accept();
                pool.submit(() -> handle(socket));   // queue the task; a worker picks it up
            }
        } finally {
            pool.shutdown();                         // best-effort: stop accepting new work
        }
    }

    private static void handle(Socket socket) {
        // identical request logic as NaiveServer (not repeated here)
    }
}
```

The difference is qualitative, not cosmetic. Under a 100,000-request burst, the executor version services those requests *through only eight threads*, queues the excess, and degrades gracefully instead of collapsing. This chapter gives you the full toolkit to reason about and use executors well.

---

## Core Concepts

### The `Executor` Abstraction

At the top of the hierarchy sits the tiny `Executor` interface — a single method:

```java
public interface Executor {
    void execute(Runnable command);
}
```

That is the whole contract: *"here is a unit of work; run it, somehow."* It deliberately says nothing about how — not which thread, not when, not in what order. This decoupling is the framework's founding idea: **task submission is separated from task execution policy.** The caller no longer cares whether the work runs on a new thread, a pooled thread, the current thread, or a queue that is drained later.

`ExecutorService` extends `Executor` and adds the machinery that makes the abstraction useful: `submit` (for tasks that return values), bulk operations (`invokeAll`, `invokeAny`), and lifecycle management (`shutdown`, `shutdownNow`, `awaitTermination`). A third interface, `ScheduledExecutorService`, adds time-based execution.

> **Think of it like...** a restaurant. You (the caller) hand a waiter an order slip (a `Runnable`). You do not care which cook prepares it, how many cooks are on shift, or whether the kitchen queues orders during the dinner rush. The restaurant's *dispatch policy* decides. The `Executor` interface is the mailbox where you drop the slip; the policy behind it is the kitchen.

### Task Decomposition: `Runnable` vs. `Callable`

Executors accept two kinds of tasks:

- **`Runnable`** — represents work that returns nothing (its `run()` method returns `void`). Use it for fire-and-forget work: logging, sending an email, updating a cache.
- **`Callable<V>`** — represents work that *produces a result* (its `call()` method returns `V`). Use it whenever the task computes something you need back: fetching a URL's contents, computing a checksum, querying a database.

The crucial asymmetry between them is **checked exceptions.** `Runnable.run()` cannot throw a checked exception; `Callable.call()` declares `throws Exception`. This makes `Callable` the natural vehicle for error-prone work, because the executor framework captures whatever the task throws and delivers it back to the caller wrapped in an `ExecutionException` when the result is retrieved. We study that mechanism in the **Futures** section.

| Aspect | `Runnable` | `Callable<V>` |
|---|---|---|
| Method | `void run()` | `V call() throws Exception` |
| Returns | nothing | a value of type `V` |
| Checked exceptions | cannot declare | may throw (captured by the executor) |
| Idiomatic use | fire-and-forget side effects | computation whose result is needed |

### The Lifecycle of an `ExecutorService`

An executor service exists in one of three states, and transitions between them are strictly one-way:

1. **Running** — after creation, the service accepts new tasks and executes submitted ones. This is the only state in which `execute` and `submit` are permitted.
2. **Shutting down** — entered by calling `shutdown()`. The service stops accepting new tasks (further submissions throw `RejectedExecutionException`), but *already-submitted* tasks continue to completion. Queued but unstarted tasks are still executed.
3. **Terminated** — entered when all submitted tasks have finished and the worker threads have exited. `isTerminated()` returns `true`. A terminated executor is permanently done; it cannot be restarted.

A *second* shutdown mode exists: `shutdownNow()` also enters the shutting-down state, but additionally **interrupts running tasks** and **returns the queue of tasks that never started**, so the caller can handle them. The full lifecycle is managed in numbered steps:

1. **Create** the executor with a factory method or constructor.
2. **Submit** tasks — as many as you like; overflow waits in the work queue.
3. **Initiate shutdown** with `shutdown()` when no more work will be submitted.
4. **Await termination** with `awaitTermination(timeout, unit)` to block until the workers finish.
5. **Force shutdown** with `shutdownNow()` if the timeout expires, and restore the thread's interrupt flag if `awaitTermination` was interrupted while you were waiting.

We apply this exact sequence in the worked example in the **ExecutorService in Practice** section.

---

## Thread Pools

A **thread pool** is a set of worker threads that are created once and then *reused* to execute many tasks. When a task is submitted, the pool hands it to an idle worker if one exists; otherwise the task waits in a **work queue** until a worker frees up. The economic argument is simple and worth stating with numbers:

> Spawning a thread and tearing it down costs roughly **tens of microseconds to milliseconds** (OS scheduling, stack allocation, TLS setup). A typical HTTP request does useful work for **1–100 ms**. For short tasks, thread creation can be 10–50% of total cost. A pool of 8 threads reused across **10,000 tasks** amortizes creation cost over 10,000 tasks instead of paying it 10,000 times — and never lets the thread count exceed 8, no matter how violent the burst.

### The Four Standard Factory Methods

`Executors` provides static factories covering the four archetypal policies. A single paragraph cannot capture their differences; the table after them is the definitive reference, but here is the intuition for each.

**`newFixedThreadPool(n)`** — exactly `n` worker threads, always alive, backed by an *unbounded* `LinkedBlockingQueue`. If all `n` threads are busy, new tasks wait in the queue. This is the workhorse for request handling and bounded parallelism.

**`newCachedThreadPool()`** — starts with zero threads and grows on demand up to `Integer.MAX_VALUE`, reusing any thread that becomes idle within 60 seconds. It uses a `SynchronousQueue`, which has **no storage capacity**: a task is handed directly to an idle worker or, if none exists, a new thread is created. Perfect for short-lived, bursty workloads; dangerous for long-running tasks, because it can spawn unbounded threads.

**`newSingleThreadExecutor()`** — one worker thread behind an unbounded queue. Tasks run strictly one at a time, in submission order. If the worker dies from an unexpected exception, the pool transparently replaces it and continues. This is the framework's answer to "I need a serial, ordered stream of work, with a safety net."

**`newWorkStealingPool()`** — a `ForkJoinPool` with parallelism equal to the number of processors. Instead of a shared queue, each worker keeps a *deque* of tasks and can "steal" tasks from other workers' deques when idle — ideal for parallel, recursive, divide-and-conquer computation where workload is uneven.

### Work-Queue Semantics and Choosing a Pool

The work queue determines how tasks wait, and therefore how the pool behaves under load:

- **Unbounded queue** (`newFixedThreadPool`, `newSingleThreadExecutor`): no task is ever rejected, but memory grows without limit if producers outpace workers.
- **No storage** (`newCachedThreadPool`): tasks never wait; instead, new threads are created. Thread count is the only bound, and it is effectively unbounded.
- **Per-worker deques** (`newWorkStealingPool`): tasks are distributed for stealing; best when subtasks are unevenly sized.

| Factory method | Pool size | Queue | Reuse scenario | Example use case |
|---|---|---|---|---|
| `newFixedThreadPool(n)` | exactly `n`, never times out | unbounded `LinkedBlockingQueue` | steady, uniform workload with a hard concurrency cap | HTTP request handling on an 8-core server |
| `newCachedThreadPool()` | 0 initial, grows to `Integer.MAX_VALUE`; idle threads die after 60 s | `SynchronousQueue` (no storage) | many short-lived, bursty tasks | ephemeral fire-and-forget jobs, e.g., fanning out analytics events |
| `newSingleThreadExecutor()` | exactly 1 (auto-replaced on death) | unbounded `LinkedBlockingQueue` | strictly serialized processing | sequential log writes, sending a single ordered data stream |
| `newWorkStealingPool()` | parallelism = CPU count | per-worker deques (work stealing) | parallel recursive computation | image processing on regions of a large bitmap |

> **Think of it like...** waitstaff in a restaurant. A fixed pool is a kitchen with a *hired staff of four cooks*: busy cooks queue new orders at the pass, and nobody new is hired regardless of the rush. A cached pool is the same kitchen with a *temp-agency agreement*: during a rush it hires as many temps as needed, and lets them go after an hour without work. A single-thread executor is a kitchen with exactly *one* cook, who cooks every dish in the order received. And a work-stealing pool is a line of *self-organizing chefs* who grab dishes from each other's stations the moment their own is clear.

### Tuning Guidance

Pool size should be derived from what your tasks actually do, not from gut feel:

- **CPU-bound tasks** (pure computation, no I/O): size the pool to the number of cores. A larger pool only adds context-switching overhead and cache contention. `Runtime.getRuntime().availableProcessors()` is the standard starting point.
- **I/O-bound tasks** (network, disk, database): threads spend most of their life blocked, so you can afford many more. A useful first approximation is

  ```
  pool size ≈ cores × (1 + expected_wait_time / compute_time)
  ```

  A task that computes for 10 ms and waits on the network for 90 ms is 10% busy, so a 4-core machine can sustain roughly `4 × 10 = 40` threads before the CPU saturates.

- **Always pair sizing with a bounded queue.** Unbounded queues under sustained load consume unbounded memory. For production systems, prefer a bounded `ArrayBlockingQueue` with an explicit rejection policy — a topic covered under **Common Pitfalls**.

---

## ExecutorService in Practice

### `execute` vs. `submit`

`execute(Runnable)` is the raw `Executor` method: it runs the task and returns `void`. Errors thrown by the task propagate to the thread's uncaught-exception handler (by default, printed to stderr), and there is no way to observe completion or obtain a result.

`submit(...)` returns a `Future`, giving you four things `execute` cannot: a handle to **retrieve the result**, a way to **detect failure**, a **cancellation hook**, and a `Future` that marks completion. There are three overloads:

```java
Future<?>            submit(Runnable task);          // no result; Future signals completion
<T> Future<T>        submit(Runnable task, T result);// result is returned when task completes
<T> Future<T>        submit(Callable<T> task);       // task's return value becomes the result
```

Rule of thumb: if you need the outcome, use `submit` with a `Callable`; if you only need the work to happen, `execute` or `submit(Runnable)` are both fine, but `submit` still gives you a completion signal and a cancellation handle.

The following example demonstrates the contrast and shows proper shutdown with a `finally` block:

```java
import java.util.concurrent.*;

public class ExecuteVsSubmit {
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);

        pool.execute(() -> System.out.println("fire-and-forget"));

        Future<Integer> future = pool.submit(() -> 21 * 2);  // Callable: returns a value

        System.out.println("Result: " + future.get());       // blocks until available

        pool.shutdown();                                     // no new tasks accepted
    }
}
```

### `invokeAll` and `invokeAny`

When you have a *collection* of tasks, the bulk methods remove the drudgery of submitting one by one:

- **`invokeAll(tasks)`** submits every task and blocks until *all* of them finish. It returns a `List<Future<V>>` in the **same order** as the input list, so you can pair results with inputs by index. Individual failures are captured in the returned futures, not thrown.
- **`invokeAny(tasks)`** submits every task and returns as soon as *any one* task **completes normally** (i.e., returns a value rather than throwing). All other tasks are cancelled. Use it for "first successful answer wins" problems — say, querying several mirror servers and taking the first healthy response.

```java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class InvokeExamples {
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            final int id = i;                              // lambdas need an effectively final variable
            tasks.add(() -> id * id);
        }

        // invokeAll: run everything, collect every result, preserve input order
        List<Future<Integer>> all = pool.invokeAll(tasks);
        for (Future<Integer> f : all) {
            System.out.println("invokeAll result: " + f.get());
        }

        // invokeAny: first task to complete normally wins
        Integer first = pool.invokeAny(tasks);
        System.out.println("invokeAny result: " + first);

        pool.shutdown();
    }
}
```

### Shutting Down an `ExecutorService`

A running executor holds **non-daemon worker threads by default**, which means a program that forgets to shut down its executor will never terminate — a classic thread leak. The three shutdown methods form the complete toolkit:

- **`shutdown()`** — no new tasks accepted; already-submitted and queued tasks complete; does not block and does not wait.
- **`shutdownNow()`** — interrupts running tasks and returns the `List<Runnable>` of queued tasks that never started. Running tasks are only *requested* to stop: if they ignore interruption, they keep running.
- **`awaitTermination(timeout, unit)`** — blocks until all tasks finish after shutdown, the timeout elapses, or the current thread is interrupted; returns `boolean`. The caller is expected to inspect the result.

> **Best practice callout:** Always shut down an executor you own, in a `finally` block, and use the shutdown → await → force shutdown sequence. Since Java 19, `ExecutorService` implements `AutoCloseable`, so try-with-resources works too; on Java 17 and earlier, use `finally`.

Here is a reusable shutdown helper embodying the full pattern, including correct `InterruptedException` handling:

```java
import java.util.concurrent.*;

public class CleanShutdown {
    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            for (int i = 0; i < 10; i++) {
                final int taskId = i;
                pool.submit(() -> System.out.println("task " + taskId));
            }
        } finally {
            shutdownAndAwait(pool);
        }
    }

    private static void shutdownAndAwait(ExecutorService pool) {
        pool.shutdown();                               // stop accepting new tasks
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();                    // force-stop stragglers
                pool.awaitTermination(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();                        // leave no threads behind
            Thread.currentThread().interrupt();        // re-assert the interrupt flag
        }
    }
}
```

### A Complete Worked Example

The following program ties everything together: a fixed pool, `Callable` tasks, `invokeAll`, result collection, and bulletproof cleanup. It computes the sum of squares of integers `1..1_000_000` by splitting the range into 8 chunks processed in parallel.

```java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ParallelSum {
    public static void main(String[] args) {
        int cores = Runtime.getRuntime().availableProcessors();
        int total = 1_000_000;
        int chunk = total / cores;

        ExecutorService pool = Executors.newFixedThreadPool(cores);
        try {
            List<Callable<Long>> tasks = new ArrayList<>();
            for (int i = 0; i < cores; i++) {
                final int start = i * chunk;
                final int end = (i == cores - 1) ? total : start + chunk;
                tasks.add(() -> sumOfSquares(start + 1, end));   // careful with the +1: ranges are 1-based
            }

            long result = 0;
            for (Future<Long> f : pool.invokeAll(tasks)) {
                result += f.get();
            }
            System.out.println("Sum of squares 1.." + total + " = " + result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.out.println("A task failed: " + e.getCause());
        } finally {
            pool.shutdown();                                     // cleanup even on failure
        }
    }

    private static long sumOfSquares(int from, int to) {
        long sum = 0;
        for (int i = from; i <= to; i++) {
            sum += (long) i * i;
        }
        return sum;
    }
}
```

> **Think of it like...** splitting a giant pile of paperwork among a team of clerks. You divide the pile into equal stacks (`chunks`), hand one to each clerk (`invokeAll`), and wait until all clerks finish before collecting their totals. The executor is the manager who decides how many clerks exist and reuses them for the next pile.

---

## Futures

A **`Future<V>`** is the executor's promise of a result that does not exist yet. Submitting a `Callable` returns a `Future` immediately — the submission is instant even though the computation may take seconds. The `Future` is a handle you can poll, block on, or cancel.

### The `Future<V>` Interface

| Method | Signature | What it does | When it throws |
|---|---|---|---|
| `get()` | `V get()` | Blocks until the task completes, then returns the result | `InterruptedException` (this thread interrupted while waiting); `ExecutionException` (task threw); `CancellationException` (task was cancelled) |
| `get(timeout, unit)` | `V get(long timeout, TimeUnit unit)` | Blocks at most `timeout`; returns result if ready | Above, plus `TimeoutException` if the deadline expires |
| `isDone()` | `boolean isDone()` | `true` once the task has completed, failed, or been cancelled — **not** just "succeeded" | never |
| `isCancelled()` | `boolean isCancelled()` | `true` if the task was cancelled before completion | never |
| `cancel(mayInterruptIfRunning)` | `boolean cancel(boolean mayInterruptIfRunning)` | Requests cancellation; returns `false` if already completed or already cancelled | never |

Two subtleties deserve emphasis because they trip up newcomers:

1. **`isDone()` ≠ success.** It is `true` when the task is done in *any* terminal state: returned a value, threw an exception, or was cancelled. Always inspect the result with `get()` to learn which.
2. **`cancel()` is cooperative.** With `mayInterruptIfRunning=true`, cancellation interrupts the worker thread — but the task must *respond* to interruption. A task that swallows interrupts or never checks `isInterrupted()` will keep running to the end.

The following example demonstrates the time-bounded wait:

```java
import java.util.concurrent.*;

public class FutureDemo {
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();

        Future<Integer> future = pool.submit(() -> {
            Thread.sleep(200);                       // simulated slow computation
            return 42;
        });

        System.out.println("done yet? " + future.isDone());   // almost certainly false

        // Never block forever on a slow task — bound the wait.
        Integer result = future.get(2, TimeUnit.SECONDS);
        System.out.println("result = " + result);

        pool.shutdown();
    }
}
```

### Handling Exceptions: `ExecutionException`

When a `Callable` throws, the exception does *not* propagate to the submitting thread. It is captured and rethrown from `get()` **wrapped** in an `ExecutionException`. Unwrapping with `getCause()` is what reveals the real failure:

```java
import java.util.concurrent.*;

public class ExecutionExceptionDemo {
    public static void main(String[] args) {
        ExecutorService pool = Executors.newSingleThreadExecutor();

        Future<Integer> future = pool.submit(() -> 100 / 0);   // ArithmeticException inside

        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();                // restore interrupt status
        } catch (ExecutionException e) {
            System.out.println("Task failed with: " + e.getCause());
        } finally {
            pool.shutdown();
        }
    }
}
```

> **Think of it like...** an order receipt at a takeout counter. The cashier hands you a numbered receipt (`Future`) immediately; your food is still being cooked. You can ask "is it ready?" (`isDone`), wait for it (`get`), say you'll give up if it takes longer than ten minutes (`get(10, MINUTES)`), or cancel the order (`cancel`). If the kitchen burned your dish, you only find out when you pick it up — the wrapper (`ExecutionException`) contains the chef's note (`getCause()`).

### `FutureTask`

`FutureTask<V>` is a concrete class that implements *both* `Runnable` and `Future<V>` — a task that computes its result lazily and can itself be handed to a thread or executor. It is the glue that lets you pass a cancellable, result-bearing task through APIs that only accept `Runnable`:

```java
import java.util.concurrent.*;

public class FutureTaskDemo {
    public static void main(String[] args) throws Exception {
        FutureTask<Integer> task = new FutureTask<>(() -> 6 * 7);

        // FutureTask is a Runnable AND a Future: execute it, then get the result.
        ExecutorService pool = Executors.newSingleThreadExecutor();
        pool.execute(task);

        System.out.println("6 * 7 = " + task.get());           // 42
        pool.shutdown();
    }
}
```

### Worked Example: A Task That Returns a Value

The following program fetches the current system time from two "services" (simulated by sleeping), uses the first result to arrive via `invokeAny`, and demonstrates cancellation of the loser. It compiles and runs as-is.

```java
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.*;

public class RacingServices {
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Callable<String> serviceA = () -> {
            Thread.sleep(500);                                  // A is slow today
            return "service A says " + LocalTime.now();
        };
        Callable<String> serviceB = () -> {
            Thread.sleep(150);                                  // B is fast
            return "service B says " + LocalTime.now();
        };

        // First task to COMPLETE NORMALLY wins; the other is cancelled.
        String winner = pool.invokeAny(List.of(serviceA, serviceB));
        System.out.println("Winner: " + winner);

        pool.shutdown();
    }
}
```

### Forward Reference: `CompletableFuture`

`Future` is a fine *polling and blocking* abstraction, but it cannot express pipelines like "run A, then feed its result to B, then combine with C" — that requires repeated blocking and manual coordination. **`CompletableFuture`** (Java 8+) extends `Future` with declarative composition: `thenApply`, `thenCompose`, and `thenCombine` chain dependent computations without ever blocking a thread, and `allOf`/`anyOf` coordinate many futures at once. It is the subject of the next chapter; for now, treat it as the answer to "I need to compose async results," and treat `Future` as the answer to "I need to retrieve one result and handle its failure."

---

## Scheduled Executors

`ScheduledExecutorService` extends `ExecutorService` and adds time-based execution. It is the framework's alarm clock: run something once after a delay, or repeatedly on a schedule. Instantiate it with `Executors.newScheduledThreadPool(n)` or `newSingleThreadScheduledExecutor()`.

> **Think of it like...** an alarm clock, a calendar, and a recurring meeting all in one. `schedule` is the one-shot alarm ("wake me in 2 hours"). `scheduleAtFixedRate` is the recurring meeting that starts at 9:00 every morning regardless of how long the last meeting ran. `scheduleWithFixedDelay` is the "30 minutes after the previous task ends" cadence — and if the previous run is still going, the next one simply waits.

### `schedule`, `scheduleAtFixedRate`, `scheduleWithFixedDelay`

- **`schedule(callable, delay, unit)`** — runs the task once after `delay` and returns a `ScheduledFuture<V>` whose `get()` yields the result.
- **`scheduleAtFixedRate(command, initialDelay, period, unit)`** — runs the task repeatedly with a fixed gap between the **start** of successive runs. If a run outlasts the period, the next run begins as soon as possible after it finishes — runs never overlap on the pool's threads.
- **`scheduleWithFixedDelay(command, initialDelay, delay, unit)`** — runs the task repeatedly with a fixed gap between the **end** of one run and the **start** of the next.

Both repeating variants return a `ScheduledFuture<?>`; cancel it (e.g., `scheduleFuture.cancel(true)`) to stop the sequence, and any exception thrown by the task silently cancels the schedule.

| Aspect | `scheduleAtFixedRate` | `scheduleWithFixedDelay` |
|---|---|---|
| Timing reference | from the **start** of each run | from the **end** of each run |
| If a run overruns | next run starts immediately after (no overlap) | next run starts `delay` after this run ends (no overlap) |
| Steady-state cadence | constant, even if work varies | stretches when work is slow |
| Good for | heartbeat/health-check pulses, telemetry sampling | back-off after work, rate-limiting a processing loop |

The ASCII timing diagram below makes the distinction concrete for a task that takes 2 seconds, with a period/delay of 5 seconds:

```
scheduleAtFixedRate(period = 5s, run = 2s):
 t=0   |--RUN--|     t=5   |--RUN--|     t=10  |--RUN--|
       |      |           |      |           |      |
       ^ start            ^ start            ^ start        <- always 5s apart

scheduleWithFixedDelay(delay = 5s, run = 2s):
 t=0   |--RUN--|   idle 5s  |--RUN--|   idle 5s  |--RUN--|
       |      |            |      |            |      |
       ^      ^ end(2s)    ^      ^ end(9s)    ^
       start                start (7s)          start (14s)  <- end-to-start gap is 5s
```

### Worked Example: Periodic Cache Cleanup

A production cache accumulates expired entries; a scheduler purges them on a fixed rate. Because eviction is fast and must happen on a clock cadence, `scheduleAtFixedRate` is the right choice:

```java
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExpiringCache {
    private final Map<String, Entry> cache = new ConcurrentHashMap<>();
    private record Entry(String value, Instant expiry) {}

    public void put(String key, String value, long ttlSeconds) {
        cache.put(key, new Entry(value, Instant.now().plusSeconds(ttlSeconds)));
    }

    public String get(String key) {
        Entry e = cache.get(key);
        if (e == null || e.expiry().isBefore(Instant.now())) {
            return null;
        }
        return e.value();
    }

    // Removes expired entries; called periodically by the scheduler.
    public void evictExpired() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(entry -> entry.getValue().expiry().isBefore(now));
    }

    public static void main(String[] args) throws InterruptedException {
        ExpiringCache cache = new ExpiringCache();
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

        // First purge after 10s, then every 10s from the START of each purge.
        cleaner.scheduleAtFixedRate(cache::evictExpired, 10, 10, TimeUnit.SECONDS);

        cache.put("config", "value-1", 5);
        System.out.println(cache.get("config"));          // "value-1"
        Thread.sleep(6_000);
        System.out.println(cache.get("config"));          // null — expired

        cleaner.shutdown();                               // stop the purger
        cleaner.awaitTermination(1, TimeUnit.SECONDS);    // wait for any in-flight purge
    }
}
```

For a health-check or watchdog that must *also* measure how long each check took, `scheduleWithFixedDelay` is the safer choice: it guarantees the interval is measured from completion, so a slow check never causes a pile-up of overlapping work.

---

## Common Pitfalls & Best Practices

### Forgetting to Shut Down

The single most common executor bug is a program that never terminates because its executor's non-daemon worker threads are still alive. If you create an executor, you own its lifecycle. **Always** reach a `shutdown()` in a `finally` block (or a try-with-resources on Java 19+), and pair it with `awaitTermination` plus `shutdownNow` as a fallback.

### Blocking the Common Pool

Java's parallel streams and `CompletableFuture` (when using the default async pool) run on a **common `ForkJoinPool`** sized to the number of cores. If you submit a blocking task to it — a sleep, a network call, a `get()` on another future — you can exhaust every thread in that pool and deadlock unrelated work that depends on it. Never block the common pool; run blocking work on your own, explicitly sized executor.

### The `Executors` Static Factories Under Load

The convenience factories are fine for prototyping and for workloads with bounded tasks, but three of them (`newFixedThreadPool`, `newSingleThreadExecutor`, `newCachedThreadPool`) are built on **unbounded** queues or unbounded thread counts. Under sustained heavy load:

- an unbounded queue swallows tasks until the JVM runs out of memory;
- a cached pool spawns threads until the JVM runs out of memory.

For production systems under adversarial load, build a **bounded custom `ThreadPoolExecutor`** with an explicit rejection policy — see below. This is the scenario where the factory methods are the wrong tool.

### Saturation and `ThreadPoolExecutor`

When every worker is busy and the queue is full, the pool has reached **saturation**, and the saturation policy decides the fate of the rejected task. `ThreadPoolExecutor` offers four policies, selectable in its constructor:

- **`AbortPolicy`** (default) — throws `RejectedExecutionException`; the submitter must handle it.
- **`CallerRunsPolicy`** — runs the rejected task on the *submitting* thread. This is a built-in back-pressure mechanism: the producer slows to the speed of the workers because it is doing the work itself.
- **`DiscardPolicy`** — silently drops the task.
- **`DiscardOldestPolicy`** — drops the oldest queued task and retries the submission.

The custom constructor takes seven parameters — core pool size, maximum pool size, keep-alive time, a work queue, a `ThreadFactory`, and a rejection handler:

```java
import java.util.concurrent.*;

public class BoundedPool {
    public static void main(String[] args) {
        int cores = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                cores, cores,                          // core and max pool size
                30, TimeUnit.SECONDS,                  // keep-alive for excess threads
                new ArrayBlockingQueue<>(100),         // BOUNDED work queue — the key change
                Executors.defaultThreadFactory(),      // or a custom naming factory
                new ThreadPoolExecutor.CallerRunsPolicy()); // saturation: run on submitter

        for (int i = 0; i < 1_000; i++) {              // way more than the pool can hold
            final int n = i;
            pool.execute(() -> System.out.println("processed " + n));
        }

        pool.shutdown();
        try {
            pool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

With a bounded queue of 100 and a `CallerRunsPolicy`, submission *never throws* and *never loses* a task: excess tasks are executed by the main thread itself, throttling the producer naturally.

### Thread Leaks Inside Tasks

If a task submits a sub-task to *another* executor and forgets to shut that executor down, each task leaks threads — a slow-motion `OutOfMemoryError`. Rule: the executor that owns a pool is the one that shuts it down. When tasks spawn their own executors, shut them down inside the task's `finally`.

### Troubleshooting Checklist

- [ ] Is every executor I created shut down, in a `finally` (or try-with-resources on 19+)?
- [ ] Are my tasks bounded in time, or could a slow task stall a fixed pool forever?
- [ ] Is my work queue bounded, so a producer stampede cannot exhaust memory?
- [ ] Do long-running or blocking tasks ever run on the common `ForkJoinPool`?
- [ ] Are worker threads named (`ThreadFactory` with a prefix) so thread dumps are readable?
- [ ] Do I handle `InterruptedException` by re-asserting `Thread.currentThread().interrupt()`?
- [ ] Do I unwrap `ExecutionException.getCause()` rather than printing the wrapper?

> **Best practice callout:** name your threads. `new ThreadFactoryBuilder`-style naming (or a simple custom `ThreadFactory` adding `"db-worker-"`) turns a cryptic thread dump into a legible diagram of which pool is stuck.

> **Warning callout:** `newCachedThreadPool` and unbounded queues are attractive in demos and lethal in production bursts. If you cannot prove your task rate is bounded, bound your pool.

---

## Real-World Use Cases

1. **HTTP request handling** — a web server (like the `ExecutorServer` at the top of this chapter) hands each accepted socket to a `newFixedThreadPool(n)`. The pool caps concurrent connections, queues overflow during spikes, and amortizes thread creation. *(Forward reference: on Java 21+, `Executors.newVirtualThreadPerTaskExecutor()` makes per-request threads cheap enough to skip pooling entirely.)*
2. **Batch data processing** — nightly jobs that transform 1,000,000 rows from a database or filesystem should use a fixed pool with chunked tasks and `invokeAll`, exactly as in the `ParallelSum` example. The fixed size keeps DB connection usage predictable, and the bounded custom variant prevents queue blowups when a batch stalls.
3. **Scheduled reporting** — a `ScheduledExecutorService` that emits a usage report every morning at 02:00 (`schedule` with an initial delay computed from "now"), and samples telemetry every 15 seconds (`scheduleAtFixedRate`). Choose `scheduleWithFixedDelay` when the next run must wait for the previous run to fully finish (e.g., DB dumps).
4. **Parallel computation** — render frames of an animation, hash chunks of a large file, or multiply matrix blocks using `newWorkStealingPool()`. Work stealing shines when subtasks are unevenly sized, because busy workers steal from idle ones instead of waiting on a global queue.
5. **First-successful-response racing** — `invokeAny` for failover: query three regionally replicated endpoints and take whichever answers first (see `RacingServices`). The losers are cancelled automatically.

---

## Exercises

### Conceptual Questions

1. A colleague says: "`isDone()` returned `true`, so my task succeeded." Why might they be wrong? Give three distinct ways a task can be done without producing a result.
2. Under heavy load, `newFixedThreadPool(8)` and `newCachedThreadPool()` fail in different ways. Describe precisely how each degrades and which one you would trust in a production batch job.
3. What is the difference between calling `shutdown()` and calling `shutdownNow()`? What does `shutdownNow()` return, and what must your code do with it?
4. Explain, in terms of the task's duration, when `scheduleAtFixedRate` behaves identically to `scheduleWithFixedDelay` — and when it does not.

### Programming Exercises

1. **Parallel sum with `invokeAll`.** Extend `ParallelSum` to time both the sequential and the parallel versions of the sum of squares up to 10,000,000. Print elapsed times with `System.nanoTime()` and report the speedup on your machine. Expected behavior: the parallel version should beat the sequential version on a multi-core machine, and the printed results must be identical.
2. **Timeout-and-cancel downloader.** Write a program that submits several `Callable` tasks to a fixed pool, each simulating a download with `Thread.sleep` of varying durations. For each `Future`, call `get(1, TimeUnit.SECONDS)`; on `TimeoutException`, cancel the future with `cancel(true)` and print a warning. Expected behavior: fast tasks print results, slow tasks are cancelled, and the program exits cleanly with a proper shutdown.
3. **Liveness monitor.** Build a `ScheduledExecutorService` that, every second, prints the current time and the size of a queue you feed it (use `ThreadPoolExecutor.getQueue().size()` and `getCompletedTaskCount()` from a live pool). Run it for ten seconds while submitting tasks in a tight loop. Expected behavior: you see a live, monotonic count of completed tasks and a bounded or unbounded queue size depending on the pool you chose.
4. **First-response failover.** Implement a pool with three `Callable` services that sleep 2000, 300, and 900 ms respectively. Use `invokeAny` to obtain the winner, then verify with `isCancelled()` that the other futures were cancelled. Expected behavior: the 300 ms service always wins, and the program completes in roughly 300 ms.

---

## Chapter Summary

- **`Executor` separates submission from execution policy**; `ExecutorService` adds result retrieval, bulk operations, and lifecycle management; `ScheduledExecutorService` adds time-based execution.
- **Thread pools reuse worker threads** instead of spawning per task, cutting overhead and bounding resource use — the difference between an 8-thread server surviving a 100,000-request burst and one crashing.
- **Choose the pool by its queue semantics**: fixed pools cap concurrency with an unbounded queue; cached pools trade queues for unbounded thread growth; single-thread pools serialize; work-stealing pools parallelize recursion.
- **Use `Callable` when you need a result, `Runnable` when you do not**; retrieve results via `Future.get()`, unwrap failures via `ExecutionException.getCause()`, and always bound blocking waits with `get(timeout, unit)`.
- **Manage the lifecycle**: `shutdown()` → `awaitTermination()` → `shutdownNow()` as a fallback, always in a `finally` block — this is the difference between a terminating program and a thread leak.
- **`scheduleAtFixedRate` measures period from task starts; `scheduleWithFixedDelay` from task ends**; prefer fixed delay when a slow run must not shorten the breathing room before the next.
- **Bound your pools in production**: custom `ThreadPoolExecutor` with a bounded queue and an explicit saturation policy (`CallerRunsPolicy` for back-pressure) beats the static factories under adversarial load.

**Connection to the next chapter.** You now know how to launch work concurrently, retrieve single results, and handle failure — but composing many dependent, asynchronous steps with `Future` is verbose and blocking. The next chapter on **`CompletableFuture`** shows how to chain, combine, and recover from asynchronous computations declaratively, and how virtual threads (Java 21+) change the economics of when you need a pool at all. The lifecycle discipline and the task-versus-policy mental model you built here are the foundation that makes those tools safe to use.

---

## Glossary

- **Executor** — the interface that decouples task submission (`execute`) from execution policy.
- **ExecutorService** — an `Executor` with lifecycle management and result-bearing submission (`submit`, `invokeAll`, `invokeAny`, `shutdown`).
- **ScheduledExecutorService** — an `ExecutorService` that runs tasks once after a delay or repeatedly on a fixed rate or fixed delay.
- **Thread Pool** — a fixed set of reusable worker threads that execute submitted tasks, with a work queue for overflow.
- **Work Queue** — the data structure that holds tasks waiting for a free worker; its boundedness determines whether the pool can reject or must absorb excess work.
- **Task** — a unit of work submitted to an executor, expressed as a `Runnable` (no result) or `Callable<V>` (result).
- **Future** — a handle to a task's eventual result; supports blocking retrieval, polling, and cancellation.
- **Saturation** — the state where all workers are busy and the queue is full; the rejection policy then decides what happens to new tasks.
- **Saturation policy** — the `RejectedExecutionHandler` that handles tasks submitted to a saturated pool (abort, caller-runs, discard, discard-oldest).
- **Shutdown** — the transition that stops the acceptance of new tasks while letting already-submitted work complete.

---

## Further Reading

- Oracle, **`java.util.concurrent` Javadoc** — the authoritative API reference for `ExecutorService`, `ThreadPoolExecutor`, `ScheduledExecutorService`, and `Future`. Read the class-level documentation first; it is the best prose in the JDK.
- **`java.util.concurrent.Executors`** Javadoc — the factory methods and their exact queue/thread semantics.
- Brian Goetz et al., ***Java Concurrency in Practice*** (Addison-Wesley, 2006) — chapters on executors and tasks remain the definitive treatment of pools, sizing, and shutdown.
- Joshua Bloch, ***Effective Java*, 3rd edition** — Item 80, "Prefer executors, tasks, and streams to threads," a concise summary of this chapter's thesis.
- JEP 444, ***Virtual Threads*** (Java 21) — the forward-looking complement to pooling: the model in this chapter, and where the platform is heading.