package com.ibooks.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public final class Chapter {
    public final int index;
    public final @NotNull String title;
    public final @NotNull String href;
    public final @Nullable Path file;
    public final @NotNull String html;

    public Chapter(int index, @NotNull String title, @NotNull String href, @Nullable Path file, @NotNull String html) {
        this.index = index;
        this.title = title;
        this.href = href;
        this.file = file;
        this.html = html;
    }
}
