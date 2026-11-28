package org.localscape.nylocas.mage;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
public class MageEarlySplitFollowthroughTest {
 private final MageStrategy s=new MageStrategy(); private final Room r=new Room(); private int key;
 private Crab add(int wave,int born,Lane lane,boolean big,Style style,int x,int y){Crab c=new Crab(++key,key,born,wave,lane,'W',big,style,MageStrategy.tile(x,y));c.pendingDamage=0;r.crabs.add(c);return c;}
 private void westFive(){r.wave=5;r.spawns[5]=28;r.ready=34;r.player=MageStrategy.tile(3291,4250);}
 @Test public void takesWestFiveMeleeChildBeforeHealthyOlderLane(){westFive();add(5,28,Lane.SOUTH,false,Style.MELEE,3295,4243);Crab c=add(5,32,Lane.SPLIT,false,Style.MELEE,3292,4250);assertSame(c,s.decide(r,34).target);}
 @Test public void preservesWoundedOlderFollowup(){westFive();Crab old=add(5,28,Lane.SOUTH,false,Style.MELEE,3292,4250);old.hp=2;add(5,32,Lane.SPLIT,false,Style.MELEE,3291,4251);assertSame(old,s.decide(r,34).target);}
 @Test public void excludesGreenChildren(){westFive();Crab old=add(5,28,Lane.SOUTH,false,Style.MELEE,3292,4250);add(5,32,Lane.SPLIT,false,Style.RANGE,3291,4251);assertSame(old,s.decide(r,34).target);}
 @Test public void excludesNulledChildren(){westFive();Crab old=add(5,28,Lane.SOUTH,false,Style.MELEE,3292,4250);add(5,32,Lane.SPLIT,false,Style.MELEE,3291,4251).nulled=true;assertSame(old,s.decide(r,34).target);}
 @Test public void excludesCoveredChildren(){westFive();Crab old=add(5,28,Lane.SOUTH,false,Style.MELEE,3292,4250);add(5,32,Lane.SPLIT,false,Style.MELEE,3291,4251).pendingDamage=8;assertSame(old,s.decide(r,34).target);}
 @Test public void stagesAtObservedWestSplitEntryDuringCooldown(){westFive();r.ready=34;r.player=MageStrategy.tile(3294,4245);Crab p=add(5,28,Lane.WEST,true,Style.RANGE,3288,4249);p.hp=0;p.dying=true;p.observeRemovalAt(32);add(4,26,Lane.SPLIT,false,Style.MELEE,3295,4244);Decision d=s.decide(r,31);assertNull(d.target);assertEquals(MageStrategy.tile(3291,4250),d.tile);assertTrue(d.reason,d.reason.contains("early split entry"));}
 @Test public void retainsMandatoryWestEightSpawn(){r.wave=8;r.spawns[8]=52;r.ready=52;r.player=MageStrategy.tile(3290,4249);Crab p=add(8,52,Lane.WEST,true,Style.MAGIC,3281,4249);add(8,51,Lane.SPLIT,false,Style.MELEE,3291,4249);assertSame(p,s.decide(r,52).target);}
 @Test public void retainsWestEightSurvivingRetry(){r.wave=8;r.spawns[8]=52;r.ready=55;r.player=MageStrategy.tile(3290,4249);Crab p=add(8,52,Lane.WEST,true,Style.MAGIC,3284,4249);s.attacked(new Decision(p,Weapon.EYE,r.player,false,"West magic 8","cast"),55,52);p.hp=4;add(8,54,Lane.SPLIT,false,Style.MELEE,3291,4249);assertSame(p,s.decide(r,55).target);}
 @Test public void takesImmediateWestSixBlueBeforeChildChase(){r.wave=6;r.spawns[6]=36;r.ready=36;r.player=MageStrategy.tile(3290,4249);Crab p=add(6,36,Lane.WEST,false,Style.MAGIC,3281,4249);add(5,32,Lane.SPLIT,false,Style.MELEE,3291,4249);assertSame(p,s.decide(r,36).target);}
 @Test public void takesSouthEightMeleeChildAfterLethalWestEight(){r.wave=8;r.spawns[8]=52;r.ready=55;r.player=MageStrategy.tile(3294,4244);add(7,40,Lane.EAST,false,Style.MELEE,3300,4252);Crab c=add(8,52,Lane.SPLIT,false,Style.MELEE,3295,4244);assertSame(c,s.decide(r,55).target);}

 @Test public void onlyHoldsTwoContestedReadyTicksForAnObservedBirth(){westFive();r.ready=32;r.player=MageStrategy.tile(3291,4250);Crab p=add(5,28,Lane.WEST,true,Style.RANGE,3288,4249);p.hp=0;p.dying=true;p.observeRemovalAt(34);Crab old=add(4,26,Lane.SPLIT,false,Style.MELEE,3292,4250);s.decide(r,31);assertNull(s.decide(r,32).target);assertNull(s.decide(r,33).target);assertSame(old,s.decide(r,34).target);}
 @Test public void finishesAnEarlyChildAfterALowRoll(){westFive();Crab c=add(5,32,Lane.SPLIT,false,Style.MELEE,3292,4250);s.attacked(new Decision(c,Weapon.SULPHUR,r.player,false,null,"hit"),34,30);c.hp=3;add(5,33,Lane.SPLIT,false,Style.MAGIC,3293,4250);assertSame(c,s.decide(r,34).target);}

 @Test public void doesNotAbandonANewlyWoundedShotForAnExistingEntryPlan(){westFive();r.player=MageStrategy.tile(3291,4250);Crab p=add(5,28,Lane.WEST,true,Style.RANGE,3288,4249);p.hp=0;p.dying=true;p.observeRemovalAt(35);Crab old=add(4,26,Lane.SPLIT,false,Style.MELEE,3292,4250);assertNull(s.decide(r,31).target);old.hp=3;assertSame(old,s.decide(r,34).target);}

 @Test public void retainsARealTwoChildScytheAfterEntryStaging(){westFive();r.ready=34;r.player=MageStrategy.tile(3291,4250);Crab p=add(5,28,Lane.WEST,true,Style.RANGE,3288,4249);p.hp=0;p.dying=true;p.observeRemovalAt(32);assertNull(s.decide(r,31).target);r.crabs.clear();add(5,32,Lane.SPLIT,false,Style.MELEE,3292,4250);add(5,32,Lane.SPLIT,false,Style.MELEE,3292,4251);Decision d=s.decide(r,34);assertEquals(Weapon.SCYTHE,d.weapon);}
}
