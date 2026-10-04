package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;

public class TokenQueueTest {

    /* ---------- Constructor & basic state ---------- */

    @Test
    public void testIsEmptyAndPeek() {
        TokenQueue tq = new TokenQueue("");
        assertTrue(tq.isEmpty());
        assertEquals(0, tq.peek());

        tq = new TokenQueue("abc");
        assertFalse(tq.isEmpty());
        assertEquals('a', tq.peek());
    }

    @Test
    public void testAddFirstStringAndChar() {
        TokenQueue tq = new TokenQueue("bc");
        tq.addFirst("a");
        assertEquals("abc", tq.toString());

        tq = new TokenQueue("bc");
        tq.addFirst('a');
        assertEquals("abc", tq.toString());
    }

    /* ---------- Matching methods ---------- */

    @Test
    public void testMatchesCaseInsensitive() {
        TokenQueue tq = new TokenQueue("AbCde");
        assertTrue(tq.matches("abc"));
        assertTrue(tq.matches("ABcDE"));
        assertFalse(tq.matches("xyz"));
    }

    @Test
    public void testMatchesCaseSensitive() {
        TokenQueue tq = new TokenQueue("AbCde");
        assertTrue(tq.matchesCS("AbC"));
        assertFalse(tq.matchesCS("abc"));
    }

    @Test
    public void testMatchesAnyString() {
        TokenQueue tq = new TokenQueue("hello");
        assertTrue(tq.matchesAny("hi", "he", "hey"));
        assertFalse(tq.matchesAny("hi", "hey"));
    }

    @Test
    public void testMatchesAnyChar() {
        TokenQueue tq = new TokenQueue("x");
        assertTrue(tq.matchesAny('x', 'y'));
        assertFalse(tq.matchesAny('a', 'b'));
    }

    @Test
    public void testMatchesStartTag() {
        TokenQueue tq = new TokenQueue("<a href=''>");
        assertTrue(tq.matchesStartTag());

        tq = new TokenQueue("<1invalid");
        assertFalse(tq.matchesStartTag());

        tq = new TokenQueue("noTag");
        assertFalse(tq.matchesStartTag());
    }

    /* ---------- Whitespace and word detection ---------- */

    @Test
    public void testMatchesWhitespace() {
        TokenQueue tq = new TokenQueue(" \t\nabc");
        assertTrue(tq.matchesWhitespace());
        tq.advance();
        assertTrue(tq.matchesWhitespace());
        tq.advance();
        assertTrue(tq.matchesWhitespace());
        tq.advance();
        assertFalse(tq.matchesWhitespace());
    }

    @Test
    public void testMatchesWord() {
        TokenQueue tq = new TokenQueue("9a_");
        assertTrue(tq.matchesWord()); // '9'
        tq.advance();
        assertTrue(tq.matchesWord()); // 'a'
        tq.advance();
        assertFalse(tq.matchesWord()); // '_' is not a letter or digit
    }

    /* ---------- Advance and consume ---------- */

    @Test
    public void testAdvanceAndConsume() {
        TokenQueue tq = new TokenQueue("xyz");
        assertEquals('x', tq.consume());
        assertEquals('y', tq.peek());
        tq.advance();
        assertEquals('z', tq.peek());
    }

    @Test
    public void testConsumeStringSuccess() {
        TokenQueue tq = new TokenQueue("HelloWorld");
        tq.consume("Hello");
        assertEquals("World", tq.remainder());
    }

    @Test(expected = IllegalStateException.class)
    public void testConsumeStringFailure() {
        TokenQueue tq = new TokenQueue("HelloWorld");
        tq.consume("Bye"); // should throw
    }

    /* ---------- consumeTo family ---------- */

    @Test
    public void testConsumeToFound() {
        TokenQueue tq = new TokenQueue("one,two,three");
        String consumed = tq.consumeTo(",");
        assertEquals("one", consumed);
        assertEquals(",two,three", tq.toString());
    }

    @Test
    public void testConsumeToNotFound() {
        TokenQueue tq = new TokenQueue("abc");
        String consumed = tq.consumeTo("z");
        assertEquals("abc", consumed);
        assertTrue(tq.isEmpty());
    }

    @Test
    public void testConsumeToIgnoreCase() {
        TokenQueue tq = new TokenQueue("aBcDeF");
        String consumed = tq.consumeToIgnoreCase("de");
        assertEquals("aBc", consumed);
        assertEquals("DeF", tq.toString()); // case of remaining is unchanged
    }

    @Test
    public void testConsumeToAny() {
        TokenQueue tq = new TokenQueue("alpha|beta,gamma");
        String consumed = tq.consumeToAny("|", ",");
        assertEquals("alpha", consumed);
        assertEquals("|beta,gamma", tq.toString());
    }

    @Test
    public void testChompTo() {
        TokenQueue tq = new TokenQueue("first:second:third");
        String data = tq.chompTo(":");
        assertEquals("first", data);
        assertEquals(":second:third", tq.toString());
    }

    @Test
    public void testChompToIgnoreCase() {
        TokenQueue tq = new TokenQueue("OneTWOthree");
        String data = tq.chompToIgnoreCase("two");
        assertEquals("One", data);
        assertEquals("TWOthree", tq.toString());
    }

    /* ---------- matchChomp ---------- */

    @Test
    public void testMatchChompSuccess() {
        TokenQueue tq = new TokenQueue("abcd");
        assertTrue(tq.matchChomp("ab"));
        assertEquals("cd", tq.toString());
    }

    @Test
    public void testMatchChompFailure() {
        TokenQueue tq = new TokenQueue("abcd");
        assertFalse(tq.matchChomp("xy"));
        assertEquals("abcd", tq.toString());
    }

    /* ---------- chompBalanced ---------- */

    @Test
    public void testChompBalancedSimple() {
        TokenQueue tq = new TokenQueue("(one (two) three) four");
        String out = tq.chompBalanced('(', ')');
        assertEquals("one (two) three", out);
        assertEquals(" four", tq.toString());
    }

    @Test
    public void testChompBalancedWithQuotesAndEscapes() {
        TokenQueue tq = new TokenQueue("(a \"(b)\" c \\) d)end");
        String out = tq.chompBalanced('(', ')');
        // The quoted "(b)" and escaped \) are ignored for balancing
        assertEquals("a \"(b)\" c \\) d", out);
        assertEquals("end", tq.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void testChompBalancedUnbalancedThrows() {
        TokenQueue tq = new TokenQueue("(unclosed");
        tq.chompBalanced('(', ')'); // should fail validation
    }

    /* ---------- static unescape ---------- */

    @Test
    public void testUnescape() {
        String escaped = "a\\\\b\\c\\\\\\d";
        // Interpretation:
        // a\\b\c\\\d  => the method removes a single backslash unless it's escaped itself
        String expected = "a\\bcd\\d"; // step‑by‑step: a\\ -> a\, then \b -> b, then \c -> c, then \\\ -> \, then d stays
        assertEquals(expected, TokenQueue.unescape(escaped));
    }

    /* ---------- consumeWhitespace ---------- */

    @Test
    public void testConsumeWhitespace() {
        TokenQueue tq = new TokenQueue("   \t\nabc");
        assertTrue(tq.consumeWhitespace());
        assertEquals("abc", tq.toString());

        tq = new TokenQueue("abc");
        assertFalse(tq.consumeWhitespace());
        assertEquals("abc", tq.toString());
    }

    /* ---------- consumeWord ---------- */

    @Test
    public void testConsumeWord() {
        TokenQueue tq = new TokenQueue("hello123!rest");
        String word = tq.consumeWord();
        assertEquals("hello123", word);
        assertEquals("!rest", tq.toString());
    }

    /* ---------- consumeTagName ---------- */

    @Test
    public void testConsumeTagName() {
        TokenQueue tq = new TokenQueue("div:main_part-2 extra");
        String tag = tq.consumeTagName();
        assertEquals("div:main_part-2", tag);
        assertEquals(" extra", tq.toString());
    }

    /* ---------- consumeElementSelector ---------- */

    @Test
    public void testConsumeElementSelector() {
        TokenQueue tq = new TokenQueue("*|svg|path_extra");
        String sel = tq.consumeElementSelector();
        assertEquals("*|svg|path", sel);
        assertEquals("_extra", tq.toString());
    }

    /* ---------- consumeCssIdentifier ---------- */

    @Test
    public void testConsumeCssIdentifier() {
        TokenQueue tq = new TokenQueue("-my_class123 more");
        String id = tq.consumeCssIdentifier();
        assertEquals("-my_class123", id);
        assertEquals(" more", tq.toString());
    }

    /* ---------- consumeAttributeKey ---------- */

    @Test
    public void testConsumeAttributeKey() {
        TokenQueue tq = new TokenQueue("data-value:foo bar");
        String key = tq.consumeAttributeKey();
        assertEquals("data-value", key);
        assertEquals(":foo bar", tq.toString());
    }

    /* ---------- remainder & toString ---------- */

    @Test
    public void testRemainderAndToString() {
        TokenQueue tq = new TokenQueue("remain");
        assertEquals("remain", tq.remainder());
        assertTrue(tq.isEmpty());
        assertEquals("", tq.toString());

        tq = new TokenQueue("abc");
        tq.consume(); // consume 'a'
        assertEquals("bc", tq.toString());
    }
}
