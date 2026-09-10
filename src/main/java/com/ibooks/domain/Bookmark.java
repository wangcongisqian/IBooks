package com.ibooks.domain;

/**
 * Bookmark类表示一个书签对象，用于存储书籍的阅读进度和笔记信息
 */
public final class Bookmark {
    // 书籍ID，用于标识所属的书籍
    public String bookId = "";
    // 章节索引，用于标识当前阅读的章节位置
    public int chapterIndex;
    // 章节标题，用于标识当前阅读的章节名称
    public String chapterTitle = "";
    // 笔记内容，存储用户在阅读过程中添加的笔记
    public String note = "";
    // 创建时间戳，记录书签创建的时间
    public long createdAt;

    /**
     * 默认构造函数，创建一个空的书签对象
     */
    public Bookmark() {
    }

    /**
     * 带参数的构造函数，用于创建一个包含完整信息的新书签
     * @param bookId 书籍ID
     * @param chapterIndex 章节索引
     * @param chapterTitle 章节标题
     * @param note 笔记内容
     */
    public Bookmark(String bookId, int chapterIndex, String chapterTitle, String note) {
        this.bookId = bookId;
        this.chapterIndex = chapterIndex;
        this.chapterTitle = chapterTitle;
        this.note = note;
        this.createdAt = System.currentTimeMillis();
    }
}
