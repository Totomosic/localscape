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
	public void westElevenMageBigIsBackedUpWhenWaveTwelveEnters()
	{
		Room r = room(12, 108);
		r.spawns[11] = 100;
		Crab c = crab(r, 11, 100, Lane.WEST, 'S', true, Style.MAGIC, 3289, 4248);
		Decision d = strategy.decide(r, 108);
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
	public void waveTwelveBackupGetsReadyTickBeforeOrdinarySplit()
	{
		Room r = room(12, 108);
		r.spawns[11] = 100;
		Crab c = crab(r, 11, 100, Lane.WEST, 'S', true, Style.MAGIC, 3289, 4248);
		crab(r, 12, 108, Lane.SPLIT, (char) 0, false, Style.RANGE, 3299, 4248);
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
