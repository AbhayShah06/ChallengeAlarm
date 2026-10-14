package com.abhayshah.challengealarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import java.util.Random;

public class MathProblemTest {
    @Test public void tiersRequireOneThroughFiveProblems() {
        for (int tier = 1; tier <= 5; tier++) assertEquals(tier, MathProblem.roundsForTier(tier));
    }

    @Test public void everyTierProducesAUsableProblem() {
        Random random = new Random(42);
        for (int tier = 1; tier <= 5; tier++) for (int i = 0; i < 100; i++) {
            MathProblem problem = MathProblem.generate(random, tier);
            assertTrue(problem.prompt.length() > 4);
            assertTrue(problem.answer >= 0);
        }
    }
}
