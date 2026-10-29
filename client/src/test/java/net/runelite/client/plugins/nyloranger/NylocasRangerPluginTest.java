package net.runelite.client.plugins.nyloranger;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.ScriptID;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class NylocasRangerPluginTest
{
	@Mock private Client client;
	@Mock private ClientThread clientThread;
	@Mock private ClientToolbar toolbar;
	@Mock private ConfigManager manager;
	@Mock private RangerConfig config;
	@InjectMocks private NylocasRangerPlugin plugin;

	@Before
	public void setup()
	{
		MockitoAnnotations.initMocks(this);
		doAnswer(call -> { ((Runnable) call.getArgument(0)).run(); return null; }).when(clientThread).invokeLater(any(Runnable.class));
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(99);
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getHealthRatio()).thenReturn(-1);
		when(player.getLocalLocation()).thenReturn(new LocalPoint(15 * 128 + 64, 32 * 128 + 64));
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3295, 4256, 0));
		WorldView view = mock(WorldView.class);
		when(client.getWorldView(-1)).thenReturn(view);
		when(client.getTopLevelWorldView()).thenReturn(view);
		when(view.getBaseX()).thenReturn(3280);
		when(view.getBaseY()).thenReturn(4224);
		when(view.getId()).thenReturn(-1);
		when(view.getSizeX()).thenReturn(104);
		when(view.getSizeY()).thenReturn(104);
		Scene scene = mock(Scene.class);
		when(view.getScene()).thenReturn(scene);
		Tile[][][] tiles = new Tile[4][104][104];
		when(scene.getTiles()).thenReturn(tiles);
		Tile tile = mock(Tile.class);
		tiles[0][15][31] = tile;
		GameObject barrier = mock(GameObject.class);
		when(tile.getGameObjects()).thenReturn(new GameObject[]{barrier});
		when(barrier.getLocalLocation()).thenReturn(new LocalPoint(15 * 128 + 64, 31 * 128 + 64));
		when(barrier.getSceneMinLocation()).thenReturn(new Point(15, 31));
		when(barrier.getId()).thenReturn(32755);
		ObjectComposition object = mock(ObjectComposition.class);
		when(client.getObjectDefinition(32755)).thenReturn(object);
		when(object.getActions()).thenReturn(new String[]{"Pass-through"});
		ItemContainer inventory = mock(ItemContainer.class);
		when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inventory);
		when(inventory.contains(anyInt())).thenReturn(true);
		ItemContainer equipment = mock(ItemContainer.class);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(equipment);
		when(equipment.contains(20997)).thenReturn(true);
		when(equipment.getItem(3)).thenReturn(new Item(20997, 1));
	}

	@Test
	public void clicksBarrierEveryGameTickButNeverConfirmsEntry() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::startUp);
		for (int tick = 0; tick < 4; tick++)
		{
			when(client.getTickCount()).thenReturn(tick);
			plugin.onGameTick(new GameTick());
			plugin.onGameTick(new GameTick()); // Duplicate callback must not double click.
		}
		verify(client, times(4)).menuAction(15, 31, MenuAction.GAME_OBJECT_FIRST_OPTION, 32755, -1, "Pass-through", "Nylocas barrier");
		verify(client, never()).menuAction(anyInt(), anyInt(), eq(MenuAction.WIDGET_CONTINUE), anyInt(), anyInt(), anyString(), anyString());
		verify(client, times(1)).runScript(ScriptID.CAMERA_DO_ZOOM, 128, 128);
		SwingUtilities.invokeAndWait(plugin::shutDown);
		when(client.getTickCount()).thenReturn(5);
		plugin.onGameTick(new GameTick());
		verify(client, times(4)).menuAction(15, 31, MenuAction.GAME_OBJECT_FIRST_OPTION, 32755, -1, "Pass-through", "Nylocas barrier");
	}


	@Test public void startRoomConfirmsBeforeSetupClicksCanCancelTheDialogue() throws Exception
	{
		entryDialogue("Start the Nylocas encounter?");
		Widget stance = mock(Widget.class);
		when(stance.getId()).thenReturn(InterfaceID.CombatInterface._1);
		when(stance.isIf3()).thenReturn(true);
		when(client.getWidget(InterfaceID.CombatInterface._1)).thenReturn(stance);
		when(client.getVarpValue(43)).thenReturn(0);
		java.util.concurrent.atomic.AtomicBoolean dialogueActive = new java.util.concurrent.atomic.AtomicBoolean(true);
		java.util.concurrent.atomic.AtomicBoolean roomStarted = new java.util.concurrent.atomic.AtomicBoolean();
		// Server combat-tab clicks close the modal and cancel its suspended entry script.
		doAnswer(call ->
		{
			MenuAction action = call.getArgument(2);
			int widget = call.getArgument(1);
			if (action == MenuAction.CC_OP && widget == InterfaceID.CombatInterface._1) dialogueActive.set(false);
			if (action == MenuAction.WIDGET_CONTINUE) roomStarted.set(dialogueActive.get());
			return null;
		}).when(client).menuAction(anyInt(), anyInt(), any(MenuAction.class), anyInt(), anyInt(), anyString(), anyString());
		ArgumentCaptor<NavigationButton> navigation = ArgumentCaptor.forClass(NavigationButton.class);
		SwingUtilities.invokeAndWait(plugin::startUp);
		verify(toolbar).addNavigation(navigation.capture());
		SwingUtilities.invokeAndWait(find(navigation.getValue().getPanel(), JButton.class, "Start room")::doClick);
		plugin.onGameTick(new GameTick());
		assertTrue("Enter must reach the server while the entry coroutine is still active", roomStarted.get());
		when(client.getTickCount()).thenReturn(1); plugin.onGameTick(new GameTick());
		verify(client, never()).getWidget(InterfaceID.CombatInterface._1);
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test public void startRoomButtonConfirmsOnceAndStopsReclickingBarrier() throws Exception
	{
		entryDialogue("Start the Nylocas encounter?");
		ArgumentCaptor<NavigationButton> navigation = ArgumentCaptor.forClass(NavigationButton.class);
		SwingUtilities.invokeAndWait(plugin::startUp);
		verify(toolbar).addNavigation(navigation.capture());
		plugin.onGameTick(new GameTick());
		verify(client, never()).menuAction(anyInt(), anyInt(), eq(MenuAction.WIDGET_CONTINUE), anyInt(), anyInt(), anyString(), anyString());
		JButton button = find(navigation.getValue().getPanel(), JButton.class, "Start room");
		assertNotNull(button);
		SwingUtilities.invokeAndWait(button::doClick);
		for (int tick = 1; tick <= 3; tick++)
		{
			when(client.getTickCount()).thenReturn(tick);
			plugin.onGameTick(new GameTick());
		}
		verify(client, times(1)).menuAction(1, InterfaceID.Chatmenu.OPTIONS, MenuAction.WIDGET_CONTINUE, 0, -1, "Enter", "");
		verify(client, never()).menuAction(15, 31, MenuAction.GAME_OBJECT_FIRST_OPTION, 32755, -1, "Pass-through", "Nylocas barrier");
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test public void waitingAtTheEntryPromptDoesNotCancelThePartnerHandshake() throws Exception
	{
		entryDialogue("Start the Nylocas encounter?");
		SwingUtilities.invokeAndWait(plugin::startUp);
		for (int tick = 0; tick < 3; tick++)
		{
			when(client.getTickCount()).thenReturn(tick);
			plugin.onGameTick(new GameTick());
		}
		verify(client, never()).menuAction(anyInt(), anyInt(), any(MenuAction.class), anyInt(), anyInt(), anyString(), anyString());
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test public void roomStartRetriesAnUnacknowledgedEntryWithAFreshPrompt() throws Exception
	{
		entryDialogue("Start the Nylocas encounter?");
		ArgumentCaptor<NavigationButton> navigation = ArgumentCaptor.forClass(NavigationButton.class);
		SwingUtilities.invokeAndWait(plugin::startUp);
		verify(toolbar).addNavigation(navigation.capture());
		SwingUtilities.invokeAndWait(find(navigation.getValue().getPanel(), JButton.class, "Start room")::doClick);
		for (int tick = 0; tick < 4; tick++)
		{
			when(client.getTickCount()).thenReturn(tick);
			plugin.onGameTick(new GameTick());
		}
		verify(client, times(1)).menuAction(1, InterfaceID.Chatmenu.OPTIONS, MenuAction.WIDGET_CONTINUE, 0, -1, "Enter", "");
		verify(client, never()).menuAction(15, 31, MenuAction.GAME_OBJECT_FIRST_OPTION, 32755, -1, "Pass-through", "Nylocas barrier");
		when(client.getTickCount()).thenReturn(4); plugin.onGameTick(new GameTick());
		verify(client).menuAction(15, 31, MenuAction.GAME_OBJECT_FIRST_OPTION, 32755, -1, "Pass-through", "Nylocas barrier");
		when(client.getTickCount()).thenReturn(5); plugin.onGameTick(new GameTick());
		verify(client, times(2)).menuAction(1, InterfaceID.Chatmenu.OPTIONS, MenuAction.WIDGET_CONTINUE, 0, -1, "Enter", "");
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test public void roomStartCanWaitForDialogueButStopCancelsTheRequest() throws Exception
	{
		ArgumentCaptor<NavigationButton> navigation = ArgumentCaptor.forClass(NavigationButton.class);
		SwingUtilities.invokeAndWait(plugin::startUp);
		verify(toolbar).addNavigation(navigation.capture());
		JButton enter = find(navigation.getValue().getPanel(), JButton.class, "Start room");
		SwingUtilities.invokeAndWait(enter::doClick);
		plugin.onGameTick(new GameTick()); // No dialogue yet: still click barrier.
		verify(client).menuAction(15, 31, MenuAction.GAME_OBJECT_FIRST_OPTION, 32755, -1, "Pass-through", "Nylocas barrier");
		entryDialogue("Start the Nylocas encounter?");
		SwingUtilities.invokeAndWait(find(navigation.getValue().getPanel(), JButton.class, "Stop ranger")::doClick);
		SwingUtilities.invokeAndWait(find(navigation.getValue().getPanel(), JButton.class, "Start ranger")::doClick);
		when(client.getTickCount()).thenReturn(1); plugin.onGameTick(new GameTick());
		verify(client, never()).menuAction(anyInt(), anyInt(), eq(MenuAction.WIDGET_CONTINUE), anyInt(), anyInt(), anyString(), anyString());
		SwingUtilities.invokeAndWait(enter::doClick);
		when(client.getTickCount()).thenReturn(2); plugin.onGameTick(new GameTick());
		verify(client).menuAction(1, InterfaceID.Chatmenu.OPTIONS, MenuAction.WIDGET_CONTINUE, 0, -1, "Enter", "");
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test public void startRoomRejectsUnrelatedOrHiddenDialogue() throws Exception
	{
		Widget options = entryDialogue("Enter another encounter?");
		ArgumentCaptor<NavigationButton> navigation = ArgumentCaptor.forClass(NavigationButton.class);
		SwingUtilities.invokeAndWait(plugin::startUp); verify(toolbar).addNavigation(navigation.capture());
		SwingUtilities.invokeAndWait(find(navigation.getValue().getPanel(), JButton.class, "Start room")::doClick);
		plugin.onGameTick(new GameTick());
		options = entryDialogue("Start the Nylocas encounter?"); when(options.isHidden()).thenReturn(true);
		when(client.getTickCount()).thenReturn(1); plugin.onGameTick(new GameTick());
		verify(client, never()).menuAction(anyInt(), anyInt(), eq(MenuAction.WIDGET_CONTINUE), anyInt(), anyInt(), anyString(), anyString());
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	private Widget entryDialogue(String title)
	{
		Widget options = mock(Widget.class), heading = mock(Widget.class), enter = mock(Widget.class), cancel = mock(Widget.class);
		when(client.getWidget(InterfaceID.Chatmenu.OPTIONS)).thenReturn(options);
		when(options.getId()).thenReturn(InterfaceID.Chatmenu.OPTIONS);
		when(options.getDynamicChildren()).thenReturn(new Widget[]{heading, enter, cancel});
		when(heading.getText()).thenReturn(title);
		when(enter.getText()).thenReturn("<col=ffffff>Enter</col>"); when(enter.getIndex()).thenReturn(1);
		when(cancel.getText()).thenReturn("Cancel"); when(cancel.getIndex()).thenReturn(2);
		return options;
	}

	@Test
	public void setsCameraBeforeBarrierClickWithoutRepeatingWhileWaiting() throws Exception
	{
		when(client.getVarcIntValue(VarClientID.CAMERA_ZOOM_SMALL)).thenReturn(512);
		when(client.getVarcIntValue(VarClientID.CAMERA_ZOOM_BIG)).thenReturn(256);
		when(client.getCameraPitchTarget()).thenReturn(128);
		SwingUtilities.invokeAndWait(plugin::startUp);
		plugin.onGameTick(new GameTick());
		InOrder order = inOrder(client);
		order.verify(client).runScript(ScriptID.CAMERA_DO_ZOOM, 128, 128);
		order.verify(client).setCameraPitchTarget(383);
		order.verify(client).menuAction(15, 31, MenuAction.GAME_OBJECT_FIRST_OPTION, 32755, -1, "Pass-through", "Nylocas barrier");
		when(client.getVarcIntValue(VarClientID.CAMERA_ZOOM_SMALL)).thenReturn(-272);
		when(client.getVarcIntValue(VarClientID.CAMERA_ZOOM_BIG)).thenReturn(-272);
		when(client.getCameraPitchTarget()).thenReturn(383);
		when(client.getTickCount()).thenReturn(1);
		plugin.onGameTick(new GameTick());
		verify(client, times(1)).runScript(ScriptID.CAMERA_DO_ZOOM, 128, 128);
		verify(client, times(1)).setCameraPitchTarget(383);
		// Camera readback changes or manual adjustments must not trigger another reset.
		when(client.getVarcIntValue(VarClientID.CAMERA_ZOOM_BIG)).thenReturn(512);
		when(client.getCameraPitchTarget()).thenReturn(128);
		when(client.getTickCount()).thenReturn(2);
		plugin.onGameTick(new GameTick());
		verify(client, times(1)).runScript(ScriptID.CAMERA_DO_ZOOM, 128, 128);
		verify(client, times(1)).setCameraPitchTarget(383);
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test
	public void preservesPreparedCameraAndDoesNotAdjustCameraAfterEntry() throws Exception
	{
		when(client.getVarcIntValue(VarClientID.CAMERA_ZOOM_SMALL)).thenReturn(128);
		when(client.getVarcIntValue(VarClientID.CAMERA_ZOOM_BIG)).thenReturn(128);
		when(client.getCameraPitchTarget()).thenReturn(512);
		SwingUtilities.invokeAndWait(plugin::startUp);
		plugin.onGameTick(new GameTick());
		Player player = client.getLocalPlayer();
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3292, 4249, 0));
		when(player.getLocalLocation()).thenReturn(new LocalPoint(12 * 128 + 64, 25 * 128 + 64));
		when(client.getVarcIntValue(VarClientID.CAMERA_ZOOM_SMALL)).thenReturn(512);
		when(client.getVarcIntValue(VarClientID.CAMERA_ZOOM_BIG)).thenReturn(512);
		when(client.getCameraPitchTarget()).thenReturn(128);
		when(client.getTickCount()).thenReturn(1);
		plugin.onGameTick(new GameTick());
		verify(client, never()).runScript(anyInt(), any(), any());
		verify(client, never()).setCameraPitchTarget(anyInt());
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test
	public void partnerEntryAndRealSpawnTriggerOpeningAttackThenBossStopsController() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::startUp);
		Player player = client.getLocalPlayer();
		when(player.getLocalLocation()).thenReturn(new LocalPoint(12 * 128 + 64, 25 * 128 + 64));
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3292, 4249, 0));
		WorldArea area = mock(WorldArea.class);
		when(player.getWorldArea()).thenReturn(area);
		when(area.hasLineOfSightTo(any(WorldView.class), any(WorldArea.class))).thenReturn(true);
		when(client.getTickCount()).thenReturn(4);
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(1);
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(8343);
		when(npc.getIndex()).thenReturn(42);
		when(npc.getName()).thenReturn("Nylocas Toxobolos");
		when(npc.getHealthRatio()).thenReturn(8);
		when(npc.getLocalLocation()).thenReturn(new LocalPoint(1 * 128 + 64, 24 * 128 + 64));
		when(npc.getWorldLocation()).thenReturn(new WorldPoint(3281, 4248, 0));
		when(npc.getWorldArea()).thenReturn(new WorldPoint(3281, 4248, 0).toWorldArea());
		NPCComposition composition = mock(NPCComposition.class);
		when(npc.getTransformedComposition()).thenReturn(composition);
		when(composition.getActions()).thenReturn(new String[]{null, "Attack"});
		when(client.getNpcs()).thenReturn(java.util.Collections.singletonList(npc));
		plugin.onNpcSpawned(new NpcSpawned(npc));
		plugin.onGameTick(new GameTick());
		verify(client).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylocas Toxobolos");
		NPC boss = mock(NPC.class);
		when(boss.getId()).thenReturn(8354);
		when(client.getNpcs()).thenReturn(java.util.Collections.singletonList(boss));
		when(client.getTickCount()).thenReturn(5);
		plugin.onGameTick(new GameTick());
		when(client.getTickCount()).thenReturn(6);
		plugin.onGameTick(new GameTick());
		verify(client, times(1)).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylocas Toxobolos");
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test
	public void liveHpUpdateFinishesSurvivorOnNextReadyTick() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::startUp);
		Player player = client.getLocalPlayer();
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3295, 4248, 0));
		WorldArea area = mock(WorldArea.class);
		when(player.getWorldArea()).thenReturn(area);
		when(area.hasLineOfSightTo(any(WorldView.class), any(WorldArea.class))).thenReturn(true);
		when(client.getTickCount()).thenReturn(100);
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(12);
		when(client.getItemContainer(InventoryID.EQUIPMENT).getItem(3)).thenReturn(new Item(12926, 1));
		when(client.getItemContainer(InventoryID.EQUIPMENT).contains(12926)).thenReturn(true);
		NPC survivor = rangedSmall(42, 3292);
		when(client.getNpcs()).thenReturn(java.util.Collections.singletonList(survivor));
		plugin.onNpcSpawned(new NpcSpawned(survivor));
		plugin.onGameTick(new GameTick());
		verify(client).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylocas Toxobolos");
		when(client.getTickCount()).thenReturn(101);
		when(player.getInteracting()).thenReturn(survivor);
		when(player.getAnimation()).thenReturn(5061);
		AnimationChanged launch = new AnimationChanged();
		launch.setActor(player);
		plugin.onAnimationChanged(launch);
		when(client.getTickCount()).thenReturn(102);
		when(survivor.getHealthRatio()).thenReturn(2);
		NPC fresh = rangedSmall(43, 3298);
		when(client.getNpcs()).thenReturn(java.util.Arrays.asList(survivor, fresh));
		plugin.onNpcSpawned(new NpcSpawned(fresh));
		plugin.onGameTick(new GameTick());
		verify(client, times(2)).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylocas Toxobolos");
		verify(client, never()).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 43, -1, "Attack", "Nylocas Toxobolos");
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	private NPC rangedSmall(int index, int x)
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(8343);
		when(npc.getIndex()).thenReturn(index);
		when(npc.getName()).thenReturn("Nylocas Toxobolos");
		when(npc.getHealthScale()).thenReturn(8);
		when(npc.getHealthRatio()).thenReturn(8);
		WorldPoint point = new WorldPoint(x, 4248, 0);
		when(npc.getWorldLocation()).thenReturn(point);
		when(npc.getWorldArea()).thenReturn(point.toWorldArea());
		NPCComposition composition = mock(NPCComposition.class);
		when(npc.getTransformedComposition()).thenReturn(composition);
		when(composition.getActions()).thenReturn(new String[]{null, "Attack"});
		return npc;
	}


	@Test
	public void acknowledgedAttacksResumeOnEveryWeaponCooldownAfterRetry() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::startUp);
		Player player = client.getLocalPlayer();
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3295, 4248, 0));
		WorldArea area = mock(WorldArea.class);
		when(player.getWorldArea()).thenReturn(area);
		when(area.hasLineOfSightTo(any(WorldView.class), any(WorldArea.class))).thenReturn(true);
		when(client.getItemContainer(InventoryID.EQUIPMENT).contains(anyInt())).thenReturn(true);
		when(client.getItemContainer(InventoryID.EQUIPMENT).getItem(3)).thenReturn(new Item(12926, 1));
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(2);
		NPC target = rangedSmall(42, 3292);
		when(client.getNpcs()).thenReturn(java.util.Collections.singletonList(target));
		when(client.getTickCount()).thenReturn(100);
		plugin.onNpcSpawned(new NpcSpawned(target));
		for (int tick = 100; tick <= 106; tick++)
		{
			when(client.getTickCount()).thenReturn(tick);
			// First request fails; later launches are observed one tick after each request.
			boolean launched = tick >= 102 && (tick - 102) % 2 == 0;
			if (tick >= 103 && (tick - 103) % 2 == 0)
			{
				HitsplatApplied hit = new HitsplatApplied();
				hit.setActor(target);
				hit.setHitsplat(new Hitsplat(HitsplatID.BLOCK_ME, 0, 0));
				plugin.onHitsplatApplied(hit);
			}
			when(player.getInteracting()).thenReturn(launched ? target : null);
			when(player.getAnimation()).thenReturn(launched ? 5061 : -1);
			plugin.onGameTick(new GameTick());
			int requests = tick == 100 ? 1 : 2 + (tick - 101) / 2;
			verify(client, times(requests)).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylocas Toxobolos");
		}
		when(player.getInteracting()).thenReturn(null);
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test
	public void unlaunchedAttackIsRetriedOnEveryTick() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::startUp);
		Player player = client.getLocalPlayer();
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3295, 4248, 0));
		WorldArea area = mock(WorldArea.class);
		when(player.getWorldArea()).thenReturn(area);
		when(area.hasLineOfSightTo(any(WorldView.class), any(WorldArea.class))).thenReturn(true);
		when(client.getItemContainer(InventoryID.EQUIPMENT).contains(anyInt())).thenReturn(true);
		when(client.getItemContainer(InventoryID.EQUIPMENT).getItem(3)).thenReturn(new Item(12926, 1));
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(1);
		NPC target = rangedSmall(42, 3292);
		when(client.getNpcs()).thenReturn(java.util.Collections.singletonList(target));
		when(client.getTickCount()).thenReturn(100);
		plugin.onNpcSpawned(new NpcSpawned(target));
		// The server can set the interaction before an attack animation arrives.
		when(player.getInteracting()).thenReturn(target);
		for (int tick = 100; tick <= 102; tick++)
		{
			when(client.getTickCount()).thenReturn(tick);
			plugin.onGameTick(new GameTick());
			verify(client, times(tick - 99)).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylocas Toxobolos");
		}
		verify(client, never()).getWidget(net.runelite.api.widgets.WidgetInfo.FIXED_VIEWPORT_MINIMAP_DRAW_AREA);
		when(player.getInteracting()).thenReturn(null);
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test
	public void dyingUnacknowledgedTargetDoesNotDelayNextAttack() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::startUp);
		Player player = client.getLocalPlayer();
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3295, 4248, 0));
		WorldArea area = mock(WorldArea.class);
		when(player.getWorldArea()).thenReturn(area);
		when(area.hasLineOfSightTo(any(WorldView.class), any(WorldArea.class))).thenReturn(true);
		when(client.getItemContainer(InventoryID.EQUIPMENT).contains(anyInt())).thenReturn(true);
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(1);
		NPC target = rangedSmall(42, 3292), replacement = rangedSmall(43, 3298);
		when(client.getNpcs()).thenReturn(java.util.Arrays.asList(target, replacement));
		when(client.getTickCount()).thenReturn(100);
		plugin.onNpcSpawned(new NpcSpawned(target));
		plugin.onNpcSpawned(new NpcSpawned(replacement));
		plugin.onGameTick(new GameTick());
		ArgumentCaptor<Integer> attacked = ArgumentCaptor.forClass(Integer.class);
		verify(client).menuAction(eq(0), eq(0), eq(MenuAction.NPC_SECOND_OPTION), attacked.capture(), eq(-1), eq("Attack"), anyString());
		NPC first = attacked.getValue() == 42 ? target : replacement;
		when(first.getHealthRatio()).thenReturn(0);
		when(client.getTickCount()).thenReturn(101);
		plugin.onGameTick(new GameTick());
		verify(client).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, attacked.getValue() == 42 ? 43 : 42, -1, "Attack", "Nylocas Toxobolos");
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test
	public void wave20DropsPendingPreCapBigAndStartsItsOwnFlickerPacket() throws Exception
	{
		ArgumentCaptor<NavigationButton> navigation = ArgumentCaptor.forClass(NavigationButton.class);
		SwingUtilities.invokeAndWait(plugin::startUp);
		verify(toolbar).addNavigation(navigation.capture());
		Player player = client.getLocalPlayer();
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3293, 4248, 0));
		WorldArea area = mock(WorldArea.class);
		when(player.getWorldArea()).thenReturn(area);
		when(area.hasLineOfSightTo(any(WorldView.class), any(WorldArea.class))).thenReturn(true);
		when(client.getItemContainer(InventoryID.EQUIPMENT).contains(anyInt())).thenReturn(true);
		when(client.getTickCount()).thenReturn(100);
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(18);
		NPC old = rangedSmall(42, 3281);
		when(old.getId()).thenReturn(8346);
		when(client.getNpcs()).thenReturn(java.util.Collections.singletonList(old));
		plugin.onNpcSpawned(new NpcSpawned(old));
		plugin.onGameTick(new GameTick()); // In its lane: cannot prefire, even with no smalls.
		verify(client, never()).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylocas Toxobolos");
		when(client.getTickCount()).thenReturn(110);
		when(old.getWorldLocation()).thenReturn(new WorldPoint(3290, 4248, 0));
		when(old.getWorldArea()).thenReturn(new WorldArea(3290, 4248, 2, 2, 0));
		plugin.onGameTick(new GameTick()); // Empty-small-room west exception; request stays unacknowledged.
		verify(client).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylocas Toxobolos");
		when(client.getTickCount()).thenReturn(111);
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(20);
		NPC current = rangedSmall(43, 3281);
		when(current.getId()).thenReturn(8345);
		when(client.getNpcs()).thenReturn(java.util.Arrays.asList(old, current));
		plugin.onNpcSpawned(new NpcSpawned(current));
		plugin.onGameTick(new GameTick());
		SwingUtilities.invokeAndWait(() ->
		{
			String status = find(navigation.getValue().getPanel(), JTextArea.class, null).getText();
			assertTrue(status.contains("Wave 20"));
			assertFalse(status.contains("Waiting for attack acknowledgement"));
		});
		when(client.getTickCount()).thenReturn(116);
		when(current.getId()).thenReturn(8346);
		when(current.getWorldLocation()).thenReturn(new WorldPoint(3286, 4248, 0));
		when(current.getWorldArea()).thenReturn(new WorldArea(3286, 4248, 2, 2, 0));
		plugin.onGameTick(new GameTick());
		verify(client, times(1)).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylocas Toxobolos");
		verify(client).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 43, -1, "Attack", "Nylocas Toxobolos");
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test
	public void bigPrefireUsesServerSouthwestAnchorInsteadOfModelCenter() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::startUp);
		Player player = client.getLocalPlayer();
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3293, 4248, 0));
		when(player.getLocalLocation()).thenReturn(new LocalPoint(13 * 128 + 64, 24 * 128 + 64));
		WorldArea area = mock(WorldArea.class);
		when(player.getWorldArea()).thenReturn(area);
		when(area.hasLineOfSightTo(any(WorldView.class), any(WorldArea.class))).thenReturn(true);
		when(client.getTickCount()).thenReturn(100);
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(20);
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(8345);
		when(npc.getIndex()).thenReturn(77);
		when(npc.getName()).thenReturn("Nylocas Ischyros");
		when(npc.getHealthRatio()).thenReturn(16);
		when(npc.getWorldLocation()).thenReturn(new WorldPoint(3281, 4248, 0));
		// Its center differs from the southwest birth anchor and is not a valid lane spawn.
		when(npc.getLocalLocation()).thenReturn(new LocalPoint(2 * 128 + 64, 25 * 128 + 64));
		NPCComposition composition = mock(NPCComposition.class);
		when(npc.getTransformedComposition()).thenReturn(composition);
		when(composition.getActions()).thenReturn(new String[]{null, "Attack"});
		when(client.getNpcs()).thenReturn(java.util.Collections.singletonList(npc));
		plugin.onNpcSpawned(new NpcSpawned(npc));
		plugin.onGameTick(new GameTick());
		when(client.getTickCount()).thenReturn(105);
		when(npc.getId()).thenReturn(8346);
		when(npc.getName()).thenReturn("Nylocas Toxobolos");
		when(npc.getWorldLocation()).thenReturn(new WorldPoint(3286, 4248, 0));
		when(npc.getWorldArea()).thenReturn(new WorldArea(3286, 4248, 2, 2, 0));
		plugin.onGameTick(new GameTick());
		verify(client).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 77, -1, "Attack", "Nylocas Toxobolos");
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test
	public void restartingAfterRespawnRechecksHitpointsInsteadOfCachedDeathFlags() throws Exception
	{
		ArgumentCaptor<NavigationButton> navigation = ArgumentCaptor.forClass(NavigationButton.class);
		SwingUtilities.invokeAndWait(plugin::startUp);
		verify(toolbar).addNavigation(navigation.capture());
		plugin.onGameTick(new GameTick());
		when(client.getTickCount()).thenReturn(1);
		when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(0);
		when(client.getLocalPlayer().isDead()).thenReturn(true);
		when(client.getLocalPlayer().getHealthRatio()).thenReturn(0);
		plugin.onGameTick(new GameTick());
		// The same player object and health bar can retain death state after respawn.
		when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(99);
		SwingUtilities.invokeAndWait(() -> find(navigation.getValue().getPanel(), JButton.class, "Start ranger").doClick());
		when(client.getTickCount()).thenReturn(2);
		plugin.onGameTick(new GameTick());
		verify(client, times(2)).menuAction(15, 31, MenuAction.GAME_OBJECT_FIRST_OPTION, 32755, -1, "Pass-through", "Nylocas barrier");
		verify(client, times(2)).runScript(ScriptID.CAMERA_DO_ZOOM, 128, 128);
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test
	public void startChecksCurrentDeathImmediatelyAndCannotArmWithZeroHitpoints() throws Exception
	{
		ArgumentCaptor<NavigationButton> navigation = ArgumentCaptor.forClass(NavigationButton.class);
		SwingUtilities.invokeAndWait(plugin::startUp);
		verify(toolbar).addNavigation(navigation.capture());
		when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(0);
		SwingUtilities.invokeAndWait(() -> find(navigation.getValue().getPanel(), JButton.class, "Start ranger").doClick());
		SwingUtilities.invokeAndWait(() -> assertEquals("Stopped: player died.",
			find(navigation.getValue().getPanel(), JTextArea.class, null).getText()));
		// Healing alone must not silently restart an attempt rejected while dead.
		when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(99);
		when(client.getTickCount()).thenReturn(1);
		plugin.onGameTick(new GameTick());
		verify(client, never()).menuAction(15, 31, MenuAction.GAME_OBJECT_FIRST_OPTION, 32755, -1, "Pass-through", "Nylocas barrier");
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test
	public void deathDuringAnAttemptUsesCurrentHitpointsEvenWithNoDeadFlagOrHealthBar() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::startUp);
		plugin.onGameTick(new GameTick());
		when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(0);
		when(client.getTickCount()).thenReturn(1);
		plugin.onGameTick(new GameTick());
		verify(client, times(1)).menuAction(15, 31, MenuAction.GAME_OBJECT_FIRST_OPTION, 32755, -1, "Pass-through", "Nylocas barrier");
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test
	public void lowPrayerAndBigAttackerAreHandledWithoutReplacingOpeningPrefire() throws Exception
	{
		when(config.prayers()).thenReturn(true);
		when(config.protectionPrayers()).thenReturn(true);
		when(config.restorePrayer()).thenReturn(true);
		when(config.prayerThreshold()).thenReturn(20);
		when(client.getRealSkillLevel(Skill.PRAYER)).thenReturn(99);
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(20);
		when(client.getItemContainer(InventoryID.INVENTORY).getItems()).thenReturn(new Item[]{new Item(143, 1)});
		ItemComposition dose = mock(ItemComposition.class);
		when(dose.getInventoryActions()).thenReturn(new String[]{"Drink"});
		when(client.getItemDefinition(143)).thenReturn(dose);
		Widget prayers = mock(Widget.class);
		Widget magic = prayerWidget(2000, "Protect from Magic");
		Widget rigour = prayerWidget(2001, "Rigour");
		when(prayers.getStaticChildren()).thenReturn(new Widget[]{magic, rigour});
		when(client.getWidget(InterfaceID.Prayerbook.CONTAINER)).thenReturn(prayers);
		SwingUtilities.invokeAndWait(plugin::startUp);
		Player player = client.getLocalPlayer();
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3292, 4249, 0));
		WorldArea area = mock(WorldArea.class);
		when(player.getWorldArea()).thenReturn(area);
		when(area.hasLineOfSightTo(any(WorldView.class), any(WorldArea.class))).thenReturn(true);
		when(client.getTickCount()).thenReturn(4);
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(1);
		NPC ranged = mock(NPC.class);
		when(ranged.getId()).thenReturn(8343);
		when(ranged.getIndex()).thenReturn(42);
		when(ranged.getName()).thenReturn("Nylocas Toxobolos");
		when(ranged.getHealthRatio()).thenReturn(8);
		when(ranged.getWorldLocation()).thenReturn(new WorldPoint(3281, 4248, 0));
		when(ranged.getWorldArea()).thenReturn(new WorldPoint(3281, 4248, 0).toWorldArea());
		NPCComposition composition = mock(NPCComposition.class);
		when(ranged.getTransformedComposition()).thenReturn(composition);
		when(composition.getActions()).thenReturn(new String[]{null, "Attack"});
		NPC big = mock(NPC.class);
		when(big.getId()).thenReturn(8353);
		when(big.getHealthRatio()).thenReturn(16);
		when(big.getInteracting()).thenReturn(player);
		when(big.getWorldLocation()).thenReturn(new WorldPoint(3293, 4250, 0));
		WorldArea threat = mock(WorldArea.class);
		when(big.getWorldArea()).thenReturn(threat);
		when(threat.distanceTo(area)).thenReturn(2);
		when(threat.hasLineOfSightTo(client.getTopLevelWorldView(), area)).thenReturn(true);
		when(client.getNpcs()).thenReturn(java.util.Arrays.asList(ranged, big));
		plugin.onNpcSpawned(new NpcSpawned(ranged));
		plugin.onNpcSpawned(new NpcSpawned(big));
		plugin.onGameTick(new GameTick());
		InOrder order = inOrder(client);
		order.verify(client).menuAction(0, InterfaceID.Inventory.ITEMS, MenuAction.ITEM_FIRST_OPTION, 143, 143, "Drink", "");
		order.verify(client).menuAction(-1, 2000, MenuAction.CC_OP, 1, -1, "Activate", "");
		order.verify(client).menuAction(-1, 2001, MenuAction.CC_OP, 1, -1, "Activate", "");
		order.verify(client).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylocas Toxobolos");
		verify(client, times(1)).menuAction(-1, 2001, MenuAction.CC_OP, 1, -1, "Activate", "");
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	private Widget prayerWidget(int id, String name)
	{
		Widget widget = mock(Widget.class);
		when(widget.getId()).thenReturn(id);
		when(widget.getIndex()).thenReturn(-1);
		when(widget.isIf3()).thenReturn(true);
		when(widget.getName()).thenReturn(name);
		when(widget.getActions()).thenReturn(new String[]{"Activate"});
		return widget;
	}

	private static <T extends Component> T find(Container parent, Class<T> type, String text)
	{
		for (Component child : parent.getComponents())
		{
			if (type.isInstance(child) && (text == null || child instanceof JButton && text.equals(((JButton) child).getText())))
				return type.cast(child);
			if (child instanceof Container)
			{
				T found = find((Container) child, type, text);
				if (found != null) return found;
			}
		}
		return null;
	}
}
