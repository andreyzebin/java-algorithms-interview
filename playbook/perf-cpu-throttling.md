# Поиск медленного кода + CPU throttling (Linux / Docker / k8s)

## Куда смотреть СНАЧАЛА — USE / RED методики

**USE** (для ресурсов): для каждого ресурса — **U**tilization, **S**aturation, **E**rrors.
**RED** (для сервисов): **R**ate (rps), **E**rrors, **D**uration (latency p50/p99).

Алгоритм "сервис тормозит", сверху вниз:
1. **RED-дашборд**: где растёт latency / errors? Какой эндпоинт, какой инстанс?
2. **Ресурсы инстанса**: CPU? Память (GC)? IO/сеть? Это сужает до класса проблемы.
3. **CPU saturation**: загрузка близка к лимиту? **Троттлинг?** (см. ниже — частая скрытая причина в k8s)
4. **Это мой код или ожидание?** CPU-bound (горит CPU) vs IO/lock-bound (ждёт). Разные инструменты.
5. **Профайлер** на горячем инстансе: CPU flame graph → горячие методы; wall-clock → где ждём.

## CPU-bound vs IO/lock-bound — как различить
- **CPU usage высокий + latency высокая** → CPU-bound, ищи горячий код (CPU flame graph)
- **CPU usage низкий + latency высокая** → ждём (IO, БД, downstream, lock contention). Wall-clock / lock profiling, thread dump (много BLOCKED/WAITING)
- **CPU usage высокий, но "ничей"** (system/steal) → троттлинг, шумный сосед, GC

## CPU throttling в Linux (cgroups CFS) — частая скрытая причина
Контейнеру дают CPU-лимит. Ядро (CFS scheduler) режет CPU **квотой за период**, НЕ "процент ядра".

### Cgroups v1
```
cpu.cfs_period_us = 100000     # период = 100ms
cpu.cfs_quota_us  = 50000      # квота = 50ms CPU за период → "0.5 ядра"
```
### Cgroups v2 (современные ядра)
```
cpu.max = "50000 100000"       # quota period (то же 0.5 CPU)
```

### Как это режет — ВАЖНО
Лимит = "сколько CPU-времени за 100ms окно". Если приложение израсходовало квоту раньше конца периода — **его замораживают до конца периода**. Даже если CPU свободен!

**Коварство:** многопоточное приложение с лимитом "2 CPU" и 20 рабочими потоками может сжечь квоту за 10ms (20 потоков × 10ms = 200ms работы = квота 2×100ms), и потом **простаивать 90ms** каждого периода. Видно как latency-спайки, при средней утилизации CPU далеко не 100%.

### Как обнаружить троттлинг
```bash
# cgroup v2
cat /sys/fs/cgroup/cpu.stat
# nr_throttled    — сколько периодов было троттлинга
# throttled_usec  — суммарно времени в троттлинге

# cgroup v1
cat /sys/fs/cgroup/cpu,cpuacct/cpu.stat
# nr_throttled, throttled_time (ns)
```
Метрика Prometheus (cAdvisor): **`container_cpu_cfs_throttled_periods_total` / `container_cpu_cfs_periods_total`** — доля затроттленных периодов. >5-10% → проблема.

### Как лечить троттлинг
- **Поднять CPU limit** или вовсе **убрать limit, оставить request** (в k8s limit опционален; многие SRE советуют не ставить CPU limit, только request — троттлинг исчезает, шедулинг по request)
- **Снизить число потоков** под лимит: JVM до Java 8u191 НЕ видела cgroup-лимит → `Runtime.availableProcessors()` возвращал все ядра хоста → пулы и ForkJoinPool раздувались → жесткий троттлинг. Современная JVM (`-XX:+UseContainerSupport`, default) уважает cgroup. Проверь `availableProcessors()` внутри контейнера.
- **`-XX:ActiveProcessorCount=N`** — явно задать, если автоопределение врёт
- GC-потоки тоже считаются: `-XX:ParallelGCThreads`, `-XX:ConciliarGCThreads`
- Поднять период нельзя на уровне pod — это решается лимитами

## CPU steal (виртуализация / облако)
`%steal` в `top`/`mstat` — гипервизор не дал CPU, отдал соседу. Высокий steal → шумный сосед / переподписка хоста. Не лечится изнутри — менять инстанс/ноду.

## Инструменты по слоям

| Слой | Инструмент |
|---|---|
| Кластер k8s | `kubectl top pods/nodes`, метрики cAdvisor → Prometheus/Grafana |
| Контейнер | `docker stats`, `/sys/fs/cgroup/*` |
| Хост Linux | `top`/`htop` (load avg, %us/%sy/%wa/%st), `vmstat 1`, `pidstat 1`, `mpstat -P ALL 1` |
| IO | `iostat -x 1`, `iotop` |
| Сеть | `ss -s`, `iftop`, `tcpdump` |
| JVM CPU | async-profiler (CPU flame graph), JFR, IntelliJ Profiler |
| JVM lock/wait | async-profiler `-e wall`/`-e lock`, jstack ×3 |
| Системные вызовы | `strace -c -p <pid>` (что вызывается), `perf top` |

### load average — как читать
`uptime` → `load average: 4.0, 2.0, 1.0` (1/5/15 мин). На машине с **4 CPU**: load 4.0 = полная загрузка, >4 = очередь. Важно делить на число ядер. На Linux load включает и процессы в **D-state** (ждут IO) — высокий load при низком CPU = IO bottleneck.

## Чек-лист "сервис тормозит в k8s"
1. Grafana: latency/errors какого эндпоинта/пода?
2. `container_cpu_cfs_throttled_periods` — троттлинг? (частая скрытая причина)
3. CPU usage высокий или низкий при высокой latency? → CPU-bound vs ожидание
4. GC: паузы? Old gen растёт? (`jstat -gcutil`)
5. Профайлер на горячем поде: CPU flame (горячий код) или wall/lock (ожидание)
6. Downstream: трейс (OTel) — где время? БД? внешний вызов?
7. `availableProcessors()` внутри контейнера == ожидаемому лимиту?

## Что важно понимать
- Куда смотреть, если сервис тормозит (USE/RED, сверху вниз)
- Как отличить CPU-bound от IO-bound
- Что такое CFS throttling, как обнаружить (`throttled_periods`, `cpu.stat`)
- Почему контейнер троттлится при средней утилизации <100%
- Почему JVM в контейнере раздувала пулы (cgroup awareness)
- Что такое CPU steal, load average и как их читать
