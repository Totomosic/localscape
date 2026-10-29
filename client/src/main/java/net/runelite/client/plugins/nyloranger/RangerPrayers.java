package net.runelite.client.plugins.nyloranger;

import org.localscape.nylocas.ranger.RangerFlickers;
import org.localscape.nylocas.ranger.RangerStrategy;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.plugins.nylocommon.NylocasPrayers;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Style;

final class RangerPrayers extends NylocasPrayers<Crab>
{
	RangerPrayers(Client client, RangerConfig config, RangerActions actions)
	{
		super(client, config, actions, new Threats<Crab>()
		{
			public int style(Crab crab) { return crab.style.ordinal(); }
			public boolean big(Crab crab) { return crab.big; }
			public boolean ignore(NPC npc, Crab crab, int tick)
			{
				return crab.dying || NylocasRangerPlugin.dying(npc) || RangerFlickers.passiveMelee(crab, tick);
			}
		}, 1);
	}

	void forAttack(Style style, int tick) { forAttack(style.ordinal(), tick); }
}
