package com.ibooks.service;

import com.ibooks.domain.LibraryBook;
import com.ibooks.domain.ParsedBook;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service(Service.Level.APP)
@State(name = "com.ibooks.service.LibraryService", storages = @Storage("ibooks-library.xml"))
public final class LibraryService implements PersistentStateComponent<LibraryService.State> {
    public static final class State {
        public List<LibraryBook> books = new ArrayList<>();
        public String lastOpenedPath = "";
    }

    private State state = new State();

    public static LibraryService getInstance() {
        return ApplicationManager.getApplication().getService(LibraryService.class);
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State loaded) {
        state = loaded;
        if (state.books == null) {
            state.books = new ArrayList<>();
        }
    }

    public List<LibraryBook> books() {
        List<LibraryBook> copy = new ArrayList<>(state.books);
        copy.sort(Comparator.comparingLong((LibraryBook b) -> b.lastOpenedAt).reversed());
        return copy;
    }

    public void remember(@NotNull ParsedBook book, int chapterIndex, int percent) {
        LibraryBook row = state.books.stream()
                .filter(b -> book.id.equals(b.id) || book.source.toString().equals(b.path))
                .findFirst()
                .orElseGet(() -> {
                    LibraryBook created = LibraryBook.from(book);
                    state.books.add(created);
                    return created;
                });
        row.id = book.id;
        row.path = book.source.toString();
        row.title = book.title;
        row.authors = book.authors;
        row.language = book.language;
        row.chapterCount = book.chapters.size();
        row.lastOpenedAt = System.currentTimeMillis();
        row.lastChapterIndex = chapterIndex;
        row.lastPercent = percent;
        state.lastOpenedPath = book.source.toString();
    }

    public void remove(String id) {
        state.books.removeIf(b -> id.equals(b.id));
    }

    public @Nullable String getLastOpenedPath() {
        return state.lastOpenedPath == null || state.lastOpenedPath.isBlank() ? null : state.lastOpenedPath;
    }
}
