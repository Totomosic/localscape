package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;

public class RangerEastEightHandoffTest
{
    private final RangerStrategy ranger = new RangerStrategy();
    private final Room room = new Room();
    private final Crab green, blue, west;

    public RangerEastEightHandoffTest()
    {
        room.wave = 9;
        room.spawns[7] = 40;
        room.spawns[8] = 52;
        room.spawns[9] = 56;
        room.player = RangerStrategy.tile(3301,4248);
        room.ready = 57;
        green = add(30,52,8,Lane.EAST,'S',false,Style.RANGE,3306,4248);
        blue = add(35,56,9,Lane.EAST,'N',false,Style.MAGIC,3310,4249);
        west = add(34,56,9,Lane.WEST,'S',true,Style.RANGE,3281,4248);
        ranger.attacked(new Decision(green,Weapon.PIPE,room.player,false,null,"East8 opening"),55);
        green.pendingDamage = 5; // Actual queued nonlethal roll, not an assumed future hit.
    }

    private Crab add(int key,int born,int wave,Lane lane,char slot,boolean big,Style style,int x,int y)
    {
        Crab c = new Crab(key,key,born,wave,lane,slot,big,style,RangerStrategy.tile(x,y));
        c.pendingDamage = 0;
        room.crabs.add(c);
        return c;
    }

    @Test public void anObservedNonlethalEastEightGetsItsPipeBeforeTheEye()
    {
        Decision stage = ranger.decide(room,56);
        assertTrue(stage.reason,stage.reason.startsWith("Keep the East8 handoff ray"));
        room.player = stage.tile;
        green.observePosition(RangerStrategy.tile(3305,4248));
        Decision pipe = ranger.decide(room,57);
        assertSame(pipe.reason,green,pipe.target);
        assertEquals(Weapon.PIPE,pipe.weapon);
        ranger.attacked(pipe,57);
        green.pendingDamage = green.hp;
        room.ready = 59;
        blue.observePosition(RangerStrategy.tile(3307,4249));
        room.player = RangerStrategy.tile(3300,4249);
        Decision eye = ranger.decide(room,59);
        assertSame(eye.reason,blue,eye.target);
        assertEquals("East magic 9",eye.rule);
        ranger.attacked(eye,59);
        blue.pendingDamage = blue.hp;
        room.ready = 62;
        room.player = RangerStrategy.tile(3294,4248);
        west.observePosition(RangerStrategy.tile(3287,4248));
        Decision opening = ranger.decide(room,62);
        assertSame(opening.reason,west,opening.target);
        assertEquals(Weapon.PIPE,opening.weapon);
    }

    @Test public void aMissedOpeningCanStillUseItsLastNamedChinWindow()
    {
        RangerStrategy fresh = new RangerStrategy();
        room.ready = 56;
        room.player = RangerStrategy.tile(3297,4248);
        green.pendingDamage = 0;
        Decision opening = fresh.decide(room,56);
        assertSame(opening.reason,green,opening.target);
        assertEquals(Weapon.CHIN,opening.weapon);
        assertEquals("East chin 8",opening.rule);
    }

    @Test public void anUnfinishedEastSixKeepsItsActualClawRecovery()
    {
        RangerStrategy fresh = new RangerStrategy();
        Crab six = add(22,36,6,Lane.EAST,'S',true,Style.MELEE,3300,4247);
        fresh.attacked(new Decision(six,Weapon.CLAWS,room.player,false,"East big claw special","Previous claw"),54);
        six.hp = 6;
        room.ready = 58;
        Decision approach = fresh.decide(room,56);
        assertFalse(approach.reason.startsWith("Keep the East8 handoff ray"));
        assertSame(six,fresh.decide(room,58).target);
    }

    @Test public void lethalCoverageKeepsTheOriginalEyeTiming()
    {
        green.pendingDamage = green.hp;
        room.ready = 56;
        Decision eye = ranger.decide(room,56);
        assertSame(eye.reason,blue,eye.target);
        assertEquals(Weapon.EYE,eye.weapon);
    }

    @Test public void aCompletedEyeCanUseItsCooldownToKeepTheGreenAndWestRays()
    {
        blue.pendingDamage = blue.hp;
        room.ready = 59;
        Decision stage = ranger.decide(room,57);
        assertTrue(stage.reason,stage.reason.startsWith("Keep the East8 handoff ray"));
        assertTrue(stage.tile.getX() < room.player.getX());
        room.player = stage.tile;
        green.observePosition(RangerStrategy.tile(3303,4248));
        Decision pipe = ranger.decide(room,59);
        assertSame(pipe.reason,green,pipe.target);
        assertEquals(Weapon.PIPE,pipe.weapon);
    }

    @Test public void aFurtherPipeThatWouldLoseTheWestParentBudgetYieldsToEye()
    {
        room.ready = 59;
        green.observePosition(RangerStrategy.tile(3303,4248));
        blue.observePosition(RangerStrategy.tile(3307,4249));
        Decision eye = ranger.decide(room,59);
        assertSame(eye.reason,blue,eye.target);
        assertEquals(Weapon.EYE,eye.weapon);
    }

    @Test public void aConfirmedWestParentSurvivorKeepsItsFollowup()
    {
        ranger.attacked(new Decision(west,Weapon.PIPE,room.player,false,"West big 9","Previous hit"),61);
        west.hp = 7;
        room.ready = 63;
        room.player = RangerStrategy.tile(3293,4248);
        west.observePosition(RangerStrategy.tile(3290,4248));
        assertSame(west,ranger.decide(room,63).target);
    }

    @Test public void aBlockedOrUnknownGreenDoesNotDisplaceTheEye()
    {
        room.ready = 56;
        room.obstructed.add(green.key);
        assertSame(blue,ranger.decide(room,56).target);
        room.obstructed.clear();
        green.pendingDamage = -1;
        assertSame(blue,ranger.decide(room,56).target);
    }

    @Test public void theLastHandoffStillNeedsAPhysicalWestReturnRay()
    {
        blue.pendingDamage = blue.hp;
        room.ready = 61;
        green.observePosition(RangerStrategy.tile(3301,4248));
        Decision tooFar = ranger.decide(room,61);
        assertFalse(tooFar.reason.startsWith("Service East8 before"));
        room.player = RangerStrategy.tile(3297,4249);
        Decision legal = ranger.decide(room,61);
        assertSame(legal.reason,green,legal.target);
        assertTrue(legal.reason.startsWith("Service East8 before"));
    }
}
