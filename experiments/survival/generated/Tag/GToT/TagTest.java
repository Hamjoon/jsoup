package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 tests for {@link Tag}.
 *
 * The tests cover:
 *   • All public methods (basic behaviour)
 *   • Edge cases such as null arguments, unknown tags, case‑sensitivity
 *   • Property combinations (block/inline, empty, self‑closing, whitespace preservation, form handling)
 *   • Equality, hashCode and toString contracts
 */
public class TagTest {

    /* --------------------------------------------------------------------- *
     *  Public method list (extracted from Tag)
     * --------------------------------------------------------------------- *
     *  String               getName()
     *  static Tag           valueOf(String tagName, ParseSettings settings)
     *  static Tag           valueOf(String tagName)
     *  boolean              isBlock()
     *  boolean              formatAsBlock()
     *  @Deprecated boolean  canContainBlock()
     *  boolean              isInline()
     *  boolean              isData()
     *  boolean              isEmpty()
     *  boolean              isSelfClosing()
     *  boolean              isKnownTag()
     *  static boolean       isKnownTag(String tagName)
     *  boolean              preserveWhitespace()
     *  boolean              isFormListed()
     *  boolean              isFormSubmittable()
     *  Tag                  setSelfClosing()          (package‑private)
     *  boolean              equals(Object o)
     *  int                  hashCode()
     *  String               toString()
     * --------------------------------------------------------------------- */

    /** Basic getter test. */
    @Test
    public void testGetName() {
        Tag p = Tag.valueOf("p");
        assertEquals("p", p.getName());
    }

    /** Known tag lookup – case insensitive via the default {@link ParseSettings}. */
    @Test
    public void testValueOfKnownTag() {
        Tag divLower = Tag.valueOf("div");
        Tag divUpper = Tag.valueOf("DIV");                // default settings ignore case
        assertSame("Pre‑registered tags must be identical objects", divLower, divUpper);
        assertTrue("Div is a block element", divLower.isBlock());
        assertTrue("Div should be formatted as a block", divLower.formatAsBlock());
    }

    /** Unknown tag – should be created on‑the‑fly and behave as an inline tag. */
    @Test
    public void testValueOfUnknownTag() {
        Tag unknown = Tag.valueOf("mycustomtag");
        assertEquals("mycustomtag", unknown.getName());
        assertFalse("Unknown tags are treated as inline", unknown.isBlock());
        assertFalse("Unknown tags are not registered as known tags", unknown.isKnownTag());
        assertFalse("Static check must also report unknown", Tag.isKnownTag("mycustomtag"));
    }

    /** {@code null} tag name must trigger a NPE (Validate.notNull). */
    @Test(expected = NullPointerException.class)
    public void testValueOfNullTagName() {
        Tag.valueOf(null);
    }

    /** Inline tag example – verify isInline() is the logical opposite of isBlock(). */
    @Test
    public void testIsInline() {
        Tag span = Tag.valueOf("span");
        assertFalse("Span is not a block element", span.isBlock());
        assertTrue("Span must be reported as inline", span.isInline());
    }

    /** Empty tags (e.g. img) must report correctly for isEmpty() and isSelfClosing(). */
    @Test
    public void testEmptyAndSelfClosingTags() {
        Tag img = Tag.valueOf("img");
        assertTrue("Img is an empty tag", img.isEmpty());
        assertTrue("Img must be self‑closing", img.isSelfClosing());

        Tag br = Tag.valueOf("br");
        assertTrue("Br is an empty tag", br.isEmpty());
        assertTrue("Br must be self‑closing", br.isSelfClosing());

        Tag div = Tag.valueOf("div");
        assertFalse("Div is not empty", div.isEmpty());
        assertFalse("Div must not be self‑closing", div.isSelfClosing());
    }

    /** Data‑only tag detection – a tag that cannot contain inline and is not empty. */
    @Test
    public void testIsData() {
        Tag script = Tag.valueOf("script");
        // script can contain inline data nodes, so isData() is false
        assertFalse("Script is not a data‑only tag", script.isData());

        // Create a synthetic unknown tag and force it to be data‑only via reflection
        Tag custom = Tag.valueOf("customdata");
        try {
            java.lang.reflect.Field canContainInline = Tag.class.getDeclaredField("canContainInline");
            java.lang.reflect.Field empty = Tag.class.getDeclaredField("empty");
            canContainInline.setAccessible(true);
            empty.setAccessible(true);
            canContainInline.setBoolean(custom, false);
            empty.setBoolean(custom, false);
        } catch (Exception e) {
            fail("Reflection failed while configuring custom data‑only tag: " + e);
        }
        assertTrue("Custom tag configured as data‑only must return true", custom.isData());
    }

    /** Whitespace‑preserving tags (pre, textarea, etc.). */
    @Test
    public void testPreserveWhitespace() {
        Tag pre = Tag.valueOf("pre");
        assertTrue("Pre must preserve whitespace", pre.preserveWhitespace());

        Tag textarea = Tag.valueOf("textarea");
        assertTrue("Textarea must preserve whitespace", textarea.preserveWhitespace());

        Tag p = Tag.valueOf("p");
        assertFalse("Paragraph must not preserve whitespace", p.preserveWhitespace());
    }

    /** Form‑related flags – listed vs submittable. */
    @Test
    public void testFormFlags() {
        Tag input = Tag.valueOf("input");
        assertTrue("Input is listed in forms", input.isFormListed());
        assertTrue("Input is submittable", input.isFormSubmittable());

        Tag button = Tag.valueOf("button");
        assertTrue("Button is listed in forms", button.isFormListed());
        assertFalse("Button is not submittable (not in submit list)", button.isFormSubmittable());

        Tag div = Tag.valueOf("div");
        assertFalse("Div is not listed in forms", div.isFormListed());
        assertFalse("Div is not submittable", div.isFormSubmittable());
    }

    /** Equality and hashCode contracts for both known and unknown tags. */
    @Test
    public void testEqualsAndHashCode() {
        Tag a1 = Tag.valueOf("a");
        Tag a2 = Tag.valueOf("a");
        assertEquals("Known tags with same name must be equal", a1, a2);
        assertEquals("Hash codes must match for equal tags", a1.hashCode(), a2.hashCode());

        Tag unknown1 = Tag.valueOf("unknown");
        Tag unknown2 = Tag.valueOf("unknown");
        assertEquals("Unknown tags with same name must be equal", unknown1, unknown2);
        assertEquals("Hash codes must match for equal unknown tags", unknown1.hashCode(), unknown2.hashCode());

        Tag div = Tag.valueOf("div");
        assertNotEquals("Different tags must not be equal", a1, div);
    }

    /** toString() must return the raw tag name. */
    @Test
    public void testToString() {
        Tag h1 = Tag.valueOf("h1");
        assertEquals("toString should return the tag name", "h1", h1.toString());
    }

    /** Static helper isKnownTag(String). */
    @Test
    public void testStaticIsKnownTag() {
        assertTrue("p is a known tag", Tag.isKnownTag("p"));
        assertFalse("nonexistent is not a known tag", Tag.isKnownTag("nonexistent"));
    }

    /** Package‑private setSelfClosing() – exercised via reflection. */
    @Test
    public void testSetSelfClosing() {
        Tag custom = Tag.valueOf("customSelfClose");
        // invoke the package‑private method
        try {
            java.lang.reflect.Method m = Tag.class.getDeclaredMethod("setSelfClosing");
            m.setAccessible(true);
            Tag returned = (Tag) m.invoke(custom);
            assertSame("setSelfClosing should return the same instance", custom, returned);
            assertTrue("After invoking setSelfClosing() the tag must be self‑closing", custom.isSelfClosing());
        } catch (Exception e) {
            fail("Reflection error while testing setSelfClosing(): " + e);
        }
    }

    /** Edge case: empty string after normalization should throw IllegalArgumentException.
     *  We simulate this by providing a custom ParseSettings implementation that returns an empty string. */
    @Test(expected = IllegalArgumentException.class)
    public void testValueOfEmptyAfterNormalization() {
        ParseSettings emptyNormalizingSettings = new ParseSettings(true) {
            @Override
            public String normalizeTag(String tagName) {
                return "";                 // force empty result
            }
        };
        Tag.valueOf("any", emptyNormalizingSettings);
    }
}
