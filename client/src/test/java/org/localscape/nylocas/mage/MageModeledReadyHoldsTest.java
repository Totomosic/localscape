package org.localscape.nylocas.mage;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
public class MageModeledReadyHoldsTest {
 private static Room room(int count,int split) {
  Room r=new Room();r.wave=16;r.spawns[16]=116;r.player=MageStrategy.tile(3298,4251);
  Crab old=new Crab(1,1,86,12,Lane.SPLIT,(char)0,false,Style.MELEE,r.player.dy(1));old.pendingDamage=0;r.crabs.add(old);
  Crab parent=new Crab(2,2,108,15,Lane.EAST,'S',true,Style.MELEE,MageStrategy.tile(3298,4247));parent.hp=0;parent.dying=true;parent.observeSplitAt(split);r.crabs.add(parent);
  for(int i=2;i<count;i++){Crab filler=new Crab(i+1,i+1,116,16,Lane.SPLIT,(char)0,false,Style.RANGE,MageStrategy.tile(3290,4243));filler.pendingDamage=0;r.crabs.add(filler);}
  return r;
 }
 @Test public void servesImmediateCapClearanceInsteadOfAContestedTwoTickHold() {
  Room r=room(12,122);assertSame(r.crabs.get(0),new MageStrategy().decide(r,120).target);
 }
 @Test public void stillHoldsWhenTheSameRoomBenefitsFromANearerSplit() {
  Decision d=new MageStrategy().decide(room(12,121),120);assertNull(d.target);assertTrue(d.reason,d.reason.startsWith("Wait for fresh crab"));
 }
 @Test public void cooldownPositioningStillCostsNoReadyAttack() {
  Room r=room(12,122);r.ready=122;Decision d=new MageStrategy().decide(r,120);assertNull(d.target);assertTrue(d.reason,d.reason.startsWith("Approach fresh during cooldown"));
 }
}