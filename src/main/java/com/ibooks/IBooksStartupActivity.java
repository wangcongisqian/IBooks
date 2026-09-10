package com.ibooks;

import com.ibooks.service.LibraryService;
import com.ibooks.settings.IBooksSettings;
import com.ibooks.ui.IBooksToolWindow;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * IBooksStartupActivity 类实现了 StartupActivity 接口，用于在项目启动时执行特定操作。
 * 该类实现了 DumbAware 接口，表示它可以在后台模式中安全运行。
 */
public final class IBooksStartupActivity implements StartupActivity, DumbAware {
    /**
     * runActivity 方法是 StartupActivity 接口的要求实现，在项目启动时被调用。
     * @param project 当前项目实例，通过 @NotNull 注解确保不为 null
     */
    @Override
    public void runActivity(@NotNull Project project) {
        // 获取 IBooks 设置实例
        IBooksSettings settings = IBooksSettings.getInstance();
        // 检查是否启用了恢复上次打开书籍的设置
        if (!settings.getState().restoreLastBook) {
            return;
        }
        // 获取最后打开的书籍路径
        String lastPath = LibraryService.getInstance().getLastOpenedPath();
        // 如果路径为空或空白字符串，则直接返回
        if (lastPath == null || lastPath.isBlank()) {
            return;
        }
        // 将路径字符串转换为 Path 对象
        Path file = Path.of(lastPath);
        // 检查路径是否指向常规文件
        if (!Files.isRegularFile(file)) {
            return;
        }
        // 使用 invokeLater 在 UI 线程中执行后续操作
        ApplicationManager.getApplication().invokeLater(() -> {
            // 检查项目是否已被销毁
            if (project.isDisposed()) {
                return;
            }
            // 获取 IBooks 工具窗口
            ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow("IBooks");
            // 如果工具窗口存在，则显示它
            if (window != null) {
                window.show(() -> {
                    // 从项目中获取 IBooks 工具窗口面板
                    IBooksToolWindow panel = IBooksToolWindow.from(project);
                    // 如果面板存在，则打开指定路径的书籍
                    if (panel != null) {
                        panel.openPath(file);
                    }
                });
            }
        });
    }
}
