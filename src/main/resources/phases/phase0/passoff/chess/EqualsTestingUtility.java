package passoff.chess;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Used indirectly to help test the <pre>equals()</pre> and
 * <pre>hashCode()</pre> methods of other classes.
 * <br>
 * This class requires that implementing classes provide a few builder methods,
 * and then it automatically adds multiple tests to the evaluation suite
 * which assert that the <pre>equals()</pre> and <pre>hashCode()</pre> methods function.
 *
 * @param <T> The type to be compared during testing.
 */
public abstract class EqualsTestingUtility<T> {
    private final String className;
    private final String itemsPlural;
    private T original;
    private T equivalent;
    private Collection<T> allDifferent;

    public EqualsTestingUtility(String className, String itemsPlural) {
        this.className = className;
        this.itemsPlural = itemsPlural;
    }

    protected abstract T buildOriginal();
    protected abstract Collection<T> buildAllDifferent();


    @BeforeEach
    public void setUp() {
        original = buildOriginal();
        equivalent = buildOriginal(); // For a second time
        allDifferent = buildAllDifferent();
    }

    @Test
    @DisplayName("Equals Testing")
    public void equalsTest() {
        Assertions.assertEquals(original, equivalent,
                className + ".equals() returned false for equivalent " + itemsPlural);
        for (var different : allDifferent) {
            Assertions.assertNotEquals(original, different,
                    className + ".equals() returned true for different " + itemsPlural);
        }
    }

    @Test
    @DisplayName("HashCode Testing")
    public void hashTest() {
        Assertions.assertEquals(original.hashCode(), equivalent.hashCode(),
                className + ".hashCode() returned different values for equivalent " + itemsPlural);
        for (var different : allDifferent) {
            Assertions.assertNotEquals(original.hashCode(), different.hashCode(),
                    className + ".hashCode() returned the same value for different " + itemsPlural);
        }
    }

    @Test
    @DisplayName("Equals & HashCode Testing")
    public void hashSetTest() {
        Set<T> set = new HashSet<>();
        set.add(original);

        // Manually test insertion of original & equal items
        Assertions.assertTrue(set.contains(original),
                "[" + className + "] Original item should exist in collection after adding original item");
        Assertions.assertTrue(set.contains(equivalent),
                "[" + className + "] Equivalent item should exist in collection after only adding original item");
        Assertions.assertEquals(1, set.size(),
                "[" + className + "] Collection should contain only 1 item after a single insert");
        set.add(equivalent);
        Assertions.assertEquals(1, set.size(),
                "[" + className + "] Collection should still contain only 1 item after adding equivalent item");

        // Programmatically test insertion of all different items
        int expectedSetSize = 1;
        for (var different : allDifferent) {
            Assertions.assertFalse(set.contains(different),
                    "[" + className + "] Different item should not be present in set before insertion");
            set.add(different);
            expectedSetSize++;
            Assertions.assertEquals(expectedSetSize, set.size(),
                    "[" + className + "] New item was counted as different during insertion");
        }

    }

}
