package org.jsoup.helper;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * JUnit 4 tests for {@link ChangeNotifyingArrayList}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic functionality of each overridden method.</li>
 *   <li>Verification that {@code onContentsChanged()} is invoked exactly once per mutating operation.</li>
 *   <li>Edge‑case handling such as empty lists, out‑of‑range indexes and {@code null} collections.</li>
 *   <li>Exception scenarios to ensure the callback is still triggered before the exception propagates.</li>
 * </ul>
 */
public class ChangeNotifyingArrayListTest {

    /** Concrete implementation that records how many times {@code onContentsChanged} is called. */
    private static class TestChangeNotifyingArrayList<E> extends ChangeNotifyingArrayList<E> {
        private int changeCount = 0;

        TestChangeNotifyingArrayList(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        public void onContentsChanged() {
            changeCount++;
        }

        /** Helper to expose the protected {@code removeRange}. */
        void exposeRemoveRange(int fromIndex, int toIndex) {
            super.removeRange(fromIndex, toIndex);
        }

        int getChangeCount() {
            return changeCount;
        }

        void resetChangeCount() {
            changeCount = 0;
        }
    }

    private TestChangeNotifyingArrayList<String> list;

    @Before
    public void setUp() {
        list = new TestChangeNotifyingArrayList<>(10);
    }

    /* --------------------------------------------------------------------- */
    /*  Basic functionality tests – one per overridden public method          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAdd_E() {
        assertTrue(list.add("a"));
        assertEquals(1, list.size());
        assertEquals("a", list.get(0));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testAdd_int_E() {
        list.add("first");
        list.resetChangeCount();

        list.add(0, "zero");
        assertEquals(2, list.size());
        assertEquals("zero", list.get(0));
        assertEquals("first", list.get(1));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testSet() {
        list.add("old");
        list.resetChangeCount();

        String previous = list.set(0, "new");
        assertEquals("old", previous);
        assertEquals("new", list.get(0));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemove_int() {
        list.addAll(Arrays.asList("x", "y", "z"));
        list.resetChangeCount();

        String removed = list.remove(1);
        assertEquals("y", removed);
        assertEquals(Arrays.asList("x", "z"), list);
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemove_Object_present() {
        list.addAll(Arrays.asList("a", "b"));
        list.resetChangeCount();

        boolean result = list.remove("b");
        assertTrue(result);
        assertEquals(Arrays.asList("a"), list);
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemove_Object_absent() {
        list.add("a");
        list.resetChangeCount();

        boolean result = list.remove("non‑existent");
        assertFalse(result);
        assertEquals(1, list.getChangeCount()); // callback still invoked
    }

    @Test
    public void testClear_nonEmpty() {
        list.addAll(Arrays.asList("one", "two"));
        list.resetChangeCount();

        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testClear_alreadyEmpty() {
        list.clear(); // should still trigger callback
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testAddAll_Collection() {
        Collection<String> toAdd = Arrays.asList("a", "b", "c");
        boolean changed = list.addAll(toAdd);
        assertTrue(changed);
        assertEquals(toAdd, list);
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testAddAll_int_Collection() {
        list.addAll(Arrays.asList("x", "y"));
        list.resetChangeCount();

        Collection<String> extra = Arrays.asList("a", "b");
        boolean changed = list.addAll(1, extra);
        assertTrue(changed);
        assertEquals(Arrays.asList("x", "a", "b", "y"), list);
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveAll() {
        list.addAll(Arrays.asList("keep", "remove1", "remove2"));
        list.resetChangeCount();

        boolean changed = list.removeAll(Arrays.asList("remove1", "remove2"));
        assertTrue(changed);
        assertEquals(Arrays.asList("keep"), list);
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRetainAll() {
        list.addAll(Arrays.asList("keep1", "keep2", "drop"));
        list.resetChangeCount();

        boolean changed = list.retainAll(Arrays.asList("keep1", "keep2"));
        assertTrue(changed);
        assertEquals(Arrays.asList("keep1", "keep2"), list);
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveRange() {
        list.addAll(Arrays.asList("a", "b", "c", "d"));
        list.resetChangeCount();

        list.exposeRemoveRange(1, 3); // removes "b" and "c"
        assertEquals(Arrays.asList("a", "d"), list);
        assertEquals(1, list.getChangeCount());
    }

    /* --------------------------------------------------------------------- */
    /*  Edge‑case and exception handling tests                               */
    /* --------------------------------------------------------------------- */

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSet_indexOutOfBounds() {
        list.add("only");
        list.resetChangeCount();

        try {
            list.set(5, "boom");
        } finally {
            // onContentsChanged should have been called before the exception
            assertEquals(1, list.getChangeCount());
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAdd_int_indexOutOfBounds() {
        list.add("a");
        list.resetChangeCount();

        try {
            list.add(5, "boom");
        } finally {
            assertEquals(1, list.getChangeCount());
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemove_int_indexOutOfBounds() {
        list.add("a");
        list.resetChangeCount();

        try {
            list.remove(2);
        } finally {
            assertEquals(1, list.getChangeCount());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testAddAll_nullCollection() {
        list.resetChangeCount();

        try {
            list.addAll(null);
        } finally {
            // The callback runs before the NPE is thrown
            assertEquals(1, list.getChangeCount());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testAddAll_int_nullCollection() {
        list.add("a");
        list.resetChangeCount();

        try {
            list.addAll(0, null);
        } finally {
            assertEquals(1, list.getChangeCount());
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddAll_int_indexOutOfBounds() {
        list.addAll(Arrays.asList("x"));
        list.resetChangeCount();

        try {
            list.addAll(5, Arrays.asList("y"));
        } finally {
            assertEquals(1, list.getChangeCount());
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveRange_invalidIndices() {
        list.addAll(Arrays.asList("a", "b"));
        list.resetChangeCount();

        try {
            list.exposeRemoveRange(2, 1); // fromIndex > toIndex
        } finally {
            assertEquals(1, list.getChangeCount());
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Additional sanity checks                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testMultipleMutationsAccumulateChangeCount() {
        list.add("one");          // +1
        list.add("two");          // +1
        list.set(0, "ONE");       // +1
        list.remove("two");       // +1
        list.clear();             // +1

        assertEquals(5, list.getChangeCount());
    }

    @Test
    public void testNoChangeWhenAddAllEmptyCollection() {
        list.add("existing");
        list.resetChangeCount();

        boolean changed = list.addAll(new ArrayList<String>());
        assertFalse(changed);
        // Even though the underlying list didn't change, the callback is still invoked
        assertEquals(1, list.getChangeCount());
    }
}
