# SQL Indexes

## Зачем
Без индекса = full table scan O(n).
С индексом:
- **B-Tree**: O(log n) для equality и range
- **Hash**: O(1) только для equality

**Trade-off**: индекс ускоряет SELECT, замедляет INSERT/UPDATE/DELETE (нужно обновить и индекс). Занимает место (10-30% от данных типично).

## Типы индексов

### B-Tree (default везде)
Сбалансированное дерево, отсортированные ключи.
Поддерживает: `=`, `<`, `>`, `<=`, `>=`, `BETWEEN`, `IN`, `LIKE 'prefix%'`, `ORDER BY`.
НЕ поддерживает: `LIKE '%suffix'`, `LIKE '%mid%'`, регекспы.

### Hash (Postgres: USING HASH)
Только `=`. Быстрее B-Tree на equality, но не работает для диапазонов и сортировки.

### Bitmap (Oracle)
Для столбцов с малой кардинальностью (пол, статус). Эффективен для `AND`/`OR`/`NOT` над несколькими.
Не подходит для часто обновляемых данных.

### GIN / GiST (PostgreSQL)
- **GIN** — multi-value: full-text (`tsvector`), `JSONB`, массивы, trigram (`pg_trgm` — для LIKE '%x%')
- **GiST** — geometric (PostGIS), range types, full-text

### BRIN (PostgreSQL)
Block Range Index — для огромных таблиц, физически отсортированных (например, по timestamp).
Очень маленький, неточный, но почти бесплатный.

### SP-GiST
Для несбалансированных деревьев — quadtree, k-d tree, radix.

## Composite Indexes — ПОРЯДОК ВАЖЕН

```sql
CREATE INDEX idx ON orders(user_id, created_at, status);
```

**Левый префикс правило** — индекс работает для:
- `WHERE user_id = ?`                                   ✓
- `WHERE user_id = ? AND created_at = ?`                 ✓
- `WHERE user_id = ? AND created_at > ?`                 ✓
- `WHERE user_id = ? AND created_at = ? AND status = ?`  ✓
- `WHERE created_at = ?`                                 ✗ (нет user_id)
- `WHERE user_id = ? AND status = ?`                     частично (использует только user_id)

**Правило построения**:
1. equality columns (`=`) первыми
2. потом range column (`<`, `>`, `BETWEEN`) — только одно работает эффективно
3. потом sort columns (`ORDER BY`)

## Covering Index (INCLUDE)
Индекс с дополнительными полями для index-only scan (не идём в heap):
```sql
CREATE INDEX idx ON orders(user_id) INCLUDE (amount, status);
-- SELECT amount, status FROM orders WHERE user_id = ? — без обращения к таблице
```

## Partial Index
Только часть строк:
```sql
CREATE INDEX idx ON orders(user_id) WHERE status = 'pending';
-- Маленький; работает только если WHERE содержит то же условие
```
Хорошо для:
- "горячих" подмножеств (active users, pending orders)
- Уникальности под условием: `UNIQUE INDEX ON users(email) WHERE deleted_at IS NULL`

## Функциональный индекс (expression index)
```sql
CREATE INDEX ON users(LOWER(email));
-- теперь WHERE LOWER(email) = 'x@y.com' использует индекс
```

## Unique Index
```sql
CREATE UNIQUE INDEX ON users(email);
-- PRIMARY KEY и UNIQUE constraint автоматически создают unique index
```

## Когда индекс НЕ помогает

```sql
WHERE UPPER(email) = 'X@Y'         -- функция на колонке (без functional index)
WHERE created_at::date = '2025'    -- cast на колонке
WHERE email LIKE '%@gmail.com'     -- leading wildcard
WHERE user_id::text = '5'          -- type mismatch (implicit cast)
WHERE status != 'active'           -- негация часто не использует
WHERE x = 1 OR y = 2               -- OR на разных колонках (нужны индексы на обе + bitmap or)
WHERE x IS NULL                    -- зависит от СУБД, в PG работает
```

Исправления:
- Функция → создать functional index
- Cast → исправить тип/убрать cast
- LIKE '%x%' → `pg_trgm` GIN index
- OR → переписать через `UNION ALL`

## Selectivity
Индекс полезен когда отсекает много строк. Bool с 50% true — индекс бесполезен, full scan дешевле.

Грубое правило: если запрос возвращает >10% таблицы — оптимизатор может игнорировать индекс. Лучше seq scan + filter.

## Cost of indexes
- INSERT/UPDATE/DELETE на индексируемое поле = +обновление индекса
- Дисковое место
- При большом количестве индексов на таблице оптимизатор тратит время на выбор плана
- WAL/redo log пишет больше

## Найти проблемы (PostgreSQL)
```sql
-- какие индексы используются
SELECT * FROM pg_stat_user_indexes ORDER BY idx_scan DESC;

-- мёртвые индексы (можно дропнуть)
SELECT schemaname, relname, indexrelname
FROM pg_stat_user_indexes WHERE idx_scan = 0;

-- размер индексов
SELECT indexrelname, pg_size_pretty(pg_relation_size(indexrelid)) AS size
FROM pg_stat_user_indexes ORDER BY pg_relation_size(indexrelid) DESC LIMIT 20;

-- дубликаты индексов
SELECT indrelid::regclass, array_agg(indexrelid::regclass)
FROM pg_index GROUP BY indrelid, indkey HAVING COUNT(*) > 1;
```

## REINDEX / VACUUM
- B-Tree фрагментируется → `REINDEX [CONCURRENTLY] INDEX idx` (PG умеет без блокировки)
- `VACUUM` чистит dead tuples, не блокирует читы. `VACUUM FULL` блокирует, переписывает таблицу.
- `ANALYZE` — обновляет статистику для оптимизатора

## Index-Only Scan глубоко (PostgreSQL)

### Как вообще работает индексный поиск
B-Tree хранит **(ключ → ctid)**, где `ctid` = физический адрес строки в heap (номер страницы, смещение).
Обычный **Index Scan**: пройти дерево → получить список ctid → **для каждого сходить в heap** (random IO) → прочитать строку.
То есть 2 шага: индекс + heap fetch. Heap fetch — самое дорогое (random IO).

### Index-Only Scan — пропускаем heap
Если **всё нужное в SELECT есть в самом индексе**, heap читать не надо:
```sql
CREATE INDEX idx_score ON events (score);              -- или (score) INCLUDE (id)
SELECT score FROM events WHERE score > 1000;           -- только score → index-only
SELECT id, score FROM events WHERE score > 1000;       -- нужен id → covering: (score) INCLUDE (id)
```
В плане: `Index Only Scan using idx_score` + строка `Heap Fetches: N`.

### Пример: миллиард строк, нужно score > x
```sql
CREATE INDEX idx_score ON events (score) INCLUDE (id, created_at);
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, score, created_at FROM events WHERE score > 999000 ORDER BY score;
```
- B-Tree отсортирован по `score` → находит границу `score > 999000` бинарным спуском (O(log n)), дальше идёт **по листьям подряд** (range scan)
- `ORDER BY score` бесплатен — индекс уже в этом порядке
- `INCLUDE (id, created_at)` → всё в индексе → **Heap Fetches: 0**
- Из миллиарда строк прочитаются только подходящие листья индекса, не вся таблица

### ⚠️ Ловушка MVCC — почему index-only иногда всё равно лезет в heap
Индекс в PostgreSQL **не хранит информацию о видимости** (какая транзакция создала/удалила версию строки). Видимость лежит в heap (`xmin`/`xmax` в заголовке строки).

Чтобы index-only НЕ ходил в heap, PostgreSQL смотрит на **Visibility Map (VM)** — битмап, где бит выставлен, если на странице heap **все строки видимы всем** транзакциям. Бит ставит `VACUUM`.

- Страница в VM как all-visible → видимость не проверяем → heap fetch не нужен ✅
- Страница НЕ в VM (были недавние write/update) → **придётся сходить в heap** проверить `xmin/xmax` → `Heap Fetches` растёт, скорость падает ❌

### Что это значит под нагрузкой write / в транзакциях
- **UPDATE строки** в PG = новая версия строки (MVCC), часто на другой странице → страница теряет all-visible бит → index-only по этой странице начинает делать heap fetches
- Активно меняющаяся таблица без своевременного `VACUUM` → VM устаревает → "index-only" фактически работает как обычный index scan (медленно)
- Долгая транзакция держит старый snapshot → `VACUUM` не может пометить страницы all-visible (старые версии ещё нужны) → деградация index-only по всей таблице
- Лечение: **агрессивный autovacuum** на горячих таблицах (`autovacuum_vacuum_scale_factor` ниже), не держать длинные транзакции, мониторить `Heap Fetches` в плане

### HOT-update — смягчает проблему
Если UPDATE **не меняет индексируемые колонки** и новая версия влезает на ту же страницу → **HOT (Heap-Only Tuple)**: индекс НЕ обновляется, цепочка версий внутри страницы. Меньше распухание индекса, проще vacuum. Поэтому: не индексируй часто меняющиеся колонки без нужды.

### Проверки
```sql
-- видно ли index-only и сколько heap fetches
EXPLAIN (ANALYZE, BUFFERS) SELECT score FROM events WHERE score > 1000;
-- доля all-visible страниц (чем ближе к relpages — тем лучше для index-only)
SELECT relname, relpages, relallvisible FROM pg_class WHERE relname = 'events';
-- когда последний autovacuum
SELECT relname, last_autovacuum, n_dead_tup FROM pg_stat_user_tables WHERE relname='events';
```

## Часто на собесе
- Объясни B-Tree
- Как работает index-only scan и почему он может всё равно лезть в heap (visibility map, MVCC)
- Что происходит с index-only scan на write-heavy таблице
- Почему индекс на `LIKE '%x'` не работает
- Что такое composite index и зачем порядок столбцов
- Чем покрывающий индекс (covering / index-only) отличается от обычного
- Когда индекс не помогает (функция, cast, OR)
- Когда индекс ВРЕДИТ (write-heavy таблица, низкая селективность)
- Что такое partial index
- В чём разница между B-Tree и Hash индексом
