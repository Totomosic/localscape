package org.localscape.nylocas.mage;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
public class MageSouth11BarrageTest {
 private static Room room() {Room r=new Room();r.wave=11;r.spawns[11]=76;r.player=MageStrategy.tile(3297,4245);r.specialEnergy=1000;return r;}
 private static Crab body(Room r,int wave,Lane lane,char slot,boolean big,Style style,int x,int y) {int k=r.crabs.size()+1;Crab c=new Crab(k,k,76,wave,lane,slot,big,style,MageStrategy.tile(x,y));c.pendingDamage=0;r.crabs.add(c);return c;}
 @Test public void barragesSouthPairBeforeOpeningWestParent() {
  Room r=room();body(r,11,Lane.WEST,'S',true,Style.MAGIC,3291,4248);
  Crab south=body(r,11,Lane.SOUTH,'W',false,Style.MAGIC,3295,4238);body(r,11,Lane.SOUTH,'E',false,Style.MAGIC,3296,4238);
  Decision d=new MageStrategy().decide(r,81);assertSame(south,d.target);assertEquals(Weapon.BARRAGE,d.weapon);
 }
 @Test public void fiveTickBarrageCooldownStillLeavesBothTwelvePackets() {
  MageStrategy s=new MageStrategy();Room r=room();Crab big=body(r,11,Lane.WEST,'S',true,Style.MAGIC,3291,4248);Crab south=body(r,11,Lane.SOUTH,'W',false,Style.MAGIC,3295,4238);Crab other=body(r,11,Lane.SOUTH,'E',false,Style.MAGIC,3296,4238);
  Decision d=s.decide(r,81);assertEquals(Weapon.BARRAGE,d.weapon);other.pendingDamage=8;s.attacked(d,82,81);r.ready=86;south.pendingDamage=8;r.wave=12;r.spawns[12]=84;
  Decision hit=s.decide(r,86);assertSame(big,hit.target);assertEquals(Weapon.EYE,hit.weapon);s.attacked(hit,87,86);big.pendingDamage=16;r.ready=89;r.player=MageStrategy.tile(3301,4249);
  Crab east=body(r,12,Lane.EAST,'S',false,Style.MELEE,3303,4248);
  // The named wave-12 lane has its own physical birth, not wave-11's birth.
  r.crabs.remove(east);east=new Crab(20,20,84,12,Lane.EAST,'S',false,Style.MELEE,MageStrategy.tile(3303,4248));east.pendingDamage=0;r.crabs.add(east);
  Decision scythe=s.decide(r,91);assertEquals("12 east scythe",scythe.rule);s.attacked(scythe,92,91);r.ready=96;r.player=MageStrategy.tile(3293,4249);
  Crab west=new Crab(21,21,84,12,Lane.WEST,'S',false,Style.MELEE,MageStrategy.tile(3292,4248));west.pendingDamage=0;r.crabs.add(west);assertEquals("12 west Dinh",s.decide(r,96).rule);
 }
 @Test public void southSurvivorUsesEyeAfterTheSingleBarrage() {
  MageStrategy s=new MageStrategy();Room r=room();Crab south=body(r,11,Lane.SOUTH,'W',false,Style.MAGIC,3295,4238);Crab other=body(r,11,Lane.SOUTH,'E',false,Style.MAGIC,3296,4238);Decision d=s.decide(r,81);assertEquals(Weapon.BARRAGE,d.weapon);other.pendingDamage=8;s.attacked(d,82,81);r.ready=86;south.hp=3;south.pendingDamage=0;
  Decision retry=s.decide(r,86);assertSame(south,retry.target);assertEquals(Weapon.EYE,retry.weapon);
 }
}