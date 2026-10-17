package net.runelite.client.plugins.localgear;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GearPresetsPanelTest
{
	@Test
	public void searchesAppliesAndSavesNamedSetups() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> saved = new ArrayList<>();
			List<GearPreset> applied = new ArrayList<>();
			GearPresetsPanel panel = new GearPresetsPanel(saved::add, applied::add, () -> { });
			assertFalse(button(panel, "Save current setup").isEnabled());
			panel.setLoggedIn(true);
			GearPreset nylo = new GearPreset("one", "Nylo Duo");
			panel.setPresets(Arrays.asList(nylo, new GearPreset("two", "Melee")));
			fields(panel).get(0).setText("DUO");
			assertEquals(1, buttons(panel, "Nylo Duo").size());
			assertTrue(buttons(panel, "Melee").isEmpty());
			button(panel, "Nylo Duo").doClick();
			assertEquals(Arrays.asList(nylo), applied);
			assertEquals("Nylo Duo", fields(panel).get(1).getText());
			button(panel, "Save current setup").doClick();
			assertEquals(Arrays.asList("Nylo Duo"), saved);
			fields(panel).get(1).setText("<bad>");
			button(panel, "Save current setup").doClick();
			assertEquals(1, saved.size());
			panel.setBusy(true);
			assertFalse(button(panel, "Nylo Duo").isEnabled());
			assertFalse(button(panel, "Save current setup").isEnabled());
			panel.reset();
			assertTrue(buttons(panel, "Nylo Duo").isEmpty());
			assertEquals("", fields(panel).get(1).getText());
			assertFalse(button(panel, "Save current setup").isEnabled());
		});
	}

	static JButton button(Container parent, String text)
	{
		return buttons(parent, text).get(0);
	}

	static List<JButton> buttons(Container parent, String text)
	{
		List<JButton> result = new ArrayList<>();
		for (Component component : parent.getComponents())
		{
			if (component instanceof JButton && text.equals(((JButton) component).getText()))
			{
				result.add((JButton) component);
			}
			if (component instanceof Container)
			{
				result.addAll(buttons((Container) component, text));
			}
		}
		return result;
	}

	static List<JTextField> fields(GearPresetsPanel panel)
	{
		List<JTextField> result = new ArrayList<>();
		for (Component component : panel.getComponents())
		{
			if (component instanceof JTextField)
			{
				result.add((JTextField) component);
			}
		}
		return result;
	}
}
