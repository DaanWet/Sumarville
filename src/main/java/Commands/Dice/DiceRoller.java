package Commands.Dice;

import java.util.Random;

public class DiceRoller {

    private final Random random;

    public DiceRoller() {
        this(new Random());
    }

    public DiceRoller(Random random) {
        this.random = random;
    }

    public int roll(int sides) {
        return random.nextInt(sides) + 1;
    }

    public int[] rollMany(int sides, int amount) {
        int[] rolls = new int[amount];
        for (int i = 0; i < amount; i++) {
            rolls[i] = roll(sides);
        }
        return rolls;
    }

    public int sum(int[] rolls) {
        int total = 0;
        for (int r : rolls) {
            total += r;
        }
        return total;
    }
}
