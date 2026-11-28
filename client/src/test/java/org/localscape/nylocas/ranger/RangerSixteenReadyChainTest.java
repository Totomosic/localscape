package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;

public class RangerSixteenReadyChainTest
{
    private final RangerStrategy ranger = new RangerStrategy();
    private final Room room = new Room();
    private final Crab blue;
    private final Crab south;

    public RangerSixteenReadyChainTest()
    {
        room.wave = 17;
        room.spawns[16] = 120;
        room.spawns[17] = 124;
        room.player = RangerStrategy.tile(3295, 4245);
        room.ready = 127;
        blue = new Crab(65, 65, 108, 14, Lane.SPLIT, (char) 0, false, Style.MAGIC,
            RangerStrategy.tile(3296, 4246));
        blue.pendingDamage = 0;
        room.crabs.add(blue);
        ranger.decide(room, 126); // The approach has already selected this ordinary blue.
        south = new Crab(76, 76, 120, 16, Lane.SOUTH, 'W', false, Style.RANGE,
            RangerStrategy.tile(3295, 4240));
        south.hp = 1;
        south.pendingDamage = 0;
        room.crabs.add(south);
    }

    @Test public void comparesAReadyTwoTickClearanceAgainstTheRememberedBlue()
    {
        Decision action = ranger.decide(room, 127);
        assertSame(action.reason, south, action.target);
        assertEquals(Weapon.PIPE, action.weapon);
        ranger.attacked(action, 127);
        south.hp = 0;
        south.dying = true;
        room.ready = 129;
        assertSame(blue, ranger.decide(room, 129).target);
    }

    @Test public void anUnfinishedEastFourteenDoesNotAcquireTheNewSmallOverride()
    {
        Crab parent = new Crab(69, 69, 100, 14, Lane.EAST, 'S', true, Style.RANGE,
            RangerStrategy.tile(3298, 4247));
        parent.pendingDamage = 0;
        room.crabs.add(parent);
        assertNotSame(south, ranger.decide(room, 127).target);
    }

    @Test public void aStillBlueSouthDoesNotBecomeAReadyPipeByForecastAlone()
    {
        south.style = Style.MAGIC;
        Decision action = ranger.decide(room, 126);
        assertFalse(action.target == south && action.weapon == Weapon.PIPE);
    }
}
