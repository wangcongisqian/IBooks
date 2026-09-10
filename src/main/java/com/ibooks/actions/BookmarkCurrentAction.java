package com.ibooks.actions;

import com.ibooks.ui.IBooksToolWindow;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class BookmarkCurrentAction extends AnAction implements DumbAware {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        IBooksToolWindow panel = project == null ? null : IBooksToolWindow.from(project);
        if (panel != null) {
            panel.bookmarkCurrent();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        IBooksToolWindow panel = project == null ? null : IBooksToolWindow.from(project);
        e.getPresentation().setEnabled(panel != null && panel.currentBook() != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
