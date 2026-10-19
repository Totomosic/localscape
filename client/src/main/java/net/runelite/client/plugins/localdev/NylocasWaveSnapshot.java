package net.runelite.client.plugins.localdev;

import java.util.Arrays;
import java.util.UUID;

/** One complete server snapshot; partial or malformed reports never replace the panel. */
final class NylocasWaveSnapshot
{
	final String room;
	final String status;
	// LNW1 counters: wave, population, stalls, saved, thrall kills;
	// small kills/one shots, big kills/one shots; kills and overkills by style;
	// split children by phase then style. Style order is melee, ranged, magic.
	private final int[] values;

	private NylocasWaveSnapshot(String room, String status, int[] values)
	{
		this.room = room;
		this.status = status;
		this.values = values;
	}

	static NylocasWaveSnapshot parse(String message)
	{
		if (message == null || !message.startsWith("LNW1 "))
		{
			return null;
		}
		String[] parts = message.split(" ");
		if (parts.length != 27 || !Arrays.asList("starting", "waves", "cleanup", "complete").contains(parts[2]))
		{
			return null;
		}
		try
		{
			String room = UUID.fromString(parts[1]).toString();
			int[] values = new int[24];
			for (int i = 0; i < values.length; i++)
			{
				values[i] = Integer.parseInt(parts[i + 3]);
				if (values[i] < 0)
				{
					return null;
				}
			}
			if (values[0] > 31 || values[6] > values[5] || values[8] > values[7]
				|| (long) values[4] > (long) values[5] + values[7]
				|| (long) values[9] + values[10] + values[11] != (long) values[5] + values[7])
			{
				return null;
			}
			for (int style = 0; style < 3; style++)
			{
				if (values[12 + style] > values[9 + style])
				{
					return null;
				}
			}
			return new NylocasWaveSnapshot(room, parts[2], values);
		}
		catch (IllegalArgumentException exception)
		{
			return null;
		}
	}

	int value(int index)
	{
		return values[index];
	}
}
