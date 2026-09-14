package com.ibooks.ui;

import com.ibooks.domain.Chapter;
import com.ibooks.settings.IBooksSettings;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ChapterView 类是一个用于显示章节内容的面板组件
 * 继承自JPanel，使用BorderLayout布局管理器
 */
public final class ChapterView extends JPanel {
    // 编辑器面板，用于显示HTML内容
    private final JTextPane editor = new JTextPane();
    // 滚动面板，包含编辑器组件
    private final JBScrollPane scroll;
    // 链接点击处理器，用于处理章节中的链接点击事件
    private Consumer<String> linkHandler = href -> {};
    private String currentHtml = "";
    private boolean readMode = false;
    private final List<String> readLines = new ArrayList<>();
    private int readLineIndex = -1;
    private Integer pendingScrollOffset = null;

    /**
     * 构造函数，初始化章节视图组件
     * 设置编辑器属性、超链接监听器和滚动面板
     */
    public ChapterView() {
        super(new BorderLayout());
        // 设置编辑器为不可编辑状态
        editor.setEditable(false);
        // 设置编辑器内容类型为HTML
        editor.setContentType("text/html");
        // 设置HTML编辑器工具包
        editor.setEditorKit(new HTMLEditorKit());
        // 使编辑器尊重显示属性设置
        editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        // 设置编辑器边框为空
        editor.setBorder(JBUI.Borders.empty());
        // 添加超链接事件监听器
        editor.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getDescription() != null) {
                linkHandler.accept(e.getDescription());
            }
        });
        // 创建滚动面板并添加编辑器
        scroll = new JBScrollPane(editor);
        scroll.setBorder(JBUI.Borders.empty());
        scroll.setWheelScrollingEnabled(true);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);

        editor.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!readMode || !shouldAdvanceOnMouseClick(e)) {
                    return;
                }
                advanceReadLine();
            }
        });
        editor.addMouseWheelListener(e -> {
            if (readMode && isWheelTrigger()) {
                if (e.getPreciseWheelRotation() > 0) {
                    advanceReadLine();
                }
            } else if (!readMode) {
                JScrollBar bar = scroll.getVerticalScrollBar();
                if (bar != null) {
                    bar.setValue(bar.getValue() + (int) (e.getPreciseWheelRotation() * bar.getUnitIncrement() * 3));
                }
            }
        });
        // 将滚动面板添加到面板中心区域
        add(scroll, BorderLayout.CENTER);
    }

    /**
     * 设置链接处理器
     * @param linkHandler 处理链接点击的函数式接口
     */
    public void setLinkHandler(Consumer<String> linkHandler) {
        this.linkHandler = linkHandler;
    }

    /**
     * 显示指定章节内容
     * @param chapter 要显示的章节对象
     */
    public void showChapter(@NotNull Chapter chapter) {
        showChapter(chapter, null);
    }

    public void showChapter(@NotNull Chapter chapter, Integer targetScrollOffset) {
        currentHtml = chapter.html;
        readLineIndex = -1;
        pendingScrollOffset = targetScrollOffset;
        if (readMode) {
            renderReadMode();
        } else {
            renderNormalMode();
        }
    }

    public void setReadMode(boolean enabled) {
        readMode = enabled;
        if (currentHtml == null || currentHtml.isBlank()) {
            return;
        }
        if (readMode) {
            readLineIndex = 0;
            renderReadMode();
        } else {
            renderNormalMode();
        }
    }

    private void renderNormalMode() {
        IBooksSettings.State s = IBooksSettings.getInstance().getState();
        int previousScroll = pendingScrollOffset != null ? pendingScrollOffset : getScrollOffset();
        editor.setEditorKit(new HTMLEditorKit());
        editor.setContentType("text/html");
        editor.setText(wrap(currentHtml, s));
        editor.setCaretPosition(0);
        editor.revalidate();
        editor.repaint();
        final int restoreTo = previousScroll;
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scroll.getVerticalScrollBar();
            if (bar != null) {
                int max = Math.max(0, bar.getMaximum() - bar.getVisibleAmount());
                int to = Math.max(0, Math.min(restoreTo, max));
                bar.setValue(to);
            }
            scroll.getViewport().setViewPosition(new java.awt.Point(0, Math.max(0, restoreTo)));
            scroll.revalidate();
            scroll.repaint();
            pendingScrollOffset = null;
        });
    }

    public boolean isReadMode() {
        return readMode;
    }

    private boolean shouldAdvanceOnMouseClick(MouseEvent e) {
        String trigger = IBooksSettings.getInstance().getState().readModeTrigger;
        if ("LEFT".equals(trigger)) {
            return SwingUtilities.isLeftMouseButton(e);
        }
        if ("RIGHT".equals(trigger)) {
            return SwingUtilities.isRightMouseButton(e);
        }
        return false;
    }

    private boolean isWheelTrigger() {
        return "WHEEL".equals(IBooksSettings.getInstance().getState().readModeTrigger);
    }

    private void renderReadMode() {
        if (currentHtml == null || currentHtml.isBlank()) {
            return;
        }
        IBooksSettings.State s = IBooksSettings.getInstance().getState();
        List<String> lines = splitReadLines(extractReadableText(currentHtml), Math.max(18, Math.min(36, 28 + (s.fontSize - 14))));
        if (lines.isEmpty()) {
            editor.setText("");
            return;
        }
        if (readLineIndex < 0 || readLineIndex >= lines.size()) {
            readLineIndex = 0;
        }

        readLines.clear();
        readLines.addAll(lines);

        editor.setEditorKit(new javax.swing.text.StyledEditorKit());
        editor.setContentType("text/plain");
        editor.setEditable(false);
        editor.setBackground(themeBackground(s.theme));
        editor.setForeground(themeForeground(s.theme));

        DefaultStyledDocument doc = new DefaultStyledDocument();
        SimpleAttributeSet base = new SimpleAttributeSet();
        StyleConstants.setFontFamily(base, s.fontFamily);
        StyleConstants.setFontSize(base, s.fontSize);
        StyleConstants.setForeground(base, themeForeground(s.theme));
        StyleConstants.setBackground(base, themeBackground(s.theme));
        StyleConstants.setAlignment(base, StyleConstants.ALIGN_LEFT);

        String currentLine = readLines.get(readLineIndex);
        try {
            doc.insertString(0, currentLine, base);
        } catch (BadLocationException ignored) {
            return;
        }

        SimpleAttributeSet highlight = new SimpleAttributeSet(base);
        StyleConstants.setBackground(highlight, new java.awt.Color(0xDDE9FF));
        StyleConstants.setForeground(highlight, new java.awt.Color(0x1A1F2B));
        StyleConstants.setBold(highlight, true);
        doc.setCharacterAttributes(0, currentLine.length(), highlight, false);

        editor.setDocument(doc);
        editor.setCaretPosition(0);
        SwingUtilities.invokeLater(() -> editor.scrollRectToVisible(new java.awt.Rectangle(0, 0, 1, 1)));
    }

    private void advanceReadLine() {
        if (!readMode || readLines.isEmpty()) {
            return;
        }
        if (readLineIndex < readLines.size() - 1) {
            readLineIndex++;
            renderReadMode();
            editor.requestFocusInWindow();
        }
    }

    /**
     * 应用主题设置
     */
    public void applyTheme() {
        applyStyles();
    }

    /**
     * 应用当前所有阅读样式（字体、行高、主题颜色等）
     * 字体大小、主题切换都调用这个方法即可
     */
    public void applyStyles() {
        IBooksSettings.State s = IBooksSettings.getInstance().getState();
        final int previousScroll = getScrollOffset();

        SwingUtilities.invokeLater(() -> {
            if (readMode) {
                renderReadMode();
                return;
            }
            editor.setEditorKit(new HTMLEditorKit());
            editor.setContentType("text/html");
            editor.setText(wrap(currentHtml, s));
            editor.setCaretPosition(0);
            editor.setBackground(themeBackground(s.theme));
            editor.revalidate();
            editor.repaint();
            SwingUtilities.invokeLater(() -> {
                JScrollBar bar = scroll.getVerticalScrollBar();
                if (bar != null) {
                    int max = bar.getMaximum();
                    int value = Math.min(previousScroll, Math.max(0, max - bar.getVisibleAmount()));
                    bar.setValue(value);
                }
            });
        });
    }

    /**
     * 获取当前滚动位置
     * @return 滚动条的当前值
     */
    public int getScrollOffset() {
        return scroll.getVerticalScrollBar().getValue();
    }

    /**
     * 设置滚动位置
     * @param value 滚动条要设置的值
     */
    public void setScrollOffset(int value) {
        setScrollOffset(value, 0);
    }

    private void setScrollOffset(int value, int attempt) {
        if (attempt > 8) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scroll.getVerticalScrollBar();
            if (bar == null) {
                return;
            }
            int max = Math.max(0, bar.getMaximum() - bar.getVisibleAmount());
            int target = Math.max(0, Math.min(value, max));
            if (bar.getValue() != target) {
                bar.setValue(target);
            }
            if (value > 0 && target == 0 && attempt < 8) {
                setScrollOffset(value, attempt + 1);
            }
        });
    }

    /**
     * 在章节内容中查找指定文本
     * @param query 要查找的文本内容
     */
    public void find(String query) {
        // 如果查询内容为空，直接返回
        if (query == null || query.isBlank()) {
            return;
        }
        // 获取编辑器文本内容
        String text = editor.getText();
        // 从当前光标位置开始查找
        int from = editor.getCaretPosition();
        int idx = indexOfIgnoreCase(text, query, from);
        // 如果未找到，从头开始查找
        if (idx < 0) {
            idx = indexOfIgnoreCase(text, query, 0);
        }
        // 如果找到匹配项
        if (idx >= 0) {
            // 设置光标位置到找到的位置
            editor.setCaretPosition(idx);
            // 请求窗口焦点
            editor.requestFocusInWindow();
        }
    }

    /**
     * 不区分大小写的字符串查找
     * @param hay 被查找的字符串
     * @param needle 要查找的字符串
     * @param from 开始查找的位置
     * @return 匹配到的位置，未找到返回-1
     */
    private static int indexOfIgnoreCase(String hay, String needle, int from) {
        return hay.toLowerCase().indexOf(needle.toLowerCase(), from);
    }

    /**
     * 包装HTML内容，添加主题类
     * @param html 原始HTML内容
     * @param s 当前设置
     * @return 包装后的HTML内容
     */
    private static String wrap(String html, IBooksSettings.State s) {
        String body = html;
        int bodyStart = html.toLowerCase().indexOf("<body");
        if (bodyStart >= 0) {
            int gt = html.indexOf('>', bodyStart);
            int bodyEnd = html.toLowerCase().lastIndexOf("</body>");
            if (gt > 0 && bodyEnd > gt) {
                body = html.substring(gt + 1, bodyEnd);
            }
        }
        return "<html><head><style type='text/css'>" + css(s) + "</style></head><body class='ibooks-body theme-" + s.theme + "'>" + body + "</body></html>";
    }

    private static String extractReadableText(String html) {
        String noScripts = html.replaceAll("(?is)<script.*?</script>", " ");
        String noStyles = noScripts.replaceAll("(?is)<style.*?</style>", " ");
        String noTags = noStyles.replaceAll("(?is)<[^>]+>", " ");
        String decoded = noTags.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'");
        return decoded.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }

    private static List<String> splitReadLines(String text, int maxLen) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String part : text.split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            if (!current.isEmpty() && current.length() + part.length() + 1 > maxLen) {
                lines.add(current.toString().trim());
                current = new StringBuilder(part);
            } else {
                if (!current.isEmpty()) {
                    current.append(' ');
                }
                current.append(part);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString().trim());
        }
        return lines;
    }

    /**
     * 生成CSS样式字符串
     * @param s 当前设置
     * @return CSS样式字符串
     */
    private static String css(IBooksSettings.State s) {
        String bg = hex(themeBackground(s.theme));
        String fg = hex(themeForeground(s.theme));
        return """
                body { font-family: %s; font-size: %dpx; line-height: %s; color: %s; background: %s; margin: 0; padding: 20px 24px 48px; }
                h1, h2, h3 { line-height: 1.25; font-weight: 700; margin: 0 0 12px 0; }
                h1 { font-size: %.1fem; }
                h2 { font-size: 1.35em; }
                h3 { font-size: 1.2em; }
                p { margin: 0 0 0.9em 0; }
                img { max-width: 100%%; }
                a { color: inherit; }
                """.formatted(s.fontFamily, s.fontSize, s.lineHeight, fg, bg, s.fontSize / 12.0 + 0.6);
    }

    /**
     * 根据主题名称获取背景颜色
     * @param theme 主题名称
     * @return 对应的背景颜色
     */
    private static java.awt.Color themeBackground(String theme) {
        return switch (theme) {
            case "linen" -> new java.awt.Color(0xE8DCC8);
            case "night" -> new java.awt.Color(0x1C1916);
            case "ink" -> new java.awt.Color(0x0C0B0A);
            default -> new java.awt.Color(0xF3EEE6);
        };
    }

    /**
     * 根据主题名称获取前景颜色
     * @param theme 主题名称
     * @return 对应的前景颜色
     */
    private static java.awt.Color themeForeground(String theme) {
        return switch (theme) {
            case "night" -> new java.awt.Color(0xE8E0D4);
            case "ink" -> new java.awt.Color(0xC8C0B4);
            case "linen" -> new java.awt.Color(0x2A2118);
            default -> new java.awt.Color(0x1C1916);
        };
    }

    /**
     * 将颜色对象转换为十六进制字符串
     * @param color 颜色对象
     * @return 十六进制颜色字符串
     */
    private static String hex(java.awt.Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
