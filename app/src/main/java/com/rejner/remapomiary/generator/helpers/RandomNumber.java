package com.rejner.remapomiary.generator.helpers;

import java.util.Random;

public class RandomNumber {
    public static int randomInt(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min nie może być większe niż max");
        }
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

    private RandomNumber() {
    }
}
