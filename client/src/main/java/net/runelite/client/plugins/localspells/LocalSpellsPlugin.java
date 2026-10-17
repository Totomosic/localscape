package net.runelite.client.plugins.localspells;

import java.util.Arrays;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.EnumID;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Local Server Spells",
	description = "Treats rune pouches as unlimited runes on the local server",
	enabledByDefault = true,
	hidden = true
)
public class LocalSpellsPlugin extends Plugin
{
	// Revision 233: rune count (also used by tooltips/filters), and spell icon availability.
	private static final int RUNE_COUNT = 19;
	private static final int SPELL_AVAILABLE = 2614;

	@Inject
	private Client client;

	private boolean countingRune;

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() != RUNE_COUNT)
		{
			return;
		}
		countingRune = false;
		if (client.getGameState() != GameState.LOGGED_IN || !hasRunePouch())
		{
			return;
		}
		int rune;
		if (event.getScriptEvent() != null)
		{
			Object[] args = event.getScriptEvent().getArguments();
			if (args == null || args.length < 4 || !(args[1] instanceof Integer))
			{
				return;
			}
			rune = (Integer) args[1];
		}
		else
		{
			int size = client.getIntStackSize();
			if (size < 3)
			{
				return;
			}
			rune = client.getIntStack()[size - 3];
		}
		// The count proc also accepts non-rune spell ingredients, such as bananas.
		countingRune = Arrays.stream(client.getEnum(EnumID.RUNEPOUCH_RUNE).getIntVals())
			.anyMatch(id -> id == rune);
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		int script = event.getScriptId();
		if ((script != RUNE_COUNT && script != SPELL_AVAILABLE)
			|| client.getGameState() != GameState.LOGGED_IN || !hasRunePouch())
		{
			return;
		}
		int size = client.getIntStackSize();
		int[] stack = client.getIntStack();
		if (script == RUNE_COUNT && countingRune && size >= 1)
		{
			// The native client uses MAX_VALUE to display an unlimited rune source.
			stack[size - 1] = Integer.MAX_VALUE;
		}
		else if (script == SPELL_AVAILABLE && size >= 2)
		{
			// All icons light up, including spells normally marked with quest-lock sprites.
			stack[size - 2] = 1;
			stack[size - 1] = -1;
		}
	}

	private boolean hasRunePouch()
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		return inv != null && (inv.contains(ItemID.BH_RUNE_POUCH)
			|| inv.contains(ItemID.BH_RUNE_POUCH_TROUVER)
			|| inv.contains(ItemID.DIVINE_RUNE_POUCH)
			|| inv.contains(ItemID.DIVINE_RUNE_POUCH_TROUVER)
			|| inv.contains(ItemID.BR_RUNE_REPLACEMENT));
	}
}
