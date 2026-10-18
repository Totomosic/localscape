package net.runelite.client.plugins.localgear;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
	name = "Local Gear Setups",
	description = "Save, search and restore equipment and inventory setups on the local server",
	tags = {"local", "gear", "presets", "setups"},
	enabledByDefault = true
)
public class GearPresetsPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ClientToolbar clientToolbar;
	private volatile GearPresetsPanel panel;
	private NavigationButton navigation;
	private volatile int session;
	private String request;
	private List<GearPreset> incoming;
	private int waitingTicks;
	private boolean requestedList;

	@Override
	protected void startUp()
	{
		panel = new GearPresetsPanel(name -> submit("save", encode(name)), preset -> submit("load", preset.id), () -> submit("list", null));
		navigation = NavigationButton.builder().tooltip("Gear setups").icon(icon()).priority(3).panel(panel).build();
		clientToolbar.addNavigation(navigation);
		clientThread.invokeLater(() -> updateLogin(client.getGameState()));
	}

	@Override
	protected void shutDown()
	{
		session++;
		clientToolbar.removeNavigation(navigation);
		panel = null;
		request = null;
		incoming = null;
		requestedList = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		updateLogin(event.getGameState());
	}

	private void updateLogin(GameState state)
	{
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			session++;
			request = null;
			incoming = null;
			requestedList = false;
			update(GearPresetsPanel::reset);
			return;
		}
		update(current -> current.setLoggedIn(state == GameState.LOGGED_IN));
		if (state == GameState.LOGGED_IN && !requestedList)
		{
			requestedList = true;
			submit("list", null);
		}
	}

	void submit(String action, String argument)
	{
		int requestedSession = session;
		clientThread.invokeLater(() ->
		{
			if (panel == null || session != requestedSession || client.getGameState() != GameState.LOGGED_IN || request != null)
			{
				return;
			}
			request = UUID.randomUUID().toString().substring(0, 8);
			incoming = null;
			waitingTicks = 0;
			update(current ->
			{
				current.setBusy(true);
				current.setStatus("list".equals(action) ? "Loading setups…" : "save".equals(action) ? "Saving setup…" : "Applying setup…");
			});
			client.runScript(49, "gearsetups " + action + " " + request + (argument == null ? "" : " " + argument));
		});
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (request != null && ++waitingTicks >= 10)
		{
			finish("No server response. Refresh setups to try again.", null);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		// Console messages carry panel data without filling the player's game chat.
		if (event.getType() != ChatMessageType.CONSOLE || request == null)
		{
			return;
		}
		String[] parts = event.getMessage().split(" ");
		if (parts.length < 3 || !"LGP1".equals(parts[0]) || !request.equals(parts[1]))
		{
			return;
		}
		try
		{
			switch (parts[2])
			{
				case "begin":
					incoming = new ArrayList<>();
					break;
				case "item":
					if (incoming == null || parts.length != 5)
					{
						throw new IllegalArgumentException();
					}
					String id = UUID.fromString(parts[3]).toString();
					String name = decode(parts[4]);
					if (!name.matches("(?:\\[[A-Za-z0-9 _-]{1,12}\\] )?[A-Za-z0-9 ._()'-]{1,24}") || name.trim().isEmpty())
					{
						throw new IllegalArgumentException();
					}
					incoming.add(new GearPreset(id, name));
					break;
				case "end":
					if (incoming == null || parts.length != 4)
					{
						throw new IllegalArgumentException();
					}
					finish(decode(parts[3]), new ArrayList<>(incoming));
					break;
				case "ok":
				case "error":
					if (parts.length != 4)
					{
						throw new IllegalArgumentException();
					}
					finish(decode(parts[3]), null);
					break;
				default:
					throw new IllegalArgumentException();
			}
		}
		catch (IllegalArgumentException exception)
		{
			finish("Could not read the setup list. Refresh to try again.", null);
		}
	}

	private void finish(String message, List<GearPreset> presets)
	{
		request = null;
		incoming = null;
		update(current ->
		{
			if (presets != null)
			{
				current.setPresets(presets);
			}
			current.setBusy(false);
			current.setStatus(message);
		});
	}

	private void update(Consumer<GearPresetsPanel> callback)
	{
		GearPresetsPanel current = panel;
		int expectedSession = session;
		SwingUtilities.invokeLater(() ->
		{
			if (current != null && panel == current && session == expectedSession)
			{
				callback.accept(current);
			}
		});
	}

	static String encode(String value)
	{
		StringBuilder result = new StringBuilder();
		for (byte valueByte : value.getBytes(StandardCharsets.UTF_8))
		{
			result.append(Character.forDigit((valueByte & 255) >>> 4, 16));
			result.append(Character.forDigit(valueByte & 15, 16));
		}
		return result.toString();
	}

	static String decode(String value)
	{
		if (value.length() % 2 != 0)
		{
			throw new IllegalArgumentException();
		}
		byte[] bytes = new byte[value.length() / 2];
		for (int i = 0; i < bytes.length; i++)
		{
			int high = Character.digit(value.charAt(i * 2), 16);
			int low = Character.digit(value.charAt(i * 2 + 1), 16);
			if (high < 0 || low < 0)
			{
				throw new IllegalArgumentException();
			}
			bytes[i] = (byte) ((high << 4) | low);
		}
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private static BufferedImage icon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(new Color(95, 185, 210));
		graphics.fillPolygon(new int[]{1, 5, 6, 10, 11, 15, 12, 11, 5, 4}, new int[]{4, 2, 4, 4, 2, 4, 8, 7, 7, 8}, 10);
		graphics.fillRoundRect(5, 5, 7, 10, 2, 2);
		graphics.setColor(Color.WHITE);
		graphics.drawLine(8, 7, 8, 12);
		graphics.dispose();
		return image;
	}
}
