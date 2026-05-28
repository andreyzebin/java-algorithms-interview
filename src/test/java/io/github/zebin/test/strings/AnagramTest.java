package io.github.zebin.test.strings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnagramTest {

    @Test
    void anagrams() {
        assertThat(Anagram.isAnagram("listen", "silent")).isTrue();
    }

    @Test
    void notAnagrams() {
        assertThat(Anagram.isAnagram("hello", "world")).isFalse();
    }

    @Test
    void differentLength() {
        assertThat(Anagram.isAnagram("abc", "abcd")).isFalse();
    }
}
