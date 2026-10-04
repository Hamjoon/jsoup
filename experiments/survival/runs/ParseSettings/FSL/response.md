###Test START##
package org.jsoup.parser;

import org.jsoup.nodes.Attributes;
import org.junit.Test;

import static org.junit.Assert.*;

public class ParseSettingsTest {

    /*** Tests for tag name normalization ***/

    @Test
    public void normalizeTag_htmlDefault_lowerCasesAndTrims() {
        ParseSettings settings = ParseSettings.htmlDefault; // preserveTagCase = false
        assertEquals("foo", settings.normalizeTag("  Foo  "));
        assertEquals("a", settings.normalizeTag("A"));
        assertEquals("", settings.normalizeTag("   "));
        assertEquals("123", settings.normalizeTag(" 123 "));
    }

    @Test
    public void normalizeTag_preserveCase_trimsOnly() {
        ParseSettings settings = ParseSettings.preserveCase; // preserveTagCase = true
        assertEquals("Foo", settings.normalizeTag("  Foo  "));
        assertEquals("BarBaz", settings.normalizeTag("BarBaz"));
        assertEquals("", settings.normalizeTag("   "));
    }

    /*** Tests for attribute name normalization ***/

    @Test
    public void normalizeAttribute_htmlDefault_lowerCasesAndTrims() {
        ParseSettings settings = ParseSettings.htmlDefault; // preserveAttributeCase = false
        assertEquals("href", settings.normalizeAttribute("  HREF  "));
        assertEquals("src", settings.normalizeAttribute("SRC"));
        assertEquals("", settings.normalizeAttribute("   "));
    }

    @Test
    public void normalizeAttribute_preserveCase_trimsOnly() {
        ParseSettings settings = ParseSettings.preserveCase; // preserveAttributeCase = true
        assertEquals("HREF", settings.normalizeAttribute("  HREF  "));
        assertEquals("src", settings.normalizeAttribute("src"));
        assertEquals("", settings.normalizeAttribute("   "));
    }

    /*** Tests for attribute collection normalization ***/

    @Test
    public void normalizeAttributes_htmlDefault_lowerCasesAttributeKeys() {
        ParseSettings settings = ParseSettings.htmlDefault; // attribute case not preserved
        Attributes attrs = new Attributes();
        attrs.put("HeLLo", "world");
        attrs.put("DATA-Value", "123");

        // keep reference for identity check
        Attributes returned = settings.normalizeAttributes(attrs);

        // The same object should be returned
        assertSame(attrs, returned);

        // Keys must be lower‑cased after normalization
        assertTrue(returned.hasKey("hello"));
        assertTrue(returned.hasKey("data-value"));
        assertFalse(returned.hasKey("HeLLo"));
        assertFalse(returned.hasKey("DATA-Value"));

        // Values are unchanged
        assertEquals("world", returned.get("hello"));
        assertEquals("123", returned.get("data-value"));
    }

    @Test
    public void normalizeAttributes_preserveCase_keepsOriginalKeys() {
        ParseSettings settings = ParseSettings.preserveCase; // attribute case preserved
        Attributes attrs = new Attributes();
        attrs.put("HeLLo", "world");
        attrs.put("DATA-Value", "123");

        Attributes returned = settings.normalizeAttributes(attrs);

        // Same instance should be returned
        assertSame(attrs, returned);

        // Keys must stay exactly as they were added
        assertTrue(returned.hasKey("HeLLo"));
        assertTrue(returned.hasKey("DATA-Value"));
        assertFalse(returned.hasKey("hello"));
        assertFalse(returned.hasKey("data-value"));

        // Values are unchanged
        assertEquals("world", returned.get("HeLLo"));
        assertEquals("123", returned.get("DATA-Value"));
    }

    /*** Tests for the static predefined settings ***/

    @Test
    public void htmlDefault_isConfiguredCorrectly() {
        ParseSettings html = ParseSettings.htmlDefault;
        // tag case not preserved → lower case
        assertEquals("div", html.normalizeTag(" DIV "));
        // attribute case not preserved → lower case
        assertEquals("class", html.normalizeAttribute(" CLASS "));
    }

    @Test
    public void preserveCase_isConfiguredCorrectly() {
        ParseSettings preserve = ParseSettings.preserveCase;
        // tag case preserved → only trim
        assertEquals("Div", preserve.normalizeTag(" Div "));
        // attribute case preserved → only trim
        assertEquals("Class", preserve.normalizeAttribute(" Class "));
    }

    /*** Additional edge‑case tests ***/

    @Test
    public void normalizeTag_withOnlyWhitespace_returnsEmptyString() {
        ParseSettings s = new ParseSettings(false, false);
        assertEquals("", s.normalizeTag("   "));
    }

    @Test
    public void normalizeAttribute_withOnlyWhitespace_returnsEmptyString() {
        ParseSettings s = new ParseSettings(false, false);
        assertEquals("", s.normalizeAttribute("\t\n"));
    }

    @Test
    public void normalizeAttributes_nullAttributesHandledGracefully() {
        // The method does not guard against null, so we verify it throws NPE as expected.
        ParseSettings s = ParseSettings.htmlDefault;
        try {
            s.normalizeAttributes(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected behaviour
        }
    }
}
###Test END##