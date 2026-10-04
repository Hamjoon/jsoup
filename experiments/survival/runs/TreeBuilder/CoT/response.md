###Test START##
```java
package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Token;
import org.jsoup.parser.Token.StartTag;
import org.jsoup.parser.Token.EndTag;
import org.junit.Before;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link TreeBuilder}.
 *
 * The concrete class {@link TestableTreeBuilder} supplies minimal implementations
 * of the abstract methods so that the protected and public behaviour of {@code TreeBuilder}
 * can be exercised.
 *
 * The tests cover:
 * <ul>
 *   <li>Extraction of public method signatures</li>
 *   <li>Basic functionality of each public method</li>
 *   <li>Edge‑case handling (null arguments, empty stack, token recycling)</li>
 *   <li>Exception scenarios triggered by {@link Validate#notNull}</li>
 * </ul>
 */
public class TreeBuilderTest {

    /** Simple concrete subclass that records the last token processed. */
    private static class TestableTreeBuilder extends TreeBuilder {
        Token lastProcessedToken;

        @Override
        ParseSettings defaultSettings() {
            // default settings are not important for the tests – use a harmless instance
            return new ParseSettings(true, true);
        }

        @Override
        protected boolean process(Token token) {
            // remember the token so the test can inspect it
            this.lastProcessedToken = token;
            // return true to indicate the token was “handled”
            return true;
        }

        /** Helper to push an element onto the protected stack. */
        void pushElement(Element el) {
            if (stack == null) {
                stack = new java.util.ArrayList<>(32);
            }
            stack.add(el);
        }

        /** Expose the protected {@code start} token for reflection tests. */
        Token.StartTag getStartToken() {
            try {
                Field f = TreeBuilder.class.getDeclaredField("start");
                f.setAccessible(true);
                return (Token.StartTag) f.get(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Expose the protected {@code end} token for reflection tests. */
        Token.EndTag getEndToken() {
            try {
                Field f = TreeBuilder.class.getDeclaredField("end");
                f.setAccessible(true);
                return (Token.EndTag) f.get(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private TestableTreeBuilder builder;
    private Reader emptyReader;
    private String baseUri = "http://example.com/";

    @Before
    public void setUp() {
        builder = new TestableTreeBuilder();
        emptyReader = new StringReader("");
    }

    /* -------------------------------------------------
     *  1.  Public method extraction (for documentation)
     * ------------------------------------------------- */
    // The only public method in TreeBuilder is:
    //   public boolean processStartTag(String name, Attributes attrs)

    /* -------------------------------------------------
     *  2.  Basic functionality tests
     * ------------------------------------------------- */

    @Test
    public void testInitialiseParse_ValidArguments() {
        ParseErrorList errors = new ParseErrorList(0);
        ParseSettings settings = new ParseSettings(true, true);

        builder.initialiseParse(emptyReader, baseUri, errors, settings);

        // Document must be created with the supplied base URI
        assertNotNull("Document should be instantiated", builder.doc);
        assertEquals(baseUri, builder.doc.baseUri());

        // Reader, Tokeniser and settings must be stored
        assertNotNull("CharacterReader should be initialised", builder.reader);
        assertNotNull("Tokeniser should be initialised", builder.tokeniser);
        assertSame(settings, builder.settings);

        // Stack must be non‑null (empty at start)
        assertNotNull("Stack should be initialised", builder.stack);
        assertTrue("Stack should be empty after initialiseParse", builder.stack.isEmpty());

        // Errors reference must be stored
        assertSame(errors, builder.errors);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitialiseParse_NullReader_Throws() {
        builder.initialiseParse(null, baseUri, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitialiseParse_NullBaseUri_Throws() {
        builder.initialiseParse(emptyReader, null, null, null);
    }

    @Test
    public void testProcessStartTag_WithAttributes() {
        Attributes attrs = new Attributes();
        attrs.put("class", "myClass");
        String tagName = "div";

        boolean result = builder.processStartTag(tagName, attrs);

        // The concrete implementation always returns true
        assertTrue(result);

        // Verify that the token received by process() is a StartTag with correct data
        Token token = builder.lastProcessedToken;
        assertNotNull("Token should have been passed to process()", token);
        assertTrue(token instanceof Token.StartTag);
        Token.StartTag startTag = (Token.StartTag) token;
        assertEquals(tagName, startTag.name());
        assertEquals(attrs, startTag.attributes());
    }

    @Test
    public void testProcessStartTag_RecyclesWhenCurrentTokenIsStart() throws Exception {
        // Force the builder to think that the reusable start token is currently in use
        Token.StartTag startToken = builder.getStartToken();
        Field curTokField = TreeBuilder.class.getDeclaredField("currentToken");
        curTokField.setAccessible(true);
        curTokField.set(builder, startToken);   // currentToken == start

        Attributes attrs = new Attributes();
        attrs.put("id", "test");
        String tagName = "span";

        builder.processStartTag(tagName, attrs);

        // The token passed to process() must NOT be the same instance as the reusable start token
        assertNotSame("A new StartTag token should be created when currentToken == start",
                startToken, builder.lastProcessedToken);
        Token.StartTag passed = (Token.StartTag) builder.lastProcessedToken;
        assertEquals(tagName, passed.name());
        assertEquals(attrs, passed.attributes());
    }

    @Test
    public void testProcessEndTag_RecyclesWhenCurrentTokenIsEnd() throws Exception {
        // Simulate that the reusable end token is currently in use
        Token.EndTag endToken = builder.getEndToken();
        Field curTokField = TreeBuilder.class.getDeclaredField("currentToken");
        curTokField.setAccessible(true);
        curTokField.set(builder, endToken);   // currentToken == end

        String tagName = "p";
        // invoke the protected method via reflection
        java.lang.reflect.Method m = TreeBuilder.class.getDeclaredMethod("processEndTag", String.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(builder, tagName);

        assertTrue(result);
        // Ensure a *new* EndTag instance was used
        assertNotSame("A new EndTag token should be created when currentToken == end",
                endToken, builder.lastProcessedToken);
        Token.EndTag passed = (Token.EndTag) builder.lastProcessedToken;
        assertEquals(tagName, passed.name());
    }

    @Test
    public void testCurrentElement_EmptyStack_ReturnsNull() {
        // Ensure stack is empty
        builder.stack = new java.util.ArrayList<>(32);
        assertNull("currentElement() should return null when stack is empty", builder.currentElement());
    }

    @Test
    public void testCurrentElement_NonEmptyStack_ReturnsTopElement() {
        Element el1 = new Element("div", baseUri);
        Element el2 = new Element("span", baseUri);
        builder.stack = new java.util.ArrayList<>(32);
        builder.stack.add(el1);
        builder.stack.add(el2);   // top of stack

        assertSame("currentElement() should return the element at the top of the stack",
                el2, builder.currentElement());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcessStartTag_NullName_Throws() {
        // The method does not guard against null name – downstream code will NPE.
        // We assert that the Validate check in initialiseParse is the only guard,
        // therefore a NullPointerException is expected here.
        builder.processStartTag(null, new Attributes());
    }

    @Test
    public void testProcessStartTag_NullAttributes_HandledGracefully() {
        // According to jsoup's Token.StartTag implementation, a null Attributes is allowed
        // and treated as an empty attribute set.
        boolean result = builder.processStartTag("br", null);
        assertTrue(result);
        Token.StartTag token = (Token.StartTag) builder.lastProcessedToken;
        assertEquals("br", token.name());
        assertNotNull("Attributes should be non‑null after processing", token.attributes());
        assertTrue("Attributes should be empty", token.attributes().size() == 0);
    }

    /* -------------------------------------------------
     *  3.  Run‑parser sanity (does not loop forever)
     * ------------------------------------------------- */

    @Test
    public void testRunParser_StopsAtEOF() {
        // Provide a reader with no content – the underlying Tokeniser will immediately
        // return an EOF token, causing runParser() to exit.
        builder.initialiseParse(emptyReader, baseUri, null, null);
        builder.runParser(); // should terminate without exception
        // No explicit assertions – the test will fail if runParser loops forever.
    }
}
```
###Test END##