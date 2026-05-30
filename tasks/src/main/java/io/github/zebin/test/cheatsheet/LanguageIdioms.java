package io.github.zebin.test.cheatsheet;

import java.util.*;

/**
 * Конспект №1 — ЯЗЫКОВЫЕ ИДИОМЫ: «как написать X в Java».
 * То, что нужно вспомнить мгновенно, пока пишешь код.
 * Запуск всего: ./gradlew :tasks:cheat
 */
public class LanguageIdioms {

    public static void run() {
        arrays();
        strings();
        numbers();
        bitManipulation();
        mathUtils();
        collections();
        iteration();
        customSort();
        gotchas();
    }

    // ====================== ARRAYS ======================
    static void arrays() {
        section("ARRAYS");

        // литерал
        int[] a = {1, 2, 3, 4, 5};
        int[][] grid = {{1, 2, 3}, {4, 5, 6}};

        // зарезервировать
        int[] zeros = new int[5];                          // [0,0,0,0,0]
        int[] filled = new int[5]; Arrays.fill(filled, -1);
        int[][] grid2 = new int[3][4];                     // 3x4 нулей

        // длина / доступ
        int n = a.length;
        int first = a[0], last = a[n - 1];

        // копия
        int[] copy = a.clone();
        int[] copy2 = Arrays.copyOf(a, a.length);
        int[] slice = Arrays.copyOfRange(a, 1, 4);         // [1, 4) -> {2,3,4}
        int[] dst = new int[5];
        System.arraycopy(a, 0, dst, 0, a.length);

        // сортировка
        int[] s = a.clone(); Arrays.sort(s);               // только asc для примитивов
        Integer[] boxed = {3, 1, 2};
        Arrays.sort(boxed, Comparator.reverseOrder());     // desc через Integer[]

        // развернуть (in-place, two pointers)
        int[] r = a.clone();
        for (int i = 0, j = r.length - 1; i < j; i++, j--) {
            int t = r[i]; r[i] = r[j]; r[j] = t;
        }

        // массив -> список
        List<Integer> list = new ArrayList<>();
        for (int x : a) list.add(x);

        // список -> int[]
        int[] back = list.stream().mapToInt(Integer::intValue).toArray();

        // печать
        System.out.println("array: " + Arrays.toString(a));
        System.out.println("reversed: " + Arrays.toString(r));
        System.out.println("grid: " + Arrays.deepToString(grid));
    }

    // ====================== STRINGS ======================
    static void strings() {
        section("STRINGS");

        String s = "hello world";
        int len = s.length();
        char c = s.charAt(0);                              // 'h'

        // подстрока [from, to)
        String sub = s.substring(0, 5);                    // "hello"
        String tail = s.substring(6);                      // "world"

        // String <-> char[]
        char[] chars = s.toCharArray();
        String back = new String(chars);
        String backRange = new String(chars, 0, 5);        // первые 5

        // развернуть
        String rev = new StringBuilder(s).reverse().toString();

        // сборка по char (mutable!)
        StringBuilder sb = new StringBuilder();
        sb.append('a').append("bc").append(42);            // "abc42"
        sb.setCharAt(0, 'A');                              // "Abc42"
        sb.deleteCharAt(sb.length() - 1);                  // "Abc4"
        sb.insert(0, '_');                                 // "_Abc4"
        String result = sb.toString();

        // сравнение
        boolean eq = "abc".equals("abc");                  // НЕ ==
        boolean ic = "ABC".equalsIgnoreCase("abc");
        int cmp = "a".compareTo("b");                      // <0

        // поиск
        boolean has = s.contains("world");
        int idx = s.indexOf("world");                      // -1 если нет
        boolean starts = s.startsWith("hello");

        // split / join
        String[] parts = "a,b,c".split(",");
        String joined = String.join("-", parts);           // "a-b-c"

        // классификация символов
        boolean isD = Character.isDigit('5');
        boolean isL = Character.isLetter('a');
        boolean isLD = Character.isLetterOrDigit('a');
        char lower = Character.toLowerCase('A');
        int digit = Character.digit('7', 10);              // 7
        int fastDigit = '7' - '0';                          // 7 (быстрый трюк)

        // счётчик букв a-z (классический "фингерпринт")
        int[] count = new int[26];
        for (char ch : "banana".toCharArray()) count[ch - 'a']++;

        System.out.println("reversed: " + rev);
        System.out.println("sub: " + sub + ", tail: " + tail);
        System.out.println("counts(banana): " + Arrays.toString(count));
        System.out.println("built: " + result);
    }

    // ====================== NUMBERS ======================
    static void numbers() {
        section("NUMBERS");

        int x = Integer.parseInt("42");
        long y = Long.parseLong("9999999999");
        int hex = Integer.parseInt("ff", 16);              // 255
        String bin = Integer.toBinaryString(13);           // "1101"
        String hexStr = Integer.toHexString(255);          // "ff"

        int max = Math.max(3, 5), min = Math.min(3, 5);
        int abs = Math.abs(-7);
        int pow = (int) Math.pow(2, 10);                   // 1024
        double sqrt = Math.sqrt(2);

        // деление и остаток
        int q = 7 / 2;                                      // 3
        int rem = 7 % 2;                                    // 1
        // mod для отрицательных: ((a % n) + n) % n

        int INF = Integer.MAX_VALUE;
        int NINF = Integer.MIN_VALUE;

        System.out.println("hex ff = " + hex + ", bin 13 = " + bin);
    }

    // ====================== BIT MANIPULATION ======================
    static void bitManipulation() {
        section("BIT MANIPULATION");

        int x = 0b1101;                                    // 13 в двоичном литерале

        // базовые операции
        int and  = x & 6;
        int or   = x | 2;
        int xor  = x ^ 5;
        int not  = ~x;
        int shl  = x << 1;                                 // *2
        int shr  = x >> 1;                                 // /2 (со знаком)
        int ushr = x >>> 1;                                // /2 без знака

        // классические трюки
        boolean isOdd = (x & 1) == 1;
        int dropLowest = x & (x - 1);                      // снять младший единичный бит
        boolean isPow2 = x > 0 && (x & (x - 1)) == 0;      // степень 2?
        int lowestBit = x & (-x);                          // выделить младший единичный бит

        // встроенные подсчёты
        int pop = Integer.bitCount(13);                    // 3
        int lz  = Integer.numberOfLeadingZeros(13);        // 28
        int tz  = Integer.numberOfTrailingZeros(8);        // 3

        // set/clear/check/toggle i-го бита
        int n = 0;
        n |= (1 << 3);                                     // set
        boolean has = (n & (1 << 3)) != 0;                 // check
        n &= ~(1 << 3);                                    // clear
        n ^= (1 << 3);                                     // toggle

        // XOR-фокус: в массиве все парные кроме одного
        int[] arr = {1, 2, 1, 3, 2};
        int single = 0;
        for (int v : arr) single ^= v;                     // = 3

        // обход всех битовых масок из n бит (для subset-DP)
        int nn = 3;
        List<String> masks = new ArrayList<>();
        for (int mask = 0; mask < (1 << nn); mask++) masks.add(Integer.toBinaryString(mask));

        System.out.println("popcount(13): " + pop);
        System.out.println("single number: " + single);
        System.out.println("isPow2(13): " + isPow2 + ", isPow2(16): " + (16 > 0 && (16 & 15) == 0));
        System.out.println("3-bit masks: " + masks);
    }

    // ====================== MATH UTILS ======================
    static void mathUtils() {
        section("MATH UTILS");

        // GCD (алгоритм Евклида)
        int g = gcd(12, 18);                               // 6
        // LCM через GCD: a / gcd * b (порядок важен, чтобы не переполнить)
        long l = (long) 12 / gcd(12, 18) * 18;             // 36

        // решето Эратосфена
        int N = 30;
        boolean[] isPrime = new boolean[N + 1];
        Arrays.fill(isPrime, true);
        isPrime[0] = isPrime[1] = false;
        for (int i = 2; i * i <= N; i++) {
            if (!isPrime[i]) continue;
            for (int j = i * i; j <= N; j += i) isPrime[j] = false;
        }
        List<Integer> primes = new ArrayList<>();
        for (int i = 2; i <= N; i++) if (isPrime[i]) primes.add(i);

        // модулярная арифметика
        final int MOD = 1_000_000_007;
        long prod = (long) 1_234_567 * 7_654_321 % MOD;    // приведение к long ОБЯЗАТЕЛЬНО
        long pow = powMod(2, 20, MOD);                     // быстрое возведение в степень

        System.out.println("gcd(12,18)=" + g + ", lcm=" + l);
        System.out.println("primes up to 30: " + primes);
        System.out.println("1234567*7654321 mod = " + prod);
        System.out.println("2^20 mod = " + pow);
    }

    static int gcd(int a, int b) {
        while (b != 0) { int t = a % b; a = b; b = t; }
        return a;
    }

    static long powMod(long base, long exp, long mod) {
        long res = 1;
        base %= mod;
        while (exp > 0) {
            if ((exp & 1) == 1) res = res * base % mod;
            base = base * base % mod;
            exp >>= 1;
        }
        return res;
    }

    // ====================== COLLECTIONS ======================
    static void collections() {
        section("COLLECTIONS");

        // List
        List<Integer> list = new ArrayList<>();
        list.add(1); list.add(2); list.add(3);
        list.get(0); list.set(0, 99);
        list.remove(0);                                    // ВНИМАНИЕ: remove(int) — по ИНДЕКСУ
        list.remove(Integer.valueOf(99));                  // по значению
        int size = list.size();
        boolean has = list.contains(2);
        List<Integer> fixed = List.of(1, 2, 3);            // immutable

        // Map
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        int v = map.getOrDefault("b", 0);
        map.putIfAbsent("a", 999);                         // не перезапишет
        map.merge("a", 1, Integer::sum);                   // map["a"] += 1
        map.computeIfAbsent("list", k -> 42);
        for (var e : map.entrySet()) {                     // e.getKey(), e.getValue()
            // ...
        }

        // Set
        Set<Integer> set = new HashSet<>();
        set.add(1); set.contains(1); set.remove(1);
        Set<Integer> sorted = new TreeSet<>();             // отсортированный

        // Deque как Stack (Stack устарел!)
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(1); stack.push(2);                      // вершина = 2
        stack.peek();                                       // 2 (не удаляет)
        stack.pop();                                        // 2 (удаляет)

        // Deque как Queue
        Deque<Integer> queue = new ArrayDeque<>();
        queue.offer(1); queue.offer(2);                    // голова = 1
        queue.peek();                                       // 1
        queue.poll();                                       // 1

        // PriorityQueue (min-heap по умолчанию)
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
        minHeap.offer(3); minHeap.offer(1); minHeap.offer(2);
        int top = minHeap.peek();                          // 1

        // TreeMap — отсортированная мапа
        TreeMap<Integer, String> tm = new TreeMap<>();
        tm.put(1, "a"); tm.put(5, "b"); tm.put(3, "c");
        Integer floor = tm.floorKey(4);                    // <= 4 -> 3
        Integer ceil = tm.ceilingKey(4);                   // >= 4 -> 5
        tm.firstKey(); tm.lastKey();

        System.out.println("map: " + map);
        System.out.println("minHeap peek: " + top);
        System.out.println("tm floor(4): " + floor + ", ceil(4): " + ceil);
    }

    // ====================== ITERATION / SORT ======================
    static void iteration() {
        section("ITERATION & SORT");

        int[] a = {5, 2, 8, 1};

        // for-each (без индекса)
        for (int x : a) { /* x */ }

        // обычный for (с индексом)
        for (int i = 0; i < a.length; i++) { /* a[i] */ }

        // 2D обход
        int[][] g = {{1, 2}, {3, 4}};
        for (int r = 0; r < g.length; r++)
            for (int col = 0; col < g[0].length; col++) { /* g[r][col] */ }

        // 4 соседа в сетке
        int[][] DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        // sort с компаратором (для объектов!)
        Integer[] boxed = {5, 2, 8, 1};
        Arrays.sort(boxed, Comparator.reverseOrder());     // desc
        Arrays.sort(boxed, (p, q) -> Integer.compare(p, q)); // НЕ p - q (переполнение)

        // sort массивов по полю
        int[][] intervals = {{1, 3}, {2, 4}, {0, 1}};
        Arrays.sort(intervals, (p, q) -> Integer.compare(p[0], q[0]));

        // List сортировка
        List<Integer> list = new ArrayList<>(List.of(3, 1, 2));
        Collections.sort(list);
        list.sort(Comparator.reverseOrder());

        // streams: топ полезного
        int sum = Arrays.stream(a).sum();
        int amax = Arrays.stream(a).max().getAsInt();
        int[] sortedArr = Arrays.stream(a).sorted().toArray();
        List<Integer> evens = Arrays.stream(a).boxed().filter(x -> x % 2 == 0).toList();

        System.out.println("sum: " + sum + ", max: " + amax);
        System.out.println("intervals by start: " + Arrays.deepToString(intervals));
        System.out.println("evens: " + evens);
    }

    // ====================== CUSTOM SORT ======================
    static void customSort() {
        section("CUSTOM SORT");

        // 1) Comparable<T> — естественный порядок класса
        List<Point> pts = new ArrayList<>(List.of(
                new Point(3, 4), new Point(1, 2), new Point(3, 1)));
        Collections.sort(pts);                             // использует compareTo

        // 2) Comparator на лету
        pts.sort(Comparator.comparingInt((Point p) -> p.x)
                .thenComparingInt(p -> p.y));
        pts.sort(Comparator.comparingInt((Point p) -> p.x).reversed());

        // 3) Сортировка int[][] по двум полям
        int[][] arr = {{1, 3}, {1, 1}, {2, 5}, {0, 9}};
        Arrays.sort(arr, (a, b) -> {
            if (a[0] != b[0]) return Integer.compare(a[0], b[0]);
            return Integer.compare(a[1], b[1]);
        });
        // эквивалентно:
        // Arrays.sort(arr, Comparator.<int[]>comparingInt(a -> a[0]).thenComparingInt(a -> a[1]));

        // 4) Строки: сначала по длине, потом лексикографически
        List<String> words = new ArrayList<>(List.of("bb", "a", "cccc", "dd"));
        words.sort(Comparator.<String>comparingInt(String::length)
                .thenComparing(Comparator.naturalOrder()));

        System.out.println("points (by x desc): " + pts);
        System.out.println("intervals: " + Arrays.deepToString(arr));
        System.out.println("words by len: " + words);
    }

    static class Point implements Comparable<Point> {
        final int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
        @Override public int compareTo(Point o) {
            if (x != o.x) return Integer.compare(x, o.x);
            return Integer.compare(y, o.y);
        }
        @Override public String toString() { return "(" + x + "," + y + ")"; }
    }

    // ====================== ГОТЧИ ======================
    static void gotchas() {
        section("GOTCHAS");

        // 1) == vs equals для String. Всегда equals.
        // 2) list.remove(int) — по индексу, list.remove(Integer.valueOf(x)) — по значению.
        // 3) Переполнение в (lo + hi) / 2 -> lo + (hi - lo) / 2.
        // 4) Stack/Vector устарели -> ArrayDeque / ArrayList.
        // 5) Math.abs(Integer.MIN_VALUE) ПЕРЕПОЛНЯЕТСЯ. Береги.
        // 6) Arrays.asList(int[]) даёт List<int[]> длины 1. Для List<Integer>:
        //    Arrays.stream(arr).boxed().toList()
        // 7) (a, b) -> a - b в компараторе ломается на больших числах -> Integer.compare(a, b).
        // 8) HashMap не сохраняет порядок. Нужен порядок вставки — LinkedHashMap.
        // 9) Integer кэшируется только в [-128, 127]. Для сравнения Integer всегда .equals() или .intValue().
        // 10) Стрим можно проитерировать ОДИН раз.

        System.out.println("прочитай комментарии в этой секции");
    }

    private static void section(String name) {
        System.out.println();
        System.out.println("===== " + name + " =====");
    }
}
