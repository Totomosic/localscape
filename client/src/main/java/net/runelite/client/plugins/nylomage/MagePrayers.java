package net.runelite.client.plugins.nylomage;

import org.localscape.nylocas.mage.MageStrategy;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.plugins.nylocommon.NylocasPrayers;
import org.localscape.nylocas.mage.MageStrategy.Crab;
import org.localscape.nylocas.mage.MageStrategy.Style;

final class MagePrayers extends NylocasPrayers<Crab>
{
	MagePrayers(Client client, MageConfig config, MageActions actions)
	{
		super(client, config, actions, new Threats<Crab>()
		{
			public int style(Crab crab) { return crab.style.ordinal(); }
			public boolean big(Crab crab) { return crab.big; }
			public boolean ignore(NPC npc, Crab crab, int tick)
			{
				return crab.dying || NylocasMagePlugin.dying(npc) || MageStrategy.passiveMelee(crab, tick);
			}
		}, 2);
	}

	void forAttack(Style style, int tick) { forAttack(style.ordinal(), tick); }
}
