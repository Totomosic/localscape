package org.localscape.nylocas.ranger;
import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;
public class RangerNoOriginalWaveOneMeleeTest {
 private Room room(int wave){Room r=new Room();r.wave=wave;r.spawns[wave]=36;r.ready=40;r.player=RangerStrategy.tile(3295,4244);return r;}
 private Crab crab(Room r,int key,int born,int wave,Lane lane,char slot,Style style){Crab c=new Crab(key,key,born,wave,lane,slot,false,style,r.player);c.pendingDamage=0;r.crabs.add(c);return c;}
 @Test public void originalEastNorthOneMeleeNeverBecomesAnAttackOrApproach(){Room r=room(6);Crab old=crab(r,1,4,1,Lane.EAST,'N',Style.MELEE);Decision d=new RangerStrategy().decide(r,40);assertNotSame(old,d.target);assertNotSame(old,d.approach);}
 @Test public void theAbsoluteIdentityRuleAlsoCoversThePostCapFallback(){Room r=room(20);Crab old=crab(r,1,4,1,Lane.EAST,'N',Style.MELEE);Decision d=new RangerStrategy().decide(r,40);assertNotSame(old,d.target);assertNotSame(old,d.approach);}
 @Test public void anotherLocalMeleeSplitStillReceivesNormalHelp(){Room r=room(6);crab(r,1,4,1,Lane.EAST,'N',Style.MELEE);Crab split=crab(r,2,34,5,Lane.SPLIT,(char)0,Style.MELEE);Decision d=new RangerStrategy().decide(r,40);assertSame(split,d.target);assertEquals(Weapon.CLAWS,d.weapon);split.nulled=true;assertNotSame(split,new RangerStrategy().decide(r,40).target);}
 @Test public void theMandatoryOriginalWestOneBowIsPreserved(){Room r=room(1);r.spawns[1]=4;r.ready=4;r.player=RangerStrategy.tile(3292,4249);Crab west=crab(r,2,4,1,Lane.WEST,'S',Style.RANGE);west.point=RangerStrategy.tile(3281,4248);Decision d=new RangerStrategy().decide(r,4);assertSame(west,d.target);assertEquals(Weapon.BOW,d.weapon);assertEquals("Opening west",d.rule);}
}
