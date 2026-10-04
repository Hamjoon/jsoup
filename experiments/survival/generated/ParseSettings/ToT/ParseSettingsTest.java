package org.jsoup.parser;

import org.jsoup.nodes.Attributes;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit 4 test suite for {@link ParseSettings}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Default static instances (htmlDefault, preserveCase)</li>
 *   <li>Constructor behaviour</li>
 *   <li>normalizeTag – trimming, case handling, empty/whitespace strings, null handling</li>
 *   <li>normalizeAttribute – same aspects as normalizeTag</li>
 *   <li>normalizeAttributes – verifies that {@link Attributes#normalize()} is called only when appropriate</li>
 * </ul>
 *
 * <p>All tests are placed in the same package as {@code ParseSettings} so that the
 * package‑private methods can be accessed directly.
 */
public class ParseSettingsTest {

    private ParseSettings defaultSettings;   // preserveTagCase = false, preserveAttributeCase = false
    private ParseSettings preserveSettings; // preserveTagCase = true,  preserveAttributeCase = true

    @Before
    public void setUp() {
        defaultSettings = new ParseSettings(false, false);
        preserveSettings = new ParseSettings(true, true);
    }

    /* -------------------------------------------------
     *  Tests for the static factory instances
     * ------------------------------------------------- */

    @Test
    public void htmlDefault_isConfiguredCorrectly() {
        assertNotNull(ParseSettings.htmlDefault);
        // htmlDefault should NOT preserve case for tag or attribute
        assertEquals("div", ParseSettings.htmlDefault.normalizeTag("DIV"));
        assertEquals("href", ParseSettings.htmlDefault.normalizeAttribute("HREF"));
    }

    @Test
    public void preserveCase_isConfiguredCorrectly() {
        assertNotNull(ParseSettings.preserveCase);
        // preserveCase should keep original case
        assertEquals("DIV", ParseSettings.preserveCase.normalizeTag("DIV"));
        assertEquals("HREF", ParseSettings.preserveCase.normalizeAttribute("HREF"));
    }

    /* -------------------------------------------------
     *  normalizeTag tests
     * ------------------------------------------------- */

    @Test
    public void normalizeTag_trimsWhitespaceAndLowerCasesWhenNotPreserved() {
        String input = "  DiV  ";
        String expected = "div";
        assertEquals(expected, defaultSettings.normalizeTag(input));
    }

    @Test
    public void normalizeTag_preservesCaseWhenFlagIsTrue() {
        String input = "  DiV  ";
        String expected = "DiV";
        assertEquals(expected, preserveSettings.normalizeTag(input));
    }

    @Test
    public void normalizeTag_emptyString() {
        assertEquals("", defaultSettings.normalizeTag(""));
        assertEquals("", preserveSettings.normalizeTag(""));
    }

    @Test
    public void normalizeTag_whitespaceOnly() {
        // trim turns it into empty string; lower‑casing empty string is still empty
        assertEquals("", defaultSettings.normalizeTag("   "));
        assertEquals("", preserveSettings.normalizeTag("   "));
    }

    @Test(expected = NullPointerException.class)
    public void normalizeTag_nullInput_throwsNPE() {
        defaultSettings.normalizeTag(null);
    }

    /* -------------------------------------------------
     *  normalizeAttribute tests
     * ------------------------------------------------- */

    @Test
    public void normalizeAttribute_trimsWhitespaceAndLowerCasesWhenNotPreserved() {
        String input = "  ClAsS  ";
        String expected = "class";
        assertEquals(expected, defaultSettings.normalizeAttribute(input));
    }

    @Test
    public void normalizeAttribute_preservesCaseWhenFlagIsTrue() {
        String input = "  ClAsS  ";
        String expected = "ClAsS";
        assertEquals(expected, preserveSettings.normalizeAttribute(input));
    }

    @Test
    public void normalizeAttribute_emptyString() {
        assertEquals("", defaultSettings.normalizeAttribute(""));
        assertEquals("", preserveSettings.normalizeAttribute(""));
    }

    @Test
    public void normalizeAttribute_whitespaceOnly() {
        assertEquals("", defaultSettings.normalizeAttribute("   "));
        assertEquals("", preserveSettings.normalizeAttribute("   "));
    }

    @Test(expected = NullPointerException.class)
    public void normalizeAttribute_nullInput_throwsNPE() {
        defaultSettings.normalizeAttribute(null);
    }

    /* -------------------------------------------------
     *  normalizeAttributes tests
     * ------------------------------------------------- */

    /**
     * Helper subclass that records whether {@link Attributes#normalize()} was invoked.
     */
    private static class SpyAttributes extends Attributes {
        private boolean normalizeCalled = false;

        @Override
        public Attributes normalize() {
            normalizeCalled = true;
            // we still want the real normalisation behaviour for completeness
            return super.normalize();
        }

        boolean isNormalizeCalled() {
            return normalizeCalled;
        }
    }

    @Test
    public void normalizeAttributes_callsNormalizeWhenPreserveAttributeCaseIsFalse() {
        SpyAttributes attrs = new SpyAttributes();
        // add a mixed‑case attribute to see that real normalisation also works
        attrs.put("HeAdEr", "value");

        Attributes returned = defaultSettings.normalizeAttributes(attrs);
        assertSame("Method should return the same Attributes instance", attrs, returned);
        assertTrue("Attributes.normalize() should have been called", attrs.isNormalizeCalled());

        // After normalisation, attribute key should be lower‑cased
        assertEquals("value", attrs.get("header"));
        assertNull("Original mixed‑case key should be gone", attrs.get("HeAdEr"));
    }

    @Test
    public void normalizeAttributes_doesNotCallNormalizeWhenPreserveAttributeCaseIsTrue() {
        SpyAttributes attrs = new SpyAttributes();
        attrs.put("HeAdEr", "value");

        Attributes returned = preserveSettings.normalizeAttributes(attrs);
        assertSame("Method should return the same Attributes instance", attrs, returned);
        assertFalse("Attributes.normalize() must NOT have been called", attrs.isNormalizeCalled());

        // Since normalisation was skipped, the key remains unchanged
        assertEquals("value", attrs.get("HeAdEr"));
        assertNull("Lower‑cased key must not exist", attrs.get("header"));
    }

    @Test(expected = NullPointerException.class)
    public void normalizeAttributes_nullInput_throwsNPE() {
        defaultSettings.normalizeAttributes(null);
    }
}
