package net.runelite.client.plugins.localsettings;

import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ParamID;
import net.runelite.api.ScriptEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Local Server Settings",
	description = "Saves in-game keybind, side-panel, transparency and chatbox scrollbar settings to the local server",
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
	private static final int TOGGLE_SIDE_PANEL_HOTKEYS = 3995;
	private static final int TOGGLE_TRANSPARENT_INVENTORY = 3976;
	private static final int TOGGLE_TRANSPARENT_CHATBOX = 3981;
	private static final int OPEN_DROPDOWN = 3851;
	private static final int SELECT_DROPDOWN = 3852;
	private static final int REFRESH_DROPDOWN = 3854;
	private static final int KEYBIND_DROPDOWN = 3;
	private static final int SETTING_DROPDOWN = 2;
	private static final int CHATBOX_SCROLLBAR_POSITION = 1;

	private boolean refreshPending;

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

		if (event.getScriptId() == SELECT_DROPDOWN && event.getScriptEvent() != null)
		{
			Object[] args = event.getScriptEvent().getArguments();
			// Server-controlled dropdowns update their label but skip the native setter.
			if (args != null && args.length == 15 && Integer.valueOf(1).equals(args[14])
				&& args[4] instanceof Integer && args[12] instanceof Integer)
			{
				int setting = (Integer) args[12];
				int value = (Integer) args[4];
				if (Integer.valueOf(KEYBIND_DROPDOWN).equals(args[1]))
				{
					submitKeybind(setting, value);
				}
				else if (Integer.valueOf(SETTING_DROPDOWN).equals(args[1])
					&& setting == CHATBOX_SCROLLBAR_POSITION && value >= 0 && value <= 1)
				{
					submit("localscape_scrollbar_position " + value);
				}
			}
		}
		else if (event.getScriptId() == SET_KEYBIND)
		{
			int[] args = intArguments(event, 2);
			if (args != null)
			{
				submitKeybind(args[0], args[1]);
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

	private void submitKeybind(int setting, int key)
	{
		if (setting >= 16 && setting <= 29 && key >= 0 && key <= 13)
		{
			submit("localscape_keybind " + setting + " " + key);
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (event.getScriptId() == TOGGLE_SIDE_PANEL_HOTKEYS)
		{
			// The native script toggles this inverted preference locally. Save its
			// resulting value so server varp updates and relogging preserve it.
			int cannotClose = client.getVarbitValue(VarbitID.HOTKEY_CANNOT_CLOSE_SIDEPANEL);
			submit("localscape_sidepanel_hotkeys " + cannotClose);
		}
		else if (event.getScriptId() == TOGGLE_TRANSPARENT_INVENTORY)
		{
			submit("localscape_transparency inventory " + client.getVarbitValue(VarbitID.SIDE_TRANSPARENCY));
		}
		else if (event.getScriptId() == TOGGLE_TRANSPARENT_CHATBOX)
		{
			submit("localscape_transparency chatbox " + client.getVarbitValue(VarbitID.CHATBOX_TRANSPARENCY));
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int varp = event.getVarpId();
		if (varp < VarPlayerID.SIDESTONE_KEYBINDINGS_1 || varp > VarPlayerID.SIDESTONE_KEYBINDINGS_3
			|| client.getGameState() != GameState.LOGGED_IN || refreshPending)
		{
			return;
		}
		refreshPending = true;
		clientThread.invokeLater(() ->
		{
			refreshPending = false;
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				refreshKeybindDropdowns();
			}
		});
	}

	private void refreshKeybindDropdowns()
	{
		Widget clickzone = client.getWidget(InterfaceID.Settings.SETTINGS_CLICKZONE);
		if (clickzone == null)
		{
			return;
		}
		// The native dropdown only redraws the clicked label. Refresh the other
		// labels too, so a displaced binding displays None as soon as it is synced.
		for (Widget child : clickzone.getDynamicChildren())
		{
			Object[] args = child == null ? null : child.getOnOpListener();
			if (args == null || args.length < 6 || !Integer.valueOf(OPEN_DROPDOWN).equals(args[0])
				|| !Integer.valueOf(KEYBIND_DROPDOWN).equals(args[1]))
			{
				continue;
			}
			int setting = client.getStructComposition((Integer) args[2]).getIntValue(ParamID.SETTING_ID);
			if (setting >= 16 && setting <= 29)
			{
				// Use the dropdown's own parent, label index and enum, just as its timer does.
				client.runScript(REFRESH_DROPDOWN, 35, args[4], args[3], args[5], setting, KEYBIND_DROPDOWN);
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
