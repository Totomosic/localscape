package net.runelite.client.plugins.localdev;

import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import net.runelite.client.ui.PluginPanel;

final class TheatreControlsPanel extends PluginPanel
{
	private final JButton createLobby = new JButton("Create lobby");
	private final JButton startRaid = new JButton("Start raid");
	private final JTextField hostName = new JTextField();
	private final JButton joinRaid = new JButton("Join raid");
	private final JLabel status = new JLabel();

	TheatreControlsPanel(Consumer<String> submit)
	{
		add(new JLabel("Threatre Controls"));
		add(new JLabel("<html>Create a lobby at the Bloat door. Up to five players can raid together.</html>"));
		createLobby.addActionListener(event -> submit.accept("theatrecreate"));
		add(createLobby);
		startRaid.addActionListener(event -> submit.accept("theatrestart"));
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
		createLobby.setEnabled(loggedIn);
		startRaid.setEnabled(loggedIn);
		hostName.setEnabled(loggedIn);
		joinRaid.setEnabled(loggedIn);
		setStatus(loggedIn ? "Ready. See game chat for results." : "Log in to use Theatre controls.");
	}

	void setStatus(String message)
	{
		status.setText("<html>" + message + "</html>");
	}
}
