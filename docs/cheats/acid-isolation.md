# ACID & Уровни изоляции

## ACID
- **A**tomicity — транзакция как целое: либо все изменения, либо ни одно. Через WAL / redo log + rollback.
- **C**onsistency — после транзакции constraints (PK, FK, CHECK, UNIQUE) валидны. Совместная ответственность БД и приложения.
- **I**solation — параллельные транзакции не видят промежуточного состояния друг друга. Регулируется уровнями.
- **D**urability — закоммиченное переживает crash. fsync журнала на диск.

## Аномалии параллельности

| Аномалия | Что произошло |
|---|---|
| **Dirty Read** | T2 прочитал данные T1 до её commit. T1 откатилась — T2 видел фантом. |
| **Non-Repeatable Read** | T1 читает строку; T2 коммитит UPDATE; T1 читает ту же строку — другое значение. |
| **Phantom Read** | T1 делает `SELECT WHERE x>5`; T2 коммитит INSERT подходящей строки; T1 повторяет — появилась новая строка. |
| **Lost Update** | T1 reads balance=100; T2 reads 100; T1 writes 150; T2 writes 200 — изменения T1 потерялись. |
| **Write Skew** | Каждая T читает что-то, проверяет инвариант, пишет на основе прочитанного. По отдельности OK, вместе ломают инвариант. Пример: "хотя бы 1 врач on-call". Двое смотрят "есть ещё один кроме меня — могу уйти", оба уходят. |

## Уровни изоляции (SQL standard)

| Уровень | Dirty | Non-Repeatable | Phantom | Write Skew |
|---|---|---|---|---|
| **Read Uncommitted** | возможно | возможно | возможно | возможно |
| **Read Committed** | НЕТ | возможно | возможно | возможно |
| **Repeatable Read** | НЕТ | НЕТ | возможно¹ | возможно |
| **Serializable** | НЕТ | НЕТ | НЕТ | НЕТ |

¹ В PostgreSQL Repeatable Read блокирует phantom через snapshot isolation. Но Write Skew всё ещё возможен.

## Дефолты СУБД
- **PostgreSQL** — Read Committed
- **MySQL InnoDB** — Repeatable Read
- **Oracle** — Read Committed (нет Read Uncommitted; нет истинного Repeatable Read — есть Serializable через SSI)
- **SQL Server** — Read Committed

## Установка
```sql
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
BEGIN;
...
COMMIT;
```
В JDBC:
```java
conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
```

## MVCC (Multi-Version Concurrency Control)
PostgreSQL, Oracle, MySQL InnoDB.

- Каждая транзакция работает с **snapshot** данных
- UPDATE создаёт новую версию строки (старая видна старым транзакциям)
- **Read не блокирует write, write не блокирует read** — главная фишка
- В PG `VACUUM` чистит мёртвые версии; в Oracle — undo segments

Cost: версии занимают место (table bloat), нужен VACUUM. Repeatable Read почти бесплатен (просто берёшь старый snapshot).

## Locks
- **Shared (S)** — для чтения. Несколько S могут сосуществовать.
- **Exclusive (X)** — для записи. Несовместим ни с чем (включая S).
- **Intention locks (IS, IX)** — на уровне таблицы, говорят что есть row-level S/X
- Гранулярность: **Row** < **Page** < **Table**

### Эксплицитные блокировки
```sql
SELECT * FROM t WHERE id = 1 FOR UPDATE;         -- row X lock до конца транзакции
SELECT * FROM t WHERE id = 1 FOR UPDATE NOWAIT;  -- не ждать, упасть если занят
SELECT * FROM t WHERE id = 1 FOR UPDATE SKIP LOCKED;  -- пропустить занятые (job queue pattern)
SELECT * FROM t WHERE id = 1 FOR SHARE;          -- row S lock
LOCK TABLE t IN EXCLUSIVE MODE;
```

## Deadlocks
Две транзакции ждут блокировки друг друга по кругу.
- БД детектит и **убивает одну** (rollback с error)
- Профилактика: брать блокировки в одном порядке во всех транзакциях
- Уменьшать длительность транзакций
- Использовать FOR UPDATE с правильным ORDER BY

В PG: `deadlock_timeout` (default 1s) — как долго ждать, прежде чем проверить deadlock.

## Serializable в PostgreSQL — SSI
Postgres НЕ использует pessimistic locking для Serializable.
Вместо — snapshot + detector serialization conflicts (SSI).
Если конфликт детектится → `serialization_failure` (SQLSTATE 40001).
**Приложение должно делать retry** на этой ошибке.

## Optimistic vs Pessimistic concurrency

**Pessimistic** — взять lock и держать:
```sql
SELECT * FROM accounts WHERE id = 1 FOR UPDATE;
-- работаем с уверенностью что никто не изменит
UPDATE accounts SET balance = ... WHERE id = 1;
```

**Optimistic** — версионирование:
```sql
-- читаем: SELECT id, balance, version FROM accounts WHERE id = 1
-- → version = 7
UPDATE accounts
SET balance = ?, version = version + 1
WHERE id = 1 AND version = 7;
-- если 0 rows affected — кто-то опередил, retry
```
Hibernate `@Version` делает это автоматически.

## Локи в PG — практика
```sql
-- что заблокировано прямо сейчас
SELECT * FROM pg_locks WHERE NOT granted;

-- кто кого ждёт
SELECT pid, locktype, mode, relation::regclass, page, tuple,
       transactionid, virtualtransaction, granted
FROM pg_locks ORDER BY pid;

-- активные запросы и блокировки
SELECT pg_blocking_pids(pid), * FROM pg_stat_activity WHERE state <> 'idle';

-- убить запрос
SELECT pg_cancel_backend(pid);          -- мягко
SELECT pg_terminate_backend(pid);        -- жёстко
```

## Что выбрать
- **Read Committed** — default для большинства задач, дешёвый
- **Repeatable Read** — отчёты, нужна consistency на длинной read-транзакции
- **Serializable** — финансы, инварианты между строками (write skew), либо явные блокировки FOR UPDATE
- **Read Uncommitted** — почти никогда. Только если плевать на корректность.

## Реальные рецепты
- Списание баланса с проверкой → `SELECT ... FOR UPDATE` + проверка + `UPDATE`
- Job queue → `SELECT ... FOR UPDATE SKIP LOCKED LIMIT 1`
- Optimistic для UI: `version` колонка, retry на conflict
- Идемпотентность критична → ключ дедупликации на стороне приложения

## Часто на собесе
- Расшифруй ACID
- Какие аномалии знаешь, что такое phantom read
- Уровни изоляции и какие аномалии каждый предотвращает
- Что такое MVCC, чем хорош (read не блокирует write)
- Что такое deadlock, как с ним бороться
- Что такое оптимистическая и пессимистическая блокировка
- Расскажи про `SELECT FOR UPDATE`
- Чем отличается дефолт MySQL и Postgres (RR vs RC)
- Когда возникает Lost Update и как предотвратить
