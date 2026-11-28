package org.localscape.nylocas.mage;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
public class MagePrecapCleaveApproachTest {
 private static Room room() { Room r=new Room();r.wave=16;r.spawns[16]=120;r.ready=124;r.player=MageStrategy.tile(3295,4250);return r; }
 private static Crab body(Room r,int x,int y,Style style) { int k=r.crabs.size()+1;Crab c=new Crab(k,k,118,15,Lane.SPLIT,(char)0,false,style,MageStrategy.tile(x,y));c.pendingDamage=0;r.crabs.add(c);return c; }
 @Test public void usesCooldownToReachThePerpendicularCleaveSquare() {
  Room r=room();body(r,3300,4251,Style.MELEE);body(r,3300,4252,Style.MELEE);
  Decision d=new MageStrategy().decide(r,120);
  assertNull(d.target);assertEquals(Weapon.SCYTHE,d.weapon);assertEquals(MageStrategy.tile(3299,4251),d.tile);
 }
 @Test public void doesNotPlanANullOnAnUncoveredMagicInTheArc() {
  Room r=room();body(r,3300,4251,Style.MELEE);body(r,3300,4252,Style.MELEE);body(r,3300,4250,Style.MAGIC);
  Decision d=new MageStrategy().decide(r,120);
  assertEquals(Weapon.SCYTHE,d.weapon);assertEquals(MageStrategy.tile(3299,4252),d.tile);
 }
 @Test public void keepsItsDueEastScythePacket() {
  Room r=room();r.wave=12;r.spawns[12]=113;r.ready=120;r.player=MageStrategy.tile(3301,4249);
  Crab east=new Crab(10,10,113,12,Lane.EAST,'S',false,Style.MELEE,MageStrategy.tile(3302,4248));east.pendingDamage=0;r.crabs.add(east);
  body(r,3300,4251,Style.MELEE);body(r,3300,4252,Style.MELEE);
  assertEquals("12 east scythe",new MageStrategy().decide(r,120).rule);
 }
}