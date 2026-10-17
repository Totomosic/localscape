package net.runelite.client.plugins.localdev;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class LocalDevPanelTest
{
	@Test
	public void selectsEachSpellbookAndSynchronizesWithoutSubmitting() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> commands = new ArrayList<>();
			LocalDevPanel panel = new LocalDevPanel(commands::add);
			try
			{
				JComboBox<?> books = component(panel, JComboBox.class);
				assertFalse(books.isEnabled());
				panel.setLoggedIn(true);
				for (String name : Arrays.asList("Ancient", "Arceuus", "Lunar", "Standard"))
				{
					books.setSelectedItem(name);
				}
				assertEquals(Arrays.asList("spellbook ancient", "spellbook arceuus", "spellbook lunar", "spellbook standard"), commands);
				panel.setSpellbook(2);
				assertEquals("Lunar", books.getSelectedItem());
				panel.setSpellbook(3);
				assertEquals("Arceuus", books.getSelectedItem());
				assertEquals(4, commands.size());
				panel.setLoggedIn(false);
				assertFalse(books.isEnabled());
			}
			finally
			{
				panel.close();
			}
		});
	}

	@Test
	public void searchesLocalItemsAndSendsSelectedIdWithQuantity() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> commands = new ArrayList<>();
			LocalDevPanel panel = new LocalDevPanel(commands::add);
			try
			{
				panel.setLoggedIn(true);
				component(panel, JTextField.class).setText("DRAGON CLAWS");
				panel.setItems(Arrays.asList(
					new LocalItem(995, "Coins", false),
					new LocalItem(13652, "Dragon claws", false),
					new LocalItem(13653, "Dragon claws", true)));
				JList<?> items = (JList<?>) component(panel, JScrollPane.class).getViewport().getView();
				assertEquals(2, items.getModel().getSize());
				items.setSelectedIndex(1);
				assertEquals("13653: Dragon claws (noted)", items.getSelectedValue().toString());
				component(panel, JSpinner.class).setValue(5);
				button(panel, "Add Selected Item").doClick();
				button(panel, "Clear Inventory").doClick();
				button(panel, "Restore Skills").doClick();
				assertEquals(Arrays.asList("invadd 13653 5", "invclear", "master"), commands);

				component(panel, JTextField.class).setText("995");
				panel.setItems(Arrays.asList(new LocalItem(995, "Coins", false)));
				assertEquals(1, items.getModel().getSize());
				assertEquals("995: Coins", items.getSelectedValue().toString());
			}
			finally
			{
				panel.close();
			}
		});
	}

	@Test
	public void disablesActionsWhenLoggedOutOrNoItemMatches() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> commands = new ArrayList<>();
			LocalDevPanel panel = new LocalDevPanel(commands::add);
			try
			{
				assertFalse(button(panel, "Clear Inventory").isEnabled());
				assertFalse(button(panel, "Restore Skills").isEnabled());
				panel.setLoggedIn(true);
				component(panel, JTextField.class).setText("nonexistent");
				panel.setItems(Arrays.asList(new LocalItem(995, "Coins", false)));
				assertFalse(button(panel, "Add Selected Item").isEnabled());
				panel.setLoggedIn(false);
				button(panel, "Clear Inventory").doClick();
				button(panel, "Restore Skills").doClick();
				assertEquals(0, commands.size());
			}
			finally
			{
				panel.close();
			}
		});
	}

	@Test
	public void rejectsInvalidQuantityRatherThanSendingPreviousValue() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> commands = new ArrayList<>();
			LocalDevPanel panel = new LocalDevPanel(commands::add);
			try
			{
				panel.setLoggedIn(true);
				component(panel, JTextField.class).setText("coins");
				panel.setItems(Arrays.asList(new LocalItem(995, "Coins", false)));
				JSpinner spinner = component(panel, JSpinner.class);
				((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setText("0");
				button(panel, "Add Selected Item").doClick();
				assertEquals(0, commands.size());
			}
			finally
			{
				panel.close();
			}
		});
	}

	private static JButton button(LocalDevPanel panel, String text)
	{
		return Arrays.stream(panel.getComponents())
			.filter(component -> component instanceof JButton && ((JButton) component).getText().equals(text))
			.map(component -> (JButton) component).findFirst().get();
	}

	private static <T extends Component> T component(LocalDevPanel panel, Class<T> type)
	{
		return descendants(panel).filter(type::isInstance).map(type::cast).findFirst().get();
	}

	private static Stream<Component> descendants(Component component)
	{
		return component instanceof Container
			? Stream.concat(Stream.of(component), Arrays.stream(((Container) component).getComponents())
				.flatMap(LocalDevPanelTest::descendants))
			: Stream.of(component);
	}
}
