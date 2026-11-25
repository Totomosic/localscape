package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageBigFollowupProbe {
 @Test public void south4SurvivorReplacesAnEast4ApproachChosenDuringItsFlight(){
 MageStrategy s=new MageStrategy();Room r=new Room();r.wave=5;r.spawns[5]=104;r.player=MageStrategy.tile(3299,4247);r.ready=112;
 Crab south=new Crab(11,11,100,4,Lane.SOUTH,'W',true,Style.MAGIC,MageStrategy.tile(3293,4243));r.crabs.add(south);
 Crab east=new Crab(12,12,100,4,Lane.EAST,'S',false,Style.MELEE,MageStrategy.tile(3301,4249));r.crabs.add(east);
 s.attacked(new Decision(south,Weapon.EYE,south.point,false,"South big 4","South big 4"),109,108);
 south.hp=8;south.ownHitHp=8;south.ownHitUntil=111;
 Decision plan=s.decide(r,110);assertSame(east,plan.target!=null?plan.target:plan.approach);
 south.hp=7;south.ownHitUntil=0;
 Decision retry=s.decide(r,112);assertSame(south,retry.target!=null?retry.target:retry.approach);
 }
 @Test public void south24SurvivorReplacesSameWaveSmallApproachAfterTheOpeningWindow(){
 MageStrategy s=new MageStrategy();Room r=new Room();r.wave=27;r.spawns[27]=108;r.player=MageStrategy.tile(3299,4247);r.ready=112;
 Crab south=new Crab(1,1,88,24,Lane.SOUTH,'W',true,Style.MAGIC,MageStrategy.tile(3293,4243));r.crabs.add(south);
 Crab split=new Crab(2,2,88,24,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3301,4249));r.crabs.add(split);
 s.attacked(new Decision(south,Weapon.EYE,south.point,false,null,"Fresh MAGIC assigned big"),109,108);
 south.hp=8;south.ownHitHp=8;south.ownHitUntil=111;
 Decision plan=s.decide(r,110);assertSame(split,plan.target!=null?plan.target:plan.approach);
 south.hp=7;south.ownHitUntil=0;
 Decision retry=s.decide(r,112);assertSame(south,retry.target!=null?retry.target:retry.approach);
 }

}
