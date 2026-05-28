package io.github.zebin.test.transfer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MainTest {

    @Test
    @DisplayName("шаблон: проверь свою задачку здесь")
    void sample() {
        int result = 2 + 2;
        System.out.println("result = " + result);
        assertThat(result).isEqualTo(4);
    }
}
