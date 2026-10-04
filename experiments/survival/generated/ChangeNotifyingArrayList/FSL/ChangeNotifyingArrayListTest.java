package org.jsoup.helper;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Tests for {@link ChangeNotifyingArrayList}.
 * A concrete subclass is used that counts how many times {@code onContentsChanged()}
 * is invoked. Each overridden method should trigger exactly one call.
 */
public class ChangeNotifyingArrayListTest {

    /** Simple concrete implementation that records change notifications. */
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

        /** Expose protected {@code removeRange} for testing. */
        void doRemoveRange(int fromIndex, int toIndex) {
            super.removeRange(fromIndex, toIndex);
        }
    }

    private TestArrayList<String> list;

    @Before
    public void setUp() {
        list = new TestArrayList<>(10);
    }

    @Test
    public void testAddCallsOnContentsChanged() {
        assertEquals(0, list.getChangeCount());
        boolean added = list.add("first");
        assertTrue(added);
        assertEquals(1, list.getChangeCount());
        assertEquals(1, list.size());
        assertEquals("first", list.get(0));
    }

    @Test
    public void testAddAtIndexCallsOnContentsChanged() {
        list.add("a");
        assertEquals(1, list.getChangeCount());

        list.add(0, "b");
        assertEquals(2, list.getChangeCount());
        assertEquals(2, list.size());
        assertEquals("b", list.get(0));
        assertEquals("a", list.get(1));
    }

    @Test
    public void testSetCallsOnContentsChanged() {
        list.add("old");
        assertEquals(1, list.getChangeCount());

        String prev = list.set(0, "new");
        assertEquals(2, list.getChangeCount());
        assertEquals("old", prev);
        assertEquals("new", list.get(0));
    }

    @Test
    public void testRemoveByIndexCallsOnContentsChanged() {
        list.addAll(Arrays.asList("x", "y"));
        assertEquals(1, list.getChangeCount()); // addAll triggers once

        String removed = list.remove(0);
        assertEquals(2, list.getChangeCount());
        assertEquals("x", removed);
        assertEquals(1, list.size());
        assertEquals("y", list.get(0));
    }

    @Test
    public void testRemoveObjectCallsOnContentsChanged() {
        list.add("elem");
        assertEquals(1, list.getChangeCount());

        boolean result = list.remove((Object) "elem");
        assertTrue(result);
        assertEquals(2, list.getChangeCount());
        assertTrue(list.isEmpty());

        // Removing a non‑existent element should still fire the callback
        result = list.remove((Object) "nothing");
        assertFalse(result);
        assertEquals(3, list.getChangeCount());
    }

    @Test
    public void testClearCallsOnContentsChanged() {
        list.addAll(Arrays.asList("a", "b", "c"));
        assertEquals(1, list.getChangeCount()); // addAll

        list.clear();
        assertEquals(2, list.getChangeCount());
        assertTrue(list.isEmpty());

        // Clearing an already empty list still notifies
        list.clear();
        assertEquals(3, list.getChangeCount());
    }

    @Test
    public void testAddAllCallsOnContentsChanged() {
        Collection<String> coll = Arrays.asList("one", "two");
        boolean changed = list.addAll(coll);
        assertTrue(changed);
        assertEquals(1, list.getChangeCount());
        assertEquals(2, list.size());
        assertEquals("one", list.get(0));
        assertEquals("two", list.get(1));

        // Adding an empty collection still triggers the callback (per contract)
        changed = list.addAll(Collections.emptyList());
        assertFalse(changed);
        assertEquals(2, list.getChangeCount());
    }

    @Test
    public void testAddAllAtIndexCallsOnContentsChanged() {
        list.addAll(Arrays.asList("a", "b"));
        assertEquals(1, list.getChangeCount());

        Collection<String> coll = Arrays.asList("x", "y");
        boolean changed = list.addAll(1, coll);
        assertTrue(changed);
        assertEquals(2, list.getChangeCount());

        assertEquals(Arrays.asList("a", "x", "y", "b"), list);
    }

    @Test
    public void testRemoveRangeCallsOnContentsChanged() {
        list.addAll(Arrays.asList("0", "1", "2", "3", "4"));
        assertEquals(1, list.getChangeCount()); // addAll

        // remove indices 1 (inclusive) to 4 (exclusive) => removes "1","2","3"
        list.doRemoveRange(1, 4);
        assertEquals(2, list.getChangeCount());
        assertEquals(Arrays.asList("0", "4"), list);
    }

    @Test
    public void testRemoveAllCallsOnContentsChanged() {
        list.addAll(Arrays.asList("a", "b", "c", "d"));
        assertEquals(1, list.getChangeCount());

        boolean changed = list.removeAll(Arrays.asList("b", "c"));
        assertTrue(changed);
        assertEquals(2, list.getChangeCount());
        assertEquals(Arrays.asList("a", "d"), list);

        // Removing a collection that does not affect the list still notifies
        changed = list.removeAll(Arrays.asList("x"));
        assertFalse(changed);
        assertEquals(3, list.getChangeCount());
    }

    @Test
    public void testRetainAllCallsOnContentsChanged() {
        list.addAll(Arrays.asList("keep1", "remove1", "keep2", "remove2"));
        assertEquals(1, list.getChangeCount());

        boolean changed = list.retainAll(Arrays.asList("keep1", "keep2"));
        assertTrue(changed);
        assertEquals(2, list.getChangeCount());
        assertEquals(Arrays.asList("keep1", "keep2"), list);

        // Retaining with the same collection again should not change contents but still notifies
        changed = list.retainAll(Arrays.asList("keep1", "keep2"));
        assertFalse(changed);
        assertEquals(3, list.getChangeCount());
    }

    @Test
    public void testMultipleOperationsAccumulateChangeCount() {
        list.add("a");               // +1
        list.add(0, "b");            // +1
        list.set(1, "c");            // +1
        list.remove((Object) "a");   // +1
        list.remove(0);             // +1
        list.clear();                // +1

        assertEquals(6, list.getChangeCount());
        assertTrue(list.isEmpty());
    }
}
