package org.localscape.nylocas.mage;
import static org.junit.Assert.*;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
public class MageSharedMiddleBlueTest
{
    private Room room()
    {
        Room r=new Room();r.wave=14;r.spawns[12]=84;r.spawns[13]=92;r.spawns[14]=100;r.ready=104;
        r.player=MageStrategy.tile(3293,4249);r.observePartner(MageStrategy.tile(3292,4249),106);return r;
    }
    private Crab blue(Room r)
    {return crab(r,59,92,13,Lane.WEST,'S',Style.MAGIC,3290,4248);}
    private Crab crab(Room r,int key,int born,int wave,Lane lane,char slot,Style style,int x,int y)
    {Crab c=new Crab(key,key,born,wave,lane,slot,false,style,MageStrategy.tile(x,y));c.pendingDamage=0;r.crabs.add(c);return c;}
    @Test public void coolingRangerReleasesASharedWestBlue()
    {Room r=room();Crab b=blue(r);assertSame(b,new MageStrategy().decide(r,104).target);}
    @Test public void cooldownStagesALegalFiringTileWithoutNpcInteraction()
    {
        Room r=room();blue(r);r.player=MageStrategy.tile(3293,4250);
        Decision d=new MageStrategy().decide(r,102);assertEquals(Weapon.EYE,d.weapon);assertNull(d.target);assertNull(d.approach);
        assertEquals(r.player,d.tile);assertTrue(d.reason.contains("shared middle-room"));
    }
    @Test public void nativeLethalFlightAlreadyOwnsTheBody()
    {Room r=room();Crab b=blue(r);b.pendingDamage=8;assertNotSame(b,new MageStrategy().decide(r,104).target);}
    @Test public void knownNonlethalFlightCanReceiveTheFinishingEye()
    {Room r=room();Crab b=blue(r);b.pendingDamage=1;b.claimedUntil=130;assertSame(b,new MageStrategy().decide(r,104).target);}
    @Test public void unknownFlightAndOwnNullRemainIneligible()
    {
        Room r=room();Crab b=blue(r);b.pendingDamage=-1;assertNotSame(b,new MageStrategy().decide(r,104).target);
        b.pendingDamage=0;b.nulled=true;assertNotSame(b,new MageStrategy().decide(r,104).target);
    }
    @Test public void freeNearbyRangerKeepsItsBlueWithoutUsefulGreenWork()
    {Room r=room();Crab b=blue(r);r.partnerReady=104;assertNotSame(b,new MageStrategy().decide(r,104).target);}
    @Test public void actualReachableGreenMakesReadyRangerBusy()
    {
        Room r=room();Crab b=blue(r);r.partnerReady=104;
        crab(r,58,92,13,Lane.WEST,'N',Style.RANGE,3290,4249);assertSame(b,new MageStrategy().decide(r,104).target);
    }
    @Test public void healthyFarAssignedSouthMeleeNoLongerForbidsUsefulBlueService()
    {
        Room r=room();Crab b=blue(r);crab(r,60,92,13,Lane.SOUTH,'W',Style.MELEE,3299,4243);
        assertSame(b,new MageStrategy().decide(r,104).target);
    }
    @Test public void actualWestFourteenMagicStillLaunchesFirst()
    {
        Room r=room();blue(r);Crab own=crab(r,63,100,14,Lane.WEST,'N',Style.MAGIC,3290,4249);
        assertSame(own,new MageStrategy().decide(r,104).target);
    }
    @Test public void confirmedWoundedFollowupStillFinishesFirst()
    {
        Room r=room();blue(r);Crab own=crab(r,60,97,13,Lane.SPLIT,(char)0,Style.MELEE,3299,4243);own.hp=3;
        Decision d=new MageStrategy().decide(r,104);assertSame(own,d.target!=null?d.target:d.approach);
    }
    @Test public void cheapTwoVictimScytheStillWins()
    {
        Room r=room();Crab b=blue(r);crab(r,70,101,14,Lane.SPLIT,(char)0,Style.MELEE,3293,4248);
        crab(r,71,101,14,Lane.SPLIT,(char)0,Style.MELEE,3294,4248);
        Decision d=new MageStrategy().decide(r,104);assertNotSame(b,d.target);assertEquals(Weapon.SCYTHE,d.weapon);
    }
    @Test public void eastFifteenOpeningStillHasItsPhysicalReturnBudget()
    {Room r=room();Crab b=blue(r);r.ready=108;r.partnerReady=110;assertNotSame(b,new MageStrategy().decide(r,108).target);}
    @Test public void currentlyBlockedReadyEyeCannotSpendAnIdleRequest()
    {Room r=room();Crab b=blue(r);r.obstructed.add(b.key);assertNotSame(b,new MageStrategy().decide(r,104).target);}
    @Test public void eastSixteenBlueUsesItsActualEarlyMagicPhase()
    {
        Room r=room();r.wave=16;r.spawns[15]=108;r.spawns[16]=116;r.ready=117;
        r.player=MageStrategy.tile(3299,4249);r.observePartner(MageStrategy.tile(3290,4249),119);
        Crab b=crab(r,80,116,16,Lane.EAST,'S',Style.MAGIC,3309,4248);
        assertSame(b,new MageStrategy().decide(r,117).target);
        r.ready=120;r.partnerReady=122;assertNotSame(b,new MageStrategy().decide(r,120).target);
    }

    @Test public void sharedEyeCannotCrossTheUnopenedEastThirteenReleaseBudget()
    {
        Room r=room();r.wave=16;r.spawns[15]=108;r.spawns[16]=116;r.ready=119;
        r.player=MageStrategy.tile(3299,4249);r.observePartner(MageStrategy.tile(3290,4249),122);
        Crab b=crab(r,68,100,14,Lane.SOUTH,'E',Style.MAGIC,3299,4243);
        Crab parent=new Crab(56,56,92,13,Lane.EAST,'S',true,Style.MELEE,MageStrategy.tile(3300,4249));
        parent.pendingDamage=0;r.crabs.add(parent);
        crab(r,73,108,15,Lane.EAST,'S',Style.RANGE,3301,4252);
        assertNotSame(b,new MageStrategy().decide(r,119).target);
    }
    @Test public void native440ServiceFitsBeforeTheEastFifteenPreparation()
    {
        Room r=room();r.spawns[14]=104;r.ready=110;r.player=MageStrategy.tile(3295,4248);
        r.observePartner(MageStrategy.tile(3294,4246),112);
        Crab b=crab(r,57,92,13,Lane.WEST,'S',Style.MAGIC,3292,4246);
        Decision d=new MageStrategy().decide(r,110);assertSame(b,d.target);
        assertEquals(Weapon.EYE,d.weapon);assertEquals(r.player,d.tile);
    }

    @Test public void southBlueCooldownMoveUsesTheClearEastFaceOfSouthwestPillar()
    {
        Room r=room();r.player=MageStrategy.tile(3290,4249);r.observePartner(MageStrategy.tile(3295,4245),107);
        Crab b=crab(r,68,100,14,Lane.SOUTH,'E',Style.MAGIC,3296,4235);r.obstructed.add(b.key);
        Decision d=new MageStrategy().decide(r,102);assertNull(d.target);assertNull(d.approach);
        assertEquals(Weapon.EYE,d.weapon);assertEquals(MageStrategy.tile(3292,4246),d.tile);
        assertTrue(d.reason.contains("shared middle-room"));
    }

    @Test public void clearStripPreparationCannotBreakTheStillActiveNorthTwelveSetup()
    {
        Room r=room();r.player=MageStrategy.tile(3290,4249);r.observePartner(MageStrategy.tile(3295,4245),107);
        Crab b=crab(r,68,100,14,Lane.SOUTH,'E',Style.MAGIC,3296,4235);r.obstructed.add(b.key);
        MageStrategy mage=new MageStrategy();Crab east=new Crab(45,45,76,11,Lane.EAST,'S',false,Style.MAGIC,MageStrategy.tile(3301,4249));
        mage.attacked(new Decision(east,Weapon.BARRAGE,r.player,false,"East barrage 11","East barrage 11"),77,76);
        Decision d=mage.decide(r,102);assertTrue(d.tile.getY()>=4249);
    }
}
