package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link ParseErrorList}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public factory methods {@code noTracking()} and {@code tracking(int)}</li>
 *   <li>Accessor {@code getMaxSize()}</li>
 *   <li>Package‑private {@code canAddError()} logic under normal, boundary and
 *       erroneous conditions.</li>
 *   <li>Behaviour when adding elements up to the declared maximum size.</li>
 * </ul>
 *
 * All tests are placed in the same package as {@code ParseErrorList} so that the
 * package‑private constructor and {@code canAddError()} method are accessible.
 */
public class ParseErrorListTest {

    /** Helper that creates a dummy {@code ParseError} instance.
     *  The real {@code ParseError} class is part of jsoup; for the purpose of
     *  these tests we only need a non‑null object, so a simple anonymous subclass
     *  is sufficient.
     */
    private static ParseError dummyError() {
        // The actual constructor signature is unknown; using an anonymous subclass works
        // because the list only stores the reference.
        return new ParseError() {};
    }

    /* -------------------------------------------------------------------- */
    /* 1. Factory methods                                                    */
    /* -------------------------------------------------------------------- */

    @Test
    public void testNoTrackingCreatesZeroSizedList() {
        ParseErrorList list = ParseErrorList.noTracking();
        assertNotNull("Factory should never return null", list);
        assertEquals("maxSize for noTracking should be 0", 0, list.getMaxSize());
        assertFalse("canAddError should be false when maxSize is 0", list.canAddError());
        assertTrue("Initially the list must be empty", list.isEmpty());
    }

    @Test
    public void testTrackingCreatesListWithInitialCapacityAndMaxSize() {
        int max = 10;
        ParseErrorList list = ParseErrorList.tracking(max);
        assertNotNull(list);
        assertEquals("maxSize should match the argument passed to tracking()", max, list.getMaxSize());
        assertTrue("canAddError should be true when list is empty and maxSize > 0", list.canAddError());
        assertTrue("Initially the list must be empty", list.isEmpty());
    }

    /* -------------------------------------------------------------------- */
    /* 2. getMaxSize()                                                       */
    /* -------------------------------------------------------------------- */

    @Test
    public void testGetMaxSizeReturnsConstructorValue() {
        ParseErrorList list = new ParseErrorList(5, 7); // package‑private ctor, same package
        assertEquals(7, list.getMaxSize());
    }

    /* -------------------------------------------------------------------- */
    /* 3. canAddError() – normal and edge cases                              */
    /* -------------------------------------------------------------------- */

    @Test
    public void testCanAddErrorWhenBelowMaxSize() {
        ParseErrorList list = ParseErrorList.tracking(3);
        assertTrue(list.canAddError()); // 0 < 3

        list.add(dummyError()); // size = 1
        assertTrue(list.canAddError()); // 1 < 3

        list.add(dummyError()); // size = 2
        assertTrue(list.canAddError()); // 2 < 3
    }

    @Test
    public void testCanAddErrorWhenAtMaxSize() {
        ParseErrorList list = ParseErrorList.tracking(2);
        list.add(dummyError()); // size = 1
        list.add(dummyError()); // size = 2
        assertFalse("canAddError must be false when size == maxSize", list.canAddError());
    }

    @Test
    public void testCanAddErrorWhenAboveMaxSize() {
        // The class itself does not prevent adding beyond maxSize;
        // canAddError() should simply return false once the limit is exceeded.
        ParseErrorList list = ParseErrorList.tracking(1);
        list.add(dummyError()); // size = 1 (reaches max)
        assertFalse(list.canAddError());

        // Force an over‑size situation
        list.add(dummyError()); // size = 2 (> max)
        assertFalse("canAddError must stay false after exceeding maxSize", list.canAddError());
    }

    @Test
    public void testCanAddErrorWithNegativeMaxSize() {
        ParseErrorList list = new ParseErrorList(5, -5);
        assertEquals(-5, list.getMaxSize());
        // size() starts at 0, 0 < -5 is false
        assertFalse("canAddError should be false for any negative maxSize", list.canAddError());

        // Adding elements does not change the result
        list.add(dummyError());
        assertFalse("canAddError must remain false after adding elements when maxSize is negative", list.canAddError());
    }

    @Test
    public void testCanAddErrorWithMaxSizeIntegerMaxValue() {
        ParseErrorList list = ParseErrorList.tracking(Integer.MAX_VALUE);
        assertTrue(list.canAddError());

        // Add a few elements to prove it still works
        for (int i = 0; i < 5; i++) {
            list.add(dummyError());
            assertTrue("canAddError should stay true when far below Integer.MAX_VALUE", list.canAddError());
        }
    }

    /* -------------------------------------------------------------------- */
    /* 4. Adding elements up to the limit                                     */
    /* -------------------------------------------------------------------- */

    @Test
    public void testAddElementsUpToMaxSizeStopsCanAddError() {
        int max = 4;
        ParseErrorList list = ParseErrorList.tracking(max);
        for (int i = 0; i < max; i++) {
            assertTrue("canAddError should be true before adding element #" + i, list.canAddError());
            list.add(dummyError());
        }
        assertEquals("List size must equal maxSize after adding maxSize elements", max, list.size());
        assertFalse("canAddError must be false after reaching maxSize", list.canAddError());
    }

    @Test
    public void testAddingBeyondMaxSizeDoesNotThrow() {
        // The class does not enforce the limit, so adding beyond maxSize should be safe.
        ParseErrorList list = ParseErrorList.tracking(2);
        list.add(dummyError());
        list.add(dummyError());
        // No exception expected here
        list.add(dummyError());
        assertEquals("Size should reflect all added elements even beyond maxSize", 3, list.size());
    }

    /* -------------------------------------------------------------------- */
    /* 5. Verify that the underlying ArrayList behaviour is intact            */
    /* -------------------------------------------------------------------- */

    @Test
    public void testArrayListOperationsStillWork() {
        ParseErrorList list = ParseErrorList.tracking(5);
        // add, get, remove, clear
        list.add(dummyError());
        list.add(dummyError());
        assertEquals(2, list.size());
        assertNotNull(list.get(0));
        assertNotNull(list.get(1));

        ParseError removed = list.remove(0);
        assertNotNull(removed);
        assertEquals(1, list.size());

        list.clear();
        assertTrue(list.isEmpty());
    }
}
