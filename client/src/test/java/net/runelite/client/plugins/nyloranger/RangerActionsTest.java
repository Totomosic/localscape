package net.runelite.client.plugins.nyloranger;

import org.localscape.nylocas.ranger.RangerStrategy;
import java.util.Arrays;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ScriptID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RangerActionsTest
{
	private Client client;
	private RangerConfig config;
	private ItemContainer inventory;
	private ItemContainer equipment;
	private RangerActions actions;

	@Before
	public void setup()
	{
		client = mock(Client.class);
		config = mock(RangerConfig.class);
		inventory = mock(ItemContainer.class);
		equipment = mock(ItemContainer.class);
		when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inventory);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(equipment);
		when(inventory.getItems()).thenReturn(new Item[]{new Item(12926, 1), new Item(100, 1)});
		when(equipment.getItem(3)).thenReturn(new Item(20997, 1));
		when(client.getVarpValue(43)).thenReturn(1);
		when(config.rangedGear()).thenReturn("100");
		definition(12926, "Wield");
		definition(100, "Wear");
		actions = new RangerActions(client, config, () -> true);
	}

	private void definition(int id, String action)
	{
		ItemComposition item = mock(ItemComposition.class);
		when(client.getItemDefinition(id)).thenReturn(item);
		when(item.getInventoryActions()).thenReturn(new String[]{null, action, null, null, "Drop"});
	}

	@Test
	public void switchesEquipmentBeforeSendingRealNpcAttackAction()
	{
		NPC npc = mock(NPC.class);
		NPCComposition composition = mock(NPCComposition.class);
		when(npc.getTransformedComposition()).thenReturn(composition);
		when(composition.getActions()).thenReturn(new String[]{null, "Attack", null, null, null});
		when(npc.getIndex()).thenReturn(42);
		when(npc.getName()).thenReturn("Nylocas Toxobolos");
		when(npc.getHealthRatio()).thenReturn(8);
		assertTrue(actions.attack(npc, Weapon.PIPE, false));
		InOrder order = inOrder(client);
		order.verify(client).menuAction(1, InterfaceID.Inventory.ITEMS, MenuAction.ITEM_SECOND_OPTION, 100, 100, "Wear", "");
		order.verify(client).menuAction(0, InterfaceID.Inventory.ITEMS, MenuAction.ITEM_SECOND_OPTION, 12926, 12926, "Wield", "");
		order.verify(client).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylocas Toxobolos");
	}

	@Test
	public void supportsIf3InventoryChildEquipActions()
	{
		when(config.rangedGear()).thenReturn("");
		Widget root = mock(Widget.class);
		Widget child = mock(Widget.class);
		when(client.getWidget(InterfaceID.Inventory.ITEMS)).thenReturn(root);
		when(root.isIf3()).thenReturn(true);
		when(root.getId()).thenReturn(InterfaceID.Inventory.ITEMS);
		when(root.getChild(0)).thenReturn(child);
		when(child.getActions()).thenReturn(new String[]{"Wield"});
		assertTrue(actions.prepare(Weapon.PIPE));
		verify(client).menuAction(0, InterfaceID.Inventory.ITEMS, MenuAction.CC_OP, 1, 12926, "Wield", "");
	}

	@Test
	public void missingWeaponStopsWithoutAttacking()
	{
		assertFalse(actions.prepare(Weapon.EYE));
		assertTrue(actions.error().contains("31113"));
		verify(client, never()).menuAction(anyInt(), anyInt(), any(), eq(42), anyInt(), eq("Attack"), anyString());
	}

	@Test
	public void doesNotAttackDyingNpc()
	{
		NPC npc = mock(NPC.class);
		when(npc.isDead()).thenReturn(true);
		assertFalse(actions.attack(npc, Weapon.PIPE, false));
		verify(client, never()).menuAction(anyInt(), anyInt(), any(), anyInt(), anyInt(), anyString(), anyString());
	}

	@Test
	public void togglesSpecialViaWidgetInsteadOfOnlyChangingLocalVarp()
	{
		when(config.meleeGear()).thenReturn("");
		when(equipment.contains(13652)).thenReturn(true);
		when(equipment.getItem(3)).thenReturn(new Item(13652, 1));
		Widget special = mock(Widget.class);
		when(special.isIf3()).thenReturn(true);
		when(special.getId()).thenReturn(InterfaceID.CombatInterface.SPECIAL_ATTACK);
		when(special.getIndex()).thenReturn(-1);
		when(client.getWidget(InterfaceID.CombatInterface.SPECIAL_ATTACK)).thenReturn(special);
		NPC npc = mock(NPC.class);
		NPCComposition composition = mock(NPCComposition.class);
		when(npc.getHealthRatio()).thenReturn(16);
		when(npc.getTransformedComposition()).thenReturn(composition);
		when(composition.getActions()).thenReturn(new String[]{"Attack"});
		assertTrue(actions.attack(npc, Weapon.CLAWS, true));
		verify(client).menuAction(-1, InterfaceID.CombatInterface.SPECIAL_ATTACK, MenuAction.CC_OP, 1, -1, "Activate", "");
	}

	@Test
	public void capturesArmorButKeepsTwoHandedWeaponAndShieldOutOfRangedSetup()
	{
		Item[] equipped = {new Item(10, 1), new Item(11, 1), new Item(12, 1), new Item(20997, 1), new Item(14, 1), new Item(15, 1)};
		when(equipment.getItems()).thenReturn(equipped);
		assertEquals("10,11,12,14", actions.capture(Style.RANGE));
		assertEquals("10,11,12,14,15", actions.capture(Style.MAGIC));
	}

	@Test
	public void parsesGearAndRejectsMalformedConfiguration()
	{
		assertEquals(Arrays.asList(100, 200), RangerActions.parseGear("100, 200,100"));
		try { RangerActions.parseGear("100,drop"); fail(); }
		catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("item IDs")); }
	}

	@Test
	public void preparingWeaponsDuringCooldownDoesNotChangeOffensivePrayers()
	{
		when(config.prayers()).thenReturn(true);
		assertTrue(actions.prepare(Weapon.PIPE));
		verify(client, never()).isPrayerActive(any());
		verify(client, never()).getWidget(InterfaceID.Prayerbook.CONTAINER);
	}

	@Test
	public void normalizesPreviousExtremeZoomToOrdinaryRoomView()
	{
		when(client.getVarcIntValue(VarClientID.CAMERA_ZOOM_SMALL)).thenReturn(-272);
		when(client.getVarcIntValue(VarClientID.CAMERA_ZOOM_BIG)).thenReturn(-272);
		when(client.getCameraPitchTarget()).thenReturn(383);
		actions.prepareRoomCamera();
		verify(client).runScript(ScriptID.CAMERA_DO_ZOOM, 128, 128);
		verify(client, never()).setCameraPitchTarget(anyInt());
	}
}
