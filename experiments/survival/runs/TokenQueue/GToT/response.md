###Test START##
```java
package org.jsoup.parser;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit4 test suite for {@link TokenQueue}.
 *
 * The tests are organised in three logical sections:
 *
 * 1. **Basic functionality** – one test per public method that verifies the
 *    expected happy‑path behaviour.
 * 2. **Edge‑case & exception handling** – tests that target boundary
 *    conditions, null handling, empty inputs and the various IllegalStateException
 *    paths in the class.
 * 3. **Combined scenarios** – tests that exercise several methods together
 *    to ensure the internal position pointer (`pos`) is updated correctly.
 *
 * All tests run against the current implementation of {@code TokenQueue}
 * (as of jsoup 1.17.2). If the implementation changes, some tests may need
 * to be updated accordingly.
 */
public class TokenQueueTest {

    /* --------------------------------------------------------------------- *
     *  Helper constants used by many tests
     * --------------------------------------------------------------------- */
    private static final char ESC = '\\';

    /* --------------------------------------------------------------------- *
     *  1. BASIC FUNCTIONALITY TESTS
     * --------------------------------------------------------------------- */

    @Test
    public void constructor_NonNullData_ShouldInitializeQueue() {
        TokenQueue tq = new TokenQueue("abc");
        assertFalse(tq.isEmpty());
        assertEquals('a', tq.peek());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_NullData_ShouldThrow() {
        new TokenQueue(null);
    }

    @Test
    public void isEmpty_EmptyQueue_ShouldReturnTrue() {
        TokenQueue tq = new TokenQueue("");
        assertTrue(tq.isEmpty());
        assertEquals(0, tq.peek());
    }

    @Test
    public void peek_NonEmpty_ShouldReturnFirstCharWithoutConsuming() {
        TokenQueue tq = new TokenQueue("xyz");
        assertEquals('x', tq.peek());
        assertFalse(tq.isEmpty()); // still non‑empty
    }

    @Test
    public void addFirst_Character_ShouldPrependChar() {
        TokenQueue tq = new TokenQueue("bc");
        tq.addFirst('a');
        assertEquals('a', tq.peek());
        assertEquals("abc", tq.toString());
    }

    @Test
    public void addFirst_String_ShouldPrependString() {
        TokenQueue tq = new TokenQueue("def");
        tq.addFirst("abc");
        assertEquals("abcdef", tq.toString());
    }

    @Test
    public void matches_CaseInsensitive_ShouldWork() {
        TokenQueue tq = new TokenQueue("AbCdEf");
        assertTrue(tq.matches("abcd"));
        assertTrue(tq.matches("ABCDEF"));
        assertFalse(tq.matches("xyz"));
    }

    @Test
    public void matchesCS_CaseSensitive_ShouldWork() {
        TokenQueue tq = new TokenQueue("AbCd");
        assertTrue(tq.matchesCS("Ab"));
        assertFalse(tq.matchesCS("ab"));
    }

    @Test
    public void matchesAny_StringVarargs_ShouldDetectMatch() {
        TokenQueue tq = new TokenQueue("hello");
        assertTrue(tq.matchesAny("hi", "he", "ho"));
        assertFalse(tq.matchesAny("hi", "ho"));
    }

    @Test
    public void matchesAny_CharVarargs_ShouldDetectMatch() {
        TokenQueue tq = new TokenQueue("z");
        assertTrue(tq.matchesAny('a', 'z', 'm'));
        assertFalse(tq.matchesAny('a', 'b'));
    }

    @Test
    public void matchesStartTag_ValidTag_ShouldReturnTrue() {
        TokenQueue tq = new TokenQueue("<a href>");
        assertTrue(tq.matchesStartTag());
    }

    @Test
    public void matchesStartTag_Invalid_ShouldReturnFalse() {
        TokenQueue tq1 = new TokenQueue("<1");
        TokenQueue tq2 = new TokenQueue("<");
        TokenQueue tq3 = new TokenQueue("a<");
        assertFalse(tq1.matchesStartTag());
        assertFalse(tq2.matchesStartTag());
        assertFalse(tq3.matchesStartTag());
    }

    @Test
    public void matchChomp_Match_ShouldConsumeAndReturnTrue() {
        TokenQueue tq = new TokenQueue("foobar");
        assertTrue(tq.matchChomp("foo"));
        assertEquals('b', tq.peek());
    }

    @Test
    public void matchChomp_NoMatch_ShouldReturnFalseAndNotConsume() {
        TokenQueue tq = new TokenQueue("foobar");
        assertFalse(tq.matchChomp("bar"));
        assertEquals('f', tq.peek());
    }

    @Test
    public void matchesWhitespace_TrueAndFalse() {
        TokenQueue tq1 = new TokenQueue(" \t\nx");
        assertTrue(tq1.matchesWhitespace());

        TokenQueue tq2 = new TokenQueue("x");
        assertFalse(tq2.matchesWhitespace());
    }

    @Test
    public void matchesWord_TrueAndFalse() {
        TokenQueue tq1 = new TokenQueue("a1_");
        assertTrue(tq1.matchesWord());

        TokenQueue tq2 = new TokenQueue("_");
        assertFalse(tq2.matchesWord());
    }

    @Test
    public void advance_ShouldMovePositionOneStep() {
        TokenQueue tq = new TokenQueue("ab");
        tq.advance();
        assertEquals('b', tq.peek());
    }

    @Test
    public void consume_ShouldReturnAndAdvance() {
        TokenQueue tq = new TokenQueue("xy");
        char c = tq.consume();
        assertEquals('x', c);
        assertEquals('y', tq.peek());
    }

    @Test
    public void consumeString_Matching_ShouldConsume() {
        TokenQueue tq = new TokenQueue("hello world");
        tq.consume("hello");
        assertEquals(' ', tq.peek());
    }

    @Test(expected = IllegalStateException.class)
    public void consumeString_NotMatching_ShouldThrow() {
        TokenQueue tq = new TokenQueue("hello");
        tq.consume("world");
    }

    @Test
    public void consumeTo_Found_ShouldReturnUpToSeq() {
        TokenQueue tq = new TokenQueue("one,two,three");
        String out = tq.consumeTo(",");
        assertEquals("one", out);
        assertEquals(',', tq.peek());
    }

    @Test
    public void consumeTo_NotFound_ShouldReturnRemainder() {
        TokenQueue tq = new TokenQueue("abc");
        String out = tq.consumeTo("x");
        assertEquals("abc", out);
        assertTrue(tq.isEmpty());
    }

    @Test
    public void consumeToIgnoreCase_Found_CaseInsensitive() {
        TokenQueue tq = new TokenQueue("AbCdEf");
        String out = tq.consumeToIgnoreCase("cD");
        assertEquals("Ab", out);
        assertEquals('c', tq.peek()); // case preserved
    }

    @Test
    public void consumeToAny_MultipleTerminators() {
        TokenQueue tq = new TokenQueue("name=val;next");
        String out = tq.consumeToAny(";", "=");
        assertEquals("name", out);
        assertTrue(tq.matchesAny('=', ';'));
    }

    @Test
    public void chompTo_ShouldConsumeAndRemoveTerminator() {
        TokenQueue tq = new TokenQueue("foo=bar");
        String data = tq.chompTo("=");
        assertEquals("foo", data);
        assertEquals('=', tq.peek()); // terminator still present before matchChomp
        tq.matchChomp("="); // consume it
        assertEquals('b', tq.peek());
    }

    @Test
    public void chompToIgnoreCase_ShouldWorkCaseInsensitive() {
        TokenQueue tq = new TokenQueue("FooBar");
        String data = tq.chompToIgnoreCase("bAr");
        assertEquals("Foo", data);
        assertTrue(tq.matches("bAr"));
    }

    @Test
    public void chompBalanced_SimpleParentheses() {
        TokenQueue tq = new TokenQueue("(one (two) three) end");
        String out = tq.chompBalanced('(', ')');
        assertEquals("one (two) three", out);
        assertEquals(" end", tq.remainder());
    }

    @Test
    public void chompBalanced_WithQuotesAndEscapes() {
        TokenQueue tq = new TokenQueue("(a \"(b)\" c\\) d)tail");
        String out = tq.chompBalanced('(', ')');
        // The inner quoted parentheses and escaped close are ignored for balancing
        assertEquals("a \"(b)\" c\\) d", out);
        assertEquals("tail", tq.remainder());
    }

    @Test(expected = IllegalStateException.class)
    public void chompBalanced_Unbalanced_ShouldThrow() {
        TokenQueue tq = new TokenQueue("(unbalanced");
        tq.chompBalanced('(', ')'); // Validate.fail throws IllegalStateException
    }

    @Test
    public void unescape_ShouldRemoveEscapesCorrectly() {
        String escaped = "a\\\\b\\c\\\\\\\\"; // a\\b\c\\
        String unescaped = TokenQueue.unescape(escaped);
        assertEquals("a\\b\\c\\\\", unescaped);
    }

    @Test
    public void consumeWhitespace_ShouldConsumeAllLeadingWhitespace() {
        TokenQueue tq = new TokenQueue(" \t\nabc");
        assertTrue(tq.consumeWhitespace());
        assertEquals('a', tq.peek());
    }

    @Test
    public void consumeWhitespace_NoWhitespace_ShouldReturnFalse() {
        TokenQueue tq = new TokenQueue("abc");
        assertFalse(tq.consumeWhitespace());
        assertEquals('a', tq.peek());
    }

    @Test
    public void consumeWord_ShouldReturnWordAndStopAtNonWord() {
        TokenQueue tq = new TokenQueue("abc123!rest");
        String word = tq.consumeWord();
        assertEquals("abc123", word);
        assertEquals('!', tq.peek());
    }

    @Test
    public void consumeTagName_ShouldAllowWordAndSpecialChars() {
        TokenQueue tq = new TokenQueue("svg:path_123>rest");
        String tag = tq.consumeTagName();
        assertEquals("svg:path_123", tag);
        assertEquals('>', tq.peek());
    }

    @Test
    public void consumeElementSelector_ShouldAllowWildcardNamespace() {
        TokenQueue tq = new TokenQueue("*|div.class");
        String sel = tq.consumeElementSelector();
        assertEquals("*|div", sel);
        assertEquals('.', tq.peek());
    }

    @Test
    public void consumeCssIdentifier_ShouldAcceptHyphensAndUnderscores() {
        TokenQueue tq = new TokenQueue("-my_id123.more");
        String id = tq.consumeCssIdentifier();
        assertEquals("-my_id123", id);
        assertEquals('.', tq.peek());
    }

    @Test
    public void consumeAttributeKey_ShouldAcceptColon() {
        TokenQueue tq = new TokenQueue("data:attr=val");
        String key = tq.consumeAttributeKey();
        assertEquals("data:attr", key);
        assertEquals('=', tq.peek());
    }

    @Test
    public void remainder_ShouldReturnEverythingLeftAndEmptyQueue() {
        TokenQueue tq = new TokenQueue("remaining");
        tq.consume("rem");
        String rest = tq.remainder();
        assertEquals("aining", rest);
        assertTrue(tq.isEmpty());
    }

    @Test
    public void toString_ShouldReflectCurrentQueue() {
        TokenQueue tq = new TokenQueue("abc");
        tq.consume(); // drop 'a'
        assertEquals("bc", tq.toString());
    }

    /* --------------------------------------------------------------------- *
     *  2. EDGE‑CASE & EXCEPTION TESTS
     * --------------------------------------------------------------------- */

    @Test
    public void matches_EmptyQueue_ShouldReturnFalse() {
        TokenQueue tq = new TokenQueue("");
        assertFalse(tq.matches("any"));
    }

    @Test
    public void matchesAny_StringVarargs_EmptyArray_ShouldReturnFalse() {
        TokenQueue tq = new TokenQueue("x");
        assertFalse(tq.matchesAny());
    }

    @Test
    public void matchesAny_CharVarargs_EmptyArray_ShouldReturnFalse() {
        TokenQueue tq = new TokenQueue("x");
        assertFalse(tq.matchesAny(new char[]{}));
    }

    @Test
    public void matchesStartTag_QueueTooShort_ShouldReturnFalse() {
        TokenQueue tq = new TokenQueue("<");
        assertFalse(tq.matchesStartTag());
    }

    @Test
    public void consumeString_SequenceLongerThanRemaining_ShouldThrow() {
        TokenQueue tq = new TokenQueue("ab");
        try {
            tq.consume("abc");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void consumeToIgnoreCase_SkipLogicWhenFirstCharIsUncased() {
        // first char '1' is not a letter, so canScan = true
        TokenQueue tq = new TokenQueue("123abcXYZ");
        String out = tq.consumeToIgnoreCase("XYZ");
        assertEquals("123abc", out);
        assertEquals('X', tq.peek());
    }

    @Test
    public void consumeToIgnoreCase_NoMatch_ShouldConsumeAll() {
        TokenQueue tq = new TokenQueue("abc");
        String out = tq.consumeToIgnoreCase("z");
        assertEquals("abc", out);
        assertTrue(tq.isEmpty());
    }

    @Test
    public void addFirst_AfterSomeConsumption_ShouldResetPosToZero() {
        TokenQueue tq = new TokenQueue("12345");
        tq.consume(); // pos = 1
        tq.addFirst("AB");
        assertEquals("AB2345", tq.toString());
        assertEquals('A', tq.peek());
    }

    @Test
    public void chompBalanced_EscapedClose_ShouldNotTerminateEarly() {
        TokenQueue tq = new TokenQueue("(a\\)b)");
        String out = tq.chompBalanced('(', ')');
        assertEquals("a\\)b", out);
        assertTrue(tq.isEmpty());
    }

    @Test
    public void unescape_EscapedEscapes_ShouldPreserveSingleBackslash() {
        String escaped = "a\\\\b";
        String result = TokenQueue.unescape(escaped);
        assertEquals("a\\b", result);
    }

    @Test
    public void consumeWord_EmptyQueue_ShouldReturnEmptyString() {
        TokenQueue tq = new TokenQueue("");
        assertEquals("", tq.consumeWord());
    }

    @Test
    public void consumeTagName_EmptyQueue_ShouldReturnEmptyString() {
        TokenQueue tq = new TokenQueue("");
        assertEquals("", tq.consumeTagName());
    }

    @Test
    public void consumeElementSelector_OnlyWildcard_ShouldReturnWildcard() {
        TokenQueue tq = new TokenQueue("*|");
        assertEquals("*|", tq.consumeElementSelector());
    }

    @Test
    public void consumeCssIdentifier_Empty_ShouldReturnEmptyString() {
        TokenQueue tq = new TokenQueue("");
        assertEquals("", tq.consumeCssIdentifier());
    }

    @Test
    public void consumeAttributeKey_Empty_ShouldReturnEmptyString() {
        TokenQueue tq = new TokenQueue("");
        assertEquals("", tq.consumeAttributeKey());
    }

    /* --------------------------------------------------------------------- *
     *  3. COMBINED SCENARIOS (ensuring state consistency)
     * --------------------------------------------------------------------- */

    @Test
    public void sequenceOfOperations_ShouldMaintainCorrectPos() {
        TokenQueue tq = new TokenQueue("  <div class='test'>Hello</div>");
        assertTrue(tq.consumeWhitespace());          // consume leading spaces
        assertTrue(tq.matchesStartTag());            // now at '<'
        assertTrue(tq.matchChomp("<"));
        assertEquals("div", tq.consumeTagName());    // consumes "div"
        assertTrue(tq.matchesWhitespace());         // space before attribute
        tq.consumeWhitespace();
        assertTrue(tq.matches("class"));
        tq.consume("class");
        assertEquals('=', tq.consume());             // consume '='
        assertEquals("'test'", tq.consumeTo(">"));  // consume attribute value up to '>'
        assertTrue(tq.matchChomp(">"));
        assertEquals("Hello", tq.chompTo("</"));
        assertEquals("</", tq.peek() + "" + tq.consume() + tq.consume()); // consume '</'
        assertEquals("div", tq.consumeTagName());
        assertTrue(tq.isEmpty());
    }
}
```
###Test END##