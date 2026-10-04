###Test START##
package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ParseErrorList}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Factory methods {@code noTracking()} and {@code tracking(int)}</li>
 *   <li>Accessor {@code getMaxSize()}</li>
 *   <li>Logic of {@code canAddError()}</li>
 *   <li>Behaviour of the underlying {@link java.util.ArrayList} regarding the size limit</li>
 * </ul>
 * The tests are written in the same package as {@link ParseErrorList} so that the
 * package‑private constructor can be accessed.
 * </p>
 */
public class ParseErrorListTest {

    /**
     * Helper that adds {@code count} {@code null} entries to the list.
     * {@code null} is acceptable because {@code ParseErrorList} extends
     * {@code ArrayList<ParseError>} and the list permits {@code null} elements.
     */
    private void addNullErrors(ParseErrorList list, int count) {
        for (int i = 0; i < count; i++) {
            list.add(null);
        }
    }

    @Test
    public void testNoTrackingFactoryCreatesZeroMaxSizeList() {
        ParseErrorList list = ParseErrorList.noTracking();

        // maxSize should be 0
        assertEquals("maxSize should be 0 for noTracking()", 0, list.getMaxSize());

        // size starts at 0
        assertEquals("Initial size should be 0", 0, list.size());

        // canAddError should be false because size (0) is not < maxSize (0)
        assertFalse("canAddError() must be false when maxSize is 0", list.canAddError());

        // Adding an element is still possible (ArrayList semantics), but canAddError stays false
        list.add(null);
        assertEquals("Size should be 1 after adding", 1, list.size());
        assertFalse("canAddError() must remain false after adding beyond maxSize", list.canAddError());
    }

    @Test
    public void testTrackingFactoryCreatesListWithGivenMaxSize() {
        int maxSize = 5;
        ParseErrorList list = ParseErrorList.tracking(maxSize);

        // Verify maxSize accessor
        assertEquals("maxSize should match the value passed to tracking()", maxSize, list.getMaxSize());

        // Initially can add errors because size (0) < maxSize (5)
        assertTrue("canAddError() should be true when list is empty", list.canAddError());

        // Add errors up to maxSize - 1 and verify canAddError stays true
        addNullErrors(list, maxSize - 1);
        assertEquals("Size should be maxSize - 1", maxSize - 1, list.size());
        assertTrue("canAddError() should still be true before reaching maxSize", list.canAddError());

        // Add one more to reach the limit
        list.add(null);
        assertEquals("Size should now equal maxSize", maxSize, list.size());
        assertFalse("canAddError() must be false when size == maxSize", list.canAddError());

        // Adding beyond maxSize is allowed by the underlying ArrayList,
        // but canAddError() must continue to return false.
        list.add(null);
        assertEquals("Size should be maxSize + 1 after extra add", maxSize + 1, list.size());
        assertFalse("canAddError() must remain false after exceeding maxSize", list.canAddError());
    }

    @Test
    public void testCanAddErrorLogicEdgeCases() {
        // maxSize of 1
        ParseErrorList list = ParseErrorList.tracking(1);
        assertTrue(list.canAddError());

        // Add first error – now at limit
        list.add(null);
        assertFalse(list.canAddError());

        // Remove an element and verify canAddError becomes true again
        list.remove(0);
        assertEquals("Size should be 0 after removal", 0, list.size());
        assertTrue("canAddError() should be true after removing to below maxSize", list.canAddError());
    }

    @Test
    public void testNegativeMaxSizeBehaviour() {
        // Negative maxSize is not prohibited by the constructor.
        // The logic should treat it as a limit that can never be satisfied.
        ParseErrorList list = new ParseErrorList(10, -3);
        assertEquals(-3, list.getMaxSize());

        // size (0) is not less than -3, so canAddError must be false immediately.
        assertFalse("canAddError() must be false for negative maxSize", list.canAddError());

        // Adding elements does not change the fact that canAddError stays false.
        list.add(null);
        assertEquals(1, list.size());
        assertFalse("canAddError() must remain false after adding elements with negative maxSize", list.canAddError());
    }

    @Test
    public void testInitialCapacityDoesNotRestrictAddingUpToMaxSize() {
        // The INITIAL_CAPACITY constant is internal; we cannot access it directly.
        // However, we can verify that a list created with tracking can hold up to maxSize
        // elements without any IllegalStateException or similar.
        int maxSize = 20;
        ParseErrorList list = ParseErrorList.tracking(maxSize);
        // Fill the list to its maxSize using null placeholders.
        addNullErrors(list, maxSize);
        assertEquals("List should contain maxSize elements", maxSize, list.size());
        assertFalse("canAddError() must be false after reaching maxSize", list.canAddError());
    }
}
###Test END##