package com.ibooks.epub;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Builds an EPUB 2/3 file with {@link EpubWriter} from chapter sources.
 */
public final class EpubGenerator {
    private EpubGenerator() {
    }

    public static final class ChapterSource {
        public final String title;
        public final String xhtml;

        public ChapterSource(String title, String xhtml) {
            this.title = title;
            this.xhtml = xhtml;
        }
    }

    public static void write(@NotNull OutputStream out,
                             @NotNull String title,
                             @NotNull String authorName,
                             @NotNull String language,
                             @Nullable Path coverImage,
                             @NotNull List<ChapterSource> chapters) throws IOException {
        if (chapters.isEmpty()) {
            throw new IOException("At least one chapter is required");
        }
        Book book = new Book();
        book.getMetadata().addTitle(title);
        book.getMetadata().setLanguage(language == null || language.isBlank() ? "en" : language);
        book.getMetadata().addAuthor(splitAuthor(authorName));

        if (coverImage != null && Files.isRegularFile(coverImage)) {
            String href = "cover" + extension(coverImage.getFileName().toString());
            Resource cover = new Resource(Files.readAllBytes(coverImage), href);
            book.setCoverImage(cover);
            book.addSection("Cover", new Resource(coverXhtml(title).getBytes(StandardCharsets.UTF_8), "cover.xhtml"));
        }

        int i = 1;
        for (ChapterSource chapter : chapters) {
            String href = "chapter-" + String.format(Locale.ROOT, "%03d", i) + ".xhtml";
            Resource resource = new Resource(chapter.xhtml.getBytes(StandardCharsets.UTF_8), href);
            book.addSection(chapter.title, resource);
            i++;
        }

        new EpubWriter().write(book, out);
    }

    public static ChapterSource fromFile(@NotNull Path file) throws IOException {
        String name = file.getFileName().toString();
        String title = name.replaceFirst("\\.(md|markdown|html|htm|xhtml)$", "");
        String raw = Files.readString(file, StandardCharsets.UTF_8);
        String xhtml;
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            xhtml = SimpleMarkdown.toXhtml(raw);
        } else {
            xhtml = SimpleMarkdown.wrapHtmlFragment(raw, title);
        }
        return new ChapterSource(title, xhtml);
    }

    private static Author splitAuthor(String name) {
        if (name == null || name.isBlank()) {
            return new Author("IBooks", "Author");
        }
        String trimmed = name.trim();
        int space = trimmed.lastIndexOf(' ');
        if (space <= 0) {
            return new Author(trimmed, "");
        }
        return new Author(trimmed.substring(0, space), trimmed.substring(space + 1));
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase(Locale.ROOT) : ".jpg";
    }

    private static String coverXhtml(String title) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>%s</title></head>
                <body style="text-align:center;margin:0;padding:0;">
                  <h1>%s</h1>
                </body>
                </html>
                """.formatted(SimpleMarkdown.escape(title), SimpleMarkdown.escape(title));
    }
}
