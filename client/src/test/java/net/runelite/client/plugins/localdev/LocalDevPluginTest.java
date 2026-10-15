package net.runelite.client.plugins.localdev;

import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ClientToolbar;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocalDevPluginTest
{
	@Mock
	private Client client;
	@Mock
	private ClientThread clientThread;
	@Mock
	private ClientToolbar clientToolbar;
	@InjectMocks
	private LocalDevPlugin plugin;
	private final List<Runnable> pending = new ArrayList<>();

	@Before
	public void setUp() throws Exception
	{
		MockitoAnnotations.initMocks(this);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		doAnswer(invocation ->
		{
			pending.add(invocation.getArgument(0));
			return null;
		}).when(clientThread).invokeLater(any(Runnable.class));
		SwingUtilities.invokeAndWait(plugin::startUp);
		pending.clear();
	}

	@After
	public void tearDown() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test
	public void sendsCommandOnClientThread()
	{
		plugin.submit("invadd 995 1000");
		verify(client, never()).runScript(any());
		pending.get(0).run();
		verify(client).runScript(49, "invadd 995 1000");
	}

	@Test
	public void doesNotSendQueuedCommandAfterLogoutAndRelogin()
	{
		plugin.submit("invclear");
		GameStateChanged logout = new GameStateChanged();
		logout.setGameState(GameState.LOGIN_SCREEN);
		plugin.onGameStateChanged(logout);
		pending.get(0).run();
		verify(client, never()).runScript(any());
	}

	@Test
	public void doesNotSendCommandWhileLoggedOut()
	{
		plugin.submit("master");
		when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
		pending.get(0).run();
		verify(client, never()).runScript(any());
	}
}
