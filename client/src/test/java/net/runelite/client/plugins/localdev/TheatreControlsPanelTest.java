package net.runelite.client.plugins.localdev;

import java.awt.Component;
import java.awt.Container;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.AbstractButton;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TheatreControlsPanelTest
{
	@Test
	public void defaultsToWaveOneAndLocksOptionsWhenStarting() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> commands = new ArrayList<>();
			TheatreControlsPanel panel = new TheatreControlsPanel(commands::add);
			JSpinner wave = wave(panel);
			assertEquals(1, wave.getValue());
			assertFalse(wave.isEnabled());
			panel.setLoggedIn(true);
			button(panel, "Start raid").doClick();
			assertEquals(Arrays.asList("theatrestart 1 zeal"), starts(commands));
			assertFalse(wave.isEnabled());
			assertFalse(button(panel, "Start raid").isEnabled());
			assertTrue(button(panel, "Create lobby").isEnabled());
			panel.setLoggedIn(true);
			assertFalse(wave.isEnabled());
			panel.setRaidStarted(false);
			assertTrue(wave.isEnabled());
		});
	}

	@Test
	public void submitsTheTypedWaveAndRejectsInvalidInput() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> commands = new ArrayList<>();
			TheatreControlsPanel panel = new TheatreControlsPanel(commands::add);
			panel.setLoggedIn(true);
			JSpinner wave = wave(panel);
			for (String invalid : Arrays.asList("0", "32", "2.5", "abc", "", "9999999999999"))
			{
				((JSpinner.DefaultEditor) wave.getEditor()).getTextField().setText(invalid);
				button(panel, "Start raid").doClick();
				assertTrue(commands.isEmpty());
				assertTrue(wave.isEnabled());
			}
			((JSpinner.DefaultEditor) wave.getEditor()).getTextField().setText("21");
			button(panel, "Start raid").doClick();
			assertEquals(Arrays.asList("theatrestart 21 zeal"), starts(commands));
			assertFalse(wave.isEnabled());
		});
	}

	@Test
	public void maxHitModesAreExclusiveAndSubmittedWithPartyHealth() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> commands = new ArrayList<>();
			TheatreControlsPanel panel = new TheatreControlsPanel(commands::add);
			AbstractButton all = option(panel, "Max hit: all weapons");
			AbstractButton slow = option(panel, "Max hit: 5 ticks or slower");
			AbstractButton hp = option(panel, "Start everyone at 250 HP");
			AbstractButton zeal = option(panel, "Replace Piety with Zeal");
			assertTrue(zeal.isSelected());
			assertFalse(zeal.isEnabled());
			assertFalse(all.isEnabled());
			assertFalse(hp.isSelected());
			panel.setLoggedIn(true);
			all.doClick();
			slow.doClick();
			assertFalse(all.isSelected());
			assertTrue(slow.isSelected());
			hp.doClick();
			button(panel, "Start raid").doClick();
			assertEquals(Arrays.asList("theatrestart 1 maxhit-slow hp250 zeal"), starts(commands));
			assertFalse(all.isEnabled());
			assertFalse(slow.isEnabled());
			assertFalse(hp.isEnabled());
			assertFalse(zeal.isEnabled());
			panel.setRaidStarted(false);
			all.doClick();
			assertFalse(slow.isSelected());
			zeal.doClick();
			button(panel, "Start raid").doClick();
			assertEquals("theatrestart 1 maxhit-all hp250", starts(commands).get(1));
		});
	}

	@Test
	public void savesSelectedOptionsForAutomaticPassageStart() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> commands = new ArrayList<>();
			TheatreControlsPanel panel = new TheatreControlsPanel(commands::add);
			panel.setLoggedIn(true);
			button(panel, "Create lobby").doClick();
			assertEquals(Arrays.asList("theatrecreate", "theatreoptions 1 zeal"), commands);
			wave(panel).setValue(21);
			assertEquals("theatreoptions 21 zeal", commands.get(commands.size() - 1));
			option(panel, "Max hit: 5 ticks or slower").doClick();
			option(panel, "Start everyone at 250 HP").doClick();
			option(panel, "Replace Piety with Zeal").doClick();
			assertEquals("theatreoptions 21 maxhit-slow hp250", commands.get(commands.size() - 1));
			assertTrue(starts(commands).isEmpty());
		});
	}

	private static List<String> starts(List<String> commands)
	{
		return commands.stream().filter(command -> command.startsWith("theatrestart ")).collect(Collectors.toList());
	}

	private static Stream<Component> descendants(Component component)
	{
		return component instanceof Container
			? Stream.concat(Stream.of(component), Arrays.stream(((Container) component).getComponents())
				.flatMap(TheatreControlsPanelTest::descendants))
			: Stream.of(component);
	}

	private static AbstractButton option(TheatreControlsPanel panel, String text)
	{
		return Stream.concat(descendants(panel), descendants(panel.raidOptions())).filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast).filter(button -> button.getText().equals(text)).findFirst().get();
	}

	static JSpinner wave(TheatreControlsPanel panel)
	{
		return Stream.concat(descendants(panel), descendants(panel.raidOptions())).filter(JSpinner.class::isInstance)
			.map(JSpinner.class::cast).findFirst().get();
	}

	private static JButton button(TheatreControlsPanel panel, String text)
	{
		return Stream.concat(descendants(panel), descendants(panel.raidOptions())).filter(JButton.class::isInstance)
			.map(JButton.class::cast).filter(button -> button.getText().equals(text)).findFirst().get();
	}
}
