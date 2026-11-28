package org.localscape.nylocas.mage;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
public class MageUpcomingBlueCooldownTest {
 private static Room room(){Room r=new Room();r.wave=5;r.spawns[5]=20;r.ready=36;r.player=MageStrategy.tile(3293,4248);Crab c=new Crab(1,1,20,5,Lane.SOUTH,'E',false,Style.MELEE,MageStrategy.tile(3299,4243));c.pendingDamage=0;r.crabs.add(c);return r;}
 @Test public void stagesForTheTypedWestSixShotInsteadOfAHealthyFarMelee(){Room r=room();Decision d=new MageStrategy().decide(r,33);assertEquals(Weapon.EYE,d.weapon);assertEquals(MageStrategy.tile(3290,4249),d.tile);assertTrue(d.reason,d.reason.startsWith("Prepare upcoming West magic 6"));}
 @Test public void doesNotReplaceAReadyAttackWithANewReadyHold(){Room r=room();Decision d=new MageStrategy().decide(r,36);assertFalse(d.reason,d.reason.startsWith("Prepare upcoming"));}
 @Test public void keepsAWoundedMeleeFollowup(){Room r=room();r.crabs.get(0).hp=4;Decision d=new MageStrategy().decide(r,33);assertFalse(d.reason,d.reason.startsWith("Prepare upcoming"));}
 @Test public void keepsAConfirmedAttackEvenWhenTheRollHasNotChangedItsHp(){Room r=room();MageStrategy s=new MageStrategy();s.attacked(new Decision(r.crabs.get(0),Weapon.SULPHUR,r.player,false,null,"prior attack"),33,32);Decision d=s.decide(r,33);assertFalse(d.reason,d.reason.startsWith("Prepare upcoming"));}
 @Test public void neverMovesAwayFromTheUncoveredSouthFourParent(){Room r=room();Crab c=new Crab(2,2,16,4,Lane.SOUTH,'W',true,Style.MAGIC,MageStrategy.tile(3295,4242));c.pendingDamage=0;r.crabs.add(c);Decision d=new MageStrategy().decide(r,33);assertFalse(d.reason,d.reason.startsWith("Prepare upcoming"));}
}