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