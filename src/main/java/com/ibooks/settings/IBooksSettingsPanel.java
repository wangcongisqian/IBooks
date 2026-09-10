package com.ibooks.settings;

import com.ibooks.IBooksBundle;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public final class IBooksSettingsPanel {
    private final JPanel panel;
    private final JSpinner fontSize = new JSpinner(new SpinnerNumberModel(17, 12, 32, 1));
    private final JSpinner lineHeight = new JSpinner(new SpinnerNumberModel(1.7, 1.2, 2.4, 0.1));
    private final JSpinner maxWidth = new JSpinner(new SpinnerNumberModel(720, 400, 1200, 20));
    private final ComboBox<String> theme = new ComboBox<>(new String[]{"paper", "linen", "night", "ink"});
    private final ComboBox<String> fontFamily = new ComboBox<>(new String[]{
            "Georgia, 'Times New Roman', serif",
            "'Palatino Linotype', Palatino, serif",
            "serif",
            "sans-serif"
    });
    private final JBCheckBox restore = new JBCheckBox(IBooksBundle.message("settings.restore"));

    public IBooksSettingsPanel() {
        lineHeight.setEditor(new JSpinner.NumberEditor(lineHeight, "0.0"));
        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel(IBooksBundle.message("settings.fontSize")), fontSize, 1, false)
                .addLabeledComponent(new JBLabel(IBooksBundle.message("settings.lineHeight")), lineHeight, 1, false)
                .addLabeledComponent(new JBLabel(IBooksBundle.message("settings.maxWidth")), maxWidth, 1, false)
                .addLabeledComponent(new JBLabel(IBooksBundle.message("settings.theme")), theme, 1, false)
                .addLabeledComponent(new JBLabel(IBooksBundle.message("settings.fontFamily")), fontFamily, 1, false)
                .addComponent(restore, 8)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        panel.setBorder(JBUI.Borders.empty(8, 0));
    }

    public JComponent getPanel() {
        return panel;
    }

    public void load(IBooksSettings.State state) {
        fontSize.setValue(state.fontSize);
        lineHeight.setValue(state.lineHeight);
        maxWidth.setValue(state.maxWidth);
        theme.setSelectedItem(state.theme);
        fontFamily.setSelectedItem(state.fontFamily);
        restore.setSelected(state.restoreLastBook);
    }

    public void apply(IBooksSettings.State state) {
        state.fontSize = (Integer) fontSize.getValue();
        state.lineHeight = ((Number) lineHeight.getValue()).doubleValue();
        state.maxWidth = (Integer) maxWidth.getValue();
        state.theme = String.valueOf(theme.getSelectedItem());
        state.fontFamily = String.valueOf(fontFamily.getSelectedItem());
        state.restoreLastBook = restore.isSelected();
    }

    public boolean isModified(IBooksSettings.State state) {
        return state.fontSize != (Integer) fontSize.getValue()
                || Math.abs(state.lineHeight - ((Number) lineHeight.getValue()).doubleValue()) > 0.01
                || state.maxWidth != (Integer) maxWidth.getValue()
                || !state.theme.equals(theme.getSelectedItem())
                || !state.fontFamily.equals(fontFamily.getSelectedItem())
                || state.restoreLastBook != restore.isSelected();
    }
}
