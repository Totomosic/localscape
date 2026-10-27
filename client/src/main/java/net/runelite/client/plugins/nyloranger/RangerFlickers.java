package net.runelite.client.plugins.nyloranger;

import net.runelite.client.plugins.nyloranger.RangerStrategy.Crab;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Lane;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Style;

/** Verified lane rotations at ages 0, 5, 7, 26; slots ES, EN, SW, SE, WS, WN. */
final class RangerFlickers
{
	private static final String[][] ROTATIONS = {
		{"AMRA", null, "MARA", null, null, "RARA"}, // 16
		{"AMAM", null, "AMAM", null, "AMAM", null},
		{"RARA", null, "RARA", null, "RARA", null},
		{"AMAM", null, "AMAM", null, "AMAM", null},
		{"MRMR", null, "ARMR", null, "MRMR", null},
		{"RMRA", "RMRM", "MRMR", "MAMR", "AMRA", "ARAM"},
		{"ARMR", null, "ARMA", "RAMA", "MRMR", null},
		{"ARMR", null, "RAMA", null, "RARM", "ARMR"},
		{"M", null, "A", null, "RAMA", null},
		{"AMAM", null, "R", null, "M", null},
		{"A", null, "MAMR", null, "A", null},
		{"AMAM", null, "MARA", null, "A", null},
		{"RAMR", "AMAM", "AMRA", "MRMA", "RMAM", "MAMR"},
		{"RMAM", "ARMA", "M", null, "RMRM", "MRAR"},
		{"A", null, "ARMA", "MRMA", "RMRM", null},
		{"RMRM", "ARAR", "AMRM", "MARM", "RMRA", "MRAR"}
	};

	static Style at(Crab crab, int age)
	{
		String rotation = rotation(crab);
		if (rotation == null) return crab.style;
		int phase = age >= 26 ? 3 : age >= 7 ? 2 : age >= 5 ? 1 : 0;
		char style = rotation.charAt(Math.min(phase, rotation.length() - 1));
		return style == 'M' ? Style.MELEE : style == 'R' ? Style.RANGE : Style.MAGIC;
	}

	static boolean passiveMelee(Crab crab, int tick)
	{
		String rotation = rotation(crab);
		return tick - crab.born >= 26 && crab.style == Style.MELEE
			&& rotation != null && rotation.length() == 4 && rotation.charAt(3) == 'M';
	}

	private static String rotation(Crab crab)
	{
		if (crab.wave < 16 || crab.wave > 31 || crab.lane == Lane.SPLIT) return null;
		int slot = crab.lane == Lane.EAST ? (crab.slot == 'N' ? 1 : 0)
			: crab.lane == Lane.SOUTH ? (crab.slot == 'E' ? 3 : 2) : (crab.slot == 'N' ? 5 : 4);
		return ROTATIONS[crab.wave - 16][slot];
	}

	static boolean matchesNext(Crab crab, Style style, int tick)
	{
		int age = tick - crab.born;
		// Keep the live style authoritative if a custom encounter differs from the fixture.
		return crab.style == style && (at(crab, age) != crab.style || at(crab, age + 1) == style);
	}
}
