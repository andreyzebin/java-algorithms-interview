# OutOfMemoryError

## Виды OOM (по тексту сообщения)

| Сообщение | Что | Что делать |
|---|---|---|
| `Java heap space` | объекты не помещаются в heap | -Xmx, искать leak |
| `GC overhead limit exceeded` | GC >98% времени, освобождает <2% | leak / heap слишком мал |
| `Metaspace` | загружено слишком много классов | `-XX:MaxMetaspaceSize`, classloader leak |
| `Direct buffer memory` | NIO direct buffers не освобождаются | `-XX:MaxDirectMemorySize`, проверь netty/etc |
| `unable to create new native thread` | OS-лимит потоков (`ulimit -u`), НЕ heap | thread pool sizes, ulimit |
| `Requested array size exceeds VM limit` | массив > `Integer.MAX_VALUE-2` элементов | редко, бага |
| `Out of swap space?` | физпамяти не хватает | не JVM проблема — OS |
| `Compressed class space` | переполнен compressed klass space | `-XX:CompressedClassSpaceSize` |
| `Cannot reserve enough native memory` | mmap/malloc неудача | native leak (JNI, off-heap) |

## Получить heap dump

**Автоматически при OOM**:
```
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/path/heap.hprof
```

**По требованию**:
```bash
jcmd <pid> GC.heap_dump /path/heap.hprof
# или
jmap -dump:format=b,file=heap.hprof <pid>
# только live (после GC, меньше шума)
jmap -dump:live,format=b,file=heap.hprof <pid>
```

## Анализ — Eclipse MAT
Главные отчёты:
- **Leak Suspects Report** — авто-анализ, кандидаты в утечки
- **Dominator Tree** — кто держит память. "Если удалить X, освободится Y". Корни наверху.
- **Histogram** — все классы, количество и размер (`Shallow Heap` — сам объект, `Retained Heap` — что он удерживает)
- **Path to GC Roots** — почему объект не собирается? Кто на него ссылается?
- **OQL** — SQL-like по объектам:
  ```sql
  SELECT * FROM java.util.HashMap WHERE size > 1000
  SELECT s FROM java.lang.String s WHERE s.value.@length > 1024
  ```

## Типичные leak-источники

1. **Статические коллекции** — кладём, забываем чистить
2. **ThreadLocal в thread pool** — поток живёт долго, ThreadLocal не очищается. Лекарство: `try/finally remove()`
3. **Listeners / callbacks** без unregister
4. **ClassLoader leaks** — особенно в Tomcat при reload: web-app классы не выгружаются, удерживают весь classloader
5. **Кэши без TTL/size limit** — заменить на Caffeine с `maximumSize` / `expireAfterWrite`
6. **`String.intern`** в больших количествах (до 7 — PermGen)
7. **`MappedByteBuffer`** / **`DirectByteBuffer`** не закрывается, держит файл
8. **Lambda capturing this** — внутренний класс держит outer
9. **Inner class** (non-static) держит ссылку на outer
10. **Connection / Statement** не закрыты → JDBC leak
11. **JDK 11+: Cleaner-зависимые ресурсы** — забыли `close()` 

## Allocation profiling
В JFR / async-profiler смотри **alloc events** — кто аллоцирует больше всего:
```bash
./asprof -d 60 -e alloc -f alloc.html <pid>
```
В JMC: вкладка "TLAB Allocations Outside TLABs" — крупные аллокации мимо TLAB.

## GC log signs of leak
```
Old gen после Full GC растёт от цикла к циклу → leak
Pause time увеличивается со временем → leak или фрагментация
```

## Чек-лист расследования OOM
1. **Снять heap dump** (или дождаться авто-дампа). В production — `jcmd GC.heap_dump` без рестарта.
2. **Открыть в MAT** → Leak Suspects сразу скажет основных кандидатов.
3. **Dominator Tree** → топ-10 кто удерживает память.
4. Для подозреваемого: **Path to GC Roots → exclude weak/soft** — кто держит strong reference.
5. **GC log** включить, посмотреть тренд Old gen.
6. **JFR с allocation profile** — где растут.
7. Воспроизвести в staging, проверить fix.

## Профилактика
- Bounded thread pools, очереди с лимитом
- Кэши с TTL / size limit (Caffeine)
- `try-with-resources` для всего AutoCloseable
- ThreadLocal `remove()` в finally
- WeakHashMap / WeakReference для caches которые не должны удерживать

## Часто на собесе
- Что такое heap dump, чем снимать
- Какие виды OOM знаешь
- Что такое GC overhead limit
- Чем отличается leak от high allocation rate (rate ↑ — много мусора, но GC справляется; leak — Old gen растёт)
- Что такое ClassLoader leak и почему он типичен в Tomcat
- Как анализировать heap dump в MAT — dominator tree, path to GC roots
