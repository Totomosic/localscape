package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;

public class RangerSouthSixteenCooldownStageTest
{
    private final RangerStrategy ranger = new RangerStrategy();
    private final Room room = new Room();

    public RangerSouthSixteenCooldownStageTest()
    {
        room.wave = 16;
        room.spawns[14] = 100;
        room.spawns[15] = 112;
        room.spawns[16] = 120;
        room.player = RangerStrategy.tile(3300, 4248);
        room.ready = 122;
        Crab south = new Crab(76, 76, 120, 16, Lane.SOUTH, 'W', false, Style.MELEE,
            RangerStrategy.tile(3295, 4234));
        south.pendingDamage = 0;
        room.crabs.add(south);
    }

    private Crab east(int wave, boolean big, Style style, int hp)
    {
        Crab c = new Crab(69 + wave, 69 + wave, room.spawns[wave], wave, Lane.EAST,
            style == Style.MAGIC ? 'N' : 'S', big, style, RangerStrategy.tile(3300, 4250));
        c.hp = hp;
        c.pendingDamage = 0;
        room.crabs.add(c);
        return c;
    }

    @Test public void movesSouthWhileKeepingTheConfirmedEastFourteenSurvivor()
    {
        Crab parent = east(14, true, Style.RANGE, 16);
        ranger.attacked(new Decision(parent, Weapon.PIPE, room.player, false, "Deferred east big 14", "Previous hit"), 120);
        parent.hp = 8;
        Decision stage = ranger.decide(room, 121);
        assertTrue(stage.reason, stage.reason.startsWith("Stage South16"));
        assertNull(stage.target);
        assertTrue(stage.tile.getY() < room.player.getY());
        assertTrue(parent.distance(stage.tile) < Weapon.PIPE.range);
        room.player = stage.tile;
        assertSame(parent, ranger.decide(room, 122).target);
    }

    @Test public void movesDuringAnEastFifteenBlueCooldownWithoutLosingTheShot()
    {
        Crab blue = east(15, false, Style.MAGIC, 8);
        Decision stage = ranger.decide(room, 121);
        assertTrue(stage.reason, stage.reason.startsWith("Stage South16"));
        assertEquals(Weapon.EYE, stage.weapon);
        room.player = stage.tile;
        assertSame(blue, ranger.decide(room, 122).target);
    }

    @Test public void doesNotLeaveAnUncoveredProtectedParentForAnOrdinaryBlue()
    {
        room.wave = 15;
        room.spawns[15] = 112;
        room.spawns[16] = -1;
        room.ready = 117;
        east(14, true, Style.RANGE, 16);
        east(15, false, Style.MAGIC, 8);
        assertFalse(ranger.decide(room, 116).reason.startsWith("Stage South16"));
    }

    @Test public void requestSixCannotPipeTheStillBlueSouthFlicker()
    {
        Crab south = room.crabs.get(0);
        south.style = Style.MAGIC;
        south.point = RangerStrategy.tile(3295, 4239);
        room.player = RangerStrategy.tile(3295, 4244);
        room.ready = 126;
        Decision action = ranger.decide(room, 126);
        assertFalse(action.target == south && action.weapon == Weapon.PIPE);
    }

    @Test public void requestSevenCanPipeTheGreenSouthFlicker()
    {
        Crab south = room.crabs.get(0);
        south.style = Style.RANGE;
        south.point = RangerStrategy.tile(3295, 4240);
        room.player = RangerStrategy.tile(3295, 4244);
        room.ready = 127;
        Decision action = ranger.decide(room, 127);
        assertSame(action.reason, south, action.target);
        assertEquals(Weapon.PIPE, action.weapon);
        assertTrue(RangerFlickers.matchesNext(south, Style.RANGE, 127));
    }
}
