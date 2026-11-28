package net.runelite.client.plugins.localdev;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import net.runelite.client.ui.PluginPanel;

final class TheatreControlsPanel extends PluginPanel
{
	private final JButton createLobby = new JButton("Create lobby");
	private final JButton startRaid = new JButton("Start raid");
	private final JSpinner startingWave = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
	private final JRadioButton normalHits = new JRadioButton("Normal hits", true);
	private final JRadioButton maxHitAll = new JRadioButton("Max hit: all weapons");
	private final JRadioButton maxHitSlow = new JRadioButton("Max hit: 5 ticks or slower");
	private final JCheckBox hitpoints250 = new JCheckBox("Start everyone at 250 HP");
	private final JCheckBox zeal = new JCheckBox("Replace Piety with Zeal", true);
	private final JTextField hostName = new JTextField(8);
	private final PluginPanel raidOptions = new PluginPanel() { };
	private final JButton joinRaid = new JButton("Join raid");
	private final JLabel status = new JLabel();
	private boolean loggedIn;
	private boolean raidStarted;
	private final Consumer<String> submit;

	TheatreControlsPanel(Consumer<String> submit)
	{
		this.submit = submit;
		createLobby.setToolTipText("Create a lobby at the Bloat door for up to five players.");
		createLobby.addActionListener(event ->
		{
			String options = selectedOptions();
			if (options != null)
			{
				submit.accept("theatrecreate");
				submit.accept("theatreoptions " + options);
			}
		});
		JPanel actions = new JPanel(new GridLayout(1, 2, 4, 0));
		actions.setOpaque(false);
		actions.add(createLobby);
		actions.add(startRaid);
		add(actions);
		JPanel wave = new JPanel(new BorderLayout(4, 0));
		wave.setOpaque(false);
		wave.add(new JLabel("Starting wave (1–31)"), BorderLayout.CENTER);
		wave.add(startingWave, BorderLayout.EAST);
		raidOptions.add(wave);
		startingWave.setToolTipText("The host's setting applies to everyone. Wave 21 onwards uses a cap of 24 nylos.");
		raidOptions.add(new JLabel("Correct-style Nylocas hits"));
		ButtonGroup maxHitMode = new ButtonGroup();
		for (JRadioButton option : new JRadioButton[]{normalHits, maxHitAll, maxHitSlow})
		{
			maxHitMode.add(option);
			raidOptions.add(option);
		}
		maxHitSlow.setToolTipText("Uses attack speed with the selected combat style, including Rapid.");
		hitpoints250.setToolTipText("Sets every party member's current HP to 250 when the raid starts.");
		raidOptions.add(hitpoints250);
		zeal.setToolTipText("Piety becomes Zeal for everyone: +25% Attack, +28% Strength, +25% Defence, with the same prayer drain.");
		raidOptions.add(zeal);
		((JSpinner.DefaultEditor) startingWave.getEditor()).getTextField().getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				saveOptions();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				saveOptions();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				saveOptions();
			}
		});
		for (AbstractButton option : new AbstractButton[]{normalHits, maxHitAll, maxHitSlow, hitpoints250, zeal})
		{
			option.addActionListener(event -> saveOptions());
		}
		startRaid.addActionListener(event ->
		{
			String options = selectedOptions();
			if (options != null)
			{
				setRaidStarted(true);
				submit.accept("theatrestart " + options);
			}
		});
		startRaid.setToolTipText("Start immediately, or enter the Bloat passage to start automatically.");
		add(new JLabel("Host name"));
		hostName.setToolTipText("Enter the host's name, including spaces.");
		joinRaid.addActionListener(event ->
		{
			String name = hostName.getText().trim();
			if (!name.matches("[A-Za-z0-9 _-]{1,12}"))
			{
				setStatus("Enter a host name of 1–12 letters, numbers, spaces, underscores or hyphens.");
				return;
			}
			submit.accept("theatrejoin " + name);
		});
		hostName.addActionListener(event -> joinRaid.doClick());
		JPanel join = new JPanel(new BorderLayout(4, 0));
		join.setOpaque(false);
		join.add(hostName, BorderLayout.CENTER);
		join.add(joinRaid, BorderLayout.EAST);
		add(join);
		add(status);
		setLoggedIn(false);
	}

	PluginPanel raidOptions()
	{
		return raidOptions;
	}

	private String selectedOptions()
	{
		try
		{
			String text = ((JSpinner.DefaultEditor) startingWave.getEditor()).getTextField().getText();
			int wave = Integer.parseInt(text.trim());
			if (wave < 1 || wave > 31)
			{
				throw new NumberFormatException();
			}
			return wave + (maxHitAll.isSelected() ? " maxhit-all" : maxHitSlow.isSelected() ? " maxhit-slow" : "")
				+ (hitpoints250.isSelected() ? " hp250" : "") + (zeal.isSelected() ? " zeal" : "");
		}
		catch (NumberFormatException exception)
		{
			setStatus("Enter a starting wave from 1 to 31.");
			return null;
		}
	}

	private void saveOptions()
	{
		if (loggedIn && !raidStarted)
		{
			String options = selectedOptions();
			if (options != null)
			{
				submit.accept("theatreoptions " + options);
			}
		}
	}

	void setLoggedIn(boolean loggedIn)
	{
		this.loggedIn = loggedIn;
		updateControls();
		setStatus(loggedIn ? (raidStarted ? "Raid started. Options locked."
			: "Enter the Bloat passage to start.") : "Log in to use Theatre controls.");
	}

	void setRaidStarted(boolean raidStarted)
	{
		if (this.raidStarted == raidStarted)
		{
			return;
		}
		this.raidStarted = raidStarted;
		setLoggedIn(loggedIn);
	}

	private void updateControls()
	{
		createLobby.setEnabled(loggedIn);
		startRaid.setEnabled(loggedIn && !raidStarted);
		startingWave.setEnabled(loggedIn && !raidStarted);
		normalHits.setEnabled(loggedIn && !raidStarted);
		maxHitAll.setEnabled(loggedIn && !raidStarted);
		maxHitSlow.setEnabled(loggedIn && !raidStarted);
		hitpoints250.setEnabled(loggedIn && !raidStarted);
		zeal.setEnabled(loggedIn && !raidStarted);
		hostName.setEnabled(loggedIn);
		joinRaid.setEnabled(loggedIn);
	}

	void setStatus(String message)
	{
		status.setText("<html><body style='width: 150px'>" + message + "</html>");
	}
}
