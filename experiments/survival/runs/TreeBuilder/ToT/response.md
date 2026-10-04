###Test START##
```java
package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link TreeBuilder}.
 *
 * The tests use a minimal concrete implementation of the abstract {@code TreeBuilder}
 * that records every {@link Token} passed to {@link #process(Token)}.
 *
 * The suite covers:
 * <ul>
 *   <li>initialisation (null checks, field population)</li>
 *   <li>public parsing entry point ({@code parse})</li>
 *   <li>the helper methods {@code processStartTag(..)}, {@code processEndTag(..)} and {@code currentElement()}</li>
 *   <li>edge cases when the internal reusable tokens are already in use</li>
 * </ul>
 */
public class TreeBuilderTest {

    /** Simple concrete TreeBuilder used only for testing. */
    private static class TestTreeBuilder extends TreeBuilder {

        /** Stores every token handed to {@link #process(Token)} for later assertions. */
        final List<Token> processed = new ArrayList<>();

        @Override
        ParseSettings defaultSettings() {
            // Settings are irrelevant for these tests – return a harmless instance.
            return new ParseSettings(true, true);
        }

        @Override
        protected boolean process(Token token) {
            // Record the token and make it the current token (mirrors real behaviour).
            processed.add(token);
            currentToken = token;
            return true;
        }

        /** Helper to push an element onto the internal stack. */
        void push(Element el) {
            stack.add(el);
        }
    }

    private TestTreeBuilder builder;
    private Document doc;
    private ParseErrorList errors;
    private ParseSettings settings;

    @Before
    public void setUp() {
        builder = new TestTreeBuilder();
        errors = new ParseErrorList(0);
        settings = new ParseSettings(true, true);
    }

    /** --------------------------------------------------------------------- *
     *  initialiseParse() – argument validation
     *  --------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void initialiseParse_nullReader_throws() {
        builder.initialiseParse(null, "http://example.com", errors, settings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void initialiseParse_nullBaseUri_throws() {
        builder.initialiseParse(new StringReader(""), null, errors, settings);
    }

    /** --------------------------------------------------------------------- *
     *  initialiseParse() – normal initialisation
     *  --------------------------------------------------------------------- */
    @Test
    public void initialiseParse_setsAllFields() {
        Reader input = new StringReader("<html></html>");
        String base = "http://example.com";

        builder.initialiseParse(input, base, errors, settings);

        // Document must be created with the supplied base URI
        assertNotNull(builder.doc);
        assertEquals(base, builder.doc.baseUri());

        // Reader and Tokeniser must be non‑null
        assertNotNull(builder.reader);
        assertNotNull(builder.tokeniser);

        // Stack must be initialised empty
        assertNotNull(builder.stack);
        assertTrue(builder.stack.isEmpty());

        // Errors and settings are stored verbatim
        assertSame(errors, builder.errors);
        assertSame(settings, builder.settings);
    }

    /** --------------------------------------------------------------------- *
     *  parse() – returns a Document and runs the parser (EOF only)
     *  --------------------------------------------------------------------- */
    @Test
    public void parse_emptyInput_returnsDocument() {
        Reader input = new StringReader("");               // triggers immediate EOF
        String base = "http://example.com";

        Document result = builder.parse(input, base, errors, settings);

        assertSame(builder.doc, result);                  // same instance that was created
        assertEquals(base, result.baseUri());
        // No elements have been added – stack stays empty
        assertTrue(builder.stack.isEmpty());
    }

    /** --------------------------------------------------------------------- *
     *  processStartTag(String) – normal reuse of the internal start token
     *  --------------------------------------------------------------------- */
    @Test
    public void processStartTag_reusesInternalStartToken() {
        builder.initialiseParse(new StringReader(""), "http://example.com", errors, settings);
        builder.processStartTag("div");

        // Exactly one token processed
        assertEquals(1, builder.processed.size());
        Token token = builder.processed.get(0);
        assertEquals(Token.TokenType.StartTag, token.type);
        assertTrue(token instanceof Token.StartTag);
        assertEquals("div", ((Token.StartTag) token).name());
    }

    /** --------------------------------------------------------------------- *
     *  processStartTag(String) – creates a fresh token when the internal one
     *  is already the current token (i.e. in use)
     *  --------------------------------------------------------------------- */
    @Test
    public void processStartTag_whenStartTokenInUse_createsNewToken() throws Exception {
        builder.initialiseParse(new StringReader(""), "http://example.com", errors, settings);

        // Use reflection to obtain the private reusable start token
        java.lang.reflect.Field startField = TreeBuilder.class.getDeclaredField("start");
        startField.setAccessible(true);
        Token.StartTag internalStart = (Token.StartTag) startField.get(builder);

        // Simulate the internal token being the current token
        builder.currentToken = internalStart;

        builder.processStartTag("span");

        // Two tokens should have been processed: the original (still in use) and the new one.
        assertEquals(1, builder.processed.size()); // only the new token is recorded by our process()
        Token token = builder.processed.get(0);
        assertEquals(Token.TokenType.StartTag, token.type);
        assertTrue(token instanceof Token.StartTag);
        assertEquals("span", ((Token.StartTag) token).name());

        // Verify that the token we processed is NOT the same instance as the internal reusable token
        assertNotSame(internalStart, token);
    }

    /** --------------------------------------------------------------------- *
     *  processStartTag(String, Attributes) – attribute handling
     *  --------------------------------------------------------------------- */
    @Test
    public void processStartTag_withAttributes_passesAttributesCorrectly() {
        builder.initialiseParse(new StringReader(""), "http://example.com", errors, settings);
        Attributes attrs = new Attributes();
        attrs.put("id", "myId");
        attrs.put("class", "myClass");

        builder.processStartTag("p", attrs);

        assertEquals(1, builder.processed.size());
        Token token = builder.processed.get(0);
        assertEquals(Token.TokenType.StartTag, token.type);
        Token.StartTag startTag = (Token.StartTag) token;
        assertEquals("p", startTag.name());
        // The token must contain the same attributes (order is not important)
        assertEquals("myId", startTag.attributes().get("id"));
        assertEquals("myClass", startTag.attributes().get("class"));
    }

    /** --------------------------------------------------------------------- *
     *  processEndTag(String) – normal reuse of the internal end token
     *  --------------------------------------------------------------------- */
    @Test
    public void processEndTag_reusesInternalEndToken() {
        builder.initialiseParse(new StringReader(""), "http://example.com", errors, settings);
        builder.processEndTag("div");

        assertEquals(1, builder.processed.size());
        Token token = builder.processed.get(0);
        assertEquals(Token.TokenType.EndTag, token.type);
        assertTrue(token instanceof Token.EndTag);
        assertEquals("div", ((Token.EndTag) token).name());
    }

    /** --------------------------------------------------------------------- *
     *  processEndTag(String) – creates a fresh token when the internal one
     *  is already the current token
     *  --------------------------------------------------------------------- */
    @Test
    public void processEndTag_whenEndTokenInUse_createsNewToken() throws Exception {
        builder.initialiseParse(new StringReader(""), "http://example.com", errors, settings);

        // Obtain the private reusable end token via reflection
        java.lang.reflect.Field endField = TreeBuilder.class.getDeclaredField("end");
        endField.setAccessible(true);
        Token.EndTag internalEnd = (Token.EndTag) endField.get(builder);

        // Simulate the internal token being the current token
        builder.currentToken = internalEnd;

        builder.processEndTag("section");

        assertEquals(1, builder.processed.size());
        Token token = builder.processed.get(0);
        assertEquals(Token.TokenType.EndTag, token.type);
        assertTrue(token instanceof Token.EndTag);
        assertEquals("section", ((Token.EndTag) token).name());

        // Ensure a different instance was used
        assertNotSame(internalEnd, token);
    }

    /** --------------------------------------------------------------------- *
     *  currentElement() – empty stack returns null
     *  --------------------------------------------------------------------- */
    @Test
    public void currentElement_emptyStack_returnsNull() {
        builder.initialiseParse(new StringReader(""), "http://example.com", errors, settings);
        assertNull(builder.currentElement());
    }

    /** --------------------------------------------------------------------- *
     *  currentElement() – returns the element on top of the stack
     *  --------------------------------------------------------------------- */
    @Test
    public void currentElement_nonEmptyStack_returnsTopElement() {
        builder.initialiseParse(new StringReader(""), "http://example.com", errors, settings);
        Element first = new Element("div");
        Element second = new Element("p");
        builder.push(first);
        builder.push(second);

        Element current = builder.currentElement();
        assertSame(second, current);
    }

    /** --------------------------------------------------------------------- *
     *  runParser() – exercised indirectly via parse() on an empty input.
     *  The test verifies that the loop terminates after an EOF token.
     *  --------------------------------------------------------------------- */
    @Test
    public void runParser_terminatesOnEof() throws IOException {
        // Empty input => tokeniser will emit a single EOF token.
        Reader input = new StringReader("");
        builder.parse(input, "http://example.com", errors, settings);

        // Exactly one token (the EOF) should have been processed.
        assertEquals(1, builder.processed.size());
        Token token = builder.processed.get(0);
        assertEquals(Token.TokenType.EOF, token.type);
    }
}
```
###Test END##