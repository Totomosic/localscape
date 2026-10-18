package net.runelite.client.plugins.localgear;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.PluginPanel;

final class GearPresetsPanel extends PluginPanel
{
	private final JTextField search = new JTextField();
	private final JTextField name = new JTextField();
	private final JButton save = new JButton("Save current setup");
	private final JButton refresh = new JButton("Refresh setups");
	private final JPanel results = new JPanel();
	private final JLabel summary = new JLabel();
	private final JLabel status = new JLabel();
	private final Consumer<GearPreset> apply;
	private List<GearPreset> presets = Collections.emptyList();
	private boolean loggedIn;
	private boolean busy;

	GearPresetsPanel(Consumer<String> saveSetup, Consumer<GearPreset> apply, Runnable reload)
	{
		this.apply = apply;
		results.setLayout(new BoxLayout(results, BoxLayout.Y_AXIS));
		add(new JLabel("Gear setups"));
		add(new JLabel("<html><body style='width: 150px'>Shared server setups restore your equipment and exact inventory layout.</html>"));
		add(new JLabel("Search saved setups"));
		search.setToolTipText("Filter setups by name.");
		add(search);
		add(summary);
		JScrollPane scroll = new JScrollPane(results);
		scroll.setPreferredSize(new Dimension(200, 300));
		add(scroll);
		refresh.addActionListener(event -> reload.run());
		add(refresh);
		add(new JLabel("Setup name"));
		name.setToolTipText("1–24 letters, numbers, spaces, apostrophes, dots, parentheses, underscores or hyphens.");
		add(name);
		save.addActionListener(event ->
		{
			String value = name.getText().trim();
			if (!value.matches("[A-Za-z0-9 ._()'-]{1,24}") || value.isEmpty())
			{
				setStatus("Enter a setup name of 1–24 letters, numbers, spaces or basic punctuation.");
				return;
			}
			saveSetup.accept(value);
		});
		name.addActionListener(event -> save.doClick());
		add(save);
		add(new JLabel("<html><body style='width: 150px'>Saving adds [Your name] for everyone to see. Save the same name to update your setup. Applying replaces your current gear and inventory.</html>"));
		add(status);
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void insertUpdate(DocumentEvent event) { filter(); }
			@Override public void removeUpdate(DocumentEvent event) { filter(); }
			@Override public void changedUpdate(DocumentEvent event) { filter(); }
		});
		setLoggedIn(false);
		filter();
	}

	void setPresets(List<GearPreset> presets)
	{
		this.presets = new ArrayList<>(presets);
		filter();
	}

	void setLoggedIn(boolean loggedIn)
	{
		this.loggedIn = loggedIn;
		updateControls();
		if (!loggedIn)
		{
			setStatus("Log in to use shared setups.");
		}
	}

	void reset()
	{
		busy = false;
		setPresets(Collections.emptyList());
		name.setText("");
		search.setText("");
		setLoggedIn(false);
	}

	void setBusy(boolean busy)
	{
		this.busy = busy;
		updateControls();
	}

	void setStatus(String message)
	{
		status.setText("<html><body style='width: 150px'>" + message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</html>");
	}

	private void filter()
	{
		results.removeAll();
		String query = search.getText().trim().toLowerCase(Locale.ROOT);
		int count = 0;
		for (GearPreset preset : presets)
		{
			if (!preset.name.toLowerCase(Locale.ROOT).contains(query))
			{
				continue;
			}
			JButton button = new JButton(preset.name);
			button.setAlignmentX(LEFT_ALIGNMENT);
			button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
			button.setToolTipText("Apply " + preset.name + ": replaces equipment and inventory.");
			button.addActionListener(event ->
			{
				name.setText(preset.name.replaceFirst("^\\[[^\\]]+\\] ", ""));
				apply.accept(preset);
			});
			results.add(button);
			results.add(Box.createVerticalStrut(5));
			count++;
		}
		summary.setText(presets.isEmpty() ? "No setups saved yet." : count + " of " + presets.size() + " setups");
		updateControls();
		results.revalidate();
		results.repaint();
	}

	private void updateControls()
	{
		boolean enabled = loggedIn && !busy;
		save.setEnabled(enabled);
		refresh.setEnabled(enabled);
		name.setEnabled(enabled);
		for (java.awt.Component component : results.getComponents())
		{
			component.setEnabled(enabled);
		}
	}
}
