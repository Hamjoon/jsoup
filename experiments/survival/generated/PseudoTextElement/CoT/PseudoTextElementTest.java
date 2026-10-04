package org.jsoup.nodes;

import org.jsoup.parser.Tag;
import org.jsoup.parser.Parser;
import org.jsoup.nodes.Document.OutputSettings;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class PseudoTextElementTest {

    private Tag tag;
    private String baseUri;
    private Attributes attributes;
    private PseudoTextElement pseudo;

    @Before
    public void setUp() {
        tag = Tag.valueOf("p");               // a simple HTML tag
        baseUri = "http://example.com/";
        attributes = new Attributes();
        attributes.put("id", "test");
        pseudo = new PseudoTextElement(tag, baseUri, attributes);
    }

    /*** 1. Constructor – basic behaviour ******************************************/
    @Test
    public void testConstructorInitialisesFields() {
        // tag
        assertEquals(tag, pseudo.tag());
        // baseUri
        assertEquals(baseUri, pseudo.baseUri());
        // attributes
        assertEquals("test", pseudo.attr("id"));
        assertTrue(pseudo.hasAttr("id"));
    }

    /*** 2. Inherited methods – sanity checks ************************************/
    @Test
    public void testInheritedAttrWorks() {
        pseudo.attr("data-foo", "bar");
        assertEquals("bar", pseudo.attr("data-foo"));
    }

    @Test
    public void testInheritedTextWorks() {
        pseudo.appendText("Hello");
        pseudo.appendText(" World");
        assertEquals("Hello World", pseudo.text());
    }

    /*** 3. outerHtmlHead – basic – does nothing ******************************/
    @Test
    public void testOuterHtmlHeadDoesNothing() throws Exception {
        StringBuilder accum = new StringBuilder();
        OutputSettings out = new OutputSettings();

        // invoke package‑private method via reflection (same package → accessible)
        Method m = PseudoTextElement.class.getDeclaredMethod(
                "outerHtmlHead", Appendable.class, int.class, OutputSettings.class);
        m.setAccessible(true);
        m.invoke(pseudo, accum, 0, out);

        assertEquals("outerHtmlHead should not modify the Appendable", "", accum.toString());
    }

    /*** 4. outerHtmlTail – basic – does nothing ******************************/
    @Test
    public void testOuterHtmlTailDoesNothing() throws Exception {
        StringBuilder accum = new StringBuilder("already");
        OutputSettings out = new OutputSettings();

        Method m = PseudoTextElement.class.getDeclaredMethod(
                "outerHtmlTail", Appendable.class, int.class, OutputSettings.class);
        m.setAccessible(true);
        m.invoke(pseudo, accum, 5, out);

        assertEquals("outerHtmlTail should not modify the Appendable", "already", accum.toString());
    }

    /*** 5. Edge cases – null Appendable ******************************************/
    @Test
    public void testOuterHtmlHeadWithNullAppendable() throws Exception {
        OutputSettings out = new OutputSettings();

        Method m = PseudoTextElement.class.getDeclaredMethod(
                "outerHtmlHead", Appendable.class, int.class, OutputSettings.class);
        m.setAccessible(true);
        // No exception should be thrown even though Appendable is null
        m.invoke(pseudo, null, 0, out);
    }

    @Test
    public void testOuterHtmlTailWithNullAppendable() throws Exception {
        OutputSettings out = new OutputSettings();

        Method m = PseudoTextElement.class.getDeclaredMethod(
                "outerHtmlTail", Appendable.class, int.class, OutputSettings.class);
        m.setAccessible(true);
        m.invoke(pseudo, null, 0, out);
    }

    /*** 6. Edge cases – null OutputSettings **************************************/
    @Test
    public void testOuterHtmlHeadWithNullOutputSettings() throws Exception {
        StringBuilder accum = new StringBuilder();
        Method m = PseudoTextElement.class.getDeclaredMethod(
                "outerHtmlHead", Appendable.class, int.class, OutputSettings.class);
        m.setAccessible(true);
        m.invoke(pseudo, accum, 0, null);
        assertEquals("", accum.toString());
    }

    @Test
    public void testOuterHtmlTailWithNullOutputSettings() throws Exception {
        StringBuilder accum = new StringBuilder();
        Method m = PseudoTextElement.class.getDeclaredMethod(
                "outerHtmlTail", Appendable.class, int.class, OutputSettings.class);
        m.setAccessible(true);
        m.invoke(pseudo, accum, 0, null);
        assertEquals("", accum.toString());
    }

    /*** 7. Edge case – negative depth ********************************************/
    @Test
    public void testOuterHtmlHeadWithNegativeDepth() throws Exception {
        StringBuilder accum = new StringBuilder();
        OutputSettings out = new OutputSettings();

        Method m = PseudoTextElement.class.getDeclaredMethod(
                "outerHtmlHead", Appendable.class, int.class, OutputSettings.class);
        m.setAccessible(true);
        m.invoke(pseudo, accum, -10, out);
        assertEquals("", accum.toString());
    }

    @Test
    public void testOuterHtmlTailWithNegativeDepth() throws Exception {
        StringBuilder accum = new StringBuilder();
        OutputSettings out = new OutputSettings();

        Method m = PseudoTextElement.class.getDeclaredMethod(
                "outerHtmlTail", Appendable.class, int.class, OutputSettings.class);
        m.setAccessible(true);
        m.invoke(pseudo, accum, -3, out);
        assertEquals("", accum.toString());
    }

    /*** 8. Constructor validation – null Tag ************************************/
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullTagThrows() {
        new PseudoTextElement(null, baseUri, attributes);
    }

    /*** 9. Constructor validation – null Attributes ******************************/
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullAttributesThrows() {
        new PseudoTextElement(tag, baseUri, null);
    }

    /*** 10. Large amount of text does not affect outerHtml* methods **************/
    @Test
    public void testLargeTextDoesNotAffectOuterHtmlMethods() throws Exception {
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 10_000; i++) {
            huge.append("x");
        }
        pseudo.appendText(huge.toString());

        // invoke both methods – they must still leave the Appendable untouched
        StringBuilder accumHead = new StringBuilder();
        StringBuilder accumTail = new StringBuilder();
        OutputSettings out = new OutputSettings();

        Method head = PseudoTextElement.class.getDeclaredMethod(
                "outerHtmlHead", Appendable.class, int.class, OutputSettings.class);
        Method tail = PseudoTextElement.class.getDeclaredMethod(
                "outerHtmlTail", Appendable.class, int.class, OutputSettings.class);
        head.setAccessible(true);
        tail.setAccessible(true);

        head.invoke(pseudo, accumHead, 0, out);
        tail.invoke(pseudo, accumTail, 0, out);

        assertEquals("", accumHead.toString());
        assertEquals("", accumTail.toString());
        // sanity check that the text was really stored
        assertEquals(huge.toString(), pseudo.text());
    }
}
