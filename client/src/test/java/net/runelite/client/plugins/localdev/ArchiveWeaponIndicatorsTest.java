package net.runelite.client.plugins.localdev;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.ChatMessageType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.VarbitChanged;
import org.junit.After;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.PartyMember;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assume.assumeTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/** Exercises the installed Plugin Hub implementation instead of duplicating its XP logic. */
public class ArchiveWeaponIndicatorsTest
{
	private Client client;
	private Player player;
	private PlayerComposition equipment;
	private Object indicator;
	private Class<?> pluginClass;
	private NPC target;
	private ArchiveWeaponIndicators bridge;
	private IndexedObjectSet<NPC> npcs;

	@Before
	public void setup() throws Exception
	{
		String jarPath = System.getProperty("localscape.nyloPluginJar", "");
		assumeTrue("Pass the installed Nylo Death Indicators JAR", new File(jarPath).isFile());
		URLClassLoader loader = new URLClassLoader(new java.net.URL[]{new File(jarPath).toURI().toURL()}, getClass().getClassLoader());
		pluginClass = loader.loadClass("com.infernostats.NyloDeathIndicatorsPlugin");
		indicator = pluginClass.getConstructor().newInstance();
		client = mock(Client.class);
		player = mock(Player.class);
		equipment = mock(PlayerComposition.class);
		WorldView world = mock(WorldView.class);
		when(client.getTopLevelWorldView()).thenReturn(world);
		when(world.getMapRegions()).thenReturn(new int[]{13122});
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getPlayerComposition()).thenReturn(equipment);
		ClientThread thread = mock(ClientThread.class);
		doAnswer(call -> { ((Runnable) call.getArgument(0)).run(); return null; }).when(thread).invokeLater(any(Runnable.class));
		set("client", client);
		set("clientThread", thread);
		set("party", mock(PartyService.class));
		set("isInNyloRegion", true);
		target = mock(NPC.class);
		when(target.getIndex()).thenReturn(12);
		when(target.getId()).thenReturn(8343);
		when(target.getWorldLocation()).thenReturn(new WorldPoint(3296, 4240, 0));
		npcs = mock(IndexedObjectSet.class);
		when(world.npcs()).thenAnswer(call -> npcs);
		when(npcs.byIndex(12)).thenReturn(target);
		when(player.getInteracting()).thenReturn(target);
		Object nylo = loader.loadClass("com.infernostats.Nylocas").getConstructor(NPC.class, int.class, int.class).newInstance(target, 12, 8);
		Field field = pluginClass.getDeclaredField("nylos");
		field.setAccessible(true);
		((List<Object>) field.get(indicator)).add(nylo);
		bridge = new ArchiveWeaponIndicators();
		Field clientField = ArchiveWeaponIndicators.class.getDeclaredField("client");
		clientField.setAccessible(true);
		clientField.set(bridge, client);
		bridge.attach(indicator);
	}

	@After
	public void teardown()
	{
		if (bridge != null)
		{
			bridge.shutDown();
		}
	}

	@Test
	public void tomeXpPredictsDeathBeforeImpact() throws Exception
	{
		when(equipment.getEquipmentId(KitType.WEAPON)).thenReturn(34725);
		process(Skill.MAGIC, 16);
		verify(target).setDead(true);
	}

	@Test
	public void obligatorXpPredictsDeathBeforeImpact() throws Exception
	{
		style(3);
		when(equipment.getEquipmentId(KitType.WEAPON)).thenReturn(34715);
		process(Skill.STRENGTH, 32);
		verify(target).setDead(true);
	}

	@Test
	public void tomeLongrangeUsesDefenceXpOnce() throws Exception
	{
		when(equipment.getEquipmentId(KitType.WEAPON)).thenReturn(34727);
		style(3);
		process(Skill.MAGIC, 8);
		verify(target, never()).setDead(true);
		process(Skill.DEFENCE, 8);
		verify(target).setDead(true);
	}

	@Test
	public void switchingToThrustStopsSingleTargetPredictions() throws Exception
	{
		when(equipment.getEquipmentId(KitType.WEAPON)).thenReturn(34717);
		style(3);
		style(0);
		process(Skill.ATTACK, 32);
		verify(target, never()).setDead(true);
		style(3);
		process(Skill.STRENGTH, 32);
		verify(target).setDead(true);
	}

	@Test
	public void shutdownRestoresNativeWeaponLists() throws Exception
	{
		when(equipment.getEquipmentId(KitType.WEAPON)).thenReturn(34725);
		bridge.shutDown();
		process(Skill.MAGIC, 16);
		verify(target, never()).setDead(true);
	}

	@Test
	public void botProjectileHidesBeforeImpactUsingNativePartyDamage() throws Exception
	{
		botMessage("localscape:nylohit:1:1:1:12:8343:3296:4240:8");
		verify(target, never()).setDead(true);
		bridge.onGameTick(new GameTick());
		verify(target).setDead(true);
		Method draw = pluginClass.getDeclaredMethod("shouldDraw", net.runelite.api.Renderable.class, boolean.class);
		draw.setAccessible(true);
		assertFalse((boolean) draw.invoke(indicator, target, false));
	}

	@Test
	public void combinesBotFlightsWithoutCountingDuplicatePackets() throws Exception
	{
		String first = "localscape:nylohit:1:1:1:12:8343:3296:4240:4";
		botMessage(first);
		botMessage(first);
		bridge.onGameTick(new GameTick());
		verify(target, never()).setDead(true);
		botMessage("localscape:nylohit:1:2:1:12:8343:3296:4240:4");
		bridge.onGameTick(new GameTick());
		verify(target).setDead(true);
	}

	@Test
	public void ignoresOldBirthAfterDespawnAndNpcSlotReuse() throws Exception
	{
		botMessage("localscape:nylohit:1:1:1:12:8343:3296:4240:3");
		bridge.onGameTick(new GameTick());
		bridge.onNpcDespawned(new NpcDespawned(target));
		NPC replacement = mock(NPC.class);
		when(replacement.getId()).thenReturn(8343);
		when(replacement.getWorldLocation()).thenReturn(new WorldPoint(3296, 4240, 0));
		when(npcs.byIndex(12)).thenReturn(replacement);
		botMessage("localscape:nylohit:1:2:1:12:8343:3296:4240:8");
		bridge.onGameTick(new GameTick());
		verify(target, never()).setDead(true);
		verify(replacement, never()).setDead(true);
	}

	@Test
	public void ignoresOrdinaryChatMalformedPacketsAndWrongTargets() throws Exception
	{
		ChatMessage chat = new ChatMessage();
		chat.setType(ChatMessageType.GAMEMESSAGE);
		chat.setMessage("localscape:nylohit:1:1:1:12:8343:3296:4240:8");
		bridge.onChatMessage(chat);
		botMessage("localscape:nylohit:bad");
		botMessage("localscape:nylohit:1:1:1:12:8343:3296:4240:-8");
		botMessage("localscape:nylohit:1:2:1:12:8344:3296:4240:8");
		botMessage("localscape:nylohit:1:3:1:12:8343:3297:4240:8");
		bridge.onGameTick(new GameTick());
		verify(target, never()).setDead(true);
	}

	@Test
	public void botDamageWorksWhileTheHostIsInARuneLiteParty() throws Exception
	{
		PartyService party = mock(PartyService.class);
		PartyMember local = mock(PartyMember.class);
		when(local.getMemberId()).thenReturn(0L);
		when(party.getLocalMember()).thenReturn(local);
		set("party", party);
		botMessage("localscape:nylohit:1:1:1:12:8343:3296:4240:8");
		bridge.onGameTick(new GameTick());
		verify(target).setDead(true);
	}

	@Test
	public void logoutDiscardsDamageWaitingForNpcUpdates() throws Exception
	{
		botMessage("localscape:nylohit:1:1:1:12:8343:3296:4240:8");
		GameStateChanged logout = new GameStateChanged();
		logout.setGameState(GameState.LOGIN_SCREEN);
		bridge.onGameStateChanged(logout);
		bridge.onGameTick(new GameTick());
		verify(target, never()).setDead(true);
	}

	@Test
	public void newerSessionDiscardsPendingDamageAndRejectsOldSession() throws Exception
	{
		botMessage("localscape:nylohit:1:1:1:12:8343:3296:4240:8");
		botMessage("localscape:nylohit:2:1:1:12:8343:3296:4240:3");
		botMessage("localscape:nylohit:1:2:1:12:8343:3296:4240:8");
		bridge.onGameTick(new GameTick());
		verify(target, never()).setDead(true);
	}

	@Test
	public void landedBotDamageIsReconciledBeforePredictingTheNextFlight() throws Exception
	{
		botMessage("localscape:nylohit:1:1:1:12:8343:3296:4240:4");
		bridge.onGameTick(new GameTick());
		HitsplatApplied impact = new HitsplatApplied();
		impact.setActor(target);
		impact.setHitsplat(new Hitsplat(HitsplatID.DAMAGE_OTHER, 4, 0));
		Method hitsplat = pluginClass.getDeclaredMethod("onHitsplatApplied", HitsplatApplied.class);
		hitsplat.setAccessible(true);
		hitsplat.invoke(indicator, impact);
		botMessage("localscape:nylohit:1:2:1:12:8343:3296:4240:2");
		bridge.onGameTick(new GameTick());
		verify(target, never()).setDead(true);
		botMessage("localscape:nylohit:1:3:1:12:8343:3296:4240:2");
		bridge.onGameTick(new GameTick());
		verify(target).setDead(true);
	}

	private void botMessage(String message)
	{
		ChatMessage event = new ChatMessage();
		event.setType(ChatMessageType.CONSOLE);
		event.setMessage(message);
		bridge.onChatMessage(event);
	}

	private void style(int style)
	{
		when(client.getVarpValue(VarPlayer.ATTACK_STYLE)).thenReturn(style);
		VarbitChanged event = new VarbitChanged();
		event.setVarpId(VarPlayer.ATTACK_STYLE);
		bridge.onVarbitChanged(event);
	}

	private void process(Skill skill, int xp) throws Exception
	{
		Method method = pluginClass.getDeclaredMethod("processXpDrop", Skill.class, int.class);
		method.setAccessible(true);
		method.invoke(indicator, skill, xp);
	}

	private void set(String name, Object value) throws Exception
	{
		Field field = pluginClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(indicator, value);
	}
}
