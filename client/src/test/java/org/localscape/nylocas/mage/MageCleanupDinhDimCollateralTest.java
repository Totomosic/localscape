package org.localscape.nylocas.mage;
import static org.junit.Assert.*;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
public class MageCleanupDinhDimCollateralTest
{
    private Room room()
    {
        Room r=new Room();r.wave=31;r.spawns[29]=255;r.spawns[30]=260;r.spawns[31]=268;
        r.ready=277;r.specialEnergy=500;r.player=MageStrategy.tile(3298,4246);
        add(r,198,270,31,Lane.SPLIT,false,Style.MELEE,3301,4245);
        add(r,199,270,31,Lane.SPLIT,false,Style.MELEE,3300,4245);return r;
    }
    private Crab add(Room r,int key,int born,int wave,Lane lane,boolean big,Style style,int x,int y)
    { Crab c=new Crab(key,key,born,wave,lane,'S',big,style,MageStrategy.tile(x,y));c.pendingDamage=0;r.crabs.add(c);return c; }
    @Test public void dimBlueSmallIsHarmlessWithoutRangerHandoff()
    { Room r=room();add(r,201,255,29,Lane.SPLIT,false,Style.MAGIC,3298,4245);assertTrue(new MageStrategy().decide(r,277).special); }
    @Test public void timestampAfterActual29StillNeedsHandoffEvenOnBirthWave29()
    { Room r=room();add(r,201,256,29,Lane.SPLIT,false,Style.MAGIC,3298,4245);assertFalse(new MageStrategy().decide(r,277).special); }
    @Test public void missingActual29DoesNotInferDimFromTheWaveNumber()
    { Room r=room();r.spawns[29]=-1;add(r,201,255,28,Lane.SPLIT,false,Style.MAGIC,3298,4245);assertFalse(new MageStrategy().decide(r,277).special); }
    @Test public void dimPartnerNullStillCannotBeDoubleNulled()
    { Room r=room();add(r,201,250,28,Lane.SPLIT,false,Style.MAGIC,3298,4245).partnerNulled=true;assertFalse(new MageStrategy().decide(r,277).special); }
    @Test public void dimWrongStyleUnassignedBigCanOnlyTakeZeroDamage()
    { Room r=room();add(r,201,250,28,Lane.EAST,true,Style.MAGIC,3298,4245);assertTrue(new MageStrategy().decide(r,277).special); }
    @Test public void freshWrongStyleBigStillVetoesCleanup()
    { Room r=room();add(r,201,256,28,Lane.EAST,true,Style.MAGIC,3298,4245);assertFalse(new MageStrategy().decide(r,277).special); }
    @Test public void matchingDimBigCannotMakeFreshChildren()
    { Room r=room();add(r,201,250,28,Lane.EAST,true,Style.MELEE,3298,4245);assertFalse(new MageStrategy().decide(r,277).special); }
    @Test public void matchingFreshUnassignedBigCannotMakeFreshChildrenEither()
    { Room r=room();add(r,201,256,28,Lane.EAST,true,Style.MELEE,3298,4245);assertFalse(new MageStrategy().decide(r,277).special); }
    @Test public void requiredDimParentStillKeepsItsResponsibility()
    { Room r=room();add(r,201,250,24,Lane.EAST,true,Style.MAGIC,3298,4245);assertFalse(new MageStrategy().decide(r,277).special); }
    @Test public void optionalWestTwentySevenIsUntouchedInEitherColor()
    {
        Room r=room();Crab big=add(r,201,250,27,Lane.WEST,true,Style.MAGIC,3298,4245);
        assertFalse(new MageStrategy().decide(r,277).special);big.style=Style.MELEE;assertFalse(new MageStrategy().decide(r,277).special);
        big.nulled=true;assertFalse(new MageStrategy().decide(r,277).special);
    }
    @Test public void dimBlueDoesNotConsumeTheSingleFreshBlueHandoff()
    {
        Room r=room();r.observePartner(MageStrategy.tile(3295,4248),278);
        add(r,201,270,31,Lane.SPLIT,false,Style.MAGIC,3298,4245);
        add(r,202,250,28,Lane.SPLIT,false,Style.MAGIC,3299,4245);
        assertTrue(new MageStrategy().decide(r,277).special);
    }

    @Test public void confirmedHiddenExpiryBigCannotConsumeAHuntSlotOrMakeChildren()
    {
        Room r=room();add(r,201,224,27,Lane.EAST,true,Style.MELEE,3298,4245);
        assertTrue(new MageStrategy().decide(r,277).special);
    }
    @Test public void boundaryBigStillUsesTheConservativeLiveSafetyScreen()
    {
        Room r=room();add(r,201,225,27,Lane.EAST,true,Style.MELEE,3298,4245);
        assertFalse(new MageStrategy().decide(r,277).special);
    }

    @Test public void currentlyMeleeDimBigCannotBorrowItsPredictedMagicFlicker()
    {
        Room r=room();r.spawns[29]=271;r.crabs.clear();
        add(r,198,275,31,Lane.SPLIT,false,Style.MELEE,3301,4245);
        add(r,199,275,31,Lane.SPLIT,false,Style.MELEE,3300,4245);
        add(r,201,271,28,Lane.EAST,true,Style.MELEE,3298,4245);
        assertFalse(new MageStrategy().decide(r,277).special);
    }
    @Test public void optionalWestTwentySevenOutsideTheSweepDoesNotCreateAWait()
    {
        Room r=room();add(r,201,250,27,Lane.WEST,true,Style.MAGIC,3290,4251);
        assertTrue(new MageStrategy().decide(r,277).special);
    }

    @Test public void nativeServices443UsesTheSameRunContactAsItsActualSulphur()
    {
        Room r=new Room();r.wave=31;r.spawns[29]=252;r.spawns[30]=256;r.spawns[31]=276;
        r.ready=300;r.specialEnergy=600;r.player=MageStrategy.tile(3298,4248);
        add(r,185,259,30,Lane.SPLIT,false,Style.MELEE,3301,4245);
        add(r,204,287,31,Lane.SPLIT,false,Style.MELEE,3300,4245);
        add(r,172,249,28,Lane.SPLIT,false,Style.MAGIC,3299,4244);
        Decision d=new MageStrategy().decide(r,300);assertTrue(d.special);assertEquals(204,d.target.key);
        assertEquals(MageStrategy.tile(3300,4246),d.tile);
    }
}
