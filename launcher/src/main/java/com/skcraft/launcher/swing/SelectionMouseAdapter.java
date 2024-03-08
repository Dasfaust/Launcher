package com.skcraft.launcher.swing;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * An implementation of MouseAdapter that makes it easier to handle left click selection.
 */
public abstract class SelectionMouseAdapter extends MouseAdapter {

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            onSelected(e);
        }
    }

    protected abstract void onSelected(MouseEvent e);
}
