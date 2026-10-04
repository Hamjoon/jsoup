###Test START##
package org.jsoup.parser;

import org.jsoup.nodes.Attributes;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit4 test suite for {@link ParseSettings}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public API (constructor, static instances)</li>
 *   <li>Basic functionality of {@code normalizeTag}, {@code normalizeAttribute},
 *       and {@code normalizeAttributes}</li>
 *   <li>Edge‑case handling (null, empty strings, leading/trailing whitespace)</li>
 *   <li>Behaviour when case‑preservation flags are toggled</li>
 *   <li>Exception scenarios (NullPointerException for null arguments)</li>
 * </ul>
 */
public class ParseSettingsTest {

    /* --------------------------------------------------------------------- */
    /*  1.  Constructor & static instances                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConstructorSetsFlags() {
        ParseSettings settings = new ParseSettings(true, false);
        // Using reflection to verify the private fields (since there are no getters)
        try {
            java.lang.reflect.Field tagField = ParseSettings.class.getDeclaredField("preserveTagCase");
            java.lang.reflect.Field attrField = ParseSettings.class.getDeclaredField("preserveAttributeCase");
            tagField.setAccessible(true);
            attrField.setAccessible(true);
            assertTrue((Boolean) tagField.get(settings));
            assertFalse((Boolean) attrField.get(settings));
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    @Test
    public void testHtmlDefaultStaticInstance() {
        ParseSettings html = ParseSettings.htmlDefault;
        assertNotNull(html);
        // htmlDefault should *not* preserve tag or attribute case
        // Verify via behaviour of normalizeTag / normalizeAttribute
        assertEquals("div", html.normalizeTag("DIV"));
        assertEquals("class", html.normalizeAttribute("CLASS"));
    }

    @Test
    public void testPreserveCaseStaticInstance() {
        ParseSettings preserve = ParseSettings.preserveCase;
        assertNotNull(preserve);
        // preserveCase should keep original case
        assertEquals("DIV", preserve.normalizeTag("DIV"));
        assertEquals("CLASS", preserve.normalizeAttribute("CLASS"));
    }

    /* --------------------------------------------------------------------- */
    /*  2.  normalizeTag tests                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNormalizeTagPreserveCase() {
        ParseSettings settings = new ParseSettings(true, false);
        assertEquals("DiV", settings.normalizeTag(" DiV "));
        assertEquals("", settings.normalizeTag("   "));
    }

    @Test
    public void testNormalizeTagLowerCase() {
        ParseSettings settings = new ParseSettings(false, false);
        assertEquals("div", settings.normalizeTag(" DiV "));
        assertEquals("", settings.normalizeTag("   "));
    }

    @Test(expected = NullPointerException.class)
    public void testNormalizeTagNull() {
        ParseSettings settings = new ParseSettings(false, false);
        settings.normalizeTag(null);
    }

    /* --------------------------------------------------------------------- */
    /*  3.  normalizeAttribute tests                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNormalizeAttributePreserveCase() {
        ParseSettings settings = new ParseSettings(false, true);
        assertEquals("ClAsS", settings.normalizeAttribute(" ClAsS "));
        assertEquals("", settings.normalizeAttribute("   "));
    }

    @Test
    public void testNormalizeAttributeLowerCase() {
        ParseSettings settings = new ParseSettings(false, false);
        assertEquals("class", settings.normalizeAttribute(" ClAsS "));
        assertEquals("", settings.normalizeAttribute("   "));
    }

    @Test(expected = NullPointerException.class)
    public void testNormalizeAttributeNull() {
        ParseSettings settings = new ParseSettings(false, false);
        settings.normalizeAttribute(null);
    }

    /* --------------------------------------------------------------------- */
    /*  4.  normalizeAttributes tests                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNormalizeAttributesWhenPreserveIsFalse() {
        ParseSettings settings = new ParseSettings(false, false);
        Attributes attrs = new Attributes();
        attrs.put("ClAsS", "value1");
        attrs.put("ID", "value2");

        Attributes returned = settings.normalizeAttributes(attrs);
        assertSame(attrs, returned); // should return same instance

        // After normalization, attribute keys should be lower‑cased
        assertTrue(attrs.hasKey("class"));
        assertTrue(attrs.hasKey("id"));
        assertFalse(attrs.hasKey("ClAsS"));
        assertFalse(attrs.hasKey("ID"));
    }

    @Test
    public void testNormalizeAttributesWhenPreserveIsTrue() {
        ParseSettings settings = new ParseSettings(false, true);
        Attributes attrs = new Attributes();
        attrs.put("ClAsS", "value1");
        attrs.put("ID", "value2");

        Attributes returned = settings.normalizeAttributes(attrs);
        assertSame(attrs, returned);

        // Keys must remain exactly as they were added
        assertTrue(attrs.hasKey("ClAsS"));
        assertTrue(attrs.hasKey("ID"));
        assertFalse(attrs.hasKey("class"));
        assertFalse(attrs.hasKey("id"));
    }

    @Test(expected = NullPointerException.class)
    public void testNormalizeAttributesNull() {
        ParseSettings settings = new ParseSettings(false, false);
        settings.normalizeAttributes(null);
    }

    /* --------------------------------------------------------------------- */
    /*  5.  Combined usage scenario                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCombinedNormalizationScenario() {
        // Simulate a parser that lower‑cases tags but preserves attribute case
        ParseSettings settings = new ParseSettings(false, true);

        String rawTag = "  HeAdEr  ";
        String rawAttr = "  DaTa-Id  ";

        assertEquals("header", settings.normalizeTag(rawTag));
        assertEquals("DaTa-Id", settings.normalizeAttribute(rawAttr));

        Attributes attrs = new Attributes();
        attrs.put("DaTa-Id", "123");
        attrs.put("Other", "456");

        settings.normalizeAttributes(attrs);
        // attribute names should stay unchanged because preserveAttributeCase == true
        assertTrue(attrs.hasKey("DaTa-Id"));
        assertTrue(attrs.hasKey("Other"));
    }
}
###Test END##