# Modern Lightweight Java Frameworks (2026)

## Главное противопоставление
**Runtime reflection-heavy** (классический Spring) ⟷ **compile-time / native-first** (Quarkus, Micronaut, Helidon).

## Расклад

| Framework | Кто | Фишка |
|---|---|---|
| **Spring Boot 4** | Broadcom (ex-Pivotal) | де-факто стандарт; AOT и Native через Spring Native |
| **Quarkus 3** | Red Hat | "supersonic, subatomic Java"; build-time оптимизации; ~12ms native cold start |
| **Micronaut 4** | Object Computing | compile-time DI/AOP БЕЗ reflection; GraalVM-first; самый быстрый JVM-старт |
| **Helidon 4 Níma** | Oracle | переписан с нуля на virtual threads; никакого Netty/reactive |
| **Vert.x 5** | Eclipse | event-loop reactive; high-throughput non-blocking |
| **Javalin** | community | микро, "library not framework", no magic |
| **Ktor** | JetBrains | Kotlin-first, корутины |
| **Dropwizard** | Coda Hale | bundle проверенных либ (Jersey + Jetty + Jackson + Metrics) |

## Performance ballpark (2026)

| Метрика | Spring Boot | Quarkus | Micronaut | Helidon Níma |
|---|---|---|---|---|
| JVM cold start | ~2s | ~1s | <700ms | ~800ms |
| Native cold start | ~100ms | ~12-50ms | ~50ms | ~50ms |
| Память (native) | ~80MB | ~30MB | ~35MB | ~35MB |
| Image size | 250-400MB → 80MB native | ~30-50MB | ~30-50MB | ~50MB |

## Три главные темы

### 1. Virtual threads → конец reactive-эры
- Project Loom (Java 21+) — миллион тонких потоков на маленьком carrier pool
- **Helidon 4 Níma** переписан с Netty/reactive на VT. Каждый реквест в своём VT, синхронный код, тот же throughput
- Новые проекты отказываются от Spring WebFlux / RxJava / Mutiny — VT решает ту же проблему C10k
- Тейк "WebFlux всё ещё нужен в 2026" — спорный

### 2. Native image — стандарт для serverless
- GraalVM Native Image: 3-4s → <100ms cold start, 250MB → 30MB memory
- AWS Lambda Java + native = **10× улучшение cold start**, реальная экономия $$$
- Spring Boot 3+ официально поддерживает (`spring-aot`, `native` profile)
- Quarkus/Micronaut спроектированы под это с нуля
- Цена: длинный build (минуты), reflection требует hints

### 3. Build-time DI vs runtime reflection
- Spring делает DI/AOP **в runtime** через reflection → startup overhead, reflection metadata
- Micronaut и Quarkus генерируют код DI/AOP **во время компиляции** (annotation processors)
- → быстрее старт, меньше памяти, нативная компиляция работает из коробки

## Как это формулировать

- *"Если сервис под Lambda / Knative — Quarkus или Micronaut, native image из коробки"*
- *"Helidon Níma — интересный кейс: переписан с Netty на virtual threads, синхронный код без reactive overhead"*
- *"Spring Boot 3.2+ всерьёз догоняет — официальный GraalVM + AOT + поддержка virtual threads"*
- *"WebFlux был ответом на C10k, но с Loom это решается синхронным кодом — проще читать и дебажить, тот же throughput"*
- *"Quarkus dev mode с live reload — лучшее DX в Java сейчас, hot replace без рестарта"*
- *"Build-time DI у Micronaut → reflection не нужен → нативная компиляция работает без hints"*

## Бонусы — звучат современно

- **Spring Boot 4** (2025) — `RestClient` (замена RestTemplate), virtual threads on Tomcat, улучшения AOT
- **Quarkus Dev Services** — testcontainers автоматически поднимаются в dev mode
- **Micronaut Data** — compile-time JDBC/JPA репозитории, без runtime proxy
- **Helidon SE vs MP** — SE imperative+functional, MP реализует MicroProfile
- **JTE** / **Jstachio** — современные compile-time template engines (альтернатива Thymeleaf)
- **Project Reactor / Mutiny** — реактивные либы, но с Loom их влияние снижается
- **MicroProfile** — стандарт для микросервисов на Jakarta EE (Config, Health, Metrics, Telemetry)

## Где какой выбрать

| Сценарий | Выбор |
|---|---|
| Enterprise, большая команда, много либ — экосистема | Spring Boot |
| Serverless (Lambda, Knative), важен cold start | Quarkus / Micronaut |
| Cloud-native + хочется virtual threads без legacy | Helidon 4 Níma |
| Reactive, очень high throughput | Vert.x |
| Микро-API, без магии | Javalin |
| Kotlin-проект | Ktor |

## Sources
- [Micronaut vs Quarkus vs Spring 2026 Shootout — HackerNoon](https://hackernoon.com/micronaut-vs-quarkus-vs-spring-the-2026-java-framework-shootout)
- [Java Frameworks 2026: Spring Boot 4 vs Quarkus 3.10 vs Micronaut 4.5 — DEV](https://dev.to/johalputt/java-frameworks-2026-spring-boot-4-vs-quarkus-310-vs-micronaut-45-141j)
- [Helidon 4 vs Quarkus 3 vs Micronaut 4: Virtual Threads — JavaCodeGeeks](https://www.javacodegeeks.com/2026/03/helidon-4-vs-quarkus-3-vs-micronaut-4-which-framework-actually-winswith-virtual-threads.html)
- [Helidon 4 Adopts Virtual Threads — InfoQ](https://www.infoq.com/articles/helidon-4-adopts-virtual-threads/)
- [GraalVM Native Image — Java's Answer to Rust's Startup Speed](https://www.javacodegeeks.com/2026/02/graalvm-native-image-javas-answer-to-rusts-startup-speed.html)
- [The best Java microframeworks to learn now — InfoWorld](https://www.infoworld.com/article/4066620/the-best-java-microframeworks-to-learn-now.html)
