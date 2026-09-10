package com.ibooks.ui;

import com.ibooks.IBooksBundle;
import com.ibooks.IBooksNotifier;
import com.ibooks.domain.Chapter;
import com.ibooks.domain.ParsedBook;
import com.ibooks.domain.ReadingProgress;
import com.ibooks.epub.EpubService;
import com.ibooks.service.BookmarkService;
import com.ibooks.service.LibraryService;
import com.ibooks.service.ProgressService;
import com.ibooks.settings.IBooksSettings;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 电子书阅读器 ToolWindow。
 * 目录：左右 JBSplitter（原实现）。
 * 工具栏：Icon 按钮；可收起为 ›，点击弹出悬浮操作菜单（类似 Services）。
 */
public final class IBooksToolWindow implements Disposable {

    public static final Key<IBooksToolWindow> KEY = Key.create("com.ibooks.IBooksToolWindow");

    /**
     * 项目实例
     */
    private final Project project;
    /**
     * 根面板，包含工具栏和内容区域
     */
    private final SimpleToolWindowPanel root = new SimpleToolWindowPanel(false, true);
    /**
     * 卡片布局管理器，用于切换图书馆和阅读器视图
     */
    private final CardLayout cards = new CardLayout();
    /**
     * 卡片布局的宿主面板，用于容纳不同视图
     */
    private final JPanel cardHost = new JPanel(cards);
    /**
     * 图书馆面板，用于显示书籍列表
     */
    private final LibraryPanel libraryPanel;
    /**
     * 章节视图，用于显示书籍内容
     */
    private final ChapterView chapterView = new ChapterView();
    /**
     * 目录树，用于显示书籍目录结构
     */
    private final TocTree tocTree = new TocTree();
    /**
     * 状态标签，显示当前阅读进度和章节信息
     */
    private final JBLabel status = new JBLabel(IBooksBundle.message("toolwindow.empty"));
    /**
     * 搜索文本框，用于在章节中搜索内容
     */
    private final SearchTextField search = new SearchTextField();

    /**
     * 工具栏按钮面板
     */
    private JPanel buttonBar;
    /**
     * 收起的工具栏面板
     */
    private JPanel collapsedBar;
    /**
     * 工具栏宿主面板
     */
    private JPanel toolbarHost;

    /**
     * 当前打开的书籍
     */
    private @Nullable ParsedBook current;
    /**
     * 当前章节索引
     */
    private int chapterIndex;
    /**
     * 目录弹出窗口
     */
    private JBPopup tocPopup; //目录浮层

    /**
     * 构造函数，初始化电子书阅读器工具窗口
     * @param project 当前项目实例
     * @param toolWindow 工具窗口实例
     */
    public IBooksToolWindow(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        libraryPanel = new LibraryPanel(this::openPath, id -> LibraryService.getInstance().remove(id));

        JComponent reader = buildReader();
        cardHost.add(libraryPanel, "library");
        cardHost.add(reader, "reader");
        cards.show(cardHost, "library");

        toolbarHost = buildToolbarHost();
        root.setToolbar(toolbarHost);
        root.setContent(cardHost);
        root.setMinimumSize(new Dimension(320, 240));
        enableDrop(root);

        tocTree.setHrefHandler(this::openHref);
        chapterView.setLinkHandler(this::openHref);

        search.addDocumentListener(new com.intellij.ui.DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull javax.swing.event.DocumentEvent e) {
                chapterView.find(search.getText());
            }
        });
    }

    /**
     * 从项目中获取IBooksToolWindow实例
     * @param project 当前项目实例
     * @return IBooksToolWindow实例，如果不存在则返回null
     */
    public static @Nullable IBooksToolWindow from(@NotNull Project project) {
        return project.getUserData(KEY);
    }

    /**
     * 获取工具窗口的组件实例
     * @return 工具窗口的根组件
     */
    public JComponent getComponent() {
        return root;
    }

    /**
     * 获取工具窗口的默认焦点组件
     * @return 可获取焦点的组件
     */
    public JComponent getPreferredFocusableComponent() {
        return root;
    }

    /**
     * 显示图书馆视图
     */
    public void showLibrary() {
        persistProgress();
        libraryPanel.reload();
        cards.show(cardHost, "library");
        status.setText(IBooksBundle.message("toolwindow.empty"));
    }

    /**
     * 打开EPUB文件选择对话框
     */
    public void openEpubDialog() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle(IBooksBundle.message("dialog.open.title"))
                .withFileFilter(file -> "epub".equalsIgnoreCase(file.getExtension()));
        VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
        if (file != null) {
            openPath(Path.of(file.getPath()));
        }
    }

    /**
     * 打开指定路径的EPUB书籍
     * @param path EPUB文件的路径
     */
    public void openPath(@NotNull Path path) {
        persistProgress();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Opening EPUB", false) {
            private ParsedBook parsed;
            private Exception error;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    parsed = EpubService.getInstance().open(path);
                } catch (IOException e) {
                    error = e;
                }
            }

            @Override
            public void onSuccess() {
                if (error != null || parsed == null) {
                    IBooksNotifier.error(project, IBooksBundle.message("notify.open.failed",
                            error == null ? path.getFileName() : error.getMessage()));
                    return;
                }
                current = parsed;
                ReadingProgress progress = ProgressService.getInstance().get(parsed.id);
                chapterIndex = progress == null
                        ? 0
                        : Math.min(progress.chapterIndex, parsed.chapters.size() - 1);
                tocTree.setBook(parsed);
                showChapter(chapterIndex);
                cards.show(cardHost, "reader");
                LibraryService.getInstance().remember(parsed, chapterIndex, percent());
                libraryPanel.reload();
                if (progress != null) {
                    chapterView.setScrollOffset(progress.scrollOffset);
                }
            }
        });
    }

    /**
     * 跳转到下一章
     */
    public void nextChapter() {
        if (current == null) {
            return;
        }
        showChapter(Math.min(current.chapters.size() - 1, chapterIndex + 1));
    }

    /**
     * 跳转到上一章
     */
    public void prevChapter() {
        showChapter(Math.max(0, chapterIndex - 1));
    }

    /**
     * 为当前章节添加书签
     */
    public void bookmarkCurrent() {
        if (current == null) {
            return;
        }
        Chapter chapter = current.chapters.get(chapterIndex);
        BookmarkService.getInstance().add(current.id, chapterIndex, chapter.title, "");
        IBooksNotifier.info(project, IBooksBundle.message("notify.bookmark.added"));
    }

    /**
     * 调整字体大小
     * @param delta 字体大小变化量，正值增大，负值减小
     */
    public void bumpFont(int delta) {
        IBooksSettings.getInstance().bumpFont(delta);
        chapterView.applyTheme();
        refreshChapter();
    }

    /**
     * 切换主题样式
     */
    public void cycleTheme() {
        IBooksSettings.getInstance().cycleTheme();
        chapterView.applyTheme();
        refreshChapter();
    }

    /**
     * 获取当前打开的书籍
     * @return 当前打开的ParsedBook实例，如果未打开书籍则返回null
     */
    public @Nullable ParsedBook currentBook() {
        return current;
    }

    /**
     * 设置工具栏的展开/收起状态
     * @param expanded true表示展开，false表示收起
     */
    public void setToolbarExpanded(boolean expanded) {
        buttonBar.setVisible(expanded);
        collapsedBar.setVisible(!expanded);
        status.setVisible(expanded);
        toolbarHost.revalidate();
        toolbarHost.repaint();
    }

    /**
     * 显示指定章节
     * @param index 要显示的章节索引
     */
    private void showChapter(int index) {
        if (current == null || index < 0 || index >= current.chapters.size()) {
            return;
        }
        persistProgress();
        chapterIndex = index;
        Chapter chapter = current.chapters.get(index);
        chapterView.showChapter(chapter);
        status.setText(current.title + "  ·  "
                + IBooksBundle.message("reader.chapter", index + 1, current.chapters.size())
                + "  ·  " + chapter.title);
        LibraryService.getInstance().remember(current, chapterIndex, percent());
    }

    /**
     * 刷新当前章节显示
     */
    private void refreshChapter() {
        if (current != null) {
            int scroll = chapterView.getScrollOffset();
            chapterView.showChapter(current.chapters.get(chapterIndex));
            chapterView.setScrollOffset(scroll);
        }
    }

    /**
     * 处理章节链接点击
     * @param href 章节链接地址
     */
    private void openHref(String href) {
        if (current == null || href == null) {
            return;
        }
        String path = href.split("#", 2)[0];
        for (Chapter chapter : current.chapters) {
            if (chapter.href.equals(path) || chapter.href.endsWith(path) || path.endsWith(chapter.href)) {
                showChapter(chapter.index);
                return;
            }
        }
    }

    /**
     * 计算当前阅读进度百分比
     * @return 阅读进度百分比
     */
    private int percent() {
        if (current == null) {
            return 0;
        }
        return Math.round((chapterIndex + 1) * 100f / current.chapters.size());
    }

    /**
     * 保存当前阅读进度
     */
    private void persistProgress() {
        if (current == null) {
            return;
        }
        ProgressService.getInstance().save(current.id, chapterIndex, chapterView.getScrollOffset());
        LibraryService.getInstance().remember(current, chapterIndex, percent());
    }

    // -------------------------------------------------------------------------
    // Toolbar
    // -------------------------------------------------------------------------

    /**
     * 构建工具栏宿主面板
     * @return 包含工具栏和状态栏的面板
     */
    private JPanel buildToolbarHost() {
        buttonBar = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 4, 2));
        buttonBar.setBorder(JBUI.Borders.empty(2, 4));

        buttonBar.add(iconButton(IBooksBundle.message("toolwindow.open"), AllIcons.Actions.MenuOpen, this::openEpubDialog));
        buttonBar.add(iconButton(IBooksBundle.message("action.library"), AllIcons.Nodes.PpLib, this::showLibrary));

        search.getTextEditor().setColumns(12);
        buttonBar.add(search);

        JButton collapseBtn = chevronButton(AllIcons.Actions.ArrowExpand, IBooksBundle.message("menu.collapse.toolbar"));
        // ArrowExpand 朝右；收起用向左更直观时可用 AllIcons.Actions.ArrowCollapse（若平台有）
        collapseBtn.setIcon(AllIcons.General.ChevronUp);
        collapseBtn.setToolTipText(IBooksBundle.message("menu.collapse.toolbar"));
        collapseBtn.addActionListener(e -> setToolbarExpanded(false));
        buttonBar.add(collapseBtn);

        collapsedBar = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 2, 0));
        collapsedBar.setBorder(JBUI.Borders.empty(1, 4));
        JButton menuBtn = chevronButton(AllIcons.General.ChevronRight, IBooksBundle.message("menu.show.actions"));
        menuBtn.addActionListener(e -> showActionsPopup(menuBtn));
        collapsedBar.add(menuBtn);
        collapsedBar.setVisible(false);

        JPanel northWrap = new JPanel(new BorderLayout());
        northWrap.add(buttonBar, BorderLayout.CENTER);
        northWrap.add(collapsedBar, BorderLayout.WEST);

        JPanel host = new JPanel(new BorderLayout());
        host.add(northWrap, BorderLayout.NORTH);
        host.add(status, BorderLayout.SOUTH);
        status.setBorder(JBUI.Borders.empty(2, 10, 6, 10));
        status.setFont(status.getFont().deriveFont(Font.PLAIN, 11f));
        return host;
    }

    /** Icon 工具按钮 */
    private static JButton iconButton(String tooltip, Icon icon, Runnable action) {
        JButton btn = new JButton(icon);
        btn.setToolTipText(tooltip);
        btn.setMargin(JBUI.insets(2));
        btn.setBorder(JBUI.Borders.empty(2));
        btn.setContentAreaFilled(false);
        btn.setFocusable(false);
        btn.setPreferredSize(new Dimension(28, 28));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    /** 小箭头按钮（收起 / 展开菜单） */
    private static JButton chevronButton(Icon icon, String tooltip) {
        JButton btn = new JButton(icon);
        btn.setToolTipText(tooltip);
        btn.setMargin(JBUI.emptyInsets());
        btn.setBorder(JBUI.Borders.empty(2));
        btn.setContentAreaFilled(false);
        btn.setFocusable(false);
        btn.setPreferredSize(new Dimension(22, 22));
        btn.setMinimumSize(new Dimension(22, 22));
        btn.setMaximumSize(new Dimension(22, 22));
        return btn;
    }

    /** 悬浮操作菜单（Services 风格） */
    private void showActionsPopup(JComponent anchor) {
        record ActionItem(String text, Icon icon, Runnable run) {}

        List<ActionItem> items = List.of(
                new ActionItem(IBooksBundle.message("toolwindow.open"), AllIcons.Actions.MenuOpen, this::openEpubDialog),
                new ActionItem(IBooksBundle.message("action.library"), AllIcons.Nodes.PpLib, this::showLibrary),
                new ActionItem(IBooksBundle.message("menu.show.toolbar"), AllIcons.General.ChevronDown, () -> setToolbarExpanded(true))
        );

        BaseListPopupStep<ActionItem> step = new BaseListPopupStep<>(null, items) {
            @Override
            public @NotNull String getTextFor(ActionItem value) {
                return value.text();
            }

            @Override
            public Icon getIconFor(ActionItem value) {
                return value.icon();
            }

            @Override
            public @Nullable PopupStep<?> onChosen(ActionItem selected, boolean finalChoice) {
                SwingUtilities.invokeLater(selected.run());
                return FINAL_CHOICE;
            }
        };

        ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
        popup.showUnderneathOf(anchor);
    }

    /**
     * 构建阅读器界面的主方法
     * @return 配置完成的JPanel面板
     */
    private JPanel buildReader() {
        // 创建主面板，使用边界布局(BorderLayout)
        JPanel mainPanel = new JPanel(new BorderLayout());
        // 顶部工具栏：放【目录弹出按钮】
        JPanel toolBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,4,2));

        JButton tocBtn = new JButton();//"▤"
        tocBtn.setToolTipText(IBooksBundle.message("toolwindow.catalogue"));
        tocBtn.setIcon(AllIcons.Actions.Minimap);

        tocBtn.addActionListener(e -> showTocPopup(tocBtn));
        toolBarPanel.add(tocBtn);
        // 顶部工具栏：放【下一章】 【上一章】 【字体大小】 【主题】 【书签】
        toolBarPanel.add(iconButton(IBooksBundle.message("action.prev"), AllIcons.Actions.Back, this::prevChapter));
        toolBarPanel.add(iconButton(IBooksBundle.message("action.next"), AllIcons.Actions.Forward, this::nextChapter));
//        toolBarPanel.add(iconButton(IBooksBundle.message("toolwindow.font.minus"), AllIcons.General.Remove, () -> bumpFont(-1)));
//        toolBarPanel.add(iconButton(IBooksBundle.message("toolwindow.font.plus"), AllIcons.General.Add, () -> bumpFont(1)));
//        toolBarPanel.add(iconButton(IBooksBundle.message("toolwindow.theme"), AllIcons.Actions.IntentionBulb, this::cycleTheme));
//        toolBarPanel.add(iconButton(IBooksBundle.message("action.bookmark"), AllIcons.Actions.Checked, this::bookmarkCurrent));

        mainPanel.add(toolBarPanel, BorderLayout.NORTH);
        // 主体只放阅读章节内容
        mainPanel.add(chapterView, BorderLayout.CENTER);

        return mainPanel;
    }

    /**
     * 弹出目录树弹窗，和截图样式一致，点击外部自动关闭
     * @param triggerComponent 触发按钮，弹窗在按钮下方弹出
     */
    private void showTocPopup(JComponent triggerComponent) {
        if(tocPopup != null && tocPopup.isVisible()){
            return;
        }

        JBScrollPane treeScroll = new JBScrollPane(tocTree);
        // 设置弹窗宽高，适配目录树
        tocPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(treeScroll, tocTree)
                .setTitle(IBooksBundle.message("toolwindow.catalogue"))
                .setResizable(true)      //支持拖拽调整弹窗大小
                .setMovable(true)        //弹窗可以拖动
                .setCancelOnClickOutside(true) //点击弹窗外部自动关闭（和截图菜单行为一样）
                .setCancelOnWindowDeactivation(true)
                .setRequestFocus(true)
                .createPopup();

        // 在按钮下方弹出
        tocPopup.showUnderneathOf(triggerComponent);
    }

    private void enableDrop(JComponent component) {
        component.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }
        });
        new DropTarget(component, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent event) {
                event.acceptDrop(DnDConstants.ACTION_COPY);
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) event.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    files.stream()
                            .filter(f -> f.getName().toLowerCase().endsWith(".epub"))
                            .findFirst()
                            .ifPresent(f -> SwingUtilities.invokeLater(() -> openPath(f.toPath())));
                    event.dropComplete(true);
                } catch (Exception e) {
                    event.dropComplete(false);
                }
            }
        });
    }

    @Override
    public void dispose() {
        persistProgress();
        project.putUserData(KEY, null);
    }
}