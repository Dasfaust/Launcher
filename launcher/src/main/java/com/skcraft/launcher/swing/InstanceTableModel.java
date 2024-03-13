/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.swing;

import com.skcraft.launcher.Instance;
import com.skcraft.launcher.InstanceList;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.util.SharedLocale;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class InstanceTableModel extends AbstractTableModel {

    private final InstanceList instances;
    private final Icon missingIcon;
    private HashMap<String, ImageIcon> instanceIcons = new HashMap<>();

    public InstanceTableModel(InstanceList instances) {
        this.instances = instances;
        missingIcon = SwingHelper.createIcon(Launcher.class, "missing_remote.png", 64, 64);
    }

    public void update() {
        instances.sort();
        fireTableDataChanged();
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "";
            case 1:
                return SharedLocale.tr("launcher.modpackColumn");
            default:
                return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return ImageIcon.class;
            case 1:
                return String.class;
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                instances.get(rowIndex).setSelected((boolean) (Boolean) value);
                break;
            case 1:
            default:
                break;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return false;
            case 1:
                return false;
            default:
                return false;
        }
    }

    @Override
    public int getRowCount() {
        return instances.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Instance instance;
        switch (columnIndex) {
            case 0:
                instance = instances.get(rowIndex);

                try {
                    if (instanceIcons.containsKey(instance.getName())) {
                        return instanceIcons.get(instance.getName());
                    } else {
                        BufferedImage img = ImageIO.read(instance.getIconUrl());
                        instanceIcons.put(instance.getName(), new ImageIcon(img));
                        return instanceIcons.get(instance.getName());
                    }
                } catch (IOException e) {
                    return missingIcon;
                }
            case 1:
                instance = instances.get(rowIndex);
                return "<html><strong>" + instance.getTitle() + "</strong><br><em>" + instance.getDomainName() + "</em><br>(v" + instance.getVersion() + ")</html>";
            default:
                return null;
        }
    }

}
