package com.ibooks;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public final class IBooksBundle extends DynamicBundle {
    @NonNls
    public static final String BUNDLE = "messages.IBooksBundle";
    private static final IBooksBundle INSTANCE = new IBooksBundle();

    private IBooksBundle() {
        super(BUNDLE);
    }

    public static @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
                                      Object @NotNull ... params) {
        return INSTANCE.getMessage(key, params);
    }

}
