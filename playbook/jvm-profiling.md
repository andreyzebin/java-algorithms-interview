# JVM Profiling

## Quick wins (бандл с JDK, zero-setup)

| Команда | Что |
|---|---|
| `jps` | список JVM-процессов на машине |
| `jcmd <pid> help` | список доступных команд этого процесса |
| `jcmd <pid> Thread.print` | thread dump |
| `jcmd <pid> GC.heap_info` | состояние heap |
| `jcmd <pid> GC.class_histogram` | гистограмма live-объектов |
| `jcmd <pid> VM.flags` | активные опции JVM |
| `jcmd <pid> VM.system_properties` | system props |
| `jstack <pid>` | стеки всех потоков (как `jcmd Thread.print`) |
| `jmap -histo <pid>` | гистограмма (`-histo:live` — только живые) |
| `jmap -dump:format=b,file=heap.hprof <pid>` | heap dump |
| `jstat -gcutil <pid> 1s` | %% зон + GC stats каждую секунду |
| `jinfo <pid>` | конфигурация JVM |

## Thread Dump — что читать
States:
- **RUNNABLE** — реально что-то делает (или ждёт IO — Java не различает)
- **BLOCKED** — ждёт монитор `synchronized`
- **WAITING** — `wait()`, `LockSupport.park()` без таймаута, `join()`
- **TIMED_WAITING** — то же с таймаутом
- **NEW** / **TERMINATED**

Признаки проблем:
- Много потоков с одним и тем же стеком на верху → горячий путь (можно оптимизировать)
- Много BLOCKED на одном lock → contention
- Один поток держит lock, остальные ждут → tracing цепочки блокировок
- Поток в socketRead / DB driver → возможно IO-bound

## JFR — Java Flight Recorder
Семплирующий профайлер встроен в OpenJDK (с 11 — бесплатно).

```bash
# запись на 60 секунд
jcmd <pid> JFR.start duration=60s filename=rec.jfr

# непрерывная запись с автоудалением старых данных
jcmd <pid> JFR.start name=live maxage=30m maxsize=200m

# дамп текущей записи в файл (без остановки)
jcmd <pid> JFR.dump name=live filename=snapshot.jfr

# остановить
jcmd <pid> JFR.stop name=live
```

Анализ — **JDK Mission Control (JMC)**. Видит:
- CPU usage, hot methods
- Allocation (где аллоцируется и сколько)
- GC pauses (длительность, причины)
- Thread states timeline
- Lock contention
- IO (file, socket)
- Exceptions

## async-profiler (production)
Sampling без safepoint bias (jstack семплит только в safepoints — биас). Flame graphs.

```bash
./asprof -d 30 -f cpu.html <pid>             # CPU flame graph
./asprof -d 30 -e alloc -f alloc.html <pid>  # allocation
./asprof -d 30 -e wall -f wall.html <pid>    # wall-clock (включая sleep/wait)
./asprof -d 30 -e lock -f lock.html <pid>    # lock contention
```

## GC logs (анализ пауз)
```bash
-Xlog:gc*:file=gc.log:time,uptime,level,tags                  # Java 9+
```
Анализ: **GCEasy** (онлайн), **GCViewer** (десктоп). Смотри:
- Длительность отдельных GC
- % времени в GC (overhead)
- Тренд роста Old gen — leak?

## JMX
```bash
-Dcom.sun.management.jmxremote.port=7091
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
```
Подключиться: **VisualVM**, **JConsole**, **JMC**.
CLI: `jmxterm`, программно: `jolokia` (HTTP-bridge для JMX).

## Метрики (production)
**Micrometer → Prometheus → Grafana** — стандарт. Ключевые:
- `jvm_memory_used_bytes{area, id}` — по зонам
- `jvm_gc_pause_seconds_count` / `_sum` — паузы
- `jvm_threads_states_threads{state}`
- `jvm_threads_live_threads`
- `process_cpu_usage`
- `system_load_average_1m`

## IDE
- **IntelliJ Profiler** — встроен (использует async-profiler / JFR под капотом)
- **VisualVM** — отдельный инструмент, легковесный
- **JMC** — для JFR в первую очередь

## Чек-лист расследования "приложение тормозит"
1. `jstat -gcutil <pid> 1s` — не паузит ли GC?
2. `jstack` 3 раза с интервалом — что висит?
3. JFR запись на 1-2 минуты — общий профиль
4. CPU flame graph → найти горячие методы
5. Allocation flame graph → лишние аллокации
6. Lock contention → синхронизация
7. GC log → паузы и причины

## Что важно понимать
- Какие инструменты профайлинга знаешь
- Чем JFR отличается от стандартного jstack/jmap (постоянный low-overhead профиль)
- Как снять thread dump в production
- Как понять, что приложение CPU-bound vs IO-bound (thread states + flame)
- Что такое safepoint bias и почему async-profiler точнее
