package io.github.zebin.test.strings;

public class Anagram {

    public static boolean isAnagram(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int[] counts = new int[Character.MAX_VALUE];
        for (int i = 0; i < a.length(); i++) {
            counts[a.charAt(i)]++;
            counts[b.charAt(i)]--;
        }
        for (int c : counts) {
            if (c != 0) return false;
        }
        return true;
    }
}
