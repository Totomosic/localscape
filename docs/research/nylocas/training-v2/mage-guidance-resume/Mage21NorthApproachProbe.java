package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class Mage21NorthApproachProbe {
 private MageStrategy s;private Crab north;
 private Room afterBarrage(){s=new MageStrategy();Room r=new Room();r.wave=21;r.spawns[21]=176;r.player=MageStrategy.tile(3290,4249);r.specialEnergy=900;
 Crab west=new Crab(112,112,176,21,Lane.WEST,'N',false,Style.MAGIC,MageStrategy.tile(3281,4249));r.crabs.add(west);
 Decision d=s.decide(r,176);assertEquals("21 west barrage",d.rule);s.attacked(d,177,176);r.ready=181;west.dying=true;west.hp=0;
 north=new Crab(100,100,162,20,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3301,4252));r.crabs.add(north);return r;}
 @Test public void barrageCooldownMovesTowardActualNorthPrimaryRatherThanFixedWestStage(){Room r=afterBarrage();Decision d=s.decide(r,177);assertNull(d.target);assertSame(north,d.approach);assertEquals(Weapon.SULPHUR,d.weapon);}
 @Test public void unknownPartnerFlightDoesNotCreateAnEarlyNorthApproach(){Room r=afterBarrage();north.claimedUntil=185;assertNull(s.decide(r,177).approach);}
 @Test public void knownLethalFlightDoesNotCreateAnEarlyNorthApproach(){Room r=afterBarrage();north.pendingDamage=8;assertNull(s.decide(r,177).approach);}
 @Test public void idealReadyTickStillRequestsTheSameSulphurPacket(){Room r=afterBarrage();r.player=MageStrategy.tile(3300,4251);Decision d=s.decide(r,181);assertSame(north,d.target);assertEquals("21 north sulphur",d.rule);}
 @Test public void completingNorthPacketStillPreservesTheSouth22ReturnBudget(){Room r=afterBarrage();r.player=MageStrategy.tile(3300,4251);
 Decision packet=s.decide(r,181);s.attacked(packet,182,181);north.dying=true;north.hp=0;
 r.wave=22;r.spawns[22]=184;r.player=MageStrategy.tile(3295,4251);r.ready=189;
 Crab filler=new Crab(119,119,183,21,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3297,4252));filler.fighting=true;r.crabs.add(filler);
 Crab primary=new Crab(125,125,184,22,Lane.SOUTH,'W',false,Style.MAGIC,MageStrategy.tile(3295,4236));r.crabs.add(primary);
 Decision d=s.decide(r,187);assertNull(d.target);assertNull(d.approach);assertEquals(Weapon.DINH,d.weapon);assertEquals(MageStrategy.tile(3295,4244),d.tile);
 }

}
