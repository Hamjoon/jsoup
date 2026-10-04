package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TokenQueue}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal usage of each public method.</li>
 *   <li>Edge cases such as empty queues, whitespace handling, quoting and escaping.</li>
 *   <li>Error scenarios – e.g. illegal arguments and illegal state conditions.</li>
 * </ul>
 */
public class TokenQueueTest {

    /* --------------------------------------------------------------------- *
     *  Construction & basic state helpers
     * --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void ctorRejectsNull() {
        new TokenQueue(null);
    }

    @Test
    public void isEmptyAndPeekBehaviour() {
        TokenQueue tq = new TokenQueue("");
        assertTrue("Empty string should be empty", tq.isEmpty());
        assertEquals("Peek on empty should return 0 char", 0, tq.peek());

        tq = new TokenQueue("abc");
        assertFalse("Non‑empty queue should not be empty", tq.isEmpty());
        assertEquals('a', tq.peek());
        assertEquals("Peek must not consume", "abc", tq.toString());
    }

    @Test
    public void addFirstPrependsCorrectly() {
        TokenQueue tq = new TokenQueue("world");
        tq.addFirst(' ');
        tq.addFirst("Hello");
        assertEquals("Hello world", tq.toString());
    }

    /* --------------------------------------------------------------------- *
     *  Matching helpers (case‑insensitive / case‑sensitive)
     * --------------------------------------------------------------------- */

    @Test
    public void matchesAndMatchesCS() {
        TokenQueue tq = new TokenQueue("Jsoup");
        assertTrue(tq.matches("jsoup"));   // case‑insensitive
        assertTrue(tq.matchesCS("Jsoup")); // case‑sensitive
        assertFalse(tq.matchesCS("jsoup"));
    }

    @Test
    public void matchesAnyStringAndChar() {
        TokenQueue tq = new TokenQueue("alpha");
        assertTrue(tq.matchesAny("ALPHA", "beta"));
        assertFalse(tq.matchesAny("gamma", "delta"));
        assertTrue(tq.matchesAny('a', 'z'));
        assertFalse(tq.matchesAny('x', 'y'));
    }

    @Test
    public void matchesStartTag() {
        TokenQueue tq = new TokenQueue("<div>");
        assertTrue(tq.matchesStartTag());

        tq = new TokenQueue("<1a>");
        assertFalse(tq.matchesStartTag()); // second char not a letter

        tq = new TokenQueue(" <b>");
        assertFalse(tq.matchesStartTag()); // leading space
    }

    @Test
    public void matchesWhitespaceAndWord() {
        TokenQueue tq = new TokenQueue(" \t\nabc");
        assertTrue(tq.matchesWhitespace());
        tq.consumeWhitespace(); // consume all whitespace
        assertEquals('a', tq.peek());
        assertTrue(tq.matchesWord());
        assertFalse(tq.matchesWhitespace());
    }

    /* --------------------------------------------------------------------- *
     *  Consuming primitives
     * --------------------------------------------------------------------- */

    @Test
    public void advanceAndConsume() {
        TokenQueue tq = new TokenQueue("xyz");
        assertEquals('x', tq.consume());
        assertEquals('y', tq.peek());
        tq.advance();
        assertEquals('z', tq.peek());
        assertEquals('z', tq.consume());
        assertTrue(tq.isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void consumeStringFailsWhenNotMatched() {
        TokenQueue tq = new TokenQueue("hello");
        tq.consume("world"); // should throw
    }

    @Test
    public void consumeStringSucceeds() {
        TokenQueue tq = new TokenQueue("foobar");
        tq.consume("foo");
        assertEquals("bar", tq.toString());
    }

    @Test
    public void consumeToAndConsumeToIgnoreCase() {
        TokenQueue tq = new TokenQueue("one,two,three");
        assertEquals("one", tq.consumeTo(","));
        assertEquals(",", tq.toString().substring(0, 1)); // delimiter left
        assertEquals("two", tq.consumeToIgnoreCase(",THREE"));
        assertEquals("", tq.remainder()); // queue now empty
    }

    @Test
    public void consumeToAnyStopsAtFirstMatch() {
        TokenQueue tq = new TokenQueue("abcXYZdef");
        String consumed = tq.consumeToAny("X", "Y", "Z");
        assertEquals("abc", consumed);
        assertEquals("XYZdef", tq.toString());
    }

    /* --------------------------------------------------------------------- *
     *  Chomping helpers (including balanced)
     * --------------------------------------------------------------------- */

    @Test
    public void chompToAndChompToIgnoreCase() {
        TokenQueue tq = new TokenQueue("data=123;more");
        assertEquals("data=", tq.chompTo(";"));
        assertEquals("123", tq.chompToIgnoreCase(";"));
        assertEquals("more", tq.remainder());
    }

    @Test
    public void chompBalancedSimple() {
        TokenQueue tq = new TokenQueue("(a(b)c)d");
        String inner = tq.chompBalanced('(', ')');
        assertEquals("a(b)c", inner);
        assertEquals("d", tq.remainder());
    }

    @Test
    public void chompBalancedWithQuotesAndEscapes() {
        // Queue contains: (a "b(c)" d \'e(f)\' g)
        TokenQueue tq = new TokenQueue("(a \"b(c)\" d \\'e(f)\\' g)");
        String inner = tq.chompBalanced('(', ')');
        // The quoted part and escaped part should be kept verbatim
        assertEquals("a \"b(c)\" d \\'e(f)\\' g", inner);
        assertTrue(tq.isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void chompBalancedUnclosedThrows() {
        TokenQueue tq = new TokenQueue("(unclosed");
        // Expect Validate.fail() which throws IllegalStateException
        tq.chompBalanced('(', ')');
    }

    @Test
    public void unescapeWorks() {
        String escaped = "a\\\\b\\c\\\\\\d";
        // Explanation: a\\b\c\\\d -> after unescape we keep the escaped backslashes
        String expected = "a\\b\\c\\d";
        assertEquals(expected, TokenQueue.unescape(escaped));
    }

    /* --------------------------------------------------------------------- *
     *  Word / tag / selector / attribute helpers
     * --------------------------------------------------------------------- */

    @Test
    public void consumeWhitespaceReturnsFlag() {
        TokenQueue tq = new TokenQueue("   abc");
        assertTrue(tq.consumeWhitespace());
        assertEquals('a', tq.peek());

        tq = new TokenQueue("abc");
        assertFalse(tq.consumeWhitespace());
    }

    @Test
    public void consumeWordAndRemainder() {
        TokenQueue tq = new TokenQueue("foo123 bar");
        assertEquals("foo123", tq.consumeWord());
        assertEquals(" bar", tq.remainder());
    }

    @Test
    public void consumeTagNameSupportsSpecialChars() {
        TokenQueue tq = new TokenQueue("svg:rect_1-2 extra");
        assertEquals("svg:rect_1-2", tq.consumeTagName());
        assertEquals(" extra", tq.remainder());
    }

    @Test
    public void consumeElementSelectorHandlesNamespaceWildcard() {
        TokenQueue tq = new TokenQueue("*|div|span-foo");
        assertEquals("*|div|span-foo", tq.consumeElementSelector());
        assertTrue(tq.isEmpty());
    }

    @Test
    public void consumeCssIdentifier() {
        TokenQueue tq = new TokenQueue("-_myId123 other");
        assertEquals("-_myId123", tq.consumeCssIdentifier());
        assertEquals(" other", tq.remainder());
    }

    @Test
    public void consumeAttributeKey() {
        TokenQueue tq = new TokenQueue("data-value:123 more");
        assertEquals("data-value", tq.consumeAttributeKey());
        assertEquals(":123 more", tq.remainder());
    }

    /* --------------------------------------------------------------------- *
     *  toString / remainder sanity checks
     * --------------------------------------------------------------------- */

    @Test
    public void toStringReflectsCurrentPosition() {
        TokenQueue tq = new TokenQueue("abcdef");
        tq.advance(); // skip 'a'
        assertEquals("bcdef", tq.toString());
        tq.consume(); // consume 'b'
        assertEquals("cdef", tq.toString());
    }

    @Test
    public void remainderConsumesAll() {
        TokenQueue tq = new TokenQueue("last piece");
        String rem = tq.remainder();
        assertEquals("last piece", rem);
        assertTrue(tq.isEmpty());
        assertEquals("", tq.toString());
    }

    /* --------------------------------------------------------------------- *
     *  matchChomp integration test
     * --------------------------------------------------------------------- */

    @Test
    public void matchChompAdvancesWhenMatched() {
        TokenQueue tq = new TokenQueue("matchMeNow");
        assertTrue(tq.matchChomp("matchMe"));
        assertEquals("Now", tq.remainder());
    }

    @Test
    public void matchChompDoesNotAdvanceWhenNotMatched() {
        TokenQueue tq = new TokenQueue("noMatchHere");
        assertFalse(tq.matchChomp("xyz"));
        assertEquals("noMatchHere", tq.toString());
    }
}
