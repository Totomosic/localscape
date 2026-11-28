package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;

public class RangerSevenEightSplitGreenTest
{
    private final RangerStrategy ranger = new RangerStrategy();
    private final Room room = new Room();
    private final Crab red;
    private final Crab green;

    public RangerSevenEightSplitGreenTest()
    {
        room.wave = 7;
        room.spawns[7] = 40;
        room.player = RangerStrategy.tile(3300, 4247);
        room.ready = 50;
        red = new Crab(25, 25, 40, 7, Lane.EAST, 'N', false, Style.MELEE,
            RangerStrategy.tile(3300, 4249));
        red.pendingDamage = 0;
        room.crabs.add(red);
        green = new Crab(19, 19, 34, 5, Lane.SPLIT, (char) 0, false, Style.RANGE,
            RangerStrategy.tile(3295, 4247));
        green.pendingDamage = 0;
        room.crabs.add(green);
    }

    @Test public void aReachableTwoTickGreenCanPrecedeTheFourTickScratch()
    {
        Decision action = ranger.decide(room, 50);
        assertSame(action.reason, green, action.target);
        assertEquals(Weapon.PIPE, action.weapon);
        assertEquals("Split green before East 8", action.rule);
    }

    @Test public void theClawCooldownCanMeetAWestSplitBeforeChasingTheScratch()
    {
        green.point = RangerStrategy.tile(3290, 4252);
        Decision stage = ranger.decide(room, 48);
        assertNull(stage.target);
        assertEquals(Weapon.PIPE, stage.weapon);
        assertTrue(stage.reason, stage.reason.startsWith("Meet a split green"));
        assertTrue(stage.tile.getX() < room.player.getX());
    }

    @Test public void anUnfinishedEastSixKeepsItsNamedSpecial()
    {
        Crab east = new Crab(22, 22, 36, 6, Lane.EAST, 'S', true, Style.MELEE,
            RangerStrategy.tile(3299, 4247));
        east.pendingDamage = 0;
        room.crabs.add(east);
        room.specialEnergy = 1000;
        Decision action = ranger.decide(room, 50);
        assertSame(action.reason, east, action.target);
        assertEquals(Weapon.CLAWS, action.weapon);
        assertTrue(action.special);
    }

    @Test public void twoGreensFitBeforeTheRetainedEastEightOpening()
    {
        ranger.attacked(ranger.decide(room, 50), 50);
        green.hp = 0;
        green.dying = true;
        room.wave = 8;
        room.spawns[8] = 52;
        room.ready = 52;
        Crab fresh = new Crab(26, 26, 52, 8, Lane.SPLIT, (char) 0, false, Style.RANGE,
            RangerStrategy.tile(3297, 4243));
        fresh.pendingDamage = 0;
        room.crabs.add(fresh);
        Crab east = new Crab(30, 30, 52, 8, Lane.EAST, 'S', false, Style.RANGE,
            RangerStrategy.tile(3310, 4248));
        east.pendingDamage = 0;
        room.crabs.add(east);
        Decision second = ranger.decide(room, 52);
        assertSame(second.reason, fresh, second.target);
        ranger.attacked(second, 52);
        fresh.hp = 0;
        fresh.dying = true;
        room.ready = 54;
        room.player = RangerStrategy.tile(3301, 4248);
        east.point = RangerStrategy.tile(3308, 4248);
        Decision incoming = ranger.decide(room, 54);
        assertSame(incoming.reason, east, incoming.target);
    }

    @Test public void aMageCoveredGreenDoesNotDisplaceTheScratch()
    {
        green.pendingDamage = green.hp;
        Decision action = ranger.decide(room, 50);
        assertEquals(Weapon.SULPHUR, action.weapon);
        assertTrue(action.target == red || action.approach == red);
    }

    @Test public void aGreenThatCannotReturnForEastEightIsLeftToTheNormalPlan()
    {
        room.wave = 8;
        room.spawns[8] = 52;
        room.ready = 54;
        room.player = RangerStrategy.tile(3290, 4252);
        green.point = room.player;
        assertNotEquals("Split green before East 8", ranger.decide(room, 54).rule);
    }
}
