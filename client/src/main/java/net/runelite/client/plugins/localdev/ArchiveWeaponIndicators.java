package net.runelite.client.plugins.localdev;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;

/** Extends the Hub plugin's weapon lists while retaining its native XP and party handling. */
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
		meleeWeapons = null;
		multikillWeapons = null;
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
