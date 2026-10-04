###Test START##
```java
package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;
import org.jsoup.parser.Parser;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.CharsetEncoder;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Entities}.
 *
 * The tests cover:
 * <ul>
 *   <li>Lookup of named entities (base & extended).</li>
 *   <li>Retrieval of characters / code‑points, including multi‑character entities.</li>
 *   <li>HTML escaping (both overloads) and special cases (null, attribute context, non‑ASCII chars).</li>
 *   <li>HTML un‑escaping (strict / non‑strict).</li>
 *   <li>Core charset detection and fast‑path {@code canEncode} logic.</li>
 *   <li>Internal loading of the entity tables (indirectly via the public API).</li>
 * </ul>
 *
 * The test class is deliberately self‑contained – it does not rely on the internal data files
 * directly, but on the public contract of {@link Entities}.  All tests run with the real
 * jsoup {@code EntitiesData} resources that ship with the library.
 */
public class EntitiesTest {

    private Document.OutputSettings defaultOut;
    private Document.OutputSettings xhtmlOut;

    @Before
    public void setUp() {
        // Default settings use UTF‑8 and the base escape mode.
        defaultOut = new Document.OutputSettings();

        // Create an OutputSettings that uses the XHTML escape mode (restricted set).
        xhtmlOut = new Document.OutputSettings();
        xhtmlOut.escapeMode(Entities.EscapeMode.xhtml);
    }

    /* --------------------------------------------------------------------- *
     *  1.  Named‑entity lookup                                            *
     * --------------------------------------------------------------------- */

    @Test
    public void testIsNamedEntity_KnownAndUnknown() {
        assertTrue("Extended set must recognise &lt;", Entities.isNamedEntity("lt"));
        assertTrue("Extended set must recognise &NotEqual;", Entities.isNamedEntity("NotEqual"));
        assertFalse("Unknown name must return false", Entities.isNamedEntity("nonexistent"));
    }

    @Test
    public void testIsBaseNamedEntity_KnownAndUnknown() {
        assertTrue("Base set must recognise &amp;", Entities.isBaseNamedEntity("amp"));
        // “NotEqual” is part of the extended set only.
        assertFalse("Base set must not recognise extended only entity", Entities.isBaseNamedEntity("NotEqual"));
        assertFalse("Unknown name must return false", Entities.isBaseNamedEntity("xyz"));
    }

    /* --------------------------------------------------------------------- *
     *  2.  Character retrieval                                            *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetCharacterByName() {
        // Deprecated method – still returns the BMP character.
        Character ch = Entities.getCharacterByName("lt");
        assertNotNull(ch);
        assertEquals('<', (char) ch);
        // For an unknown name it returns the null character (codepoint 0).
        assertEquals(Character.valueOf((char) 0), Entities.getCharacterByName("unknown"));
    }

    @Test
    public void testGetByName_SingleAndMultiCharacter() {
        // Simple known entity.
        assertEquals("<", Entities.getByName("lt"));
        assertEquals("&", Entities.getByName("amp"));

        // Multi‑character entity – according to the comment in load() the name
        // “NotNestedLessLess” maps to two code points.
        String multi = Entities.getByName("NotNestedLessLess");
        assertNotNull("Multi‑character entity must be present", multi);
        assertEquals(2, multi.codePointCount(0, multi.length()));
        // Verify the exact code points (U+226A and U+226A, i.e. “≪” repeated).
        int[] expected = {0x226A, 0x226A};
        int[] actual = {multi.codePointAt(0), multi.codePointAt(Character.charCount(expected[0]))};
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testCodepointsForName() {
        int[] buf = new int[2];

        // Single‑code‑point entity.
        int count = Entities.codepointsForName("gt", buf);
        assertEquals(1, count);
        assertEquals('>', buf[0]);

        // Multi‑code‑point entity.
        count = Entities.codepointsForName("NotNestedLessLess", buf);
        assertEquals(2, count);
        assertEquals(0x226A, buf[0]); // first ‘≪’
        assertEquals(0x226A, buf[1]); // second ‘≪’
    }

    /* --------------------------------------------------------------------- *
     *  3.  HTML escaping                                                 *
     * --------------------------------------------------------------------- */

    @Test
    public void testEscape_NullInput() {
        assertEquals("", Entities.escape((String) null));
    }

    @Test
    public void testEscape_BasicCharacters() {
        String raw = "a & b < c > d \" e";
        String escaped = Entities.escape(raw, defaultOut);
        // Expected: &amp; for &, &lt; for <, &gt; for >, &quot; inside attribute context only.
        assertEquals("a &amp; b &lt; c &gt; d \" e", escaped);
    }

    @Test
    public void testEscape_AttributeContext() throws IOException {
        // Use the internal escape method via reflection to toggle inAttribute = true.
        Method m = Entities.class.getDeclaredMethod(
                "escape", Appendable.class, String.class, Document.OutputSettings.class,
                boolean.class, boolean.class, boolean.class);
        m.setAccessible(true);

        StringBuilder out = new StringBuilder();
        String src = "\"value & <test>\"";

        // In attribute mode: quotes and ampersand must be escaped, < and > are left as‑is.
        m.invoke(null, out, src, defaultOut, true, false, false);
        assertEquals("&quot;value &amp; <test>&quot;", out.toString());
    }

    @Test
    public void testEscape_NonAsciiCharacter_UtfEncoding() {
        // The euro sign (€) is not ASCII but UTF‑8 can encode it, so it should pass through.
        String raw = "price €10";
        String escaped = Entities.escape(raw, defaultOut);
        assertEquals(raw, escaped);
    }

    @Test
    public void testEscape_NonAsciiCharacter_AsciiEncoding() {
        // Force ASCII encoding – characters outside 0x7F must be escaped as numeric entities.
        Document.OutputSettings asciiOut = new Document.OutputSettings();
        asciiOut.charset("US-ASCII");
        asciiOut.escapeMode(Entities.EscapeMode.base); // base mode for entity names.

        String raw = "price €10";
        String escaped = Entities.escape(raw, asciiOut);
        // Euro sign should become &#x20ac; (hexadecimal numeric entity).
        assertEquals("price &#x20ac;10", escaped);
    }

    @Test
    public void testEscape_XhtmlMode_Nbsp() {
        // In XHTML mode, non‑breaking space must be escaped as a numeric entity.
        String raw = "\u00A0"; // NBSP
        String escaped = Entities.escape(raw, xhtmlOut);
        assertEquals("&#xa0;", escaped);
    }

    /* --------------------------------------------------------------------- *
     *  4.  HTML un‑escaping                                               *
     * --------------------------------------------------------------------- */

    @Test
    public void testUnescape_Simple() {
        assertEquals("<", Entities.unescape("&lt;"));
        assertEquals("&", Entities.unescape("&amp;"));
        // Missing semicolon is allowed when strict = false (the public API).
        assertEquals("<", Entities.unescape("&lt"));
    }

    @Test
    public void testUnescape_StrictMode() throws Exception {
        // The private overload supports strict mode – we invoke it via reflection.
        Method m = Entities.class.getDeclaredMethod("unescape", String.class, boolean.class);
        m.setAccessible(true);
        // With strict = true a missing semicolon must stay unchanged.
        String result = (String) m.invoke(null, "&lt", true);
        assertEquals("&lt", result);
    }

    /* --------------------------------------------------------------------- *
     *  5.  Core charset detection & fast‑path encoding check             *
     * --------------------------------------------------------------------- */

    @Test
    public void testCoreCharset_byName() {
        assertEquals(Entities.CoreCharset.ascii, Entities.CoreCharset.byName("US-ASCII"));
        assertEquals(Entities.CoreCharset.utf, Entities.CoreCharset.byName("UTF-8"));
        assertEquals(Entities.CoreCharset.utf, Entities.CoreCharset.byName("UTF-16LE"));
        assertEquals(Entities.CoreCharset.fallback, Entities.CoreCharset.byName("ISO-8859-1"));
    }

    @Test
    public void testCanEncode_FastPath() throws Exception {
        // Access the private static method via reflection.
        Method canEncode = Entities.class.getDeclaredMethod("canEncode",
                Entities.CoreCharset.class, char.class, CharsetEncoder.class);
        canEncode.setAccessible(true);

        // ASCII charset – only chars < 0x80 are encodable.
        boolean asciiLow = (boolean) canEncode.invoke(null,
                Entities.CoreCharset.ascii, (char) 0x7F, null);
        boolean asciiHigh = (boolean) canEncode.invoke(null,
                Entities.CoreCharset.ascii, (char) 0x80, null);
        assertTrue(asciiLow);
        assertFalse(asciiHigh);

        // UTF charset – all characters are considered encodable.
        boolean utfAny = (boolean) canEncode.invoke(null,
                Entities.CoreCharset.utf, (char) 0xFFFF, null);
        assertTrue(utfAny);
    }

    /* --------------------------------------------------------------------- *
     *  6.  Entity tables are loaded correctly (indirect test)              *
     * --------------------------------------------------------------------- */

    @Test
    public void testEntityTablesLoaded() {
        // The static initializer should have populated the base and extended tables.
        // Verify that a known base entity exists and that the size is greater than zero.
        assertTrue("Base entity table must contain at least one entry",
                Entities.EscapeMode.base.size() > 0);
        assertTrue("Extended entity table must contain at least one entry",
                Entities.EscapeMode.extended.size() > 0);

        // Spot‑check that the internal arrays are parallel and searchable.
        String[] baseKeys = Entities.EscapeMode.base.nameKeys;
        int[] baseVals = Entities.EscapeMode.base.codeVals;
        assertEquals("nameKeys and codeVals must have the same length",
                baseKeys.length, baseVals.length);
        // The first entry should be “lt” (by definition of the data file ordering).
        assertEquals("lt", baseKeys[0]);
        assertEquals('<', (char) baseVals[0]);
    }

    /* --------------------------------------------------------------------- *
     *  7.  Defensive programming – verify that null/empty strings behave   *
     * --------------------------------------------------------------------- */

    @Test
    public void testEscape_EmptyString() {
        assertEquals("", Entities.escape("", defaultOut));
    }

    @Test
    public void testGetByName_EmptyString() {
        // Empty name should resolve to empty string (no entity).
        assertEquals("", Entities.getByName(""));
    }

    @Test
    public void testCodepointsForName_EmptyString() {
        int[] buf = new int[2];
        int count = Entities.codepointsForName("", buf);
        assertEquals(0, count);
    }

    @Test(expected = SerializationException.class)
    public void testEscape_IOExceptionPropagation() throws Exception {
        // Force an IOException by passing a faulty Appendable.
        Appendable badAppendable = new Appendable() {
            @Override public Appendable append(CharSequence csq) throws IOException {
                throw new IOException("forced");
            }
            @Override public Appendable append(CharSequence csq, int start, int end) throws IOException {
                throw new IOException("forced");
            }
            @Override public Appendable append(char c) throws IOException {
                throw new IOException("forced");
            }
        };
        // Use reflection to call the internal escape method.
        Method m = Entities.class.getDeclaredMethod(
                "escape", Appendable.class, String.class, Document.OutputSettings.class,
                boolean.class, boolean.class, boolean.class);
        m.setAccessible(true);
        try {
            m.invoke(null, badAppendable, "test", defaultOut, false, false, false);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // The wrapper should re‑throw a SerializationException.
            throw e.getCause();
        }
    }
}
```
###Test END##