package net.runelite.client.plugins.localdev;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import net.runelite.client.ui.PluginPanel;

final class LocalToolsPanel extends PluginPanel
{
	LocalToolsPanel(PluginPanel theatre, PluginPanel gear, PluginPanel debug, PluginPanel waves)
	{
		addSection("Theatre controls", theatre, true);
		addSection("Gear setups", gear, false);
		addSection("Debug tools", debug, false);
		addSection("Nylocas waves", waves, false);
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
