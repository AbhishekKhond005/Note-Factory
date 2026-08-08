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