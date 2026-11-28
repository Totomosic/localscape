package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;

public class RangerNativeChinAnchorTest
{
    private final RangerStrategy ranger = new RangerStrategy();
    private final Room room = new Room();
    private final Crab child, parent;

    public RangerNativeChinAnchorTest()
    {
        room.wave = 11; room.spawns[10] = 68; room.spawns[11] = 76;
        room.ready = 81; room.player = RangerStrategy.tile(3295,4249);
        child = add(44,72,10,Lane.SPLIT,(char)0,false,3299,4253);
        parent = add(43,68,10,Lane.EAST,'S',true,3300,4251); parent.hp = 15;
        for (String name : new String[]{"West chin 10","East chin 10","South chin 10"})
        {
            Crab prior = new Crab(100,100,68,10,Lane.SOUTH,'W',false,Style.RANGE,RangerStrategy.tile(3295,4241));
            ranger.attacked(new Decision(prior,Weapon.CHIN,room.player,false,name,name),76);
        }
    }

    private Crab add(int key,int born,int wave,Lane lane,char slot,boolean big,int x,int y)
    {
        Crab c = new Crab(key,key,born,wave,lane,slot,big,Style.RANGE,RangerStrategy.tile(x,y));
        c.pendingDamage=0; room.crabs.add(c); return c;
    }

    @Test public void aParentFootprintTwoTilesAwayDoesNotCreateAChinPair()
    {
        Decision shot = ranger.decide(room,81);
        assertSame(shot.reason,parent,shot.target);
        assertEquals(Weapon.PIPE,shot.weapon);
    }

    @Test public void anActualAdjacentParentAnchorRetainsTheTwoBodyChin()
    {
        parent.point = RangerStrategy.tile(3300,4252);
        Decision shot = ranger.decide(room,81);
        assertSame(shot.reason,child,shot.target);
        assertEquals(Weapon.CHIN,shot.weapon);
    }

    @Test public void anActualSmallPairStillUsesItsAreaShot()
    {
        room.crabs.remove(parent);
        add(45,72,10,Lane.SPLIT,(char)0,false,3300,4253);
        assertEquals(Weapon.CHIN,ranger.decide(room,81).weapon);
    }

    @Test public void aDistantParentStillGetsItsOwnOrdinaryService()
    {
        room.crabs.remove(child);
        Decision shot = ranger.decide(room,81);
        assertSame(parent,shot.target);
        assertEquals(Weapon.PIPE,shot.weapon);
    }
}
