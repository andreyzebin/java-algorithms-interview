# Playbook — полевые заметки

Что ломается в Java-сервисе на проде и как с этим жить. Не учебник и не справочник API —
конспект «под капотом + эксплуатация»: JVM internals, диагностика, данные под нагрузкой, SRE.
Глубокий system design сознательно вынесен за скобки — тут слой рантайма и Ops.

Открывай в IntelliJ (preview .md — `Ctrl+Shift+A` → "Preview").

## Java / JVM
- [JVM](jvm.md) — структура, ClassLoader, JIT, флаги
- [Java Memory Model](java-memory-model.md) — happens-before, volatile, synchronized
- [Garbage Collection](garbage-collection.md) — generational, G1, ZGC, тюнинг
- [Generics](generics.md) — type erasure, PECS, wildcards
- [Class Initialization](initialization.md) — порядок инициализации static/instance
- [JVM Profiling](jvm-profiling.md) — jcmd, JFR, async-profiler
- [OOM Profiling](oom-profiling.md) — виды OOM, heap dump, MAT
- [JIT & Warmup](jit-and-warmup.md) — C1/C2, tiered, deopt, прогрев, CRaC, Leyden
- [Modern Java & JVM](modern-java-jvm.md) — фишки 9-25, Loom, Valhalla, Panama
- [Modern Frameworks](modern-frameworks.md) — Quarkus, Micronaut, Helidon Níma, GraalVM Native
- [Banking / Transactional Stack](banking-stack.md) — VT vs reactive, JPA bottlenecks, saga, outbox, observability

## SQL / Базы
- [SQL](sql.md) — JOIN, window, CTE, идиомы
- [SQL Indexes](sql-indexes.md) — B-Tree, composite, partial, covering, index-only scan + MVCC
- [Query Plan Optimization](sql-query-optimization.md) — EXPLAIN, scan/join types, red flags
- [ACID & Isolation](acid-isolation.md) — аномалии, уровни, MVCC, locks

## SRE / Performance / Infra
- [Perf & CPU Throttling](perf-cpu-throttling.md) — поиск медленного кода, USE/RED, CFS throttling в k8s
- [SRE: Load Balancing](sre-load-balancing.md) — L4/L7, retry между кластерами, outlier detection, failover

---
Бонус — алгоритмический код-конспект в модуле [`../tasks/`](../tasks): `./gradlew :tasks:cheat`.
