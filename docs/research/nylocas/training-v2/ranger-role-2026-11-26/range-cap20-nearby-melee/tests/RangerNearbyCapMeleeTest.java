package org.localscape.nylocas.ranger;
import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;
public class RangerNearbyCapMeleeTest {
 private Crab south,east,small;
 private Crab add(Room r,int key,int born,int wave,Lane lane,boolean big,Style style,int x,int y){Crab c=new Crab(key,key,born,wave,lane,'S',big,style,RangerStrategy.tile(x,y));c.pendingDamage=0;r.crabs.add(c);return c;}
 private Room nativeFrame(){Room r=new Room();r.wave=19;r.spawns[18]=144;r.spawns[19]=152;r.ready=158;r.player=RangerStrategy.tile(3291,4246);for(int i=0;i<3;i++)add(r,80+i,124,17,Lane.values()[i],true,Style.MELEE,3299,4251);add(r,72,108,15,Lane.SOUTH,true,Style.MAGIC,3292,4244);south=add(r,88,144,18,Lane.SOUTH,true,Style.RANGE,3293,4243);east=add(r,89,144,18,Lane.EAST,true,Style.RANGE,3295,4253);for(int i=0;i<3;i++)add(r,90+i,152,19,Lane.values()[i],true,Style.MELEE,3295,4240);add(r,93,156,19,Lane.SPLIT,false,Style.MELEE,3288,4248);small=add(r,94,156,19,Lane.SPLIT,false,Style.MELEE,3289,4249);return r;}
 @Test public void actualEarlyMeleeChildCanReceiveANormalScratchBeforeTheCap(){Room r=nativeFrame();Decision d=new RangerStrategy().decide(r,158);assertSame(d.reason,small,d.target);assertEquals(Weapon.CLAWS,d.weapon);assertFalse(d.special);}
 @Test public void confirmedParentSurvivorRemainsAheadOfTheCapMelee(){Room r=nativeFrame();RangerStrategy s=new RangerStrategy();s.attacked(new Decision(south,Weapon.PIPE,r.player,false,null,"actualparenthit"),156);south.hp=3;assertSame(south,s.decide(r,158).target);}
 @Test public void fourTilesAwayCannotCreateACrossRoomChase(){Room r=nativeFrame();small.point=RangerStrategy.tile(3289,4250);assertSame(south,new RangerStrategy().decide(r,158).target);}
 @Test public void theNewMeleeAllowanceEndsAfterTheBoundedWindow(){Room r=nativeFrame();r.ready=161;assertSame(south,new RangerStrategy().decide(r,161).target);}
 @Test public void knownLethalNullAndSameCycleMageClaimRemainProtected(){for(int guard=0;guard<3;guard++){Room r=nativeFrame();if(guard==0)small.pendingDamage=8;if(guard==1)small.nulled=true;if(guard==2)small.provisionalClaimUntil=159;assertNotSame("guard"+guard,small,new RangerStrategy().decide(r,158).target);}}
 @Test public void ordinaryReadyRangeStillOutranksTheMeleeInsideTheCapTier(){Room r=nativeFrame();Crab range=add(r,95,157,19,Lane.SPLIT,false,Style.RANGE,3291,4247);assertSame(range,new RangerStrategy().decide(r,158).target);}
 @Test public void actualTwentyReleasesTheCapMeleeException(){Room r=nativeFrame();r.wave=20;r.spawns[20]=158;assertSame(south,new RangerStrategy().decide(r,158).target);}
}
