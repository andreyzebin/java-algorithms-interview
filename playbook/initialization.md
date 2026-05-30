# Class Initialization

## Этапы (по JLS)
1. **Loading** — ClassLoader находит байткод и создаёт `Class` объект
2. **Linking** = Verification (валидность) + Preparation (статики обнуляются default-значениями) + Resolution (разрешение символьных ссылок)
3. **Initialization** — выполняются `<clinit>`: static-инициализаторы и static-блоки в порядке текста

## Триггеры инициализации класса
- `new ClassName()`
- доступ к **нестатическому-final** static полю или вызов static метода
- `Class.forName("...")` (3-арг вариант с `initialize=false` — не инициализирует)
- если инициализируется подкласс — родитель инициализируется первым
- запуск `main(String[])`

НЕ триггерит:
- `ClassName.class` — Class-объект уже есть от loading
- `ClassLoader.loadClass(name)` — только loading
- Доступ к `static final` константе примитива/String — компилятор inlin-ит значение в callsite

## Порядок инициализации объекта

```java
class Parent {
    static int sP = log("sP");                  // 1
    static { log("static parent block"); }      // 1

    int iP = log("iP");                         // 4
    { log("instance parent block"); }           // 4
    Parent() { log("parent ctor body"); }       // 5
}

class Child extends Parent {
    static int sC = log("sC");                  // 2
    static { log("static child block"); }       // 2

    int iC = log("iC");                         // 6
    { log("instance child block"); }            // 6
    Child() { log("child ctor body"); }         // 7
}

new Child();
```

Порядок при `new Child()`:
1. **Static Parent** (поля и static-блоки в порядке текста) — если ещё не было
2. **Static Child** (то же) — один раз за жизнь JVM
3. *↑ ВСЁ ВЫШЕ — однократно при первой инициализации классов*
4. **Instance Parent** field initializers + instance init blocks в порядке текста
5. **Parent constructor body**
6. **Instance Child** field initializers + instance init blocks в порядке текста
7. **Child constructor body**

## Важное
- **Field initializer и instance init block идут в порядке текста (смешанно!)**
- Static-блоки и static-инициализаторы — тоже в порядке текста
- `this` во время field init / blocks ссылается на **частично сконструированный** объект — если вызвать переопределённый метод из конструктора родителя, тот увидит непроинициализированные поля ребёнка
- Каждое `<clinit>` выполняется JVM под блокировкой — **lazy и thread-safe** автоматически

## Lazy init: Initialization-on-Demand Holder
```java
class Singleton {
    private Singleton() {}
    private static class Holder {
        static final Singleton INSTANCE = new Singleton();
    }
    public static Singleton get() { return Holder.INSTANCE; }
}
```
- `Holder` загружается только при первом обращении к `get()`
- `<clinit>` Holder — атомарен по JLS
- Лучше чем double-checked locking: проще, без `volatile`, без `synchronized`

## Compile-time constants — ловушка
```java
public static final int X = 42;                // INLINED в callsite — рекомпил callers!
public static final int Y = computeAtRuntime(); // НЕ inlined — runtime init
public static final String Z = "hello";        // INLINED
```
Если в либе `public static final int VERSION = 1` → consumer-код после рекомпила имеет `1` зашитую. Поменяли на `2` без пересборки consumer-а — он всё ещё видит `1`.

## ClassLoader API
```java
Class<?> a = Class.forName("Foo");                          // load + init
Class<?> b = Class.forName("Foo", false, loader);           // load, БЕЗ init
Class<?> c = loader.loadClass("Foo");                       // только load
```

## Common pitfalls
- Циклическая static-инициализация (A.x читает B, B.y читает A) — увидишь partially-initialized класс с default значениями
- Forward reference в static-инициализаторе на ещё необъявленное поле — ошибка компиляции (illegal forward reference)
- `final` поле должно быть присвоено **ровно один раз** до конца конструктора
- Вызов overridable метода из конструктора → метод подкласса увидит непроинициализированные поля подкласса

## Что важно понимать
- Порядок выполнения: static родителя → static дитя → поля родителя → ctor родителя → поля дитя → ctor дитя
- Когда происходит инициализация класса
- Что такое class loader, как работает delegation
- Чем `Class.forName` отличается от `loader.loadClass`
- Как сделать singleton — Holder idiom
- Что такое static-инициализатор и сколько раз он выполняется
