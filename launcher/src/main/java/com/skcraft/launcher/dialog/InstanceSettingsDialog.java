package com.skcraft.launcher.dialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skcraft.launcher.Instance;
import com.skcraft.launcher.InstanceSettings;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.dialog.component.BetterComboBox;
import com.skcraft.launcher.launch.MemorySettings;
import com.skcraft.launcher.launch.runtime.JavaRuntime;
import com.skcraft.launcher.launch.runtime.JavaRuntimeFinder;
import com.skcraft.launcher.model.modpack.Manifest;
import com.skcraft.launcher.persistence.Persistence;
import com.skcraft.launcher.swing.FormPanel;
import com.skcraft.launcher.swing.LinedBoxPanel;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.util.HttpRequest;
import com.skcraft.launcher.util.SharedLocale;
import lombok.NonNull;
import lombok.extern.java.Log;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;

@Log
public class InstanceSettingsDialog extends JDialog {
	private final Instance instance;
	private final Launcher launcher;

	private final LinedBoxPanel formsPanel = new LinedBoxPanel(false);

	private final JCheckBox enableCustomRuntime = new JCheckBox(SharedLocale.tr("instance.options.customJava"), true);
	private final FormPanel runtimePanel = new FormPanel();

	private final FormPanel memorySettingsPanel = new FormPanel();
	private final JSpinner minMemorySpinner = new JSpinner();
	private final JSpinner maxMemorySpinner = new JSpinner();
	private final JComboBox<JavaRuntime> javaRuntimeBox = new JComboBox<>();
	private final JTextArea javaArgsBox = new JTextArea(5, 10);

	private final LinedBoxPanel buttonsPanel = new LinedBoxPanel(true);
	private final JButton restoreButton = new JButton(SharedLocale.tr("button.restore"));
	private final JButton okButton = new JButton(SharedLocale.tr("button.save"));
	private final JButton cancelButton = new JButton(SharedLocale.tr("button.cancel"));

	private boolean saved = false;

	public InstanceSettingsDialog(Window owner, Instance instance, @NonNull Launcher launcher) {
		super(owner);
		this.instance = instance;
		this.launcher = launcher;

		setTitle(SharedLocale.tr("instance.options.title"));
		setModalityType(DEFAULT_MODALITY_TYPE);
		initComponents();
		setSize(new Dimension(400, 500));
		setLocationRelativeTo(owner);
	}

	private void initComponents() {
		memorySettingsPanel.addRow(new JLabel(SharedLocale.tr("options.minMemory")), minMemorySpinner);
		memorySettingsPanel.addRow(new JLabel(SharedLocale.tr("options.maxMemory")), maxMemorySpinner);
		memorySettingsPanel.addRow(new JLabel(SharedLocale.tr("options.jvmArguments")));
		javaArgsBox.setLineWrap(true);
		JScrollPane scroll = SwingHelper.wrapScrollPane(javaArgsBox);
		scroll.setMinimumSize(new Dimension(0, javaArgsBox.getPreferredSize().height));
		memorySettingsPanel.addRow(scroll);

		// TODO: Do we keep this list centrally somewhere? Or is actively refreshing good?
		JavaRuntime[] javaRuntimes = JavaRuntimeFinder.getAvailableRuntimes().toArray(new JavaRuntime[0]);
		javaRuntimeBox.setModel(new DefaultComboBoxModel<>(javaRuntimes));

		runtimePanel.addRow(enableCustomRuntime);
		runtimePanel.addRow(new JLabel(SharedLocale.tr("options.jvmRuntime")), javaRuntimeBox);

		buttonsPanel.addElement(restoreButton);
		buttonsPanel.addGlue();
		buttonsPanel.addElement(cancelButton);
		buttonsPanel.addElement(okButton);

		enableCustomRuntime.addActionListener(e -> {
			javaRuntimeBox.setEnabled(enableCustomRuntime.isSelected());
		});

		okButton.addActionListener(e -> {
			save();
			dispose();
		});

		cancelButton.addActionListener(e -> dispose());

		restoreButton.addActionListener(e -> {
			ObjectMapper mapper = new ObjectMapper();
			try {
				Manifest manifest = mapper.readValue(instance.getManifestPath(), Manifest.class);
				instance.getSettings().setMemorySettings(new MemorySettings());
				instance.getSettings().getMemorySettings().setMinMemory(manifest.getDefaultHeapAllocation() * 1024);
				instance.getSettings().getMemorySettings().setMaxMemory(manifest.getDefaultHeapAllocation() * 1024);
				instance.getSettings().setCustomJvmArgs(manifest.getDefaultJVMArguments());
				updateComponents();
			} catch (Exception ex) {
				log.warning("Could not load manifest.json for instance: ");
				log.warning(ex.toString());
			}
		});

		formsPanel.addElement(memorySettingsPanel);
		formsPanel.addElement(runtimePanel);

		add(formsPanel, BorderLayout.NORTH);
		add(buttonsPanel, BorderLayout.SOUTH);

		updateComponents();
	}

	private void updateComponents() {
		if (launcher.getConfig().isUseInstanceJVMSettings()) {
			minMemorySpinner.setEnabled(true);
			maxMemorySpinner.setEnabled(true);
			javaArgsBox.setEnabled(true);

			MemorySettings memorySettings = instance.getSettings().getMemorySettings();
			if (memorySettings == null) {
				memorySettings = new MemorySettings();
				instance.getSettings().setMemorySettings(memorySettings);
			}
			minMemorySpinner.setValue(memorySettings.getMinMemory());
			maxMemorySpinner.setValue(memorySettings.getMaxMemory());

			javaArgsBox.setText(instance.getSettings().getCustomJvmArgs());
		} else {
			minMemorySpinner.setEnabled(false);
			maxMemorySpinner.setEnabled(false);
			javaArgsBox.setEnabled(false);
		}

		if (instance.getSettings().getRuntime() != null) {
			javaRuntimeBox.setEnabled(true);
			enableCustomRuntime.setSelected(true);
		} else {
			javaRuntimeBox.setEnabled(false);
			enableCustomRuntime.setSelected(false);
		}

		restoreButton.setEnabled(launcher.getConfig().isUseInstanceJVMSettings());
		javaRuntimeBox.setSelectedItem(instance.getSettings().getRuntime());
	}

	private void save() {
		if (launcher.getConfig().isUseInstanceJVMSettings()) {
			MemorySettings memorySettings = instance.getSettings().getMemorySettings();

			memorySettings.setMinMemory((int) minMemorySpinner.getValue());
			memorySettings.setMaxMemory((int) maxMemorySpinner.getValue());

			instance.getSettings().setCustomJvmArgs(javaArgsBox.getText());
		}

		if (enableCustomRuntime.isSelected()) {
			instance.getSettings().setRuntime((JavaRuntime) javaRuntimeBox.getSelectedItem());
		} else {
			instance.getSettings().setRuntime(null);
		}

		saved = true;
	}

	public static boolean open(Window parent, Instance instance, @NonNull Launcher launcher) {
		InstanceSettingsDialog dialog = new InstanceSettingsDialog(parent, instance, launcher);
		dialog.setVisible(true);

		if (dialog.saved) {
			Persistence.commitAndForget(instance);
		}

		return dialog.saved;
	}
}
