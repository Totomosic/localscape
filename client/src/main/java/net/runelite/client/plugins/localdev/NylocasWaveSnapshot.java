package net.runelite.client.plugins.localdev;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** One complete server snapshot; partial or malformed reports never replace the panel. */
final class NylocasWaveSnapshot
{
	final String room;
	final String status;
	final List<PlayerLuck> players;
	// LNW2/LNW3 counters: wave, population, stalls, saved, thrall kills;
	// small kills/one shots, big kills/one shots; kills and overkills by style;
	// split children by phase then style; luck attempts/actual/expected (millionths)
	// for pre small/big, post small/big; unmeasured first attacks. LNW3 appends the
	// whole-room luck percentile in millionths. Style order is melee, ranged, magic.
	// LNW4 keeps those counters, then adds the member count and, per member,
	// a base64url UTF-8 name followed by the 14 luck counters (indices 24–37 above).
	private final long[] values;

	private NylocasWaveSnapshot(String room, String status, long[] values, List<PlayerLuck> players)
	{
		this.room = room;
		this.status = status;
		this.values = values;
		this.players = Collections.unmodifiableList(players);
	}

	static NylocasWaveSnapshot parse(String message)
	{
		if (message == null || !(message.startsWith("LNW2 ") || message.startsWith("LNW3 ") || message.startsWith("LNW4 ")))
		{
			return null;
		}
		String[] parts = message.split(" ");
		boolean perPlayer = message.startsWith("LNW4 ");
		int counters = message.startsWith("LNW2 ") ? 37 : 38;
		if (parts.length < counters + 3 || (!perPlayer && parts.length != counters + 3)
			|| !Arrays.asList("starting", "waves", "cleanup", "complete").contains(parts[2]))
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
			List<PlayerLuck> players = new ArrayList<>();
			if (perPlayer)
			{
				if (parts.length < 42) return null;
				int count = Integer.parseInt(parts[41]);
				if (count < 0 || count > 5 || parts.length != 42 + count * 15) return null;
				Set<String> names = new HashSet<>();
				for (int player = 0; player < count; player++)
				{
					int offset = 42 + player * 15;
					String name = new String(Base64.getUrlDecoder().decode(parts[offset]), StandardCharsets.UTF_8);
					if (name.trim().isEmpty() || name.length() > 12 || !names.add(name)) return null;
					long[] luck = new long[14];
					for (int i = 0; i < luck.length; i++) luck[i] = Long.parseLong(parts[offset + 1 + i]);
					if (!Luck.valid(luck)) return null;
					players.add(new PlayerLuck(name, new Luck(luck)));
				}
			}
			return new NylocasWaveSnapshot(room, parts[2], values, players);
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

	Luck luck()
	{
		return new Luck(Arrays.copyOfRange(values, 24, values.length));
	}

	static final class PlayerLuck
	{
		final String name;
		final Luck luck;

		private PlayerLuck(String name, Luck luck)
		{
			this.name = name;
			this.luck = luck;
		}
	}

	static final class Luck
	{
		private final long[] values;

		private Luck(long[] values)
		{
			this.values = values;
		}

		long value(int index)
		{
			return values[index];
		}

		boolean hasPercentile()
		{
			return values.length == 14;
		}

		private static boolean valid(long[] values)
		{
			for (int i = 0; i < values.length; i++)
			{
				if (values[i] < 0 || (i >= 12 || i % 3 != 2) && values[i] > Integer.MAX_VALUE) return false;
			}
			for (int group = 0; group < 4; group++)
			{
				int offset = group * 3;
				if (values[offset + 1] > values[offset] || values[offset + 2] / 1000000.0 > values[offset]) return false;
			}
			return values[13] <= 1000000;
		}
	}
}
