package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageLoneRangedProbe {
 private Room room(){Room r=new Room();r.wave=31;r.spawns[31]=100;r.player=MageStrategy.tile(3295,4248);return r;}
 private Crab small(Room r,int key,int wave){Crab c=new Crab(key,key,100,wave,Lane.SPLIT,(char)0,false,Style.RANGE,MageStrategy.tile(3296,4248));r.crabs.add(c);return c;}
 @Test public void loneFreshRangedSmallGetsNormalPipeHelp(){Room r=room();Crab c=small(r,1,31);Decision d=new MageStrategy().decide(r,110);assertSame(c,d.target);assertEquals(Weapon.PIPE,d.weapon);}
 @Test public void loneFreshRangedBigGetsNormalPipeHelp(){Room r=room();Crab c=new Crab(1,1,100,30,Lane.WEST,'S',true,Style.RANGE,MageStrategy.tile(3296,4248));r.crabs.add(c);Decision d=new MageStrategy().decide(r,110);assertSame(c,d.target);assertEquals(Weapon.PIPE,d.weapon);}
 @Test public void ignoredOldLivingCrabStillPreventsTheLoneException(){Room r=room();small(r,1,31);small(r,2,29);assertNull(new MageStrategy().decide(r,110).target);}
 @Test public void dyingOldCrabDoesNotPreventTheLoneException(){Room r=room();Crab c=small(r,1,31);Crab old=small(r,2,29);old.dying=true;Decision d=new MageStrategy().decide(r,110);assertSame(c,d.target);}
 @Test public void partnerFlightStillProtectsTheLoneTarget(){Room r=room();Crab c=small(r,1,31);c.claimedUntil=115;assertNull(new MageStrategy().decide(r,110).target);}
 @Test public void nullTargetStillCannotBeHelped(){Room r=room();Crab c=small(r,1,31);c.nulled=true;assertNull(new MageStrategy().decide(r,110).target);}
 @Test public void oldLoneTargetStillHonorsActual31Cutoff(){Room r=room();small(r,1,29);assertNull(new MageStrategy().decide(r,110).target);}
 @Test public void expiringLoneTargetStillHonorsAgeEligibility(){Room r=room();small(r,1,31);assertNull(new MageStrategy().decide(r,149).target);}
 @Test public void zeroHpTransientIsNotConfusedWithTheOneLivingTarget(){Room r=room();Crab dead=small(r,1,31);dead.hp=0;Crab live=small(r,2,31);assertSame(live,new MageStrategy().decide(r,110).target);}

}
