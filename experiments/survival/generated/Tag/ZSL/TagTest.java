package org.jsoup.parser;

import org.junit.Test;
import org.junit.BeforeClass;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit4 test suite for {@link Tag}.
 */
public class TagTest {

    /**
     * Ensure the static initialisation of {@link Tag} has been executed before any tests run.
     * The static block in {@code Tag} registers all known tags.
     */
    @BeforeClass
    public static void setUpClass() {
        // No explicit set‑up required – referencing Tag forces class loading.
        Class<?> ignored = Tag.class;
    }

    /* --------------------------------------------------------------------- */
    /*  Known block tag – "p"                                               */
    /* --------------------------------------------------------------------- */
    @Test
    public void testBlockTagProperties() {
        Tag p1 = Tag.valueOf("p");
        Tag p2 = Tag.valueOf("p");

        // same instance from registry
        assertSame("Repeated valueOf should return the same cached instance", p1, p2);

        // basic characteristics
        assertTrue("p should be a block element", p1.isBlock());
        assertFalse("p should not be inline", p1.isInline());
        assertEquals("p should be formatted as inline (formatAsBlock = false)", false, p1.formatAsBlock());
        assertEquals("canContainBlock delegates to isBlock", p1.isBlock(), p1.canContainBlock());

        // data / empty / self‑closing checks
        assertFalse("p is not a data‑only tag", p1.isData());
        assertFalse("p is not empty", p1.isEmpty());
        assertFalse("p is not self‑closing", p1.isSelfClosing());

        // registration checks
        assertTrue("p must be a known tag (instance)", p1.isKnownTag());
        assertTrue("p must be a known tag (static)", Tag.isKnownTag("p"));

        // whitespace / form related flags
        assertFalse("p does not preserve whitespace", p1.preserveWhitespace());
        assertFalse("p is not form listed", p1.isFormListed());
        assertFalse("p is not form submittable", p1.isFormSubmittable());

        // toString returns the tag name
        assertEquals("toString should return the tag name", "p", p1.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  Known inline tag – "img"                                            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testInlineEmptyTagProperties() {
        Tag img = Tag.valueOf("img");

        assertFalse("img is not a block element", img.isBlock());
        assertTrue("img is inline", img.isInline());
        assertFalse("img formatAsBlock flag should be false for inline tags", img.formatAsBlock());

        // img is defined as empty and cannot contain inline content
        assertTrue("img is empty", img.isEmpty());
        assertTrue("img is self‑closing (empty)", img.isSelfClosing());
        assertFalse("img is not a data‑only tag", img.isData());

        // registration
        assertTrue("img must be a known tag", img.isKnownTag());
        assertTrue("static known‑tag check for img", Tag.isKnownTag("img"));

        // form flags – img is not form related
        assertFalse("img is not form listed", img.isFormListed());
        assertFalse("img is not form submittable", img.isFormSubmittable());

        // whitespace
        assertFalse("img does not preserve whitespace", img.preserveWhitespace());

        // toString
        assertEquals("toString should return the tag name", "img", img.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  Tag that preserves whitespace – "pre"                               */
    /* --------------------------------------------------------------------- */
    @Test
    public void testPreserveWhitespaceTag() {
        Tag pre = Tag.valueOf("pre");

        assertTrue("pre is a block element", pre.isBlock());
        assertFalse("pre formatAsBlock should be false (listed in formatAsInlineTags)", pre.formatAsBlock());
        assertTrue("pre must preserve whitespace", pre.preserveWhitespace());

        // other flags
        assertFalse("pre is not empty", pre.isEmpty());
        assertFalse("pre is not self‑closing", pre.isSelfClosing());
        assertFalse("pre is not a data‑only tag", pre.isData());
    }

    /* --------------------------------------------------------------------- */
    /*  Form‑related tag – "input"                                          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testFormListedAndSubmittableTag() {
        Tag input = Tag.valueOf("input");

        assertFalse("input is inline", input.isBlock());
        assertTrue("input is form listed", input.isFormListed());
        assertTrue("input is form submittable", input.isFormSubmittable());

        // input is empty and self‑closing
        assertTrue("input is empty", input.isEmpty());
        assertTrue("input is self‑closing", input.isSelfClosing());
    }

    /* --------------------------------------------------------------------- */
    /*  Unknown tag handling – case normalisation                           */
    /* --------------------------------------------------------------------- */
    @Test
    public void testUnknownTagNormalization() {
        // Use the normalising settings (lower‑case) – the tag name will be lower‑cased.
        Tag unknown1 = Tag.valueOf("MyCustomTag", ParseSettings.normaliseTag);
        Tag unknown2 = Tag.valueOf("mycustomtag", ParseSettings.normaliseTag);
        Tag unknown3 = Tag.valueOf("MYCUSTOMTAG", ParseSettings.normaliseTag);

        // All three should be equal (same name after normalisation)
        assertEquals("Normalized unknown tags should be equal", unknown1, unknown2);
        assertEquals("All normalized unknown tags should be equal", unknown2, unknown3);
        assertEquals("Hash codes must be consistent for equal tags", unknown1.hashCode(), unknown2.hashCode());

        // They must NOT be the same instance (unknown tags are not cached)
        assertNotSame("Unknown tags are not cached, instances differ", unknown1, unknown2);

        // Verify default characteristics for a generic unknown tag
        assertFalse("Unknown tag defaults to inline (isBlock = false)", unknown1.isBlock());
        assertTrue("Unknown tag should be inline", unknown1.isInline());
        assertFalse("Unknown tag is not empty", unknown1.isEmpty());
        assertFalse("Unknown tag is not self‑closing", unknown1.isSelfClosing());
        assertFalse("Unknown tag does not preserve whitespace", unknown1.preserveWhitespace());
        assertFalse("Unknown tag is not a known tag", unknown1.isKnownTag());
        assertFalse("Static check for unknown tag", Tag.isKnownTag("mycustomtag"));
    }

    /* --------------------------------------------------------------------- */
    /*  Equality and hashCode contract on known tags                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCodeForKnownTags() {
        Tag div1 = Tag.valueOf("div");
        Tag div2 = Tag.valueOf("div");

        // reflexive, symmetric, transitive via same instance
        assertTrue(div1.equals(div2));
        assertTrue(div2.equals(div1));
        assertEquals(div1.hashCode(), div2.hashCode());

        // inequality with different tag
        Tag span = Tag.valueOf("span");
        assertFalse("div and span must not be equal", div1.equals(span));
        // hash codes can collide by chance but should differ in normal operation
        assertNotEquals(div1.hashCode(), span.hashCode());
    }

    /* --------------------------------------------------------------------- */
    /*  Defensive checks – null arguments                                   */
    /* --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testValueOfNullTagNameThrows() {
        Tag.valueOf(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfEmptyTagNameAfterNormalizationThrows() {
        // NormaliseTag will trim and lower‑case; an empty string after trimming should fail.
        Tag.valueOf("   ", ParseSettings.normaliseTag);
    }
}
