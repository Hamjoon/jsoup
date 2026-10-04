package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;

public class ParseErrorListTest {

    /** Helper that creates a minimal ParseError and adds it to the list. */
    private static void addDummyError(ParseErrorList list) {
        // ParseError constructor (int pos, int lineNum, String errorMsg) exists in jsoup.
        list.add(new ParseError(0, 0, "dummy"));
    }

    // -------------------------------------------------------------------------
    //  noTracking() tests
    // -------------------------------------------------------------------------
    @Test
    public void noTracking_ShouldReturnZeroMaxAndNoAdd() {
        ParseErrorList list = ParseErrorList.noTracking();
        assertEquals("maxSize should be 0 for noTracking()", 0, list.getMaxSize());
        assertFalse("canAddError() must be false when maxSize is 0", list.canAddError());
        // Even if we try to add, canAddError stays false
        addDummyError(list);
        assertEquals("size should be 1 after manual add", 1, list.size());
        assertFalse("canAddError() must remain false after adding", list.canAddError());
    }

    @Test
    public void noTracking_ReturnsNewInstanceEachCall() {
        ParseErrorList first = ParseErrorList.noTracking();
        ParseErrorList second = ParseErrorList.noTracking();
        assertNotSame("Each call should return a new instance", first, second);
        assertEquals(first.getMaxSize(), second.getMaxSize());
    }

    // -------------------------------------------------------------------------
    //  tracking(int) basic tests
    // -------------------------------------------------------------------------
    @Test
    public void tracking_ShouldSetMaxSizeAndAllowAdd() {
        int max = 5;
        ParseErrorList list = ParseErrorList.tracking(max);
        assertEquals("maxSize should match argument", max, list.getMaxSize());
        assertTrue("canAddError() must be true on empty list", list.canAddError());
    }

    @Test
    public void tracking_ZeroMax_ShouldNeverAllowAdd() {
        ParseErrorList list = ParseErrorList.tracking(0);
        assertEquals(0, list.getMaxSize());
        assertFalse(list.canAddError());
        addDummyError(list);
        assertFalse(list.canAddError());
    }

    @Test
    public void tracking_NegativeMax_ShouldNeverAllowAdd() {
        ParseErrorList list = ParseErrorList.tracking(-5);
        assertEquals(-5, list.getMaxSize());
        assertFalse("Negative maxSize must make canAddError false immediately", list.canAddError());
        addDummyError(list);
        assertFalse(list.canAddError());
    }

    // -------------------------------------------------------------------------
    //  canAddError() behaviour
    // -------------------------------------------------------------------------
    @Test
    public void canAddError_BelowAndAtLimit() {
        int max = 3;
        ParseErrorList list = ParseErrorList.tracking(max);
        // Fill up to max-1
        for (int i = 0; i < max - 1; i++) {
            assertTrue("Should be able to add before reaching max", list.canAddError());
            addDummyError(list);
        }
        // Exactly at max-1, still true
        assertTrue(list.canAddError());
        // Add the last allowed element
        addDummyError(list);
        assertEquals(max, list.size());
        // Now we are at the limit
        assertFalse("After reaching maxSize, canAddError must be false", list.canAddError());
    }

    @Test
    public void canAddError_AfterRemovingElements() {
        int max = 2;
        ParseErrorList list = ParseErrorList.tracking(max);
        addDummyError(list);
        addDummyError(list);
        assertFalse(list.canAddError()); // at limit

        // Remove one element and check again
        list.remove(0);
        assertTrue("After removing, we are below maxSize", list.canAddError());
    }

    @Test
    public void canAddError_DoesNotPreventAddBeyondMax() {
        int max = 1;
        ParseErrorList list = ParseErrorList.tracking(max);
        addDummyError(list); // reaches limit
        assertFalse(list.canAddError());

        // Force an extra add – ArrayList permits it
        addDummyError(list);
        assertEquals("Size can exceed maxSize because ArrayList does not block", 2, list.size());
        assertFalse("canAddError must stay false after overflow", list.canAddError());
    }

    // -------------------------------------------------------------------------
    //  getMaxSize() getter test (already used elsewhere but keep explicit)
    // -------------------------------------------------------------------------
    @Test
    public void getMaxSize_ReturnsConstructorValue() {
        ParseErrorList list = ParseErrorList.tracking(7);
        assertEquals(7, list.getMaxSize());
    }

    // -------------------------------------------------------------------------
    //  Initial capacity sanity check (reflection on the backing array)
    // -------------------------------------------------------------------------
    @Test
    public void tracking_InitialCapacityIs16() throws Exception {
        ParseErrorList list = ParseErrorList.tracking(10);
        // The backing array is a private field named "elementData" in ArrayList (Java 8+)
        java.lang.reflect.Field dataField = java.util.ArrayList.class.getDeclaredField("elementData");
        dataField.setAccessible(true);
        Object[] elementData = (Object[]) dataField.get(list);
        assertEquals("Initial capacity of tracking list should be 16", 16, elementData.length);
    }
}
