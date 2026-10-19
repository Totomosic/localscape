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
				NylocasWavesPanel waves = new NylocasWavesPanel();
				LocalToolsPanel panel = new LocalToolsPanel(theatre, gear, debug, waves);
				JToggleButton theatreHeader = header(panel, 0);
				JToggleButton gearHeader = header(panel, 1);
				JToggleButton debugHeader = header(panel, 2);
				assertEquals("▼ Theatre controls", theatreHeader.getText());
				assertEquals("▶ Gear setups", gearHeader.getText());
				assertEquals("▶ Debug tools", debugHeader.getText());
				assertEquals("▶ Nylocas waves", header(panel, 3).getText());
				assertTrue(theatre.isVisible());
				assertFalse(gear.isVisible());
				assertFalse(debug.isVisible());
				TheatreControlsPanelTest.wave(theatre).setValue(21);
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
		JPanel section = (JPanel) panel.getComponent(index);
		return (JToggleButton) ((BorderLayout) section.getLayout()).getLayoutComponent(BorderLayout.NORTH);
	}
}
