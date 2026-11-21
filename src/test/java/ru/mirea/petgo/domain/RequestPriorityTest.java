package ru.mirea.petgo.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestPriorityTest {
    @ParameterizedTest
    @CsvSource({"29,LOW", "30,NORMAL", "59,NORMAL", "60,HIGH"})
    void calculatesPriorityFromDuration(int minutes, RequestPriority expected) {
        assertEquals(expected, RequestPriority.forDuration(minutes));
    }
}
