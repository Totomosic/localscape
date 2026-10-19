package net.runelite.client.plugins.localgear;

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
import net.runelite.client.ui.PluginPanel;

// Gear setup protocol and state, hosted by the Local Development plugin.
public class GearPresetsPlugin
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	private volatile GearPresetsPanel panel;
	private volatile int session;
	private String request;
	private List<GearPreset> incoming;
	private String incomingDefaultId;
	private String defaultPresetId;
	private int waitingTicks;
	private boolean requestedList;

	public PluginPanel startUp()
	{
		panel = new GearPresetsPanel(name -> submit("save", encode(name)), preset -> submit("load", preset.id),
			() -> submit("list", null), preset -> submit("default", preset.id == null ? "none" : preset.id));
		clientThread.invokeLater(() -> updateLogin(client.getGameState()));
		return panel;
	}

	public void shutDown()
	{
		session++;
		panel = null;
		request = null;
		incoming = null;
		incomingDefaultId = null;
		defaultPresetId = null;
		requestedList = false;
	}

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
			incomingDefaultId = null;
			defaultPresetId = null;
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
				current.setStatus("list".equals(action) ? "Loading setups…" : "save".equals(action) ? "Saving setup…"
					: "default".equals(action) ? "Saving default setup…" : "Applying setup…");
			});
			client.runScript(49, "gearsetups " + action + " " + request + (argument == null ? "" : " " + argument));
		});
	}

	public void onGameTick(GameTick event)
	{
		if (request != null && ++waitingTicks >= 10)
		{
			finish("No server response. Refresh setups to try again.", null);
		}
	}

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
					incomingDefaultId = null;
					break;
				case "default":
					if (incoming == null || parts.length != 4)
					{
						throw new IllegalArgumentException();
					}
					incomingDefaultId = "none".equals(parts[3]) ? null : UUID.fromString(parts[3]).toString();
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
		if (presets != null)
		{
			defaultPresetId = incomingDefaultId;
		}
		String selectedDefault = defaultPresetId;
		request = null;
		incoming = null;
		incomingDefaultId = null;
		update(current ->
		{
			if (presets != null)
			{
				current.setPresets(presets);
			}
			current.setDefaultPreset(selectedDefault);
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
}
