package net.runelite.client.plugins.nyloranger;

import org.localscape.nylocas.ranger.RangerStrategy;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RangerPrayersTest
{
	private Client client;
	private RangerConfig config;
	private RangerPrayers prayers;
	private ItemContainer inventory;
	private Player player;
	private WorldArea local;
	private WorldView view;
	private final Map<NPC, Crab> crabs = new LinkedHashMap<>();
	private int index;

	@Before
	public void setup()
	{
		client = mock(Client.class);
		config = mock(RangerConfig.class);
		when(config.prayers()).thenReturn(true);
		when(config.protectionPrayers()).thenReturn(true);
		when(config.restorePrayer()).thenReturn(true);
		when(config.prayerThreshold()).thenReturn(20);
		when(client.getRealSkillLevel(Skill.PRAYER)).thenReturn(99);
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(99);
		inventory = mock(ItemContainer.class);
		when(inventory.getItems()).thenReturn(new Item[0]);
		when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inventory);
		player = mock(Player.class);
		local = mock(WorldArea.class);
		when(player.getWorldArea()).thenReturn(local);
		when(client.getLocalPlayer()).thenReturn(player);
		view = mock(WorldView.class);
		when(client.getTopLevelWorldView()).thenReturn(view);
		Widget root = mock(Widget.class);
		when(client.getWidget(InterfaceID.Prayerbook.CONTAINER)).thenReturn(root);
		Widget[] children = new Widget[6];
		String[] names = {"Piety", "Rigour", "Augury", "Protect from Melee", "Protect from Missiles", "Protect from Magic"};
		for (int i = 0; i < children.length; i++)
		{
			children[i] = mock(Widget.class);
			when(children[i].getName()).thenReturn("<col=ff0000>" + names[i] + "</col>");
			when(children[i].getActions()).thenReturn(new String[]{"Activate"});
			when(children[i].getId()).thenReturn(1000 + i);
			when(children[i].getIndex()).thenReturn(-1);
			when(children[i].isIf3()).thenReturn(true);
		}
		when(root.getStaticChildren()).thenReturn(children);
		prayers = new RangerPrayers(client, config, new RangerActions(client, config, () -> true));
	}

	private NPC attacker(boolean big, Style style)
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(8348 + (big ? 3 : 0) + style.ordinal());
		when(npc.getInteracting()).thenReturn(player);
		when(npc.getHealthRatio()).thenReturn(big ? 16 : 8);
		WorldArea area = mock(WorldArea.class);
		when(area.distanceTo(local)).thenReturn(1);
		when(area.isInMeleeDistance(local)).thenReturn(true);
		when(area.hasLineOfSightTo(view, local)).thenReturn(true);
		when(npc.getWorldArea()).thenReturn(area);
		crabs.put(npc, new Crab(++index, index, 0, 12, Lane.SPLIT, (char) 0, big, style, RangerStrategy.tile(3294, 4248)));
		return npc;
	}

	private void activated(int widget, int times)
	{
		verify(client, times(times)).menuAction(-1, widget, MenuAction.CC_OP, 1, -1, "Activate", "");
	}

	private void potion(int... ids)
	{
		Item[] items = new Item[ids.length];
		for (int i = 0; i < ids.length; i++)
		{
			items[i] = new Item(ids[i], 1);
			ItemComposition definition = mock(ItemComposition.class);
			when(definition.getInventoryActions()).thenReturn(new String[]{"Drink", null, null, null, "Drop"});
			when(client.getItemDefinition(ids[i])).thenReturn(definition);
		}
		when(inventory.getItems()).thenReturn(items);
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(20);
	}

	@Test
	public void activeOffensivePrayerIsNotToggledBetweenShots()
	{
		when(client.isPrayerActive(Prayer.RIGOUR)).thenReturn(true);
		for (int tick = 0; tick < 8; tick++) prayers.tick(crabs, tick);
		verify(client, never()).menuAction(anyInt(), anyInt(), any(), anyInt(), anyInt(), anyString(), anyString());
	}

	@Test
	public void switchesOnlyToNewAttackStyleAndKeepsItDuringCooldown()
	{
		when(client.isPrayerActive(Prayer.RIGOUR)).thenReturn(true);
		prayers.forAttack(Style.MAGIC, 0);
		activated(1002, 1);
		activated(1001, 0); // No explicit off toggle for Rigour.
		when(client.isPrayerActive(Prayer.RIGOUR)).thenReturn(false);
		when(client.isPrayerActive(Prayer.AUGURY)).thenReturn(true);
		for (int tick = 1; tick < 8; tick++) prayers.tick(crabs, tick);
		activated(1002, 1);
		activated(1001, 0);
	}

	@Test
	public void meleePrayerStillRecognizesLocalScapeZealLabel()
	{
		Widget piety = client.getWidget(InterfaceID.Prayerbook.CONTAINER).getStaticChildren()[0];
		when(piety.getName()).thenReturn("<col=ff0000>Zeal</col>");
		prayers.forAttack(Style.MELEE, 0);
		activated(1000, 1);
	}

	@Test
	public void delayedPrayerFeedbackDoesNotCauseEveryTickToggleRequests()
	{
		prayers.tick(crabs, 0);
		prayers.forAttack(Style.RANGE, 0);
		prayers.tick(crabs, 1);
		activated(1001, 1);
		when(client.isPrayerActive(Prayer.RIGOUR)).thenReturn(true);
		prayers.tick(crabs, 2);
		activated(1001, 1);
	}

	@Test
	public void oneBigAttackerOutranksSeveralSmallAttackers()
	{
		attacker(true, Style.MELEE);
		for (int i = 0; i < 4; i++) attacker(false, Style.MAGIC);
		prayers.tick(crabs, 0);
		activated(1003, 1);
		activated(1005, 0);
	}

	@Test
	public void moreBigAttackersWinBeforeSmallAttackerCounts()
	{
		attacker(true, Style.MAGIC);
		attacker(true, Style.MAGIC);
		attacker(true, Style.RANGE);
		for (int i = 0; i < 5; i++) attacker(false, Style.RANGE);
		prayers.tick(crabs, 0);
		activated(1005, 1);
		activated(1004, 0);
	}

	@Test
	public void equalThreatsKeepCurrentProtectionAndNoThreatDoesNotTurnItOff()
	{
		when(client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC)).thenReturn(true);
		attacker(true, Style.RANGE);
		attacker(true, Style.MAGIC);
		prayers.tick(crabs, 0);
		crabs.clear();
		for (int tick = 1; tick < 8; tick++) prayers.tick(crabs, tick);
		activated(1004, 0);
		activated(1005, 0);
	}

	@Test
	public void ignoresAttackersTargetingPartnerAndPillarForms()
	{
		NPC partnerBig = attacker(true, Style.MAGIC);
		when(partnerBig.getInteracting()).thenReturn(mock(Player.class));
		NPC pillarBig = attacker(true, Style.MAGIC);
		when(pillarBig.getId()).thenReturn(8347);
		attacker(false, Style.RANGE);
		prayers.tick(crabs, 0);
		activated(1004, 1);
		activated(1005, 0);
	}

	@Test
	public void ignoresDyingOutOfRangeAndObstructedBigs()
	{
		NPC dying = attacker(true, Style.MAGIC);
		when(dying.getHealthRatio()).thenReturn(0);
		NPC distant = attacker(true, Style.MAGIC);
		when(distant.getWorldArea().distanceTo(local)).thenReturn(3);
		NPC blocked = attacker(true, Style.MAGIC);
		when(blocked.getWorldArea().hasLineOfSightTo(view, local)).thenReturn(false);
		NPC diagonal = attacker(true, Style.MELEE);
		when(diagonal.getWorldArea().isInMeleeDistance(local)).thenReturn(false);
		attacker(false, Style.RANGE);
		prayers.tick(crabs, 0);
		activated(1004, 1);
		activated(1005, 0);
		activated(1003, 0);
	}

	@Test
	public void inactiveLateMeleeFlickerCannotStealProtection()
	{
		NPC passive = attacker(true, Style.MELEE);
		crabs.put(passive, new Crab(++index, index, 0, 17, Lane.WEST, 'S', true, Style.MELEE, RangerStrategy.tile(3294, 4248)));
		attacker(false, Style.RANGE);
		prayers.tick(crabs, 26);
		activated(1004, 1);
		activated(1003, 0);
	}

	@Test
	public void normalMeleeAggroStillThreatensAfterAge26()
	{
		attacker(true, Style.MELEE);
		attacker(false, Style.RANGE);
		prayers.tick(crabs, 26);
		activated(1003, 1);
	}

	@Test
	public void protectionFollowsAttackingCrabsCurrentFlickerStyle()
	{
		NPC big = attacker(true, Style.RANGE);
		prayers.tick(crabs, 0);
		when(client.isPrayerActive(Prayer.PROTECT_FROM_MISSILES)).thenReturn(true);
		when(big.getId()).thenReturn(8353);
		crabs.get(big).style = Style.MAGIC;
		prayers.tick(crabs, 1);
		activated(1004, 1);
		activated(1005, 1);
	}

	@Test
	public void drinksEveryStandardDoseThroughNativeInventoryAction()
	{
		for (int id : new int[]{2434, 139, 141, 143})
		{
			prayers.reset();
			clearInvocations(client);
			potion(id);
			prayers.tick(crabs, 0);
			verify(client).menuAction(0, InterfaceID.Inventory.ITEMS, MenuAction.ITEM_FIRST_OPTION, id, id, "Drink", "");
		}
	}

	@Test
	public void finishesPartUsedBottleFirstAndEnforcesThreeTickDoseCooldown()
	{
		potion(2434, 143);
		for (int tick = 0; tick < 3; tick++) prayers.tick(crabs, tick);
		verify(client, times(1)).menuAction(1, InterfaceID.Inventory.ITEMS, MenuAction.ITEM_FIRST_OPTION, 143, 143, "Drink", "");
		prayers.tick(crabs, 3);
		verify(client, times(2)).menuAction(1, InterfaceID.Inventory.ITEMS, MenuAction.ITEM_FIRST_OPTION, 143, 143, "Drink", "");
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(50);
		prayers.tick(crabs, 6);
		verify(client, times(2)).menuAction(1, InterfaceID.Inventory.ITEMS, MenuAction.ITEM_FIRST_OPTION, 143, 143, "Drink", "");
	}

	@Test
	public void doesNotDrinkAboveThresholdOrWasteADoseNearFullPrayer()
	{
		potion(143);
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(21);
		prayers.tick(crabs, 0);
		when(config.prayerThreshold()).thenReturn(99);
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(99);
		prayers.tick(crabs, 1);
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(70);
		prayers.tick(crabs, 2);
		verify(client, never()).menuAction(anyInt(), eq(InterfaceID.Inventory.ITEMS), any(), anyInt(), anyInt(), eq("Drink"), anyString());
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(68);
		prayers.tick(crabs, 3);
		verify(client).menuAction(0, InterfaceID.Inventory.ITEMS, MenuAction.ITEM_FIRST_OPTION, 143, 143, "Drink", "");
	}

	@Test
	public void lowPrayerWithoutSuppliesIsReportedAndCanRecover()
	{
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(10);
		prayers.tick(crabs, 0);
		assertTrue(prayers.warning().contains("no prayer potion"));
		potion(143);
		prayers.tick(crabs, 1);
		assertNull(prayers.warning());
	}

	@Test
	public void supportsIf3DrinkAndDoesNotTogglePrayersWithZeroPoints()
	{
		potion(143);
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(0);
		Widget root = mock(Widget.class);
		Widget child = mock(Widget.class);
		when(root.isIf3()).thenReturn(true);
		when(root.getId()).thenReturn(InterfaceID.Inventory.ITEMS);
		when(root.getChild(0)).thenReturn(child);
		when(child.getActions()).thenReturn(new String[]{"Drink"});
		when(client.getWidget(InterfaceID.Inventory.ITEMS)).thenReturn(root);
		prayers.tick(crabs, 0);
		verify(client).menuAction(0, InterfaceID.Inventory.ITEMS, MenuAction.CC_OP, 1, 143, "Drink", "");
		activated(1001, 0);
	}

	@Test
	public void configurationCanDisablePrayerAndSupplyManagement()
	{
		when(config.prayers()).thenReturn(false);
		when(config.protectionPrayers()).thenReturn(false);
		when(config.restorePrayer()).thenReturn(false);
		potion(143);
		attacker(true, Style.MAGIC);
		prayers.tick(crabs, 0);
		prayers.forAttack(Style.MELEE, 0);
		verify(client, never()).menuAction(anyInt(), anyInt(), any(), anyInt(), anyInt(), anyString(), anyString());
	}
}
