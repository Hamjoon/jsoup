###Test START##
package org.jsoup.parser;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Comprehensive JUnit4 tests for {@link TokenQueue}.
 */
public class TokenQueueTest {

    // ----------------------------------------------------------------------
    // Basic queue behaviour
    // ----------------------------------------------------------------------
    @Test
    public void testIsEmptyAndPeek() {
        TokenQueue tq = new TokenQueue("");
        assertTrue(tq.isEmpty());
        assertEquals(0, tq.peek());

        tq = new TokenQueue("a");
        assertFalse(tq.isEmpty());
        assertEquals('a', tq.peek());
        // peek does not consume
        assertEquals('a', tq.peek());
    }

    @Test
    public void testAddFirst() {
        TokenQueue tq = new TokenQueue("bc");
        tq.addFirst('a');
        assertEquals("abc", tq.toString());

        // addFirst with a string when pos is not at 0
        tq = new TokenQueue("xyz");
        // consume one char to move pos
        assertEquals('x', tq.consume());
        tq.addFirst("AB");
        // Should prepend before the remaining "yz"
        assertEquals("AByz", tq.toString());
    }

    // ----------------------------------------------------------------------
    // Matching helpers
    // ----------------------------------------------------------------------
    @Test
    public void testMatchesAndMatchesCS() {
        TokenQueue tq = new TokenQueue("AbCde");
        assertTrue(tq.matches("abc"));
        assertTrue(tq.matches("AbC"));
        assertFalse(tq.matches("abz"));

        assertTrue(tq.matchesCS("AbC"));
        assertFalse(tq.matchesCS("abc"));
    }

    @Test
    public void testMatchesAnyStringAndChar() {
        TokenQueue tq = new TokenQueue("hello");
        assertTrue(tq.matchesAny("HE", "he", "hi"));
        assertFalse(tq.matchesAny("hi", "ho"));

        assertTrue(tq.matchesAny('h', 'x', 'y'));
        assertFalse(tq.matchesAny('a', 'b'));
    }

    @Test
    public void testMatchesStartTag() {
        TokenQueue tq1 = new TokenQueue("<a");
        assertTrue(tq1.matchesStartTag());

        TokenQueue tq2 = new TokenQueue("<1");
        assertFalse(tq2.matchesStartTag());

        TokenQueue tq3 = new TokenQueue("a<");
        assertFalse(tq3.matchesStartTag());

        TokenQueue tq4 = new TokenQueue("<");
        assertFalse(tq4.matchesStartTag());
    }

    // ----------------------------------------------------------------------
    // Whitespace / word helpers
    // ----------------------------------------------------------------------
    @Test
    public void testMatchesWhitespaceAndWord() {
        TokenQueue tq = new TokenQueue(" \t\nA1_");
        assertTrue(tq.matchesWhitespace());
        tq.consumeWhitespace(); // consume all leading whitespace
        assertFalse(tq.matchesWhitespace());

        // now at 'A'
        assertTrue(tq.matchesWord());
        // '_' is not a letter or digit
        tq.consume(); // consume 'A'
        tq.consume(); // consume '1'
        assertFalse(tq.matchesWord());
    }

    // ----------------------------------------------------------------------
    // Advance and consume
    // ----------------------------------------------------------------------
    @Test
    public void testAdvanceAndConsume() {
        TokenQueue tq = new TokenQueue("ab");
        assertEquals('a', tq.consume());
        assertEquals('b', tq.peek());
        tq.advance();
        assertTrue(tq.isEmpty());
    }

    @Test
    public void testConsumeStringSuccessAndFailure() {
        TokenQueue tq = new TokenQueue("foobar");
        tq.consume("foo");
        assertEquals("bar", tq.remainder());

        // reset
        tq = new TokenQueue("foobar");
        try {
            tq.consume("baz");
            fail("Expected IllegalStateException for non‑matching consume");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    // ----------------------------------------------------------------------
    // consumeTo / consumeToIgnoreCase / consumeToAny
    // ----------------------------------------------------------------------
    @Test
    public void testConsumeTo() {
        TokenQueue tq = new TokenQueue("one,two,three");
        String consumed = tq.consumeTo(",");
        assertEquals("one", consumed);
        // the comma is left on the queue
        assertEquals(",two,three", tq.toString());

        // when delimiter not present, returns remainder
        tq = new TokenQueue("abc");
        assertEquals("abc", tq.consumeTo("z"));
        assertTrue(tq.isEmpty());
    }

    @Test
    public void testConsumeToIgnoreCase() {
        TokenQueue tq = new TokenQueue("AbCdeFGh");
        String consumed = tq.consumeToIgnoreCase("de");
        assertEquals("AbC", consumed);
        // delimiter remains (case‑preserved)
        assertEquals("deFGh", tq.toString());

        // scan optimisation when first char is not case‑sensitive
        tq = new TokenQueue("12345");
        consumed = tq.consumeToIgnoreCase("6");
        assertEquals("12345", consumed);
        assertTrue(tq.isEmpty());
    }

    @Test
    public void testConsumeToAny() {
        TokenQueue tq = new TokenQueue("a b,c;d");
        String consumed = tq.consumeToAny(";", ",", " ");
        assertEquals("a", consumed);
        // the delimiter that stopped the scan is left on queue
        assertEquals(" b,c;d", tq.toString());

        // no terminator present – consumes all
        tq = new TokenQueue("xyz");
        assertEquals("xyz", tq.consumeToAny("!"));
        assertTrue(tq.isEmpty());
    }

    // ----------------------------------------------------------------------
    // chompTo / chompToIgnoreCase
    // ----------------------------------------------------------------------
    @Test
    public void testChompTo() {
        TokenQueue tq = new TokenQueue("foo=bar;baz");
        String data = tq.chompTo(";");
        assertEquals("foo=bar", data);
        // delimiter consumed
        assertEquals("baz", tq.toString());
    }

    @Test
    public void testChompToIgnoreCase() {
        TokenQueue tq = new TokenQueue("Foo=Bar;Baz");
        String data = tq.chompToIgnoreCase(";");
        assertEquals("Foo=Bar", data);
        assertEquals("Baz", tq.toString());
    }

    // ----------------------------------------------------------------------
    // chompBalanced
    // ----------------------------------------------------------------------
    @Test
    public void testChompBalancedSimple() {
        TokenQueue tq = new TokenQueue("(one (two) three) four");
        String bal = tq.chompBalanced('(', ')');
        assertEquals("one (two) three", bal);
        assertEquals(" four", tq.toString());
    }

    @Test
    public void testChompBalancedWithQuotesAndEscapes() {
        // quotes should be ignored for balancing, escaped chars stay in output
        TokenQueue tq = new TokenQueue("(a \"(b)\" c\\) d)X");
        String bal = tq.chompBalanced('(', ')');
        // The inner quoted parentheses are ignored for depth counting,
        // the escaped \) is treated as a literal backslash+paren inside the result.
        assertEquals("a \"(b)\" c\\) d", bal);
        assertEquals("X", tq.toString());
    }

    @Test
    public void testChompBalancedUnbalancedThrows() {
        TokenQueue tq = new TokenQueue("(unbalanced");
        try {
            tq.chompBalanced('(', ')');
            fail("Expected IllegalStateException for unbalanced markers");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    // ----------------------------------------------------------------------
    // static unescape
    // ----------------------------------------------------------------------
    @Test
    public void testUnescape() {
        assertEquals("a\\b", TokenQueue.unescape("a\\\\b"));
        assertEquals("ab", TokenQueue.unescape("a\\b"));
        assertEquals("\\", TokenQueue.unescape("\\\\"));
        assertEquals("", TokenQueue.unescape(""));
    }

    // ----------------------------------------------------------------------
    // consumeWhitespace, consumeWord, consumeTagName, consumeElementSelector,
    // consumeCssIdentifier, consumeAttributeKey
    // ----------------------------------------------------------------------
    @Test
    public void testConsumeWhitespace() {
        TokenQueue tq = new TokenQueue(" \t\nabc");
        assertTrue(tq.consumeWhitespace());
        assertEquals("abc", tq.toString());

        // no leading whitespace
        tq = new TokenQueue("xyz");
        assertFalse(tq.consumeWhitespace());
        assertEquals("xyz", tq.toString());
    }

    @Test
    public void testConsumeWord() {
        TokenQueue tq = new TokenQueue("abc123!def");
        assertEquals("abc123", tq.consumeWord());
        assertEquals("!def", tq.toString());
    }

    @Test
    public void testConsumeTagName() {
        TokenQueue tq = new TokenQueue("my-tag_name:extra");
        assertEquals("my-tag_name", tq.consumeTagName());
        assertEquals(":extra", tq.toString());
    }

    @Test
    public void testConsumeElementSelector() {
        TokenQueue tq = new TokenQueue("*|svg|path-123");
        assertEquals("*|svg|path-123", tq.consumeElementSelector());
        assertTrue(tq.isEmpty());
    }

    @Test
    public void testConsumeCssIdentifier() {
        TokenQueue tq = new TokenQueue("-my_class123$");
        assertEquals("-my_class123", tq.consumeCssIdentifier());
        assertEquals("$", tq.toString());
    }

    @Test
    public void testConsumeAttributeKey() {
        TokenQueue tq = new TokenQueue("data-value:foo");
        assertEquals("data-value", tq.consumeAttributeKey());
        assertEquals(":foo", tq.toString());
    }

    // ----------------------------------------------------------------------
    // remainder and toString
    // ----------------------------------------------------------------------
    @Test
    public void testRemainderAndToString() {
        TokenQueue tq = new TokenQueue("remaining");
        assertEquals("remain", tq.consumeTo("ing"));
        assertEquals("ing", tq.toString()); // toString does NOT consume
        assertEquals("ing", tq.remainder()); // remainder consumes the rest
        assertTrue(tq.isEmpty());
        // after remainder, toString should be empty
        assertEquals("", tq.toString());
    }
}
###Test END##