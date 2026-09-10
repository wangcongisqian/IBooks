package com.ibooks;

import com.ibooks.epub.SimpleMarkdown;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SimpleMarkdownTest {
    @Test
    public void convertsHeadingsAndEmphasis() {
        String xhtml = SimpleMarkdown.toXhtml("# Title\n\nHello **world** and *you*.\n\n- one\n- two\n");
        assertTrue(xhtml.contains("<h1>Title</h1>"));
        assertTrue(xhtml.contains("<strong>world</strong>"));
        assertTrue(xhtml.contains("<em>you</em>"));
        assertTrue(xhtml.contains("<li>one</li>"));
        assertTrue(xhtml.contains("<html"));
    }

    @Test
    public void escapesHtml() {
        String xhtml = SimpleMarkdown.toXhtml("Use <script> tags");
        assertTrue(xhtml.contains("<script>"));
        assertTrue(!xhtml.contains("<script>"));
    }
}
