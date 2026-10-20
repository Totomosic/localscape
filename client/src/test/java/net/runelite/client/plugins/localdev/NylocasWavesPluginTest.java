package net.runelite.client.plugins.localdev;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class NylocasWavesPluginTest
{
	@Mock
	private Client client;
	@Mock
	private ClientToolbar toolbar;
	@InjectMocks
	private NylocasWavesPlugin plugin;

	@Test
	public void hidesTransportMessagesWhileKeepingSidebarUpdates() throws Exception
	{
		MockitoAnnotations.initMocks(this);
		ArgumentCaptor<NavigationButton> navigation = ArgumentCaptor.forClass(NavigationButton.class);
		SwingUtilities.invokeAndWait(() ->
		{
			plugin.startUp();
			verify(toolbar).addNavigation(navigation.capture());
		});
		for (String prefix : new String[]{"LNW2", "LNW3"})
		{
			String message = prefix + " e9f61c86-97d0-4be3-823f-c990f342cbee waves 1 "
				+ String.join(" ", java.util.Collections.nCopies(prefix.equals("LNW3") ? 37 : 36, "0"));
			ChatMessage event = check(ChatMessageType.CONSOLE);
			event.setMessage(message);
			plugin.onChatMessage(event);
			assertEquals(0, visibility(ChatMessageType.CONSOLE, message, 1));
			assertEquals(message, event.getMessage());
			SwingUtilities.invokeAndWait(() -> assertTrue(hasLabel(navigation.getValue().getPanel(), "Wave 1 / 31")));
		}
		ChatMessage event = check(ChatMessageType.CONSOLE);
		plugin.onChatMessage(event);
		assertEquals(0, visibility(ChatMessageType.CONSOLE, event.getMessage(), 1));
		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals(1, table(navigation.getValue().getPanel()).getRowCount());
			plugin.shutDown();
		});
	}

	@Test
	public void leavesUnrelatedChatAndFilterResultsUntouched()
	{
		MockitoAnnotations.initMocks(this);
		ScriptCallbackEvent unrelated = new ScriptCallbackEvent();
		unrelated.setEventName("otherCallback");
		plugin.onScriptCallbackEvent(unrelated);
		verifyNoInteractions(client);
		assertEquals(1, visibility(ChatMessageType.CONSOLE, "A normal console message", 1));
		assertEquals(1, visibility(ChatMessageType.CONSOLE, "LNW3ish text", 1));
		for (String message : new String[]{"LNW2 payload", "LNW3 payload", "LNC1 payload"})
		{
			assertEquals(1, visibility(ChatMessageType.GAMEMESSAGE, message, 1));
			assertEquals(1, visibility(ChatMessageType.PUBLICCHAT, message, 1));
		}
		assertEquals(0, visibility(ChatMessageType.CONSOLE, "Previously filtered message", 0));
	}

	private int visibility(ChatMessageType type, String message, int initial)
	{
		int[] stack = {123, initial, type.getType(), 42, 456};
		Object[] objects = {"untouched", message, "outside stack"};
		when(client.getIntStack()).thenReturn(stack);
		when(client.getIntStackSize()).thenReturn(4);
		when(client.getObjectStack()).thenReturn(objects);
		when(client.getObjectStackSize()).thenReturn(2);
		ScriptCallbackEvent event = new ScriptCallbackEvent();
		event.setEventName("chatFilterCheck");
		plugin.onScriptCallbackEvent(event);
		assertArrayEquals(new int[]{123, stack[1], type.getType(), 42, 456}, stack);
		assertArrayEquals(new Object[]{"untouched", message, "outside stack"}, objects);
		return stack[1];
	}

	private static boolean hasLabel(Container container, String text)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JLabel && text.equals(((JLabel) child).getText())) return true;
			if (child instanceof Container && hasLabel((Container) child, text)) return true;
		}
		return false;
	}

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
