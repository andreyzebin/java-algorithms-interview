# Banking / Transactional Service Stack (2026)

## TL;DR — что выбрать

Для классического transactional CRUD-сервиса (request → DB → response, без streaming):
```
Spring Boot 3.2+ (или 4)
  + virtual threads on Tomcat       (spring.threads.virtual.enabled=true)
  + ZGC generational                (-XX:+UseZGC, паузы <1ms)
  + JDBC + HikariCP                 (НЕ R2DBC)
  + Spring Data JDBC / jOOQ         (для тяжёлых случаев JPA)
  + Resilience4j                    (circuit breaker, retry, bulkhead)
  + Micrometer + OpenTelemetry      → Prometheus + Tempo / Jaeger
```

В 2026 для банковского transactional сервиса **virtual threads + ZGC вытеснили reactive**. Реактив оставляют только для streaming / SSE / WebSocket.

## Цифры из бенчей 2026
- VT в Spring MVC: +40% throughput vs WebFlux на DB-heavy workload (Red Hat)
- −25% tail latency vs WebFlux (Netflix)
- Прогноз: 70% adoption VT к 2027

## Где reactive ещё нужен

- **Streaming pipelines** — Kafka → process → Kafka, с backpressure
- **Server-Sent Events / WebSockets** с тысячами long-lived connections
- **API-gateway** с агрегацией множества downstream-вызовов (хотя `StructuredTaskScope` теперь часто покрывает)
- **Сложная композиция async** — `Mono.zip()`, fan-out/fan-in операторы

## Является ли синхронный JDBC/JPA узким горлышком?

**Нет, не JDBC сам по себе.** Узкое горлышко — **connection pool**, и оно ОДНО И ТО ЖЕ у sync и reactive.

### Что реально лимитирует
- HikariCP типично 10-20 коннектов (= лимит БД ÷ кол-во инстансов)
- БД переживает ~100-300 одновременных активных коннектов до деградации
- R2DBC тоже имеет пул и тоже упирается в эту цифру

### В чём разница sync vs reactive под нагрузкой
**Старый sync (платформенные потоки)**: 200 Tomcat потоков × 8MB = 1.6GB на стек, потоки простаивают пока ждут DB. Это плохая утилизация, НО throughput упирается в пул, а не в потоки.

**Sync + virtual threads**: 100k VT × 1KB ≈ копейки памяти, ждут на пуле естественно. Пул throttles тебя на уровне БД.

**Reactive**: Не блокирует потоки, но downstream backpressure сигналит "стоп" вверх по цепочке. На уровне БД — тот же лимит пула.

### Итог
Если DB справляется с N коннектами — sync с VT и reactive дадут примерно одинаковый throughput. Reactive выигрывает только если основное время не в DB, а в межсервисных вызовах (но и тут VT с `StructuredTaskScope` догоняют).

## Где JPA реально тормозит (не из-за блокировки)

| Проблема | Что |
|---|---|
| **N+1 queries** | lazy loading коллекций → отдельный SELECT на каждого родителя |
| **Long-running session** | держит connection в пуле дольше, чем нужно |
| **Dirty checking overhead** | Hibernate проверяет все managed entities на изменения |
| **Cascade explosions** | сохранение root каскадирует на сотни детей |
| **Гнилые `@OneToMany` без `fetch=LAZY`** | EAGER подтягивает граф целиком |
| **Generated SQL не оптимален** | сложные join'ы лучше писать руками |
| **L2 cache конфликты** | invalidation в кластере, stale data |

### Чем заменить JPA когда жмёт
- **Spring Data JDBC** — простой aggregate-based, нет proxy/lazy/cache, нет сессии
- **jOOQ** — type-safe SQL builder, полный контроль, генерится из схемы
- **MyBatis** — explicit XML/annotation мапперы
- **Hibernate Reactive / R2DBC** — только если реально нужен reactive end-to-end
- **Quarkus Panache** — ActiveRecord-style upper над Hibernate, меньше boilerplate

## Pinning — JPA-специфичный нюанс с VT

Виртуальный поток "пиннится" к carrier-потоку (не может unmount) при:
- `synchronized` блоке
- `native` методе с long-running blocking IO

→ если pin'нутся все carriers (по умолч. = CPU cores), throughput падает.

Что было: Hibernate < 6.2 использовал `synchronized` в горячих путях → pinning.
Сейчас: Hibernate 6.5+ и Spring 6.1+ переписали критические места на `ReentrantLock` → проблема ушла.

Проверить pinning: `-Djdk.tracePinnedThreads=full` — логи покажут стектрейсы pin'ов.

## Native image — нужен ли?

| Сценарий | Native ОК? |
|---|---|
| Lambda / Knative / Cloud Run (cold start критичен) | да |
| Kubernetes c HPA, autoscaling реагирует за секунды | да |
| Долгоживущий pod, JIT прогревается | нет — JIT догонит и обгонит native, билд проще |
| Микросервис где важен `time-to-first-request` | да |
| Сервис с reflection-heavy либами (Hibernate, MapStruct) | боль — много hints |

Цена native: длинный билд (минуты), GraalVM hints на reflection, меньше JIT-оптимизаций.

## Специфика банка — не про фреймворк

| Тема | Решение |
|---|---|
| Распределённые транзакции | **Saga** (хореография через события, или оркестратор). НЕ 2PC — не масштабируется |
| Гарантия публикации событий | **Transactional Outbox** + Debezium / Kafka CDC |
| Идемпотентность | `Idempotency-Key` HTTP header → dedup-таблица с TTL |
| Exactly-once в Kafka | producer.idempotent + transactional consumer (read-process-write) |
| Защита от штормов / DDoS | Resilience4j RateLimiter + Bulkhead, edge rate limiting |
| Circuit breaker для downstream | Resilience4j CircuitBreaker (или Sentinel) |
| Сериализация одновременных транзакций | DB Serializable (PG SSI) + retry на `serialization_failure` |
| Tracing платежа end-to-end | OpenTelemetry, `traceparent` в HTTP + Kafka headers, MDC во все логи |
| Audit log | append-only таблица или event sourcing на критичных доменах |
| Безопасность | Spring Security + OAuth2 / JWT, mTLS между сервисами |
| Hot-reload конфига | Spring Cloud Config / Consul / Vault, `@RefreshScope` |
| Feature flags | Unleash / Flagsmith / LaunchDarkly |

## Observability stack (must-have)

| Слой | Инструмент |
|---|---|
| Metrics | Micrometer → Prometheus → Grafana |
| Tracing | OpenTelemetry (Java agent) → Tempo / Jaeger |
| Logging | structured JSON (Logback + logstash-encoder) → Loki / ELK |
| Корреляция логов | `traceId`, `spanId`, `userId`, `requestId` в MDC |
| Алертинг | Prometheus AlertManager / Grafana Alerts |
| SLO/SLI | latency p99/p99.9, error rate, saturation |
| Real-user monitoring | Sentry / Datadog для front |

## Resilience patterns — must-know

| Паттерн | Когда |
|---|---|
| **Retry с экспоненциальным backoff + jitter** | transient failures (timeout, 503) |
| **Circuit breaker** | downstream деградировал — перестать его бить |
| **Bulkhead** | изолировать пулы коннектов по downstream-у, чтобы один не положил всё |
| **Rate limiter** | защита downstream и себя |
| **Timeout** | всегда явный, не ждать вечно |
| **Fallback** | деградация: вернуть кэш / дефолт / 503 |
| **Idempotency key** | повторный POST не дублирует операцию |

## Кратко одной фразой

> "Для нового transactional сервиса в 2026 — Spring Boot 3 на Tomcat с включёнными virtual threads, ZGC generational, синхронный JPA или Spring Data JDBC. Reactive стек не выбираю — VT покрывает throughput, debug/monitoring у реактива дороже. Узкое горлышко не в синхронности, а в connection pool — оно одинаковое у sync и reactive. Reactive оставляю для streaming / SSE / WebSocket. Если cold start критичен — Quarkus с native image."

## Sources
- [Virtual Threads vs WebFlux 2026 — plus8soft](https://plus8soft.com/blog/virtual-threads-vs-webflux/)
- [Virtual Threads Just Made 10 Years of Reactive Java Pointless — Medium](https://medium.com/engineering-playbook/virtual-threads-just-made-10-years-of-reactive-java-pointless-ad7c4fa8810f)
- [Virtual Threads In Production Spring Boot — simplifiedlearningblog](https://simplifiedlearningblog.com/advanced-concurrency-with-java-virtual-threads/)
- [Spring Boot Microservices Roadmap 2026 — javaguides](https://www.javaguides.net/2025/12/spring-boot-microservices-roadmap-2026.html)
