package org.jsoup.helper;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * JUnit‑4 test suite for {@link ChangeNotifyingArrayList}.
 *
 * The tests verify two aspects:
 *
 * 1. The underlying {@link java.util.ArrayList} behaviour (elements are added,
 *    removed, set, etc. exactly as a normal ArrayList would do).
 * 2. The abstract callback {@code onContentsChanged()} is invoked **once**
 *    for every mutating operation, even when the operation does not actually
 *    modify the list (e.g. {@code remove(Object)} that returns {@code false}).
 *
 * A concrete subclass {@code TestChangeNotifyingArrayList} is used that simply
 * counts how many times the callback has been triggered.
 */
public class ChangeNotifyingArrayListTest {

    /** Simple concrete implementation that just counts callback invocations. */
    private static class TestChangeNotifyingArrayList<E> extends ChangeNotifyingArrayList<E> {
        private int changeCount = 0;

        TestChangeNotifyingArrayList(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        public void onContentsChanged() {
            changeCount++;
        }

        /** Expose the protected {@code removeRange} for testing. */
        void invokeRemoveRange(int fromIndex, int toIndex) {
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
    /* 1.  Basic mutating operations – verify callback and list semantics      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddElementCallsOnContentsChanged() {
        assertEquals(0, list.getChangeCount());

        assertTrue(list.add("A"));
        assertEquals(1, list.size());
        assertEquals("A", list.get(0));

        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testAddAtIndexCallsOnContentsChanged() {
        list.addAll(Arrays.asList("X", "Y"));
        list.resetChangeCount();

        list.add(1, "Z");
        assertEquals(Arrays.asList("X", "Z", "Y"), list);
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testSetCallsOnContentsChanged() {
        list.addAll(Arrays.asList("old"));
        list.resetChangeCount();

        String previous = list.set(0, "new");
        assertEquals("old", previous);
        assertEquals("new", list.get(0));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveByIndexCallsOnContentsChanged() {
        list.addAll(Arrays.asList("first", "second"));
        list.resetChangeCount();

        String removed = list.remove(0);
        assertEquals("first", removed);
        assertEquals(1, list.size());
        assertEquals("second", list.get(0));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveObjectCallsOnContentsChanged_ElementPresent() {
        list.addAll(Arrays.asList("a", "b", "c"));
        list.resetChangeCount();

        boolean result = list.remove((Object) "b");
        assertTrue(result);
        assertEquals(Arrays.asList("a", "c"), list);
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveObjectCallsOnContentsChanged_ElementAbsent() {
        list.addAll(Arrays.asList("a", "b"));
        list.resetChangeCount();

        boolean result = list.remove((Object) "z");
        assertFalse(result);
        assertEquals(2, list.size());
        assertEquals(1, list.getChangeCount()); // callback still fires
    }

    @Test
    public void testClearCallsOnContentsChanged() {
        list.addAll(Arrays.asList("x", "y"));
        list.resetChangeCount();

        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testAddAllCallsOnContentsChanged() {
        Collection<String> c = Arrays.asList("1", "2", "3");
        assertEquals(0, list.getChangeCount());

        boolean changed = list.addAll(c);
        assertTrue(changed);
        assertEquals(c, list);
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testAddAllAtIndexCallsOnContentsChanged() {
        list.addAll(Arrays.asList("a", "b"));
        list.resetChangeCount();

        Collection<String> c = Arrays.asList("x", "y");
        boolean changed = list.addAll(1, c);
        assertTrue(changed);
        assertEquals(Arrays.asList("a", "x", "y", "b"), list);
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveRangeCallsOnContentsChanged() {
        list.addAll(Arrays.asList("0", "1", "2", "3", "4"));
        list.resetChangeCount();

        list.invokeRemoveRange(1, 4); // should remove indices 1,2,3
        assertEquals(Arrays.asList("0", "4"), list);
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveAllCallsOnContentsChanged() {
        list.addAll(Arrays.asList("a", "b", "c", "d"));
        list.resetChangeCount();

        boolean changed = list.removeAll(Arrays.asList("b", "d", "x"));
        assertTrue(changed);
        assertEquals(Arrays.asList("a", "c"), list);
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRetainAllCallsOnContentsChanged() {
        list.addAll(Arrays.asList("a", "b", "c"));
        list.resetChangeCount();

        boolean changed = list.retainAll(Arrays.asList("b", "c"));
        assertTrue(changed);
        assertEquals(Arrays.asList("b", "c"), list);
        assertEquals(1, list.getChangeCount());
    }

    /* --------------------------------------------------------------------- */
    /* 2.  Edge cases – empty collections, null elements, empty list, etc.   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAllEmptyCollection() {
        assertTrue(list.addAll(Collections.emptyList()));
        assertTrue(list.isEmpty()); // addAll returns true even if collection empty (ArrayList spec)
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testAddAllAtIndexEmptyCollection() {
        list.add("existing");
        list.resetChangeCount();

        assertTrue(list.addAll(0, Collections.emptyList()));
        assertEquals(1, list.size());
        assertEquals("existing", list.get(0));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testClearOnEmptyList() {
        assertTrue(list.isEmpty());
        list.clear(); // should still fire the callback
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testAddNullElement() {
        assertTrue(list.add(null));
        assertNull(list.get(0));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveNullElementPresent() {
        list.addAll(Arrays.asList("a", null, "b"));
        list.resetChangeCount();

        boolean removed = list.remove((Object) null);
        assertTrue(removed);
        assertEquals(Arrays.asList("a", "b"), list);
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveNullElementAbsent() {
        list.addAll(Arrays.asList("a", "b"));
        list.resetChangeCount();

        boolean removed = list.remove((Object) null);
        assertFalse(removed);
        assertEquals(2, list.size());
        assertEquals(1, list.getChangeCount()); // callback still fires
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Error scenarios – passing null collections where not allowed       */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testAddAll_NullCollection_ThrowsNPE() {
        list.addAll(null);
    }

    @Test(expected = NullPointerException.class)
    public void testAddAllAtIndex_NullCollection_ThrowsNPE() {
        list.addAll(0, null);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveAll_NullCollection_ThrowsNPE() {
        list.removeAll(null);
    }

    @Test(expected = NullPointerException.class)
    public void testRetainAll_NullCollection_ThrowsNPE() {
        list.retainAll(null);
    }

    /* --------------------------------------------------------------------- */
    /* 4.  Index‑related error handling                                        */
    /* --------------------------------------------------------------------- */

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddAtInvalidIndex_ThrowsException() {
        list.add(5, "oops");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetInvalidIndex_ThrowsException() {
        list.set(0, "nope");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveInvalidIndex_ThrowsException() {
        list.remove(0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddAllAtInvalidIndex_ThrowsException() {
        list.addAll(2, Arrays.asList("a"));
    }

    /* --------------------------------------------------------------------- */
    /* 5.  Verify that multiple operations accumulate the change count       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testMultipleOperationsAccumulateChangeCount() {
        list.add("one");
        list.add("two");
        list.remove("one");
        list.clear();

        // 4 mutating calls → 4 notifications
        assertEquals(4, list.getChangeCount());
    }
}
