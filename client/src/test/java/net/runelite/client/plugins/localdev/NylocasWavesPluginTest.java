package net.runelite.client.plugins.localdev;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

public class NylocasWavesPluginTest
{
	@Mock
	private ClientToolbar toolbar;
	@InjectMocks
	private NylocasWavesPlugin plugin;

	@Test
	public void ownsItsSidebarAndDiscardsQueuedUpdatesAfterShutdown() throws Exception
	{
		MockitoAnnotations.initMocks(this);
		ArgumentCaptor<NavigationButton> navigation = ArgumentCaptor.forClass(NavigationButton.class);
		SwingUtilities.invokeAndWait(() ->
		{
			plugin.startUp();
			verify(toolbar).addNavigation(navigation.capture());
			assertEquals("Nylocas Waves", navigation.getValue().getTooltip());
			plugin.onChatMessage(check(ChatMessageType.CONSOLE));
			plugin.shutDown();
			verify(toolbar).removeNavigation(navigation.getValue());
		});
		SwingUtilities.invokeAndWait(() -> assertEquals(0, table(navigation.getValue().getPanel()).getRowCount()));
	}

	@Test
	public void acceptsConsoleReportsAndIgnoresGameChat() throws Exception
	{
		MockitoAnnotations.initMocks(this);
		ArgumentCaptor<NavigationButton> navigation = ArgumentCaptor.forClass(NavigationButton.class);
		SwingUtilities.invokeAndWait(() ->
		{
			plugin.startUp();
			verify(toolbar).addNavigation(navigation.capture());
			plugin.onChatMessage(check(ChatMessageType.GAMEMESSAGE));
		});
		SwingUtilities.invokeAndWait(() -> assertEquals(0, table(navigation.getValue().getPanel()).getRowCount()));
		plugin.onChatMessage(check(ChatMessageType.CONSOLE));
		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals(1, table(navigation.getValue().getPanel()).getRowCount());
			plugin.shutDown();
		});
	}

	private static ChatMessage check(ChatMessageType type)
	{
		ChatMessage event = new ChatMessage();
		event.setType(type);
		event.setMessage("LNC1 e9f61c86-97d0-4be3-823f-c990f342cbee 1 4 1 0 0 12");
		return event;
	}

	private static JTable table(Container container)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JTable) return (JTable) child;
			if (child instanceof Container)
			{
				JTable found = table((Container) child);
				if (found != null) return found;
			}
		}
		return null;
	}
}
