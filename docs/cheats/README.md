# Cheatsheets

Шпаргалки для собеседования. Открывай в IntelliJ (preview .md — `Ctrl+Shift+A` → "Preview").

## Java / JVM
- [JVM](jvm.md) — структура, ClassLoader, JIT, флаги
- [Java Memory Model](java-memory-model.md) — happens-before, volatile, synchronized
- [Garbage Collection](garbage-collection.md) — generational, G1, ZGC, тюнинг
- [Generics](generics.md) — type erasure, PECS, wildcards
- [Class Initialization](initialization.md) — порядок инициализации static/instance
- [JVM Profiling](jvm-profiling.md) — jcmd, JFR, async-profiler
- [OOM Profiling](oom-profiling.md) — виды OOM, heap dump, MAT
- [Modern Java & JVM](modern-java-jvm.md) — фишки 9-25, Loom, Valhalla, Panama
- [Modern Frameworks](modern-frameworks.md) — Quarkus, Micronaut, Helidon Níma, GraalVM Native
- [Banking / Transactional Stack](banking-stack.md) — VT vs reactive, JPA bottlenecks, saga, outbox, observability

## SQL / Базы
- [SQL](sql.md) — JOIN, window, CTE, идиомы
- [SQL Indexes](sql-indexes.md) — B-Tree, composite, partial, covering
- [Query Plan Optimization](sql-query-optimization.md) — EXPLAIN, scan/join types
- [ACID & Isolation](acid-isolation.md) — аномалии, уровни, MVCC, locks

## Алгоритмы (`./gradlew cheat`)
Идиомы в `src/main/java/io/github/zebin/test/cheatsheet/`, разбиты по режиму вспоминания:
- `LanguageIdioms.java` — «как написать X в Java»: arrays, strings, numbers, bits, math, collections, sort, gotchas
- `AlgorithmPatterns.java` — техники: matrix, two pointers, sliding window, binary search, prefix sums, monotonic stack, greedy, backtracking, DP, fast input
- `DataStructures.java` — структуры: graph/tree (BFS/DFS), linked list, trie, union-find, heap/top-K
- `Cheatsheet.java` — лаунчер, запускает все три
