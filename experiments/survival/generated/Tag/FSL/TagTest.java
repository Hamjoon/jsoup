package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;

public class TagTest {

    /** Test that known block tags have the correct default flags. */
    @Test
    public void testKnownBlockTagProperties() {
        Tag p = Tag.valueOf("p");               // block tag, but formatAsBlock overridden
        assertTrue(p.isBlock());
        assertFalse(p.formatAsBlock());         // "p" is in formatAsInlineTags
        assertTrue(p.isInline() == false);
        assertFalse(p.isEmpty());
        assertFalse(p.isSelfClosing());
        assertFalse(p.preserveWhitespace());
        assertFalse(p.isFormListed());
        assertFalse(p.isFormSubmittable());

        Tag div = Tag.valueOf("div");
        assertTrue(div.isBlock());
        assertTrue(div.formatAsBlock());
        assertFalse(div.isInline());
        assertFalse(div.isEmpty());
        assertFalse(div.isSelfClosing());
        assertFalse(div.preserveWhitespace());
        assertFalse(div.isFormListed());
        assertFalse(div.isFormSubmittable());
    }

    /** Test that known inline tags have the correct default flags. */
    @Test
    public void testKnownInlineTagProperties() {
        Tag img = Tag.valueOf("img");
        assertFalse(img.isBlock());
        assertFalse(img.formatAsBlock());
        assertTrue(img.isInline());
        assertTrue(img.isEmpty());
        assertTrue(img.isSelfClosing());        // empty => self‑closing
        assertFalse(img.preserveWhitespace());
        assertFalse(img.isFormListed());
        assertFalse(img.isFormSubmittable());

        Tag a = Tag.valueOf("a");
        assertFalse(a.isBlock());
        assertFalse(a.formatAsBlock());
        assertTrue(a.isInline());
        assertFalse(a.isEmpty());
        assertFalse(a.isSelfClosing());
        assertFalse(a.preserveWhitespace());
        assertFalse(a.isFormListed());
        assertFalse(a.isFormSubmittable());
    }

    /** Test tags that preserve whitespace. */
    @Test
    public void testPreserveWhitespaceTag() {
        Tag pre = Tag.valueOf("pre");
        assertTrue(pre.preserveWhitespace());
        Tag textarea = Tag.valueOf("textarea");
        assertTrue(textarea.preserveWhitespace());

        // A tag that does not preserve whitespace
        Tag span = Tag.valueOf("span");
        assertFalse(span.preserveWhitespace());
    }

    /** Test form‑related flags. */
    @Test
    public void testFormRelatedTags() {
        Tag input = Tag.valueOf("input");
        assertTrue(input.isFormListed());
        assertTrue(input.isFormSubmittable());

        Tag button = Tag.valueOf("button");
        assertTrue(button.isFormListed());
        assertFalse(button.isFormSubmittable()); // button is listed but not submittable

        Tag div = Tag.valueOf("div");
        assertFalse(div.isFormListed());
        assertFalse(div.isFormSubmittable());
    }

    /** Test isKnownTag both static and instance methods. */
    @Test
    public void testIsKnownTag() {
        assertTrue(Tag.isKnownTag("div"));
        assertTrue(Tag.isKnownTag("IMG")); // map is case‑sensitive, so upper‑case should be false
        assertFalse(Tag.isKnownTag("unknownTag"));

        Tag unknown = Tag.valueOf("unknownTag");
        assertFalse(unknown.isKnownTag());
        Tag known = Tag.valueOf("div");
        assertTrue(known.isKnownTag());
    }

    /** Test case‑insensitive lookup using ParseSettings.htmlDefault (normalizes to lower case). */
    @Test
    public void testValueOfCaseInsensitive() {
        Tag lower = Tag.valueOf("div", ParseSettings.htmlDefault);
        Tag upper = Tag.valueOf("DIV", ParseSettings.htmlDefault);
        assertSame("Case‑insensitive lookup should return the same instance", lower, upper);
    }

    /** Test case‑sensitive lookup using ParseSettings.preserveCase. */
    @Test
    public void testValueOfCaseSensitive() {
        Tag lower = Tag.valueOf("div", ParseSettings.preserveCase);
        Tag upper = Tag.valueOf("DIV", ParseSettings.preserveCase);
        assertNotSame("Case‑sensitive lookup should return different instances", lower, upper);
        assertEquals("Names should be preserved", "DIV", upper.getName());
    }

    /** Test unknown tags are created with default (inline) behaviour and are equal by content. */
    @Test
    public void testUnknownTagDefaultsAndEquality() {
        Tag t1 = Tag.valueOf("myCustom", ParseSettings.htmlDefault);
        Tag t2 = Tag.valueOf("mycustom", ParseSettings.htmlDefault); // normalized to same lower‑case name
        assertEquals("Unknown tags with same normalized name should be equal", t1, t2);
        assertEquals("Hash codes must match for equal objects", t1.hashCode(), t2.hashCode());

        // Different unknown names are not equal
        Tag t3 = Tag.valueOf("anotherTag", ParseSettings.htmlDefault);
        assertNotEquals(t1, t3);
    }

    /** Test the isData() logic (inline only tags that are not empty). */
    @Test
    public void testIsData() {
        Tag span = Tag.valueOf("span"); // inline, can contain inline, not empty
        assertFalse(span.isData());

        Tag title = Tag.valueOf("title"); // block, formatAsInline, but not empty
        assertFalse(title.isData());

        // Create a synthetic unknown tag that cannot contain inline and is not empty
        Tag dataTag = new Tag("dataTag");
        // Using reflection to set the fields that are otherwise inaccessible
        setPrivateField(dataTag, "canContainInline", false);
        setPrivateField(dataTag, "empty", false);
        assertTrue(dataTag.isData());
    }

    /** Test self‑closing flag for empty tags and for manually set self‑closing. */
    @Test
    public void testSelfClosing() {
        Tag br = Tag.valueOf("br");
        assertTrue(br.isSelfClosing()); // empty => self‑closing

        Tag custom = Tag.valueOf("customSelfClosing", ParseSettings.htmlDefault).setSelfClosing();
        assertTrue(custom.isSelfClosing());
        assertFalse(custom.isEmpty());
    }

    /** Verify toString returns the tag name. */
    @Test
    public void testToString() {
        Tag h1 = Tag.valueOf("h1");
        assertEquals("h1", h1.toString());

        Tag unknown = Tag.valueOf("myTag");
        assertEquals("mytag", unknown.toString()); // htmlDefault normalizes to lower case
    }

    /** Test equals contract for various built‑in tags. */
    @Test
    public void testEqualsContract() {
        Tag p1 = Tag.valueOf("p");
        Tag p2 = Tag.valueOf("p");
        assertTrue(p1.equals(p2));
        assertEquals(p1.hashCode(), p2.hashCode());

        Tag div = Tag.valueOf("div");
        assertFalse(p1.equals(div));

        // Different properties (formatAsBlock) cause inequality
        Tag pre = Tag.valueOf("pre");
        Tag preCopy = Tag.valueOf("pre");
        assertTrue(pre.equals(preCopy));
        setPrivateField(preCopy, "formatAsBlock", false);
        assertFalse(pre.equals(preCopy));
    }

    // -------------------------------------------------------------------------
    // Helper method to set private fields via reflection (used only for testing).
    // -------------------------------------------------------------------------
    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = Tag.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
