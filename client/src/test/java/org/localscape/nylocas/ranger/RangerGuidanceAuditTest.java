package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

/** Isolated desired-behavior probes from Jordan's 25 Nov guidance. Not canonical tests. */
public class RangerGuidanceAuditTest
{
	private final RangerStrategy strategy = new RangerStrategy();
	private int key;

	private Room room(int wave, int spawn)
	{
		Room r = new Room();
		r.wave = wave;
		r.spawns[wave] = spawn;
		r.player = RangerStrategy.tile(3295, 4248);
		r.specialEnergy = 1000;
		return r;
	}

	private Crab crab(Room r, int wave, int born, Lane lane, char slot, boolean big, Style style, int x, int y)
	{
		Crab c = new Crab(++key, key, born, wave, lane, slot, big, style, RangerStrategy.tile(x, y));
		r.crabs.add(c);
		return c;
	}

	@Test
	public void southSixSmallNeverUsesBowEvenOutsidePipeRange()
	{
		Room r = room(6, 100);
		r.player = RangerStrategy.tile(3295, 4243);
		Crab c = crab(r, 6, 100, Lane.SOUTH, 'W', false, Style.RANGE, 3295, 4233);
		Decision d = strategy.decide(r, 100);
		assertNotEquals(d.reason, Weapon.BOW, d.weapon);
	}

	@Test
	public void waveSevenBigStillKeepsItsNamedBow()
	{
		Room r = room(7, 100);
		r.player = RangerStrategy.tile(3295, 4243);
		Crab c = crab(r, 7, 100, Lane.SOUTH, 'W', true, Style.RANGE, 3295, 4233);
		Decision d = strategy.decide(r, 100);
		assertSame(c, d.target);
		assertEquals(Weapon.BOW, d.weapon);
	}

	@Test
	public void westElevenMageBigIsBackedUpAfterTheMagesOpeningOpportunity()
	{
		Room r = room(12, 108);
		r.spawns[11] = 100;
		Crab c = crab(r, 11, 100, Lane.WEST, 'S', true, Style.MAGIC, 3289, 4248);
		assertNotSame(c, strategy.decide(r, 108).target);
		Decision d = strategy.decide(r, 111);
		assertSame(d.reason, c, d.target);
		assertEquals(Weapon.EYE, d.weapon);
	}

	@Test
	public void westElevenBackupStillRespectsPartnerFlight()
	{
		Room r = room(12, 108);
		r.spawns[11] = 100;
		Crab c = crab(r, 11, 100, Lane.WEST, 'S', true, Style.MAGIC, 3289, 4248);
		c.claimedUntil = 115;
		assertNotSame(c, strategy.decide(r, 108).target);
	}

	@Test
	public void freshSplitOutranksUnopenedVeryOldSmall()
	{
		Room r = room(8, 100);
		crab(r, 1, 70, Lane.SPLIT, (char) 0, false, Style.RANGE, 3291, 4248);
		Crab c = crab(r, 8, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3299, 4248);
		assertSame(c, strategy.decide(r, 101).target);
	}

	@Test
	public void confirmedSurvivorKeepsFollowupAfterImpact()
	{
		Room r = room(8, 100);
		Crab c = crab(r, 8, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3291, 4248);
		Decision d = strategy.decide(r, 100);
		strategy.attacked(d, 101);
		c.hp = 2;
		crab(r, 8, 102, Lane.SPLIT, (char) 0, false, Style.RANGE, 3299, 4248);
		r.ready = 102;
		assertSame(c, strategy.decide(r, 102).target);
	}

	@Test
	public void immediatelyCrossesEastWithTheBowAfterTheWestOpeningShot()
	{
		Room r = room(1, 4);
		r.player = RangerStrategy.tile(3292, 4249);
		Crab west = crab(r, 1, 4, Lane.WEST, 'S', false, Style.RANGE, 3281, 4248);
		crab(r, 1, 4, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4233);
		Decision first = strategy.decide(r, 4);
		assertSame(west, first.target);
		assertEquals(Weapon.BOW, first.weapon);
		strategy.attacked(first, 5);
		west.pendingDamage = west.hp;
		r.ready = 4 + Weapon.BOW.cooldown;

		for (int tick = 5; tick < 8; tick++)
		{
			Decision cross = strategy.decide(r, tick);
			assertEquals(cross.reason, Weapon.BOW, cross.weapon);
			assertEquals(cross.reason, RangerStrategy.tile(3298, 4249), cross.tile);
			assertNull(cross.target);
			r.player = r.player.dx(2);
		}
		r.wave = 2;
		r.spawns[2] = 8;
		Crab east = crab(r, 2, 8, Lane.EAST, 'S', false, Style.RANGE, 3310, 4248);
		east.point = RangerStrategy.tile(3309, 4248);
		Decision second = strategy.decide(r, r.ready);
		assertSame(east, second.target);
		assertEquals(Weapon.BOW, second.weapon);
	}

	@Test
	public void southThirtyChinReservesItsGreenFlickerThroughTheDimCutoff()
	{
		// Recorded seed 152274597635335: wave 30 was born exactly at wave 28 + 16.
		Room r = room(31, 260);
		r.spawns[28] = 240;
		r.spawns[29] = 252;
		r.spawns[30] = 256;
		r.player = RangerStrategy.tile(3291, 4248);
		r.ready = 260;
		Crab east = crab(r, 30, 256, Lane.SOUTH, 'E', false, Style.MELEE, 3296, 4237);
		Crab west = crab(r, 30, 256, Lane.SOUTH, 'W', false, Style.MAGIC, 3295, 4237);
		Crab filler = crab(r, 31, 260, Lane.SPLIT, (char) 0, false, Style.RANGE, 3294, 4248);

		Decision stage = strategy.decide(r, 260);
		assertEquals(stage.reason, Weapon.CHIN, stage.weapon);
		assertNull(stage.target);
		assertEquals(RangerStrategy.tile(3293, 4249), stage.tile);
		r.player = stage.tile;
		r.obstructed.add(west.key);
		east.style = west.style = Style.RANGE;
		east.point = east.point.dy(1);
		west.point = west.point.dy(1);
		Decision shot = strategy.decide(r, 261);
		assertSame(shot.reason, east, shot.target);
		assertEquals(Weapon.CHIN, shot.weapon);
		assertEquals("South chin flicker 30", shot.rule);
		strategy.attacked(shot, 262);

		r.crabs.remove(filler);
		assertNull(strategy.decide(r, 262).target);
		east.style = west.style = Style.MELEE;
		assertNull(strategy.decide(r, 263).target);
	}

	@Test
	public void waveThirtyLeavesTimeToStopOnTheWestPipeTileBeforeCrossingForTheChin()
	{
		Room r = room(30, 256);
		r.player = RangerStrategy.tile(3296, 4248);
		crab(r, 30, 256, Lane.WEST, 'S', true, Style.RANGE, 3281, 4248);
		crab(r, 29, 251, Lane.SPLIT, (char) 0, false, Style.RANGE, 3296, 4247);

		Decision stage = strategy.decide(r, 256);
		assertNull(stage.reason, stage.target);
		assertEquals(RangerStrategy.tile(3291, 4248), stage.tile);
	}

	@Test
	public void unavailableWestThirtyPipeLeavesItsApproachTickForTheSouthChin()
	{
		Room r = room(30, 264);
		r.player = RangerStrategy.tile(3291, 4248);
		r.ready = 268;
		crab(r, 30, 264, Lane.WEST, 'S', true, Style.RANGE, 3284, 4248);
		crab(r, 30, 264, Lane.SOUTH, 'E', false, Style.MELEE, 3296, 4236);

		Decision stage = strategy.decide(r, 267);
		assertEquals(stage.reason, Weapon.CHIN, stage.weapon);
		assertEquals(RangerStrategy.tile(3293, 4249), stage.tile);
	}

	@Test
	public void southThirtyChinCanUseTheOtherGreenPrimary()
	{
		Room r = room(31, 260);
		r.spawns[28] = 240;
		r.spawns[29] = 252;
		r.spawns[30] = 256;
		r.player = RangerStrategy.tile(3295, 4247);
		r.ready = 261;
		Crab east = crab(r, 30, 256, Lane.SOUTH, 'E', false, Style.RANGE, 3296, 4238);
		east.pendingDamage = east.hp;
		Crab west = crab(r, 30, 256, Lane.SOUTH, 'W', false, Style.RANGE, 3295, 4238);
		Decision shot = strategy.decide(r, 261);
		assertSame(shot.reason, west, shot.target);
		assertEquals(Weapon.CHIN, shot.weapon);

		strategy.reset();
		west.pendingDamage = west.hp;
		Crab fresh = crab(r, 31, 260, Lane.SPLIT, (char) 0, false, Style.RANGE, 3294, 4248);
		assertSame(fresh, strategy.decide(r, 261).target);
	}

	@Test
	public void openingEastBowTakesFirstReadyTickOverNearbyMagicFiller()
	{
		Room r = room(2, 108);
		r.spawns[1] = 104;
		r.player = RangerStrategy.tile(3297, 4249);
		Crab c = crab(r, 2, 108, Lane.EAST, 'S', false, Style.RANGE, 3308, 4248);
		crab(r, 1, 104, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4239);
		crab(r, 2, 108, Lane.WEST, 'N', false, Style.MAGIC, 3283, 4249);
		Decision d = strategy.decide(r, 110);
		assertSame(d.reason, c, d.target);
		assertEquals(Weapon.BOW, d.weapon);
	}

	@Test
	public void mageKeepsWestElevenUntilActualTwelveSpawns()
	{
		Room r = room(11, 100);
		Crab c = crab(r, 11, 100, Lane.WEST, 'S', true, Style.MAGIC, 3289, 4248);
		assertNotSame(c, strategy.decide(r, 108).target);
	}

	@Test
	public void waveTwelveBackupYieldsWhenRangerIsNulled()
	{
		Room r = room(12, 108);
		r.spawns[11] = 100;
		Crab c = crab(r, 11, 100, Lane.WEST, 'S', true, Style.MAGIC, 3289, 4248);
		c.nulled = true;
		assertNotSame(c, strategy.decide(r, 108).target);
	}

	@Test
	public void waveTwelvePartnerNullBackupGetsTheReadyTickBeforeOrdinarySplit()
	{
		Room r = room(12, 108);
		r.spawns[11] = 100;
		Crab c = crab(r, 11, 100, Lane.WEST, 'S', true, Style.MAGIC, 3289, 4248);
		crab(r, 12, 108, Lane.SPLIT, (char) 0, false, Style.RANGE, 3299, 4248);
		c.partnerNulled = true;
		assertSame(c, strategy.decide(r, 108).target);
	}

	@Test
	public void claimedOpeningEastDoesNotBlockUsefulWork()
	{
		Room r = room(2, 108);
		r.player = RangerStrategy.tile(3297, 4249);
		Crab c = crab(r, 2, 108, Lane.EAST, 'S', false, Style.RANGE, 3308, 4248);
		c.claimedUntil = 140;
		Crab fallback = crab(r, 1, 104, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4239);
		assertSame(fallback, strategy.decide(r, 110).target);
	}

	@Test
	public void openingEastUsesBowBeforeAgeTwoWhenReadyAndReachable()
	{
		Room r = room(2, 108);
		r.player = RangerStrategy.tile(3297, 4249);
		Crab c = crab(r, 2, 108, Lane.EAST, 'S', false, Style.RANGE, 3309, 4248);
		crab(r, 1, 104, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4238);
		Decision d = strategy.decide(r, 109);
		assertSame(d.reason, c, d.target);
		assertEquals(Weapon.BOW, d.weapon);
	}

	@Test
	public void zeroDamageFollowupRetainsTargetAfterFlightResolves()
	{
		Room r = room(8, 100);
		Crab c = crab(r, 8, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3291, 4248);
		Decision d = strategy.decide(r, 100);
		strategy.attacked(d, 101);
		c.ownHitUntil = 0;
		crab(r, 8, 103, Lane.SPLIT, (char) 0, false, Style.RANGE, 3299, 4248);
		r.ready = 103;
		assertSame(c, strategy.decide(r, 103).target);
	}
}
