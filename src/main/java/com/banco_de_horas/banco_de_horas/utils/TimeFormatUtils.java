package com.banco_de_horas.banco_de_horas.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TimeFormatUtils {

    private TimeFormatUtils() {
    }

    public static String formatHours(BigDecimal hours) {

        if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0) {
            return "0h 0min";
        }

        // Parte inteira → horas
        int wholeHours = hours.intValue();

        // Parte decimal → minutos
        BigDecimal decimalPart = hours.subtract(BigDecimal.valueOf(wholeHours));

        int minutes = decimalPart
            .multiply(BigDecimal.valueOf(60))
            .setScale(0, RoundingMode.DOWN)
            .intValue();

        // Ajuste de segurança (ex: 3.999h → 4h 0min)
        if (minutes == 60) {
            wholeHours += 1;
            minutes = 0;
        }

        return wholeHours + "h " + minutes + "min";
    }
}
