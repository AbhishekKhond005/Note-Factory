# 03-java-method-behavior

> Merged study notes for **03-java-method-behavior**

---

# Binding and Resolution in Java

## 1. Introduction

Every Java expression that invokes a method or reads a field involves a *decision*: exactly which method body should run, or which field should be read. That decision-making process is called **resolution**, and the specific choice that ends up binding a call site to an implementation is called **binding**. Java splits the work between two agents that run at very different times. The **compiler** performs **compile-time resolution**, using only the *reference type* (the type written in the source code). The **JVM** performs **runtime resolution**, using the *object type* (the actual class of the object sitting on the heap). Getting these two straight is the difference between writing code that behaves predictably and code that contains subtle, production-only bugs.

A useful analogy is a phone directory. When you look up "Acme Plumbing" and get a number, the directory has performed *compile-time resolution*: it matched your request to an entry. But when you dial, whoever answers may be a receptionist, the owner, or a contractor — the *actual* person behind the number. The directory picks the *entry*; the phone system routes the *call* to whoever is really there. In Java, the compiler is the directory (picking a *signature* by the static reference type), and the JVM is the phone switchboard (picking the *implementation* by the dynamic object type). A call like `shape.draw()` compiles against the `Shape` reference, but the JVM dispatches it to `Circle.draw()` if the object is a `Circle`.

**What you will learn:**
- What static (compile-time) binding and dynamic (runtime) binding mean, and when each applies.
- How overload resolution selects a method signature in three phases, and why it can never see the runtime type.
- How override resolution selects an implementation at runtime using virtual dispatch.
- The deep difference between reference type and object type, and how casting changes one but never the other.
- How to read a program and predict exactly which method runs — the core skill behind debugging and design.

## 2. Core Concepts

### Static binding vs. dynamic binding

**Static binding** is resolution performed by the compiler from the *reference type* alone; the decision is baked into the bytecode before the program runs. **Dynamic binding** is resolution performed by the JVM at runtime from the *object type*; the same call site can invoke different implementations on different executions.

*Intuition:* static binding asks "which *signature* does this reference type expose?", dynamic binding asks "which *implementation* does this object actually carry?"

```java
Shape s = new Circle();
s.draw();          // dynamic binding -> Circle.draw()
```

### Compile-time resolution vs. runtime resolution

**Compile-time resolution** decides which method signature (or field) a source expression refers to, checking applicability, ambiguity, and accessibility; it can reject the program with an error. **Runtime resolution** takes the already-chosen signature and selects the most specific overriding implementation in the object's class hierarchy.

*Intuition:* compile-time resolution answers "is this call legal, and which declaration does it mean?"; runtime resolution answers "now, which version of that declaration actually executes?"

```java
List<String> xs = new ArrayList<>();
xs.size();         // compiler: List.size(); runtime: ArrayList.size()
```

### Reference type (compile-time type) vs. object type (runtime type)

The **reference type** is the declared type of the variable or expression — what the compiler uses. The **object type** is the class that was actually instantiated with `new` — what the JVM inspects at runtime.

*Intuition:* the reference type is the *lens* through which the compiler views the object; the object type is what the object *really is*.

```java
Animal a = new Dog();   // reference type: Animal; object type: Dog
```

### Declared type vs. actual type

These are synonyms for reference type and object type, used especially in the Java Language Specification. A parameter declared as `Animal` has declared type `Animal`; the argument passed may have actual type `Dog`. Throughout these notes, "reference type/declared type" and "object type/actual type" are interchangeable.

### Method signature and resolution

A **method signature** is the method name plus the *types and order* of its parameters (JLS §8.4.2); the return type and thrown exceptions are *not* part of the signature. Overload resolution matches call arguments to signatures; override resolution matches signatures to implementations.

*Intuition:* the signature is the "phone number" the compiler dials; the implementation is "who answers".

```java
void f(int x)          // signature: f(int)
void f(long x)         // signature: f(long) — a different overload
```

### Side-by-side comparison

| Term | What it is | Determined by | Known at | Controls |
|---|---|---|---|---|
| Static binding | Compiler picks a method signature | Reference type | Compile time | Overload resolution, access checks |
| Dynamic binding | JVM picks an implementation | Object type | Runtime | Override resolution, virtual dispatch |
| Reference type | Declared type of a variable | Source code | Compile time | Which members are visible, field access |
| Object type | Class created with `new` | Runtime value | Runtime | Which overridden method runs |
| Signature | Name + parameter types | Declaration | Compile time | Whether a call matches an overload |

## 3. Compile-Time Resolution

When the compiler encounters a method invocation like `obj.m(arg)`, it must decide *which declaration of `m`* the source code means. Everything it is allowed to know is the **reference type** of `obj` and the **compile-time types** of `arg`. It cannot inspect the runtime object — the program hasn't run yet — so the decision is made blind to the object type, and the result is recorded in the bytecode as a specific signature plus an index into a method table. This whole process is *static*: recompiling with different reference types can change the chosen signature, but running the same compiled code never changes it.

### Overload resolution phases

When several methods share a name, the compiler first collects the **potentially applicable** methods: every method with that name, with the right number of parameters, and accessible from the call site, considering the reference type (JLS §15.12.2.1). It then searches for the most specific applicable method in **three phases** (JLS §15.12.2.2–§15.12.2.4):

1. **Phase 1** — applicable by *strict invocation*: subtyping and widening only. **No boxing/unboxing, no varargs.**
2. **Phase 2** — applicable by *loose invocation*: boxing and unboxing are now allowed, but **still no varargs**.
3. **Phase 3** — applicable by *variable-arity invocation*: varargs are allowed.

The phases are tried in order; as soon as any phase produces at least one applicable method, the compiler stops and picks the **most specific** method among them (JLS §15.12.2.5) — the one whose parameter types are subtypes of every other candidate's parameter types. Only if the phases all fail does compilation fail with "no suitable method found." In Java 8+, the same machinery handles lambdas: a lambda's "type" is the target functional interface, so `accept(s -> ...)` is resolved against the `Predicate`/`Consumer`-style overloads in the same phase order, with a lambda counting as applicable only when a functional interface type matches.

Program 1 shows the phases in action (Java 11+):

```java
// Java 11+
public class OverloadPhases {
    static void pick(int x)      { System.out.println("pick(int)"); }
    static void pick(long x)     { System.out.println("pick(long)"); }
    static void pick(Integer x)  { System.out.println("pick(Integer)"); }
    static void pick(int... xs)  { System.out.println("pick(int...)"); }

    public static void main(String[] args) {
        pick(42);        // Phase 1, exact primitive match            -> pick(int)
        byte b = 3;
        pick(b);         // Phase 1, widening byte->int beats boxing   -> pick(int)
        Integer boxed = 42;
        pick(boxed);     // Phase 1, exact reference match            -> pick(Integer)
        pick(5L);        // Phase 1, exact long match                 -> pick(long)
        pick();          // Phases 1-2 fail; only Phase 3 varargs fits -> pick(int...)
    }
}
// Output:
// pick(int)
// pick(int)
// pick(Integer)
// pick(long)
// pick(int...)
```

Notice two instructive details. `pick(b)` picks `pick(int)` because a widening primitive conversion (byte→int) is a *phase-1* candidate, which beats the phase-2 boxing candidate `pick(Integer)` — widening always wins over boxing. And `pick(42)` picks `pick(int)` over `pick(long)` even though both are phase-1 candidates, because `int` is more specific than `long` for an `int` argument.

### Ambiguity: when the most-specific tie-breaker fails

If two or more applicable methods remain and none is more specific than all the others, the compiler reports an **ambiguous** call. This is a compile-time error, never a runtime one — Java refuses to guess. Program 2 shows a classic case (Java 11+):

```java
// Java 11+
public class AmbiguousCall {
    static void choose(int... xs)  { System.out.println("int..."); }
    static void choose(long... xs) { System.out.println("long..."); }

    public static void main(String[] args) {
        choose(); // ERROR (below)
    }
}
// javac AmbiguousCall.java
// AmbiguousCall.java:9: error: reference to choose is ambiguous
//     both method choose(int...) in AmbiguousCall and method choose(long...)
//     in AmbiguousCall match
```

Both `choose(int...)` and `choose(long...)` are applicable via Phase 3 with zero arguments. But `int[]` is not a subtype of `long[]` and vice versa, so neither is more specific — the call is ambiguous and the program will not compile. The fix is to disambiguate the call, e.g. `choose(new int[]{1, 2})`.

### The `null` literal

`null` is assignable to *every* reference type, so it is an applicable argument to any overload taking a reference parameter — and this often creates ambiguity. Program 3 (Java 11+):

```java
// Java 11+
public class NullAmbiguity {
    static void accept(String s)  { System.out.println("String: " + s); }
    static void accept(Integer i) { System.out.println("Integer: " + i); }

    public static void main(String[] args) {
        accept(null); // ERROR (below)
    }
}
// javac NullAmbiguity.java
// NullAmbiguity.java:9: error: reference to accept is ambiguous
//     both method accept(java.lang.String) in NullAmbiguity and
//     method accept(java.lang.Integer) in NullAmbiguity match
```

`String` and `Integer` are unrelated types, so neither parameter type is more specific. The fix is a cast — `accept((String) null);` — which changes the *argument's reference type* so the compiler can resolve deterministically.

### Why overload resolution is always static

Overload resolution happens before any object exists, so the runtime type can never influence it. `Animal a = new Dog(); a.speak()` resolves the overload (and, if `speak` is overloaded, the *signature*) using the static type `Animal`; the fact that the object is a `Dog` only matters *afterwards*, at dispatch time, and only among implementations of the already-chosen signature. The compiler also performs **access checking** (is the method visible from the reference type? is it `public`/`protected`/`private`/package?) and checks **return-type compatibility** — a call is only legal if the declared return type of the chosen method is assignable to what the expression context requires (Java 5+ additionally allows **covariant return types** for overrides, where an overriding method may narrow the return type). A **`final`** method cannot be overridden, so it has exactly one implementation in any hierarchy; resolution of a `final` method is therefore effectively static — the compiler and JVM always agree on which body runs.

## 4. Runtime Resolution

Compile-time resolution picks a *signature*; the JVM still has to pick a *body*. That is **runtime resolution**, also called **virtual method dispatch**. When the JVM executes a compiled call to `obj.m(args)`, it uses the **object type** of `obj` — the actual class allocated on the heap — and walks the class hierarchy from that class upward, selecting the *most specific* override of `m` it finds. If `Dog` overrides `speak()`, the call dispatches to `Dog.speak()`; if not, the JVM looks in `Animal`, and so on up to `Object`.

Conceptually, each class carries a **method table (vtable)** — a per-class lookup of method signatures to their executable implementations. A `Dog` object's vtable has `Dog.speak`; a `Cat` object's vtable has `Cat.speak`. Dispatch is then "look up the signature in the object's vtable." The JVM may cache this lookup, inline it, and even *devirtualize* (statically resolve) it when it can prove a single target — but the language semantics are always dynamic: the object decides.

Program 4 demonstrates plain override dispatch (Java 11+):

```java
// Java 11+
public class OverrideDispatch {
    public static void main(String[] args) {
        Animal a = new Dog();
        a.speak();          // reference type Animal, object type Dog
        Animal m = new Cat();
        m.speak();          // reference type Animal, object type Cat
    }
}
class Animal {
    void speak() { System.out.println("Animal speaks"); }
}
class Dog extends Animal {
    @Override
    void speak() { System.out.println("Dog barks"); }
}
class Cat extends Animal {
    @Override
    void speak() { System.out.println("Cat meows"); }
}
// Output:
// Dog barks
// Cat meows
```

Both call sites are compiled against `Animal` (the reference type), yet each invokes a different implementation because the JVM dispatches on the object type. The same bytecode, the same call site, different behavior per object.

### When dispatch is static even at runtime

Three kinds of methods are *not* subject to virtual dispatch, because they cannot be overridden in a way that matters:

- **`private` methods** — invisible to subclasses, never overridden; invoked by `invokespecial`, resolved entirely against the declaring class.
- **`static` methods** — belong to the class, not the instance; invoked by `invokestatic`. A subclass declaring a same-signature static method *hides* (does not override) the parent's.
- **`final` (instance) methods** — cannot be overridden, so there is only ever one implementation; dispatch is effectively static (the JIT commonly devirtualizes and inlines them).

```java
class Shape {
    private   void p() { System.out.println("private"); }   // never dispatched
    static    void s() { System.out.println("static Shape"); }  // invokestatic
    final     void f() { System.out.println("final"); }      // non-overridable
    protected void v() { System.out.println("virtual Shape"); } // dispatch target
}
```

Even in code that looks like overriding, `s()` can only ever call `Shape.s` regardless of the object type, because `invokestatic` ignores the receiver entirely.

### Fields: hiding, not overriding

**Instance fields are not polymorphic.** Field access is resolved *at compile time* using the reference type and is never re-resolved at runtime — a field read is a plain memory access into the object, with the offset computed from the *reference type's* view of the layout. If a subclass declares a field with the same name, it *hides* the superclass field rather than overriding it; which one you see depends entirely on the reference type. Methods, by contrast, are virtual. This "hiding vs. overriding" split is the single most confusing corner of the model, and Program 5 contrasts it directly (Java 11+):

```java
// Java 11+
public class HidingVsOverriding {
    public static void main(String[] args) {
        Parent p = new Child();
        System.out.println("p.name     = " + p.name);      // field:  reference type wins
        System.out.println("p.salute() = " + p.salute());  // method: object type wins
    }
}
class Parent {
    String name = "Parent";
    String salute() { return name + " says hi"; }
}
class Child extends Parent {
    String name = "Child";                     // hides Parent.name
    @Override
    String salute() { return name + " says hi"; } // overrides Parent.salute
}
// Output:
// p.name     = Parent
// p.salute() = Child says hi
```

`p.name` is `Parent`'s field because the reference type is `Parent` — the field binding is static. `p.salute()` is `Child`'s method because dispatch is dynamic; and inside `Child.salute()`, `name` refers to `Child`'s own (hidden) field, giving "Child says hi." Field access follows the reference; method invocation follows the object.

### `super.method()` in a multi-level hierarchy

`super` is a *static* escape hatch: `super.m()` explicitly invokes the superclass's implementation, bypassing virtual dispatch from that point. Program 6 shows a three-level chain (Java 11+):

```java
// Java 11+
public class SuperChain {
    public static void main(String[] args) {
        Puppy p = new Puppy();
        p.describe();
    }
}
class Animal {
    void describe() { System.out.println("Animal describes"); }
}
class Dog extends Animal {
    @Override
    void describe() {
        super.describe();          // explicitly calls Animal.describe
        System.out.println("Dog describes");
    }
}
class Puppy extends Dog {
    @Override
    void describe() {
        super.describe();          // explicitly calls Dog.describe
        System.out.println("Puppy describes");
    }
}
// Output:
// Animal describes
// Dog describes
// Puppy describes
```

`Puppy.describe` invokes `super.describe()` which statically targets `Dog.describe`; that method in turn invokes its own `super.describe()` targeting `Animal.describe`. `super` always refers to the *direct superclass of the class where the call is written* — it is resolved from the source class, not from the runtime object, which is why it can "skip" implementations a dynamic dispatch would have selected.

## 5. Reference Type vs. Object Type

Everything in binding reduces to two types: the **reference type**, fixed in source code and known at compile time, and the **object type**, created at runtime by `new`. The reference type determines what you are *allowed to write* and how overloads and fields resolve; the object type determines which overridden *implementation* runs. They can differ — and almost always do the moment you use inheritance polymorphically.

| Row | Reference type (declared type) | Object type (actual type) |
|---|---|---|
| **Definition** | The type of the variable or expression as written | The class of the object on the heap |
| **Determined by** | Source code (`Animal a = ...`) | The constructor call (`new Dog()`) |
| **When known** | Compile time | Runtime |
| **Field access** | Chooses the field (hiding) | Irrelevant — fields never dispatch |
| **Method invocation** | Determines which *signature* is legal | Determines which *implementation* runs |
| **Overload resolution** | The *only* input | Never consulted |
| **Override resolution** | Only restricts the candidate set | The deciding input |
| **Casting** | `(Dog)` changes this | Never changes |
| **Example** | `Animal a` | `new Dog()` |

The rules that move values between reference types are **reference conversions**. A **widening** conversion (subtype to supertype, e.g., `Dog` → `Animal`, or to an implemented interface) is always safe and implicit — assignment and passing as an argument apply it automatically. A **narrowing** conversion (supertype to subtype, e.g., `Animal` → `Dog`) requires an explicit **cast**, `(Dog) a`. The cast *only* changes the reference type through which the compiler views the object; it never changes the object itself. If the object is not actually a `Dog`, the cast succeeds at compile time (the reference types are related) but throws `ClassCastException` at runtime — the JVM verifies the object type when the cast executes.

Program 7 brings the pitfalls together (Java 11+):

```java
// Java 11+
public class CastingPitfalls {
    public static void main(String[] args) {
        Animal a = new Dog();
        a.speak();            // Dog barks: dynamic dispatch

        // a.fetch();         // COMPILE ERROR: Animal (the reference type)
        //                    //   has no method fetch()

        Dog d = (Dog) a;      // narrow cast: legal, object really is a Dog
        d.fetch();            // Dog fetches

        Animal x = new Cat();
        Dog bad = (Dog) x;    // compile-time OK, but object is a Cat:
                              //   ClassCastException at runtime
    }
}
class Animal {
    void speak() { System.out.println("Animal speaks"); }
}
class Dog extends Animal {
    @Override
    void speak() { System.out.println("Dog barks"); }
    void fetch() { System.out.println("Dog fetches"); }
}
class Cat extends Animal {
    @Override
    void speak() { System.out.println("Cat meows"); }
}
// Output:
// Dog barks
// Dog fetches
// Exception in thread "main" java.lang.ClassCastException: class Cat cannot
//   be cast to class Dog (Cat and Dog are in unnamed module of loader 'app')
```

Three lessons in one program: (1) `a.fetch()` is illegal even though the object can fetch, because the reference type `Animal` doesn't expose `fetch`; (2) the downcast `(Dog) a` succeeds because the object *is* a `Dog` — casting changed the reference type, not the object; (3) `(Dog) x` where `x` holds a `Cat` is syntactically legal but fails at runtime because the object type refuses the conversion.

## 6. Overload Resolution vs. Override Resolution

These two processes are constantly confused, so it is worth pinning them down side by side. **Overload resolution** happens entirely at compile time and asks: *given the reference types of the arguments, which signature is the best match?* **Override resolution** happens at runtime and asks: *given the object's actual class, which implementation of that signature runs?*

| Aspect | Overload resolution | Override resolution |
|---|---|---|
| **When** | Compile time (in the compiler) | Runtime (in the JVM) |
| **What triggers it** | Writing a call expression | Executing a compiled call |
| **Type used** | Reference types of receiver & args | Object type of receiver |
| **Decides** | Which *signature* is chosen | Which *body* is chosen |
| **Example** | `f(int)` vs `f(long)` | `Animal.speak()` vs `Dog.speak()` |
| **Can fail?** | Yes — "ambiguous" / "no suitable method" | No — some implementation always exists |
| **How to reason** | "Which method name+parameters match?" | "Which class is the object really from?" |

**Rule of thumb: the compiler picks the *signature*; the JVM picks the *implementation*.** They happen in that strict order: overloading first (compile time), overriding second (runtime), on top of the already-chosen signature. Overriding never "re-decides" which overload was chosen; it only chooses among the implementations of that signature.

Program 8 combines both in one hierarchy, so you can trace each decision (Java 11+):

```java
// Java 11+
public class OverloadOverride {
    public static void main(String[] args) {
        Calculator base = new Calculator();
        Scientific calc = new Scientific();
        Calculator c = new Scientific();   // polymorphic reference

        System.out.println("base.compute(5)   = " + base.compute(5));    // (1)
        System.out.println("base.compute(5.0) = " + base.compute(5.0));  // (2)
        System.out.println("calc.compute(5)   = " + calc.compute(5));    // (3)
        System.out.println("c.compute(5)      = " + c.compute(5));       // (4)
    }
}
class Calculator {
    int compute(int x)    { return x * 2; }
    int compute(double x) { return (int) (x * 3); }
}
class Scientific extends Calculator {
    @Override
    int compute(int x) { return x * x; }   // overrides ONLY the int version
}
// Output:
// base.compute(5)   = 10
// base.compute(5.0) = 15
// calc.compute(5)   = 25
// c.compute(5)      = 25
```

Trace table for each call site:

| Call site | Compiler decision (overload) | Runtime decision (override) | Result |
|---|---|---|---|
| `base.compute(5)` | `compute(int)` — exact match | `Calculator.compute(int)` | `10` |
| `base.compute(5.0)` | `compute(double)` — exact match | `Calculator.compute(double)` | `15` |
| `calc.compute(5)` | `compute(int)` — reference type `Scientific` | `Scientific.compute(int)` — object is `Scientific` | `25` |
| `c.compute(5)` | `compute(int)` — reference type `Calculator` | `Scientific.compute(int)` — object is `Scientific` | `25` |

The critical comparison is (3) versus (4): in both, the argument `5` selects the `int` overload, and in both the override dispatches to `Scientific` because the object is a `Scientific`. But in (4) the reference type `Calculator` *determined* the overload choice; the compiler never considered `Scientific`'s override as a candidate overload. Overriding only operates on the signature the compiler already chose.

## 7. Real-World Context and Use Cases

### Frameworks and dependency injection

Frameworks invert control: *you* write overridable methods or interface implementations, and *the framework* calls them at the right moment. A servlet container, a JUnit runner, or a Spring `@Service` all rely on runtime dispatch to invoke *your* subclass methods from code compiled against *their* abstract types. Overloading makes those APIs ergonomic on the caller's side — a single `send(message)` handling `String`, `Message`, or `Collection` — while overriding supplies extensibility on the implementer's side. Both forms are load-bearing: DI containers inject objects whose runtime type is often an unannotated subclass, and everything hinges on dispatch using the object type.

```java
interface Handler { void handle(Event e); }
class LogHandler implements Handler {
    @Override public void handle(Event e) { System.out.println("logged"); }
}
Handler h = new LogHandler();   // framework calls h.handle(e);
h.handle(new Event());          // -> LogHandler.handle at runtime
```

### Design patterns

The classic behavioral patterns are, at bottom, *runtime dispatch* arranged into shapes. **Strategy** delegates a decision to an object implementing an interface — the `strategy` field's object type decides behavior: `ctx.execute(a, b)` dispatches to `Add.execute` or `Multiply.execute`. **Template Method** fixes an algorithm skeleton in a `final` superclass method and lets subclasses override *hook* methods; the skeleton's calls to hooks are virtual, so each subclass produces a different pipeline. **Command** encapsulates an action as an object whose `execute()` is dispatched by an invoker that knows only the interface. All three are unremarkable *without* dynamic dispatch and impossible *with* only static binding.

```java
// Strategy
interface Strategy { int execute(int a, int b); }
class Add implements Strategy { public int execute(int a, int b) { return a + b; } }
class Ctx { Strategy s; int run(int a, int b) { return s.execute(a, b); } }

// Template Method
abstract class Processor {
    public final void process() { read(); transform(); write(); } // fixed skeleton
    protected abstract void read();
    protected abstract void transform();  // hook
    private void write() { }
}
```

### Refactoring hazards

Because overload resolution is purely static and silent, refactoring can change behavior without any error. **Changing a parameter type** on an overload shifts which calls bind to it: change `log(String)` to `log(CharSequence)` and every call site with a `String` argument now binds to a *different* signature — and if a `log(StringBuilder)` overload exists, `log("x")` may now be ambiguous. **Adding a new overload** re-runs the phase algorithm for every existing call: adding `pick(Integer)` to Program 1 would silently change some `pick(...)` call sites from `pick(int)` to `pick(Integer)` if a phase-1 candidate disappears. Overload resolution never warns you; the fix is to audit call sites after any signature change and to avoid overloading on types related by subtyping (see *Effective Java*, below).

### Performance and correctness

Dynamic dispatch costs a bit: the JVM must do an indirect method-table lookup per virtual call, though the JIT compensates with inline caching and devirtualization. `final` methods (and `private`/`static`) can be aggressively inlined because their target is fixed — a real, if modest, performance lever in hot paths. But optimizing by sprinkling `final` everywhere sacrifices the flexibility that inheritance-based designs rely on, so the engineering default should be: **prefer dynamic dispatch and clarity; add `final` for invariants or measured hot spots, not as a reflex.** Correctness-wise, dynamic dispatch is what makes overriding work at all — eliminating it eliminates polymorphism, not the bugs.

## 8. Common Pitfalls and Interview-Style Questions

### Pitfalls

- **Calling an overridable method from a constructor.**
  - *The mistake:* `class Base { Base() { init(); } }` where `init()` is overridden.
  - *Why it happens:* the superclass constructor runs before the subclass's field initializers, but dispatch is dynamic — so `this.init()` calls the subclass override *before* its fields are initialized.
  - *The fix:* declare `init()` `private` or `final`, or initialize fields before delegating; never expose overridable methods from a constructor.

- **Overload + override interaction.**
  - *The mistake:* assuming `c.compute(5)` calls the *subclass* method because the object is a subclass.
  - *Why it happens:* overloading is resolved on the *reference* type first; the subclass override only competes among implementations of the chosen signature.
  - *The fix:* remember "compiler picks signature, JVM picks implementation"; trace reference type for the signature, object type for the body.

- **`null` overload ambiguity.**
  - *The mistake:* calling `accept(null)` when both `accept(String)` and `accept(Integer)` exist.
  - *Why it happens:* `null` is applicable to every reference type and the two parameter types are incomparable, so the most-specific test fails.
  - *The fix:* cast the argument (`accept((String) null)`) or add a single most-specific overload.

- **Field hiding surprises.**
  - *The mistake:* reading `p.name` through a `Parent` reference and expecting the `Child` value.
  - *Why it happens:* fields are resolved by reference type; a same-named field in the subclass *hides* the parent's, and which one you get depends on the reference type.
  - *The fix:* never declare same-named fields in subclasses; use methods (`getName()`) if you want polymorphic access.

- **"Overriding" a static method.**
  - *The mistake:* `Parent p = new Child(); p.staticMethod();` expecting `Child`'s version.
  - *Why it happens:* static methods are bound by `invokestatic` to the *reference type's* class; a same-signature static method in the subclass merely *hides* it.
  - *The fix:* call static methods through the class that declares them, and add `@Override`-aware tooling only for instance methods; a static "override" cannot exist.

- **Relying on the runtime type for overloads.**
  - *The mistake:* `print(someAnimal)` expecting a `Dog` overload to run because the object is a `Dog`.
  - *Why it happens:* overload resolution uses only the *argument's reference type*; the object type is invisible to it.
  - *The fix:* if you genuinely need type-based behavior, use a virtual method (`a.print()`) or `instanceof` + cast — not overloads.

- **Casting without `instanceof`.**
  - *The mistake:* `Dog d = (Dog) x;` where `x` may hold a `Cat`.
  - *Why it happens:* the cast checks the object type only at runtime and throws `ClassCastException` on mismatch.
  - *The fix:* guard with `x instanceof Dog` before casting, or design so that casts are unnecessary.

### Questions

**1. Q:** Which types participate in overload resolution: the reference type, the object type, or both?  
**A:** Only the reference types of the receiver and arguments. Overload resolution is a compile-time activity; the object type does not exist yet and can never affect it.

**2. Q:** Is the return type part of a method signature? Can it be used to disambiguate overloads?  
**A:** No — a signature is the name plus parameter types (JLS §8.4.2). Two methods differing only in return type cannot coexist as overloads, and return type is never used to choose among them.

**3. Q:** Why is widening preferred over boxing in phase 1?  
**A:** Because phase 1 (strict invocation) accepts only subtyping/widening conversions; boxing is introduced in phase 2. The phases are searched in order, so a phase-1 candidate always beats a phase-2 candidate.

**4. Q:** Can a `final` method be overridden? How does that affect dispatch?  
**A:** No. A `final` method has exactly one implementation in the hierarchy, so its resolution is effectively static; the JIT can safely inline or devirtualize it.

**5. Q:** What is the difference between hiding and overriding?  
**A:** Overriding is for instance methods: the object type selects the implementation at runtime, and `@Override` semantics apply. Hiding applies to fields and static methods: the reference type selects which member is visible, and there is no dynamic dispatch.

**6. Code-tracing challenge.** Predict the exact output of this program (Java 11+):

```java
// Java 11+
class Base {
    Base() { init(); }
    void init() { System.out.println("Base.init"); }
}
class Derived extends Base {
    private String name = "initialized";
    @Override
    void init() { System.out.println("Derived.init name=" + name); }
}
public class TraceChallenge {
    public static void main(String[] args) {
        new Derived();
    }
}
```

**Expected output:**
```java
// Output:
// Derived.init name=null
```

**Step-by-step:** (1) `new Derived()` triggers `Derived`'s constructor, which first invokes the implicit `super()` — `Base`'s constructor. (2) `Base`'s constructor body calls `init()`. (3) The receiver is a `Derived` object, so dispatch is dynamic and `Derived.init()` runs — even though we are inside `Base`'s constructor code. (4) At this moment, `Derived`'s field initializers have *not* run (they execute only after the superclass constructor returns), so `name` is still `null` (its default value). Hence `Derived.init name=null`. (5) Only afterward does `name = "initialized"` execute. This is precisely the constructor pitfall from above, in executable form.

## 9. Summary

**Key takeaways:**

| Concept | Rule |
|---|---|
| Overload resolution | Compile time, reference types only, three phases, most-specific tie-breaker |
| Override resolution | Runtime, object type only, selects most specific implementation |
| Reference type | Controls legality, overloads, field access, casting; known at compile time |
| Object type | Controls which overridden method runs; known at runtime |
| Fields vs. methods | Fields hide (reference type); methods override (object type) |
| Static-bound kinds | `private`, `static`, `final` methods; field access; `super` calls |
| Debugging motto | "Compiler picks the signature; JVM picks the implementation." |

**Further reading:**
- **The Java Language Specification** (Java SE 17): §8.4.2 (method signatures), §8.4.8.3 & §8.4.9 (inheritance, overriding, hiding), §15.12 (method invocation expressions), §15.12.2 (compile-time step, including §15.12.2.2–.4 phases and §15.12.2.5 most specific method), §15.12.4 (runtime step and method dispatch), §15.11.1 (field access).
- **Joshua Bloch, *Effective Java*, 3rd ed.**: Item 52, "Use overloading judiciously" (the classic overload-resolution hazards and the subtyping-overload trap); Item 53, "Use interfaces only to define types"; Item 51, "Design method signatures carefully"; Item 19, "Design and document for inheritance or else prohibit it" (the constructor-dispatch pitfall and overriding dangers).
- For the reflective/corner-case details of dispatching, JLS §15.12.4.4 "Choose Method to Invoke" and the discussion of `invokevirtual`/`invokeinterface` in *The Java Virtual Machine Specification*.

Binding and resolution are the bridge between what Java *looks like* on the page and what it *does* at runtime. Master the distinction between the reference type and the object type, keep the motto "the compiler picks the signature, the JVM picks the implementation" in mind, and most polymorphic mysteries — and their bugs — dissolve.

---

# Java, Explained: Five Confusions That Trip Up Every Learner

## Overview / "Why This Confuses People"

Polymorphism is the moment in a Java course when many students suddenly feel like the language is lying to them. You write `Parent p = new Child();`, call `p.greet()`, and something unexpected happens. You annotate a static method with `@Override` and the compiler refuses — yet an *identical-looking* method on the same class compiles fine. You know the object you created is a `Motorcycle`, but you can't call `wheelie()` on it. The root cause of nearly every one of these "gotchas" is a single, deeply-held misunderstanding: most beginners treat **classes** and **objects** as if they were the same thing, and they assume that *what you type* and *what actually happens* are always identical.

The reality is that Java deliberately separates three ideas that beginners tend to blur: **what you wrote** (the source code), **what type the reference variable declares** (the compile-time type), and **what kind of object actually sits in memory** (the runtime type). When these three line up — as they do in the simple examples you see first — the language feels predictable. When they diverge — as they do in inheritance hierarchies — the confusion begins.

By the end of these notes you will be able to: explain, in your own words and at the JVM level, why static methods *hide* instead of *override*; articulate why instance methods cannot exist without an object; predict which methods a variable can *call* versus which implementation will actually *run*; use runtime polymorphism to design extensible systems; and write overrides with covariant return types. More importantly, you will stop memorizing "exceptions to the rules" and start seeing one consistent set of rules underneath.

---

## Section 1: Why Static Methods Are Not Overridden

### The Conceptual Explanation

Here is the single most important sentence in this section: **overriding** is a *runtime* mechanism, and **static methods** are resolved at *compile time*. The two ideas live in different worlds and can never meet.

When you override an instance method, you are saying, "Objects of my class will respond to this message differently from objects of the parent class." The JVM cannot know which object it will be handed until the program runs, so it postpones the decision until runtime. A static method, by contrast, belongs to the *class itself* — it has no object attached to it at all. Because there is no object, there is nothing to postpone; the compiler can look at the reference type on the left side of the dot and decide, right then, exactly which class's method to call. The decision is *frozen into the bytecode* before your program ever executes.

When a child class declares a static method with the same signature as a parent, the child method does not replace the parent method. It **hides** it. The parent method still exists, fully intact, and which one you get depends entirely on the *type of the reference you used to call it*. This is the classic beginner trap, and the code below is the classic beginner surprise.

### The "Wrong" Example: What Beginners Write and Expect

```java
class Parent {
    public static void greet() {
        System.out.println("Hello from Parent");
    }
}

class Child extends Parent {
    // WRONG-EXAMPLE: This looks like an override, but it is a HIDE.
    // The word 'static' makes @Override impossible and resolution compile-time.
    public static void greet() {
        System.out.println("Hello from Child");
    }
}

public class StaticBindingDemo {
    public static void main(String[] args) {
        Parent p = new Child();   // reference type is Parent, object is Child
        Child c = new Child();    // reference type is Child, object is Child

        // Beginners expect BOTH lines to print "Hello from Child"
        // because both objects are actually Child objects.
        p.greet();
        c.greet();
    }
}
```

**Expected Output**

```text
Hello from Parent
Hello from Child
```

The first call, `p.greet()`, is resolved at compile time using the reference type `Parent` — the compiler never even looks at what object `p` points to, because static methods don't care. So you get "Hello from Parent," which shocks most students. The second call uses a `Child` reference, so the compiler binds it to `Child.greet()`. The object identity was irrelevant in both cases; only the reference type on the left of the dot mattered.

> "But I wrote `@Override` and it compiled — doesn't that mean I overrode it?"
>
> No — and here is the gotcha. If you put `@Override` on a *static* method that "matches" a parent static method, the code **will not compile** (`@Override` is only legal on methods that override an instance method). If your `@Override` *did* compile, then one of two things was true: the parent method was actually an *instance* method, or you were annotating a genuinely overridden instance method. The annotation is a compiler check, not a magic spell — it doesn't change what the method is; it only *verifies* what you claimed.

### The "Correct" Example: Understanding Hiding

```java
class Parent {
    public static void identify() {
        System.out.println("I am a static method defined on Parent");
    }

    // A normal instance method, for contrast later in this section.
    public void instanceGreeting() {
        System.out.println("Instance method runs on: " + getClass().getSimpleName());
    }
}

class Child extends Parent {
    // CORRECT-EXAMPLE: We are deliberately HIDING Parent.identify().
    // We say nothing about overriding, because we are not overriding.
    public static void identify() {
        System.out.println("I am a static method defined on Child");
    }

    // This genuinely overrides Parent.instanceGreeting().
    @Override
    public void instanceGreeting() {
        System.out.println("Overridden instance method runs on: " + getClass().getSimpleName());
    }
}

public class HidingVsOverridingDemo {
    public static void main(String[] args) {
        Parent p = new Child();

        // Static: resolved from the reference type (Parent) at compile time.
        p.identify();

        // Instance: resolved from the runtime object (Child) at run time.
        p.instanceGreeting();
    }
}
```

**Expected Output**

```text
I am a static method defined on Parent
Overridden instance method runs on: Child
```

The same reference variable `p` produces two different behaviors in two adjacent lines. `p.identify()` binds statically to `Parent` because the compiler decides static calls immediately. `p.instanceGreeting()` binds dynamically to the *Child* object in memory, because the JVM defers instance-call resolution until runtime. This one example — a static call and an instance call through the *same variable* — is the fastest way to internalize the difference.

### The Analogy: An Org Chart and Employee Badges

Think of a company org chart. The "Sales" department has a name that is written on a sign on the door. That name belongs to the *department*, not to any individual employee. When someone says "go talk to Sales," they walk to the Sales office — even if a specific employee inside belongs to a different team. Now imagine every employee wears a badge that shows *their personal* name.

- The **class** is the department. Its static method name (like `greet`) is the sign on the door.
- The **object** is the employee. Its instance methods are the personal behaviors written on the badge.
- Calling `p.greet()` where `p` has type `Parent` is like someone telling you, "go talk to the department named on this *map label*." You go to `Parent`'s office, because that's what the label says — you never check which actual employee sits there.
- A child class "hiding" a static method is a new department hanging its own sign; the old department's sign still exists on the old door.

> **Memory hook:** *Static = sign on the door. Instance = badge on the employee. The sign is read by anyone walking past; the badge is read only when you actually meet the person.* If you're calling through a `Parent`-typed reference, you read the `Parent` sign. Period.

### What's Happening Under the Hood?

When you compile `Parent p = new Child(); p.greet();`, the Java compiler emits a bytecode instruction called `invokestatic`, and it bakes the *symbolic reference* to `Parent.greet()` directly into the class file. The JVM never performs a lookup based on the object type for that instruction — there is no method table consultation, no vtable walk, nothing. `invokestatic` targets a specific class's method, full stop. Contrast this with the instance call `p.instanceGreeting()`, which compiles to `invokevirtual` — that instruction tells the JVM, "find the method named `instanceGreeting` in the *actual* class of the object on the stack, and run the most specific version." That single bytecode difference, `invokestatic` versus `invokevirtual`, is the entire mechanical explanation for everything in this section.

### Hiding vs. Overriding: The Comparison Table

| Aspect | Method **Hiding** (static) | Method **Overriding** (instance) |
|---|---|---|
| Binding time | Compile time (`invokestatic`) | Runtime (`invokevirtual`) |
| Decided by | The *reference type* on the left of the dot | The *runtime type* of the object |
| `@Override` allowed? | **No** — compiler error | **Yes** (recommended) |
| Parent method status | Still exists; merely shadowed for the child | Effectively replaced for child objects |
| What the child method can reference | Only `static` members | Both instance and static members |
| Keyword in play | `static` | `extends` + override |
| `super` call from child method | Legal (call `Parent.greet()`) | Legal (`super.greet()`) |
| Polymorphic? | **Never** | The core of polymorphism |

### Real-World Use Case: The `Collections` and `Math` Utility Classes

Every Java developer calls static methods daily without thinking about why they're static. `Collections.sort(list)` and `Math.max(a, b)` are static because sorting and maximum-finding are *operations on values handed to them*, not behaviors of the objects themselves. The list is passed **in** as an argument precisely because the method does not "belong to" any single list.

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollectionsDemo {
    public static void main(String[] args) {
        List<String> words = new ArrayList<>(List.of("banana", "apple", "cherry"));

        // sort is a STATIC method on the Collections class.
        // The list is passed as an argument; sort does not run "on" the list.
        Collections.sort(words);

        System.out.println(words);
    }
}
```

**Expected Output**

```text
[apple, banana, cherry]
```

`Collections.sort(words)` compiles to `invokestatic`; the JVM calls the one and only `Collections.sort`. No subclass of `Collections` can ever change how this line behaves, which is exactly the point — utility classes are meant to be final, fixed behavior. This is why the standard library authors made these methods static, and why you should too for your own pure helper functions.

> **Transition:** Now that we know static methods belong to classes and are frozen at compile time, the obvious question is *why* instance methods are the opposite — why they demand an object at all.

---

## Section 2: Why Instance Methods Need Objects

### The Conceptual Explanation

An instance method is, by definition, a method that reads or modifies **object state** — the values stored in the fields of one specific object. Every instance method carries a hidden parameter, named **`this`**, which is a reference to the object the method was called on. That `this` is the entire reason the method needs an object: without an object, there is no state to read, no state to modify, and no meaning to the word "instance" in the method's name.

Compare the two families of methods:

- **Static methods** receive *only* the explicit arguments you pass. They cannot touch `this` (there is no `this`). They are functions attached to a class.
- **Instance methods** receive their explicit arguments *plus* the implicit `this`. Every time you write `alice.deposit(25)`, you are really calling `deposit` with two arguments: the object `alice` (as `this`) and the number `25`.

This is why the compiler rejects an instance method call without an object. `deposit(25)` alone is meaningless — *which* account's balance changes? `size()` alone is meaningless — *which* list? The object isn't a formality; it is the actual subject of the sentence.

### The Working Example: `this` Is the Implicit Argument

```java
public class BankAccount {
    private double balance;

    public BankAccount(double startingBalance) {
        this.balance = startingBalance;   // 'this' is the account under construction
    }

    public void deposit(double amount) {
        // 'this' is the hidden argument: the exact account being changed.
        // Without this.balance we could not know WHICH balance to update.
        this.balance += amount;
    }

    public double getBalance() {
        return this.balance;
    }

    public static void main(String[] args) {
        BankAccount alice = new BankAccount(100.0);
        BankAccount bob = new BankAccount(50.0);

        // Each call passes a different hidden 'this' argument.
        alice.deposit(25.0);
        bob.deposit(10.0);

        System.out.println(alice.getBalance());
        System.out.println(bob.getBalance());
    }
}
```

**Expected Output**

```text
125.0
60.0
```

`alice.deposit(25.0)` executes the *same* method code as `bob.deposit(10.0)` — there is only one copy of `deposit` in memory — but the first call binds `this` to the alice object, and the second binds `this` to the bob object. Alice's balance becomes `125.0`; Bob's becomes `60.0`. The method code is shared; the state it acts upon is not. That is the whole meaning of an instance method.

### Static vs. Instance: A Contrast Table

| Aspect | `Thread.sleep(1000)` (static) | `list.size()` (instance) |
|---|---|---|
| Needs an object? | **No** — sleeps *any* thread, needs no state | **Yes** — must know *which* list |
| Hidden `this` parameter | None | The list itself |
| What state it operates on | None (global/class behavior) | The list's internal elements |
| When resolved | Compile time | Runtime |
| Typical wording | "a class-level operation" | "a behavior of the object" |
| Example of misuse | `alice.deposit(25)` fails without an object | `Thread.sleep()` works with zero objects |

### The Analogy: A Recipe Versus a Chef With Ingredients

A recipe card is a *class*: it contains instructions that describe a dish in general — "whisk eggs, add flour, bake at 180°C." It is useful, but the recipe by itself feeds no one; it has no ingredients and no oven. A **chef** is an *object*: the chef has a specific set of ingredients (field values) in their kitchen, and when they follow the recipe, they apply it to *their* ingredients. Two chefs following the same recipe produce two different dishes if their ingredient supplies differ.

- The **recipe** is the instance method's code — one copy, shared by all.
- The **chef** is the object; the chef's pantry is the object's fields (`this.balance`).
- "Follow the recipe" is the method call: it always needs a chef attached (`alice.deposit`, not just `deposit`).
- A **static** method, by contrast, is a rule posted on the wall ("always wash your hands before cooking") — it applies to everyone and needs no individual chef's pantry to be meaningful.

> **Memory hook:** *The method is the recipe; `this` is the chef. You cannot cook the recipe in the abstract — you need a specific chef with specific ingredients.* Every instance method is secretly a two-argument method: `(this, explicitArgs...)`.

### Common-Student-Voice: Addressing the Fear

> "Why can't I just call `getBalance()` without an object if there's only one account in my program?"
>
> Because Java's type system has no notion of "the one account." The compiler cannot prove that only one object exists, so it forces you to be explicit about *which* object every time. This rule is what makes your code correct when your program grows from one account to a million. The annoyance at the start is the safety net at scale.

> "But `String.format(...)` and `Math.random()` work with no object — so instance methods are pointless, right?"
>
> Not at all. Those methods are *static by design* precisely because they have no object state to act upon. The moment a method needs state — `String.toLowerCase()`, `List.add(...)`, `BankAccount.deposit(...)` — Java requires an object. The distinction isn't arbitrary; it mirrors whether the operation needs `this`.

### What's Happening Under the Hood?

The JVM stores method *code* once, in a shared memory region called the **method area**. There are not a thousand copies of `deposit()` for a thousand `BankAccount` objects — there is one. What changes per call is the **`this` reference**, which the compiler arranges to be pushed onto the Java operand stack as the *first* argument before every `invokevirtual` instruction. The invoked method then reads its first parameter slot as `this` and uses it to locate the object's fields (each field is an offset into the object's heap layout). So the physical reality is: shared code, per-call `this`, per-object fields. That is the entire architecture, and it explains why instance methods are *cheap* — the only per-call work is pushing one reference and doing a table lookup.

### Real-World Use Case: `String` Methods Everywhere

You interact with instance methods constantly in the JDK. `String` is an immutable object, and nearly every operation on a string is an instance method that needs the string as `this`.

```java
public class StringMethodsDemo {
    public static void main(String[] args) {
        String greeting = "hello";

        // toUpperCase() is an INSTANCE method — it operates on the
        // characters of THIS greeting object (this).
        String loud = greeting.toUpperCase();

        // valueOf() is a STATIC method — it converts an int without
        // needing any String object to act upon.
        String number = String.valueOf(42);

        System.out.println(loud);
        System.out.println(number);
    }
}
```

**Expected Output**

```text
HELLO
42
```

`greeting.toUpperCase()` works because there is an object to transform; the JDK authors gave `String` a whole family of instance methods (`toLowerCase`, `substring`, `replace`, `trim`) because each one operates on the string's internal character array. `String.valueOf(42)` needs no object because converting an int to a string is a pure function of the int. Whenever you see an API that offers both forms — `String.format(...)` (static) and `"abc".format(...)`-style helpers (instance) — you're seeing this exact design decision in action.

> **Transition:** If instance methods run on the object, then the *type of the reference* you use to reach that object starts to matter a great deal — which leads directly to the confusion of "two types."

---

## Section 3: Why Reference Type Matters

### The Conceptual Explanation

Every variable that holds an object actually holds **two types** that can disagree:

1. The **declared type** (also called the reference type or compile-time type) — written in the variable declaration, e.g., `Vehicle v`. The compiler uses this type for *all* checks: "Is the method you're calling defined on this type?" and "Is this assignment legal?"
2. The **runtime type** (also called the actual type) — determined by the constructor you called, e.g., `new Motorcycle()`. The JVM uses this type at runtime to decide *which implementation* of an overridden method to run.

Beginners usually think there's only one type — "I made a `Motorcycle`, so everything about it is `Motorcycle`." But the compiler is not clairvoyant. When you write `Vehicle v = m;`, the compiler knows only that `v` can be trusted to *behave like* a `Vehicle`. It will happily let you call every method declared on `Vehicle`. It will refuse, with a compile error, to let you call a method that exists only on `Motorcycle` — even though, at runtime, the object really is a `Motorcycle`. The compiler is protecting you from a promise it cannot verify: a `Vehicle` reference *could* be pointing at a `Car` that has no `wheelie()` method at all.

This "two types" idea is the foundation of every remaining section in these notes. Reference type controls what you *may say*; runtime type controls what *actually happens*.

### The Working Example: Declared vs. Runtime Type

```java
class Vehicle {
    public void startEngine() {
        System.out.println("Vehicle engine starting");
    }

    public void honk() {
        System.out.println("Vehicle honk: beep");
    }
}

class Motorcycle extends Vehicle {
    @Override
    public void honk() {
        System.out.println("Motorcycle honk: beep beep beep");
    }

    // A method that exists ONLY on Motorcycle, not on Vehicle.
    public void wheelie() {
        System.out.println("Motorcycle popping a wheelie");
    }
}

public class ReferenceTypeDemo {
    public static void main(String[] args) {
        Motorcycle m = new Motorcycle();

        // Upcast: the DECLARED type is now Vehicle, but the RUNTIME
        // type of the object in memory is still Motorcycle.
        Vehicle v = m;

        // Compiles: honk() is declared on Vehicle.
        // Runs: Motorcycle.honk(), because the runtime object is a Motorcycle.
        v.honk();

        // Does NOT compile, even though the object really is a Motorcycle:
        // v.wheelie();   // <-- try uncommenting this line

        // Downcast first, then the Motorcycle-only method becomes reachable.
        ((Motorcycle) v).wheelie();
    }
}
```

**Expected Output**

```text
Motorcycle honk: beep beep beep
Motorcycle popping a wheelie
```

The call `v.honk()` compiles because `honk` is declared on `Vehicle`, and it prints the `Motorcycle` version because the object was created with `new Motorcycle()`. The call `v.wheelie()` would *fail to compile* — the compiler sees the declared type `Vehicle`, checks whether `Vehicle` has `wheelie()`, finds it does not, and refuses the program before it ever runs. Only after an explicit downcast `(Motorcycle) v` does the compiler allow the call. Notice the asymmetry: the compiler is strict about the *reference*, while the JVM is faithful to the *object*.

### A Diagram: The Two Types

```text
Reference variable:            Object on the heap:
                               ┌─────────────────────────┐
        Vehicle v;  ──────────►│   Motorcycle object     │
        (declared type)        │                         │
                               │  honk()      → Motorcycle version
                               │  startEngine()→ Vehicle version
                               │  wheelie()   → Motorcycle-only
                               └─────────────────────────┘

   The compiler looks at "Vehicle v"
     → only honk() and startEngine() are callable.
   The JVM looks at "new Motorcycle()"
     → honk() dispatches to the Motorcycle version.
```

### Declared Type vs. Runtime Type: The Table

| Example | Declared (compile-time) type | Runtime (actual) type | What **compiles** | What **runs** |
|---|---|---|---|---|
| `Vehicle v = new Motorcycle();` | `Vehicle` | `Motorcycle` | `v.honk()`, `v.startEngine()` | `Motorcycle.honk()` (overridden), `Vehicle.startEngine()` |
| `v.wheelie()` | `Vehicle` | `Motorcycle` | **Nothing — compile error** | Never runs |
| `((Motorcycle) v).wheelie()` | `Motorcycle` (after cast) | `Motorcycle` | `wheelie()` | `Motorcycle.wheelie()` |
| `Motorcycle m = new Motorcycle();` | `Motorcycle` | `Motorcycle` | `m.honk()`, `m.wheelie()` | `Motorcycle` versions of both |

> **Memory hook:** *The compiler reads the label on the box; the JVM reads the contents of the box.* The label tells you what operations are *legal*; the contents tell you what the operations actually *do*.

### Common-Student-Voice: The Follow-Up Questions

> "But I *know* `v` points to a Motorcycle — why does the compiler punish me?"
>
> Because the compiler can't read your mind, and more importantly, it can't prove that `v` will always point to a `Motorcycle`. The whole point of a `Vehicle` reference is that it might later be reassigned to a `Car` or a `Truck`. Allowing `v.wheelie()` would let you call a method that a `Car` doesn't have. The compiler enforces the *contract*, not the coincidence.

> "So is casting a hack? Should I avoid it?"
>
> Downcasting is not a hack; it's a *verified* narrowing. The JVM inserts a `checkcast` instruction that throws `ClassCastException` if the object isn't actually the cast target. Cast when you have extra information the type system can't express — but if you find yourself casting constantly, it's usually a design smell that signals you should have used polymorphism instead (Section 4).

### What's Happening Under the Hood?

Every local variable and parameter in a Java method has a *declared type* recorded in the method's bytecode attributes (in the `LocalVariableTable` and the method descriptor). When `javac` sees `v.honk()`, it looks up `honk` in `Vehicle`'s set of methods; finding it, it emits `invokevirtual` referencing `Vehicle.honk`. Crucially, the bytecode instruction *does not* contain "Vehicle" as a hard target in the way `invokestatic` does — `invokevirtual` contains the name, the argument types, and the *return type* of the method, and the JVM resolves it by walking the runtime class's method table. When you downcast, the compiler inserts a `checkcast` bytecode instruction; at runtime the JVM verifies the object really is (a subtype of) the cast target, and throws `ClassCastException` if it is not. So even "the compiler refused" and "the cast worked" are mechanical, inspectable steps in the bytecode.

### Real-World Use Case: Programming to Interfaces in the Collections Framework

The single most common place this shows up in professional Java is the idiom `List<String> names = new ArrayList<>();`. The declared type is the interface `List`; the runtime type is `ArrayList` (or later, `LinkedList`). Whole libraries accept `List` parameters and call only `List` methods — which is exactly the safety the compiler enforces.

```java
import java.util.ArrayList;
import java.util.List;

public class ListDemo {
    // Accept a List, not an ArrayList — this is "program to the interface."
    public static void printSizes(List<String> first, List<String> second) {
        // size() and get() are declared on List, so this compiles no matter
        // which concrete list implementation we receive at runtime.
        System.out.println(first.size() + " + " + second.size()
                + " = " + (first.size() + second.size()));
    }

    public static void main(String[] args) {
        List<String> names = new ArrayList<>(List.of("Ada", "Grace"));

        // get(int) compiles because List declares it.
        String first = names.get(0);

        // A method that exists ONLY on ArrayList would NOT compile here:
        // names.ensureCapacity(100);   // <-- ArrayList-only, rejected

        System.out.println(first);
        printSizes(names, List.of("Alan", "Dennis", "Barbara"));
    }
}
```

**Expected Output**

```text
Ada
2 + 3 = 5
```

`names.get(0)` compiles because `List` declares `get`. `names.ensureCapacity(100)` would fail to compile because `ensureCapacity` is an `ArrayList`-only method and the declared type is `List` — the same mechanism that blocked `v.wheelie()` above. This is why framework methods like `Collections.sort(List<T>)` and Spring's injection points declare interfaces: they are safe for *any* implementation, and the compile-time check guarantees it.

> **Transition:** Once you accept that the reference type gates *what you may call* while the runtime type decides *what runs*, the next question is why the language bothers to make the runtime decision at all — that's the reason runtime polymorphism exists.

---

## Section 4: Why Runtime Polymorphism Exists

### The Conceptual Explanation

Runtime polymorphism — also called **dynamic dispatch** — is the answer to a design problem: *how do you write code that works for an entire family of types, even types that haven't been invented yet?* Without dynamic dispatch, you'd need an `if`-chain for every possible kind of object, and every time someone added a new subclass, you'd have to edit that chain — and so would every library that used it.

The mechanism is simple to state: when you call an overridden instance method through a reference, the JVM ignores the reference type and asks the *actual object* which version of the method to run. That's it. But the *consequences* are enormous. It means you can write `for (Shape s : shapes) { s.draw(); }` once, and the loop handles `Circle`, `Rectangle`, `Triangle`, and any future subclass without being changed. The method called is decided by the runtime object, not the reference. This is the **Liskov substitution principle** in action: if `Circle` IS-A `Shape` and behaves properly, any code that expects a `Shape` can safely be handed a `Circle`.

### The Working Example: The Shape Hierarchy

```java
abstract class Shape {
    // The contract: every shape can report its area.
    public abstract double area();

    // A concrete method on Shape that relies on the abstract area().
    // Because area() is virtual, this method behaves differently per subclass.
    public void describe() {
        System.out.println("I am a " + getClass().getSimpleName()
                + " with area " + area());
    }
}

class Circle extends Shape {
    private final double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }
}

class Rectangle extends Shape {
    private final double width;
    private final double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public double area() {
        return width * height;
    }
}

public class PolymorphismDemo {
    public static void main(String[] args) {
        // One array, mixed runtime types, one declared type: Shape.
        Shape[] shapes = { new Circle(1.0), new Rectangle(2.0, 3.0) };

        // The SAME call site serves every shape.
        // draw()-style logic: the object decides what happens.
        for (Shape s : shapes) {
            s.describe();
        }
    }
}
```

**Expected Output**

```text
I am a Circle with area 3.141592653589793
I am a Rectangle with area 6.0
```

The loop contains a single call, `s.describe()`, compiled once against `Shape`. Yet for the first iteration the JVM dispatches to `Circle.describe()` (inherited, but running `Circle.area()`), and for the second to the `Rectangle` path. The magic in `describe()` is even more subtle: `describe()` itself is *not* overridden anywhere — it lives only on `Shape` — but the `area()` call inside it *is* dispatched dynamically, so the same printed sentence reads "Circle... 3.14" and "Rectangle... 6.0". One method, one call site, infinitely many behaviors. That is polymorphism.

### The Analogy: The Traffic Controller

A traffic controller stands at an intersection and gives every approaching vehicle the same signal: a green "go." Each vehicle *reacts differently* to the identical instruction. A car accelerates and shifts gears. A bike pedals forward. A truck rumbles away, its engine roaring. The controller never needs to know how to drive each vehicle — they just give the signal and trust the vehicle to know itself.

- The **traffic controller** is your code (`for (Shape s : shapes) { s.describe(); }`).
- The **"go" signal** is the method call (`s.describe()`).
- The **vehicles** are the objects (`Circle`, `Rectangle`).
- The **reaction** is the overridden method body — chosen by the object, not by the controller.
- A **new type of vehicle** appearing next year (say, a skateboard) is a new subclass; the controller still just says "go." No intersection code needs to change.

> **Memory hook:** *The caller says WHAT; the object decides HOW.* If you ever find yourself writing `if (x instanceof Circle) ... else if (x instanceof Rectangle) ...`, you have stopped being a traffic controller and started trying to drive every vehicle yourself.

### Compile-Time vs. Runtime Decision: The Table

| Aspect | **Static binding** (Section 1) | **Dynamic dispatch** (this section) |
|---|---|---|
| When decided | Compile time | Runtime |
| Based on | Reference type | Runtime (actual) type |
| Which methods | `static`, `private`, `final` | Overridable instance methods |
| Bytecode | `invokestatic`, `invokespecial` | `invokevirtual`, `invokeinterface` |
| Can be changed by subclasses? | No | Yes |
| Cost | Near zero | One vtable lookup per call |
| Typical use | Utility calls, constructors | Frameworks, event systems, plugins |

### What's Happening Under the Hood?

Each class, when loaded, gets a **method table** (vtable) in the JVM's memory: an array of pointers to the actual code of every virtual method it can run, in a fixed order determined by the superclass chain. When a subclass overrides a method, it places its *own* code pointer in the same slot its parent used. `invokevirtual` works in three steps: (1) pop the object reference, (2) read the class of that object, (3) index into that class's vtable using the *fixed slot number* that was computed at compile time, then jump to whatever code pointer is there. Because slots are positional, the same bytecode works for every class in the hierarchy — the only variable is which table the object's class points to. This is why adding a new subclass costs zero changes to existing callers: the new class just builds its own table with the same slot layout.

### Why Does Java Bother? Real-World Context

Dynamic dispatch is the backbone of every major Java ecosystem feature. **GUI frameworks** (JavaFX, Swing): an event handler is registered as a callback, and the framework calls `handle(event)` on whatever listener object you supplied — it has no idea what your listener does. **Collections and sorting**: `Collections.sort(list, comparator)` calls `comparator.compare(a, b)` polymorphically; you can sort the same list in five different orders by passing five different comparators. **Plugin systems and DI frameworks** (Spring): a framework instantiates your classes and calls interface methods on them, often before your code has even met the framework. None of this would be possible with static binding — a framework cannot know your class at the time it was compiled, so it *must* defer the decision to runtime.

### Real-World Use Case: Sorting With a Comparator

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ComparatorDemo {
    public static void main(String[] args) {
        List<String> names = new ArrayList<>(List.of("Alice", "bob", "Carol"));

        // sort() is the "traffic controller": it calls compare() on whatever
        // Comparator object it receives. The exact ordering logic is decided
        // at runtime by the object we hand it.
        Collections.sort(names, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return a.compareToIgnoreCase(b);
            }
        });

        System.out.println(names);

        // The same sort() call, a different runtime Comparator → different order.
        Collections.sort(names, Comparator.reverseOrder());
        System.out.println(names);
    }
}
```

**Expected Output**

```text
[Alice, bob, Carol]
[Carol, bob, Alice]
```

`Collections.sort` is compiled once, against the `Comparator` interface, and its internal `compare` calls dispatch to whatever comparator object is passed. With the case-insensitive comparator, the order is `[Alice, bob, Carol]`; with `reverseOrder()`, the *same* `sort` method produces the reverse. Two behaviors from one method, zero changes to the library — that is why frameworks and libraries are written against interfaces and why your own code should be, too.

> **Transition:** With polymorphism in place, one more piece completes the picture: the return type of an overridden method may itself be refined — which is the surprisingly flexible rule of covariant return types.

---

## Section 5: Covariant Return Types

### The Conceptual Explanation

**Covariant return types** are the rule that allows an overriding method to declare a *more specific* return type than the method it overrides. If `Parent` declares `Animal makePet()`, a `Child` may override it as `Dog makePet()` — because every `Dog` IS-A `Animal`, so anyone expecting an `Animal` is still fully satisfied by a `Dog`.

This might sound like a niche detail, but it was a genuine *feature addition*: it was introduced in **Java 5 (2004)**. Before that, an override had to have the *exact same* return type — the return type was said to be **invariant**. Why did Java bother to relax the rule? Because the pre-Java-5 situation forced ugly, unnecessary casts and made good API design impossible. If your method genuinely promised a more specific type, you had to override with the parent return type anyway, and then cast at every call site. Java 5 removed that friction.

Why is it *safe*? Because of the subtype relationship. The contract of the original method is "callers get an `Animal`." The override returns a `Dog`, which *is* an `Animal`. No caller can tell the difference; no caller's expectation is violated. The return type only became more informative, never less. That directionality — more specific down the hierarchy — is exactly what "covariant" means: the return types vary *together* with the class types.

### The Working Example: An Inheritance Hierarchy

```java
class Animal {
    public void speak() {
        System.out.println("Some animal noise");
    }
}

class Dog extends Animal {
    @Override
    public void speak() {
        System.out.println("Woof!");
    }

    // A method that only dogs have.
    public void fetch() {
        System.out.println("Fetching the stick...");
    }
}

class AnimalShelter {
    // The original contract: every shelter can produce an Animal.
    public Animal adopt() {
        return new Animal();
    }
}

class DogShelter extends AnimalShelter {
    // COVARIANT RETURN: the override may return Dog instead of Animal.
    // This is legal because Dog IS-A Animal.
    @Override
    public Dog adopt() {
        return new Dog();
    }
}

public class CovariantReturnDemo {
    public static void main(String[] args) {
        AnimalShelter shelter = new DogShelter();

        // Even through an AnimalShelter reference, the runtime object is a Dog.
        Animal pet = shelter.adopt();
        pet.speak();

        // No cast needed here: the DECLARED type of the return is Dog,
        // so Dog-only methods are directly callable.
        DogShelter dogShelter = new DogShelter();
        Dog realDog = dogShelter.adopt();
        realDog.fetch();
    }
}
```

**Expected Output**

```text
Woof!
Fetching the stick...
```

First, `shelter.adopt()` — called through the `AnimalShelter` reference — runs `DogShelter.adopt()` (dynamic dispatch) and returns a `Dog` stored in an `Animal` variable; `pet.speak()` prints "Woof!" because the runtime type is `Dog`. Second, through the `DogShelter` reference, the method's declared return type is `Dog`, so `realDog.fetch()` compiles *without any cast*. That second line is the entire payoff of covariant returns: the compiler already knows you got a `Dog`, so it hands you Dog's full API.

### The "Before" Example: The Cast-Filled World of Java ≤ 1.4

```java
// BEFORE (pre-Java-5 style): covariant returns were illegal,
// so the override had to keep the parent return type.
class BeforeDogShelter extends AnimalShelter {
    // WRONG-EXAMPLE for Java 1.4: returning Dog here would NOT compile.
    // You were forced to return Animal and cast at every call site.
    @Override
    public Animal adopt() {
        return new Dog();   // the Dog is disguised as an Animal
    }
}

public class BeforeDemo {
    public static void main(String[] args) {
        BeforeDogShelter shelter = new BeforeDogShelter();

        // Ugly: the call site must downcast even though the shelter
        // "obviously" always returns a Dog.
        Dog dog = (Dog) shelter.adopt();

        // And if you forget the cast, fetch() is unreachable:
        // shelter.adopt().fetch();   // <-- does not compile without the cast
        System.out.println("Got a " + dog.getClass().getSimpleName());
    }
}
```

**Expected Output**

```text
Got a Dog
```

Every call site that wanted a `Dog` was forced to write `(Dog) shelter.adopt()`. If you forgot the cast, you couldn't call `fetch()` at all. If the implementation later changed to return a `Cat`, the cast would throw `ClassCastException` at runtime. Covariant returns eliminate the boilerplate and move the check to compile time — the compiler now *proves* the return is a `Dog`, so no cast and no surprise.

### Return Type Relationships: The Table

| Overridden return type | Override return type | Allowed? | Why |
|---|---|---|---|
| `Animal` | `Animal` | ✅ Yes | Exact match (invariant) |
| `Animal` | `Dog` | ✅ Yes | **Covariant** — `Dog` IS-A `Animal`, safe |
| `Dog` | `Animal` | ❌ **No** | Contravariant — an override could hand callers a non-`Dog`; breaks the contract |
| `Animal` | `String` | ❌ **No** | Unrelated types — no subtype relationship |
| `int` | `long` | ❌ **No** | Primitives are invariant; widening is *not* covariance |

> **Memory hook:** *The override may return a subtype, never a supertype.* Down the class hierarchy, return types may only become **more specific** — the same direction the classes themselves go. That's why it's called *co*-variant: the return types vary *with* the class types.

### Why Is It Safe, and Why Did Java 5 Bother?

Safety comes from the subtype guarantee: a method's return type is a promise to callers, and returning a `Dog` when an `Animal` was promised is a *stronger* promise, never a weaker one. Every caller that accepted `Animal` accepts `Dog` with zero changes. Java 5 introduced the feature because the pre-2004 world was actively hostile to good design: API authors either wrote methods returning the most general useful type (forcing casts everywhere) or duplicated methods. Modern Java APIs like `Object.clone()` (which returns `Object`, and whose overrides can return the concrete type) and the fluent `Stream`/`StringBuilder` styles lean on covariant returns constantly.

### Common-Student-Voice: The Classic Doubt

> "Wait — if the return type can differ, how does the JVM even *know* which method is which?"
>
> Because the return type is part of a method's signature for the *compiler and the JVM*. The JVM method descriptor encodes `(arguments)returntype`, so `adopt()Animal` and `adopt()Dog` are distinguishable in the class file. When you write an override with a covariant return type, `javac` generates a **bridge method**: a synthetic `adopt()Animal` method that simply calls your `adopt()Dog` and casts the result. That bridge keeps the JVM's method-resolution rules intact while giving *you* the nice type. The JVM sees two methods; your source code sees one. It's compiler magic that just works.

### What's Happening Under the Hood?

At the bytecode level, covariant overrides compile to two methods: your real method (with the specific return type) plus a synthetic **bridge method** holding the parent's descriptor, whose body is simply `return (Animal) this.adopt()Dog();`. The bridge is marked with `ACC_BRIDGE | ACC_SYNTHETIC` and exists solely to satisfy the JVM's method-resolution lookup (which matches on name plus descriptor). Without the bridge, a call through a `AnimalShelter` reference would fail to find `DogShelter.adopt()`. The `javac` compiler generates bridges automatically, so you never write them — but if you ever look at decompiled or disassembled code (`javap -c`), this is what you'll see.

### Real-World Use Case: `Object.clone()` and Fluent APIs

`Object.clone()` returns `Object`, so every class that implements `Cloneable` can override it with a covariant return type — and the JDK's own `StringBuffer`, `StringBuilder`, and `Date` classes do exactly that. The same pattern powers the fluent style where chained calls must return the concrete type.

```java
class Message implements Cloneable {
    private final String text;

    public Message(String text) {
        this.text = text;
    }

    // Covariant override: we promise a Message, not just an Object.
    @Override
    public Message clone() {
        try {
            // super.clone() returns Object; we cast once, here, and
            // callers never have to.
            return (Message) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public String getText() {
        return text;
    }

    public static void main(String[] args) {
        Message original = new Message("Hello, world");

        // No cast at the call site — clone() is declared to return Message.
        Message copy = original.clone();

        // Because Message has no mutable state, the clone is "equal"
        // in content to the original.
        System.out.println(copy.getText());
    }
}
```

**Expected Output**

```text
Hello, world
```

`copy` is typed `Message` directly from `original.clone()`, with no downcast — the covariant return type in the override makes that possible. If the JDK had not added this feature, every `clone()` caller in the entire ecosystem would carry a `(Message)` cast. That one-line difference is the whole story of Java 5's improvement: the language got *safer* and *more concise* at the same time.

> **Transition:** Now that all five confusions have been unpacked, let's compress everything into the mistakes to avoid and the rules to keep.

---

## Common Mistakes & Gotchas

- **Putting `@Override` on a static method.** It's a compile error — the annotation only verifies *overrides*, and static methods can only be *hidden*. Correction: if the method is `static`, drop the annotation and expect hiding.
- **Expecting a static method to be polymorphic through a parent reference.** `Parent p = new Child(); p.greet();` calls `Parent.greet()`. Correction: remember `invokestatic` binds to the reference type at compile time.
- **Calling an instance method without an object.** `getBalance()` with no receiver won't compile. Correction: every instance method needs a `this`; use `alice.getBalance()` or make the method `static` if it truly needs no state.
- **Forgetting that the compiler uses the declared type.** `List<String> names = new ArrayList<>(); names.ensureCapacity(100);` fails. Correction: declare the variable as the type whose full API you need, or cast.
- **Believing the runtime type governs *what you may call*.** It governs only *which implementation runs*. The declared type governs legality. Correction: two types, two jobs — keep them separate.
- **Downcasting without a check when you're not certain.** `(Motorcycle) v` throws `ClassCastException` at runtime if the object isn't a `Motorcycle`. Correction: guard with `instanceof`, or redesign to avoid the cast.
- **Writing `if (x instanceof Y) ... else if (x instanceof Z) ...` chains.** You've reimplemented dynamic dispatch by hand. Correction: define the behavior in the subclasses (an overridden method) and let the JVM dispatch.
- **Assuming an override must have the *same* return type.** Since Java 5 it may be a *subtype* — but never a supertype. Correction: covariant returns are legal; contravariant returns are not.
- **Expecting primitive widening to be covariant.** `int` → `long` is *not* a valid covariant return change. Correction: covariance applies to reference types only.
- **Thinking a `final` class's methods can be overridden.** A `final` class cannot be subclassed at all, so no override is possible. Correction: finality is a deliberate "polymorphism off" switch.

---

## Summary / Key Takeaways

- **Static methods are never overridden — they are hidden.** Resolution happens at compile time via `invokestatic`, based on the *reference type*, and `@Override` on a static method is a compile error.
- **Instance methods exist once in memory, but `this` changes per call.** The object is the hidden first argument; without it there is no state to operate on, which is why `list.size()` needs a list but `Math.max(...)` needs nothing.
- **Every object variable has two types: declared and runtime.** The declared type decides *what compiles*; the runtime type decides *what runs*. `Vehicle v = new Motorcycle()` gives you the `Motorcycle` object but only the `Vehicle` API.
- **Dynamic dispatch is why `what runs` differs from `what compiles`.** `invokevirtual` looks up the method in the runtime object's vtable, enabling one call site to serve any subclass — the foundation of frameworks, GUIs, sorting, and dependency injection.
- **Covariant return types let an override return a subtype** of the overridden method's return type, because a subtype IS-A its supertype. Java 5 added this to eliminate pointless casts; the compiler generates a synthetic bridge method under the hood.
- **The whole confusion reduces to three questions:** What did I declare? (compiler) What object did I create? (JVM) Does the method have a `this`? (static vs. instance).
- **When you find yourself casting, checking `instanceof`, or writing `if/else` chains over subclass types, stop** — you're fighting the language instead of using its polymorphism.

---

## Practice Exercises

**1. (Easy) What is the output?**
```java
class A {
    public static void tag() { System.out.println("A"); }
    public void speak() { System.out.println("A speaks"); }
}
class B extends A {
    public static void tag() { System.out.println("B"); }
    @Override public void speak() { System.out.println("B speaks"); }
}
public class Q1 {
    public static void main(String[] args) {
        A ref = new B();
        ref.tag();
        ref.speak();
        B.tag();
    }
}
```

**2. (Easy) What is the output?**
```java
public class Q2 {
    static int counter = 0;
    int value;

    Q2(int v) { value = v; counter++; }

    int bump() { value++; counter++; return value; }

    public static void main(String[] args) {
        Q2 a = new Q2(1);
        Q2 b = new Q2(10);
        System.out.println(a.bump());
        System.out.println(b.bump());
        System.out.println(Q2.counter);
    }
}
```

**3. (Medium) Does this compile? If not, why, and how do you fix it?**
```java
class Fruit { public void peel() {} }
class Apple extends Fruit { public void core() {} }

public class Q3 {
    public static void main(String[] args) {
        Fruit f = new Apple();
        f.core();                // (a)
        ((Apple) f).core();      // (b)
        Fruit g = new Fruit();
        ((Apple) g).core();      // (c)
    }
}
```
For each of the three lines, state whether it compiles and (if it runs) what happens at runtime.

**4. (Medium) Write the code.** Design a `Payment` hierarchy with an abstract method `double amount()`. Add `CashPayment` and `CardPayment` subclasses with a `surcharge` field added in the override for `CardPayment` (via a covariant-return helper `withSurcharge(double)` that returns `CardPayment`). Then in `main`, iterate a `Payment[]` array and print each payment's amount, demonstrating that `amount()` is dispatched dynamically.

**5. (Hard) Explain and fix.** A teammate wrote this and says the output is `"Circle drawn"` for both lines:
```java
class Shape2 { public void draw() { System.out.println("Shape drawn"); } }
class Circle2 extends Shape2 { public void draw() { System.out.println("Circle drawn"); } }
public class Q5 {
    static void drawIt(Shape2 s) { s.draw(); }
    public static void main(String[] args) {
        Shape2 s = new Circle2();
        drawIt(s);
        Shape2[] arr = { new Circle2(), new Shape2() };
        for (Shape2 x : arr) drawIt(x);
    }
}
```
Explain exactly what actually prints and why. Then describe how the vtable makes this work — name the bytecode instruction used for `s.draw()` and explain the three steps the JVM performs.

---

### Answer Key

**1.** Output: `A`, `B speaks`, `B`. `ref.tag()` binds statically to declared type `A`; `ref.speak()` dispatches to the runtime type `B`; `B.tag()` is explicitly the child's static method.

**2.** Output: `2`, `11`, `3`. Each `bump()` uses its own object's `this.value` (1→2, 10→11), while `counter` is shared static state incremented by two constructors plus two `bump()` calls.

**3.** (a) does **not compile** — `core()` is not declared on `Fruit`. (b) compiles and runs — the object really is an `Apple`. (c) compiles but throws `ClassCastException` at runtime — the object is a plain `Fruit`, so the JVM's `checkcast` fails.

**4.** Sample:
```java
abstract class Payment { public abstract double amount(); }
class CashPayment extends Payment {
    private final double cash;
    CashPayment(double c) { cash = c; }
    @Override public double amount() { return cash; }
}
class CardPayment extends Payment {
    private final double charged;
    CardPayment(double base, double surcharge) { charged = base + surcharge; }
    CardPayment withSurcharge(double extra) { return new CardPayment(0, extra); }
    @Override public double amount() { return charged; }
}
```
Key idea: `amount()` is called through a `Payment` reference and resolved by runtime type.

**5.** Actual output is `Circle drawn`, `Circle drawn`, `Shape drawn`. The vtable slots for `draw` differ per class (`Circle2`'s slot points to its own `draw`; `Shape2`'s points to the base), and `invokevirtual` (1) pops the receiver, (2) reads its class, (3) indexes that class's vtable at the compile-time-fixed slot to find the method to run.

---

# Method Types in Java: Static, Final, Private, Overloaded, and Overridden Methods

## Opening Overview

Every Java program you have written so far is built from methods — the named, reusable blocks of behavior that give objects life. But not all methods are created equal. A `static` method belongs to a class; an *instance* method belongs to an object. A `final` method is locked against future change; a `private` method is hidden from the outside world entirely. Two methods can share a name yet be perfectly legal (overloading), or a child class can replace a parent's behavior entirely (overriding). These five *method types* form the grammar of everyday Java code, and the decisions you make about them ripple outward into every API you design.

Understanding method types is not trivia — it is the difference between code that is rigid and fragile and code that is flexible and maintainable. When you choose `static` over instance, you decide where state lives. When you mark a method `final`, you decide how much future developers (including yourself) may change behavior. When you overload or override, you decide how your classes read at the call site and how polymorphism powers your architecture.

This chapter will teach you:

- **1. Introduction** — the anatomy of a method and how invocations are executed.
- **2. Static methods** — class-level behavior, utility classes, and the `main` method.
- **3. Final methods** — locking behavior against overriding.
- **4. Private methods** — encapsulation and implementation hiding.
- **5. Overloaded methods** — the same name, different parameters.
- **6. Overridden methods** — subclass customization and runtime polymorphism.
- **7–10.** Comparison, pitfalls, practice exercises, and a summary of everything covered.

By the end, you will not just *know* these method types — you will know *when* to use each one, which is the real skill.

---

## 1. Introduction to Methods in Java

Before we distinguish the five method types, we need a shared vocabulary. A **method** is a named block of statements that can be invoked (called) to perform an action or compute a result. Methods let you decompose a problem, reuse logic, and hide complexity.

### 1.1 Quick Recap: Method Signature, Return Type, Parameter List, Access Modifiers

Every method declaration has several mandatory and optional parts. Consider this declaration:

```java
public  double  calculateArea(double radius)  {
    return Math.PI * radius * radius;
}
```

- **Access modifier** — `public` controls *who* can call the method (the method's visibility). Options are `public`, `protected`, default (package-private), and `private`.
- **Return type** — `double` is the type of value the method produces. A method that returns nothing uses the keyword `void`.
- **Method name** — `calculateArea`, a verb-like identifier following camelCase convention.
- **Parameter list** — `(double radius)` declares the inputs the caller must supply, each with a type and a name.
- **Body** — the `{ ... }` block containing the statements executed on each call.

The **method signature** is the method's *identity* used by the compiler: it consists of the **method name and the parameter list** (types and order). Crucially, the *return type and access modifier are not part of the signature*. This distinction matters enormously for overloading (Section 5) and overriding (Section 6).

> **Best Practice:** Name methods with verbs (`getTotal`, `isEmpty`, `sendInvoice`) and keep each method focused on exactly one responsibility. A method that does one thing is easy to name, test, and reuse.

### 1.2 The Method Call Stack and Method Invocation

When your program calls a method, the JVM pushes a new **stack frame** onto the **call stack** — a LIFO (last-in, first-out) structure that tracks which method is currently executing and where execution must resume when it finishes. Each frame holds the method's local variables, parameters, and the return address.

```java
public class CallStackDemo {
    public static void main(String[] args) {
        int result = add(3, 4);     // frame for main() exists; add() is pushed on top
        System.out.println(result); // after add() returns, main() resumes
    }

    static int add(int a, int b) {
        return a + b;               // a and b live only in add()'s frame
    }
}
```

*Listing 1.1 — A minimal stack trace in action. Output: `7`*

When `main` calls `add`, a new frame is created for `add`, and control jumps into it. When `add` returns, its frame is popped off the stack, the return value `7` is handed back to `main`, and `main` continues. If a method calls itself too deeply (runaway recursion), the JVM throws `StackOverflowError` — the stack has run out of space.

Every call site must resolve *which* method body actually executes — and, as you will see in Sections 5 and 6, this resolution follows two different rules: at *compile time* for overloaded methods and at *runtime* for overridden methods.

The table below maps the method types we will study to their sections:

| Method Category | Keyword(s) | Key Property | Section |
|---|---|---|---|
| Static method | `static` | Belongs to the class, not an instance | Section 2 |
| Final method | `final` | Cannot be overridden by subclasses | Section 3 |
| Private method | `private` | Visible only within the declaring class | Section 4 |
| Overloaded method | (none; same name) | Same name, different parameter list | Section 5 |
| Overridden method | (none; in subclass) | Subclass redefines inherited behavior | Section 6 |

*Table 1.1 — The five method types and where each is covered in this chapter.*

---

## 2. Static Methods

Think of a class as an architectural **blueprint** and each object as a **house** built from it. Instance methods are behaviors of the *house* ("turn on the lights," "open the door") — they need a real house to run on. Static methods are behaviors of the *blueprint itself* — they describe rules about houses in general and need no particular house at all.

### 2.1 What Makes a Method Static

A **static method** belongs to the *class*, not to any instance of the class. It can be invoked without creating an object. Because there is no instance, a static method has no `this` reference — there is no "current object" for it to inspect.

```java
public class MathUtils {
    public static int max(int a, int b) {
        return (a > b) ? a : b;   // no instance state needed — pure function of its arguments
    }
}
```

*Listing 2.1 — A static method as a pure function (fragment).*

The method `max` computes entirely from its parameters. It does not touch any instance field, which is exactly what makes it a natural candidate for `static`.

> **Analogy:** Static methods are like instructions printed on a blueprint — "all houses built from this blueprint must have a fire escape." Instance methods are like actions you perform on a finished house — "open the front door." You can follow the blueprint's instructions without any house; you cannot open a door without a house.

### 2.2 Syntax and Declaration Rules

The syntax is straightforward: the `static` modifier sits between the access modifier and the return type.

```java
public class Counter {
    private static int totalCount = 0;   // static FIELD: shared across all instances

    public static void incrementTotal() { // static METHOD
        totalCount++;                     // OK: can access static fields
        System.out.println("Total is now " + totalCount);
    }

    private int instanceCount = 0;

    public static void show() {
        // instanceCount++;  // ERROR: cannot reference instance field from static context
        // this.instanceCount; // ERROR: 'this' is not available in a static context
        System.out.println("Hello from a static method");
    }
}
```

*Listing 2.2 — Static fields and static methods (fragment).*

The declaration rules follow directly from "no instance exists":

- A static method can call **other static members** (static fields and static methods) directly.
- A static method **cannot access instance fields or instance methods** without an object reference.
- A static method **cannot use `this` or `super`** — there is no current instance.
- `static` methods are resolved at **compile time**, not runtime (more in Section 7.3).

### 2.3 Calling Static Methods: Class Name vs. Instance Reference

The correct, preferred way to call a static method is through the **class name**:

```java
Math.max(10, 5);          // Correct — calls static max on the Math class
Counter.incrementTotal(); // Correct — calls static method via the class
```

Java also *permits* calling a static method through an instance reference, but this is a **bad idea** — it is misleading, and the compiler warns against it. The JVM ignores the object entirely and uses only the *declared type* of the reference.

```java
Counter c = new Counter();
c.incrementTotal(); // compiles with a warning: "static method should be accessed in a static way"
```

*Listing 2.3 — Calling a static method via an instance (fragment).*

> **Pitfall:** Calling a static method through an instance reference is legal but confusing. It suggests the call depends on the object's state when it does not. Always call static methods via the class name — readers, and your future self, will thank you.

### 2.4 The `main` Method as the Canonical Example

You have written `main` dozens of times without perhaps realizing you were writing a static method. The JVM must launch your program *before any objects exist*, so the entry point has to be static:

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello, world!"); // System.out is a static field; println is an instance method
    }
}
```

*Listing 2.4 — The canonical static method: the program entry point.*

Three modifiers must be exactly right, or the JVM refuses to launch the program:

- `public` — so the JVM (an external caller) can access it.
- `static` — so it can run without an instance.
- `void` — the entry point returns nothing to the OS.

The parameter `String[] args` receives command-line arguments. Note the subtlety in the body: `System.out` is a *static field* holding a `PrintStream` object, and `println` is an *instance method* invoked on that object — a perfect miniature example of both method types working together.

### 2.5 Utility Classes and Factory Patterns — Real-World Use Cases

Static methods shine when behavior is a pure function of its inputs, or when you need to create objects in a controlled way.

**Utility classes** group related operations that share no state. The Java standard library is full of them: `Math` (`Math.sqrt`, `Math.random`), `Collections` (`Collections.sort`, `Collections.max`), `Arrays` (`Arrays.copyOf`), and `String` (`String.valueOf`, `String.format`). Such classes often declare a `private` constructor to prevent instantiation, signaling "this class is a toolbox, not a mold":

```java
public class TextTools {
    private TextTools() { /* prevent instantiation — utility class */ }

    public static String reverse(String input) {
        return new StringBuilder(input).reverse().toString();
    }

    public static int countVowels(String input) {
        int count = 0;
        for (char ch : input.toLowerCase().toCharArray()) {
            if ("aeiou".indexOf(ch) >= 0) count++;
        }
        return count;
    }
}
```

*Listing 5.5-referenced — a typical utility class pattern (fragment).*

**Factory methods** are static methods that create and return instances, often choosing a subtype or configuring the object. `Integer.valueOf(int)` returns a cached `Integer` where possible; `LocalDate.of(y, m, d)` builds a date safely; `Collections.emptyList()` returns an immutable empty list without you writing `new`. The factory pattern centralizes construction logic and gives it a descriptive name:

```java
public class Book {
    private final String title;
    private Book(String title) { this.title = title; }

    public static Book of(String title) {        // named constructor via static factory
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        return new Book(title);                  // validation happens in ONE place
    }

    public static Book specialEdition(String title) {
        return new Book("Special Edition: " + title);
    }
}
```

*Listing 2.5 — Static factory methods in practice.*

> **Real World:** The `Collections` class cannot be instantiated and offers dozens of static utilities operating on collections. When you write `Collections.sort(list)`, you rely on a static method — no `Collections` object ever exists. This is the utility-class idiom used throughout the JDK and in libraries like Guava.

### 2.6 Common Pitfalls: Accessing Instance Fields and `this`

Because beginners often blur "class" and "instance," the classic error is trying to use instance state inside a static method:

```java
public class Broken {
    private int value = 10;                 // instance field

    public static void printValue() {
        // System.out.println(value);       // COMPILE ERROR: non-static variable value
        //                                 // cannot be referenced from a static context
        // System.out.println(this.value);  // COMPILE ERROR: 'this' cannot be used in a static context
    }
}
```

*Listing 2.6 — The classic static/instance confusion. Both statements are compile errors.*

**Why?** When `printValue` runs, no `Broken` object necessarily exists — so `value`, which lives *inside* each object, may not exist either. The compiler refuses to guess. The fix is either to make `value` static (if it is genuinely class-level state) or to pass an instance in:

```java
public static void printValue(Broken b) {   // accept the instance as a parameter
    System.out.println(b.value);
}
```

Another subtle pitfall: because static methods bind at compile time, calling a "static" version of a method through an instance reference can hide the fact that subclasses cannot override it polymorphically (see Section 7.3).

---

## 3. Final Methods

A class is a contract with the world. Sometimes, you want to say: *this behavior is the behavior — nobody may change it.* That is the job of the `final` modifier.

### 3.1 Purpose of `final` Methods

A **final method** is a method that **cannot be overridden** by a subclass. Once declared `final`, its implementation is fixed for the entire inheritance hierarchy below it. It is a promise: "every subclass, now and forever, will use this exact behavior."

Why would you want that? Overriding is powerful, but it is also a point of fragility. If a subclass replaces a method that holds a critical invariant — a security check, a consistency guarantee — it could break the whole design. Marking the method `final` makes the invariant unbreakable.

> **Analogy:** A `final` method is like a **sealed law**: Parliament writes it and says "no amendment, ever." A non-final method is an ordinary law that later governments may revise. Both are legitimate in a democracy — but for the rules that protect fundamental rights, you want them sealed.

### 3.2 Syntax and Where `final` Can Appear

The `final` keyword appears in the method declaration, after the access modifier:

```java
public class Vehicle {
    private final int maxSpeed;

    public Vehicle(int maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public final int getMaxSpeed() {     // final method: cannot be overridden
        return maxSpeed;
    }

    public void start() {                 // ordinary method: CAN be overridden
        System.out.println("Vehicle starting...");
    }
}
```

*Listing 3.1 — A final accessor (fragment).*

Related forms of `final`:

- **Final class:** `public final class String { ... }` — a final class *cannot be extended at all*, and therefore *every* method of a final class is effectively final (the compiler knows no subclass can ever exist to override them).
- **Final variables:** `final int LIMIT = 100;` — the variable can be assigned only once. (A common beginner confusion: `final` on a *field* prevents reassignment; `final` on a *method* prevents overriding. Same word, different rules.)
- You cannot mark a method both `static` and `final` *and* expect overriding behavior — static methods are never overridden anyway (Section 7.3), but the combination is legal and simply reinforces that the behavior is fixed.

### 3.3 Why and When to Use Them: Template Method Pattern, Security, and Invariants

Three concrete motivations justify the `final` modifier.

**1. The Template Method Pattern.** A base class defines the *skeleton* of an algorithm and allows subclasses to fill in only specific steps. The skeleton itself must not change, so it is declared `final`:

```java
public abstract class DataImporter {
    // TEMPLATE: the algorithm skeleton — fixed for all subclasses
    public final void importFile(String path) {
        String data = readFile(path);
        validate(data);
        save(parse(data));
        log(path);
    }

    protected abstract void validate(String data);  // steps for subclasses to implement
    protected abstract Object parse(String data);
    protected void log(String path) { System.out.println("Imported " + path); }
    private String readFile(String path) { return "raw data"; }
    private void save(Object o) { /* ... */ }
}
```

*Listing 3.2 — The template method pattern protects the algorithm's structure (fragment).*

A subclass can customize `validate` and `parse`, but it *cannot* reorder or remove steps — `importFile` is `final`.

**2. Security and invariants.** In the JDK, `String` is final, and its methods cannot be overridden. This prevents malicious subclasses from faking string behavior, which protects security-sensitive code that trusts `String`. Similarly, if your class's correctness depends on a method always behaving a certain way — a bank transfer's audit step, a cache-invalidation routine — mark it `final`.

**3. Performance.** Historically, the JIT compiler could inline `final` methods more aggressively. Modern JVMs do this regardless, so treat performance as a *side benefit*, not the reason — the real reasons are design and safety.

> **Real World:** The `Object.getClass()` method is `final`. The JVM relies on every object reporting its true runtime class; allowing subclasses to fake it would break reflection, debugging, and the entire type system. Locking such foundational methods is standard practice in frameworks.

### 3.4 Compile-Time Error Demonstration

Attempting to override a final method is a compile-time error. The compiler catches it *before* your program ever runs:

```java
public class Sport {
    public final void playByOfficialRules() {
        System.out.println("Play by official rules");
    }
}

public class Chess extends Sport {
    // @Override
    // public void playByOfficialRules() { ... }
    // COMPILE ERROR: cannot override the final method from Sport
}
```

*Listing 3.3 — Overriding a final method is rejected by the compiler (fragment).*

The exact compiler message resembles:

```
Chess.java:5: error: playByOfficialRules() in Chess cannot override
    playByOfficialRules() in Sport
  overridden method is final
```

The compiler is not being pedantic; it is protecting the contract the base class author established. If you find yourself wanting to override a `final` method, step back: the author *deliberately* locked it. Either the design intends for you to use composition instead of inheritance, or you need a different hook point (often a `protected` non-final method provided for exactly that purpose).

> **Best Practice:** Default to *non-final* methods unless you have a concrete reason to lock behavior. Overuse of `final` makes frameworks impossible to extend; underuse invites fragile overrides. Mark `final` only the methods whose behavior must be invariant.

---

## 4. Private Methods

If `final` controls *change over time*, `private` controls *visibility in space*. A private method is the most restricted member in Java: it is accessible only within the class that declares it.

### 4.1 Visibility Rules — Accessible Only Within the Declaring Class

The access modifiers, from most to least restrictive:

| Modifier | Accessible in same class | Same package | Subclass (other package) | Anywhere |
|---|---|---|---|---|
| `private` | ✅ | ❌ | ❌ | ❌ |
| *default* (none) | ✅ | ✅ | ❌ | ❌ |
| `protected` | ✅ | ✅ | ✅ | ❌ |
| `public` | ✅ | ✅ | ✅ | ✅ |

*Table 4.1 — Access modifier visibility matrix.*

A `private` method can be called by *any* code inside the same class — constructors, other methods, other private methods — but by nothing outside, including subclasses.

### 4.2 Encapsulation and Implementation Hiding

Encapsulation is the principle of hiding internal details behind a stable public interface. Private methods are the workhorses of that principle: they let you break a complex public operation into small, readable, testable steps *without* exposing those steps to the world.

```java
public class OrderService {
    public void placeOrder(Order order) {
        validateOrder(order);              // internal step #1
        double total = computeTotal(order);// internal step #2
        persist(order, total);             // internal step #3
        notifyCustomer(order);             // internal step #4
    }

    private void validateOrder(Order order) { /* checks stock, prices, addresses */ }
    private double computeTotal(Order order) { /* applies discounts and tax */ return 0.0; }
    private void persist(Order order, double total) { /* writes to database */ }
    private void notifyCustomer(Order order) { /* sends email */ }
}
```

*Listing 4.1 — Public facade with private helpers (fragment).*

**Why private?** Today all four steps live in one class. Tomorrow you might reorder them, replace the email service with SMS, or switch databases. Because callers only see `placeOrder`, you can rewrite every helper without breaking a single client. That is the maintainability payoff of implementation hiding.

> **Real World:** Service classes in Spring applications are a perfect example. The public `@Service` methods form the API; the dozen or so `private` methods beneath them hold SQL snippets, validation logic, and notification details. IDEs even let you extract a selection into a private method with one keystroke — a daily habit for working developers.

### 4.3 Private Methods and Overriding — Shadowed, Not Overridden

Here is the subtle part: **private methods cannot be overridden**, because a subclass cannot even *see* them. If a subclass declares a method with the same signature as a private method in its superclass, it is a brand-new method — completely unrelated. We say the subclass's method **hides (shadows)** the parent's.

```java
public class Parent {
    private void greet() { System.out.println("Parent says hi"); }

    public void callGreet() {
        greet();   // always invokes Parent.greet(), never Child.greet()
    }
}

public class Child extends Parent {
    private void greet() { System.out.println("Child says hi"); } // unrelated method

    public void callChildGreet() {
        greet();   // invokes Child.greet() — different method entirely
    }
}

public class PrivateDemo {
    public static void main(String[] args) {
        Parent p = new Child();
        p.callGreet();        // prints "Parent says hi" — Child.greet is invisible to Parent
        // p.greet();         // COMPILE ERROR: greet() has private access in Parent
    }
}
```

*Listing 4.2 — Private methods are hidden, never overridden. Output: `Parent says hi`*

Note that `@Override` on the subclass's `greet()` would be a compile error, precisely because the compiler knows there is nothing to override. This hiding behavior is why `final` and `private` interact so comfortably: a private method is *implicitly* final in the sense that no subclass can ever change how the parent calls it.

> **Pitfall:** Do not write a private method in a subclass expecting "polymorphic" behavior. A reference of the parent type will always invoke the parent's private method. If you want subclasses to customize behavior, the method must be `protected` or `public` and non-private.

### 4.4 Using Private Methods for Code Reuse and Reducing Duplication

Inside one class, duplication breeds bugs: fix a formula in one place and forget the copy elsewhere. Private methods let you centralize repeated logic:

```java
public class ReportGenerator {
    public String customerSummary(Customer c) {
        return header(c) + "Total orders: " + count(c.orders()) + line();
    }

    public String inventorySummary(Product p) {
        return header(p) + "Units left: " + p.unitsLeft() + line();
    }

    private String header(Object o) { return "== " + o.getClass().getSimpleName() + " ==\n"; }
    private String line()           { return "\n" + "-".repeat(30) + "\n"; }

    private int count(java.util.List<?> items) { return items.size(); }
}
```

*Listing 4.3 — Private helpers eliminate duplicated formatting logic (fragment).*

**Why private and not `protected` or `public`?** The helpers `header`, `line`, and `count` are *implementation details* — no external caller needs them, and no subclass should depend on them. Keeping them private gives you the freedom to refactor them freely later.

> **Best Practice:** Ask of every method: *Who should be allowed to call this?* If the honest answer is "only this class," make it `private`. Visibility is not decoration — it is a promise about your API's stability.

---

## 5. Overloaded Methods

Sometimes the same *concept* deserves the same *name*, even with different inputs. Java supports this directly with **method overloading**.

### 5.1 Definition: Same Method Name, Different Parameter Lists

**Overloading** means declaring multiple methods in the *same class* with the **same name** but **different parameter lists** (different types, different count, or both). The compiler treats them as entirely separate methods.

```java
public class Greeter {
    public String greet() {
        return "Hello!";
    }

    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    public String greet(String name, String title) {
        return "Hello, " + title + " " + name + "!";
    }
}
```

*Listing 5.1 — Three overloads of `greet` (fragment).*

### 5.2 Overload Resolution Rules

When the compiler sees a call like `greet("Ada")`, it must pick *one* overload. It uses a strict priority order:

1. **Exact match** — the argument types match a parameter list exactly.
2. **Widening** — promote the argument to a wider primitive type (`int` → `long` → `float` → `double`), or to a supertype for references.
3. **Boxing** — wrap a primitive in its wrapper class (`int` → `Integer`).
4. **Varargs** — pack the arguments into an array (`int...`).

```java
public class ResolutionDemo {
    public static void choose(int n)        { System.out.println("int"); }
    public static void choose(long n)       { System.out.println("long"); }
    public static void choose(Integer n)    { System.out.println("Integer"); }
    public static void choose(int... nums)  { System.out.println("varargs"); }

    public static void main(String[] args) {
        choose(42);      // exact match         -> "int"
        choose(42L);     // exact match         -> "long"
        int x = 5;
        // choose(Integer.valueOf(x)) implicit -> "Integer" (boxing, only if no widening/exact)
        choose(x, 7, 9); // varargs             -> "varargs"
    }
}
```

*Listing 5.2 — Resolution priority in action. Output: `int`, `long`, `varargs`*

The compiler tries steps in order, and the first viable match wins. `choose(5)` never reaches `Integer` because `choose(int)` is an exact match; a call like `choose(5.5)` would fail entirely because `double` cannot widen or box into any of the options.

### 5.3 Valid vs. Invalid Overloads

Overloads must differ in their **parameter lists**. The return type, access modifier, and thrown exceptions are *not* part of the signature and cannot be the sole difference.

**Invalid — same parameters, different return type:**

```java
public class InvalidOverload {
    public int calc(int x) { return x; }
    // public double calc(int x) { return x * 1.0; }
    // COMPILE ERROR: calc(int) is already defined
}
```

*Listing 5.3 — Return type alone cannot differentiate overloads (fragment).*

**Valid — different parameter types or counts:**

```java
public class ValidOverload {
    public double calc(double x) { return x; }      // different parameter TYPE
    public int calc(int x, int y) { return x + y; } // different parameter COUNT
    public int calc(int... xs) { return xs.length; }// varargs counts as a distinct list
}
```

*Listing 5.4 — Valid overloads (fragment).*

**Why can't the return type differentiate?** Because Java allows you to *ignore* a method's return value — `calc(5);` as a statement. If two `calc(int)` methods differed only in return type, the compiler could not tell which one the bare call `calc(5)` meant.

### 5.4 Real-World Examples

**`String.valueOf(...)`** — The JDK overloads `valueOf` for every primitive type and `char[]`, so `String.valueOf(42)` and `String.valueOf(true)` both work:

```java
String a = String.valueOf(42);     // "42"
String b = String.valueOf(3.14);   // "3.14"
String c = String.valueOf(true);   // "true"
```

*Listing 5.5 — Overloads in `String.valueOf` (fragment).*

**`PrintStream.println(...)`** — `System.out.println` is overloaded for every primitive, `String`, `char[]`, `Object`, and the no-argument version. The `Object` overload is the "catch-all": any type without a more specific overload is converted via `String.valueOf(Object)`.

**Constructor overloading** — Constructors are methods too, and overloading them lets clients build objects at different levels of detail:

```java
public class Person {
    private final String name;
    private final int age;
    private final String email;

    public Person(String name) {
        this(name, 0, "");           // delegate to the full constructor
    }

    public Person(String name, int age) {
        this(name, age, "");
    }

    public Person(String name, int age, String email) { // the "real" constructor
        this.name = name;
        this.age = age;
        this.email = email;
    }
}
```

*Listing 5.6 — Constructor overloading with delegation via `this(...)`.*

> **Real World:** Data Transfer Objects (DTOs) and Builder-pattern classes lean heavily on constructor overloading — a DTO may offer a no-arg constructor for serialization frameworks, a minimal constructor for quick tests, and a full constructor for production use. Frameworks like Jackson and Hibernate reflectively call these overloads, so naming and signature design directly affect library compatibility.

### 5.5 Ambiguity Pitfalls

Overloading is compile-time resolution, and sometimes the compiler simply *cannot* decide — or, worse, silently picks a surprising overload.

**The `null` literal.** `null` is compatible with *every* reference type. Passing it to overloads is ambiguous unless exactly one overload is more specific:

```java
public class Ambiguous {
    static void print(String s)    { System.out.println("String"); }
    static void print(Integer i)   { System.out.println("Integer"); }

    public static void main(String[] args) {
        // print(null);
        // COMPILE ERROR: reference to print is ambiguous —
        // both print(String) and print(Integer) match
        print((String) null);   // cast resolves the ambiguity -> "String"
    }
}
```

*Listing 5.7 — The `null` literal is ambiguous between reference-type overloads. Output: `String`*

**Narrowing vs. boxing.** Java never silently *narrows* a primitive (`double` → `int`) to satisfy an overload, and it prefers widening over boxing:

```java
public class NarrowingDemo {
    static void go(int n)  { System.out.println("int"); }
    static void go(long n) { System.out.println("long"); }

    public static void main(String[] args) {
        byte b = 5;
        go(b);   // widening byte -> int  -> "int" (narrowing to byte is NOT considered)
    }
}
```

*Listing 5.8 — Widening beats boxing and narrowing is never automatic. Output: `int`*

> **Pitfall:** Ambiguity errors are the compiler's *protection*. When a call is genuinely ambiguous, do not try to "fix" it with casts unless you are certain; instead reconsider whether the overloads themselves are well-designed. Two overloads accepting unrelated types that both match `null` are a design smell.

---

## 6. Overridden Methods

Where overloading is about *many methods, one name, chosen at compile time*, overriding is about *one method name, redefined by a subclass, chosen at runtime*. This is the heart of polymorphism.

### 6.1 Definition: Subclass Redefines an Inherited Method

**Overriding** occurs when a subclass declares a method with the same signature as a method in its superclass, *replacing* the inherited behavior for objects of the subclass. When you call the method on a subclass instance through a superclass reference, the subclass's version runs.

```java
public class Animal {
    public void speak() { System.out.println("Some animal sound"); }
}

public class Dog extends Animal {
    @Override
    public void speak() { System.out.println("Woof!"); }
}

public class Cat extends Animal {
    @Override
    public void speak() { System.out.println("Meow!"); }
}

public class PolymorphismDemo {
    public static void main(String[] args) {
        Animal pet = new Dog();
        pet.speak();   // prints "Woof!" — Dog's version runs, chosen at RUNTIME
        Animal other = new Cat();
        other.speak(); // prints "Meow!"
    }
}
```

*Listing 6.1 — The essence of overriding. Output: `Woof!`, `Meow!`*

> **Analogy:** Overriding is like a **child learning a family recipe but making it their own** — same dish name, same basic steps, their own twist. A cookbook reference (`Animal pet`) pointing at a `Dog` object always gets the *dog's* version of `speak`, because the dog knows best what a dog does.

### 6.2 Rules: Same Signature, Covariant Return, No Stricter Access, No New Checked Exceptions

The compiler enforces a precise contract on overrides:

1. **Same signature** — same name and parameter list. (Parameter *names* may differ; types must not.)
2. **Covariant return type** — the return type may be the same *or a subtype* of the parent's return type. `Dog speak()` can return `Dog` where the parent returns `Animal`.
3. **No stricter access** — an override may widen access (`protected` → `public`) but must not narrow it (`public` → `private`).
4. **No new checked exceptions** — an override may throw *fewer or narrower* checked exceptions, never *new or broader* ones. (Unchecked exceptions like `RuntimeException` are always allowed.)

```java
public class Shape {
    public Shape copy() throws CloneNotSupportedException { return new Shape(); }
}

public class Circle extends Shape {
    @Override
    public Circle copy() {        // covariant return: Circle IS-A Shape
        return new Circle();      // no checked exception at all — that's allowed
    }
}
```

*Listing 6.2 — Covariant returns and relaxed exceptions (fragment).*

**Why these rules?** All four protect *callers* who hold a superclass reference. If `Shape` is the compile-time type, the caller expects `copy()` to return a `Shape` (a `Circle` is fine) and expects no exceptions beyond those declared. Rules 3 and 4 ensure a reference typed as `Animal` can call `speak()` as safely as it could on an `Animal`.

### 6.3 The `@Override` Annotation and Why It Is Essential

`@Override` is an **annotation** — a hint to the compiler (and to future readers) that the method intends to override a superclass method. The compiler then *verifies* that an override actually exists; if not, it errors:

```java
public class Dog extends Animal {
    @Override
    public void speaks() { System.out.println("Woof!"); }
    // COMPILE ERROR: method does not override or implement a method from a supertype
}
```

*Listing 6.3 — A typo caught by `@Override` (fragment).*

Without `@Override`, the typo `speaks()` would silently create a new method, the real `speak()` would never be customized, and your program would break at *runtime* in the most confusing way possible. With the annotation, the compiler catches the mistake at compile time.

> **Best Practice:** Annotate *every* overriding method with `@Override`. It costs nothing, documents intent, and converts an entire class of silent bugs into compile-time errors. This is one of the cheapest safety measures in all of Java.

### 6.4 Runtime Polymorphism / Dynamic Dispatch — Foundation of `equals`, `toString`, `hashCode`

**Dynamic dispatch** means the JVM chooses which overridden method to run based on the *actual runtime type* of the object, not the declared type of the reference. This is what makes frameworks and collections work.

Every class inherits `toString`, `equals`, and `hashCode` from `Object`. Overriding them correctly is the foundation of correct collections, logging, and object equality:

```java
public final class Point {
    private final int x;
    private final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;                    // same reference: fast path
        if (!(o instanceof Point p)) return false;     // wrong type (pattern matching)
        return p.x == x && p.y == y;                   // compare all significant fields
    }

    @Override
    public int hashCode() {
        return 31 * x + y;                             // consistent with equals
    }

    @Override
    public String toString() {
        return "Point(" + x + ", " + y + ")";
    }
}
```

*Listing 6.4 — The `equals`/`hashCode`/`toString` contract in action (Java 16+ pattern matching).*

Now the `HashMap` machinery, the `Collections.sort` comparators, and your own debug logs all get correct behavior *automatically*, because they call `equals` and friends polymorphically on `Object` references and the JVM dispatches to `Point`'s versions. The rule: **if two objects are `equals`, they must have the same `hashCode`** — otherwise `HashMap` may fail to find them.

> **Real World:** Overriding `equals` and `hashCode` is precisely how `HashMap` keys work in production. Records (Java 16+) give you correct `equals`/`hashCode`/`toString` for free; for ordinary classes you must write them — and getting them wrong is one of the most common sources of mysterious production bugs.

### 6.5 `super.method()` Calls and Calling Overridden Methods from Constructors

**`super.method()`** lets an overriding method invoke the parent's version, typically to extend rather than replace behavior:

```java
public class SavingsAccount extends Account {
    @Override
    public void deposit(double amount) {
        super.deposit(amount);            // run the base logic first
        System.out.println("Savings bonus credited!"); // then add subclass behavior
    }
}
```

*Listing 6.5 — Extending behavior with `super.deposit()` (fragment).*

**The constructor pitfall.** If a constructor calls a *non-final, overridable* method, the subclass's override runs *before the subclass's fields are initialized*:

```java
public class Base {
    public Base() {
        show();   // dispatches to the MOST-DERIVED override — dangerous!
    }
    public void show() { System.out.println("Base show"); }
}

public class Derived extends Base {
    private final String message = "initialized!";

    public Derived() { super(); } // Base constructor runs first

    @Override
    public void show() {
        System.out.println("Derived show, message = " + message);
    }
}
```

*Listing 6.6 — The constructor-override pitfall.*

`new Derived()` prints `Derived show, message = null` — because at the time `Base()` calls `show()`, `Derived`'s fields have not yet been initialized.

> **Pitfall:** Never call an overridable method from a constructor. It will dispatch to the subclass override on an only-partially-constructed object, and the subclass's fields will be `null`/`0`. This is one of the most infamous subtle bugs in Java — the fix is to make the method `final`, make it `private`, or restructure so constructors call only non-polymorphic code.

### 6.6 Real-World Framework Use: Spring/DAO Overrides and Default Methods

**Spring/DAO pattern.** Service and repository interfaces define contracts; implementations override them. The framework calls your methods polymorphically:

```java
public interface UserRepository {
    Optional<User> findById(long id);
}

public class JdbcUserRepository implements UserRepository {
    @Override
    public Optional<User> findById(long id) {
        // JDBC query code...
        return Optional.of(new User(id, "Ada"));
    }
}

public class UserService {
    public String describeUser(UserRepository repo, long id) {
        return repo.findById(id).map(User::toString).orElse("not found");
    }
}
```

*Listing 6.7 — Interface implementation as overriding (fragment).*

**Interface default methods vs. overrides.** A `default` method in an interface provides a fallback implementation. A class may *override* it — and the class's override always wins over the interface default:

```java
public interface Discountable {
    default double applyDiscount(double price) {
        return price * 0.9;      // default 10% off
    }
}

public class PremiumCustomer implements Discountable {
    @Override
    public double applyDiscount(double price) {
        return price * 0.75;     // premium: 25% off — override wins
    }
}
```

*Listing 6.8 — Overriding an interface default method (fragment).*

> **Real World:** The `Comparator` interface ships many default methods (`thenComparing`, `reversed`) precisely so library users can override or compose comparison logic without breaking existing implementations. The whole Spring ecosystem — `@Controller`, `@Service`, repository interfaces — is built on the runtime dispatch that overriding provides.

---

## 7. Comparing the Method Types

You now hold all five tools. Let us line them up side by side.

### 7.1 Comprehensive Comparison Table

| Method Type | Keyword(s) | Scope | Can Be Overridden? | Can Be Overloaded? | Typical Use Case |
|---|---|---|---|---|---|
| Static method | `static` | Class (no instance needed) | No (hidden/compile-time) | Yes | Utility methods (`Math.max`), factories (`LocalDate.of`), `main` |
| Final method | `final` | Instance (of declaring class) | No (compiler forbids it) | Yes | Template method skeleton, security/invariant-critical behavior |
| Private method | `private` | Within the declaring class only | No (invisible to subclasses) | Yes | Internal helpers, encapsulation, DRY inside a class |
| Overloaded method | none (same name, different params) | Same class | Yes | — (this *is* overloading) | `println(...)`, `String.valueOf(...)`, constructors |
| Overridden method | none (`@Override` annotation) | Subclass of the declaring class | — (this *is* overriding) | Yes | Polymorphic behavior, `equals`/`toString`, framework hooks |

*Table 7.1 — Comprehensive comparison of the five method types.*

### 7.2 When to Choose Each Method Type — Decision Guidance

Start with a single question: **does this behavior depend on the object's state?**

- If **no** — the result is a pure function of the arguments, or a class-level operation — prefer **`static`**. Think `Math.max`, `Collections.sort`, factory methods.
- If **yes** — the behavior needs instance fields — use an **instance method**. Then ask two more questions:
  - **Should subclasses be allowed to change it?** If the behavior must be invariant for correctness or security, make it **`final`**. If it is part of an algorithm skeleton others extend, keep it non-final and `protected` for hooks.
  - **Should anyone outside the class call it?** If only the class needs it, make it **`private`**. If only subclasses need it, use `protected`.
- **Overloading** is a readability decision: use the same name for the same *concept* at different parameter shapes. **Overriding** is a design decision: use it to customize inherited behavior polymorphically.

**API design and maintainability.** These five choices are the primary vocabulary of your public API. `static` tells callers "no state, safe to call anywhere"; `final` tells them "this contract is sealed"; `private` tells them "this is not your business — I can change it anytime." Overloads shape how natural your API reads at the call site, and overrides determine how much future behavior you (or framework users) can adapt. In production codebases, a well-chosen method type *is* the documentation: it encodes intent that no comment could express as cheaply. Getting these decisions wrong — making everything `public` by default, sealing methods that must be extensible, hiding hooks that frameworks need — produces code that is hard to evolve and expensive to maintain.

### 7.3 How These Types Interact

**Static + final.** Legal and meaningful: `public static final int MAX = 100;` is a constant; `public static final void util()` fixes a utility's behavior forever (though since static methods are never overridden, `final` mainly documents intent).

**Private + final.** A private method *cannot* be overridden (it is invisible), so `final` is redundant — but harmless. The compiler accepts `private final void helper()`, and many style guides allow it for clarity.

**Static vs. instance overriding.** Static methods **hide**, never override. If a subclass declares a static method with the same signature as the parent's, the method is *hidden*, and which one runs is decided by the *compile-time type of the reference*:

```java
public class SuperStat {
    public static void who() { System.out.println("Super"); }
}
public class SubStat extends SuperStat {
    public static void who() { System.out.println("Sub"); }
}

public class StaticHideDemo {
    public static void main(String[] args) {
        SuperStat.who();              // "Super" — resolved at compile time
        SubStat.who();                // "Sub"
        SuperStat s = new SubStat();
        s.who();                      // "Super"! NOT polymorphic — static methods hide
    }
}
```

*Listing 7.1 — Static methods are hidden, not overridden. Output: `Super`, `Sub`, `Super`*

> **Pitfall:** Because static methods bind at compile time, never call a hidden static method through an instance reference or a superclass variable — you will get the parent's version regardless of the actual object. If you need polymorphism, the methods must be instance (non-static) methods.

---

## 8. Common Mistakes and Best Practices

### 8.1 Common Mistakes and Their Corrections

1. **Calling static methods through instance references.**
   *Mistake:* `counter.incrementTotal();` where `incrementTotal` is static.
   *Correction:* `Counter.incrementTotal();` — call via the class name; it documents that no instance is involved.

2. **Accessing instance fields from a static method.**
   *Mistake:* Reading `this.value` inside a `static` method.
   *Correction:* Make the field static if it is genuinely class-level, or pass an instance as a parameter.

3. **Trying to override a `final` method.**
   *Mistake:* Re-declaring a `final` method in a subclass.
   *Correction:* Use composition or ask the author for a different hook point; a `final` method is a deliberate contract.

4. **Forgetting `@Override`.**
   *Mistake:* A subclass method with a typo — `speaks()` instead of `speak()` — silently not overriding anything.
   *Correction:* Annotate every override; the compiler then catches signature mistakes at compile time.

5. **Narrowing access when overriding.**
   *Mistake:* Overriding a `public` method with a `protected` one.
   *Correction:* Widen (or keep) access; a subclass can never be *less* accessible than its parent.

6. **Breaking the `equals`/`hashCode` contract.**
   *Mistake:* Overriding `equals` but not `hashCode`, or using mutable fields in `hashCode`.
   *Correction:* Override both consistently and use only immutable, significant fields; `equals` objects must share a `hashCode`.

7. **Calling overridable methods from constructors.**
   *Mistake:* A base constructor invoking a non-final method that subclasses override.
   *Correction:* Make such methods `final` or `private`, or refactor to avoid polymorphic calls during construction.

8. **Designing ambiguous overloads.**
   *Mistake:* Overloads like `print(String)` and `print(Integer)` where `print(null)` is ambiguous.
   *Correction:* Avoid overloads with unrelated reference types that both accept `null`; if unavoidable, cast at the call site deliberately.

### 8.2 Best-Practices Checklist

- [ ] Prefer `static` only for behavior independent of instance state; name utility classes with a plural noun and give them a `private` constructor.
- [ ] Use `@Override` on every overridden method — always.
- [ ] Make access as *restrictive as possible*: `private` by default for helpers, `protected` only for subclass hooks, `public` only for the real API.
- [ ] Mark methods `final` only when behavior must be invariant; do not seal everything out of habit.
- [ ] In overrides, never narrow access and never add checked exceptions.
- [ ] Keep the `equals`/`hashCode`/`toString` contract consistent, using immutable fields.
- [ ] Never call overridable methods from constructors.
- [ ] For overloads, keep each overload's behavior semantically consistent — callers expect the same *idea* under one name.
- [ ] Use `this(...)` to delegate between overloaded constructors and avoid duplication.
- [ ] Document intent: a comment like `// static: no state, pure function` near non-obvious choices is cheap and valuable.

---

## 9. Practice Exercises

### 9.1 The Exercises

**Exercise 1 — Static or Instance? (Beginner)**
For each method, decide whether it should be `static` or instance, and justify:
(a) `double computeArea(double radius)` on a `Circle`;
(b) `void setName(String name)` on a `Student`;
(c) `String toUpper(String input)` on a `StringTools` class;
(d) `LocalDate today()` on a `DateFactory`.
*Hint:* Ask: does the method need to read or write fields of the receiving object?

**Exercise 2 — Final Method Error (Beginner)**
Given `class Device { public final void powerOn() { } }`, write a subclass `Phone extends Device` that tries to override `powerOn()`. Compile it, read the error, then fix it by removing the override and instead calling `powerOn()` from a new non-conflicting method.
*Hint:* The compiler message will mention "overridden method is final."

**Exercise 3 — Private Helper Refactor (Intermediate)**
Write a class `LoanCalculator` with a public method `monthlyPayment(principal, rate, years)` and a second public method `totalInterest(principal, rate, years)`. Both need to compute the monthly rate and the number of payments. Extract the shared calculations into `private` helper methods and show that the public methods stay correct.
*Hint:* `monthlyRate = rate / 1200.0` and `months = years * 12`.

**Exercise 4 — Overload Puzzle (Intermediate)**
Given these overloads — `void play(int n)`, `void play(long n)`, `void play(Integer n)`, `void play(int... ns)` — determine the output (or error) of: `play(5)`, `play(5L)`, `play(5, 6)`, and `play((Integer) 5)`. Then write the program and verify.
*Hint:* Apply the resolution order: exact → widening → boxing → varargs.

**Exercise 5 — Polymorphic Override + Contract (Intermediate)**
Create `abstract class Shape` with `abstract double area()` and an overridable `String description()`. Implement `Circle` and `Square`. In `main`, store both in a `Shape[]`, loop and print `description()` and `area()`. Confirm the runtime type determines which `description` runs.
*Hint:* You need exactly one `area()` implementation per subclass — the abstract method *forces* overriding.

**Exercise 6 — The Constructor Trap (Advanced)**
Build a class `Base` whose constructor calls a public non-final method `init()`, and a subclass `Derived` that overrides `init()` and sets a field. Run the program and observe the `null`/default output. Then fix it using one of: making `init()` final, making it private, or moving the call out of the constructor. Explain which fix you chose and why.
*Hint:* Recall Section 6.5 — field initializers of the subclass run *after* the superclass constructor.

**Exercise 7 — Design a small service API (Advanced)**
Design a class `PaymentService` with: a `public` method `process(Payment p)`; a `private` method `validate(Payment p)` reused by `process`; an overloaded `process(Payment p, boolean notify)`; a `final` method `getCurrency()`. Then subclass it to add a `BonusPaymentService` that overrides `process` only if possible — and document why any restriction you hit exists.
*Hint:* Review Section 7.1 before choosing the modifiers; some choices are mutually incompatible with overriding.

### 9.2 Reference Solutions

**Solution 1 — Static or Instance?**

```java
public class Circle {
    private final double radius;
    public Circle(double radius) { this.radius = radius; }

    public double computeArea() {          // INSTANCE: needs the radius field
        return Math.PI * radius * radius;
    }
}

public class Student {
    private String name;
    public void setName(String name) { this.name = name; } // INSTANCE: writes a field
}

public class StringTools {
    private StringTools() { }                               // prevent instantiation
    public static String toUpper(String input) {            // STATIC: pure function
        return input.toUpperCase();
    }
}

public class DateFactory {
    public static java.time.LocalDate today() {             // STATIC: no state needed
        return java.time.LocalDate.now();
    }
}
```

**Solution 2 — Final Method Error**

```java
public class Device {
    public final void powerOn() {
        System.out.println("Device powered on");
    }
}

public class Phone extends Device {
    // @Override
    // public void powerOn() { }   // COMPILE ERROR: cannot override final method

    public void powerOnWithBell() { // new, non-conflicting method
        powerOn();                  // calls the inherited final method
        System.out.println("Ring ring!");
    }

    public static void main(String[] args) {
        new Phone().powerOnWithBell();
    }
}
// Output: Device powered on
//         Ring ring!
```

**Solution 3 — Private Helper Refactor**

```java
public class LoanCalculator {
    public double monthlyPayment(double principal, double annualRate, int years) {
        double r = monthlyRate(annualRate);        // private helper
        int n = months(years);                     // private helper
        if (r == 0) return principal / n;
        double factor = Math.pow(1 + r, n);
        return principal * r * factor / (factor - 1);
    }

    public double totalInterest(double principal, double annualRate, int years) {
        return monthlyPayment(principal, annualRate, years) * months(years) - principal;
    }

    private double monthlyRate(double annualRate) { return annualRate / 1200.0; } // / 100 / 12
    private int months(int years)                   { return years * 12; }

    public static void main(String[] args) {
        LoanCalculator lc = new LoanCalculator();
        System.out.printf("Payment: %.2f%n", lc.monthlyPayment(200_000, 5.0, 30));
        System.out.printf("Interest: %.2f%n", lc.totalInterest(200_000, 5.0, 30));
    }
}
// Output (illustrative):
// Payment: 1073.64
// Interest: 186511.57
```

**Solution 4 — Overload Puzzle**

```java
public class OverloadPuzzle {
    static void play(int n)         { System.out.println("int"); }
    static void play(long n)        { System.out.println("long"); }
    static void play(Integer n)     { System.out.println("Integer"); }
    static void play(int... ns)     { System.out.println("varargs"); }

    public static void main(String[] args) {
        play(5);            // exact match            -> int
        play(5L);           // exact match            -> long
        play(5, 6);         // no 2-int overload; fits varargs -> varargs
        play((Integer) 5);  // exact match            -> Integer
    }
}
// Output: int
//         long
//         varargs
//         Integer
```

**Solution 5 — Polymorphic Override**

```java
public abstract class Shape {
    public abstract double area();                 // forces every subclass to override

    public String description() {                  // overridable, defaults provided
        return "A generic shape";
    }
}

public class Circle extends Shape {
    private final double r;
    public Circle(double r) { this.r = r; }

    @Override public double area() { return Math.PI * r * r; }

    @Override public String description() { return "A circle with radius " + r; }
}

public class Square extends Shape {
    private final double side;
    public Square(double side) { this.side = side; }

    @Override public double area() { return side * side; }

    @Override public String description() { return "A square with side " + side; }
}

public class ShapeDemo {
    public static void main(String[] args) {
        Shape[] shapes = { new Circle(2), new Square(3) };
        for (Shape s : shapes) {
            System.out.println(s.description() + " -> area " + s.area());
        }
    }
}
// Output:
// A circle with radius 2.0 -> area 12.566370614359172
// A square with side 3.0 -> area 9.0
```

**Solution 6 — The Constructor Trap**

```java
public class Base {
    public Base() {
        init();               // dispatches to Derived.init() BEFORE Derived fields exist
    }
    public void init() { System.out.println("Base init"); }
}

public class Derived extends Base {
    private final String tag = "ready";

    public Derived() { }      // implicit super() runs Base's constructor first

    @Override public void init() {
        System.out.println("Derived init, tag = " + tag); // tag is still null here!
    }

    public static void main(String[] args) {
        new Derived();
    }
}
// Output: Derived init, tag = null   <-- the trap

// FIX (chosen: make the hook non-polymorphic by making it private):
public class BaseFixed {
    public BaseFixed() {
        doInit();   // private -> NOT polymorphic -> always BaseFixed.doInit
    }
    private void doInit() { System.out.println("Base init"); }
}

public class DerivedFixed extends BaseFixed {
    private final String tag = "ready";
    private void doInit() { System.out.println("Derived init, tag = " + tag); }
    // This doInit() is an unrelated private method — it will NEVER be called from BaseFixed's constructor.
    public static void main(String[] args) {
        new DerivedFixed(); // Output: Base init
    }
}
```

**Solution 7 — Small Service API**

```java
public class PaymentService {
    public void process(Payment p) {
        validate(p);                 // private helper reused everywhere
        System.out.println("Processing " + p.amount() + " " + getCurrency());
    }

    public void process(Payment p, boolean notify) {   // overloaded version
        process(p);                                    // delegate to the base logic
        if (notify) System.out.println("Customer notified");
    }

    private void validate(Payment p) {                 // private: implementation detail
        if (p.amount() <= 0) throw new IllegalArgumentException("Amount must be positive");
    }

    public final String getCurrency() { return "USD"; } // final: currency is invariant

    public static void main(String[] args) {
        PaymentService svc = new PaymentService();
        svc.process(new Payment(99.99), true);
    }
}

class Payment {
    private final double amount;
    Payment(double amount) { this.amount = amount; }
    double amount() { return amount; }
}
// Output: Processing 99.99 USD
//         Customer notified

// Why subclassing process() works: process is PUBLIC and NON-FINAL, so a subclass may override it.
// Why getCurrency() is safe: FINAL means no subclass can silently switch currencies —
//   a deliberate design choice protecting financial invariants.
// Why validate() stays private: no one outside this class should bypass validation.
```

---

## 10. Key Takeaways and Summary

**Key concepts in one place:**

- **Static methods** belong to the class, run without an instance, and are resolved at *compile time* — perfect for utilities, factories, and the `main` entry point.
- **Final methods** cannot be overridden — they lock down invariants, template skeletons, and security-critical behavior.
- **Private methods** are visible only within their class — they power encapsulation, implementation hiding, and DRY refactoring.
- **Overloaded methods** share a name but differ in parameters; resolution follows *exact → widening → boxing → varargs* and happens at compile time.
- **Overridden methods** let subclasses replace inherited behavior; resolution happens at *runtime* via dynamic dispatch, enabling polymorphism.
- **`@Override`** turns silent override mistakes into compile-time errors — always use it.
- **Static methods hide, never override** — polymorphism requires instance methods.
- **Private methods are shadowed, never overridden** — a subclass's same-signature method is unrelated.
- **Never call overridable methods from constructors** — the subclass override runs on a half-built object.
- The `equals`/`hashCode`/`toString` contract depends entirely on correct overriding and consistent implementations.

**Glossary:**

| Term | Definition |
|---|---|
| Method | A named block of statements that can be invoked to perform an action or produce a result. |
| Method signature | The method's name plus its parameter list — its identity for overloading and overriding. |
| Static method | A method declared with `static` that belongs to the class and needs no instance. |
| Instance method | A method that belongs to an object and may access its instance fields. |
| Final method | A method declared with `final` that cannot be overridden by subclasses. |
| Private method | A method declared with `private`, callable only within the declaring class. |
| Overloading | Declaring multiple methods with the same name but different parameter lists in one class. |
| Overriding | A subclass redefining a method inherited from a superclass, with the same signature. |
| Dynamic dispatch | The runtime selection of an overridden method based on the object's actual type. |
| Covariant return type | An override returning a subtype of the parent method's return type. |
| Encapsulation | Hiding implementation details behind a stable public interface, often via private methods. |
| `@Override` | An annotation that makes the compiler verify a method truly overrides a supertype method. |

*Table 10.1 — Glossary of key terms.*

You have now met the five method types that structure every Java codebase: **static** methods for class-level logic, **final** methods for unbreakable contracts, **private** methods for hidden implementation, **overloaded** methods for readable APIs, and **overridden** methods for polymorphic behavior. Master the *when*, not just the *how*, and every class you design will communicate its intent through its method types alone.

---

# Chapter: `static` vs `instance` Members in Java

## 1. Introduction

Imagine you are writing software for a small school. You need to register every student with their own name, grade, and locker number — but you *also* need to know how many students have enrolled today. The per-student facts (name, grade) are obviously different for every student. The count, however, is a school-wide fact: it is the same number no matter which student you ask. These are two fundamentally different kinds of data, and Java models them with two different kinds of members: **instance members** and **static members**.

This chapter is a deep, beginner-friendly dive into one of the most misunderstood topics in Java. We will cover:

- What classes, objects, and memory actually are;
- **Instance fields** and **instance methods** — data and behavior owned by each object;
- **Static fields** and **static methods** — data and behavior owned by the class itself;
- How `this` interacts with each world;
- A practical decision guide for *when to use which*;
- Real-world examples, common pitfalls, interview questions, and exercises.

"Static vs instance" trips up developers for years because the distinction is about *ownership* and *memory*, not about syntax. The moment you understand *who owns the data*, the whole topic clicks into place. By the end of this chapter, you will be able to look at any Java class and instantly classify every member as static or instance — and, more importantly, explain *why*.

---

## 2. Core Concepts: What Are Classes, Objects, and Memory?

Before we can talk about static vs instance, we need two definitions nailed down.

- A **class** is a *blueprint*. It describes *what kinds of data* exist (fields) and *what behaviors* are possible (methods).
- An **object** (also called an **instance**) is a *concrete realization* built from that blueprint. A class defines the structure; `new` builds actual objects that live in memory.

**Analogy: cookie cutters and cookies.** A cookie cutter is the *class*: it defines the shape, but you cannot eat it. Each cookie pressed out of the cutter is an *object*: it has the same shape as every other cookie, but it is a separate physical thing. One cutter can produce a thousand cookies, each independent. The same is true in Java: `new Student(...)` can produce a thousand `Student` objects, each with its own data.

Here is a minimal class skeleton with **no** static members yet — just a blueprint for a student:

```java
public class Student {
    // Instance fields: each Student object gets its OWN copies of these
    private String name;
    private int grade;

    // Constructor: runs when an object is created with `new`
    public Student(String name, int grade) {
        this.name = name;
        this.grade = grade;
    }

    // Instance method: operates on one particular Student object
    public void display() {
        System.out.println(name + " is in grade " + grade);
    }
}
```

Every `new Student(...)` call allocates a fresh chunk of memory on the **heap** (Java's big object-storage area). The fields `name` and `grade` inside that chunk belong *only* to that one object. This is the heart of the instance world.

---

## 3. Instance Fields

### Definition

An **instance field** is a variable declared inside a class *without* the `static` keyword. It belongs to each **individual object**. Every object created from the class gets its **own copy** of every instance field, with its own value.

### Key Properties

- Declared without `static`: `private String name;`
- Each `new` object receives its own copy — two objects can hold different values simultaneously.
- Accessed through a **reference** to an object (`alice.name`), not through the class name.
- Initialized when the object is constructed (via the constructor, field initializer, or default value).
- Lives inside the object on the **heap**.

**Analogy: everyone's name is their own.** In a classroom, every student has their own name, their own backpack, their own phone number. Alice's phone number is not Bob's phone number — they just happen to have *fields with the same shape*. An instance field is exactly that: a slot for a value that every object possesses, but that holds that object's personal data.

### Code Example

```java
public class Student {
    // Instance fields
    private String name;
    private int grade;

    public Student(String name, int grade) {
        this.name = name;
        this.grade = grade;
    }

    // Instance method: reads THIS object's fields
    public void display() {
        System.out.println(name + " is in grade " + grade);
    }

    public static void main(String[] args) {
        // Two separate objects, each with its OWN copies of name and grade
        Student alice = new Student("Alice", 10);
        Student bob   = new Student("Bob", 11);

        alice.display();   // prints Alice's data
        bob.display();     // prints Bob's data

        // They are independent objects, not the same object
        System.out.println("alice == bob? " + (alice == bob));   // false
    }
}
```

Expected output:

```
Alice is in grade 10
Bob is in grade 11
alice == bob? false
```

### Memory Diagram

On the heap, each object is a self-contained box. Changing `alice.grade` can never affect `bob.grade`, because they live in different boxes:

```
        HEAP (objects live here)                    STACK (references live here)

┌──────────────────────────────────┐
│   Student object #1              │        ┌─────────┐
│   ┌──────────────────────────┐   │        │  alice  │──┐
│   │ name: "Alice"            │   │        └─────────┘  │
│   │ grade: 10                │   │                     └──────▶┐
│   └──────────────────────────┘   │                              │
│                                  │                              ▼
│   Student object #2              │        ┌─────────┐   ┌──────────────────────┐
│   ┌──────────────────────────┐   │        │   bob   │──▶│ name: "Bob"          │
│   │ name: "Bob"              │   │        └─────────┘   │ grade: 11            │
│   │ grade: 11                │   │                     └──────────────────────┘
│   └──────────────────────────┘   │
└──────────────────────────────────┘
```

The variable `alice` does not *contain* the object; it *points to* it. There are two boxes, two copies of `name`, two copies of `grade`. That is the defining trait of instance fields.

---

## 4. Static Fields (Class Variables)

### Definition

A **static field** (or **class variable**) is a field declared with the `static` keyword. It belongs to the **class itself**, not to any object. No matter how many objects you create — zero, one, or a million — there is **exactly one** copy of a static field in the entire program.

### Key Properties

- Declared with `static`: `private static int studentCount;`
- **One copy exists total**, shared by all instances (and usable even when no instance exists).
- Accessible via the class name: `Student.studentCount`.
- Technically also reachable through an instance reference (`someStudent.studentCount`), but this is **discouraged** — it works, yet it misleads readers into thinking the value belongs to the object. Modern IDEs warn about it.
- Initialized when the class is first loaded, and it lives in the class's metadata area (not inside any object).

**Analogy: the classroom whiteboard vs. personal notebooks.** Each student's *notebook* is private — Bob cannot read Alice's notes. But the *whiteboard* at the front of the room is shared: everyone sees the same words, and when one person erases it, it is gone for everyone. A static field is that whiteboard. It belongs to the class (the classroom), not to any single student.

### Code Example

```java
public class Student {
    // Instance fields: one per object
    private String name;
    private int grade;

    // STATIC field: ONE copy for the entire program, shared by all students
    private static int studentCount = 0;

    public Student(String name, int grade) {
        this.name = name;
        this.grade = grade;
        studentCount++;                    // every new student bumps the shared counter
    }

    public void display() {
        System.out.println(name + " is in grade " + grade);
    }

    public static void main(String[] args) {
        Student alice = new Student("Alice", 10);
        Student bob   = new Student("Bob", 11);
        Student carol = new Student("Carol", 9);

        // Access the shared value through the CLASS name (best practice)
        System.out.println("Total students created: " + Student.studentCount);

        // Discouraged but legal — works, yet suggests the value is per-object
        System.out.println("Via an instance: " + alice.studentCount);
    }
}
```

Expected output:

```
Total students created: 3
Via an instance: 3
```

Notice that `alice.studentCount`, `bob.studentCount`, and `Student.studentCount` all print `3` — because they are all the *same single slot of memory*.

### Instance vs Static Fields — Comparison Table

| Aspect | Instance Field | Static Field |
|---|---|---|
| Declared with | `private String name;` | `private static int count;` |
| Owned by | Each individual object | The class itself |
| Number of copies | One per object (`new`) | Exactly one total |
| Memory location | Inside each object on the heap | Class metadata / method area (one shared slot) |
| Access syntax | `object.field` | `ClassName.field` (recommended) |
| Accessible before any object exists? | No | Yes |
| Initialization timing | When each object is constructed | When the class is first loaded |
| Default value | Depends on the object's constructor | Depends on the class's initializer |

---

## 5. Instance Methods

### Definition

An **instance method** is a method declared *without* `static`. It is a behavior that operates on **one specific object's data**. Because it runs on behalf of an object, it can read and write that object's instance fields.

### Key Properties

- Can access everything: the object's instance fields, other instance methods, and also static fields and static methods.
- Implicitly receives a hidden reference called `this`, which points to the object the method was called on.
- Must be invoked on an **object reference**: `alice.getGrade()`. You cannot call it via the class name (`Student.getGrade()` will not compile).
- Each call is *bound* to a specific object — two objects calling the same method use that method with their own data.

**Analogy: the "start engine" button.** Every car has a start button, but pressing it starts *that* car's engine, using *that* car's fuel gauge and ignition state. The *procedure* is the same for all cars (that's the method), but it always acts on the specific car whose button you pressed (that's the object).

### Code Example

```java
public class Student {
    private String name;
    private int grade;

    public Student(String name, int grade) {
        this.name = name;
        this.grade = grade;
    }

    // Instance method: reads THIS object's grade
    public int getGrade() {
        return grade;                    // implicitly: return this.grade;
    }

    // Instance method: modifies THIS object's grade
    public void updateGrade(int newGrade) {
        if (newGrade >= 1 && newGrade <= 12) {
            this.grade = newGrade;       // 'this' makes the assignment unambiguous
        } else {
            System.out.println("Invalid grade: " + newGrade);
        }
    }

    public void display() {
        System.out.println(name + " is in grade " + grade);
    }

    public static void main(String[] args) {
        Student alice = new Student("Alice", 10);
        Student bob   = new Student("Bob", 11);

        System.out.println("Alice's grade: " + alice.getGrade());
        alice.updateGrade(11);           // affects ONLY alice
        alice.display();
        bob.display();                   // bob is untouched
    }
}
```

Expected output:

```
Alice's grade: 10
Alice is in grade 11
Bob is in grade 11
```

`updateGrade(11)` changed Alice's grade from 10 to 11, while Bob's remained 11 (he was already there, by coincidence). The key point: the method operated on *Alice's* copy of the field — never on Bob's.

---

## 6. Static Methods (Class Methods)

### Definition

A **static method** is a method declared with the `static` keyword. It belongs to the **class**, not to any object. It is a piece of behavior that does not need an object to run — it has no `this` reference and cannot touch any object's instance state.

### Key Properties

- Declared with `static`: `public static int max(int a, int b)`.
- Called via the class name: `Math.max(3, 7)`, `Student.printTotalStudents()`.
- **Cannot** access instance fields or call instance methods directly — there is no object, hence no `this`, hence nothing to read instance data from.
- **Can** access static fields and call other static methods.
- Called on an instance (`someStudent.printTotalStudents()`) compiles but is **bad practice** — it implies object-specific behavior where there is none.

**Analogy: a shared utility in the school workshop.** A bench-mounted saw in a workshop is shared by everyone: you walk up to it and use it; it has no idea *which* student you are, and it does not need to know. `Math.max(...)` is exactly that — a tool you use without creating any object. A personal drill, in contrast, is tuned to its owner — that's an instance method, because it depends on the object's state.

### Code Example

```java
public class Student {
    private String name;
    private int grade;
    private static int studentCount = 0;

    public Student(String name, int grade) {
        this.name = name;
        this.grade = grade;
        studentCount++;
    }

    // STATIC method: reports on the class-wide counter, no object needed
    public static void printTotalStudents() {
        System.out.println("Total students: " + studentCount);
    }

    // STATIC utility method: pure function of its arguments
    public static String formatName(String name) {
        if (name == null || name.isBlank()) {
            return "Unknown";
        }
        return name.trim();
    }

    public static void main(String[] args) {
        Student alice = new Student("Alice", 10);
        Student bob   = new Student("Bob", 11);

        // Static methods are called on the CLASS, not on an object
        Student.printTotalStudents();

        String clean = Student.formatName("   Carol   ");
        System.out.println("Formatted: '" + clean + "'");
    }
}
```

Expected output:

```
Total students: 2
Formatted: 'Carol'
```

`printTotalStudents()` reads only the static field `studentCount`; `formatName()` needs no object state at all — it is a pure function of its parameter. Neither requires an object.

### What Will NOT Compile

A static method has no object to work with. Trying to touch an instance field or call an instance method from a static context is a compile-time error:

```java
public class Student {
    private String name;               // instance field
    private static int studentCount = 0;

    // STATIC method attempting to use instance state — DOES NOT COMPILE
    public static void printName() {
        // COMPILE ERROR: "Cannot make a static reference to the
        // non-static field 'name'"
        System.out.println(name);

        // COMPILE ERROR: "Cannot make a static reference to the
        // non-static method 'display()'"
        display();
    }

    // Even calling an instance method through a local variable is not
    // allowed here, because no Student object exists in this scope:
    public static void tryInstanceAccess() {
        Student s = new Student("Alice", 10);
        // This IS allowed — 's' names a real object:
        s.display();
    }

    public void display() {
        System.out.println(name);
    }
}
```

The compiler rejects `name` and `display()` inside `printName()` because at that point there is *no object* — the static method could be called as `Student.printName()` before any `Student` ever exists. The mental model: **static methods stand outside the objects; instance members live inside them.**

---

## 7. `this` and `static` Interaction

Every **instance** method and constructor receives an implicit reference called `this` — a pointer to the object on which the call was made. **Static** contexts have **no `this`**, because a static member is not invoked "on" any object. There is nothing for `this` to point at.

This single fact explains every rule in this chapter:

- Static methods can't read instance fields → no object → no `this`.
- Static methods can't call instance methods → no receiver object.
- Instance methods *can* access static members → `this` is present, and the class's shared data is always reachable.

### Field Shadowing

`this` also resolves naming collisions. When a constructor parameter has the same name as a field, the parameter **shadows** the field, and `this.field` is the only way to reach the field.

### Code Example: Both in Action

```java
public class BankAccount {
    private String owner;      // instance field
    private double balance;    // instance field

    private static int accountsOpened = 0;   // static field

    // Constructor: 'this' distinguishes fields from parameters (shadowing)
    public BankAccount(String owner, double balance) {
        this.owner = owner;
        this.balance = balance;
        accountsOpened++;
    }

    // Instance method: 'this' is implicit but can be written explicitly
    public void deposit(double amount) {
        this.balance += amount;
    }

    public void printOwner() {
        // 'owner' here is shorthand for 'this.owner'
        System.out.println("Account owned by " + owner);
    }

    // Static method: NO 'this' exists here
    public static void printAccountsOpened() {
        System.out.println("Accounts opened: " + accountsOpened);
        // The following would NOT compile — no 'this' in a static context:
        // System.out.println(owner);
        // printOwner();
    }

    public static void main(String[] args) {
        BankAccount a = new BankAccount("Maria", 1000);
        a.deposit(250);        // call instance method ON an object
        a.printOwner();

        BankAccount.printAccountsOpened();   // call static method ON the class

        // In static main(), you cannot say: printOwner();
        // You must name an object: a.printOwner();
    }
}
```

Expected output:

```
Account owned by Maria
Accounts opened: 1
```

**Summary:** `this` is the object's way of referring to itself. Static members don't belong to any object, so they simply have no `this`.

---

## 8. When to Use Which (Decision Guide)

Every design decision about static vs instance reduces to one question: **whose data is this?**

### Decision Table

| Situation | Use Instance | Use Static | Reason |
|---|---|---|---|
| State that varies per object (name, balance, ID) | ✅ Yes | ❌ No | Each object must carry its own value |
| Shared counter / total across all objects | ❌ No | ✅ Yes | One value must be visible to every object |
| Utility/helper functions (`Math.max`, formatting) | ❌ No | ✅ Yes | Pure logic, no object state required |
| Constants identical for every object (`TAX_RATE`) | ❌ No | ✅ Yes | `static final` — one unchangeable copy |
| Factory methods (`Integer.valueOf(...)`) | ❌ No | ✅ Yes | Produces objects but is not an object's behavior |
| A value that must stay the same across all objects | ❌ No | ✅ Yes | Sharing is the whole point |
| Behavior that reads/writes an object's fields | ✅ Yes | ❌ No | Needs `this` to reach the data |
| Behavior independent of any object | ❌ No | ✅ Yes | No state to bind to |

### Rules of Thumb

- **If each object needs its own value → instance field.** A student's name, an order's amount.
- **If the value is shared by all objects or belongs to the class → static field.** A running count of orders, a global tax rate.
- **If the method reads or writes instance state → instance method.** `calculateTotal()`, `updateGrade(...)`.
- **If the method needs no object state → static method.** `formatName(...)`, `Math.max(...)`.

### Why Is `main` Static?

A Java program starts when the JVM runs `main`. At that instant, **no object of your class exists yet** — the JVM cannot call a non-static `main` on an object it hasn't created. So `main` must be static: it is the entry point that runs before (and then constructs) any objects:

```java
public class App {
    public static void main(String[] args) {
        // No App object exists here. That is exactly why main is static.
        System.out.println("Program starting...");
    }
}
```

### Constants: `static final`

Constants that must be identical for every object are written as `static final` fields in **UPPER_SNAKE_CASE**:

```java
public class Order {
    public static final double TAX_RATE = 0.08;   // shared, unchangeable
    public static final int MAX_ITEMS = 50;
}
```

`final` makes the reference unchangeable; `static` makes it a single shared copy. Together: one shared, read-only value for the whole program.

---

## 9. Real-World Context and Use Cases

The static/instance distinction isn't a classroom curiosity — it powers large Java systems every day.

- **`Math.PI` and `Math.max(...)`** — pure static utility. The `Math` class is never instantiated; you cannot create a `Math` object. Everything is `static` because none of it needs per-object state.

- **The Singleton pattern** — a class that allows exactly one instance uses a `static` field to hold that instance plus a private constructor:

```java
public class Config {
    private static Config instance;          // the single shared instance

    private Config() {}                      // nobody can call `new Config()`

    public static Config getInstance() {     // static: reachable without an object
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }
}
```

- **Object counting / session counters in web apps.** Every time a user logs in, the application increments a shared `static int activeSessions`. Each session object itself is instance data (per user), but the *total* is class-wide.

- **Configuration constants shared app-wide.** Database URLs, feature flags, and default timeouts are `public static final` constants so every component reads the same value from one place.

- **Database connection pools / shared caches.** A connection pool holds a shared set of pooled connections in a static (or singleton-managed) structure. Each *connection* is an instance; the *pool* is shared.

### Complete Runnable Case Study: An `Order` System

This program uses all four member types together: a static shared counter, a static constant, instance fields, an instance method, and a static method.

```java
public class Order {

    // ---- STATIC members (owned by the class) ----
    private static int totalOrders = 0;             // shared counter
    public static final double TAX_RATE = 0.08;     // shared constant

    // ---- INSTANCE members (owned by each order) ----
    private final int orderId;
    private double amount;

    public Order(double amount) {
        this.orderId = ++totalOrders;   // unique ID from the shared counter
        this.amount = amount;
    }

    // Instance method: needs this order's data
    public double calculateTotal() {
        return amount + amount * TAX_RATE;
    }

    // Instance method: prints this order
    public void display() {
        System.out.printf("Order #%d: subtotal $%.2f, total $%.2f%n",
                orderId, amount, calculateTotal());
    }

    // Static methods: no object state needed
    public static int getTotalOrders() {
        return totalOrders;
    }

    public static void resetDailyCount() {
        totalOrders = 0;
    }

    public static void main(String[] args) {
        Order o1 = new Order(100.0);
        Order o2 = new Order(250.0);

        o1.display();
        o2.display();

        System.out.println("Total orders so far: " + Order.getTotalOrders());
        System.out.println("Tax rate: " + Order.TAX_RATE);

        Order.resetDailyCount();
        System.out.println("After reset: " + Order.getTotalOrders());
    }
}
```

Expected console output:

```
Order #1: subtotal $100.00, total $108.00
Order #2: subtotal $250.00, total $270.00
Total orders so far: 2
Tax rate: 0.08
After reset: 0
```

Notice how the pieces fit: each `Order` object carries its own `amount` and gets a unique `orderId`; the class tracks the running total in `totalOrders`; `TAX_RATE` is one shared constant; `calculateTotal()` is instance (it reads `this.amount`); `resetDailyCount()` is static (it mutates only the shared counter).

---

## 10. Common Pitfalls and Interview Questions

### Common Mistakes

1. **Accessing an instance member from a static context.** The most common error: `System.out.println(name)` inside a `static` method. The compiler rejects it — there is no object, so there is no `name`.
2. **Calling static methods through an instance reference.** `someOrder.resetDailyCount()` compiles, but it *lies* about what is happening and confuses maintainers. Always call static members via the class name.
3. **Overusing static.** Making everything static destroys encapsulation: global mutable state is hard to test, easy to corrupt, and impossible to isolate per user or per request.
4. **Assuming static fields are thread-safe.** Static fields are *shared*, not *safe*. Two threads mutating `totalOrders++` concurrently can lose updates unless you synchronize or use atomic types like `AtomicInteger`.
5. **Hiding static fields in subclasses.** A subclass can declare a field with the same name as a parent's static field, "hiding" it. The two fields coexist, and behavior becomes surprising.
6. **Treating static as a speed hack.** "Make it static, it's faster" — static methods do avoid some virtual-dispatch overhead, but premature "optimization" that breaks correct design is never worth it.
7. **Using `static final` for values that should be configurable.** A hard-coded constant can't change at runtime; if a "constant" may differ across deployments, it belongs in configuration, not in `static final`.

### Practice Questions

**Q1.** A class `Car` has `private int speed;` and `private static int carsMade;`. If you create three `Car` objects, how many copies of `speed` and how many copies of `carsMade` exist?

<details>
<summary>Answer</summary>
Three copies of `speed` (one per object) and exactly **one** copy of `carsMade` (shared by the class).
</details>

**Q2.** Can a static method call an instance method? Can an instance method call a static method? Explain.

<details>
<summary>Answer</summary>
A static method **cannot** call an instance method directly — it has no object (no `this`). An instance method **can** call a static method — the shared class data is always available to any object.
</details>

**Q3.** Why must `main` be declared `static`?

<details>
<summary>Answer</summary>
The JVM invokes `main` to start the program, before any objects of the class exist. A static method is callable without an object, which is exactly what the entry point requires.
</details>

**Q4.** You need a method `double add(double a, double b)`. Static or instance? Why?

<details>
<summary>Answer</summary>
**Static.** The method uses no object state — it is a pure function of its parameters, like `Math.max`.
</details>

**Q5.** A `BankAccount` needs `double balance`. Static or instance? Why?

<details>
<summary>Answer</summary>
**Instance.** The balance is per-account data; each account object must hold its own value so accounts don't overwrite each other.
</details>

---

## 11. Key Takeaways / Summary

- A **class** is a blueprint; an **object** is a concrete instance built from it with `new`.
- **Instance fields** live inside each object on the heap — one copy per object.
- **Static fields** live in class metadata — exactly one copy shared by everything.
- **Instance methods** operate on a specific object, have implicit `this`, and can touch both instance and static members.
- **Static methods** belong to the class, have no `this`, and can only touch static members.
- Access static members through the **class name**; access instance members through an **object reference**.
- Decision rule: *per-object data → instance; class-wide data → static; behavior on object state → instance; pure logic → static.*
- `main` is static because it runs before any object exists.
- Constants shared everywhere are `public static final` in `UPPER_SNAKE_CASE`.

### Compact Comparison Table

| Feature | Instance Member | Static Member |
|---|---|---|
| Keyword | none | `static` |
| Owner | an object | the class |
| `this` available? | Yes | No |
| Copies | one per object | one total |
| Access | `object.member` | `ClassName.member` |
| Can access instance fields? | Yes | No |
| Can access static fields? | Yes | Yes |
| Exists without any object? | No | Yes |

---

## 12. Exercises

Work through these in order. Each builds on the last.

**Exercise 1 — Concept recognition (easy).** Read the following class and classify every member as *static* or *instance*. For each static member, state whether it is safe and correct as static:

```java
public class Library {
    private String name;
    private static int booksTotal = 0;
    private static final double FINE_RATE = 0.10;

    public Library(String name) { this.name = name; booksTotal++; }

    public void borrow() { /* ... */ }

    public static String openingHours() { return "9 AM - 9 PM"; }
}
```

> **Hint:** Ask two questions of every member: *Does it need an object to run?* and *Does its value differ per object?* If a field's value must differ per object, static is wrong for it.

**Exercise 2 — Implementation (medium).** Write a `Counter` class with:

- an instance field `int count` and instance methods `increment()` and `getCount()`;
- a static field `int totalIncrements` that counts every increment performed by *any* `Counter` object;
- a static method `getTotalIncrements()`.

In `main`, create two counters, call `increment()` several times on each, and print both the per-object counts and the shared total.

> **Hint:** Remember that the shared counter must be incremented inside `increment()` — every call, no matter which object makes it, adds to the class-wide total.

**Exercise 3 — Design (hard).** Here is a badly designed class where everything is static:

```java
public class User {
    public static String username = "";
    public static int level = 1;

    public static void levelUp() { level++; }
    public static void print() { System.out.println(username + " level " + level); }
}
```

Explain what breaks if two users log in, then refactor it to use instance fields and methods for per-user state while keeping genuinely class-wide data (e.g., `totalUsers`) static. Justify every decision.

> **Hint:** When two objects must coexist with different values, static fields cannot represent them — they all share the same slot. List which fields are *inherently per-user* and which are *inherently global* before you write any code.

---

### Closing Thought

Static and instance are not a style choice — they are a statement about **ownership**. Instance members answer "what does *this* object know?" Static members answer "what does the *class* know?" Get the ownership right, and the syntax, the compiler errors, and the design all fall into place.