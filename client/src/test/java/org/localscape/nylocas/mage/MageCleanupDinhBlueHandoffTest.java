package org.localscape.nylocas.mage;
import static org.junit.Assert.*;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
public class MageCleanupDinhBlueHandoffTest
{
    private Room room()
    {
        Room r=new Room(); r.wave=31;r.spawns[29]=240;r.spawns[30]=254;r.spawns[31]=268;
        r.ready=297;r.specialEnergy=600;r.player=MageStrategy.tile(3298,4246);
        r.observePartner(MageStrategy.tile(3295,4248),298);
        small(r,198,3301,4245,Style.MELEE);small(r,199,3300,4245,Style.MELEE);
        small(r,201,3297,4245,Style.MAGIC); return r;
    }
    private Crab small(Room r,int key,int x,int y,Style style)
    { Crab c=new Crab(key,key,275,31,Lane.SPLIT,(char)0,false,style,MageStrategy.tile(x,y));c.pendingDamage=0;r.crabs.add(c);return c; }
    @Test public void oneFreshBlueCanGoToAlreadyCoolingReadyRanger()
    { Room r=room(); assertTrue(new MageStrategy().decide(r,297).special); }
    @Test public void concurrentReadyRangerCannotPromiseTheNextEye()
    { Room r=room();r.partnerReady=297;assertFalse(new MageStrategy().decide(r,297).special); }
    @Test public void longerCooldownCannotPromisePromptDispatch()
    { Room r=room();r.partnerReady=300;assertFalse(new MageStrategy().decide(r,297).special); }
    @Test public void secondFreshBlueVetoesTheHandoff()
    { Room r=room();small(r,202,3297,4246,Style.MAGIC);assertFalse(new MageStrategy().decide(r,297).special); }
    @Test public void rangerNullCannotBeDoubleNulled()
    { Room r=room();r.crabs.get(2).partnerNulled=true;assertFalse(new MageStrategy().decide(r,297).special); }
    @Test public void unknownDamageCannotBeHandedOff()
    { Room r=room();r.crabs.get(2).pendingDamage=-1;assertFalse(new MageStrategy().decide(r,297).special); }
    @Test public void greenOutsideHuntRadiusStillReservesTheRanger()
    { Room r=room();small(r,202,3291,4252,Style.RANGE);assertFalse(new MageStrategy().decide(r,297).special); }
    @Test public void westThirtyOutsideHuntRadiusStillReservesTheRanger()
    {
        Room r=room();Crab parent=new Crab(202,202,254,30,Lane.WEST,'S',true,Style.MELEE,MageStrategy.tile(3291,4251));
        parent.pendingDamage=0;r.crabs.add(parent);assertFalse(new MageStrategy().decide(r,297).special);
    }
    @Test public void archivedPartnerDefaultsCannotEnableBlueCollateral()
    { Room r=room();r.rangerPosition=null;r.partnerReady=0;assertFalse(new MageStrategy().decide(r,297).special); }
    @Test public void unreachableBlueCannotBePromised()
    { Room r=room();r.observePartner(MageStrategy.tile(3292,4254),298);assertFalse(new MageStrategy().decide(r,297).special); }

    @Test public void nativeSeed004UsesTheSameContactAsItsConfirmedLaterDinh()
    {
        Room r=new Room();r.wave=31;r.spawns[29]=240;r.spawns[31]=264;r.ready=287;r.specialEnergy=500;
        r.player=MageStrategy.tile(3293,4252);r.observePartner(MageStrategy.tile(3293,4250),288);
        small(r,198,3290,4252,Style.MELEE);small(r,203,3290,4252,Style.MELEE);small(r,204,3291,4252,Style.MAGIC);
        Decision d=new MageStrategy().decide(r,287);assertTrue(d.special);assertEquals(198,d.target.key);
        assertEquals(MageStrategy.tile(3291,4252),d.tile);
    }
}
