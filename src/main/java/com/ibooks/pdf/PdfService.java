package com.ibooks.pdf;

import com.ibooks.domain.ParsedBook;
import com.intellij.openapi.application.PathManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PdfService {
    private static final PdfService INSTANCE = new PdfService();

    private PdfService() {
    }

    public static PdfService getInstance() {
        return INSTANCE;
    }

    public @NotNull ParsedBook open(@NotNull Path pdfFile) throws IOException {
        return PdfParser.parse(pdfFile, cacheRoot());
    }

    public @NotNull Path cacheRoot() throws IOException {
        Path dir = Path.of(PathManager.getPluginTempPath(), "ibooks-cache-pdf");
        Files.createDirectories(dir);
        return dir;
    }
}
