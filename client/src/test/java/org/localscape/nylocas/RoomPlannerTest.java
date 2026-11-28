package org.localscape.nylocas;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;

import static org.junit.Assert.*;

public class RoomPlannerTest
{
	private static final int NOW = 100;
	private static final Tile CENTER = new Tile(3295, 4248, 0);

	private Room room(int wave, int count)
	{
		Room r = new Room();
		r.wave = wave;
		r.spawns[wave] = NOW;
		r.player = CENTER;
		r.ready = NOW + 30;
		for (int i = 0; i < count; i++)
			r.crabs.add(new Crab(i + 1, i + 1, NOW, wave, Lane.SPLIT, (char) 0, false, Style.RANGE, CENTER));
		return r;
	}

	private RoomProjection.Outcome idle(Room r)
	{
		return new RoomProjection(RoomPlanner.ranger(r, NOW), 0).run(null);
	}

	@Test public void anObservedSplitStepPredictsAtMostTwoTilesAndStopsBeforeAPillar()
	{
		Tile previous = CENTER.dx(-1);
		assertEquals(CENTER.dx(2), AttackTiming.position(CENTER, previous, 3, 1, 5, false));
		assertEquals(CENTER.dx(1), AttackTiming.position(CENTER, previous, 0, 12, 5, true));
		Tile south = new Tile(3292, 4244, 0);
		assertEquals(south, AttackTiming.position(south, south.dx(1), 3, 2, 2, false));
		assertEquals(CENTER, AttackTiming.position(CENTER, CENTER, 3, 4, 2, false));
	}

	@Test public void secondSulphurSplatCanFreeTheTwelfthSlotOneTickEarlier()
	{
		MageStrategy.Room r = new MageStrategy.Room();
		r.wave = 19;
		r.player = CENTER;
		r.ready = NOW;
		r.spawns[19] = NOW - 9; // Next admission at NOW + 3.
		for (int key = 1; key <= 12; key++)
		{
			MageStrategy.Crab c = new MageStrategy.Crab(key, key, NOW - 4, 19, MageStrategy.Lane.SPLIT,
				(char) 0, false, MageStrategy.Style.MELEE, key == 1 ? CENTER.dx(1) : CENTER.dx(4));
			c.nulled = key != 1;
			c.hp = 1;
			r.crabs.add(c);
		}
		RoomPlanner<String> planner = RoomPlanner.mage(r, NOW);
		RoomPlanner.Choice<String> scythe = new RoomPlanner.Choice<>("single splat", 1,
			RoomPlanner.Strike.SCYTHE, NOW, CENTER, false, 0);
		RoomPlanner.Choice<String> sulphur = new RoomPlanner.Choice<>("two splats", 1,
			RoomPlanner.Strike.SULPHUR, NOW, CENTER, false, 0);
		boolean saved = false;
		for (int scenario = 0; scenario < RoomPlanner.SCENARIOS; scenario++)
		{
			int one = new RoomProjection(planner, scenario).run(scythe).transitionStalls;
			int two = new RoomProjection(planner, scenario).run(sulphur).transitionStalls;
			if (one == 1 && two == 0) saved = true;
		}
		assertTrue(saved);
	}

	@Test public void fastWaveTwentyNineLeavesTheNextTicksSplitFreshInTheRollout()
	{
		Room r = room(31, 0);
		r.spawns[28] = 88;
		r.spawns[29] = 92;
		r.ready = NOW;
		Crab original = new Crab(1, 1, 92, 29, Lane.SPLIT, (char) 0, false, Style.RANGE, CENTER);
		r.crabs.add(original);
		assertEquals(0, idle(r).damage);
		Crab child = new Crab(2, 2, 93, 29, Lane.SPLIT, (char) 0, false, Style.RANGE, CENTER.dx(1));
		r.crabs.add(child);
		assertTrue(idle(r).damage > 0);
		r.spawns[29] = -1;
		assertEquals(0, idle(r).damage);
	}

	@Test public void waveTwentyStillNeedsOldCapAndChecksEveryFourTicks()
	{
		Room r = room(19, 12);
		RoomProjection.Outcome full = idle(r);
		assertEquals(19, full.wave);
		assertEquals(2, full.stalls); // last-spawned 19, at +12 and +16 inclusive
		assertEquals(2, full.transitionStalls);
		r.crabs.remove(0);
		RoomProjection.Outcome admitted = idle(r);
		assertEquals(20, admitted.wave);
		assertEquals(14, admitted.occupied); // 11 + all three original bigs
		assertEquals(0, admitted.stalls);
	}

	@Test public void rolledLethalImpactAndBodyRemovalHaveSeparateClocks()
	{
		Room r = room(20, 24);
		Crab small = r.crabs.get(0);
		small.pendingDamage = 8;
		small.observeIncoming(new int[16]);
		small.incomingHits[15] = 8;
		RoomProjection.Outcome late = idle(r);
		assertEquals(1, late.stalls); // death at +15 still occupies the +16 admission
		assertEquals(23, late.living);
		assertEquals(24, late.occupied);
		small.incomingHits[15] = 0;
		small.incomingHits[14] = 8;
		RoomProjection.Outcome early = idle(r);
		assertEquals(0, early.stalls);
		assertEquals(21, early.wave);
		assertEquals(29, early.occupied); // removal before the same-tick check, full six admitted
		assertEquals(8, small.hp);
		assertEquals(24, r.crabs.size());
		assertEquals(-1, r.spawns[21]);
	}

	@Test public void observedSplitOnLastTickReplacesTheParentWithTwoLivingChildren()
	{
		Room r = room(31, 0);
		Crab parent = new Crab(17, 1, 80, 30, Lane.WEST, 'S', true, Style.RANGE, CENTER);
		parent.hp = 0; parent.dying = true;
		r.crabs.add(parent);
		parent.observeSplitAt(NOW + 16);
		RoomProjection.Outcome inside = idle(r);
		assertEquals(1, inside.splits);
		assertEquals(2, inside.living);
		assertEquals(2, inside.occupied);
		parent.observeSplitAt(NOW + 17);
		RoomProjection.Outcome beyond = idle(r);
		assertEquals(0, beyond.splits);
		assertEquals(0, beyond.living);
		assertEquals(1, beyond.occupied);
		assertEquals(0, beyond.emptyCleanupTicks);
	}

	@Test public void aVisibleSmallDeathAnimationCanFreeTheSlotAtTheNextCheck()
	{
		Room r = room(19, 12);
		r.spawns[19] = NOW - 11;
		Crab dying = r.crabs.get(0);
		dying.hp = 0;
		dying.dying = true;
		dying.observeRemovalAt(NOW + 2);
		assertEquals(1, idle(r).transitionStalls);
		dying.observeRemovalAt(NOW + 1);
		assertEquals(0, idle(r).transitionStalls);
	}

	@Test public void rangerTakesTheOtherSplitWhenTheMageIsRunningTowardOne()
	{
		for (int direction : new int[] {-1, 1})
		{
			Room r = room(31, 0);
			r.ready = NOW;
			r.spawns[29] = NOW - 12;
			r.observePartner(CENTER, NOW);
			r.observePartner(CENTER.dx(direction * 2), NOW);
			for (int side : new int[] {-1, 1})
				r.crabs.add(new Crab(side + 2, side + 2, NOW, 31, Lane.SPLIT, (char) 0, false,
					Style.MAGIC, CENTER.dx(side * 3)));
			RoomPlanner<Integer> planner = RoomPlanner.ranger(r, NOW);
			for (Crab c : r.crabs)
				planner.offer(c.key, c.key, RoomPlanner.Strike.EYE, NOW, CENTER, false, 0);
			assertEquals(Integer.valueOf(2 - direction), planner.choose());
			RoomPlanner<Integer> only = RoomPlanner.ranger(r, NOW);
			only.offer(2 + direction, 2 + direction, RoomPlanner.Strike.EYE, NOW, CENTER, false, 0);
			assertEquals(Integer.valueOf(2 + direction), only.choose());
		}
	}

	@Test public void aProjectedStallOpensTimeForAShotBeforeTheShiftedPacket()
	{
		Room r = room(20, 24);
		r.spawns[20] = 97; // first check at 113, retry at 117
		r.ready = 113;
		for (Crab c : r.crabs) c.nulled = true;
		Crab filler = r.crabs.get(0);
		filler.nulled = false;
		filler.hp = 1;
		RoomPlanner<String> planner = RoomPlanner.ranger(r, NOW);
		planner.reserve(113, CENTER, RoomPlanner.Strike.CHIN, 21, Lane.EAST.ordinal());
		int kills = 0;
		for (int i = 0; i < RoomPlanner.SCENARIOS; i++)
		{
			RoomProjection.Outcome end = new RoomProjection(planner, i).run(null);
			assertEquals(20, end.wave);
			assertEquals(1, end.stalls);
			kills += end.damage;
		}
		// The east pack is still outside. A two-tick pipe fits before its revised opening.
		assertTrue(kills > 0);
		assertEquals(1, filler.hp);
	}

	@Test public void splitScenariosCoverAllThreeStylesAndDifferentPairs()
	{
		int[][] counts = new int[2][3];
		boolean[] pairs = new boolean[9];
		for (int i = 0; i < RoomPlanner.SCENARIOS; i++)
		{
			int a = RoomProjection.splitStyle(17, 0, i), b = RoomProjection.splitStyle(17, 1, i);
			counts[0][a]++; counts[1][b]++;
			assertFalse(pairs[a * 3 + b]);
			pairs[a * 3 + b] = true;
		}
		for (int[] count : counts) assertArrayEquals(new int[] {2, 2, 2}, count);
	}

	@Test public void aPreparationCanCostAnAdmissionWhileThePartnerRemainsFreeToShoot()
	{
		Room r = room(19, 12);
		Crab small = r.crabs.get(0);
		small.hp = 1;
		r.ready = NOW;
		RoomPlanner<String> planner = RoomPlanner.ranger(r, NOW);
		planner.prepare("stage", NOW + 13, CENTER.dx(3), 0);
		planner.offer("shoot", small.key, RoomPlanner.Strike.PIPE, NOW, CENTER, false, 0);
		assertEquals("shoot", planner.choose());
		RoomPlanner.Choice<String> hold = new RoomPlanner.Choice<>("stage", 0, null, NOW + 13, CENTER.dx(3), false, 0);
		assertTrue(new RoomProjection(planner, 0).run(hold).transitionStalls > 0);
		r.observePartner(CENTER, NOW);
		assertEquals(0, new RoomProjection(RoomPlanner.ranger(r, NOW), 0).run(hold).transitionStalls);
	}

	@Test public void stagingForEastElevenIncludesTheBarrageOnTheIncomingPair()
	{
		MageStrategy.Room r = new MageStrategy.Room();
		r.wave = 10;
		r.spawns[10] = NOW - 7;
		r.ready = NOW + 11;
		r.player = CENTER.dx(6).dy(1);
		RoomPlanner<String> planner = RoomPlanner.mage(r, NOW);
		planner.prepare("wait", NOW + 1, r.player, 0);
		planner.prepareSpawn("east barrage", NOW + 1, r.player, 0, RoomPlanner.Strike.BARRAGE,
			11, MageStrategy.Lane.EAST.ordinal(), (char) 0, false);
		assertEquals("east barrage", planner.choose());
	}

	@Test public void aStalledWaveCannotSupplyTheStagedTarget()
	{
		Room r = room(10, 12);
		r.spawns[10] = NOW - 7;
		r.ready = NOW;
		for (Crab c : r.crabs) c.nulled = true;
		RoomPlanner<String> planner = RoomPlanner.ranger(r, NOW);
		planner.prepare("wait", NOW + 1, CENTER, 0.1);
		planner.prepareSpawn("west shot", NOW + 1, CENTER, 0, RoomPlanner.Strike.EYE,
			11, Lane.WEST.ordinal(), (char) 0, true);
		assertEquals("wait", planner.choose());
	}

	@Test public void lowRollLeavesASurvivorAndReplanningDoesNotChangeTheObservation()
	{
		Room r = room(31, 0);
		Crab big = new Crab(10, 10, NOW, 31, Lane.SPLIT, (char) 0, true, Style.RANGE, CENTER);
		r.crabs.add(big);
		// Launch at the end of the horizon. Its impact is visible before another shot can land.
		r.ready = NOW + 13;
		RoomPlanner<String> p = RoomPlanner.ranger(r, NOW);
		RoomPlanner.Choice<String> first = new RoomPlanner.Choice<>("pipe", 10, RoomPlanner.Strike.PIPE,
			r.ready, CENTER, false, 0);
		boolean survivor = false, lethal = false;
		for (int i = 0; i < RoomPlanner.SCENARIOS; i++)
		{
			RoomProjection.Outcome end = new RoomProjection(p, i).run(first);
			survivor |= end.living == 1; lethal |= end.living == 0;
			assertEquals(end.damage, new RoomProjection(p, i).run(first).damage);
		}
		assertTrue(survivor);
		assertTrue(lethal);
		assertEquals(16, big.hp);
		assertFalse(big.dying);
	}

	@Test public void allForecastArrivalsMatchTheNativeWaveFixture() throws Exception
	{
		String relative = "upstream/rsmod/content/areas/dungeons/nylocas/src/integration/resources/nylocas-waves.csv";
		Path root = Paths.get("").toAbsolutePath();
		while (root != null && !Files.exists(root.resolve(relative))) root = root.getParent();
		assertNotNull(root);
		List<String> lines = Files.readAllLines(root.resolve(relative));
		List<String> slots = Arrays.asList("WestSouth", "WestNorth", "SouthWest", "SouthEast", "EastSouth", "EastNorth");
		boolean[][] seen = new boolean[32][6];
		for (String line : lines.subList(1, lines.size()))
		{
			String[] v = line.split(",", -1);
			int wave = Integer.parseInt(v[0]), index = slots.indexOf(v[2]);
			char type = WaveSchedule.spawn(wave, index);
			assertEquals(Integer.parseInt(v[1]), WaveSchedule.delayAfter(wave - 1));
			assertEquals(v[5].equals("Big"), Character.isUpperCase(type));
			assertEquals(Arrays.asList("Melee", "Ranged", "Magic").indexOf(v[6]), WaveSchedule.style(type));
			char physical = index / 2 == 1 ? (v[3].equals("3295") ? 'W' : 'E') : (v[4].equals("4249") ? 'N' : 'S');
			assertEquals(physical, WaveSchedule.slot(wave, index));
			seen[wave][index] = true;
		}
		for (int wave = 1; wave <= 31; wave++)
			for (int index = 0; index < 6; index++) assertEquals(seen[wave][index], WaveSchedule.spawn(wave, index) != '.');
	}
	@Test public void strategyTwoProjectileBoundariesMatchNativeTravelAndDamageQueue()
	{
		assertEquals(3, RoomProjection.flight(RoomPlanner.Strike.PIPE, 5, true));
		assertEquals(4, RoomProjection.flight(RoomPlanner.Strike.PIPE, 6, true));
		assertEquals(3, RoomProjection.flight(RoomPlanner.Strike.EYE, 4, true));
		assertEquals(4, RoomProjection.flight(RoomPlanner.Strike.EYE, 5, true));
		assertEquals(3, RoomProjection.flight(RoomPlanner.Strike.PIPE, 6, false));
		assertEquals(4, RoomProjection.flight(RoomPlanner.Strike.EYE, 4, false));
	}

	@Test public void projectedTomeDamageStacksSpecialAndStoredEmpowerment()
	{
		assertEquals(32, RoomProjection.tomeMaxHit(32, false, false));
		assertEquals(51, RoomProjection.tomeMaxHit(32, true, false));
		assertEquals(51, RoomProjection.tomeMaxHit(32, false, true));
		assertEquals(70, RoomProjection.tomeMaxHit(32, true, true));
	}

}
