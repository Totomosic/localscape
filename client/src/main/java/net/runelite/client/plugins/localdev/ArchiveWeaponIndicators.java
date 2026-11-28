package net.runelite.client.plugins.localdev;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;

/** Adapts local weapons and bot flights to the Hub plugin's native XP and party handling. */
@Slf4j
@PluginDescriptor(name = "Local Archive Death Indicators", enabledByDefault = true, hidden = true)
public class ArchiveWeaponIndicators extends Plugin
{
	private static final String INDICATOR_CLASS = "com.infernostats.NyloDeathIndicatorsPlugin";
	private static final Set<Integer> TOMES = Set.of(34725, 34727);
	private static final Set<Integer> OBLIGATORS = Set.of(34715, 34717);

	@Inject
	private Client client;
	@Inject
	private PluginManager pluginManager;

	private Object indicator;
	private final NylocasBotDamage botDamage = new NylocasBotDamage();
	private Constructor<?> damageConstructor;
	private Method receiveDamage;
	private Method setDamageMember;
	private Set<Integer> meleeWeapons;
	private Set<Integer> multikillWeapons;
	private final List<Runnable> restoreNativeIds = new ArrayList<>();

	@Override
	protected void startUp()
	{
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (pluginManager.isPluginEnabled(plugin))
			{
				attach(plugin);
			}
		}
	}

	@Override
	protected void shutDown()
	{
		detach();
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		if (!INDICATOR_CLASS.equals(event.getPlugin().getClass().getName()))
		{
			return;
		}
		if (event.isLoaded())
		{
			attach(event.getPlugin());
		}
		else
		{
			detach();
		}
	}

	void attach(Object plugin)
	{
		Class<?> type = plugin.getClass();
		if (!INDICATOR_CLASS.equals(type.getName()) || plugin == indicator)
		{
			return;
		}
		detach();
		try
		{
			Set<Integer> staves = weaponList(type, "POWERED_STAVES");
			meleeWeapons = weaponList(type, "NYLO_MELEE_WEAPONS");
			multikillWeapons = weaponList(type, "MULTIKILL_MELEE_WEAPONS");
			remember(staves, TOMES);
			remember(meleeWeapons, OBLIGATORS);
			remember(multikillWeapons, OBLIGATORS);
			staves.addAll(TOMES);
			Class<?> damageType = type.getClassLoader().loadClass("com.infernostats.NpcDamaged");
			damageConstructor = damageType.getConstructor(int.class, int.class);
			receiveDamage = type.getDeclaredMethod("onNpcDamaged", damageType);
			receiveDamage.setAccessible(true);
			setDamageMember = damageType.getMethod("setMemberId", long.class);
			indicator = plugin;
			updateStyle();
		}
		catch (ReflectiveOperationException | RuntimeException ex)
		{
			detach();
			log.warn("Unable to register local weapons with Nylo Death Indicators", ex);
		}
	}

	@SuppressWarnings("unchecked")
	private static Set<Integer> weaponList(Class<?> type, String name) throws ReflectiveOperationException
	{
		Field field = type.getDeclaredField(name);
		field.setAccessible(true);
		return (Set<Integer>) field.get(null);
	}

	private void remember(Set<Integer> list, Set<Integer> weapons)
	{
		for (int weapon : weapons)
		{
			boolean present = list.contains(weapon);
			restoreNativeIds.add(() -> { if (present) { list.add(weapon); } else { list.remove(weapon); } });
		}
	}

	private void detach()
	{
		restoreNativeIds.forEach(Runnable::run);
		restoreNativeIds.clear();
		indicator = null;
		damageConstructor = null;
		receiveDamage = null;
		setDamageMember = null;
		botDamage.reset();
		meleeWeapons = null;
		multikillWeapons = null;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (indicator != null && event.getType() == ChatMessageType.CONSOLE)
		{
			botDamage.receive(event.getMessage());
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (indicator != null)
		{
			botDamage.apply(client, this::receiveBotDamage);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		botDamage.despawn(event.getNpc());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// Discard queued packets and NPC references across logout, hopping and scene rebuilds.
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			botDamage.reset();
		}
	}

	private void receiveBotDamage(int index, int damage)
	{
		if (indicator == null)
		{
			return;
		}
		try
		{
			Object message = damageConstructor.newInstance(index, damage);
			// This is a local bot notification, never the local RuneLite party member's echo.
			setDamageMember.invoke(message, Long.MIN_VALUE);
			receiveDamage.invoke(indicator, message);
		}
		catch (ReflectiveOperationException | RuntimeException ex)
		{
			log.warn("Unable to deliver bot damage to Nylo Death Indicators", ex);
			detach();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarpId() == VarPlayer.ATTACK_STYLE)
		{
			updateStyle();
		}
	}

	private void updateStyle()
	{
		if (indicator == null)
		{
			return;
		}
		// Only Smash (the fourth stance, three ticks) has a single-target XP drop.
		if (client.getVarpValue(VarPlayer.ATTACK_STYLE) == 3)
		{
			meleeWeapons.addAll(OBLIGATORS);
			multikillWeapons.removeAll(OBLIGATORS);
		}
		else
		{
			meleeWeapons.removeAll(OBLIGATORS);
			multikillWeapons.addAll(OBLIGATORS);
		}
	}
}
