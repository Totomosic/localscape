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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class NylocasWavesPluginTest
{
	private static final String LONG_REPORT = "LNW4 62105b94-b7ee-47ba-bc30-b43e2586783b waves "
		+ "28 26 17 1 0 69 60 15 9 23 34 27 15 2 4 9 18 7 5 10 7 0 0 0 "
		+ "69 60 54011924 14 7 8103637 0 0 0 0 0 0 4 902663 2 "
		+ "QQ 33 31 27694478 8 5 5041096 0 0 0 0 0 0 3 911442 "
		+ "Qg 36 29 26317446 6 2 3062542 0 0 0 0 0 0 1 704350";
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
		for (String prefix : new String[]{"LNW2", "LNW3", "LNW4"})
		{
			String message = prefix + " e9f61c86-97d0-4be3-823f-c990f342cbee waves 1 "
				+ String.join(" ", java.util.Collections.nCopies(prefix.equals("LNW2") ? 36 : 37, "0"))
				+ (prefix.equals("LNW4") ? " 0" : "");
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
		for (String message : new String[]{"LNW2 payload", "LNW3 payload", "LNW4 payload", "LNF1 payload", "LNC1 payload"})
		{
			assertEquals(0, visibility(ChatMessageType.CONSOLE, message, 1));
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

	@Test
	public void reassemblesOversizedWave28ReportBeforeUpdatingSidebar() throws Exception
	{
		MockitoAnnotations.initMocks(this);
		ArgumentCaptor<NavigationButton> navigation = ArgumentCaptor.forClass(NavigationButton.class);
		SwingUtilities.invokeAndWait(() ->
		{
			plugin.startUp();
			verify(toolbar).addNavigation(navigation.capture());
		});
		String header = "LNF1 62105b94-b7ee-47ba-bc30-b43e2586783b 1 ";
		ChatMessage event = check(ChatMessageType.CONSOLE);
		event.setMessage(header + "0 2 " + LONG_REPORT.substring(0, 180));
		plugin.onChatMessage(event);
		SwingUtilities.invokeAndWait(() -> assertFalse(hasLabel(navigation.getValue().getPanel(), "Wave 28 / 31")));
		event.setMessage(header + "1 2 " + LONG_REPORT.substring(180));
		plugin.onChatMessage(event);
		SwingUtilities.invokeAndWait(() ->
		{
			assertTrue(hasLabel(navigation.getValue().getPanel(), "Wave 28 / 31"));
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
