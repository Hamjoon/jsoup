package org.jsoup.nodes;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link FormElement}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction and basic getters.</li>
 *   <li>addElement chaining and internal Elements collection.</li>
 *   <li>removeChild interaction with the internal Elements list.</li>
 *   <li>submit() – URL resolution, HTTP method selection and validation of missing action/baseUri.</li>
 *   <li>formData() – handling of all supported control types, disabled fields, missing names,
 *       default values for checkboxes/radios, and select elements with/without a selected option.</li>
 * </ul>
 */
public class FormElementTest {

    private FormElement form;
    private static final String BASE_URI = "http://example.com/page.html";

    @Before
    public void setUp() {
        // Create a <form> element with a base URI – the Tag must be a form tag.
        form = new FormElement(Tag.valueOf("form"), BASE_URI, new Attributes());
    }

    /** -----------------------------------------------------------------------
     *  1. elements() – should return the internal Elements instance (initially empty)
     *  --------------------------------------------------------------------- */
    @Test
    public void testElementsInitiallyEmpty() {
        Elements els = form.elements();
        assertNotNull("elements() must never return null", els);
        assertTrue("New FormElement should have no associated controls", els.isEmpty());
    }

    /** -----------------------------------------------------------------------
     *  2. addElement() – should add the element, return the same FormElement
     *  --------------------------------------------------------------------- */
    @Test
    public void testAddElementChainingAndPresence() {
        Element input = new Element(Tag.valueOf("input"), "", new Attributes().put("name", "username"));
        FormElement returned = form.addElement(input);
        assertSame("addElement should return the same FormElement for chaining", form, returned);
        assertEquals("Element should be stored in the internal list", 1, form.elements().size());
        assertSame("Stored element must be the one we added", input, form.elements().first());
    }

    /** -----------------------------------------------------------------------
     *  3. removeChild() – when a child node is removed it must also be removed
     *     from the FormElement's internal Elements collection.
     *  --------------------------------------------------------------------- */
    @Test
    public void testRemoveChildUpdatesElementsList() {
        // add a child element to the DOM tree and to the form's control list
        Element input = new Element(Tag.valueOf("input"), "", new Attributes().put("name", "email"));
        form.appendChild(input);          // registers as a child node
        form.addElement(input);           // registers as a form control

        // sanity check
        assertEquals(1, form.elements().size());

        // invoke protected removeChild via a subclass (or reflection)
        // Since removeChild is protected we can create an anonymous subclass to expose it.
        FormElement exposable = new FormElement(Tag.valueOf("form"), BASE_URI, new Attributes()) {
            @Override
            public FormElement addElement(Element element) {
                super.addElement(element);
                return this;
            }

            public void exposeRemoveChild(Node n) {
                super.removeChild(n);
            }
        };
        exposable.appendChild(input);
        exposable.addElement(input);
        assertEquals(1, exposable.elements().size());

        exposable.exposeRemoveChild(input);
        assertTrue("After removal the internal elements list must be empty", exposable.elements().isEmpty());
    }

    /** -----------------------------------------------------------------------
     *  4. submit() – successful GET submission (default method)
     *  --------------------------------------------------------------------- */
    @Test
    public void testSubmitCreatesGetConnectionWithActionFromAttribute() {
        form.attr("action", "/search");
        // No explicit method attribute → defaults to GET
        Connection conn = form.submit();

        // The connection is a real HttpConnection; we can inspect its request.
        assertEquals("http://example.com/search", conn.request().url().toString());
        assertEquals(Connection.Method.GET, conn.request().method());
        // No data yet – formData() is empty because we added no controls.
        assertTrue("GET request should have empty data when no controls are present", conn.request().data().isEmpty());
    }

    /** -----------------------------------------------------------------------
     *  5. submit() – successful POST submission when method attribute is POST
     *  --------------------------------------------------------------------- */
    @Test
    public void testSubmitCreatesPostConnectionWhenMethodIsPost() {
        form.attr("action", "http://example.org/submit");
        form.attr("method", "POST");
        Connection conn = form.submit();

        assertEquals("http://example.org/submit", conn.request().url().toString());
        assertEquals(Connection.Method.POST, conn.request().method());
    }

    /** -----------------------------------------------------------------------
     *  6. submit() – throws when action cannot be resolved and baseUri is empty
     *  --------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testSubmitThrowsWhenActionAndBaseUriMissing() {
        // Create a form with no base URI and no action attribute.
        FormElement lonely = new FormElement(Tag.valueOf("form"), "", new Attributes());
        lonely.submit(); // should throw
    }

    /** -----------------------------------------------------------------------
     *  7. formData() – comprehensive data extraction
     *
     *  The form contains:
     *   * a text input (name=txt, value=hello)
     *   * a disabled input (should be ignored)
     *   * an input without name (ignored)
     *   * a checked checkbox (name=chk, value=on)
     *   * an unchecked checkbox (ignored)
     *   * a checked radio (name=rad, value=choice1)
     *   * an unchecked radio (ignored)
     *   * a select with a selected option (name=sel, selected value=opt2)
     *   * a select without any selected option (name=sel2, first option value=optA)
     *   * a textarea (name=area, value=textarea)
     *  --------------------------------------------------------------------- */
    @Test
    public void testFormDataCollectsAllSupportedControls() {
        // 1. Text input
        Element txt = new Element(Tag.valueOf("input"), "", new Attributes()
                .put("type", "text")
                .put("name", "txt")
                .put("value", "hello"));
        form.addElement(txt);

        // 2. Disabled input (should be skipped)
        Element disabled = new Element(Tag.valueOf("input"), "", new Attributes()
                .put("type", "text")
                .put("name", "disabled")
                .put("value", "nope")
                .put("disabled", "disabled"));
        form.addElement(disabled);

        // 3. Input without name (skipped)
        Element noName = new Element(Tag.valueOf("input"), "", new Attributes()
                .put("type", "text")
                .put("value", "ghost"));
        form.addElement(noName);

        // 4. Checked checkbox (value defaults to "on")
        Element chkChecked = new Element(Tag.valueOf("input"), "", new Attributes()
                .put("type", "checkbox")
                .put("name", "chk")
                .put("checked", "checked"));
        form.addElement(chkChecked);

        // 5. Unchecked checkbox (skipped)
        Element chkUnchecked = new Element(Tag.valueOf("input"), "", new Attributes()
                .put("type", "checkbox")
                .put("name", "chk2"));
        form.addElement(chkUnchecked);

        // 6. Checked radio
        Element radChecked = new Element(Tag.valueOf("input"), "", new Attributes()
                .put("type", "radio")
                .put("name", "rad")
                .put("value", "choice1")
                .put("checked", "checked"));
        form.addElement(radChecked);

        // 7. Unchecked radio (skipped)
        Element radUnchecked = new Element(Tag.valueOf("input"), "", new Attributes()
                .put("type", "radio")
                .put("name", "rad")
                .put("value", "choice2"));
        form.addElement(radUnchecked);

        // 8. Select with a selected option
        Element select = new Element(Tag.valueOf("select"), "", new Attributes().put("name", "sel"));
        Element opt1 = new Element(Tag.valueOf("option"), "", new Attributes().put("value", "opt1"));
        Element opt2 = new Element(Tag.valueOf("option"), "", new Attributes().put("value", "opt2").put("selected", "selected"));
        select.appendChild(opt1);
        select.appendChild(opt2);
        form.addElement(select);

        // 9. Select without any selected option – first option should be used
        Element selectNoSel = new Element(Tag.valueOf("select"), "", new Attributes().put("name", "sel2"));
        Element optA = new Element(Tag.valueOf("option"), "", new Attributes().put("value", "optA"));
        Element optB = new Element(Tag.valueOf("option"), "", new Attributes().put("value", "optB"));
        selectNoSel.appendChild(optA);
        selectNoSel.appendChild(optB);
        form.addElement(selectNoSel);

        // 10. Textarea (treated like a generic element – value is its text)
        Element textarea = new Element(Tag.valueOf("textarea"), "", new Attributes().put("name", "area"));
        textarea.text("textarea");
        form.addElement(textarea);

        // Retrieve form data
        List<Connection.KeyVal> data = form.formData();

        // Helper to find a key in the list
        java.util.function.Predicate<Connection.KeyVal> hasKey(String key) {
            return kv -> kv.key().equals(key);
        }

        // Expected size: txt, chk, rad, sel, sel2, area  => 6 entries
        assertEquals("Form data should contain exactly 6 key‑value pairs", 6, data.size());

        // Validate each expected entry
        assertTrue("txt should be present", data.stream().anyMatch(kv -> kv.key().equals("txt") && kv.value().equals("hello")));
        assertTrue("checked checkbox should be present with default 'on'", data.stream().anyMatch(kv -> kv.key().equals("chk") && kv.value().equals("on")));
        assertTrue("checked radio should be present", data.stream().anyMatch(kv -> kv.key().equals("rad") && kv.value().equals("choice1")));
        assertTrue("select with selected option should use that option", data.stream().anyMatch(kv -> kv.key().equals("sel") && kv.value().equals("opt2")));
        assertTrue("select without selected option should fall back to first option", data.stream().anyMatch(kv -> kv.key().equals("sel2") && kv.value().equals("optA")));
        assertTrue("textarea value should be captured", data.stream().anyMatch(kv -> kv.key().equals("area") && kv.value().equals("textarea")));
    }

    /** -----------------------------------------------------------------------
     *  8. formData() – ensures that a checkbox with a custom value respects that value.
     *  --------------------------------------------------------------------- */
    @Test
    public void testCheckboxCustomValueIsUsedWhenChecked() {
        Element chk = new Element(Tag.valueOf("input"), "", new Attributes()
                .put("type", "checkbox")
                .put("name", "agree")
                .put("value", "yes")
                .put("checked", "checked"));
        form.addElement(chk);
        List<Connection.KeyVal> data = form.formData();
        assertEquals(1, data.size());
        Connection.KeyVal kv = data.get(0);
        assertEquals("agree", kv.key());
        assertEquals("yes", kv.value());
    }

    /** -----------------------------------------------------------------------
     *  9. formData() – verify that a select element with multiple selected options
     *     adds each selected option as a separate entry.
     *  --------------------------------------------------------------------- */
    @Test
    public void testSelectMultipleSelectedOptions() {
        Element select = new Element(Tag.valueOf("select"), "", new Attributes().put("name", "multi"));
        Element opt1 = new Element(Tag.valueOf("option"), "", new Attributes().put("value", "A").put("selected", "selected"));
        Element opt2 = new Element(Tag.valueOf("option"), "", new Attributes().put("value", "B").put("selected", "selected"));
        Element opt3 = new Element(Tag.valueOf("option"), "", new Attributes().put("value", "C"));
        select.appendChild(opt1);
        select.appendChild(opt2);
        select.appendChild(opt3);
        form.addElement(select);

        List<Connection.KeyVal> data = form.formData();
        // Two entries expected for the same name
        assertEquals(2, data.size());
        assertTrue(data.stream().anyMatch(kv -> kv.key().equals("multi") && kv.value().equals("A")));
        assertTrue(data.stream().anyMatch(kv -> kv.key().equals("multi") && kv.value().equals("B")));
    }

    /** -----------------------------------------------------------------------
     *  10. submit() – verify that when no "action" attribute is present the form's
     *      base URI is used as the target URL.
     *  --------------------------------------------------------------------- */
    @Test
    public void testSubmitUsesBaseUriWhenActionMissing() {
        // No "action" attribute -> fallback to base URI supplied at construction.
        Connection conn = form.submit();
        assertEquals(BASE_URI, conn.request().url().toString());
        assertEquals(Connection.Method.GET, conn.request().method());
    }
}
