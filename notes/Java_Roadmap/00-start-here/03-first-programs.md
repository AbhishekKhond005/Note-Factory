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