package org.jsoup.nodes;

import org.jsoup.parser.Parser;
import org.jsoup.select.Evaluator;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ElementTest {

    /** Helper – creates a simple element with optional children */
    private Element makeElement(String tag, String... childTags) {
        Element el = new Element(Tag.valueOf(tag), "");
        for (String ct : childTags) {
            el.appendElement(ct);
        }
        return el;
    }

    /* ---------- 1. tagName ---------- */
    @Test public void tagName_returnsTag() {
        Element el = new Element(Tag.valueOf("div"), "");
        assertEquals("div", el.tagName());
    }

    @Test public void tagName_setsTag() {
        Element el = new Element(Tag.valueOf("span"), "");
        el.tagName("p");
        assertEquals("p", el.tagName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void tagName_emptyString_throws() {
        new Element("div").tagName("");
    }

    /* ---------- 2. tag ---------- */
    @Test public void tag_returnsTagObject() {
        Tag t = Tag.valueOf("img");
        Element el = new Element(t, "");
        assertSame(t, el.tag());
    }

    /* ---------- 3. isBlock ---------- */
    @Test public void isBlock_trueForDiv() {
        Element el = new Element("div");
        assertTrue(el.isBlock());
    }

    @Test public void isBlock_falseForSpan() {
        Element el = new Element("span");
        assertFalse(el.isBlock());
    }

    /* ---------- 4. id ---------- */
    @Test public void id_returnsEmptyWhenMissing() {
        Element el = new Element("p");
        assertEquals("", el.id());
    }

    @Test public void id_returnsAttributeWhenPresent() {
        Element el = new Element("p").attr("id", "myId");
        assertEquals("myId", el.id());
    }

    /* ---------- 5. attr(String,String) ---------- */
    @Test public void attr_setsAndGetsStringAttribute() {
        Element el = new Element("a");
        el.attr("href", "http://example.com");
        assertEquals("http://example.com", el.attr("href"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void attr_nullKey_throws() {
        new Element("a").attr(null, "value");
    }

    /* ---------- 6. attr(String,boolean) ---------- */
    @Test public void attr_boolean_trueAddsBooleanAttribute() {
        Element el = new Element("input");
        el.attr("checked", true);
        assertTrue(el.hasAttr("checked"));
        assertEquals("", el.attr("checked"));
    }

    @Test public void attr_boolean_falseRemovesAttribute() {
        Element el = new Element("input").attr("checked", true);
        el.attr("checked", false);
        assertFalse(el.hasAttr("checked"));
    }

    /* ---------- 7. dataset ---------- */
    @Test public void dataset_returnsDataAttributes() {
        Element el = new Element("div")
                .attr("data-name", "jsoup")
                .attr("data-version", "1.0");
        Map<String,String> data = el.dataset();
        assertEquals("jsoup", data.get("name"));
        assertEquals("1.0", data.get("version"));
        assertFalse(data.containsKey("data-name"));
    }

    /* ---------- 8. parent ---------- */
    @Test public void parent_returnsParentElement() {
        Element parent = new Element("div");
        Element child = new Element("p");
        parent.appendChild(child);
        assertSame(parent, child.parent());
    }

    /* ---------- 9. parents ---------- */
    @Test public void parents_collectsAncestorStack() {
        Element html = new Element("html");
        Element body = new Element("body");
        Element div  = new Element("div");
        html.appendChild(body);
        body.appendChild(div);
        Elements ancestors = div.parents();
        assertEquals(2, ancestors.size());
        assertEquals("body", ancestors.get(0).tagName());
        assertEquals("html", ancestors.get(1).tagName());
    }

    /* ---------- 10. child(int) ---------- */
    @Test public void child_byIndex_returnsCorrectElement() {
        Element ul = makeElement("ul", "li", "li", "li");
        Element second = ul.child(1);
        assertEquals("li", second.tagName());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void child_invalidIndex_throws() {
        Element ul = makeElement("ul");
        ul.child(0);
    }

    /* ---------- 11. children ---------- */
    @Test public void children_returnsElementsList() {
        Element div = makeElement("div", "p", "span");
        Elements kids = div.children();
        assertEquals(2, kids.size());
        assertTrue(kids.stream().anyMatch(e -> e.tagName().equals("p")));
        assertTrue(kids.stream().anyMatch(e -> e.tagName().equals("span")));
    }

    /* ---------- 12. textNodes ---------- */
    @Test public void textNodes_filtersOnlyTextNodes() {
        Element p = new Element("p");
        p.appendText("Hello ");
        p.appendElement("b");
        p.appendText("World");
        List<TextNode> txt = p.textNodes();
        assertEquals(2, txt.size());
        assertEquals("Hello ", txt.get(0).getWholeText());
        assertEquals("World", txt.get(1).getWholeText());
    }

    /* ---------- 13. dataNodes ---------- */
    @Test public void dataNodes_filtersOnlyDataNodes() {
        Element script = new Element("script");
        script.appendChild(new DataNode("var a = 1;"));
        List<DataNode> data = script.dataNodes();
        assertEquals(1, data.size());
        assertEquals("var a = 1;", data.get(0).getWholeData());
    }

    /* ---------- 14. select / selectFirst / is ---------- */
    @Test public void select_findsElementsByCss() {
        Element doc = Parser.parse("<div><p class='intro'>Hello</p><p>Bye</p></div>", "");
        Elements ps = doc.select("p.intro");
        assertEquals(1, ps.size());
        assertEquals("Hello", ps.first().text());
    }

    @Test public void selectFirst_returnsFirstMatchOrNull() {
        Element doc = Parser.parse("<ul><li>One</li><li>Two</li></ul>", "");
        Element firstLi = doc.selectFirst("li");
        assertNotNull(firstLi);
        assertEquals("One", firstLi.text());

        assertNull(doc.selectFirst("span"));
    }

    @Test public void is_matchesEvaluator() {
        Element el = new Element("div").addClass("box");
        Evaluator ev = new Evaluator.Class("box");
        assertTrue(el.is(ev));
    }

    /* ---------- 15. appendChild / prependChild ---------- */
    @Test public void appendChild_addsAtEnd() {
        Element div = new Element("div");
        div.appendChild(new TextNode("A"));
        div.appendChild(new TextNode("B"));
        assertEquals("AB", div.text());
    }

    @Test public void prependChild_addsAtStart() {
        Element div = new Element("div");
        div.appendChild(new TextNode("B"));
        div.prependChild(new TextNode("A"));
        assertEquals("AB", div.text());
    }

    @Test(expected = IllegalArgumentException.class)
    public void appendChild_null_throws() {
        new Element("div").appendChild(null);
    }

    /* ---------- 16. insertChildren (Collection) ---------- */
    @Test public void insertChildren_collection_insertsAtIndex() {
        Element ul = new Element("ul");
        ul.appendElement("li").text("One");
        List<Node> nodes = Arrays.asList(new Element(Tag.valueOf("li"), "").text("Two"),
                                         new Element(Tag.valueOf("li"), "").text("Three"));
        ul.insertChildren(1, nodes);
        Elements lis = ul.children();
        assertEquals("One", lis.get(0).text());
        assertEquals("Two", lis.get(1).text());
        assertEquals("Three", lis.get(2).text());
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertChildren_badIndex_throws() {
        new Element("ul").insertChildren(5, Collections.emptyList());
    }

    /* ---------- 17. insertChildren (varargs) ---------- */
    @Test public void insertChildren_varargs_insertsCorrectly() {
        Element ul = new Element("ul");
        ul.appendElement("li").text("One");
        Node two = new Element(Tag.valueOf("li"), "").text("Two");
        Node three = new Element(Tag.valueOf("li"), "").text("Three");
        ul.insertChildren(1, two, three);
        Elements lis = ul.children();
        assertEquals("Two", lis.get(1).text());
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertChildren_varargs_null_throws() {
        new Element("ul").insertChildren(0, (Node[]) null);
    }

    /* ---------- 18. appendElement / prependElement ---------- */
    @Test public void appendElement_createsAndAppends() {
        Element div = new Element("div");
        Element span = div.appendElement("span");
        assertEquals("span", span.tagName());
        assertSame(div, span.parent());
        assertEquals(1, div.children().size());
    }

    @Test public void prependElement_createsAndPrepends() {
        Element div = new Element("div");
        div.appendElement("p");
        Element span = div.prependElement("span");
        assertEquals("span", span.tagName());
        assertEquals(span, div.children().first());
    }

    /* ---------- 19. appendText / prependText ---------- */
    @Test public void appendText_addsTextNode() {
        Element p = new Element("p");
        p.appendText("Hello");
        p.appendText(" World");
        assertEquals("Hello World", p.text());
    }

    @Test public void prependText_addsAtStart() {
        Element p = new Element("p");
        p.appendText("World");
        p.prependText("Hello ");
        assertEquals("Hello World", p.text());
    }

    @Test(expected = IllegalArgumentException.class)
    public void appendText_null_throws() {
        new Element("p").appendText(null);
    }

    /* ---------- 20. append / prepend (HTML) ---------- */
    @Test public void append_parsesHtmlFragment() {
        Element div = new Element("div");
        div.append("<p>One</p><span>Two</span>");
        Elements kids = div.children();
        assertEquals(2, kids.size());
        assertEquals("p", kids.get(0).tagName());
        assertEquals("One", kids.get(0).text());
        assertEquals("span", kids.get(1).tagName());
    }

    @Test public void prepend_parsesHtmlFragment() {
        Element div = new Element("div");
        div.append("<p>Second</p>");
        div.prepend("<p>First</p>");
        Elements kids = div.children();
        assertEquals("First", kids.get(0).text());
        assertEquals("Second", kids.get(1).text());
    }

    /* ---------- 21. empty ---------- */
    @Test public void empty_removesAllChildren() {
        Element div = new Element("div");
        div.appendElement("p");
        div.appendElement("span");
        assertEquals(2, div.childNodeSize());
        div.empty();
        assertEquals(0, div.childNodeSize());
    }

    /* ---------- 22. cssSelector ---------- */
    @Test public void cssSelector_usesIdWhenPresent() {
        Element el = new Element("div").attr("id", "myId");
        assertEquals("#myId", el.cssSelector());
    }

    @Test public void cssSelector_buildsFullPathWhenNoId() {
        Element html = new Element("html");
        Element body = html.appendElement("body");
        Element div  = body.appendElement("div").addClass("box");
        String sel = div.cssSelector();
        // Expected pattern: html > body > div.box (no nth‑child because unique)
        assertTrue(sel.endsWith("div.box"));
        assertTrue(sel.startsWith("html > body >"));
    }

    /* ---------- 23. class handling ---------- */
    @Test public void classNames_parsesClassesCorrectly() {
        Element el = new Element("div").attr("class", "a b   c");
        Set<String> set = el.classNames();
        assertEquals(new LinkedHashSet<>(Arrays.asList("a","b","c")), set);
    }

    @Test public void hasClass_detectsCaseInsensitive() {
        Element el = new Element("div").addClass("MyClass");
        assertTrue(el.hasClass("myclass"));
        assertFalse(el.hasClass("other"));
    }

    @Test public void addClass_appendsWhenMissing() {
        Element el = new Element("div");
        el.addClass("first");
        el.addClass("second");
        assertTrue(el.hasClass("first"));
        assertTrue(el.hasClass("second"));
    }

    @Test public void removeClass_deletesExisting() {
        Element el = new Element("div").addClass("keep").addClass("drop");
        el.removeClass("drop");
        assertTrue(el.hasClass("keep"));
        assertFalse(el.hasClass("drop"));
    }

    @Test public void toggleClass_addsAndRemoves() {
        Element el = new Element("div");
        el.toggleClass("toggle");
        assertTrue(el.hasClass("toggle"));
        el.toggleClass("toggle");
        assertFalse(el.hasClass("toggle"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void classNames_nullSet_throws() {
        new Element("div").classNames(null);
    }

    /* ---------- 24. val / val(String) ---------- */
    @Test public void val_textarea_usesText() {
        Element ta = new Element("textarea").text("Hello");
        assertEquals("Hello", ta.val());
    }

    @Test public void val_input_usesValueAttribute() {
        Element input = new Element("input").attr("value", "42");
        assertEquals("42", input.val());
    }

    @Test public void val_setOnTextarea_updatesText() {
        Element ta = new Element("textarea");
        ta.val("New");
        assertEquals("New", ta.text());
    }

    @Test public void val_setOnInputUpdatesAttribute() {
        Element input = new Element("input");
        input.val("99");
        assertEquals("99", input.attr("value"));
    }

    /* ---------- 25. html / html(String) ---------- */
    @Test public void html_returnsInnerHtml() {
        Element div = new Element("div");
        div.append("<p>One</p><span>Two</span>");
        assertEquals("<p>One</p><span>Two</span>", div.html());
    }

    @Test public void html_setReplacesContent() {
        Element div = new Element("div");
        div.append("<p>Old</p>");
        div.html("<b>New</b>");
        assertEquals("<b>New</b>", div.html());
        assertEquals("New", div.text());
    }

    /* ---------- 26. toString / outerHtml ---------- */
    @Test public void toString_producesOuterHtml() {
        Element div = new Element("div").attr("id","x");
        div.append("<p>Hi</p>");
        assertEquals(div.outerHtml(), div.toString());
    }

    /* ---------- 27. clone / shallowClone ---------- */
    @Test public void clone_producesDeepCopy() {
        Element original = new Element("div").attr("id","orig");
        original.appendElement("p").text("para");
        Element copy = original.clone();
        assertNotSame(original, copy);
        assertEquals("orig", copy.id());
        assertEquals(1, copy.childNodeSize());
        assertEquals("para", copy.selectFirst("p").text());
    }

    @Test public void shallowClone_copiesOnlyTagAndAttrs() {
        Element original = new Element("div").attr("class","c");
        original.appendElement("p");
        Element shallow = original.shallowClone();
        assertEquals("div", shallow.tagName());
        assertTrue(shallow.hasClass("c"));
        assertEquals(0, shallow.childNodeSize());
    }

    /* ---------- 28. edge‑case: regex parsing error ---------- */
    @Test(expected = IllegalArgumentException.class)
    public void getElementsByAttributeValueMatching_invalidRegex_throws() {
        new Element("div").getElementsByAttributeValueMatching("data", "[unclosed");
    }

    /* ---------- 29. edge‑case: null arguments for boolean class methods ---------- */
    @Test(expected = IllegalArgumentException.class)
    public void addClass_null_throws() {
        new Element("div").addClass(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeClass_null_throws() {
        new Element("div").removeClass(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toggleClass_null_throws() {
        new Element("div").toggleClass(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void hasClass_null_throws() {
        new Element("div").hasClass(null);
    }

    /* ---------- 30. edge‑case: constructor null arguments ---------- */
    @Test(expected = IllegalArgumentException.class)
    public void ctor_nullTag_throws() {
        new Element((Tag) null, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctor_nullBaseUri_throws() {
        new Element(Tag.valueOf("div"), null, new Attributes());
    }
}
