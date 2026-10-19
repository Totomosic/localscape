package net.runelite.client.plugins.localdev;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NylocasWavesPanelTest
{
	private static final String ROOM = "e9f61c86-97d0-4be3-823f-c990f342cbee";
	private static final String COUNTS = "31 0 2 1 1 2 1 1 1 1 1 1 1 0 0 1 1 0 0 0 1 0 1 1 2 1 1000000 1 1 500000 3 0 1500000 1 0 250000 0";

	@Test
	public void rejectsIncompleteInvalidAndUnrelatedSnapshots()
	{
		assertNull(NylocasWaveSnapshot.parse(null));
		assertNull(NylocasWaveSnapshot.parse("LGP1 hello"));
		assertNull(NylocasWaveSnapshot.parse("LNW2 " + ROOM + " cleanup 31"));
		assertNull(NylocasWaveSnapshot.parse("LNW2 bad cleanup " + COUNTS));
		assertNull(NylocasWaveSnapshot.parse("LNW2 " + ROOM + " unknown " + COUNTS));
		assertNull(NylocasWaveSnapshot.parse("LNW2 " + ROOM + " complete " + COUNTS.replaceFirst("31", "32")));
		assertNull(NylocasWaveSnapshot.parse("LNW2 " + ROOM + " complete " + COUNTS.replaceFirst("31", "-1")));
		assertNull(NylocasWaveSnapshot.parse("LNW2 " + ROOM + " complete " + COUNTS.replace("2 1 1 1 1", "2 3 1 1 1")));
		assertNotNull(NylocasWaveSnapshot.parse("LNW2 " + ROOM + " complete " + COUNTS));
	}

	@Test
	public void displaysPercentagesAndFinalReportUntilNextRoomSnapshot() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			NylocasWavesPanel panel = new NylocasWavesPanel();
			panel.update(NylocasWaveSnapshot.parse("LNW2 " + ROOM + " complete " + COUNTS));
			List<String> labels = labels(panel);
			assertTrue(labels.contains("50.0% (1/2)"));
			assertTrue(labels.contains("61.5%"));
			assertTrue(labels.contains("100.0% (1/1)"));
			assertTrue(labels.stream().anyMatch(text -> text.contains("Waves complete")));
			assertTrue(labels.stream().anyMatch(text -> text.contains("Melee 50.0% (1/2)")));
			panel.setVisible(false);
			panel.setVisible(true);
			assertTrue(labels(panel).contains("50.0% (1/2)"));
			panel.update(NylocasWaveSnapshot.parse("LNW2 6a24f9d1-7ae2-4eb3-b916-f590ca2e4da2 starting "
				+ "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0"));
			assertTrue(labels(panel).contains("— (0/0)"));
			assertTrue(labels(panel).stream().anyMatch(text -> text.contains("Starting room")));
		});
	}

	@Test
	public void timelineKeepsNaturalChecksAndEveryRetryAndResetsForNextRoom() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			NylocasWavesPanel panel = new NylocasWavesPanel();
			panel.addCheck(NylocasWaveCheck.parse("LNC1 " + ROOM + " 1 40 20 0 12 12"));
			panel.addCheck(NylocasWaveCheck.parse("LNC1 " + ROOM + " 2 44 20 1 13 12"));
			panel.addCheck(NylocasWaveCheck.parse("LNC1 " + ROOM + " 3 48 20 2 11 12"));
			panel.addCheck(NylocasWaveCheck.parse("LNC1 " + ROOM + " 3 48 20 2 11 12"));
			panel.update(NylocasWaveSnapshot.parse("LNW2 " + ROOM + " complete " + COUNTS));
			JTable timeline = table(panel);
			assertEquals(3, timeline.getRowCount());
			assertEquals("Natural", timeline.getValueAt(0, 2));
			assertEquals("Retry 2", timeline.getValueAt(2, 2));
			assertEquals(48, timeline.getValueAt(2, 0));
			assertEquals("11 / 12", timeline.getValueAt(2, 3));
			assertTrue(tooltips(panel).stream().anyMatch(text -> text.contains("Pre cap") && text.contains("Post cap") && text.contains("Smalls") && text.contains("Bigs")));
			panel.addCheck(NylocasWaveCheck.parse("LNC1 6a24f9d1-7ae2-4eb3-b916-f590ca2e4da2 1 4 21 0 12 24"));
			assertEquals(1, timeline.getRowCount());
			assertEquals("12 / 24", timeline.getValueAt(0, 3));
		});
	}

	@Test
	public void rejectsMalformedChecksAndLuckCounters()
	{
		assertNull(NylocasWaveCheck.parse("LNC1 " + ROOM + " 1 40 20 0 12"));
		assertNull(NylocasWaveCheck.parse("LNC1 " + ROOM + " 0 40 20 0 12 12"));
		assertNull(NylocasWaveCheck.parse("LNC1 " + ROOM + " 1 40 21 0 12 12"));
		assertNull(NylocasWaveCheck.parse("LNC1 " + ROOM + " 1 -1 20 0 12 12"));
		assertNull(NylocasWaveSnapshot.parse("LNW2 " + ROOM + " complete " + COUNTS.replace("2 1 1000000", "2 3 1000000")));
		assertNull(NylocasWaveSnapshot.parse("LNW2 " + ROOM + " complete " + COUNTS.replace("2 1 1000000", "2 1 3000000")));
	}

	private static JTable table(Container container)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JTable) return (JTable) child;
			if (child instanceof Container)
			{
				JTable found = table((Container) child);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static List<String> tooltips(Container container)
	{
		List<String> result = new ArrayList<>();
		for (Component child : container.getComponents())
		{
			if (child instanceof JLabel && ((JLabel) child).getToolTipText() != null) result.add(((JLabel) child).getToolTipText());
			if (child instanceof Container) result.addAll(tooltips((Container) child));
		}
		return result;
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
