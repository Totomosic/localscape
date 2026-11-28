package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.Crab;
import org.localscape.nylocas.mage.MageStrategy.Decision;
import org.localscape.nylocas.mage.MageStrategy.Lane;
import org.localscape.nylocas.mage.MageStrategy.Room;
import org.localscape.nylocas.mage.MageStrategy.Style;
import org.localscape.nylocas.mage.MageStrategy.Weapon;

import static org.junit.Assert.*;

public class MageLaneSurvivorTest
{
	private final Room room = new Room();
	private final MageStrategy mage = new MageStrategy();
	private final Crab green = new Crab(42, 48, 68, 10, Lane.EAST, 'N', false, Style.RANGE,
		MageStrategy.tile(3302, 4249));

	public MageLaneSurvivorTest()
	{
		// Seed 244661013755948. Wave 11 was held at the first check and the mage
		// reached its barrage square with an east-10 survivor still in pipe range.
		room.wave = 10;
		room.spawns[10] = 68;
		room.ready = 76;
		room.player = MageStrategy.tile(3301, 4249);
		room.observePartner(MageStrategy.tile(3295, 4248), 78);
		green.pendingDamage = 0;
		room.crabs.add(green);
		for (int i = 0; i < 11; i++)
		{
			Crab other = new Crab(i + 1, i + 1, 44, 7, Lane.SPLIT, (char) 0, false, Style.MELEE,
				MageStrategy.tile(3292, 4251));
			other.nulled = true;
			room.crabs.add(other);
		}
	}

	@Test public void shootsAnUncoveredOriginalFromTheBarrageSquare()
	{
		Decision shot = mage.decide(room, 76);
		assertSame(green, shot.target);
		assertEquals(Weapon.PIPE, shot.weapon);
		assertEquals(room.player, shot.tile);
		assertTrue(shot.reason.contains("uncovered small"));
	}

	@Test public void aLethalChinAlreadyInFlightKeepsTheBarragePreparation()
	{
		green.pendingDamage = green.hp;
		green.observeIncoming(new int[] {0, green.hp});
		assertNotSame(green, mage.decide(room, 76).target);
	}

	@Test public void holdsTheLastTickBeforeTheEastBarrage()
	{
		assertNotSame(green, mage.decide(room, 79).target);
	}
}
