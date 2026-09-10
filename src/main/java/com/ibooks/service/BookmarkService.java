package com.ibooks.service;

import com.ibooks.domain.Bookmark;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service(Service.Level.APP)
@State(name = "com.ibooks.service.BookmarkService", storages = @Storage("ibooks-bookmarks.xml"))
public final class BookmarkService implements PersistentStateComponent<BookmarkService.State> {
    public static final class State {
        public List<Bookmark> bookmarks = new ArrayList<>();
    }

    private State state = new State();

    public static BookmarkService getInstance() {
        return ApplicationManager.getApplication().getService(BookmarkService.class);
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State loaded) {
        state = loaded;
        if (state.bookmarks == null) {
            state.bookmarks = new ArrayList<>();
        }
    }

    public List<Bookmark> forBook(String bookId) {
        return state.bookmarks.stream()
                .filter(b -> bookId.equals(b.bookId))
                .collect(Collectors.toList());
    }

    public Bookmark add(String bookId, int chapterIndex, String chapterTitle, String note) {
        Bookmark bookmark = new Bookmark(bookId, chapterIndex, chapterTitle, note);
        state.bookmarks.add(bookmark);
        return bookmark;
    }

    public void remove(Bookmark bookmark) {
        state.bookmarks.remove(bookmark);
    }

    public String exportMarkdown(String bookId, String bookTitle) {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(bookTitle).append(" bookmarks\n\n");
        for (Bookmark bookmark : forBook(bookId)) {
            md.append("- ").append(bookmark.chapterTitle);
            if (bookmark.note != null && !bookmark.note.isBlank()) {
                md.append(" — ").append(bookmark.note);
            }
            md.append("\n");
        }
        return md.toString();
    }
}
