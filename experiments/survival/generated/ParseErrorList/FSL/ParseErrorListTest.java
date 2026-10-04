package org.jsoup.parser;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * JUnit 4 test cases for {@link ParseErrorList}.
 */
public class ParseErrorListTest {

    @Test
    public void testNoTrackingProperties() {
        ParseErrorList list = ParseErrorList.noTracking();

        // maxSize should be 0
        assertEquals(0, list.getMaxSize());

        // With maxSize 0, canAddError must be false even when the list is empty
        assertFalse("canAddError should be false for no‑tracking list", list.canAddError());

        // Adding an element should not change the fact that canAddError stays false
        list.add(null);
        assertFalse("canAddError should remain false after adding elements", list.canAddError());
        assertEquals(1, list.size());
    }

    @Test
    public void testTrackingZeroMaxSize() {
        ParseErrorList list = ParseErrorList.tracking(0);

        assertEquals(0, list.getMaxSize());
        assertFalse("canAddError should be false when maxSize is zero", list.canAddError());

        // Adding an element does not affect canAddError (still false)
        list.add(null);
        assertFalse(list.canAddError());
        assertEquals(1, list.size());
    }

    @Test
    public void testTrackingPositiveMaxSize() {
        int max = 3;
        ParseErrorList list = ParseErrorList.tracking(max);

        assertEquals(max, list.getMaxSize());

        // Initially we can add errors
        assertTrue(list.canAddError());

        // Add up to maxSize - 1 elements and verify canAddError stays true
        for (int i = 0; i < max - 1; i++) {
            list.add(null);
            assertTrue("canAddError should be true when size < maxSize (size=" + list.size() + ")", list.canAddError());
        }

        // Add the element that reaches maxSize
        list.add(null);
        assertEquals(max, list.size());
        assertFalse("canAddError should be false when size == maxSize", list.canAddError());

        // Adding beyond maxSize does not change canAddError (still false)
        list.add(null);
        assertEquals(max + 1, list.size());
        assertFalse(list.canAddError());
    }

    @Test
    public void testCanAddErrorWhenBelowMax() {
        ParseErrorList list = ParseErrorList.tracking(5);
        // size = 0, maxSize = 5
        assertTrue(list.canAddError());

        // add two elements
        list.add(null);
        list.add(null);
        assertEquals(2, list.size());
        assertTrue("canAddError should be true when size < maxSize", list.canAddError());
    }

    @Test
    public void testCanAddErrorWhenAtMax() {
        int max = 2;
        ParseErrorList list = ParseErrorList.tracking(max);
        list.add(null);
        list.add(null);
        assertEquals(max, list.size());
        assertFalse("canAddError must be false when size == maxSize", list.canAddError());
    }

    @Test
    public void testCanAddErrorWithNegativeMaxSize() {
        ParseErrorList list = ParseErrorList.tracking(-1);
        assertEquals(-1, list.getMaxSize());

        // size (0) is not less than -1, so canAddError should be false immediately
        assertFalse("canAddError should be false when maxSize is negative", list.canAddError());

        // Adding elements does not make canAddError true
        list.add(null);
        assertFalse(list.canAddError());
    }

    @Test
    public void testListBehavesLikeArrayList() {
        ParseErrorList list = ParseErrorList.tracking(10);
        // Verify typical ArrayList operations
        assertTrue(list.isEmpty());

        // add, get, set, remove
        list.add(null);
        list.add(null);
        assertEquals(2, list.size());
        assertNull(list.get(0));
        assertNull(list.get(1));

        list.set(1, null);
        assertNull(list.get(1));

        list.remove(0);
        assertEquals(1, list.size());
        assertNull(list.get(0));
    }
}
