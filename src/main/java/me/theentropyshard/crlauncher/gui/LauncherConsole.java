/*
 * CRLauncher - https://github.com/CRLauncher/CRLauncher
 * Copyright (C) 2024 CRLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package me.theentropyshard.crlauncher.gui;

import me.theentropyshard.crlauncher.CRLauncher;
import me.theentropyshard.crlauncher.Language;
import me.theentropyshard.crlauncher.utils.OperatingSystem;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.WindowListener;

public class LauncherConsole {
    private static final int DEFAULT_X = 80;
    private static final int DEFAULT_Y = 80;
    private static final int INITIAL_WIDTH = 480;
    private static final int INITIAL_HEIGHT = 280;
    private static final int INITIAL_FONT_SIZE = 14;
    private static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, LauncherConsole.INITIAL_FONT_SIZE);

    public static final String SCROLL_DOWN = "gui.console.scrollDown";
    public static final String COPY = "gui.console.copyButton";
    public static final String CLEAR = "gui.console.clearButton";
    public static final String TITLE = "gui.console.title";

    private final JCheckBox scrollDown;
    public static LauncherConsole instance;
    private final JTextPane textPane;
    private final SimpleAttributeSet attrs;
    private final JFrame frame;
    private final JScrollPane scrollPane;
    private final JButton copyButton;
    private final JButton clearButton;

    public LauncherConsole() {
        this.textPane = new JTextPane() {
            @Override
            protected void paintComponent(Graphics g) {
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                super.paintComponent(g);
            }
        };
        this.textPane.setPreferredSize(new Dimension(LauncherConsole.INITIAL_WIDTH, LauncherConsole.INITIAL_HEIGHT));
        this.textPane.setFont(LauncherConsole.FONT);
        ((DefaultCaret) this.textPane.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        this.textPane.setEditable(false);

        this.attrs = new SimpleAttributeSet();

        this.scrollPane = new JScrollPane(
            this.textPane,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        this.scrollPane.setUI(new FlatSmoothScrollPaneUI());

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(this.scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(bottomPanel, BorderLayout.SOUTH);

        Language language = CRLauncher.getInstance().getLanguage();

        this.scrollDown = new JCheckBox(language.getString(LauncherConsole.SCROLL_DOWN));
        this.scrollDown.setSelected(CRLauncher.getInstance().getSettings().consoleScrollDown);
        this.scrollDown.addActionListener(e -> {
            CRLauncher.getInstance().getSettings().consoleScrollDown = this.scrollDown.isSelected();
            this.scrollToBottom();
        });

        bottomPanel.add(this.scrollDown);

        this.copyButton = new JButton(language.getString(LauncherConsole.COPY));
        this.copyButton.addActionListener(e -> {
            OperatingSystem.copyToClipboard(this.textPane.getText());
        });
        bottomPanel.add(this.copyButton);

        this.clearButton = new JButton(language.getString(LauncherConsole.CLEAR));
        this.clearButton.addActionListener(e -> {
            this.textPane.setText("");
        });
        bottomPanel.add(this.clearButton);

        this.frame = new JFrame(language.getString(LauncherConsole.TITLE));
        this.frame.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        this.frame.add(panel, BorderLayout.CENTER);
        this.frame.pack();
        this.frame.setLocation(LauncherConsole.DEFAULT_X, LauncherConsole.DEFAULT_Y);
    }

    private void scrollToBottom() {
        if (this.scrollDown.isSelected()) {
            JScrollBar scrollBar = this.scrollPane.getVerticalScrollBar();
            scrollBar.setValue(scrollBar.getMaximum());
        }
    }

    public JFrame getFrame() {
        return this.frame;
    }

    public void setVisible(boolean visibility) {
        this.frame.setVisible(visibility);
    }

    public void addWindowListener(WindowListener listener) {
        this.frame.addWindowListener(listener);
    }

    public LauncherConsole setColor(Color c) {
        SwingUtilities.invokeLater(() -> {
            StyleConstants.setForeground(this.attrs, c);
        });

        return this;
    }

    public LauncherConsole setBold(boolean bold) {
        SwingUtilities.invokeLater(() -> {
            StyleConstants.setBold(this.attrs, bold);
        });

        return this;
    }

    public void write(String line) {
        SwingUtilities.invokeLater(() -> {
            Document document = this.textPane.getDocument();

            try {
                document.insertString(document.getLength(), line, this.attrs);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }

            this.scrollToBottom();
        });
    }

    public void reloadLanguage() {
        Language language = CRLauncher.getInstance().getLanguage();

        this.frame.setTitle(language.getString(LauncherConsole.TITLE));
        this.scrollDown.setText(language.getString(LauncherConsole.SCROLL_DOWN));
        this.copyButton.setText(language.getString(LauncherConsole.COPY));
        this.clearButton.setText(language.getString(LauncherConsole.CLEAR));
    }
}
