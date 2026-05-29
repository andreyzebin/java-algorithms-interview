package io.github.zebin.test.cheatsheet;

import java.util.*;

/**
 * Шпаргалка №3 — СТРУКТУРЫ ДАННЫХ: граф, дерево, связный список, trie, DSU, heap.
 * Структуры с собственными узлами/классами + операции над ними.
 * Запуск всего: ./gradlew cheat
 */
public class DataStructures {

    public static void run() {
        graphTraversal();
        linkedList();
        trie();
        unionFind();
        heapTopK();
    }

    // ====================== GRAPH / TREE: BFS / DFS ======================
    static void graphTraversal() {
        section("GRAPH / TREE (BFS / DFS)");

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

    // ====================== HEAP / TOP K ======================
    static void heapTopK() {
        section("HEAP / TOP K");

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

    private static void section(String name) {
        System.out.println();
        System.out.println("===== " + name + " =====");
    }
}
