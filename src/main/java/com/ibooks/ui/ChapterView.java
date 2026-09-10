package com.ibooks.ui;

import com.ibooks.domain.Chapter;
import com.ibooks.settings.IBooksSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.BorderLayout;
import java.util.function.Consumer;

/**
 * ChapterView 类是一个用于显示章节内容的面板组件
 * 继承自JPanel，使用BorderLayout布局管理器
 */
public final class ChapterView extends JPanel {
    // 编辑器面板，用于显示HTML内容
    private final JEditorPane editor = new JEditorPane();
    // 滚动面板，包含编辑器组件
    private final JBScrollPane scroll;
    // 链接点击处理器，用于处理章节中的链接点击事件
    private Consumer<String> linkHandler = href -> {};

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
        // 获取当前设置
        IBooksSettings.State s = IBooksSettings.getInstance().getState();
        // 获取HTML编辑器工具包
        HTMLEditorKit kit = (HTMLEditorKit) editor.getEditorKit();
        // 获取样式表
        StyleSheet sheet = kit.getStyleSheet();
        // 添加CSS规则
        sheet.addRule(css(s));
        // 包装HTML内容
        String wrapped = wrap(chapter.html, s);
        // 设置编辑器文本内容
        editor.setText(wrapped);
        // 将光标位置设置到开头
        editor.setCaretPosition(0);
        // 在事件调度线程中执行滚动条归零操作
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    /**
     * 应用主题设置
     */
    public void applyTheme() {
        // 获取当前设置
        IBooksSettings.State s = IBooksSettings.getInstance().getState();
        // 获取HTML文档
        HTMLDocument doc = (HTMLDocument) editor.getDocument();
        // 添加CSS规则
        doc.getStyleSheet().addRule(css(s));
        // 设置背景颜色
        editor.setBackground(themeBackground(s.theme));
        // 重绘编辑器
        editor.repaint();
    }

    /**
     * 应用当前所有阅读样式（字体、行高、主题颜色等）
     * 字体大小、主题切换都调用这个方法即可
     */
    public void applyStyles() {
        IBooksSettings.State s = IBooksSettings.getInstance().getState();

        // 必须在 EDT 上操作 UI
        SwingUtilities.invokeLater(() -> {
            HTMLEditorKit kit = (HTMLEditorKit) editor.getEditorKit();

            // 1. 彻底替换 StyleSheet（避免旧 font-size 规则残留）
            StyleSheet newSheet = new StyleSheet();
            newSheet.addRule(css(s));
            kit.setStyleSheet(newSheet);

            // 2. 同步背景色（body 的 background 有时不够，再设一次组件背景）
            editor.setBackground(themeBackground(s.theme));

            // 3. 强制重新渲染（关键！只改 StyleSheet 通常不会刷新已显示内容）
            String html = editor.getText();
            if (html != null && !html.isBlank()) {
                // 重新用当前设置包装一次（保证 class 和主题也一致）
                // 注意：这里简单处理，如果 chapter 原始 html 需要保留，可缓存原始 body
                editor.setText(html);          // 最稳妥的强制重绘方式
                // 如果你之前保存了原始 chapter，也可以重新调用 showChapter(currentChapter)
            }

            editor.revalidate();
            editor.repaint();
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
        // 在事件调度线程中执行滚动条设置
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(value));
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
        // 查找body标签开始位置
        int bodyStart = html.toLowerCase().indexOf("<body");
        if (bodyStart >= 0) {
            // 查找body标签结束位置
            int gt = html.indexOf('>', bodyStart);
            // 查找body标签结束位置
            int bodyEnd = html.toLowerCase().lastIndexOf("</body>");
            if (gt > 0 && bodyEnd > gt) {
                // 提取body内容
                body = html.substring(gt + 1, bodyEnd);
            }
        }
        // 返回包装后的HTML
        return "<html><head></head><body class='ibooks-body theme-" + s.theme + "'>" + body + "</body></html>";
    }

    /**
     * 生成CSS样式字符串
     * @param s 当前设置
     * @return CSS样式字符串
     */
    private static String css(IBooksSettings.State s) {
        // 获取背景颜色
        String bg = hex(themeBackground(s.theme));
        // 获取前景颜色
        String fg = hex(themeForeground(s.theme));
        // 返回格式化的CSS字符串
        return """
                body { font-family: %s; font-size: %dpx; line-height: %s; color: %s; background: %s; margin: 0; padding: 20px 24px 48px; }
                h1,h2,h3 { line-height: 1.25; }
                img { max-width: 100%%; }
                a { color: inherit; }
                """.formatted(s.fontFamily, s.fontSize, s.lineHeight, fg, bg);
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
