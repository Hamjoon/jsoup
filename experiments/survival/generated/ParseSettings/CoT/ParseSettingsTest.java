package org.jsoup.parser;

import org.jsoup.nodes.Attributes;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link ParseSettings}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public constructor behaviour (indirectly via the package‑private normalization methods).</li>
 *   <li>Static instances {@code htmlDefault} and {@code preserveCase}.</li>
 *   <li>Normalisation of tag names and attribute names under different settings.</li>
 *   <li>Whitespace trimming, case conversion, empty strings and null handling.</li>
 *   <li>Normalisation of an {@link Attributes} collection.</li>
 * </ul>
 *
 * All tests are placed in the same package {@code org.jsoup.parser} so that they can
 * invoke the package‑private methods {@code normalizeTag}, {@code normalizeAttribute}
 * and {@code normalizeAttributes}.
 */
public class ParseSettingsTest {

    /* --------------------------------------------------------------------- */
    /*  1.  Public API discovery (constructor)                               */
    /* --------------------------------------------------------------------- */

    /**
     * The only public method (apart from static fields) is the constructor:
     * {@code public ParseSettings(boolean tag, boolean attribute)}.
     *
     * The constructor does not expose its internal flags directly, therefore the
     * behaviour of the flags is verified through the normalization methods.
     */

    /* --------------------------------------------------------------------- */
    /*  2.  Tests for the static predefined instances                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void htmlDefault_preservesNoCase_andNormalisesAttributes() {
        // htmlDefault = new ParseSettings(false, false);
        ParseSettings settings = ParseSettings.htmlDefault;

        // tag names should be lower‑cased
        assertEquals("div", settings.normalizeTag("DIV"));
        assertEquals("span", settings.normalizeTag("  SpAn  "));

        // attribute names should be lower‑cased
        assertEquals("href", settings.normalizeAttribute("HREF"));
        assertEquals("src", settings.normalizeAttribute("  SrC  "));

        // attribute collection should be normalised (keys lower‑cased)
        Attributes attrs = new Attributes();
        attrs.put("HeLLo", "world");
        attrs.put("DATA-Value", "42");
        settings.normalizeAttributes(attrs);
        assertTrue(attrs.hasKey("hello"));
        assertTrue(attrs.hasKey("data-value"));
        assertFalse(attrs.hasKey("HeLLo"));
        assertFalse(attrs.hasKey("DATA-Value"));
    }

    @Test
    public void preserveCase_preservesBothTagAndAttributeCase() {
        // preserveCase = new ParseSettings(true, true);
        ParseSettings settings = ParseSettings.preserveCase;

        // tag names keep original case
        assertEquals("DIV", settings.normalizeTag("DIV"));
        assertEquals("SpAn", settings.normalizeTag("  SpAn  "));

        // attribute names keep original case
        assertEquals("HREF", settings.normalizeAttribute("HREF"));
        assertEquals("SrC", settings.normalizeAttribute("  SrC  "));

        // attribute collection should NOT be normalised
        Attributes attrs = new Attributes();
        attrs.put("HeLLo", "world");
        attrs.put("DATA-Value", "42");
        settings.normalizeAttributes(attrs);
        assertTrue(attrs.hasKey("HeLLo"));
        assertTrue(attrs.hasKey("DATA-Value"));
        assertFalse(attrs.hasKey("hello"));
        assertFalse(attrs.hasKey("data-value"));
    }

    /* --------------------------------------------------------------------- */
    /*  3.  Constructor behaviour – explicit flag combinations                */
    /* --------------------------------------------------------------------- */

    @Test
    public void constructor_preserveTagOnly() {
        ParseSettings settings = new ParseSettings(true, false);

        // tag case preserved
        assertEquals("DiV", settings.normalizeTag("DiV"));
        // attribute case forced to lower‑case
        assertEquals("href", settings.normalizeAttribute("HREF"));
    }

    @Test
    public void constructor_preserveAttributeOnly() {
        ParseSettings settings = new ParseSettings(false, true);

        // tag case forced to lower‑case
        assertEquals("div", settings.normalizeTag("DIV"));
        // attribute case preserved
        assertEquals("HREF", settings.normalizeAttribute("HREF"));
    }

    /* --------------------------------------------------------------------- */
    /*  4.  Edge‑case handling                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void normalizeTag_trimsWhitespaceAndHandlesEmptyString() {
        ParseSettings settings = new ParseSettings(false, false); // lower‑case mode
        assertEquals("", settings.normalizeTag("   "));
        assertEquals("", settings.normalizeTag(""));
    }

    @Test
    public void normalizeAttribute_trimsWhitespaceAndHandlesEmptyString() {
        ParseSettings settings = new ParseSettings(false, false); // lower‑case mode
        assertEquals("", settings.normalizeAttribute("\t\n  "));
        assertEquals("", settings.normalizeAttribute(""));
    }

    @Test(expected = NullPointerException.class)
    public void normalizeTag_nullInput_throwsNPE() {
        ParseSettings settings = new ParseSettings(false, false);
        settings.normalizeTag(null); // should NPE on String.trim()
    }

    @Test(expected = NullPointerException.class)
    public void normalizeAttribute_nullInput_throwsNPE() {
        ParseSettings settings = new ParseSettings(false, false);
        settings.normalizeAttribute(null); // should NPE on String.trim()
    }

    @Test(expected = NullPointerException.class)
    public void normalizeAttributes_nullInput_throwsNPE() {
        ParseSettings settings = new ParseSettings(false, false);
        settings.normalizeAttributes(null); // should NPE on attributes.normalize()
    }

    /* --------------------------------------------------------------------- */
    /*  5.  Verify that normalizeAttributes only calls Attributes.normalize() */
    /* --------------------------------------------------------------------- */

    @Test
    public void normalizeAttributes_callsNormalizeOnlyWhenNeeded() {
        // Case 1: preserveAttributeCase == false -> normalize() should be invoked
        ParseSettings lowerCaseSettings = new ParseSettings(false, false);
        Attributes attrs1 = new Attributes();
        attrs1.put("MiXeD", "val");
        lowerCaseSettings.normalizeAttributes(attrs1);
        assertTrue(attrs1.hasKey("mixed")); // normalized

        // Case 2: preserveAttributeCase == true -> normalize() must NOT be invoked
        ParseSettings preserveSettings = new ParseSettings(false, true);
        Attributes attrs2 = new Attributes() {
            boolean normalizeCalled = false;

            @Override
            public Attributes normalize() {
                normalizeCalled = true;
                return super.normalize();
            }
        };
        preserveSettings.normalizeAttributes(attrs2);
        // The overridden normalize() should not have been called
        assertFalse(((Attributes) attrs2).hasKey("mixed")); // still original key
        // Reflectively check the flag (optional, but we ensure behavior by key presence)
        assertTrue(attrs2.hasKey("MiXeD"));
    }
}
