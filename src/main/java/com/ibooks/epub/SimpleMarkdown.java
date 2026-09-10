package com.ibooks.epub;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** Tiny Markdown subset used when generating EPUB from .md files — no extra library. */
public final class SimpleMarkdown {
    private SimpleMarkdown() {
    }

    public static @NotNull String toXhtml(@NotNull String markdown) {
        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        StringBuilder body = new StringBuilder();
        List<String> para = new ArrayList<>();
        boolean inList = false;
        for (String raw : lines) {
            String line = raw;
            if (line.startsWith("```")) {
                flushPara(body, para);
                continue;
            }
            if (line.startsWith("# ")) {
                flushList(body, inList);
                inList = false;
                flushPara(body, para);
                body.append("<h1>").append(inline(line.substring(2).trim())).append("</h1>\n");
            } else if (line.startsWith("## ")) {
                flushList(body, inList);
                inList = false;
                flushPara(body, para);
                body.append("<h2>").append(inline(line.substring(3).trim())).append("</h2>\n");
            } else if (line.startsWith("### ")) {
                flushList(body, inList);
                inList = false;
                flushPara(body, para);
                body.append("<h3>").append(inline(line.substring(4).trim())).append("</h3>\n");
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                flushPara(body, para);
                if (!inList) {
                    body.append("<ul>\n");
                    inList = true;
                }
                body.append("<li>").append(inline(line.substring(2).trim())).append("</li>\n");
            } else if (line.isBlank()) {
                flushList(body, inList);
                inList = false;
                flushPara(body, para);
            } else {
                flushList(body, inList);
                inList = false;
                para.add(line);
            }
        }
        flushList(body, inList);
        flushPara(body, para);
        return wrap(body.toString());
    }

    public static @NotNull String wrapHtmlFragment(@NotNull String html, @NotNull String title) {
        String inner = html;
        if (!inner.toLowerCase().contains("<html")) {
            inner = wrap("<h1>" + escape(title) + "</h1>\n" + inner);
        }
        return inner;
    }

    private static void flushPara(StringBuilder body, List<String> para) {
        if (para.isEmpty()) {
            return;
        }
        body.append("<p>").append(inline(String.join(" ", para))).append("</p>\n");
        para.clear();
    }

    private static void flushList(StringBuilder body, boolean inList) {
        if (inList) {
            body.append("</ul>\n");
        }
    }

    private static String inline(String text) {
        String escaped = escape(text);
        escaped = escaped.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        escaped = escaped.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
        escaped = escaped.replaceAll("`(.+?)`", "<code>$1</code>");
        return escaped;
    }

    public static String escape(String text) {
        return text.replace("&", "&")
                .replace("<", "<")
                .replace(">", ">")
                .replace("\"", "");
    }

    private static String wrap(String body) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="zh">
                <head>
                  <meta charset="utf-8"/>
                  <title>Chapter</title>
                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(body);
    }
}
