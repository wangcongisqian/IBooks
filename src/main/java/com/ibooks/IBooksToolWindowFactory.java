package com.ibooks;

import com.ibooks.service.LibraryService;
import com.ibooks.settings.IBooksSettings;
import com.ibooks.ui.IBooksToolWindow;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

public final class IBooksToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        IBooksToolWindow panel = new IBooksToolWindow(project, toolWindow);
        Content content = ContentFactory.getInstance().createContent(panel.getComponent(), "", false);
        content.setPreferredFocusableComponent(panel.getPreferredFocusableComponent());
        content.setDisposer(panel);
        toolWindow.getContentManager().addContent(content);
        project.putUserData(IBooksToolWindow.KEY, panel);

        if (IBooksSettings.getInstance().getState().restoreLastBook) {
            String lastPath = LibraryService.getInstance().getLastOpenedPath();
            if (lastPath != null && !lastPath.isBlank()) {
                Path file = Path.of(lastPath);
                if (Files.isRegularFile(file)) {
                    panel.openPath(file);
                }
            }
        }
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }
}
