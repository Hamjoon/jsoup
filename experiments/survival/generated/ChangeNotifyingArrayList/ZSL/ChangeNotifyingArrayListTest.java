package org.jsoup.helper;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ChangeNotifyingArrayList}.
 *
 * The abstract class is exercised via a concrete test subclass that
 * records how many times {@code onContentsChanged()} has been invoked.
 */
public class ChangeNotifyingArrayListTest {

    /** Simple concrete implementation that counts callbacks. */
    private static class TestArrayList<E> extends ChangeNotifyingArrayList<E> {
        private int changeCount = 0;

        TestArrayList(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        public void onContentsChanged() {
            changeCount++;
        }

        int getChangeCount() {
            return changeCount;
        }

        /** Expose protected removeRange for testing. */
        void callRemoveRange(int from, int to) {
            super.removeRange(from, to);
        }
    }

    private TestArrayList<String> list;

    @Before
    public void setUp() {
        // initialise with a capacity that is larger than the number of test elements
        list = new TestArrayList<>(10);
    }

    @Test
    public void constructor_hasZeroSize() {
        assertEquals(0, list.size());
        assertEquals(0, list.getChangeCount());
    }

    @Test
    public void add_callsOnContentsChanged_andAddsElement() {
        assertTrue(list.add("a"));
        assertEquals(1, list.size());
        assertEquals("a", list.get(0));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void addAtIndex_callsOnContentsChanged_andInsertsElement() {
        list.add("first");
        list.add(0, "zero");
        assertEquals(2, list.size());
        assertEquals("zero", list.get(0));
        assertEquals("first", list.get(1));
        assertEquals(2, list.getChangeCount()); // one for each add
    }

    @Test
    public void set_callsOnContentsChanged_andReplacesElement() {
        list.addAll(Arrays.asList("a", "b", "c"));
        int prevCount = list.getChangeCount();

        String old = list.set(1, "B");
        assertEquals("b", old);
        assertEquals("B", list.get(1));
        assertEquals(prevCount + 1, list.getChangeCount());
    }

    @Test
    public void removeByIndex_callsOnContentsChanged_andRemovesElement() {
        list.addAll(Arrays.asList("x", "y", "z"));
        int prevCount = list.getChangeCount();

        String removed = list.remove(1);
        assertEquals("y", removed);
        assertEquals(2, list.size());
        assertEquals("x", list.get(0));
        assertEquals("z", list.get(1));
        assertEquals(prevCount + 1, list.getChangeCount());
    }

    @Test
    public void removeObject_callsOnContentsChanged_evenIfNotPresent() {
        list.addAll(Arrays.asList("p", "q"));
        int prevCount = list.getChangeCount();

        // existing element
        assertTrue(list.remove("p"));
        assertEquals(1, list.size());
        assertEquals(prevCount + 1, list.getChangeCount());

        // non‑existing element – should still trigger the callback
        assertFalse(list.remove("nonexistent"));
        assertEquals(prevCount + 2, list.getChangeCount());
    }

    @Test
    public void clear_callsOnContentsChanged_andEmptiesList() {
        list.addAll(Arrays.asList("a", "b"));
        int prevCount = list.getChangeCount();

        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(prevCount + 1, list.getChangeCount());
    }

    @Test
    public void addAll_callsOnContentsChanged_andAddsAllElements() {
        Collection<String> coll = Arrays.asList("1", "2", "3");
        assertTrue(list.addAll(coll));
        assertEquals(3, list.size());
        assertEquals("1", list.get(0));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void addAllEmptyCollection_callsOnContentsChanged_andReturnsFalse() {
        Collection<String> empty = Collections.emptyList();
        assertFalse(list.addAll(empty));
        assertEquals(0, list.size());
        // Even though nothing was added, the callback is still invoked
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void addAllAtIndex_callsOnContentsChanged_andInsertsAllElements() {
        list.addAll(Arrays.asList("a", "d"));
        Collection<String> middle = Arrays.asList("b", "c");
        assertTrue(list.addAll(1, middle));
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
        assertEquals(2, list.getChangeCount()); // first addAll + second addAllAtIndex
    }

    @Test
    public void removeAll_callsOnContentsChanged_andRemovesMatchingElements() {
        list.addAll(Arrays.asList("a", "b", "c", "b"));
        Collection<String> toRemove = Arrays.asList("b", "x");
        assertTrue(list.removeAll(toRemove));
        assertEquals(Arrays.asList("a", "c"), list);
        assertEquals(2, list.getChangeCount()); // addAll + removeAll
    }

    @Test
    public void removeAllWithNoMatch_callsOnContentsChanged_andReturnsFalse() {
        list.addAll(Arrays.asList("a", "b"));
        int prev = list.getChangeCount();

        assertFalse(list.removeAll(Collections.singleton("z")));
        assertEquals(prev + 1, list.getChangeCount());
        assertEquals(2, list.size());
    }

    @Test
    public void retainAll_callsOnContentsChanged_andKeepsOnlySpecifiedElements() {
        list.addAll(Arrays.asList("a", "b", "c"));
        Collection<String> retain = Arrays.asList("a", "c", "x");
        assertTrue(list.retainAll(retain));
        assertEquals(Arrays.asList("a", "c"), list);
        assertEquals(2, list.getChangeCount()); // addAll + retainAll
    }

    @Test
    public void retainAllWithNoChange_callsOnContentsChanged_andReturnsFalse() {
        list.addAll(Arrays.asList("a", "b"));
        int prev = list.getChangeCount();

        assertFalse(list.retainAll(Arrays.asList("a", "b", "c")));
        assertEquals(prev + 1, list.getChangeCount());
        assertEquals(2, list.size());
    }

    @Test
    public void removeRange_callsOnContentsChanged_andRemovesRange() {
        list.addAll(Arrays.asList("0", "1", "2", "3", "4"));
        int prev = list.getChangeCount();

        // remove indices 1 (inclusive) to 4 (exclusive) -> elements "1","2","3"
        list.callRemoveRange(1, 4);
        assertEquals(Arrays.asList("0", "4"), list);
        assertEquals(prev + 1, list.getChangeCount());
    }

    @Test
    public void multipleOperations_accumulateChangeCountCorrectly() {
        list.add("a");               // 1
        list.add("b");               // 2
        list.set(0, "A");            // 3
        list.remove("b");            // 4
        list.addAll(Arrays.asList("x", "y")); // 5
        list.removeAll(Collections.singleton("x")); // 6
        list.retainAll(Collections.singleton("A")); // 7
        list.clear();                // 8

        assertEquals(8, list.getChangeCount());
    }
}
