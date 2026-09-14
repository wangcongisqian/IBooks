package com.ibooks.epub;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Makes chapter XHTML self-contained enough for {@link javax.swing.JTextPane}:
 * inlines local CSS, rewrites relative image src to absolute file URIs.
 */
public final class HtmlRewriter {
    private static final Pattern IMG = Pattern.compile(
            "(?i)(<img\\b[^>]*?\\bsrc\\s*=\\s*['\"])([^'\"]+)(['\"])");
    private static final Pattern CSS_LINK = Pattern.compile(
            "(?i)<link\\b[^>]*rel\\s*=\\s*['\"]stylesheet['\"][^>]*>");
    private static final Pattern HREF = Pattern.compile("(?i)\\bhref\\s*=\\s*['\"]([^'\"]+)['\"]");

    private HtmlRewriter() {
    }

    public static @NotNull String rewrite(@NotNull String html, @NotNull Path chapterFile, @NotNull Path extractedDir) {
        String withCss = inlineStylesheets(html, chapterFile, extractedDir);
        Matcher matcher = IMG.matcher(withCss);
        StringBuffer out = new StringBuffer();
        Path chapterDir = chapterFile.getParent() == null ? extractedDir : chapterFile.getParent();
        while (matcher.find()) {
            String src = matcher.group(2);
            if (src.startsWith("data:") || src.startsWith("http://") || src.startsWith("https://") || src.startsWith("file:")) {
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            Path resolved = chapterDir.resolve(src.split("#", 2)[0]).normalize();
            if (Files.isRegularFile(resolved)) {
                String uri = resolved.toUri().toString();
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(1) + uri + matcher.group(3)));
            } else {
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String inlineStylesheets(String html, Path chapterFile, Path extractedDir) {
        Path chapterDir = chapterFile.getParent() == null ? extractedDir : chapterFile.getParent();
        Matcher matcher = CSS_LINK.matcher(html);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String tag = matcher.group();
            Matcher href = HREF.matcher(tag);
            String replacement = tag;
            if (href.find()) {
                Path css = chapterDir.resolve(href.group(1)).normalize();
                if (Files.isRegularFile(css)) {
                    try {
                        String cssText = Files.readString(css, StandardCharsets.UTF_8);
                        replacement = "<style type=\"text/css\">" + cssText + "</style>";
                    } catch (Exception ignored) {
                        replacement = "";
                    }
                }
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public static boolean isHtmlHref(@NotNull String href) {
        String path = href.split("#", 2)[0].toLowerCase(Locale.ROOT);
        return path.endsWith(".xhtml") || path.endsWith(".html") || path.endsWith(".htm");
    }
}
