package net.runelite.client.plugins.localdev;

import java.util.IdentityHashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

final class ZealPrayer
{
	private final Map<Widget, String> originalText = new IdentityHashMap<>();
	private boolean enabled;

	void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	void update(Client client)
	{
		update(client.getWidget(InterfaceID.Prayerbook.CONTAINER));
		update(client.getWidget(InterfaceID.Prayerbook.TOOLTIP));
		update(client.getWidget(InterfaceID.Quickprayer.BUTTONS));
		if (!enabled)
		{
			originalText.clear();
		}
	}

	private void update(Widget widget)
	{
		if (widget == null)
		{
			return;
		}
		String name = widget.getName();
		if (name != null)
		{
			String replacement = enabled ? name.replace("Piety", "Zeal") : name.replace("Zeal", "Piety");
			if (!name.equals(replacement))
			{
				widget.setName(replacement);
			}
		}
		String text = widget.getText();
		if (enabled && text != null && text.contains("Piety"))
		{
			originalText.put(widget, text);
			widget.setText(text.replace("Piety", "Zeal").replace("20%", "25%").replace("23%", "28%"));
		}
		else if (!enabled && text != null && text.contains("Zeal") && originalText.containsKey(widget))
		{
			widget.setText(originalText.get(widget));
		}
		update(widget.getStaticChildren());
		update(widget.getDynamicChildren());
	}

	private void update(Widget[] widgets)
	{
		if (widgets != null)
		{
			for (Widget widget : widgets)
			{
				update(widget);
			}
		}
	}
}
