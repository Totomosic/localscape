package net.runelite.client.plugins.localsettings;

import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptEvent;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Local Server Settings",
	description = "Saves in-game keybind settings to the local server",
	enabledByDefault = true,
	hidden = true
)
public class LocalSettingsPlugin extends Plugin
{
	// Revision 233: settings_set_keybind, settings_clicked_button, and docheat.
	private static final int SET_KEYBIND = 3968;
	private static final int CLICK_SETTING_BUTTON = 3969;
	private static final int SEND_COMMAND = 49;
	private static final int RESTORE_KEYBINDS = 58;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (event.getScriptId() == SET_KEYBIND)
		{
			int[] args = intArguments(event, 2);
			if (args != null && args[0] >= 16 && args[0] <= 29 && args[1] >= 0 && args[1] <= 13)
			{
				submit("localscape_keybind " + args[0] + " " + args[1]);
			}
		}
		else if (event.getScriptId() == CLICK_SETTING_BUTTON)
		{
			int[] args = intArguments(event, 1);
			if (args != null && args[0] == RESTORE_KEYBINDS)
			{
				submit("localscape_keybind_reset");
			}
		}
	}

	private int[] intArguments(ScriptPreFired event, int count)
	{
		int[] args = new int[count];
		ScriptEvent root = event.getScriptEvent();
		if (root != null)
		{
			Object[] inputs = root.getArguments();
			if (inputs == null || inputs.length <= count)
			{
				return null;
			}
			for (int i = 0; i < count; i++)
			{
				if (!(inputs[i + 1] instanceof Integer))
				{
					return null;
				}
				args[i] = (Integer) inputs[i + 1];
			}
		}
		else
		{
			// Proc arguments are still on the interpreter stack at ScriptPreFired.
			int size = client.getIntStackSize();
			if (size < count)
			{
				return null;
			}
			System.arraycopy(client.getIntStack(), size - count, args, 0, count);
		}
		return args;
	}

	private void submit(String command)
	{
		// Queue after the current script finishes; runScript cannot be nested.
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				client.runScript(SEND_COMMAND, command);
			}
		});
	}
}
