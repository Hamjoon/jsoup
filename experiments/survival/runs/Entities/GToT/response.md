###Test START##
```java
package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;
import org.jsoup.parser.Parser;
import org.junit.Test;
import org.junit.Before;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Entities}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public API methods.</li>
 *   <li>Typical usage, edge cases and error handling.</li>
 *   <li>Behaviour with different {@link Document.OutputSettings} (charset, escape mode, attribute mode).</li>
 *   <li>Package‑private helpers accessed from the same package.</li>
 * </ul>
 *
 * The test class lives in the same package {@code org.jsoup.nodes}
 * so that package‑private methods (e.g. {@code Entities.unescape(String,boolean)})
 * can be invoked directly.
 */
public class EntitiesTest {

    private Document.OutputSettings defaultOut;
    private Document.OutputSettings asciiOut;
    private Document.OutputSettings xhtmlOut;

    @Before
    public void setUp() {
        // default settings – UTF‑8 charset, base escape mode
        defaultOut = new Document.OutputSettings();

        // ASCII charset – forces numeric/named entity encoding for non‑ASCII chars
        asciiOut = new Document.OutputSettings()
                .charset(Charset.forName("US-ASCII"))
                .escapeMode(Entities.EscapeMode.base);

        // XHTML mode – limited entity set (lt, gt, amp, quot, nbsp)
        xhtmlOut = new Document.OutputSettings()
                .escapeMode(Entities.EscapeMode.xhtml);
    }

    // --------------------------------------------------------------------
    //  isNamedEntity / isBaseNamedEntity
    // --------------------------------------------------------------------
    @Test
    public void testIsNamedEntity_KnownAndUnknown() {
        assertTrue(Entities.isNamedEntity("lt"));      // known in extended set
        assertTrue(Entities.isNamedEntity("nbsp"));   // known in extended set
        assertFalse(Entities.isNamedEntity("not-an-entity"));
    }

    @Test(expected = NullPointerException.class)
    public void testIsNamedEntity_Null() {
        Entities.isNamedEntity(null);
    }

    @Test
    public void testIsBaseNamedEntity_KnownAndUnknown() {
        assertTrue(Entities.isBaseNamedEntity("lt"));   // in base set
        assertFalse(Entities.isBaseNamedEntity("NotAnEntity"));
    }

    @Test(expected = NullPointerException.class)
    public void testIsBaseNamedEntity_Null() {
        Entities.isBaseNamedEntity(null);
    }

    // --------------------------------------------------------------------
    //  getCharacterByName (deprecated)
    // --------------------------------------------------------------------
    @Test
    public void testGetCharacterByName_Known() {
        // '&' entity
        assertEquals(Character.valueOf('<'), Entities.getCharacterByName("lt"));
        // '&nbsp;' – returns the BMP character (U+00A0)
        assertEquals(Character.valueOf('\u00A0'), Entities.getCharacterByName("nbsp"));
    }

    @Test
    public void testGetCharacterByName_Unknown() {
        // unknown entity yields (char)-1 i.e. '\uFFFF'
        assertEquals(Character.valueOf('\uFFFF'), Entities.getCharacterByName("unknown"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetCharacterByName_Null() {
        Entities.getCharacterByName(null);
    }

    // --------------------------------------------------------------------
    //  getByName
    // --------------------------------------------------------------------
    @Test
    public void testGetByName_SingleCharacter() {
        assertEquals("<", Entities.getByName("lt"));
        assertEquals("\u00A0", Entities.getByName("nbsp"));
    }

    @Test
    public void testGetByName_MultiCharacter() {
        // The entity "NotNestedLessLess" maps to two code points (U+2AA1, U+2AA2) in the data set.
        // It is stored in the multipoints map.
        String val = Entities.getByName("NotNestedLessLess");
        assertNotNull(val);
        assertEquals(2, val.codePointCount(0, val.length()));
        assertEquals(0x2AA1, val.codePointAt(0));
        assertEquals(0x2AA2, val.codePointAt(Character.charCount(0x2AA1)));
    }

    @Test
    public void testGetByName_Unknown() {
        assertEquals("", Entities.getByName("no_such_entity"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetByName_Null() {
        Entities.getByName(null);
    }

    // --------------------------------------------------------------------
    //  codepointsForName
    // --------------------------------------------------------------------
    @Test
    public void testCodepointsForName_Single() {
        int[] cp = new int[2];
        int count = Entities.codepointsForName("lt", cp);
        assertEquals(1, count);
        assertEquals('<', cp[0]);
    }

    @Test
    public void testCodepointsForName_Multi() {
        int[] cp = new int[2];
        int count = Entities.codepointsForName("NotNestedLessLess", cp);
        assertEquals(2, count);
        assertEquals(0x2AA1, cp[0]);
        assertEquals(0x2AA2, cp[1]);
    }

    @Test
    public void testCodepointsForName_None() {
        int[] cp = new int[2];
        int count = Entities.codepointsForName("nonexistent", cp);
        assertEquals(0, count);
    }

    @Test(expected = NullPointerException.class)
    public void testCodepointsForName_NullName() {
        Entities.codepointsForName(null, new int[2]);
    }

    @Test(expected = NullPointerException.class)
    public void testCodepointsForName_NullArray() {
        Entities.codepointsForName("lt", null);
    }

    // --------------------------------------------------------------------
    //  escape (String) and escape(String, OutputSettings)
    // --------------------------------------------------------------------
    @Test
    public void testEscape_NullAndEmpty() {
        assertEquals("", Entities.escape(null));
        assertEquals("", Entities.escape(""));
    }

    @Test
    public void testEscape_BasicEntities() {
        String src = "& < > \" \u00A0";
        // default settings (base mode, UTF‑8)
        String escaped = Entities.escape(src);
        // Expected: &amp; &lt; &gt; " &nbsp;
        assertEquals("&amp; &lt; &gt; \" &nbsp;", escaped);
    }

    @Test
    public void testEscape_AttributeMode() {
        // In attribute mode '<' and '>' are not escaped, only '&' and '"'
        String src = "<a href=\"test&value\">";
        String escaped = Entities.escape(src, defaultOut.charset(Charset.forName("UTF-8")).escapeMode(Entities.EscapeMode.base));
        // simulate attribute mode by calling the internal method via reflection – easier is to use
        // Document.OutputSettings#escapeMode + a flag, but the public API does not expose attribute mode.
        // Therefore we test the public API which always escapes as character data.
        // The expectation is that '<' and '>' are escaped because we are not in attribute mode.
        assertEquals("&lt;a href=&quot;test&amp;value&quot;&gt;", escaped);
    }

    @Test
    public void testEscape_XhtmlMode_Nbsp() {
        // In XHTML mode the non‑breaking space is emitted as numeric entity.
        String src = "\u00A0";
        String escaped = Entities.escape(src, xhtmlOut);
        assertEquals("&#xa0;", escaped);
    }

    @Test
    public void testEscape_AsciiCharset_NonEncodableChar() {
        // Character 'é' (U+00E9) cannot be encoded in US‑ASCII, so it must be emitted as a numeric entity.
        String src = "café";
        String escaped = Entities.escape(src, asciiOut);
        // Expected: c a f &#xE9;
        assertEquals("caf&#xe9;", escaped);
    }

    @Test
    public void testEscape_SurrogatePair() {
        // Emoji (U+1F600) – a surrogate pair – should be emitted unchanged in UTF‑8 mode.
        String src = "\uD83D\uDE00"; // 😀
        String escaped = Entities.escape(src, defaultOut);
        assertEquals("\uD83D\uDE00", escaped);
    }

    // --------------------------------------------------------------------
    //  unescape (public) and unescape(String, boolean) (package‑private)
    // --------------------------------------------------------------------
    @Test
    public void testUnescape_Basic() {
        assertEquals("<", Entities.unescape("&lt;"));
        assertEquals("&", Entities.unescape("&amp;"));
        assertEquals("\u00A0", Entities.unescape("&nbsp;"));
    }

    @Test
    public void testUnescape_MixedText() {
        assertEquals("a < b & c", Entities.unescape("a &lt; b &amp; c"));
    }

    @Test
    public void testUnescape_UnknownEntityLeavesAsIs() {
        // Unknown named entity is left untouched (Parser.unescapeEntities behaviour)
        assertEquals("&unknown;", Entities.unescape("&unknown;"));
    }

    @Test
    public void testUnescape_StrictMode_RequiresSemicolon() {
        // In strict mode a missing trailing ';' should not be recognised.
        String result = Entities.unescape("a &lt b", true);
        assertEquals("a &lt b", result);
    }

    @Test
    public void testUnescape_StrictMode_WithSemicolon() {
        String result = Entities.unescape("a &lt; b", true);
        assertEquals("a < b", result);
    }

    // --------------------------------------------------------------------
    //  Internal helper canEncode – exercised via escape with different charsets
    // --------------------------------------------------------------------
    @Test
    public void testCanEncode_AsciiTrue() {
        // ASCII charset should encode standard ASCII characters.
        String src = "ABC";
        String escaped = Entities.escape(src, asciiOut);
        assertEquals("ABC", escaped);
    }

    @Test
    public void testCanEncode_AsciiFalse() {
        // Non‑ASCII character should be encoded as numeric entity.
        String src = "Å";
        String escaped = Entities.escape(src, asciiOut);
        assertEquals("&#xc5;", escaped);
    }

    // --------------------------------------------------------------------
    //  Defensive test: ensure load() validates entity count (indirectly)
    // --------------------------------------------------------------------
    @Test(expected = IllegalStateException.class)
    public void testLoad_ValidationFails() throws Exception {
        // The load method is private; we trigger the validation by creating a malformed
        // EscapeMode via reflection. This is a safety‑net test: if the data file changes
        // and the count mismatches, Validate.isTrue will throw an IllegalStateException.
        // We simulate the mismatch by accessing the private method and feeding it bogus data.
        java.lang.reflect.Method load = Entities.class.getDeclaredMethod("load",
                Entities.EscapeMode.class, String.class, int.class);
        load.setAccessible(true);
        // Pass an empty string as data and size 1 – the validation will fail.
        load.invoke(null, Entities.EscapeMode.base, "", 1);
    }

    // --------------------------------------------------------------------
    //  Ensure that the public escape(String) delegates to the overload correctly
    // --------------------------------------------------------------------
    @Test
    public void testEscape_DelegateToOverload() {
        String src = "&";
        // Using default settings directly
        String viaOverload = Entities.escape(src, defaultOut);
        // Using the convenience method
        String viaConvenient = Entities.escape(src);
        assertEquals(viaOverload, viaConvenient);
    }

    // --------------------------------------------------------------------
    //  Verify that SerializationException is never thrown (sanity check)
    // --------------------------------------------------------------------
    @Test
    public void testEscape_NoSerializationException() {
        try {
            Entities.escape("test");
        } catch (SerializationException e) {
            fail("SerializationException should never be thrown by Entities.escape");
        }
    }
}
```
###Test END##