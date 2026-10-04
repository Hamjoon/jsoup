**Step 1 – Public methods (signatures)**  

| # | Method | Signature |
|---|--------|-----------|
| 1 | `Elements()` | `public Elements()` |
| 2 | `Elements(int)` | `public Elements(int initialCapacity)` |
| 3 | `Elements(Collection<Element>)` | `public Elements(Collection<Element> elements)` |
| 4 | `Elements(List<Element>)` | `public Elements(List<Element> elements)` |
| 5 | `Elements(Element… )` | `public Elements(Element... elements)` |
| 6 | `clone()` | `public Elements clone()` |
| 7 | `attr(String)` | `public String attr(String attributeKey)` |
| 8 | `hasAttr(String)` | `public boolean hasAttr(String attributeKey)` |
| 9 | `eachAttr(String)` | `public List<String> eachAttr(String attributeKey)` |
|10| `attr(String,String)` | `public Elements attr(String attributeKey, String attributeValue)` |
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

**Step 2 – Basic test ideas (one per method)**  

*Constructors* – verify size after creation.  
*clone()* – cloned list contains deep‑cloned elements (modifying original does not affect clone).  
*attr(String)* – returns first matching attribute, empty string when none.  
*hasAttr(String)* – true/false depending on presence.  
*eachAttr(String)* – list contains values only for elements that have the attribute.  
*attr(String,String)* – sets attribute on all elements; verify with `attr(key)`.  
*removeAttr(String)* – attribute removed from all; verify with `hasAttr`.  
*addClass/removeClass/toggleClass/hasClass* – manipulate class attribute and verify.  
*val()* – returns value of first form element; empty when list empty.  
*val(String)* – sets value on all form elements; verify with `val()`.  
*text()* – concatenates texts with spaces.  
*hasText()* – true when any element has non‑blank text.  
*eachText()* – list of texts for elements that have text.  
*html()* – concatenates inner HTML separated by newline.  
*outerHtml()* – concatenates outer HTML separated by newline.  
*toString()* – same as `outerHtml()`.  
*tagName(String)* – changes tag name of each element.  
*html(String)* – replaces inner HTML of each element.  
*prepend/append* – add HTML before/after existing inner HTML.  
*before/after* – insert HTML before/after the element in the DOM.  
*wrap(String)* – wrap each element; throws `IllegalArgumentException` for empty string.  
*unwrap()* – removes element but keeps children.  
*empty()* – clears children of each element.  
*remove()* – removes each element from the DOM.  
*select(String)* – returns filtered Elements using a CSS query.  
*not(String)* – returns Elements that do **not** match the query.  
*eq(int)* – returns a single‑element list or empty when index out of bounds.  
*is(String)* – true when any element matches the selector.  
*next/next(String)/nextAll/nextAll(String)* – sibling navigation.  
*prev/prev(String)/prevAll/prevAll(String)* – previous‑sibling navigation.  
*parents()* – returns the full ancestor chain (no duplicates).  
*first()/last()* – return first / last element or null on empty list.  
*traverse(NodeVisitor)* – visitor is called for each node (test with a counter).  
*filter(NodeFilter)* – filter is applied (test with a counter).  
*forms()* – returns only `FormElement` instances.

---

**Step 3 – Edge‑case & exception scenarios**

| Method | Edge / Exception |
|--------|------------------|
| Constructors | `new Elements(0)` – size 0; `new Elements((Collection<Element>)null)` – should throw `NullPointerException` (inherited ArrayList). |
| `attr(String)` / `hasAttr` / `eachAttr` | Empty list → returns "" / false / empty list. |
| `attr(String,String)` | Null key/value → `NullPointerException` from `Element.attr`. |
| `removeAttr` | Removing non‑existent attribute → no error, still false afterwards. |
| `addClass/removeClass/toggleClass` | Adding duplicate class → should not duplicate; removing absent class → no change. |
| `val()` | Empty list → returns "". |
| `text()/eachText()` | Elements with only whitespace → `hasText()` false, `eachText()` empty. |
| `html()/outerHtml()` | Empty list → returns empty string. |
| `wrap(String)` | Empty or null string → `IllegalArgumentException` (Validate.notEmpty). |
| `eq(int)` | Negative index / index ≥ size → empty Elements. |
| `is(String)` | Invalid selector → `Selector.SelectorParseException` (propagated). |
| `next/prev` families | Elements without siblings → empty Elements. |
| `parents()` | Elements that share ancestors – result must not contain duplicates. |
| `first()/last()` | Empty list → returns null. |
| `traverse(NodeVisitor)` / `filter(NodeFilter)` | Visitor / filter that throws exception – exception should propagate. |
| `forms()` | List without forms → empty list. |

---

**Step 4 – Full JUnit 4 test file**

Below is the complete **ElementsTest.java** that incorporates the basic tests, edge‑case tests, and the exception test for `wrap`.  
The file starts with `###Test START##` and ends with `###Test END##` as requested.

```java
###Test START##
package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.jsoup.select.NodeFilter;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ElementsTest {

    private Document doc;
    private Elements divs;
    private Elements paras;
    private Elements forms;
    private Elements empty;

    @Before
    public void setUp() {
        String html = "<html><head><title>Test</title></head>"
                + "<body>"
                + "<div id='d1' class='c1'><p id='p1'>Hello</p></div>"
                + "<div id='d2' class='c2'><p id='p2'>World</p></div>"
                + "<form id='f1'><input name='q' value='test'></form>"
                + "<form id='f2'></form>"
                + "</body></html>";
        doc = Jsoup.parse(html);
        divs = doc.select