package net.runelite.client.plugins.localdev;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
	name = "Local Development",
	description = "Search and spawn items, clear inventory, and restore skills on the local server",
	tags = {"local", "development"},
	enabledByDefault = true
)
public class LocalDevPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	private final List<LocalItem> items = new ArrayList<>();
	private volatile LocalDevPanel panel;
	private NavigationButton navigation;
	private volatile int session;
	private int nextItemId;
	private boolean itemsLoaded;

	@Override
	protected void startUp()
	{
		panel = new LocalDevPanel(this::submit);
		navigation = NavigationButton.builder()
			.tooltip("Local Development")
			.icon(createIcon())
			.priority(1)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigation);
		clientThread.invokeLater(() -> updateLoginState(client.getGameState()));
	}

	@Override
	protected void shutDown()
	{
		session++;
		clientToolbar.removeNavigation(navigation);
		LocalDevPanel oldPanel = panel;
		panel = null;
		SwingUtilities.invokeLater(oldPanel::close);
		clientThread.invokeLater(this::resetItems);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		updateLoginState(event.getGameState());
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Custom RuneLite build active.", "");
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (panel == null || itemsLoaded || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		// Read definitions on the client thread in small batches to keep the game responsive.
		int end = Math.min(nextItemId + 250, client.getItemCount());
		for (; nextItemId < end; nextItemId++)
		{
			ItemComposition item = client.getItemDefinition(nextItemId);
			String name = item.getMembersName();
			if (name != null && !name.trim().isEmpty() && !"null".equalsIgnoreCase(name)
				&& item.getPlaceholderTemplateId() == -1)
			{
				items.add(new LocalItem(nextItemId, name, item.getNote() != -1));
			}
		}
		if (nextItemId >= client.getItemCount())
		{
			itemsLoaded = true;
			List<LocalItem> snapshot = new ArrayList<>(items);
			updatePanel(current -> current.setItems(snapshot));
		}
	}

	private void updateLoginState(GameState state)
	{
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			session++;
			resetItems();
			updatePanel(current -> current.setItems(null));
		}
		updatePanel(current -> current.setLoggedIn(state == GameState.LOGGED_IN));
	}

	private void resetItems()
	{
		items.clear();
		nextItemId = 0;
		itemsLoaded = false;
	}

	void submit(String command)
	{
		int requestedSession = session;
		clientThread.invokeLater(() ->
		{
			if (panel != null && requestedSession == session && client.getGameState() == GameState.LOGGED_IN)
			{
				client.runScript(49, command);
				updatePanel(current -> current.setStatus("Requested. See game chat for the result."));
			}
		});
	}

	private void updatePanel(java.util.function.Consumer<LocalDevPanel> update)
	{
		LocalDevPanel current = panel;
		SwingUtilities.invokeLater(() ->
		{
			if (current != null && panel == current)
			{
				update.accept(current);
			}
		});
	}

	private static BufferedImage createIcon()
	{
		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = icon.createGraphics();
		graphics.setColor(new Color(240, 190, 70));
		graphics.drawRoundRect(0, 1, 15, 13, 3, 3);
		graphics.drawLine(3, 5, 6, 8);
		graphics.drawLine(6, 8, 3, 11);
		graphics.drawLine(9, 11, 12, 11);
		graphics.dispose();
		return icon;
	}
}
