# Query Plan Optimization

## EXPLAIN — читаем план
```sql
EXPLAIN SELECT ...;                  -- предсказание, без выполнения
EXPLAIN ANALYZE SELECT ...;          -- + реальное выполнение (запрос ВЫПОЛНЯЕТСЯ!)
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) SELECT ...;
EXPLAIN (ANALYZE, COSTS, BUFFERS, TIMING) SELECT ...;
```
В MySQL: `EXPLAIN ANALYZE` тоже есть с 8.0; `EXPLAIN FORMAT=JSON`.

## Что читать в плане (PostgreSQL)

### Scan методы
| Тип | Когда | Хорошо? |
|---|---|---|
| **Seq Scan** | full table scan | плохо для больших таблиц с маленькой выборкой; ОК если возвращаем большую долю |
| **Index Scan** | walk B-Tree, потом heap для каждого матча | ОК для маленькой выборки |
| **Index Only Scan** | всё нужное в индексе, без обращения к heap | отлично |
| **Bitmap Index Scan** | собирает биты совпадающих row-id | первая стадия Bitmap Heap |
| **Bitmap Heap Scan** | сортированно читает heap по битам | ОК для среднего объёма |
| **TID Scan** | по `ctid` (внутренний row-id) | редко |

### Join методы
| Тип | Когда оптимизатор выбирает |
|---|---|
| **Nested Loop** | одна сторона маленькая + индекс по join-ключу на большой |
| **Hash Join** | одна сторона помещается в hash-таблицу (work_mem); equality joins |
| **Merge Join** | обе стороны отсортированы по join-ключу (например, через индексы) |

### Sort
- **Sort** — explicit sort (если `ORDER BY` и нет индекса по нужному порядку)
- **external merge** в плане → work_mem мал, пишет на диск — медленно
- Решение: увеличить work_mem, либо индекс по ORDER BY

## ANALYZE / Statistics
Оптимизатор работает по статистике распределения данных.
```sql
ANALYZE my_table;                  -- обновить статистику
ANALYZE VERBOSE my_table;
```
Автоматический ANALYZE есть, но при больших batch-изменениях полезно вручную.

В плане смотри: `rows=N` (оценка) vs `actual rows=N` (реально). Если расходятся в 10x+ → статистика устарела или нерепрезентативна (нужен `CREATE STATISTICS` для коррелированных колонок).

## Типичные проблемы

### 1. N+1
SELECT для родителей, потом по запросу для каждого ребёнка → N+1 запросов.
**Решение**: один JOIN, либо batch-fetch (IN-список), либо `JOIN FETCH` в JPA.

### 2. OFFSET pagination — медленно при больших OFFSET
```sql
... LIMIT 20 OFFSET 100000;  -- БД сканирует 100020 строк
```
**Решение** — keyset pagination:
```sql
... WHERE (created_at, id) < (:last_created, :last_id)
ORDER BY created_at DESC, id DESC LIMIT 20;
```

### 3. SELECT *
- Тянет лишние колонки (TOAST в Postgres — большие поля)
- Ломает Index-Only Scan
- Хрупко к изменениям схемы

### 4. Implicit cast
```sql
WHERE id = '5';   -- если id INTEGER → возможно cast колонки, не использует индекс
```
**Решение**: соблюдать типы в коде (используй prepared statement с правильным типом).

### 5. Функция от индексируемой колонки
```sql
WHERE DATE(created_at) = '2025-01-01';
WHERE UPPER(email) = 'X';
```
**Решение** — переписать диапазоном или функциональный индекс:
```sql
WHERE created_at >= '2025-01-01' AND created_at < '2025-01-02';
-- или
CREATE INDEX ON users(LOWER(email));
WHERE LOWER(email) = 'x';
```

### 6. OR на разных колонках
```sql
WHERE x = 1 OR y = 2;        -- часто seq scan
```
**Решение**: `UNION ALL` двух частей (каждая использует свой индекс):
```sql
SELECT * FROM t WHERE x = 1
UNION ALL
SELECT * FROM t WHERE y = 2 AND x <> 1;
```

### 7. Слишком много JOIN
Оптимизатор может выбрать плохой порядок (search space взрывается).
- `SET join_collapse_limit = 12` — увеличить
- В Postgres нет hints — переписать через CTE
- В Oracle/MySQL — есть hints (`/*+ USE_INDEX(...) */`)

### 8. Параметр sniffing / generic plan
При prepared statement Postgres может построить generic plan по статистике, который плох для конкретных значений.
- `SET plan_cache_mode = force_custom_plan;` — пересчитывать каждый раз
- Иногда добавить hint: `OFFSET 0` обманывает оптимизатор не объединять подзапрос

## DISTINCT vs GROUP BY
Обычно эквивалентны:
```sql
SELECT DISTINCT user_id FROM orders;
SELECT user_id FROM orders GROUP BY user_id;
```
GROUP BY обычно через hash; DISTINCT может тоже. План скажет.

## EXISTS vs IN vs JOIN
- `EXISTS (SELECT 1 FROM x WHERE ...)` — проверка наличия, semi-join
- `IN (SELECT x FROM ...)` — для маленьких списков; в больших — NULL-семантика отличается!
- `JOIN ... GROUP BY` — для агрегаций

Современные оптимизаторы часто эквивалентны на простых случаях. EXISTS обычно безопасный default.

## Materialized Views
Закэшированный результат запроса в реальной таблице:
```sql
CREATE MATERIALIZED VIEW user_stats AS SELECT ...;
REFRESH MATERIALIZED VIEW [CONCURRENTLY] user_stats;
```
Для дорогой аналитики, обновляемой по cron.

## Партиционирование
Большая таблица бьётся на партиции по range / list / hash.
- Запрос с фильтром по партиционирующему ключу → читается только нужная партиция (**partition pruning**)
- Управление: drop старой партиции = моментально, без VACUUM
- В PG: `PARTITION BY RANGE (created_at)` + декларативные партиции

## Connection pooling
Открытие connection дорого. Используй pool: **HikariCP** (Java), **PgBouncer** (proxy).

## Чек-лист "почему медленно"
1. `EXPLAIN ANALYZE` запроса
2. Реальные vs ожидаемые rows → если расходятся, `ANALYZE`
3. Seq Scan на большой таблице → нужен индекс или нет статистики
4. Nested Loop с большой outer → нужен индекс на inner, или Hash Join
5. Sort на диске → work_mem
6. Index Scan но всё равно медленно → BUFFERS, IO-bound
7. Длительность не на сканах, а на других нодах → проверить функции в SELECT, агрегации

## На что смотреть в плане в первую очередь (red flags)

1. **`rows` (estimate) vs `actual rows`** расходятся в 10×+ → статистика устарела (`ANALYZE`) или коррелированные колонки (`CREATE STATISTICS`). Оптимизатор строит плохой план на плохой оценке.
2. **`Seq Scan` на большой таблице** с селективным `WHERE` → нет индекса / не используется.
3. **`Rows Removed by Filter: N`** большое → читаем много, выбрасываем много — нужен более точный индекс.
4. **`Heap Fetches: N`** в Index Only Scan большое → visibility map устарела, нужен VACUUM (см. sql-indexes).
5. **`Sort Method: external merge Disk: NkB`** → сортировка не влезла в `work_mem`, ушла на диск.
6. **`Nested Loop` с большим числом итераций** (loops=N большое) → нужен Hash/Merge Join или индекс на inner.
7. **`Buffers: shared read=N`** большое (а не `hit`) → холодный кэш / IO-bound, данные не в `shared_buffers`.
8. **Время сосредоточено в одной ноде** (`actual time` этой ноды ≫ детей) → вот узкое место.
9. **`(never executed)`** ветки → планировщик их отбросил (норм, но проверь не из-за плохой оценки).

`actual time=START..END` — START это время до первой строки, END до последней. `loops` умножает.

## Виды search / scan — шпаргалка
- **Seq Scan** — линейно вся таблица. Хорош только если берём большую долю строк.
- **Index Scan** — спуск по B-Tree + heap fetch на каждую строку (random IO).
- **Index Only Scan** — всё в индексе + visibility map, без heap (или почти). Быстрее всех на узкой выборке.
- **Bitmap Index Scan → Bitmap Heap Scan** — собирает все ctid в битмап, потом читает heap **по порядку страниц** (sequential, не random). Для средней доли строк и для комбинирования нескольких индексов (`BitmapAnd`/`BitmapOr`).
- **Index Scan Backward** — для `ORDER BY ... DESC` по тому же индексу.

## Часто на собесе
- Что показывает EXPLAIN, как читать план
- Чем отличается Nested Loop / Hash Join / Merge Join
- Что такое seq scan vs index scan
- Что такое N+1 и как решить
- Почему OFFSET плох для пагинации, как сделать keyset
- Когда optimizer выбирает full scan вместо индекса (низкая selectivity, > ~10% строк)
- Как ускорить count(*) на огромной таблице (приблизительный count из pg_class.reltuples)
