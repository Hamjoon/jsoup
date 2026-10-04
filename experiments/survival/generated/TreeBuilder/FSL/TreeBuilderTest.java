/***************************************************************************************************
 * JUnit‑4 test suite for the abstract class {@code TreeBuilder}.
 *
 * The tests verify the behaviour of the protected utility methods that are common to all concrete
 * implementations:
 *
 *   • initialiseParse – sets up the parsing environment (reader, tokeniser, document, stack …)
 *   • processStartTag(String)          – re‑uses the cached {@code Token.StartTag} when possible
 *   • processStartTag(String,Attributes) – same but with attribute payload
 *   • processEndTag(String)            – similar logic for {@code Token.EndTag}
 *   • currentElement()                 – returns the element on top of the stack or {@code null}
 *
 * Because {@code TreeBuilder} is abstract we provide a minimal concrete subclass
 * {@code TestTreeBuilder} that implements the abstract methods.  Its {@code process(Token)} method
 * simply records the token that was handed to it so the test can make assertions about which
 * instance was used.
 *
 * The test class lives in the same package as the class under test (org.jsoup.parser) so it can
 * access protected members directly.
 **************************************************************************************************/
package org.jsoup.parser;

import static org.junit.Assert.*;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;

/**
 * Concrete implementation used only for testing the non‑abstract behaviour of {@code TreeBuilder}.
 */
class TestTreeBuilder extends TreeBuilder {

    /** Holds the last token handed to {@link #process(Token)} so the test can inspect it. */
    Token lastProcessedToken;

    /** Dummy implementation – the real library returns default parsing settings. */
    @Override
    ParseSettings defaultSettings() {
        return new ParseSettings();          // safe default – never {@code null}
    }

    /**
     * Very small processing routine – it does not try to build a real DOM, it merely records the
     * token that arrived.  For {@code StartTag} tokens we push a placeholder {@code Element}
     * onto the stack so that {@link #currentElement()} can be exercised.
     */
    @Override
    protected boolean process(Token token) {
        // remember the token for the test
        lastProcessedToken = token;

        // Very small DOM‑like behaviour so that currentElement() can be verified
        if (token instanceof Token.StartTag) {
            Token.StartTag start = (Token.StartTag) token;
            // create a minimal Element – the tag name is whatever was supplied
            Element el = new Element(Tag.valueOf(start.name()), baseUri);
            stack.add(el);
        } else if (token instanceof Token.EndTag) {
            // pop if we have something on the stack
            if (!stack.isEmpty()) {
                stack.remove(stack.size() - 1);
            }
        }
        // always signal that processing succeeded
        return true;
    }
}

/**
 * Test suite exercising the protected helper methods of {@link TreeBuilder}.
 */
public class TreeBuilderTest {

    private TestTreeBuilder builder;
    private Document doc;
    private static final String BASE_URI = "http://example.com";

    @Before
    public void setUp() {
        builder = new TestTreeBuilder();
    }

    /** Helper to fetch the private {@code start} token via reflection. */
    private Token.StartTag getPrivateStartToken() throws Exception {
        Field f = TreeBuilder.class.getDeclaredField("start");
        f.setAccessible(true);
        return (Token.StartTag) f.get(builder);
    }

    /** Helper to fetch the private {@code end} token via reflection. */
    private Token.EndTag getPrivateEndToken() throws Exception {
        Field f = TreeBuilder.class.getDeclaredField("end");
        f.setAccessible(true);
        return (Token.EndTag) f.get(builder);
    }

    /* --------------------------------------------------------------------- *
     *  1. initialiseParse – basic sanity checks
     * --------------------------------------------------------------------- */
    @Test
    public void initialiseParse_setsUpAllFields() {
        Reader input = new StringReader("dummy");
        ParseErrorList errors = new ParseErrorList(0);
        ParseSettings settings = new ParseSettings();

        // invoke the protected method via reflection (it is not public)
        try {
            java.lang.reflect.Method m = TreeBuilder.class
                    .getDeclaredMethod("initialiseParse", Reader.class, String.class,
                            ParseErrorList.class, ParseSettings.class);
            m.setAccessible(true);
            m.invoke(builder, input, BASE_URI, errors, settings);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        // verify that the document has the correct base uri
        assertNotNull("Document should be created", builder.doc);
        assertEquals(BASE_URI, builder.doc.baseUri());

        // the stack must be instantiated and empty
        assertNotNull(builder.stack);
        assertTrue(builder.stack.isEmpty());

        // reader, tokeniser and error list must be non‑null
        assertNotNull(builder.reader);
        assertNotNull(builder.tokeniser);
        assertSame(errors, builder.errors);
        assertSame(settings, builder.settings);
    }

    /* --------------------------------------------------------------------- *
     *  2. processStartTag(String) – re‑use of the cached token
     * --------------------------------------------------------------------- */
    @Test
    public void processStartTag_reusesStartTokenWhenNotInUse() throws Exception {
        // initialise so that the builder is ready (stack, doc etc.)
        builder.initialiseParse(new StringReader(""), BASE_URI,
                new ParseErrorList(0), new ParseSettings());

        // currentToken is null → the cached token may be reused
        builder.currentToken = null;
        builder.processStartTag("p");

        // the token that reached process() must be the same instance as the private 'start'
        Token.StartTag privateStart = getPrivateStartToken();
        assertSame("The cached start token should be reused", privateStart,
                builder.lastProcessedToken);
        assertEquals("p", ((Token.StartTag) builder.lastProcessedToken).name());
    }

    @Test
    public void processStartTag_createsNewTokenWhenStartIsInUse() throws Exception {
        builder.initialiseParse(new StringReader(""), BASE_URI,
                new ParseErrorList(0), new ParseSettings());

        // Simulate that the cached start token is currently being used elsewhere
        Token.StartTag privateStart = getPrivateStartToken();
        builder.currentToken = privateStart;   // mark it as "in use"

        builder.processStartTag("div");

        // The token handed to process() must *not* be the same instance as the cached one
        assertNotSame("A new token should be allocated when the cached one is in use",
                privateStart, builder.lastProcessedToken);
        assertEquals("div", ((Token.StartTag) builder.lastProcessedToken).name());
    }

    /* --------------------------------------------------------------------- *
     *  3. processStartTag(String,Attributes) – attribute handling and reuse logic
     * --------------------------------------------------------------------- */
    @Test
    public void processStartTagWithAttributes_usesCachedTokenWhenFree() {
        builder.initialiseParse(new StringReader(""), BASE_URI,
                new ParseErrorList(0), new ParseSettings());

        builder.currentToken = null; // cached token is free
        Attributes attrs = new Attributes();
        attrs.put("id", "test");

        builder.processStartTag("span", attrs);

        // The token processed must be the same cached instance
        Token.StartTag privateStart = null;
        try {
            privateStart = getPrivateStartToken();
        } catch (Exception e) {
            fail("Reflection error: " + e);
        }
        assertSame(privateStart, builder.lastProcessedToken);
        Token.StartTag processed = (Token.StartTag) builder.lastProcessedToken;
        assertEquals("span", processed.name());
        assertEquals("test", processed.attributes().get("id"));
    }

    @Test
    public void processStartTagWithAttributes_createsNewTokenWhenCachedInUse() throws Exception {
        builder.initialiseParse(new StringReader(""), BASE_URI,
                new ParseErrorList(0), new ParseSettings());

        Token.StartTag privateStart = getPrivateStartToken();
        builder.currentToken = privateStart; // mark as in‑use

        Attributes attrs = new Attributes();
        attrs.put("class", "myClass");
        builder.processStartTag("a", attrs);

        // Must be a *different* instance
        assertNotSame(privateStart, builder.lastProcessedToken);
        Token.StartTag processed = (Token.StartTag) builder.lastProcessedToken;
        assertEquals("a", processed.name());
        assertEquals("myClass", processed.attributes().get("class"));
    }

    /* --------------------------------------------------------------------- *
     *  4. processEndTag(String) – similar reuse logic to start tag
     * --------------------------------------------------------------------- */
    @Test
    public void processEndTag_reusesEndTokenWhenFree() throws Exception {
        builder.initialiseParse(new StringReader(""), BASE_URI,
                new ParseErrorList(0), new ParseSettings());

        builder.currentToken = null; // end token is free
        builder.processEndTag("section");

        Token.EndTag privateEnd = getPrivateEndToken();
        assertSame("Cached end token should be reused", privateEnd, builder.lastProcessedToken);
        assertEquals("section", ((Token.EndTag) builder.lastProcessedToken).name());
    }

    @Test
    public void processEndTag_createsNewTokenWhenCachedInUse() throws Exception {
        builder.initialiseParse(new StringReader(""), BASE_URI,
                new ParseErrorList(0), new ParseSettings());

        Token.EndTag privateEnd = getPrivateEndToken();
        builder.currentToken = privateEnd; // simulate "in use"

        builder.processEndTag("footer");

        // New instance expected
        assertNotSame(privateEnd, builder.lastProcessedToken);
        assertEquals("footer", ((Token.EndTag) builder.lastProcessedToken).name());
    }

    /* --------------------------------------------------------------------- *
     *  5. currentElement() – stack top handling
     * --------------------------------------------------------------------- */
    @Test
    public void currentElement_returnsNullOnEmptyStack() {
        builder.initialiseParse(new StringReader(""), BASE_URI,
                new ParseErrorList(0), new ParseSettings());

        assertNull("Empty stack should yield null", builder.currentElement());
    }

    @Test
    public void currentElement_returnsTopOfStack() {
        builder.initialiseParse(new StringReader(""), BASE_URI,
                new ParseErrorList(0), new ParseSettings());

        Element first = new Element(Tag.valueOf("first"), BASE_URI);
        Element second = new Element(Tag.valueOf("second"), BASE_URI);
        builder.stack.add(first);
        builder.stack.add(second); // second is on top

        assertSame("Should return the element on top of the stack", second,
                builder.currentElement());
    }

    /* --------------------------------------------------------------------- *
     *  6. parse() and runParser() – end‑to‑end sanity (uses a tiny custom tokeniser)
     * --------------------------------------------------------------------- */
    @Test
    public void parse_runsParserUntilEOF() {
        // The real Tokeniser would try to read real HTML.  For this test we supply a Reader that
        // immediately yields EOF, which makes the while‑loop in runParser() terminate after one
        // iteration.
        Reader input = new StringReader(""); // empty source → EOF straight away
        Document out = builder.parse(input, BASE_URI, new ParseErrorList(0), new ParseSettings());

        // The returned Document must be the one created during initialiseParse()
        assertNotNull(out);
        assertEquals(BASE_URI, out.baseUri());
        // No elements should have been pushed because we never emitted a start‑tag token
        assertTrue("Stack must stay empty when tokeniser only emits EOF", builder.stack.isEmpty());
    }
}
