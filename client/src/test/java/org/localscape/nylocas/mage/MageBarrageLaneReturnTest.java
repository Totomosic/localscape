package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.Crab;
import org.localscape.nylocas.mage.MageStrategy.Decision;
import org.localscape.nylocas.mage.MageStrategy.Lane;
import org.localscape.nylocas.mage.MageStrategy.Room;
import org.localscape.nylocas.mage.MageStrategy.Style;
import org.localscape.nylocas.mage.MageStrategy.Weapon;

import static org.junit.Assert.*;

public class MageBarrageLaneReturnTest
{
	@Test
	public void crossesOntoTheEastCastingRowBeforeWaveEleven()
	{
		Room room = new Room();
		room.wave = 10;
		room.spawns[10] = 68;
		room.player = MageStrategy.tile(3296, 4252);
		room.ready = 76;
		Decision move = new MageStrategy().decide(room, 74);
		assertNull(move.target);
		assertEquals(Weapon.BARRAGE, move.weapon);
		assertEquals(MageStrategy.tile(3298, 4250), move.tile);
	}

	@Test
	public void movesNorthwestWhileKeepingTheLastSouthEyeBeforeTwentyOne()
	{
		Room room = new Room();
		room.wave = 20;
		room.spawns[20] = 156;
		room.player = MageStrategy.tile(3295, 4243);
		room.ready = 169;
		Crab blue = new Crab(104, 104, 165, 20, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			MageStrategy.tile(3298, 4245));
		blue.pendingDamage = 0;
		room.crabs.add(blue);
		MageStrategy mage = new MageStrategy();
		Decision move = mage.decide(room, 168);
		assertNull(move.target);
		assertEquals(Weapon.EYE, move.weapon);
		assertTrue(move.tile.getY() > room.player.getY());
		assertTrue(move.tile.getX() <= room.player.getX());
		room.player = move.tile;
		Decision shot = mage.decide(room, 169);
		assertSame(blue, shot.target);
		assertEquals(Weapon.EYE, shot.weapon);
	}
}
