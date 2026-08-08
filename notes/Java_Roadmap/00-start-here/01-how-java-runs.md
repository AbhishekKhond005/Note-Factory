# How Java Runs: From Source Code to Execution

---

## 1. Learning Objectives

By the end of this chapter, you will be able to:

1. **Trace** the complete lifecycle of a Java program through its three stages: source code, bytecode, and JVM execution.
2. **Distinguish** between the JDK, JRE, and JVM—and explain what each provides and when to use each.
3. **Compile** a `.java` file with `javac`, **run** it with `java`, and **package** it into an executable JAR with `jar`.
4. **Diagnose and fix** classpath-related errors like `ClassNotFoundException` and `NoClassDefFoundError`.
5. **Organize** Java code into packages and compile/run code that spans multiple packages and third-party libraries.

---

## 2. The Big Picture: From Source to Execution

Java occupies a unique spot in the programming language ecosystem. It is neither purely **compiled** (like C or Rust) nor purely **interpreted** (like Python or JavaScript). It is **both**—and that hybrid design is the key to its "write once, run anywhere" promise.

### The Pipeline in a Nutshell

```
┌──────────────┐     javac      ┌──────────────┐     JVM       ┌────────────────┐
│  Source Code  │ ──────────▶   │   Bytecode    │ ──────────▶  │  Machine Code   │
│  Hello.java   │   compiler    │  Hello.class  │   runtime    │  (platform-specific) │
└──────────────┘               └──────────────┘               └────────────────┘
       │                              │                              │
  You write                       Portable                      OS executes
  human-readable                  intermediate                   native instructions
  Java source                     binary format                  (x86, ARM, etc.)
```

### The Analogy: A Play, Translated for International Tour

Imagine you are a playwright who has written a play in English. Your producer wants to stage it in five different countries: France, Japan, Brazil, Germany, and Egypt.

- **Step 1 — Write the script (Source Code):** You write the dialogue, stage directions, and character notes in English. Any theater company in the world can read it—as long as they understand English.
- **Step 2 — Translate to an intermediate script (Bytecode):** You hire a translator who converts your English script into a **universal stage notation**—a set of abstract instructions like "Actor enters stage left," "Lights fade to black," "Character A delivers line 42." This notation is not specific to any one theater; any production crew can interpret it.
- **Step 3 — Stage the play in each country (JVM Execution):** Each local crew takes that universal notation and produces a **live performance** in their own language and style. The French crew reads "Actor enters stage left" and has their actor walk on. The Japanese crew does the same, but with their own actor on their own stage. The universal notation stays the same; the *performance* is adapted to each platform.

| Analogy Element | Java Concept |
|---|---|
| English script | `.java` source code |
| Universal stage notation | `.class` bytecode |
| Translator (`javac`) | The Java compiler |
| Local production crew | The JVM on each platform |
| The live performance | Machine-code execution |

**Why this matters:** This design means you write Java code once, compile it once, and ship the `.class` file (or a JAR containing it) to any platform with a JVM—Windows, Linux, macOS, a Raspberry Pi, or a mainframe. No recompilation needed. That portability is why Java dominates enterprise backends, Android apps, and large-scale distributed systems.

---

## 3. Source Code → Bytecode → JVM (The Three Stages)

Let us now walk through each stage in detail, with concrete code you can compile and run yourself.

### 3.1 Stage 1: Source Code (`.java` files)

A `.java` file is a plain-text file containing Java source code. It must follow the syntax rules of the Java language. Every valid Java source file ends with the `.java` extension.

#### A Minimal `HelloWorld.java`

Create a file named `HelloWorld.java` with the following content:

```java
/**
 * HelloWorld — the first program every Java developer writes.
 */
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

Let us annotate every line:

| Line | What It Is | Explanation |
|---|---|---|
| `public class HelloWorld {` | **Class declaration** | Every Java program is defined inside a class. `public` means this class is accessible from anywhere. The class name `HelloWorld` **must** match the filename (`HelloWorld.java`). |
| `public static void main(String[] args) {` | **Entry-point method** | The JVM calls this method to start the program. `public` so the JVM can access it; `static` so no object instance is needed; `void` because it returns nothing; `String[] args` is an array of command-line arguments. |
| `System.out.println("Hello, World!");` | **Output statement** | `System.out` is the standard output stream. `println` prints the string followed by a newline. The string literal `"Hello, World!"` is the message. |
| `}` | **Closing braces** | Match each opening brace. Indentation is convention, not syntax—but do not skip it. |

> **⚠️ Filename rule:** In Java, a `public` class must be declared in a file whose name matches the class name, including capitalization. `HelloWorld.java` contains `public class HelloWorld`. If they do not match, `javac` will emit an error.

### 3.2 Stage 2: Compilation to Bytecode (`.class` files)

Once you have written `HelloWorld.java`, you compile it with the **Java compiler**, `javac`.

```text
$ javac HelloWorld.java
```

If the compilation succeeds, the output is invisible—you get no news is good news. But if you list the directory contents, you will see a new file:

```text
$ ls
HelloWorld.java   HelloWorld.class
```

The `.class` file contains **bytecode**—a platform-independent binary instruction set that the JVM understands. It is *not* machine code (x86 or ARM). It is an intermediate representation.

#### Peeking Inside with `javap`

The JDK ships with a disassembler tool, `javap`, that lets you inspect the bytecode in human-readable form:

```text
$ javap -c HelloWorld
```

The output looks something like this:

```class
Compiled from "HelloWorld.java"
public class HelloWorld {
  public HelloWorld();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  public static void main(java.lang.String[]);
    Code:
       0: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
       3: ldc           #13                 // String Hello, World!
       5: invokevirtual #15                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
       8: return
}
```

The first method (`public HelloWorld()`) is the **default constructor** — the compiler generates one even though we did not write one. The second method is our `main`. Let us map each of the three bytecode instructions in `main` back to the original source.

| Source Code Line | Bytecode Instruction | Operand / Comment | Meaning |
|---|---|---|---|
| `System.out.println(...)` | `getstatic #7` | `#7` = `System.out` field | Push the static field `System.out` (a `PrintStream`) onto the operand stack. |
| `"Hello, World!"` | `ldc #13` | `#13` = `"Hello, World!"` string constant | Load the string constant from the constant pool onto the stack. |
| `System.out.println(...)` | `invokevirtual #15` | `#15` = `PrintStream.println(String)` | Call the `println` method on the `PrintStream` object, consuming the string argument. |
| `}` | `return` | (none) | Return `void` from the method. |

Let us break down each instruction:

- **`getstatic`**: Retrieves the value of a static field from a class. Here, it grabs `System.out`, which is a `PrintStream` object. The `#7` is a symbolic reference to the constant pool entry for `System.out`.
- **`ldc`**: "Load constant." It pushes a constant from the constant pool onto the operand stack. In this case, the string `"Hello, World!"`.
- **`invokevirtual`**: Invokes an instance method on an object, dispatching based on the object's runtime type. Here, it calls `PrintStream.println(String)`. The `#15` references the method signature in the constant pool.
- **`return`**: Returns from a `void` method. Control goes back to the caller (the JVM's startup code).

> **Why this matters:** Bytecode is the *lingua franca* of the Java ecosystem. Any language that compiles to bytecode (Kotlin, Scala, Clojure, Groovy, Java itself) can run on the JVM. When you understand bytecode, you can debug performance issues, reason about how the JIT compiler optimizes your code, and even write tools that manipulate compiled classes.

### 3.3 Stage 3: The Java Virtual Machine (JVM) Runs the Bytecode

The **Java Virtual Machine (JVM)** is an abstract computing machine. It has its own instruction set (bytecode), its own memory model, and its own execution engine. When you run:

```text
$ java HelloWorld
```

The JVM starts up, **loads** the `HelloWorld.class` file, **verifies** the bytecode for safety, **links** the class, and then executes the `main` method.

#### Runtime Memory Areas

The JVM divides memory into several regions during execution:

| Area | Purpose | Analogy |
|---|---|---|
| **Method Area** | Stores class-level data: bytecode, constant pool, static variables, method metadata. | The backstage script library — every play's script is stored here. |
| **Heap** | Stores all Java objects created at runtime (instances of classes, arrays). Garbage collection happens here. | The prop warehouse — all physical objects (actors' props, furniture) live here and are discarded when no longer needed. |
| **Stack** | Each thread has a private stack. Frames are pushed for each method call; each frame holds local variables, partial results, and the operand stack. | The stage manager's clipboard — each scene (method call) gets its own page with notes on local props and cues. |
| **PC Register** | Each thread has a program counter that points to the next bytecode instruction to execute. | The line in the script the actor is currently reading. |
| **Native Method Stack** | Used for native (non-Java) method calls—typically C code accessed through the Java Native Interface (JNI). | A backstage pass for visiting technicians who do not speak the play's language. |

#### Two Execution Modes: Interpretation and JIT Compilation

The JVM does *not* execute bytecode as native code by default. It does something more clever.

**1. Interpretation (slow start, no warm-up):**
The JVM begins by **interpreting** bytecode—reading each instruction one by one and executing the corresponding native operation. This is like a human translator who reads each sentence aloud in real time. It works immediately, but it is slow.

**2. Just-In-Time (JIT) Compilation (fast after warm-up):**
The JVM monitors which methods are called most frequently—the "hot spots" of the application. When a method passes a certain threshold, the JVM triggers the **JIT compiler**, which compiles that entire method's bytecode into native machine code (x86, ARM, etc.) and caches it. Subsequent calls execute at full native speed.

> **Analogy:** Imagine a simultaneous interpreter at the United Nations. At first, she listens to each sentence and translates it word by word (interpretation). But after hearing the same phrase ten times, she memorizes it and can produce the translation instantly (JIT compilation). The more a phrase is used, the faster it becomes.

**Why this matters:** This hybrid approach gives Java the best of both worlds. Startup is fast (no costly full compilation like C++), but long-running server applications approach native speed after warm-up. This is why Java dominates in server-side and backend environments where applications run for days or weeks.

> **A deep dive into JIT optimization strategies (inlining, escape analysis, lock coarsening) is beyond this chapter. The key idea is that the JVM *learns* from runtime behavior and optimizes accordingly—a technique that ahead-of-time compilers cannot match.**

---

## 4. The Toolchain: JDK vs JRE vs JVM

Newcomers to Java are often confused by the alphabet soup: JVM, JRE, JDK. Let us clarify each one.

### 4.1 Definitions Table

| Component | What It Is | What It Contains |
|---|---|---|
| **JVM** | The **runtime engine** that executes bytecode. | Bytecode verifier, class loader, execution engine (interpreter + JIT compiler), garbage collector, runtime memory areas (heap, stack, method area, PC registers, native method stack). |
| **JRE** | The **runtime environment**—everything needed to *run* Java programs. | The JVM + core libraries (`java.base`, `java.sql`, `java.net`, etc.) + supporting files (e.g., `rt.jar` in Java 8, or the module images in Java 9+). |
| **JDK** | The **development kit**—everything needed to *develop* Java programs. | The JRE + developer tools: `javac` (compiler), `jar` (archiver), `javadoc` (documentation generator), `jdb` (debugger), `jlink` (custom runtime builder), `jpackage` (installer builder), and more. |

### 4.2 Visual Relationship

```
┌─────────────────────────────────────────────────────┐
│                      JDK                            │
│  ┌─────────────────────────────────────────────┐    │
│  │                    JRE                       │    │
│  │  ┌──────────────────────────────────────┐    │    │
│  │  │                JVM                   │    │    │
│  │  │  ┌────────────────────────────┐      │    │    │
│  │  │  │ Class Loader              │      │    │    │
│  │  │  │ Bytecode Verifier         │      │    │    │
│  │  │  │ Execution Engine          │      │    │    │
│  │  │  │   ├─ Interpreter          │      │    │    │
│  │  │  │   └─ JIT Compiler         │      │    │    │
│  │  │  │ Garbage Collector         │      │    │    │
│  │  │  │ Memory Areas              │      │    │    │
│  │  │  └────────────────────────────┘      │    │    │
│  │  │                                      │    │    │
│  │  │  Java Core Libraries                 │    │    │
│  │  │  (java.base, java.sql, etc.)         │    │    │
│  │  └──────────────────────────────────────┘    │    │
│  │                                              │    │
│  │  Developer Tools                             │    │
│  │  ├─ javac (compiler)                        │    │
│  │  ├─ jar (archiver)                          │    │
│  │  ├─ javadoc (documentation)                 │    │
│  │  ├─ jdb (debugger)                          │    │
│  │  ├─ jlink (custom runtime)                  │    │
│  │  └─ jpackage (installer)                    │    │
│  └──────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘

       JDK ⊃ JRE ⊃ JVM
```

### 4.3 Practical Guidance

#### Which One Do I Install?

| Your Goal | Install |
|---|---|
| I want to **write and compile** Java programs. | **JDK** (Java Development Kit). It includes everything. |
| I only need to **run** Java programs on a server. | **JRE** (Java Runtime Environment)—but note that starting with Java 11, Oracle no longer ships a standalone JRE. You use `jlink` to build a custom runtime, or install a JDK and use the `jmods` to create a minimal JRE. |
| I am curious about the JVM internals but not writing code. | Install a JDK and explore the tools. You get the JVM as part of it. |

> **⚠️ Legacy note:** In Java 8 and earlier, Oracle distributed separate JDK and JRE installers. Starting with Java 11, the JRE is no longer a separate download. For most developers, just install the JDK and use `java` and `javac` from there.

#### Verifying Your Installation

Open a terminal and run these commands:

```text
$ java -version
openjdk version "21" 2023-09-19 LTS
OpenJDK Runtime Environment (build 21+35)
OpenJDK 64-Bit Server VM (build 21+35, mixed mode, sharing)

$ javac -version
javac 21
```

The `java -version` output tells you the JVM version and the execution mode (`mixed mode` means it uses both interpretation and JIT compilation). `javac -version` confirms the compiler is available.

> If you get `command not found`, the JDK's `bin` directory is not in your `PATH`. On Unix systems, add `export PATH=$PATH:/usr/lib/jvm/java-21/bin` to your shell profile (adjusting the path to match your installation).

---

## 5. The Developer Tools in Action

Now that you understand the conceptual model, let us get our hands dirty with the three essential tools: `javac`, `java`, and `jar`.

### 5.1 `javac` — The Compiler

The `javac` compiler reads `.java` source files and produces `.class` bytecode files.

#### Basic Syntax

```text
javac [options] [sourcefiles]
```

#### Common Flags

| Flag | Purpose | Example |
|---|---|---|
| `-d <directory>` | Write `.class` files to the specified directory (creating package subdirectories as needed). | `javac -d out HelloWorld.java` writes `HelloWorld.class` to `out/`. |
| `-cp` or `-classpath` | Specify where to find user-defined classes and libraries during compilation. | `javac -cp lib/guava.jar -d out src/*.java` |
| `-verbose` | Print detailed info about what the compiler is doing (which classes it loads, etc.). | `javac -verbose HelloWorld.java` |

#### Examples

**Compile a single file in the current directory:**

```text
$ ls
HelloWorld.java
$ javac HelloWorld.java
$ ls
HelloWorld.java   HelloWorld.class
```

**Compile with an output directory:**

```text
$ mkdir -p out
$ javac -d out HelloWorld.java
$ ls out/
HelloWorld.class
```

The `-d` flag keeps your source directory clean. Professionals *always* use `-d` or a build tool that does it for them.

**Compile multiple files with a classpath dependency:**

```text
$ javac -d out -cp lib/guava.jar src/com/example/*.java
```

This compiles all `.java` files in `src/com/example/`, using `guava.jar` on the classpath, and places the `.class` files in `out/com/example/`.

#### What a Compile Error Looks Like

Suppose you forget a semicolon:

```java
public class Broken {
    public static void main(String[] args) {
        System.out.println("Oops")
    }
}
```

```text
$ javac Broken.java
Broken.java:3: error: ';' expected
        System.out.println("Oops")
                                   ^
1 error
```

The compiler tells you:
- The **file** (`Broken.java`)
- The **line number** (`:3`)
- The **error description** (`';' expected`)
- A **caret** (`^`) pointing to the exact location.

Always start debugging at the first error. Sometimes one error causes a cascade of follow-on errors—fix the first one and recompile.

### 5.2 `java` — The Launcher

The `java` command launches the JVM and executes a Java class.

#### Basic Syntax

```text
java [options] <classname> [args...]
```

> **⚠️ Crucial distinction:** You pass the **class name**, not the file name. `java HelloWorld` is correct; `java HelloWorld.class` will fail. Do not include the `.class` extension.

#### Examples

**Run a class in the default package (current directory):**

```text
$ javac HelloWorld.java
$ java HelloWorld
Hello, World!
```

**Run a class in a named package with `-cp`:**

```text
$ javac -d out src/com/example/HelloWorld.java
$ java -cp out com.example.HelloWorld
Hello, World!
```

Notice: the `java` command takes `com.example.HelloWorld` (dots separating package components), not `com/example/HelloWorld` (slashes).

**Pass command-line arguments:**

```java
public class ArgsDemo {
    public static void main(String[] args) {
        System.out.println("Received " + args.length + " arguments:");
        for (int i = 0; i < args.length; i++) {
            System.out.println("  args[" + i + "] = " + args[i]);
        }
    }
}
```

```text
$ javac ArgsDemo.java
$ java ArgsDemo apple banana "cherry pie"
Received 3 arguments:
  args[0] = apple
  args[1] = banana
  args[2] = cherry pie
```

**Specify the classpath explicitly:**

```text
$ java -cp out:lib/guava.jar:lib/other.jar com.example.MyApp
```

On Windows, replace `:` with `;`:
```text
$ java -cp out;lib\guava.jar;lib\other.jar com.example.MyApp
```

**Run an executable JAR:**

```text
$ java -jar myapp.jar arg1 arg2
```

The `-jar` flag tells the JVM to read the `Main-Class` attribute from the JAR's manifest to determine which class contains `main`.

### 5.3 `jar` — The Archiver

A **JAR** (Java ARchive) file is a ZIP file that bundles `.class` files, resources (images, configuration files), metadata, and optionally a **manifest** file. A JAR can be made **executable** by specifying the main class in the manifest.

#### Common Commands

| Command | What It Does |
|---|---|
| `jar cf myapp.jar -C out .` | **Create** a JAR file. `-C out` changes to the `out` directory, and `.` adds everything in it. |
| `jar tf myapp.jar` | **List** the contents (table of contents). |
| `jar xf myapp.jar` | **Extract** the JAR's contents into the current directory. |
| `jar uf myapp.jar newfile.class` | **Update** an existing JAR by adding a file. |

#### Creating an Executable JAR — End to End

**Step 1: Write and compile a simple program**

```java
// Greeter.java
public class Greeter {
    public static void main(String[] args) {
        String name = args.length > 0 ? args[0] : "World";
        System.out.println("Hello, " + name + "!");
    }
}
```

Compile it to an `out` directory:

```text
$ javac -d out Greeter.java
```

**Step 2: Create a manifest file**

Create a file called `MANIFEST.MF` (the name and capitalization matter):

```text
Main-Class: Greeter
```

Every manifest must end with a blank line. The full content:

```text
Main-Class: Greeter

```

> **⚠️** The manifest file must end with a newline. If the last line is not a newline, the JAR tool may silently ignore the `Main-Class` attribute.

**Step 3: Create the JAR**

```text
$ jar cfm Greeter.jar MANIFEST.MF -C out .
```

Flags explained:
- `c` — create
- `f` — write to file `Greeter.jar`
- `m` — use the specified manifest file (`MANIFEST.MF`)
- `-C out .` — change to `out/` directory and add everything

**Step 4: Inspect the JAR contents**

```text
$ jar tf Greeter.jar
META-INF/
META-INF/MANIFEST.MF
Greeter.class
```

**Step 5: Run the JAR**

```text
$ java -jar Greeter.jar
Hello, World!

$ java -jar Greeter.jar Ada
Hello, Ada!
```

#### The Structure of `META-INF/MANIFEST.MF`

The manifest is a simple key-value text file. A fully populated manifest might look like:

```text
Manifest-Version: 1.0
Created-By: 21 (OpenJDK)
Main-Class: com.example.Main
Class-Path: lib/guava.jar lib/commons-codec.jar
```

- `Main-Class`: specifies the entry point for `java -jar`.
- `Class-Path`: specifies additional JARs to include on the classpath relative to the JAR's location.

**Why this matters:** Every major Java framework (Spring Boot, Quarkus, Micronaut) packages applications as executable JARs. Understanding how JARs work gives you insight into how your production artifacts are built and deployed.

---

## 6. Classpath & Packagepath

### 6.1 Packages

As your programs grow, you need to organize your code. **Packages** are Java's mechanism for namespace management, access control, and project organization.

#### Why Packages Exist

- **Namespace management:** Two developers can both create a class named `User` without conflict if they put them in different packages (`com.example.auth.User` vs `com.example.billing.User`).
- **Access control:** Package-private (default) access restricts visibility to classes within the same package.
- **Project organization:** Packages mirror the logical structure of your application (e.g., `controller`, `service`, `repository`, `model`).

#### Convention: Reverse Domain Name

The standard convention is to use your organization's domain name in reverse:

```
com.example.myapp
org.mycompany.project
edu.university.course
```

#### Directory Structure

Packages map directly to directories on disk. Each dot in the package name becomes a subdirectory.

For a class `HelloWorld` in package `com.example`:

```
src/
  com/
    example/
      HelloWorld.java
```

#### The `package` Statement

Your source file must declare its package at the very top (before any imports):

```java
package com.example;

public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello from a package!");
    }
}
```

#### Compiling and Running with Packages

```text
$ mkdir -p src/com/example
$ # Write HelloWorld.java into src/com/example/

$ javac -d out src/com/example/HelloWorld.java
$ java -cp out com.example.HelloWorld
Hello from a package!
```

Notice:
- The compiler creates `out/com/example/HelloWorld.class` (mirroring the package structure).
- The `java` command uses the fully qualified class name `com.example.HelloWorld`.
- The `-cp out` tells the JVM that `out/` is the root of the package hierarchy.

### 6.2 Classpath

The **classpath** is to the JVM what `$PATH` is to your shell—it tells the JVM where to look for `.class` files (and JARs) when loading classes at runtime (and at compile time).

#### Default Classpath

If you do not specify a classpath, the JVM uses the **current directory** (`.`) as the classpath. This is why `java HelloWorld` works when `HelloWorld.class` is in the current directory.

#### How to Set the Classpath

**1. Command-line flag (preferred):**

```text
java -cp out:lib/guava.jar:lib/commons-codec.jar com.example.MyApp
```

On Windows:

```text
java -cp out;lib\guava.jar;lib\commons-codec.jar com.example.MyApp
```

**2. Environment variable (avoid this):**

```text
export CLASSPATH=out:lib/guava.jar
java com.example.MyApp
```

> **⚠️ Never set the `CLASSPATH` environment variable globally.** It silently affects all Java programs on your system, leading to mysterious class-loading failures. Always use `-cp` explicitly in scripts and build configurations.

#### Three Concrete Scenarios

**Scenario 1: Single class in the default package**

```
Directory: ~/projects/hello/
├── HelloWorld.class
```

```text
$ java HelloWorld
```

This works because the JVM looks in the current directory (the default classpath) and finds `HelloWorld.class`.

---

**Scenario 2: Single class in a named package with `-cp`**

```
Directory: ~/projects/greeter/
├── out/
│   └── com/
│       └── example/
│           └── HelloWorld.class
```

```text
$ java -cp out com.example.HelloWorld
```

The JVM receives `com.example.HelloWorld`. It translates dots to slashes (`com/example/HelloWorld.class`) and searches for that file under every directory on the classpath. It finds `out/com/example/HelloWorld.class`.

---

**Scenario 3: Depending on a library JAR**

```
Directory: ~/projects/myapp/
├── out/
│   └── com/
│       └── example/
│           └── MyApp.class
└── lib/
    └── guava-31.1-jre.jar
```

```java
package com.example;

import com.google.common.base.Strings;

public class MyApp {
    public static void main(String[] args) {
        System.out.println(Strings.repeat("Java ", 3));
    }
}
```

**Compile with the JAR on the classpath:**

```text
$ javac -d out -cp lib/guava-31.1-jre.jar src/com/example/MyApp.java
```

**Run with both `out` and the JAR on the classpath:**

```text
$ java -cp out:lib/guava-31.1-jre.jar com.example.MyApp
Java Java Java
```

#### Classpath for `javac` vs `java`

- **`javac -cp`** (or `-classpath`): Used during compilation to resolve class references in your source code. If your code imports a class from a library, that library must be on the compile-time classpath.
- **`java -cp`**: Used at runtime to locate `.class` files and load them into the JVM. Every class your application uses must be on the runtime classpath.

> **Key insight:** A class available at compile time may be **missing** at runtime, causing `NoClassDefFoundError`. This is the most common classpath pitfall in Java development. A class available at runtime must also be available at compile time (or the source code would not compile). Always ensure your runtime classpath is a *superset* of your compile-time classpath.

### 6.3 Common Pitfalls (Troubleshooting Table)

| Symptom | Likely Cause | Fix |
|---|---|---|
| `ClassNotFoundException` | The class is simply not on the classpath at runtime. | Verify the classpath includes the directory or JAR containing the class. Check spelling of the class name. |
| `NoClassDefFoundError` | The class was available at compile time but is missing at runtime. | Add the missing dependency to the runtime classpath. A classic example: your code compiled against Guava, but the deployment script forgot to include `guava.jar`. |
| `NoSuchMethodError` | Version mismatch: the class was compiled against one version of a library but runs against a different (older) version. | Recompile against the exact library version used at runtime. Use a dependency manager (Maven/Gradle) to lock versions. |
| `Exception in thread "main" java.lang.NoClassDefFoundError: Bad class file` | The directory structure does not match the package declaration. | Ensure the `.class` file is in the correct subdirectory. For `com.example.HelloWorld`, the classpath root must contain `com/example/HelloWorld.class`, not `HelloWorld.class` directly. |
| `could not find or load main class X` | The class name was misspelled, the file extension was included, or the classpath is wrong. | Use the fully qualified class name (e.g., `com.example.Main`), without `.class` or `.java`. Ensure the classpath root points to the correct directory. |
| `error: cannot find symbol` (compile time) | A class or method referenced in source code is not on the compile-time classpath. | Add the required JAR or source file to `javac -cp`. |

**Why this matters:** Classpath issues are one of the most common causes of production outages in Java. A server can compile fine on a developer's machine, pass CI, and then crash at 3 AM in production because a JAR is missing from the deployment. Modern build tools like Maven and Gradle are essentially **classpath managers**—they resolve, download, and organize dependencies so you do not have to. Nevertheless, understanding the raw classpath concepts is essential for debugging when those tools fail.

---

## 7. Summary

- **Java is both compiled and interpreted:** Source code (`.java`) is compiled to bytecode (`.class`), and the JVM interprets and JIT-compiles that bytecode at runtime. This hybrid model gives Java both portability and performance.
- **The JVM is an abstract machine** with its own instruction set, memory areas (heap, stack, method area, PC registers, native method stack), and a two-phase execution engine (interpreter + JIT compiler).
- **JDK ⊃ JRE ⊃ JVM:** The JDK is for development (includes `javac`, `jar`, `javadoc`, etc.), the JRE is for runtime (JVM + core libraries), and the JVM is the execution engine at the core.
- **The three essential tools are `javac` (compile), `java` (run), and `jar` (package).** Use `-d` with `javac`, `-cp` with both `javac` and `java`, and `-jar` with executable JARs.
- **The classpath is the fundamental mechanism for locating classes.** Packages map to directory structures, and the classpath tells the JVM where the root of that structure is. Classpath errors (`ClassNotFoundException`, `NoClassDefFoundError`) are among the most common and most important to understand.

> **The one-sentence takeaway:** *A Java program is written as source code, compiled to platform-independent bytecode, and executed by a virtual machine that blends interpretation with just-in-time compilation—giving you portability across platforms without sacrificing long-running performance.*

---

## 8. Exercises

### Exercise 1 — Recall (Easy)

List the three stages a Java program goes through from source code to execution. For each stage, name:
- The file extension involved (if any).
- The tool (or component) responsible.
- The output produced.

---

### Exercise 2 — Apply (Medium)

Given the following directory tree:

```
project/
├── out/
│   └── com/
│       └── example/
│           ├── App.class
│           └── Utils.class
└── lib/
    └── jackson-core-2.15.0.jar
```

For each of the following commands, state whether it will succeed or fail, and if it fails, explain why.

a) `$ java -cp out com.example.App`
b) `$ java -cp out com/example/App`
c) `$ java -cp out:lib/jackson-core-2.15.0.jar com.example.App`
d) `$ java -classpath out com.example.App`
e) `$ java -jar App.class` (assuming no JAR file exists)

---

### Exercise 3 — Analyze (Medium-Hard)

Consider the following Java source code:

```java
package calc;

public class Calculator {
    public static int add(int a, int b) {
        return a + b;
    }

    public static void main(String[] args) {
        int result = add(10, 20);
        System.out.println(result);
    }
}
```

a) Compile this class and run `javap -c calc.Calculator`. In the bytecode for `add`, which instruction performs the actual addition? What is its mnemonic?

b) In the bytecode for `main`, which instruction is used to load the integer arguments `10` and `20` onto the stack? Why does it use that particular instruction instead of `ldc`?

c) The `System.out.println(result)` call invokes `invokevirtual`. What would be different if `println` were a static method? (Answer conceptually—you do not need to produce bytecode.)

---

### Exercise 4 — Create (Hard)

Write a small Java program spanning two packages:

1. **Package `com.greeting`**: Contains a class `Greeter` with a static method `String greet(String name)` that returns `"Hello, " + name + "!"`.

2. **Package `com.app`**: Contains a class `Main` that calls `Greeter.greet` with a command-line argument (or `"World"` if none is provided) and prints the result.

Complete the following tasks:

a) Write both source files and organize them under a `src/` directory matching the package structure.

b) Compile both files with `javac -d out` in a single command.

c) Create a manifest file (`MANIFEST.MF`) that makes `com.app.Main` the entry point.

d) Package the compiled classes into an executable JAR named `helloapp.jar`.

e) Run the JAR with `java -jar helloapp.jar` and again with `java -jar helloapp.jar Alice`. Verify the output.

---

*End of Chapter: How Java Runs*