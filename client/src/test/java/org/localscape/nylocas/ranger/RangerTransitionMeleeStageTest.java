package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerTransitionMeleeStageTest
{
	private final Room room = new Room();
	private final RangerStrategy ranger = new RangerStrategy();
	private final Crab west;
	private final Crab south;

	public RangerTransitionMeleeStageTest()
	{
		room.wave = 18;
		room.spawns[18] = 136;
		room.ready = 147;
		room.player = RangerStrategy.tile(3296, 4247);
		west = add(83, 136, 18, Lane.WEST, true, Style.RANGE, 3291, 4247);
		south = add(84, 136, 18, Lane.SOUTH, true, Style.RANGE, 3295, 4243);
		add(85, 136, 18, Lane.EAST, true, Style.RANGE, 3299, 4250);
		add(70, 112, 15, Lane.SPLIT, false, Style.MELEE, 3290, 4252);
		add(81, 135, 17, Lane.SPLIT, false, Style.MELEE, 3291, 4252);
		add(72, 112, 15, Lane.SOUTH, true, Style.MAGIC, 3292, 4244);
		add(78, 124, 17, Lane.WEST, true, Style.MAGIC, 3290, 4245);
		add(79, 124, 17, Lane.SOUTH, true, Style.MAGIC, 3298, 4243);
		add(80, 124, 17, Lane.EAST, true, Style.MAGIC, 3300, 4251);
		for (int i = 0; i < 3; i++)
		{
			Crab dying = add(100 + i, 120, 16, Lane.SPLIT, false, Style.RANGE, 3301, 4252);
			dying.hp = 0;
			dying.dying = true;
		}
	}

	private Crab add(int key, int born, int wave, Lane lane, boolean big, Style style, int x, int y)
	{
		Crab crab = new Crab(key, key, born, wave, lane, lane == Lane.SOUTH ? 'W' : 'S', big, style,
			RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void aHeldParentGapCanPrepareAQuickMeleeWithoutDroppingTheWestShot()
	{
		// Native failure239959028216592: the northwest smalls otherwise remain unreachable.
		Decision first = ranger.decide(room, 146);
		assertTrue(first.reason, first.reason.startsWith("Stage local melee"));
		room.player = first.tile;
		room.crabs.removeIf(c -> c.dying);
		west.observePosition(RangerStrategy.tile(3292, 4247));
		Decision second = ranger.decide(room, 147);
		assertTrue(second.reason, second.reason.startsWith("Stage local melee"));
		room.player = second.tile;
		room.wave = 19;
		room.spawns[19] = 148;
		west.observePosition(RangerStrategy.tile(3293, 4247));
		Decision parent = ranger.decide(room, 148);
		assertSame(parent.reason, west, parent.target);
		assertEquals(Weapon.PIPE, parent.weapon);
		ranger.attacked(parent, 149);
		west.pendingDamage = 23;
		room.ready = 150;
		Decision approach = ranger.decide(room, 149);
		assertNotNull(approach.reason, approach.approach);
		assertEquals(Weapon.SULPHUR, approach.weapon);
		room.player = approach.tile;
		Decision melee = ranger.decide(room, 150);
		assertNotNull(melee.target);
		assertFalse(melee.target.big);
		assertEquals(Weapon.SULPHUR, melee.weapon);
	}

	@Test
	public void aLateMeleeCannotConsumeTheRemainingRangeParentServiceBudget()
	{
		room.wave = 19;
		room.spawns[19] = 148;
		room.ready = 154;
		west.pendingDamage = west.hp;
		room.player = RangerStrategy.tile(3292, 4251);
		Decision decision = ranger.decide(room, 154);
		assertTrue(decision.reason, decision.target != null && decision.target.big);
		assertEquals(Weapon.PIPE, decision.weapon);
	}
}
