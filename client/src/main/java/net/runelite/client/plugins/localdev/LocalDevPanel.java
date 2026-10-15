package net.runelite.client.plugins.localdev;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.PluginPanel;

final class LocalDevPanel extends PluginPanel
{
	private static final int RESULT_LIMIT = 100;

	private final JTextField search = new JTextField();
	private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
	private final DefaultListModel<LocalItem> results = new DefaultListModel<>();
	private final JList<LocalItem> itemList = new JList<>(results);
	private final JLabel summary = new JLabel("Log in to load items.");
	private final JLabel status = new JLabel("Log in to use server controls.");
	private final JButton addItem = new JButton("Add Selected Item");
	private final JButton clearInventory = new JButton("Clear Inventory");
	private final JButton restoreSkills = new JButton("Restore Skills");
	private final JButton teleportNylocas = new JButton("Teleport to Nylocas");
	private final JButton teleportNylocasBridge = new JButton("Teleport to Nylocas Bridge");
	private final JButton teleportBloatExit = new JButton("Teleport to Bloat Exit");
	private final JButton startRaid = new JButton("Start Raid");
	private final Timer searchTimer = new Timer(200, event -> filterItems());
	private List<LocalItem> items = Collections.emptyList();
	private boolean loaded;
	private boolean loggedIn;

	LocalDevPanel(Consumer<String> submit)
	{
		add(new JLabel("Local Development"));
		add(new JLabel("Server admin access required."));
		restoreSkills.setToolTipText("Restore all skills, Hitpoints, and Prayer to level 99.");
		restoreSkills.addActionListener(event -> submit.accept("master"));
		clearInventory.setToolTipText("Remove all items from your inventory.");
		clearInventory.addActionListener(event -> submit.accept("invclear"));
		add(restoreSkills);
		add(clearInventory);
		teleportNylocas.setToolTipText("Enter an empty instance of the original Nylocas room.");
		teleportNylocas.addActionListener(event -> submit.accept("nylocas"));
		add(teleportNylocas);
		teleportNylocasBridge.setToolTipText("Teleport onto the bridge outside the Nylocas room.");
		teleportNylocasBridge.addActionListener(event -> submit.accept("nylocasbridge"));
		add(teleportNylocasBridge);
		teleportBloatExit.setToolTipText("Teleport just before the Bloat exit door to the Nylocas bridge.");
		teleportBloatExit.addActionListener(event -> submit.accept("bloatexit"));
		add(teleportBloatExit);
		startRaid.setToolTipText("Start the raid and show the ToB party HP orbs before passing the barrier.");
		startRaid.addActionListener(event -> submit.accept("starttob"));
		add(startRaid);
		add(new JLabel("Search items by name or ID"));
		add(search);
		add(summary);
		itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		itemList.addListSelectionListener(event -> updateButtons());
		JScrollPane scroll = new JScrollPane(itemList);
		scroll.setPreferredSize(new Dimension(200, 260));
		add(scroll);
		JPanel amount = new JPanel(new BorderLayout(6, 0));
		amount.add(new JLabel("Quantity"), BorderLayout.WEST);
		amount.add(quantity, BorderLayout.CENTER);
		add(amount);
		addItem.addActionListener(event ->
		{
			LocalItem selected = itemList.getSelectedValue();
			if (selected == null)
			{
				return;
			}
			try
			{
				quantity.commitEdit();
			}
			catch (java.text.ParseException ex)
			{
				setStatus("Enter a quantity from 1 to 2,147,483,647.");
				return;
			}
			submit.accept("invadd " + selected.id + " " + quantity.getValue());
		});
		add(addItem);
		add(status);
		searchTimer.setRepeats(false);
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				searchTimer.restart();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				searchTimer.restart();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				searchTimer.restart();
			}
		});
		setLoggedIn(false);
	}

	void setItems(List<LocalItem> items)
	{
		loaded = items != null;
		this.items = loaded ? items : Collections.emptyList();
		filterItems();
	}

	void setLoggedIn(boolean loggedIn)
	{
		this.loggedIn = loggedIn;
		setStatus(loggedIn ? "Ready. Server responses appear in game chat." : "Log in to use server controls.");
		if (!loaded)
		{
			filterItems();
		}
		updateButtons();
	}

	void setStatus(String message)
	{
		status.setText("<html>" + message + "</html>");
	}

	void close()
	{
		searchTimer.stop();
	}

	private void filterItems()
	{
		results.clear();
		if (!loaded)
		{
			summary.setText(loggedIn ? "Loading item list…" : "Log in to load items.");
			return;
		}
		String query = search.getText().trim().toLowerCase(Locale.ROOT);
		if (query.isEmpty())
		{
			summary.setText("Enter an item name or ID.");
			return;
		}
		int matches = 0;
		for (LocalItem item : items)
		{
			if (item.matches(query))
			{
				if (matches++ < RESULT_LIMIT)
				{
					results.addElement(item);
				}
			}
		}
		summary.setText(matches == 0 ? "No matching items."
			: "Showing " + results.size() + " of " + matches + " matches.");
		if (!results.isEmpty())
		{
			itemList.setSelectedIndex(0);
		}
	}

	private void updateButtons()
	{
		restoreSkills.setEnabled(loggedIn);
		clearInventory.setEnabled(loggedIn);
		teleportNylocas.setEnabled(loggedIn);
		teleportNylocasBridge.setEnabled(loggedIn);
		teleportBloatExit.setEnabled(loggedIn);
		startRaid.setEnabled(loggedIn);
		addItem.setEnabled(loggedIn && itemList.getSelectedValue() != null);
	}
}
