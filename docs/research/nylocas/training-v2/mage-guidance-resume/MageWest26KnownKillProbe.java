package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageWest26KnownKillProbe {
 private Room room(int forecast,int tick){Room r=new Room();r.wave=27;r.spawns[27]=108;r.player=MageStrategy.tile(3291,4248);
 Crab old=new Crab(1,1,100,26,Lane.WEST,'S',true,Style.MAGIC,MageStrategy.tile(3288,4248));old.hp=12;old.ownHitHp=12;old.ownHitUntil=135;old.pendingDamage=forecast;r.crabs.add(old);
 Crab next=new Crab(2,2,108,27,Lane.WEST,'S',true,Style.MAGIC,MageStrategy.tile(3288,4248));r.crabs.add(next);return r;}
 private Crab selected(Decision d){return d.target!=null?d.target:d.approach;}
 @Test public void knownLethal26FlightReleasesEarly27(){Room r=room(12,111);assertSame(r.crabs.get(1),selected(new MageStrategy().decide(r,111)));}
 @Test public void unknown26FlightStillDefers27(){Room r=room(-1,111);assertNotSame(r.crabs.get(1),selected(new MageStrategy().decide(r,111)));}
 @Test public void knownNonlethal26FlightStillFinishes26(){Room r=room(11,111);assertSame(r.crabs.get(0),selected(new MageStrategy().decide(r,111)));}
 @Test public void knownZero26FlightStillFinishes26(){Room r=room(0,111);assertSame(r.crabs.get(0),selected(new MageStrategy().decide(r,111)));}
 @Test public void knownKillDoesNotReopen27AfterItsEntryWindow(){Room r=room(12,118);assertNotSame(r.crabs.get(1),selected(new MageStrategy().decide(r,118)));}
}
