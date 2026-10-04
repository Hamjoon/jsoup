**1. Public API of `PseudoTextElement`**

| # | Visibility | Return type | Method / Constructor | Parameters |
|---|------------|-------------|----------------------|------------|
| 1 | **public** | `PseudoTextElement` | **PseudoTextElement** (constructor) | `Tag tag`, `String baseUri`, `Attributes attributes` |
| 2 | **public** | `String` | `attr(String key)` *(inherited from `Element`)* | `String key` |
| 3 | **public** | `Attributes` | `attributes()` *(inherited from `Element`)* | – |
| 4 | **public** | `String` | `baseUri()` *(inherited from `Element`)* | – |
| 5 | **public** | `String` | `text()` *(inherited from `Element`)* | – |
| 6 | **public** | `Element` | `appendText(String text)` *(inherited from `Element`)* | `String text` |
| 7 | **public** | `Element` | `remove()` *(inherited from `Element`)* | – |
| … | … | … | *(all other public members of `Element`)* | … |

*Only the constructor is declared directly in `PseudoTextElement`; the rest are inherited from `org.jsoup.nodes.Element`.*

---

**2. Basic JUnit‑4 test cases (functionality)**  

| Test | What is verified |
|------|------------------|
| `testConstructorInitialisesFields` | The `tag`, `baseUri` and `attributes` passed to the constructor are correctly stored and can be retrieved via the inherited getters. |
| `testOuterHtmlHeadDoesNothing` | Calling the package‑private `outerHtmlHead` on a fresh `StringBuilder` leaves the builder unchanged. |
| `testOuterHtmlTailDoesNothing` | Same as above for `outerHtmlTail`. |
| `testInheritedAttrWorks` | The inherited `attr(String)` method works on the pseudo‑element (demonstrates that the object behaves like a normal `Element`). |
| `testInheritedTextWorks` | Adding text with `appendText` and reading it back with `text()` works. |

---

**3. Edge‑case & exception scenarios**

| Edge case | Reasoning |
|-----------|-----------|
| **Null `Appendable`** passed to `outerHtmlHead` / `outerHtmlTail` | The overridden methods are empty, so they should *not* throw a `NullPointerException`. The test ensures the call succeeds. |
| **Null `Document.OutputSettings`** passed to `outerHtmlHead` / `outerHtmlTail` | Same reasoning – the implementation does not touch the argument, therefore it must accept `null`. |
| **Negative depth** value | Depth is only used for indentation in the real implementation; the overridden version ignores it, so any integer (including negative) should be accepted without side‑effects. |
| **Empty / null `Tag`** – not allowed by the superclass, but we can verify that passing a `null` tag to the constructor throws `IllegalArgumentException` (the `Element` constructor validates the tag). |
| **Null `Attributes`** – the superclass accepts a non‑null `Attributes` instance; passing `null` should raise `IllegalArgumentException`. |
| **Large amount of text** – ensure that `appendText` does not interfere with the overridden HTML methods (still produce no output). |

---

**4. JUnit‑4 test file that covers everything**

```java
###Test START##
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
###Test END##
```

**Explanation of the test file**

* The test class lives in the same package (`org.jsoup.nodes`) so it can access the package‑private `outerHtmlHead` and `outerHtmlTail` methods via reflection.  
* `setUp()` creates a reusable `PseudoTextElement` instance with a simple `<p>` tag and one attribute.  
* Basic tests verify that the constructor correctly propagates its arguments to the inherited getters and that inherited mutators (`attr`, `appendText`) work as expected.  
* For the overridden methods we invoke them reflectively and assert that the supplied `Appendable` remains unchanged, regardless of depth, null arguments, or a large amount of internal text.  
* Edge‑case tests ensure that null arguments do **not** cause unexpected `NullPointerException`s, that negative depth values are tolerated, and that the superclass‑level validation in the `Element` constructor throws `IllegalArgumentException` when mandatory arguments (`Tag`, `Attributes`) are null.  
* The final test confirms that even after appending a very large string, the no‑op HTML methods still produce no output, proving they truly ignore the element’s content.

All tests are written with JUnit 4 (`@Test`, `@Before`, `expected` attribute) and can be run directly with any JUnit‑4 compatible runner.