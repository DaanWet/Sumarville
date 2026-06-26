package Commands.Food;

import Domain.FoodItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FoodCommandsTest {

    private static List<FoodItem> sample() {
        return List.of(
                new FoodItem(10L, "Pizza", "🍕"),
                new FoodItem(20L, "Sushi", "🍣"),
                new FoodItem(30L, "Burger", "🍔"));
    }

    @Test
    void matchesFirstItem() { // the case the old off-by-one broke (returned -1)
        Optional<FoodItem> m = FoodCommands.findByEmoji(sample(), "🍕");
        assertTrue(m.isPresent());
        assertEquals(10L, m.get().id());
    }

    @Test
    void matchesMiddleItem() { // old code returned the item BEFORE this one
        Optional<FoodItem> m = FoodCommands.findByEmoji(sample(), "🍣");
        assertTrue(m.isPresent());
        assertEquals(20L, m.get().id());
    }

    @Test
    void matchesLastItem() {
        assertEquals(30L, FoodCommands.findByEmoji(sample(), "🍔").orElseThrow().id());
    }

    @Test
    void emptyWhenNotFound() {
        assertTrue(FoodCommands.findByEmoji(sample(), "🌮").isEmpty());
    }

    @Test
    void emptyWhenListEmpty() {
        assertTrue(FoodCommands.findByEmoji(List.of(), "🍕").isEmpty());
    }

    @Test
    void matchIsCaseInsensitive() { // findByEmoji uses equalsIgnoreCase
        Optional<FoodItem> m = FoodCommands.findByEmoji(
                List.of(new FoodItem(1L, "Taco", ":TACO:")), ":taco:");
        assertTrue(m.isPresent());
        assertEquals(1L, m.get().id());
    }
}
