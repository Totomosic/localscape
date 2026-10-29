package net.runelite.client.plugins.nyloranger;

import org.localscape.nylocas.ranger.RangerStrategy;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.plugins.nylocommon.NylocasActions;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

/** Ranger equipment adapter for the shared native client controls. */
final class RangerActions extends NylocasActions
{
	RangerActions(Client client, RangerConfig config, BooleanSupplier active)
	{
		super(client, config, active, java.util.Arrays.stream(Weapon.values()).mapToInt(w -> w.itemId).toArray(), 20997, 1, 1);
	}

	List<Integer> gear(Style style) { return gear(style.ordinal()); }
	String capture(Style style) { return capture(style.ordinal()); }
	boolean prepare(Weapon weapon) { return prepare(weapon.itemId, weapon.style.ordinal(), weapon.stance); }
	boolean attack(NPC npc, Weapon weapon, boolean special)
	{
		return attack(npc, weapon.itemId, weapon.style.ordinal(), weapon.stance, special);
	}
}
