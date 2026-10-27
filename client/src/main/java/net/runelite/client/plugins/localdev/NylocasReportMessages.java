package net.runelite.client.plugins.localdev;

import java.util.UUID;

/** Bounded, atomic reassembly of snapshots carried over byte-length console packets. */
final class NylocasReportMessages
{
	private static final int MAX_TEXT = 252;
	private static final int CHUNK_SIZE = 180;
	private static final int MAX_PARTS = 32;
	private static final int TIMEOUT_TICKS = 10;
	private String room;
	private long sequence;
	private String[] chunks;
	private int received;
	private int started;

	synchronized void reset()
	{
		room = null;
		sequence = 0;
		chunks = null;
		received = 0;
	}

	synchronized void expire(int tick)
	{
		if (chunks != null && tick - started >= TIMEOUT_TICKS) chunks = null;
	}

	synchronized String accept(String message, int tick)
	{
		expire(tick);
		if (message == null) return null;
		if (!message.startsWith("LNF1 "))
		{
			// Legacy and short snapshots remain supported. Never let pending fragments
			// overwrite a later complete snapshot from this ordered console stream.
			if (message.startsWith("LNW2 ") || message.startsWith("LNW3 ") || message.startsWith("LNW4 ")) chunks = null;
			return message;
		}
		if (message.length() > MAX_TEXT) return null;
		String[] fields = message.split(" ", 6);
		if (fields.length != 6 || fields[5].isEmpty() || fields[5].length() > CHUNK_SIZE) return null;
		try
		{
			String nextRoom = UUID.fromString(fields[1]).toString();
			long nextSequence = Long.parseLong(fields[2]);
			int index = Integer.parseInt(fields[3]);
			int count = Integer.parseInt(fields[4]);
			if (nextSequence <= 0 || count < 1 || count > MAX_PARTS || index < 0 || index >= count) return null;
			if (nextRoom.equals(room) && nextSequence < sequence) return null;
			if (!nextRoom.equals(room) || nextSequence > sequence)
			{
				room = nextRoom;
				sequence = nextSequence;
				chunks = new String[count];
				received = 0;
				started = tick;
			}
			if (chunks == null) return null; // Already completed, expired or rejected.
			if (chunks.length != count || chunks[index] != null && !chunks[index].equals(fields[5]))
			{
				chunks = null;
				return null;
			}
			if (chunks[index] == null) { chunks[index] = fields[5]; received++; }
			if (received != chunks.length) return null;
			String complete = String.join("", chunks);
			chunks = null;
			return complete.startsWith("LNW4 " + room + " ") ? complete : null;
		}
		catch (IllegalArgumentException exception)
		{
			return null;
		}
	}
}
