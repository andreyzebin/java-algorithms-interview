package io.github.zebin.test.cheatsheet;

import java.util.*;

/**
 * Конспект №2 — АЛГОРИТМИЧЕСКИЕ ПАТТЕРНЫ: техники решения задач.
 * То, что вспоминаешь, когда думаешь над подходом.
 * Запуск всего: ./gradlew :tasks:cheat
 */
public class AlgorithmPatterns {

    public static void run() {
        matrices();
        twoPointers();
        slidingWindow();
        binarySearch();
        prefixSums();
        monotonicStack();
        greedyIntervals();
        backtracking();
        dp();
        fastInput();
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

        // спиральный обход (классический приём)
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

    // ====================== FAST INPUT (шаблон, не вызывается) ======================
    static void fastInput() {
        section("FAST INPUT");
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
        System.out.println("прочитай комментарии в этой секции (stdin reading)");
    }

    private static void section(String name) {
        System.out.println();
        System.out.println("===== " + name + " =====");
    }
}
