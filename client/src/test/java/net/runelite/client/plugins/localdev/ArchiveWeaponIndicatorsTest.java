package net.runelite.client.plugins.localdev;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.List;
import net.runelite.api.Client;
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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assume.assumeTrue;
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
