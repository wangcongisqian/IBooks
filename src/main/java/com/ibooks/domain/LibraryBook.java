package com.ibooks.domain;

public final class LibraryBook {
    public String id = "";
    public String path = "";
    public String title = "";
    public String authors = "";
    public String language = "";
    public int chapterCount;
    public long lastOpenedAt;
    public int lastChapterIndex;
    public int lastPercent;

    public LibraryBook() {
    }

    public static LibraryBook from(ParsedBook book) {
        LibraryBook row = new LibraryBook();
        row.id = book.id;
        row.path = book.source.toString();
        row.title = book.title;
        row.authors = book.authors;
        row.language = book.language;
        row.chapterCount = book.chapters.size();
        row.lastOpenedAt = System.currentTimeMillis();
        return row;
    }
}
