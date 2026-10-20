package net.runelite.client.plugins.localdev;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
	name = "Nylocas Waves",
	description = "Live Nylocas room report, one-shot luck, and wave-check timeline",
	tags = {"nylocas", "theatre", "waves", "luck"},
	enabledByDefault = true
)
public class NylocasWavesPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	private volatile NylocasWavesPanel panel;
	private NavigationButton navigation;

	@Override
	protected void startUp()
	{
		panel = new NylocasWavesPanel();
		navigation = NavigationButton.builder()
			.tooltip("Nylocas Waves")
			.icon(createIcon())
			.priority(2)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigation);
	}

	@Override
	protected void shutDown()
	{
		panel = null;
		clientToolbar.removeNavigation(navigation);
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!"chatFilterCheck".equals(event.getEventName()))
		{
			return;
		}
		int[] intStack = client.getIntStack();
		int size = client.getIntStackSize();
		if (intStack[size - 2] != ChatMessageType.CONSOLE.getType())
		{
			return;
		}
		String message = (String) client.getObjectStack()[client.getObjectStackSize() - 1];
		if (message.startsWith("LNW2 ") || message.startsWith("LNW3 ") || message.startsWith("LNW4 ") || message.startsWith("LNC1 "))
		{
			// Hide only the rendered line; ChatMessage still delivers the original payload.
			intStack[size - 3] = 0;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.CONSOLE)
		{
			return;
		}
		NylocasWaveSnapshot report = NylocasWaveSnapshot.parse(event.getMessage());
		NylocasWaveCheck check = NylocasWaveCheck.parse(event.getMessage());
		NylocasWavesPanel current = panel;
		if (current != null && (report != null || check != null))
		{
			SwingUtilities.invokeLater(() ->
			{
				if (panel == current)
				{
					if (report != null)
					{
						current.update(report);
					}
					else
					{
						current.addCheck(check);
					}
				}
			});
		}
	}

	private static BufferedImage createIcon()
	{
		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = icon.createGraphics();
		graphics.setColor(new Color(112, 202, 186));
		graphics.drawLine(1, 14, 15, 14);
		graphics.fillRoundRect(2, 8, 3, 5, 2, 2);
		graphics.fillRoundRect(7, 4, 3, 9, 2, 2);
		graphics.fillRoundRect(12, 1, 3, 12, 2, 2);
		graphics.dispose();
		return icon;
	}
}
