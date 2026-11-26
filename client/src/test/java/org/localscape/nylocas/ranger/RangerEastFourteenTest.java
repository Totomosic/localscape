package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class RangerEastFourteenTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();
	private final Crab parent;
	private final Crab magic;
	private final Crab ranged;

	public RangerEastFourteenTest()
	{
		room.wave = 15;
		room.spawns[14] = 100;
		room.spawns[15] = 108;
		room.player = RangerStrategy.tile(3296, 4248);
		parent = add(1, 100, 14, Lane.EAST, 'S', true, Style.RANGE);
		magic = add(2, 108, 15, Lane.EAST, 'N', false, Style.MAGIC);
		ranged = add(3, 108, 15, Lane.EAST, 'S', false, Style.RANGE);
	}

	@Test
	public void east15SmallsTakePriorityUntilBothHaveLethalCoverage()
	{
		magic.pendingDamage = magic.hp - 1;
		ranged.pendingDamage = ranged.hp;
		add(4, 112, 15, Lane.SPLIT, (char) 0, false, Style.RANGE).hp = 1;
		assertSame(magic, ranger.decide(room, 112).target);

		magic.pendingDamage = magic.hp;
		Decision opening = ranger.decide(room, 112);
		assertSame(parent, opening.target);
		assertSame(Weapon.PIPE, opening.weapon);
	}

	@Test
	public void aZeroRollAndUnknownPartnerDamageKeepTheGateClosed()
	{
		magic.pendingDamage = 0;
		ranged.pendingDamage = -1;
		ranged.claimedUntil = 119;
		assertSame(magic, ranger.decide(room, 112).target);
	}

	@Test
	public void theParentGetsABoundedRecoveryBeforeTransitionDespiteAnEast15Survivor()
	{
		room.wave = 16;
		room.spawns[16] = 116;
		magic.nulled = true;
		ranged.nulled = true;
		assertNotSame(parent, ranger.decide(room, 117).target);
		assertSame(parent, ranger.decide(room, 118).target);
	}

	@Test
	public void aReachableFinalSmallCanFinishJustPastTheOrdinaryFallback()
	{
		room.wave = 16;
		room.spawns[16] = 116;
		ranged.pendingDamage = ranged.hp;
		magic.hp = 3;
		assertSame(magic, ranger.decide(room, 118).target);
		magic.pendingDamage = magic.hp;
		assertSame(parent, ranger.decide(room, 120).target);
	}

	@Test
	public void aBlockedFinalSmallDoesNotSpendTheExtension()
	{
		room.wave = 16;
		room.spawns[16] = 116;
		ranged.pendingDamage = ranged.hp;
		room.obstructed.add(magic.key);
		assertSame(parent, ranger.decide(room, 118).target);
	}

	@Test
	public void repeatedMissesStillReleaseEast14BeforeTransition()
	{
		room.wave = 17;
		room.spawns[17] = 120;
		ranged.pendingDamage = ranged.hp;
		assertSame(magic, ranger.decide(room, 122).target);
		assertSame(parent, ranger.decide(room, 123).target);
	}

	@Test
	public void recoveryAfterTheCapIncreasePrioritizesTheUnfinishedParentAboveFiller()
	{
		room.wave = 22;
		room.spawns[22] = 133;
		room.crabs.remove(magic);
		room.crabs.remove(ranged);
		add(4, 135, 22, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(parent, ranger.decide(room, 136).target);
	}

	private Crab add(int key, int born, int wave, Lane lane, char slot, boolean big, Style style)
	{
		Crab crab = new Crab(key, key, born, wave, lane, slot, big, style, room.player.dx(1));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}
}
