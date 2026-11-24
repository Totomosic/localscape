package net.runelite.client.plugins.localdev;

import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Local Archive Weapons",
	description = "Displays Obligator attack styles in the revision 233 combat tab",
	enabledByDefault = true,
	hidden = true
)
public class ArchiveWeaponStyles extends Plugin
{
	private static final int COMBAT_TAB = 593;

	@Inject
	private Client client;

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		int script = event.getScriptId();
		if (script != 324 && script != 420 && script != 7593)
		{
			return;
		}
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		Item weapon = equipment == null ? null : equipment.getItem(3);
		if (weapon == null || !client.getItemDefinition(weapon.getId()).getName().startsWith("Obligator"))
		{
			return;
		}
		setText(3, "Category: Extending Chain Mace");
		setText(8, "Thrust");
		setText(12, "Thrust");
		setText(20, "Smash");
		setTooltip(5, "Accurate", "5 ticks (3.0s)", true);
		setTooltip(9, "Aggressive", "5 ticks (3.0s)", true);
		setTooltip(17, "Aggressive", "3 ticks (1.8s)", false);
	}

	private void setText(int child, String text)
	{
		Widget widget = client.getWidget(COMBAT_TAB, child);
		if (widget != null)
		{
			widget.setText(text);
		}
	}

	private void setTooltip(int child, String style, String speed, boolean thrust)
	{
		Widget widget = client.getWidget(COMBAT_TAB, child);
		if (widget != null)
		{
			// Revision 233's combat setup uses script 526 and tooltip container 44.
			// Keep its native repeat timing and mouse-leave cleanup.
			widget.setOnMouseRepeatListener(526, -2147483645, -1, (COMBAT_TAB << 16) | 44,
				style + "<br>Crush<br>" + speed
					+ (thrust ? "<br>Pierces up to three targets" : "<br>Single target"), 25, 140);
		}
	}
}
