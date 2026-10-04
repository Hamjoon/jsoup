package org.jsoup.nodes;

import org.junit.Test;
import org.jsoup.parser.Parser;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Entities}.
 * <p>
 * The tests cover all public static methods and also verify the
 * behaviour of the escaping algorithm with different output settings.
 * </p>
 */
public class EntitiesTest {

    /***  isNamedEntity / isBaseNamedEntity  ***/

    @Test
    public void testIsNamedEntity() {
        // known entity present in the extended set
        assertTrue(Entities.isNamedEntity("lt"));
        assertTrue(Entities.isNamedEntity("amp"));
        // unknown entity
        assertFalse(Entities.isNamedEntity("thisIsNotAnEntity"));
    }

    @Test
    public void testIsBaseNamedEntity() {
        // base set contains a subset of the extended set
        assertTrue(Entities.isBaseNamedEntity("lt"));
        assertTrue(Entities.isBaseNamedEntity("amp"));
        // entity that exists only in the extended set (e.g. "euro")
        assertFalse(Entities.isBaseNamedEntity("euro"));
    }

    /***  getCharacterByName (deprecated)  ***/

    @Test
    public void testGetCharacterByName() {
        // known entity – should return the correct character
        assertEquals('<', (char) Entities.getCharacterByName("lt"));
        assertEquals('>', (char) Entities.getCharacterByName("gt"));
        assertEquals('&', (char) Entities.getCharacterByName("amp"));
        // unknown entity – the method returns (char)-1 (i.e. '\uffff')
        assertEquals('\uFFFF', (char) Entities.getCharacterByName("unknownEntity"));
    }

    /***  getByName – single and multi‑codepoint entities  ***/

    @Test
    public void testGetByNameSingleCodepoint() {
        assertEquals("<", Entities.getByName("lt"));
        assertEquals("&", Entities.getByName("amp"));
        assertEquals("\u00A0", Entities.getByName("nbsp")); // non‑breaking space
    }

    @Test
    public void testGetByNameMultiCodepoint() {
        // “NotNestedLessLess” is a known multi‑codepoint entity (U+226A U+003C)
        String val = Entities.getByName("NotNestedLessLess");
        assertNotNull(val);
        assertEquals(2, val.codePointCount(0, val.length()));
        int[] cps = val.codePoints().toArray();
        assertArrayEquals(new int[]{0x226A, 0x003C}, cps);
    }

    /***  codepointsForName  ***/

    @Test
    public void testCodepointsForNameSingle() {
        int[] out = new int[2];
        int count = Entities.codepointsForName("lt", out);
        assertEquals(1, count);
        assertEquals('<', (char) out[0]);
    }

    @Test
    public void testCodepointsForNameMulti() {
        int[] out = new int[2];
        int count = Entities.codepointsForName("NotNestedLessLess", out);
        assertEquals(2, count);
        assertEquals(0x226A, out[0]);
        assertEquals(0x003C, out[1]);
    }

    @Test
    public void testCodepointsForNameUnknown() {
        int[] out = new int[2];
        int count = Entities.codepointsForName("noSuchEntity", out);
        assertEquals(0, count);
    }

    /***  escape (String) – null handling  ***/

    @Test
    public void testEscapeNull() {
        assertEquals("", Entities.escape(null));
    }

    /***  escape (String, OutputSettings) – basic characters  ***/

    @Test
    public void testEscapeBasicCharacters() {
        Document.OutputSettings out = new Document.OutputSettings(); // defaults: UTF‑8, base mode
        String src = "a & b < c > d \" e";
        String escaped = Entities.escape(src, out);
        // &, <, > must be escaped, " stays because we are not in an attribute
        assertEquals("a &amp; b &lt; c &gt; d \" e", escaped);
    }

    /***  escape – non‑ASCII with ASCII charset (numeric entity fallback) ***/

    @Test
    public void testEscapeNonAsciiWithAsciiCharset() {
        Document.OutputSettings out = new Document.OutputSettings()
                .charset(StandardCharsets.US_ASCII)   // forces fallback for non‑ASCII chars
                .escapeMode(Entities.EscapeMode.base);

        // The character é (U+00E9) has a named entity “eacute” in the extended set,
        // but not in the base set, so it should be emitted as a numeric hex entity.
        String src = "café";
        String escaped = Entities.escape(src, out);
        // Expected: "caf&#xe9;"
        assertTrue(escaped.contains("&#xe9;"));
        assertEquals("caf&#xe9;", escaped);
    }

    /***  escape – XHTML mode forces numeric entity for NBSP ***/

    @Test
    public void testEscapeNbspXhtmlMode() {
        Document.OutputSettings out = new Document.OutputSettings()
                .escapeMode(Entities.EscapeMode.xhtml); // XHTML mode

        String src = "\u00A0"; // non‑breaking space
        String escaped = Entities.escape(src, out);
        // In XHTML mode, NBSP is emitted as a numeric entity, not &nbsp;
        assertEquals("&#xa0;", escaped);
    }

    /***  unescape – basic entities  ***/

    @Test
    public void testUnescapeBasic() {
        String escaped = "a &lt; b &amp; c &gt; d &quot; e";
        String unescaped = Entities.unescape(escaped);
        assertEquals("a < b & c > d \" e", unescaped);
    }

    /***  unescape – tolerant (no trailing ';') ***/

    @Test
    public void testUnescapeWithoutSemicolon() {
        // Parser.unescapeEntities with strict = false accepts missing ';'
        String escaped = "a &lt b &amp c &gt d";
        String unescaped = Entities.unescape(escaped);
        assertEquals("a < b & c > d", unescaped);
    }

    /***  round‑trip test: escape → unescape ***/

    @Test
    public void testEscapeUnescapeRoundTrip() {
        Document.OutputSettings out = new Document.OutputSettings()
                .charset(StandardCharsets.UTF_8)
                .escapeMode(Entities.EscapeMode.base);

        String original = "À ∑ <script>var x = \"&\";</script> ©";
        String escaped = Entities.escape(original, out);
        String roundTrip = Entities.unescape(escaped);
        // The round‑trip should equal the original (aside from possible differences in
        // numeric vs named entities – both decode to the same characters)
        assertEquals(original, roundTrip);
    }

    /***  internal helper – ensure load() populated multipoints correctly ***/

    @Test
    public void testMultipointsPopulation() {
        // Multipoints are stored in the private static map; we can verify via public API.
        // The entity “NotNestedLessLess” is known to be a multipoint.
        assertEquals(2, Entities.codepointsForName("NotNestedLessLess", new int[2]));
    }

    /***  verify that escape respects attribute flag via reflection ***/

    @Test
    public void testEscapeInAttributeContext() throws Exception {
        // The public API does not expose the “inAttribute” flag, but we can
        // invoke the package‑private escape method via reflection to test it.
        Document.OutputSettings out = new Document.OutputSettings()
                .escapeMode(Entities.EscapeMode.base);

        java.lang.reflect.Method m = Entities.class.getDeclaredMethod(
                "escape", Appendable.class, String.class,
                Document.OutputSettings.class, boolean.class, boolean.class, boolean.class);
        m.setAccessible(true);

        StringBuilder accum = new StringBuilder();
        String src = "<a href=\"test\">\"quote\" &</a>";
        // inAttribute = true → double quotes must be escaped, < and > stay as characters
        m.invoke(null, accum, src, out, true, false, false);
        String result = accum.toString();

        // Expected: <a href=&quot;test&quot;>&quot;quote&quot; &amp;</a>
        assertEquals("<a href=&quot;test&quot;>&quot;quote&quot; &amp;</a>", result);
    }

    /***  verify that normaliseWhite works (via reflection) ***/

    @Test
    public void testEscapeNormaliseWhite() throws Exception {
        Document.OutputSettings out = new Document.OutputSettings()
                .escapeMode(Entities.EscapeMode.base);

        java.lang.reflect.Method m = Entities.class.getDeclaredMethod(
                "escape", Appendable.class, String.class,
                Document.OutputSettings.class, boolean.class, boolean.class, boolean.class);
        m.setAccessible(true);

        StringBuilder accum = new StringBuilder();
        // Input contains mixed whitespace (tabs, newlines) – should be collapsed to a single space.
        String src = "Hello\t \n  World";
        m.invoke(null, accum, src, out, false, true, false);
        String result = accum.toString();

        assertEquals("Hello World", result);
    }
}
