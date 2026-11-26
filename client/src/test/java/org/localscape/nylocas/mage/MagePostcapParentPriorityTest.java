package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;

import static org.junit.Assert.*;

public class MagePostcapParentPriorityTest
{
	@Test
	public void anOlderRequiredParentGetsItsLastTwoScytheOpportunitiesBeforeAYoungerFullParent()
	{
		// Seed135959225567683 at253: opening West25 here left East24 untouched until expiry.
		Room room = new Room();
		room.wave = 29;
		room.spawns[28] = 240;
		room.spawns[29] = 252;
		room.ready = 253;
		room.player = MageStrategy.tile(3293, 4245);
		Crab east = new Crab(140, 140, 212, 24, Lane.EAST, 'S', true, Style.MELEE,
			MageStrategy.tile(3294, 4245));
		Crab west = new Crab(143, 143, 220, 25, Lane.WEST, 'S', true, Style.MELEE,
			MageStrategy.tile(3293, 4245));
		Crab south = new Crab(172, 172, 252, 29, Lane.SOUTH, 'W', true, Style.MELEE,
			MageStrategy.tile(3295, 4234));
		east.pendingDamage = west.pendingDamage = south.pendingDamage = 0;
		room.crabs.add(east);
		room.crabs.add(west);
		room.crabs.add(south);
		Decision attack = new MageStrategy().decide(room, 253);
		assertSame(east, attack.target);
		assertEquals(Weapon.SCYTHE, attack.weapon);
	}

	@Test
	public void southTwentyKeepsItsAssignedMeleePriorityAfterTheWaveCounterMoves()
	{
		Room room = new Room();
		room.wave = 21;
		room.spawns[21] = 168;
		room.player = MageStrategy.tile(3295, 4247);
		Crab south = new Crab(96, 96, 152, 20, Lane.SOUTH, 'W', true, Style.MELEE,
			MageStrategy.tile(3294, 4245));
		south.pendingDamage = 0;
		room.crabs.add(south);
		Crab small = new Crab(97, 97, 176, 21, Lane.SPLIT, (char) 0, false, Style.MELEE,
			MageStrategy.tile(3295, 4248));
		small.pendingDamage = 0;
		room.crabs.add(small);
		MageStrategy strategy = new MageStrategy();
		strategy.attacked(new Decision(south, Weapon.EYE, room.player, false, "South opener 20", "opening"), 154, 153);
		strategy.attacked(new Decision(small, Weapon.BARRAGE, room.player, false, "21 west barrage", "barrage"), 169, 168);
		strategy.attacked(new Decision(small, Weapon.SULPHUR, room.player, false, "21 north sulphur", "north follow-up"), 174, 173);
		Decision attack = strategy.decide(room, 176);
		assertSame(south, attack.target);
		assertEquals(Weapon.SCYTHE, attack.weapon);
	}

	@Test
	public void southTwentyCanUseTheNormalPipeInItsPermanentRangePhase()
	{
		Room room = new Room();
		room.wave = 22;
		room.spawns[22] = 176;
		room.player = MageStrategy.tile(3295, 4248);
		Crab south = new Crab(96, 96, 152, 20, Lane.SOUTH, 'W', true, Style.RANGE,
			MageStrategy.tile(3295, 4244));
		south.pendingDamage = 0;
		room.crabs.add(south);
		Crab small = new Crab(97, 97, 178, 22, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			MageStrategy.tile(3296, 4248));
		small.pendingDamage = 0;
		room.crabs.add(small);
		Decision attack = new MageStrategy().decide(room, 178);
		assertSame(south, attack.target);
		assertEquals(Weapon.PIPE, attack.weapon);
	}

	@Test
	public void preparesAnAssignedParentBeforeTakingAnImmediateSmallFiller()
	{
		Room room = new Room();
		room.wave = 27;
		room.spawns[27] = 220;
		room.player = MageStrategy.tile(3290, 4248);
		Crab parent = new Crab(1, 1, 196, 24, Lane.EAST, 'S', true, Style.MELEE,
			MageStrategy.tile(3300, 4248));
		Crab filler = new Crab(2, 2, 219, 27, Lane.SPLIT, (char) 0, false, Style.MELEE,
			MageStrategy.tile(3291, 4248));
		parent.pendingDamage = filler.pendingDamage = 0;
		room.crabs.add(parent);
		room.crabs.add(filler);

		Decision move = new MageStrategy().decide(room, 220);
		assertSame(parent, move.target != null ? move.target : move.approach);
	}

	@Test
	public void southTwentyNineWinsOverANewerSmallDuringActualThirty()
	{
		Room room = new Room();
		room.wave = 30;
		room.spawns[29] = 240;
		room.spawns[30] = 244;
		room.player = MageStrategy.tile(3299, 4249);
		Crab parent = new Crab(1, 1, 240, 29, Lane.SOUTH, 'W', true, Style.MELEE,
			MageStrategy.tile(3295, 4243));
		Crab filler = new Crab(2, 2, 248, 30, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			MageStrategy.tile(3300, 4249));
		parent.pendingDamage = filler.pendingDamage = 0;
		room.crabs.add(parent);
		room.crabs.add(filler);

		Decision move = new MageStrategy().decide(room, 248);
		assertSame(parent, move.target != null ? move.target : move.approach);
	}

	@Test
	public void actualThirtyOneKeepsOnlyTheRequiredParentExceptionsToTheBirthCutoff()
	{
		int[] waves = {23, 24, 24, 24, 25, 26, 29, 30};
		Lane[] lanes = {Lane.SOUTH, Lane.WEST, Lane.SOUTH, Lane.EAST, Lane.WEST, Lane.WEST, Lane.SOUTH, Lane.EAST};
		Style[] styles = {Style.MELEE, Style.MELEE, Style.MAGIC, Style.MELEE, Style.MELEE, Style.MAGIC, Style.MELEE, Style.MAGIC};
		for (int i = 0; i < waves.length; i++)
		{
			Room room = new Room();
			room.wave = 31;
			room.spawns[28] = 228;
			room.spawns[29] = 240;
			room.spawns[30] = 244;
			room.spawns[31] = 248;
			room.player = MageStrategy.tile(3295, 4248);
			int born = waves[i] <= 26 ? 224 : waves[i] == 29 ? 240 : 244;
			Crab parent = new Crab(1, 1, born, waves[i], lanes[i], 'S', true, styles[i],
				MageStrategy.tile(3294, 4246));
			parent.pendingDamage = 0;
			room.crabs.add(parent);
			Decision attack = new MageStrategy().decide(room, 248);
			assertSame("required parent " + waves[i] + lanes[i], parent, attack.target);
			parent.nulled = true;
			assertNull(new MageStrategy().decide(room, 248).target);
		}

		Room room = new Room();
		room.wave = 31;
		room.spawns[29] = 240;
		Crab oldSplit = new Crab(2, 2, 240, 29, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			room.player.dy(1));
		room.crabs.add(oldSplit);
		assertNull(new MageStrategy().decide(room, 248).target);
	}

	@Test
	public void cleanupSweepCannotSpendTheReadyTickOnSmallsBeforeAnAssignedParent()
	{
		Room room = new Room();
		room.wave = 31;
		room.spawns[29] = 240;
		room.specialEnergy = 1000;
		room.player = MageStrategy.tile(3295, 4248);
		Crab parent = new Crab(1, 1, 240, 29, Lane.SOUTH, 'W', true, Style.MELEE,
			MageStrategy.tile(3294, 4246));
		parent.pendingDamage = 0;
		room.crabs.add(parent);
		for (int i = 0; i < 2; i++)
		{
			Crab small = new Crab(2 + i, 2 + i, 248, 31, Lane.SPLIT, (char) 0, false, Style.MELEE,
				MageStrategy.tile(3294 + i, 4249));
			small.pendingDamage = 0;
			room.crabs.add(small);
		}
		Decision attack = new MageStrategy().decide(room, 248);
		assertSame(parent, attack.target);
		assertEquals(Weapon.SCYTHE, attack.weapon);
	}

	@Test
	public void anUnopenedEastThirtySurvivorRetainsRecoveryPastItsOpeningDeadline()
	{
		Room room = new Room();
		room.wave = 31;
		room.spawns[29] = 240;
		room.spawns[30] = 244;
		room.spawns[31] = 248;
		room.player = MageStrategy.tile(3295, 4248);
		Crab parent = new Crab(1, 1, 244, 30, Lane.EAST, 'S', true, Style.MAGIC,
			MageStrategy.tile(3299, 4248));
		parent.pendingDamage = 0;
		room.crabs.add(parent);
		Decision recovery = new MageStrategy().decide(room, 274);
		assertSame(parent, recovery.target);
		assertEquals(Weapon.EYE, recovery.weapon);

		parent.pendingDamage = parent.hp;
		assertNull(new MageStrategy().decide(room, 274).target);
		parent.pendingDamage = 0;
		assertNull(new MageStrategy().decide(room, 293).target);
	}
}
