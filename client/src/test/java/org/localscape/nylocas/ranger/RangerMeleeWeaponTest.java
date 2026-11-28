package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;

/** Ordinary strength-stance sulphur and charged claw swaps share the same melee clothing. */
public class RangerMeleeWeaponTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();

	private Crab red(int key, int wave, int born, Lane lane, boolean big, int x, int y)
	{
		Crab crab = new Crab(key, key, born, wave, lane, 'S', big, Style.MELEE, RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	private Crab eastSix()
	{
		room.wave = 7; room.spawns[6] = 36; room.spawns[7] = 40;
		room.ready = 45; room.player = RangerStrategy.tile(3301, 4246);
		return red(1, 6, 36, Lane.EAST, true, 3300, 4247);
	}

	private Crab cleanup()
	{
		room.wave = 31; room.spawns[29] = 244; room.spawns[31] = 260;
		room.player = RangerStrategy.tile(3294, 4248); room.cleanup = true;
		return red(1, 31, 272, Lane.SPLIT, false, 3295, 4248);
	}

	@Test public void sulphurUsesTheNativeFourTickStrengthStance()
	{
		assertEquals(29084, Weapon.SULPHUR.itemId);
		assertEquals(Style.MELEE, Weapon.SULPHUR.style);
		assertEquals(4, Weapon.SULPHUR.cooldown);
		assertEquals(1, Weapon.SULPHUR.range);
		assertEquals(1, Weapon.SULPHUR.stance);
		assertEquals(13652, Weapon.CLAWS.itemId);
	}

	@Test public void theOpeningScratchUsesSulphurAndKeepsItsNamedWindow()
	{
		Rule scratch = RangerStrategy.RULES.stream().filter(r -> r.name.equals("Opening scratch")).findFirst().get();
		assertEquals(Weapon.SULPHUR, scratch.weapon);
		assertEquals(9, scratch.first); assertEquals(13, scratch.last); assertFalse(scratch.special);
	}

	@Test public void ordinaryPrecapMeleeDoesNotSpendOrEquipClaws()
	{
		room.wave = 5; room.spawns[5] = 20; room.player = RangerStrategy.tile(3295, 4247);
		room.specialEnergy = 1000;
		Crab crab = red(1, 5, 29, Lane.SPLIT, false, 3296, 4247);
		Decision hit = ranger.decide(room, 30);
		assertSame(crab, hit.target); assertEquals(Weapon.SULPHUR, hit.weapon); assertFalse(hit.special);
	}

	@Test public void theRequiredEastSixFirstSwingIsStillAChargedClawSwap()
	{
		Crab crab = eastSix(); room.specialEnergy = 500;
		Decision hit = ranger.decide(room, 45);
		assertSame(crab, hit.target); assertEquals(Weapon.CLAWS, hit.weapon); assertTrue(hit.special);
		assertEquals("East big claw special", hit.rule);
	}

	@Test public void insufficientEastSixEnergyUsesOrdinarySulphur()
	{
		Crab crab = eastSix(); room.specialEnergy = 499;
		Decision hit = ranger.decide(room, 45);
		assertSame(crab, hit.target); assertEquals(Weapon.SULPHUR, hit.weapon); assertFalse(hit.special);
	}

	@Test public void aSurvivingEastSixReturnsToSulphurAfterTheConfirmedSpecial()
	{
		Crab crab = eastSix(); room.specialEnergy = 1000;
		Decision first = ranger.decide(room, 45); ranger.attacked(first, 45);
		crab.hp = 8; crab.pendingDamage = 0; room.ready = 49;
		Decision next = ranger.decide(room, 49);
		assertSame(crab, next.target); assertEquals(Weapon.SULPHUR, next.weapon); assertFalse(next.special);
	}

	@Test public void anEligibleLateSpecialExplicitlySwapsFromSulphurToClaws()
	{
		Crab crab = cleanup(); room.specialEnergy = 500;
		Decision hit = ranger.decide(room, 276);
		assertSame(crab, hit.target); assertEquals(Weapon.CLAWS, hit.weapon); assertTrue(hit.special);
	}

	@Test public void ordinaryLateMeleeStaysSulphurBelowTheSpecialBudget()
	{
		Crab crab = cleanup(); room.specialEnergy = 499;
		Decision hit = ranger.decide(room, 276);
		assertSame(crab, hit.target); assertEquals(Weapon.SULPHUR, hit.weapon); assertFalse(hit.special);
	}

	@Test public void aThreeHpLateSurvivorDoesNotEquipClawsJustBecauseEnergyRemains()
	{
		Crab crab = cleanup(); room.specialEnergy = 1000; crab.hp = 3;
		Decision hit = ranger.decide(room, 276);
		assertSame(crab, hit.target); assertEquals(Weapon.SULPHUR, hit.weapon); assertFalse(hit.special);
	}
}
