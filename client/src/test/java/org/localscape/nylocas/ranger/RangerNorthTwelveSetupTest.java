package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerNorthTwelveSetupTest
{
	private final Room room = new Room();
	private final RangerStrategy ranger = new RangerStrategy();
	private final Crab small;
	private final Crab west;

	public RangerNorthTwelveSetupTest()
	{
		room.wave = 12;
		room.spawns[11] = 100;
		room.spawns[12] = 108;
		room.player = RangerStrategy.tile(3295, 4247);
		room.ready = 112;
		small = add(1, 100, 11, Lane.SPLIT, (char) 0, false, Style.RANGE, 3300, 4248);
		west = add(2, 108, 12, Lane.WEST, 'S', false, Style.MELEE, 3281, 4248);
		add(3, 108, 12, Lane.SOUTH, 'W', true, Style.MELEE, 3295, 4233);
	}

	private Crab add(int key, int born, int wave, Lane lane, char slot, boolean big, Style style, int x, int y)
	{
		Crab crab = new Crab(key, key, born, wave, lane, slot, big, style, RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void cooldownMoveReachesTheNorthernLaneWithoutLosingTheSelectedPipe()
	{
		Decision stage = ranger.decide(room, 110);
		assertTrue(stage.reason, stage.reason.startsWith("North lane setup"));
		assertEquals(4249, stage.tile.getY());
		assertNull(stage.approach);
		room.player = stage.tile;
		Decision attack = ranger.decide(room, 112);
		assertSame(attack.reason, small, attack.target);
		assertEquals(Weapon.PIPE, attack.weapon);
	}

	@Test
	public void aReadyUsefulShotDoesNotWaitForTheNorthernRow()
	{
		room.ready = 110;
		assertSame(small, ranger.decide(room, 110).target);
	}

	@Test
	public void anObservedWest12PrimaryReleasesTheSetupEvenOnAZeroRoll()
	{
		west.claimedUntil = 141;
		Decision decision = ranger.decide(room, 110);
		assertFalse(decision.reason, decision.reason.startsWith("North lane setup"));
	}

	@Test
	public void aNorthernDetourThatWouldMissThePlannedShotIsRejected()
	{
		room.player = RangerStrategy.tile(3295, 4243);
		room.ready = 111;
		room.crabs.remove(small);
		add(4, 100, 11, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4242);
		Decision decision = ranger.decide(room, 110);
		assertFalse(decision.reason, decision.reason.startsWith("North lane setup"));
	}

	@Test
	public void noCandidateTimeCanSetUpWithoutInventingAnAttack()
	{
		room.crabs.remove(small);
		Decision stage = ranger.decide(room, 110);
		assertTrue(stage.reason, stage.reason.startsWith("North lane setup"));
		assertNull(stage.target);
		assertNull(stage.approach);
		assertEquals(4249, stage.tile.getY());
	}
}
