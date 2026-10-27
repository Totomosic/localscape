package net.runelite.client.plugins.nyloranger;

import java.awt.Canvas;
import java.awt.EventQueue;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.function.BooleanSupplier;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.ScriptID;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Style;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Weapon;

/** All side effects live here: native menu actions and native minimap input, never client-only varp writes. */
final class RangerActions
{
	private static final int ROOM_ZOOM = 128;
	private static final int ROOM_PITCH = 383;
	private final Client client;
	private final RangerConfig config;
	private final BooleanSupplier active;
	private volatile int generation;
	private String error;

	RangerActions(Client client, RangerConfig config, BooleanSupplier active)
	{
		this.client = client;
		this.config = config;
		this.active = active;
	}

	void cancelQueuedInput() { generation++; }
	String error() { return error; }

	boolean validate()
	{
		try
		{
			for (Weapon weapon : Weapon.values()) requireItem(weapon.itemId);
			for (Style style : Style.values()) for (int id : gear(style)) requireItem(id);
			error = null;
			return true;
		}
		catch (IllegalArgumentException exception)
		{
			error = exception.getMessage();
			return false;
		}
	}

	private void requireItem(int id)
	{
		if (!contains(InventoryID.INVENTORY, id) && !contains(InventoryID.EQUIPMENT, id))
		{
			throw new IllegalArgumentException("Missing item " + id + " (inventory or equipment)");
		}
	}

	List<Integer> gear(Style style)
	{
		return parseGear(style == Style.RANGE ? config.rangedGear() : style == Style.MAGIC ? config.magicGear() : config.meleeGear());
	}

	static List<Integer> parseGear(String text)
	{
		Set<Integer> ids = new LinkedHashSet<>();
		if (text == null || text.trim().isEmpty()) return Collections.emptyList();
		for (String token : text.trim().split("[,\\s]+"))
		{
			try
			{
				int id = Integer.parseInt(token);
				if (id <= 0) throw new NumberFormatException();
				ids.add(id);
			}
			catch (NumberFormatException exception)
			{
				throw new IllegalArgumentException("Equipment must contain positive item IDs");
			}
		}
		return new ArrayList<>(ids);
	}

	boolean prepare(Weapon weapon)
	{
		try
		{
			boolean changingWeapon = equippedWeapon() != weapon.itemId;
			for (int id : gear(weapon.style)) if (id != weapon.itemId && !isWeapon(id)) equip(id);
			equip(weapon.itemId);
			// Equip loads the server's saved category stance. Send the desired stance
			// after every weapon change, even if the old weapon already displayed it.
			if (changingWeapon || client.getVarpValue(43) != weapon.stance)
			{
				clickWidget(weapon.stance == 3 ? InterfaceID.CombatInterface._3 : InterfaceID.CombatInterface._1);
			}
			error = null;
			return true;
		}
		catch (IllegalArgumentException exception)
		{
			error = exception.getMessage();
			return false;
		}
	}

	boolean prepareWaiting() { return prepare(Weapon.BOW); }

	void prepareRoomCamera()
	{
		int fixed = client.getVarcIntValue(VarClientID.CAMERA_ZOOM_SMALL);
		int resizable = client.getVarcIntValue(VarClientID.CAMERA_ZOOM_BIG);
		// Use the same zoom script as Camera's reset shortcut, for both layouts.
		// Use the ordinary outer limit, also correcting a previous extreme zoom.
		if (fixed != ROOM_ZOOM || resizable != ROOM_ZOOM)
		{
			client.runScript(ScriptID.CAMERA_DO_ZOOM, ROOM_ZOOM, ROOM_ZOOM);
		}
		// A low viewing angle can still hide the far side of the room.
		if (client.getCameraPitchTarget() < ROOM_PITCH) client.setCameraPitchTarget(ROOM_PITCH);
	}

	boolean attack(NPC npc, Weapon weapon, boolean special)
	{
		if (npc.isDead() || npc.getHealthRatio() == 0 || !prepare(weapon)) return false;
		cancelQueuedInput();
		// Requests are ordered: equipment, stance (when available), special, then attack.
		if (special != (client.getVarpValue(301) != 0)) clickWidget(InterfaceID.CombatInterface.SPECIAL_ATTACK);
		String[] options = npc.getTransformedComposition() == null ? null : npc.getTransformedComposition().getActions();
		int index = action(options, "Attack");
		if (index < 0) { error = "Target has no Attack action"; return false; }
		MenuAction[] operations = {MenuAction.NPC_FIRST_OPTION, MenuAction.NPC_SECOND_OPTION,
			MenuAction.NPC_THIRD_OPTION, MenuAction.NPC_FOURTH_OPTION, MenuAction.NPC_FIFTH_OPTION};
		client.menuAction(0, 0, operations[index], npc.getIndex(), -1, "Attack", npc.getName());
		return true;
	}

	void controls()
	{
		// Native toggles, not setVarpValue: the server must receive the change.
		if (client.getVarpValue(172) == 0) clickWidget(InterfaceID.CombatInterface.RETALIATE);
		if (client.getVarpValue(173) == 0) clickWidget(WidgetInfo.MINIMAP_TOGGLE_RUN_ORB.getPackedId());
	}

	void stopAttacking()
	{
		Player player = client.getLocalPlayer();
		if (player != null && player.getInteracting() != null) walk(normalize(player.getWorldLocation()));
	}

	void cancelAttack()
	{
		Player player = client.getLocalPlayer();
		if (player != null && player.getInteracting() != null) walk(normalize(player.getWorldLocation()), true);
	}

	boolean walk(WorldPoint template)
	{
		return walk(template, false);
	}

	private boolean walk(WorldPoint template, boolean stopping)
	{
		Collection<WorldPoint> points = WorldPoint.toLocalInstance(client, template);
		Player player = client.getLocalPlayer();
		WorldPoint actual = points.stream().min(java.util.Comparator.comparingInt(p -> p.distanceTo(player.getWorldLocation())))
			.orElse(null);
		if (actual == null) { error = "Room tile is outside the loaded instance"; return false; }
		WorldPoint goal = actual;
		actual = reachable().keySet().stream().min(java.util.Comparator.comparingInt(p -> p.distanceTo(goal))).orElse(actual);
		LocalPoint local = LocalPoint.fromWorld(client, actual);
		Point minimap = local == null ? null : Perspective.localToMinimap(client, local);
		if (minimap == null) { error = "Keep the minimap visible to move"; return false; }
		Canvas canvas = client.getCanvas();
		int expected = ++generation;
		// WALK's public menu opcode opens scene selection; it does not reliably select
		// a tile. A native minimap press supplies the actual destination and packet.
		EventQueue.invokeLater(() ->
		{
			if (generation != expected || !stopping && !active.getAsBoolean()) return;
			long now = System.currentTimeMillis();
			canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, now, MouseEvent.BUTTON1_DOWN_MASK,
				minimap.getX(), minimap.getY(), 1, false, MouseEvent.BUTTON1));
			canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, now + 1, 0,
				minimap.getX(), minimap.getY(), 1, false, MouseEvent.BUTTON1));
		});
		return true;
	}

	boolean approach(NPC npc, int range, WorldPoint stage)
	{
		WorldPoint destination = null;
		int best = Integer.MAX_VALUE;
		for (Map.Entry<WorldPoint, Integer> entry : reachable().entrySet())
		{
			WorldPoint point = entry.getKey();
			if (point.toWorldArea().distanceTo(npc.getWorldArea()) > range
				|| !point.toWorldArea().hasLineOfSightTo(client.getTopLevelWorldView(), npc.getWorldArea())) continue;
			WorldPoint source = normalize(LocalPoint.fromWorld(client, point));
			int cost = entry.getValue() * 100 + source.distanceTo(stage);
			if (cost < best) { best = cost; destination = source; }
		}
		if (destination == null) { error = "No reachable attack tile for target"; return false; }
		return walk(destination);
	}

	private Map<WorldPoint, Integer> reachable()
	{
		Map<WorldPoint, Integer> result = new LinkedHashMap<>();
		ArrayDeque<WorldPoint> queue = new ArrayDeque<>();
		WorldPoint origin = client.getLocalPlayer().getWorldLocation();
		result.put(origin, 0);
		queue.add(origin);
		while (!queue.isEmpty())
		{
			WorldPoint current = queue.remove();
			for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++)
			{
				if (dx == 0 && dy == 0) continue;
				WorldPoint next = current.dx(dx).dy(dy);
				if (result.containsKey(next)) continue;
				LocalPoint local = LocalPoint.fromWorld(client, next);
				if (local == null) continue;
				WorldPoint source = normalize(local);
				if (source.getX() < 3290 || source.getX() > 3301 || source.getY() < 4243 || source.getY() > 4254) continue;
				if (!current.toWorldArea().canTravelInDirection(client.getTopLevelWorldView(), dx, dy)) continue;
				result.put(next, result.get(current) + 1);
				queue.add(next);
			}
		}
		return result;
	}

	boolean barrier()
	{
		if (client.getTopLevelWorldView() == null) return false;
		Tile[][][] tiles = client.getTopLevelWorldView().getScene().getTiles();
		for (Tile[] row : tiles[client.getPlane()]) for (Tile tile : row)
		{
			if (tile == null) continue;
			List<TileObject> objects = new ArrayList<>();
			Collections.addAll(objects, tile.getGameObjects());
			objects.add(tile.getWallObject());
			objects.add(tile.getDecorativeObject());
			for (TileObject object : objects)
			{
				if (object == null) continue;
				Point position = object instanceof GameObject ? ((GameObject) object).getSceneMinLocation()
					: new Point(object.getLocalLocation().getSceneX(), object.getLocalLocation().getSceneY());
				WorldPoint source = normalize(LocalPoint.fromScene(position.getX(), position.getY(), client.getTopLevelWorldView()));
				if (source.getPlane() != 0 || source.getY() != 4255 || source.getX() < 3295 || source.getX() > 3296) continue;
				String[] options = client.getObjectDefinition(object.getId()).getActions();
				if (options == null || options.length == 0 || options[0] == null) continue;
				client.menuAction(position.getX(), position.getY(), MenuAction.GAME_OBJECT_FIRST_OPTION,
					object.getId(), -1, options[0], "Nylocas barrier");
				// Intentionally never choose Enter, Continue, or any dialogue option.
				return true;
			}
		}
		return false;
	}

	WorldPoint normalize(LocalPoint point) { return WorldPoint.fromLocalInstance(client, point); }
	WorldPoint normalize(WorldPoint point) { return normalize(LocalPoint.fromWorld(client, point)); }

	int equippedWeapon()
	{
		ItemContainer equipped = client.getItemContainer(InventoryID.EQUIPMENT);
		Item item = equipped == null ? null : equipped.getItem(3);
		return item == null ? -1 : item.getId();
	}

	String capture(Style style)
	{
		ItemContainer equipped = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipped == null) return "";
		List<String> ids = new ArrayList<>();
		Item[] items = equipped.getItems();
		for (int slot = 0; slot < items.length; slot++)
		{
			// The controller chooses its own weapon. Ranged/melee weapons are two-handed.
			if (slot == 3 || slot == 5 && style != Style.MAGIC) continue;
			if (items[slot] != null && items[slot].getId() > 0) ids.add(Integer.toString(items[slot].getId()));
		}
		return String.join(",", ids);
	}

	private void equip(int id)
	{
		if (contains(InventoryID.EQUIPMENT, id)) return;
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory != null)
		{
			Item[] items = inventory.getItems();
			for (int slot = 0; slot < items.length; slot++) if (items[slot].getId() == id)
			{
				if (!itemAction(slot, id, "Wear", "Wield", "Equip"))
					throw new IllegalArgumentException("Item " + id + " cannot be equipped");
				return;
			}
		}
		throw new IllegalArgumentException("Missing item " + id);
	}

	boolean drinkPrayerPotion(int slot, int id) { return itemAction(slot, id, "Drink"); }

	private boolean itemAction(int slot, int id, String... wanted)
	{
		Widget root = client.getWidget(InterfaceID.Inventory.ITEMS);
		Widget child = root == null ? null : root.getChild(slot);
		int option = child == null ? -1 : itemOption(child.getActions(), wanted);
		if (root != null && root.isIf3() && option >= 0)
		{
			client.menuAction(slot, root.getId(), MenuAction.CC_OP, option + 1, id, child.getActions()[option], "");
			return true;
		}
		String[] options = client.getItemDefinition(id).getInventoryActions();
		option = itemOption(options, wanted);
		if (option < 0) return false;
		MenuAction[] operations = {MenuAction.ITEM_FIRST_OPTION, MenuAction.ITEM_SECOND_OPTION,
			MenuAction.ITEM_THIRD_OPTION, MenuAction.ITEM_FOURTH_OPTION, MenuAction.ITEM_FIFTH_OPTION};
		client.menuAction(slot, InterfaceID.Inventory.ITEMS, operations[option], id, id, options[option], "");
		return true;
	}

	private boolean isWeapon(int id)
	{
		for (Weapon weapon : Weapon.values()) if (weapon.itemId == id) return true;
		return false;
	}

	private boolean contains(InventoryID container, int id)
	{
		ItemContainer items = client.getItemContainer(container);
		return items != null && items.contains(id);
	}

	private void clickWidget(int id)
	{
		Widget widget = client.getWidget(id);
		if (widget != null) clickWidget(widget);
	}

	private void clickWidget(Widget widget)
	{
		client.menuAction(widget.getIndex(), widget.getId(), widget.isIf3() ? MenuAction.CC_OP : MenuAction.WIDGET_TYPE_1,
			1, -1, "Activate", "");
	}

	private static int itemOption(String[] options, String[] wanted)
	{
		if (options == null) return -1;
		for (int i = 0; i < Math.min(options.length, 5); i++)
			for (String value : wanted) if (value.equalsIgnoreCase(options[i])) return i;
		return -1;
	}

	private static int action(String[] options, String value)
	{
		if (options != null) for (int i = 0; i < Math.min(5, options.length); i++) if (value.equalsIgnoreCase(options[i])) return i;
		return -1;
	}
}
