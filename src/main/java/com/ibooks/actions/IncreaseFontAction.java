package com.ibooks.actions;

import com.ibooks.ui.IBooksToolWindow;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

public class IncreaseFontAction extends AnAction implements DumbAware {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (e.getProject() == null) {
            return;
        }
        IBooksToolWindow panel = IBooksToolWindow.from(e.getProject());
        if (panel != null) {
            panel.bumpFont(1);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
