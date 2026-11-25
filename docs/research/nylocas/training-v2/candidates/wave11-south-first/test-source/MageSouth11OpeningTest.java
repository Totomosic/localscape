package org.localscape.nylocas.mage;

import org.junit.Test;
import static org.junit.Assert.*;
import org.localscape.nylocas.mage.MageStrategy.*;

public class MageSouth11OpeningTest
{
    private static final class Fixture
    {
        final MageStrategy strategy = new MageStrategy();
        final Room room = new Room();
        final Crab west, south, east;
        Fixture()
        {
            // Live112 seed31: wave11 first seen80, barrage80 at3301,4249.
            room.wave = 11; room.spawns[11] = 80; room.specialEnergy = 1000;
            room.player = MageStrategy.tile(3301, 4249);
            west = new Crab(46, 52, 80, 11, Lane.WEST, 'S', true, Style.MAGIC, MageStrategy.tile(3281, 4248));
            south = new Crab(47, 53, 80, 11, Lane.SOUTH, 'W', false, Style.MAGIC, MageStrategy.tile(3295, 4233));
            east = new Crab(49, 55, 80, 11, Lane.EAST, 'N', false, Style.MAGIC, MageStrategy.tile(3310, 4249));
            room.crabs.add(west); room.crabs.add(south); room.crabs.add(east);
            Decision opening = strategy.decide(room, 80);
            assertEquals("East barrage 11", opening.rule);
            assertSame(east, opening.target);
            strategy.attacked(opening, 81, 80);
            room.ready = 85;
            // Exact following observation; planning during barrage cooldown chooses the route.
            west.point = MageStrategy.tile(3282, 4248);
            south.point = MageStrategy.tile(3295, 4234);
            east.point = MageStrategy.tile(3309, 4249);
        }
    }

    @Test public void observedGeometryRoutesSouthAfterBarrageThenOpensWestAtEight()
    {
        Fixture f = new Fixture();
        Decision plan = f.strategy.decide(f.room, 81);
        assertSame(f.south, plan.approach); assertEquals(Weapon.EYE, plan.weapon);
        f.room.player = MageStrategy.tile(3294, 4247);
        f.west.point = MageStrategy.tile(3286, 4248);
        f.south.point = MageStrategy.tile(3295, 4238);
        f.east.point = MageStrategy.tile(3305, 4249);
        Decision south = f.strategy.decide(f.room, 85);
        assertSame(f.south, south.target);
        f.strategy.attacked(south, 86, 85);
        f.room.ready = 88;
        f.room.wave = 12; f.room.spawns[12] = 88;
        f.west.point = MageStrategy.tile(3289, 4248);
        f.south.point = MageStrategy.tile(3295, 4241);
        f.south.ownHitUntil = 0; // Even a completed zero hit must not monopolize the next cast.
        f.east.point = MageStrategy.tile(3302, 4249);
        assertSame(f.west, f.strategy.decide(f.room, 88).target);
    }

    @Test public void unavailableSouthLeavesTheExistingWestOpeningFree()
    {
        for (int state = 0; state < 3; state++)
        {
            Fixture f = new Fixture();
            if (state == 0) f.south.nulled = true;
            if (state == 1) f.south.claimedUntil = 90;
            if (state == 2) { f.south.ownHitUntil = 90; f.south.ownHitHp = 0; }
            assertSame(f.west, f.strategy.decide(f.room, 81).approach);
        }
    }

    @Test public void southPreferenceCannotReplaceAnEarlierFeasibleWestAttack()
    {
        Fixture f = new Fixture();
        f.room.ready = 81;
        f.room.player = MageStrategy.tile(3290, 4248);
        assertSame(f.west, f.strategy.decide(f.room, 81).target);
    }
}
