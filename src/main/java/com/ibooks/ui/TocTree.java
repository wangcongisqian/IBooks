package com.ibooks.ui;

import com.ibooks.domain.ParsedBook;
import com.ibooks.domain.TocNode;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.function.Consumer;

/**
 * TocTree类是一个继承自Tree的最终类，用于表示目录树结构。
 * 它能够处理目录节点的点击事件，并根据需要触发相应的链接处理程序。
 */
public final class TocTree extends Tree {
    // 用于处理目录链接的消费者接口，默认为空操作
    private Consumer<String> hrefHandler = href -> {};

    /**
     * 构造函数，初始化目录树的基本属性和选择事件监听器
     */
    public TocTree() {
        // 设置根节点不可见
        setRootVisible(false);
        // 显示根节点的展开/折叠句柄
        setShowsRootHandles(true);
        // 设置默认的树模型，根节点为"toc"
        setModel(new DefaultTreeModel(new DefaultMutableTreeNode("toc")));
        // 添加树选择事件监听器
        addTreeSelectionListener(e -> {
            // 获取最后选择的路径组件
            Object last = getLastSelectedPathComponent();
            // 检查是否为DefaultMutableTreeNode节点且其用户对象为TocNode
            if (last instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof TocNode toc) {
                // 如果TocNode有href且不为空，则触发hrefHandler
                if (toc.href != null && !toc.href.isBlank()) {
                    hrefHandler.accept(toc.href);
                }
            }
        });
    }

    /**
     * 设置链接处理程序
     * @param hrefHandler 处理链接的消费者接口
     */
    public void setHrefHandler(Consumer<String> hrefHandler) {
        this.hrefHandler = hrefHandler;
    }

    /**
     * 设置书籍内容到目录树
     * @param book 已解析的书籍对象
     */
    public void setBook(@NotNull ParsedBook book) {
        // 创建根节点，使用书籍标题
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(book.title);
        // 遍历书籍目录，添加到根节点
        for (TocNode node : book.toc) {
            root.add(toTree(node));
        }
        // 设置新的树模型
        setModel(new DefaultTreeModel(root));
        // 展开第一行
        expandRow(0);
        // 如果有子节点，选择第一个子节点
        if (root.getChildCount() > 0) {
            setSelectionPath(new TreePath(((DefaultMutableTreeNode) root.getFirstChild()).getPath()));
        }
    }

    /**
     * 将TocNode转换为树节点
     * @param node 目录节点
     * @return 对应的树节点
     */
    private static DefaultMutableTreeNode toTree(TocNode node) {
        // 创建树节点，使用TocNode作为用户对象
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
        // 递归处理子节点
        for (TocNode child : node.children) {
            treeNode.add(toTree(child));
        }
        return treeNode;
    }
}
