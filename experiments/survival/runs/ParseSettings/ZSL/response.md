###Test START##
package org.jsoup.parser;

import org.jsoup.nodes.Attributes;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit4 tests for {@link ParseSettings}.
 */
public class ParseSettingsTest {

    /**
     * Verify the static constants are correctly initialized.
     */
    @Test
    public void testStaticInstances() {
        // htmlDefault should NOT preserve tag or attribute case.
        ParseSettings html = ParseSettings.htmlDefault;
        assertNotNull(html);
        // Use reflection to check internal flags (optional, but we can infer via behavior)
        String tag = html.normalizeTag("DiV");
        String attr = html.normalizeAttribute("ClAsS");
        assertEquals("div", tag);   // lower‑cased
        assertEquals("class", attr); // lower‑cased

        // preserveCase should preserve both tag and attribute case.
        ParseSettings preserve = ParseSettings.preserveCase;
        assertNotNull(preserve);
        tag = preserve.normalizeTag("DiV");
        attr = preserve.normalizeAttribute("ClAsS");
        assertEquals("DiV", tag);   // original case retained
        assertEquals("ClAsS", attr); // original case retained
    }

    /**
     * Test normalizeTag with various inputs when case preservation is disabled.
     */
    @Test
    public void testNormalizeTagHtmlDefault() {
        ParseSettings settings = new ParseSettings(false, false); // same as htmlDefault

        // Simple lower‑casing
        assertEquals("p", settings.normalizeTag("P"));
        // Trimming whitespace
        assertEquals("div", settings.normalizeTag("  DIV  "));
        // Already lower case stays unchanged
        assertEquals("span", settings.normalizeTag("span"));
        // Mixed case
        assertEquals("h1", settings.normalizeTag("h1"));
        // Empty string after trim
        assertEquals("", settings.normalizeTag("   "));
    }

    /**
     * Test normalizeTag when tag case preservation is enabled.
     */
    @Test
    public void testNormalizeTagPreserveCase() {
        ParseSettings settings = new ParseSettings(true, false);

        // No lower‑casing, only trim
        assertEquals("P", settings.normalizeTag("P"));
        assertEquals("DiV", settings.normalizeTag("  DiV  "));
        assertEquals("", settings.normalizeTag("   "));
    }

    /**
     * Test normalizeAttribute with case preservation disabled (default HTML behavior).
     */
    @Test
    public void testNormalizeAttributeHtmlDefault() {
        ParseSettings settings = new ParseSettings(false, false);

        // Lower‑casing
        assertEquals("href", settings.normalizeAttribute("HREF"));
        // Trimming whitespace
        assertEquals("src", settings.normalizeAttribute("  SRC  "));
        // Already lower case stays unchanged
        assertEquals("alt", settings.normalizeAttribute("alt"));
        // Mixed case
        assertEquals("data-test", settings.normalizeAttribute("Data-Test"));
        // Empty string after trim
        assertEquals("", settings.normalizeAttribute("   "));
    }

    /**
     * Test normalizeAttribute when attribute case preservation is enabled.
     */
    @Test
    public void testNormalizeAttributePreserveCase() {
        ParseSettings settings = new ParseSettings(false, true);

        // No lower‑casing, only trim
        assertEquals("HREF", settings.normalizeAttribute("HREF"));
        assertEquals("Src", settings.normalizeAttribute("  Src  "));
        assertEquals("", settings.normalizeAttribute("   "));
    }

    /**
     * Verify that normalizeAttributes lower‑cases attribute keys when preservation is disabled.
     */
    @Test
    public void testNormalizeAttributesHtmlDefault() {
        ParseSettings settings = new ParseSettings(false, false);
        Attributes attrs = new Attributes();
        attrs.put("ClAsS", "myClass");
        attrs.put("ID", "myId");
        attrs.put("data-TeSt", "value");

        // Before normalization, keys retain original case (jsoup stores as given)
        assertTrue(attrs.hasKey("ClAsS"));
        assertTrue(attrs.hasKey("ID"));
        assertTrue(attrs.hasKey("data-TeSt"));

        // Perform normalization
        Attributes result = settings.normalizeAttributes(attrs);
        assertSame(attrs, result); // should return the same instance

        // After normalization, all keys should be lower‑cased
        assertTrue(result.hasKey("class"));
        assertTrue(result.hasKey("id"));
        assertTrue(result.hasKey("data-test"));

        // Original mixed‑case keys should no longer exist
        assertFalse(result.hasKey("ClAsS"));
        assertFalse(result.hasKey("ID"));
        assertFalse(result.hasKey("data-TeSt"));
    }

    /**
     * Verify that normalizeAttributes does NOT modify attribute keys when preservation is enabled.
     */
    @Test
    public void testNormalizeAttributesPreserveCase() {
        ParseSettings settings = new ParseSettings(false, true);
        Attributes attrs = new Attributes();
        attrs.put("ClAsS", "myClass");
        attrs.put("ID", "myId");

        Attributes result = settings.normalizeAttributes(attrs);
        assertSame(attrs, result);

        // Keys should remain exactly as they were added
        assertTrue(result.hasKey("ClAsS"));
        assertTrue(result.hasKey("ID"));
        assertFalse(result.hasKey("class"));
        assertFalse(result.hasKey("id"));
    }

    /**
     * Additional sanity check: a custom combination (preserve tag case, not attribute case).
     */
    @Test
    public void testCustomSettingsCombination() {
        ParseSettings settings = new ParseSettings(true, false);

        // Tag case preserved
        assertEquals("MyTag", settings.normalizeTag("  MyTag  "));

        // Attribute case not preserved (lower‑cased)
        assertEquals("myattr", settings.normalizeAttribute("  MyAttr  "));
    }

    /**
     * Ensure that constructor correctly stores the flags (indirectly verified via behavior).
     */
    @Test
    public void testConstructorBehavior() {
        // Both true
        ParseSettings bothPreserve = new ParseSettings(true, true);
        assertEquals("Tag", bothPreserve.normalizeTag(" Tag "));
        assertEquals("Attr", bothPreserve.normalizeAttribute(" Attr "));

        // Both false
        ParseSettings bothLower = new ParseSettings(false, false);
        assertEquals("tag", bothLower.normalizeTag(" Tag "));
        assertEquals("attr", bothLower.normalizeAttribute(" Attr "));
    }
}
###Test END##