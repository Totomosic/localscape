package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerStrategyVersionTwoTest
{
	private final RangerStrategy strategy = new RangerStrategy();
	private int key;

	private Room room(int wave)
	{
		Room room = new Room();
		room.configureStrategy(2);
		room.wave = wave;
		room.spawns[wave] = 100;
		room.player = RangerStrategy.tile(3295, 4248);
		room.specialEnergy = 1000;
		return room;
	}

	private Crab crab(Room room, int wave, Lane lane, char slot, boolean big, Style style, int x, int y)
	{
		Crab c = new Crab(++key, key, 100, wave, lane, slot, big, style, RangerStrategy.tile(x, y));
		c.pendingDamage = 0;
		room.crabs.add(c);
		return c;
	}

	@Test
	public void effectiveGearIsPerRoomAndLegacyDefaultsRemainUnchanged()
	{
		Room legacy = new Room();
		Room versionTwo = room(7);
		assertEquals(12926, legacy.itemId(Weapon.PIPE));
		assertEquals(5, legacy.range(Weapon.PIPE));
		assertEquals(31113, legacy.itemId(Weapon.EYE));
		assertEquals(28688, versionTwo.itemId(Weapon.PIPE));
		assertEquals(6, versionTwo.range(Weapon.PIPE));
		assertEquals(34725, versionTwo.itemId(Weapon.EYE));
		assertEquals(8, versionTwo.range(Weapon.EYE));
		assertEquals(3, versionTwo.cooldown(Weapon.EYE));
		versionTwo.observeTomeCharges(3);
		assertEquals(2, versionTwo.cooldown(Weapon.EYE));
		assertEquals(3, legacy.cooldown(Weapon.EYE));
		versionTwo.observeTomeCharges(0);
		assertEquals(3, versionTwo.cooldown(Weapon.EYE));
	}

	@Test
	public void southThreeUsesCrossbowsAfterTheOpeningBows()
	{
		Room room = room(3);
		Crab east = crab(room, 2, Lane.EAST, 'S', false, Style.RANGE, 3300, 4248);
		strategy.attacked(new Decision(east, Weapon.BOW, room.player, false, "Opening east", "test"), 99);
		east.dying = true;
		room.player = RangerStrategy.tile(3296, 4244);
		Crab south = crab(room, 3, Lane.SOUTH, 'E', false, Style.RANGE, 3296, 4237);
		Decision action = strategy.decide(room, 102);
		assertSame(action.reason, south, action.target);
		assertEquals(Weapon.PIPE, action.weapon);
		assertFalse(action.special);
	}

	@Test
	public void westSixAndSouthSevenMagicReplaceEastSixClaws()
	{
		Room room = room(7);
		crab(room, 6, Lane.EAST, 'S', true, Style.MELEE, 3297, 4248);
		Crab west = crab(room, 6, Lane.WEST, 'N', false, Style.MAGIC, 3291, 4249);
		Crab south = crab(room, 7, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4245);
		crab(room, 7, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3299, 4248);
		Decision action = strategy.decide(room, 110);
		assertSame(action.reason, south, action.target);
		assertEquals(Weapon.EYE, action.weapon);
		assertTrue(action.special);
		strategy.attacked(action, 110);
		south.dying = true;
		room.observeTomeCharges(3);
		room.specialEnergy = 500;
		action = strategy.decide(room, 112);
		assertSame(action.reason, west, action.target);
		assertEquals(Weapon.EYE, action.weapon);
		assertFalse(action.special);
	}

	@Test
	public void eastSixIsNeverAClawDutyInVersionTwo()
	{
		Room room = room(7);
		room.spawns[6] = 100;
		Crab east = crab(room, 6, Lane.EAST, 'S', true, Style.MELEE, 3297, 4248);
		Decision action = strategy.decide(room, 110);
		assertNotSame(action.reason, east, action.target);
		assertNotEquals(Weapon.CLAWS, action.weapon);
	}

	@Test
	public void tomeQuotasRequireConfirmedAttacksAndReserveWestEleven()
	{
		Room room = room(7);
		Crab south = crab(room, 7, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4245);
		crab(room, 6, Lane.WEST, 'N', false, Style.MAGIC, 3291, 4249);
		crab(room, 7, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3299, 4248);
		Decision first = strategy.decide(room, 110);
		assertTrue(first.special);
		assertTrue(strategy.decide(room, 110).special);
		strategy.attacked(first, 110);
		south.ownHitUntil = 0;
		assertFalse(strategy.decide(room, 112).special);
		room.crabs.clear();
		room.wave = 11;
		room.spawns[11] = 100;
		room.specialEnergy = 500;
		Crab west = crab(room, 11, Lane.WEST, 'S', true, Style.MAGIC, 3291, 4248);
		Decision eleven = strategy.decide(room, 104);
		assertSame(eleven.reason, west, eleven.target);
		assertTrue(eleven.special);
		strategy.attacked(eleven, 104);
		west.ownHitUntil = 0;
		assertFalse(strategy.decide(room, 107).special);
	}

	@Test
	public void westElevenFinishesWithChargedTomeBeforeOtherWork()
	{
		Room room = room(12);
		room.spawns[11] = 100;
		room.observeTomeCharges(2);
		Crab west = crab(room, 11, Lane.WEST, 'S', true, Style.MAGIC, 3291, 4248);
		west.hp = 3;
		crab(room, 12, Lane.SPLIT, (char) 0, false, Style.RANGE, 3295, 4249);
		Decision action = strategy.decide(room, 115);
		assertSame(action.reason, west, action.target);
		assertEquals(Weapon.EYE, action.weapon);
		assertTrue(action.special);
	}

	@Test
	public void westTwentyFourGetsTwoCrossbowsThenSouthGetsTheSpecial()
	{
		Room room = room(24);
		room.player = RangerStrategy.tile(3291, 4248);
		Crab west = crab(room, 24, Lane.WEST, 'S', true, Style.RANGE, 3285, 4248);
		Crab south = crab(room, 24, Lane.SOUTH, 'E', true, Style.MAGIC, 3295, 4244);
		Decision first = strategy.decide(room, 101);
		assertSame(first.reason, west, first.target);
		assertEquals(Weapon.PIPE, first.weapon);
		assertFalse(first.special);
		strategy.attacked(first, 101);
		west.hp = 8;
		west.ownHitUntil = 0;
		Decision second = strategy.decide(room, 103);
		assertSame(second.reason, west, second.target);
		assertEquals(Weapon.PIPE, second.weapon);
		assertFalse(second.special);
		strategy.attacked(second, 103);
		west.pendingDamage = west.hp;
		room.observeTomeCharges(0);
		Decision followup = strategy.decide(room, 105);
		assertSame(followup.reason, south, followup.target);
		assertEquals(Weapon.EYE, followup.weapon);
		assertTrue(followup.special);
		strategy.attacked(followup, 105);
		south.ownHitUntil = 0;
		south.hp = 3;
		assertFalse(strategy.decide(room, 108).special);
	}

	@Test
	public void survivingWestTwentyFourUsesTomeAndSouthUsesStoredCharges()
	{
		Room room = room(24);
		room.player = RangerStrategy.tile(3291, 4248);
		Crab west = crab(room, 24, Lane.WEST, 'S', true, Style.MAGIC, 3288, 4248);
		Crab south = crab(room, 24, Lane.SOUTH, 'E', true, Style.MAGIC, 3295, 4244);
		Decision first = strategy.decide(room, 105);
		assertSame(first.reason, west, first.target);
		assertEquals(Weapon.EYE, first.weapon);
		assertTrue(first.special);
		strategy.attacked(first, 105);
		west.pendingDamage = west.hp;
		room.observeTomeCharges(3);
		Decision second = strategy.decide(room, 108);
		assertSame(second.reason, south, second.target);
		assertFalse(second.special);
		assertEquals(2, room.cooldown(second.weapon));
	}

	@Test
	public void knownLethalAndOwnNullStillPreventTomeDuty()
	{
		Room room = room(11);
		Crab west = crab(room, 11, Lane.WEST, 'S', true, Style.MAGIC, 3291, 4248);
		west.pendingDamage = west.hp;
		assertNotSame(west, strategy.decide(room, 104).target);
		west.pendingDamage = 0;
		west.nulled = true;
		assertNotSame(west, strategy.decide(room, 104).target);
	}
	@Test
	public void southSevenBowKeepsItsWindowBeforeMagicSupport()
	{
		Room room = room(7);
		room.player = RangerStrategy.tile(3295, 4243);
		Crab big = crab(room, 7, Lane.SOUTH, 'W', true, Style.RANGE, 3295, 4235);
		crab(room, 7, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4235);
		crab(room, 6, Lane.WEST, 'N', false, Style.MAGIC, 3291, 4249);
		Decision shot = strategy.decide(room, 101);
		assertSame(shot.reason, big, shot.target);
		assertEquals(Weapon.BOW, shot.weapon);
		strategy.attacked(shot, 101);
		big.pendingDamage = big.hp;
		room.player = RangerStrategy.tile(3295, 4245);
		shot = strategy.decide(room, 106);
		assertEquals(Weapon.EYE, shot.weapon);
	}

	@Test
	public void southElevenEastYieldsToReadyMageWhenWestIsCovered()
	{
		Room room = room(11);
		room.observePartner(RangerStrategy.tile(3295, 4248), 108);
		Crab east = crab(room, 11, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4244);
		Crab west = crab(room, 11, Lane.SOUTH, 'W', false, Style.MAGIC, 3295, 4244);
		west.pendingDamage = west.hp;
		Crab alternative = new Crab(++key, key, 108, 11, Lane.SPLIT, (char) 0,
			false, Style.MAGIC, RangerStrategy.tile(3296, 4248));
		alternative.pendingDamage = 0;
		room.crabs.add(alternative);
		Decision shot = strategy.decide(room, 108);
		assertSame(shot.reason, alternative, shot.target);
		assertNotSame(east, shot.target);
	}

	@Test
	public void southElevenEastIsAvailableWhileMageHasUncoveredWest()
	{
		Room room = room(11);
		room.observePartner(RangerStrategy.tile(3295, 4248), 108);
		Crab east = crab(room, 11, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4244);
		crab(room, 11, Lane.SOUTH, 'W', false, Style.MAGIC, 3295, 4244);
		Decision shot = strategy.decide(room, 108);
		assertSame(shot.reason, east, shot.target);
		assertEquals(Weapon.EYE, shot.weapon);
	}

	@Test
	public void staleReadyMageDoesNotHoldSouthElevenForever()
	{
		Room room = room(11);
		room.observePartner(RangerStrategy.tile(3295, 4248), 108);
		Crab east = crab(room, 11, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4244);
		Decision shot = strategy.decide(room, 111);
		assertSame(shot.reason, east, shot.target);
	}

	@Test
	public void versionTwoOpeningContinuesToScratchAfterSouthCrossbows()
	{
		Room room = room(3);
		room.player = RangerStrategy.tile(3296, 4244);
		Crab east = crab(room, 2, Lane.EAST, 'S', false, Style.RANGE, 3300, 4248);
		strategy.attacked(new Decision(east, Weapon.BOW, room.player, false, "Opening east", "test"), 99);
		east.dying = true;
		Crab south = crab(room, 3, Lane.SOUTH, 'E', false, Style.RANGE, 3296, 4237);
		Decision crossbows = strategy.decide(room, 102);
		assertEquals("South crossbows 3", crossbows.rule);
		strategy.attacked(crossbows, 102);
		south.pendingDamage = south.hp;
		Crab scratch = crab(room, 2, Lane.SOUTH, 'W', false, Style.MELEE, 3295, 4244);
		Decision next = strategy.decide(room, 110);
		assertSame(next.reason, scratch, next.target);
		assertEquals(Weapon.SULPHUR, next.weapon);
		assertEquals("Opening scratch", next.rule);
	}

	@Test
	public void twoMagicTargetsBeforeWestElevenDoNotSpendTheFlexibleSpecial()
	{
		Room room = room(7);
		Crab south = crab(room, 7, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4245);
		crab(room, 6, Lane.WEST, 'N', false, Style.MAGIC, 3291, 4249);
		Decision shot = strategy.decide(room, 110);
		assertSame(shot.reason, south, shot.target);
		assertEquals(Weapon.EYE, shot.weapon);
		assertFalse(shot.special);
	}

	@Test
	public void twoMagicTargetsAfterConfirmedWestElevenCanUseTheFlexibleSpecial()
	{
		Room room = room(11);
		Crab west = crab(room, 11, Lane.WEST, 'S', true, Style.MAGIC, 3291, 4248);
		Decision opening = strategy.decide(room, 104);
		assertSame(opening.reason, west, opening.target);
		assertTrue(opening.special);
		strategy.attacked(opening, 104);
		room.crabs.clear();
		room.wave = 13;
		room.spawns[13] = 100;
		room.specialEnergy = 500;
		room.observeTomeCharges(0);
		crab(room, 13, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4248);
		crab(room, 13, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3296, 4248);
		Decision shot = strategy.decide(room, 110);
		assertNotNull(shot.reason, shot.target);
		assertEquals(Weapon.EYE, shot.weapon);
		assertTrue(shot.special);
	}

	private Crab rememberOrdinaryMagic(Room room, int charges)
	{
		room.specialEnergy = 0;
		room.observeTomeCharges(charges);
		Crab magic = crab(room, 14, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3296, 4248);
		Decision first = strategy.decide(room, 101);
		assertSame(first.reason, magic, first.target);
		assertEquals(Weapon.EYE, first.weapon);
		return magic;
	}

	@Test
	public void readyCrossbowClearancePrecedesRememberedOrdinaryUnchargedMagic()
	{
		Room room = room(14);
		Crab magic = rememberOrdinaryMagic(room, 0);
		Crab green = crab(room, 14, Lane.SPLIT, (char) 0, false, Style.RANGE, 3294, 4248);
		Decision shot = strategy.decide(room, 102);
		assertSame(shot.reason, green, shot.target);
		assertEquals(Weapon.PIPE, shot.weapon);
		strategy.attacked(shot, 102);
		green.pendingDamage = green.hp;
		shot = strategy.decide(room, 104);
		assertSame(shot.reason, magic, shot.target);
		assertEquals(Weapon.EYE, shot.weapon);
	}

	@Test
	public void storedTomeChargeKeepsUsefulMagicPreference()
	{
		Room room = room(14);
		Crab magic = rememberOrdinaryMagic(room, 1);
		crab(room, 14, Lane.SPLIT, (char) 0, false, Style.RANGE, 3294, 4248);
		Decision shot = strategy.decide(room, 102);
		assertSame(shot.reason, magic, shot.target);
		assertEquals(Weapon.EYE, shot.weapon);
	}

	@Test
	public void ordinaryMagicRemainsUsefulWhenCrossbowsNeedAnApproach()
	{
		Room room = room(14);
		Crab magic = rememberOrdinaryMagic(room, 0);
		crab(room, 14, Lane.SPLIT, (char) 0, false, Style.RANGE, 3303, 4248);
		Decision shot = strategy.decide(room, 102);
		assertSame(shot.reason, magic, shot.target);
		assertEquals(Weapon.EYE, shot.weapon);
	}

	@Test
	public void mageNullRescueRetainsItsUnchargedTomeShot()
	{
		Room room = room(14);
		Crab magic = rememberOrdinaryMagic(room, 0);
		magic.partnerNulled = true;
		crab(room, 14, Lane.SPLIT, (char) 0, false, Style.RANGE, 3294, 4248);
		Decision shot = strategy.decide(room, 102);
		assertSame(shot.reason, magic, shot.target);
		assertEquals(Weapon.EYE, shot.weapon);
	}

	@Test
	public void confirmedMagicFollowupSurvivesTheCrossbowPreference()
	{
		Room room = room(14);
		Crab magic = rememberOrdinaryMagic(room, 0);
		Decision first = strategy.decide(room, 101);
		strategy.attacked(first, 101);
		magic.ownHitUntil = 0;
		crab(room, 14, Lane.SPLIT, (char) 0, false, Style.RANGE, 3294, 4248);
		Decision shot = strategy.decide(room, 104);
		assertSame(shot.reason, magic, shot.target);
		assertEquals(Weapon.EYE, shot.weapon);
	}

	@Test
	public void usefulTomeSpecialIsNotReplacedWithCrossbowFiller()
	{
		Room room = room(14);
		Crab magic = rememberOrdinaryMagic(room, 0);
		room.specialEnergy = 1000;
		crab(room, 14, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3296, 4249);
		crab(room, 14, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3296, 4247);
		crab(room, 14, Lane.SPLIT, (char) 0, false, Style.RANGE, 3294, 4248);
		Decision shot = strategy.decide(room, 102);
		assertSame(shot.reason, magic, shot.target);
		assertEquals(Weapon.EYE, shot.weapon);
		assertTrue(shot.special);
	}

	@Test
	public void injuredMagicSmallIsFinishedBeforeAnOrdinaryGreen()
	{
		Room room = room(14);
		Crab magic = rememberOrdinaryMagic(room, 0);
		magic.hp = 3;
		crab(room, 14, Lane.SPLIT, (char) 0, false, Style.RANGE, 3294, 4248);
		assertSame(magic, strategy.decide(room, 102).target);
	}

	@Test
	public void oldGreenDoesNotDisplaceFreshMagic()
	{
		Room room = room(14);
		Crab magic = rememberOrdinaryMagic(room, 0);
		Crab old = new Crab(++key, key, 90, 12, Lane.SPLIT, (char) 0,
			false, Style.RANGE, RangerStrategy.tile(3294, 4248));
		old.pendingDamage = 0;
		room.crabs.add(old);
		assertSame(magic, strategy.decide(room, 102).target);
	}

	@Test
	public void versionOneKeepsItsOrdinaryMagicCommitment()
	{
		Room room = room(14);
		room.configureStrategy(1);
		Crab magic = rememberOrdinaryMagic(room, 0);
		crab(room, 14, Lane.SPLIT, (char) 0, false, Style.RANGE, 3294, 4248);
		assertSame(magic, strategy.decide(room, 102).target);
	}

	@Test
	public void namedEastNineMagicHandoffKeepsItsUnchargedShot()
	{
		Room room = room(9);
		room.specialEnergy = 0;
		Crab magic = crab(room, 9, Lane.EAST, 'N', false, Style.MAGIC, 3298, 4249);
		Decision first = strategy.decide(room, 101);
		assertSame(first.reason, magic, first.target);
		assertEquals("East magic 9", first.rule);
		crab(room, 9, Lane.SPLIT, (char) 0, false, Style.RANGE, 3294, 4248);
		Decision shot = strategy.decide(room, 102);
		assertSame(shot.reason, magic, shot.target);
		assertEquals("East magic 9", shot.rule);
		assertFalse(shot.special);
	}

	@Test
	public void blockedCrossbowTargetDoesNotDisplaceUsefulMagic()
	{
		Room room = room(14);
		Crab magic = rememberOrdinaryMagic(room, 0);
		Crab green = crab(room, 14, Lane.SPLIT, (char) 0, false, Style.RANGE, 3294, 4248);
		room.obstructed.add(green.key);
		assertSame(magic, strategy.decide(room, 102).target);
	}

	@Test
	public void missedWestElevenSpecialUsesOrdinaryFollowupAndKeepsFlexibleSpecialForAnotherTarget()
	{
		// Native validation seeds11280346 and11280478 left zero stored charges
		// after the first west11 special. Other magic made its flexible slot eligible.
		Room room = room(11);
		Crab west = crab(room, 11, Lane.WEST, 'S', true, Style.MAGIC, 3291, 4248);
		Decision special = strategy.decide(room, 104);
		assertSame(special.reason, west, special.target);
		assertTrue(special.special);
		strategy.attacked(special, 104);
		room.specialEnergy = 500;
		room.observeTomeCharges(0);
		room.ready = 107;
		west.ownHitUntil = 0;
		west.pendingDamage = 0;
		crab(room, 11, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4247);
		crab(room, 11, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3296, 4247);
		Decision followup = strategy.decide(room, 107);
		assertSame(followup.reason, west, followup.target);
		assertEquals(Weapon.EYE, followup.weapon);
		assertFalse(followup.special);
		strategy.attacked(followup, 107);
		west.pendingDamage = west.hp;
		room.ready = 110;
		Decision flexible = strategy.decide(room, 110);
		assertNotNull(flexible.reason, flexible.target);
		assertNotSame(west, flexible.target);
		assertEquals(Weapon.EYE, flexible.weapon);
		assertTrue(flexible.special);
	}

}
