package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageWest30MeleeTakeoverProbe {
 private Crab parent,small;
 private Room nativeFrame(){Room r=new Room();r.wave=31;r.spawns[28]=228;r.spawns[30]=256;r.spawns[31]=260;r.ready=282;r.specialEnergy=900;r.player=MageStrategy.tile(3294,4246);
 parent=new Crab(185,185,256,30,Lane.WEST,'S',true,Style.MELEE,MageStrategy.tile(3290,4245));parent.hp=4;parent.pendingDamage=0;r.crabs.add(parent);
 small=new Crab(200,200,271,31,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3292,4244));small.pendingDamage=0;r.crabs.add(small);return r;}
 @Test public void nativePermanentMeleeParentGetsScytheInsteadOfOrdinarySmallAt282(){Room r=nativeFrame();Decision d=new MageStrategy().decide(r,282);assertSame(parent,d.target);assertEquals(Weapon.SCYTHE,d.weapon);}
 @Test public void existingSameWaveSmallIntentCannotOverrideParentTakeover(){Room r=nativeFrame();small=new Crab(201,201,257,30,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3292,4244));r.crabs.set(1,small);MageStrategy s=new MageStrategy();parent.style=Style.RANGE;assertSame(small,s.decide(r,281).approach);parent.style=Style.MELEE;assertSame(parent,s.decide(r,282).target);}
 @Test public void partnerNullRescueStillPreemptsHealthyParent(){Room r=nativeFrame();Crab rescue=new Crab(3,3,271,31,Lane.SPLIT,(char)0,false,Style.MAGIC,MageStrategy.tile(3293,4248));rescue.partnerNulled=true;rescue.pendingDamage=0;r.crabs.add(rescue);assertSame(rescue,new MageStrategy().decide(r,282).target);}
 @Test public void ownEast30MustRemainCoveredFirst(){Room r=nativeFrame();Crab east=new Crab(3,3,256,30,Lane.EAST,'S',true,Style.MAGIC,MageStrategy.tile(3298,4248));east.pendingDamage=0;r.crabs.add(east);assertNotSame(parent,new MageStrategy().decide(r,282).target);}
 @Test public void ownNamedEast31MustRemainCoveredFirst(){Room r=nativeFrame();Crab east=new Crab(3,3,260,31,Lane.EAST,'N',false,Style.MAGIC,MageStrategy.tile(3298,4248));east.pendingDamage=0;r.crabs.add(east);assertSame(east,new MageStrategy().decide(r,282).target);}
 @Test public void knownLethalOwnEastCoverageReleasesTakeover(){Room r=nativeFrame();Crab east=new Crab(3,3,256,30,Lane.EAST,'S',true,Style.MAGIC,MageStrategy.tile(3298,4248));east.pendingDamage=16;r.crabs.add(east);assertSame(parent,new MageStrategy().decide(r,282).target);}
 @Test public void dimmedParentRemainsIgnored(){Room r=nativeFrame();r.spawns[28]=240;assertNotSame(parent,new MageStrategy().decide(r,282).target);}
 @Test public void ownNullStillBlocksParent(){Room r=nativeFrame();parent.nulled=true;assertNotSame(parent,new MageStrategy().decide(r,282).target);}
 @Test public void knownLethalParentFlightStillYields(){Room r=nativeFrame();parent.pendingDamage=4;assertNotSame(parent,new MageStrategy().decide(r,282).target);}
 @Test public void rangeWindowIsNotStolen(){Room r=nativeFrame();parent.style=Style.RANGE;r.ready=281;assertSame(small,new MageStrategy().decide(r,281).target);}
}
