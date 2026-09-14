package com.ibooks.epub;

import com.ibooks.domain.Chapter;
import com.ibooks.domain.ParsedBook;
import com.ibooks.domain.TocNode;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Reads an EPUB with {@link EpubReader} (epublib-core) and materialises chapters
 * plus resources onto disk so Swing can render them.
 */
public final class EpubParser {
    private EpubParser() {
    }

    public static @NotNull ParsedBook parseText(@NotNull Path textFile, @NotNull Path cacheRoot) throws IOException {
        if (!Files.isRegularFile(textFile)) {
            throw new IOException("Not a file: " + textFile);
        }
        String id = hashPath(textFile);
        Path extracted = cacheRoot.resolve(id);
        Files.createDirectories(extracted);

        byte[] bytes = Files.readAllBytes(textFile);
        String text = new String(bytes, detectCharset(bytes));
        String clean = text.replace("\r\n", "\n").replace('\r', '\n');
        String title = textFile.getFileName().toString();
        int dotIndex = title.lastIndexOf('.');
        if (dotIndex > 0) {
            title = title.substring(0, dotIndex);
        }
        String firstLine = clean.lines().filter(line -> !line.isBlank()).findFirst().orElse(title);
        if (firstLine.length() <= 80) {
            title = firstLine.strip();
        }

        String html = buildPlainTextHtml(clean);
        Chapter chapter = new Chapter(0, title, "chapter-0", extracted.resolve("chapter-0.txt"), html);
        List<TocNode> toc = List.of(new TocNode(title, chapter.href));
        return new ParsedBook(id, textFile, title, "Unknown author", "en", "", null, null, List.of(chapter), toc, extracted);
    }

    public static @NotNull ParsedBook parse(@NotNull Path epubFile, @NotNull Path cacheRoot) throws IOException {
        if (!Files.isRegularFile(epubFile)) {
            throw new IOException("Not a file: " + epubFile);
        }
        String id = hashPath(epubFile);
        Path extracted = cacheRoot.resolve(id);
        Files.createDirectories(extracted);

        Book book;
        try (InputStream in = new BufferedInputStream(Files.newInputStream(epubFile))) {
            book = new EpubReader().readEpub(in);
        } catch (Exception e) {
            throw new IOException("epublib-core failed to parse " + epubFile.getFileName(), e);
        }

        extractResources(book, extracted);

        Metadata meta = book.getMetadata();
        String title = firstOr(meta.getTitles(), epubFile.getFileName().toString().replaceFirst("\\.epub$", ""));
        String authors = meta.getAuthors().stream()
                .map(EpubParser::formatAuthor)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
        if (authors.isBlank()) {
            authors = "Unknown author";
        }
        String language = meta.getLanguage() == null || meta.getLanguage().isBlank() ? "en" : meta.getLanguage();
        String description = firstOr(meta.getDescriptions(), "");

        byte[] coverBytes = null;
        String coverMime = null;
        Resource cover = book.getCoverImage();
        if (cover != null && cover.getData() != null) {
            coverBytes = cover.getData();
            coverMime = cover.getMediaType() != null ? cover.getMediaType().getName() : "image/jpeg";
        }

        List<Chapter> chapters = new ArrayList<>();
        int index = 0;
        for (SpineReference spineRef : book.getSpine().getSpineReferences()) {
            Resource resource = spineRef.getResource();
            if (resource == null) {
                continue;
            }
            String href = resource.getHref() == null ? "chapter-" + index + ".xhtml" : resource.getHref();
            Path chapterFile = extracted.resolve(href).normalize();
            String html = "";
            if (Files.isRegularFile(chapterFile)) {
                html = Files.readString(chapterFile, StandardCharsets.UTF_8);
                html = HtmlRewriter.rewrite(html, chapterFile, extracted);
            } else if (resource.getData() != null) {
                html = new String(resource.getData(), StandardCharsets.UTF_8);
            }
            String chapterTitle = titleFrom(html, href, index);
            chapters.add(new Chapter(index, chapterTitle, href, Files.isRegularFile(chapterFile) ? chapterFile : null, html));
            index++;
        }

        if (chapters.isEmpty()) {
            throw new IOException("EPUB has no readable spine");
        }

        List<TocNode> toc = mapToc(book.getTableOfContents().getTocReferences());
        if (toc.isEmpty()) {
            for (Chapter chapter : chapters) {
                toc.add(new TocNode(chapter.title, chapter.href));
            }
        }

        return new ParsedBook(id, epubFile, title, authors, language, description, coverBytes, coverMime, chapters, toc, extracted);
    }

    private static void extractResources(Book book, Path extracted) throws IOException {
        for (Resource resource : book.getResources().getAll()) {
            if (resource == null || resource.getHref() == null || resource.getData() == null) {
                continue;
            }
            Path out = extracted.resolve(resource.getHref()).normalize();
            if (!out.startsWith(extracted)) {
                continue;
            }
            Files.createDirectories(out.getParent());
            Files.write(out, resource.getData());
        }
    }

    private static List<TocNode> mapToc(List<TOCReference> refs) {
        List<TocNode> nodes = new ArrayList<>();
        if (refs == null) {
            return nodes;
        }
        for (TOCReference ref : refs) {
            String href = ref.getCompleteHref();
            String title = ref.getTitle() == null || ref.getTitle().isBlank() ? href : ref.getTitle();
            TocNode node = new TocNode(title, href);
            node.children.addAll(mapToc(ref.getChildren()));
            nodes.add(node);
        }
        return nodes;
    }

    private static String titleFrom(String html, String href, int index) {
        int start = indexOfIgnoreCase(html, "<title>");
        int end = indexOfIgnoreCase(html, "</title>");
        if (start >= 0 && end > start) {
            String t = html.substring(start + 7, end).replaceAll("\\s+", " ").trim();
            if (!t.isBlank()) {
                return t;
            }
        }
        start = indexOfIgnoreCase(html, "<h1");
        if (start >= 0) {
            int gt = html.indexOf('>', start);
            int close = indexOfIgnoreCase(html, "</h1>");
            if (gt > 0 && close > gt) {
                String t = html.substring(gt + 1, close).replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
                if (!t.isBlank()) {
                    return t;
                }
            }
        }
        String name = Path.of(href).getFileName().toString().replaceFirst("\\.(xhtml|html|htm)$", "");
        return name.isBlank() ? "Chapter " + (index + 1) : name;
    }

    private static int indexOfIgnoreCase(String hay, String needle) {
        return hay.toLowerCase(Locale.ROOT).indexOf(needle);
    }

    private static String firstOr(List<String> values, String fallback) {
        if (values == null) {
            return fallback;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return fallback;
    }

    private static String formatAuthor(Author author) {
        if (author == null) {
            return "";
        }
        String first = author.getFirstname() == null ? "" : author.getFirstname().trim();
        String last = author.getLastname() == null ? "" : author.getLastname().trim();
        return (first + " " + last).trim();
    }

    private static String buildPlainTextHtml(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] paragraphs = normalized.split("\\n\\s*\\n");
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        for (String paragraph : paragraphs) {
            String safe = paragraph.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
            safe = safe.replace("\n", "<br>");
            html.append("<p>").append(safe).append("</p>");
        }
        if (paragraphs.length == 0 || paragraphs[0].isBlank()) {
            html.append("<p></p>");
        }
        html.append("</body></html>");
        return html.toString();
    }

    private static java.nio.charset.Charset detectCharset(byte[] bytes) {
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        if (bytes.length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
            return StandardCharsets.UTF_16BE;
        }
        if (bytes.length >= 2 && (bytes[0] == 0 || bytes[1] == 0)) {
            return StandardCharsets.UTF_16;
        }
        return StandardCharsets.UTF_8;
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
