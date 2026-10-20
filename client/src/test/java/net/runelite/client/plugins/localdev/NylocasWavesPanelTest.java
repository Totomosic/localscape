package net.runelite.client.plugins.localdev;

import java.awt.Component;
import java.awt.Container;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.laf.RuneLiteLAF;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NylocasWavesPanelTest
{
	private static final String ROOM = "e9f61c86-97d0-4be3-823f-c990f342cbee";
	private static final String COUNTS = "31 0 2 1 1 2 1 1 1 1 1 1 1 0 0 1 1 0 0 0 1 0 1 1 2 1 1000000 1 1 500000 3 0 1500000 1 0 250000 0";
	private static final String FIRST_LUCK = "1 1 500000 0 0 0 0 0 0 0 0 0 0 750000";
	private static final String SECOND_LUCK = "1 0 500000 0 0 0 0 0 0 0 0 0 0 250000";
	private static final String EMPTY_LUCK = "0 0 0 0 0 0 0 0 0 0 0 0 0 0";

	@Test
	public void displaysSeparateNamedLuckRowsWithIndividualBreakdownsAndResetsRoster() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			NylocasWavesPanel panel = new NylocasWavesPanel();
			String report = "LNW4 " + ROOM + " complete " + COUNTS + " 500000 3 "
				+ player("First Raider", FIRST_LUCK) + " " + player("Second", SECOND_LUCK) + " " + player("Idle", EMPTY_LUCK);
			panel.update(NylocasWaveSnapshot.parse(report));
			panel.setSize(225, panel.getPreferredSize().height);
			layout(panel);
			List<JLabel> components = labelComponents(panel);
			for (String[] expected : new String[][]{{"First Raider", "200.0%", "75.0"}, {"Second", "0.0%", "25.0"}, {"Idle", "—", "—"}})
			{
				JLabel name = components.stream().filter(label -> expected[0].equals(label.getText())).findFirst().get();
				assertTrue(name.isVisible());
				assertTrue(name.getPreferredSize().width <= name.getWidth());
				JLabel value = (JLabel) name.getParent().getComponent(1);
				assertTrue(value.getPreferredSize().width <= value.getWidth());
				assertEquals(expected[1], ((JLabel) name.getParent().getComponent(1)).getText());
				assertTrue(name.getToolTipText().contains("Luck percentile: " + expected[2]));
				assertTrue(name.getToolTipText().contains("Pre cap"));
			}
			panel.update(NylocasWaveSnapshot.parse(report.replace(FIRST_LUCK, SECOND_LUCK)));
			JLabel first = components.stream().filter(label -> "First Raider".equals(label.getText())).findFirst().get();
			assertEquals("0.0%", ((JLabel) first.getParent().getComponent(1)).getText());
			panel.addCheck(NylocasWaveCheck.parse("LNC1 6a24f9d1-7ae2-4eb3-b916-f590ca2e4da2 1 4 1 0 0 12"));
			assertFalse(labels(panel).contains("First Raider"));
			panel.update(NylocasWaveSnapshot.parse("LNW4 6a24f9d1-7ae2-4eb3-b916-f590ca2e4da2 starting "
				+ String.join(" ", java.util.Collections.nCopies(38, "0")) + " 1 " + player("New Raider", EMPTY_LUCK)));
			assertTrue(labels(panel).contains("New Raider"));
			assertFalse(labels(panel).contains("Second"));
		});
	}

	@Test
	public void rejectsMalformedPlayerLuckWithoutReplacingTheSnapshot()
	{
		String header = "LNW4 " + ROOM + " complete " + COUNTS + " 500000";
		assertNull(NylocasWaveSnapshot.parse(header));
		assertNull(NylocasWaveSnapshot.parse(header + " 1"));
		assertNull(NylocasWaveSnapshot.parse(header + " -1"));
		assertNull(NylocasWaveSnapshot.parse(header + " 6"));
		assertNull(NylocasWaveSnapshot.parse(header + " 1 %%% " + FIRST_LUCK));
		assertNull(NylocasWaveSnapshot.parse(header + " 1 " + player("First Raider", FIRST_LUCK.replace("750000", "1000001"))));
		assertNull(NylocasWaveSnapshot.parse(header + " 1 " + player("First Raider", FIRST_LUCK.replace("1 1 500000", "1 2 500000"))));
		assertNull(NylocasWaveSnapshot.parse(header + " 2 " + player("Same", FIRST_LUCK) + " " + player("Same", SECOND_LUCK)));
		assertNotNull(NylocasWaveSnapshot.parse(header + " 0"));
	}

	private static String player(String name, String luck)
	{
		return Base64.getUrlEncoder().withoutPadding().encodeToString(name.getBytes(StandardCharsets.UTF_8)) + " " + luck;
	}

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
			assertTrue(labels.contains("<html>Melee 1<br>Ranged 1<br>Magic 0</html>"));
			assertTrue(labels.contains("<html>Melee 0<br>Ranged 0<br>Magic 1</html>"));
			assertTrue(labels.contains("<html>Melee 0<br>Ranged 1<br>Magic 1</html>"));
			assertFalse(labels.stream().anyMatch(text -> text.contains("of all splits")));
			panel.setVisible(false);
			panel.setVisible(true);
			assertTrue(labels(panel).contains("50.0% (1/2)"));
			panel.update(NylocasWaveSnapshot.parse("LNW2 6a24f9d1-7ae2-4eb3-b916-f590ca2e4da2 starting "
				+ "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0"));
			assertTrue(labels(panel).contains("— (0/0)"));
			assertTrue(labels(panel).stream().anyMatch(text -> text.contains("Starting room")));
			assertEquals(3, labels(panel).stream().filter(text -> text.equals("<html>Melee 0<br>Ranged 0<br>Magic 0</html>")).count());
		});
	}

	@Test
	public void validatesAndDisplaysAttackLuckPercentileAndSplitBoundaries() throws Exception
	{
		assertNull(NylocasWaveSnapshot.parse("LNW3 " + ROOM + " complete " + COUNTS));
		assertNull(NylocasWaveSnapshot.parse("LNW3 " + ROOM + " complete " + COUNTS + " -1"));
		assertNull(NylocasWaveSnapshot.parse("LNW3 " + ROOM + " complete " + COUNTS + " 1000001"));
		assertFalse(NylocasWaveSnapshot.parse("LNW2 " + ROOM + " complete " + COUNTS).hasLuckPercentile());
		SwingUtilities.invokeAndWait(() ->
		{
			NylocasWavesPanel panel = new NylocasWavesPanel();
			for (int rank : new int[]{0, 500000, 955000, 1000000})
			{
				panel.update(NylocasWaveSnapshot.parse("LNW3 " + ROOM + " complete " + COUNTS + " " + rank));
				assertTrue(labels(panel).contains(String.format(java.util.Locale.ROOT, "%.1f", rank / 10000.0)));
			}
			assertTrue(labels(panel).contains("Luck percentile"));
			assertTrue(labels(panel).contains("Post cap · waves 21–28"));
			assertTrue(labels(panel).contains("Cleanup · wave 29 onward"));
			assertTrue(tooltips(panel).stream().anyMatch(text -> text.contains("including all its splats")));
			panel.update(NylocasWaveSnapshot.parse("LNW2 " + ROOM + " complete " + COUNTS));
			List<String> labels = labels(panel);
			assertEquals("—", labels.get(labels.indexOf("Luck percentile") + 1));
			String empty = "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0";
			panel.update(NylocasWaveSnapshot.parse("LNW3 " + ROOM + " starting " + empty));
			labels = labels(panel);
			assertEquals("—", labels.get(labels.indexOf("Luck percentile") + 1));
		});
	}

	@Test
	public void usesScalableFontsWithRuneLiteLookAndFeel() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			LookAndFeel previous = UIManager.getLookAndFeel();
			try
			{
				UIManager.setLookAndFeel(new RuneLiteLAF());
				NylocasWavesPanel panel = new NylocasWavesPanel();
				panel.setSize(225, panel.getPreferredSize().height);
				layout(panel);
				for (JLabel label : labelComponents(panel))
				{
					assertTrue(label.getText(), label.getPreferredSize().width <= label.getWidth());
					assertEquals(label.getText(), FontManager.getDefaultFont().getFamily(), label.getFont().getFamily());
					assertEquals(FontManager.getDefaultFont().getFamily(), label.createToolTip().getFont().getFamily());
				}
				assertEquals(FontManager.getDefaultFont().getFamily(), table(panel).getFont().getFamily());
				assertEquals(FontManager.getDefaultFont().getFamily(), table(panel).getTableHeader().getFont().getFamily());
			}
			catch (UnsupportedLookAndFeelException e)
			{
				throw new AssertionError(e);
			}
			finally
			{
				try
				{
					UIManager.setLookAndFeel(previous);
				}
				catch (UnsupportedLookAndFeelException e)
				{
					throw new AssertionError(e);
				}
			}
		});
	}

	@Test
	public void luckTooltipRemainsAvailableWhileShownAndRestoresNormalTimeout() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			NylocasWavesPanel panel = new NylocasWavesPanel();
			NylocasWaveSnapshot report = NylocasWaveSnapshot.parse("LNW2 " + ROOM + " complete " + COUNTS);
			panel.update(report);
			ToolTipManager manager = ToolTipManager.sharedInstance();
			int previousDelay = manager.getDismissDelay();
			try
			{
				for (JLabel label : labelComponents(panel))
				{
					if (!label.getText().equals("% luck") && !label.getText().equals("61.5%")) continue;
					JToolTip tooltip = label.createToolTip();
					tooltip.setTipText(label.getToolTipText());
					tooltip.addNotify();
					try
					{
						assertEquals(Integer.MAX_VALUE, manager.getDismissDelay());
						panel.update(report);
						assertTrue(tooltip.getTipText().contains("Whole room"));
					}
					finally
					{
						tooltip.removeNotify();
					}
					assertEquals(previousDelay, manager.getDismissDelay());
				}
			}
			finally
			{
				manager.setDismissDelay(previousDelay);
			}
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

	private static void layout(Container container)
	{
		container.doLayout();
		for (Component child : container.getComponents())
		{
			if (child instanceof Container) layout((Container) child);
		}
	}

	private static List<JLabel> labelComponents(Container container)
	{
		List<JLabel> result = new ArrayList<>();
		for (Component child : container.getComponents())
		{
			if (child instanceof JLabel) result.add((JLabel) child);
			if (child instanceof Container) result.addAll(labelComponents((Container) child));
		}
		return result;
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
