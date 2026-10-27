package net.runelite.client.plugins.nyloranger;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Crab;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Decision;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Lane;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Room;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Style;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Weapon;
import org.junit.Test;

import static org.junit.Assert.*;

public class RangerStrategyTest
{
	private final RangerStrategy strategy = new RangerStrategy();
	private int key;

	private Room room(int wave, int spawn)
	{
		Room room = new Room();
		room.wave = wave;
		room.spawns[wave] = spawn;
		room.player = RangerStrategy.tile(3295, 4248);
		room.specialEnergy = 1000;
		return room;
	}

	private Crab crab(Room room, int wave, int born, Lane lane, char slot, boolean big, Style style)
	{
		Crab crab = new Crab(++key, key, born, wave, lane, slot, big, style, RangerStrategy.tile(3295, 4246));
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void prefersFreshSmallToOldSmall()
	{
		Room room = room(12, 90);
		crab(room, 8, 65, Lane.SPLIT, (char) 0, false, Style.RANGE);
		Crab fresh = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(fresh, strategy.decide(room, 101).target);
	}

	@Test
	public void doesNotDoubleUpDuringPartnerProjectileWindow()
	{
		Room room = room(12, 90);
		Crab claimed = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
		claimed.claimedUntil = 105;
		assertNull(strategy.decide(room, 101).target);
		assertSame(claimed, strategy.decide(room, 105).target);
	}

	@Test
	public void doesNotOpenUnassignedMagicBigBeforeCap()
	{
		Room room = room(17, 100);
		crab(room, 17, 100, Lane.WEST, 'S', true, Style.MAGIC);
		assertNull(strategy.decide(room, 100).target);
	}

	@Test
	public void waitsInsteadOfEyeThatWouldBlockWave23Prefire()
	{
		Room room = room(22, 100);
		room.player = RangerStrategy.tile(3295, 4244);
		crab(room, 22, 109, Lane.SPLIT, (char) 0, false, Style.MAGIC);
		Decision decision = strategy.decide(room, 110);
		assertNull(decision.target);
		assertEquals(Weapon.BOW, decision.weapon);
		assertEquals(RangerStrategy.tile(3295, 4244), decision.tile);
	}

	@Test
	public void permitsTwoTickFillerThatFitsButNotOneTickLater()
	{
		Room room = room(22, 100);
		room.player = RangerStrategy.tile(3295, 4244);
		Crab fresh = crab(room, 22, 109, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(fresh, strategy.decide(room, 110).target);
		assertNull(strategy.decide(room, 111).target);
	}

	@Test
	public void southPrefireOutranksFreshSplit()
	{
		Room room = room(23, 192);
		Crab south = crab(room, 23, 192, Lane.SOUTH, 'W', true, Style.RANGE);
		crab(room, 23, 192, Lane.SPLIT, (char) 0, false, Style.RANGE);
		Decision decision = strategy.decide(room, 192);
		assertSame(south, decision.target);
		assertEquals(Weapon.BOW, decision.weapon);
	}

	@Test
	public void wave20UsesWestBigWhileRangedNotItsInitialMeleeStyle()
	{
		Room room = room(20, 100);
		Crab west = crab(room, 20, 100, Lane.WEST, 'S', true, Style.RANGE);
		Decision decision = strategy.decide(room, 105);
		assertSame(west, decision.target);
		assertEquals(Weapon.BOW, decision.weapon);
		west.style = Style.MELEE;
		assertNull(strategy.decide(room, 105).target);
	}

	@Test
	public void willNotReleaseAttackIntoNextTicksWrongStyle()
	{
		Room room = room(20, 100);
		crab(room, 20, 100, Lane.WEST, 'S', true, Style.RANGE);
		assertNull(strategy.decide(room, 106).target); // Melee at age seven, before the request executes.
	}

	@Test
	public void cooldownCannotBeBypassedByPrefire()
	{
		Room room = room(20, 100);
		crab(room, 20, 100, Lane.WEST, 'S', true, Style.RANGE);
		room.ready = 106;
		Decision decision = strategy.decide(room, 105);
		assertNull(decision.target);
		assertEquals(Weapon.BOW, decision.weapon);
	}

	@Test
	public void carriesWave27BigIntoWave28()
	{
		Room room = room(28, 108);
		Crab big = crab(room, 27, 100, Lane.SOUTH, 'W', true, Style.RANGE);
		assertSame(big, strategy.decide(room, 108).target);
	}

	@Test
	public void stallsDoNotAdvanceWaveOrResetTargetAge()
	{
		Room room = room(28, 100);
		Crab split = crab(room, 28, 112, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(split, strategy.decide(room, 113).target);
		assertEquals(28, room.wave);
		assertEquals(100, room.spawns[28]);
	}

	@Test
	public void preservesWave29LanePacketAfter30Spawns()
	{
		Room room = room(30, 104);
		Crab north = crab(room, 29, 100, Lane.WEST, 'N', false, Style.RANGE);
		assertSame(north, strategy.decide(room, 105).target);
		assertFalse(room.cleanup);
	}

	@Test
	public void cutoffIsOnlyAppliedAfterFinalLanePacket()
	{
		Room room = room(31, 100);
		Crab north = crab(room, 31, 100, Lane.WEST, 'N', false, Style.RANGE);
		assertSame(north, strategy.decide(room, 105).target);
		assertFalse(room.cleanup);
	}

	@Test
	public void finalCleanupIgnoresOldCrabsButKeepsFreshChildOfOldParent()
	{
		Room room = room(31, 100);
		crab(room, 29, 95, Lane.WEST, 'S', false, Style.RANGE);
		crab(room, 25, 90, Lane.SOUTH, 'W', true, Style.RANGE);
		// No lineage filter: this split's own birth wave is 31 even if its parent was 25.
		Crab child = crab(room, 31, 119, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(child, strategy.decide(room, 121).target);
		assertTrue(room.cleanup);
		room.crabs.remove(child);
		assertNull(strategy.decide(room, 122).target);
	}

	@Test
	public void nearbyScratchCannotDisplaceUsefulRangedWork()
	{
		Room room = room(12, 90);
		Crab melee = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.MELEE);
		melee.point = room.player.dx(1);
		Crab range = crab(room, 12, 99, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(range, strategy.decide(room, 101).target);
	}

	@Test
	public void clawBurstRequiresEnergyButStillFallsBackToScratch()
	{
		Room room = room(7, 104);
		Crab big = crab(room, 6, 100, Lane.EAST, 'S', true, Style.MELEE);
		Decision decision = strategy.decide(room, 110);
		assertSame(big, decision.target);
		assertTrue(decision.special);
		room.specialEnergy = 400;
		assertFalse(strategy.decide(room, 110).special);
	}

	@Test
	public void requestsAreNotCompletedUntilAttackIsAcknowledged()
	{
		Room room = room(23, 100);
		Crab target = crab(room, 23, 100, Lane.SOUTH, 'W', true, Style.RANGE);
		Decision first = strategy.decide(room, 100);
		assertEquals("South bow 23", strategy.decide(room, 101).rule);
		strategy.attacked(first, 101);
		assertNotEquals("South bow 23", strategy.decide(room, 102).rule);
		target.dying = true;
		assertNull(strategy.decide(room, 102).target);
	}

	@Test
	public void flickerFixtureIncludesLateChangesAndFixedSplits()
	{
		Room room = room(31, 100);
		Crab north = crab(room, 31, 100, Lane.WEST, 'N', false, Style.MELEE);
		assertEquals(Style.MELEE, RangerFlickers.at(north, 0));
		assertEquals(Style.RANGE, RangerFlickers.at(north, 5));
		assertEquals(Style.MAGIC, RangerFlickers.at(north, 7));
		assertEquals(Style.RANGE, RangerFlickers.at(north, 26));
		Crab split = crab(room, 31, 100, Lane.SPLIT, (char) 0, false, Style.MELEE);
		assertEquals(Style.MELEE, RangerFlickers.at(split, 26));
	}

	@Test
	public void classifiesOnlyTrueLaneAnchorsAsLaneBirths()
	{
		assertEquals(Lane.WEST, NylocasRangerPlugin.lane(new WorldPoint(3281, 4249, 0)));
		assertEquals(Lane.EAST, NylocasRangerPlugin.lane(new WorldPoint(3309, 4248, 0)));
		assertEquals(Lane.SOUTH, NylocasRangerPlugin.lane(new WorldPoint(3296, 4233, 0)));
		assertEquals(Lane.SPLIT, NylocasRangerPlugin.lane(new WorldPoint(3295, 4248, 0)));
	}
}
