package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerSouthSixOwnershipTest
{
	private final RangerStrategy ranger = new RangerStrategy();

	private Crab add(Room room, int key, int born, int wave, Lane lane, char slot, boolean big, Style style, int x, int y)
	{
		Crab crab = new Crab(key, key, born, wave, lane, slot, big, style, RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void southStagingKeepsTheEyeAndEnablesSouth6ChinBeforeBow7AndClaws6()
	{
		Room room = new Room();
		room.wave = 5;
		room.spawns[5] = 20;
		room.player = RangerStrategy.tile(3293, 4246);
		room.ready = 34;
		Crab magic = add(room, 1, 32, 5, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3289, 4248);
		Decision stage = ranger.decide(room, 33);
		assertTrue(stage.reason, stage.reason.startsWith("Stage South6"));
		room.player = stage.tile;
		Decision eye = ranger.decide(room, 34);
		assertSame(magic, eye.target);
		ranger.attacked(eye, 35);
		magic.hp = 3;
		magic.pendingDamage = 0;
		room.ready = 37;
		room.player = ranger.decide(room, 35).tile;
		room.wave = 6;
		room.spawns[6] = 36;
		Crab south6 = add(room, 2, 36, 6, Lane.SOUTH, 'W', false, Style.RANGE, 3295, 4234);
		Crab east6 = add(room, 3, 36, 6, Lane.EAST, 'S', true, Style.MELEE, 3308, 4248);
		Decision chin = ranger.decide(room, 37);
		assertSame(chin.reason, south6, chin.target);
		assertEquals(Weapon.CHIN, chin.weapon);
		ranger.attacked(chin, 38);
		south6.pendingDamage = south6.hp;
		room.ready = 40;
		room.wave = 7;
		room.spawns[7] = 40;
		Crab south7 = add(room, 4, 40, 7, Lane.SOUTH, 'W', true, Style.RANGE, 3295, 4233);
		Decision bow = ranger.decide(room, 40);
		assertSame(bow.reason, south7, bow.target);
		assertEquals(Weapon.BOW, bow.weapon);
		ranger.attacked(bow, 41);
		south7.pendingDamage = south7.hp;
		room.ready = 45;
		room.specialEnergy = 1000;
		room.player = RangerStrategy.tile(3301, 4246);
		east6.observePosition(RangerStrategy.tile(3300, 4247));
		Decision claws = ranger.decide(room, 45);
		assertSame(claws.reason, east6, claws.target);
		assertEquals(Weapon.CLAWS, claws.weapon);
		assertTrue(claws.special);
	}

	@Test
	public void south6CanUseTheSameTickPipeBeforeAStillReachableSouth7Bow()
	{
		Room room = new Room();
		room.wave = 7;
		room.spawns[6] = 36;
		room.spawns[7] = 40;
		room.player = RangerStrategy.tile(3295, 4243);
		Crab small = add(room, 1, 36, 6, Lane.SOUTH, 'W', false, Style.RANGE, 3295, 4237);
		add(room, 2, 40, 7, Lane.SOUTH, 'W', true, Style.RANGE, 3295, 4233);
		Decision pipe = ranger.decide(room, 40);
		assertSame(pipe.reason, small, pipe.target);
		assertEquals(Weapon.PIPE, pipe.weapon);
	}

	@Test
	public void aSouth6ShotThatWouldMissTheBowWindowDoesNotPreemptIt()
	{
		Room room = new Room();
		room.wave = 7;
		room.spawns[6] = 36;
		room.spawns[7] = 40;
		room.player = RangerStrategy.tile(3295, 4243);
		add(room, 1, 36, 6, Lane.SOUTH, 'W', false, Style.RANGE, 3295, 4240);
		Crab big = add(room, 2, 40, 7, Lane.SOUTH, 'W', true, Style.RANGE, 3295, 4236);
		assertSame(big, ranger.decide(room, 43).target);
	}

	@Test
	public void anUnfinishedWest5KeepsItsGroundAndFollowup()
	{
		Room room = new Room();
		room.wave = 5;
		room.spawns[5] = 20;
		room.ready = 34;
		room.player = RangerStrategy.tile(3290, 4248);
		Crab west = add(room, 1, 20, 5, Lane.WEST, 'S', true, Style.RANGE, 3291, 4248);
		ranger.attacked(new Decision(west, Weapon.PIPE, room.player, false, "West big 5", "Previous pipe"), 32);
		west.hp = 3;
		west.pendingDamage = 0;
		assertFalse(ranger.decide(room, 33).reason.startsWith("Stage South6"));
		assertSame(west, ranger.decide(room, 34).target);
	}

	@Test
	public void theNamedEast8ChinDoesNotNeedAPairWhenPipeCannotLaunchYet()
	{
		Room room = new Room();
		room.wave = 8;
		room.spawns[8] = 52;
		room.player = RangerStrategy.tile(3300, 4248);
		Crab east = add(room, 1, 52, 8, Lane.EAST, 'S', false, Style.RANGE, 3310, 4248);
		Decision chin = ranger.decide(room, 52);
		assertSame(chin.reason, east, chin.target);
		assertEquals(Weapon.CHIN, chin.weapon);
	}
}
