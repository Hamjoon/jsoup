package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit4 tests for {@link TokenQueue}.
 */
public class TokenQueueTest {

    /* --------------------------------------------------------------------- *
     *  Public method signatures (extracted from TokenQueue)
     * --------------------------------------------------------------------- *
     * public TokenQueue(String data)
     * public boolean isEmpty()
     * public char peek()
     * public void addFirst(Character c)
     * public void addFirst(String seq)
     * public boolean matches(String seq)
     * public boolean matchesCS(String seq)
     * public boolean matchesAny(String... seq)
     * public boolean matchesAny(char... seq)
     * public boolean matchesStartTag()
     * public boolean matchChomp(String seq)
     * public boolean matchesWhitespace()
     * public boolean matchesWord()
     * public void advance()
     * public char consume()
     * public void consume(String seq)
     * public String consumeTo(String seq)
     * public String consumeToIgnoreCase(String seq)
     * public String consumeToAny(String... seq)
     * public String chompTo(String seq)
     * public String chompToIgnoreCase(String seq)
     * public String chompBalanced(char open, char close)
     * public static String unescape(String in)
     * public boolean consumeWhitespace()
     * public String consumeWord()
     * public String consumeTagName()
     * public String consumeElementSelector()
     * public String consumeCssIdentifier()
     * public String consumeAttributeKey()
     * public String remainder()
     * public String toString()
     * --------------------------------------------------------------------- */

    /* -------------------- Constructor & basic state -------------------- */

    @Test(expected = NullPointerException.class)
    public void constructorThrowsOnNull() {
        new TokenQueue(null);
    }

    @Test
    public void isEmptyAndRemainderBehaviour() {
        TokenQueue tq = new TokenQueue("");
        assertTrue(tq.isEmpty());
        assertEquals("", tq.remainder());
        assertTrue(tq.isEmpty());

        tq = new TokenQueue("abc");
        assertFalse(tq.isEmpty());
        assertEquals("abc", tq.remainder());
        assertTrue(tq.isEmpty());
    }

    @Test
    public void peekBehaviour() {
        TokenQueue tq = new TokenQueue("");
        assertEquals(0, tq.peek());

        tq = new TokenQueue("x");
        assertEquals('x', tq.peek());
        // peek does not consume
        assertEquals('x', tq.consume());
    }

    @Test
    public void addFirstCharAndString() {
        TokenQueue tq = new TokenQueue("bc");
        tq.addFirst('a');
        assertEquals("abc", tq.toString());

        tq = new TokenQueue("def");
        tq.addFirst("abc");
        assertEquals("abcdef", tq.toString());
    }

    /* -------------------- Matching methods -------------------- */

    @Test
    public void matchesCaseInsensitive() {
        TokenQueue tq = new TokenQueue("AbCde");
        assertTrue(tq.matches("abc"));
        assertTrue(tq.matches("ABC"));
        assertFalse(tq.matches("abx"));
    }

    @Test
    public void matchesCaseSensitive() {
        TokenQueue tq = new TokenQueue("AbCde");
        assertTrue(tq.matchesCS("AbC"));
        assertFalse(tq.matchesCS("abc"));
    }

    @Test
    public void matchesAnyStringVarargs() {
        TokenQueue tq = new TokenQueue("hello");
        assertTrue(tq.matchesAny("hi", "heLLo", "bye"));
        assertFalse(tq.matchesAny("hi", "bye"));
    }

    @Test
    public void matchesAnyCharVarargs() {
        TokenQueue tq = new TokenQueue("x");
        assertTrue(tq.matchesAny('a', 'x', 'z'));
        assertFalse(tq.matchesAny('a', 'b'));

        TokenQueue empty = new TokenQueue("");
        assertFalse(empty.matchesAny('a'));
    }

    @Test
    public void matchesStartTag() {
        TokenQueue tq = new TokenQueue("<a href=''>");
        assertTrue(tq.matchesStartTag());

        tq = new TokenQueue("<1abc");
        assertFalse(tq.matchesStartTag());

        tq = new TokenQueue("<");
        assertFalse(tq.matchesStartTag());
    }

    @Test
    public void matchChompBehaviour() {
        TokenQueue tq = new TokenQueue("foobar");
        assertTrue(tq.matchChomp("foo"));
        assertEquals("bar", tq.remainder());

        tq = new TokenQueue("foobar");
        assertFalse(tq.matchChomp("baz"));
        assertEquals("foobar", tq.toString());
    }

    @Test
    public void matchesWhitespaceAndWord() {
        TokenQueue tq = new TokenQueue(" \t\nabc");
        assertTrue(tq.matchesWhitespace());
        tq.consumeWhitespace(); // consume whitespace
        assertTrue(tq.matchesWord());
        assertEquals('a', tq.peek());

        tq = new TokenQueue("9xyz");
        assertTrue(tq.matchesWord());
        assertFalse(tq.matchesWhitespace());
    }

    /* -------------------- Consume / Advance -------------------- */

    @Test
    public void advanceAndConsume() {
        TokenQueue tq = new TokenQueue("ab");
        assertEquals('a', tq.consume());
        assertEquals('b', tq.peek());

        tq.advance();
        assertTrue(tq.isEmpty());

        // advance on empty should stay empty
        tq.advance();
        assertTrue(tq.isEmpty());
    }

    @Test(expected = StringIndexOutOfBoundsException.class)
    public void consumeOnEmptyThrows() {
        TokenQueue tq = new TokenQueue("");
        tq.consume(); // should throw
    }

    @Test
    public void consumeStringSuccessAndFailure() {
        TokenQueue tq = new TokenQueue("helloWorld");

        // success
        tq.consume("hello");
        assertEquals("World", tq.toString());

        // failure – mismatched sequence
        try {
            tq.consume("xyz");
            fail("Expected IllegalStateException for mismatched sequence");
        } catch (IllegalStateException e) {
            // expected
        }

        // failure – sequence longer than remaining
        try {
            tq.consume("WorldExtra");
            fail("Expected IllegalStateException for sequence too long");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void consumeToAndConsumeToIgnoreCase() {
        TokenQueue tq = new TokenQueue("one TWO three");
        String part = tq.consumeTo("TWO");
        assertEquals("one ", part);
        assertEquals("TWO three", tq.toString());

        // case-insensitive version
        tq = new TokenQueue("one two three");
        part = tq.consumeToIgnoreCase("TWO");
        assertEquals("one ", part);
        assertEquals("two three", tq.toString());
    }

    @Test
    public void consumeToWhenNotFoundReturnsRemainder() {
        TokenQueue tq = new TokenQueue("abcdef");
        String part = tq.consumeTo("xyz");
        assertEquals("abcdef", part);
        assertTrue(tq.isEmpty());
    }

    @Test
    public void consumeToAnyStopsAtFirstMatch() {
        TokenQueue tq = new TokenQueue("abcXYZdef");
        String part = tq.consumeToAny("XYZ", "def");
        assertEquals("abc", part);
        assertEquals("XYZdef", tq.toString());
    }

    @Test
    public void chompToAndChompToIgnoreCase() {
        TokenQueue tq = new TokenQueue("start<end>rest");
        String data = tq.chompTo("<end>");
        assertEquals("start", data);
        assertEquals("rest", tq.toString());

        tq = new TokenQueue("alphaBETAgamma");
        data = tq.chompToIgnoreCase("beta");
        assertEquals("alpha", data);
        assertEquals("gamma", tq.toString());
    }

    /* -------------------- Balanced chomp -------------------- */

    @Test
    public void chompBalancedSimple() {
        TokenQueue tq = new TokenQueue("(one (two) three) four");
        String bal = tq.chompBalanced('(', ')');
        assertEquals("one (two) three", bal);
        assertEquals(" four", tq.toString());
    }

    @Test
    public void chompBalancedWithQuotesAndEscapes() {
        // Quotes should be ignored inside the balanced region
        TokenQueue tq = new TokenQueue("(a \"(b)\" c)end");
        String bal = tq.chompBalanced('(', ')');
        assertEquals("a \"(b)\" c", bal);
        assertEquals("end", tq.toString());

        // Escaped quotes should not toggle inQuote flag
        tq = new TokenQueue("(a \\\"(b)\\\" c)end");
        bal = tq.chompBalanced('(', ')');
        assertEquals("a \\\"(b)\\\" c", bal);
        assertEquals("end", tq.toString());
    }

    @Test
    public void chompBalancedUnbalancedThrows() {
        TokenQueue tq = new TokenQueue("(unbalanced");
        try {
            tq.chompBalanced('(', ')');
            fail("Expected IllegalArgumentException for unbalanced markers");
        } catch (IllegalArgumentException e) {
            // Expected – Validate.fail throws IllegalArgumentException
        }
    }

    /* -------------------- Static utility -------------------- */

    @Test
    public void unescapeWorks() {
        assertEquals("abc", TokenQueue.unescape("abc"));
        assertEquals("a\\b", TokenQueue.unescape("a\\\\b")); // double backslash becomes single backslash
        assertEquals("a\\b\\c", TokenQueue.unescape("a\\\\b\\\\c"));
    }

    /* -------------------- Whitespace & Word consumption -------------------- */

    @Test
    public void consumeWhitespaceBehaviour() {
        TokenQueue tq = new TokenQueue("   \t\nabc");
        assertTrue(tq.consumeWhitespace());
        assertEquals("abc", tq.toString());

        tq = new TokenQueue("abc");
        assertFalse(tq.consumeWhitespace());
        assertEquals("abc", tq.toString());
    }

    @Test
    public void consumeWordBehaviour() {
        TokenQueue tq = new TokenQueue("hello123!rest");
        String word = tq.consumeWord();
        assertEquals("hello123", word);
        assertEquals("!rest", tq.toString());

        // no leading word
        tq = new TokenQueue("!noWord");
        assertEquals("", tq.consumeWord());
        assertEquals("!noWord", tq.toString());
    }

    /* -------------------- Tag and selector consumption -------------------- */

    @Test
    public void consumeTagNameIncludesSpecialChars() {
        TokenQueue tq = new TokenQueue("svg:path_123-abc rest");
        String tag = tq.consumeTagName();
        assertEquals("svg:path_123-abc", tag);
        assertEquals(" rest", tq.toString());
    }

    @Test
    public void consumeElementSelectorHandlesNamespaceAndWildcard() {
        TokenQueue tq = new TokenQueue("*|div|span_rest");
        String sel = tq.consumeElementSelector();
        assertEquals("*|div|span", sel);
        assertEquals("_rest", tq.toString());
    }

    @Test
    public void consumeCssIdentifier() {
        TokenQueue tq = new TokenQueue("-_myId123!more");
        String id = tq.consumeCssIdentifier();
        assertEquals("-_myId123", id);
        assertEquals("!more", tq.toString());
    }

    @Test
    public void consumeAttributeKey() {
        TokenQueue tq = new TokenQueue("data-value:extra");
        String key = tq.consumeAttributeKey();
        assertEquals("data-value:", key);
        assertEquals("extra", tq.toString());
    }

    /* -------------------- toString and remainder -------------------- */

    @Test
    public void toStringDoesNotConsume() {
        TokenQueue tq = new TokenQueue("abc");
        assertEquals("abc", tq.toString());
        assertEquals("abc", tq.toString()); // still same
    }

    @Test
    public void remainderConsumesAll() {
        TokenQueue tq = new TokenQueue("xyz");
        assertEquals("xyz", tq.remainder());
        assertTrue(tq.isEmpty());
        assertEquals("", tq.toString());
    }
}
