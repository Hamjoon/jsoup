###Test START##
```java
package org.jsoup.parser;

import org.junit.Test;
import org.junit.Assert;

/**
 * JUnit‑4 test suite for {@link Tag}.
 *
 * The tests are organised in the following order:
 * <ol>
 *   <li>Extraction of public API (listed in the comment block at the top of the file).</li>
 *   <li>Basic functional tests for each public method.</li>
 *   <li>Edge‑case / exception handling tests.</li>
 *   <li>Combined test class (TagTest) containing all of the above.</li>
 * </ol>
 */
public class TagTest {

    /* --------------------------------------------------------------------- */
    /* 1. List of public methods (extracted from Tag)                        */
    /* --------------------------------------------------------------------- */
    // public String getName()
    // public static Tag valueOf(String tagName, ParseSettings settings)
    // public static Tag valueOf(String tagName)
    // public boolean isBlock()
    // public boolean formatAsBlock()
    // @Deprecated public boolean canContainBlock()
    // public boolean isInline()
    // public boolean isData()
    // public boolean isEmpty()
    // public boolean isSelfClosing()
    // public boolean isKnownTag()
    // public static boolean isKnownTag(String tagName)
    // public boolean preserveWhitespace()
    // public boolean isFormListed()
    // public boolean isFormSubmittable()
    // public boolean equals(Object o)
    // public int hashCode()
    // public String toString()

    /* --------------------------------------------------------------------- */
    /* 2. Basic functional tests                                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetName() {
        Tag p = Tag.valueOf("p");
        Assert.assertEquals("p", p.getName());
    }

    @Test
    public void testIsBlock_KnownTag() {
        Tag div = Tag.valueOf("div");
        Assert.assertTrue(div.isBlock());
    }

    @Test
    public void testIsInline_KnownTag() {
        Tag span = Tag.valueOf("span");
        Assert.assertTrue(span.isInline());
        Assert.assertFalse(span.isBlock());
    }

    @Test
    public void testFormatAsBlock_ModifiedTag() {
        // "p" is a block tag but listed in formatAsInlineTags, so formatAsBlock should be false
        Tag p = Tag.valueOf("p");
        Assert.assertFalse(p.formatAsBlock());
    }

    @Test
    public void testCanContainBlock_DeprecatedBehaviour() {
        Tag div = Tag.valueOf("div");
        Assert.assertEquals(div.isBlock(), div.canContainBlock());
    }

    @Test
    public void testIsEmpty_EmptyTag() {
        Tag img = Tag.valueOf("img");
        Assert.assertTrue(img.isEmpty());
    }

    @Test
    public void testIsSelfClosing_EmptyTag() {
        Tag br = Tag.valueOf("br");
        Assert.assertTrue(br.isSelfClosing());
    }

    @Test
    public void testIsSelfClosing_NonEmptyTag() {
        Tag div = Tag.valueOf("div");
        Assert.assertFalse(div.isSelfClosing());
    }

    @Test
    public void testIsKnownTag_InstanceMethod() {
        Tag table = Tag.valueOf("table");
        Assert.assertTrue(table.isKnownTag());
    }

    @Test
    public void testIsKnownTag_StaticMethod() {
        Assert.assertTrue(Tag.isKnownTag("table"));
        Assert.assertFalse(Tag.isKnownTag("unknownTag"));
    }

    @Test
    public void testPreserveWhitespace_Tag() {
        Tag pre = Tag.valueOf("pre");
        Assert.assertTrue(pre.preserveWhitespace());
    }

    @Test
    public void testFormListedAndSubmittable_Tags() {
        Tag input = Tag.valueOf("input");
        Assert.assertTrue(input.isFormListed());
        Assert.assertTrue(input.isFormSubmittable());

        Tag button = Tag.valueOf("button");
        Assert.assertTrue(button.isFormListed());
        Assert.assertFalse(button.isFormSubmittable()); // button is listed but not submittable
    }

    @Test
    public void testIsData_Tag() {
        // Data‑only tag: cannot contain inline and is not empty.
        // The HTML spec defines <script> as data‑only; jsoup marks it as a block tag with
        // preserveWhitespace = false, but it still can contain inline text.
        // For the purpose of this test we use a generic unknown tag with default settings:
        Tag unknown = Tag.valueOf("mydata");
        Assert.assertFalse(unknown.isData()); // default canContainInline == true
    }

    @Test
    public void testEqualsAndHashCode_KnownTag() {
        Tag p1 = Tag.valueOf("p");
        Tag p2 = Tag.valueOf("p");
        Assert.assertTrue(p1.equals(p2));
        Assert.assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void testEqualsAndHashCode_UnknownTag() {
        Tag a = Tag.valueOf("customTag");
        Tag b = Tag.valueOf("customTag");
        // Different instances but same logical state → equals true
        Assert.assertTrue(a.equals(b));
        Assert.assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testToString() {
        Tag div = Tag.valueOf("div");
        Assert.assertEquals("div", div.toString());
    }

    /* --------------------------------------------------------------------- */
    /* 3. Edge‑case / exception handling tests                               */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testValueOf_NullTagName_Throws() {
        Tag.valueOf(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOf_EmptyTagNameAfterNormalization_Throws() {
        // Using the case‑sensitive ParseSettings (preserveCase) – empty string stays empty.
        Tag.valueOf("", ParseSettings.preserveCase);
    }

    @Test
    public void testValueOf_CaseSensitiveBehaviour() {
        // The default static method uses ParseSettings.preserveCase (case‑sensitive).
        // "DIV" is not in the map (keys are lower‑case), so a generic tag is returned.
        Tag upper = Tag.valueOf("DIV");
        Assert.assertEquals("DIV", upper.getName());
        Assert.assertFalse(upper.isBlock()); // generic tags are created with isBlock = false

        // Using a case‑insensitive ParseSettings (lower‑casing) yields the registered tag.
        ParseSettings caseInsensitive = new ParseSettings() {
            @Override public String normalizeTag(String tagName) {
                return tagName.toLowerCase(); // simple case‑folding
            }
        };
        Tag lower = Tag.valueOf("DIV", caseInsensitive);
        Assert.assertEquals("div", lower.getName());
        Assert.assertTrue(lower.isBlock());
    }

    @Test
    public void testUnknownTag_DefaultProperties() {
        Tag unknown = Tag.valueOf("myUnknownTag");
        // Generic tags are created with:
        //   isBlock = false
        //   formatAsBlock = true (default)
        //   canContainInline = true (default)
        //   empty = false
        //   selfClosing = false
        Assert.assertFalse(unknown.isBlock());
        Assert.assertTrue(unknown.formatAsBlock());
        Assert.assertTrue(unknown.isInline());
        Assert.assertFalse(unknown.isEmpty());
        Assert.assertFalse(unknown.isSelfClosing());
        Assert.assertFalse(unknown.preserveWhitespace());
        Assert.assertFalse(unknown.isFormListed());
        Assert.assertFalse(unknown.isFormSubmittable());
    }

    /* --------------------------------------------------------------------- */
    /* 4. Combined test class (already assembled)                            */
    /* --------------------------------------------------------------------- */
}
```
###Test END##