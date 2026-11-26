package org.localscape.nylocas.ranger;
import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;
public class RangerFreshCapPriorityTest {
 private Crab old,south;
 private Room nativeFrame(){Room r=new Room();r.wave=19;r.spawns[18]=140;r.spawns[19]=148;r.ready=154;r.player=RangerStrategy.tile(3292,4246);for(int i=0;i<4;i++)add(r,70+i,120,17,Lane.WEST,true,Style.MELEE,3299,4251);Crab body=add(r,87,140,18,Lane.WEST,true,Style.RANGE,3290,4248);body.hp=0;body.dying=true;south=add(r,88,140,18,Lane.SOUTH,true,Style.RANGE,3292,4243);add(r,89,140,18,Lane.EAST,true,Style.RANGE,3295,4253);for(int i=0;i<3;i++)add(r,90+i,148,19,Lane.values()[i],true,Style.MELEE,3295,4240);old=add(r,73,112,15,Lane.EAST,false,Style.MAGIC,3294,4246);return r;}
 private Crab add(Room r,int key,int born,int wave,Lane lane,boolean big,Style style,int x,int y){Crab c=new Crab(key,key,born,wave,lane,'N',big,style,RangerStrategy.tile(x,y));c.pendingDamage=0;r.crabs.add(c);return c;}
 @Test public void nativeOldFifteenCapClearanceCannotBeDiscardedByFreshParentPreference(){Room r=nativeFrame();Decision d=new RangerStrategy().decide(r,154);assertSame(d.reason,old,d.target);assertEquals(Weapon.EYE,d.weapon);}
 @Test public void lowPopulationKeepsOrdinaryFreshPreference(){Room r=nativeFrame();r.crabs.remove(0);assertSame(south,new RangerStrategy().decide(r,154).target);}
 @Test public void aRealParentSurvivorRemainsProtected(){Room r=nativeFrame();RangerStrategy s=new RangerStrategy();s.attacked(new Decision(south,Weapon.PIPE,r.player,false,null,"actualpriorhit"),152);south.hp=3;assertSame(south,s.decide(r,154).target);}
 @Test public void ordinaryEqualTimeFreshStillWinsOutsideTheCapPredicate(){Room r=new Room();r.wave=13;r.spawns[13]=100;r.ready=100;r.player=RangerStrategy.tile(3295,4248);Crab old=add(r,1,64,9,Lane.SPLIT,false,Style.MAGIC,3294,4248);Crab fresh=add(r,2,100,13,Lane.SPLIT,false,Style.RANGE,3296,4248);assertSame(fresh,new RangerStrategy().decide(r,100).target);}
}
