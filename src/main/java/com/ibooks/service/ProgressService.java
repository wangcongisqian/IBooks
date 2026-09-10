package com.ibooks.service;

import com.ibooks.domain.ReadingProgress;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Service(Service.Level.APP)
@State(name = "com.ibooks.service.ProgressService", storages = @Storage("ibooks-progress.xml"))
public final class ProgressService implements PersistentStateComponent<ProgressService.State> {
    public static final class State {
        public Map<String, ReadingProgress> byBook = new HashMap<>();
    }

    private State state = new State();

    public static ProgressService getInstance() {
        return ApplicationManager.getApplication().getService(ProgressService.class);
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State loaded) {
        state = loaded;
        if (state.byBook == null) {
            state.byBook = new HashMap<>();
        }
    }

    public @Nullable ReadingProgress get(String bookId) {
        return state.byBook.get(bookId);
    }

    public void save(String bookId, int chapterIndex, int scrollOffset) {
        state.byBook.put(bookId, new ReadingProgress(bookId, chapterIndex, scrollOffset));
    }
}
