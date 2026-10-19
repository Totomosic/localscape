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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;

final class NylocasWavesPanel extends PluginPanel
{
	private static final Color ACCENT = new Color(112, 202, 186);
	private static final Color MUTED = new Color(170, 175, 184);
	private final JLabel status = new JLabel("Start a Nylocas room to record.");
	private final JLabel remaining = new JLabel("Party report · updates live");
	private final JLabel luck;
	private final JLabel luckTitle = new JLabel("% luck");
	private final JLabel smalls;
	private final JLabel bigs;
	private final JLabel[] overkills;
	private final JLabel saved;
	private final JLabel stalls;
	private final JLabel thralls;
	private final JLabel[] splits = new JLabel[3];
	private final JLabel checksTitle = new JLabel("Wave-check timeline");
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
		luck = row(performance, luckTitle);
		luck.setFont(luck.getFont().deriveFont(Font.BOLD, 20f));
		luck.setForeground(ACCENT);
		smalls = row(performance, new JLabel("Smalls"));
		bigs = row(performance, new JLabel("Bigs"));
		smalls.setToolTipText("One damaging hitsplat / all combat kills. Misses ignored; natural expiries excluded.");
		bigs.setToolTipText(smalls.getToolTipText());
		performance.add(note("100% = expected one-shots. Hover for breakdown."));
		JPanel efficiency = section("Room efficiency");
		stalls = row(efficiency, new JLabel("Cap stalls"));
		saved = row(efficiency, new JLabel("Stalls saved"));
		thralls = row(efficiency, new JLabel("Thrall kills"));
		saved.setToolTipText("Successful cap checks that would have stalled with overkilled bodies removed one tick later, accounting for big splits.");
		thralls.setToolTipText("Nylos whose lethal hitsplat came from a thrall, across the party.");
		JPanel overkill = section("Overkills by Nylo style");
		overkills = new JLabel[]{row(overkill, new JLabel("Melee")), row(overkill, new JLabel("Ranged")), row(overkill, new JLabel("Magic"))};
		for (JLabel value : overkills)
		{
			value.setToolTipText("An extra hitsplat on the lethal tick / combat kills of this Nylo style at death.");
		}
		JPanel splitSection = section("Split children");
		String[] phases = {"Pre cap · waves 1–20", "Post cap · waves 21–30", "Cleanup · wave 31 onward"};
		for (int phase = 0; phase < splits.length; phase++)
		{
			JLabel title = new JLabel(phases[phase]);
			title.setBorder(BorderFactory.createEmptyBorder(8, 0, 3, 0));
			title.setForeground(MUTED);
			splitSection.add(title);
			splits[phase] = new JLabel("<html>Melee — · Ranged — · Magic —</html>");
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
		table.setFont(table.getFont().deriveFont(11f));
		table.getTableHeader().setFont(table.getFont());
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setResizingAllowed(false);
		int[] widths = {34, 33, 69, 58};
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
		long allSplits = 0;
		for (int index = 15; index < 24; index++) allSplits += report.value(index);
		for (int phaseIndex = 0; phaseIndex < splits.length; phaseIndex++)
		{
			int offset = 15 + phaseIndex * 3;
			long total = report.value(offset) + report.value(offset + 1) + report.value(offset + 2);
			splits[phaseIndex].setText("<html><b>" + percent(total, allSplits) + " of all splits</b><br>Melee " + percent(report.value(offset), total)
				+ "<br>Ranged " + percent(report.value(offset + 1), total)
				+ "<br>Magic " + percent(report.value(offset + 2), total) + "</html>");
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
		String tooltip = "<html><b>One-shot luck · 100% = expected</b><br>Actual / expected one-shots, using each attack's gear and damage range.<br>"
			+ "First damaging splat per Nylo; misses ignored. Cleanup is post cap.<br><br>"
			+ "<table cellpadding='4'><tr><th align='left'>Phase</th><th>All</th><th>Smalls</th><th>Bigs</th></tr>"
			+ "<tr><td>Pre cap</td><td>" + luckPercent(report, 0, 1) + "</td><td>" + luckPercent(report, 0) + "</td><td>" + luckPercent(report, 1) + "</td></tr>"
			+ "<tr><td>Post cap</td><td>" + luckPercent(report, 2, 3) + "</td><td>" + luckPercent(report, 2) + "</td><td>" + luckPercent(report, 3) + "</td></tr>"
			+ "<tr><td>Whole room</td><td>" + luckPercent(report, 0, 1, 2, 3) + "</td><td>" + luckPercent(report, 0, 2) + "</td><td>" + luckPercent(report, 1, 3) + "</td></tr></table><br>"
			+ luckCounts(report) + "<br>Thrall-first hits and untracked damage rolls excluded: " + report.value(36) + "</html>";
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
			JLabel heading = new JLabel(title);
			heading.setFont(heading.getFont().deriveFont(Font.BOLD));
			section.add(heading);
		}
		add(section);
		return section;
	}

	private static JLabel note(String text)
	{
		JLabel label = new JLabel("<html><div style='width:174px'>" + text + "</div></html>");
		label.setFont(label.getFont().deriveFont(11f));
		label.setForeground(MUTED);
		return label;
	}

	private static JLabel row(JPanel parent, JLabel title)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		row.add(title, BorderLayout.WEST);
		JLabel value = new JLabel("—");
		row.add(value, BorderLayout.EAST);
		parent.add(row);
		return value;
	}

	private static String percent(long count, long total)
	{
		return total == 0 ? "— (0/0)" : String.format(Locale.ROOT, "%.1f%% (%d/%d)", 100.0 * count / total, count, total);
	}
}
