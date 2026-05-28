package io.github.zebin.test.cheatsheet;

import java.util.*;

/**
 * Шпаргалка идиом Java для собеса. Запуск: ./gradlew cheat
 * Каждая секция — отдельный метод, можно открыть и подсмотреть синтаксис.
 */
public class Cheatsheet {

    public static void main(String[] args) {
        arrays();
        matrices();
        strings();
        numbers();
        bitManipulation();
        mathUtils();
        collections();
        topK();
        iteration();
        customSort();
        greedyIntervals();
        twoPointers();
        slidingWindow();
        prefixSums();
        monotonicStack();
        binarySearch();
        bfsDfs();
        unionFind();
        linkedList();
        trie();
        backtracking();
        dp();
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

    // ====================== MATRIX / 2D ======================
    static void matrices() {
        section("MATRIX / 2D");

        // объявление
        int[][] m = {{1, 2, 3},
                     {4, 5, 6},
                     {7, 8, 9}};

        int[][] empty = new int[3][4];                     // 3x4 нулей
        int[][] jagged = new int[3][];                     // "зубчатый": ряды разной длины
        jagged[0] = new int[]{1};
        jagged[1] = new int[]{1, 2};
        jagged[2] = new int[]{1, 2, 3};

        // размеры
        int rows = m.length;
        int cols = m[0].length;                            // у обычной прямоугольной

        // доступ: m[row][col] — сначала строка, потом столбец
        int v = m[1][2];                                   // 6

        // глубокая копия (clone() копирует только верхний массив ссылок!)
        int[][] shallow = m.clone();                       // НЕ копия, ссылается на те же строки
        int[][] deep = new int[rows][];
        for (int i = 0; i < rows; i++) deep[i] = m[i].clone();

        // обход (4 направления — DIRS — самый удобный шаблон)
        int[][] DIRS4 = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        int[][] DIRS8 = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};

        // транспонирование (квадратная, in-place)
        int[][] t = deepCopy(m);
        for (int i = 0; i < rows; i++)
            for (int j = i + 1; j < cols; j++) {
                int tmp = t[i][j]; t[i][j] = t[j][i]; t[j][i] = tmp;
            }

        // поворот на 90° по часовой = транспонировать + развернуть каждую строку
        int[][] rot = deepCopy(m);
        for (int i = 0; i < rows; i++)
            for (int j = i + 1; j < cols; j++) {
                int tmp = rot[i][j]; rot[i][j] = rot[j][i]; rot[j][i] = tmp;
            }
        for (int[] row : rot) {
            for (int i = 0, j = row.length - 1; i < j; i++, j--) {
                int tmp = row[i]; row[i] = row[j]; row[j] = tmp;
            }
        }

        // спиральный обход (часто на собесах)
        List<Integer> spiral = spiral(m);

        // суммы строк / столбцов
        int rowSum1 = 0; for (int x : m[1]) rowSum1 += x;
        int colSum1 = 0; for (int i = 0; i < rows; i++) colSum1 += m[i][1];

        // диагонали (квадратная)
        int diag = 0, anti = 0;
        for (int i = 0; i < rows; i++) { diag += m[i][i]; anti += m[i][rows - 1 - i]; }

        // поиск в отсортированной по строкам И столбцам матрице (углом)
        // [[1,4,7],[2,5,8],[3,6,9]] — начинаем с правого-верхнего
        int target = 5;
        int r = 0, c = cols - 1;
        boolean found = false;
        while (r < rows && c >= 0) {
            if (m[r][c] == target) { found = true; break; }
            else if (m[r][c] > target) c--;
            else r++;
        }

        // подсчёт островов (DFS, mutates grid)
        int[][] island = {{1,1,0,0},
                          {1,0,0,1},
                          {0,0,1,1},
                          {0,0,0,0}};
        int count = countIslands(deepCopy(island));

        // префиксные суммы 2D: prefix[i+1][j+1] = sum(m[0..i][0..j])
        int[][] pref = new int[rows + 1][cols + 1];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                pref[i+1][j+1] = m[i][j] + pref[i][j+1] + pref[i+1][j] - pref[i][j];
        // сумма прямоугольника [r1..r2][c1..c2] (включительно):
        // pref[r2+1][c2+1] - pref[r1][c2+1] - pref[r2+1][c1] + pref[r1][c1]
        int rectSum = pref[2+1][2+1] - pref[1][2+1] - pref[2+1][1] + pref[1][1];

        // печать
        System.out.println("matrix:");        printMatrix(m);
        System.out.println("transposed:");    printMatrix(t);
        System.out.println("rotated 90° CW:"); printMatrix(rot);
        System.out.println("spiral: " + spiral);
        System.out.println("diag = " + diag + ", anti = " + anti);
        System.out.println("found 5 (corner search): " + found);
        System.out.println("islands count: " + count);
        System.out.println("rect sum m[1..2][1..2] = " + rectSum);
    }

    static int[][] deepCopy(int[][] a) {
        int[][] r = new int[a.length][];
        for (int i = 0; i < a.length; i++) r[i] = a[i].clone();
        return r;
    }

    static void printMatrix(int[][] a) {
        for (int[] row : a) System.out.println("  " + Arrays.toString(row));
    }

    static List<Integer> spiral(int[][] m) {
        List<Integer> out = new ArrayList<>();
        int top = 0, bot = m.length - 1, left = 0, right = m[0].length - 1;
        while (top <= bot && left <= right) {
            for (int c = left; c <= right; c++) out.add(m[top][c]);          // →
            top++;
            for (int r = top; r <= bot; r++) out.add(m[r][right]);           // ↓
            right--;
            if (top <= bot) {
                for (int c = right; c >= left; c--) out.add(m[bot][c]);      // ←
                bot--;
            }
            if (left <= right) {
                for (int r = bot; r >= top; r--) out.add(m[r][left]);        // ↑
                left++;
            }
        }
        return out;
    }

    static int countIslands(int[][] g) {
        int cnt = 0;
        for (int i = 0; i < g.length; i++)
            for (int j = 0; j < g[0].length; j++)
                if (g[i][j] == 1) { sinkIsland(g, i, j); cnt++; }
        return cnt;
    }

    static void sinkIsland(int[][] g, int i, int j) {
        if (i < 0 || j < 0 || i >= g.length || j >= g[0].length || g[i][j] != 1) return;
        g[i][j] = 0;                                       // "топим" — пометили
        sinkIsland(g, i - 1, j);
        sinkIsland(g, i + 1, j);
        sinkIsland(g, i, j - 1);
        sinkIsland(g, i, j + 1);
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

    // ====================== TWO POINTERS ======================
    static void twoPointers() {
        section("TWO POINTERS");

        // палиндром
        String s = "racecar";
        boolean pal = true;
        for (int i = 0, j = s.length() - 1; i < j; i++, j--) {
            if (s.charAt(i) != s.charAt(j)) { pal = false; break; }
        }

        // two-sum на отсортированном массиве
        int[] arr = {1, 2, 4, 7, 11, 15};
        int target = 15;
        int l = 0, r = arr.length - 1;
        int[] ans = null;
        while (l < r) {
            int s2 = arr[l] + arr[r];
            if (s2 == target) { ans = new int[]{l, r}; break; }
            else if (s2 < target) l++;
            else r--;
        }

        System.out.println("palindrome racecar: " + pal);
        System.out.println("two-sum=15 indices: " + Arrays.toString(ans));
    }

    // ====================== SLIDING WINDOW ======================
    static void slidingWindow() {
        section("SLIDING WINDOW");

        // макс. сумма подмассива длины k
        int[] a = {2, 1, 5, 1, 3, 2};
        int k = 3;
        int sum = 0;
        for (int i = 0; i < k; i++) sum += a[i];
        int best = sum;
        for (int i = k; i < a.length; i++) {
            sum += a[i] - a[i - k];                        // окно сдвинулось
            best = Math.max(best, sum);
        }

        // длиннейшая подстрока без повторов
        String s = "abcabcbb";
        Map<Character, Integer> lastSeen = new HashMap<>();
        int lo = 0, longest = 0;
        for (int hi = 0; hi < s.length(); hi++) {
            char ch = s.charAt(hi);
            if (lastSeen.containsKey(ch) && lastSeen.get(ch) >= lo) {
                lo = lastSeen.get(ch) + 1;
            }
            lastSeen.put(ch, hi);
            longest = Math.max(longest, hi - lo + 1);
        }

        System.out.println("max sum k=3: " + best);
        System.out.println("longest unique substr: " + longest);
    }

    // ====================== BINARY SEARCH ======================
    static void binarySearch() {
        section("BINARY SEARCH");

        int[] a = {1, 3, 5, 7, 9, 11};
        int target = 7;

        // точное совпадение
        int lo = 0, hi = a.length - 1, found = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;                  // против переполнения!
            if (a[mid] == target) { found = mid; break; }
            else if (a[mid] < target) lo = mid + 1;
            else hi = mid - 1;
        }

        // lower bound: индекс первого элемента >= target
        int t = 6;
        int lb = 0, ub = a.length;                         // полуинтервал [lb, ub)
        while (lb < ub) {
            int mid = lb + (ub - lb) / 2;
            if (a[mid] < t) lb = mid + 1;
            else ub = mid;
        }
        // если lb == a.length — таких нет

        // встроенное (для отсортированного массива)
        int builtin = Arrays.binarySearch(a, 7);           // если нет — возвращает -(insertion+1)

        System.out.println("found 7 at: " + found);
        System.out.println("lower_bound(6): " + lb);
    }

    // ====================== BFS / DFS ======================
    static void bfsDfs() {
        section("BFS / DFS");

        // граф как adjacency list
        Map<Integer, List<Integer>> g = new HashMap<>();
        int[][] edges = {{1, 2}, {1, 3}, {2, 4}, {3, 4}, {4, 5}};
        for (int[] e : edges) {
            g.computeIfAbsent(e[0], k -> new ArrayList<>()).add(e[1]);
            g.computeIfAbsent(e[1], k -> new ArrayList<>()).add(e[0]);
        }

        // BFS
        int start = 1;
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        List<Integer> bfsOrder = new ArrayList<>();
        queue.offer(start); visited.add(start);
        while (!queue.isEmpty()) {
            int node = queue.poll();
            bfsOrder.add(node);
            for (int next : g.getOrDefault(node, List.of())) {
                if (visited.add(next)) queue.offer(next);  // add возвращает false если уже было
            }
        }

        // DFS (рекурсивный)
        Set<Integer> seen = new HashSet<>();
        List<Integer> dfsOrder = new ArrayList<>();
        dfs(g, start, seen, dfsOrder);

        // BFS по сетке — расстояния в шагах
        int[][] grid = {{0, 0, 0}, {1, 1, 0}, {0, 0, 0}};
        int[][] dist = bfsGrid(grid, 0, 0);

        // Дерево + обход inorder
        TreeNode root = new TreeNode(1,
                new TreeNode(2, new TreeNode(4), null),
                new TreeNode(3));
        List<Integer> inorder = new ArrayList<>();
        inorder(root, inorder);

        System.out.println("BFS order from 1: " + bfsOrder);
        System.out.println("DFS order from 1: " + dfsOrder);
        System.out.println("inorder: " + inorder);
        System.out.println("grid dist:");
        for (int[] row : dist) System.out.println("  " + Arrays.toString(row));
    }

    static void dfs(Map<Integer, List<Integer>> g, int v, Set<Integer> seen, List<Integer> out) {
        if (!seen.add(v)) return;
        out.add(v);
        for (int u : g.getOrDefault(v, List.of())) dfs(g, u, seen, out);
    }

    static int[][] bfsGrid(int[][] grid, int sr, int sc) {
        int m = grid.length, n = grid[0].length;
        int[][] d = new int[m][n];
        for (int[] row : d) Arrays.fill(row, -1);
        int[][] DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        Deque<int[]> q = new ArrayDeque<>();
        q.offer(new int[]{sr, sc}); d[sr][sc] = 0;
        while (!q.isEmpty()) {
            int[] p = q.poll();
            for (int[] dxy : DIRS) {
                int r = p[0] + dxy[0], c = p[1] + dxy[1];
                if (r < 0 || r >= m || c < 0 || c >= n) continue;
                if (grid[r][c] == 1 || d[r][c] != -1) continue;
                d[r][c] = d[p[0]][p[1]] + 1;
                q.offer(new int[]{r, c});
            }
        }
        return d;
    }

    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int v) { this.val = v; }
        TreeNode(int v, TreeNode l, TreeNode r) { val = v; left = l; right = r; }
    }

    static void inorder(TreeNode node, List<Integer> out) {
        if (node == null) return;
        inorder(node.left, out);
        out.add(node.val);
        inorder(node.right, out);
    }

    // ====================== LINKED LIST ======================
    static void linkedList() {
        section("LINKED LIST");

        // собрать 1 -> 2 -> 3
        ListNode head = new ListNode(1);
        head.next = new ListNode(2);
        head.next.next = new ListNode(3);

        // развернуть
        ListNode rev = reverseList(head);

        // дамп
        List<Integer> dump = new ArrayList<>();
        for (ListNode n = rev; n != null; n = n.next) dump.add(n.val);

        // быстрый/медленный (середина списка)
        ListNode slow = rev, fast = rev;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        // slow указывает на середину

        // dummy head — удобный приём при удалении/вставке
        ListNode dummy = new ListNode(0);
        dummy.next = rev;
        // ... манипуляции ...
        ListNode result = dummy.next;

        System.out.println("reversed list: " + dump);
        System.out.println("middle val: " + slow.val);
    }

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int v) { this.val = v; }
    }

    static ListNode reverseList(ListNode head) {
        ListNode prev = null, cur = head;
        while (cur != null) {
            ListNode next = cur.next;
            cur.next = prev;
            prev = cur;
            cur = next;
        }
        return prev;
    }

    // ====================== DYNAMIC PROGRAMMING ======================
    static void dp() {
        section("DP");

        // climbing stairs: dp[i] = dp[i-1] + dp[i-2]
        int n = 10;
        int[] dp = new int[n + 1];
        dp[0] = dp[1] = 1;
        for (int i = 2; i <= n; i++) dp[i] = dp[i - 1] + dp[i - 2];

        // мемоизация через мапу
        Map<Integer, Integer> memo = new HashMap<>();
        int fib = fib(20, memo);

        // knapsack-like 2D шаблон
        // int[][] f = new int[items + 1][capacity + 1];
        // for (int i = 1; i <= items; i++)
        //     for (int w = 0; w <= capacity; w++)
        //         f[i][w] = (weight[i-1] > w) ? f[i-1][w]
        //                 : Math.max(f[i-1][w], f[i-1][w - weight[i-1]] + value[i-1]);

        System.out.println("stairs(10): " + dp[n]);
        System.out.println("fib(20): " + fib);
    }

    static int fib(int n, Map<Integer, Integer> memo) {
        if (n < 2) return n;
        Integer cached = memo.get(n);
        if (cached != null) return cached;
        int v = fib(n - 1, memo) + fib(n - 2, memo);
        memo.put(n, v);
        return v;
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

    // ====================== TOP K (HEAP) ======================
    static void topK() {
        section("TOP K (HEAP)");

        // k наибольших — min-heap размера k (выкидываем самое маленькое)
        int[] arr = {3, 1, 5, 12, 2, 11};
        int k = 3;
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        for (int x : arr) {
            minHeap.offer(x);
            if (minHeap.size() > k) minHeap.poll();
        }
        List<Integer> topLargest = new ArrayList<>(minHeap);
        topLargest.sort(Comparator.reverseOrder());

        // k ближайших точек к (0,0) — heap по квадрату расстояния
        int[][] points = {{1, 1}, {2, 2}, {3, 3}, {0, 1}};
        PriorityQueue<int[]> heap = new PriorityQueue<>(
                Comparator.comparingInt(p -> p[0] * p[0] + p[1] * p[1]));
        for (int[] p : points) heap.offer(p);
        int[] closest = heap.poll();

        // top-K по частоте: HashMap + min-heap по частоте
        int[] nums = {1, 1, 1, 2, 2, 3};
        Map<Integer, Integer> freq = new HashMap<>();
        for (int v : nums) freq.merge(v, 1, Integer::sum);
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[1] - b[1]);
        for (var e : freq.entrySet()) {
            pq.offer(new int[]{e.getKey(), e.getValue()});
            if (pq.size() > 2) pq.poll();
        }
        List<Integer> top2 = new ArrayList<>();
        while (!pq.isEmpty()) top2.add(pq.poll()[0]);

        System.out.println("top-3 largest: " + topLargest);
        System.out.println("closest to origin: " + Arrays.toString(closest));
        System.out.println("top-2 frequent: " + top2);
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

    // ====================== GREEDY / INTERVALS ======================
    static void greedyIntervals() {
        section("GREEDY / INTERVALS");

        // merge intervals: сортируем по началу, склеиваем пересекающиеся
        int[][] intervals = {{1, 3}, {2, 6}, {8, 10}, {15, 18}};
        Arrays.sort(intervals, Comparator.comparingInt(a -> a[0]));
        List<int[]> merged = new ArrayList<>();
        for (int[] iv : intervals) {
            if (merged.isEmpty() || merged.getLast()[1] < iv[0]) merged.add(iv.clone());
            else merged.getLast()[1] = Math.max(merged.getLast()[1], iv[1]);
        }

        // максимум непересекающихся: сортировать по КОНЦУ, жадно брать
        int[][] iv = {{1, 3}, {2, 4}, {3, 5}, {6, 7}};
        Arrays.sort(iv, Comparator.comparingInt(a -> a[1]));
        int picked = 0, lastEnd = Integer.MIN_VALUE;
        for (int[] x : iv) if (x[0] >= lastEnd) { picked++; lastEnd = x[1]; }

        // meeting rooms II — мин. число параллельных встреч
        int[] start = {0, 5, 15}, end = {10, 20, 30};
        Arrays.sort(start); Arrays.sort(end);
        int rooms = 0, ei = 0;
        for (int si = 0; si < start.length; si++) {
            if (start[si] < end[ei]) rooms++;
            else ei++;
        }

        System.out.println("merged: " + merged.stream().map(Arrays::toString).toList());
        System.out.println("max non-overlapping: " + picked);
        System.out.println("meeting rooms: " + rooms);
    }

    // ====================== PREFIX SUMS 1D ======================
    static void prefixSums() {
        section("PREFIX SUMS 1D");

        int[] a = {1, 2, 3, 4, 5};

        // префикс: pref[i+1] = a[0] + ... + a[i]
        int[] pref = new int[a.length + 1];
        for (int i = 0; i < a.length; i++) pref[i + 1] = pref[i] + a[i];
        int sumLR = pref[3 + 1] - pref[1];                 // a[1..3] = 2+3+4 = 9

        // subarray sum equals K — главная идиома: prefSum -> count
        int[] nums = {1, 1, 1};
        int K = 2;
        Map<Integer, Integer> cnt = new HashMap<>();
        cnt.put(0, 1);                                     // пустой префикс = 0
        int sum = 0, ans = 0;
        for (int x : nums) {
            sum += x;
            ans += cnt.getOrDefault(sum - K, 0);
            cnt.merge(sum, 1, Integer::sum);
        }

        // subarray sum divisible by K — ключ = ((sum % K) + K) % K
        int[] nums2 = {4, 5, 0, -2, -3, 1};
        int KK = 5;
        Map<Integer, Integer> remCnt = new HashMap<>();
        remCnt.put(0, 1);
        int s2 = 0, ans2 = 0;
        for (int x : nums2) {
            s2 += x;
            int r = ((s2 % KK) + KK) % KK;
            ans2 += remCnt.getOrDefault(r, 0);
            remCnt.merge(r, 1, Integer::sum);
        }

        System.out.println("sum a[1..3] = " + sumLR);
        System.out.println("subarray sum=2 count: " + ans);
        System.out.println("subarray div by 5 count: " + ans2);
    }

    // ====================== MONOTONIC STACK ======================
    static void monotonicStack() {
        section("MONOTONIC STACK");

        // next greater element справа (-1 если нет). Храним ИНДЕКСЫ, не значения.
        int[] a = {2, 1, 2, 4, 3};
        int[] nge = new int[a.length];
        Arrays.fill(nge, -1);
        Deque<Integer> stack = new ArrayDeque<>();
        for (int i = 0; i < a.length; i++) {
            while (!stack.isEmpty() && a[stack.peek()] < a[i]) {
                nge[stack.pop()] = a[i];
            }
            stack.push(i);
        }

        // daily temperatures — через сколько дней станет теплее
        int[] temps = {73, 74, 75, 71, 69, 72, 76, 73};
        int[] wait = new int[temps.length];
        Deque<Integer> st = new ArrayDeque<>();
        for (int i = 0; i < temps.length; i++) {
            while (!st.isEmpty() && temps[st.peek()] < temps[i]) {
                int j = st.pop();
                wait[j] = i - j;
            }
            st.push(i);
        }

        System.out.println("next greater: " + Arrays.toString(nge));
        System.out.println("daily wait:   " + Arrays.toString(wait));
    }

    // ====================== UNION-FIND (DSU) ======================
    static void unionFind() {
        section("UNION-FIND");

        DSU dsu = new DSU(6);                              // элементы 0..5
        dsu.union(0, 1);
        dsu.union(2, 3);
        dsu.union(1, 2);
        boolean connected = dsu.find(0) == dsu.find(3);
        boolean notConn   = dsu.find(0) == dsu.find(5);
        int comps = dsu.components;

        System.out.println("0~3? " + connected + ", 0~5? " + notConn + ", components=" + comps);
    }

    static class DSU {
        int[] parent, rank;
        int components;
        DSU(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i;
            components = n;
        }
        int find(int x) {
            while (parent[x] != x) {
                parent[x] = parent[parent[x]];             // path halving
                x = parent[x];
            }
            return x;
        }
        boolean union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra == rb) return false;
            if (rank[ra] < rank[rb]) { int t = ra; ra = rb; rb = t; }
            parent[rb] = ra;
            if (rank[ra] == rank[rb]) rank[ra]++;
            components--;
            return true;
        }
    }

    // ====================== TRIE ======================
    static void trie() {
        section("TRIE");

        Trie t = new Trie();
        t.insert("apple");
        t.insert("app");
        t.insert("banana");

        System.out.println("search 'apple': " + t.search("apple"));     // true
        System.out.println("search 'app':   " + t.search("app"));       // true
        System.out.println("search 'apt':   " + t.search("apt"));       // false
        System.out.println("startsWith 'ban': " + t.startsWith("ban")); // true
    }

    static class Trie {
        static class Node {
            Node[] kids = new Node[26];                    // a..z
            boolean end;
        }
        Node root = new Node();

        void insert(String w) {
            Node cur = root;
            for (char c : w.toCharArray()) {
                int i = c - 'a';
                if (cur.kids[i] == null) cur.kids[i] = new Node();
                cur = cur.kids[i];
            }
            cur.end = true;
        }
        boolean search(String w)     { Node n = walk(w); return n != null && n.end; }
        boolean startsWith(String p) { return walk(p) != null; }

        private Node walk(String s) {
            Node cur = root;
            for (char c : s.toCharArray()) {
                cur = cur.kids[c - 'a'];
                if (cur == null) return null;
            }
            return cur;
        }
    }

    // ====================== BACKTRACKING ======================
    static void backtracking() {
        section("BACKTRACKING");

        int[] nums = {1, 2, 3};

        // все перестановки
        List<List<Integer>> perms = new ArrayList<>();
        permute(nums, new boolean[nums.length], new ArrayList<>(), perms);

        // все подмножества (включая пустое)
        List<List<Integer>> subs = new ArrayList<>();
        subsets(nums, 0, new ArrayList<>(), subs);

        // все комбинации C(n, k): k чисел из 1..n
        List<List<Integer>> combs = new ArrayList<>();
        combine(1, 4, 2, new ArrayList<>(), combs);

        // правильные скобочные последовательности длины 2n
        List<String> parens = new ArrayList<>();
        genParens(3, 3, new StringBuilder(), parens);

        System.out.println("permutations: " + perms);
        System.out.println("subsets: " + subs);
        System.out.println("C(4,2): " + combs);
        System.out.println("parens n=3: " + parens);
    }

    static void permute(int[] nums, boolean[] used, List<Integer> path, List<List<Integer>> out) {
        if (path.size() == nums.length) { out.add(new ArrayList<>(path)); return; }
        for (int i = 0; i < nums.length; i++) {
            if (used[i]) continue;
            used[i] = true; path.add(nums[i]);
            permute(nums, used, path, out);
            path.removeLast(); used[i] = false;            // откат
        }
    }

    static void subsets(int[] nums, int start, List<Integer> path, List<List<Integer>> out) {
        out.add(new ArrayList<>(path));                    // КАЖДЫЙ префикс — подмножество
        for (int i = start; i < nums.length; i++) {
            path.add(nums[i]);
            subsets(nums, i + 1, path, out);
            path.removeLast();
        }
    }

    static void combine(int start, int n, int k, List<Integer> path, List<List<Integer>> out) {
        if (path.size() == k) { out.add(new ArrayList<>(path)); return; }
        for (int i = start; i <= n; i++) {
            path.add(i);
            combine(i + 1, n, k, path, out);
            path.removeLast();
        }
    }

    static void genParens(int open, int close, StringBuilder cur, List<String> out) {
        if (open == 0 && close == 0) { out.add(cur.toString()); return; }
        if (open > 0) {
            cur.append('('); genParens(open - 1, close, cur, out);
            cur.deleteCharAt(cur.length() - 1);
        }
        if (close > open) {                                // закрывать можно только если открытых меньше
            cur.append(')'); genParens(open, close - 1, cur, out);
            cur.deleteCharAt(cur.length() - 1);
        }
    }

    // ====================== FAST INPUT (шаблон, не вызывается) ======================
    // Если задача читает с stdin, Scanner МЕДЛЕННЫЙ. Шаблон:
    //
    //   var br = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
    //   int n = Integer.parseInt(br.readLine().trim());
    //   var st = new java.util.StringTokenizer(br.readLine());
    //   int[] a = new int[n];
    //   for (int i = 0; i < n; i++) a[i] = Integer.parseInt(st.nextToken());
    //
    // Быстрый вывод:
    //   var pw = new java.io.PrintWriter(
    //       new java.io.BufferedWriter(new java.io.OutputStreamWriter(System.out)));
    //   pw.println(answer);
    //   pw.flush();
    //
    // Для маленьких — Scanner (sc.nextInt() / sc.next() / sc.nextLine()).

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

    // ====================== helpers ======================
    private static void section(String name) {
        System.out.println();
        System.out.println("===== " + name + " =====");
    }
}
