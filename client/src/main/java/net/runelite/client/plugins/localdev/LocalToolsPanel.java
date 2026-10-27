package net.runelite.client.plugins.localdev;

import java.awt.BorderLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import net.runelite.client.ui.PluginPanel;

final class LocalToolsPanel extends PluginPanel
{
	private final JButton teleportLobby = new JButton("Teleport to lobby");

	LocalToolsPanel(Consumer<String> submit, PluginPanel theatre, PluginPanel gear, PluginPanel debug)
	{
		teleportLobby.setToolTipText("Teleport to the Ver Sinhaza raid lobby.");
		teleportLobby.addActionListener(event -> submit.accept("sinhaza"));
		teleportLobby.setEnabled(false);
		add(teleportLobby);
		addSection("Theatre controls", theatre, true);
		addSection("Gear setups", gear, false);
		addSection("Debug tools", debug, false);
	}

	void setLoggedIn(boolean loggedIn)
	{
		teleportLobby.setEnabled(loggedIn);
	}

	private void addSection(String title, PluginPanel content, boolean expanded)
	{
		JPanel section = new JPanel(new BorderLayout(0, 3));
		section.setOpaque(false);
		content.setBorder(BorderFactory.createEmptyBorder());
		content.setVisible(expanded);
		JToggleButton header = new JToggleButton((expanded ? "▼ " : "▶ ") + title, expanded);
		header.setHorizontalAlignment(SwingConstants.LEFT);
		header.addActionListener(event ->
		{
			header.setText((header.isSelected() ? "▼ " : "▶ ") + title);
			content.setVisible(header.isSelected());
			section.revalidate();
			section.repaint();
		});
		section.add(header, BorderLayout.NORTH);
		section.add(content, BorderLayout.CENTER);
		add(section);
	}
}
