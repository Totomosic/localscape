package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageIdleParentCooldownProbe {
 private Crab parent;
 private Room frame(){Room r=new Room();r.wave=31;r.spawns[28]=228;r.spawns[30]=256;r.spawns[31]=260;r.ready=304;r.player=MageStrategy.tile(3294,4253);r.specialEnergy=900;
 parent=new Crab(183,183,256,30,Lane.WEST,'S',true,Style.MELEE,MageStrategy.tile(3290,4245));parent.hp=15;parent.pendingDamage=0;r.crabs.add(parent);
 Crab own=new Crab(194,194,267,31,Lane.SPLIT,(char)0,false,Style.MAGIC,MageStrategy.tile(3292,4253));own.hp=7;own.pendingDamage=14;r.crabs.add(own);
 Crab old=new Crab(181,181,252,29,Lane.EAST,'N',false,Style.MAGIC,MageStrategy.tile(3301,4252));old.pendingDamage=0;r.crabs.add(old);return r;}
 @Test public void native302FrameMovesTowardParentDuringEyeCooldownForAge48Request(){Room r=frame();Decision d=new MageStrategy().decide(r,302);assertNull(d.target);assertSame(parent,d.approach);assertEquals(Weapon.SCYTHE,d.weapon);}
 @Test public void knownNonlethalPartnerClawsKeepTheApproachUseful(){Room r=frame();parent.pendingDamage=13;parent.claimedUntil=335;assertSame(parent,new MageStrategy().decide(r,302).approach);}
 @Test public void knownLethalParentCoverageAvoidsTheApproach(){Room r=frame();parent.pendingDamage=15;assertNull(new MageStrategy().decide(r,302).approach);}
 @Test public void ordinaryMagicDoesNotStopTheWest30LifecycleApproach(){Room r=frame();Crab own=r.crabs.get(1);own.pendingDamage=0;assertSame(parent,new MageStrategy().decide(r,302).approach);}
 @Test public void normalAttackCooldownStillPreventsAnEarlyLaunch(){Room r=frame();Decision d=new MageStrategy().decide(r,302);assertNull(d.target);}
}
