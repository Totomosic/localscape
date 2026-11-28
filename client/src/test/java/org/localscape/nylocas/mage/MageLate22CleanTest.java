package org.localscape.nylocas.mage;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
public class MageLate22CleanTest {
 private static Room room(){Room r=new Room();r.wave=22;r.spawns[21]=168;r.spawns[22]=176;r.ready=185;r.specialEnergy=500;r.player=MageStrategy.tile(3295,4244);body(r,22,176,Lane.SOUTH,'W',false,Style.MELEE,3295,4243);body(r,22,176,Lane.WEST,'S',true,Style.MELEE,3292,4245);body(r,21,174,Lane.SPLIT,(char)0,false,Style.MAGIC,3296,4246);return r;}
 private static Crab body(Room r,int wave,int born,Lane lane,char slot,boolean big,Style style,int x,int y){int k=r.crabs.size()+1;Crab c=new Crab(k,k,born,wave,lane,slot,big,style,MageStrategy.tile(x,y));c.pendingDamage=0;r.crabs.add(c);return c;}
 @Test public void usesOneReadyEyeToCleanAnObservedSouthMagicBeforeDinh(){Room r=room();Decision d=new MageStrategy().decide(r,185);assertSame(r.crabs.get(2),d.target);assertEquals(Weapon.EYE,d.weapon);}
 @Test public void confirmedClearRetainsTheActualDinhOnTheNearbyWestBig(){Room r=room();MageStrategy s=new MageStrategy();Decision d=s.decide(r,185);s.attacked(d,186,185);r.ready=188;r.crabs.get(2).pendingDamage=8;r.crabs.get(0).hp=0;r.crabs.get(0).dying=true;r.player=MageStrategy.tile(3294,4245);Decision sweep=s.decide(r,188);assertSame(r.crabs.get(1),sweep.target);assertEquals("22 south Dinh",sweep.rule);assertTrue(sweep.special);}
 @Test public void coveredMagicKeepsTheNormalDinh(){Room r=room();r.crabs.get(2).pendingDamage=8;assertEquals("22 south Dinh",new MageStrategy().decide(r,185).rule);}
 @Test public void noClusterKeepsTheNormalDinh(){Room r=room();r.crabs.get(1).hp=0;r.crabs.get(1).dying=true;assertEquals("22 south Dinh",new MageStrategy().decide(r,185).rule);}
 @Test public void aProposedButUnconfirmedClearDoesNotDeferThePacket(){Room r=room();MageStrategy s=new MageStrategy();assertEquals(Weapon.EYE,s.decide(r,185).weapon);r.crabs.get(2).pendingDamage=8;assertEquals("22 south Dinh",s.decide(r,186).rule);}
 @Test public void twoClearCastsAreTheMaximumAndTheDinhIsReadyForTwentyFour(){Room r=room();MageStrategy s=new MageStrategy();Crab second=body(r,21,175,Lane.SPLIT,(char)0,false,Style.MAGIC,3297,4245);Decision a=s.decide(r,185);s.attacked(a,186,185);a.target.pendingDamage=8;r.ready=188;Decision b=s.decide(r,188);assertEquals(Weapon.EYE,b.weapon);s.attacked(b,189,188);b.target.pendingDamage=8;r.ready=191;r.crabs.get(0).hp=0;r.crabs.get(0).dying=true;r.player=MageStrategy.tile(3294,4245);body(r,21,175,Lane.SPLIT,(char)0,false,Style.MAGIC,3296,4246);Decision d=s.decide(r,191);assertEquals("22 south Dinh",d.rule);assertEquals(196,191+d.weapon.cooldown);}
 @Test public void aRequiredSouthTwentyRecoveryStillOwnsTheTick(){Room r=room();Crab parent=body(r,20,160,Lane.SOUTH,'W',true,Style.MAGIC,3295,4243);parent.hp=4;assertSame(parent,new MageStrategy().decide(r,185).target);}

 @Test public void readyMageClearsBothSouthBluesFromAgeSevenAndSweepsAtThirteen()
 {
     Room r = room();
     MageStrategy mage = new MageStrategy();
     Crab other = body(r, 22, 179, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3296, 4245);
     r.ready = 183;
     Decision first = mage.decide(r, 183);
     assertEquals(Weapon.EYE, first.weapon);
     assertEquals("Clear south magic before 22 Dinh", first.reason);
     mage.attacked(first, 184, 183);
     first.target.pendingDamage = first.target.hp;

     r.ready = 186;
     Decision second = mage.decide(r, 186);
     assertEquals(Weapon.EYE, second.weapon);
     assertNotSame(first.target, second.target);
     assertTrue(first.target == other || second.target == other);
     mage.attacked(second, 187, 186);
     second.target.pendingDamage = second.target.hp;

     r.ready = 189;
     r.player = MageStrategy.tile(3294, 4245);
     r.crabs.get(0).dying = true;
     r.crabs.get(0).hp = 0;
     Decision sweep = mage.decide(r, 189);
     assertSame(r.crabs.get(1), sweep.target);
     assertTrue(sweep.special);
     assertEquals("22 south Dinh", sweep.rule);
 }
}
