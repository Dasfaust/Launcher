/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.dialog;

import com.skcraft.concurrency.ObservableFuture;
import com.skcraft.launcher.Instance;
import com.skcraft.launcher.InstanceList;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.launch.LaunchListener;
import com.skcraft.launcher.launch.LaunchOptions;
import com.skcraft.launcher.launch.LaunchOptions.UpdatePolicy;
import com.skcraft.launcher.swing.*;
import com.skcraft.launcher.util.SharedLocale;
import com.skcraft.launcher.util.SwingExecutor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import static com.skcraft.launcher.util.SharedLocale.tr;

/**
 * The main launcher frame.
 */
@Log
public class LauncherFrame extends JFrame {

    private final Launcher launcher;

    @Getter
    private final InstanceTable instancesTable = new InstanceTable();
    private final InstanceTableModel instancesModel;
    @Getter
    private final JScrollPane instanceScroll = new JScrollPane(instancesTable);
    private WebpagePanel webViewHome;
    private HashMap<String, WebpagePanel> instanceNewsFeeds = new HashMap<>();
    private JSplitPane splitPane;
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final JButton launchButton = new JButton(SharedLocale.tr("launcher.launch"));
    private final JButton refreshButton = new JButton(SharedLocale.tr("launcher.checkForUpdates"));
    @Getter private final JMenuItem optionsMenu = new JMenuItem(SharedLocale.tr("launcher.options"));
    @Getter private final JMenuItem consoleMenu = new JMenuItem(SharedLocale.tr("options.launcherConsole"));
    @Getter private final JMenuItem aboutMenu = new JMenuItem(SharedLocale.tr("options.about"));
    @Getter private Instance selectedInstance = null;
    private int previousSelectedRow = 0;

    /**
     * Create a new frame.
     *
     * @param launcher the launcher
     */
    public LauncherFrame(@NonNull Launcher launcher) {
        super(tr("launcher.title", launcher.getVersion()));

        this.launcher = launcher;
        instancesModel = new InstanceTableModel(launcher.getInstances());

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(600, 300));
        initComponents();
        pack();
        setLocationRelativeTo(null);

        SwingHelper.setFrameIcon(this, Launcher.class, "icon.png");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                loadInstances();

                if (launcher.getUpdateManager().getPendingUpdate()) {
                    launcher.getUpdateManager().performUpdate(LauncherFrame.this);
                }
            }
        });
    }

    private void initComponents() {
        JPanel container = createContainerPanel();
        container.setLayout(new MigLayout("fill, insets dialog", "[][]push[][]", "[grow][]"));

        tabbedPane.setBorder(null);
        tabbedPane.setOpaque(false);
        webViewHome = createNewsPanel(launcher.getNewsURL());
        tabbedPane.addTab(SharedLocale.tr("launcher.home"), webViewHome);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, instanceScroll, tabbedPane);

        int ctrlKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        optionsMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ctrlKeyMask));
        consoleMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.SHIFT_MASK));

        JMenuBar menuBar;
        JMenu menu;

        menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createEmptyBorder());

        Insets menuInset = new Insets(2, 2, 2, 2);

        menu = new JMenu("File");
        menu.setMargin(menuInset);
        menu.setMnemonic('f');
        menuBar.add(menu);
        menu.add(optionsMenu);
        menu.add(consoleMenu);
        menu.addSeparator();
        menu.add(aboutMenu);

        setJMenuBar(menuBar);

        instancesTable.setModel(instancesModel);
        launchButton.setFont(launchButton.getFont().deriveFont(Font.BOLD));
        splitPane.setDividerLocation(250);
        splitPane.setDividerSize(4);
        splitPane.setOpaque(false);
        container.add(splitPane, "grow, wrap, span 5, gapbottom unrel, w null:680, h null:350");
        SwingHelper.flattenJSplitPane(splitPane);
        container.add(refreshButton);
        launchButton.setEnabled(false);
        container.add(launchButton);

        add(container, BorderLayout.CENTER);

        instancesTable.addMouseListener(new DoubleClickToButtonAdapter(launchButton));

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                previousSelectedRow = instancesTable.getSelectedRow();
                loadInstances();
                webViewHome.browse(launcher.getNewsURL(), false);
                launcher.getUpdateManager().checkForUpdate(LauncherFrame.this);
            }
        });

        optionsMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showOptions();
            }
        });

        consoleMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ConsoleFrame.showMessages();
            }
        });

        aboutMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AboutDialog.showAboutDialog(LauncherFrame.this);
            }
        });

        launchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launch();
            }
        });

        instancesTable.addMouseListener(new PopupMouseAdapter() {
            @Override
            protected void showPopup(MouseEvent e) {
                int index = instancesTable.rowAtPoint(e.getPoint());
                if (index >= 0) {
                    instancesTable.setRowSelectionInterval(index, index);
                    Instance newSelectedInstance = launcher.getInstances().get(index);
                    if (selectedInstance == null || !newSelectedInstance.getName().equals(selectedInstance.getName())) {
                        selectedInstance = newSelectedInstance;
                    }
                    launchButton.setEnabled(true);
                }
                popupInstanceMenu(e.getComponent(), e.getX(), e.getY(), selectedInstance);
            }
        });

        instancesTable.addMouseListener(new SelectionMouseAdapter() {
            @Override
            protected void onSelected(MouseEvent e) {
                int index = instancesTable.rowAtPoint(e.getPoint());
                if (index >= 0) {
                    instancesTable.setRowSelectionInterval(index, index);
                    Instance newSelectedInstance = launcher.getInstances().get(index);
                    launchButton.setEnabled(true);

                    tabbedPane.setSelectedIndex(1);

                    if (selectedInstance == null || !newSelectedInstance.getName().equals(selectedInstance.getName())) {
                        selectedInstance = newSelectedInstance;
                        showInstanceNewsFeed(selectedInstance, true);
                    }
                }
            }
        });
    }

    protected JPanel createContainerPanel() {
        return new JPanel();
    }

    /**
     * Return the news panel.
     *
     * @return the news panel
     */
    protected WebpagePanel createNewsPanel(URL url) {
        return WebpagePanel.forURL(url, false);
    }

    /**
     * Popup the menu for the instances.
     *
     * @param component the component
     * @param x mouse X
     * @param y mouse Y
     * @param selected the selected instance, possibly null
     */
    private void popupInstanceMenu(Component component, int x, int y, final Instance selected) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;

        if (selected != null) {
            menuItem = new JMenuItem(!selected.isLocal() ? tr("instance.install") : tr("instance.launch"));
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    launch();
                }
            });
            popup.add(menuItem);

            if (selected.isLocal()) {
                popup.addSeparator();

                menuItem = new JMenuItem(SharedLocale.tr("instance.openFolder"));
                menuItem.addActionListener(ActionListeners.browseDir(
                        LauncherFrame.this, selected.getContentDir(), true));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.openSaves"));
                menuItem.addActionListener(ActionListeners.browseDir(
                        LauncherFrame.this, new File(selected.getContentDir(), "saves"), true));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.openResourcePacks"));
                menuItem.addActionListener(ActionListeners.browseDir(
                        LauncherFrame.this, new File(selected.getContentDir(), "resourcepacks"), true));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.openScreenshots"));
                menuItem.addActionListener(ActionListeners.browseDir(
                        LauncherFrame.this, new File(selected.getContentDir(), "screenshots"), true));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.copyAsPath"));
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        File dir = selected.getContentDir();
                        dir.mkdirs();
                        SwingHelper.setClipboard(dir.getAbsolutePath());
                    }
                });
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.openSettings"));
                menuItem.addActionListener(e -> {
                    InstanceSettingsDialog.open(this, selected, launcher);
                });
                popup.add(menuItem);

                popup.addSeparator();

                if (!selected.isUpdatePending()) {
                    menuItem = new JMenuItem(SharedLocale.tr("instance.forceUpdate"));
                    menuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            selected.setUpdatePending(true);
                            launch();
                            instancesModel.update();
                        }
                    });
                    popup.add(menuItem);
                }

                menuItem = new JMenuItem(SharedLocale.tr("instance.hardForceUpdate"));
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        confirmHardUpdate(selected);
                    }
                });
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.deleteFiles"));
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        confirmDelete(selected);
                    }
                });
                popup.add(menuItem);
            }

            popup.addSeparator();
        }

        menuItem = new JMenuItem(SharedLocale.tr("launcher.refreshList"));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadInstances();
            }
        });
        popup.add(menuItem);

        popup.show(component, x, y);

    }

    private void confirmDelete(Instance instance) {
        if (!SwingHelper.confirmDialog(this,
                tr("instance.confirmDelete", instance.getTitle()), SharedLocale.tr("confirmTitle"))) {
            return;
        }

        ObservableFuture<Instance> future = launcher.getInstanceTasks().delete(this, instance);

        // Update the list of instances after updating
        future.addListener(new Runnable() {
            @Override
            public void run() {
                loadInstances();
            }
        }, SwingExecutor.INSTANCE);
    }

    private void confirmHardUpdate(Instance instance) {
        if (!SwingHelper.confirmDialog(this, SharedLocale.tr("instance.confirmHardUpdate"), SharedLocale.tr("confirmTitle"))) {
            return;
        }

        ObservableFuture<Instance> future = launcher.getInstanceTasks().hardUpdate(this, instance);

        // Update the list of instances after updating
        future.addListener(new Runnable() {
            @Override
            public void run() {
                launch();
                instancesModel.update();
            }
        }, SwingExecutor.INSTANCE);
    }

    private void loadInstances() {
        ObservableFuture<InstanceList> future = launcher.getInstanceTasks().reloadInstances(this);

        future.addListener(new Runnable() {
            @Override
            public void run() {
                instancesModel.update();

                if (previousSelectedRow < instancesTable.getRowCount()) {
                    instancesTable.setRowSelectionInterval(previousSelectedRow, previousSelectedRow);
                    selectedInstance = launcher.getInstances().get(instancesTable.getSelectedRow());
                } else if (instancesTable.getRowCount() > 0) {
                    instancesTable.setRowSelectionInterval(0, 0);
                    selectedInstance = launcher.getInstances().get(instancesTable.getSelectedRow());
                } else {
                    selectedInstance = null;
                }

                for (int i = 0; i < instancesTable.getRowCount(); i++) {
                    Instance instance = launcher.getInstances().get(i);
                    if (!instanceNewsFeeds.containsKey(instance.getName())) {
                        instanceNewsFeeds.put(instance.getName(), WebpagePanel.forURL(instance.getNewsUrl(), true));
                    }
                }

                if (selectedInstance != null) {
                    launchButton.setEnabled(true);
                    instanceNewsFeeds.get(selectedInstance.getName()).browse(selectedInstance.getNewsUrl(), false);
                    showInstanceNewsFeed(selectedInstance, tabbedPane.getSelectedIndex() != 0);
                }

                requestFocus();
            }
        }, SwingExecutor.INSTANCE);

        ProgressDialog.showProgress(this, future, SharedLocale.tr("launcher.checkingTitle"), SharedLocale.tr("launcher.checkingStatus"));
        SwingHelper.addErrorDialogCallback(this, future);
    }

    private void showInstanceNewsFeed(Instance instance, boolean switchTab) {
        if (tabbedPane.getTabCount() > 1) {
            tabbedPane.remove(1);
        }

        WebpagePanel newsFeed = instanceNewsFeeds.get(instance.getName());
        if (!newsFeed.isActivated()) {
            newsFeed.loadPlaceholder();
        }

        tabbedPane.addTab(instance.getTitle(), newsFeed);

        if (switchTab) {
            if (tabbedPane.getSelectedIndex() == 0) {
                tabbedPane.setSelectedIndex(1);
            }
        }
    }

    private void showOptions() {
        ConfigurationDialog configDialog = new ConfigurationDialog(this, launcher);
        configDialog.setVisible(true);
    }

    private void launch() {
        if (selectedInstance != null) {
            LaunchOptions options = new LaunchOptions.Builder()
                    .setInstance(selectedInstance)
                    .setListener(new LaunchListenerImpl(this))
                    .setUpdatePolicy(UpdatePolicy.UPDATE_IF_SESSION_ONLINE)
                    .setWindow(this)
                    .build();
            launcher.getLaunchSupervisor().launch(options);
        }
    }

    private static class LaunchListenerImpl implements LaunchListener {
        private final WeakReference<LauncherFrame> frameRef;
        private final Launcher launcher;

        private LaunchListenerImpl(LauncherFrame frame) {
            this.frameRef = new WeakReference<LauncherFrame>(frame);
            this.launcher = frame.launcher;
        }

        @Override
        public void instancesUpdated() {
            LauncherFrame frame = frameRef.get();
            if (frame != null) {
                frame.instancesModel.update();
            }
        }

        @Override
        public void gameStarted() {
            LauncherFrame frame = frameRef.get();
            if (frame != null) {
                frame.dispose();
            }
        }

        @Override
        public void gameClosed() {
            Window newLauncherWindow = launcher.showLauncherWindow();
            launcher.getUpdateManager().checkForUpdate(newLauncherWindow);
        }
    }
}
