package org.localscape.nylocas.mage;

import static org.junit.Assert.*;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;

public class MageCleanupDinhReachTest
{
    private Room room(int ready)
    {
        Room r = new Room(); r.wave = 31; r.spawns[29] = 240; r.spawns[30] = 254;
        r.spawns[31] = 268; r.ready = ready; r.specialEnergy = 600;
        r.player = MageStrategy.tile(3298,4246); return r;
    }
    private Crab small(Room r, int key, int x, int y)
    {
        Crab c = new Crab(key,key,275,31,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(x,y));
        c.pendingDamage = 0; r.crabs.add(c); return c;
    }
    private void cluster(Room r) { small(r,198,3301,4245); small(r,199,3300,4245); }
    @Test public void nativeSeed895ReadyRunReachesActualPrimaryAndBothReds()
    {
        Room r=room(297); cluster(r);
        Decision d=new MageStrategy().decide(r,297);
        assertEquals("Cleanup Dinh",d.rule); assertEquals(199,d.target.key);
        assertEquals(MageStrategy.tile(3299,4245),d.tile); assertTrue(d.special);
    }
    @Test public void cooldownQueuesExplicitSafeGroundContact()
    {
        Room r=room(300); cluster(r); r.player=MageStrategy.tile(3296,4248);
        Decision d=new MageStrategy().decide(r,297);
        assertEquals(Weapon.DINH,d.weapon); assertNull(d.target); assertNull(d.approach);
        assertTrue(d.reason.contains("during cooldown")); assertFalse(d.special);
    }
    @Test public void aMidpointWithoutPrimaryContactIsNotASweep()
    {
        Room r=room(297); r.player=MageStrategy.tile(3296,4252);
        small(r,188,3292,4254); small(r,199,3299,4254);
        assertFalse(new MageStrategy().decide(r,297).special);
    }
    @Test public void insufficientEnergyCannotSpendSpec()
    { Room r=room(297); cluster(r); r.specialEnergy=499; assertFalse(new MageStrategy().decide(r,297).special); }
    @Test public void knownLethalAndDimBodiesDoNotSupplySecondUsefulVictim()
    {
        Room r=room(297); cluster(r); r.crabs.get(0).pendingDamage=8;
        assertFalse(new MageStrategy().decide(r,297).special);
        r.crabs.clear(); small(r,198,3301,4245);
        Crab dim=new Crab(199,199,240,31,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3300,4245));
        dim.pendingDamage=0; r.crabs.add(dim);
        assertFalse(new MageStrategy().decide(r,297).special);
    }
    @Test public void unclaimedBlueAndPartnerNulledBlueStillVetoCoreSweep()
    {
        Room r=room(297); cluster(r); Crab blue=small(r,201,3298,4245); blue.style=Style.MAGIC;
        assertFalse(new MageStrategy().decide(r,297).special);
        blue.partnerNulled=true; assertFalse(new MageStrategy().decide(r,297).special);
    }
    @Test public void southThirtyFirstGreenCannotBeNulled()
    {
        Room r=room(280); cluster(r); r.spawns[30]=276;
        Crab green=new Crab(202,202,276,30,Lane.SOUTH,'E',false,Style.RANGE,MageStrategy.tile(3298,4245));
        green.pendingDamage=0; r.crabs.add(green);
        assertFalse(new MageStrategy().decide(r,280).special);
    }
    @Test public void allocatedWestThirtyParentCannotBeClipped()
    {
        Room r=room(297); cluster(r);
        Crab parent=new Crab(202,202,254,30,Lane.WEST,'S',true,Style.MELEE,MageStrategy.tile(3298,4245));
        parent.pendingDamage=0; r.crabs.add(parent);
        assertFalse(new MageStrategy().decide(r,297).special);
    }

    @Test public void anotherConfirmedCleanupSweepNeedsActualNewEnergyAndVictims()
    {
        Room r=room(297); cluster(r); r.specialEnergy=1000; MageStrategy mage=new MageStrategy();
        Decision first=mage.decide(r,297); assertTrue(first.special); mage.attacked(first,298,297);
        r.crabs.clear(); small(r,208,3301,4245); small(r,209,3300,4245);r.ready=302;r.specialEnergy=500;
        assertTrue(mage.decide(r,302).special);
        r.specialEnergy=0;assertFalse(mage.decide(r,302).special);
    }
    @Test public void unsafeTiedNativeContactCannotBeChosenByItsSafeAlternative()
    {
        Room r=room(297);r.player=MageStrategy.tile(3296,4248);
        small(r,198,3298,4250);small(r,199,3301,4245);
        Crab blue=small(r,201,3292,4254);blue.style=Style.MAGIC;
        assertFalse(new MageStrategy().decide(r,297).special);
    }
}
