package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

/** Seed143 local158: after-cycle lease telemetry is not a planning-input snapshot. */
public class West19PhaseReplayTest
{
	@Test
	public void predecisionLeaseBlocksTheWindowThatPostcycleTelemetryFalselyAdmits()
	{
		RangerStrategy strategy = new RangerStrategy();
		Room room = new Room();
		room.wave = 19;
		room.spawns[18] = 140;
		room.spawns[19] = 148;
		room.ready = 158;
		room.player = RangerStrategy.tile(3293, 4249);
		Crab west = new Crab(90, 96, 148, 19, Lane.WEST, 'S', true, Style.MAGIC,
			RangerStrategy.tile(3291, 4249));
		room.crabs.add(west);
		// NativePRIMARY151 confirmed this exact target/rule, and its hit already resolved.
		strategy.attacked(new Decision(west, Weapon.EYE, room.player, false, "West magic big 19", "Opening"), 152);
		west.hp = 8;
		west.ownHitUntil = 0;
		Crab east = new Crab(87, 93, 140, 18, Lane.EAST, 'S', true, Style.RANGE,
			RangerStrategy.tile(3292, 4253));
		east.hp = 16;
		east.ownHitHp = 0;
		room.crabs.add(east);
		// Prior TRACE157 retained the156 launch's188 lease: this is helper158's input.
		east.ownHitUntil = 188;
		assertEquals(-1, strategy.west19PacketDue(room, 158));
		// TRACE158 prints only after native processing and releaseAppliedFlights.
		// Mixing that cleared lease with its earlier HP/position fields invents a window.
		east.ownHitUntil = 0;
		assertEquals(158, strategy.west19PacketDue(room, 158));
	}
}
