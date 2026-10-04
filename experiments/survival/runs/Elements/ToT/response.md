###Test START##
```java
package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeFilter;
import org.jsoup.select.NodeVisitor;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Elements}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>All public constructors</li>
 *   <li>Typical use‑cases for every method</li>
 *   <li>Edge cases such as empty collections, missing attributes, out‑of‑bounds indexes</li>
 *   <li>Error scenarios (e.g. {@code wrap("")} should throw IllegalArgumentException)</li>
 *   <li>Behaviour of deep‑copy via {@code clone()}</li>
 *   <li>Interaction with the underlying {@link Element} implementation</li>
 * </ul>
 * </p>
 */
public class ElementsTest {

    private Document doc;
    private Elements allDivs;
    private Elements paragraphs;
    private Elements spans;
    private Elements emptyElements;

    @Before
    public void setUp() {
        String html = ""
                + "<html>"
                + " <body>"
                + "   <div id='d1' class='c1'>"
                + "     <p class='p1'>Hello <span id='s1'>World</span></p>"
                + "     <p class='p2'>Foo</p>"
                + "   </div>"
                + "   <div id='d2' class='c2'>"
                + "     <form id='f1'><input name='i1' value='v1'></form>"
                + "   </div>"
                + " </body>"
                + "</html>";
        doc = Jsoup.parse(html, "", Parser.xmlParser());

        allDivs = doc.select("div");
        paragraphs = doc.select("p");
        spans = doc.select("span");
        emptyElements = new Elements(); // empty list for edge tests
    }

    /* ---------------------------------------------------------------------- */
    /* Constructors                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructor() {
        Elements e = new Elements();
        assertTrue(e.isEmpty());
    }

    @Test
    public void testCapacityConstructor() {
        Elements e = new Elements(10);
        assertEquals(0, e.size()); // capacity does not affect size
    }

    @Test
    public void testCollectionConstructor() {
        List<Element> list = Arrays.asList(paragraphs.get(0), paragraphs.get(1));
        Elements e = new Elements(list);
        assertEquals(2, e.size());
        assertSame(list.get(0), e.get(0));
        assertSame(list.get(1), e.get(1));
    }

    @Test
    public void testVarArgsConstructor() {
        Elements e = new Elements(paragraphs.get(0), spans.get(0));
        assertEquals(2, e.size());
        assertSame(paragraphs.get(0), e.get(0));
        assertSame(spans.get(0), e.get(1));
    }

    /* ---------------------------------------------------------------------- */
    /* clone() – deep copy                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCloneDeepCopy() {
        Elements original = new Elements(paragraphs);
        Elements copy = original.clone();

        // Ensure a new list instance
        assertNotSame(original, copy);
        // Ensure each element is a deep clone (different object reference)
        for (int i = 0; i < original.size(); i++) {
            assertNotSame(original.get(i), copy.get(i));
            // but the cloned element should have the same HTML
            assertEquals(original.get(i).outerHtml(), copy.get(i).outerHtml());
        }

        // Modifying the original must not affect the clone
        original.attr("data-modified", "yes");
        assertFalse(copy.hasAttr("data-modified"));
    }

    /* ---------------------------------------------------------------------- */
    /* Attribute handling                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAttrGetFirstMatch() {
        // Only the first <div> has id='d1'
        assertEquals("d1", allDivs.attr("id"));
    }

    @Test
    public void testAttrGetWhenMissingReturnsEmptyString() {
        assertEquals("", allDivs.attr("nonexistent"));
    }

    @Test
    public void testHasAttr() {
        assertTrue(paragraphs.hasAttr("class"));
        assertFalse(paragraphs.hasAttr("nonexistent"));
    }

    @Test
    public void testEachAttrOnlyReturnsExisting() {
        List<String> classes = paragraphs.eachAttr("class");
        assertEquals(2, classes.size());
        assertTrue(classes.contains("p1"));
        assertTrue(classes.contains("p2"));
    }

    @Test
    public void testAttrSetterChainsAndAppliesToAll() {
        Elements modified = paragraphs.attr("data-test", "123");
        assertSame(paragraphs, modified);
        for (Element p : paragraphs) {
            assertEquals("123", p.attr("data-test"));
        }
    }

    @Test
    public void testRemoveAttr() {
        paragraphs.attr("data-tmp", "tmp");
        paragraphs.removeAttr("data-tmp");
        for (Element p : paragraphs) {
            assertFalse(p.hasAttr("data-tmp"));
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Class handling                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddClass() {
        Elements e = new Elements(paragraphs.get(0));
        e.addClass("newClass");
        assertTrue(paragraphs.get(0).hasClass("newClass"));
    }

    @Test
    public void testRemoveClass() {
        Elements e = new Elements(paragraphs.get(0));
        e.removeClass("p1");
        assertFalse(paragraphs.get(0).hasClass("p1"));
    }

    @Test
    public void testToggleClassAddsWhenMissing() {
        Elements e = new Elements(paragraphs.get(1));
        e.toggleClass("toggle");
        assertTrue(paragraphs.get(1).hasClass("toggle"));
    }

    @Test
    public void testToggleClassRemovesWhenPresent() {
        Elements e = new Elements(paragraphs.get(0));
        e.toggleClass("p1");
        assertFalse(paragraphs.get(0).hasClass("p1"));
    }

    @Test
    public void testHasClass() {
        assertTrue(paragraphs.hasClass("p1"));
        assertFalse(paragraphs.hasClass("nonexistent"));
    }

    /* ---------------------------------------------------------------------- */
    /* Form value handling                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testValGetterAndSetter() {
        Elements inputs = doc.select("input");
        assertEquals("v1", inputs.val()); // first input value
        inputs.val("newVal");
        for (Element in : inputs) {
            assertEquals("newVal", in.val());
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Text handling                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTextCombined() {
        // All paragraphs combined with a space separator
        assertEquals("Hello World Foo", paragraphs.text());
    }

    @Test
    public void testHasText() {
        assertTrue(paragraphs.hasText());
        assertFalse(emptyElements.hasText());
    }

    @Test
    public void testEachTextSkipsEmpty() {
        // Create an element with no text
        Element empty = new Element(Tag.valueOf("p"), "");
        Elements mixed = new Elements(paragraphs.get(0), empty);
        List<String> texts = mixed.eachText();
        assertEquals(1, texts.size());
        assertEquals("Hello World", texts.get(0));
    }

    /* ---------------------------------------------------------------------- */
    /* HTML handling                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHtmlAndOuterHtml() {
        String inner = paragraphs.get(0).html(); // "Hello <span id=\"s1\">World</span>"
        String outer = paragraphs.get(0).outerHtml(); // "<p class=\"p1\">Hello <span id=\"s1\">World</span></p>"
        assertEquals(inner, paragraphs.html());
        assertEquals(outer, paragraphs.outerHtml());

        // Multiple elements should be newline‑separated
        String combinedOuter = paragraphs.outerHtml();
        assertTrue(combinedOuter.contains("\n"));
    }

    @Test
    public void testToStringIsAliasForOuterHtml() {
        assertEquals(paragraphs.outerHtml(), paragraphs.toString());
    }

    @Test
    public void testTagNameChange() {
        paragraphs.tagName("section");
        for (Element el : paragraphs) {
            assertEquals("section", el.tagName());
        }
    }

    @Test
    public void testHtmlSetterReplacesInnerHtml() {
        paragraphs.html("<b>bold</b>");
        for (Element p : paragraphs) {
            assertEquals("<b>bold</b>", p.html());
        }
    }

    @Test
    public void testPrependAndAppend() {
        paragraphs.prepend("<i>pre</i>").append("<i>post</i>");
        for (Element p : paragraphs) {
            String html = p.html();
            assertTrue(html.startsWith("<i>pre</i>"));
            assertTrue(html.endsWith("<i>post</i>"));
        }
    }

    @Test
    public void testBeforeAndAfter() {
        // Before and after affect the DOM, not just inner HTML
        Elements before = new Elements(paragraphs.get(0));
        before.before("<hr id='hrBefore'/>");
        assertNotNull(doc.selectFirst("#hrBefore"));

        Elements after = new Elements(paragraphs.get(0));
        after.after("<hr id='hrAfter'/>");
        assertNotNull(doc.selectFirst("#hrAfter"));
    }

    @Test
    public void testWrapValidHtml() {
        spans.wrap("<em class='wrapper'></em>");
        assertEquals("em", spans.get(0).parent().tagName());
        assertTrue(spans.get(0).parent().hasClass("wrapper"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrapEmptyHtmlThrows() {
        spans.wrap("");
    }

    @Test
    public void testUnwrapKeepsChildren() {
        // <p> contains <span id='s1'>World</span>
        Element p = paragraphs.get(0);
        p.unwrap(); // removes <p> but keeps its children under <div id='d1'>
        assertNull(doc.selectFirst("p"));
        assertNotNull(doc.selectFirst("span#s1"));
        // The span should now be a direct child of the div
        assertEquals("div", doc.selectFirst("span#s1").parent().tagName());
    }

    @Test
    public void testEmptyRemovesAllChildren() {
        Element div = allDivs.get(0);
        div.empty();
        assertEquals("", div.html());
    }

    @Test
    public void testRemoveDeletesElementsFromDom() {
        Element p = paragraphs.get(1);
        p.remove();
        assertNull(doc.selectFirst("p.p2"));
    }

    /* ---------------------------------------------------------------------- */
    /* Filtering & selection                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSelectWithinElements() {
        Elements divChildren = allDivs.select("p");
        assertEquals(3, divChildren.size()); // two <p> in first div, none in second
    }

    @Test
    public void testNotFiltersOutMatchingElements() {
        Elements divs = doc.select("div");
        Elements filtered = divs.not(".c1"); // remove div with class c1
        assertEquals(1, filtered.size());
        assertEquals("d2", filtered.attr("id"));
    }

    @Test
    public void testEqValidIndex() {
        Elements secondDiv = allDivs.eq(1);
        assertEquals(1, secondDiv.size());
        assertEquals("d2", secondDiv.attr("id"));
    }

    @Test
    public void testEqOutOfBoundsReturnsEmpty() {
        Elements none = allDivs.eq(5);
        assertTrue(none.isEmpty());
    }

    @Test
    public void testIsMatchesAtLeastOneElement() {
        assertTrue(paragraphs.is("p.p1"));
        assertFalse(paragraphs.is(".nonexistent"));
    }

    /* ---------------------------------------------------------------------- */
    /* Sibling navigation                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNextAndPrevSiblings() {
        Element firstP = paragraphs.get(0);
        Elements next = new Elements(firstP).next();
        assertEquals(1, next.size());
        assertEquals(paragraphs.get(1), next.get(0));

        Elements prev = new Elements(paragraphs.get(1)).prev();
        assertEquals(1, prev.size());
        assertEquals(paragraphs.get(0), prev.get(0));
    }

    @Test
    public void testNextAllWithFilter() {
        Elements allSiblings = new Elements(paragraphs.get(0)).nextAll("p.p2");
        assertEquals(1, allSiblings.size());
        assertEquals(paragraphs.get(1), allSiblings.get(0));
    }

    @Test
    public void testPrevAllWithoutFilter() {
        Elements prevAll = new Elements(paragraphs.get(1)).prevAll();
        assertEquals(1, prevAll.size());
        assertEquals(paragraphs.get(0), prevAll.get(0));
    }

    /* ---------------------------------------------------------------------- */
    /* Ancestor handling                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testParentsReturnsUniqueAncestors() {
        Elements spanParents = spans.parents();
        // <span> -> <p> -> <div> -> <body> -> <html>
        Set<String> expectedTags = new LinkedHashSet<>(Arrays.asList("p", "div", "body", "html"));
        List<String> actualTags = new ArrayList<>();
        for (Element e : spanParents) {
            actualTags.add(e.tagName());
        }
        // Order is preserved (LinkedHashSet) and duplicates removed
        assertEquals(new ArrayList<>(expectedTags), actualTags);
    }

    /* ---------------------------------------------------------------------- */
    /* List‑like helpers                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFirstAndLast() {
        assertSame(paragraphs.get(0), paragraphs.first());
        assertSame(paragraphs.get(paragraphs.size() - 1), paragraphs.last());
        assertNull(emptyElements.first());
        assertNull(emptyElements.last());
    }

    /* ---------------------------------------------------------------------- */
    /* Traversal & filtering                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTraverseCollectsAllNodes() {
        final List<Node> visited = new ArrayList<>();
        NodeVisitor collector = new NodeVisitor() {
            @Override public void head(Node node, int depth) { visited.add(node); }
            @Override public void tail(Node node, int depth) { /* ignore */ }
        };
        paragraphs.traverse(collector);
        // Should contain each paragraph and its descendant text nodes
        assertTrue(visited.contains(paragraphs.get(0)));
        assertTrue(visited.contains(paragraphs.get(1)));
    }

    @Test
    public void testFilterKeepsOnlyAcceptedNodes() {
        // Remove all <span> elements via a filter that returns false for span tags
        NodeFilter filter = new NodeFilter() {
            @Override public FilterResult head(Node node, int depth) {
                return node instanceof Element && ((Element) node).tagName().equals("span")
                        ? FilterResult.REMOVE : FilterResult.CONTINUE;
            }
            @Override public FilterResult tail(Node node, int depth) { return FilterResult.CONTINUE; }
        };
        // Apply filter on the whole document
        Elements all = doc.select("*");
        all.filter(filter);
        assertNull(doc.selectFirst("span"));
    }

    /* ---------------------------------------------------------------------- */
    /* Form extraction                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFormsExtraction() {
        Elements forms = doc.select("form");
        List<FormElement> extracted = forms.forms();
        assertEquals(1, extracted.size());
        assertEquals("f1", extracted.get(0).id());
    }

    /* ---------------------------------------------------------------------- */
    /* Additional edge cases                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAttrOnEmptyElementsReturnsEmptyString() {
        assertEquals("", emptyElements.attr("any"));
    }

    @Test
    public void testHasAttrOnEmptyElementsReturnsFalse() {
        assertFalse(emptyElements.hasAttr("any"));
    }

    @Test
    public void testEachAttrOnEmptyElementsReturnsEmptyList() {
        assertTrue(emptyElements.eachAttr("any").isEmpty());
    }

    @Test
    public void testHasClassOnEmptyElementsReturnsFalse() {
        assertFalse(emptyElements.hasClass("any"));
    }

    @Test
    public void testValOnEmptyElementsReturnsEmptyString() {
        assertEquals("", emptyElements.val());
    }

    @Test
    public void testTextOnEmptyElementsReturnsEmptyString() {
        assertEquals("", emptyElements.text());
    }

    @Test
    public void testHtmlOnEmptyElementsReturnsEmptyString() {
        assertEquals("", emptyElements.html());
    }

    @Test
    public void testOuterHtmlOnEmptyElementsReturnsEmptyString() {
        assertEquals("", emptyElements.outerHtml());
    }
}
```
###Test END##