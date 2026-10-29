package net.runelite.client.plugins.nylomage;

import org.localscape.nylocas.mage.MageStrategy;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import net.runelite.client.ui.PluginPanel;

final class MagePanel extends PluginPanel
{
	private final JTextArea status = new JTextArea("Waiting for login");

	MagePanel(Runnable start, Runnable stop, Runnable startRoom, Consumer<MageStrategy.Style> capture)
	{
		setLayout(new BorderLayout(0, 8));
		add(new JLabel("Duo Nylocas Mage"), BorderLayout.NORTH);
		status.setEditable(false);
		status.setLineWrap(true);
		status.setWrapStyleWord(true);
		status.setOpaque(false);
		add(status, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 5));
		JButton run = new JButton("Start mage");
		run.addActionListener(event -> start.run());
		buttons.add(run);
		JButton enter = new JButton("Start room");
		enter.addActionListener(event -> startRoom.run());
		buttons.add(enter);
		JButton pause = new JButton("Stop mage");
		pause.addActionListener(event -> stop.run());
		buttons.add(pause);
		for (MageStrategy.Style style : MageStrategy.Style.values())
		{
			JButton button = new JButton("Capture " + style.name().toLowerCase() + " equipment");
			button.addActionListener(event -> capture.accept(style));
			buttons.add(button);
		}
		add(buttons, BorderLayout.SOUTH);
	}

	void status(String text) { status.setText(text); }
}
