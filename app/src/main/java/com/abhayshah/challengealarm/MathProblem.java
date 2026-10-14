package com.abhayshah.challengealarm;

import java.util.Random;

final class MathProblem {
    final String prompt;
    final int answer;

    MathProblem(String prompt, int answer) {
        this.prompt = prompt;
        this.answer = answer;
    }

    static int roundsForTier(int tier) { return Math.max(1, Math.min(5, tier)); }

    static MathProblem generate(Random random, int requestedTier) {
        int tier = Math.max(1, Math.min(5, requestedTier));
        if (tier == 1) {
            int a = between(random, 1, 9), b = between(random, 1, 9);
            return new MathProblem(a + " + " + b, a + b);
        }
        if (tier == 2) {
            int a = between(random, 10, 40), b = between(random, 2, 20);
            if (random.nextBoolean()) return new MathProblem(a + " + " + b, a + b);
            if (b > a) { int swap = a; a = b; b = swap; }
            return new MathProblem(a + " − " + b, a - b);
        }
        if (tier == 3) {
            if (random.nextBoolean()) {
                int a = between(random, 2, 12), b = between(random, 2, 12);
                return new MathProblem(a + " × " + b, a * b);
            }
            int a = between(random, 25, 99), b = between(random, 10, 75);
            return new MathProblem(a + " + " + b, a + b);
        }
        if (tier == 4) {
            if (random.nextBoolean()) {
                int divisor = between(random, 3, 15), answer = between(random, 3, 18);
                return new MathProblem((divisor * answer) + " ÷ " + divisor, answer);
            }
            int a = between(random, 11, 30), b = between(random, 6, 20);
            return new MathProblem(a + " × " + b, a * b);
        }
        int a = between(random, 8, 25), b = between(random, 3, 12), c = between(random, 10, 60);
        if (random.nextBoolean()) return new MathProblem("(" + a + " + " + b + ") × " + c, (a + b) * c);
        return new MathProblem((a * b) + " + " + c + " − " + b, a * b + c - b);
    }

    private static int between(Random random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
}
