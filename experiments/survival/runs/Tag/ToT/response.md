###Test START##
```java
package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Tag}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal and edge cases for {@code valueOf}</li>
 *   <li>All boolean property getters</li>
 *   <li>Equality, hashCode and toString contracts</li>
 *   <li>Static utility methods</li>
 *   <li>Exception handling for invalid arguments</li>
 * </ul>
 */
public class TagTest {

    /* --------------------------------------------------------------------- *
     *  valueOf(..) – normal / case‑insensitive behaviour
     * --------------------------------------------------------------------- */

    @Test
    public void testValueOf_KnownTag_CaseInsensitive_WithHtmlDefaultSettings() {
        // ParseSettings.htmlDefault normalises tag names to lower‑case.
        Tag lower = Tag.valueOf("div", ParseSettings.htmlDefault);
        Tag mixed = Tag.valueOf("DiV", ParseSettings.htmlDefault);
        // Both must be the same pre‑registered instance.
        assertSame("Case‑insensitive lookup should return the registered instance",
                lower, mixed);
        assertTrue(lower.isBlock());
        assertTrue(mixed.isKnownTag());
    }

    @Test
    public void testValueOf_KnownTag_CaseSensitive_PreserveCaseSettings() {
        // PreserveCase does not normalise – "DIV" is unknown.
        Tag lower = Tag.valueOf("div", ParseSettings.preserveCase);
        Tag upper = Tag.valueOf("DIV", ParseSettings.preserveCase);
        // lower is the registered block tag, upper is a newly created generic tag.
        assertNotSame(lower, upper);
        assertTrue(lower.isKnownTag());
        assertFalse(upper.isKnownTag());

        // generic tags are inline (isBlock == false) by default
        assertFalse(upper.isBlock());
        assertTrue(upper.isInline());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOf_NullTagName_Throws() {
        Tag.valueOf(null, ParseSettings.htmlDefault);
    }

    @Test
    public void testValueOf_EmptyAfterNormalization_Throws() {
        // ParseSettings.htmlDefault will trim and lower‑case; an empty string becomes empty.
        try {
            Tag.valueOf("   ", ParseSettings.htmlDefault);
            fail("Expected IllegalArgumentException for empty tag name after normalisation");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testValueOf_UnknownTag_ReturnsGenericInlineTag() {
        Tag unknown = Tag.valueOf("my‑custom‑tag", ParseSettings.htmlDefault);
        assertEquals("my‑custom‑tag", unknown.getName());
        assertFalse(unknown.isKnownTag());
        assertFalse(unknown.isBlock());          // generic tags are inline
        assertTrue(unknown.isInline());
        assertFalse(unknown.isEmpty());
        assertFalse(unknown.isSelfClosing());
        assertFalse(unknown.preserveWhitespace());
    }

    /* --------------------------------------------------------------------- *
     *  Boolean property getters – known tags
     * --------------------------------------------------------------------- */

    @Test
    public void testBlockAndInlineProperties() {
        Tag p = Tag.valueOf("p");
        assertTrue("p should be a block element", p.isBlock());
        assertFalse("p should not be inline", p.isInline());

        Tag span = Tag.valueOf("span");
        assertFalse("span should be inline", span.isBlock());
        assertTrue("span should be inline", span.isInline());
    }

    @Test
    public void testFormatAsBlockAndInlineFlags() {
        Tag p = Tag.valueOf("p");
        // p is a block element but listed in formatAsInlineTags
        assertFalse("p should be formatted as inline", p.formatAsBlock());

        Tag div = Tag.valueOf("div");
        assertTrue("div should be formatted as block", div.formatAsBlock());
    }

    @Test
    public void testEmptyTagCharacteristics() {
        Tag img = Tag.valueOf("img");
        assertTrue("img must be empty", img.isEmpty());
        assertFalse("img cannot contain inline content", img.canContainInline);
        assertTrue("img is self‑closing (empty)", img.isSelfClosing());

        // verify that a non‑empty, non‑self‑closing tag is not self‑closing
        Tag a = Tag.valueOf("a");
        assertFalse(a.isEmpty());
        assertTrue(a.isSelfClosing() == false);
    }

    @Test
    public void testPreserveWhitespaceTags() {
        Tag pre = Tag.valueOf("pre");
        assertTrue("pre should preserve whitespace", pre.preserveWhitespace());

        Tag title = Tag.valueOf("title");
        assertTrue("title should preserve whitespace", title.preserveWhitespace());

        Tag div = Tag.valueOf("div");
        assertFalse("div should not preserve whitespace", div.preserveWhitespace());
    }

    @Test
    public void testFormRelatedFlags() {
        Tag input = Tag.valueOf("input");
        assertTrue("input should be listed in a form", input.isFormListed());
        assertTrue("input should be submittable", input.isFormSubmittable());

        Tag button = Tag.valueOf("button");
        assertTrue("button is listed in a form", button.isFormListed());
        assertFalse("button is not submittable", button.isFormSubmittable());

        Tag div = Tag.valueOf("div");
        assertFalse("div is not a form listed element", div.isFormListed());
        assertFalse("div is not a form submittable element", div.isFormSubmittable());
    }

    @Test
    public void testIsDataTag() {
        // A data‑only tag is !canContainInline && !isEmpty()
        // Use reflection to create a tag that matches that condition.
        Tag custom = Tag.valueOf("customdata");
        setPrivateField(custom, "canContainInline", false);
        setPrivateField(custom, "empty", false);

        assertTrue("customdata should be identified as data‑only", custom.isData());
    }

    /* --------------------------------------------------------------------- *
     *  Known‑tag static helpers
     * --------------------------------------------------------------------- */

    @Test
    public void testIsKnownTag_InstanceAndStatic() {
        Tag p = Tag.valueOf("p");
        assertTrue(p.isKnownTag());
        assertTrue(Tag.isKnownTag("p"));
        assertFalse(Tag.isKnownTag("nonexistent"));
    }

    /* --------------------------------------------------------------------- *
     *  Equality, hashCode and toString
     * --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode_SameInstance() {
        Tag div1 = Tag.valueOf("div");
        Tag div2 = Tag.valueOf("div");
        assertEquals(div1, div2);
        assertEquals(div1.hashCode(), div2.hashCode());
    }

    @Test
    public void testEqualsAndHashCode_DifferentInstancesSameName() {
        // Two generic tags with the same unknown name are equal
        Tag a = Tag.valueOf("foobar");
        Tag b = Tag.valueOf("foobar");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testEquals_DifferentNames() {
        Tag img = Tag.valueOf("img");
        Tag br = Tag.valueOf("br");
        assertNotEquals(img, br);
    }

    @Test
    public void testEquals_NullAndOtherClass() {
        Tag span = Tag.valueOf("span");
        assertNotEquals(span, null);
        assertNotEquals(span, "span");
    }

    @Test
    public void testToStringReturnsTagName() {
        Tag script = Tag.valueOf("script");
        assertEquals("script", script.toString());
    }

    /* --------------------------------------------------------------------- *
     *  Helper to set private fields via reflection (used for isData test)
     * --------------------------------------------------------------------- */
    private void setPrivateField(Tag tag, String fieldName, Object value) {
        try {
            Field field = Tag.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(tag, value);
        } catch (Exception e) {
            throw new RuntimeException("Reflection failed for field: " + fieldName, e);
        }
    }
}
```
###Test END##