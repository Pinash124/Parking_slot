package com.smartparking.service;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Objects;

final class NullSafety {

    private NullSafety() {
    }

    @NonNull
    @SuppressWarnings("null")
    static <T> T requireNonNull(@Nullable T value) {
        return Objects.requireNonNull(value);
    }
}
