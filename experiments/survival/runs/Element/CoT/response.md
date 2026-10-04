**1. Public API of `org.jsoup.nodes.Element` (extracted from the source)**  

| # | Return type | Method signature | Brief description |
|---|--------------|-------------------|-------------------|
| 1 | `Element` | `Element(String tag)` | ctor – creates a stand‑alone element from a tag name |
| 2 | `Element` | `Element(Tag tag, String baseUri, Attributes attributes)` | ctor – creates a stand‑alone element with explicit tag, base‑uri and attributes |
| 3 | `Element` | `Element(Tag tag, String baseUri)` | ctor – creates a stand‑alone element with tag & base‑uri |
| 4 | `String` | `tagName()` | current tag name |
| 5 | `Element` | `tagName(String tagName)` | change the tag name (throws if empty) |
| 6 | `Tag` | `tag()` | the underlying `Tag` object |
| 7 | `boolean` | `isBlock()` | true if the tag is a block‑level element |
| 8 | `String` | `id()` | value of the *id* attribute (or empty string) |
| 9 | `Element` | `attr(String key, String value)` | set a normal attribute (fluent) |
|10 | `Element` | `attr(String key, boolean value)` | set/remove a boolean attribute |
|11 | `Map<String,String>` | `dataset()` | view of all `data‑*` attributes |
|12 | `Element` | `parent()` | direct parent (or `null`) |
|13 | `Elements` | `parents()` | list of ancestors, closest first |
|14 | `Element` | `child(int index)` | child element by *element* index (throws `IndexOutOfBoundsException`) |
|15 | `Elements` | `children()` | list of direct child elements |
|16 | `List<TextNode>` | `textNodes()` | unmodifiable list of child text nodes |
|17 | `List<DataNode>` | `dataNodes()` | unmodifiable list of child data nodes |
|18 | `Elements` | `select(String cssQuery)` | CSS selector – may throw `SelectorParseException` |
|19 | `Element` | `selectFirst(String cssQuery)` | first match of CSS selector |
|20 | `boolean` | `is(String cssQuery)` | true if this element matches the selector |
|21 | `boolean` | `is(Evaluator evaluator)` | true if this element matches the evaluator |
|22 | `Element` | `appendChild(Node child)` | add a child node (fluent) |
|23 | `Element` | `appendTo(Element parent)` | move this element to the supplied parent |
|24 | `Element` | `prependChild(Node child)` | add a child node at the start |
|25 | `Element` | `insertChildren(int index, Collection<? extends Node> children)` | insert a collection of nodes |
|26 | `Element` | `insertChildren(int index, Node... children)` | insert var‑args nodes |
|27 | `Element` | `appendElement(String tagName)` | create a new child element (last) and return it |
|28 | `Element` | `prependElement(String tagName)` | create a new child element (first) and return it |
|29 | `Element` | `appendText(String text)` | create a `TextNode` and append it |
|30 | `Element` | `prependText(String text)` | create a `TextNode` and prepend it |
|31 | `Element` | `append(String html)` | parse *html* and append the resulting nodes |
|32 | `Element` | `prepend(String html)` | parse *html* and prepend the resulting nodes |
|33 | `Element` | `before(String html)` | insert html before this element |
|34 | `Element` | `before(Node node)` | insert node before this element |
|35 | `Element` | `after(String html)` | insert html after this element |
|36 | `Element` | `after(Node node)` | insert node after this element |
|37 | `Element` | `empty()` | remove all child nodes |
|38 | `Element` | `wrap(String html)` | wrap this element with supplied html |
|39 | `String` | `cssSelector()` | unique CSS selector for this element |
|40 | `Elements` | `siblingElements()` | all sibling elements (excluding self) |
|41 | `Element` | `nextElementSibling()` | next sibling element or `null` |
|42 | `Element` | `previousElementSibling()` | previous sibling element or `null` |
|43 | `Element` | `firstElementSibling()` | first sibling element of the parent |
|44 | `Element` | `lastElementSibling()` | last sibling element of the parent |
|45 | `int` | `elementSiblingIndex()` | index of this element among its element‑siblings |
|46 | `Elements` | `getElementsByTag(String tagName)` |
|47 | `Element` | `getElementById(String id)` |
|48 | `Elements` | `getElementsByClass(String className)` |
|49 | `Elements` | `getElementsByAttribute(String key)` |
|50 | `Elements` | `getElementsByAttributeStarting(String keyPrefix)` |
|51 | `Elements` | `getElementsByAttributeValue(String key, String value)` |
|52 | `Elements` | `getElementsByAttributeValueNot(String key, String value)` |
|53 | `Elements` | `getElementsByAttributeValueStarting(String key, String valuePrefix)` |
|54 | `Elements` | `getElementsByAttributeValueEnding(String key, String valueSuffix)` |
|55 | `Elements` | `getElementsByAttributeValueContaining(String key, String match)` |
|56 | `Elements` | `getElementsByAttributeValueMatching(String key, Pattern pattern)` |
|57 | `Elements` | `getElementsByAttributeValueMatching(String key, String regex)` |
|58 | `Elements` | `getElementsByIndexLessThan(int index)` |
|59 | `Elements` | `getElementsByIndexGreaterThan(int index)` |
|60 | `Elements` | `getElementsByIndexEquals(int index)` |
|61 | `Elements` | `getElementsContainingText(String searchText)` |
|62 | `Elements` | `getElementsContainingOwnText(String searchText)` |
|63 | `Elements` | `getElementsMatchingText(Pattern pattern)` |
|64 | `Elements` | `getElementsMatchingText(String regex)` |
|65 | `Elements` | `getElementsMatchingOwnText(Pattern pattern)` |
|66 | `Elements` | `getElementsMatchingOwnText(String regex)` |
|67 | `Elements` | `getAllElements()` |
|68 | `String` | `text()` |
|69 | `String` | `wholeText()` |
|70 | `String` | `ownText()` |
|71 | `Element` | `text(String text)` |
|72 | `boolean` | `hasText()` |
|73 | `String` | `data()` |
|74 | `String` | `className()` |
|75 | `Set<String>` | `classNames()` |
|76 | `Element` | `classNames(Set<String> classNames)` |
|77 | `boolean` | `hasClass(String className)` |
|78 | `Element` | `addClass(String className)` |
|79 | `Element` | `removeClass(String className)` |
|80 | `Element` | `toggleClass(String className)` |
|81 | `String` | `val()` |
|82 | `Element` | `val(String value)` |
|83 | `String` | `html()` |
|84 | `Element` | `html(String html)` |
|85 | `String` | `toString()` (delegates to `outerHtml()`) |
|86 | `Element` | `clone()` |
|87 | `Element` | `shallowClone()` |
|88 | `Element` | `html(Appendable accum)` (inherited, overridden) |

*(Methods inherited from `Node` that are overridden are also listed because they are part of the public API of `Element`.)*  

---

**2. Basic JUnit 4 test cases (one per public method)**  

Below is a concise description of a *happy‑path* test for each method.  
The actual implementation is provided in the complete test file that follows.

| # | Test name | What is verified |
|---|-----------|------------------|
| 1 | `testConstructorTagString()` | `new Element("div")` creates a non‑null element with tag name *div* |
| 2 | `testConstructorTagBaseUriAttributes()` | ctor with explicit `Tag`, base‑uri and empty `Attributes` works |
| 3 | `testTagNameGetter()` | `tagName()` returns the current tag name |
| 4 | `testTagNameSetter()` | `tagName("section")` changes the tag and returns the same instance |
| 5 | `testTagNameSetterEmpty()` | passing empty string throws `IllegalArgumentException` |
| 6 | `testTag()` | returns the underlying `Tag` object and its name matches |
| 7 | `testIsBlock()` | a `<div>` is block, a `<span>` is not |
| 8 | `testId()` | returns the *id* attribute value or empty string |
| 9 | `testAttrStringString()` | normal attribute is set and can be read back |
|10 | `testAttrStringBoolean()` | boolean attribute set to **true** creates empty value; **false** removes it |
|11 | `testDataset()` | `data-foo="bar"` appears in the `dataset()` map |
|12 | `testParent()` | after `appendChild` the child’s `parent()` points to the original element |
|13 | `testParents()` | a nested element returns a list of its ancestors in correct order |
|14 | `testChildByIndex()` | `child(0)` returns the first child element |
|15 | `testChildren()` | `children()` returns a live `Elements` collection of child elements |
|16 | `testTextNodes()` | only `TextNode` objects are returned, list is unmodifiable |
|17 | `testDataNodes()` | only `DataNode` objects are returned, list is unmodifiable |
|18 | `testSelect()` | simple CSS selector (`"p"`) returns expected elements |
|19 | `testSelectFirst()` | returns the first matching element or `null` when none |
|20 | `testIsSelector()` | `is("div")` works for matching tag |
|21 | `testIsEvaluator()` | using a `Evaluator.Tag` works |
|22 | `testAppendChild()` | a child node is added and sibling index is correct |
|23 | `testAppendTo()` | element is moved under a new parent |
|24 | `testPrependChild()` | node is inserted at index 0 |
|25 | `testInsertChildrenCollection()` | collection insertion works at a positive index |
|26 | `testInsertChildrenVarargs()` | var‑args insertion works, negative index rolls‑around |
|27 | `testAppendElement()` | creates a new child element with given tag |
|28 | `testPrependElement()` | creates a new first child element |
|29 | `testAppendText()` | appends a `TextNode` containing the supplied string |
|30 | `testPrependText()` | prepends a `TextNode` |
|31 | `testAppendHtml()` | parses HTML fragment and appends resulting nodes |
|32 | `testPrependHtml()` | parses HTML fragment and prepends nodes |
|33 | `testBeforeString()` | inserts markup before the element |
|34 | `testBeforeNode()` | inserts a node before the element |
|35 | `testAfterString()` | inserts markup after the element |
|36 | `testAfterNode()` | inserts a node after the element |
|37 | `testEmpty()` | removes all child nodes |
|38 | `testWrap()` | wraps the element with a new parent element |
|39 | `testCssSelectorWithId()` | when element has an id, selector is `#id` |
|40 | `testSiblingElements()` | returns all siblings except self |
|41 | `testNextElementSibling()` | returns the next element sibling or `null` |
|42 | `testPreviousElementSibling()` | returns the previous element sibling or `null` |
|43 | `testFirstElementSibling()` | returns the first sibling element of the parent |
|44 | `testLastElementSibling()` | returns the last sibling element of the parent |
|45 | `testElementSiblingIndex()` | index among element‑siblings matches expectation |
|46 | `testGetElementsByTag()` | finds all descendants with a given tag (case‑insensitive) |
|47 | `testGetElementById()` | returns the element with the supplied id (or `null`) |
|48 | `testGetElementsByClass()` | finds descendants that contain the supplied class |
|49 | `testGetElementsByAttribute()` | finds elements that have a given attribute |
|50 | `testGetElementsByAttributeStarting()` | finds elements whose attribute name starts with a prefix |
|51 | `testGetElementsByAttributeValue()` | exact key/value match |
|52 | `testGetElementsByAttributeValueNot()` | opposite of the previous test |
|53 | `testGetElementsByAttributeValueStarting()` | value‑prefix match |
|54 | `testGetElementsByAttributeValueEnding()` | value‑suffix match |
|55 | `testGetElementsByAttributeValueContaining()` | substring match |
|56 | `testGetElementsByAttributeValueMatchingPattern()` | regex pattern match |
|57 | `testGetElementsByAttributeValueMatchingRegex()` | regex string compilation and match |
|58 | `testGetElementsByIndexLessThan()` | sibling index less‑than query |
|59 | `testGetElementsByIndexGreaterThan()` | sibling index greater‑than query |
|60 | `testGetElementsByIndexEquals()` | sibling index equals query |
|61 | `testGetElementsContainingText()` | case‑insensitive text containment |
|62 | `testGetElementsContainingOwnText()` | own‑text containment |
|63 | `testGetElementsMatchingTextPattern()` | regex on full text |
|64 | `testGetElementsMatchingTextRegex()` | regex string version |
|65 | `testGetElementsMatchingOwnTextPattern()` | regex on own text |
|66 | `testGetElementsMatchingOwnTextRegex()` | regex string version |
|67 | `testGetAllElements()` | returns the element itself plus all descendants |
|68 | `testText()` | returns normalized combined text |
|69 | `testWholeText()` | returns raw concatenated text (no whitespace normalisation) |
|70 | `testOwnText()` | returns only the element’s direct text |
|71 | `testSetText()` | clears children and sets a single text node |
|72 | `testHasText()` | true when any descendant text node is non‑blank |
|73 | `testData()` | concatenates `DataNode` and `Comment` contents |
|74 | `testClassName()` | literal value of the *class* attribute |
|75 | `testClassNames()` | split into a `Set` (empty set when no class) |
|76 | `testSetClassNames()` | overwrites the *class* attribute correctly |
|77 | `testHasClass()` | case‑insensitive check, including multi‑class attribute |
|78 | `testAddClass()` | adds a class without duplicating existing ones |
|79 | `testRemoveClass()` | removes an existing class, leaves others untouched |
|80 | `testToggleClass()` | toggles presence of a class |
|81 | `testValTextarea()` | for `<textarea>` returns its text |
|82 | `testValInput()` | for `<input>` returns the *value* attribute |
|83 | `testHtmlGetter()` | returns inner HTML of children |
|84 | `testHtmlSetter()` | replaces inner HTML |
|85 | `testToString()` | delegates to `outerHtml()` (checked via `html()`) |
|86 | `testClone()` | deep clone produces equal but distinct objects |
|87 | `testShallowClone()` | shallow clone copies tag, base‑uri and attributes but no children |
|88 | `testHtmlAppendable()` | `html(Appendable)` writes the HTML into a `StringBuilder` |

---

**3. Edge‑case & exception scenarios identified**

| Method | Edge case / invalid input | Expected behaviour |
|--------|---------------------------|--------------------|
| `tagName(String)` | empty string or `null` | `IllegalArgumentException` (empty) or `NullPointerException` (null) |
| `attr(String, String)` | `null` key or `null` value | `NullPointerException` (validated in `Node.attr`) |
| `appendChild(Node)` | `null` argument | `NullPointerException` |
| `insertChildren(int, Collection)` | `null` collection, index < ‑size‑1, index > size | `IllegalArgumentException` for null, `IllegalArgumentException` for out‑of‑bounds |
| `insertChildren(int, Node...)` | `null` var‑args, out‑of‑bounds index | Same as above |
| `select(String)` | malformed CSS query (e.g. `"??"`) | `Selector.SelectorParseException` (unchecked) |
| `getElementsByAttributeValueMatching(String, String)` | invalid regex (e.g. `"*["`) | `IllegalArgumentException` wrapping `PatternSyntaxException` |
| `getElementsByAttributeValueMatching(String, Pattern)` | `null` pattern | `NullPointerException` |
| `cssSelector()` | element without id and multiple identical siblings – must append `:nth-child` | verify that the selector contains `:nth-child(` |
| `hasClass(String)` | `null` argument | `NullPointerException` (validated by `Validate.notNull`) |
| `classNames(Set<String>)` | `null` set | `NullPointerException` |
| `addClass/removeClass/toggleClass` | `null` argument | `NullPointerException` |
| `val(String)` | `null` value | `NullPointerException` (validated by `Validate.notNull`) |
| `text(String)` | `null` text | `NullPointerException` |
| `appendText/prependText` | `null` text | `NullPointerException` |
| `append/ prepend(String)` | `null` html | `NullPointerException` |
| `wrap(String)` | malformed HTML that cannot be parsed into a single element | `IllegalArgumentException` (as per Jsoup’s `wrap` implementation) – we test a simple valid case; the failure path is covered by the underlying Jsoup library, not re‑implemented here. |

---

**4. Complete JUnit 4 test file (`ElementTest.java`)**  

```java
###Test START##
package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.select.Evaluator;
import org.jsoup.select.Selector;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public class ElementTest {

    private Document doc;               // a simple document used for most tests
    private Element div;               // <div id="root"><p class="a b">Hello <span>World</span></p></div>

    @Before
    public void setUp() {
        doc = Jsoup.parse("<div id='root'><p class='a b'>Hello <span>World</span></p></div>");
        div = doc.selectFirst("div");
        assertNotNull(div);
    }

    /* -------------------------------------------------
       1‑3   constructors & basic tag handling
       ------------------------------------------------- */
    @Test
    public void testConstructorTagString() {
        Element el = new Element("section");
        assertEquals("section", el.tagName());
    }

    @Test
    public void testConstructorTagBaseUriAttributes() {
        Tag tag = Tag.valueOf("p");
        Attributes attrs = new Attributes();
        attrs.put("title", "test");
        Element el = new Element(tag, "http://example.com", attrs);
        assertEquals("p", el.tagName());
        assertEquals("http://example.com", el.baseUri());
        assertEquals("test", el.attr("title"));
    }

    @Test
    public void testTagNameGetterSetter() {
        Element el = new Element("div");
        assertEquals("div", el.tagName());
        el.tagName("section");
        assertEquals("section", el.tagName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTagNameSetterEmpty() {
        new Element("div").tagName("");
    }

    @Test
    public void testTag() {
        Tag t = Tag.valueOf("img");
        Element el = new Element(t, "", new Attributes());
        assertSame(t, el.tag());
    }

    @Test
    public void testIsBlock() {
        Element block = new Element("div");
        Element inline = new Element("span");
        assertTrue(block.isBlock());
        assertFalse(inline.isBlock());
    }

    @Test
    public void testId() {
        assertEquals("root", div.id());
        Element noId = new Element("p");
        assertEquals("", noId.id());
    }

    /* -------------------------------------------------
       9‑11   attributes & dataset
       ------------------------------------------------- */
    @Test
    public void testAttrStringString() {
        Element el = new Element("a");
        el.attr("href", "http://example.com");
        assertEquals("http://example.com", el.attr("href"));
    }

    @Test(expected = NullPointerException.class)
    public void testAttrStringString_NullKey() {
        new Element("a").attr(null, "v");
    }

    @Test(expected = NullPointerException.class)
    public void testAttrStringString_NullValue() {
        new Element("a").attr("k", null);
    }

    @Test
    public void testAttrStringBoolean() {
        Element el = new Element("input");
        el.attr("checked", true);
        assertTrue(el.hasAttr("checked"));
        assertEquals("", el.attr("checked")); // boolean attribute has empty value

        el.attr("checked", false);
        assertFalse(el.hasAttr("checked"));
    }

    @Test
    public void testDataset() {
        Element el = new Element("div");
        el.attr("data-foo", "bar");
        el.attr("data-number", "42");
        Map<String, String> ds = el.dataset();
        assertEquals(2, ds.size());
        assertEquals("bar", ds.get("foo"));
        assertEquals("42", ds.get("number"));
    }

    /* -------------------------------------------------
       12‑15   parent / children navigation
       ------------------------------------------------- */
    @Test
    public void testParent() {
        Element p = div.selectFirst("p");
        assertSame(div, p.parent());
    }

    @Test
    public void testParents() {
        Element span = div.selectFirst("span");
        Elements ancestors = span.parents();
        assertEquals(2, ancestors.size()); // p and div
        assertEquals("p", ancestors.get(0).tagName());
        assertEquals("div", ancestors.get(1).tagName());
    }

    @Test
    public void testChildByIndex() {
        Element p = div.selectFirst("p");
        Element span = p.child(0); // only one element child
        assertEquals("span", span.tagName());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testChildByIndexOutOfBounds() {
        div.child(10);
    }

    @Test
    public void testChildren() {
        Elements ch = div.children();
        assertEquals(1, ch.size());
        assertEquals("p", ch.get(0).tagName());
    }

    /* -------------------------------------------------
       16‑17   node filters
       ------------------------------------------------- */
    @Test
    public void testTextNodes() {
        Element p = div.selectFirst("p");
        List<TextNode> txt = p.textNodes();
        assertEquals(2, txt.size()); // "Hello " and " " after span (Jsoup collapses)
        // verify immutability
        try {
            txt.add(new TextNode("bad"));
            fail("List should be unmodifiable");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testDataNodes() {
        Element script = new Element("script");
        script.appendChild(new DataNode("var a = 1;"));
        List<DataNode> data = script.dataNodes();
        assertEquals(1, data.size());
        assertEquals("var a = 1;", data.get(0).getWholeData());
    }

    /* -------------------------------------------------
       18‑21   selectors & evaluators
       ------------------------------------------------- */
    @Test
    public void testSelect() {
        Elements sel = div.select("p");
        assertEquals(1, sel.size());
        assertEquals("p", sel.get(0).tagName());
    }

    @Test
    public void testSelectFirst() {
        Element first = div.selectFirst("p");
        assertNotNull(first);
        assertEquals("p", first.tagName());

        assertNull(div.selectFirst("nonexistent"));
    }

    @Test
    public void testIsSelector() {
        assertTrue(div.is("div"));
        assertFalse(div.is("span"));
    }

    @Test
    public void testIsEvaluator() {
        Evaluator ev = new Evaluator.Tag("div");
        assertTrue(div.is(ev));
    }

    /* -------------------------------------------------
       22‑32   DOM manipulation helpers
       ------------------------------------------------- */
    @Test
    public void testAppendChild() {
        Element child = new Element("span");
        div.appendChild(child);
        assertEquals(2, div.childNodeSize());
        assertSame(div, child.parent());
        assertEquals(1, child.siblingIndex());
    }

    @Test(expected = NullPointerException.class)
    public void testAppendChild_Null() {
        div.appendChild(null);
    }

    @Test
    public void testAppendTo() {
        Element newEl = new Element("section");
        newEl.appendTo(div);
        assertSame(div, newEl.parent());
        assertTrue(div.children().contains(newEl));
    }

    @Test
    public void testPrependChild() {
        Element first = new Element("b");
        div.prependChild(first);
        assertEquals(first, div.childNode(0));
    }

    @Test
    public void testInsertChildrenCollection() {
        Element a = new Element("a");
        Element b = new Element("b");
        List<Element> list = Arrays.asList(a, b);
        div.insertChildren(1, list);
        assertEquals(a, div.childNode(1));
        assertEquals(b, div.childNode(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertChildrenCollection_Null() {
        div.insertChildren(0, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertChildrenCollection_OutOfBounds() {
        div.insertChildren(100, Collections.emptyList());
    }

    @Test
    public void testInsertChildrenVarargs() {
        Element a = new Element("a");
        Element b = new Element("b");
        div.insertChildren(-1, a, b); // -1 means append
        assertEquals(b, div.childNode(div.childNodeSize() - 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertChildrenVarargs_OutOfBounds() {
        div.insertChildren(5, new Element("x"));
    }

    @Test
    public void testAppendElement() {
        Element child = div.appendElement("section");
        assertEquals("section", child.tagName());
        assertTrue(div.children().contains(child));
    }

    @Test
    public void testPrependElement() {
        Element child = div.prependElement("header");
        assertEquals("header", child.tagName());
        assertSame(child, div.childNode(0));
    }

    @Test
    public void testAppendText() {
        div.appendText("FOO");
        assertTrue(div.text().contains("FOO"));
    }

    @Test(expected = NullPointerException.class)
    public void testAppendText_Null() {
        div.appendText(null);
    }

    @Test
    public void testPrependText() {
        div.prependText("BAR");
        assertTrue(div.text().startsWith("BAR"));
    }

    @Test
    public void testAppendHtml() {
        div.append("<p id='new'>New</p>");
        assertNotNull(div.selectFirst("#new"));
    }

    @Test
    public void testPrependHtml() {
        div.prepend("<h1>Title</h1>");
        Element h1 = div.child(0);
        assertEquals("h1", h1.tagName());
    }

    /* -------------------------------------------------
       33‑38   sibling insertion & wrapping
       ------------------------------------------------- */
    @Test
    public void testBeforeString() {
        Element p = div.selectFirst("p");
        p.before("<hr/>");
        assertEquals("hr", p.previousElementSibling().tagName());
    }

    @Test
    public void testBeforeNode() {
        Element p = div.selectFirst("p");
        Element hr = new Element("hr");
        p.before(hr);
        assertEquals(hr, p.previousElementSibling());
    }

    @Test
    public void testAfterString() {
        Element p = div.selectFirst("p");
        p.after("<footer/>");
        assertEquals("footer", p.nextElementSibling().tagName());
    }

    @Test
    public void testAfterNode() {
        Element p = div.selectFirst("p");
        Element foot = new Element("footer");
        p.after(foot);
        assertEquals(foot, p.nextElementSibling());
    }

    @Test
    public void testEmpty() {
        Element p = div.selectFirst("p");
        p.empty();
        assertEquals(0, p.childNodeSize());
        assertEquals("", p.text());
    }

    @Test
    public void testWrap() {
        Element span = div.selectFirst("span");
        span.wrap("<div class='wrapper'></div>");
        Element wrapper = span.parent();
        assertEquals("wrapper", wrapper.className());
    }

    /* -------------------------------------------------
       39‑45   selectors & sibling helpers
       ------------------------------------------------- */
    @Test
    public void testCssSelectorWithId() {
        assertEquals("#root", div.cssSelector());
    }

    @Test
    public void testSiblingElements() {
        Element p = div.selectFirst("p");
        p.appendElement("a");
        Elements siblings = p.siblingElements(); // only the <a> we just added
        assertEquals(1, siblings.size());
        assertEquals("a", siblings.get(0).tagName());
    }

    @Test
    public void testNextElementSibling() {
        Element p = div.selectFirst("p");
        Element a = p.appendElement("a");
        assertSame(a, p.nextElementSibling());
        assertNull(a.nextElementSibling());
    }

    @Test
    public void testPreviousElementSibling() {
        Element p = div.selectFirst("p");
        Element a = p.appendElement("a");
        assertSame(p, a.previousElementSibling());
        assertNull(p.previousElementSibling());
    }

    @Test
    public void testFirstAndLastElementSibling() {
        Element first = div.appendElement("first");
        Element middle = div.appendElement("mid");
        Element last = div.appendElement("last");
        assertSame(first, middle.firstElementSibling());
        assertSame(last, middle.lastElementSibling());
    }

    @Test
    public void testElementSiblingIndex() {
        Element first = div.appendElement("first");
        Element second = div.appendElement("second");
        assertEquals(0, first.elementSiblingIndex());
        assertEquals(1, second.elementSiblingIndex());
    }

    /* -------------------------------------------------
       46‑66   query helpers (tag, class, attribute, text, regex)
       ------------------------------------------------- */
    @Test
    public void testGetElementsByTag() {
        Elements els = div.getElementsByTag("span");
        assertEquals(1, els.size());
        assertEquals("span", els.get(0).tagName());
    }

    @Test
    public void testGetElementById() {
        Element found = div.getElementById("root");
        assertSame(div, found);
        assertNull(div.getElementById("nonexistent"));
    }

    @Test
    public void testGetElementsByClass() {
        Elements els = div.getElementsByClass("a");
        assertEquals(1, els.size());
        assertEquals("p", els.get(0).tagName());
    }

    @Test
    public void testGetElementsByAttribute() {
        Elements els = div.getElementsByAttribute("class");
        assertEquals(1, els.size());
        assertEquals("p", els.get(0).tagName());
    }

    @Test
    public void testGetElementsByAttributeStarting() {
        div.attr("data-foo", "bar");
        Elements els = div.getElementsByAttributeStarting("data-");
        assertEquals(1, els.size());
    }

    @Test
    public void testGetElementsByAttributeValue() {
        Element input = new Element("input");
        input.attr("type", "text");
        div.appendChild(input);
        Elements els = div.getElementsByAttributeValue("type", "text");
        assertEquals(1, els.size());
        assertSame(input, els.get(0));
    }

    @Test
    public void testGetElementsByAttributeValueNot() {
        Element input = new Element("input");
        input.attr("type", "password");
        div.appendChild(input);
        Elements els = div.getElementsByAttributeValueNot("type", "text");
        assertTrue(els.contains(input));
    }

    @Test
    public void testGetElementsByAttributeValueStarting() {
        Element a = new Element("a");
        a.attr("href", "https://example.com");
        div.appendChild(a);
        Elements els = div.getElementsByAttributeValueStarting("href", "https://");
        assertTrue(els.contains(a));
    }

    @Test
    public void testGetElementsByAttributeValueEnding() {
        Element a = new Element("a");
        a.attr("href", "http://example.org");
        div.appendChild(a);
        Elements els = div.getElementsByAttributeValueEnding("href", ".org");
        assertTrue(els.contains(a));
    }

    @Test
    public void testGetElementsByAttributeValueContaining() {
        Element a = new Element("a");
        a.attr("href", "http://example.com/page");
        div.appendChild(a);
        Elements els = div.getElementsByAttributeValueContaining("href", "example.com");
        assertTrue(els.contains(a));
    }

    @Test
    public void testGetElementsByAttributeValueMatchingPattern() {
        Element a = new Element("a");
        a.attr("href", "http://foo.com");
        div.appendChild(a);
        Elements els = div.getElementsByAttributeValueMatching("href", Pattern.compile("foo\\.com"));
        assertTrue(els.contains(a));
    }

    @Test
    public void testGetElementsByAttributeValueMatchingRegex() {
        Element a = new Element("a");
        a.attr("href", "http://bar.com");
        div.appendChild(a);
        Elements els = div.getElementsByAttributeValueMatching("href", "bar\\.com");
        assertTrue(els.contains(a));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetElementsByAttributeValueMatchingInvalidRegex() {
        div.getElementsByAttributeValueMatching("href", "*[");
    }

    @Test
    public void testGetElementsByIndexLessThan() {
        Element first = div.appendElement("first");
        Element second = div.appendElement("second");
        Elements less = div.getElementsByIndexLessThan(1);
        assertTrue(less.contains(first));
        assertFalse(less.contains(second));
    }

    @Test
    public void testGetElementsByIndexGreaterThan() {
        Element first = div.appendElement("first");
        Element second = div.appendElement("second");
        Elements greater = div.getElementsByIndexGreaterThan(0);
        assertTrue(greater.contains(second));
        assertFalse(greater.contains(first));
    }

    @Test
    public void testGetElementsByIndexEquals() {
        Element first = div.appendElement("first");
        Elements eq = div.getElementsByIndexEquals(0);
        assertTrue(eq.contains(first));
    }

    @Test
    public void testGetElementsContainingText() {
        Elements els = div.getElementsContainingText("Hello");
        assertEquals(1, els.size());
        assertEquals("p", els.get(0).tagName());
    }

    @Test
    public void testGetElementsContainingOwnText() {
        Elements els = div.getElementsContainingOwnText("Hello");
        assertEquals(1, els.size());
        assertEquals("p", els.get(0).tagName());
    }

    @Test
    public void testGetElementsMatchingTextPattern() {
        Elements els = div.getElementsMatchingText(Pattern.compile("Hello"));
        assertEquals(1, els.size());
    }

    @Test
    public void testGetElementsMatchingTextRegex() {
        Elements els = div.getElementsMatchingText("Hello");
        assertEquals(1, els.size());
    }

    @Test
    public void testGetElementsMatchingOwnTextPattern() {
        Elements els = div.getElementsMatchingOwnText(Pattern.compile("Hello"));
        assertEquals(1, els.size());
    }

    @Test
    public void testGetElementsMatchingOwnTextRegex() {
        Elements els = div.getElementsMatchingOwnText("Hello");
        assertEquals(1, els.size());
    }

    @Test
    public void testGetAllElements() {
        Elements all = div.getAllElements();
        // div, p, span = 3 elements
        assertEquals(3, all.size());
    }

    /* -------------------------------------------------
       68‑73   text handling
       ------------------------------------------------- */
    @Test
    public void testText() {
        assertEquals("Hello World", div.selectFirst("p").text());
    }

    @Test
    public void testWholeText() {
        assertEquals("Hello World", div.selectFirst("p").wholeText());
    }

    @Test
    public void testOwnText() {
        assertEquals("Hello", div.selectFirst("p").ownText());
    }

    @Test
    public void testSetText() {
        Element p = div.selectFirst("p");
        p.text("New content");
        assertEquals("New content", p.text());
        assertEquals(1, p.childNodeSize()); // only one TextNode left
    }

    @Test
    public void testHasText() {
        assertTrue(div.selectFirst("p").hasText());
        Element empty = new Element("div");
        assertFalse(empty.hasText());
    }

    @Test
    public void testData() {
        Element script = new Element("script");
        script.appendChild(new DataNode("alert('x');"));
        script.appendChild(new Comment("a comment"));
        assertEquals("alert('x');a comment", script.data());
    }

    /* -------------------------------------------------
       74‑80   class handling
       ------------------------------------------------- */
    @Test
    public void testClassName() {
        Element p = div.selectFirst("p");
        assertEquals("a b", p.className());
    }

    @Test
    public void testClassNames() {
        Element p = div.selectFirst("p");
        Set<String> set = p.classNames();
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
    }

    @Test
    public void testSetClassNames() {
        Element el = new Element("div");
        Set<String> cls = new LinkedHashSet<>(Arrays.asList("one", "two"));
        el.classNames(cls);
        assertEquals("one two", el.className());
    }

    @Test
    public void testHasClass() {
        Element p = div.selectFirst("p");
        assertTrue(p.hasClass("A")); // case‑insensitive
        assertFalse(p.hasClass("c"));
    }

    @Test
    public void testAddClass() {
        Element p = div.selectFirst("p");
        p.addClass("c");
        assertTrue(p.hasClass("c"));
        assertEquals(3, p.classNames().size());
    }

    @Test
    public void testRemoveClass() {
        Element p = div.selectFirst("p");
        p.removeClass("a");
        assertFalse(p.hasClass("a"));
        assertTrue(p.hasClass("b"));
    }

    @Test
    public void testToggleClass() {
        Element p = div.selectFirst("p");
        p.toggleClass("a"); // removes
        assertFalse(p.hasClass("a"));
        p.toggleClass("z"); // adds
        assertTrue(p.hasClass("z"));
    }

    /* -------------------------------------------------
       81‑82   form values
       ------------------------------------------------- */
    @Test
    public void testValTextarea() {
        Element txt = new Element("textarea");
        txt.text("some text");
        assertEquals("some text", txt.val());
    }

    @Test
    public void testValInput() {
        Element input = new Element("input");
        input.attr("value", "123");
        assertEquals("123", input.val());
    }

    @Test
    public void testSetValTextarea() {
        Element txt = new Element("textarea");
        txt.val("new");
        assertEquals("new", txt.text());
    }

    @Test
    public void testSetValInput() {
        Element input = new Element("input");
        input.val("456");
        assertEquals("456", input.attr("value"));
    }

    /* -------------------------------------------------
       83‑85   HTML getters / setters / toString
       ------------------------------------------------- */
    @Test
    public void testHtmlGetter() {
        String html = div.selectFirst("p").html();
        assertTrue(html.contains("<span"));
    }

    @Test
    public void testHtmlSetter() {
        Element p = div.selectFirst("p");
        p.html("<b>Bold</b>");
        assertEquals("Bold", p.text());
        assertEquals(1, p.childNodeSize());
        assertEquals("b", p.child(0).tagName());
    }

    @Test
    public void testToStringDelegatesToOuterHtml() {
        String outer = div.selectFirst("p").toString();
        assertTrue(outer.startsWith("<p"));
        assertTrue(outer.endsWith("</p>"));
    }

    /* -------------------------------------------------
       86‑87   cloning
       ------------------------------------------------- */
    @Test
    public void testCloneDeep() {
        Element clone = div.clone();
        assertNotSame(div, clone);
        assertEquals(div.html(), clone.html());
        // modify clone – original must stay unchanged
        clone.appendElement("section");
        assertNotEquals(div.html(), clone.html());
    }

    @Test
    public void testShallowClone() {
        Element shallow = div.shallowClone();
        assertEquals(div.tagName(), shallow.tagName());
        assertEquals(div.baseUri(), shallow.baseUri());
        assertEquals(div.attributes().html(), shallow.attributes().html());
        assertEquals(0, shallow.childNodeSize()); // no children copied
    }

    /* -------------------------------------------------
       88   html(Appendable) overload
       ------------------------------------------------- */
    @Test
    public void testHtmlAppendable() {
        StringBuilder sb = new StringBuilder();
        div.html(sb);
        assertTrue(sb.toString().contains("<div"));
    }

    /* -------------------------------------------------
       Additional edge‑case checks
       ------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testTagNameSetter_Null() {
        new Element("div").tagName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHasClass_Null() {
        new Element("p").hasClass(null);
    }

    @Test(expected = NullPointerException.class)
    public void testClassNames_NullSet() {
        new Element("p").classNames(null);
    }

    @Test(expected = NullPointerException.class)
    public void testVal_Null() {
        new Element("input").val(null);
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void testSelectInvalidQuery() {
        div.select("??");
    }
}
###Test END##
```