package net.runelite.client.plugins.localdev;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;

/** Local server messages are applied after NPC updates, against the same NPC incarnation. */
final class NylocasBotDamage
{
	private static final String PREFIX = "localscape:nylohit:";
	private final List<Damage> pending = new ArrayList<>();
	private final Map<Long, NPC> births = new HashMap<>();
	private final Map<Integer, Long> slots = new HashMap<>();
	private long session;
	private long sequence;

	void receive(String message)
	{
		if (message == null || !message.startsWith(PREFIX))
		{
			return;
		}
		String[] parts = message.substring(PREFIX.length()).split(":", -1);
		if (parts.length != 8)
		{
			return;
		}
		try
		{
			long incomingSession = Long.parseLong(parts[0]);
			long incomingSequence = Long.parseLong(parts[1]);
			Damage damage = new Damage(Long.parseLong(parts[2]), Integer.parseInt(parts[3]),
				Integer.parseInt(parts[4]), Integer.parseInt(parts[5]), Integer.parseInt(parts[6]), Integer.parseInt(parts[7]));
			if (incomingSession <= 0 || incomingSequence <= 0 || damage.birth <= 0 || damage.index < 0
				|| damage.index >= 65535 || damage.id < 8342 || damage.id > 8353 || damage.amount <= 0
				|| damage.amount > 1000 || damage.x < 0 || damage.x > 16383 || damage.y < 0 || damage.y > 16383)
			{
				return;
			}
			if (incomingSession < session || (incomingSession == session && incomingSequence <= sequence))
			{
				return;
			}
			if (incomingSession != session)
			{
				reset();
				session = incomingSession;
			}
			sequence = incomingSequence;
			Long previous = slots.put(damage.index, damage.birth);
			if (previous != null && previous != damage.birth)
			{
				births.put(previous, null);
			}
			pending.add(damage);
		}
		catch (NumberFormatException ignored)
		{
			// Ignore incompatible or malformed local control messages.
		}
	}

	void apply(Client client, BiConsumer<Integer, Integer> submit)
	{
		List<Damage> batch = new ArrayList<>(pending);
		pending.clear();
		WorldView world = client.getTopLevelWorldView();
		if (world != null && world.getMapRegions() != null
			&& Arrays.stream(world.getMapRegions()).anyMatch(region -> region == 13122))
		{
			for (Damage damage : batch)
			{
				if (!Long.valueOf(damage.birth).equals(slots.get(damage.index)))
				{
					continue;
				}
				NPC npc = world.npcs().byIndex(damage.index);
				if (npc == null || npc.getId() != damage.id || npc.getWorldLocation().getX() != damage.x
					|| npc.getWorldLocation().getY() != damage.y)
				{
					continue;
				}
				if (births.containsKey(damage.birth) && births.get(damage.birth) != npc)
				{
					continue;
				}
				births.put(damage.birth, npc);
				submit.accept(damage.index, damage.amount);
			}
		}
	}

	void despawn(NPC npc)
	{
		births.replaceAll((birth, bound) -> bound == npc ? null : bound);
		pending.removeIf(damage -> damage.index == npc.getIndex());
	}

	void reset()
	{
		pending.clear();
		births.clear();
		slots.clear();
		session = 0;
		sequence = 0;
	}

	private static final class Damage
	{
		private final long birth;
		private final int index;
		private final int id;
		private final int x;
		private final int y;
		private final int amount;

		private Damage(long birth, int index, int id, int x, int y, int amount)
		{
			this.birth = birth;
			this.index = index;
			this.id = id;
			this.x = x;
			this.y = y;
			this.amount = amount;
		}
	}
}
