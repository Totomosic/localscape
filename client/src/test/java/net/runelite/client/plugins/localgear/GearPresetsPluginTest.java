package net.runelite.client.plugins.localgear;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GearPresetsPluginTest
{
	@Mock private Client client;
	@Mock private ClientThread clientThread;
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
		SwingUtilities.invokeAndWait(() -> panel = (GearPresetsPanel) plugin.startUp());
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
		reply(request, "item " + id + " " + GearPresetsPlugin.encode("[Alice] Nylo Duo"));
		reply(request, "end " + GearPresetsPlugin.encode("Ready"));
		SwingUtilities.invokeAndWait(() -> GearPresetsPanelTest.button(panel, "[Alice] Nylo Duo").doClick());
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
	public void acceptsSharedListsBeyondOnePlayersLimitAndFullLengthPrefixedNames() throws Exception
	{
		String request = token();
		reply(request, "begin");
		for (int i = 0; i < 101; i++)
		{
			reply(request, "item " + UUID.randomUUID() + " " + GearPresetsPlugin.encode("[Bob] Shared " + i));
		}
		String fullName = "[Player123456] 123456789012345678901234";
		reply(request, "item " + UUID.randomUUID() + " " + GearPresetsPlugin.encode(fullName));
		reply(request, "end " + GearPresetsPlugin.encode("Ready"));
		flush();
		assertEquals(1, GearPresetsPanelTest.buttons(panel, "[Bob] Shared 100").size());
		assertEquals(1, GearPresetsPanelTest.buttons(panel, fullName).size());
		SwingUtilities.invokeAndWait(() ->
		{
			GearPresetsPanelTest.button(panel, fullName).doClick();
			assertEquals("123456789012345678901234", GearPresetsPanelTest.fields(panel).get(1).getText());
		});
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

	@Test
	public void loadsAndSavesDefaultSelectionAndNoneWithoutApplyingGear() throws Exception
	{
		String id = "11111111-2222-3333-4444-555555555555";
		replyList(id, id);
		flush();
		assertEquals("[Alice] Nylo Duo", GearPresetsPanelTest.dropdown(panel).getSelectedItem().toString());
		assertEquals(1, commands.size());
		SwingUtilities.invokeAndWait(() -> GearPresetsPanelTest.dropdown(panel).setSelectedIndex(0));
		drain();
		assertTrue(commands.get(1).matches("gearsetups default [0-9a-f]{8} none"));
		replyList(id, "none");
		flush();
		assertEquals("None", GearPresetsPanelTest.dropdown(panel).getSelectedItem().toString());
		SwingUtilities.invokeAndWait(() -> GearPresetsPanelTest.dropdown(panel).setSelectedIndex(1));
		drain();
		assertTrue(commands.get(2).matches("gearsetups default [0-9a-f]{8} " + id));
		replyList(id, id);
		flush();
		assertEquals(3, commands.size());
		GameStateChanged logout = new GameStateChanged();
		logout.setGameState(GameState.LOGIN_SCREEN);
		plugin.onGameStateChanged(logout);
		flush();
		assertEquals("None", GearPresetsPanelTest.dropdown(panel).getSelectedItem().toString());
	}

	@Test
	public void restoresConfirmedDefaultWhenSavingFailsOrTimesOut() throws Exception
	{
		String id = "11111111-2222-3333-4444-555555555555";
		replyList(id, id);
		flush();
		SwingUtilities.invokeAndWait(() -> GearPresetsPanelTest.dropdown(panel).setSelectedIndex(0));
		drain();
		reply(token(), "error " + GearPresetsPlugin.encode("Could not save default."));
		flush();
		assertEquals("[Alice] Nylo Duo", GearPresetsPanelTest.dropdown(panel).getSelectedItem().toString());
		SwingUtilities.invokeAndWait(() -> GearPresetsPanelTest.dropdown(panel).setSelectedIndex(0));
		drain();
		for (int tick = 0; tick < 10; tick++)
		{
			plugin.onGameTick(new GameTick());
		}
		flush();
		assertEquals("[Alice] Nylo Duo", GearPresetsPanelTest.dropdown(panel).getSelectedItem().toString());
		assertEquals(3, commands.size());
	}

	private void replyList(String id, String defaultId)
	{
		String request = token();
		reply(request, "begin");
		reply(request, "default " + defaultId);
		reply(request, "item " + id + " " + GearPresetsPlugin.encode("[Alice] Nylo Duo"));
		reply(request, "end " + GearPresetsPlugin.encode("Ready"));
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
