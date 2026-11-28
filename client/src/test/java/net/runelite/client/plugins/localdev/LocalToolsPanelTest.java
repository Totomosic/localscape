package net.runelite.client.plugins.localdev;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocalToolsPanelTest
{
	@Test
	public void sectionsFollowRequestedOrderAndCollapseIndependentlyWithoutLosingInputs() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			TheatreControlsPanel theatre = new TheatreControlsPanel(command -> { });
			PluginPanel gear = new PluginPanel() { };
			LocalDevPanel debug = new LocalDevPanel(command -> { });
			try
			{
				LocalToolsPanel panel = new LocalToolsPanel(command -> { }, theatre, gear, debug);
				JToggleButton theatreHeader = header(panel, 0);
				JToggleButton optionsHeader = header(panel, 1);
				JToggleButton botsHeader = header(panel, 2);
				JToggleButton gearHeader = header(panel, 3);
				JToggleButton debugHeader = header(panel, 4);
				assertEquals("▼ Theatre controls", theatreHeader.getText());
				assertEquals("▶ Raid options", optionsHeader.getText());
				assertEquals("▶ Nylocas bots", botsHeader.getText());
				assertEquals("▶ Gear setups", gearHeader.getText());
				assertEquals("▶ Debug tools", debugHeader.getText());
				assertEquals(6, panel.getComponentCount());
				assertTrue(theatre.isVisible());
				assertFalse(gear.isVisible());
				assertFalse(debug.isVisible());
				assertFalse(theatre.raidOptions().isVisible());
				TheatreControlsPanelTest.wave(theatre).setValue(21);
				optionsHeader.doClick();
				assertTrue(theatre.raidOptions().isVisible());
				optionsHeader.doClick();
				assertFalse(theatre.raidOptions().isVisible());
				assertEquals(21, TheatreControlsPanelTest.wave(theatre).getValue());
				int expandedHeight = panel.getPreferredSize().height;
				theatreHeader.doClick();
				assertFalse(theatre.isVisible());
				assertTrue(panel.getPreferredSize().height < expandedHeight);
				gearHeader.doClick();
				debugHeader.doClick();
				assertFalse(theatre.isVisible());
				assertTrue(gear.isVisible());
				assertTrue(debug.isVisible());
				theatreHeader.doClick();
				assertTrue(theatre.isVisible());
				assertEquals(21, TheatreControlsPanelTest.wave(theatre).getValue());
				assertTrue(gear.isVisible());
				assertTrue(debug.isVisible());
			}
			finally
			{
				debug.close();
			}
		});
	}

	private static JToggleButton header(LocalToolsPanel panel, int index)
	{
		JPanel section = (JPanel) panel.getComponent(index + 1);
		return (JToggleButton) ((BorderLayout) section.getLayout()).getLayoutComponent(BorderLayout.NORTH);
	}
}
