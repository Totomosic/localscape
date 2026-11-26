package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;

import static org.junit.Assert.*;

public class MageOptionalWest27Test
{
	private final MageStrategy strategy = new MageStrategy();
	private final Room room = new Room();
	private int key;

	private Crab crab(int wave, int born, Lane lane, boolean big, Style style, int x, int y)
	{
		Crab c = new Crab(++key, key, born, wave, lane, 'S', big, style, MageStrategy.tile(x, y));
		c.pendingDamage = 0;
		room.crabs.add(c);
		return c;
	}

	private Crab west()
	{
		room.wave = 27;
		room.spawns[27] = 220;
		room.player = MageStrategy.tile(3292, 4248);
		return crab(27, 220, Lane.WEST, true, Style.MAGIC, 3284, 4248);
	}

	@Test
	public void leavesOptionalParentWhileAnyOtherBodyIsAlive()
	{
		Crab optional = west();
		crab(22, 180, Lane.SPLIT, false, Style.RANGE, 3300, 4254);
		Decision d = strategy.decide(room, 223);
		assertNotSame(optional, d.target);
		assertNotSame(optional, d.approach);
	}

	@Test
	public void queuedLethalAndOwnNullStillCountAsOtherLivingBodies()
	{
		Crab optional = west();
		Crab mandatory = crab(26, 216, Lane.WEST, true, Style.MAGIC, 3288, 4248);
		mandatory.nulled = true;
		mandatory.pendingDamage = mandatory.hp;
		assertNotSame(optional, strategy.decide(room, 223).target);
	}

	@Test
	public void permitsTheOptionalParentOnlyAfterAllOtherBodiesHaveDied()
	{
		Crab optional = west();
		Crab dead = crab(26, 216, Lane.WEST, true, Style.MAGIC, 3288, 4248);
		dead.hp = 0;
		dead.dying = true;
		assertSame(optional, strategy.decide(room, 223).target);
	}

	@Test
	public void anEarlyBirthDoesNotOverrideItsActualPositionPastTheDoorway()
	{
		Crab optional = west();
		optional.point = MageStrategy.tile(3293, 4248);
		assertNotSame(optional, strategy.decide(room, 223).target);
	}

	@Test
	public void anEarlyParentBesideANorthwestPillarIsOutsideTheWestLane()
	{
		Crab optional = west();
		optional.point = MageStrategy.tile(3290, 4252);
		assertNotSame(optional, strategy.decide(room, 223).target);
		assertFalse(MageStrategy.eligible(room, optional, 223));
	}

	@Test
	public void allowsOneLateralTileOnlyAtTheDoorway()
	{
		Crab optional = west();
		optional.point = MageStrategy.tile(3291, 4247);
		assertSame(optional, strategy.decide(room, 223).target);
		optional.point = MageStrategy.tile(3288, 4247);
		assertFalse(MageStrategy.eligible(room, optional, 223));
	}

	@Test
	public void aConfirmedOpeningDoesNotPermitLateFollowups()
	{
		Crab optional = west();
		Decision first = strategy.decide(room, 223);
		assertSame(optional, first.target);
		strategy.attacked(first, 224, 223);
		room.ready = 226;
		optional.point = MageStrategy.tile(3294, 4248);
		assertNotSame(optional, strategy.decide(room, 230).target);
	}

	@Test
	public void genericCooperationCannotReopenALateOptionalParent()
	{
		Crab optional = west();
		room.wave = 31;
		room.spawns[29] = 210;
		optional.partnerNulled = true;
		optional.point = MageStrategy.tile(3295, 4248);
		assertFalse(MageStrategy.eligible(room, optional, 248));
		assertNotSame(optional, strategy.decide(room, 248).target);
	}
}
