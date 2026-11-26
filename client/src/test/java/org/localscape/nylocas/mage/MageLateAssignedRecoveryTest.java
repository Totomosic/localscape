package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.Crab;
import org.localscape.nylocas.mage.MageStrategy.Decision;
import org.localscape.nylocas.mage.MageStrategy.Lane;
import org.localscape.nylocas.mage.MageStrategy.Room;
import org.localscape.nylocas.mage.MageStrategy.Style;
import org.localscape.nylocas.mage.MageStrategy.Weapon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class MageLateAssignedRecoveryTest
{
	private final MageStrategy strategy = new MageStrategy();
	private final Room room = new Room();
	private int key;

	private Crab parent(int wave, int born, Lane lane, Style style, int x, int y)
	{
		Crab crab = new Crab(++key, key, born, wave, lane, 'S', true, style, MageStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	private void beforeLateDinh()
	{
		room.wave = 28;
		room.spawns[28] = 240;
		room.ready = 241;
		room.specialEnergy = 500;
		room.player = MageStrategy.tile(3293, 4246);
		parent(26, 228, Lane.SOUTH, Style.MELEE, 3294, 4244);
	}

	private Crab lowRollWest26BeforeDinh()
	{
		// Seed87034019481696 at246: Eyes237/240/243 have done just two damage.
		room.wave = 28;
		room.spawns[28] = 240;
		room.ready = 246;
		room.specialEnergy = 500;
		room.player = MageStrategy.tile(3293, 4245);
		Crab west = parent(26, 228, Lane.WEST, Style.MAGIC, 3293, 4247);
		west.hp = 14;
		parent(26, 228, Lane.SOUTH, Style.MELEE, 3292, 4244);
		room.crabs.add(new Crab(++key, key, 240, 28, Lane.WEST, 'N', false, Style.MELEE, MageStrategy.tile(3289, 4249)));
		return west;
	}

	@Test
	public void spendsTheRemainingDinhWindowSlackOnTheRequiredMagicParent()
	{
		Crab west = lowRollWest26BeforeDinh();
		Decision cast = strategy.decide(room, 246);
		assertSame(west, cast.target);
		assertEquals(Weapon.EYE, cast.weapon);
	}

	@Test
	public void doesNotNullTheRequiredMagicParentToMeetTheMeleeSweepDeadline()
	{
		Crab west = lowRollWest26BeforeDinh();
		Decision cast = strategy.decide(room, 248);
		assertSame(west, cast.target);
		assertEquals(Weapon.EYE, cast.weapon);
	}

	@Test
	public void aQueuedLethalHitReleasesTheLateDinh()
	{
		Crab west = lowRollWest26BeforeDinh();
		west.pendingDamage = west.hp;
		assertEquals(Weapon.DINH, strategy.decide(room, 248).weapon);
	}

	@Test
	public void anOwnNullCannotBeRecoveredAndDoesNotBlockDinh()
	{
		Crab west = lowRollWest26BeforeDinh();
		west.nulled = true;
		assertEquals(Weapon.DINH, strategy.decide(room, 248).weapon);
	}

	@Test
	public void theSweepUsesLowerLeftRadiusRatherThanTheBigFootprint()
	{
		Crab west = lowRollWest26BeforeDinh();
		west.point = MageStrategy.tile(3287, 4245);
		assertEquals(5, west.distance(room.player));
		assertEquals(Weapon.DINH, strategy.decide(room, 248).weapon);
	}

	@Test
	public void aBlockedLowerLeftRayDoesNotBlockASafeSweep()
	{
		Crab west = lowRollWest26BeforeDinh();
		room.dinhObstructed.add(west.key);
		assertEquals(Weapon.DINH, strategy.decide(room, 248).weapon);
	}

	@Test
	public void clearsTheMagicParentBeforeTheSweepCanNullIt()
	{
		// Packed seed 57209168987153 at tick 241.
		beforeLateDinh();
		parent(25, 220, Lane.WEST, Style.MELEE, 3292, 4247);
		Crab magic = parent(26, 228, Lane.WEST, Style.MAGIC, 3290, 4247);

		Decision cast = strategy.decide(room, 241);
		assertSame(magic, cast.target);
		assertEquals(Weapon.EYE, cast.weapon);
	}

	@Test
	public void letsAnEarlierLethalHitCoverTheMagicParent()
	{
		beforeLateDinh();
		Crab melee = parent(25, 220, Lane.WEST, Style.MELEE, 3292, 4247);
		Crab magic = parent(26, 228, Lane.WEST, Style.MAGIC, 3290, 4247);
		magic.pendingDamage = magic.hp;

		assertSame(melee, strategy.decide(room, 241).target);
	}

	@Test
	public void preservesTheAlreadyOpenedWestTwentyFive()
	{
		beforeLateDinh();
		Crab melee = parent(25, 220, Lane.WEST, Style.MELEE, 3292, 4247);
		Crab magic = parent(26, 228, Lane.WEST, Style.MAGIC, 3290, 4247);
		melee.hp = 3;
		strategy.attacked(new Decision(melee, Weapon.SCYTHE, room.player, false, null, "previous attack"), 237, 236);

		assertSame(melee, strategy.decide(room, 241).target);
	}

	private Crab coreWaveTwentyEight()
	{
		// Packed seed 164046106452528. Both mandatory parents are intact,
		// but the coming melee Dinh can null the nearer magic parent.
		room.wave = 28;
		room.spawns[28] = 236;
		room.ready = 237;
		room.specialEnergy = 500;
		room.player = MageStrategy.tile(3294, 4247);
		return parent(26, 224, Lane.WEST, Style.MAGIC, 3291, 4248);
	}

	@Test
	public void attacksWestTwentySixFirstAndReturnsToEastTwentyFourAfterDinh()
	{
		Crab west = coreWaveTwentyEight();
		Crab east = parent(24, 208, Lane.EAST, Style.MELEE, 3295, 4247);
		Crab south = parent(26, 224, Lane.SOUTH, Style.MELEE, 3292, 4244);
		room.crabs.add(new Crab(++key, key, 236, 28, Lane.WEST, 'N', false, Style.MELEE, MageStrategy.tile(3289, 4249)));

		Decision first = strategy.decide(room, 237);
		assertSame(west, first.target);
		assertEquals(Weapon.EYE, first.weapon);
		strategy.attacked(first, 238, 237);
		west.hp = 0;
		west.dying = true;

		room.ready = 240;
		room.player = MageStrategy.tile(3293, 4245);
		Decision sweep = strategy.decide(room, 244);
		assertSame(south, sweep.target);
		assertEquals(Weapon.DINH, sweep.weapon);
		strategy.attacked(sweep, 245, 244);
		south.hp = 0;
		south.dying = true;
		room.ready = 249;

		Decision next = strategy.decide(room, 249);
		assertSame(east, next.target);
		assertEquals(Weapon.SCYTHE, next.weapon);
	}

	@Test
	public void keepsTheAlreadyWoundedEastTwentyFourAtTheFront()
	{
		coreWaveTwentyEight();
		Crab east = parent(24, 208, Lane.EAST, Style.MELEE, 3295, 4247);
		east.hp = 4;

		assertSame(east, strategy.decide(room, 237).target);
	}

	@Test
	public void retainsEastTwentyFourWhenItWouldRunOutOfTimeAfterDinh()
	{
		coreWaveTwentyEight();
		Crab east = parent(24, 204, Lane.EAST, Style.MELEE, 3295, 4247);

		assertSame(east, strategy.decide(room, 237).target);
	}

	@Test
	public void canReserveWestTwentySixBeforeActualWaveTwentyEight()
	{
		// Packed seed 164046106452595, while 28 is still the next admission.
		room.wave = 27;
		room.spawns[27] = 224;
		room.ready = 230;
		room.specialEnergy = 500;
		room.player = MageStrategy.tile(3293, 4246);
		Crab west = parent(26, 220, Lane.WEST, Style.MAGIC, 3290, 4247);
		parent(24, 204, Lane.EAST, Style.MELEE, 3295, 4247);

		assertSame(west, strategy.decide(room, 230).target);
	}

	@Test
	public void leavesThePackedEastDoorwayInTimeForSouthTwentyNine()
	{
		// Packed seed 57209168987191 at tick 260. The eastern barrier
		// adds a vertical step at the start of the return.
		room.wave = 30;
		room.spawns[29] = 256;
		room.spawns[30] = 260;
		room.ready = 260;
		room.player = MageStrategy.tile(3301, 4249);
		Crab south = parent(29, 256, Lane.SOUTH, Style.MELEE, 3295, 4237);
		Crab east = parent(30, 260, Lane.EAST, Style.MAGIC, 3310, 4248);
		Crab split = new Crab(++key, key, 227, 27, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			MageStrategy.tile(3301, 4252));
		split.pendingDamage = 0;
		room.crabs.add(split);

		Decision stage = strategy.decide(room, 260);
		assertNotSame(east, stage.target);
		assertNotSame(east, stage.approach);
		assertNotSame(split, stage.target);
		assertNotSame(split, stage.approach);
		assertEquals(Weapon.SCYTHE, stage.weapon);
		room.player = MageStrategy.tile(3295, 4243);
		south.point = MageStrategy.tile(3295, 4240);
		assertSame(south, strategy.decide(room, 263).target);
	}

	@Test
	public void takesTheWestwardTurnBeforeTheLastSafeSouthTwentyNineReturn()
	{
		// In the first recovery replay, a cast from (3299, 4249)
		// left two southbound moves and no westward turn.
		room.wave = 29;
		room.spawns[29] = 256;
		room.ready = 260;
		room.player = MageStrategy.tile(3299, 4249);
		Crab south = parent(29, 256, Lane.SOUTH, Style.MELEE, 3295, 4235);
		Crab split = new Crab(++key, key, 227, 27, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			MageStrategy.tile(3301, 4252));
		split.pendingDamage = 0;
		room.crabs.add(split);

		Decision turn = strategy.decide(room, 258);
		assertEquals(Weapon.SCYTHE, turn.weapon);
		assertEquals(MageStrategy.tile(3297, 4249), turn.tile);
		assertEquals(null, turn.approach);

		room.player = turn.tile;
		south.point = MageStrategy.tile(3295, 4236);
		assertEquals(MageStrategy.tile(3295, 4243), strategy.decide(room, 259).tile);

		room.wave = 30;
		room.spawns[30] = 260;
		room.player = MageStrategy.tile(3296, 4247);
		south.point = MageStrategy.tile(3295, 4237);
		assertSame(south, strategy.decide(room, 260).approach);
	}

	@Test
	public void crossesTheWestCorridorBeforeTurningTowardSouthTwentyNine()
	{
		// Packed seed 164046106452635. A cast from the west side left
		// the mage outside the doorway when wave 31 was about to dim it.
		room.wave = 29;
		room.spawns[29] = 256;
		room.ready = 260;
		room.player = MageStrategy.tile(3291, 4249);
		Crab south = parent(29, 256, Lane.SOUTH, Style.MELEE, 3295, 4236);
		Crab split = new Crab(++key, key, 243, 28, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			MageStrategy.tile(3299, 4254));
		split.pendingDamage = 0;
		room.crabs.add(split);

		Decision turn = strategy.decide(room, 259);
		assertEquals(Weapon.SCYTHE, turn.weapon);
		assertEquals(MageStrategy.tile(3293, 4249), turn.tile);
		assertNull(turn.approach);

		room.wave = 30;
		room.spawns[30] = 260;
		room.player = MageStrategy.tile(3293, 4247);
		south.point = MageStrategy.tile(3295, 4237);
		assertSame(south, strategy.decide(room, 260).approach);

		room.player = MageStrategy.tile(3294, 4243);
		room.ready = 263;
		south.point = MageStrategy.tile(3295, 4240);
		assertSame(south, strategy.decide(room, 263).target);
	}
}
