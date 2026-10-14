package net.runelite.client.plugins.localsettings;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptEvent;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.client.callback.ClientThread;
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

	@Before
	public void setUp()
	{
		MockitoAnnotations.initMocks(this);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		doAnswer(invocation ->
		{
			pending.add(invocation.getArgument(0));
			return null;
		}).when(clientThread).invokeLater(any(Runnable.class));
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
