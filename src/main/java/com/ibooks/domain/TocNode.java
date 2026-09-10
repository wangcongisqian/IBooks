package com.ibooks.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class TocNode {
    public final @NotNull String title;
    public final @Nullable String href;
    public final @NotNull List<TocNode> children = new ArrayList<>();

    public TocNode(@NotNull String title, @Nullable String href) {
        this.title = title;
        this.href = href;
    }

    @Override
    public String toString() {
        return title;
    }
}
