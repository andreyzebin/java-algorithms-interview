# SQL

## Порядок в запросе vs порядок выполнения
**Текст**: `SELECT → FROM → JOIN → WHERE → GROUP BY → HAVING → ORDER BY → LIMIT`
**Логика**: `FROM/JOIN → WHERE → GROUP BY → HAVING → SELECT → DISTINCT → ORDER BY → LIMIT`

Поэтому в WHERE нельзя ссылаться на alias из SELECT (SELECT ещё не выполнен).
Поэтому в HAVING можно агрегаты, в WHERE нельзя.

```sql
SELECT u.id, u.name, COUNT(o.id) AS order_count
FROM users u
LEFT JOIN orders o ON o.user_id = u.id
WHERE u.created_at >= '2025-01-01'
GROUP BY u.id, u.name
HAVING COUNT(o.id) > 5
ORDER BY order_count DESC
LIMIT 10;
```

## JOIN
| Тип | Что |
|---|---|
| `INNER JOIN` | только совпадения в обеих |
| `LEFT JOIN` | все из левой + совпадения справа (NULL если нет) |
| `RIGHT JOIN` | зеркало LEFT |
| `FULL OUTER JOIN` | объединение, NULL с обеих сторон |
| `CROSS JOIN` | декартово произведение |
| Self join | таблица сама с собой — иерархии (manager_id) |

### Anti-join (всё в левой, чего НЕТ в правой)
```sql
SELECT u.* FROM users u
LEFT JOIN orders o ON o.user_id = u.id
WHERE o.id IS NULL;
-- или (оптимизатор обычно делает то же):
SELECT * FROM users u WHERE NOT EXISTS (
    SELECT 1 FROM orders o WHERE o.user_id = u.id
);
```

### Semi-join (есть совпадение)
```sql
SELECT * FROM users u WHERE EXISTS (
    SELECT 1 FROM orders o WHERE o.user_id = u.id AND o.amount > 100
);
```

## Aggregation
```sql
SELECT
    COUNT(*),                          -- все строки (включая NULL в полях)
    COUNT(amount),                     -- только не-NULL
    COUNT(DISTINCT user_id),
    SUM(amount), AVG(amount),
    MIN(created_at), MAX(created_at)
FROM orders;
```
Группировка: `GROUP BY user_id`.
Фильтр по агрегату: `HAVING SUM(amount) > 1000`.

`GROUP BY` без агрегатов = `DISTINCT`.

## Window functions (не сворачивают строки!)
```sql
SELECT
    user_id, order_date, amount,
    ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY order_date)         AS rn,
    RANK()       OVER (PARTITION BY user_id ORDER BY amount DESC)        AS rk,
    DENSE_RANK() OVER (PARTITION BY user_id ORDER BY amount DESC)        AS drk,
    SUM(amount)  OVER (PARTITION BY user_id ORDER BY order_date
                       ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running,
    LAG(amount, 1)  OVER (PARTITION BY user_id ORDER BY order_date)      AS prev_amt,
    LEAD(amount, 1) OVER (PARTITION BY user_id ORDER BY order_date)      AS next_amt,
    AVG(amount)  OVER (PARTITION BY user_id
                       ORDER BY order_date
                       ROWS BETWEEN 2 PRECEDING AND CURRENT ROW)         AS ma3
FROM orders;
```

- `ROW_NUMBER` — уникальные 1,2,3... (без дублей даже при равенстве)
- `RANK` — 1,2,2,4 (пропускает после ties)
- `DENSE_RANK` — 1,2,2,3 (не пропускает)

## CTE (Common Table Expression)
```sql
WITH recent AS (
    SELECT * FROM orders WHERE created_at > NOW() - INTERVAL '7 days'
),
agg AS (
    SELECT user_id, SUM(amount) AS total FROM recent GROUP BY user_id
)
SELECT u.name, a.total FROM users u JOIN agg a ON a.user_id = u.id;
```

### Рекурсивный CTE — иерархии/графы
```sql
WITH RECURSIVE tree AS (
    -- база (anchor)
    SELECT id, parent_id, name, 1 AS depth
    FROM categories WHERE parent_id IS NULL

    UNION ALL

    -- шаг (recursive)
    SELECT c.id, c.parent_id, c.name, t.depth + 1
    FROM categories c
    JOIN tree t ON c.parent_id = t.id
)
SELECT * FROM tree;
```

## Top-N per group
```sql
-- самый дорогой заказ каждого пользователя
SELECT * FROM (
    SELECT *,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY amount DESC) AS rn
    FROM orders
) t WHERE rn = 1;
```

## UPSERT
**PostgreSQL**:
```sql
INSERT INTO users(id, name) VALUES (1, 'Andrey')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;
-- EXCLUDED — proposed row, который не вставился
```
**MySQL**:
```sql
INSERT INTO users(id, name) VALUES (1, 'Andrey')
ON DUPLICATE KEY UPDATE name = VALUES(name);
```

## INSERT / UPDATE / DELETE с возвратом (PostgreSQL)
```sql
INSERT INTO ... RETURNING id, created_at;
UPDATE users SET name=$1 WHERE id=$2 RETURNING *;
DELETE FROM users WHERE id=$1 RETURNING id;
```

## Транзакции
```sql
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;            -- или ROLLBACK
```
Изоляция:
```sql
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
```

## Полезные идиомы
```sql
-- NULL-safe
COALESCE(x, 0)                            -- замена NULL на default
NULLIF(x, 0)                              -- x если x != 0 иначе NULL
CASE WHEN x>0 THEN 'pos' WHEN x<0 THEN 'neg' ELSE 'zero' END

-- строки (Postgres)
'a' || ' ' || 'b'                         -- конкат
LOWER, UPPER, TRIM, LENGTH
SUBSTRING(s FROM 1 FOR 3)                 -- первые 3 символа
SPLIT_PART('a,b,c', ',', 2)               -- 'b'
REGEXP_REPLACE(s, '\d+', '#')

-- даты (Postgres)
NOW(), CURRENT_DATE, CURRENT_TIMESTAMP
date_trunc('month', x)                    -- 2025-05-28 → 2025-05-01
EXTRACT(YEAR FROM x)
x + INTERVAL '1 day'
AGE(now(), birth_date)

-- сортировка с NULL
ORDER BY x ASC NULLS LAST
ORDER BY x DESC NULLS FIRST
```

## Group filtering pattern
```sql
-- пользователи у которых ВСЕ заказы > 100
SELECT user_id FROM orders
GROUP BY user_id
HAVING MIN(amount) > 100;

-- пользователи у которых ХОТЯ БЫ один заказ > 100
SELECT DISTINCT user_id FROM orders WHERE amount > 100;
```

## Что важно понимать
- Разница INNER / LEFT / RIGHT / FULL JOIN
- Чем отличается WHERE от HAVING
- Чем GROUP BY от DISTINCT
- Что делает оконная функция, чем отличается от group by
- ROW_NUMBER vs RANK vs DENSE_RANK
- Как написать рекурсивный CTE для иерархии
- Как реализовать UPSERT
- Как реализовать pagination для большой таблицы (keyset, не OFFSET)
