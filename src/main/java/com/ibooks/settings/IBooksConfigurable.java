package com.ibooks.settings;

import com.ibooks.IBooksBundle;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.Objects;

public final class IBooksConfigurable implements Configurable {
    private IBooksSettingsPanel panel;

    @Override
    public @Nls String getDisplayName() {
        return IBooksBundle.message("settings.displayName");
    }

    @Override
    public @Nullable JComponent createComponent() {
        panel = new IBooksSettingsPanel();
        return panel.getPanel();
    }

    @Override
    public boolean isModified() {
        return panel != null && panel.isModified(Objects.requireNonNull(IBooksSettings.getInstance().getState()));
    }

    @Override
    public void apply() {
        if (panel != null) {
            panel.apply(Objects.requireNonNull(IBooksSettings.getInstance().getState()));
        }
    }

    @Override
    public void reset() {
        if (panel != null) {
            panel.load(Objects.requireNonNull(IBooksSettings.getInstance().getState()));
        }
    }

    @Override
    public void disposeUIResources() {
        panel = null;
    }
}
