package io.github.zebin.test.cheatsheet;

/**
 * Лаунчер шпаргалок. Запуск: ./gradlew cheat
 *
 * Идиомы разбиты на 3 класса по режиму вспоминания:
 *   1. {@link LanguageIdioms}    — «как написать X в Java» (синтаксис, recall).
 *   2. {@link AlgorithmPatterns} — техники решения (two pointers, BS, DP, backtracking...).
 *   3. {@link DataStructures}    — структуры со своими классами (граф, дерево, список, trie, DSU, heap).
 *
 * Открой нужный файл — каждый самодостаточен.
 */
public class Cheatsheet {

    public static void main(String[] args) {
        LanguageIdioms.run();
        AlgorithmPatterns.run();
        DataStructures.run();
    }
}
