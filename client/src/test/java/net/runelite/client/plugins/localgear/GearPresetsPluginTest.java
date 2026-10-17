package net.runelite.client.plugins.localgear;

import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GearPresetsPluginTest
{
	@Mock private Client client;
	@Mock private ClientThread clientThread;
	@Mock private ClientToolbar toolbar;
	@InjectMocks private GearPresetsPlugin plugin;
	private final List<Runnable> pending = new ArrayList<>();
	private final List<String> commands = new ArrayList<>();
	private GearPresetsPanel panel;

	@Before
	public void setUp() throws Exception
	{
		MockitoAnnotations.initMocks(this);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		doAnswer(invocation -> { pending.add(invocation.getArgument(0)); return null; })
			.when(clientThread).invokeLater(any(Runnable.class));
		doAnswer(invocation -> { commands.add(invocation.getArgument(1)); return null; })
			.when(client).runScript(any(), any());
		SwingUtilities.invokeAndWait(plugin::startUp);
		ArgumentCaptor<NavigationButton> navigation = ArgumentCaptor.forClass(NavigationButton.class);
		verify(toolbar).addNavigation(navigation.capture());
		panel = (GearPresetsPanel) navigation.getValue().getPanel();
		drain();
	}

	@After
	public void tearDown() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test
	public void loadsSearchableButtonsAndSavesCasePreservedNamesOnTheClientThread() throws Exception
	{
		String request = token();
		String id = "11111111-2222-3333-4444-555555555555";
		reply(request, "begin");
		reply(request, "item " + id + " " + GearPresetsPlugin.encode("Nylo Duo"));
		reply(request, "end " + GearPresetsPlugin.encode("Ready"));
		SwingUtilities.invokeAndWait(() -> GearPresetsPanelTest.button(panel, "Nylo Duo").doClick());
		assertEquals(1, commands.size());
		drain();
		assertTrue(commands.get(1).matches("gearsetups load [0-9a-f]{8} " + id));
		reply(token(), "ok " + GearPresetsPlugin.encode("Equipped Nylo Duo."));
		SwingUtilities.invokeAndWait(() ->
		{
			GearPresetsPanelTest.fields(panel).get(1).setText("Nylo Solo");
			GearPresetsPanelTest.button(panel, "Save current setup").doClick();
		});
		drain();
		String payload = commands.get(2).split(" ")[3];
		assertEquals("Nylo Solo", GearPresetsPlugin.decode(payload.toLowerCase()));
	}

	@Test
	public void clearsAnotherCharactersSetupsAndRejectsQueuedCommandsAndStaleResponses() throws Exception
	{
		String oldRequest = token();
		reply(oldRequest, "begin");
		reply(oldRequest, "item 11111111-2222-3333-4444-555555555555 " + GearPresetsPlugin.encode("Old account"));
		GameStateChanged logout = new GameStateChanged();
		logout.setGameState(GameState.LOGIN_SCREEN);
		plugin.onGameStateChanged(logout);
		reply(oldRequest, "end " + GearPresetsPlugin.encode("Ready"));
		flush();
		assertTrue(GearPresetsPanelTest.buttons(panel, "Old account").isEmpty());
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		GameStateChanged login = new GameStateChanged();
		login.setGameState(GameState.LOGGED_IN);
		plugin.onGameStateChanged(login);
		drain();
		reply(oldRequest, "error " + GearPresetsPlugin.encode("Stale response"));
		reply(token(), "begin");
		reply(token(), "end " + GearPresetsPlugin.encode("Ready"));
		plugin.submit("save", GearPresetsPlugin.encode("Queued"));
		plugin.onGameStateChanged(logout);
		drain();
		assertEquals(2, commands.size());
	}

	@Test
	public void timeoutAndInvalidResponsesLeaveRefreshAvailable() throws Exception
	{
		for (int tick = 0; tick < 10; tick++)
		{
			plugin.onGameTick(new GameTick());
		}
		flush();
		assertTrue(GearPresetsPanelTest.button(panel, "Refresh setups").isEnabled());
		SwingUtilities.invokeAndWait(() -> GearPresetsPanelTest.button(panel, "Refresh setups").doClick());
		drain();
		reply(token(), "item malformed abcd");
		flush();
		assertTrue(GearPresetsPanelTest.button(panel, "Refresh setups").isEnabled());
		assertTrue(GearPresetsPanelTest.button(panel, "Save current setup").isEnabled());
	}

	private String token()
	{
		return commands.get(commands.size() - 1).split(" ")[2];
	}

	private void reply(String request, String body)
	{
		ChatMessage message = mock(ChatMessage.class);
		when(message.getType()).thenReturn(ChatMessageType.CONSOLE);
		when(message.getMessage()).thenReturn("LGP1 " + request + " " + body);
		plugin.onChatMessage(message);
	}

	private void drain() throws Exception
	{
		while (!pending.isEmpty())
		{
			pending.remove(0).run();
		}
		flush();
	}

	private void flush() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { });
	}
}
