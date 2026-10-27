package net.runelite.client.plugins.nyloranger;

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
import net.runelite.client.plugins.nyloranger.RangerStrategy.Crab;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Style;

/** Sustained prayers and supplies, independent of gear preparation and attack cooldown. */
final class RangerPrayers
{
	private static final Prayer[] OFFENSIVE = {Prayer.PIETY, Prayer.RIGOUR, Prayer.AUGURY};
	private static final Prayer[] PROTECTION = {Prayer.PROTECT_FROM_MELEE, Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MAGIC};
	// Finish a partly used bottle before opening another one.
	private static final int[] POTIONS = {ItemID._1DOSEPRAYERRESTORE, ItemID._2DOSEPRAYERRESTORE,
		ItemID._3DOSEPRAYERRESTORE, ItemID._4DOSEPRAYERRESTORE};
	private final Client client;
	private final RangerConfig config;
	private final RangerActions actions;
	private final Map<Prayer, Integer> requested = new EnumMap<>(Prayer.class);
	private Prayer offensive = Prayer.RIGOUR;
	private Prayer protection;
	private int nextDrink;
	private String warning;

	RangerPrayers(Client client, RangerConfig config, RangerActions actions)
	{
		this.client = client;
		this.config = config;
		this.actions = actions;
	}

	void reset()
	{
		offensive = Prayer.RIGOUR;
		protection = null;
		requested.clear();
		nextDrink = 0;
		warning = null;
	}

	String warning() { return warning; }

	void tick(Map<NPC, Crab> crabs, int tick)
	{
		restore(tick);
		if (config.protectionPrayers())
		{
			protection = chooseProtection(crabs, tick);
			if (protection != null) activate(protection, tick);
		}
		if (config.prayers()) activate(offensive, tick);
	}

	void forAttack(Style style, int tick)
	{
		offensive = OFFENSIVE[style.ordinal()];
		if (config.prayers()) activate(offensive, tick);
	}

	private Prayer chooseProtection(Map<NPC, Crab> crabs, int tick)
	{
		Prayer current = protection;
		for (Prayer prayer : PROTECTION) if (client.isPrayerActive(prayer)) current = prayer;
		Player player = client.getLocalPlayer();
		if (player == null) return current;
		int[] bigs = new int[3];
		int[] smalls = new int[3];
		for (Map.Entry<NPC, Crab> entry : crabs.entrySet())
		{
			NPC npc = entry.getKey();
			Crab crab = entry.getValue();
			if (npc.getId() < 8348 || npc.getId() > 8353 || npc.getInteracting() != player
				|| crab.dying || NylocasRangerPlugin.dying(npc) || RangerFlickers.passiveMelee(crab, tick)) continue;
			WorldArea area = npc.getWorldArea();
			WorldArea local = player.getWorldArea();
			if (area == null || local == null) continue;
			int distance = area.distanceTo(local);
			if (crab.style == Style.MELEE ? !area.isInMeleeDistance(local) : distance < 1 || distance > 2) continue;
			if (!area.hasLineOfSightTo(client.getTopLevelWorldView(), local)) continue;
			(crab.big ? bigs : smalls)[crab.style.ordinal()]++;
		}
		int best = -1;
		for (int style = 0; style < 3; style++)
		{
			if (bigs[style] == 0 && smalls[style] == 0) continue;
			if (best < 0 || bigs[style] > bigs[best]
				|| bigs[style] == bigs[best] && smalls[style] > smalls[best]
				|| bigs[style] == bigs[best] && smalls[style] == smalls[best] && PROTECTION[style] == current)
				best = style;
		}
		// Keep the last overhead on between threats instead of flicking it off.
		return best < 0 ? current : PROTECTION[best];
	}

	private void restore(int tick)
	{
		warning = null;
		if (!config.restorePrayer()) return;
		int current = client.getBoostedSkillLevel(Skill.PRAYER);
		int maximum = client.getRealSkillLevel(Skill.PRAYER);
		int restored = Math.min(maximum, 7 + maximum / 4);
		if (maximum <= 0 || current > config.prayerThreshold() || maximum - current < restored || tick < nextDrink) return;
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
