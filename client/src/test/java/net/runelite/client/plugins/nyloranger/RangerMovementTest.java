package net.runelite.client.plugins.nyloranger;

import java.awt.Canvas;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RangerMovementTest
{
	private RangerActions actions;
	private final AtomicInteger presses = new AtomicInteger();
	private final AtomicReference<Point> clicked = new AtomicReference<>();
	private int[][] flags;
	private Widget minimap;

	@Before
	public void setup()
	{
		Client client = mock(Client.class);
		Player player = mock(Player.class);
		WorldView view = mock(WorldView.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getTopLevelWorldView()).thenReturn(view);
		when(client.getWorldView(-1)).thenReturn(view);
		when(view.getId()).thenReturn(-1);
		when(view.getBaseX()).thenReturn(3280);
		when(view.getBaseY()).thenReturn(4224);
		when(view.getSizeX()).thenReturn(104);
		when(view.getSizeY()).thenReturn(104);
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3295, 4248, 0));
		when(player.getLocalLocation()).thenReturn(new LocalPoint(15 * 128 + 64, 24 * 128 + 64));
		flags = new int[104][104];
		CollisionData collision = mock(CollisionData.class);
		when(collision.getFlags()).thenReturn(flags);
		when(view.getCollisionMaps()).thenReturn(new CollisionData[]{collision, collision, collision, collision});
		when(client.getMinimapZoom()).thenReturn(4.0);
		minimap = mock(Widget.class);
		when(client.getWidget(WidgetInfo.FIXED_VIEWPORT_MINIMAP_DRAW_AREA)).thenReturn(minimap);
		when(minimap.getCanvasLocation()).thenReturn(new Point(0, 0));
		when(minimap.getWidth()).thenReturn(160);
		when(minimap.getHeight()).thenReturn(160);
		Canvas canvas = new Canvas();
		canvas.addMouseListener(new MouseAdapter()
		{
			@Override public void mousePressed(MouseEvent event)
			{
				presses.incrementAndGet();
				clicked.set(new Point(event.getX(), event.getY()));
			}
		});
		when(client.getCanvas()).thenReturn(canvas);
		actions = new RangerActions(client, mock(RangerConfig.class), () -> true);
	}

	@Test
	public void walkingActuallyPressesNativeMinimapAtProjectedDestination() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> assertTrue(actions.walk(new WorldPoint(3294, 4248, 0))));
		SwingUtilities.invokeAndWait(() -> { });
		assertEquals(1, presses.get());
		assertEquals(new Point(76, 80), clicked.get());
	}

	@Test
	public void queuedMovementIsDiscardedAfterStop() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			assertTrue(actions.walk(new WorldPoint(3294, 4248, 0)));
			actions.cancelQueuedInput();
		});
		SwingUtilities.invokeAndWait(() -> { });
		assertEquals(0, presses.get());
	}

	@Test
	public void blockedPillarTileIsReplacedWithReachableTile() throws Exception
	{
		flags[14][24] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
		SwingUtilities.invokeAndWait(() -> assertTrue(actions.walk(new WorldPoint(3294, 4248, 0))));
		SwingUtilities.invokeAndWait(() -> { });
		assertNotEquals(new Point(76, 80), clicked.get());
	}

	@Test
	public void hiddenMinimapCannotProduceFalseSuccessfulMovement() throws Exception
	{
		when(minimap.isHidden()).thenReturn(true);
		assertFalse(actions.walk(new WorldPoint(3294, 4248, 0)));
		SwingUtilities.invokeAndWait(() -> { });
		assertEquals(0, presses.get());
		assertTrue(actions.error().contains("minimap"));
	}
}
