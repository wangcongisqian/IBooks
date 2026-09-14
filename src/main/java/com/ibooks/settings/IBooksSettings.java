package com.ibooks.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.APP)
@State(name = "com.ibooks.settings.IBooksSettings", storages = @Storage("ibooks.xml"))
public final class IBooksSettings implements PersistentStateComponent<IBooksSettings.State> {
    public static final class State {
        public int fontSize = 17;
        public double lineHeight = 1.7;
        public int maxWidth = 720;
        public String theme = "paper";
        public String fontFamily = "Georgia, 'Times New Roman', serif";
        public boolean restoreLastBook = true;
        public String readModeTrigger = "LEFT";
    }

    private final State state = new State();

    public static IBooksSettings getInstance() {
        return ApplicationManager.getApplication().getService(IBooksSettings.class);
    }

    @Override
    public @NotNull State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State loaded) {
        XmlSerializerUtil.copyBean(loaded, state);
        if (state.fontSize < 12) {
            state.fontSize = 12;
        }
        if (state.fontSize > 32) {
            state.fontSize = 32;
        }
        if (state.lineHeight < 1.2) {
            state.lineHeight = 1.2;
        }
        if (state.maxWidth < 400) {
            state.maxWidth = 400;
        }
        if (state.theme == null || state.theme.isBlank()) {
            state.theme = "paper";
        }
        if (state.readModeTrigger == null || state.readModeTrigger.isBlank()) {
            state.readModeTrigger = "LEFT";
        }
        switch (state.readModeTrigger) {
            case "LEFT", "RIGHT", "WHEEL" -> {}
            default -> state.readModeTrigger = "LEFT";
        }
    }

    public void bumpFont(int delta) {
        state.fontSize = Math.max(12, Math.min(32, state.fontSize + delta));
    }

    public void cycleTheme() {
        switch (state.theme) {
            case "paper" -> state.theme = "linen";
            case "linen" -> state.theme = "night";
            case "night" -> state.theme = "ink";
            default -> state.theme = "paper";
        }
    }
}
