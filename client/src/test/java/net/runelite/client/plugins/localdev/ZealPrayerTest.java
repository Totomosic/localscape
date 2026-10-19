package net.runelite.client.plugins.localdev;

import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZealPrayerTest
{
	@Test
	public void renamesPrayerAndTooltipAndRestoresPiety()
	{
		Client client = mock(Client.class);
		Widget prayer = mock(Widget.class);
		Widget tooltip = mock(Widget.class);
		Widget quickPrayer = mock(Widget.class);
		Widget buttons = mock(Widget.class);
		when(client.getWidget(InterfaceID.Prayerbook.CONTAINER)).thenReturn(prayer);
		when(client.getWidget(InterfaceID.Prayerbook.TOOLTIP)).thenReturn(tooltip);
		when(client.getWidget(InterfaceID.Quickprayer.BUTTONS)).thenReturn(buttons);
		when(buttons.getDynamicChildren()).thenReturn(new Widget[]{quickPrayer});
		when(prayer.getName()).thenReturn("<col=ff9040>Piety</col>");
		when(quickPrayer.getName()).thenReturn("Piety");
		String original = "Level 70<br>Piety<br>+20% Attack, +23% Strength, +25% Defence";
		String upgraded = "Level 70<br>Zeal<br>+25% Attack, +28% Strength, +25% Defence";
		when(tooltip.getText()).thenReturn(original);
		ZealPrayer zeal = new ZealPrayer();
		zeal.setEnabled(true);
		zeal.update(client);
		verify(prayer).setName("<col=ff9040>Zeal</col>");
		verify(quickPrayer).setName("Zeal");
		verify(tooltip).setText(upgraded);

		when(prayer.getName()).thenReturn("<col=ff9040>Zeal</col>");
		when(quickPrayer.getName()).thenReturn("Zeal");
		when(tooltip.getText()).thenReturn(upgraded);
		zeal.setEnabled(false);
		zeal.update(client);
		verify(prayer).setName("<col=ff9040>Piety</col>");
		verify(quickPrayer).setName("Piety");
		verify(tooltip).setText(original);
	}
}
