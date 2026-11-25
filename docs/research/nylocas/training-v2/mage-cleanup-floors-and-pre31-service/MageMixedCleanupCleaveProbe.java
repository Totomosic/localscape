package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageMixedCleanupCleaveProbe {
 private Crab primary,other,hazard;
 private Room native279(){Room r=new Room();r.wave=31;r.spawns[28]=236;r.spawns[29]=240;r.spawns[30]=264;r.spawns[31]=268;r.ready=279;r.player=MageStrategy.tile(3301,4251);
 primary=new Crab(202,202,279,31,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3301,4252));primary.pendingDamage=0;r.crabs.add(primary);
 other=new Crab(198,198,271,31,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3301,4252));other.pendingDamage=0;r.crabs.add(other);
 hazard=new Crab(201,201,279,31,Lane.SPLIT,(char)0,false,Style.MAGIC,MageStrategy.tile(3300,4251));hazard.pendingDamage=0;r.crabs.add(hazard);return r;}
 private void sulphur(Room r){Decision d=new MageStrategy().decide(r,279);assertEquals(Weapon.SULPHUR,d.weapon);assertTrue(d.target==primary||d.target==other);}
 private void scythe(Room r){assertEquals(Weapon.SCYTHE,new MageStrategy().decide(r,279).weapon);}
 @Test public void native279AvoidsCleaveThatNullsTheNewMagicSibling(){sulphur(native279());}
 @Test public void rangedSiblingAlsoKeepsAUsefulBackupLifetime(){Room r=native279();hazard.style=Style.RANGE;sulphur(r);}
 @Test public void alreadyOwnNullSiblingDoesNotWasteUsefulMeleeAoe(){Room r=native279();hazard.nulled=true;scythe(r);}
 @Test public void lethalCoveredSiblingDoesNotWasteUsefulMeleeAoe(){Room r=native279();hazard.pendingDamage=8;scythe(r);}
 @Test public void knownNonlethalSiblingStillNeedsFutureMageService(){Room r=native279();hazard.pendingDamage=7;sulphur(r);}
 @Test public void unknownFlightCannotBeAssumedLethal(){Room r=native279();hazard.pendingDamage=-1;hazard.claimedUntil=310;sulphur(r);}
 @Test public void alreadyDimmedSiblingDoesNotWasteUsefulMeleeAoe(){Room r=native279();r.crabs.remove(hazard);hazard=new Crab(201,201,252,29,Lane.SPLIT,(char)0,false,Style.MAGIC,MageStrategy.tile(3300,4251));hazard.pendingDamage=0;r.crabs.add(hazard);scythe(r);}
 @Test public void dyingSiblingDoesNotWasteUsefulMeleeAoe(){Room r=native279();hazard.dying=true;scythe(r);}
 @Test public void zeroHpSiblingDoesNotWasteUsefulMeleeAoe(){Room r=native279();hazard.hp=0;scythe(r);}
 @Test public void outsideTheNextCyclePossibleArcKeepsAoe(){Room r=native279();hazard.point=MageStrategy.tile(3297,4248);scythe(r);}
 @Test public void bothWalkingTowardArcCanCloseCurrentThreeTileGap(){Room r=native279();hazard.point=MageStrategy.tile(3298,4252);sulphur(r);}
 @Test public void ordinaryBefore31RotationStaysUnchanged(){Room r=native279();r.wave=30;scythe(r);}
 @Test public void bigPrimaryScytheIsUnchanged(){Room r=native279();r.ready=290;r.crabs.clear();Crab parent=new Crab(2,2,264,30,Lane.WEST,'S',true,Style.MELEE,MageStrategy.tile(3301,4252));parent.pendingDamage=0;parent.partnerNulled=true;r.crabs.add(parent);r.crabs.add(hazard);assertEquals(Weapon.SCYTHE,new MageStrategy().decide(r,290).weapon);}
}
