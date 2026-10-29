package net.runelite.client.plugins.nylomage;

import org.localscape.nylocas.mage.MageStrategy;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import org.localscape.nylocas.mage.MageStrategy.Weapon;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

public class MageActionsTest
{
	private Client client;
	private MageActions actions;
	private ItemContainer equipment;
	private NPC npc;
	@Before public void setup()
	{
		client = mock(Client.class); equipment = mock(ItemContainer.class);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(equipment);
		when(equipment.contains(anyInt())).thenReturn(true);
		when(equipment.getItem(3)).thenReturn(new Item(27624, 1));
		when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(1);
		actions = new MageActions(client, new MageConfig() {}, () -> true);
		npc = mock(NPC.class); when(npc.getHealthRatio()).thenReturn(8); when(npc.getIndex()).thenReturn(42);
		when(npc.getName()).thenReturn("Nylocas Hagios");
		NPCComposition composition = mock(NPCComposition.class); when(npc.getTransformedComposition()).thenReturn(composition);
		when(composition.getActions()).thenReturn(new String[]{null, "Attack"});
	}
	@Test public void barrageSelectsNativeSpellBeforeTargetingNpc()
	{
		Widget spell = mock(Widget.class); when(client.getWidget(InterfaceID.MagicSpellbook.ICE_BARRAGE)).thenReturn(spell);
		when(spell.getId()).thenReturn(InterfaceID.MagicSpellbook.ICE_BARRAGE); when(spell.getIndex()).thenReturn(-1);
		assertTrue(actions.attack(npc, Weapon.BARRAGE, false));
		InOrder order = inOrder(client);
		order.verify(client).menuAction(-1, InterfaceID.MagicSpellbook.ICE_BARRAGE, MenuAction.WIDGET_TARGET, 0, -1, "Cast", "Ice Barrage");
		order.verify(client).menuAction(0, 0, MenuAction.WIDGET_TARGET_ON_NPC, 42, -1, "Cast", "Nylocas Hagios");
		verify(client, never()).menuAction(anyInt(), anyInt(), eq(MenuAction.NPC_SECOND_OPTION), anyInt(), anyInt(), anyString(), anyString());
	}
	@Test public void missingSpellWidgetFailsWithoutSendingNpcTarget()
	{
		assertFalse(actions.attack(npc, Weapon.BARRAGE, false)); assertTrue(actions.error().contains("Ice Barrage"));
		verify(client, never()).menuAction(anyInt(), anyInt(), eq(MenuAction.WIDGET_TARGET_ON_NPC), anyInt(), anyInt(), anyString(), anyString());
	}
	@Test public void dinhSpecialIsToggledAfterPreparingWeaponAndBeforeAttack()
	{
		Widget special = mock(Widget.class); when(client.getWidget(InterfaceID.CombatInterface.SPECIAL_ATTACK)).thenReturn(special);
		when(special.getId()).thenReturn(InterfaceID.CombatInterface.SPECIAL_ATTACK); when(special.getIndex()).thenReturn(-1);
		when(special.isIf3()).thenReturn(true);
		assertTrue(actions.attack(npc, Weapon.DINH, true));
		InOrder order = inOrder(client);
		order.verify(client).menuAction(-1, InterfaceID.CombatInterface.SPECIAL_ATTACK, MenuAction.CC_OP, 1, -1, "Activate", "");
		order.verify(client).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylocas Hagios");
	}
	@Test public void variantsAndAyakOpeningValidateOnlyTheirConfiguredWeapons()
	{
		actions = new MageActions(client, new MageConfig()
		{
			public boolean shadowOpening() { return false; }
			public int scytheId() { return 25739; }
			public int dinhId() { return 28682; }
		}, () -> true);
		assertTrue(actions.validate()); verify(equipment).contains(25739); verify(equipment).contains(28682);
		verify(equipment, never()).contains(27275); verify(equipment, never()).contains(22325);
	}
	@Test public void wrongSpellbookOrMissingWeaponPreventsArming()
	{
		when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(0);
		assertFalse(actions.validate()); assertTrue(actions.error().contains("Ancient"));
		when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(1); when(equipment.contains(29084)).thenReturn(false);
		assertFalse(actions.validate()); assertTrue(actions.error().contains("29084"));
	}
	@Test public void selectsTheRequestedCombatButtonForEveryStance()
	{
		int[] buttons = {InterfaceID.CombatInterface._0, InterfaceID.CombatInterface._1,
			InterfaceID.CombatInterface._2, InterfaceID.CombatInterface._3};
		for (int stance = 0; stance < buttons.length; stance++)
		{
			clearInvocations(client);
			when(client.getVarpValue(43)).thenReturn((stance + 1) % buttons.length);
			Widget button = mock(Widget.class);
			when(button.getId()).thenReturn(buttons[stance]);
			when(button.getIndex()).thenReturn(-1);
			when(button.isIf3()).thenReturn(true);
			when(client.getWidget(buttons[stance])).thenReturn(button);
			assertTrue(actions.prepare(27624, 2, stance));
			verify(client).menuAction(-1, buttons[stance], MenuAction.CC_OP, 1, -1, "Activate", "");
		}
	}
	@Test public void preparesShadowInLongrangeBeforeTheRoomStarts()
	{
		Widget button = mock(Widget.class);
		when(button.getId()).thenReturn(InterfaceID.CombatInterface._3);
		when(button.getIndex()).thenReturn(-1);
		when(button.isIf3()).thenReturn(true);
		when(client.getWidget(InterfaceID.CombatInterface._3)).thenReturn(button);
		assertTrue(actions.prepareWaiting());
		verify(client).menuAction(-1, InterfaceID.CombatInterface._3, MenuAction.CC_OP, 1, -1, "Activate", "");
	}
}
