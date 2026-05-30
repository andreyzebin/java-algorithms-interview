# Java Service Playbook

Полевые заметки про Java-сервис в проде: всё, что ломается на уровне рантайма и эксплуатации,
и как это диагностировать и чинить. Фокус — **Ops + код**, без глубокого system design.

Не курс и не пересказ официальных доков — конспект «под капотом + что болит на проде»,
собранный из практики. Если что-то полезно — забирай.

## Два подпроекта

### 📘 [`playbook/`](playbook) — полевые заметки (главное)
Markdown-конспекты по тому, что реально определяет поведение сервиса под нагрузкой:

- **JVM** — internals, ClassLoader, JIT и прогрев, GC, JMM, инициализация
- **Профилирование** — JFR, async-profiler, поиск медленного кода, OOM, CPU throttling в k8s
- **Современная Java** — фичи 9-25, Loom/virtual threads, Quarkus/Micronaut/Helidon, GraalVM
- **Данные** — SQL-идиомы, индексы и index-only scan под MVCC, чтение query plan, ACID и изоляция
- **SRE** — балансировка L4/L7, retry/failover между кластерами, outlier detection

Старт: [`playbook/README.md`](playbook/README.md).

### 🧩 [`tasks/`](tasks) — алгоритмический код-конспект (бонус)
Gradle-модуль на Java 25: runnable-идиомы (как писать BFS, binary search, DP, two pointers…)
плюс готовый harness, чтобы быстро накидать решение и прогнать тест.

```bash
./gradlew :tasks:cheat   # прогнать все идиомы-демки и посмотреть вывод
./gradlew :tasks:run     # запустить main
./gradlew :tasks:test    # прогнать юнит-тесты
```

Идиомы разбиты по режиму вспоминания:
- `LanguageIdioms` — «как написать X в Java»
- `AlgorithmPatterns` — техники (matrix, two pointers, sliding window, BS, DP, backtracking…)
- `DataStructures` — граф/дерево, linked list, trie, union-find, heap

## Стек
Gradle 9.3 (Kotlin DSL) · Java 25 toolchain (Foojay auto-provision) · JUnit 6 · AssertJ.
