package com.ibooks.pdf;

import com.ibooks.domain.Chapter;
import com.ibooks.domain.ParsedBook;
import com.ibooks.domain.TocNode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public final class PdfParser {
    private PdfParser() {
    }

    public static @NotNull ParsedBook parse(@NotNull Path pdfFile, @NotNull Path cacheRoot) throws IOException {
        if (!Files.isRegularFile(pdfFile)) {
            throw new IOException("Not a file: " + pdfFile);
        }

        String id = hashPath(pdfFile);
        Path extracted = cacheRoot.resolve(id);
        Files.createDirectories(extracted);

        try (PDDocument document = Loader.loadPDF(pdfFile.toFile())) {
            List<String> pageTexts = extractPageTexts(document);
            if (pageTexts.isEmpty()) {
                throw new IOException("PDF contains no readable text: " + pdfFile.getFileName());
            }

            String title = titleFrom(pdfFile, document, pageTexts);
            List<TocNode> toc = extractOutline(document);
            List<Chapter> chapters = new ArrayList<>();
            if (!toc.isEmpty()) {
                for (int i = 0; i < toc.size() && i < pageTexts.size(); i++) {
                    String text = normalize(pageTexts.get(Math.min(i, pageTexts.size() - 1)));
                    if (text.isBlank()) {
                        continue;
                    }
                    String chapterTitle = toc.get(i).title;
                    String href = "chapter-" + i + ".html";
                    String html = buildHtml(text, chapterTitle);
                    chapters.add(new Chapter(i, chapterTitle, href, extracted.resolve(href), html));
                }
            }
            if (chapters.isEmpty()) {
                for (int i = 0; i < pageTexts.size(); i++) {
                    String text = normalize(pageTexts.get(i));
                    if (text.isBlank()) {
                        continue;
                    }
                    String chapterTitle = "Page " + (i + 1);
                    if (i == 0 && !title.isBlank()) {
                        chapterTitle = title;
                    }
                    String href = "chapter-" + i + ".html";
                    String html = buildHtml(text, chapterTitle);
                    chapters.add(new Chapter(i, chapterTitle, href, extracted.resolve(href), html));
                }
            }
            if (chapters.isEmpty()) {
                throw new IOException("PDF contains no extractable text: " + pdfFile.getFileName());
            }

            if (toc.isEmpty()) {
                toc = new ArrayList<>();
                for (Chapter chapter : chapters) {
                    toc.add(new TocNode(chapter.title, chapter.href));
                }
            }
            return new ParsedBook(id, pdfFile, title, authorFrom(document), "en", "", null, null, chapters, toc, extracted);
        }
    }

    private static List<String> extractPageTexts(PDDocument document) throws IOException {
        List<String> pages = new ArrayList<>();
        int pageCount = document.getNumberOfPages();
        for (int i = 1; i <= pageCount; i++) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(i);
            stripper.setEndPage(i);
            stripper.setSortByPosition(true);
            StringWriter writer = new StringWriter();
            stripper.writeText(document, writer);
            String text = writer.toString();
            if (text != null && !text.isBlank()) {
                pages.add(text);
            }
        }
        return pages;
    }

    private static String titleFrom(Path pdfFile, PDDocument document, List<String> pageTexts) {
        PDDocumentInformation info = document.getDocumentInformation();
        String title = info.getTitle();
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        String fileName = pdfFile.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            fileName = fileName.substring(0, dot);
        }
        for (String page : pageTexts) {
            String candidate = firstMeaningfulLine(page);
            if (!candidate.isBlank()) {
                return candidate;
            }
        }
        return fileName;
    }

    private static String authorFrom(PDDocument document) {
        PDDocumentInformation info = document.getDocumentInformation();
        String author = info.getAuthor();
        return author == null || author.isBlank() ? "Unknown author" : author;
    }

    private static List<TocNode> extractOutline(PDDocument document) {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        PDDocumentOutline outline = catalog.getDocumentOutline();
        if (outline == null) {
            return List.of();
        }
        List<TocNode> nodes = new ArrayList<>();
        appendOutlineNodes(nodes, outline);
        return nodes;
    }

    private static void appendOutlineNodes(List<TocNode> result, PDOutlineNode node) {
        PDOutlineItem item = node.getFirstChild();
        while (item != null) {
            String title = item.getTitle();
            if (title != null && !title.isBlank()) {
                String href = "chapter-" + result.size() + ".html";
                result.add(new TocNode(title.trim(), href));
            }
            if (item.hasChildren()) {
                appendOutlineNodes(result, item);
            }
            item = item.getNextSibling();
        }
    }

    private static String firstMeaningfulLine(String pageText) {
        String[] lines = pageText.split("\\r?\\n");
        for (String line : lines) {
            String candidate = line.trim();
            if (candidate.length() > 2 && candidate.length() < 120 && candidate.chars().anyMatch(Character::isLetterOrDigit)) {
                return candidate.replaceAll("\\s+", " ");
            }
        }
        return "";
    }

    private static String normalize(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        normalized = normalized.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]+", " ");
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized.trim();
    }

    private static String buildHtml(String text, String title) {
        String[] paragraphs = text.split("(?<=\\.)\\s+");
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        if (title != null && !title.isBlank()) {
            String escapedTitle = title.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
            html.append("<h1 style='font-size: 1.7em; font-weight: 700; margin: 0 0 14px 0; line-height: 1.3; letter-spacing: 0.02em;'>")
                    .append(escapedTitle)
                    .append("</h1>");
        }
        for (String paragraph : paragraphs) {
            String clean = paragraph.trim();
            if (clean.isBlank()) {
                continue;
            }
            String escaped = clean.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
            html.append("<p>").append(escaped).append("</p>");
        }
        if (!html.toString().contains("<p>")) {
            html.append("<p>").append(text).append("</p>");
        }
        html.append("</body></html>");
        return html.toString();
    }

    private static String hashPath(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String key = path.toAbsolutePath() + ":" + Files.size(path) + ":" + Files.getLastModifiedTime(path);
            return HexFormat.of().formatHex(digest.digest(key.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (Exception e) {
            return Integer.toHexString(path.toAbsolutePath().toString().hashCode());
        }
    }
}
