package net.runelite.client.plugins.nylomage;

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

public class NylocasMagePluginTest
{
	@Mock private Client client;
	@Mock private ClientThread clientThread;
	@Mock private ClientToolbar toolbar;
	@Mock private ConfigManager manager;
	@Mock private MageConfig config;
	@InjectMocks private NylocasMagePlugin plugin;

	@Before
	public void setup()
	{
		MockitoAnnotations.initMocks(this);
		when(config.shadowOpening()).thenReturn(true);
		when(config.scytheId()).thenReturn(22325);
		when(config.dinhId()).thenReturn(21015);
		when(config.barrageStaffId()).thenReturn(27624);
		when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(1);
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
		when(equipment.contains(27275)).thenReturn(true);
		when(equipment.getItem(3)).thenReturn(new Item(27275, 1));
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
		when(stance.getId()).thenReturn(InterfaceID.CombatInterface._3);
		when(stance.isIf3()).thenReturn(true);
		when(client.getWidget(InterfaceID.CombatInterface._3)).thenReturn(stance);
		when(client.getVarpValue(43)).thenReturn(0);
		java.util.concurrent.atomic.AtomicBoolean dialogueActive = new java.util.concurrent.atomic.AtomicBoolean(true);
		java.util.concurrent.atomic.AtomicBoolean roomStarted = new java.util.concurrent.atomic.AtomicBoolean();
		// Server combat-tab clicks close the modal and cancel its suspended entry script.
		doAnswer(call ->
		{
			MenuAction action = call.getArgument(2);
			int widget = call.getArgument(1);
			if (action == MenuAction.CC_OP && widget == InterfaceID.CombatInterface._3) dialogueActive.set(false);
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
		verify(client, never()).getWidget(InterfaceID.CombatInterface._3);
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
		SwingUtilities.invokeAndWait(find(navigation.getValue().getPanel(), JButton.class, "Stop mage")::doClick);
		SwingUtilities.invokeAndWait(find(navigation.getValue().getPanel(), JButton.class, "Start mage")::doClick);
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

	@Test public void realBirthThenShadowOpeningAndBossStopsController() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::startUp);
		Player player = client.getLocalPlayer();
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3296, 4244, 0));
		WorldArea area = mock(WorldArea.class); when(player.getWorldArea()).thenReturn(area);
		when(area.hasLineOfSightTo(any(WorldView.class), any(WorldArea.class))).thenReturn(true);
		NPC small = small(42, 8344, 3296, 4233);
		when(client.getNpcs()).thenReturn(java.util.Collections.singletonList(small));
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(1);
		when(client.getTickCount()).thenReturn(4); plugin.onNpcSpawned(new NpcSpawned(small)); plugin.onGameTick(new GameTick());
		when(client.getTickCount()).thenReturn(5); plugin.onGameTick(new GameTick());
		verify(client).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylo");
		NPC boss = mock(NPC.class); when(boss.getId()).thenReturn(8354);
		when(client.getNpcs()).thenReturn(java.util.Collections.singletonList(boss));
		when(client.getTickCount()).thenReturn(6); plugin.onGameTick(new GameTick());
		when(client.getTickCount()).thenReturn(7); plugin.onGameTick(new GameTick());
		verify(client, times(1)).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylo");
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test public void nativeApproachKeepsRequestingAndDoesNotAcknowledgeAStaleAnimation() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::startUp);
		Player player = client.getLocalPlayer();
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3295, 4248, 0));
		WorldArea area = mock(WorldArea.class); when(player.getWorldArea()).thenReturn(area);
		when(area.hasLineOfSightTo(any(WorldView.class), any(WorldArea.class))).thenReturn(true);
		when(client.getItemContainer(InventoryID.EQUIPMENT).contains(anyInt())).thenReturn(true);
		when(client.getItemContainer(InventoryID.EQUIPMENT).getItem(3)).thenReturn(new Item(29084, 1));
		NPC target = small(42, 8342, 3301, 4252);
		when(client.getNpcs()).thenReturn(java.util.Collections.singletonList(target));
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(3);
		when(client.getTickCount()).thenReturn(100);
		plugin.onNpcSpawned(new NpcSpawned(target)); plugin.onGameTick(new GameTick());
		verify(client).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylo");
		// A previous sulphur animation while still outside reach is not a confirmed launch.
		when(player.getInteracting()).thenReturn(target);
		when(player.getAnimation()).thenReturn(2068);
		when(client.getTickCount()).thenReturn(101); plugin.onGameTick(new GameTick());
		verify(client, times(2)).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylo");
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test public void doesNotRetryQueuedApproachAfterFourWaveBoundary() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::startUp);
		Player player = client.getLocalPlayer(); when(player.getWorldLocation()).thenReturn(new WorldPoint(3295, 4248, 0));
		WorldArea area = mock(WorldArea.class); when(player.getWorldArea()).thenReturn(area);
		when(area.hasLineOfSightTo(any(WorldView.class), any(WorldArea.class))).thenReturn(true);
		when(client.getItemContainer(InventoryID.EQUIPMENT).contains(anyInt())).thenReturn(true);
		NPC old = small(42, 8342, 3301, 4252);
		when(client.getNpcs()).thenReturn(java.util.Collections.singletonList(old));
		when(client.getTickCount()).thenReturn(100);
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(18);
		plugin.onNpcSpawned(new NpcSpawned(old)); plugin.onGameTick(new GameTick());
		verify(client).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylo");
		when(client.getTickCount()).thenReturn(101);
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(23);
		when(player.getInteracting()).thenReturn(old);
		plugin.onGameTick(new GameTick());
		verify(client, times(1)).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylo");
		java.lang.reflect.Field pending = NylocasMagePlugin.class.getDeclaredField("pending");
		pending.setAccessible(true);
		assertNull(pending.get(plugin));
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test public void onlyTheFinalWaveDiscardsWave29AndAllowsAFreshChild() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::startUp);
		Player player = client.getLocalPlayer(); when(player.getWorldLocation()).thenReturn(new WorldPoint(3295, 4248, 0));
		WorldArea area = mock(WorldArea.class); when(player.getWorldArea()).thenReturn(area);
		when(area.hasLineOfSightTo(any(WorldView.class), any(WorldArea.class))).thenReturn(true);
		when(client.getItemContainer(InventoryID.EQUIPMENT).contains(anyInt())).thenReturn(true);
		NPC old = small(42, 8344, 3295, 4247);
		when(client.getNpcs()).thenReturn(java.util.Collections.singletonList(old));
		when(client.getTickCount()).thenReturn(228);
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(29);
		plugin.onNpcSpawned(new NpcSpawned(old)); plugin.onGameTick(new GameTick());
		verify(client).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylo");
		when(client.getTickCount()).thenReturn(229);
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(30);
		plugin.onGameTick(new GameTick());
		NPC child = small(43, 8344, 3295, 4249);
		when(client.getTickCount()).thenReturn(232);
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(31);
		when(client.getNpcs()).thenReturn(java.util.Arrays.asList(old, child));
		plugin.onNpcSpawned(new NpcSpawned(child)); plugin.onGameTick(new GameTick());
		verify(client).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 43, -1, "Attack", "Nylo");
		verify(client, times(2)).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylo");
		SwingUtilities.invokeAndWait(plugin::shutDown);
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
		when(client.getItemContainer(InventoryID.EQUIPMENT).getItem(3)).thenReturn(new Item(31113, 1));
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(2);
		NPC target = small(42, 8344, 3295, 4247);
		when(client.getNpcs()).thenReturn(java.util.Collections.singletonList(target));
		when(client.getTickCount()).thenReturn(100);
		plugin.onNpcSpawned(new NpcSpawned(target));
		for (int tick = 100; tick <= 108; tick++)
		{
			when(client.getTickCount()).thenReturn(tick);
			// First request fails; later launches are observed one tick after each request.
			boolean launched = tick >= 102 && (tick - 102) % 3 == 0;
			if (tick >= 103 && (tick - 103) % 3 == 0)
			{
				HitsplatApplied hit = new HitsplatApplied();
				hit.setActor(target);
				hit.setHitsplat(new Hitsplat(HitsplatID.BLOCK_ME, 0, 0));
				plugin.onHitsplatApplied(hit);
			}
			when(player.getInteracting()).thenReturn(launched ? target : null);
			when(player.getAnimation()).thenReturn(launched ? 12397 : -1);
			plugin.onGameTick(new GameTick());
			int requests = tick == 100 ? 1 : 2 + (tick - 101) / 3;
			verify(client, times(requests)).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylo");
		}
		when(player.getInteracting()).thenReturn(null);
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test public void unlaunchedAttackIsRetriedOnEveryTick() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::startUp);
		Player player = client.getLocalPlayer();
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3295, 4248, 0));
		WorldArea area = mock(WorldArea.class); when(player.getWorldArea()).thenReturn(area);
		when(area.hasLineOfSightTo(any(WorldView.class), any(WorldArea.class))).thenReturn(true);
		when(client.getItemContainer(InventoryID.EQUIPMENT).contains(anyInt())).thenReturn(true);
		when(client.getItemContainer(InventoryID.EQUIPMENT).getItem(3)).thenReturn(new Item(31113, 1));
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(2);
		NPC target = small(42, 8344, 3295, 4247);
		when(client.getNpcs()).thenReturn(java.util.Collections.singletonList(target));
		when(client.getTickCount()).thenReturn(100); plugin.onNpcSpawned(new NpcSpawned(target));
		// The server can set the interaction before an attack animation arrives.
		when(player.getInteracting()).thenReturn(target);
		for (int tick = 100; tick <= 102; tick++)
		{
			when(client.getTickCount()).thenReturn(tick); plugin.onGameTick(new GameTick());
			verify(client, times(tick - 99)).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, 42, -1, "Attack", "Nylo");
		}
		verify(client, never()).getWidget(net.runelite.api.widgets.WidgetInfo.FIXED_VIEWPORT_MINIMAP_DRAW_AREA);
		when(player.getInteracting()).thenReturn(null);
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test public void dyingUnacknowledgedTargetDoesNotDelayNextAttack() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::startUp);
		Player player = client.getLocalPlayer();
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3295, 4248, 0));
		WorldArea area = mock(WorldArea.class); when(player.getWorldArea()).thenReturn(area);
		when(area.hasLineOfSightTo(any(WorldView.class), any(WorldArea.class))).thenReturn(true);
		when(client.getItemContainer(InventoryID.EQUIPMENT).contains(anyInt())).thenReturn(true);
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(2);
		NPC target = small(42, 8344, 3295, 4247), replacement = small(43, 8344, 3295, 4249);
		when(client.getNpcs()).thenReturn(java.util.Arrays.asList(target, replacement));
		when(client.getTickCount()).thenReturn(100);
		plugin.onNpcSpawned(new NpcSpawned(target)); plugin.onNpcSpawned(new NpcSpawned(replacement));
		plugin.onGameTick(new GameTick());
		ArgumentCaptor<Integer> attacked = ArgumentCaptor.forClass(Integer.class);
		verify(client).menuAction(eq(0), eq(0), eq(MenuAction.NPC_SECOND_OPTION), attacked.capture(), eq(-1), eq("Attack"), anyString());
		NPC first = attacked.getValue() == 42 ? target : replacement;
		when(first.getHealthRatio()).thenReturn(0);
		when(client.getTickCount()).thenReturn(101); plugin.onGameTick(new GameTick());
		verify(client).menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, attacked.getValue() == 42 ? 43 : 42, -1, "Attack", "Nylo");
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test public void zeroHitpointsPreventsArming() throws Exception
	{
		when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(0);
		SwingUtilities.invokeAndWait(plugin::startUp); plugin.onGameTick(new GameTick());
		verify(client, never()).menuAction(anyInt(), anyInt(), eq(MenuAction.NPC_SECOND_OPTION), anyInt(), anyInt(), anyString(), anyString());
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test public void refusesStartingHalfwayThroughExistingWaves() throws Exception
	{
		when(client.getLocalPlayer().getWorldLocation()).thenReturn(new WorldPoint(3295, 4248, 0));
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)).thenReturn(12);
		SwingUtilities.invokeAndWait(plugin::startUp); plugin.onGameTick(new GameTick());
		verify(client, never()).menuAction(anyInt(), anyInt(), eq(MenuAction.NPC_SECOND_OPTION), anyInt(), anyInt(), anyString(), anyString());
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	private NPC small(int index, int id, int x, int y)
	{
		NPC npc = mock(NPC.class); when(npc.getIndex()).thenReturn(index); when(npc.getId()).thenReturn(id);
		when(npc.getName()).thenReturn("Nylo"); when(npc.getHealthRatio()).thenReturn(8); when(npc.getHealthScale()).thenReturn(8);
		WorldPoint point = new WorldPoint(x, y, 0); when(npc.getWorldLocation()).thenReturn(point); when(npc.getWorldArea()).thenReturn(point.toWorldArea());
		NPCComposition composition = mock(NPCComposition.class); when(npc.getTransformedComposition()).thenReturn(composition);
		when(composition.getActions()).thenReturn(new String[]{null, "Attack"}); return npc;
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
