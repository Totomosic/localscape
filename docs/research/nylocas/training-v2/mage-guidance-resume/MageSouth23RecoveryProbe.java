package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageSouth23RecoveryProbe {
 @Test public void backsUpSouth23BeforeOpeningNew24WhenBothCanAttackNow(){
 Room r=new Room();r.wave=24;r.spawns[24]=108;r.player=MageStrategy.tile(3295,4244);
 Crab old=new Crab(1,1,100,23,Lane.SOUTH,'W',true,Style.MELEE,MageStrategy.tile(3295,4243));old.hp=6;r.crabs.add(old);
 Crab fresh=new Crab(2,2,108,24,Lane.SOUTH,'W',true,Style.MAGIC,MageStrategy.tile(3296,4244));r.crabs.add(fresh);
 assertSame(old,new MageStrategy().decide(r,111).target);
 }
 @Test public void south23StillBelongsToMageIfItSurvivesUntilItsLaterMagicPhase(){
 Room r=new Room();r.wave=27;r.spawns[27]=125;r.player=MageStrategy.tile(3295,4244);
 Crab old=new Crab(1,1,100,23,Lane.SOUTH,'W',true,Style.MAGIC,MageStrategy.tile(3295,4243));old.hp=6;r.crabs.add(old);
 Decision d=new MageStrategy().decide(r,127);assertSame(old,d.target);assertEquals(Weapon.EYE,d.weapon);
 }
 @Test public void doesNotOpenSouth23BeforeItsNormalMeleeBackupWindow(){
 Room r=new Room();r.wave=23;r.spawns[23]=100;r.player=MageStrategy.tile(3295,4244);
 Crab old=new Crab(1,1,100,23,Lane.SOUTH,'W',true,Style.MAGIC,MageStrategy.tile(3295,4238));r.crabs.add(old);
 assertNull(new MageStrategy().decide(r,105).target);
 }
}
