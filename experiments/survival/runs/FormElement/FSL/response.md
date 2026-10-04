###Test START##
package org.jsoup.nodes;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.helper.Validate;
import org.jsoup.parser.Tag;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class FormElementTest {

    /** Helper to create a generic element with given tag name and attributes */
    private Element createElement(String tagName, String name, String type, String value, boolean checked, boolean disabled, boolean selected) {
        Tag tag = Tag.valueOf(tagName);
        Element el = new Element(tag, "", new Attributes());
        if (name != null) el.attr("name", name);
        if (type != null) el.attr("type", type);
        if (value != null) el.attr("value", value);
        if (checked) el.attr("checked", "checked");
        if (disabled) el.attr("disabled", "disabled");
        if (selected) el.attr("selected", "selected");
        return el;
    }

    @Test
    public void testAddAndRetrieveElements() {
        FormElement form = new FormElement(Tag.valueOf("form"), "http://example.com", new Attributes());

        Element input = createElement("input", "username", "text", "john", false, false, false);
        Element select = createElement("select", "country", null, null, false, false, false);
        Element option = createElement("option", null, null, "US", false, false, true);
        select.appendChild(option);

        form.addElement(input).addElement(select);

        Elements elems = form.elements();
        assertEquals(2, elems.size());
        assertTrue(elems.contains(input));
        assertTrue(elems.contains(select));
    }

    @Test
    public void testRemoveChildUpdatesElementsList() {
        FormElement form = new FormElement(Tag.valueOf("form"), "http://example.com", new Attributes());

        Element input = createElement("input", "email", "email", "a@b.com", false, false, false);
        form.addElement(input);
        assertEquals(1, form.elements().size());

        // remove via Element's remove() which calls parentNode.removeChild()
        input.remove(); // this will invoke FormElement.removeChild internally
        assertEquals(0, form.elements().size());
    }

    @Test
    public void testFormDataBasicInputs() {
        FormElement form = new FormElement(Tag.valueOf("form"), "http://example.com", new Attributes());

        // normal text input
        Element txt = createElement("input", "firstName", "text", "John", false, false, false);
        // disabled input should be ignored
        Element disabled = createElement("input", "secret", "text", "top", false, true, false);
        // input without name should be ignored
        Element noName = createElement("input", null, "text", "value", false, false, false);
        // checkbox unchecked (ignored)
        Element uncheckedBox = createElement("input", "subscribe", "checkbox", "yes", false, false, false);
        // checkbox checked with explicit value
        Element checkedBox = createElement("input", "agree", "checkbox", "yes", true, false, false);
        // checkbox checked without value (defaults to "on")
        Element checkedBoxNoVal = createElement("input", "terms", "checkbox", "", true, false, false);
        // radio checked
        Element radio = createElement("input", "gender", "radio", "male", true, false, false);
        // radio unchecked (ignored)
        Element radioUnchecked = createElement("input", "gender", "radio", "female", false, false, false);
        // non-submittable element (div) should be ignored
        Element div = createElement("div", "ignored", null, null, false, false, false);

        form.addElement(txt)
                .addElement(disabled)
                .addElement(noName)
                .addElement(uncheckedBox)
                .addElement(checkedBox)
                .addElement(checkedBoxNoVal)
                .addElement(radio)
                .addElement(radioUnchecked)
                .addElement(div);

        List<Connection.KeyVal> data = form.formData();

        // Expected entries: firstName, agree, terms (as "on"), gender
        assertEquals(4, data.size());

        assertTrue(data.stream().anyMatch(kv -> kv.key().equals("firstName") && kv.value().equals("John")));
        assertTrue(data.stream().anyMatch(kv -> kv.key().equals("agree") && kv.value().equals("yes")));
        assertTrue(data.stream().anyMatch(kv -> kv.key().equals("terms") && kv.value().equals("on")));
        assertTrue(data.stream().anyMatch(kv -> kv.key().equals("gender") && kv.value().equals("male")));
    }

    @Test
    public void testFormDataSelectWithSelectedOption() {
        FormElement form = new FormElement(Tag.valueOf("form"), "http://example.com", new Attributes());

        Element select = createElement("select", "city", null, null, false, false, false);
        Element opt1 = createElement("option", null, null, "NY", false, false, false);
        Element opt2 = createElement("option", null, null, "LA", false, false, true); // selected
        Element opt3 = createElement("option", null, null, "SF", false, false, false);
        select.appendChild(opt1);
        select.appendChild(opt2);
        select.appendChild(opt3);

        form.addElement(select);
        List<Connection.KeyVal> data = form.formData();

        assertEquals(1, data.size());
        Connection.KeyVal kv = data.get(0);
        assertEquals("city", kv.key());
        assertEquals("LA", kv.value());
    }

    @Test
    public void testFormDataSelectWithoutSelectedUsesFirstOption() {
        FormElement form = new FormElement(Tag.valueOf("form"), "http://example.com", new Attributes());

        Element select = createElement("select", "fruit", null, null, false, false, false);
        Element opt1 = createElement("option", null, null, "Apple", false, false, false);
        Element opt2 = createElement("option", null, null, "Banana", false, false, false);
        select.appendChild(opt1);
        select.appendChild(opt2);

        form.addElement(select);
        List<Connection.KeyVal> data = form.formData();

        assertEquals(1, data.size());
        Connection.KeyVal kv = data.get(0);
        assertEquals("fruit", kv.key());
        assertEquals("Apple", kv.value());
    }

    @Test
    public void testSubmitCreatesConnectionWithCorrectMethodAndData() {
        FormElement form = new FormElement(Tag.valueOf("form"), "http://example.com/base", new Attributes());
        form.attr("action", "/submit");
        form.attr("method", "post");

        // add a simple input
        Element txt = createElement("input", "q", "text", "jsoup", false, false, false);
        form.addElement(txt);

        Connection conn = form.submit();
        // Action URL should be resolved against baseUri
        assertEquals("http://example.com/submit", conn.request().url().toString());

        // Method should be POST
        assertEquals(Connection.Method.POST, conn.request().method());

        // Data should contain the input
        List<Connection.KeyVal> data = conn.request().data();
        assertEquals(1, data.size());
        Connection.KeyVal kv = data.get(0);
        assertEquals("q", kv.key());
        assertEquals("jsoup", kv.value());
    }

    @Test
    public void testSubmitFallsBackToBaseUriWhenNoAction() {
        FormElement form = new FormElement(Tag.valueOf("form"), "http://example.com/base", new Attributes());
        // No action attribute, method defaults to GET
        form.attr("method", "GET");
        Element txt = createElement("input", "search", "text", "test", false, false, false);
        form.addElement(txt);

        Connection conn = form.submit();
        assertEquals("http://example.com/base", conn.request().url().toString());
        assertEquals(Connection.Method.GET, conn.request().method());
        assertEquals("test", conn.request().data("search").value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubmitThrowsWhenActionCannotBeResolved() {
        // baseUri is empty and no action attribute -> Validate.notEmpty should fire
        FormElement form = new FormElement(Tag.valueOf("form"), "", new Attributes());
        // No action attribute set
        form.submit(); // should throw
    }
}
###Test END##