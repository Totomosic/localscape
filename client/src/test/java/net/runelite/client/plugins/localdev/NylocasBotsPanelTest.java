package net.runelite.client.plugins.localdev;

import java.awt.Component;
import java.awt.Container;
import java.util.stream.Stream;
import javax.swing.JLabel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NylocasBotsPanelTest
{
	@Test
	public void addsEachRoleWithTheSelectedStrategyOnlyOnClick() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> commands = new ArrayList<>();
			NylocasBotsPanel panel = new NylocasBotsPanel(commands::add);
			panel.setLoggedIn(true);
			JComboBox<?> strategy = strategy(panel);
			for (int version = 0; version < 2; version++)
			{
				strategy.setSelectedIndex(version);
				assertEquals(version * 3, commands.size());
				button(panel, "Add ranger bot").doClick();
				button(panel, "Add mage bot").doClick();
				button(panel, "Spectate bot duo").doClick();
			}
			assertEquals(Arrays.asList("nylobot range 1", "nylobot mage 1", "nylobot spectate 1",
				"nylobot range 2", "nylobot mage 2", "nylobot spectate 2"), commands);
		});
	}

	@Test
	public void locksControlsWhileLoggedOutOrRaidStartedAndKeepsSelection() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> commands = new ArrayList<>();
			NylocasBotsPanel panel = new NylocasBotsPanel(commands::add);
			assertControlsEnabled(panel, false);
			panel.setLoggedIn(true);
			assertControlsEnabled(panel, true);
			strategy(panel).setSelectedIndex(1);
			panel.setRaidStarted(true);
			assertControlsEnabled(panel, false);
			button(panel, "Spectate bot duo").doClick();
			assertTrue(commands.isEmpty());
			panel.setLoggedIn(false);
			panel.setRaidStarted(false);
			assertControlsEnabled(panel, false);
			panel.setLoggedIn(true);
			assertControlsEnabled(panel, true);
			assertEquals(1, strategy(panel).getSelectedIndex());
			panel.setLoggedIn(false);
			assertControlsEnabled(panel, false);
		});
	}

	@Test
	public void showsSelectedStrategyAndUpdatesOpenInfoWithoutSendingCommands() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> commands = new ArrayList<>();
			NylocasBotsPanel panel = new NylocasBotsPanel(commands::add);
			JLabel details = descendants(panel).filter(JLabel.class::isInstance).map(JLabel.class::cast)
				.filter(label -> label.getText().contains("Original duo")).findFirst().get();
			assertFalse(details.isVisible());
			int compactHeight = panel.getPreferredSize().height;
			button(panel, "Info").doClick();
			assertTrue(details.isVisible());
			assertTrue(panel.getPreferredSize().height > compactHeight);
			assertTrue(details.getText().contains("Toxic blowpipe"));
			strategy(panel).setSelectedIndex(1);
			assertTrue(details.getText().contains("Ascension crossbows"));
			assertTrue(details.getText().contains("Zorya's tome"));
			assertTrue(details.isVisible());
			button(panel, "Hide").doClick();
			assertFalse(details.isVisible());
			assertEquals(compactHeight, panel.getPreferredSize().height);
			assertTrue(commands.isEmpty());
		});
	}

	private static Stream<Component> descendants(Component component)
	{
		return component instanceof Container
			? Stream.concat(Stream.of(component), Arrays.stream(((Container) component).getComponents())
				.flatMap(NylocasBotsPanelTest::descendants))
			: Stream.of(component);
	}

	private static void assertControlsEnabled(NylocasBotsPanel panel, boolean enabled)
	{
		assertEquals(enabled, strategy(panel).isEnabled());
		for (String text : Arrays.asList("Add ranger bot", "Add mage bot", "Spectate bot duo"))
		{
			JButton button = button(panel, text);
			assertEquals(enabled, button.isEnabled());
		}
	}

	static JButton button(NylocasBotsPanel panel, String text)
	{
		return descendants(panel)
			.filter(component -> component instanceof JButton && ((JButton) component).getText().equals(text))
			.map(component -> (JButton) component).findFirst().get();
	}

	private static JComboBox<?> strategy(NylocasBotsPanel panel)
	{
		return descendants(panel).filter(JComboBox.class::isInstance)
			.map(component -> (JComboBox<?>) component).findFirst().get();
	}
}
