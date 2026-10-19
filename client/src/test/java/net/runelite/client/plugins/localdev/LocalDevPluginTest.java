package net.runelite.client.plugins.localdev;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.localgear.GearPresetsPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.PluginPanel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
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
	@Mock
	private GearPresetsPlugin gearPresets;
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
		SwingUtilities.invokeAndWait(() ->
		{
			when(gearPresets.startUp()).thenReturn(new PluginPanel() { });
			plugin.startUp();
		});
		pending.clear();
	}

	@After
	public void tearDown() throws Exception
	{
		SwingUtilities.invokeAndWait(plugin::shutDown);
	}

	@Test
	public void raidStateLocksOptionsForPartyMembersAndUnlocksForANewLobby() throws Exception
	{
		ArgumentCaptor<NavigationButton> navigation = ArgumentCaptor.forClass(NavigationButton.class);
		verify(clientToolbar).addNavigation(navigation.capture());
		LocalToolsPanel tools = (LocalToolsPanel) navigation.getValue().getPanel();
		JPanel section = (JPanel) tools.getComponent(0);
		TheatreControlsPanel theatre = (TheatreControlsPanel) ((BorderLayout) section.getLayout()).getLayoutComponent(BorderLayout.CENTER);
		GameStateChanged login = new GameStateChanged();
		login.setGameState(GameState.LOGGED_IN);
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_PARTYSTATUS)).thenReturn(2);
		plugin.onGameStateChanged(login);
		SwingUtilities.invokeAndWait(() -> assertFalse(TheatreControlsPanelTest.wave(theatre).isEnabled()));
		when(client.getVarbitValue(VarbitID.TOB_CLIENT_PARTYSTATUS)).thenReturn(0);
		plugin.onGameTick(new GameTick());
		SwingUtilities.invokeAndWait(() -> assertTrue(TheatreControlsPanelTest.wave(theatre).isEnabled()));
	}

	@Test
	public void hostsOneSidebarAndForwardsGearEvents() throws Exception
	{
		ArgumentCaptor<NavigationButton> navigation = ArgumentCaptor.forClass(NavigationButton.class);
		verify(clientToolbar).addNavigation(navigation.capture());
		assertEquals(4, navigation.getValue().getPanel().getComponentCount());
		GameStateChanged state = new GameStateChanged();
		state.setGameState(GameState.LOGIN_SCREEN);
		GameTick tick = new GameTick();
		ChatMessage chat = new ChatMessage();
		plugin.onGameStateChanged(state);
		plugin.onGameTick(tick);
		plugin.onChatMessage(chat);
		verify(gearPresets).onGameStateChanged(state);
		verify(gearPresets).onGameTick(tick);
		verify(gearPresets).onChatMessage(chat);
	}

	@Test
	public void zealDisplayFollowsServerStateAndResetsOnLogout()
	{
		Widget prayer = mock(Widget.class);
		when(client.getWidget(InterfaceID.Prayerbook.CONTAINER)).thenReturn(prayer);
		when(prayer.getName()).thenReturn("Piety");
		ChatMessage state = new ChatMessage();
		state.setType(ChatMessageType.PUBLICCHAT);
		state.setMessage("localscape:zeal:1");
		plugin.onChatMessage(state);
		verify(prayer, never()).setName("Zeal");
		state.setType(ChatMessageType.CONSOLE);
		plugin.onChatMessage(state);
		verify(prayer).setName("Zeal");
		when(prayer.getName()).thenReturn("Zeal");
		GameStateChanged logout = new GameStateChanged();
		logout.setGameState(GameState.LOGIN_SCREEN);
		plugin.onGameStateChanged(logout);
		verify(prayer).setName("Piety");
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
