package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageIdleMeleeParentProbe {
 private Crab parent; private Room nativeFrame(){Room r=new Room();r.wave=31;r.spawns[30]=272;r.spawns[31]=276;r.ready=312;r.specialEnergy=900;r.player=MageStrategy.tile(3297,4249);
 parent=new Crab(185,185,272,30,Lane.WEST,'S',true,Style.MELEE,MageStrategy.tile(3290,4245));parent.hp=7;parent.pendingDamage=0;r.crabs.add(parent);
 Crab other=new Crab(197,197,279,31,Lane.SPLIT,(char)0,false,Style.RANGE,MageStrategy.tile(3290,4252));other.hp=3;other.pendingDamage=5;r.crabs.add(other);return r;}
 private Crab selected(Decision d){return d.target!=null?d.target:d.approach;}
 @Test public void nativeHealthyParentGetsAnApproachBeforeExpiryWhileOtherBodyIsAlive(){Room r=nativeFrame();Decision d=new MageStrategy().decide(r,312);assertSame(parent,selected(d));assertEquals(Weapon.SCYTHE,d.weapon);}
 @Test public void ownReadyMagicPreemptsIdleParentHelp(){Room r=nativeFrame();Crab own=new Crab(3,3,300,31,Lane.SPLIT,(char)0,false,Style.MAGIC,MageStrategy.tile(3298,4249));own.pendingDamage=0;r.crabs.add(own);assertSame(own,new MageStrategy().decide(r,312).target);}
 @Test public void ownFutureMeleeApproachAlsoPreemptsIdleParentHelp(){Room r=nativeFrame();Crab own=new Crab(3,3,300,31,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3301,4253));own.pendingDamage=0;r.crabs.add(own);assertSame(own,selected(new MageStrategy().decide(r,312)));}
 @Test public void ownNullStillBlocksTheParent(){Room r=nativeFrame();parent.nulled=true;assertNull(new MageStrategy().decide(r,312).target);}
 @Test public void knownLethalFlightStillProtectsTheParent(){Room r=nativeFrame();parent.pendingDamage=7;assertNull(new MageStrategy().decide(r,312).target);}
 @Test public void knownNonlethalFlightAllowsUsefulHelp(){Room r=nativeFrame();parent.pendingDamage=1;assertSame(parent,selected(new MageStrategy().decide(r,312)));}
 @Test public void unknownPartnerClaimRemainsConservative(){Room r=nativeFrame();parent.pendingDamage=-1;parent.claimedUntil=330;assertNull(new MageStrategy().decide(r,312).target);}
 @Test public void parentPastNormalAttackLifetimeRemainsIneligible(){Room r=nativeFrame();assertNull(new MageStrategy().decide(r,321).target);}
 @Test public void idleHelperDoesNotInventAnAttackDuringCooldown(){Room r=nativeFrame();r.ready=314;assertNull(new MageStrategy().decide(r,312).target);}
 @Test public void helperIsLimitedToActual31(){Room r=nativeFrame();r.wave=30;assertNull(new MageStrategy().decide(r,312).target);}
}
