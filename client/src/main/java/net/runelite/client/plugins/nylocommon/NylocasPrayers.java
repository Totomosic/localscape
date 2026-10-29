package net.runelite.client.plugins.nylocommon;

import org.localscape.nylocas.PrayerPolicy;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;

/** Sustained prayers and supplies, independent of gear preparation and attack cooldown. */
public class NylocasPrayers<C>
{
	public interface Settings extends NylocasActions.Equipment
	{
		boolean prayers();
		boolean protectionPrayers();
		boolean restorePrayer();
		int prayerThreshold();
	}

	public interface Threats<C>
	{
		int style(C crab);
		boolean big(C crab);
		boolean ignore(NPC npc, C crab, int tick);
	}

	private static final Prayer[] OFFENSIVE = {Prayer.PIETY, Prayer.RIGOUR, Prayer.AUGURY};
	private static final Prayer[] PROTECTION = {Prayer.PROTECT_FROM_MELEE, Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MAGIC};
	// Finish a partly used bottle before opening another one.
	private static final int[] POTIONS = {ItemID._1DOSEPRAYERRESTORE, ItemID._2DOSEPRAYERRESTORE,
		ItemID._3DOSEPRAYERRESTORE, ItemID._4DOSEPRAYERRESTORE};
	private final Client client;
	private final Settings config;
	private final Threats<C> threats;
	private final Prayer initial;
	private final NylocasActions actions;
	private final Map<Prayer, Integer> requested = new EnumMap<>(Prayer.class);
	private Prayer offensive = Prayer.RIGOUR;
	private Prayer protection;
	private int nextDrink;
	private String warning;

	public NylocasPrayers(Client client, Settings config, NylocasActions actions, Threats<C> threats, int initialStyle)
	{
		this.client = client;
		this.config = config;
		this.actions = actions;
		this.threats = threats;
		this.initial = OFFENSIVE[initialStyle];
		this.offensive = initial;
	}

	public void reset()
	{
		offensive = initial;
		protection = null;
		requested.clear();
		nextDrink = 0;
		warning = null;
	}

	public String warning() { return warning; }

	public void tick(Map<NPC, C> crabs, int tick)
	{
		restore(tick);
		if (config.protectionPrayers())
		{
			protection = chooseProtection(crabs, tick);
			if (protection != null) activate(protection, tick);
		}
		if (config.prayers()) activate(offensive, tick);
	}

	public void forAttack(int style, int tick)
	{
		offensive = OFFENSIVE[style];
		if (config.prayers()) activate(offensive, tick);
	}

	private Prayer chooseProtection(Map<NPC, C> crabs, int tick)
	{
		Prayer current = protection;
		for (Prayer prayer : PROTECTION) if (client.isPrayerActive(prayer)) current = prayer;
		Player player = client.getLocalPlayer();
		if (player == null) return current;
		int[] bigs = new int[3];
		int[] smalls = new int[3];
		for (Map.Entry<NPC, C> entry : crabs.entrySet())
		{
			NPC npc = entry.getKey();
			C crab = entry.getValue();
			if (npc.getId() < 8348 || npc.getId() > 8353 || npc.getInteracting() != player
				|| threats.ignore(npc, crab, tick)) continue;
			WorldArea area = npc.getWorldArea();
			WorldArea local = player.getWorldArea();
			if (area == null || local == null) continue;
			int distance = area.distanceTo(local);
			if (threats.style(crab) == 0 ? !area.isInMeleeDistance(local) : distance < 1 || distance > 2) continue;
			if (!area.hasLineOfSightTo(client.getTopLevelWorldView(), local)) continue;
			(threats.big(crab) ? bigs : smalls)[threats.style(crab)]++;
		}
		int active = -1;
		for (int style = 0; style < 3; style++) if (PROTECTION[style] == current) active = style;
		int best = PrayerPolicy.protection(bigs, smalls, active);
		// Keep the last overhead on between threats instead of flicking it off.
		return best < 0 ? current : PROTECTION[best];
	}

	private void restore(int tick)
	{
		warning = null;
		if (!config.restorePrayer()) return;
		int current = client.getBoostedSkillLevel(Skill.PRAYER);
		int maximum = client.getRealSkillLevel(Skill.PRAYER);
		if (!PrayerPolicy.restore(current, maximum, config.prayerThreshold(), tick, nextDrink)) return;
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		Item[] items = inventory == null ? null : inventory.getItems();
		if (items != null) for (int id : POTIONS) for (int slot = 0; slot < items.length; slot++)
		{
			if (items[slot] == null || items[slot].getId() != id || items[slot].getQuantity() <= 0) continue;
			if (actions.drinkPrayerPotion(slot, id)) nextDrink = tick + 3;
			else warning = "Cannot drink the prayer potion.";
			return;
		}
		warning = "Low prayer: no prayer potion available.";
	}

	private void activate(Prayer prayer, int tick)
	{
		if (client.isPrayerActive(prayer)) { requested.remove(prayer); return; }
		if (client.getBoostedSkillLevel(Skill.PRAYER) <= 0) return;
		Integer previous = requested.get(prayer);
		if (previous != null && tick - previous < 2) return;
		Widget widget = find(client.getWidget(InterfaceID.Prayerbook.CONTAINER), prayer);
		if (widget == null) return;
		// Activating the new prayer replaces the old member of its exclusive group.
		// Never send a separate off toggle or toggle a prayer that is already active.
		client.menuAction(widget.getIndex(), widget.getId(), widget.isIf3() ? MenuAction.CC_OP : MenuAction.WIDGET_TYPE_1,
			1, -1, "Activate", "");
		requested.put(prayer, tick);
	}

	private Widget find(Widget widget, Prayer prayer)
	{
		if (widget == null) return null;
		String label = widget.getName() == null ? "" : widget.getName().toLowerCase(Locale.ROOT)
			.replaceAll("<[^>]*>", "").replaceAll("[^a-z]", "");
		String name;
		switch (prayer)
		{
			case PIETY: name = "piety"; break;
			case RIGOUR: name = "rigour"; break;
			case AUGURY: name = "augury"; break;
			case PROTECT_FROM_MAGIC: name = "protectfrommagic"; break;
			case PROTECT_FROM_MISSILES: name = "protectfrommissiles"; break;
			case PROTECT_FROM_MELEE: name = "protectfrommelee"; break;
			default: return null;
		}
		if (widget.getActions() != null && (label.contains(name) || prayer == Prayer.PIETY && label.contains("zeal")
			|| prayer == Prayer.PROTECT_FROM_MISSILES && (label.contains("protectfromranged") || label.contains("protectfromrange")))) return widget;
		Widget[][] children = {widget.getStaticChildren(), widget.getDynamicChildren()};
		for (Widget[] group : children) if (group != null) for (Widget child : group)
		{
			Widget result = find(child, prayer);
			if (result != null) return result;
		}
		return null;
	}
}
