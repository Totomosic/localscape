package net.runelite.client.plugins.localdev;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NylocasWavesPanelTest
{
	private static final String ROOM = "e9f61c86-97d0-4be3-823f-c990f342cbee";
	private static final String COUNTS = "31 0 2 1 1 2 1 1 1 1 1 1 1 0 0 1 1 0 0 0 1 0 1 1";

	@Test
	public void rejectsIncompleteInvalidAndUnrelatedSnapshots()
	{
		assertNull(NylocasWaveSnapshot.parse(null));
		assertNull(NylocasWaveSnapshot.parse("LGP1 hello"));
		assertNull(NylocasWaveSnapshot.parse("LNW1 " + ROOM + " cleanup 31"));
		assertNull(NylocasWaveSnapshot.parse("LNW1 bad cleanup " + COUNTS));
		assertNull(NylocasWaveSnapshot.parse("LNW1 " + ROOM + " unknown " + COUNTS));
		assertNull(NylocasWaveSnapshot.parse("LNW1 " + ROOM + " complete " + COUNTS.replaceFirst("31", "32")));
		assertNull(NylocasWaveSnapshot.parse("LNW1 " + ROOM + " complete " + COUNTS.replaceFirst("31", "-1")));
		assertNull(NylocasWaveSnapshot.parse("LNW1 " + ROOM + " complete " + COUNTS.replace("2 1 1 1 1", "2 3 1 1 1")));
		assertNotNull(NylocasWaveSnapshot.parse("LNW1 " + ROOM + " complete " + COUNTS));
	}

	@Test
	public void displaysPercentagesAndFinalReportUntilNextRoomSnapshot() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			NylocasWavesPanel panel = new NylocasWavesPanel();
			panel.update(NylocasWaveSnapshot.parse("LNW1 " + ROOM + " complete " + COUNTS));
			List<String> labels = labels(panel);
			assertTrue(labels.contains("50.0% (1/2)"));
			assertTrue(labels.contains("100.0% (1/1)"));
			assertTrue(labels.stream().anyMatch(text -> text.contains("Waves complete")));
			assertTrue(labels.stream().anyMatch(text -> text.contains("Melee 50.0% (1/2)")));
			panel.setVisible(false);
			panel.setVisible(true);
			assertTrue(labels(panel).contains("50.0% (1/2)"));
			panel.update(NylocasWaveSnapshot.parse("LNW1 6a24f9d1-7ae2-4eb3-b916-f590ca2e4da2 starting "
				+ "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0"));
			assertTrue(labels(panel).contains("— (0/0)"));
			assertTrue(labels(panel).stream().anyMatch(text -> text.contains("Starting room")));
		});
	}

	private static List<String> labels(Container container)
	{
		List<String> labels = new ArrayList<>();
		for (Component child : container.getComponents())
		{
			if (child instanceof JLabel)
			{
				labels.add(((JLabel) child).getText());
			}
			if (child instanceof Container)
			{
				labels.addAll(labels((Container) child));
			}
		}
		return labels;
	}
}
