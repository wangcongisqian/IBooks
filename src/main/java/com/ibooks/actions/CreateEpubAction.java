package com.ibooks.actions;

import com.ibooks.IBooksBundle;
import com.ibooks.IBooksNotifier;
import com.ibooks.epub.EpubGenerator;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CreateEpubAction extends AnAction implements DumbAware {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        MetaDialog dialog = new MetaDialog(project);
        if (!dialog.showAndGet()) {
            return;
        }
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, true)
                .withTitle(IBooksBundle.message("dialog.create.files"))
                .withFileFilter(file -> {
                    String ext = file.getExtension();
                    if (ext == null) {
                        return false;
                    }
                    String lower = ext.toLowerCase(Locale.ROOT);
                    return lower.equals("html") || lower.equals("htm") || lower.equals("xhtml")
                            || lower.equals("md") || lower.equals("markdown");
                });
        VirtualFile[] files = FileChooser.chooseFiles(descriptor, project, null);
        if (files.length == 0) {
            Messages.showWarningDialog(project, IBooksBundle.message("dialog.create.empty"), IBooksBundle.message("dialog.create.title"));
            return;
        }
        FileSaverDescriptor saver = new FileSaverDescriptor(IBooksBundle.message("dialog.create.title"), "", "epub");
        VirtualFileWrapper target = FileChooserFactory.getInstance()
                .createSaveFileDialog(saver, project)
                .save(dialog.title.replaceAll("\\s+", "-") + ".epub");
        if (target == null) {
            return;
        }
        try {
            List<EpubGenerator.ChapterSource> chapters = new ArrayList<>();
            for (VirtualFile file : files) {
                chapters.add(EpubGenerator.fromFile(Path.of(file.getPath())));
            }
            Path out = target.getFile().toPath();
            try (var stream = Files.newOutputStream(out)) {
                EpubGenerator.write(stream, dialog.title, dialog.author, dialog.language, null, chapters);
            }
            IBooksNotifier.info(project, IBooksBundle.message("dialog.create.success", out));
        } catch (Exception ex) {
            IBooksNotifier.error(project, ex.getMessage());
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    private static final class MetaDialog extends DialogWrapper {
        private final JBTextField titleField = new JBTextField("Untitled");
        private final JBTextField authorField = new JBTextField("Author");
        private final JBTextField languageField = new JBTextField("zh");
        String title;
        String author;
        String language;

        MetaDialog(Project project) {
            super(project);
            setTitle(IBooksBundle.message("dialog.create.title"));
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            return FormBuilder.createFormBuilder()
                    .addLabeledComponent(new JBLabel(IBooksBundle.message("dialog.create.name")), titleField)
                    .addLabeledComponent(new JBLabel(IBooksBundle.message("dialog.create.author")), authorField)
                    .addLabeledComponent(new JBLabel(IBooksBundle.message("dialog.create.language")), languageField)
                    .getPanel();
        }

        @Override
        protected void doOKAction() {
            title = titleField.getText().trim();
            author = authorField.getText().trim();
            language = languageField.getText().trim();
            if (title.isEmpty()) {
                title = "Untitled";
            }
            super.doOKAction();
        }
    }
}
