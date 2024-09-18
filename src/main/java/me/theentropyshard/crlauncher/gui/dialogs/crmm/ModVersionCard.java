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

package me.theentropyshard.crlauncher.gui.dialogs.crmm;

import me.theentropyshard.crlauncher.CRLauncher;
import me.theentropyshard.crlauncher.cosmic.mods.Mod;
import me.theentropyshard.crlauncher.cosmic.mods.cosmicquilt.QuiltMod;
import me.theentropyshard.crlauncher.cosmic.mods.puzzle.PuzzleMod;
import me.theentropyshard.crlauncher.crmm.model.project.ProjectFile;
import me.theentropyshard.crlauncher.crmm.model.project.ProjectVersion;
import me.theentropyshard.crlauncher.gui.dialogs.ProgressDialog;
import me.theentropyshard.crlauncher.gui.dialogs.instancesettings.tab.mods.ModsTab;
import me.theentropyshard.crlauncher.gui.dialogs.instancesettings.tab.mods.puzzle.PuzzleModsView;
import me.theentropyshard.crlauncher.gui.dialogs.instancesettings.tab.mods.quilt.QuiltModsView;
import me.theentropyshard.crlauncher.gui.utils.MessageBox;
import me.theentropyshard.crlauncher.gui.utils.Worker;
import me.theentropyshard.crlauncher.instance.Instance;
import me.theentropyshard.crlauncher.instance.InstanceType;
import me.theentropyshard.crlauncher.logging.Log;
import me.theentropyshard.crlauncher.network.download.HttpDownload;
import me.theentropyshard.crlauncher.network.progress.ProgressNetworkInterceptor;
import me.theentropyshard.crlauncher.utils.FileUtils;
import me.theentropyshard.crlauncher.utils.StreamUtils;
import me.theentropyshard.crlauncher.utils.json.Json;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import okhttp3.OkHttpClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

public class ModVersionCard extends JPanel {
    private Color defaultColor;
    private Color hoveredColor;
    private Color pressedColor;

    private final int border = 12;

    private boolean mouseOver;
    private boolean mousePressed;

    private final JLabel versionNameLabel;
    private final JButton downloadButton;

    public ModVersionCard(ProjectVersion version, Instance instance, ModsTab modsTab) {
        super(new BorderLayout());

        this.versionNameLabel = new JLabel(version.getVersionNumber());
        this.add(this.versionNameLabel, BorderLayout.WEST);

        this.add(Box.createHorizontalBox(), BorderLayout.CENTER);

        this.downloadButton = new JButton("Download");
        this.downloadButton.addActionListener(e -> {
            new Worker<Mod, Void>("downloading mod " + version.getTitle()) {
                @Override
                protected Mod work() throws Exception {
                    ProjectFile primaryFile = version.getPrimaryFile();

                    ProgressDialog progressDialog = new ProgressDialog("Downloading " + primaryFile.getName());

                    OkHttpClient httpClient = CRLauncher.getInstance().getHttpClient().newBuilder()
                        .addNetworkInterceptor(new ProgressNetworkInterceptor(progressDialog))
                        .build();

                    Path saveAs = switch (instance.getType()) {
                        case VANILLA, FABRIC -> null;
                        case QUILT -> instance.getQuiltModsDir().resolve(primaryFile.getName());
                        case PUZZLE -> instance.getPuzzleModsDir().resolve(primaryFile.getName());
                    };

                    if (saveAs == null) {
                        return null;
                    }

                    HttpDownload download = new HttpDownload.Builder()
                        .url(primaryFile.getUrl())
                        .expectedSize(primaryFile.getSize())
                        .httpClient(httpClient)
                        .saveAs(saveAs)
                        .build();

                    SwingUtilities.invokeLater(() -> progressDialog.setVisible(true));
                    download.execute();
                    SwingUtilities.invokeLater(() -> progressDialog.getDialog().dispose());

                    try (ZipFile file = new ZipFile(saveAs.toFile())) {
                        FileHeader fileHeader = file.getFileHeader(
                            instance.getType() == InstanceType.QUILT ?
                                "quilt.mod.json" : "puzzle.mod.json"
                        );

                        String json = StreamUtils.readToString(file.getInputStream(fileHeader));

                        Mod mod = Json.parse(json,
                            switch (instance.getType()) {
                                case VANILLA, FABRIC -> throw new UnsupportedOperationException();
                                case QUILT -> QuiltMod.class;
                                case PUZZLE -> PuzzleMod.class;
                            }
                        );

                        if (instance.getType() == InstanceType.QUILT) {
                            QuiltMod quiltMod = (QuiltMod) mod;
                            if (instance.getQuiltMods().stream().anyMatch(qMod -> qMod.quiltLoader.id.equals(quiltMod.quiltLoader.id))) {
                                MessageBox.showErrorMessage(CRLauncher.frame, "Mod with id '" + quiltMod.quiltLoader.id + "' already added!");
                                return null;
                            }
                        } else if (instance.getType() == InstanceType.PUZZLE) {
                            PuzzleMod puzzleMod = (PuzzleMod) mod;
                            if (instance.getPuzzleMods().stream().anyMatch(pMod -> puzzleMod.getId().equals(pMod.getId()))) {
                                MessageBox.showErrorMessage(CRLauncher.frame, "Mod with id '" + puzzleMod.getId() + "' already added!");
                                return null;
                            }
                        }

                        mod.setActive(true);

                        return mod;
                    }
                }

                @Override
                protected void done() {
                    Mod mod;
                    try {
                        mod = this.get();
                    } catch (InterruptedException | ExecutionException ex) {
                        Log.error(ex);

                        return;
                    }

                    JPanel modsView = modsTab.getModsView();

                    if (modsView instanceof QuiltModsView quiltModsView) {
                        quiltModsView.getQuiltModsModel().add((QuiltMod) mod);
                    } else if (modsView instanceof PuzzleModsView puzzleModsView) {
                        puzzleModsView.getPuzzleModsModel().add((PuzzleMod) mod);
                    }
                }
            }.execute();
        });
        this.add(this.downloadButton, BorderLayout.EAST);

        this.setOpaque(false);
        this.setDefaultColor(UIManager.getColor("InstanceItem.defaultColor"));
        this.setHoveredColor(UIManager.getColor("InstanceItem.hoveredColor"));
        this.setPressedColor(UIManager.getColor("InstanceItem.pressedColor"));

        this.setBorder(new EmptyBorder(
            this.border,
            this.border,
            this.border,
            this.border
        ));

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                ModVersionCard.this.mouseOver = true;
                ModVersionCard.this.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ModVersionCard.this.mouseOver = false;
                ModVersionCard.this.repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                ModVersionCard.this.mousePressed = true;
                ModVersionCard.this.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                ModVersionCard.this.mousePressed = false;
                ModVersionCard.this.repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        this.paintBackground(g2d);

        super.paintComponent(g2d);
    }

    private void paintBackground(Graphics2D g2d) {
        Color color = this.defaultColor;

        if (this.mouseOver) {
            color = this.hoveredColor;
        }

        if (this.mousePressed) {
            color = this.pressedColor;
        }

        g2d.setColor(color);
        g2d.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 10, 10);
    }

    public void setDefaultColor(Color defaultColor) {
        this.defaultColor = defaultColor;
    }

    public void setHoveredColor(Color hoveredColor) {
        this.hoveredColor = hoveredColor;
    }

    public void setPressedColor(Color pressedColor) {
        this.pressedColor = pressedColor;
    }
}
