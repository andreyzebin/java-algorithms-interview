# Modern Java & JVM (9 → 25)

## Java 9 (2017)
- **JPMS / Project Jigsaw** — модули, `module-info.java`, `requires`, `exports`, `opens`. Сильная инкапсуляция.
- **JShell** — REPL (`jshell` в командной строке)
- **Collection factory methods**: `List.of(...)`, `Set.of(...)`, `Map.of(k,v,...)` — immutable
- **Stream API**: `takeWhile`, `dropWhile`, `iterate(seed, hasNext, next)`, `ofNullable`
- **Collectors.toUnmodifiableList()`
- **Optional**: `ifPresentOrElse`, `or`, `stream`
- **Map.entry(k, v)`
- **Compact strings** — Latin-1 строки занимают 1 байт на символ (если можно)
- **G1 — default GC**

## Java 10 (2018)
- **`var`** — type inference локальных переменных
  ```java
  var list = new ArrayList<String>();      // ArrayList<String>
  var entries = map.entrySet();
  for (var e : entries) ...
  ```
- Application CDS

## Java 11 LTS (2018)
- **HttpClient** — HTTP/2, async, WebSocket. Замена `HttpURLConnection`.
  ```java
  HttpClient.newHttpClient()
      .sendAsync(HttpRequest.newBuilder(URI.create("...")).build(), BodyHandlers.ofString())
      .thenApply(HttpResponse::body);
  ```
- **String**: `isBlank()`, `strip()` (Unicode-aware, ≠ `trim()`), `repeat(n)`, `lines()`
- **`var` в lambda parameters**: `(@NonNull var x) -> ...`
- Single-file source: `java Hello.java`
- Удалены Java EE и CORBA модули
- **Epsilon GC** (no-op)
- **ZGC** (экспериментальный)

## Java 14 (2020)
- **Switch expressions** (GA)
  ```java
  String name = switch (day) {
      case MON, TUE, WED, THU, FRI -> "weekday";
      case SAT, SUN -> "weekend";
  };
  // ещё с yield для блока:
  int x = switch (n) {
      case 0 -> 0;
      default -> { var v = compute(n); yield v * 2; }
  };
  ```
- **Helpful NullPointerExceptions** (`-XX:+ShowCodeDetailsInExceptionMessages`) — говорит какая именно ссылка null
- **Records** (preview)

## Java 15 (2020)
- **Text blocks** (GA)
  ```java
  String json = """
          { "name": "Andrey" }
          """;
  ```
- **Sealed classes** (preview)
- **Pattern matching for instanceof** (preview)
- **ZGC**, **Shenandoah** — production-ready

## Java 16 (2021)
- **Records** (GA)
  ```java
  record Point(int x, int y) {}
  // авто: ctor, accessor p.x() / p.y(), equals, hashCode, toString
  // final, не наследуется, поля final
  ```
- **Pattern matching for instanceof** (GA)
  ```java
  if (obj instanceof String s && !s.isBlank()) {
      // s — String, scope ограничен ветвью
  }
  ```

## Java 17 LTS (2021)
- **Sealed classes/interfaces** (GA)
  ```java
  sealed interface Shape permits Circle, Square, Triangle {}
  // только перечисленные классы могут implements
  // подкласс: final | sealed | non-sealed
  ```
- Удалён Applet API
- Foreign Function & Memory API (incubator)

## Java 19-20 (2022-23)
- **Virtual threads** (preview)
- **Pattern matching for switch** (preview)
- **Record patterns** (preview)

## Java 21 LTS (2023)
- **Virtual threads** (GA, **Project Loom**)
  ```java
  Thread.startVirtualThread(() -> doWork());

  try (var ex = Executors.newVirtualThreadPerTaskExecutor()) {
      for (var task : tasks) ex.submit(task);
  }  // ex.close() ждёт завершения
  ```
  Миллионы лёгких потоков на маленьком пуле платформенных. Continue blocking-style без reactive.
- **Pattern matching for switch** (GA)
  ```java
  return switch (shape) {
      case Circle c    -> "circle r=" + c.radius();
      case Square s    -> "square " + s.side();
      case Triangle t  -> "triangle";
      case null        -> "nothing";
  };
  ```
- **Sequenced collections** — `getFirst()`, `getLast()`, `addFirst()`, `addLast()`, `reversed()`. Унифицированный API для упорядоченных коллекций.
- **Generational ZGC** (`-XX:+UseZGC` теперь с поколениями)
- **Record patterns** (preview)
- **String templates** (preview)

## Java 23-25
- **Record patterns** (GA)
  ```java
  if (obj instanceof Point(int x, int y) && x > 0) { /* x,y in scope */ }
  ```
- **Unnamed variables**: `_` для неиспользуемых параметров
  ```java
  try { ... } catch (Exception _) { ... }
  map.forEach((_, v) -> System.out.println(v));
  ```
- **Scoped values** (preview/GA) — иммутабельная альтернатива `ThreadLocal` для виртуальных потоков
- **Structured concurrency** (preview)
  ```java
  try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      var f1 = scope.fork(() -> fetchA());
      var f2 = scope.fork(() -> fetchB());
      scope.join().throwIfFailed();
      return new Result(f1.get(), f2.get());
  }
  ```
- **String templates** — переработаны (был preview, отозвали, переделывают)
- **Compact source files и instance main methods** (preview):
  ```java
  void main() {
      println("hello");        // без class HelloWorld { public static void main(...) }
  }
  ```
- **Class-Data Sharing (AOT)** — быстрый старт (для serverless / cli)

## Современные JVM-проекты

### Project Loom — доставлено
Virtual threads, structured concurrency, scoped values.
- VT шедулятся JVM на маленьком пуле платформенных потоков (carrier threads)
- Блокирующий IO (НЕ holds the carrier) — Carrier свободен, VT парк-ается
- НЕ подходит: synchronized (pin), JNI, длинные CPU-bound задачи

### Project Valhalla (в работе)
- **Value classes / primitive classes** — объекты без identity, без объектного заголовка
- Цель: убрать boxing, дать `List<int>` без обёртки
- Inline classes, null-restricted types

### Project Panama — доставлено
- **Foreign Function & Memory API** (GA в 22) — современная замена JNI и `Unsafe`
- Прямая работа с нативной памятью и вызов нативных функций без C-кода

### Project Leyden — в работе
- AOT (ahead-of-time) компиляция, CDS улучшения
- Быстрый старт (serverless, CLI tools)

### GC
- **ZGC** — generational с 21, паузы <1ms на TB-heap
- **Shenandoah** — низкие паузы (Red Hat)
- **G1** — default, доработки в каждом релизе

## LTS-релизы
- Java 8 (2014, поддержка до 2030+) — legacy
- Java 11 (2018, до 2026)
- Java 17 (2021, до 2029)
- Java 21 (2023, до 2031)
- Java 25 (2025, до 2033)

## Что в собес-разговоре звучит современно
- "Используем records для DTO"
- "Sealed-иерархия + pattern matching switch — type-safe state machine без visitor"
- "Виртуальные потоки — отказались от Reactor / WebFlux в новых сервисах"
- "Generational ZGC на 21 — паузы <1ms, забыли про tuning GC"
- "Pattern matching снимает boilerplate с instanceof"
- "Text blocks вместо склейки JSON-строк"
