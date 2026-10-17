package net.runelite.client.plugins.localdev;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
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
			assertEquals(Arrays.asList("theatrestart 1"), commands);
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
			assertEquals(Arrays.asList("theatrestart 21"), commands);
			assertFalse(wave.isEnabled());
		});
	}

	static JSpinner wave(TheatreControlsPanel panel)
	{
		return Arrays.stream(panel.getComponents()).filter(JSpinner.class::isInstance)
			.map(JSpinner.class::cast).findFirst().get();
	}

	private static JButton button(TheatreControlsPanel panel, String text)
	{
		return Arrays.stream(panel.getComponents()).filter(JButton.class::isInstance)
			.map(JButton.class::cast).filter(button -> button.getText().equals(text)).findFirst().get();
	}
}
