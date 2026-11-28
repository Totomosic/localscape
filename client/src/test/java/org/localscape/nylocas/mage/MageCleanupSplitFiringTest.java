package org.localscape.nylocas.mage;
import static org.junit.Assert.*;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
public class MageCleanupSplitFiringTest
{
    private Room room()
    {
        Room r=new Room();r.wave=31;r.spawns[29]=240;r.spawns[30]=250;r.spawns[31]=260;
        r.ready=274;r.player=MageStrategy.tile(3299,4248);r.observePartner(MageStrategy.tile(3301,4248),274);
        crab(r,198,260,31,Lane.SPLIT,false,Style.MAGIC,3296,4248);
        crab(r,155,220,27,Lane.WEST,true,Style.MAGIC,3290,4251);return r;
    }
    private Crab crab(Room r,int key,int born,int wave,Lane lane,boolean big,Style style,int x,int y)
    {Crab c=new Crab(key,key,born,wave,lane,'S',big,style,MageStrategy.tile(x,y));c.pendingDamage=0;r.crabs.add(c);return c;}
    private boolean split(Decision d){return d.reason.startsWith("Stage cleanup split");}
    @Test public void committedEyeKeepsFastFlightAndGetsNearBothKnownWestChildren()
    {
        Room r=room();Decision d=new MageStrategy().decide(r,272);assertTrue(split(d));assertNull(d.target);assertNull(d.approach);
        assertEquals(Weapon.EYE,d.weapon);assertEquals(3295,d.tile.getX());
        assertTrue(d.tile.distanceTo(r.crabs.get(1).point)<=5);
        assertTrue(d.tile.distanceTo(r.crabs.get(0).point)<=2);assertTrue(d.reason.contains("@274"));assertTrue(d.reason.contains("@275"));
    }
    @Test public void readyAttackIsNeverReplacedByAnApproach()
    {Room r=room();r.ready=272;assertFalse(split(new MageStrategy().decide(r,272)));}
    @Test public void slowUtilityIntervalCannotInventFreeGroundCycles()
    {Room r=room();r.ready=276;assertFalse(split(new MageStrategy().decide(r,272)));}
    @Test public void rangerAlreadyCoveringBothAnchorsPreventsDuplicateStaging()
    {Room r=room();r.observePartner(MageStrategy.tile(3291,4250),274);assertFalse(split(new MageStrategy().decide(r,272)));}
    @Test public void lateBusyRangerDoesNotPretendToCoverTheBirth()
    {Room r=room();r.observePartner(MageStrategy.tile(3291,4250),280);assertTrue(split(new MageStrategy().decide(r,272)));}
    @Test public void explicitActualTwentyNineCutoffIsRequired()
    {Room r=room();r.spawns[29]=-1;assertFalse(split(new MageStrategy().decide(r,272)));}
    @Test public void dimCurrentTargetNeverJustifiesARealShot()
    {Room r=room();r.spawns[29]=260;assertFalse(split(new MageStrategy().decide(r,272)));}
    @Test public void knownLethalCurrentFlightAndUnknownDamageDoNotStageAnotherShot()
    {
        Room r=room();r.crabs.get(0).pendingDamage=8;assertFalse(split(new MageStrategy().decide(r,272)));
        r.crabs.get(0).pendingDamage=-1;assertFalse(split(new MageStrategy().decide(r,272)));
    }
    @Test public void movingOrFightingCurrentTargetCannotBorrowAStableRay()
    {
        Room r=room();r.crabs.get(0).fighting=true;assertFalse(split(new MageStrategy().decide(r,272)));
        r=room();r.crabs.get(0).observePosition(MageStrategy.tile(3295,4248));assertFalse(split(new MageStrategy().decide(r,272)));
    }
    @Test public void originalWestThirtyNeedsAConfirmedDeathClock()
    {
        Room r=room();r.crabs.remove(1);crab(r,185,220,30,Lane.WEST,true,Style.MELEE,3290,4251);
        assertFalse(split(new MageStrategy().decide(r,272)));
        r.crabs.get(1).hp=0;r.crabs.get(1).dying=true;r.crabs.get(1).observeSplitAt(275);
        assertTrue(split(new MageStrategy().decide(r,272)));
    }
    @Test public void futureClockWithoutConfirmedDeadBodyIsNotASplit()
    {Room r=room();r.crabs.get(1).observeSplitAt(275);assertFalse(split(new MageStrategy().decide(r,272)));}
    @Test public void naturalForecastCannotUsePendingParentDamage()
    {Room r=room();r.crabs.get(1).pendingDamage=1;assertFalse(split(new MageStrategy().decide(r,272)));}
    @Test public void futureBirthMustStayInsideTheImminentWindow()
    {Room r=room();r.crabs.remove(1);crab(r,155,224,27,Lane.WEST,true,Style.MAGIC,3290,4251);assertFalse(split(new MageStrategy().decide(r,272)));}
    @Test public void visibleFreshRedKeepsItsPhysicalContactDistance()
    {
        Room r=room();r.crabs.get(0).hp=1;crab(r,200,260,31,Lane.SPLIT,false,Style.MELEE,3300,4245);
        Decision d=new MageStrategy().decide(r,272);assertEquals(Weapon.EYE,d.weapon);assertFalse(split(d));
    }
    @Test public void protectedFirstSouthThirtyGreenStillKeepsItsWindow()
    {
        Room r=room();r.spawns[30]=268;crab(r,181,268,30,Lane.SOUTH,false,Style.RANGE,3296,4242);
        assertFalse(split(new MageStrategy().decide(r,272)));
    }
    @Test public void committedPipeRetainsItsNativeFlightWhileMovingTowardKnownChildren()
    {
        Room r=room();r.crabs.get(0).style=Style.RANGE;r.crabs.get(0).partnerNulled=true;
        Decision d=new MageStrategy().decide(r,272);assertEquals(Weapon.PIPE,d.weapon);assertTrue(split(d));
        assertTrue(d.tile.distanceTo(r.crabs.get(0).point)<=5);
    }

    @Test public void woundedNaturalParentNeedsAConfirmedClockInsteadOfAGuessedExpiry()
    {Room r=room();r.crabs.get(1).hp=3;assertFalse(split(new MageStrategy().decide(r,272)));}
}
