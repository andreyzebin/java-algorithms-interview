# SRE: балансировщики, отказоустойчивость, retry между кластерами

## Виды балансировщиков

### L4 (transport, TCP/UDP)
Балансирует по IP:port, не смотрит в содержимое. Быстрый, дешёвый, не терминирует TLS (passthrough).
- Примеры: AWS NLB, IPVS, Linux LVS, HAProxy (в TCP mode), kube-proxy (iptables/IPVS)
- Алгоритмы: round-robin, least-conn, hash по source IP (session affinity)
- Не умеет: маршрутизацию по URL/заголовкам, per-request retry, content-based routing

### L7 (application, HTTP/gRPC)
Понимает HTTP: путь, заголовки, методы, cookies. Терминирует TLS.
- Примеры: Envoy, NGINX, HAProxy (HTTP mode), AWS ALB, Traefik, Istio (на Envoy), API Gateway
- Умеет: routing по path/host/header, retry/timeout per-request, circuit breaking, rate limiting, header rewrite, canary/weighted, sticky sessions, gRPC-aware
- Дороже L4 (парсит каждый запрос)

### Где они в стеке
```
DNS (GeoDNS, round-robin)            ← глобальный, грубый
  └─ Global LB / Anycast             ← между регионами/дата-центрами
      └─ L4 LB (NLB)                 ← вход в кластер, TCP
          └─ L7 LB / Ingress (Envoy/NGINX/ALB)  ← routing, retry, TLS
              └─ Service mesh (Istio/Linkerd)    ← между сервисами (sidecar)
                  └─ pods
```

### Что какой балансировщик решает
| Проблема | Решение |
|---|---|
| Распределить нагрузку между инстансами | любой LB, round-robin / least-conn |
| Не слать на мёртвый инстанс | health checks (active/passive) |
| Не слать на деградировавший инстанс | outlier detection (passive) |
| Перенаправить по URL/версии API | L7 routing |
| Канарейка / blue-green | L7 weighted routing |
| Защита от перегрузки | rate limiting, circuit breaker, load shedding |
| Липкие сессии | session affinity (L4 hash / L7 cookie) |
| Глобальная отказоустойчивость регионов | GeoDNS / Anycast / global LB |
| mTLS между сервисами | service mesh |

## Балансировка между двумя кластерами k8s
```
                    ┌─ L7 Global LB (Envoy / ALB / Cloudflare) ─┐
   клиент  ───────► │   health checks + outlier detection       │
                    └──────────┬───────────────────┬────────────┘
                          Cluster A            Cluster B
                          (иногда сетевые        (здоров)
                           проблемы к
                           внешнему сервису)
```

## Кейс: кластер A иногда теряет сеть к внешнему сервису

### Как ОБНАРУЖИТЬ
1. **Метрики с разбивкой по кластеру/зоне** (label `cluster=A/B`):
   - error rate / latency внешнего вызова по кластерам — у A всплески, у B нет
   - `upstream_rq_timeout`, `upstream_cx_connect_fail` в Envoy по кластеру
2. **Distributed tracing (OpenTelemetry/Jaeger)**: трейс упавшего запроса показывает, что время/ошибка на span'е вызова внешнего сервиса именно из подов кластера A
3. **Synthetic / blackbox probes** из каждого кластера к внешнему сервису (blackbox_exporter) — у A периодически fail, у B нет → проблема локализована в сети кластера A, не во внешнем сервисе
4. **Корреляция**: совпадает ли с деплоями CNI, сетевой политикой, NAT gateway, conntrack table full, DNS (CoreDNS) в кластере A
5. **Egress-специфика**: часто это NAT gateway / SNAT port exhaustion / `conntrack: table full` / MTU / DNS таймауты в одном кластере. Смотреть `conntrack -S`, логи CoreDNS, метрики NAT GW

### Как СДЕЛАТЬ, чтобы упавшие запросы НЕ падали безвозвратно

**1. Retry на L7 (идемпотентность обязательна!)**
- Retry на **другой upstream** (другой кластер), не на тот же:
  - Envoy: `retry_policy` + `retry_host_predicate` (`previous_hosts`) + `host_selection_retry_max_attempts` → повтор пойдёт на другой хост/кластер, не на упавший
  - `retry_on: connect-failure,refused-stream,unavailable,5xx,reset` + бюджет `num_retries`
- ⚠️ Retry **только идемпотентных** операций (GET, или POST с Idempotency-Key) — иначе двойное списание

**2. Outlier detection (passive health check)**
- Envoy сам **выкидывает** кластер/endpoint из пула после N подряд 5xx / timeout (`consecutive_5xx`, `consecutive_gateway_failure`), на время (`base_ejection_time`), с экспонентой
- Деградировавший кластер A временно исключается → трафик идёт на B автоматически

**3. Active health checks**
- LB периодически пингует health-эндпоинт; **deep health check** должен проверять и доступность внешнего сервиса из этого кластера → A зафейлит проверку → выводится из ротации

**4. Circuit breaker + fallback (на стороне приложения)**
- Resilience4j: при череде ошибок внешнего вызова — открыть circuit, отдать fallback (кэш/деградация), не копить таймауты

**5. Timeouts + budget**
- Жёсткий timeout на внешний вызов; **retry budget** (≤20-30% трафика в ретраях) чтобы не устроить retry storm и не положить B
- **Hedged requests** — слать дубль на B, если A не ответил за p95 (ценой лишней нагрузки)

**6. Failover на уровне трафика**
- Приоритизация: основной кластер + failover-приоритет (Envoy `priority`), B как backup пул
- Локальность: предпочитать локальную зону, при деградации — failover в другую (locality-weighted LB)

### Сводно — слои защиты
```
timeout → retry (на другой кластер, идемпотентно, с budget)
        → outlier detection (выкинуть больной кластер)
        → circuit breaker (перестать долбить, fallback)
        → health check (вывести из ротации)
        → failover priority (B как backup)
```

## Алгоритмы балансировки
- **Round-robin** — по кругу. Просто, не учитывает нагрузку.
- **Weighted RR** — с весами (канарейка, разные по мощности инстансы).
- **Least connections** — кому меньше активных соединений. Лучше при разной длительности запросов.
- **Least request / P2C** (power-of-two-choices) — выбрать 2 случайных, отдать менее загруженному. Дёшево и почти оптимально (Envoy default).
- **Consistent hashing (ring hash / maglev)** — стабильная привязка ключа к бэкенду (кэши, sticky). При выпадении узла переезжает мало ключей.
- **Hash по source IP** — простая липкость сессии на L4.

## Health checks
- **Active** — LB сам опрашивает `/health`. Быстро ловит, но даёт нагрузку.
- **Passive (outlier detection)** — наблюдает за реальным трафиком, выкидывает по ошибкам. Без лишних запросов.
- **liveness** (рестартнуть pod) vs **readiness** (убрать из LB, не рестартить) vs **startup** (дать прогреться) — в k8s это РАЗНЫЕ пробы. Путать нельзя: liveness на тяжёлую проверку → kill-loop.

## Часто на собесе / в SRE-интервью
- Разница L4 и L7 балансировщиков, что умеет L7
- Алгоритмы балансировки, когда least-conn лучше RR
- Что такое outlier detection vs active health check
- Как сделать retry безопасным (идемпотентность, retry budget, retry storm)
- Liveness vs readiness vs startup проба
- Как обнаружить, что один кластер/зона деградирует (per-cluster метрики, blackbox probes, tracing)
- Что такое circuit breaker, hedged requests, load shedding
- Как failover между регионами (GeoDNS, Anycast, locality LB)
