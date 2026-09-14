package com.ibooks.epub;

import com.ibooks.domain.ParsedBook;
import com.intellij.openapi.application.PathManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EpubService {
    private static final EpubService INSTANCE = new EpubService();

    private EpubService() {
    }

    public static EpubService getInstance() {
        return INSTANCE;
    }

    public @NotNull ParsedBook open(@NotNull Path bookFile) throws IOException {
        String lower = bookFile.getFileName().toString().toLowerCase();
        if (lower.endsWith(".txt")) {
            return EpubParser.parseText(bookFile, cacheRoot());
        }
        if (lower.endsWith(".pdf")) {
            return com.ibooks.pdf.PdfService.getInstance().open(bookFile);
        }
        return EpubParser.parse(bookFile, cacheRoot());
    }

    public @NotNull Path cacheRoot() throws IOException {
        Path dir = Path.of(PathManager.getPluginTempPath(), "ibooks-cache");
        Files.createDirectories(dir);
        return dir;
    }
}
