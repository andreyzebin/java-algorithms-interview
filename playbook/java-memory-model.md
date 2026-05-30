# Java Memory Model (JMM)

## Зачем
Описывает, что один поток видит из изменений другого. CPU и компилятор могут переупорядочивать операции — JMM задаёт правила, когда нельзя.

## Happens-Before (HB)
Если A *happens-before* B, то эффект A виден B. Гарантирует visibility + ordering.

Основные рёбра:
1. **Program order** — внутри одного потока операции HB по тексту
2. **Monitor lock** — `unlock(M)` HB следующий `lock(M)` того же монитора
3. **Volatile** — write волатильной переменной HB следующее чтение её же
4. **Thread start** — `t.start()` HB всё внутри потока t
5. **Thread join** — всё внутри t HB код после `t.join()`
6. **Final fields** — корректно построенные final поля видны без синхронизации
7. **Transitive** — A HB B, B HB C ⇒ A HB C

## volatile
- **visibility** — другие потоки увидят последнюю запись
- **ordering** — нельзя переупорядочивать через volatile
- **НЕ обеспечивает атомарность compound-операций**: `volatile int x; x++` — НЕ атомарно (read+write)
- Хорошо для: одиночных флагов (`volatile boolean stopped`), double-checked locking, publication

## synchronized
- **mutex** + visibility + ordering
- `synchronized` метод = `synchronized(this)`; статический = `synchronized(Class)`
- `synchronized(this)` ≠ `synchronized(SomeClass.class)` — РАЗНЫЕ мониторы
- `wait()` / `notify()` / `notifyAll()` — только в synchronized блоке на том же объекте
- При выходе из synchronized — все мутации публикуются

## final
- Корректно опубликованный final примитив/ссылка виден всем без доп. синхронизации
- НО только если `this` не утекает в конструкторе (например, регистрация в листенере) — иначе другие потоки увидят non-final view

## Atomic классы
`AtomicInteger`, `AtomicLong`, `AtomicReference`, `LongAdder`, `LongAccumulator`:
- CAS (compareAndSet) внутри
- volatile поле value
- `incrementAndGet()` — атомарный `++`
- `updateAndGet(x -> ...)` — апдейт через лямбду
- `LongAdder` лучше под высокой контентионностью (распределяет на ячейки)

## Гонки
- **Data race** — две операции на одной переменной без HB, минимум одна — write. По JMM это undefined behavior.
- **Race condition** — логическая ошибка, может быть и без data race (если операции "атомарны", но неправильно скомбинированы).

## Double-Checked Locking (правильно)
```java
private volatile Singleton instance;          // volatile ОБЯЗАТЕЛЕН с Java 5+
public Singleton get() {
    if (instance == null) {
        synchronized (this) {
            if (instance == null) instance = new Singleton();
        }
    }
    return instance;
}
```
Лучше — Holder idiom:
```java
private static class Holder { static final Singleton I = new Singleton(); }
public static Singleton get() { return Holder.I; }
```
Lazy + thread-safe + без synchronized (JLS гарантирует один `<clinit>` на класс).

## Известные ловушки
- `HashMap` в многопоточке — данные теряются, до Java 8 ещё и infinite loop при resize
- `ConcurrentHashMap` не делает атомарными compound-операции: `cm.put(k, cm.get(k)+1)` — гонка, используй `compute` / `merge`
- `Collections.synchronizedMap(...)` — каждая операция атомарна, но итерация — нет (нужен внешний lock)
- Иммутабельные (`String`, `BigInteger`, records с final полями) — потокобезопасны "бесплатно"
- `ThreadLocal` в thread pool — поток живёт, значение не очищается → leak

## Memory barriers (под капотом)
- `LoadLoad`, `LoadStore`, `StoreLoad`, `StoreStore` — JVM расставляет вокруг volatile/synchronized в зависимости от архитектуры
- На x86 многие барьеры дешевы (TSO), на ARM — нет

## Что важно понимать
- Расскажи про happens-before
- Чем volatile отличается от synchronized
- Почему `volatile counter++` — гонка
- Когда нужен double-checked locking и почему volatile
- Что такое compare-and-swap
- ABA-проблема в CAS (решается AtomicStampedReference)
