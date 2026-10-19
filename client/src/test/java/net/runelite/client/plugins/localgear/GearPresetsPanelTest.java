package net.runelite.client.plugins.localgear;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
			GearPresetsPanel panel = new GearPresetsPanel(saved::add, applied::add, () -> { }, preset -> { });
			assertFalse(button(panel, "Save current setup").isEnabled());
			panel.setLoggedIn(true);
			GearPreset nylo = new GearPreset("one", "[Alice] Nylo Duo");
			panel.setPresets(Arrays.asList(nylo, new GearPreset("two", "[Bob] Melee")));
			fields(panel).get(0).setText("ALICE");
			assertEquals(1, buttons(panel, "[Alice] Nylo Duo").size());
			assertTrue(buttons(panel, "[Bob] Melee").isEmpty());
			button(panel, "[Alice] Nylo Duo").doClick();
			assertEquals(Arrays.asList(nylo), applied);
			assertEquals("Nylo Duo", fields(panel).get(1).getText());
			button(panel, "Save current setup").doClick();
			assertEquals(Arrays.asList("Nylo Duo"), saved);
			fields(panel).get(1).setText("<bad>");
			button(panel, "Save current setup").doClick();
			assertEquals(1, saved.size());
			panel.setBusy(true);
			assertFalse(button(panel, "[Alice] Nylo Duo").isEnabled());
			assertFalse(button(panel, "Save current setup").isEnabled());
			panel.reset();
			assertTrue(buttons(panel, "[Alice] Nylo Duo").isEmpty());
			assertEquals("", fields(panel).get(1).getText());
			assertFalse(button(panel, "Save current setup").isEnabled());
		});
	}

	@Test
	public void defaultDropdownIncludesAllSetupsAndPreservesSelectionWithoutSendingDuringRefresh() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<GearPreset> defaults = new ArrayList<>();
			GearPresetsPanel panel = new GearPresetsPanel(name -> { }, preset -> { }, () -> { }, defaults::add);
			JComboBox<?> dropdown = dropdown(panel);
			assertFalse(dropdown.isEnabled());
			assertEquals("None", dropdown.getSelectedItem().toString());
			panel.setLoggedIn(true);
			GearPreset nylo = new GearPreset("one", "[Alice] Nylo Duo");
			GearPreset melee = new GearPreset("two", "[Bob] Melee");
			panel.setPresets(Arrays.asList(nylo, melee));
			panel.setDefaultPreset(nylo.id);
			assertTrue(defaults.isEmpty());
			assertEquals(nylo, dropdown.getSelectedItem());
			fields(panel).get(0).setText("ALICE");
			assertEquals(3, dropdown.getItemCount());
			dropdown.setSelectedIndex(2);
			assertEquals(Arrays.asList(melee), defaults);
			panel.setPresets(Arrays.asList(new GearPreset("one", "Updated"), new GearPreset("two", "Shared")));
			assertEquals("Shared", dropdown.getSelectedItem().toString());
			assertEquals(1, defaults.size());
			dropdown.setSelectedIndex(0);
			assertEquals(null, defaults.get(1).id);
			panel.setBusy(true);
			assertFalse(dropdown.isEnabled());
			panel.reset();
			assertFalse(dropdown.isEnabled());
			assertEquals(1, dropdown.getItemCount());
			assertEquals("None", dropdown.getSelectedItem().toString());
			assertEquals(2, defaults.size());
		});
	}

	static JComboBox<?> dropdown(GearPresetsPanel panel)
	{
		return Arrays.stream(panel.getComponents()).filter(JComboBox.class::isInstance)
			.map(JComboBox.class::cast).findFirst().get();
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
