package org.jsoup.nodes;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.helper.Validate;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

/**
 * JUnit‑4 test suite for {@link FormElement}.
 *
 * The tests cover:
 * <ul>
 *   <li>construction and basic element handling (addElement, elements, removeChild)</li>
 *   <li>the {@code submit()} method – URL resolution, HTTP method selection and error handling</li>
 *   <li>the {@code formData()} method – handling of all supported control types,
 *       disabled fields, missing names, default values and select fallback logic</li>
 * </ul>
 */
public class FormElementTest {

    /** Helper to create a generic {@link Element} with a given tag name. */
    private static Element newElement(String tagName) {
        return new Element(Tag.valueOf(tagName), "", new Attributes());
    }

    /** Helper to create an <input> element with the requested attributes. */
    private static Element input(String type, String name, String value,
                                 boolean checked, boolean disabled) {
        Element el = newElement("input");
        el.attr("type", type);
        if (name != null) el.attr("name", name);
        if (value != null) el.attr("value", value);
        if (checked) el.attr("checked", "checked");
        if (disabled) el.attr("disabled", "disabled");
        return el;
    }

    /** Helper to create a <select> element with a list of <option> children.
     *  The {@code selectedOptionValue} may be {@code null} to indicate that
     *  no option is pre‑selected. */
    private static Element select(String name, String... options) {
        // options format: "value|selected" where selected is optional and equals "selected"
        Element sel = newElement("select");
        if (name != null) sel.attr("name", name);
        for (String optSpec : options) {
            String[] parts = optSpec.split("\\|", 2);
            String value = parts[0];
            boolean selected = parts.length > 1 && "selected".equalsIgnoreCase(parts[1]);

            Element opt = newElement("option");
            opt.attr("value", value);
            opt.text(value); // fallback text
            if (selected) opt.attr("selected", "selected");
            sel.appendChild(opt);
        }
        return sel;
    }

    /** Exposes the protected {@code removeChild(Node)} method via reflection. */
    private void invokeRemoveChild(FormElement form, Node child) throws Exception {
        Method m = FormElement.class.getDeclaredMethod("removeChild", Node.class);
        m.setAccessible(true);
        m.invoke(form, child);
    }

    /* --------------------------------------------------------------------- *
     *  Basic element handling
     * --------------------------------------------------------------------- */

    @Test
    public void testAddElementAndElementsList() {
        FormElement form = new FormElement(Tag.valueOf("form"), "http://example.com", new Attributes());
        Element input = input("text", "username", "john", false, false);
        form.addElement(input);

        Elements elems = form.elements();
        Assert.assertEquals(1, elems.size());
        Assert.assertSame(input, elems.first());
    }

    @Test
    public void testRemoveChildUpdatesElementsCollection() throws Exception {
        FormElement form = new FormElement(Tag.valueOf("form"), "http://example.com", new Attributes());

        Element input1 = input("text", "first", "a", false, false);
        Element input2 = input("text", "second", "b", false, false);
        form.addElement(input1).addElement(input2);

        // verify both are present
        Assert.assertEquals(2, form.elements().size());

        // remove the first child via the protected method
        invokeRemoveChild(form, input1);

        // the internal Elements collection must have been updated
        Elements remaining = form.elements();
        Assert.assertEquals(1, remaining.size());
        Assert.assertSame(input2, remaining.first());
    }

    /* --------------------------------------------------------------------- *
     *  submit()
     * --------------------------------------------------------------------- */

    @Test
    public void testSubmitDefaultsToGetAndUsesBaseUriWhenNoAction() {
        FormElement form = new FormElement(Tag.valueOf("form"), "http://example.com/base", new Attributes());
        // No explicit action attribute; method defaults to GET
        Connection conn = form.submit();

        // Jsoup.connect returns an HttpConnection – we can safely cast
        HttpConnection httpConn = (HttpConnection) conn;
        Connection.Request req = httpConn.request();

        Assert.assertEquals("http://example.com/base", req.url().toString());
        Assert.assertEquals(Connection.Method.GET, req.method());
    }

    @Test
    public void testSubmitUsesActionAttributeAndPostMethod() {
        FormElement form = new FormElement(Tag.valueOf("form"), "http://example.com/base", new Attributes());
        form.attr("action", "/submit");
        form.attr("method", "post");

        Connection conn = form.submit();
        HttpConnection httpConn = (HttpConnection) conn;
        Connection.Request req = httpConn.request();

        Assert.assertEquals("http://example.com/submit", req.url().toString());
        Assert.assertEquals(Connection.Method.POST, req.method());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubmitThrowsWhenActionCannotBeResolved() {
        // No base URI and no action attribute – Validate.notEmpty will fire
        FormElement form = new FormElement(Tag.valueOf("form"), "", new Attributes());
        form.submit(); // should throw
    }

    /* --------------------------------------------------------------------- *
     *  formData()
     * --------------------------------------------------------------------- */

    @Test
    public void testFormDataCollectsVariousControlTypes() {
        FormElement form = new FormElement(Tag.valueOf("form"), "http://example.com", new Attributes());

        // text input
        form.addElement(input("text", "username", "john", false, false));
        // password input
        form.addElement(input("password", "pwd", "secret", false, false));
        // checked checkbox with explicit value
        form.addElement(input("checkbox", "subscribe", "yes", true, false));
        // checked checkbox with empty value – should default to "on"
        form.addElement(input("checkbox", "agree", "", true, false));
        // unchecked checkbox – must be ignored
        form.addElement(input("checkbox", "ignoreMe", "no", false, false));
        // disabled input – must be ignored
        form.addElement(input("text", "disabled", "won't appear", false, true));
        // radio group – only checked one appears
        form.addElement(input("radio", "gender", "male", true, false));
        form.addElement(input("radio", "gender", "female", false, false));
        // input without name – ignored
        form.addElement(input("text", null, "noName", false, false));

        // select with a pre‑selected option
        form.addElement(select("city", "NY|selected", "LA"));
        // select without any selected option – first option should be used
        form.addElement(select("country", "US", "CA"));

        List<Connection.KeyVal> data = form.formData();

        // Build a simple map for easier assertions
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        for (Connection.KeyVal kv : data) {
            map.put(kv.key(), kv.value());
        }

        // Expected key‑value pairs
        Assert.assertEquals("john", map.get("username"));
        Assert.assertEquals("secret", map.get("pwd"));
        Assert.assertEquals("yes", map.get("subscribe"));
        Assert.assertEquals("on", map.get("agree"));
        Assert.assertEquals("male", map.get("gender"));
        Assert.assertEquals("NY", map.get("city"));
        Assert.assertEquals("US", map.get("country"));

        // Ensure no unexpected keys are present
        Assert.assertEquals(7, map.size());
    }

    @Test
    public void testFormDataSelectWithoutOptionsYieldsNoEntry() {
        FormElement form = new FormElement(Tag.valueOf("form"), "http://example.com", new Attributes());
        // create a <select name="emptySelect"></select> (no <option> children)
        Element emptySelect = newElement("select");
        emptySelect.attr("name", "emptySelect");
        form.addElement(emptySelect);

        List<Connection.KeyVal> data = form.formData();
        // The list should be empty because there is nothing to submit
        Assert.assertTrue(data.isEmpty());
    }

    @Test
    public void testFormDataReturnsCopyNotLiveReference() {
        FormElement form = new FormElement(Tag.valueOf("form"), "http://example.com", new Attributes());
        form.addElement(input("text", "field", "value", false, false));

        List<Connection.KeyVal> first = form.formData();
        // Mutate the returned list
        first.clear();

        // A fresh call must still return the original data
        List<Connection.KeyVal> second = form.formData();
        Assert.assertEquals(1, second.size());
        Assert.assertEquals("field", second.get(0).key());
        Assert.assertEquals("value", second.get(0).value());
    }
}
