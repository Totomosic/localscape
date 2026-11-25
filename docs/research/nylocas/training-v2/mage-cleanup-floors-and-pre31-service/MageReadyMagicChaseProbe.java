package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageReadyMagicChaseProbe {
 private Crab magic,melee;
 private Room nativeFrame(){Room r=new Room();r.wave=31;r.spawns[28]=232;r.spawns[29]=236;r.spawns[30]=260;r.spawns[31]=268;r.ready=272;r.player=MageStrategy.tile(3296,4247);
 magic=new Crab(183,183,254,29,Lane.SPLIT,(char)0,false,Style.MAGIC,MageStrategy.tile(3292,4244));magic.pendingDamage=0;r.crabs.add(magic);
 melee=new Crab(191,191,267,30,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3297,4244));melee.pendingDamage=0;melee.fighting=false;r.crabs.add(melee);return r;}
 @Test public void native272ConfirmedInrangeMagicPrecedesNonadjacentSplitSulphurChase(){Room r=nativeFrame();Decision d=new MageStrategy().decide(r,272);assertSame(magic,d.target);assertEquals(Weapon.EYE,d.weapon);}
 @Test public void blockedMagicRayCannotPreemptTheMelee(){Room r=nativeFrame();r.obstructed.add(magic.key);assertSame(melee,new MageStrategy().decide(r,272).target);}
 @Test public void adjacentMeleeKeepsNewestReadyPriority(){Room r=nativeFrame();melee.point=MageStrategy.tile(3296,4246);assertSame(melee,new MageStrategy().decide(r,272).target);}
 @Test public void ownNullMagicCannotPreemptTheMelee(){Room r=nativeFrame();magic.nulled=true;assertSame(melee,new MageStrategy().decide(r,272).target);}
 @Test public void knownLethalMagicCannotPreemptTheMelee(){Room r=nativeFrame();magic.pendingDamage=8;assertSame(melee,new MageStrategy().decide(r,272).target);}
 @Test public void knownUnknownMagicClaimCannotPreemptTheMelee(){Room r=nativeFrame();magic.pendingDamage=-1;magic.claimedUntil=299;assertSame(melee,new MageStrategy().decide(r,272).target);}
 @Test public void partnerNullMeleeRetainsUrgentRescue(){Room r=nativeFrame();melee.partnerNulled=true;assertSame(melee,new MageStrategy().decide(r,272).target);}
 @Test public void cooldownStillPermitsApproachInsteadOfInventingCast(){Room r=nativeFrame();r.ready=275;Decision d=new MageStrategy().decide(r,272);assertNull(d.target);}
 @Test public void namedEast31StillPrecedesOrdinaryMagic(){Room r=nativeFrame();r.crabs.remove(melee);Crab c=new Crab(200,200,269,31,Lane.EAST,'N',false,Style.MAGIC,MageStrategy.tile(3300,4249));c.pendingDamage=0;r.crabs.add(c);assertSame(c,new MageStrategy().decide(r,272).target);}
 @Test public void dimmedMagicCannotPreemptTheMelee(){Room r=nativeFrame();r.spawns[29]=254;assertSame(melee,new MageStrategy().decide(r,272).target);}
 @Test public void noGenericBefore31Change(){Room r=nativeFrame();r.wave=30;assertSame(melee,new MageStrategy().decide(r,272).target);}
 @Test public void magicRequiringItsOwnApproachDoesNotPreemptMelee(){Room r=nativeFrame();magic.point=MageStrategy.tile(3286,4244);assertSame(melee,new MageStrategy().decide(r,272).target);}
 @Test public void olderSameWaveMeleeIntentCannotOverrideTheReadyCast(){Room r=nativeFrame();r.crabs.remove(melee);melee=new Crab(191,191,255,29,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3297,4244));melee.pendingDamage=0;r.crabs.add(melee);MageStrategy strategy=new MageStrategy();magic.nulled=true;r.ready=271;strategy.decide(r,271);magic.nulled=false;assertSame(magic,strategy.decide(r,272).target);}
 @Test public void normalScytheClusterKeepsItsAoePriority(){Room r=nativeFrame();melee.point=MageStrategy.tile(3297,4245);Crab third=new Crab(193,193,267,30,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3296,4246));third.pendingDamage=0;r.crabs.add(third);Crab second=new Crab(192,192,267,30,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3297,4246));second.pendingDamage=0;r.crabs.add(second);Decision d=new MageStrategy().decide(r,272);assertNotSame(magic,d.target);assertEquals(Weapon.SCYTHE,d.weapon);}
}

