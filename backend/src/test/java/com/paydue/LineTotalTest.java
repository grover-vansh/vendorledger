package com.paydue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;

class LineTotalTest {

    @Test
    void quantityTimesPriceRoundsToTwoDecimals() {
        BigDecimal total = new BigDecimal("20").multiply(new BigDecimal("450.00"))
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("9000.00"), total);
    }
}
