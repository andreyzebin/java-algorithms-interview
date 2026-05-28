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

## SQL / Базы
- [SQL](sql.md) — JOIN, window, CTE, идиомы
- [SQL Indexes](sql-indexes.md) — B-Tree, composite, partial, covering
- [Query Plan Optimization](sql-query-optimization.md) — EXPLAIN, scan/join types
- [ACID & Isolation](acid-isolation.md) — аномалии, уровни, MVCC, locks

## Алгоритмы
- См. `src/main/java/io/github/zebin/test/cheatsheet/Cheatsheet.java` (`./gradlew cheat`)
