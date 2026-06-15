package Commands.Dice;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DiceRollerTest {

    @Test
    void rollIsWithinBounds() {
        DiceRoller r = new DiceRoller(new Random(42));
        for (int i = 0; i < 1000; i++) {
            int v = r.roll(20);
            assertTrue(v >= 1 && v <= 20, "out of bounds: " + v);
        }
    }

    @Test
    void rollManyReturnsRequestedCountWithinBounds() {
        DiceRoller r = new DiceRoller(new Random(1));
        int[] rolls = r.rollMany(6, 5);
        assertEquals(5, rolls.length);
        for (int v : rolls) {
            assertTrue(v >= 1 && v <= 6);
        }
    }

    @Test
    void sumAddsAllRolls() {
        DiceRoller r = new DiceRoller(new Random(1));
        assertEquals(15, r.sum(new int[]{1, 2, 3, 4, 5}));
    }
}
