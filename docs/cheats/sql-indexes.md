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

## Часто на собесе
- Объясни B-Tree
- Почему индекс на `LIKE '%x'` не работает
- Что такое composite index и зачем порядок столбцов
- Чем покрывающий индекс (covering / index-only) отличается от обычного
- Когда индекс не помогает (функция, cast, OR)
- Когда индекс ВРЕДИТ (write-heavy таблица, низкая селективность)
- Что такое partial index
- В чём разница между B-Tree и Hash индексом
