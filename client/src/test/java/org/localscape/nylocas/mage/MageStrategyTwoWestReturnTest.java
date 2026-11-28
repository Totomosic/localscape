package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
import static org.localscape.nylocas.mage.MageStrategy.tile;

public class MageStrategyTwoWestReturnTest
{
	private final MageStrategy mage = new MageStrategy();
	private final Room room = new Room();
	private Crab east;
	private void witness(boolean confirmed)
	{
		// Native three-chin seed11280001: East24 launches206, then a south
		// Eye move207 precedes a native swap208 that pauses the west return.
		room.configureStrategy(2); room.wave = 24; room.spawns[23] = 192; room.spawns[24] = 200;
		room.player = tile(3301, 4248); room.ready = 211;
		east = add(136, 24, 200, Lane.EAST, true, Style.MELEE, 3302, 4248);
		east.pendingDamage = 57;
		add(128, 22, 180, Lane.SPLIT, false, Style.MAGIC, 3299, 4243).hp = 1;
		if (confirmed) mage.attacked(new Decision(east, Weapon.SCYTHE, room.player, false,
			"East melee big 24", "East melee big 24"), 207, 206);
	}
	private Crab add(int key, int wave, int born, Lane lane, boolean big, Style style, int x, int y)
	{
		Crab c = new Crab(key, key + 6, born, wave, lane, (char) 0, big, style, tile(x, y));
		c.pendingDamage = 0; room.crabs.add(c); return c;
	}
	@Test public void theFirstFreeCooldownStepGoesWestAfterKnownLethalEastDamage()
	{
		witness(true); Decision d = mage.decide(room, 207);
		assertEquals("Return west25 during the lethal east24 flight", d.reason);
		assertEquals(tile(3291, 4248), d.tile); assertNull(d.target);
	}
	@Test public void uncertainOrNonlethalEastDamageRetainsTheFollowUp()
	{
		witness(true); east.pendingDamage = 3;
		assertFalse(mage.decide(room, 207).reason.startsWith("Return west25"));
		east.pendingDamage = -1;
		assertFalse(mage.decide(room, 207).reason.startsWith("Return west25"));
	}
	@Test public void noConfirmedSwingCannotInventTheWestReturnWindow()
	{
		witness(false);
		assertFalse(mage.decide(room, 207).reason.startsWith("Return west25"));
	}
	@Test public void unfinishedSouthTwentyThreeKeepsItsService()
	{
		witness(true); add(132, 23, 192, Lane.SOUTH, true, Style.MELEE, 3295, 4243).hp = 4;
		assertFalse(mage.decide(room, 207).reason.startsWith("Return west25"));
	}
	@Test public void anImmediatelyUsefulMeleeSmallKeepsItsApproach()
	{
		witness(true); add(140, 24, 204, Lane.SPLIT, false, Style.MELEE, 3301, 4247);
		assertFalse(mage.decide(room, 207).reason.startsWith("Return west25"));
	}
	@Test public void laterCooldownTicksDoNotRestartTheGroundRoute()
	{
		witness(true); room.ready = 211;
		assertFalse(mage.decide(room, 208).reason.startsWith("Return west25"));
	}
	@Test public void versionOneKeepsItsPriorCooldownChoice()
	{
		witness(true); room.configureStrategy(1);
		assertFalse(mage.decide(room, 207).reason.startsWith("Return west25"));
	}
	@Test public void westTwentyFiveCanStillLaunchAtItsEntryFromTheStagedSide()
	{
		witness(true); mage.decide(room, 207); east.hp = 0; east.dying = true;
		room.player = tile(3291, 4248); room.wave = 25; room.spawns[25] = 208;
		Crab west = add(141, 25, 208, Lane.WEST, true, Style.MELEE, 3287, 4248);
		Decision d = mage.decide(room, 214);
		assertSame(west, d.target); assertEquals(Weapon.SCYTHE, d.weapon);
	}
}
