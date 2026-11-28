package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;

public class RangerEastTenBoundedClearanceTest
{
    private final Room room = new Room();
    private final RangerStrategy ranger = new RangerStrategy();
    private final Crab parent;
    private final Crab small;

    public RangerEastTenBoundedClearanceTest()
    {
        room.wave = 11;
        room.spawns[10] = 68;
        room.spawns[11] = 76;
        room.player = RangerStrategy.tile(3297, 4249);
        parent = new Crab(43, 43, 68, 10, Lane.EAST, 'S', true, Style.RANGE,
            RangerStrategy.tile(3300, 4249));
        parent.hp = 12;
        parent.pendingDamage = 0;
        room.crabs.add(parent);
        small = new Crab(41, 41, 68, 10, Lane.SOUTH, 'W', false, Style.RANGE,
            RangerStrategy.tile(3296, 4245));
        small.pendingDamage = 0;
        room.crabs.add(small);
        ranger.attacked(new Decision(small, Weapon.CHIN, room.player, false, "South chin 10", "Previous packet"), 75);
    }

    @Test public void firstReadyCycleStillClearsAPacketSmall()
    {
        assertSame(small, ranger.decide(room, 78).target);
    }

    @Test public void continuingPacketRecoveryDoesNotConsumeTheParentServiceWindow()
    {
        Decision action = ranger.decide(room, 80);
        assertSame(action.reason, parent, action.target);
        assertEquals(Weapon.PIPE, action.weapon);
        assertFalse(action.special);
    }

    @Test public void lethalPendingCoverageDoesNotReshootTheParent()
    {
        parent.pendingDamage = parent.hp;
        assertSame(small, ranger.decide(room, 80).target);
    }

    @Test public void confirmedSmallSurvivorKeepsItsNextShot()
    {
        ranger.attacked(new Decision(small, Weapon.PIPE, room.player, false, null, "Previous hit"), 78);
        small.hp = 3;
        assertSame(small, ranger.decide(room, 80).target);
    }
}
