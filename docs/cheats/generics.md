# Generics

## Type Erasure
Generics существуют только в compile-time. В рантайме `List<String>` = `List`.

Что НЕЛЬЗЯ из-за стирания:
- `new T()` — ошибка, нужно `Supplier<T>` или `Class<T>`
- `new T[10]` — ошибка
- `instanceof T` — ошибка
- `T.class` — ошибка
- Перегрузка `f(List<String>)` и `f(List<Integer>)` — одна сигнатура после erasure, не скомпилится

Что РАБОТАЕТ:
- Generic метод бросает generic exception: `<T extends Throwable> void m() throws T`
- Reflection — типы стираются, но информация о generic *в сигнатуре* доступна через `getGenericSuperclass()`, `getGenericReturnType()`

## Wildcards — PECS
**P**roducer **E**xtends, **C**onsumer **S**uper.

- `List<? extends Animal>` — *читаем* как Animal. Можем читать (всё что внутри — Animal или подтип). **НЕ можем писать** (кроме null) — неизвестен конкретный тип.
- `List<? super Cat>` — *кладём* Cat. **Читаем только как Object** (потому что внутри может быть Cat, Animal, Object).
- `List<?>` — unbounded, только Object для чтения и null для записи.

Классика — generic copy:
```java
public static <T> void copy(List<? extends T> src, List<? super T> dst) {
    for (T x : src) dst.add(x);
}
```

## Bounded type parameter
```java
<T extends Comparable<T>>                 // T должен быть Comparable<T>
<T extends Number & Comparable<T>>        // несколько границ через &
                                          // (только один класс, остальное — интерфейсы)
```

## Generic метод
```java
public static <T> T firstNonNull(T a, T b) {
    return a != null ? a : b;
}
// вызов: тип обычно выводится; явный — Util.<String>firstNonNull(...)
```

## Generic класс
```java
class Box<T> {
    private T value;
    public T get() { return value; }
    public void set(T v) { value = v; }
}
```

## Что НЕ компилится
```java
T t = new T();                        // ERROR
T[] arr = new T[10];                  // ERROR
List<T>[] arr = new List<T>[10];      // ERROR (generic array)
if (obj instanceof T) ...             // ERROR
catch (T e) ...                       // ERROR
class A<T> { static T t; }            // ERROR (static + type param)
class A<T> { static void m(T t){} }   // ERROR
```

## Generic array workaround
```java
@SuppressWarnings("unchecked")
T[] arr = (T[]) new Object[n];                   // unchecked, всё ок если не утекает наружу
T[] arr2 = (T[]) Array.newInstance(clazz, n);    // если есть Class<T> — типобезопасно
```

## @SafeVarargs
Подавляет heap pollution warning для `T... varargs`. Применима к `static`, `final`, `private` методам (с Java 9 — и к `private`).

## Вариантность
- `List<Dog>` НЕ является `List<Animal>` — **invariant**
- `Dog[]` ЯВЛЯЕТСЯ `Animal[]` — массивы **covariant** (наследие, причина `ArrayStoreException` в рантайме)
- Чтобы получить ковариантность для дженерика — wildcard: `List<? extends Animal>`

## Recursive bound
```java
<T extends Comparable<T>>          // T сравним с самим собой
class Enum<E extends Enum<E>>      // ровно тот же тип (паттерн "self-bounded")
```

## Bridge methods
При extends generic interface компилятор генерирует bridge-метод с erased сигнатурой для совместимости. Просто знать о существовании.

## Generic interface implementation
```java
interface Comparator<T> { int compare(T a, T b); }
class IntCmp implements Comparator<Integer> {
    @Override public int compare(Integer a, Integer b) { return a - b; }
}
```

## Часто на собесе
- Что такое type erasure и его последствия (нет `new T()`, нет `T[]`)
- Объясни PECS
- В чём разница `List<Object>` и `List<?>` (первая фиксирована — кладём всё; вторая — кладём только null)
- Разница `<T extends Foo>` и `<? extends Foo>` (type parameter vs wildcard; первый именован и переиспользуется в сигнатуре)
- Что выведется при reflection если генерик стёрт
- Зачем нужен `super` в `<? super T>`
