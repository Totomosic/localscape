package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageRequiredParentAgeProbe {
 @Test public void stillFinishesRequired24EastDuringActual29ButHonorsActual31Cutoff(){
 Room r=new Room();r.wave=29;r.spawns[29]=136;r.player=MageStrategy.tile(3295,4248);
 Crab old=new Crab(1,1,100,24,Lane.EAST,'S',true,Style.MELEE,MageStrategy.tile(3296,4248));old.hp=1;r.crabs.add(old);
 MageStrategy s=new MageStrategy();assertSame(old,s.decide(r,136).target);
 r.wave=31;r.spawns[31]=140;assertNull(s.decide(r,140).target);
 }
 @Test public void required23SouthLaterMagicRecoveryRemainsEligibleDuringActual28(){
 Room r=new Room();r.wave=28;r.spawns[28]=127;r.player=MageStrategy.tile(3295,4248);
 Crab old=new Crab(1,1,100,23,Lane.SOUTH,'W',true,Style.MAGIC,MageStrategy.tile(3296,4248));old.hp=1;r.crabs.add(old);
 Decision d=new MageStrategy().decide(r,127);assertSame(old,d.target);assertEquals(Weapon.EYE,d.weapon);
 }
 @Test public void oldOrdinarySplitDoesNotGainRequiredParentException(){
 Room r=new Room();r.wave=29;r.spawns[29]=136;r.player=MageStrategy.tile(3295,4248);
 Crab old=new Crab(1,1,100,24,Lane.SPLIT,(char)0,false,Style.MAGIC,MageStrategy.tile(3296,4248));r.crabs.add(old);
 assertNull(new MageStrategy().decide(r,136).target);
 }
}
