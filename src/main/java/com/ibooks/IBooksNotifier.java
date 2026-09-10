package com.ibooks;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public final class IBooksNotifier {
    private static final String GROUP_ID = "IBooks";

    private IBooksNotifier() {
    }

    public static void info(@Nullable Project project, String message) {
        notify(project, message, NotificationType.INFORMATION);
    }

    public static void error(@Nullable Project project, String message) {
        notify(project, message, NotificationType.ERROR);
    }

    private static void notify(@Nullable Project project, String message, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(IBooksBundle.message("name"), message, type)
                .notify(project);
    }
}
