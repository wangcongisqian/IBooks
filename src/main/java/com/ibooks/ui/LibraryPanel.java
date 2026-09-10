package com.ibooks.ui;

import com.ibooks.IBooksBundle;
import com.ibooks.domain.LibraryBook;
import com.ibooks.service.LibraryService;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class LibraryPanel extends JPanel {
    private final DefaultListModel<LibraryBook> model = new DefaultListModel<>();
    private final JBList<LibraryBook> list = new JBList<>(model);

    public LibraryPanel(Consumer<Path> openHandler, Consumer<String> removeHandler) {
        super(new BorderLayout());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setEmptyText(IBooksBundle.message("library.empty"));
        list.setCellRenderer(new BookRenderer());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                LibraryBook book = list.getSelectedValue();
                if (book != null && book.path != null) {
                    openHandler.accept(Path.of(book.path));
                }
            }
        });
        list.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("DELETE"), "remove");
        list.getActionMap().put("remove", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                LibraryBook book = list.getSelectedValue();
                if (book != null) {
                    removeHandler.accept(book.id);
                    reload();
                }
            }
        });
        add(new JBScrollPane(list), BorderLayout.CENTER);
        reload();
    }

    public void reload() {
        model.clear();
        for (LibraryBook book : LibraryService.getInstance().books()) {
            model.addElement(book);
        }
    }

    private static final class BookRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel cell = new JPanel(new BorderLayout());
            cell.setBorder(JBUI.Borders.empty(8, 10));
            cell.setOpaque(true);
            cell.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            if (value instanceof LibraryBook book) {
                JBLabel title = new JBLabel(book.title);
                title.setFont(title.getFont().deriveFont(Font.BOLD));
                title.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
                JBLabel meta = new JBLabel(book.authors + "  ·  " + IBooksBundle.message("library.progress", book.lastPercent));
                meta.setForeground(isSelected ? list.getSelectionForeground() : JBUI.CurrentTheme.Label.disabledForeground());
                cell.add(title, BorderLayout.NORTH);
                cell.add(meta, BorderLayout.SOUTH);
            }
            return cell;
        }
    }

    public @Nullable LibraryBook selected() {
        return list.getSelectedValue();
    }
}
