package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageDinh12FoodProbe {
 private MageStrategy s;private Crab west;
 private Room afterScythe(int request){s=new MageStrategy();Room r=new Room();r.wave=12;r.spawns[12]=84;r.player=MageStrategy.tile(3301,4249);r.specialEnergy=1000;
 Crab east=new Crab(1,1,84,12,Lane.EAST,'S',false,Style.MELEE,MageStrategy.tile(3302,4248));r.crabs.add(east);
 Decision d=s.decide(r,request);assertEquals("12 east scythe",d.rule);s.attacked(d,request+1,request);east.dying=true;east.hp=0;
 r.wave=13;r.spawns[13]=92;r.player=MageStrategy.tile(3293,4249);r.ready=request+5;
 west=new Crab(2,2,84,12,Lane.WEST,'S',false,Style.MELEE,MageStrategy.tile(3294,4247));r.crabs.add(west);return r;}
 @Test public void foodBlockedNormalDueStillUsesDinhAtFirstActionableAge15(){Room r=afterScythe(91);Decision d=s.decide(r,99);assertSame(west,d.target);assertEquals(Weapon.DINH,d.weapon);assertEquals("12 west Dinh",d.rule);}
 @Test public void normalFiveTickScytheDinhSequenceStillLaunchesAtItsOriginalDue(){Room r=afterScythe(91);Decision d=s.decide(r,96);assertSame(west,d.target);assertEquals(Weapon.DINH,d.weapon);}
 @Test public void delayedScytheAnchorAlsoGetsThreeTickNativeFoodRecovery(){Room r=afterScythe(92);Decision d=s.decide(r,100);assertSame(west,d.target);assertEquals(Weapon.DINH,d.weapon);}
 @Test public void unknownPartnerFlightStillProtectsTheReservedPrimary(){Room r=afterScythe(91);west.claimedUntil=101;assertNull(s.decide(r,99).target);}
 @Test public void foodRecoveryDoesNotIgnoreNativeNull(){Room r=afterScythe(91);west.nulled=true;assertNull(s.decide(r,99).target);}
 @Test public void finiteRecoveryWindowDoesNotKeepDinhReservationForever(){Room r=afterScythe(91);Decision d=s.decide(r,100);assertFalse(d.target==west && d.weapon==Weapon.DINH);}
}
