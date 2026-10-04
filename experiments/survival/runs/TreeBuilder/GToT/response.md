###Test START##
package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TreeBuilder}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public method signatures extraction (implicit – all public methods are exercised).</li>
 *   <li>Normal use cases for each method.</li>
 *   <li>Edge‑case handling such as {@code null} arguments and token reuse.</li>
 *   <li>Exception scenarios (IllegalArgumentException, NullPointerException).</li>
 * </ul>
 *
 * Because {@code TreeBuilder} is abstract, a minimal concrete subclass
 * {@code TestTreeBuilder} is defined inside the test file.  It implements the
 * abstract methods with simple logic that is sufficient for exercising the
 * protected behaviour of the base class.
 */
public class TreeBuilderTest {

    /** Minimal concrete implementation used only for testing the protected API. */
    private static class TestTreeBuilder extends TreeBuilder {
        /** Captured token passed to {@link #process(Token)} – used for assertions. */
        Token capturedToken;

        /** Simple {@link ParseSettings} implementation – not relevant for the tests. */
        @Override
        ParseSettings defaultSettings() {
            // All settings enabled – the concrete values are irrelevant for the tests.
            return new ParseSettings(true, true);
        }

        /**
         * Very small processing implementation:
         * <ul>
         *   <li>Stores the token in {@link #capturedToken} for later inspection.</li>
         *   <li>Updates the element {@code stack} for start/end tags so that
         *   {@link #currentElement()} can be verified.</li>
         * </ul>
         */
        @Override
        protected boolean process(Token token) {
            this.capturedToken = token;
            if (token instanceof Token.StartTag) {
                Token.StartTag startTag = (Token.StartTag) token;
                // Create a dummy element using the tag name – the actual element content is irrelevant.
                Element el = new Element(startTag.name(), baseUri);
                // Add any supplied attributes.
                if (startTag.hasAttributes()) {
                    el.attributes().addAll(startTag.attributes());
                }
                stack.add(el);
            } else if (token instanceof Token.EndTag) {
                // Pop the current element if the stack is not empty.
                if (!stack.isEmpty()) {
                    stack.remove(stack.size() - 1);
                }
            }
            // Returning true signals that parsing should continue.
            return true;
        }

        /** Helper to clear the captured token between tests. */
        void resetCapturedToken() {
            capturedToken = null;
        }
    }

    private TestTreeBuilder builder;
    private final String baseUri = "http://example.com/";

    @Before
    public void setUp() {
        builder = new TestTreeBuilder();
    }

    /* ---------------------------------------------------------------------- */
    /*  1. initialiseParse – normal behaviour                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void initialiseParse_ValidInput_InitialisesAllFields() {
        Reader input = new StringReader("<html></html>");
        ParseErrorList errors = ParseErrorList.tracking(0);
        ParseSettings settings = new ParseSettings(true, true);

        builder.initialiseParse(input, baseUri, errors, settings);

        assertNotNull("Document should be created", builder.doc);
        assertEquals("Base URI must be stored", baseUri, builder.baseUri);
        assertNotNull("CharacterReader must be initialised", builder.reader);
        assertNotNull("Tokeniser must be initialised", builder.tokeniser);
        assertNotNull("Stack must be initialised", builder.stack);
        assertTrue("Stack should be empty after initialiseParse", builder.stack.isEmpty());
        assertEquals("Settings must be stored", settings, builder.settings);
        assertEquals("Error list must be stored", errors, builder.errors);
        assertNull("Current token should be null after initialiseParse", builder.currentToken);
    }

    /* ---------------------------------------------------------------------- */
    /*  2. initialiseParse – null arguments (edge cases)                      */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void initialiseParse_NullReader_Throws() {
        builder.initialiseParse(null, baseUri, ParseErrorList.tracking(0), new ParseSettings(true, true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void initialiseParse_NullBaseUri_Throws() {
        builder.initialiseParse(new StringReader("<p/>"), null, ParseErrorList.tracking(0), new ParseSettings(true, true));
    }

    /* ---------------------------------------------------------------------- */
    /*  3. parse – high‑level convenience method                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void parse_ValidInput_ReturnsDocument() {
        Reader input = new StringReader("<html><body>Test</body></html>");
        Document doc = builder.parse(input, baseUri, ParseErrorList.tracking(0), new ParseSettings(true, true));
        assertNotNull("parse() must return a non‑null Document", doc);
        assertEquals("Document base URI must be preserved", baseUri, doc.baseUri());
    }

    /* ---------------------------------------------------------------------- */
    /*  4. processStartTag(String) – simple start‑tag handling                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void processStartTag_SimpleName_ProcessesCorrectly() {
        builder.resetCapturedToken();
        boolean result = builder.processStartTag("div");
        assertTrue("processStartTag should return true (as defined by process)", result);
        assertNotNull("Token should have been captured", builder.capturedToken);
        assertTrue("Captured token must be a StartTag", builder.capturedToken instanceof Token.StartTag);
        Token.StartTag tag = (Token.StartTag) builder.capturedToken;
        assertEquals("Tag name must match", "div", tag.name());
        assertFalse("Tag should have no attributes", tag.hasAttributes());
        // Verify that the element stack now contains the element created for the tag.
        assertEquals("Stack size should be 1 after processing a start tag", 1, builder.stack.size());
        Element el = builder.stack.get(0);
        assertEquals("Element tag name must match", "div", el.tagName());
    }

    @Test(expected = NullPointerException.class)
    public void processStartTag_NullName_ThrowsNPE() {
        // The Token.StartTag implementation will throw NPE when name() is called with null.
        builder.processStartTag(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  5. processStartTag(String, Attributes) – start‑tag with attributes   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void processStartTag_WithAttributes_ProcessesCorrectly() {
        Attributes attrs = new Attributes();
        attrs.put("id", "testId");
        attrs.put("class", "myClass");

        builder.resetCapturedToken();
        boolean result = builder.processStartTag("span", attrs);
        assertTrue(result);
        assertNotNull(builder.capturedToken);
        assertTrue(builder.capturedToken instanceof Token.StartTag);
        Token.StartTag tag = (Token.StartTag) builder.capturedToken;
        assertEquals("span", tag.name());
        assertTrue(tag.hasAttributes());
        assertEquals("testId", tag.attributes().get("id"));
        assertEquals("myClass", tag.attributes().get("class"));

        // Stack should contain the element with the same attributes.
        assertEquals(1, builder.stack.size());
        Element el = builder.stack.get(0);
        assertEquals("span", el.tagName());
        assertEquals("testId", el.attr("id"));
        assertEquals("myClass", el.attr("class"));
    }

    @Test(expected = NullPointerException.class)
    public void processStartTag_WithNullAttributes_ThrowsNPE() {
        // Passing a null Attributes instance should propagate a NPE inside the token logic.
        builder.processStartTag("a", null);
    }

    /* ---------------------------------------------------------------------- */
    /*  6. processEndTag(String) – simple end‑tag handling                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void processEndTag_SimpleName_ProcessesCorrectly() {
        // First push a start tag so that there is something on the stack.
        builder.processStartTag("p");
        builder.resetCapturedToken();

        boolean result = builder.processEndTag("p");
        assertTrue(result);
        assertNotNull(builder.capturedToken);
        assertTrue(builder.capturedToken instanceof Token.EndTag);
        Token.EndTag endTag = (Token.EndTag) builder.capturedToken;
        assertEquals("p", endTag.name());

        // Stack should be empty after processing the matching end tag.
        assertTrue("Stack must be empty after matching end tag", builder.stack.isEmpty());
    }

    @Test
    public void processEndTag_NoMatchingStartTag_StackRemainsEmpty() {
        // Stack is empty before processing an end tag.
        boolean result = builder.processEndTag("ul");
        assertTrue(result);
        assertNotNull(builder.capturedToken);
        assertTrue(builder.capturedToken instanceof Token.EndTag);
        // Stack should still be empty – the implementation simply ignores the mismatch.
        assertTrue(builder.stack.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /*  7. currentElement() – retrieving the top of the element stack          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void currentElement_EmptyStack_ReturnsNull() {
        assertNull("When stack is empty, currentElement() must return null", builder.currentElement());
    }

    @Test
    public void currentElement_NonEmptyStack_ReturnsTopElement() {
        builder.processStartTag("section");
        builder.processStartTag("article");
        Element current = builder.currentElement();
        assertNotNull(current);
        assertEquals("article", current.tagName());
    }

    /* ---------------------------------------------------------------------- */
    /*  8. runParser – integration test with a tiny custom tokeniser          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void runParser_ProcessesAllTokensUntilEOF() {
        // Build a very small token sequence: <h1> … </h1> EOF
        List<Token> tokenSequence = new ArrayList<>();
        tokenSequence.add(new Token.StartTag().name("h1"));
        tokenSequence.add(new Token.EndTag().name("h1"));
        tokenSequence.add(new Token(Token.TokenType.EOF));

        // Replace the real tokeniser with a stub that serves the prepared sequence.
        builder.tokeniser = new StubTokeniser(tokenSequence);
        // Initialise the other required fields manually (normally done by initialiseParse).
        builder.doc = new Document(baseUri);
        builder.stack = new ArrayList<>(32);
        builder.baseUri = baseUri;
        builder.errors = ParseErrorList.tracking(0);
        builder.settings = new ParseSettings(true, true);

        // Run the parser – it should consume all tokens and leave the stack empty.
        builder.runParser();

        // Two start/end tags should have been processed, leaving the stack empty.
        assertTrue("Stack must be empty after processing matching start/end tags", builder.stack.isEmpty());
        // The captured token will be the last processed token (the EOF token).
        assertNotNull(builder.capturedToken);
        assertEquals("Last token processed must be EOF", Token.TokenType.EOF, builder.capturedToken.type);
    }

    /**
     * Very small stub that mimics {@link Tokeniser} but returns a predefined list of tokens.
     */
    private static class StubTokeniser extends Tokeniser {
        private final List<Token> tokens;
        private int index = 0;

        StubTokeniser(List<Token> tokens) {
            // The super‑constructor requires a CharacterReader and a ParseErrorList.
            // We can safely pass dummy values because the overridden {@link #read()} does not use them.
            super(new CharacterReader(new StringReader("")), ParseErrorList.tracking(0));
            this.tokens = tokens;
        }

        @Override
        public Token read() {
            if (index < tokens.size()) {
                return tokens.get(index++);
            }
            // Default fallback – return an EOF token if the list is exhausted.
            return new Token(Token.TokenType.EOF);
        }
    }
}
###Test END##