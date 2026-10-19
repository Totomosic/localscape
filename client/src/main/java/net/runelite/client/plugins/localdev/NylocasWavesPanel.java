package net.runelite.client.plugins.localdev;

import java.awt.BorderLayout;
import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.PluginPanel;

final class NylocasWavesPanel extends PluginPanel
{
	private final JLabel status = new JLabel("<html>Start the Nylocas room to record a report.</html>");
	private final JLabel smalls;
	private final JLabel bigs;
	private final JLabel[] overkills;
	private final JLabel saved;
	private final JLabel stalls;
	private final JLabel thralls;
	private final JLabel[] splits = new JLabel[3];

	NylocasWavesPanel()
	{
		add(status);
		add(new JLabel("<html><b>One shot kills</b></html>"));
		smalls = row("Smalls");
		bigs = row("Bigs");
		smalls.setToolTipText("Combat kills with one damaging hitsplat / all combat kills. Misses are ignored.");
		bigs.setToolTipText(smalls.getToolTipText());
		add(new JLabel("<html><b>Overkilled by Nylo style</b></html>"));
		overkills = new JLabel[]{row("Melee"), row("Ranged"), row("Magic")};
		for (JLabel value : overkills)
		{
			value.setToolTipText("An extra hitsplat on the lethal tick / combat kills of this Nylo style at death.");
		}
		saved = row("Stalls saved");
		stalls = row("Cap stalls");
		thralls = row("Thrall kills");
		saved.setToolTipText("Successful cap checks that would have stalled with the overkilled bodies removed one tick later, accounting for big splits.");
		thralls.setToolTipText("Nylos whose lethal hitsplat came from a thrall, across the party.");
		add(new JLabel("<html><b>Split children by style</b></html>"));
		String[] phases = {"Before cap increase (waves 1–20)", "After cap increase (waves 21–30)", "Cleanup (after wave 31 spawns)"};
		for (int phase = 0; phase < splits.length; phase++)
		{
			add(new JLabel("<html>" + phases[phase] + "</html>"));
			splits[phase] = new JLabel("<html>Melee — · Ranged — · Magic —</html>");
			add(splits[phase]);
		}
		add(new JLabel("<html>Party totals. Kill percentages exclude natural expiries. Splits include all children at birth. Report stays until the next room starts.</html>"));
	}

	void update(NylocasWaveSnapshot report)
	{
		String phase;
		switch (report.status)
		{
			case "starting": phase = "Starting room"; break;
			case "cleanup": phase = "Cleanup"; break;
			case "complete": phase = "Waves complete"; break;
			default: phase = "Wave " + report.value(0) + " / 31"; break;
		}
		status.setText("<html><b>" + phase + "</b><br>" + report.value(1) + " Nylos remaining</html>");
		smalls.setText(percent(report.value(6), report.value(5)));
		bigs.setText(percent(report.value(8), report.value(7)));
		for (int style = 0; style < 3; style++)
		{
			overkills[style].setText(percent(report.value(12 + style), report.value(9 + style)));
		}
		stalls.setText(Integer.toString(report.value(2)));
		saved.setText(Integer.toString(report.value(3)));
		thralls.setText(Integer.toString(report.value(4)));
		long allSplits = 0;
		for (int index = 15; index < 24; index++)
		{
			allSplits += report.value(index);
		}
		for (int phaseIndex = 0; phaseIndex < splits.length; phaseIndex++)
		{
			int offset = 15 + phaseIndex * 3;
			long total = (long) report.value(offset) + report.value(offset + 1) + report.value(offset + 2);
			splits[phaseIndex].setText("<html><b>" + percent(total, allSplits) + " of all splits</b><br>Melee " + percent(report.value(offset), total)
				+ "<br>Ranged " + percent(report.value(offset + 1), total)
				+ "<br>Magic " + percent(report.value(offset + 2), total) + "</html>");
		}
	}

	private JLabel row(String title)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		row.add(new JLabel(title), BorderLayout.WEST);
		JLabel value = new JLabel("—");
		row.add(value, BorderLayout.EAST);
		add(row);
		return value;
	}

	private static String percent(long count, long total)
	{
		return total == 0 ? "— (0/0)" : String.format(Locale.ROOT, "%.1f%% (%d/%d)", 100.0 * count / total, count, total);
	}
}
