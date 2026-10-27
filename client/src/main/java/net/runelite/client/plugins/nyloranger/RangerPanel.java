package net.runelite.client.plugins.nyloranger;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import net.runelite.client.ui.PluginPanel;

final class RangerPanel extends PluginPanel
{
	private final JTextArea status = new JTextArea("Waiting for login");

	RangerPanel(Runnable start, Runnable stop, Consumer<RangerStrategy.Style> capture)
	{
		setLayout(new BorderLayout(0, 8));
		add(new JLabel("Duo Nylocas Ranger"), BorderLayout.NORTH);
		status.setEditable(false);
		status.setLineWrap(true);
		status.setWrapStyleWord(true);
		status.setOpaque(false);
		add(status, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 5));
		JButton run = new JButton("Start ranger");
		run.addActionListener(event -> start.run());
		buttons.add(run);
		JButton pause = new JButton("Stop ranger");
		pause.addActionListener(event -> stop.run());
		buttons.add(pause);
		for (RangerStrategy.Style style : RangerStrategy.Style.values())
		{
			JButton button = new JButton("Capture " + style.name().toLowerCase() + " equipment");
			button.addActionListener(event -> capture.accept(style));
			buttons.add(button);
		}
		add(buttons, BorderLayout.SOUTH);
	}

	void status(String text) { status.setText(text); }
}
