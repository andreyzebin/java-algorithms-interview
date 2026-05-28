# Garbage Collection

## Generational Hypothesis
Большинство объектов умирают молодыми. Делим heap на поколения, чистим часто молодое, редко старое.

## Heap Layout (классический)
```
┌─────────────────── Young Generation ─────────────────┐  ┌─── Old Generation ───┐
│  Eden     │ Survivor 0 │ Survivor 1  │                 │                       │
└──────────────────────────────────────┘                 └───────────────────────┘
```
+ **Metaspace** (native, отдельно) — метаданные классов

## Жизненный цикл объекта
1. Allocate в Eden
2. Minor GC: выжившие → S0
3. Следующий Minor GC: выжившие из Eden + S0 → S1 (S0 чистится)
4. После N циклов (`-XX:MaxTenuringThreshold`, ≈15) → промоушн в Old
5. Огромные объекты (humongous) могут идти сразу в Old

## Виды GC
- **Minor GC** — только Young. STW, но обычно <100ms.
- **Major GC** — Old gen.
- **Full GC** — всё, включая metaspace. Дорого.

## Алгоритмы

| Алгоритм | Когда выбрать | Паузы | Throughput |
|---|---|---|---|
| **Serial** (`-XX:+UseSerialGC`) | tiny apps, 1 CPU | большие | средний |
| **Parallel** (`-XX:+UseParallelGC`) | batch/throughput-критично | большие | максимум |
| **CMS** | удалён в 14 | низкие | средний |
| **G1** (`-XX:+UseG1GC`, default с 9) | general purpose | низкие предсказуемые | хорош |
| **ZGC** (`-XX:+UseZGC`) | huge heap (TB), сверхнизкая латентность | <1ms | хорош |
| **Shenandoah** (`-XX:+UseShenandoahGC`) | low pause (Red Hat) | <10ms | хорош |
| **Epsilon** (`-XX:+UseEpsilonGC`) | no-op, бенчмарки | — | — |

## G1 (default)
- Heap делится на ~2000 регионов фикс. размера
- Каждый регион — Eden / Survivor / Old / Humongous (динамически)
- Концентрируется на регионах с наибольшим мусором → "Garbage First"
- Цель паузы: `-XX:MaxGCPauseMillis=200`
- Concurrent marking, copying compaction

## ZGC / Shenandoah
- Concurrent compaction — двигают объекты пока приложение работает
- Используют forwarding pointers + load barriers
- Паузы O(1) — независимо от размера heap
- ZGC generational с Java 21 — раньше был single-gen

## Reference types
- **Strong** — обычные ссылки. GC не трогает пока достижимо.
- **Soft** (`SoftReference`) — собирается перед OOM. Подходит для кэшей "если есть память — держим".
- **Weak** (`WeakReference`) — собирается при следующем GC. `WeakHashMap`.
- **Phantom** (`PhantomReference`) — для пост-mortem cleanup. Уведомление через ReferenceQueue.

## finalize() — НЕ ИСПОЛЬЗОВАТЬ
- Непредсказуемый
- Удлиняет жизнь объекта на цикл GC
- Может воскресить объект
- Deprecated с 9, удалят
- Замена: `try-with-resources` (`AutoCloseable`) и `java.lang.ref.Cleaner`

## Тюнинг (грубые правила)
- `-Xms` == `-Xmx` — фиксирует размер, нет дорогих resize
- Allocation rate низкий + объекты долго живут → больше Old (`-XX:NewRatio=4`)
- Allocation rate высокий, объекты короткоживущие → больше Young
- Цель: minor GC часто и быстро, full GC почти никогда

## STW (Stop-The-World)
Все приложение-потоки замораживаются на safepoint. У всех GC есть STW-фазы. Цель современных GC — минимизировать.

## Диагностика
```bash
# unified GC log (Java 9+)
-Xlog:gc*:file=gc.log:time,uptime,level,tags

jstat -gcutil <pid> 1s            # %% использования каждой зоны + кол-во и время GC
jcmd <pid> GC.heap_info
jcmd <pid> GC.run                 # форсировать (для тестов!)
```
Анализ GC log: **GCEasy** (онлайн), **GCViewer** (десктоп).

## Часто спрашивают
- Какие алгоритмы GC знаешь, отличия G1 / ZGC
- Что такое STW
- Объясни generational hypothesis
- Зачем Survivor 0 / Survivor 1 (compaction + tenuring count)
- В чём минус CMS (фрагментация → может потребовать Full GC; sweep долгий)
- Что такое promotion failure / concurrent mode failure
- Когда происходит Full GC и почему его боятся
