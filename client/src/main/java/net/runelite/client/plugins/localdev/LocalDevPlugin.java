package net.runelite.client.plugins.localdev;

import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Local Development",
	description = "Confirms that the custom local development client is running",
	tags = {"local", "development"},
	enabledByDefault = true
)
public class LocalDevPlugin extends Plugin
{
	@Inject
	private Client client;

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Custom RuneLite build active.", "");
		}
	}
}
