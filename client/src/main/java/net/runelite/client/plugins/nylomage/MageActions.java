package net.runelite.client.plugins.nylomage;

import org.localscape.nylocas.mage.MageStrategy;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.nylocommon.NylocasActions;
import org.localscape.nylocas.mage.MageStrategy.Style;
import org.localscape.nylocas.mage.MageStrategy.Weapon;

/** Mage weapon variants and a native spell selection/target pair, never a fake autocast varp. */
final class MageActions extends NylocasActions
{
	private final MageConfig config;
	MageActions(Client client, MageConfig config, BooleanSupplier active)
	{
		super(client, config, active, Arrays.stream(Weapon.values())
			.filter(w -> w != Weapon.SHADOW || config.shadowOpening()).mapToInt(w -> id(config, w)).toArray(),
			config.shadowOpening() ? 27275 : 31113, 2, config.shadowOpening() ? 0 : 3);
		this.config = config;
	}

	private static int id(MageConfig config, Weapon weapon)
	{
		return weapon == Weapon.SCYTHE ? config.scytheId() : weapon == Weapon.DINH ? config.dinhId()
			: weapon == Weapon.BARRAGE ? config.barrageStaffId() : weapon.itemId;
	}

	int itemId(Weapon weapon) { return id(config, weapon); }
	List<Integer> gear(Style style) { return gear(style.ordinal()); }
	String capture(Style style) { return capture(style.ordinal()); }
	boolean prepare(Weapon weapon) { return prepare(itemId(weapon), weapon.style.ordinal(), weapon.stance); }

	@Override
	public boolean validate()
	{
		if (client.getVarbitValue(VarbitID.SPELLBOOK) != 1)
		{
			error = "Switch to Ancient Magicks for the mage barrage packets.";
			return false;
		}
		return super.validate();
	}

	boolean attack(NPC npc, Weapon weapon, boolean special)
	{
		if (weapon != Weapon.BARRAGE) return attack(npc, itemId(weapon), weapon.style.ordinal(), weapon.stance, special);
		if (npc.isDead() || npc.getHealthRatio() == 0 || !prepare(weapon)) return false;
		Widget spell = client.getWidget(InterfaceID.MagicSpellbook.ICE_BARRAGE);
		if (spell == null) { error = "Ice Barrage widget unavailable; open Ancient Magicks."; return false; }
		cancelQueuedInput();
		if (client.getVarpValue(301) != 0) clickWidget(InterfaceID.CombatInterface.SPECIAL_ATTACK);
		client.menuAction(spell.getIndex(), spell.getId(), MenuAction.WIDGET_TARGET, 0, -1, "Cast", "Ice Barrage");
		client.menuAction(0, 0, MenuAction.WIDGET_TARGET_ON_NPC, npc.getIndex(), -1, "Cast", npc.getName());
		return true;
	}
}
