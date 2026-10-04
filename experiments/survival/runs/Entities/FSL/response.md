###Test START##
package org.jsoup.nodes;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.jsoup.parser.Parser;
import org.junit.Test;

/**
 * Comprehensive JUnit 4 tests for {@link Entities}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>named‑entity look‑ups (base and extended)</li>
 *   <li>character retrieval by name</li>
 *   <li>multi‑code‑point entities</li>
 *   <li>HTML escaping with different output settings (default, attribute mode, ASCII charset)</li>
 *   <li>HTML un‑escaping (strict vs. non‑strict)</li>
 *   <li>edge cases such as {@code null}, empty strings and unknown entities</li>
 * </ul>
 * </p>
 */
public class EntitiesTest {

    /* --------------------------------------------------------------------- */
    /*  isNamedEntity / isBaseNamedEntity                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsNamedEntity_knownAndUnknown() {
        // known entity in the extended set
        assertTrue(Entities.isNamedEntity("lt"));
        assertTrue(Entities.isNamedEntity("amp"));
        // known entity that is NOT in the base set (e.g. "nbsp" is base, "copy" is base, but "NotEqualTilde" is extended only)
        assertTrue(Entities.isNamedEntity("NotEqualTilde")); // extended‑only entity
        // unknown entity
        assertFalse(Entities.isNamedEntity("thisDoesNotExist"));
        // null should throw NPE (the implementation does not guard against null)
        try {
            Entities.isNamedEntity(null);
            fail("Expected NullPointerException for null argument");
        } catch (NullPointerException expected) {
            // ok
        }
    }

    @Test
    public void testIsBaseNamedEntity_knownAndExtendedOnly() {
        // base entities
        assertTrue(Entities.isBaseNamedEntity("lt"));
        assertTrue(Entities.isBaseNamedEntity("amp"));
        // extended‑only entity – should be false for base set
        assertFalse(Entities.isBaseNamedEntity("NotEqualTilde"));
        // unknown entity
        assertFalse(Entities.isBaseNamedEntity("foobar"));
    }

    /* --------------------------------------------------------------------- */
    /*  getCharacterByName / getByName / codepointsForName                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetCharacterByName_singleCodepoint() {
        // simple ASCII entity
        assertEquals(Character.valueOf('<'), Entities.getCharacterByName("lt"));
        // non‑ASCII entity (non‑breaking space)
        assertEquals(Character.valueOf('\u00A0'), Entities.getCharacterByName("nbsp"));
    }

    @Test
    public void testGetByName_singleAndMultiCodepoint() {
        // single‑codepoint entity
        assertEquals("<", Entities.getByName("lt"));
        // multi‑code‑point entity – the source comment mentions NotNestedLessLess (U+226E U+0338)
        String multi = Entities.getByName("NotNestedLessLess");
        assertNotNull(multi);
        assertEquals(2, multi.codePointCount(0, multi.length()));
        assertEquals(0x226E, multi.codePointAt(0));
        assertEquals(0x0338, multi.codePointAt(Character.offsetByCodePoints(multi, 0, 1)));

        // unknown entity returns empty string
        assertEquals("", Entities.getByName("unknownEntity"));
    }

    @Test
    public void testCodepointsForName_singleAndMulti() {
        int[] cp = new int[2];

        // single codepoint
        int count = Entities.codepointsForName("lt", cp);
        assertEquals(1, count);
        assertEquals('<', cp[0]);

        // multi‑codepoint entity
        count = Entities.codepointsForName("NotNestedLessLess", cp);
        assertEquals(2, count);
        assertEquals(0x226E, cp[0]);
        assertEquals(0x0338, cp[1]);

        // unknown entity
        count = Entities.codepointsForName("noSuchEntity", cp);
        assertEquals(0, count);
    }

    /* --------------------------------------------------------------------- */
    /*  escape (default settings)                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEscape_nullAndEmpty() {
        // null input should return empty string
        assertEquals("", Entities.escape(null));
        // empty input should return empty string
        assertEquals("", Entities.escape(""));
    }

    @Test
    public void testEscape_basicHtmlCharacters() {
        String input = "<div class=\"test\">Tom & Jerry</div>";
        String expected = "&lt;div class=\"test\"&gt;Tom &amp; Jerry&lt;/div&gt;";
        assertEquals(expected, Entities.escape(input));
    }

    @Test
    public void testEscape_withAttributeMode() throws IOException {
        // In attribute mode, '<' and '>' are not escaped, but '"' is.
        Document.OutputSettings out = new Document.OutputSettings();
        out.escapeMode(Entities.EscapeMode.base);
        StringBuilder sb = new StringBuilder();
        Entities.escape(sb, "a=\"b\" <c>", out, true, false, false);
        // Expected: double‑quote becomes &quot;, '<' and '>' stay as literal characters
        assertEquals("a=&quot;b&quot; <c>", sb.toString());
    }

    @Test
    public void testEscape_asciiCharsetNonEncodableChar() throws IOException {
        // Force ASCII charset – characters beyond 0x7F must be escaped.
        Document.OutputSettings out = new Document.OutputSettings();
        out.charset(StandardCharsets.US_ASCII);
        out.escapeMode(Entities.EscapeMode.base);

        StringBuilder sb = new StringBuilder();
        // 'é' (U+00E9) is not encodable in US‑ASCII, should become a named entity (&eacute;) or numeric.
        Entities.escape(sb, "café", out, false, false, false);
        String escaped = sb.toString();

        // The base entity set includes &eacute; for 0x00E9
        assertTrue("Escaped output should contain &eacute; or numeric reference",
                escaped.contains("&eacute;") || escaped.contains("&#x00e9;"));
        // Ensure the rest of the string is unchanged
        assertTrue(escaped.startsWith("caf"));
    }

    @Test
    public void testEscape_nonBreakingSpaceInXhtmlMode() throws IOException {
        // In XHTML mode, non‑breaking space is output as numeric reference.
        Document.OutputSettings out = new Document.OutputSettings();
        out.escapeMode(Entities.EscapeMode.xhtml);
        out.charset(StandardCharsets.UTF_8);

        StringBuilder sb = new StringBuilder();
        Entities.escape(sb, "\u00A0", out, false, false, false);
        assertEquals("&#xa0;", sb.toString());
    }

    @Test
    public void testEscape_nonBreakingSpaceInBaseMode() throws IOException {
        // In base mode, non‑breaking space is &nbsp;
        Document.OutputSettings out = new Document.OutputSettings();
        out.escapeMode(Entities.EscapeMode.base);
        out.charset(StandardCharsets.UTF_8);

        StringBuilder sb = new StringBuilder();
        Entities.escape(sb, "\u00A0", out, false, false, false);
        assertEquals("&nbsp;", sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  unescape (strict vs non‑strict)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testUnescape_basic() {
        String escaped = "&lt;div&gt;Tom &amp; Jerry&lt;/div&gt;";
        String expected = "<div>Tom & Jerry</div>";
        assertEquals(expected, Entities.unescape(escaped));
    }

    @Test
    public void testUnescape_missingSemicolon_nonStrict() {
        // Non‑strict mode accepts entities without a trailing ';'
        String input = "&ltdiv&gt";
        String expected = "<div>";
        assertEquals(expected, Entities.unescape(input));
    }

    @Test
    public void testUnescape_missingSemicolon_strict() {
        // Strict mode requires ';' – the entity should stay unchanged.
        String input = "&ltdiv&gt";
        String result = Entities.unescape(input, true);
        assertEquals(input, result);
    }

    @Test
    public void testUnescape_unknownEntity() {
        // Unknown entities are left as‑is.
        String input = "&unknown; text";
        assertEquals(input, Entities.unescape(input));
    }

    /* --------------------------------------------------------------------- */
    /*  Internal helper validation (canEncode) – exercised via escape       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCanEncode_asciiCharset() throws IOException {
        // Verify that ASCII charset only allows characters < 0x80.
        Document.OutputSettings out = new Document.OutputSettings();
        out.charset(StandardCharsets.US_ASCII);
        out.escapeMode(Entities.EscapeMode.base);

        StringBuilder sb = new StringBuilder();
        // Include a mix of ASCII and non‑ASCII characters.
        Entities.escape(sb, "A\u00A0\u20AC", out, false, false, false);
        String result = sb.toString();

        // 'A' should be unchanged, non‑breaking space -> &nbsp; (base set), Euro sign -> numeric reference
        assertTrue(result.contains("A"));
        assertTrue(result.contains("&nbsp;"));
        assertTrue(result.contains("&#x20ac;"));
    }
}
###Test END##