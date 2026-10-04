###Test START##
```java
package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Evaluator.Tag;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 tests for {@link Selector}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical usage of each public method.</li>
 *   <li>Edge‑cases such as empty inputs, {@code null}s and duplicate elements.</li>
 *   <li>Correct exception types and messages for invalid arguments.</li>
 *   <li>Behaviour of the package‑private {@code filterOut} helper.</li>
 * </ul>
 */
public class SelectorTest {

    /** Helper that builds a small document used by many tests. */
    private Document sampleDoc() {
        // language=HTML
        String html = "<!doctype html>\n" +
                "<html>\n" +
                " <head><title>Test</title></head>\n" +
                " <body>\n" +
                "   <div id='root'>\n" +
                "     <p class='para'>First</p>\n" +
                "     <p class='para'>Second</p>\n" +
                "     <span class='note'>Note</span>\n" +
                "   </div>\n" +
                "   <div class='other'>\n" +
                "     <p>Other para</p>\n" +
                "   </div>\n" +
                " </body>\n" +
                "</html>";
        return Jsoup.parse(html);
    }

    /* --------------------------------------------------------------------- */
    /* select(String, Element)                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void select_StringAndElement_TypicalQuery() {
        Document doc = sampleDoc();
        Element root = doc.selectFirst("div#root");
        Elements result = Selector.select("p.para", root);
        assertEquals(2, result.size());
        for (Element el : result) {
            assertEquals("p", el.tagName());
            assertTrue(el.hasClass("para"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_StringAndElement_EmptyQuery_Throws() {
        Document doc = sampleDoc();
        Selector.select("", doc);
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_StringAndElement_NullRoot_Throws() {
        Selector.select("p", null);
    }

    @Test
    public void select_StringAndElement_InvalidCss_ThrowsSelectorParseException() {
        Document doc = sampleDoc();
        try {
            Selector.select("???", doc);
            fail("Expected SelectorParseException");
        } catch (Selector.SelectorParseException e) {
            assertTrue(e.getMessage().contains("???"));
        }
    }

    /* --------------------------------------------------------------------- */
    /* select(Evaluator, Element)                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void select_EvaluatorAndElement_Typical() {
        Document doc = sampleDoc();
        Element root = doc.body();
        Evaluator evaluator = new Tag("p"); // matches all <p> elements
        Elements result = Selector.select(evaluator, root);
        // There are three <p> elements in the sample doc
        assertEquals(3, result.size());
        for (Element el : result) {
            assertEquals("p", el.tagName());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_EvaluatorAndElement_NullEvaluator_Throws() {
        Document doc = sampleDoc();
        Selector.select((Evaluator) null, doc.body());
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_EvaluatorAndElement_NullRoot_Throws() {
        Evaluator evaluator = new Tag("div");
        Selector.select(evaluator, null);
    }

    /* --------------------------------------------------------------------- */
    /* select(String, Iterable<Element>)                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void select_StringAndIterable_MultipleRoots_Deduplication() {
        Document doc = sampleDoc();
        Element root1 = doc.selectFirst("div#root");
        Element root2 = doc.selectFirst("div.other");

        // Both roots contain a <p> element with text "Other para" and "First", etc.
        // Selecting "p" from both roots yields 4 elements, but the <p> inside root1
        // appears only once, and the <p> inside root2 is distinct.
        Elements result = Selector.select("p", Arrays.asList(root1, root2));
        // Expected: 4 <p> elements (2 in root1, 1 in root2, plus the <p> inside root2)
        // Actually root2 has 1 <p>, root1 has 2 <p>, total 3 distinct <p>.
        assertEquals(3, result.size());

        // Ensure deduplication works when the same element appears via two roots
        // (e.g., using the document body as both roots)
        Elements dedup = Selector.select("p", Arrays.asList(doc.body(), doc.body()));
        assertEquals(3, dedup.size()); // still only three distinct <p>
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_StringAndIterable_EmptyQuery_Throws() {
        Document doc = sampleDoc();
        Selector.select("", Collections.singletonList(doc.body()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_StringAndIterable_NullRoots_Throws() {
        Selector.select("p", null);
    }

    /* --------------------------------------------------------------------- */
    /* filterOut (package‑private)                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void filterOut_RemovesElementsMatchingEquals() {
        Document doc = sampleDoc();
        Element p1 = doc.selectFirst("p:contains(First)");
        Element p2 = doc.selectFirst("p:contains(Second)");
        Element p3 = doc.selectFirst("p:contains(Other)");

        List<Element> source = Arrays.asList(p1, p2, p3);
        List<Element> toRemove = Arrays.asList(p2, doc.selectFirst("span.note")); // span is not in source

        Elements filtered = Selector.filterOut(source, toRemove);
        assertEquals(2, filtered.size());
        assertTrue(filtered.contains(p1));
        assertTrue(filtered.contains(p3));
        assertFalse(filtered.contains(p2));
    }

    @Test
    public void filterOut_EmptyCollections_ReturnsEmpty() {
        Elements filtered = Selector.filterOut(Collections.emptyList(), Collections.emptyList());
        assertTrue(filtered.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* selectFirst(String, Element)                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void selectFirst_Found_ReturnsFirstMatch() {
        Document doc = sampleDoc();
        Element root = doc.body();
        Element firstPara = Selector.selectFirst("p.para", root);
        assertNotNull(firstPara);
        assertEquals("First", firstPara.text()); // The first <p class='para'> contains "First"
    }

    @Test
    public void selectFirst_NotFound_ReturnsNull() {
        Document doc = sampleDoc();
        Element root = doc.body();
        Element result = Selector.selectFirst("ul.nonexistent", root);
        assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void selectFirst_EmptyQuery_Throws() {
        Document doc = sampleDoc();
        Selector.selectFirst("", doc.body());
    }

    /* --------------------------------------------------------------------- */
    /* SelectorParseException behavior                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void selectorParseException_MessageFormatting() {
        String badQuery = "div>>p";
        try {
            Selector.select(badQuery, sampleDoc().body());
            fail("Expected SelectorParseException");
        } catch (Selector.SelectorParseException e) {
            // The message should contain the original selector string
            assertTrue(e.getMessage().contains(badQuery));
        }
    }

    /* --------------------------------------------------------------------- */
    /* Additional edge case: using a custom Evaluator that throws           */
    /* --------------------------------------------------------------------- */

    private static class ThrowingEvaluator implements Evaluator {
        @Override
        public boolean matches(Element root, Element element) {
            throw new RuntimeException("Evaluator failure");
        }
    }

    @Test(expected = RuntimeException.class)
    public void select_EvaluatorThrows_PropagatesException() {
        Document doc = sampleDoc();
        Selector.select(new ThrowingEvaluator(), doc.body());
    }

    /* --------------------------------------------------------------------- */
    /* Verify that filterOut uses equals (not identity)                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void filterOut_UsesEquals_NotIdentity() {
        // Create two distinct Element objects that are equal by value (same tag, attributes, etc.)
        Element a = new Element("div");
        a.attr("id", "same");
        Element b = new Element("div");
        b.attr("id", "same");

        assertNotSame(a, b);
        assertEquals(a, b); // jsoup Element overrides equals to compare tag+attributes+children

        Elements filtered = Selector.filterOut(Arrays.asList(a), Arrays.asList(b));
        // Because equals returns true, b should be considered "out" and a removed
        assertTrue(filtered.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* Verify that identity deduplication works in select(String, Iterable)  */
    /* --------------------------------------------------------------------- */

    @Test
    public void select_Iterable_DedupeByIdentity_NotEquals() {
        Document doc = sampleDoc();
        // Two elements with identical markup but different instances
        Element clone1 = doc.selectFirst("p.para").clone();
        Element clone2 = doc.selectFirst("p.para").clone();

        // They are equal but not the same identity
        assertEquals(clone1, clone2);
        assertNotSame(clone1, clone2);

        // Pass both as roots; each root will find its own <p> element, but the result
        // list should contain both because deduplication is by identity.
        Elements result = Selector.select("p", Arrays.asList(clone1, clone2));
        assertEquals(2, result.size());
    }
}
```
###Test END##