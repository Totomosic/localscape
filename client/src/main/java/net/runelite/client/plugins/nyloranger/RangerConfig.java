package net.runelite.client.plugins.nyloranger;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(RangerConfig.GROUP)
public interface RangerConfig extends Config
{
	String GROUP = "nyloranger";

	@ConfigItem(keyName = "rangedGear", name = "Ranged equipment", description = "Comma-separated item IDs; use Capture ranged in the sidebar", position = 0)
	default String rangedGear() { return ""; }

	@ConfigItem(keyName = "magicGear", name = "Magic equipment", description = "Comma-separated item IDs; use Capture magic in the sidebar", position = 1)
	default String magicGear() { return ""; }

	@ConfigItem(keyName = "meleeGear", name = "Melee equipment", description = "Comma-separated item IDs; use Capture melee in the sidebar", position = 2)
	default String meleeGear() { return ""; }

	@ConfigItem(keyName = "prayers", name = "Offensive prayers", description = "Keep Rigour, Augury or Piety active; switch only for actual attacks", position = 3)
	default boolean prayers() { return true; }

	@ConfigItem(keyName = "protectionPrayers", name = "Protection prayers", description = "Protect against fighting nylocas attacking you, prioritising big attackers", position = 4)
	default boolean protectionPrayers() { return true; }

	@ConfigItem(keyName = "restorePrayer", name = "Drink prayer potions", description = "Restore low prayer using a carried prayer potion", position = 5)
	default boolean restorePrayer() { return true; }

	@Range(min = 1, max = 99)
	@ConfigItem(keyName = "prayerThreshold", name = "Restore prayer at", description = "Drink at or below this many prayer points, when a dose will fit", position = 6)
	default int prayerThreshold() { return 20; }
}
