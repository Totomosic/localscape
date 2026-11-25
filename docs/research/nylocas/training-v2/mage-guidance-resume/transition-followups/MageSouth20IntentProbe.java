package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageSouth20IntentProbe {
 private MageStrategy s;private Crab parent,small;
 private Room nativeFrames(){s=new MageStrategy();Room r=new Room();r.wave=20;r.spawns[20]=156;r.player=MageStrategy.tile(3293,4244);r.ready=164;r.specialEnergy=900;
 parent=new Crab(96,96,156,20,Lane.SOUTH,'W',true,Style.MAGIC,MageStrategy.tile(3295,4234));r.crabs.add(parent);
 small=new Crab(101,101,159,20,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3295,4246));small.pendingDamage=0;r.crabs.add(small);
 s.attacked(new Decision(parent,Weapon.EYE,parent.point,false,"South opener 20","South opener 20"),158,157);parent.ownHitUntil=0;parent.hp=11;parent.pendingDamage=0;parent.style=Style.RANGE;parent.point=MageStrategy.tile(3295,4238);
 Decision before=s.decide(r,161);assertSame(small,before.target!=null?before.target:before.approach);
 r.player=MageStrategy.tile(3295,4246);parent.style=Style.MELEE;parent.point=MageStrategy.tile(3295,4240);small.point=MageStrategy.tile(3297,4248);return r;}
 @Test public void nativeSouth20MeleeFlickerReplacesOlderSmallApproachWhenEquallyEarly(){Room r=nativeFrames();Decision d=s.decide(r,163);assertSame(parent,d.target!=null?d.target:d.approach);}
 @Test public void provenLethalParentDoesNotDisplaceUsefulSmallWork(){Room r=nativeFrames();parent.pendingDamage=11;Decision d=s.decide(r,163);assertSame(small,d.target!=null?d.target:d.approach);}
 @Test public void earlierSmallStillWinsIfParentCannotReachTheAttackBudget(){Room r=nativeFrames();r.player=MageStrategy.tile(3290,4249);parent.point=MageStrategy.tile(3301,4243);small.point=MageStrategy.tile(3291,4249);Decision d=s.decide(r,163);assertSame(small,d.target!=null?d.target:d.approach);}
 @Test public void required21BarrageStillPreemptsTheUrgentSouth20Survivor(){Room r=nativeFrames();r.wave=21;r.spawns[21]=172;r.ready=172;r.player=MageStrategy.tile(3290,4249);parent.point=MageStrategy.tile(3291,4248);parent.hp=1;
 Crab west=new Crab(112,112,172,21,Lane.WEST,'N',false,Style.MAGIC,MageStrategy.tile(3281,4249));r.crabs.add(west);
 Decision d=s.decide(r,172);assertSame(west,d.target);assertEquals("21 west barrage",d.rule);}

}
