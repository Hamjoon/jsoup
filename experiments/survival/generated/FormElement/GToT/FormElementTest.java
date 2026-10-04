package org.jsoup.nodes;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.helper.Validate;
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
 *   <li>Public API surface (constructors & methods)</li>
 *   <li>Typical usage scenarios</li>
 *   <li>Edge‑case handling (missing attributes, disabled fields, empty values, etc.)</li>
 *   <li>Exception paths (unable to resolve action URL)</li>
 * </ul>
 */
public class FormElementTest {

    private static final String BASE_URI = "http://example.com/";
    private FormElement form;

    /**
     * Build a form that contains a wide variety of controls.
     *
     * <pre>
     * &lt;form id="testForm" method="post" action="/submit"&gt;
     *   &lt;input type="text" name="txt" value="textValue"&gt;
     *   &lt;input type="checkbox" name="chk" value="chkVal" checked&gt;
     *   &lt;input type="checkbox" name="chk2" value="chk2Val"&gt;               <!-- not checked -->
     *   &lt;input type="radio" name="rad" value="radVal" checked&gt;
     *   &lt;input type="radio" name="rad" value="radVal2"&gt;                  <!-- not checked -->
     *   &lt;input type="text" name="disabled" value="nope" disabled&gt;
     *   &lt;input type="text" value="noName"&gt;                               <!-- no name -->
     *   &lt;select name="sel"&gt;
     *       &lt;option value="opt1"&gt;One&lt;/option&gt;
     *       &lt;option value="opt2" selected&gt;Two&lt;/option&gt;
     *   &lt;/select&gt;
     *   &lt;select name="selNoSel"&gt;
     *       &lt;option value="first"&gt;First&lt;/option&gt;
     *       &lt;option value="second"&gt;Second&lt;/option&gt;
     *   &lt;/select&gt;
     *   &lt;select name="emptySelect"&gt;&lt;/select&gt;                         <!-- no options -->
     * &lt;/form&gt;
     * </pre>
     */
    @Before
    public void setUp() throws Exception {
        String html = "<html><head><title>Test</title></head>"
                + "<body>"
                + "<form id='testForm' method='post' action='/submit'>"
                + "  <input type='text' name='txt' value='textValue'/>"
                + "  <input type='checkbox' name='chk' value='chkVal' checked/>"
                + "  <input type='checkbox' name='chk2' value='chk2Val'/>"
                + "  <input type='radio' name='rad' value='radVal' checked/>"
                + "  <input type='radio' name='rad' value='radVal2'/>"
                + "  <input type='text' name='disabled' value='nope' disabled/>"
                + "  <input type='text' value='noName'/>"
                + "  <select name='sel'>"
                + "     <option value='opt1'>One</option>"
                + "     <option value='opt2' selected>Two</option>"
                + "  </select>"
                + "  <select name='selNoSel'>"
                + "     <option value='first'>First</option>"
                + "     <option value='second'>Second</option>"
                + "  </select>"
                + "  <select name='emptySelect'></select>"
                + "</form>"
                + "</body></html>";

        // Parse with a base URI so that relative action URLs resolve correctly.
        Document doc = Jsoup.parse(html, BASE_URI);
        form = (FormElement) doc.selectFirst("form");
        assertNotNull("Form element should be parsed as FormElement", form);
    }

    /** -----------------------------------------------------------------------
     *  1. Public API extraction – sanity check that the expected public methods exist.
     * ----------------------------------------------------------------------- */
    @Test
    public void testPublicMethodsExistence() {
        // Constructor already exercised in @Before
        assertNotNull(form.elements());
        assertSame(form, form.addElement(new Element(Tag.valueOf("input"), BASE_URI)));
        assertNotNull(form.submit());
        assertNotNull(form.formData());
    }

    /** -----------------------------------------------------------------------
     *  2. elements() – should initially be empty and reflect added elements.
     * ----------------------------------------------------------------------- */
    @Test
    public void testElementsInitiallyEmpty() {
        // The internal Elements collection should start empty because the parser
        // does not automatically populate it.
        assertTrue("Elements collection should start empty", form.elements().isEmpty());
    }

    @Test
    public void testAddElementAddsToElements() {
        Element input = new Element(Tag.valueOf("input"), BASE_URI);
        input.attr("name", "newField").attr("value", "newVal");
        form.addElement(input);
        Elements elems = form.elements();
        assertEquals(1, elems.size());
        assertSame(input, elems.first());
    }

    /** -----------------------------------------------------------------------
     *  3. removeChild (protected) – verify that removing a child also removes it from
     *     the form's internal element list.
     * ----------------------------------------------------------------------- */
    @Test
    public void testRemoveChildUpdatesElements() throws Exception {
        // Subclass that makes removeChild public for testing.
        class TestableForm extends FormElement {
            TestableForm(Tag tag, String baseUri, Attributes attributes) {
                super(tag, baseUri, attributes);
            }

            public void removeChildPublic(Node n) {
                super.removeChild(n);
            }
        }

        TestableForm testForm = new TestableForm(Tag.valueOf("form"), BASE_URI, new Attributes());

        // Add a child element manually and also register it via addElement.
        Element child = new Element(Tag.valueOf("input"), BASE_URI);
        child.attr("name", "child");
        testForm.appendChild(child);          // normal DOM tree
        testForm.addElement(child);           // registers in form's Elements list

        assertEquals(1, testForm.elements().size());

        // Now invoke protected removeChild via the public wrapper.
        testForm.removeChildPublic(child);
        assertEquals(0, testForm.elements().size());
    }

    /** -----------------------------------------------------------------------
     *  4. submit() – normal operation (GET & POST) and action‑URL resolution.
     * ----------------------------------------------------------------------- */
    @Test
    public void testSubmitCreatesConnectionWithCorrectMethodAndAction() {
        Connection connection = form.submit();
        assertNotNull(connection);
        // Verify that the request URL is the absolute URL derived from the form's action.
        String expectedUrl = BASE_URI + "submit";
        assertEquals(expectedUrl, connection.request().url().toString());

        // Form's method attribute is "post" → should be POST.
        assertEquals(Connection.Method.POST, connection.request().method());

        // Verify that the request data contains the expected key‑value pairs.
        List<Connection.KeyVal> data = connection.request().data();
        // Extract keys for easier assertion.
        assertTrue(data.stream().anyMatch(kv -> kv.key().equals("txt") && kv.value().equals("textValue")));
        assertTrue(data.stream().anyMatch(kv -> kv.key().equals("chk") && kv.value().equals("chkVal")));
        assertTrue(data.stream().anyMatch(kv -> kv.key().equals("rad") && kv.value().equals("radVal")));
        assertTrue(data.stream().anyMatch(kv -> kv.key().equals("sel") && kv.value().equals("opt2")));
        assertTrue(data.stream().anyMatch(kv -> kv.key().equals("selNoSel") && kv.value().equals("first")));
        // emptySelect has no options → should not appear.
        assertFalse(data.stream().anyMatch(kv -> kv.key().equals("emptySelect")));
        // Disabled and nameless inputs must be omitted.
        assertFalse(data.stream().anyMatch(kv -> kv.key().equals("disabled")));
        assertFalse(data.stream().anyMatch(kv -> kv.key().isEmpty()));
    }

    @Test
    public void testSubmitDefaultsToGETWhenMethodMissing() {
        // Clone the form and remove the method attribute.
        FormElement getForm = (FormElement) form.clone();
        getForm.removeAttr("method");
        Connection conn = getForm.submit();
        assertEquals(Connection.Method.GET, conn.request().method());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubmitThrowsWhenActionCannotBeDetermined() {
        // Construct a form with no base URI and no action attribute.
        FormElement badForm = new FormElement(Tag.valueOf("form"), "", new Attributes());
        // No action attribute set → baseUri is empty, triggering validation failure.
        badForm.submit(); // should throw IllegalArgumentException
    }

    /** -----------------------------------------------------------------------
     *  5. formData() – exhaustive coverage of input types, edge cases and defaults.
     * ----------------------------------------------------------------------- */
    @Test
    public void testFormDataCollectsAllSupportedControls() {
        List<Connection.KeyVal> data = form.formData();

        // Helper to find a key‑val pair.
        java.util.function.Predicate<Connection.KeyVal> kv(String key) {
            return kv -> kv.key().equals(key);
        }

        // Text input
        assertTrue(data.stream().anyMatch(kv("txt").and(kv -> kv.value().equals("textValue"))));

        // Checkbox – only the checked one should appear, with its explicit value.
        assertTrue(data.stream().anyMatch(kv("chk").and(kv -> kv.value().equals("chkVal"))));
        assertFalse(data.stream().anyMatch(kv("chk2")));

        // Radio – only the checked one.
        assertTrue(data.stream().anyMatch(kv("rad").and(kv -> kv.value().equals("radVal"))));

        // Disabled input must be ignored.
        assertFalse(data.stream().anyMatch(kv("disabled")));

        // Input without a name must be ignored.
        assertFalse(data.stream().anyMatch(kv("")));

        // Select with an explicitly selected option.
        assertTrue(data.stream().anyMatch(kv("sel").and(kv -> kv.value().equals("opt2"))));

        // Select without a selected option – should fall back to the first option.
        assertTrue(data.stream().anyMatch(kv("selNoSel").and(kv -> kv.value().equals("first"))));

        // Empty select – should not contribute any entry.
        assertFalse(data.stream().anyMatch(kv("emptySelect")));
    }

    @Test
    public void testFormDataCheckboxWithoutValueDefaultsToOn() {
        // Build a minimal form containing a single checked checkbox without a value attribute.
        String html = "<form>"
                + "<input type='checkbox' name='agree' checked/>"
                + "</form>";
        Document doc = Jsoup.parse(html, BASE_URI);
        FormElement chkForm = (FormElement) doc.selectFirst("form");
        List<Connection.KeyVal> data = chkForm.formData();
        assertEquals(1, data.size());
        Connection.KeyVal kv = data.get(0);
        assertEquals("agree", kv.key());
        assertEquals("on", kv.value()); // default per implementation
    }

    @Test
    public void testFormDataSelectWithNoOptionsProducesNoEntry() {
        String html = "<form>"
                + "<select name='empty'></select>"
                + "</form>";
        Document doc = Jsoup.parse(html, BASE_URI);
        FormElement emptySelectForm = (FormElement) doc.selectFirst("form");
        List<Connection.KeyVal> data = emptySelectForm.formData();
        assertTrue("Select with no options should not generate a key‑val pair", data.isEmpty());
    }

    @Test
    public void testFormDataMultipleSelectedOptions() {
        String html = "<form>"
                + "<select name='multi' multiple>"
                + "  <option value='a' selected>A</option>"
                + "  <option value='b' selected>B</option>"
                + "  <option value='c'>C</option>"
                + "</select>"
                + "</form>";
        Document doc = Jsoup.parse(html, BASE_URI);
        FormElement multiForm = (FormElement) doc.selectFirst("form");
        List<Connection.KeyVal> data = multiForm.formData();

        // Both selected options should be present as separate key‑val entries.
        assertEquals(2, data.size());
        assertTrue(data.stream().anyMatch(kv -> kv.key().equals("multi") && kv.value().equals("a")));
        assertTrue(data.stream().anyMatch(kv -> kv.key().equals("multi") && kv.value().equals("b")));
    }
}
