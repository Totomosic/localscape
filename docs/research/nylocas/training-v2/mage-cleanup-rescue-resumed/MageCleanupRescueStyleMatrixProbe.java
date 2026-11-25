package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageCleanupRescueStyleMatrixProbe {
 @Test public void partnerNullRescueCoversEveryLegalCurrentStyleForSmallAndBigTargets() {
  for(boolean big:new boolean[]{false,true})for(Style style:Style.values()) {
   Room r=new Room();r.wave=31;r.ready=310;r.player=MageStrategy.tile(3295,4248);r.spawns[30]=280;r.spawns[31]=284;
   int born=big && style==Style.RANGE ? 290:280;
   Lane lane=big?(style==Style.MAGIC?Lane.EAST:Lane.WEST):Lane.SPLIT;
   Crab rescue=new Crab(1,1,born,big?30:31,lane,big?'S':(char)0,big,style,MageStrategy.tile(3293,4247));
   rescue.partnerNulled=true;rescue.pendingDamage=-1;rescue.claimedUntil=340;r.crabs.add(rescue);
   Crab ordinary=new Crab(2,2,300,31,Lane.SPLIT,(char)0,false,Style.MAGIC,MageStrategy.tile(3296,4249));ordinary.pendingDamage=0;r.crabs.add(ordinary);
   Decision d=new MageStrategy().decide(r,310);
   assertSame("big="+big+" style="+style,rescue,d.target);
   assertEquals(style==Style.MAGIC?Weapon.EYE:style==Style.RANGE?Weapon.PIPE:big?Weapon.SCYTHE:Weapon.SULPHUR,d.weapon);
  }
 }
}
