package com.ibooks.domain;

public final class ReadingProgress {
    public String bookId = "";
    public int chapterIndex;
    public int scrollOffset;
    public long updatedAt;

    public ReadingProgress() {
    }

    public ReadingProgress(String bookId, int chapterIndex, int scrollOffset) {
        this.bookId = bookId;
        this.chapterIndex = chapterIndex;
        this.scrollOffset = scrollOffset;
        this.updatedAt = System.currentTimeMillis();
    }

    public int percent(int chapterCount) {
        if (chapterCount <= 0) {
            return 0;
        }
        return Math.min(100, Math.round((chapterIndex + 1) * 100f / chapterCount));
    }
}
