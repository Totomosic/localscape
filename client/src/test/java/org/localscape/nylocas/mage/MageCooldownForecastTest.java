package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;

import static org.junit.Assert.*;

public class MageCooldownForecastTest
{
	@Test
	public void aLowOpeningShadowStillLeadsToTheWestTwoEye()
	{
		MageStrategy strategy = new MageStrategy();
		Room room = new Room();
		room.wave = 1;
		room.spawns[1] = 4;
		Crab south = body(room, 1, 4, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4234);
		strategy.attacked(strategy.decide(room, 5), 6, 5);
		south.pendingDamage = 2;
		room.wave = 2;
		room.spawns[2] = 8;
		room.ready = 10;
		room.player = MageStrategy.tile(3292, 4248);
		Crab west = body(room, 2, 8, Lane.WEST, 'N', false, Style.MAGIC, 3284, 4249);
		Decision follow = strategy.decide(room, 11);
		assertSame(west, follow.target);
		assertEquals(Weapon.EYE, follow.weapon);
	}

	@Test
	public void confirmedSouthShadowImmediatelyCrossesWestBeforeWaveTwoExists()
	{
		MageStrategy strategy = new MageStrategy();
		Room room = new Room();
		room.wave = 1;
		room.spawns[1] = 4;
		Crab south = body(room, 1, 4, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4234);
		Decision cast = strategy.decide(room, 5);
		assertSame(south, cast.target);
		assertEquals(Weapon.SHADOW, cast.weapon);
		strategy.attacked(cast, 6, 5);
		room.ready = 10;
		// A known nonlethal roll must not pull the mage south during cooldown.
		south.pendingDamage = 1;
		for (int tick = 6; tick <= 9; tick++)
		{
			Decision move = strategy.decide(room, tick);
			assertNull(move.target);
			assertTrue(move.tile.getX() < room.player.getX());
			assertTrue(move.tile.getY() >= 4248);
			assertTrue(move.reason, move.reason.contains("West magic 2"));
		}
		room.wave = 2;
		room.spawns[2] = 8;
		room.player = MageStrategy.tile(3292, 4249);
		Crab west = body(room, 2, 8, Lane.WEST, 'N', false, Style.MAGIC, 3283, 4249);
		assertSame(west, strategy.decide(room, 10).target);
	}

	@Test
	public void noCandidateCooldownStagesForAnObservedSplitBeforeGenericLaneSetup()
	{
		MageStrategy strategy = new MageStrategy();
		Room room = splitRoom();
		room.ready = 124;
		Crab parent = body(room, 15, 108, Lane.EAST, 'S', true, Style.MELEE, 3300, 4252);
		parent.hp = 0;
		parent.dying = true;
		parent.observeSplitAt(124);
		Decision move = strategy.decide(room, 120);
		assertNull(move.target);
		assertTrue(move.reason, move.reason.contains("parent:" + parent.key));
		assertTrue(move.tile.getX() >= 3299);
		assertTrue(move.tile.getY() >= 4251);
	}

	@Test
	public void oldFillerUsesAllOfCooldownToReachAnUpcomingLane()
	{
		MageStrategy strategy = new MageStrategy();
		Room room = new Room();
		room.wave = 5;
		room.spawns[5] = 20;
		room.ready = 36;
		room.player = MageStrategy.tile(3301, 4249);
		body(room, 3, 12, Lane.SPLIT, (char) 0, false, Style.MELEE, 3300, 4249);
		Decision move = strategy.decide(room, 32);
		assertNull(move.target);
		assertEquals(Weapon.EYE, move.weapon);
		assertEquals(MageStrategy.tile(3290, 4249), move.tile);
		assertTrue(move.reason, move.reason.contains("lane:6"));
	}

	@Test
	public void noCandidateWaitsOnItsSplitFiringTileThenUsesTheNewborn()
	{
		MageStrategy strategy = new MageStrategy();
		Room room = splitRoom();
		room.ready = 122;
		Crab parent = body(room, 15, 108, Lane.EAST, 'S', true, Style.MELEE, 3300, 4252);
		parent.hp = 0;
		parent.dying = true;
		parent.observeSplitAt(122);
		Decision move = strategy.decide(room, 120);
		room.player = move.tile;
		assertEquals(move.tile, strategy.decide(room, 121).tile);
		room.crabs.remove(parent);
		Crab split = body(room, 16, 122, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3300, 4252);
		assertSame(split, strategy.decide(room, 122).target);
	}

	private static Room splitRoom()
	{
		Room room = new Room();
		room.wave = 16;
		room.spawns[16] = 120;
		room.player = MageStrategy.tile(3290, 4248);
		return room;
	}

	private static Crab body(Room room, int wave, int born, Lane lane, char slot, boolean big, Style style, int x, int y)
	{
		int key = room.crabs.size() + 1;
		Crab crab = new Crab(key, key, born, wave, lane, slot, big, style, MageStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}
}
