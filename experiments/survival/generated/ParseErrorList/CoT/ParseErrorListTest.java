package org.jsoup.parser;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ParseErrorList}.
 * <p>
 * The tests are written in the same package as the class under test so that the
 * package‑private methods {@code canAddError()} and {@code getMaxSize()} can be
 * accessed directly.
 * </p>
 */
public class ParseErrorListTest {

    private ParseErrorList emptyList;      // created by noTracking()
    private ParseErrorList trackingList;   // created by tracking(maxSize)

    @Before
    public void setUp() {
        emptyList = ParseErrorList.noTracking();
        trackingList = ParseErrorList.tracking(3); // a small, non‑zero max size
    }

    /* --------------------------------------------------------------------- */
    /*  Basic tests for the two public factory methods                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void noTracking_returnsListWithZeroMaxSize() {
        assertNotNull("noTracking() must not return null", emptyList);
        assertEquals("maxSize for noTracking() must be 0", 0, emptyList.getMaxSize());
        assertEquals("Initial size must be 0", 0, emptyList.size());
        assertFalse("canAddError() must be false when maxSize is 0", emptyList.canAddError());
    }

    @Test
    public void tracking_returnsListWithGivenMaxSize() {
        assertNotNull("tracking() must not return null", trackingList);
        assertEquals("maxSize must match the value passed to tracking()", 3, trackingList.getMaxSize());
        assertEquals("Initial size must be 0", 0, trackingList.size());
        assertTrue("canAddError() must be true when size < maxSize", trackingList.canAddError());
    }

    /* --------------------------------------------------------------------- */
    /*  Edge‑case tests for canAddError()                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void canAddError_isTrueWhenBelowMaxSize() {
        // size is 0, maxSize is 3 → should be able to add
        assertTrue(trackingList.canAddError());

        // add two elements (nulls are allowed by ArrayList)
        trackingList.add(null);
        trackingList.add(null);
        assertEquals(2, trackingList.size());
        assertTrue("still below maxSize, canAddError() should be true", trackingList.canAddError());
    }

    @Test
    public void canAddError_isFalseWhenAtMaxSize() {
        // Fill the list up to its maxSize
        trackingList.add(null);
        trackingList.add(null);
        trackingList.add(null);
        assertEquals(3, trackingList.size());
        assertFalse("size == maxSize → canAddError() must be false", trackingList.canAddError());
    }

    @Test
    public void canAddError_isFalseWhenMaxSizeIsZero() {
        ParseErrorList zeroMax = ParseErrorList.tracking(0);
        assertEquals(0, zeroMax.getMaxSize());
        assertFalse("maxSize 0 → canAddError() must be false", zeroMax.canAddError());
    }

    @Test
    public void canAddError_isFalseWhenMaxSizeIsNegative() {
        ParseErrorList negativeMax = ParseErrorList.tracking(-5);
        assertEquals(-5, negativeMax.getMaxSize());
        // size (0) is NOT less than a negative maxSize
        assertFalse("negative maxSize → canAddError() must be false", negativeMax.canAddError());
    }

    /* --------------------------------------------------------------------- */
    /*  Behaviour of add() relative to maxSize (the class does NOT enforce)  */
    /* --------------------------------------------------------------------- */

    @Test
    public void addDoesNotPreventExceedingMaxSize() {
        // The list reports it cannot add more, but the underlying ArrayList still accepts elements.
        trackingList.add(null);
        trackingList.add(null);
        trackingList.add(null); // now at maxSize
        assertFalse(trackingList.canAddError());

        // Adding another element should still succeed (no exception)
        trackingList.add(null);
        assertEquals("Size should be maxSize+1 because add() is not guarded", 4, trackingList.size());
    }

    @Test
    public void noTrackingListCanStillAcceptElements() {
        // Even though canAddError() is false, the underlying list can hold items.
        emptyList.add(null);
        assertEquals("Size should be 1 even though maxSize is 0", 1, emptyList.size());
        assertFalse("canAddError() remains false", emptyList.canAddError());
    }

    /* --------------------------------------------------------------------- */
    /*  Additional sanity checks (inheritance from ArrayList)                */
    /* --------------------------------------------------------------------- */

    @Test
    public void listIsInstanceOfArrayList() {
        assertTrue("ParseErrorList must be an ArrayList", emptyList instanceof java.util.ArrayList);
    }

    @Test
    public void clearResetsSizeAndCanAddError() {
        trackingList.add(null);
        trackingList.add(null);
        assertEquals(2, trackingList.size());
        trackingList.clear();
        assertEquals(0, trackingList.size());
        assertTrue("After clear, canAddError() should be true again", trackingList.canAddError());
    }
}
