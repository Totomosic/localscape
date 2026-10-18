package net.runelite.client.plugins.localsettings;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptEvent;
import net.runelite.api.StructComposition;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LocalSettingsPluginTest
{
	@Mock
	private Client client;

	@Mock
	private ClientThread clientThread;

	@InjectMocks
	private LocalSettingsPlugin plugin;

	private final List<Runnable> pending = new ArrayList<>();
	private final EventBus eventBus = new EventBus();

	@Before
	public void setUp()
	{
		MockitoAnnotations.initMocks(this);
		eventBus.register(plugin);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		doAnswer(invocation ->
		{
			pending.add(invocation.getArgument(0));
			return null;
		}).when(clientThread).invokeLater(any(Runnable.class));
	}

	@Test
	public void savesServerControlledDropdownSelection()
	{
		// Captured from the live revision-233 Controls tab: the last argument is 1,
		// which makes 3852 skip the 3968 proc watched by the original bridge.
		ScriptEvent input = mock(ScriptEvent.class);
		when(input.getArguments()).thenReturn(new Object[]{3852, 3, "F2", 36, 2,
			8781843, 8781851, 8781850, 8781853, 8781854, 8781847, 3506, 19, 35, 1});
		ScriptPreFired event = new ScriptPreFired(3852);
		event.setScriptEvent(input);
		plugin.onScriptPreFired(event);

		assertEquals(1, pending.size());
		pending.get(0).run();
		verify(client).runScript(49, "localscape_keybind 19 2");
	}

	@Test
	public void clientControlledDropdownSavesOnceThroughNativeProc()
	{
		ScriptEvent input = mock(ScriptEvent.class);
		when(input.getArguments()).thenReturn(new Object[]{3852, 3, "F2", 36, 2,
			8781843, 8781851, 8781850, 8781853, 8781854, 8781847, 3506, 19, 35, 0});
		ScriptPreFired event = new ScriptPreFired(3852);
		event.setScriptEvent(input);
		plugin.onScriptPreFired(event);
		assertEquals(0, pending.size());
		when(client.getIntStack()).thenReturn(new int[]{19, 2});
		when(client.getIntStackSize()).thenReturn(2);
		plugin.onScriptPreFired(new ScriptPreFired(3968));
		assertEquals(1, pending.size());
	}

	@Test
	public void serverControlledDropdownSupportsNoneAndRejectsOtherSettings()
	{
		Object[] args = {3852, 3, "None", 36, 0, 8781843, 8781851, 8781850,
			8781853, 8781854, 8781847, 3506, 19, 35, 1};
		ScriptEvent input = mock(ScriptEvent.class);
		when(input.getArguments()).thenReturn(args);
		ScriptPreFired event = new ScriptPreFired(3852);
		event.setScriptEvent(input);
		plugin.onScriptPreFired(event);
		args[12] = 238;
		plugin.onScriptPreFired(event);
		args[12] = 19;
		args[4] = 14;
		plugin.onScriptPreFired(event);
		args[4] = 0;
		args[1] = 2;
		plugin.onScriptPreFired(event);
		assertEquals(1, pending.size());
		pending.get(0).run();
		verify(client).runScript(49, "localscape_keybind 19 0");
	}

	@Test
	public void refreshesAllKeybindDropdownsAfterServerUpdates()
	{
		Widget clickzone = mock(Widget.class);
		Widget inventory = dropdown(100, 25, 10);
		Widget stats = dropdown(101, 19, 20);
		Widget music = dropdown(102, 27, 30);
		Widget unrelated = mock(Widget.class);
		when(unrelated.getOnOpListener()).thenReturn(new Object[]{3851, 2});
		when(clickzone.getDynamicChildren()).thenReturn(new Widget[]{inventory, null, stats, music, unrelated});
		when(client.getWidget(InterfaceID.Settings.SETTINGS_CLICKZONE)).thenReturn(clickzone);

		for (int varp : new int[]{1224, 1225, 1226})
		{
			VarbitChanged event = new VarbitChanged();
			event.setVarpId(varp);
			eventBus.post(event);
		}
		assertEquals(1, pending.size());
		verify(client, never()).runScript(any());
		pending.get(0).run();
		// The native redraw reads the authoritative binding, including None, from varps.
		// Script 3854 takes the label index before the parent component id.
		verify(client).runScript(3854, 35, 10, InterfaceID.Settings.SETTINGS_CONTENT, 1164, 25, 3);
		verify(client).runScript(3854, 35, 20, InterfaceID.Settings.SETTINGS_CONTENT, 1164, 19, 3);
		verify(client).runScript(3854, 35, 30, InterfaceID.Settings.SETTINGS_CONTENT, 1164, 27, 3);
	}

	@Test
	public void ignoresUnrelatedVarpsAndClosedSettings()
	{
		VarbitChanged event = new VarbitChanged();
		event.setVarpId(100);
		eventBus.post(event);
		verifyZeroInteractions(clientThread);
		event.setVarpId(1224);
		eventBus.post(event);
		pending.forEach(Runnable::run);
		verify(client, never()).runScript(any());
	}

	private Widget dropdown(int structId, int setting, int label)
	{
		Widget widget = mock(Widget.class);
		// Revision 233's settings keybind dropdown installs this 3851 listener.
		when(widget.getOnOpListener()).thenReturn(new Object[]{3851, 3, structId,
			InterfaceID.Settings.SETTINGS_CONTENT, label, 1164, 100, 20, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0});
		StructComposition struct = mock(StructComposition.class);
		when(client.getStructComposition(structId)).thenReturn(struct);
		when(struct.getIntValue(1077)).thenReturn(setting);
		return widget;
	}

	@Test
	public void submitsProcArgumentsAfterInterpreterReturns()
	{
		int[] stack = {99, 25, 2};
		when(client.getIntStack()).thenReturn(stack);
		when(client.getIntStackSize()).thenReturn(stack.length);
		plugin.onScriptPreFired(new ScriptPreFired(3968));

		verify(client, never()).runScript(any());
		assertEquals(1, pending.size());
		stack[1] = 16;
		stack[2] = 12;
		pending.get(0).run();
		verify(client).runScript(49, "localscape_keybind 25 2");
	}

	@Test
	public void submitsRootArgumentsAndSupportsUnbinding()
	{
		ScriptEvent input = mock(ScriptEvent.class);
		when(input.getArguments()).thenReturn(new Object[]{3968, 25, 0});
		ScriptPreFired event = new ScriptPreFired(3968);
		event.setScriptEvent(input);
		plugin.onScriptPreFired(event);
		pending.get(0).run();
		verify(client).runScript(49, "localscape_keybind 25 0");
	}

	@Test
	public void ignoresOtherSettingsAndInvalidSelections()
	{
		when(client.getIntStackSize()).thenReturn(2);
		for (int[] args : new int[][]{{238, 1}, {25, -1}, {25, 14}})
		{
			when(client.getIntStack()).thenReturn(args);
			plugin.onScriptPreFired(new ScriptPreFired(3968));
		}
		when(client.getIntStackSize()).thenReturn(1);
		plugin.onScriptPreFired(new ScriptPreFired(3968));
		plugin.onScriptPreFired(new ScriptPreFired(3967));
		verifyZeroInteractions(clientThread);
	}

	@Test
	public void savesBothTransparencyTogglesAndCapturesEachQueuedValue()
	{
		for (int value : new int[]{1, 0})
		{
			when(client.getVarbitValue(4609)).thenReturn(value);
			plugin.onScriptPostFired(new ScriptPostFired(3976));
			when(client.getVarbitValue(4608)).thenReturn(value);
			plugin.onScriptPostFired(new ScriptPostFired(3981));
		}

		verify(client, never()).runScript(any());
		assertEquals(4, pending.size());
		pending.forEach(Runnable::run);
		verify(client).runScript(49, "localscape_transparency inventory 1");
		verify(client).runScript(49, "localscape_transparency chatbox 1");
		verify(client).runScript(49, "localscape_transparency inventory 0");
		verify(client).runScript(49, "localscape_transparency chatbox 0");
	}

	@Test
	public void transparencySyncDoesNotResubmitToggles()
	{
		// Native redraws and server varp updates must not create a save loop.
		for (int script : new int[]{921, 2373, 3848, 3849, 6716})
		{
			plugin.onScriptPostFired(new ScriptPostFired(script));
		}
		verifyZeroInteractions(clientThread);
	}

	@Test
	public void ignoresTransparencyTogglesAndCancelsQueuedSavesOnLogout()
	{
		plugin.onScriptPostFired(new ScriptPostFired(3976));
		plugin.onScriptPostFired(new ScriptPostFired(3981));
		when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
		plugin.onScriptPostFired(new ScriptPostFired(3976));
		plugin.onScriptPostFired(new ScriptPostFired(3981));
		assertEquals(2, pending.size());
		pending.forEach(Runnable::run);
		verify(client, never()).runScript(any());
	}

	@Test
	public void submitsSidePanelHotkeyToggle()
	{
		when(client.getVarbitValue(4611)).thenReturn(1);
		plugin.onScriptPostFired(new ScriptPostFired(3995));

		verify(client, never()).runScript(any());
		assertEquals(1, pending.size());
		pending.get(0).run();
		verify(client).runScript(49, "localscape_sidepanel_hotkeys 1");
	}

	@Test
	public void capturesBothSidePanelHotkeyStatesBeforeQueueRuns()
	{
		when(client.getVarbitValue(4611)).thenReturn(1);
		plugin.onScriptPostFired(new ScriptPostFired(3995));
		when(client.getVarbitValue(4611)).thenReturn(0);
		plugin.onScriptPostFired(new ScriptPostFired(3995));

		assertEquals(2, pending.size());
		pending.forEach(Runnable::run);
		verify(client).runScript(49, "localscape_sidepanel_hotkeys 1");
		verify(client).runScript(49, "localscape_sidepanel_hotkeys 0");
	}

	@Test
	public void ignoresOtherScriptsAndSidePanelToggleWhileLoggedOut()
	{
		plugin.onScriptPostFired(new ScriptPostFired(3996));
		when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
		plugin.onScriptPostFired(new ScriptPostFired(3995));
		verifyZeroInteractions(clientThread);
	}

	@Test
	public void cancelsPendingSidePanelToggleOnLogout()
	{
		plugin.onScriptPostFired(new ScriptPostFired(3995));
		when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
		pending.get(0).run();
		verify(client, never()).runScript(any());
	}

	@Test
	public void submitsRestoreDefaultsButton()
	{
		when(client.getIntStack()).thenReturn(new int[]{58});
		when(client.getIntStackSize()).thenReturn(1);
		plugin.onScriptPreFired(new ScriptPreFired(3969));
		pending.get(0).run();
		verify(client).runScript(49, "localscape_keybind_reset");
	}

	@Test
	public void cancelsPendingSubmissionOnLogout()
	{
		when(client.getIntStack()).thenReturn(new int[]{25, 2});
		when(client.getIntStackSize()).thenReturn(2);
		plugin.onScriptPreFired(new ScriptPreFired(3968));
		when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
		pending.get(0).run();
		verify(client, never()).runScript(any());
	}
}
