package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerForecastPreparationTest
{
	private Room room(int ready)
	{
		Room room = new Room();
		room.wave = 17;
		room.spawns[17] = 100;
		room.ready = ready;
		room.player = RangerStrategy.tile(3295, 4248);
		return room;
	}

	private Crab parent(Room room, int splitAt)
	{
		Crab parent = new Crab(1, 1, 80, 13, Lane.EAST, 'S', true, Style.MELEE,
			RangerStrategy.tile(3300, 4250));
		parent.hp = 0;
		parent.dying = true;
		parent.observeSplitAt(splitAt);
		room.crabs.add(parent);
		return parent;
	}

	private Crab small(Room room, Style style)
	{
		Crab small = new Crab(2, 2, 80, 15, Lane.SPLIT, (char) 0, false, style,
			room.player.dy(-1));
		small.pendingDamage = 0;
		room.crabs.add(small);
		return small;
	}

	@Test
	public void idleCooldownPreparesBothActualChildSquaresBeforeTheirBirth()
	{
		Room room = room(102);
		Crab parent = parent(room, 102);
		Decision decision = new RangerStrategy().decide(room, 100);
		assertTrue(decision.reason, decision.reason.contains("children of #1"));
		assertNull(decision.target);
		assertNull(decision.approach);
		assertEquals(Weapon.PIPE, decision.weapon);
		assertTrue(decision.tile.distanceTo(parent.point) <= Weapon.PIPE.range);
		assertTrue(decision.tile.distanceTo(parent.point.dx(1).dy(1)) <= Weapon.PIPE.range);
		assertTrue((room.player.distanceTo(decision.tile) + 1) / 2 <= 2);
	}

	@Test
	public void timedLethalImpactsCanPrepareDuringCooldownBeforeTheDeathAnimation()
	{
		Room room = room(106);
		Crab parent = parent(room, -1);
		parent.hp = 16;
		parent.dying = false;
		parent.pendingDamage = 16;
		parent.observeIncoming(new int[] {0, 8, 8});
		Decision decision = new RangerStrategy().decide(room, 100);
		assertTrue(decision.reason, decision.reason.contains("@108: children of #1"));
	}

	@Test
	public void anUntimedRolledTotalCannotInventASplitBirth()
	{
		Room room = room(105);
		Crab parent = parent(room, -1);
		parent.hp = 16;
		parent.dying = false;
		parent.pendingDamage = 16;
		Decision decision = new RangerStrategy().decide(room, 100);
		assertFalse(decision.reason, decision.reason.contains("children of"));
	}

	@Test
	public void aOneTickBirthCanReleaseAnUnhitSmallTwoWavesOld()
	{
		Room room = room(102);
		parent(room, 103);
		small(room, Style.MAGIC);
		Decision decision = new RangerStrategy().decide(room, 102);
		assertNull(decision.target);
		assertTrue(decision.reason, decision.reason.contains("children of #1"));
	}

	@Test
	public void aReadyPipeStillFitsBeforeABirthTwoTicksAway()
	{
		Room room = room(102);
		parent(room, 104);
		Crab small = small(room, Style.RANGE);
		assertSame(small, new RangerStrategy().decide(room, 102).target);
	}

	@Test
	public void aConfirmedSurvivorKeepsItsFollowupAheadOfAFutureSplit()
	{
		Room room = room(102);
		parent(room, 103);
		Crab small = small(room, Style.MAGIC);
		RangerStrategy ranger = new RangerStrategy();
		ranger.attacked(new Decision(small, Weapon.EYE, room.player, false, null, "Previous hit"), 99);
		small.hp = 3;
		small.pendingDamage = 0;
		assertSame(small, ranger.decide(room, 102).target);
	}

	@Test
	public void aNamedPacketKeepsItsStagingAheadOfAnIdleSplit()
	{
		Room room = room(102);
		room.wave = 9;
		room.spawns[9] = 92;
		parent(room, 103);
		Decision decision = new RangerStrategy().decide(room, 102);
		assertFalse(decision.reason, decision.reason.contains("children of"));
		assertEquals(Weapon.CHIN, decision.weapon);
	}
}
