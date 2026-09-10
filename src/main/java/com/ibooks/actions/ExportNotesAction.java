package com.ibooks.actions;

import com.ibooks.IBooksBundle;
import com.ibooks.IBooksNotifier;
import com.ibooks.domain.ParsedBook;
import com.ibooks.service.BookmarkService;
import com.ibooks.ui.IBooksToolWindow;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ExportNotesAction extends AnAction implements DumbAware {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        IBooksToolWindow panel = project == null ? null : IBooksToolWindow.from(project);
        if (panel == null || panel.currentBook() == null) {
            return;
        }
        ParsedBook book = panel.currentBook();
        FileSaverDescriptor saver = new FileSaverDescriptor(IBooksBundle.message("dialog.export.title"), "", "md");
        VirtualFileWrapper target = FileChooserFactory.getInstance()
                .createSaveFileDialog(saver, project)
                .save(book.title.replaceAll("\\s+", "-") + "-bookmarks.md");
        if (target == null) {
            return;
        }
        try {
            String md = BookmarkService.getInstance().exportMarkdown(book.id, book.title);
            Files.writeString(target.getFile().toPath(), md, StandardCharsets.UTF_8);
            IBooksNotifier.info(project, IBooksBundle.message("dialog.export.success", target.getFile().getPath()));
        } catch (Exception ex) {
            IBooksNotifier.error(project, ex.getMessage());
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
