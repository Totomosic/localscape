package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.Crab;
import org.localscape.nylocas.mage.MageStrategy.Decision;
import org.localscape.nylocas.mage.MageStrategy.Lane;
import org.localscape.nylocas.mage.MageStrategy.Room;
import org.localscape.nylocas.mage.MageStrategy.Style;
import org.localscape.nylocas.mage.MageStrategy.Weapon;

import static org.junit.Assert.*;

public class MageEastTwentyEightAfterSweepTest
{
	private final MageStrategy mage = new MageStrategy();
	private final Room room = new Room();
	private final Crab east = new Crab(2, 2, 212, 28, Lane.EAST, 'S', false,
		Style.MELEE, MageStrategy.tile(3301, 4246));
	private final Crab south = new Crab(3, 3, 224, 29, Lane.SOUTH, 'W', true,
		Style.MELEE, MageStrategy.tile(3295, 4236));

	public MageEastTwentyEightAfterSweepTest()
	{
		Crab primary = new Crab(1, 1, 200, 26, Lane.SOUTH, 'W', true,
			Style.MELEE, MageStrategy.tile(3292, 4244));
		room.player = MageStrategy.tile(3294, 4245);
		mage.attacked(new Decision(primary, Weapon.DINH, room.player, true, "26 Dinh on 28", "sweep"), 221, 220);
		room.wave = 29;
		room.spawns[28] = 212;
		room.spawns[29] = 224;
		room.ready = 225;
		east.pendingDamage = south.pendingDamage = 0;
		room.crabs.add(east);
		room.crabs.add(south);
	}

	@Test public void usesTheSweepsCooldownToReachTheEastSmall()
	{
		room.wave = 28;
		room.spawns[29] = -1;
		room.crabs.remove(south);
		Decision step = mage.decide(room, 221);
		assertNull(step.target);
		assertEquals(Weapon.SULPHUR, step.weapon);
		assertTrue(step.tile.getX() >= 3299);
	}

	@Test public void aFourTickSulphurLeavesTimeForTheSouthDoorway()
	{
		room.player = MageStrategy.tile(3300, 4246);
		Decision attack = mage.decide(room, 225);
		assertSame(east, attack.target);
		assertEquals(Weapon.SULPHUR, attack.weapon);

		mage.attacked(attack, 226, 225);
		room.ready = 229;
		room.player = MageStrategy.tile(3295, 4243);
		south.point = MageStrategy.tile(3295, 4240);
		assertSame(south, mage.decide(room, 231).target);
	}

	@Test public void theEarlierSouthDoorwayKeepsItsSwing()
	{
		room.spawns[29] = 220;
		room.player = MageStrategy.tile(3300, 4246);
		assertNotEquals("East 28 before the South 29 doorway", mage.decide(room, 225).reason);
	}

	@Test public void aCoveredSmallFreesTheCooldownForOtherWork()
	{
		east.pendingDamage = east.hp;
		assertNotEquals("Cross to East 28 after the south sweep", mage.decide(room, 224).reason);
	}
}
