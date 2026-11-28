package org.localscape.nylocas.mage;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
import static org.localscape.nylocas.mage.MageStrategy.tile;

public class MageStrategyTwoSouthTwentyPipeTest
{
	private final MageStrategy mage = new MageStrategy();
	private final Room room = new Room();
	private Crab south, blue;
	private void setup()
	{
		room.configureStrategy(2); room.wave = 20; room.spawns[20] = 100;
		room.player = tile(3295, 4243); room.ready = 105;
		south = new Crab(94, 100, 100, 20, Lane.SOUTH, 'W', true, Style.RANGE, tile(3295, 4238));
		south.pendingDamage = 13; room.crabs.add(south);
		blue = new Crab(99, 105, 103, 20, Lane.SPLIT, (char) 0, false, Style.MAGIC, tile(3296, 4244));
		blue.pendingDamage = 0; room.crabs.add(blue);
	}
	@Test public void aReadyRangeFivePipeUsesTheOpeningEyesKnownNonlethalRemainder()
	{
		setup(); Decision d = mage.decide(room, 105);
		assertSame(south, d.target); assertEquals(Weapon.PIPE, d.weapon);
	}
	@Test public void aMissedPipeStillLeavesTheMeleeSevenRequestReady()
	{
		setup(); Decision pipe = mage.decide(room, 105); mage.attacked(pipe, 106, 105);
		south.hp = 3; south.pendingDamage = 0; south.style = Style.MELEE; south.point = tile(3295, 4240);
		room.ready = 107;
		Decision d = mage.decide(room, 107);
		assertSame(south, d.target); assertEquals(Weapon.SCYTHE, d.weapon);
	}
	@Test public void aLethalPipeForecastFreesTheAgeSevenEyeSlot()
	{
		setup(); Decision pipe = mage.decide(room, 105); mage.attacked(pipe, 106, 105);
		south.pendingDamage = 18; south.style = Style.MELEE; south.point = tile(3295, 4240); room.ready = 107;
		Decision d = mage.decide(room, 107);
		assertSame(blue, d.target); assertEquals(Weapon.EYE, d.weapon);
	}
	@Test public void foodOrAnEarlierAttackCannotPushThePipeIntoTheNextFlicker()
	{
		setup(); room.ready = 106;
		assertNotEquals(Weapon.PIPE, mage.decide(room, 105).weapon);
		assertNotEquals(Weapon.PIPE, mage.decide(room, 106).weapon);
	}
	@Test public void aKnownLethalOpeningEyeDoesNotReceiveAnExtraPipe()
	{
		setup(); south.pendingDamage = 16;
		assertNotSame(south, mage.decide(room, 105).target);
	}
	@Test public void unknownCoverageRetainsTheOriginalRecovery()
	{
		setup(); south.pendingDamage = -1;
		assertNotEquals(Weapon.PIPE, mage.decide(room, 105).weapon);
	}
	@Test public void versionOneKeepsItsExistingAgeSevenPreparation()
	{
		setup(); room.configureStrategy(1);
		assertNotEquals(Weapon.PIPE, mage.decide(room, 105).weapon);
	}
}
