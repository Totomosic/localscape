package net.runelite.client.plugins.localdev;

import java.util.function.Consumer;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
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
	private final JTextField hostName = new JTextField();
	private final JButton joinRaid = new JButton("Join raid");
	private final JLabel status = new JLabel();
	private boolean loggedIn;
	private boolean raidStarted;

	TheatreControlsPanel(Consumer<String> submit)
	{
		add(new JLabel("Theatre controls"));
		add(new JLabel("<html>Create a lobby at the Bloat door. Up to five players can raid together.</html>"));
		createLobby.addActionListener(event -> submit.accept("theatrecreate"));
		add(createLobby);
		add(new JLabel("Raid options"));
		add(new JLabel("Starting wave (1–31)"));
		startingWave.setToolTipText("The host's setting applies to everyone. Wave 21 onwards uses a cap of 24 nylos.");
		add(startingWave);
		add(new JLabel("Correct-style Nylocas hits"));
		ButtonGroup maxHitMode = new ButtonGroup();
		for (JRadioButton option : new JRadioButton[]{normalHits, maxHitAll, maxHitSlow})
		{
			maxHitMode.add(option);
			add(option);
		}
		maxHitSlow.setToolTipText("Uses attack speed with the selected combat style, including Rapid.");
		hitpoints250.setToolTipText("Sets every party member's current HP to 250 when the raid starts.");
		add(hitpoints250);
		startRaid.addActionListener(event ->
		{
			int wave;
			try
			{
				String text = ((JSpinner.DefaultEditor) startingWave.getEditor()).getTextField().getText();
				wave = Integer.parseInt(text.trim());
				if (wave < 1 || wave > 31)
				{
					throw new NumberFormatException();
				}
			}
			catch (NumberFormatException exception)
			{
				setStatus("Enter a starting wave from 1 to 31.");
				return;
			}
			startingWave.setValue(wave);
			setRaidStarted(true);
			String options = maxHitAll.isSelected() ? " maxhit-all" : maxHitSlow.isSelected() ? " maxhit-slow" : "";
			if (hitpoints250.isSelected())
			{
				options += " hp250";
			}
			submit.accept("theatrestart " + wave + options);
		});
		add(startRaid);
		add(new JLabel("Host's player name"));
		hostName.setToolTipText("Enter the host's name, including spaces.");
		add(hostName);
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
		add(joinRaid);
		add(new JLabel("<html>Join before the host starts. Start raid unlocks the door for the party.</html>"));
		add(status);
		setLoggedIn(false);
	}

	void setLoggedIn(boolean loggedIn)
	{
		this.loggedIn = loggedIn;
		updateControls();
		setStatus(loggedIn ? (raidStarted ? "Raid options are locked. Create a new lobby to change them."
			: "Ready. The host's options apply when starting the raid.") : "Log in to use Theatre controls.");
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
		hostName.setEnabled(loggedIn);
		joinRaid.setEnabled(loggedIn);
	}

	void setStatus(String message)
	{
		status.setText("<html>" + message + "</html>");
	}
}
