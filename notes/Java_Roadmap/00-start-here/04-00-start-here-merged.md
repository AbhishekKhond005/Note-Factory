# 00-start-here

> Merged study notes for **00-start-here**

---

# First Programs

## 1. Learning Objectives

After completing this chapter, you will be able to:

- **Write, compile, and run** a minimal Java program from scratch using a class declaration and the `main` method.
- **Use `System.out.println()` and `System.out.print()`** to display text output on the console, and control formatting with escape sequences.
- **Declare and use variables** of primitive data types (`int`, `double`) and perform arithmetic operations, including understanding integer versus floating-point division.
- **Write conditional logic** using `if`/`else if`/`else` with comparison and logical operators, and read user input with `Scanner`.
- **Construct `for` and `while` loops** to repeat blocks of code, avoiding common pitfalls like off-by-one errors and infinite loops.

---

## 2. Introduction

Writing your first program is a rite of passage — a moment when abstract concepts transform into something you can see, run, and control. You are no longer just *using* technology; you are *telling* it exactly what to do. This chapter lays the foundation for everything that follows: syntax (the grammar of the language), structure (how programs are organised), compilation (translating human-readable code into machine instructions), and execution (running the result).

Think of writing a program like **giving a recipe to a robot chef**. The robot is extremely literal: it follows every instruction exactly as written, in sequence. If you say "chop two carrots," it does exactly that — but if you forget the semicolon at the end of the sentence, the robot gets confused and refuses to move forward. Your job as a programmer is to write clear, precise, unambiguous instructions. By the end of this chapter, you will have written four complete recipes that the robot (your computer) can execute without hesitation.

---

## 3. Core Concepts

### 3.1 Structure of a Java Program

Every Java program lives inside a **class**. A class is a container that groups related code together. To make a program runnable, the class must contain a special method called `main` — this is the entry point where the Java Virtual Machine (JVM) begins execution.

**Anatomy of a Java source file:**

```
ClassName.java                  ← filename must match class name
  ┌──────────────────────────┐
  │ public class ClassName { │   ← class declaration
  │     public static void   │
  │         main(String[]    │   ← main method signature
  │         args) {          │
  │         // your code     │   ← body of the method
  │     }                    │
  │ }                        │
  └──────────────────────────┘
```

Let us break down the key parts:

- **`public class ClassName`** — Declares a class that is publicly accessible. The class name must match the filename (e.g., `HelloWorld.java` must contain `public class HelloWorld`).
- **`public static void main(String[] args)`** — This is the signature of the main method. Every word has a purpose:
  - `public` means other parts of the program can access it.
  - `static` means it belongs to the class itself, not to any particular instance.
  - `void` means it returns no value.
  - `String[] args` is an array of command-line arguments (we will use this later).
- **Curly braces `{}`** define the *body* of the class and the *body* of the method. Everything inside them belongs to that block.
- **Semicolons `;`** terminate each complete statement, like a period ends a sentence.

**A minimal, runnable Java program:**

```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, world!");
    }
}
```

To run this, you would save it as `HelloWorld.java`, then compile with `javac HelloWorld.java` (produces `HelloWorld.class`), and execute with `java HelloWorld`.

---

### 3.2 Output and the Console

The **console** is the text-based window where your program's output appears. In Java, we send output to the console using the `System.out` object, which represents the standard output stream.

**`System.out.println()` vs `System.out.print()`**

| Method | Behaviour | Cursor after execution |
|---|---|---|
| `System.out.println("text")` | Prints text, then moves to the **next line** | Beginning of the next line |
| `System.out.print("text")` | Prints text only, **no newline** | Immediately after the last character |

**Escape sequences** are special character combinations that represent characters you cannot type directly. They begin with a backslash `\`:

| Sequence | Meaning | Example output |
|---|---|---|
| `\n` | Newline (moves to next line) | `"A\nB"` → `A` then `B` on a new line |
| `\t` | Tab (horizontal tab) | `"A\tB"` → `A       B` |
| `\\` | Backslash itself | `"\\n"` → `\n` |
| `\"` | Double quote inside a string | `"She said \"Hi\""` → `She said "Hi"` |

**Java code example demonstrating each variant:**

```java
public class OutputDemo {
    public static void main(String[] args) {
        System.out.print("This is print() — ");
        System.out.print("no newline after this.");
        System.out.println();  // prints an empty line

        System.out.println("This is println() —");
        System.out.println("each call starts on a new line.");

        // Escape sequences
        System.out.println("Line1\nLine2\nLine3");
        System.out.println("Column1\tColumn2\tColumn3");
        System.out.println("She said \"Java is fun!\"");
    }
}
```

**Expected console output:**

```text
This is print() — no newline after this.
This is println() —
each call starts on a new line.
Line1
Line2
Line3
Column1	Column2	Column3
She said "Java is fun!"
```

---

## 4. Worked Examples

### 4.1 Hello World

#### Purpose

The "Hello, World!" program is the traditional first program in any language. It teaches you the minimal structure required for a runnable Java program: class declaration, `main` method, and output. It verifies that your development environment (compiler and runtime) is set up correctly.

#### Step-by-step explanation

```
Line 1:  public class HelloWorld {
Line 2:      public static void main(String[] args) {
Line 3:          System.out.println("Hello, world!");
Line 4:      }
Line 5:  }
```

- **Line 1:** Declares a public class named `HelloWorld`. The filename **must** be `HelloWorld.java`.
- **Line 2:** Declares the `main` method — the entry point. The JVM looks for this exact signature when starting the program.
- **Line 3:** Calls `System.out.println()` to print the string `"Hello, world!"` followed by a newline. The string literal is enclosed in double quotes.
- **Line 4:** Closing brace of the `main` method.
- **Line 5:** Closing brace of the `HelloWorld` class.

#### Java Code Example

```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, world!");
    }
}
```

#### Expected Output

```text
Hello, world!
```

#### Common Mistakes

1. **Missing semicolon** — Every statement in Java ends with `;`. Forgetting it produces a compilation error like `';' expected`.
2. **Wrong capitalisation of `String`** — Java is case-sensitive. Writing `string` (lowercase) or `STRING` will cause an error. It must be `String` (capital S).
3. **Forgetting the `main` method signature** — Writing `public void main(...)` without `static`, or misspelling `main` as `Main` or `mian`, means the JVM cannot find the entry point and you get a "Main method not found" error.

#### Analogy

Writing "Hello, World!" is like **saying your first word as a baby**. It is simple, unimpressive on its own, but it proves that you can make sounds — that your vocal cords work and someone is listening. Every complex sentence you will ever speak is built on the same mechanism. Similarly, every Java program you will ever write is built on the structure demonstrated here.

---

### 4.2 Calculator (Basic Arithmetic)

#### Purpose

This program introduces **variables** (named containers that store values), **data types** (`int` for whole numbers, `double` for decimal numbers), **arithmetic operators** (`+`, `-`, `*`, `/`, `%`), and the concept of **assignment** (storing a value into a variable). It also highlights the difference between **integer division** (which truncates the decimal part) and **floating-point division** (which preserves it).

#### Step-by-step explanation

1. Declare two integer variables `a` and `b` and assign them values.
2. Perform each arithmetic operation, storing results in appropriately typed variables.
3. Print each result with a descriptive label.
4. Demonstrate integer division separately, then show how using a `double` operand changes the result.

#### Java Code Example

```java
public class Calculator {
    public static void main(String[] args) {
        // Declare and initialise variables
        int a = 17;
        int b = 5;

        // Integer arithmetic
        int sum = a + b;
        int difference = a - b;
        int product = a * b;
        int quotient = a / b;   // integer division — truncates!
        int remainder = a % b;  // modulus operator

        System.out.println("a = " + a + ", b = " + b);
        System.out.println("Sum:        " + sum);
        System.out.println("Difference: " + difference);
        System.out.println("Product:    " + product);
        System.out.println("Quotient (int): " + quotient);
        System.out.println("Remainder:  " + remainder);

        // Floating-point division
        double x = 17.0;
        double y = 5.0;
        double preciseQuotient = x / y;
        System.out.println("Quotient (double): " + preciseQuotient);

        // Mixed-type: int / double → double
        double mixed = a / y;  // 17 / 5.0
        System.out.println("Mixed division:    " + mixed);
    }
}
```

#### Expected Output

```text
a = 17, b = 5
Sum:        22
Difference: 12
Product:    85
Quotient (int): 3
Remainder:  2
Quotient (double): 3.4
Mixed division:    3.4
```

#### Common Mistakes

1. **Integer division truncation** — Newcomers expect `17 / 5` to produce `3.4`. But when both operands are `int`, Java performs integer division, discarding the fractional part. The result is `3`. To get `3.4`, at least one operand must be a `double`.
2. **Uninitialised variables** — Declaring a variable without assigning a value (`int x;`) and then trying to use it causes a "variable might not have been initialised" error. Always assign a value before use.
3. **Type mismatch** — Assigning a `double` value to an `int` variable (e.g., `int x = 3.4;`) causes a compilation error because you would lose precision. You must use an explicit **cast** `(int)` if that is intentional.

#### Analogy

Variables are like **labelled jars on a shelf**. Each jar has a label (the variable name) and a capacity (the data type). An `int` jar can hold only whole marbles; a `double` jar can hold fractions of a marble. The arithmetic operators are like actions: `+` pours two jars into one, `/` splits marbles into groups, and `%` tells you the leftover after splitting. Integer division is like saying "how many full groups of 5 marbles can I make from 17 marbles?" — the answer is 3 groups, with 2 left over.

---

### 4.3 Condition Checker

#### Purpose

This program introduces **boolean expressions** (conditions that evaluate to `true` or `false`), **selection statements** (`if`/`else if`/`else`), **comparison operators** (`==`, `!=`, `<`, `>`, `<=`, `>=`), **logical operators** (`&&`, `||`, `!`), and reading user input via the `Scanner` class.

#### Step-by-step explanation

1. Import `java.util.Scanner` so we can read keyboard input.
2. Create a `Scanner` object connected to `System.in` (the keyboard).
3. Prompt the user and read an integer using `nextInt()`.
4. Use `if`/`else if`/`else` to check whether the number is positive, negative, or zero.
5. Use the modulus operator `%` and comparison `==` to check even/odd.
6. Close the `Scanner` to free system resources (good practice).
7. Print results conditionally based on the checks.

#### Java Code Example

```java
import java.util.Scanner;

public class ConditionChecker {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter an integer: ");
        int number = scanner.nextInt();

        // Check positive / negative / zero
        if (number > 0) {
            System.out.println(number + " is POSITIVE.");
        } else if (number < 0) {
            System.out.println(number + " is NEGATIVE.");
        } else {
            System.out.println("The number is ZERO.");
        }

        // Check even / odd (using modulo)
        if (number % 2 == 0) {
            System.out.println(number + " is EVEN.");
        } else {
            System.out.println(number + " is ODD.");
        }

        // Combined condition example: is it a small positive number?
        if (number > 0 && number <= 10) {
            System.out.println(number + " is a small positive number (1–10).");
        }

        scanner.close();
    }
}
```

#### Expected Output (three different runs)

**Run 1:**
```text
Enter an integer: 7
7 is POSITIVE.
7 is ODD.
7 is a small positive number (1–10).
```

**Run 2:**
```text
Enter an integer: -3
-3 is NEGATIVE.
-3 is ODD.
```

**Run 3:**
```text
Enter an integer: 0
The number is ZERO.
0 is EVEN.
```

#### Common Mistakes

1. **Using `=` instead of `==`** — In Java, a single `=` is **assignment**, not comparison. Writing `if (number = 5)` assigns `5` to `number` instead of comparing — and it causes a compilation error because the result is an `int`, not a `boolean`.
2. **Dangling `else`** — When `if` statements are nested, an `else` attaches to the **nearest** `if`. Always use curly braces `{}` to make the intended structure clear, even for single-line bodies.
3. **Missing braces** — Omitting braces for an `if` body means only the **first** statement after the `if` is conditional. This leads to subtle bugs where a second statement always executes.

#### Analogy

An `if` statement is like **a railway switchman**. The train (program execution) approaches a fork in the tracks. The switchman checks a condition (e.g., "is the cargo fragile?"). If `true`, the train goes down one track (the `if` branch); otherwise it goes down the other (the `else` branch). Logical operators (`&&`, `||`) are like having **two switchmen** who must both agree (`&&` — "this **and** that") or where only one needs to agree (`||` — "this **or** that") to flip the switch.

---

### 4.4 Loops Practice

#### Purpose

This section introduces **iteration** — repeating a block of code multiple times. You will learn the **`for` loop** (best when you know the number of repetitions in advance), the **`while` loop** (best when you repeat until a condition changes), and get a brief mention of the **`do-while`** loop (always executes the body at least once).

#### Step-by-step explanation

**Program 1 — For loop countdown:**
1. A `for` loop has three parts in its header: initialisation (`int i = 10`), condition (`i >= 1`), and update (`i--`).
2. The body runs as long as the condition is `true`.
3. Each iteration prints the current value of `i`.

**Program 2 — While loop sum:**
1. Prompt the user for a number `N`.
2. Initialise a running total `sum = 0` and a counter `i = 1`.
3. The `while` loop checks `i <= N` before each iteration.
4. Inside the loop, add `i` to `sum`, then increment `i`.
5. After the loop finishes, print the result.

#### Java Code Examples

**Program 1: For loop — countdown from 10 to 1**

```java
public class Countdown {
    public static void main(String[] args) {
        System.out.println("Countdown beginning...");

        for (int i = 10; i >= 1; i--) {
            System.out.println(i);
        }

        System.out.println("Blast off!");
    }
}
```

**Expected Output:**
```text
Countdown beginning...
10
9
8
7
6
5
4
3
2
1
Blast off!
```

---

**Program 2: While loop — sum 1 to N**

```java
import java.util.Scanner;

public class SumToN {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter a positive integer N: ");
        int n = scanner.nextInt();

        int sum = 0;
        int i = 1;

        while (i <= n) {
            sum = sum + i;   // add current i to sum
            i++;             // increment i — critical!
        }

        System.out.println("The sum of 1 to " + n + " is " + sum + ".");
        scanner.close();
    }
}
```

**Expected Output (with N = 5):**
```text
Enter a positive integer N: 5
The sum of 1 to 5 is 15.
```

---

**Quick Comparison of Loop Types**

| Loop type | When to use | Guaranteed to run at least once? |
|---|---|---|
| `for` | Known number of iterations (e.g., count from 1 to 10) | No (condition checked first) |
| `while` | Unknown iterations, depends on a condition (e.g., keep reading input until user types `quit`) | No (condition checked first) |
| `do-while` | Need the body to execute at least once before checking | Yes (condition checked after) |

A `do-while` loop looks like this:

```java
int x = 1;
do {
    System.out.println(x);
    x++;
} while (x <= 5);
```

#### Common Mistakes

1. **Infinite loops** — If the condition never becomes `false`, the loop runs forever. For example, forgetting `i++` in a `while` loop means `i` never changes, and `i <= n` remains `true` indefinitely. Press `Ctrl+C` (or `Ctrl+Break`) to kill an infinite loop.
2. **Off-by-one errors** — Using `<` instead of `<=` (or vice versa) causes the loop to execute one too few or one too many times. Always trace through with a small value (e.g., N = 1) to verify.
3. **Forgetting the loop variable update** — In a `for` loop, the update happens automatically in the header. In a `while` loop, you must write `i++;` (or whatever update) inside the body, or the loop will never advance.

#### Analogy

A loop is like **a treadmill**. You know you need to take a certain number of steps (a `for` loop — you set the count beforehand) or you need to keep walking until you have burned 200 calories (a `while` loop — you check a condition each time). If you forget to increase the step counter or check the calorie burn, you stay on the treadmill forever — an infinite loop!

The **loop variable** is like a **lap counter** on a track. Each time you complete a lap (one iteration), the counter increments. The race ends when the counter reaches the target number (`for`) or when the coach blows the whistle (`while`).

---

## 5. Summary / Key Takeaways

- Every Java program requires a **class declaration** and a **`main` method** with the exact signature `public static void main(String[] args)`.
- Use **`System.out.println()`** to print text followed by a newline; use **`System.out.print()`** to print without a newline. **Escape sequences** like `\n` and `\t` give you fine control over output formatting.
- **Variables** are named containers with a **data type** (`int`, `double`, etc.). **Assignment** (`=`) stores a value into a variable. Arithmetic operators (`+`, `-`, `*`, `/`, `%`) perform calculations, but **integer division truncates** the fractional part.
- **`if`/`else if`/`else`** statements let your program make decisions based on **boolean expressions** using comparison (`==`, `!=`, `<`, `>`, `<=`, `>=`) and logical (`&&`, `||`, `!`) operators. The **`Scanner`** class reads user input from the keyboard.
- **Loops** repeat code: a **`for`** loop is ideal for a known number of iterations; a **`while`** loop is ideal when the number depends on a runtime condition. Always update the loop variable and verify your boundary conditions to avoid infinite loops and off-by-one errors.
- Every statement ends with a **semicolon** (`;`), and every block of code is enclosed in **curly braces** (`{}`).

---

## 6. Practice Exercises

**Exercise 1 — Personalised Greeting** (Easy)

Modify the Hello World program to declare a `String` variable `name`, assign it your name, and then use `System.out.println` to print `"Hello, <name>!"`. Experiment with both `print` and `println` to understand the difference.

**Exercise 2 — Temperature Converter** (Easy)

Write a program that declares a `double` variable `celsius`, assigns it a value (e.g., `25.0`), converts it to Fahrenheit using the formula `F = C × 9/5 + 32`, and prints both temperatures. **Hint:** Be careful with integer division! Make sure `9/5` is computed as `9.0 / 5.0`.

**Exercise 3 — Number Classifier** (Medium)

Write a program that uses `Scanner` to read an integer from the user. Then classify it using `if`/`else` statements:
- Print `"Negative"` if it is less than 0.
- Print `"Small"` if it is between 0 and 10 (inclusive).
- Print `"Medium"` if it is between 11 and 100 (inclusive).
- Print `"Large"` if it is greater than 100.

**Hint for Exercise 3:** Check the largest range first, or use `else if` in the correct order — the order of your conditions matters!

**Exercise 4 — Multiplication Table** (Medium)

Write a program that uses a `for` loop to print the multiplication table for the number 7 (from 1 to 12). The output should look like:

```text
7 × 1 = 7
7 × 2 = 14
...
7 × 12 = 84
```

**Hint for Exercise 4:** You can build a single string inside the loop using string concatenation (`+`) and print it each iteration.

**Exercise 5 — Guess the Number** (Challenging)

Write a program that:
1. Generates a random number between 1 and 100 using `int secret = (int)(Math.random() * 100) + 1;`
2. Uses a `while` loop to repeatedly ask the user to guess the number (use `Scanner`).
3. After each guess, tells the user if their guess is `"Too high"` or `"Too low"`.
4. When the user guesses correctly, prints `"Correct!"` and the number of attempts it took.
5. The loop should stop when the guess is correct.

This combines `Scanner`, `while` loops, `if`/`else`, and variables — a great capstone for this chapter.

---

## 7. Self-Check Questions

Test your understanding by answering these questions, then click the `<details>` block to reveal the answer.

**Question 1**

What is the output of the following code snippet?

```java
System.out.print("A");
System.out.println("B");
System.out.print("C");
```

<details>
<summary>Click to reveal answer</summary>

```text
AB
C
```
The first `print` outputs "A" without a newline. Then `println("B")` outputs "B" and moves to the next line. Finally `print("C")` outputs "C" on the new line.
</details>

---

**Question 2**

What value is stored in the variable `result` after this code executes?

```java
int result = 19 / 5;
```

<details>
<summary>Click to reveal answer</summary>

`3`. Both `19` and `5` are `int` literals, so Java performs integer division and truncates the fractional part. `19 / 5 = 3.8`, which truncates to `3`.
</details>

---

**Question 3**

True or False: The following `while` loop will execute exactly 10 times, printing numbers 1 through 10.

```java
int i = 0;
while (i < 10) {
    System.out.println(i);
    i++;
}
```

<details>
<summary>Click to reveal answer</summary>

**False.** The loop will execute 10 times, but it prints numbers 0 through 9, not 1 through 10, because `i` starts at `0` and the condition is `i < 10`. To print 1 through 10, start `i` at `1` and change the condition to `i <= 10`.
</details>

---

**Question 4**

What is the difference between `=` and `==` in Java? Why does `if (x = 5)` cause a compilation error?

<details>
<summary>Click to reveal answer</summary>

`=` is the **assignment operator** — it stores a value into a variable (e.g., `x = 5` means "store the value 5 into x"). `==` is the **equality comparison operator** — it checks whether two values are equal and returns a `boolean` (`true` or `false`). Writing `if (x = 5)` causes a compilation error because the condition in an `if` statement must be a `boolean` expression, but `x = 5` evaluates to an `int` (the value 5), not a `boolean`.
</details>

---

**Question 5**

What does the following program print?

```java
public class Mystery {
    public static void main(String[] args) {
        for (int i = 1; i <= 3; i++) {
            for (int j = 1; j <= i; j++) {
                System.out.print("*");
            }
            System.out.println();
        }
    }
}
```

<details>
<summary>Click to reveal answer</summary>

```text
*
**
***
```
This is a **nested loop**. The outer loop runs 3 times (i = 1, 2, 3). The inner loop runs `i` times each iteration, printing `i` asterisks on a single line. After each inner loop finishes, `println()` moves to the next line.
</details>

---

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

---

## Chapter: language basics

### Learning Objectives

By the end of this chapter, you will be able to:

- Declare and initialize variables of each primitive Java type and explain their memory footprints
- Apply type casting rules—both implicit and explicit—to avoid precision loss and data corruption
- Evaluate expressions involving arithmetic, relational, logical, and bitwise operators with correct precedence
- Read console input using the `Scanner` class and produce formatted output using `printf` and `String.format`
- Distinguish between primitive and reference types, and explain how autoboxing and unboxing work under the hood
- Write well-formatted, self-documenting Java code using consistent naming conventions, indentation, and Javadoc comments
- Identify and avoid common Java pitfalls such as floating-point inaccuracy, string comparison with `==`, and the `nextInt()`/`nextLine()` interleaving bug

---

### 1. Introduction

Imagine you are moving into a new apartment. You have boxes for different kinds of items: a sturdy box labeled "Books" for your heavy textbooks, a small plastic bin labeled "Spices" for the kitchen, and a fragile-sticker box labeled "Glassware" for your coffee mugs. In Java, **variables** are those boxes, and **data types** are the labels that tell you what kind of thing each box can hold. **Operators** are the tools you use to combine, compare, and transform the contents of those boxes—like a kitchen scale, a measuring cup, or a knife. Without mastering these building blocks, you cannot hope to assemble a working program, let alone a large object-oriented system.

This chapter lays the absolute foundation of every Java program you will ever write. We will begin with **variables and data types** (the boxes and labels), move through **operators** (the tools), examine **type casting** (pouring contents between differently sized containers), explore **console input and output** (talking to the user), and finish with **comments and formatting conventions** (keeping your workspace tidy and readable). Each section builds on the last, and by the end you will be able to write simple, correct, interactive Java programs from scratch.

---

### 2. Variables and Data Types

#### 2.1 Primitive Data Types

Java defines exactly eight primitive types. They are the atoms from which all other data structures are composed. Unlike objects, primitives store values directly in memory—there is no reference indirection.

| Data Type | Size      | Range / Values                              | Default Value | Example Literal           |
|-----------|-----------|---------------------------------------------|---------------|---------------------------|
| `byte`    | 8 bits    | –128 to 127                                 | `0`           | `byte b = 100;`           |
| `short`   | 16 bits   | –32,768 to 32,767                           | `0`           | `short s = 3_000;`        |
| `int`     | 32 bits   | –2³¹ to 2³¹–1 (≈ ±2.1×10⁹)                 | `0`           | `int i = 42_000;`         |
| `long`    | 64 bits   | –2⁶³ to 2⁶³–1                              | `0L`          | `long l = 99_000_000L;`   |
| `float`   | 32 bits   | ±1.4×10⁻⁴⁵ to ±3.4×10³⁸, ~7 decimal digits | `0.0f`        | `float f = 3.14f;`        |
| `double`  | 64 bits   | ±4.9×10⁻³²⁴ to ±1.8×10³⁰⁸, ~15 digits      | `0.0d`        | `double d = 3.14159;`     |
| `char`    | 16 bits   | Unicode code point 0 to 65,535              | `'\u0000'`    | `char c = 'A';`           |
| `boolean` | not exact | `true` or `false`                           | `false`       | `boolean ok = true;`      |

> **Gotcha!** Underscores in numeric literals (e.g., `99_000_000L`) are ignored by the compiler. Use them to make large numbers readable—but never at the start or end of a literal.

```java
public class PrimitiveDemo {
    public static void main(String[] args) {
        // byte — small counters
        byte level = 127;
        System.out.println("Byte value: " + level);   // 127

        // short — moderate ranges
        short temperature = -5_000;
        System.out.println("Short value: " + temperature); // -5000

        // int — the default integer type
        int population = 8_000_000_000; // error: out of range!
        int population = 8_000_000;      // compiles fine
        System.out.println("Int population: " + population);

        // long — when int is not enough
        long worldPopulation = 8_000_000_000L;
        System.out.println("Long population: " + worldPopulation);

        // float — careful with precision (see Gotcha below)
        float pi = 3.1415926535f;          // loses digits
        System.out.println("Float pi: " + pi); // 3.1415927

        // double — default for floating-point literals
        double precisePi = 3.141592653589793;
        System.out.println("Double pi: " + precisePi);

        // char — a single Unicode character
        char letter = 'Z';
        char omega = '\u03A9';   // Greek capital letter Omega
        System.out.println("Letters: " + letter + " " + omega);

        // boolean — truth values
        boolean isJavaFun = true;
        System.out.println("Java is fun: " + isJavaFun);
    }
}
/* Expected output:
   Byte value: 127
   Short value: -5000
   Int population: 8000000
   Long population: 8000000000
   Float pi: 3.1415927
   Double pi: 3.141592653589793
   Letters: Z Ω
   Java is fun: true
*/
```

> **Gotcha!** Floating-point arithmetic is not exact. Try `0.1 + 0.2` in any language that follows IEEE 754, and you will get `0.30000000000000004`, not `0.3`. This is not a bug—it is a consequence of representing base-10 fractions in base-2. **Never compare floating-point values with `==`.** Instead, check that the absolute difference is less than a small epsilon, e.g., `Math.abs(a - b) < 1e-9`.

```java
public class FloatPrecision {
    public static void main(String[] args) {
        double a = 0.1;
        double b = 0.2;
        double sum = a + b;
        System.out.println("0.1 + 0.2 = " + sum);
        System.out.println("Are they equal? " + (sum == 0.3)); // false

        // Safe comparison:
        double epsilon = 1e-9;
        System.out.println("Close enough? " + (Math.abs(sum - 0.3) < epsilon)); // true
    }
}
/* Expected output:
   0.1 + 0.2 = 0.30000000000000004
   Are they equal? false
   Close enough? true
*/
```

> **Why This Matters:** Choosing the right primitive type affects memory, performance, and correctness. Large-scale systems (databases, game engines, scientific computing) save gigabytes by using `byte` or `short` instead of `int` when the range is known to be small. Floating-point rounding has caused real-world disasters, including a Patriot missile failure in 1991 (due to a time-accumulated precision error).

---

#### 2.2 Reference Data Types

A **reference type** variable does not hold the actual data—it holds the memory address (a reference) of an object stored elsewhere on the **heap**. Primitives store values directly on the **stack**. This distinction is fundamental.

| Property            | Primitive             | Reference                          |
|---------------------|-----------------------|------------------------------------|
| Stores              | Value directly        | Address (pointer) to an object     |
| Memory location     | Stack                 | Object on heap, reference on stack |
| Default value       | 0 / 0.0 / false       | `null`                             |
| Can call methods    | No                    | Yes                                |

The most common reference type you will encounter early on is `String`.

```java
public class StringDemo {
    public static void main(String[] args) {
        // Declaration and initialization
        String greeting = "Hello, World!";
        String name = "Alice";

        // Concatenation with '+'
        String message = greeting + " My name is " + name;
        System.out.println(message);

        // Calling a method
        int len = message.length();
        System.out.println("Length: " + len);

        // null and the dreaded NullPointerException
        String empty = null;
        // System.out.println(empty.length());  // ← NullPointerException at runtime!
    }
}
/* Expected output:
   Hello, World! My name is Alice
   Length: 37
*/
```

> **Warning!** Any reference variable can hold `null`. Calling a method on a `null` reference throws a `NullPointerException`. This is one of the most common runtime errors in Java. **Always check for `null`** when a reference might not be initialized:
> ```java
> if (name != null) {
>     System.out.println(name.length());
> }
> ```

> **Why This Matters:** Understanding the primitive-vs-reference distinction explains why `==` works differently for `int` (value comparison) vs `String` (reference comparison—see Section 3.2). It also clarifies why passing an object to a method can modify the original, while passing a primitive never does.

---

#### 2.3 Variable Naming and Scope

Java has strict rules and strong conventions for variable names.

**Rules** (enforced by the compiler):
- Must begin with a letter, `$`, or `_` (but never start with `$` or `_` in practice—they are reserved for special purposes).
- After the first character, may contain letters, digits, `$`, or `_`.
- Cannot be a **reserved keyword** (`int`, `class`, `if`, `null`, etc.).
- Case-sensitive: `count` and `Count` are different.

**Conventions** (not enforced but universal):
- Use **camelCase**: `totalScore`, `firstName`, `maxValue`.
- Class names use **PascalCase**: `String`, `ArrayList`, `CustomerReport`.
- Constants use **UPPER_SNAKE_CASE**: `MAX_SIZE`, `PI`, `DEFAULT_TIMEOUT`.
- Choose **meaningful, pronounceable names**. `int x` is acceptable for a loop counter; `double qty` is acceptable for quantity. Avoid `int a, b, c, d;` in business logic.

**Scope** determines where a variable is visible. The primary rule is **block scope**: a variable declared inside a pair of braces `{ }` is only accessible within that block.

```java
public class ScopeDemo {
    public static void main(String[] args) {
        int outer = 10;
        {
            int inner = 20;
            System.out.println("Inside block — outer: " + outer + ", inner: " + inner);
            outer = 30;  // allowed — outer's scope includes this block
        }
        // System.out.println(inner); // COMPILE ERROR: inner is out of scope
        System.out.println("Outside block — outer: " + outer);
    }
}
/* Expected output:
   Inside block — outer: 10, inner: 20
   Outside block — outer: 30
*/
```

| Scope Type      | Where Declared                       | Accessible From                                     |
|-----------------|--------------------------------------|-----------------------------------------------------|
| **Local**       | Inside a method or block             | After declaration until the end of the enclosing `}` |
| **Parameter**   | Method parameter list                | Entire method body                                  |
| **Instance**    | Inside a class, outside any method   | All non-static methods of the class                 |
| **Class (static)** | `static` field in a class        | Anywhere the class is visible (via class name)      |

> **Why This Matters:** Proper scoping prevents accidental variable reuse and makes code easier to reason about. A variable should have the **narrowest possible scope**—declare it inside the loop if it is only used there. This reduces bugs and improves readability.

---

### 3. Operators

#### 3.1 Arithmetic Operators

Java provides the usual suspects: `+`, `-`, `*`, `/`, `%`. Two nuances deserve special attention: **integer division truncation** and **modulus with negatives**.

```java
public class ArithmeticDemo {
    public static void main(String[] args) {
        int a = 17;
        int b = 5;

        int sum = a + b;         // 22
        int diff = a - b;        // 12
        int product = a * b;     // 85
        int quotient = a / b;    // 3   ← truncation toward zero
        int remainder = a % b;   // 2

        System.out.println("17 / 5 = " + quotient + " (remainder " + remainder + ")");

        // Negative modulus
        System.out.println("17 % -5 = " + (17 % -5));   //  2
        System.out.println("-17 % 5 = " + (-17 % 5));   // -2

        // Compound assignment
        int x = 10;
        x += 5;   // x = x + 5 → 15
        x *= 2;   // x = x * 2 → 30
        System.out.println("After compound ops: " + x);
    }
}
/* Expected output:
   17 / 5 = 3 (remainder 2)
   17 % -5 = 2
   -17 % 5 = -2
   After compound ops: 30
*/
```

> **Best Practice:** In Java, the result of `%` has the sign of the **dividend** (left-hand side). This is consistent with truncation toward zero.

**Compound assignment operators** (`+=`, `-=`, `*=`, `/=`, `%=`) perform the operation and then assign the result. They differ from simple `x = x + 5` only in that they implicitly cast the result to the type of the left-hand side, which can matter in mixed-type expressions:

```java
int i = 5;
i += 3.7;   // equivalent to i = (int)(i + 3.7) → 8 (compiles)
// i = i + 3.7;  // COMPILE ERROR — cannot assign double to int
```

> **Why This Matters:** Integer division crops up constantly—computing averages, pagination, grid positions. Misunderstanding truncation leads to off-by-one errors. Compound assignments are idiomatic in Java; learn to read them fluently.

---

#### 3.2 Relational and Logical Operators

**Relational operators** compare two values and produce a `boolean`.

| Operator | Meaning              |
|----------|----------------------|
| `==`     | Equal to             |
| `!=`     | Not equal to         |
| `<`      | Less than            |
| `>`      | Greater than         |
| `<=`     | Less than or equal   |
| `>=`     | Greater than or equal|

**Logical operators** combine boolean expressions.

| Operator | Meaning         | Short-circuit? |
|----------|-----------------|----------------|
| `&&`     | AND             | Yes            |
| `||`     | OR              | Yes            |
| `!`      | NOT (unary)     | N/A            |
| `&`      | AND (non-short) | No             |
| `|`      | OR (non-short)  | No             |

**Short-circuit evaluation** means the second operand is evaluated only if necessary. For `&&`, if the left operand is `false`, the result is `false` regardless of the right operand—so Java skips evaluating it entirely.

```java
public class ShortCircuitDemo {
    public static void main(String[] args) {
        int x = 5;
        int y = 0;

        // Without short-circuit: would divide by zero
        boolean result = (y != 0) && (x / y > 1);
        System.out.println("Result (safe): " + result); // false, no exception

        // With non-short-circuit & : crashes!
        // boolean crash = (y != 0) & (x / y > 1);  // ArithmeticException!
    }
}
/* Expected output:
   Result (safe): false
*/
```

> **Warning! Always compare strings with `.equals()`, never with `==`.**
> The `==` operator compares **references** for objects, not contents. Two `String` objects may contain identical text but live at different memory addresses.
> ```java
> String s1 = new String("hello");
> String s2 = new String("hello");
> System.out.println(s1 == s2);        // false (different objects)
> System.out.println(s1.equals(s2));   // true  (same content)
> ```
> The exception is **string interning**: string literals are automatically pooled, so `"hello" == "hello"` happens to be `true`. Never rely on this—always use `.equals()` for content comparison.

> **Why This Matters:** Short-circuit evaluation is the foundation of safe guards like `(obj != null && obj.method())`. Logical operators form the basis of every `if`, `while`, and `for` condition. Using `&` where `&&` is intended is a subtle bug that can crash your program.

---

#### 3.3 Bitwise and Shift Operators (Overview)

These operators work directly on the binary representations of integer types (`int`, `long`). They are essential for systems programming, cryptography, and performance-sensitive code.

| Operator | Name           | Example          |
|----------|----------------|------------------|
| `&`      | Bitwise AND    | `0b1100 & 0b1010` → `0b1000` |
| `|`      | Bitwise OR     | `0b1100 | 0b1010` → `0b1110` |
| `^`      | Bitwise XOR    | `0b1100 ^ 0b1010` → `0b0110` |
| `~`      | Bitwise NOT    | `~0b1100` → `...11110011` (two's complement) |
| `<<`     | Left shift     | `5 << 2` → `20` (multiply by 4) |
| `>>`     | Signed right shift | `-16 >> 2` → `-4` (sign-extended) |
| `>>>`    | Unsigned right shift | `-16 >>> 2` → large positive (zero-extended) |

**Practical use case: flag checking**

```java
public class BitmaskDemo {
    // Permission flags
    static final int READ    = 1 << 0; // 001
    static final int WRITE   = 1 << 1; // 010
    static final int EXECUTE = 1 << 2; // 100

    public static void main(String[] args) {
        int permissions = READ | WRITE; // 011

        System.out.println("Can read?   " + ((permissions & READ)    != 0));  // true
        System.out.println("Can write?  " + ((permissions & WRITE)   != 0));  // true
        System.out.println("Can exec?   " + ((permissions & EXECUTE) != 0));  // false
    }
}
/* Expected output:
   Can read?   true
   Can write?  true
   Can exec?   false
*/
```

> **Why This Matters:** Bitwise operations are the fastest possible operations on a CPU. They are used in compression, encryption, graphics, network protocols, and low-level hardware control. Most application developers use them rarely, but when you need them, there is no substitute.

---

#### 3.4 Operator Precedence and Associativity

When an expression contains multiple operators, Java uses a fixed **precedence** hierarchy to decide which operation happens first. When two operators have the same precedence, **associativity** (left-to-right or right-to-left) breaks the tie.

| Precedence | Operator(s)                     | Associativity   |
|------------|---------------------------------|-----------------|
| 1 (highest)| `++` `--` `+` `-` `~` `!` (unary) | right-to-left |
| 2          | `*` `/` `%`                     | left-to-right   |
| 3          | `+` `-`                         | left-to-right   |
| 4          | `<<` `>>` `>>>`                 | left-to-right   |
| 5          | `<` `>` `<=` `>=` `instanceof` | left-to-right   |
| 6          | `==` `!=`                       | left-to-right   |
| 7          | `&` (bitwise AND)               | left-to-right   |
| 8          | `^` (bitwise XOR)               | left-to-right   |
| 9          | `|` (bitwise OR)                | left-to-right   |
| 10         | `&&`                            | left-to-right   |
| 11         | `||`                            | left-to-right   |
| 12         | `?:` (ternary)                  | right-to-left   |
| 13 (lowest)| `=` `+=` `-=` etc.              | right-to-left   |

```java
public class PrecedenceDemo {
    public static void main(String[] args) {
        // Without parentheses — misleading
        int result = 5 + 3 * 4 - 8 / 2;
        // Evaluated as: 5 + (3 * 4) - (8 / 2) = 5 + 12 - 4 = 13
        System.out.println("Without parens: " + result);

        // What we actually intended:
        int intended = ((5 + 3) * (4 - 8)) / 2;
        // Evaluated as: (8 * (-4)) / 2 = -32 / 2 = -16
        System.out.println("With parens: " + intended);
    }
}
/* Expected output:
   Without parens: 13
   With parens: -16
*/
```

> **Best Practice:** Do not rely on memorising the full precedence table. **Use parentheses liberally** to make your intent clear to both the compiler and human readers. Code is read far more often than it is written.

> **Why This Matters:** Misplaced operator precedence has caused critical bugs in production software, including incorrect financial calculations and security vulnerabilities. When in doubt, parenthesise.

---

### 4. Type Casting

Think of casting as pouring liquid between containers. If you pour from a small cup into a large pitcher (**widening**), nothing spills—every drop fits. If you pour from a large pitcher into a small cup (**narrowing**), liquid overflows and is lost forever.

#### 4.1 Implicit (Widening) Casting

Widening conversions happen automatically when you assign a value of a smaller type to a variable of a larger type. No data is lost because the target type has a larger range.

```
byte → short → int → long → float → double
```

```java
public class WideningDemo {
    public static void main(String[] args) {
        byte b = 42;
        short s = b;   // byte → short (implicit)
        int i = s;     // short → int (implicit)
        long l = i;    // int → long (implicit)
        float f = l;   // long → float (implicit — possible precision loss for very large longs!)
        double d = f;  // float → double (implicit)

        System.out.println("byte " + b + " → short " + s + " → int " + i
            + " → long " + l + " → float " + f + " → double " + d);

        // Automatic promotion in expressions:
        int x = 5;
        double y = 2.5;
        // x is promoted to double before addition
        double result = x + y;  // 7.5
        System.out.println("Promotion: " + x + " + " + y + " = " + result);
    }
}
/* Expected output:
   byte 42 → short 42 → int 42 → long 42 → float 42.0 → double 42.0
   Promotion: 5 + 2.5 = 7.5
*/
```

> **Gotcha!** `long → float` is a widening conversion, but `float` has only ~7 significant decimal digits vs `long`'s 19. For very large `long` values (greater than 2²⁴ or less than –2²⁴), precision is lost even though the cast is "implicit."

---

#### 4.2 Explicit (Narrowing) Casting

Narrowing a larger type into a smaller type requires an explicit **cast operator**: `(targetType) value`. Data loss may occur—this is your way of telling the compiler, "I know the risk; do it anyway."

```java
public class NarrowingDemo {
    public static void main(String[] args) {
        double pi = 3.141592653589793;
        int truncated = (int) pi;              // drops fractional part → 3
        System.out.println("(int) " + pi + " = " + truncated);

        // Overflow example
        int large = 1_000_000;
        byte tooSmall = (byte) large;          // truncates to 8 bits
        System.out.println("(byte) " + large + " = " + tooSmall); // nonsense value

        // char and numeric types
        char ch = (char) 65;                   // 'A'
        int ascii = (int) 'Z';                 // 90
        System.out.println("(char)65 = " + ch);
        System.out.println("(int)'Z' = " + ascii);

        // float → int truncation (not rounding)
        float price = 3.99f;
        int dollars = (int) price;             // 3, not 4
        System.out.println("(int) " + price + " = " + dollars);
    }
}
/* Expected output:
   (int) 3.141592653589793 = 3
   (byte) 1000000 = 64
   (char)65 = A
   (int)'Z' = 90
   (int) 3.99 = 3
*/
```

> **Best Practice:** When narrowing, always ask yourself: **could the value ever exceed the target type's range?** If yes, guard with a range check before casting or use a safe conversion method (e.g., `Math.toIntExact(long)` throws `ArithmeticException` on overflow).

> **Why This Matters:** Narrowing casts appear in network protocols (reading bytes into chars), file I/O, graphics (colour channel clamping), and when interfacing with older APIs. Knowing exactly what is lost—and when—prevents silent data corruption.

---

#### 4.3 Wrapper Classes and Autoboxing/Unboxing

Every primitive type has a corresponding **wrapper class** in `java.lang`:

| Primitive | Wrapper   |
|-----------|-----------|
| `byte`    | `Byte`    |
| `short`   | `Short`   |
| `int`     | `Integer` |
| `long`    | `Long`    |
| `float`   | `Float`   |
| `double`  | `Double`  |
| `char`    | `Character` |
| `boolean` | `Boolean` |

Wrappers let you treat primitives as objects—essential for use in generics (`ArrayList<Integer>`, not `ArrayList<int>`).

**Autoboxing** is the automatic conversion from a primitive to its wrapper. **Unboxing** is the reverse.

```java
import java.util.ArrayList;

public class AutoboxingDemo {
    public static void main(String[] args) {
        // Autoboxing: int → Integer
        Integer boxed = 42;   // compiler inserts Integer.valueOf(42)
        System.out.println("Boxed value: " + boxed);

        // Unboxing: Integer → int
        int unboxed = boxed;  // compiler inserts boxed.intValue()
        System.out.println("Unboxed value: " + unboxed);

        // Practical use: collections
        ArrayList<Integer> numbers = new ArrayList<>();
        numbers.add(10);       // autoboxing
        numbers.add(20);
        int sum = numbers.get(0) + numbers.get(1);  // unboxing + addition
        System.out.println("Sum: " + sum);

        // Null danger
        Integer maybeNull = null;
        // int crash = maybeNull;  // NullPointerException at unboxing!
    }
}
/* Expected output:
   Boxed value: 42
   Unboxed value: 42
   Sum: 30
*/
```

> **Performance Caveat!** Autoboxing/unboxing looks free, but it is not. Each boxing operation creates a new object (though `Integer` caches values –128 to 127). **In tight loops, use primitives.**
> ```java
> // SLOW — creates 10 million Integer objects
> Long sum = 0L;
> for (long i = 0; i < 10_000_000; i++) {
>     sum += i;   // autoboxing on every iteration
> }
> // FAST — uses primitive throughout
> long sum = 0L;
> for (long i = 0; i < 10_000_000; i++) {
>     sum += i;
> }
> ```

> **Why This Matters:** Collections in Java (which we cover in later chapters) work only with objects. Wrappers bridge the primitive/object divide. Understanding autoboxing explains both the convenience (terse code) and the pitfalls (null pointer on unboxing, performance overhead).

---

### 5. Input / Output

#### 5.1 Console Output

Java provides three main methods for outputting text to the console.

| Method                 | Behaviour                                        |
|------------------------|--------------------------------------------------|
| `System.out.print(x)`  | Prints `x` without a trailing newline            |
| `System.out.println(x)`| Prints `x` followed by a newline                 |
| `System.out.printf(fmt, args...)` | Prints formatted text using format specifiers |

```java
public class OutputDemo {
    public static void main(String[] args) {
        String name = "Alice";
        int age = 20;
        double height = 1.75;

        // print vs println
        System.out.print("Hello, ");
        System.out.println(name);

        // printf with format specifiers
        System.out.printf("Name: %s, Age: %d, Height: %.2f m%n", name, age, height);

        // Common format specifiers:
        // %d  — integer
        // %f  — floating-point
        // %s  — string
        // %n  — newline (platform-independent)
        // %.2f — two decimal places
    }
}
/* Expected output:
   Hello, Alice
   Name: Alice, Age: 20, Height: 1.75 m
*/
```

> **Why This Matters:** `printf`-style formatting is standard across many languages (C, C++, Python, Ruby). Master it once, use it everywhere. It is essential for producing aligned tables, reports, and debug output.

---

#### 5.2 Console Input

Reading input from the console is done with the `Scanner` class from `java.util`.

```java
import java.util.Scanner;

public class InputDemo {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter your name: ");
        String name = scanner.nextLine();

        System.out.print("Enter your age: ");
        int age = scanner.nextInt();       // leaves newline in buffer!

        // Workaround for nextLine() after nextInt()
        scanner.nextLine();                // consume leftover newline

        System.out.print("Enter your height in meters: ");
        double height = scanner.nextDouble();
        scanner.nextLine();                // consume newline (good practice)

        scanner.close();

        System.out.printf("Hello %s! You are %d years old and %.2f m tall.%n",
                          name, age, height);
    }
}
```

> **Gotcha! The `nextInt()` / `nextLine()` trap.**  
> `nextInt()` reads only the integer token and leaves the `\n` (newline) character in the input buffer. The subsequent `nextLine()` immediately consumes that leftover newline and returns an empty string. **Always add an extra `scanner.nextLine()` after `nextInt()` or `nextDouble()`** if the next read is a `nextLine()`.

Sample run:
```
Enter your name: Alice
Enter your age: 20
Enter your height in meters: 1.75
Hello Alice! You are 20 years old and 1.75 m tall.
```

> **Why This Matters:** Interactive CLI programs are the simplest way to test logic, and every developer writes them. Understanding `Scanner`'s token-based behaviour prevents the most common input bug in beginner Java code.

---

#### 5.3 Formatted Strings

`String.format()` works identically to `printf` but returns a `String` instead of printing to the console. This is useful for building strings that you might log, store, or display later.

```java
public class StringFormatDemo {
    public static void main(String[] args) {
        String name = "Bob";
        int score = 95;
        double average = 88.7;

        String report = String.format("Student: %s | Score: %d/100 | Average: %.1f%%",
                                      name, score, average);
        System.out.println(report);

        // Reusing the formatted string
        String header = String.format("%-15s %5s %8s%n", "Name", "Score", "Average");
        System.out.println(header);
    }
}
/* Expected output:
   Student: Bob | Score: 95/100 | Average: 88.7%
   Name             Score   Average
*/
```

> **Best Practice:** Prefer `String.format()` when you need to build a string for later use, and `printf()` when you want immediate console output. The format specifiers (`%s`, `%d`, `%f`, `%n`, `%-15s` for left-aligned width-15) are identical between the two.

---

### 6. Comments and Formatting

#### 6.1 Comment Styles

Java supports three comment styles:

| Style      | Syntax           | Purpose                         |
|------------|------------------|---------------------------------|
| Single-line| `// text`        | Brief inline explanation        |
| Multi-line | `/* ... */`      | Block explanation, temporary disable |
| Javadoc    | `/** ... */`     | API documentation (processed by `javadoc` tool) |

```java
/**
 * Calculates the body mass index (BMI) given weight and height.
 *
 * @param weightKg  weight in kilograms (must be > 0)
 * @param heightM   height in meters (must be > 0)
 * @return BMI value rounded to one decimal place, or -1 if inputs are invalid
 */
public static double calculateBMI(double weightKg, double heightM) {
    if (weightKg <= 0 || heightM <= 0) {
        return -1; // invalid input
    }
    double bmi = weightKg / (heightM * heightM);
    // Round to one decimal place
    return Math.round(bmi * 10.0) / 10.0;
}
```

Javadoc comments start with `/**` (not `/*`) and contain **tags** like `@param` and `@return`. When you run the `javadoc` tool, these comments are automatically turned into HTML documentation—exactly like the official Java API docs.

> **Best Practice:** Write Javadoc for every `public` method of every class. For `private` methods, use `//` comments only if the logic is non-obvious. Good Javadoc explains **what** the method does, **what** the parameters mean, and **what** is returned—not how the implementation works.

---

#### 6.2 Code Formatting Conventions

Consistent formatting is not optional—it is a professional requirement. The Java community overwhelmingly follows these conventions:

| Element             | Convention                          | Example                        |
|---------------------|-------------------------------------|--------------------------------|
| **Indentation**     | 4 spaces (no tabs)                  | `····int x = 1;`               |
| **Braces**          | K&R style: opening brace on same line | `if (x > 0) {`              |
| **Blank lines**     | One between logical sections        | Between method declarations    |
| **Classes**         | PascalCase                          | `CustomerReport`               |
| **Methods/variables** | camelCase                        | `getTotal()`, `firstName`      |
| **Constants**       | UPPER_SNAKE_CASE                    | `MAX_RETRIES`, `DEFAULT_PORT`  |
| **Package names**   | All lowercase, no underscores       | `com.example.myapp`            |

```java
// Well-formatted example
public class FormattingExample {
    private static final int MAX_RETRIES = 3;

    private String name;

    public FormattingExample(String name) {
        this.name = name;
    }

    public String greet() {
        return "Hello, " + name + "!";
    }

    public static void main(String[] args) {
        FormattingExample example = new FormattingExample("Alice");
        System.out.println(example.greet());
    }
}
```

> **Why This Matters:** In industry, you will read far more code than you write. Consistent formatting means you can understand a teammate's code without stylistic friction. Many teams enforce formatting automatically with tools like `checkstyle` or `spotless`.

---

#### 6.3 Self-Documenting Code

The best comment is the one you do not need to write—because the code itself is clear enough.

**Before** (cryptic, comment-dependent spaghetti):

```java
// Process data
int a = 0;
for (int i = 0; i < list.length; i++) {
    // Add to total if valid
    if (list[i] != null && list[i] > 0) {
        a = a + list[i]; // accumulate
    }
}
int b = a / list.length; // get average
```

**After** (self-documenting, no unnecessary comments):

```java
double computeAveragePositiveSalary(int[] salaries) {
    int sum = 0;
    int count = 0;
    for (int salary : salaries) {
        if (salary > 0) {
            sum += salary;
            count++;
        }
    }
    return count == 0 ? 0.0 : (double) sum / count;
}
```

> **Best Practice:** Follow these guidelines to reduce the need for comments:
> 1. Use descriptive variable names (`salaries` not `list`, `sum` not `a`).
> 2. Extract blocks of logic into small, well-named methods.
> 3. Avoid "obvious" comments like `// increment i` above `i++`.
> 4. Reserve comments for **why** (business rules, workarounds, gotchas), not **what** or **how**.

> **Why This Matters:** Self-documenting code is cheaper to maintain. Comments can become stale (code changes, comments stay outdated), but clean code tells the truth. When you apply for internships or jobs, your code style is part of your portfolio.

---

### End-of-Chapter Exercises

#### Multiple-Choice Questions

**1.** Which of the following is the default value of a `boolean` variable in Java?  
a) `true`  
b) `false`  
c) `null`  
d) `0`

**2.** What is the output of `System.out.println(10 / 4);`?  
a) `2.5`  
b) `2`  
c) `2.0`  
d) Compiler error

**3.** Which operator is used to compare the *contents* of two `String` objects?  
a) `==`  
b) `!=`  
c) `.equals()`  
d) `compare()`

**4.** After executing `int x = (int) 3.99;`, what is the value of `x`?  
a) `3`  
b) `4`  
c) `3.99`  
d) `3.0`

**5.** What happens when the following code runs?  
```java
Integer n = null;
int m = n;
```
a) `m` is assigned `0`  
b) `m` is assigned `null`  
c) `NullPointerException`  
d) Compiler error

**Answer Key:**  
1. b (`false`)  
2. b (`2` — integer division truncates)  
3. c (`.equals()`)  
4. a (`3` — truncation, not rounding)  
5. c (`NullPointerException` at unboxing)

---

#### Coding Exercises

**Exercise 1 (Easy): Temperature Converter**

Write a program that:
1. Prompts the user for a temperature in Fahrenheit (using `Scanner`).
2. Converts it to Celsius using the formula: `C = (F - 32) * 5 / 9`.
3. Prints the result formatted to one decimal place.

*Focus on: console I/O, integer vs floating-point division, printf formatting.*

---

**Exercise 2 (Medium): Compound Interest Calculator**

Write a program that:
1. Prompts for principal (`double`), annual rate as a percentage (`double`), number of years (`int`), and compounding frequency per year (`int`).
2. Computes the final amount using the formula:  
   `A = P * (1 + r/n)^(n*t)`  
   where `r` is the decimal rate (e.g., 5% → 0.05).
3. Uses `Math.pow()` and prints the result with two decimal places.
4. Uses descriptive variable names and includes a Javadoc comment for the core calculation method.

*Focus on: mixed-type expressions, Math library, formatting, self-documenting code.*

---

**Exercise 3 (Hard): Bitmask Permission System**

Write a program that:
1. Defines permission constants using bit shifts: `READ = 1 << 0`, `WRITE = 1 << 1`, `EXECUTE = 1 << 2`, `DELETE = 1 << 3`.
2. Defines three user roles as bitmasks:
   - `ADMIN = READ | WRITE | EXECUTE | DELETE`
   - `EDITOR = READ | WRITE`
   - `VIEWER = READ`
3. Reads a role name from the user (`"ADMIN"`, `"EDITOR"`, or `"VIEWER"`).
4. Uses a `switch` expression to assign the corresponding bitmask.
5. Asks the user which permission to check (`"READ"`, `"WRITE"`, `"EXECUTE"`, `"DELETE"`) and prints whether the role has it.
6. Handles invalid role or permission names gracefully.

*Focus on: bitwise operators, switch expressions, defensive programming, Scanner.*

---

#### Exploration Prompt

Research the **IEEE 754 floating-point standard** and write a short reflection (1–2 paragraphs) addressing:
- Why can `0.1` not be represented exactly in binary?
- What real-world incidents have been caused by floating-point rounding errors? (Find at least two documented cases.)
- How do languages or libraries work around this issue? (e.g., Java's `BigDecimal`, Python's `decimal` module, or the `strictfp` keyword.)

Explore on your own and come to class ready to discuss. There is no single "right answer"—the goal is to practice researching technical topics and forming your own understanding.

---

*This concludes the chapter on language basics. You should now be comfortable declaring variables of all primitive types, performing arithmetic and logical operations, casting between types, reading and writing console data, and writing clean, well-commented code. These skills are the bedrock of everything that follows—classes, objects, inheritance, collections, and beyond.*