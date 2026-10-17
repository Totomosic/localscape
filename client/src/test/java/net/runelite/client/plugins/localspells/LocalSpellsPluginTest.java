package net.runelite.client.plugins.localspells;

import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.ItemID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.when;

public class LocalSpellsPluginTest
{
	@Mock
	private Client client;
	@Mock
	private ItemContainer inventory;
	@Mock
	private EnumComposition runes;
	@InjectMocks
	private LocalSpellsPlugin plugin;

	@Before
	public void setUp()
	{
		MockitoAnnotations.initMocks(this);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inventory);
		when(client.getEnum(EnumID.RUNEPOUCH_RUNE)).thenReturn(runes);
		when(runes.getIntVals()).thenReturn(new int[]{556, 554, 565});
	}

	@Test
	public void everyPouchLightsSpellsAndClearsTheLockSpriteWhilePresent()
	{
		for (int pouch : new int[]{ItemID.BH_RUNE_POUCH, ItemID.BH_RUNE_POUCH_TROUVER,
			ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_TROUVER, ItemID.BR_RUNE_REPLACEMENT})
		{
			int[] stack = {123, 0, 3012};
			when(client.getIntStack()).thenReturn(stack);
			when(client.getIntStackSize()).thenReturn(3);
			when(inventory.contains(pouch)).thenReturn(true);
			plugin.onScriptPostFired(new ScriptPostFired(2614));
			assertArrayEquals(new int[]{123, 1, -1}, stack);
			when(inventory.contains(pouch)).thenReturn(false);
			stack[1] = 0;
			stack[2] = -1;
			plugin.onScriptPostFired(new ScriptPostFired(2614));
			assertArrayEquals(new int[]{123, 0, -1}, stack);
		}
	}

	@Test
	public void suppliesUnlimitedRuneCountsWithoutReplacingOtherIngredients()
	{
		when(inventory.contains(ItemID.BH_RUNE_POUCH)).thenReturn(true);
		for (int item : new int[]{556, 554, 565, 1963})
		{
			int[] args = {123, item, -1, -1};
			when(client.getIntStack()).thenReturn(args);
			when(client.getIntStackSize()).thenReturn(4);
			plugin.onScriptPreFired(new ScriptPreFired(19));
			int[] result = {123, 0};
			when(client.getIntStack()).thenReturn(result);
			when(client.getIntStackSize()).thenReturn(2);
			plugin.onScriptPostFired(new ScriptPostFired(19));
			assertArrayEquals(new int[]{123, item == 1963 ? 0 : Integer.MAX_VALUE}, result);
		}
	}

	@Test
	public void ignoresOtherScriptsMissingInventoryAndLoggedOutState()
	{
		int[] stack = {0, 3012};
		when(client.getIntStack()).thenReturn(stack);
		when(client.getIntStackSize()).thenReturn(2);
		when(inventory.contains(ItemID.BH_RUNE_POUCH)).thenReturn(true);
		plugin.onScriptPostFired(new ScriptPostFired(100));
		assertArrayEquals(new int[]{0, 3012}, stack);
		when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
		plugin.onScriptPostFired(new ScriptPostFired(2614));
		assertArrayEquals(new int[]{0, 3012}, stack);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(null);
		plugin.onScriptPostFired(new ScriptPostFired(2614));
		assertArrayEquals(new int[]{0, 3012}, stack);
	}
}
