package org.localscape.nylocas.ranger;
import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;

public class RangerSouthSixSevenChainTest
{
    private final RangerStrategy ranger=new RangerStrategy();
    private final Room room=new Room();
    private final Crab westFive, westFour, ordinary;
    public RangerSouthSixSevenChainTest()
    {
        room.wave=5;room.spawns[4]=16;room.spawns[5]=20;
        room.player=RangerStrategy.tile(3295,4243);room.ready=35;room.specialEnergy=1000;
        westFive=add(13,20,5,Lane.WEST,'S',true,Style.RANGE,3290,4248);
        westFour=add(10,16,4,Lane.WEST,'N',false,Style.RANGE,3290,4249);
        ranger.attacked(new Decision(westFive,Weapon.PIPE,room.player,false,"West big 5","Actual opening"),23);
        ranger.attacked(new Decision(westFour,Weapon.PIPE,room.player,false,"West small 4","Actual opening"),25);
        westFive.hp=0;westFive.dying=true;westFour.hp=0;westFour.dying=true;
        ordinary=add(16,28,5,Lane.SPLIT,(char)0,false,Style.MAGIC,3292,4243);
    }
    private Crab add(int key,int born,int wave,Lane lane,char slot,boolean big,Style style,int x,int y)
    {
        Crab c=new Crab(key,key,born,wave,lane,slot,big,style,RangerStrategy.tile(x,y));
        c.pendingDamage=0;room.crabs.add(c);return c;
    }
    private boolean chain(Decision d){return d.reason.startsWith("Prepare South6 -> South7 chain");}
    private Crab southSix(int request)
    {
        room.wave=6;room.spawns[6]=36;
        Crab six=add(21,36,6,Lane.SOUTH,'W',false,Style.RANGE,3295,4234);
        ranger.attacked(new Decision(six,Weapon.CHIN,room.player,false,"South chin 6","Actual chin"),request+1);
        six.pendingDamage=8;room.ready=request+3;return six;
    }
    @Test public void oneReadyWaitKeepsTheChinAndBowWindows()
    { Decision d=ranger.decide(room,35);assertTrue(d.reason,chain(d));assertEquals(Weapon.CHIN,d.weapon);assertNull(d.target); }
    @Test public void anEyeThatStillLeavesChinByOneCanGoNow()
    { room.ready=34;Decision d=ranger.decide(room,34);assertSame(ordinary,d.target);assertEquals(Weapon.EYE,d.weapon); }
    @Test public void aHealthyUnopenedFourTickMeleeCanYieldTwoTicks()
    {
        room.ready=34;ordinary.style=Style.MELEE;ordinary.point=RangerStrategy.tile(3295,4244);
        assertTrue(chain(ranger.decide(room,34)));assertTrue(chain(ranger.decide(room,35)));
    }
    @Test public void aReadyPipeKeepsItsTwoTickWorkUnit()
    {
        ordinary.style=Style.RANGE;ordinary.point=RangerStrategy.tile(3295,4241);
        Decision d=ranger.decide(room,35);assertSame(ordinary,d.target);assertEquals(Weapon.PIPE,d.weapon);
    }
    @Test public void aRealOrdinaryZeroRollKeepsItsNextEye()
    {
        ranger.attacked(new Decision(ordinary,Weapon.EYE,room.player,false,null,"Actual zero"),33);
        ordinary.pendingDamage=0;Decision d=ranger.decide(room,35);assertFalse(chain(d));assertSame(ordinary,d.target);
    }
    @Test public void aWoundedOrdinarySmallKeepsItsFinish()
    { ordinary.hp=2;Decision d=ranger.decide(room,35);assertFalse(chain(d));assertSame(ordinary,d.target); }
    @Test public void aPartnerNullRescueIsNotPostponed()
    { ordinary.partnerNulled=true;assertFalse(chain(ranger.decide(room,35))); }
    @Test public void anUnknownFlightIsNotAnUnusedOrdinarySlot()
    { ordinary.pendingDamage=-1;assertFalse(chain(ranger.decide(room,35))); }
    @Test public void aSurvivingWestFiveRetainsItsNamedRecovery()
    { westFive.hp=1;westFive.dying=false;assertFalse(chain(ranger.decide(room,35))); }
    @Test public void anUnknownWestFiveFlightDoesNotCountAsCovered()
    {
        westFive.hp=8;westFive.dying=false;westFive.pendingDamage=-1;westFive.ownHitUntil=40;westFive.ownHitHp=8;
        assertFalse(chain(ranger.decide(room,35)));
    }
    @Test public void aFailedSixAdmissionDoesNotFabricateANewWave()
    { room.ready=36;assertFalse(chain(ranger.decide(room,36))); }
    @Test public void twoWaitsDoNotBecomeAnUnlimitedReadyHold()
    {
        room.ready=34;ordinary.style=Style.MELEE;ordinary.point=RangerStrategy.tile(3295,4244);
        assertTrue(chain(ranger.decide(room,34)));assertTrue(chain(ranger.decide(room,35)));
        assertFalse(chain(ranger.decide(room,36)));
    }
    @Test public void ordinaryCooldownMovementDoesNotLeaveTheSouthRay()
    {
        southSix(37);Decision d=ranger.decide(room,38);assertTrue(d.reason,chain(d));assertEquals(Weapon.BOW,d.weapon);
        assertEquals(RangerStrategy.tile(3295,4243),d.tile);assertNull(d.target);
    }
    @Test public void theBirthBowLeavesTheExplicitEastSixSpecialAtAgeNine()
    {
        southSix(37);Crab east=add(22,36,6,Lane.EAST,'S',true,Style.MELEE,3305,4248);
        assertTrue(chain(ranger.decide(room,38)));assertTrue(chain(ranger.decide(room,39)));
        room.wave=7;room.spawns[7]=40;room.ready=40;
        Crab south=add(23,40,7,Lane.SOUTH,'W',true,Style.RANGE,3295,4233);
        Decision bow=ranger.decide(room,40);assertSame(bow.reason,south,bow.target);assertEquals(Weapon.BOW,bow.weapon);
        ranger.attacked(bow,41);south.pendingDamage=16;
        room.ready=45;room.player=RangerStrategy.tile(3300,4248);east.point=RangerStrategy.tile(3299,4248);
        Decision claw=ranger.decide(room,45);assertSame(claw.reason,east,claw.target);assertEquals(Weapon.CLAWS,claw.weapon);assertTrue(claw.special);
    }
    @Test public void aSurvivingSixGreenKeepsItsActualPipeBeforeTheBow()
    {
        Crab six=southSix(36);six.pendingDamage=0;six.hp=1;six.point=RangerStrategy.tile(3295,4237);
        room.ready=39;Decision d=ranger.decide(room,39);assertFalse(chain(d));assertSame(six,d.target);assertEquals(Weapon.PIPE,d.weapon);
    }
    @Test public void aLateChinDoesNotCauseAFutileBowReservation()
    { southSix(38);assertFalse(chain(ranger.decide(room,39))); }
    @Test public void anUnknownSouthSixFlightDoesNotAuthorizeTheNextBow()
    {
        Crab six=southSix(36);six.pendingDamage=-1;six.ownHitUntil=40;six.ownHitHp=8;
        room.ready=39;assertFalse(chain(ranger.decide(room,39)));
    }
    @Test public void anAlreadyCoveredSixDoesNotCauseAChinWait()
    {
        room.wave=6;room.spawns[6]=36;room.ready=36;
        Crab six=add(21,36,6,Lane.SOUTH,'W',false,Style.RANGE,3295,4233);six.pendingDamage=8;
        assertFalse(chain(ranger.decide(room,36)));
    }
    @Test public void thePhysicalSouthReturnMustFitBeforeTheBow()
    {
        southSix(37);room.player=RangerStrategy.tile(3290,4251);
        assertFalse(chain(ranger.decide(room,39)));
    }
}
