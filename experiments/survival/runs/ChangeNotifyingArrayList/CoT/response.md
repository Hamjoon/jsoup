###Test START##
```java
package org.jsoup.helper;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link ChangeNotifyingArrayList}.
 *
 * The tests verify:
 *   1. Normal behaviour of each overridden mutating method.
 *   2. That {@code onContentsChanged()} is invoked exactly once per call,
 *      even when the underlying {@link ArrayList} throws an exception.
 *   3. Edge‑case handling such as out‑of‑bounds indexes, {@code null}
 *      arguments, empty collections and operations on an empty list.
 */
public class ChangeNotifyingArrayListTest {

    /** Concrete implementation that counts how many times {@code onContentsChanged} is called. */
    private static class TestChangeNotifyingArrayList<E> extends ChangeNotifyingArrayList<E> {
        private int changeCount = 0;

        TestChangeNotifyingArrayList(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        public void onContentsChanged() {
            changeCount++;
        }

        int getChangeCount() {
            return changeCount;
        }

        void resetChangeCount() {
            changeCount = 0;
        }

        /** Expose the protected {@code removeRange} for testing purposes. */
        void invokeRemoveRange(int fromIndex, int toIndex) {
            super.removeRange(fromIndex, toIndex);
        }
    }

    private TestChangeNotifyingArrayList<String> list;

    @Before
    public void setUp() {
        list = new TestChangeNotifyingArrayList<>(10);
        list.addAll(Arrays.asList("a", "b", "c", "d"));
        list.resetChangeCount(); // start each test with a clean counter
    }

    /* -------------------------------------------------
     *  Public API extracted from ChangeNotifyingArrayList
     * -------------------------------------------------
     *
     *  ChangeNotifyingArrayList(int initialCapacity)
     *  abstract void onContentsChanged()
     *  E set(int index, E element)
     *  boolean add(E e)
     *  void add(int index, E element)
     *  E remove(int index)
     *  boolean remove(Object o)
     *  void clear()
     *  boolean addAll(Collection<? extends E> c)
     *  boolean addAll(int index, Collection<? extends E> c)
     *
     *  (protected) void removeRange(int fromIndex, int toIndex)
     *  (public) boolean removeAll(Collection<?> c)
     *  (public) boolean retainAll(Collection<?> c)
     */

    /* --------------------------- *
     *  Basic functional tests    *
     * --------------------------- */

    @Test
    public void testAddElement() {
        boolean added = list.add("e");
        assertTrue(added);
        assertEquals(5, list.size());
        assertEquals("e", list.get(4));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testAddAtIndexValid() {
        list.add(2, "x");
        assertEquals(5, list.size());
        assertEquals("x", list.get(2));
        assertEquals("c", list.get(3)); // original element shifted right
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testAddAtIndexInvalid() {
        try {
            list.add(10, "y");
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        // onContentsChanged is called *before* the super method throws
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testSetValidIndex() {
        String previous = list.set(1, "z");
        assertEquals("b", previous);
        assertEquals("z", list.get(1));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testSetInvalidIndex() {
        try {
            list.set(-1, "z");
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveByIndexValid() {
        String removed = list.remove(2);
        assertEquals("c", removed);
        assertEquals(3, list.size());
        assertEquals("d", list.get(2));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveByIndexInvalid() {
        try {
            list.remove(100);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveObjectPresent() {
        boolean result = list.remove((Object) "b");
        assertTrue(result);
        assertFalse(list.contains("b"));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveObjectAbsent() {
        boolean result = list.remove((Object) "nonexistent");
        assertFalse(result);
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testClearNonEmpty() {
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testClearEmpty() {
        list.clear(); // first clear makes it empty
        list.resetChangeCount();
        list.clear(); // second clear on already empty list
        assertTrue(list.isEmpty());
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testAddAllCollection() {
        Collection<String> toAdd = Arrays.asList("x", "y");
        boolean changed = list.addAll(toAdd);
        assertTrue(changed);
        assertEquals(6, list.size());
        assertEquals("x", list.get(4));
        assertEquals("y", list.get(5));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testAddAllEmptyCollection() {
        boolean changed = list.addAll(Collections.emptyList());
        assertFalse(changed);
        assertEquals(4, list.size()); // unchanged
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testAddAllNullCollection() {
        try {
            list.addAll(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testAddAllAtIndexValid() {
        Collection<String> extra = Arrays.asList("x", "y");
        boolean changed = list.addAll(2, extra);
        assertTrue(changed);
        assertEquals(6, list.size());
        assertEquals("x", list.get(2));
        assertEquals("y", list.get(3));
        assertEquals("c", list.get(4)); // original element shifted
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testAddAllAtIndexInvalid() {
        try {
            list.addAll(10, Arrays.asList("z"));
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveAll() {
        Collection<String> toRemove = Arrays.asList("b", "d", "nonexistent");
        boolean changed = list.removeAll(toRemove);
        assertTrue(changed);
        assertEquals(2, list.size());
        assertFalse(list.contains("b"));
        assertFalse(list.contains("d"));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveAllEmptyCollection() {
        boolean changed = list.removeAll(Collections.emptyList());
        assertFalse(changed);
        assertEquals(4, list.size());
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRetainAll() {
        Collection<String> toRetain = Arrays.asList("a", "c");
        boolean changed = list.retainAll(toRetain);
        assertTrue(changed);
        assertEquals(2, list.size());
        assertTrue(list.containsAll(toRetain));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRetainAllNoChange() {
        Collection<String> toRetain = Arrays.asList("a", "b", "c", "d");
        boolean changed = list.retainAll(toRetain);
        assertFalse(changed);
        assertEquals(4, list.size());
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveRangeValid() {
        // remove elements at indexes 1 (b) and 2 (c) => list becomes [a, d]
        list.invokeRemoveRange(1, 3);
        assertEquals(2, list.size());
        assertEquals("a", list.get(0));
        assertEquals("d", list.get(1));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveRangeInvalid() {
        try {
            list.invokeRemoveRange(3, 2); // from > to -> IllegalArgumentException in AbstractList
            fail("Expected IndexOutOfBoundsException or IllegalArgumentException");
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            // expected
        }
        // onContentsChanged is called before the exception propagates
        assertEquals(1, list.getChangeCount());
    }

    /* --------------------------- *
     *  Edge‑case / null handling  *
     * --------------------------- */

    @Test
    public void testAddNullElement() {
        boolean added = list.add(null);
        assertTrue(added);
        assertNull(list.get(4));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testSetNullElement() {
        list.set(0, null);
        assertNull(list.get(0));
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveNullWhenPresent() {
        list.add(null);
        list.resetChangeCount();
        boolean removed = list.remove(null);
        assertTrue(removed);
        assertEquals(5, list.size()); // original 4 + null added - removed null
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testRemoveNullWhenAbsent() {
        boolean removed = list.remove(null);
        assertFalse(removed);
        assertEquals(1, list.getChangeCount());
    }

    @Test
    public void testAddAllCollectionWithNulls() {
        Collection<String> coll = Arrays.asList("x", null, "y");
        boolean changed = list.addAll(coll);
        assertTrue(changed);
        assertEquals(7, list.size());
        assertNull(list.get(5));
        assertEquals(1, list.getChangeCount());
    }
}
```
###Test END##