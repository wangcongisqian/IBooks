package com.ibooks.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ParsedBook {
    public final @NotNull String id;
    public final @NotNull Path source;
    public final @NotNull String title;
    public final @NotNull String authors;
    public final @NotNull String language;
    public final @NotNull String description;
    public final @Nullable byte[] coverBytes;
    public final @Nullable String coverMime;
    public final @NotNull List<Chapter> chapters;
    public final @NotNull List<TocNode> toc;
    public final @NotNull Path extractedDir;

    public ParsedBook(@NotNull String id,
                      @NotNull Path source,
                      @NotNull String title,
                      @NotNull String authors,
                      @NotNull String language,
                      @NotNull String description,
                      @Nullable byte[] coverBytes,
                      @Nullable String coverMime,
                      @NotNull List<Chapter> chapters,
                      @NotNull List<TocNode> toc,
                      @NotNull Path extractedDir) {
        this.id = id;
        this.source = source;
        this.title = title;
        this.authors = authors;
        this.language = language;
        this.description = description;
        this.coverBytes = coverBytes;
        this.coverMime = coverMime;
        this.chapters = new ArrayList<>(chapters);
        this.toc = new ArrayList<>(toc);
        this.extractedDir = extractedDir;
    }
}
