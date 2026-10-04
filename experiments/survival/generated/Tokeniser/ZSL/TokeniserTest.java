package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Entities;
import org.jsoup.parser.Token.Character;
import org.jsoup.parser.Token.Comment;
import org.jsoup.parser.Token.Doctype;
import org.jsoup.parser.Token.StartTag;
import org.jsoup.parser.Token.Tag;
import org.jsoup.parser.Token.TokenType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive JUnit‑4 tests for {@link Tokeniser}.
 *
 * The tests use lightweight stub implementations of {@link CharacterReader}
 * and {@link ParseErrorList} so they can run without pulling in the full
 * jsoup parser machinery.
 */
public class TokeniserTest {

    /** Simple stub that mimics the behaviour required by {@link Tokeniser}. */
    private static class StubCharacterReader implements CharacterReader {
        private final String input;
        private int pos = 0;
        private int mark = -1;

        StubCharacterReader(String input) {
            this.input = input == null ? "" : input;
        }

        @Override public boolean isEmpty() { return pos >= input.length(); }

        @Override public char current() { return isEmpty() ? (char) -1 : input.charAt(pos); }

        @Override public void advance() { if (!isEmpty()) pos++; }

        @Override public boolean matches(char c) { return !isEmpty() && current() == c; }

        @Override public boolean matches(char... chars) {
            if (isEmpty()) return false;
            char cur = current();
            for (char c : chars) if (c == cur) return true;
            return false;
        }

        @Override public boolean matchesAnySorted(char[] sorted) {
            // sorted array – binary search is overkill, linear scan is fine for tests
            for (char c : sorted) if (matches(c)) return true;
            return false;
        }

        @Override public boolean matchConsume(String seq) {
            if (input.startsWith(seq, pos)) {
                pos += seq.length();
                return true;
            }
            return false;
        }

        @Override public boolean matchConsumeIgnoreCase(String seq) {
            int len = seq.length();
            if (pos + len > input.length()) return false;
            String sub = input.substring(pos, pos + len);
            if (sub.equalsIgnoreCase(seq)) {
                pos += len;
                return true;
            }
            return false;
        }

        @Override public String consumeHexSequence() {
            int start = pos;
            while (!isEmpty() && ((current() >= '0' && current() <= '9') ||
                                 (current() >= 'a' && current() <= 'f') ||
                                 (current() >= 'A' && current() <= 'F'))) {
                advance();
            }
            return input.substring(start, pos);
        }

        @Override public String consumeDigitSequence() {
            int start = pos;
            while (!isEmpty() && Character.isDigit(current())) {
                advance();
            }
            return input.substring(start, pos);
        }

        @Override public String consumeLetterThenDigitSequence() {
            int start = pos;
            while (!isEmpty() && (Character.isLetter(current()) || Character.isDigit(current()))) {
                advance();
            }
            return input.substring(start, pos);
        }

        @Override public boolean matchesLetter() { return !isEmpty() && Character.isLetter(current()); }

        @Override public boolean matchesDigit() { return !isEmpty() && Character.isDigit(current()); }

        @Override public void consume() { advance(); }

        @Override public String consumeTo(char c) {
            int start = pos;
            while (!isEmpty() && current() != c) {
                advance();
            }
            return input.substring(start, pos);
        }

        @Override public int pos() { return pos; }

        @Override public void mark() { mark = pos; }

        @Override public void rewindToMark() { if (mark >= 0) pos = mark; }

        // Unused methods of the real CharacterReader are left with dummy implementations
        @Override public String consumeTo(String seq) { throw new UnsupportedOperationException(); }
        @Override public String consumeToAny(char... chars) { throw new UnsupportedOperationException(); }
        @Override public String consumeToAny(String seq) { throw new UnsupportedOperationException(); }
        @Override public String consumeToEnd() { throw new UnsupportedOperationException(); }
        @Override public boolean matchConsume(char c) { throw new UnsupportedOperationException(); }
        @Override public boolean matchConsumeIgnoreCase(char c) { throw new UnsupportedOperationException(); }
        @Override public boolean matches(String seq) { throw new UnsupportedOperationException(); }
        @Override public boolean matchesAny(char... chars) { throw new UnsupportedOperationException(); }
        @Override public boolean matchesAny(String seq) { throw new UnsupportedOperationException(); }
        @Override public boolean matchesIgnoreCase(String seq) { throw new UnsupportedOperationException(); }
        @Override public boolean matchesLetter() { throw new UnsupportedOperationException(); }
        @Override public boolean matchesDigit() { throw new UnsupportedOperationException(); }
        @Override public boolean matchesWhitespace() { throw new UnsupportedOperationException(); }
        @Override public boolean matches(char c) { throw new UnsupportedOperationException(); }
    }

    /** Very small stub for {@link ParseErrorList}. */
    private static class StubParseErrorList implements ParseErrorList {
        private final List<ParseError> errors = new ArrayList<>();

        @Override public boolean canAddError() { return true; }

        @Override public void add(ParseError error) { errors.add(error); }

        @Override public List<ParseError> getErrors() { return errors; }

        // Remaining methods of the real interface are not needed for the tests
        @Override public int size() { return errors.size(); }
        @Override public ParseError get(int index) { return errors.get(index); }
        @Override public boolean isEmpty() { return errors.isEmpty(); }
        @Override public void addAll(ParseErrorList other) { errors.addAll(other.getErrors()); }
    }

    private Tokeniser tokeniser;
    private StubParseErrorList errorList;

    @Before
    public void setUp() {
        // default empty reader – specific tests will replace it with a fresh one
        tokeniser = new Tokeniser(new StubCharacterReader(""), new StubParseErrorList());
        errorList = (StubParseErrorList) tokeniser.errors; // package‑private access is allowed inside same package
    }

    /* --------------------------------------------------------------------- *
     *  Tests for the low‑level emit(…) overloads and read()
     * --------------------------------------------------------------------- */

    @Test
    public void emitTokenAndReadReturnsIt() {
        Token.StartTag start = new Token.StartTag();
        start.name("div");
        tokeniser.emit(start);

        Token read = tokeniser.read();
        Assert.assertEquals(TokenType.StartTag, read.type);
        Assert.assertEquals("div", ((Token.StartTag) read).tagName);
    }

    @Test
    public void emitStringBuffersUntilRead() {
        tokeniser.emit("a");
        tokeniser.emit("b");
        tokeniser.emit("c");
        Token read = tokeniser.read(); // should return a Character token with "abc"
        Assert.assertEquals(TokenType.Character, read.type);
        Character ch = (Character) read;
        Assert.assertEquals("abc", ch.getData());
    }

    @Test
    public void emitCharArrayAndCodepointArray() {
        tokeniser.emit(new char[]{'x', 'y'});
        tokeniser.emit(new int[]{0x61, 0x62}); // 'a','b'
        Token read = tokeniser.read();
        Assert.assertEquals(TokenType.Character, read.type);
        Assert.assertEquals("xyab", ((Character) read).getData());
    }

    @Test
    public void emitSingleChar() {
        tokeniser.emit('Z');
        Token read = tokeniser.read();
        Assert.assertEquals(TokenType.Character, read.type);
        Assert.assertEquals("Z", ((Character) read).getData());
    }

    /* --------------------------------------------------------------------- *
     *  State handling tests
     * --------------------------------------------------------------------- */

    @Test
    public void transitionChangesState() {
        tokeniser.transition(TokeniserState.Rcdata);
        Assert.assertEquals(TokeniserState.Rcdata, tokeniser.getState());
    }

    @Test
    public void advanceTransitionAdvancesReaderAndChangesState() {
        StubCharacterReader r = new StubCharacterReader("ABC");
        tokeniser = new Tokeniser(r, new StubParseErrorList());

        tokeniser.advanceTransition(TokeniserState.Data);
        Assert.assertEquals(1, r.pos()); // one char consumed
        Assert.assertEquals(TokeniserState.Data, tokeniser.getState());
    }

    /* --------------------------------------------------------------------- *
     *  Character reference consumption tests
     * --------------------------------------------------------------------- */

    @Test
    public void consumeDecimalCharacterReference() {
        StubCharacterReader r = new StubCharacterReader("#65;");
        tokeniser = new Tokeniser(r, new StubParseErrorList());

        int[] ref = tokeniser.consumeCharacterReference(null, false);
        Assert.assertNotNull(ref);
        Assert.assertEquals(1, ref.length);
        Assert.assertEquals('A', ref[0]);
    }

    @Test
    public void consumeHexCharacterReference() {
        StubCharacterReader r = new StubCharacterReader("#x41;");
        tokeniser = new Tokeniser(r, new StubParseErrorList());

        int[] ref = tokeniser.consumeCharacterReference(null, false);
        Assert.assertNotNull(ref);
        Assert.assertEquals('A', ref[0]);
    }

    @Test
    public void missingSemicolonStillReturnsReferenceButAddsError() {
        StubCharacterReader r = new StubCharacterReader("#65");
        tokeniser = new Tokeniser(r, new StubParseErrorList());

        int[] ref = tokeniser.consumeCharacterReference(null, false);
        Assert.assertNotNull(ref);
        Assert.assertEquals('A', ref[0]);
        Assert.assertFalse(errorList.getErrors().isEmpty());
    }

    @Test
    public void numericReferenceOutOfRangeReturnsReplacementChar() {
        // Unicode code point > 0x10FFFF
        StubCharacterReader r = new StubCharacterReader("#1114112;"); // 0x110000
        tokeniser = new Tokeniser(r, new StubParseErrorList());

        int[] ref = tokeniser.consumeCharacterReference(null, false);
        Assert.assertNotNull(ref);
        Assert.assertEquals(Tokeniser.replacementChar, ref[0]);
        Assert.assertFalse(errorList.getErrors().isEmpty());
    }

    @Test
    public void namedEntityReferenceWithSemicolon() {
        StubCharacterReader r = new StubCharacterReader("amp;");
        tokeniser = new Tokeniser(r, new StubParseErrorList());

        int[] ref = tokeniser.consumeCharacterReference(null, false);
        Assert.assertNotNull(ref);
        Assert.assertEquals('&', ref[0]);
        Assert.assertTrue(errorList.getErrors().isEmpty());
    }

    @Test
    public void namedEntityReferenceWithoutSemicolonIsAllowedOnlyIfBaseEntity() {
        StubCharacterReader r = new StubCharacterReader("amp"); // missing ';'
        tokeniser = new Tokeniser(r, new StubParseErrorList());

        int[] ref = tokeniser.consumeCharacterReference(null, false);
        Assert.assertNotNull(ref);
        Assert.assertEquals('&', ref[0]); // "amp" is a base named entity
        Assert.assertTrue(errorList.getErrors().isEmpty());
    }

    @Test
    public void invalidNamedEntityReturnsNullAndAddsError() {
        StubCharacterReader r = new StubCharacterReader("unknown;");
        tokeniser = new Tokeniser(r, new StubParseErrorList());

        int[] ref = tokeniser.consumeCharacterReference(null, false);
        Assert.assertNull(ref);
        Assert.assertFalse(errorList.getErrors().isEmpty());
    }

    @Test
    public void consumeCharacterReferenceInAttributeStopsAtLetterDigitOrEquals() {
        // "&ampx" – after "amp" there is a letter 'x', so the reference must be rejected in attribute mode
        StubCharacterReader r = new StubCharacterReader("ampx;");
        tokeniser = new Tokeniser(r, new StubParseErrorList());

        int[] ref = tokeniser.consumeCharacterReference(null, true);
        Assert.assertNull(ref);
        // No error is added because the parser simply treats it as literal '&'
    }

    /* --------------------------------------------------------------------- *
     *  Tag creation / emission tests
     * --------------------------------------------------------------------- */

    @Test
    public void createStartTagPendingAndEmit() {
        tokeniser.createTagPending(true);
        tokeniser.tagPending.name("p");
        tokeniser.emitTagPending();

        Token read = tokeniser.read();
        Assert.assertEquals(TokenType.StartTag, read.type);
        Assert.assertEquals("p", ((StartTag) read).tagName);
    }

    @Test
    public void createEndTagPendingAndEmit() {
        tokeniser.createTagPending(false);
        tokeniser.tagPending.name("span");
        tokeniser.emitTagPending();

        Token read = tokeniser.read();
        Assert.assertEquals(TokenType.EndTag, read.type);
        Assert.assertEquals("span", ((Token.EndTag) read).tagName);
    }

    @Test
    public void createAndEmitComment() {
        tokeniser.createCommentPending();
        tokeniser.commentPending.append("hello");
        tokeniser.emitCommentPending();

        Token read = tokeniser.read();
        Assert.assertEquals(TokenType.Comment, read.type);
        Assert.assertEquals("hello", ((Comment) read).getData());
    }

    @Test
    public void createAndEmitDoctype() {
        tokeniser.createDoctypePending();
        tokeniser.doctypePending.name("html");
        tokeniser.emitDoctypePending();

        Token read = tokeniser.read();
        Assert.assertEquals(TokenType.Doctype, read.type);
        Assert.assertEquals("html", ((Doctype) read).name);
    }

    @Test
    public void createTempBufferResetsDataBuffer() {
        tokeniser.dataBuffer.append("some data");
        tokeniser.createTempBuffer();
        Assert.assertEquals(0, tokeniser.dataBuffer.length());
    }

    /* --------------------------------------------------------------------- *
     *  Appropriateness of end‑tag tests
     * --------------------------------------------------------------------- */

    @Test
    public void appropriateEndTagDetectionWorks() {
        // Emit a start tag first – this sets lastStartTag
        tokeniser.createTagPending(true);
        tokeniser.tagPending.name("section");
        tokeniser.emitTagPending();
        tokeniser.read(); // consume the start tag

        // Now create an end tag with the same name
        tokeniser.createTagPending(false);
        tokeniser.tagPending.name("section");
        Assert.assertTrue(tokeniser.isAppropriateEndTagToken());
        Assert.assertEquals("section", tokeniser.appropriateEndTagName());
    }

    @Test
    public void inappropriateEndTagDetectionFails() {
        tokeniser.createTagPending(true);
        tokeniser.tagPending.name("div");
        tokeniser.emitTagPending();
        tokeniser.read();

        tokeniser.createTagPending(false);
        tokeniser.tagPending.name("span");
        Assert.assertFalse(tokeniser.isAppropriateEndTagToken());
        Assert.assertEquals("div", tokeniser.appropriateEndTagName());
    }

    /* --------------------------------------------------------------------- *
     *  Error handling tests
     * --------------------------------------------------------------------- */

    @Test
    public void errorAddsParseError() {
        tokeniser.error(TokeniserState.Data);
        Assert.assertEquals(1, errorList.getErrors().size());
        Assert.assertTrue(errorList.getErrors().get(0).getErrorMessage().contains("Unexpected character"));
    }

    @Test
    public void eofErrorAddsParseError() {
        tokeniser.eofError(TokeniserState.Data);
        Assert.assertEquals(1, errorList.getErrors().size());
        Assert.assertTrue(errorList.getErrors().get(0).getErrorMessage().contains("EOF"));
    }

    @Test
    public void genericErrorAddsParseError() {
        tokeniser.error("custom error message");
        Assert.assertEquals(1, errorList.getErrors().size());
        Assert.assertTrue(errorList.getErrors().get(0).getErrorMessage().contains("custom error message"));
    }

    /* --------------------------------------------------------------------- *
     *  Miscellaneous utility tests
     * --------------------------------------------------------------------- */

    @Test
    public void currentNodeInHtmlNamespaceAlwaysTrue() {
        Assert.assertTrue(tokeniser.currentNodeInHtmlNS());
    }

    @Test
    public void unescapeEntitiesHandlesMultipleReferences() {
        // Input: "a &amp; b &#x3C; c &unknown; d"
        StubCharacterReader r = new StubCharacterReader("a &amp; b &#x3C; c &unknown; d");
        tokeniser = new Tokeniser(r, new StubParseErrorList());

        String result = tokeniser.unescapeEntities(false);
        // Expected: a & b < c &unknown; d   (unknown entity left as literal)
        Assert.assertEquals("a & b < c &unknown; d", result);
        // Two errors should have been recorded: missing semicolon for unknown and unknown name
        Assert.assertEquals(2, errorList.getErrors().size());
    }
}
