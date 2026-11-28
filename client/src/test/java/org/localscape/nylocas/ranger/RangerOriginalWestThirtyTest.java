package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;

public class RangerOriginalWestThirtyTest
{
	private Room room(int wave)
	{
		Room room = new Room();
		room.wave = wave;
		room.player = RangerStrategy.tile(3293, 4249);
		room.spawns[28] = 284;
		room.spawns[29] = 296;
		room.spawns[30] = 300;
		room.spawns[31] = 304;
		return room;
	}

	private Crab crab(Room room, int key, int born, int wave, Lane lane, char slot, boolean big, Style style)
	{
		Crab crab = new Crab(key, key, born, wave, lane, slot, big, style, room.player);
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	private void chin(RangerStrategy strategy, Room room)
	{
		Crab south = crab(room, 2, 300, 30, Lane.SOUTH, 'E', false, Style.RANGE);
		strategy.attacked(new Decision(south, Weapon.CHIN, room.player, false,
			"South chin flicker 30", "Scheduled south chin"), 305);
		room.crabs.remove(south);
		room.ready = 308;
	}

	@Test
	public void firstGreenSouthChinStillOutranksTheParentAcrossTheDim()
	{
		Room room = room(31);
		Crab west = crab(room, 1, 300, 30, Lane.WEST, (char) 0, true, Style.MELEE);
		Crab south = crab(room, 2, 300, 30, Lane.SOUTH, 'E', false, Style.RANGE);
		Decision shot = new RangerStrategy().decide(room, 305);
		assertSame(south, shot.target);
		assertEquals("South chin flicker 30", shot.rule);
		assertNotSame(west, shot.approach);
	}

	@Test
	public void originalWestParentOutranksReadyWave31AndPartnerNullRescueAfterTheChin()
	{
		Room room = room(31);
		RangerStrategy strategy = new RangerStrategy();
		chin(strategy, room);
		Crab west = crab(room, 1, 300, 30, Lane.WEST, (char) 0, true, Style.RANGE);
		Crab fresh = crab(room, 3, 306, 31, Lane.SPLIT, (char) 0, false, Style.RANGE);
		fresh.partnerNulled = true;
		crab(room, 4, 304, 31, Lane.WEST, 'S', false, Style.RANGE);
		Decision shot = strategy.decide(room, 308);
		assertSame(west, shot.target);
		assertEquals(Weapon.PIPE, shot.weapon);
	}

	@Test
	public void parentPrioritySurvivesAConfirmedNonlethalHit()
	{
		Room room = room(31);
		RangerStrategy strategy = new RangerStrategy();
		chin(strategy, room);
		Crab west = crab(room, 1, 300, 30, Lane.WEST, (char) 0, true, Style.RANGE);
		Decision shot = strategy.decide(room, 308);
		strategy.attacked(shot, 308);
		west.hp = 3;
		room.ready = 310;
		crab(room, 4, 309, 31, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(west, strategy.decide(room, 310).target);
	}

	@Test
	public void lethalDamageInFlightReleasesTheParentPriority()
	{
		Room room = room(31);
		RangerStrategy strategy = new RangerStrategy();
		chin(strategy, room);
		Crab west = crab(room, 1, 300, 30, Lane.WEST, (char) 0, true, Style.RANGE);
		west.pendingDamage = west.hp;
		Crab fresh = crab(room, 4, 307, 31, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(fresh, strategy.decide(room, 308).target);
	}

	@Test
	public void anOriginalParentSurvivingToPermanentMeleeStillGetsFinished()
	{
		Room room = room(31);
		RangerStrategy strategy = new RangerStrategy();
		chin(strategy, room);
		Crab west = crab(room, 1, 300, 30, Lane.WEST, (char) 0, true, Style.MELEE);
		crab(room, 4, 324, 31, Lane.SPLIT, (char) 0, false, Style.RANGE);
		Decision shot = strategy.decide(room, 326);
		assertSame(west, shot.target);
		assertEquals(Weapon.SULPHUR, shot.weapon);
	}

	@Test
	public void aFarParentGetsManualMovementWhileThePipeCoolsDown()
	{
		Room room = room(31);
		RangerStrategy strategy = new RangerStrategy();
		chin(strategy, room);
		Crab west = crab(room, 1, 300, 30, Lane.WEST, (char) 0, true, Style.RANGE);
		west.point = RangerStrategy.tile(3290, 4248);
		room.player = RangerStrategy.tile(3301, 4248);
		Decision movement = strategy.decide(room, 306);
		assertNull(movement.target);
		assertSame(west, movement.approach);
		assertFalse(movement.pathToTarget);
	}

	@Test
	public void theChinCooldownPreparesForTheParentsAgeSevenRangeReturn()
	{
		Room room = room(31);
		RangerStrategy strategy = new RangerStrategy();
		chin(strategy, room);
		Crab west = crab(room, 1, 300, 30, Lane.WEST, (char) 0, true, Style.MELEE);
		Decision movement = strategy.decide(room, 306);
		assertNull(movement.target);
		assertSame(west, movement.approach);
		assertEquals(Weapon.PIPE, movement.weapon);
		west.style = Style.RANGE;
		assertSame(west, strategy.decide(room, 308).target);
	}

	@Test
	public void anObstructedParentGetsAnExplicitApproachInsteadOfAnNpcClick()
	{
		Room room = room(31);
		RangerStrategy strategy = new RangerStrategy();
		chin(strategy, room);
		Crab west = crab(room, 1, 300, 30, Lane.WEST, (char) 0, true, Style.RANGE);
		room.obstructed.add(west.key);
		Decision movement = strategy.decide(room, 308);
		assertNull(movement.target);
		assertSame(west, movement.approach);
		assertFalse(movement.pathToTarget);
	}
}
