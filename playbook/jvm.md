# JVM

## Что делает JVM
1. Грузит байткод (`.class`) через ClassLoader
2. Исполняет (interpreter → JIT)
3. Управляет памятью (GC)

## Структура
```
Class Loader Subsystem
  Bootstrap → Platform → Application → Custom (parent-first delegation)

Runtime Data Areas
  Heap          — объекты, массивы (общая)
  Metaspace     — метаданные классов (общая, native memory)
  JVM Stack     — фреймы методов (на поток) — SOE при переполнении
  PC Register   — указатель на текущую инструкцию (на поток)
  Native Stack  — для JNI
  Code Cache    — нативный код от JIT

Execution Engine
  Interpreter → JIT (C1/C2) → GC
```

## ClassLoader (parent-first)
1. **Bootstrap** (native) — `java.base` (rt.jar до 9)
2. **Platform** — `java.sql`, `java.xml` ...
3. **Application** — classpath
4. **Custom** — web servers, OSGi

Ребёнок сначала спрашивает родителя; если тот не нашёл — грузит сам.

## JIT — Just In Time
- **Interpreter** — сразу, но медленно
- **C1 (client)** — быстрая компиляция, простые оптимизации
- **C2 (server)** — агрессивные оптимизации: inline, escape analysis, loop unrolling, dead code elimination
- **Tiered compilation** (default) — Interp → C1 → C2 по мере прогрева
- Порог: `-XX:CompileThreshold` (≈10000 вызовов)
- **OSR** (On-Stack Replacement) — горячий метод заменяется JIT-версией прямо во время выполнения
- **Deoptimization** — JIT может откатиться к interpreter (если оптимизация оказалась невалидной)

## Bytecode (минимум для понимания)
- Stack-based VM (не регистровая)
- `javap -c MyClass` — посмотреть байткод
- Stack frame = local variables array + operand stack + reference на constant pool

## Полезные флаги
| Флаг | Что |
|---|---|
| `-Xms<size>` / `-Xmx<size>` | min/max heap |
| `-Xss<size>` | размер стека потока |
| `-XX:MaxMetaspaceSize=` | лимит metaspace |
| `-XX:+UseG1GC` | G1 (default с 9) |
| `-XX:+UseZGC` / `-XX:+UseShenandoahGC` | low-latency GC |
| `-Xlog:gc*` | unified GC logging (9+) |
| `-XX:+HeapDumpOnOutOfMemoryError` | дамп при OOM |
| `-XX:HeapDumpPath=/path` | куда |
| `-XX:+PrintFlagsFinal` | все опции с значениями |
| `-XX:+UnlockDiagnosticVMOptions` | разблокировать диагностические |
| `-XX:+PrintCompilation` | что JIT компилит |

## Команды
- `jps` — список JVM-процессов
- `jcmd <pid> help` — список команд для процесса
- `jcmd <pid> VM.flags` — активные флаги
- `jcmd <pid> Thread.print` — thread dump
- `jcmd <pid> GC.heap_info`
- `jstack <pid>` — стеки потоков
- `jmap -dump:format=b,file=heap.hprof <pid>` — heap dump
- `jstat -gcutil <pid> 1s` — GC stats каждую секунду
- `jinfo <pid>` — конфигурация

## Часто спрашивают
- Чем отличается heap от stack — где что лежит
- Что такое metaspace, чем отличается от PermGen (PermGen был фиксированного размера, в heap; metaspace — в native memory, растёт)
- Как работает class loader, parent-first delegation
- Что делает JIT и когда — горячие методы, tiered
- Зачем JVM compressed oops (-XX:+UseCompressedOops, default до 32GB)
