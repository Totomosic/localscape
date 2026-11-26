package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;

public class RangerSouthTwentyNullRescueTest
{
	private Room room(int wave)
	{
		Room room = new Room();
		room.wave = wave;
		room.spawns[wave] = 188;
		room.player = RangerStrategy.tile(3295, 4248);
		return room;
	}

	private Crab add(Room room, int key, int wave, int born, Lane lane, boolean big, Style style)
	{
		Crab crab = new Crab(key, key, born, wave, lane, (char) 0, big, style, room.player);
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	private Crab south(Room room)
	{
		Crab parent = add(room, 96, 20, 160, Lane.SOUTH, true, Style.RANGE);
		parent.partnerNulled = true;
		return parent;
	}

	@Test
	public void permanentRangeSouthTwentyWithANullMageOutranksOrdinaryWorkAfterTwentyTwo()
	{
		for (int wave : new int[] {23, 24, 25, 26, 27, 28})
		{
			Room room = room(wave);
			Crab parent = south(room);
			add(room, 100, wave, 188, Lane.SPLIT, false, Style.RANGE);
			Decision shot = new RangerStrategy().decide(room, 190);
			assertSame(wave + ": " + shot.reason, parent, shot.target);
			assertEquals(Weapon.PIPE, shot.weapon);
		}
	}

	@Test
	public void actualMageNullReleasesAnUnhitFillerApproach()
	{
		Room room = room(23);
		room.ready = 190;
		Crab parent = south(room);
		parent.partnerNulled = false;
		Crab small = add(room, 100, 23, 188, Lane.SPLIT, false, Style.RANGE);
		RangerStrategy ranger = new RangerStrategy();
		assertSame(small, ranger.decide(room, 189).approach);
		parent.partnerNulled = true;
		assertSame(parent, ranger.decide(room, 190).target);
	}

	@Test
	public void theRescueCanPrepareDuringCooldownAndFinishNonlethalFeedback()
	{
		Room room = room(23);
		room.ready = 192;
		Crab parent = south(room);
		parent.point = RangerStrategy.tile(3291, 4243);
		RangerStrategy ranger = new RangerStrategy();
		Decision movement = ranger.decide(room, 190);
		assertNull(movement.target);
		assertSame(parent, movement.approach);
		assertFalse(movement.pathToTarget);
		Decision shot = ranger.decide(room, 192);
		ranger.attacked(shot, 192);
		parent.hp = 3;
		room.ready = 194;
		add(room, 100, 23, 193, Lane.SPLIT, false, Style.RANGE);
		assertSame(parent, ranger.decide(room, 194).target);
	}

	@Test
	public void realClaimsLethalFlightAndOwnNullStillProtectTheParent()
	{
		for (int guard = 0; guard < 8; guard++)
		{
			Room room = room(23);
			Crab parent = south(room);
			if (guard == 0) parent.nulled = true;
			if (guard == 1) parent.pendingDamage = parent.hp;
			if (guard == 2) parent.pendingDamage = -1;
			if (guard == 3) parent.partnerNulled = false;
			if (guard == 4) { parent.pendingDamage = -1; parent.claimedUntil = 195; }
			if (guard == 5) parent.provisionalClaimUntil = 195;
			if (guard == 6) parent.dying = true;
			if (guard == 7) parent.hp = 0;
			Crab small = add(room, 100, 23, 188, Lane.SPLIT, false, Style.RANGE);
			assertSame("guard " + guard, small, new RangerStrategy().decide(room, 190).target);
		}
	}

	@Test
	public void theRescueYieldsItsCooldownToTheNamedWestTwentyFourPipe()
	{
		Room room = room(24);
		room.spawns[24] = 190;
		Crab parent = south(room);
		Crab west = add(room, 101, 24, 190, Lane.WEST, true, Style.RANGE);
		west.point = RangerStrategy.tile(3283, 4248);
		Decision preparation = new RangerStrategy().decide(room, 192);
		assertNotSame(parent, preparation.target);
		assertNotSame(parent, preparation.approach);
		assertEquals(Weapon.PIPE, preparation.weapon);
	}

	@Test
	public void aNamedSouthTwentyThreeBowKeepsItsAttackTick()
	{
		Room room = room(23);
		Crab parent = south(room);
		Crab bow = add(room, 101, 23, 188, Lane.SOUTH, true, Style.RANGE);
		Decision shot = new RangerStrategy().decide(room, 190);
		assertSame(bow, shot.target);
		assertNotSame(parent, shot.target);
		assertEquals("South bow 23", shot.rule);
	}

	@Test
	public void theRescueDoesNotBypassTheCleanupDimOrParentExpiry()
	{
		for (int guard = 0; guard < 2; guard++)
		{
			Room room = room(guard == 0 ? 31 : 25);
			Crab parent = south(room);
			Decision decision = new RangerStrategy().decide(room, guard == 0 ? 190 : 209);
			assertNotSame(parent, decision.target);
			assertNotSame(parent, decision.approach);
		}
	}
}
