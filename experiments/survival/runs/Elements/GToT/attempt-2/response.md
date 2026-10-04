**Step 1 – Public API of `Elements`**

| # | Method (including constructors) | Signature |
|---|--------------------------------|-----------|
| 1 | `Elements()` | `public Elements()` |
| 2 | `Elements(int)` | `public Elements(int initialCapacity)` |
| 3 | `Elements(Collection<Element>)` | `public Elements(Collection<Element> elements)` |
| 4 | `Elements(List<Element>)` | `public Elements(List<Element> elements)` |
| 5 | `Elements(Element…)` | `public Elements(Element... elements)` |
| 6 | `clone()` | `public Elements clone()` |
| 7 | `attr(String)` | `public String attr(String attributeKey)` |
| 8 | `hasAttr(String)` | `public boolean hasAttr(String attributeKey)` |
| 9 | `eachAttr(String)` | `public List<String> eachAttr(String attributeKey)` |
|10| `attr(String, String)` | `public Elements attr(String attributeKey, String attributeValue)` |
|11| `removeAttr(String)` | `public Elements removeAttr(String attributeKey)` |
|12| `addClass(String)` | `public Elements addClass(String className)` |
|13| `removeClass(String)` | `public Elements removeClass(String className)` |
|14| `toggleClass(String)` | `public Elements toggleClass(String className)` |
|15| `hasClass(String)` | `public boolean hasClass(String className)` |
|16| `val()` | `public String val()` |
|17| `val(String)` | `public Elements val(String value)` |
|18| `text()` | `public String text()` |
|19| `hasText()` | `public boolean hasText()` |
|20| `eachText()` | `public List<String> eachText()` |
|21| `html()` | `public String html()` |
|22| `outerHtml()` | `public String outerHtml()` |
|23| `toString()` | `public String toString()` |
|24| `tagName(String)` | `public Elements tagName(String tagName)` |
|25| `html(String)` | `public Elements html(String html)` |
|26| `prepend(String)` | `public Elements prepend(String html)` |
|27| `append(String)` | `public Elements append(String html)` |
|28| `before(String)` | `public Elements before(String html)` |
|29| `after(String)` | `public Elements after(String html)` |
|30| `wrap(String)` | `public Elements wrap(String html)` |
|31| `unwrap()` | `public Elements unwrap()` |
|32| `empty()` | `public Elements empty()` |
|33| `remove()` | `public Elements remove()` |
|34| `select(String)` | `public Elements select(String query)` |
|35| `not(String)` | `public Elements not(String query)` |
|36| `eq(int)` | `public Elements eq(int index)` |
|37| `is(String)` | `public boolean is(String query)` |
|38| `next()` | `public Elements next()` |
|39| `next(String)` | `public Elements next(String query)` |
|40| `nextAll()` | `public Elements nextAll()` |
|41| `nextAll(String)` | `public Elements nextAll(String query)` |
|42| `prev()` | `public Elements prev()` |
|43| `prev(String)` | `public Elements prev(String query)` |
|44| `prevAll()` | `public Elements prevAll()` |
|45| `prevAll(String)` | `public Elements prevAll(String query)` |
|46| `parents()` | `public Elements parents()` |
|47| `first()` | `public Element first()` |
|48| `last()` | `public Element last()` |
|49| `traverse(NodeVisitor)` | `public Elements traverse(NodeVisitor nodeVisitor)` |
|50| `filter(NodeFilter)` | `public Elements filter(NodeFilter nodeFilter)` |
|51| `forms()` | `public List<FormElement> forms()` |

---

**Step 2 – Basic JUnit 4 test ideas (one per method)**  
*Only a single representative test is shown for each method; the full test suite (step 5) expands these with edge‑case checks.*

| # | Test name | What it verifies |
|---|-----------|-------------------|
| 1 | `testConstructorEmpty` | `new Elements()` creates an empty list |
| 2 | `testConstructorWithCapacity` | Capacity constructor does not throw and list is empty |
| 3 | `testConstructorFromCollection` | Elements built from a collection contain the same elements |
| 4 | `testConstructorFromArray` | Var‑args constructor stores all supplied elements |
| 5 | `testCloneDeepCopy` | `clone()` returns a new list whose elements are deep‑cloned |
| 6 | `testAttrReturnsFirstMatch` | `attr("id")` returns the attribute of the first element that has it |
| 7 | `testHasAttr` | `hasAttr("class")` is true when any element has the attribute |
| 8 | `testEachAttr` | `eachAttr("href")` returns a list of all href values, skipping elements without it |
| 9 | `testAttrSetAll` | `attr(key,value)` sets the attribute on **all** elements |
|10 | `testRemoveAttr` | `removeAttr` deletes the attribute from every element |
|11 | `testAddClass` | `addClass` adds a CSS class to each element |
|12 | `testRemoveClass` | `removeClass` removes a CSS class from each element |
|13 | `testToggleClassAdds` | `toggleClass` adds the class when it is absent |
|14 | `testToggleClassRemoves` | `toggleClass` removes the class when it is present |
|15 | `testHasClass` | `hasClass` reports true if any element contains the class |
|16 | `testValGet` | `val()` returns the value of the first form element |
|17 | `testValSet` | `val(String)` sets the same value on all form elements |
|18 | `testTextCombined` | `text()` concatenates texts of all elements separated by a space |
|19 | `testHasText` | `hasText()` is true when at least one element has non‑blank text |
|20 | `testEachText` | `eachText()` returns texts only for elements that have non‑blank text |
|21 | `testHtmlCombined` | `html()` joins inner HTML of each element with newline |
|22 | `testOuterHtmlCombined` | `outerHtml()` joins outer HTML of each element with newline |
|23 | `testToStringAliasesOuterHtml` | `toString()` equals `outerHtml()` |
|24 | `testTagNameChange` | `tagName("span")` changes the tag name of each element |
|25 | `testHtmlSet` | `html(String)` replaces inner HTML of each element |
|26 | `testPrepend` | `prepend("<b>")` inserts HTML at the beginning of each element |
|27 | `testAppend` | `append("<i>")` inserts HTML at the end of each element |
|28 | `testBefore` | `before("<hr>")` inserts a sibling before each element |
|29 | `testAfter` | `after("<hr>")` inserts a sibling after each element |
|30 | `testWrap` | `wrap("<div class='wrapper'></div>")` wraps each element |
|31 | `testUnwrap` | `unwrap()` removes the element but keeps its children |
|32 | `testEmpty` | `empty()` removes all child nodes of each element |
|33 | `testRemove` | `remove()` detaches each element from the DOM |
|34 | `testSelect` | `select("p")` returns only `<p>` elements |
|35 | `testNot` | `not(".skip")` removes matching elements from the list |
|36 | `testEqValid` | `eq(1)` returns a list containing only the element at index 1 |
|37 | `testEqOutOfBounds` | `eq(99)` returns an empty list |
|38 | `testIsTrue` | `is("div")` is true when any element matches the selector |
|39 | `testIsFalse` | `is(".nonexistent")` is false |
|40 | `testNextSibling` | `next()` returns immediate following sibling elements |
|41 | `testPrevSiblingFiltered` | `prev("div")` returns previous siblings that match the filter |
|42 | `testParentsDeduplication` | `parents()` returns a set‑like collection without duplicates |
|43 | `testFirstAndLast` | `first()`/`last()` return correct elements or `null` when empty |
|44 | `testTraverseCallsVisitor` | `traverse` invokes the supplied `NodeVisitor` on each node |
|45 | `testFilterCallsNodeFilter` | `filter` invokes the supplied `NodeFilter` on each node |
|46 | `testFormsExtraction` | `forms()` returns only elements that are instances of `FormElement` |

---

**Step 3 – Edge‑case & Exception Scenarios**

| Method | Edge case / Exception | Test name |
|--------|-----------------------|-----------|
| `attr(String)` | `null` key → `NullPointerException` from underlying `Element` | `testAttrNullKey` |
| `hasAttr(String)` | Empty list → returns `false` | `testHasAttrEmptyList` |
| `eachAttr(String)` | No element has the attribute → returns empty list | `testEachAttrNone` |
| `attr(String, String)` | `null` value → attribute set to `"null"` (Jsoup behaviour) – verify no NPE | `testAttrSetNullValue` |
| `addClass/removeClass/toggleClass` | `null` class name → `IllegalArgumentException` (Jsoup validates) | `testAddClassNull` |
| `wrap(String)` | Empty string or `null` → `IllegalArgumentException` from `Validate.notEmpty` | `testWrapEmptyString` |
| `val()` | Empty Elements → returns empty string | `testValEmpty` |
| `val(String)` | `null` value → sets `null` as value (Jsoup stores `"null"`), verify behaviour | `testValSetNull` |
| `text()` | Elements with only whitespace → whitespace is kept in concatenation (but `hasText` returns false) | `testTextWhitespaceOnly` |
| `html()` / `outerHtml()` | Empty Elements → returns empty string | `testHtmlEmpty` |
| `eq(int)` | Negative index → returns empty list | `testEqNegative` |
| `is(String)` | Invalid CSS query → throws `Selector.SelectorParseException` | `testIsInvalidQuery` |
| `next(String)` / `prev(String)` | Query that matches none → returns empty list | `testNextFilteredNone` |
| `parents()` | Elements with common ancestors → duplicates must be removed | `testParentsNoDuplicates` |
| `traverse(NodeVisitor)` | Visitor throws RuntimeException → propagated | `testTraverseExceptionPropagation` |
| `filter(NodeFilter)` | Filter throws RuntimeException → propagated | `testFilterExceptionPropagation` |
| `forms()` | No form elements → returns empty list | `testFormsEmpty` |

---

**Step 4 – JUnit 4 Tests for Edge Cases & Exceptions**  
(Only the additional edge‑case tests are shown; the basic tests from step 2 are also included in the final file.)

```java
@Test(expected = NullPointerException.class)
public void testAttrNullKey() {
    Elements els = new Elements(new Element(Tag.valueOf("div"), ""));
    els.attr(null);
}

@Test
public void testHasAttrEmptyList() {
    Elements empty = new Elements();
    assertFalse(empty.hasAttr("class"));
}

@Test
public void testEachAttrNone() {
    Elements els = new Elements(
            new Element(Tag.valueOf("a"), "").attr("href", "http://example.com"),
            new Element(Tag.valueOf("span"), "")
    );
    List<String> hrefs = els.eachAttr("title"); // no title attribute anywhere
    assertTrue(hrefs.isEmpty());
}

@Test
public void testAddClassNull() {
    Elements els = new Elements(new Element(Tag.valueOf("p"), ""));
    try {
        els.addClass(null);
        fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
        // expected
    }
}

@Test(expected = IllegalArgumentException.class)
public void testWrapEmptyString() {
    Elements els = new Elements(new Element(Tag.valueOf("b"), ""));
    els.wrap("");
}
```

*(Similar edge‑case tests are added for the remaining rows in the table above.)*

---

**Step 5 – Complete JUnit 4 Test File (`ElementsTest.java`)**

```java
###Test START##
package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.jsoup.select.NodeFilter;
import org.jsoup.select.Selector;
import org.jsoup.select.Evaluator;
import org.jsoup.select.QueryParser;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ElementsTest {

    /* -------------------------------------------------------------------- *
     *  Helper methods to build a small DOM used by many tests               *
     * -------------------------------------------------------------------- */
    private Document createTestDocument() {
        String html = ""
                + "<html><head><title>Test</title></head>"
                + "<body>"
                + "  <div id='div1' class='c1'>"
                + "    <p id='p1'>Hello <b>World</b></p>"
                + "    <p id='p2' class='c2'>   </p>"
                + "    <a id='a1' href='http://example.com'>link</a>"
                + "  </div>"
                + "  <div id='div2' class='c3'>"
                + "    <span id='s1'>Span text</span>"
                + "  </div>"
                + "  <form id='form1'>"
                + "    <input name='q' value='initial'/>"
                + "  </form>"
                + "</body></html>";
        return Jsoup.parse(html);
    }

    private Elements select(String css) {
        return createTestDocument().select(css);
    }

    /* -------------------------------------------------------------------- *
     *  1. Constructors                                                     *
     * -------------------------------------------------------------------- */
    @Test
    public void testConstructorEmpty() {
        Elements els = new Elements();
        assertTrue(els.isEmpty());
    }

    @Test
    public void testConstructorWithCapacity() {
        Elements els = new Elements(10);
        assertTrue(els.isEmpty());
    }

    @Test
    public void testConstructorFromCollection() {
        List<Element> list = Arrays.asList(new Element(Tag.valueOf("p"), ""), new Element(Tag.valueOf("div"), ""));
        Elements els = new Elements(list);
        assertEquals(2, els.size());
        assertTrue(els.containsAll(list));
    }

    @Test
    public void testConstructorFromArray() {
        Element e1 = new Element(Tag.valueOf("p"), "");
        Element e2 = new Element(Tag.valueOf("div"), "");
        Elements els = new Elements(e1, e2);
        assertEquals(2, els.size());
        assertTrue(els.contains(e1));
        assertTrue(els.contains(e2));
    }

    @Test
    public void testCloneDeepCopy() {
        Elements original = select("p");
        Elements copy = original.clone();
        assertEquals(original.size(), copy.size());

        // modify original, ensure copy is unaffected (deep clone)
        original.get(0).attr("data-mod", "yes");
        assertFalse(copy.get(0).hasAttr("data-mod"));
    }

    /* -------------------------------------------------------------------- *
     *  2. Attribute handling                                               *
     * -------------------------------------------------------------------- */
    @Test
    public void testAttrReturnsFirstMatch() {
        Elements els = select("div, a");
        String id = els.attr("id");
        // first element with an id attribute is the <div id='div1'>
        assertEquals("div1", id);
    }

    @Test
    public void testHasAttr() {
        Elements els = select("p, a");
        assertTrue(els.hasAttr("id"));
        assertFalse(els.hasAttr("nonexistent"));
    }

    @Test
    public void testEachAttr() {
        Elements els = select("a, p");
        List<String> hrefs = els.eachAttr("href");
        assertEquals(1, hrefs.size());
        assertEquals("http://example.com", hrefs.get(0));
    }

    @Test
    public void testAttrSetAll() {
        Elements els = select("p");
        els.attr("data-test", "value");
        for (Element e : els) {
            assertEquals("value", e.attr("data-test"));
        }
    }

    @Test
    public void testRemoveAttr() {
        Elements els = select("a");
        assertTrue(els.first().hasAttr("href"));
        els.removeAttr("href");
        assertFalse(els.first().hasAttr("href"));
    }

    @Test
    public void testAddClass() {
        Elements els = select("p");
        els.addClass("newClass");
        for (Element e : els) {
            assertTrue(e.hasClass("newClass"));
        }
    }

    @Test
    public void testRemoveClass() {
        Elements els = select("div");
        els.removeClass("c1");
        assertFalse(els.first().hasClass("c1"));
    }

    @Test
    public void testToggleClassAdds() {
        Elements els = select("p");
        els.toggleClass("toggleMe");
        for (Element e : els) {
            assertTrue(e.hasClass("toggleMe"));
        }
    }

    @Test
    public void testToggleClassRemoves() {
        Elements els = select("div.c1");
        assertTrue(els.first().hasClass("c1"));
        els.toggleClass("c1");
        assertFalse(els.first().hasClass("c1"));
    }

    @Test
    public void testHasClass() {
        Elements els = select("div");
        assertTrue(els.hasClass("c1"));
        assertFalse(els.hasClass("nonexistent"));
    }

    /* -------------------------------------------------------------------- *
     *  3. Form value handling                                              *
     * -------------------------------------------------------------------- */
    @Test
    public void testValGet() {
        Elements forms = select("input[name=q]");
        assertEquals("initial", forms.val());
    }

    @Test
    public void testValSet() {
        Elements inputs = select("input");
        inputs.val("newVal");
        for (Element e : inputs) {
            assertEquals("newVal", e.val());
        }
    }

    @Test
    public void testValEmpty() {
        Elements empty = new Elements();
        assertEquals("", empty.val());
    }

    @Test
    public void testValSetNull() {
        Elements inputs = select("input");
        inputs.val(null);
        for (Element e : inputs) {
            // Jsoup stores the string "null"
            assertEquals("null", e.val());
        }
    }

    /* -------------------------------------------------------------------- *
     *  4. Text & HTML aggregation                                          *
     * -------------------------------------------------------------------- */
    @Test
    public void testTextCombined() {
        Elements els = select("p");
        String txt = els.text();
        // "Hello World" (from first <p>) + "   " (second <p>) -> trimmed spaces kept between elements
        assertEquals("Hello World    ", txt);
    }

    @Test
    public void testHasText() {
        Elements els = select("p");
        assertTrue(els.hasText()); // first <p> has text
    }

    @Test
    public void testEachText() {
        Elements els = select("p");
        List<String> texts = els.eachText();
        assertEquals(1, texts.size()); // second <p> is only whitespace
        assertEquals("Hello World", texts.get(0));
    }

    @Test
    public void testHtmlCombined() {
        Elements els = select("p");
        String html = els.html();
        assertTrue(html.contains("<b>World</b>"));
        assertTrue(html.contains("\n"));
    }

    @Test
    public void testOuterHtmlCombined() {
        Elements els = select("p");
        String outer = els.outerHtml();
        assertTrue(outer.startsWith("<p"));
        assertTrue(outer.contains("\n"));
    }

    @Test
    public void testToStringAliasesOuterHtml() {
        Elements els = select("p");
        assertEquals(els.outerHtml(), els.toString());
    }

    @Test
    public void testHtmlSet() {
        Elements els = select("p");
        els.html("<i>italic</i>");
        for (Element e : els) {
            assertEquals("<i>italic</i>", e.html());
        }
    }

    @Test
    public void testPrepend() {
        Elements els = select("p");
        els.prepend("<b>");
        for (Element e : els) {
            assertTrue(e.html().startsWith("<b>"));
        }
    }

    @Test
    public void testAppend() {
        Elements els = select("p");
        els.append("<i>");
        for (Element e : els) {
            assertTrue(e.html().endsWith("<i>"));
        }
    }

    @Test
    public void testBefore() {
        Elements pEls = select("p");
        pEls.before("<hr>");
        // after insertion, a <hr> should appear before each <p>
        Elements hrs = select("hr");
        assertEquals(pEls.size(), hrs.size());
    }

    @Test
    public void testAfter() {
        Elements pEls = select("p");
        pEls.after("<hr>");
        Elements hrs = select("hr");
        assertEquals(pEls.size(), hrs.size());
    }

    @Test
    public void testWrap() {
        Elements els = select("a");
        els.wrap("<div class='wrapper'></div>");
        for (Element e : els) {
            assertEquals("wrapper", e.parent().className());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrapEmptyString() {
        Elements els = select("a");
        els.wrap("");
    }

    @Test
    public void testUnwrap() {
        Elements fontEls = new Elements(createTestDocument().select("p"));
        // add a wrapper <div class='wrap'>
        fontEls.wrap("<div class='wrap'></div>");
        Elements wrapped = createTestDocument().select("div.wrap");
        assertEquals(2, wrapped.size());
        wrapped.unwrap();
        // after unwrap the <div class='wrap'> should be gone but children remain
        assertTrue(createTestDocument().select("div.wrap").isEmpty());
        assertEquals(2, createTestDocument().select("p").size());
    }

    @Test
    public void testEmpty() {
        Elements divs = select("div");
        divs.empty();
        for (Element e : divs) {
            assertEquals("", e.html());
        }
    }

    @Test
    public void testRemove() {
        Elements divs = select("div");
        divs.remove();
        assertTrue(createTestDocument().select("div").isEmpty());
    }

    /* -------------------------------------------------------------------- *
     *  5. Selection & filtering                                            *
     * -------------------------------------------------------------------- */
    @Test
    public void testSelect() {
        Elements all = createTestDocument().select("*");
        Elements onlyPs = all.select("p");
        assertEquals(2, onlyPs.size());
        for (Element e : onlyPs) {
            assertEquals("p", e.tagName());
        }
    }

    @Test
    public void testNot() {
        Elements divs = select("div");
        Elements filtered = divs.not("#div1");
        assertEquals(1, filtered.size());
        assertEquals("div2", filtered.first().id());
    }

    @Test
    public void testEqValid() {
        Elements els = select("p");
        Elements second = els.eq(1);
        assertEquals(1, second.size());
        assertEquals("p2", second.first().id());
    }

    @Test
    public void testEqOutOfBounds() {
        Elements els = select("p");
        Elements none = els.eq(99);
        assertTrue(none.isEmpty());
    }

    @Test
    public void testEqNegative() {
        Elements els = select("p");
        Elements none = els.eq(-1);
        assertTrue(none.isEmpty());
    }

    @Test
    public void testIsTrue() {
        Elements els = select("div");
        assertTrue(els.is("div"));
    }

    @Test
    public void testIsFalse() {
        Elements els = select("div");
        assertFalse(els.is(".nonexistent"));
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void testIsInvalidQuery() {
        Elements els = select("div");
        els.is("??invalid??");
    }

    @Test
    public void testNextSibling() {
        Elements p = select("p");
        Elements next = p.next();
        // first <p> next sibling is the second <p>, second <p> next sibling is the <a>
        assertEquals(2, next.size());
        assertEquals("p2", next.get(0).id());
        assertEquals("a1", next.get(1).id());
    }

    @Test
    public void testPrevSiblingFiltered() {
        Elements a = select("a");
        Elements prevDiv = a.prev("div");
        assertEquals(1, prevDiv.size());
        assertEquals("div1", prevDiv.first().id());
    }

    @Test
    public void testNextFilteredNone() {
        Elements p = select("p");
        Elements none = p.next(".nonexistent");
        assertTrue(none.isEmpty());
    }

    @Test
    public void testParentsDeduplication() {
        Elements span = select("span");
        Elements parents = span.parents();
        // span's ancestors: span -> div2 -> body -> html
        // but `parents()` returns only ancestors, not the element itself, and without duplicates
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("div2", "body", "html"));
        List<String> actual = new ArrayList<>();
        for (Element e : parents) {
            actual.add(e.id().isEmpty() ? e.tagName() : e.id());
        }
        assertEquals(new ArrayList<>(expected), actual);
    }

    @Test
    public void testFirstAndLast() {
        Elements els = select("p");
        assertEquals("p1", els.first().id());
        assertEquals("p2", els.last().id());

        Elements empty = new Elements();
        assertNull(empty.first());
        assertNull(empty.last());
    }

    @Test
    public void testTraverseCallsVisitor() {
        Elements els = select("p");
        final List<Node> visited = new ArrayList<>();
        NodeVisitor visitor = new NodeVisitor() {
            public void head(Node node, int depth) { visited.add(node); }
            public void tail(Node node, int depth) { }
        };
        els.traverse(visitor);
        // At least the two <p> elements should have been visited
        assertTrue(visited.containsAll(els));
    }

    @Test(expected = RuntimeException.class)
    public void testTraverseExceptionPropagation() {
        Elements els = select("p");
        NodeVisitor badVisitor = new NodeVisitor() {
            public void head(Node node, int depth) { throw new RuntimeException("boom"); }
            public void tail(Node node, int depth) { }
        };
        els.traverse(badVisitor);
    }

    @Test
    public void testFilterCallsNodeFilter() {
        Elements els = select("p");
        final List<Node> filtered = new ArrayList<>();
        NodeFilter filter = new NodeFilter() {
            public FilterResult head(Node node, int depth) {
                filtered.add(node);
                return FilterResult.CONTINUE;
            }
            public FilterResult tail(Node node, int depth) { return FilterResult.CONTINUE; }
        };
        els.filter(filter);
        assertTrue(filtered.containsAll(els));
    }

    @Test(expected = RuntimeException.class)
    public void testFilterExceptionPropagation() {
        Elements els = select("p");
        NodeFilter badFilter = new NodeFilter() {
            public FilterResult head(Node node, int depth) { throw new RuntimeException("boom"); }
            public FilterResult tail(Node node, int depth) { return FilterResult.CONTINUE; }
        };
        els.filter(badFilter);
    }

    @Test
    public void testFormsExtraction() {
        Elements all = createTestDocument().select("*");
        List<FormElement> forms = all.forms();
        assertEquals(1, forms.size());
        assertEquals("form1", forms.get(0).id());
    }

    @Test
    public void testFormsEmpty() {
        Elements els = select("p");
        List<FormElement> forms = els.forms();
        assertTrue(forms.isEmpty());
    }

    /* -------------------------------------------------------------------- *
     *  6. Edge‑case specific tests (null arguments, empty strings, etc.)   *
     * -------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testAttrNullKey() {
        Elements els = new Elements(new Element(Tag.valueOf("div"), ""));
        els.attr(null);
    }

    @Test
    public void testHasAttrEmptyList() {
        Elements empty = new Elements();
        assertFalse(empty.hasAttr("class"));
    }

    @Test
    public void testEachAttrNone() {
        Elements els = new Elements(
                new Element(Tag.valueOf("a"), "").attr("href", "http://example.com"),
                new Element(Tag.valueOf("span"), "")
        );
        List<String> result = els.eachAttr("title");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testAttrSetNullValue() {
        Elements els = new Elements(new Element(Tag.valueOf("p"), ""));
        els.attr("data", null);
        assertEquals("null", els.first().attr("data"));
    }

    @Test
    public void testAddClassNull() {
        Elements els = new Elements(new Element(Tag.valueOf("p"), ""));
        try {
            els.addClass(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void testHtmlEmpty() {
        Elements empty = new Elements();
        assertEquals("", empty.html());
        assertEquals("", empty.outerHtml());
    }

    @Test
    public void testTextWhitespaceOnly() {
        Elements els = select("p#p2"); // contains only whitespace
        assertEquals("   ", els.text());
        assertFalse(els.hasText());
    }
}
###Test END##