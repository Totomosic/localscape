package net.runelite.client.plugins.localdev;

import java.awt.BorderLayout;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.PluginPanel;

final class NylocasBotsPanel extends PluginPanel
{
	private final JComboBox<String> strategy = new JComboBox<>(new String[]{"Strategy 1", "Strategy 2"});
	private final JButton strategyInfo = new JButton("Info");
	private final JLabel strategyDetails = new JLabel();
	private final JButton addRanger = new JButton("Add ranger bot");
	private final JButton addMage = new JButton("Add mage bot");
	private final JButton spectate = new JButton("Spectate bot duo");
	private boolean loggedIn;
	private boolean raidStarted;

	NylocasBotsPanel(Consumer<String> submit)
	{
		add(new JLabel("<html><body style='width: 150px'>Add bots in a solo lobby before entering the Bloat passage.</html>"));
		add(new JLabel("Bot strategy"));
		strategy.setToolTipText("Applies when adding bots. Spectate uses the selected strategy for both bots.");
		JPanel selection = new JPanel(new BorderLayout(4, 0));
		selection.setOpaque(false);
		selection.add(strategy, BorderLayout.CENTER);
		strategyInfo.setToolTipText("Show information about the selected strategy.");
		strategyInfo.getAccessibleContext().setAccessibleName("Strategy info");
		strategyInfo.addActionListener(event ->
		{
			strategyDetails.setVisible(!strategyDetails.isVisible());
			strategyInfo.setText(strategyDetails.isVisible() ? "Hide" : "Info");
			revalidate();
			repaint();
		});
		selection.add(strategyInfo, BorderLayout.EAST);
		add(selection);
		strategyDetails.setVisible(false);
		JPanel details = new JPanel(new BorderLayout());
		details.setOpaque(false);
		details.add(strategyDetails, BorderLayout.CENTER);
		add(details);
		strategy.addActionListener(event -> updateStrategyDetails());
		updateStrategyDetails();
		addRanger.setToolTipText("Add a ranger as your partner in a two-player raid.");
		addMage.setToolTipText("Add a mage as your partner in a two-player raid.");
		spectate.setToolTipText("Watch a ranger and mage. Stay outside the Nylocas barrier after passing the Bloat exit.");
		addRanger.addActionListener(event -> submit.accept(command("range")));
		addMage.addActionListener(event -> submit.accept(command("mage")));
		spectate.addActionListener(event -> submit.accept(command("spectate")));
		add(addRanger);
		add(addMage);
		add(spectate);
		updateControls();
	}

	private void updateStrategyDetails()
	{
		String details = strategy.getSelectedIndex() == 0
			? "<b>Strategy 1 — Original duo</b><br><br>"
				+ "<b>Ranger:</b> Toxic blowpipe, twisted bow and black chinchompas for ranged targets; Eye of Ayak for magic. Prioritises ranged, then magic, then melee targets.<br><br>"
				+ "<b>Mage:</b> Eye of Ayak and barrages for magic targets, scythe and Dinh's bulwark for melee. Prioritises melee, then magic, then ranged targets.<br><br>"
				+ "Uses the original lane assignments and special-attack rotation."
			: "<b>Strategy 2 — Revised duo</b><br><br>"
				+ "<b>Ranger:</b> Ascension crossbows replace the blowpipe; Zorya's tome replaces Eye of Ayak. Uses tome specials and stored faster attacks for magic targets.<br><br>"
				+ "<b>Assignments:</b> Mage handles the wave 6 east melee big. Ranger takes the wave 11 west magic big and wave 24 south magic big.<br><br>"
				+ "<b>Mage:</b> Same gear as Strategy 1, with revised priorities and Dinh's special timing, including a delayed wave 22 special."
				+ "<br><br>Spectate applies the selected strategy to both bots.";
		strategyDetails.setText("<html><body style='width: 150px'>" + details + "</html>");
		revalidate();
		repaint();
	}

	void setLoggedIn(boolean loggedIn)
	{
		this.loggedIn = loggedIn;
		updateControls();
	}

	void setRaidStarted(boolean raidStarted)
	{
		this.raidStarted = raidStarted;
		updateControls();
	}

	private String command(String role)
	{
		return "nylobot " + role + " " + (strategy.getSelectedIndex() + 1);
	}

	private void updateControls()
	{
		boolean enabled = loggedIn && !raidStarted;
		strategy.setEnabled(enabled);
		addRanger.setEnabled(enabled);
		addMage.setEnabled(enabled);
		spectate.setEnabled(enabled);
	}
}
