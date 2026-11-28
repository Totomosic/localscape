package org.localscape.nylocas.mage;
import static org.junit.Assert.*;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
public class MageSouthFourRedPacketTest
{
    private static class Rig
    {
        final Room room=new Room();final MageStrategy mage=new MageStrategy();final Crab a,b;
        Rig(Style left,Style right)
        {
            room.wave=5;room.spawns[4]=16;room.spawns[5]=20;room.ready=32;room.player=MageStrategy.tile(3296,4243);
            room.observePartner(MageStrategy.tile(3296,4244),34);
            Crab south=new Crab(11,11,16,4,Lane.SOUTH,'W',true,Style.MAGIC,MageStrategy.tile(3295,4239));
            south.hp=0;south.dying=true;south.observeSplitAt(28);room.crabs.add(south);
            Crab west=new Crab(13,13,20,5,Lane.WEST,'S',true,Style.RANGE,MageStrategy.tile(3286,4248));
            west.hp=0;west.dying=true;west.observeSplitAt(31);room.crabs.add(west);mage.decide(room,27);
            room.crabs.remove(south);
            a=new Crab(16,16,28,5,Lane.SPLIT,(char)0,false,left,MageStrategy.tile(3295,4239));
            b=new Crab(17,17,28,5,Lane.SPLIT,(char)0,false,right,MageStrategy.tile(3296,4240));
            a.pendingDamage=b.pendingDamage=0;room.crabs.add(a);room.crabs.add(b);mage.decide(room,28);
            a.observePosition(MageStrategy.tile(3295,4240));b.observePosition(MageStrategy.tile(3296,4241));
        }
        void ready(boolean stable)
        {
            room.player=MageStrategy.tile(3294,4244);
            a.observePosition(MageStrategy.tile(3295,4242));b.observePosition(MageStrategy.tile(3296,4243));
            a.observePosition(MageStrategy.tile(3295,4243));b.observePosition(MageStrategy.tile(3295,4244));
            if(stable){a.observePosition(a.point);b.observePosition(b.point);}
        }
    }
    @Test public void newerWestForecastCannotPullTheObservedSouthRedOffItsReadyIntercept()
    {
        Rig x=new Rig(Style.MELEE,Style.MELEE);Decision d=x.mage.decide(x.room,29);
        assertTrue(d.reason.contains("Intercept observed South4"));assertNull(d.target);assertNull(d.approach);
        assertEquals(MageStrategy.tile(3294,4244),d.tile);assertEquals(Weapon.SULPHUR,d.weapon);
    }
    @Test public void native444SelectsHealthyRedWhileRangerCanFinishItsDamagedOtherRed()
    {
        Rig x=new Rig(Style.MELEE,Style.MELEE);x.ready(false);x.b.hp=7;
        Decision d=x.mage.decide(x.room,32);assertSame(x.a,d.target);assertEquals(Weapon.SULPHUR,d.weapon);
        assertEquals("South4 red intercept",d.rule);
    }
    @Test public void nextCooldownPhysicallyReturnsWestAndPreservesGroundRoute()
    {
        Rig x=new Rig(Style.MELEE,Style.MELEE);x.ready(false);x.b.hp=7;
        Decision d=x.mage.decide(x.room,32);x.mage.attacked(d,33,32);x.room.ready=36;
        for(int t=33;t<36;t++)
        {Decision p=x.mage.decide(x.room,t);assertNull(p.target);assertNull(p.approach);assertEquals(MageStrategy.tile(3292,4249),p.tile);}
        x.room.wave=6;x.room.spawns[6]=36;x.room.player=MageStrategy.tile(3292,4249);
        Crab west=new Crab(20,20,36,6,Lane.WEST,'N',false,Style.MAGIC,MageStrategy.tile(3281,4249));west.pendingDamage=0;x.room.crabs.add(west);
        assertSame(west,x.mage.decide(x.room,36).target);
    }
    @Test public void actualPerpendicularRedPairCanUseOneScytheBeforeWestSix()
    {
        Rig x=new Rig(Style.MELEE,Style.MELEE);x.ready(true);x.room.observePartner(MageStrategy.tile(3290,4249),36);
        Decision d=x.mage.decide(x.room,32);assertEquals(Weapon.SCYTHE,d.weapon);assertEquals("South4 red intercept",d.rule);
    }
    @Test public void diagonalAdjacencyIsNotAnEarlyScytheDoubleHit()
    {
        Rig x=new Rig(Style.MELEE,Style.MELEE);x.ready(true);x.room.observePartner(MageStrategy.tile(3290,4249),36);
        x.a.observePosition(MageStrategy.tile(3295,4244));x.a.observePosition(x.a.point);
        x.b.observePosition(MageStrategy.tile(3294,4245));x.b.observePosition(x.b.point);
        assertEquals(Weapon.SULPHUR,x.mage.decide(x.room,32).weapon);
    }
    @Test public void rangerGreensDoNotCreateAnIntercept()
    {Rig x=new Rig(Style.RANGE,Style.RANGE);assertFalse(x.mage.decide(x.room,29).reason.contains("Intercept observed South4"));}
    @Test public void lethalFlightsAlreadyOwnTheRedBodies()
    {
        Rig x=new Rig(Style.MELEE,Style.RANGE);x.a.pendingDamage=8;
        assertFalse(x.mage.decide(x.room,29).reason.contains("Intercept observed South4"));
    }
    @Test public void ownNullNeverBecomesAPacketTarget()
    {Rig x=new Rig(Style.MELEE,Style.RANGE);x.a.nulled=true;assertFalse(x.mage.decide(x.room,29).reason.contains("Intercept observed South4"));}
    @Test public void anUnfinishedAssignedEastBlueStillWins()
    {
        Rig x=new Rig(Style.MELEE,Style.MELEE);Crab own=new Crab(15,15,20,5,Lane.EAST,'N',false,Style.MAGIC,MageStrategy.tile(3301,4249));
        own.pendingDamage=0;x.room.crabs.add(own);assertFalse(x.mage.decide(x.room,29).reason.contains("Intercept observed South4"));
    }
    @Test public void interceptCannotSpendWestSixCooldownBudget()
    {Rig x=new Rig(Style.MELEE,Style.MELEE);x.room.ready=35;assertFalse(x.mage.decide(x.room,29).reason.contains("Intercept observed South4"));}
    @Test public void readyPlayerUsesOnlyAnActuallyReachableRed()
    {
        Rig x=new Rig(Style.MELEE,Style.RANGE);x.room.ready=29;x.room.player=MageStrategy.tile(3301,4252);
        Decision d=x.mage.decide(x.room,29);assertNotEquals("South4 red intercept",d.rule);
    }
}
