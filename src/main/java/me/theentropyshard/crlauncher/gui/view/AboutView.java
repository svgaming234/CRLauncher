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

package me.theentropyshard.crlauncher.gui.view;

import me.theentropyshard.crlauncher.BuildConfig;
import me.theentropyshard.crlauncher.utils.OperatingSystem;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;

public class AboutView extends JPanel {
    private static final String GITHUB_LINK = "https://github.com/CRLauncher/CRLauncher";

    public AboutView() {
        this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.VERTICAL;

        this.addLine(this, gbc, String.format("CRLauncher %s - simple Cosmic launcher", BuildConfig.APP_VERSION));
        this.addLine(this, gbc, "by TheEntropyShard");

        JTextPane textPane = new JTextPane();
        textPane.setEditorKit(new HTMLEditorKit());
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        textPane.setText("<html> More at <a href=\"" + AboutView.GITHUB_LINK + "\">" + AboutView.GITHUB_LINK + "</a> </html>");
        textPane.addHyperlinkListener(e -> {
            if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
                return;
            }

            OperatingSystem.browse(AboutView.GITHUB_LINK);
        });

        gbc.gridy++;
        this.add(textPane, gbc);
    }

    private void addLine(JPanel panel, GridBagConstraints gbc, String text) {
        gbc.gridy++;
        panel.add(new JLabel(text), gbc);
    }
}
