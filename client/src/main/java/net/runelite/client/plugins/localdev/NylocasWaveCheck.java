package net.runelite.client.plugins.localdev;

import java.util.UUID;

/** Population is cap occupancy at admission, including bodies awaiting removal. */
final class NylocasWaveCheck
{
	final String room;
	final int sequence;
	final int tick;
	final int wave;
	final int attempt;
	final int population;
	final int cap;

	private NylocasWaveCheck(String room, int sequence, int tick, int wave, int attempt, int population, int cap)
	{
		this.room = room;
		this.sequence = sequence;
		this.tick = tick;
		this.wave = wave;
		this.attempt = attempt;
		this.population = population;
		this.cap = cap;
	}

	static NylocasWaveCheck parse(String message)
	{
		if (message == null || !message.startsWith("LNC1 "))
		{
			return null;
		}
		String[] parts = message.split(" ");
		if (parts.length != 8)
		{
			return null;
		}
		try
		{
			String room = UUID.fromString(parts[1]).toString();
			int[] values = new int[6];
			for (int i = 0; i < values.length; i++)
			{
				values[i] = Integer.parseInt(parts[i + 2]);
				if (values[i] < 0)
				{
					return null;
				}
			}
			if (values[0] == 0 || values[2] < 1 || values[2] > 31 || values[5] != (values[2] <= 20 ? 12 : 24))
			{
				return null;
			}
			return new NylocasWaveCheck(room, values[0], values[1], values[2], values[3], values[4], values[5]);
		}
		catch (IllegalArgumentException exception)
		{
			return null;
		}
	}
}
