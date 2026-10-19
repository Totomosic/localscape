package net.runelite.client.plugins.localdev;

import java.util.Arrays;
import java.util.UUID;

/** One complete server snapshot; partial or malformed reports never replace the panel. */
final class NylocasWaveSnapshot
{
	final String room;
	final String status;
	// LNW2/LNW3 counters: wave, population, stalls, saved, thrall kills;
	// small kills/one shots, big kills/one shots; kills and overkills by style;
	// split children by phase then style; luck attempts/actual/expected (millionths)
	// for pre small/big, post small/big; unmeasured first attacks. LNW3 appends the
	// whole-room luck percentile in millionths. Style order is melee, ranged, magic.
	private final long[] values;

	private NylocasWaveSnapshot(String room, String status, long[] values)
	{
		this.room = room;
		this.status = status;
		this.values = values;
	}

	static NylocasWaveSnapshot parse(String message)
	{
		if (message == null || !(message.startsWith("LNW2 ") || message.startsWith("LNW3 ")))
		{
			return null;
		}
		String[] parts = message.split(" ");
		int counters = message.startsWith("LNW3 ") ? 38 : 37;
		if (parts.length != counters + 3 || !Arrays.asList("starting", "waves", "cleanup", "complete").contains(parts[2]))
		{
			return null;
		}
		try
		{
			String room = UUID.fromString(parts[1]).toString();
			long[] values = new long[counters];
			for (int i = 0; i < values.length; i++)
			{
				values[i] = Long.parseLong(parts[i + 3]);
				if (values[i] < 0 || (i < 24 || i == 36 || (i - 24) % 3 != 2) && values[i] > Integer.MAX_VALUE)
				{
					return null;
				}
			}
			if (values[0] > 31 || values[6] > values[5] || values[8] > values[7]
				|| values[4] > values[5] + values[7]
				|| values[9] + values[10] + values[11] != values[5] + values[7])
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
			for (int group = 0; group < 4; group++)
			{
				int offset = 24 + group * 3;
				if (values[offset + 1] > values[offset] || values[offset + 2] / 1000000.0 > values[offset])
				{
					return null;
				}
			}
			if (counters == 38 && values[37] > 1000000)
			{
				return null;
			}
			return new NylocasWaveSnapshot(room, parts[2], values);
		}
		catch (IllegalArgumentException exception)
		{
			return null;
		}
	}

	long value(int index)
	{
		return values[index];
	}

	boolean hasLuckPercentile()
	{
		return values.length == 38;
	}
}
