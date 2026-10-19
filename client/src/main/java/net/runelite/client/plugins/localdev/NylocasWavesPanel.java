package net.runelite.client.plugins.localdev;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

final class NylocasWavesPanel extends PluginPanel
{
	private static final Font TEXT_FONT = FontManager.getDefaultFont().deriveFont(12f);
	private static final Color ACCENT = new Color(112, 202, 186);
	private static final Color MUTED = new Color(170, 175, 184);
	private final JLabel status = label("Waiting for Nylocas");
	private final JLabel remaining = label("Party report · updates live");
	private final JLabel luck = luckLabel("—");
	private final JLabel luckTitle = luckLabel("% luck");
	private final JLabel percentile;
	private final JLabel smalls;
	private final JLabel bigs;
	private final JLabel[] overkills;
	private final JLabel saved;
	private final JLabel stalls;
	private final JLabel thralls;
	private final JLabel[] splits = new JLabel[3];
	private final JLabel checksTitle = label("Wave-check timeline");
	private final DefaultTableModel checksModel = new DefaultTableModel(new String[]{"Tick", "Wave", "Check", "Alive"}, 0)
	{
		@Override
		public boolean isCellEditable(int row, int column)
		{
			return false;
		}
	};
	private final List<NylocasWaveCheck> checks = new ArrayList<>();
	private String room;

	NylocasWavesPanel()
	{
		setLayout(new DynamicGridLayout(0, 1, 0, 12));
		JPanel header = section("NYLOCAS WAVES");
		status.setFont(status.getFont().deriveFont(Font.BOLD, 14f));
		header.add(status);
		remaining.setForeground(MUTED);
		header.add(remaining);
		JPanel performance = section("One-shot performance");
		row(performance, luckTitle, luck);
		luck.setFont(luck.getFont().deriveFont(Font.BOLD, 20f));
		luck.setForeground(ACCENT);
		percentile = row(performance, label("Luck percentile"));
		percentile.setToolTipText("<html>0–100: higher is luckier; 50 is typical.<br>Exact distribution of one-attack kills using each measured attack's chance.<br>Counts outcomes below yours plus half of ties. Accuracy misses are ignored.</html>");
		smalls = row(performance, label("Smalls"));
		bigs = row(performance, label("Bigs"));
		smalls.setToolTipText("One attack / all combat kills. Includes all splats from that attack. Misses ignored; natural expiries excluded.");
		bigs.setToolTipText(smalls.getToolTipText());
		performance.add(note("100% = expected one-shots. Hover for breakdown."));
		JPanel efficiency = section("Room efficiency");
		stalls = row(efficiency, label("Cap stalls"));
		saved = row(efficiency, label("Stalls saved"));
		thralls = row(efficiency, label("Thrall kills"));
		saved.setToolTipText("Successful cap checks that would have stalled with overkilled bodies removed one tick later, accounting for big splits.");
		thralls.setToolTipText("Nylos whose lethal hitsplat came from a thrall, across the party.");
		JPanel overkill = section("Overkills by Nylo style");
		overkills = new JLabel[]{row(overkill, label("Melee")), row(overkill, label("Ranged")), row(overkill, label("Magic"))};
		for (JLabel value : overkills)
		{
			value.setToolTipText("An extra hitsplat on the lethal tick / combat kills of this Nylo style at death.");
		}
		JPanel splitSection = section("Split children");
		String[] phases = {"Pre cap · waves 1–20", "Post cap · waves 21–28", "Cleanup · wave 29 onward"};
		for (int phase = 0; phase < splits.length; phase++)
		{
			JLabel title = label(phases[phase]);
			title.setBorder(BorderFactory.createEmptyBorder(8, 0, 3, 0));
			title.setForeground(MUTED);
			splitSection.add(title);
			splits[phase] = label("<html>Melee 0<br>Ranged 0<br>Magic 0</html>");
			splitSection.add(splits[phase]);
		}
		JPanel timeline = section(null);
		checksTitle.setFont(checksTitle.getFont().deriveFont(Font.BOLD));
		timeline.add(checksTitle);
		timeline.add(note("Room ticks · green = spawn · red = stall"));
		JTable table = new JTable(checksModel)
		{
			@Override
			public String getToolTipText(MouseEvent event)
			{
				int row = rowAtPoint(event.getPoint());
				if (row < 0) return null;
				NylocasWaveCheck check = checks.get(row);
				return "Wave " + check.wave + (check.attempt == 0 ? ": natural check" : ": retry " + check.attempt)
					+ " at room tick " + check.tick + ". " + check.population + " bodies / cap " + check.cap
					+ (check.population >= check.cap ? ". Stalled; retry in 4 ticks." : ". Wave admitted.")
					+ " Includes dying bodies until removal, before this wave spawns.";
			}
		};
		table.setRowHeight(27);
		table.setShowGrid(false);
		table.setRowSelectionAllowed(false);
		table.setFocusable(false);
		table.setFont(TEXT_FONT.deriveFont(11f));
		table.getTableHeader().setFont(table.getFont());
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setResizingAllowed(false);
		int[] widths = {36, 44, 63, 52};
		for (int column = 0; column < widths.length; column++)
		{
			table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
		}
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(JTable source, Object value, boolean selected, boolean focus, int row, int column)
			{
				super.getTableCellRendererComponent(source, value, selected, focus, row, column);
				boolean stalled = checks.get(row).population >= checks.get(row).cap;
				setBackground(row % 2 == 0 ? ColorScheme.DARKER_GRAY_COLOR : ColorScheme.DARK_GRAY_COLOR);
				setForeground(column == 3 ? (stalled ? new Color(244, 143, 143) : ACCENT) : Color.WHITE);
				setHorizontalAlignment(column == 2 ? SwingConstants.LEFT : SwingConstants.CENTER);
				return this;
			}
		});
		timeline.add(table.getTableHeader());
		timeline.add(table);
		add(note("Party totals. Report stays until the next room starts. Splits count every child at birth."));
	}

	void update(NylocasWaveSnapshot report)
	{
		beginRoom(report.room);
		String phase;
		switch (report.status)
		{
			case "starting": phase = "Starting room"; break;
			case "cleanup": phase = "Cleanup"; break;
			case "complete": phase = "Waves complete"; break;
			default: phase = "Wave " + report.value(0) + " / 31"; break;
		}
		status.setText(phase);
		remaining.setText(report.value(1) + " Nylos remaining");
		smalls.setText(percent(report.value(6), report.value(5)));
		bigs.setText(percent(report.value(8), report.value(7)));
		for (int style = 0; style < 3; style++)
		{
			overkills[style].setText(percent(report.value(12 + style), report.value(9 + style)));
		}
		stalls.setText(Long.toString(report.value(2)));
		saved.setText(Long.toString(report.value(3)));
		thralls.setText(Long.toString(report.value(4)));
		updateLuck(report);
		for (int phaseIndex = 0; phaseIndex < splits.length; phaseIndex++)
		{
			int offset = 15 + phaseIndex * 3;
			splits[phaseIndex].setText("<html>Melee " + report.value(offset)
				+ "<br>Ranged " + report.value(offset + 1)
				+ "<br>Magic " + report.value(offset + 2) + "</html>");
		}
		revalidate();
		repaint();
	}

	void addCheck(NylocasWaveCheck check)
	{
		beginRoom(check.room);
		if (!checks.isEmpty() && check.sequence <= checks.get(checks.size() - 1).sequence) return;
		checks.add(check);
		checksModel.addRow(new Object[]{check.tick, check.wave, check.attempt == 0 ? "Natural" : "Retry " + check.attempt, check.population + " / " + check.cap});
		checksTitle.setText("Wave-check timeline · " + checks.size());
		revalidate();
		repaint();
	}

	private void beginRoom(String next)
	{
		if (!next.equals(room))
		{
			room = next;
			checks.clear();
			checksModel.setRowCount(0);
			checksTitle.setText("Wave-check timeline");
		}
	}

	private void updateLuck(NylocasWaveSnapshot report)
	{
		luck.setText(luckPercent(report, 0, 1, 2, 3));
		long attempts = 0;
		for (int group = 0; group < 4; group++) attempts += report.value(24 + group * 3);
		String rank = report.hasLuckPercentile() && attempts > 0
			? String.format(Locale.ROOT, "%.1f", report.value(37) / 10000.0) : "—";
		percentile.setText(rank);
		String tooltip = "<html><b>One-shot luck · 100% = expected</b><br>Actual / expected one-shots, using each attack's gear and damage range.<br>"
			+ "First damaging attack per Nylo, including all its splats; misses ignored. Cleanup is post cap.<br>"
			+ "Luck percentile: " + rank + " (0–100; higher is luckier; ties split equally).<br><br>"
			+ "<table cellpadding='4'><tr><th align='left'>Phase</th><th>All</th><th>Smalls</th><th>Bigs</th></tr>"
			+ "<tr><td>Pre cap</td><td>" + luckPercent(report, 0, 1) + "</td><td>" + luckPercent(report, 0) + "</td><td>" + luckPercent(report, 1) + "</td></tr>"
			+ "<tr><td>Post cap</td><td>" + luckPercent(report, 2, 3) + "</td><td>" + luckPercent(report, 2) + "</td><td>" + luckPercent(report, 3) + "</td></tr>"
			+ "<tr><td>Whole room</td><td>" + luckPercent(report, 0, 1, 2, 3) + "</td><td>" + luckPercent(report, 0, 2) + "</td><td>" + luckPercent(report, 1, 3) + "</td></tr></table><br>"
			+ luckCounts(report) + "<br>Thrall-first attacks and untracked damage rolls excluded: " + report.value(36) + "</html>";
		luck.setToolTipText(tooltip);
		luckTitle.setToolTipText(tooltip);
	}

	private static String luckCounts(NylocasWaveSnapshot report)
	{
		long attempts = 0, actual = 0, expected = 0;
		for (int group = 0; group < 4; group++)
		{
			attempts += report.value(24 + group * 3);
			actual += report.value(25 + group * 3);
			expected += report.value(26 + group * 3);
		}
		return String.format(Locale.ROOT, "%d measured Nylos · %d actual / %.2f expected one-shots", attempts, actual, expected / 1000000.0);
	}

	private static String luckPercent(NylocasWaveSnapshot report, int... groups)
	{
		long actual = 0, expected = 0;
		for (int group : groups)
		{
			actual += report.value(25 + group * 3);
			expected += report.value(26 + group * 3);
		}
		return expected == 0 ? "—" : String.format(Locale.ROOT, "%.1f%%", 100000000.0 * actual / expected);
	}

	private JPanel section(String title)
	{
		JPanel section = new JPanel(new DynamicGridLayout(0, 1, 0, 7));
		section.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		section.setBorder(BorderFactory.createEmptyBorder(10, 9, 12, 9));
		if (title != null)
		{
			JLabel heading = label(title);
			heading.setFont(heading.getFont().deriveFont(Font.BOLD));
			section.add(heading);
		}
		add(section);
		return section;
	}

	private static JLabel note(String text)
	{
		JLabel label = label("<html><div style='width:140px'>" + text + "</div></html>");
		label.setFont(label.getFont().deriveFont(11f));
		label.setForeground(MUTED);
		return label;
	}

	private static JLabel row(JPanel parent, JLabel title)
	{
		JLabel value = label("—");
		row(parent, title, value);
		return value;
	}

	private static void row(JPanel parent, JLabel title, JLabel value)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		row.add(title, BorderLayout.WEST);
		row.add(value, BorderLayout.EAST);
		parent.add(row);
	}

	private static JLabel label(String text)
	{
		JLabel label = new JLabel(text)
		{
			@Override
			public JToolTip createToolTip()
			{
				JToolTip tooltip = super.createToolTip();
				tooltip.setFont(TEXT_FONT);
				return tooltip;
			}
		};
		label.setFont(TEXT_FONT);
		return label;
	}

	private static JLabel luckLabel(String text)
	{
		JLabel label = new JLabel(text)
		{
			@Override
			public JToolTip createToolTip()
			{
				return new LuckTooltip(this);
			}
		};
		label.setFont(TEXT_FONT);
		return label;
	}

	private static final class LuckTooltip extends JToolTip
	{
		private int previousDismissDelay;

		private LuckTooltip(JComponent owner)
		{
			setComponent(owner);
			setFont(TEXT_FONT);
		}

		@Override
		public void addNotify()
		{
			super.addNotify();
			// Swing starts its dismissal timer after showing the popup. Keep the
			// breakdown readable until the pointer leaves, then restore other tips.
			ToolTipManager manager = ToolTipManager.sharedInstance();
			previousDismissDelay = manager.getDismissDelay();
			manager.setDismissDelay(Integer.MAX_VALUE);
		}

		@Override
		public void removeNotify()
		{
			ToolTipManager.sharedInstance().setDismissDelay(previousDismissDelay);
			super.removeNotify();
		}
	}

	private static String percent(long count, long total)
	{
		return total == 0 ? "— (0/0)" : String.format(Locale.ROOT, "%.1f%% (%d/%d)", 100.0 * count / total, count, total);
	}
}
