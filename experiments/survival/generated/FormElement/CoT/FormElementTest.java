package org.jsoup.nodes;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.helper.Validate;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class FormElementTest {

    private FormElement form;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // create a bare <form> element with a base URI – the base URI is needed for submit() when no action attribute
        Tag formTag = Tag.valueOf("form");
        Attributes attrs = new Attributes();
        form = new FormElement(formTag, "http://example.com/", attrs);
    }

    /* ---------------------------------------------------------------------- */
    /*  1. elements() – basic behaviour                                        */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testElementsInitiallyEmpty() {
        Elements elems = form.elements();
        assertNotNull(elems);
        assertTrue(elems.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /*  2. addElement – chaining and collection update                         */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testAddElementAddsAndReturnsSelf() {
        Element input = new Element(Tag.valueOf("input"), "", new Attributes());
        input.attr("name", "username").attr("value", "john");

        FormElement returned = form.addElement(input);
        assertSame("addElement should return the same FormElement for chaining", form, returned);
        assertEquals(1, form.elements().size());
        assertSame(input, form.elements().first());
    }

    /* ---------------------------------------------------------------------- */
    /*  3. submit – basic GET submission (no method attribute)                */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testSubmitCreatesGetConnectionWhenNoMethod() {
        // add a simple input so that formData() returns something
        Element txt = new Element(Tag.valueOf("input"), "", new Attributes());
        txt.attr("name", "q").attr("value", "jsoup");
        form.addElement(txt);

        // Mock static Jsoup.connect(...)
        try (MockedStatic<Jsoup> jsoupMock = Mockito.mockStatic(Jsoup.class)) {
            Connection mockConn = mock(Connection.class);
            when(mockConn.data(anyList())).thenReturn(mockConn);
            when(mockConn.method(any(Connection.Method.class))).thenReturn(mockConn);
            jsoupMock.when(() -> Jsoup.connect("http://example.com/")).thenReturn(mockConn);

            Connection conn = form.submit();

            // verify the chain was called with correct data and GET method
            verify(mockConn).data(form.formData());
            verify(mockConn).method(Connection.Method.GET);
            assertSame(mockConn, conn);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  4. submit – POST method handling (case‑insensitive)                     */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testSubmitUsesPostWhenMethodIsPost() {
        form.attr("method", "PoSt"); // mixed case

        try (MockedStatic<Jsoup> jsoupMock = Mockito.mockStatic(Jsoup.class)) {
            Connection mockConn = mock(Connection.class);
            when(mockConn.data(anyList())).thenReturn(mockConn);
            when(mockConn.method(any(Connection.Method.class))).thenReturn(mockConn);
            jsoupMock.when(() -> Jsoup.connect("http://example.com/")).thenReturn(mockConn);

            Connection conn = form.submit();

            verify(mockConn).method(Connection.Method.POST);
            assertSame(mockConn, conn);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  5. submit – explicit action attribute                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testSubmitUsesActionAttributeWhenPresent() {
        form.attr("action", "/search");
        form.attr("method", "GET");

        try (MockedStatic<Jsoup> jsoupMock = Mockito.mockStatic(Jsoup.class)) {
            Connection mockConn = mock(Connection.class);
            when(mockConn.data(anyList())).thenReturn(mockConn);
            when(mockConn.method(any(Connection.Method.class))).thenReturn(mockConn);
            jsoupMock.when(() -> Jsoup.connect("http://example.com/search")).thenReturn(mockConn);

            Connection conn = form.submit();

            verify(mockConn).method(Connection.Method.GET);
            assertSame(mockConn, conn);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  6. submit – missing/empty action and missing base URI → exception    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testSubmitThrowsWhenNoActionAndNoBaseUri() {
        // create a form with an empty base URI
        FormElement badForm = new FormElement(Tag.valueOf("form"), "", new Attributes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Could not determine a form action URL for submit");

        badForm.submit(); // should throw
    }

    @Test
    public void testSubmitThrowsWhenActionIsEmptyString() {
        form.attr("action", "   "); // whitespace considered empty after trim by Validate.notEmpty

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Could not determine a form action URL for submit");

        form.submit();
    }

    /* ---------------------------------------------------------------------- */
    /*  7. formData – simple text input                                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testFormDataSimpleInput() {
        Element txt = new Element(Tag.valueOf("input"), "", new Attributes());
        txt.attr("name", "email").attr("value", "a@b.c");
        form.addElement(txt);

        List<Connection.KeyVal> data = form.formData();
        assertEquals(1, data.size());
        assertEquals("email", data.get(0).key());
        assertEquals("a@b.c", data.get(0).value());
    }

    /* ---------------------------------------------------------------------- */
    /*  8. formData – element without name is ignored                         */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testFormDataIgnoresElementsWithoutName() {
        Element txt = new Element(Tag.valueOf("input"), "", new Attributes());
        txt.attr("value", "no-name");
        form.addElement(txt);

        assertTrue(form.formData().isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /*  9. formData – disabled element is ignored                              */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testFormDataIgnoresDisabledElements() {
        Element txt = new Element(Tag.valueOf("input"), "", new Attributes());
        txt.attr("name", "field").attr("value", "val").attr("disabled", "true");
        form.addElement(txt);

        assertTrue(form.formData().isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 10. formData – checkbox handling (checked / unchecked)                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testFormDataCheckboxCheckedAndUnchecked() {
        // checked checkbox with explicit value
        Element cb1 = new Element(Tag.valueOf("input"), "", new Attributes());
        cb1.attr("type", "checkbox")
            .attr("name", "opt")
            .attr("value", "yes")
            .attr("checked", "checked");
        // unchecked checkbox
        Element cb2 = new Element(Tag.valueOf("input"), "", new Attributes());
        cb2.attr("type", "checkbox")
            .attr("name", "opt")
            .attr("value", "no");

        form.addElement(cb1);
        form.addElement(cb2);

        List<Connection.KeyVal> data = form.formData();
        assertEquals(1, data.size());
        assertEquals("opt", data.get(0).key());
        assertEquals("yes", data.get(0).value());
    }

    @Test
    public void testFormDataCheckboxCheckedWithoutValueDefaultsToOn() {
        Element cb = new Element(Tag.valueOf("input"), "", new Attributes());
        cb.attr("type", "checkbox")
          .attr("name", "agree")
          .attr("checked", "checked"); // no value attribute

        form.addElement(cb);
        List<Connection.KeyVal> data = form.formData();
        assertEquals(1, data.size());
        assertEquals("agree", data.get(0).key());
        assertEquals("on", data.get(0).value());
    }

    /* ---------------------------------------------------------------------- */
    /* 11. formData – radio handling (checked / unchecked)                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testFormDataRadioCheckedOnly() {
        Element r1 = new Element(Tag.valueOf("input"), "", new Attributes());
        r1.attr("type", "radio").attr("name", "color").attr("value", "red").attr("checked", "checked");
        Element r2 = new Element(Tag.valueOf("input"), "", new Attributes());
        r2.attr("type", "radio").attr("name", "color").attr("value", "blue");

        form.addElement(r1);
        form.addElement(r2);

        List<Connection.KeyVal> data = form.formData();
        assertEquals(1, data.size());
        assertEquals("color", data.get(0).key());
        assertEquals("red", data.get(0).value());
    }

    /* ---------------------------------------------------------------------- */
    /* 12. formData – select with selected options                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testFormDataSelectWithSelectedOption() {
        // <select name="city"><option value="ny">NY</option><option value="la" selected>LA</option></select>
        Element select = new Element(Tag.valueOf("select"), "", new Attributes());
        select.attr("name", "city");

        Element opt1 = new Element(Tag.valueOf("option"), "", new Attributes());
        opt1.attr("value", "ny").text("NY");

        Element opt2 = new Element(Tag.valueOf("option"), "", new Attributes());
        opt2.attr("value", "la").attr("selected", "selected").text("LA");

        select.appendChild(opt1);
        select.appendChild(opt2);

        form.addElement(select);
        List<Connection.KeyVal> data = form.formData();

        assertEquals(1, data.size());
        assertEquals("city", data.get(0).key());
        assertEquals("la", data.get(0).value());
    }

    /* ---------------------------------------------------------------------- */
    /* 13. formData – select without any selected option (falls back to first) */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testFormDataSelectWithoutSelectedOptionUsesFirst() {
        // <select name="country"><option value="us">US</option><option value="ca">CA</option></select>
        Element select = new Element(Tag.valueOf("select"), "", new Attributes());
        select.attr("name", "country");

        Element opt1 = new Element(Tag.valueOf("option"), "", new Attributes());
        opt1.attr("value", "us").text("US");

        Element opt2 = new Element(Tag.valueOf("option"), "", new Attributes());
        opt2.attr("value", "ca").text("CA");

        select.appendChild(opt1);
        select.appendChild(opt2);

        form.addElement(select);
        List<Connection.KeyVal> data = form.formData();

        assertEquals(1, data.size());
        assertEquals("country", data.get(0).key());
        assertEquals("us", data.get(0).value()); // first option's value
    }

    /* ---------------------------------------------------------------------- */
    /* 14. formData – multiple elements with same name (order preserved)       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testFormDataMultipleSameName() {
        Element i1 = new Element(Tag.valueOf("input"), "", new Attributes());
        i1.attr("name", "list").attr("value", "a");
        Element i2 = new Element(Tag.valueOf("input"), "", new Attributes());
        i2.attr("name", "list").attr("value", "b");
        Element i3 = new Element(Tag.valueOf("input"), "", new Attributes());
        i3.attr("name", "list").attr("value", "c");

        form.addElement(i1);
        form.addElement(i2);
        form.addElement(i3);

        List<Connection.KeyVal> data = form.formData();
        assertEquals(3, data.size());
        assertEquals("a", data.get(0).value());
        assertEquals("b", data.get(1).value());
        assertEquals("c", data.get(2).value());
    }

    /* ---------------------------------------------------------------------- */
    /* 15. formData – non‑submittable element (e.g., <div>) is ignored          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testFormDataIgnoresNonSubmittableElements() {
        Element div = new Element(Tag.valueOf("div"), "", new Attributes());
        div.attr("name", "ignored").text("some text");
        form.addElement(div);

        assertTrue(form.formData().isEmpty());
    }
}
