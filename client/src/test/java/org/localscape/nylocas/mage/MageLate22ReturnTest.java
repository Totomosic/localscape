package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.Crab;
import org.localscape.nylocas.mage.MageStrategy.Decision;
import org.localscape.nylocas.mage.MageStrategy.Lane;
import org.localscape.nylocas.mage.MageStrategy.Room;
import org.localscape.nylocas.mage.MageStrategy.Style;
import org.localscape.nylocas.mage.MageStrategy.Weapon;

import static org.junit.Assert.*;

public class MageLate22ReturnTest
{
	private final MageStrategy mage = new MageStrategy();
	private final Room room = new Room();

	private Crab add(int key, int wave, int born, Lane lane, char slot, boolean big, Style style, int x, int y)
	{
		Crab crab = new Crab(key, key, born, wave, lane, slot, big, style, MageStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	@Test public void southEyeKeepsAPassiveReturnWhenBothBigsAreOutOfSight()
	{
		// Native seed 239959028216560 previously chased east after the Eye and lost the sweep.
		room.wave = 22;
		room.spawns[22] = 184;
		room.player = MageStrategy.tile(3294, 4245);
		room.specialEnergy = 500;
		Crab west = add(126, 22, 184, Lane.WEST, 'S', true, Style.MELEE, 3290, 4248);
		Crab south = add(127, 22, 184, Lane.SOUTH, 'W', false, Style.MELEE, 3295, 4242);
		Crab east = add(129, 22, 184, Lane.EAST, 'S', true, Style.MELEE, 3300, 4248);
		east.fighting = true;
		Crab magic = add(111, 20, 171, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3299, 4243);
		Decision clear = mage.decide(room, 193);
		assertSame(magic, clear.target);
		assertEquals(Weapon.EYE, clear.weapon);
		mage.attacked(clear, 194, 193);
		magic.pendingDamage = 8;
		room.ready = 196;
		south.point = MageStrategy.tile(3295, 4243);
		room.obstructed.add(east.key);
		room.obstructed.add(west.key);
		Decision move = mage.decide(room, 194);
		assertSame(south, move.approach);
		assertTrue(move.tile.getY() <= 4245);
		assertSame(south, mage.decide(room, 196).target);
	}

	@Test public void approachingEastBigCanAnchorTheSameDelayedSweep()
	{
		room.wave = 22;
		room.spawns[22] = 176;
		room.player = MageStrategy.tile(3295, 4244);
		room.specialEnergy = 500;
		Crab south = add(1, 22, 176, Lane.SOUTH, 'W', false, Style.MELEE, 3295, 4242);
		Crab east = add(2, 22, 176, Lane.EAST, 'S', true, Style.MELEE, 3298, 4246);
		east.fighting = true;
		Crab magic = add(3, 21, 174, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3296, 4245);
		Decision clear = mage.decide(room, 185);
		assertSame(magic, clear.target);
		mage.attacked(clear, 186, 185);
		magic.pendingDamage = 8;
		room.ready = 188;
		east.point = MageStrategy.tile(3296, 4245);
		south.point = MageStrategy.tile(3294, 4243);
		Decision special = mage.decide(room, 188);
		assertSame(east, special.target);
		assertEquals("22 south Dinh", special.rule);
		assertTrue(special.special);

		east.nulled = true;
		assertSame(south, mage.decide(room, 188).target);
	}
}
