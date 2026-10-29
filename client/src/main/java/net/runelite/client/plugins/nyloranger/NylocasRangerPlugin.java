package net.runelite.client.plugins.nyloranger;

import org.localscape.nylocas.Tile;
import org.localscape.nylocas.ranger.RangerStrategy;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.AnimationID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
	name = "Duo Nylocas Ranger",
	description = "Plays the recording-derived duo ranger wave strategy on LocalScape",
	tags = {"nylocas", "ranger", "duo", "local"},
	enabledByDefault = false
)
public class NylocasRangerPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ClientToolbar toolbar;
	@Inject private ConfigManager configManager;
	@Inject private RangerConfig config;

	private final RangerStrategy strategy = new RangerStrategy();
	private final Map<NPC, Crab> tracked = new IdentityHashMap<>();
	private final Map<NPC, Integer> births = new IdentityHashMap<>();
	private Room room = new Room();
	private RangerActions actions;
	private RangerPrayers prayers;
	private volatile boolean running;
	private volatile RangerPanel panel;
	private NavigationButton navigation;
	private Decision pending;
	private int requestedTick;
	private int serial;
	private int lastTick = -1;
	private boolean entered;
	private boolean validated;
	private boolean cameraPrepared;
	private boolean roomStartRequested;
	private boolean roomStartSubmitted;

	@Provides
	RangerConfig provideConfig(ConfigManager manager) { return manager.getConfig(RangerConfig.class); }

	@Override
	protected void startUp()
	{
		actions = new RangerActions(client, config, () -> running);
		prayers = new RangerPrayers(client, config, actions);
		panel = new RangerPanel(() -> clientThread.invokeLater(this::start),
			() -> clientThread.invokeLater(() -> stop("Stopped")),
			() -> clientThread.invokeLater(this::startRoom),
			style -> clientThread.invokeLater(() -> capture(style)));
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(new Color(85, 200, 110));
		graphics.drawOval(1, 1, 13, 13);
		graphics.drawLine(3, 13, 13, 3);
		graphics.dispose();
		navigation = NavigationButton.builder().tooltip("Duo Nylocas Ranger").icon(image).priority(3).panel(panel).build();
		toolbar.addNavigation(navigation);
		clientThread.invokeLater(this::start);
	}

	@Override
	protected void shutDown()
	{
		running = false;
		if (actions != null) actions.cancelQueuedInput();
		if (entered && client.getGameState() == GameState.LOGGED_IN) actions.cancelAttack();
		panel = null;
		toolbar.removeNavigation(navigation);
		clientThread.invokeLater(this::reset);
	}

	private void start()
	{
		if (panel == null) return;
		reset();
		Player player = client.getLocalPlayer();
		if (client.getGameState() == GameState.LOGGED_IN && player != null)
		{
			if (isPlayerDead()) { stop("Stopped: player died."); return; }
			WorldPoint point = actions.normalize(player.getWorldLocation());
			if (inArena(point) && client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL) > 0)
			{
				stop("Start outside the barrier so crab birth ages can be tracked.");
				return;
			}
		}
		running = true;
		status("Armed. Waiting at the Nylocas barrier for the partner.");
	}

	private void startRoom()
	{
		if (!running || client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
		{
			status("Start the agent at the barrier before starting the room.");
			return;
		}
		WorldPoint point = actions.normalize(client.getLocalPlayer().getWorldLocation());
		if (entered || !nearRoom(point) || point.getY() < 4255 || isPlayerDead())
		{
			status("Start room is available while waiting outside the Nylocas barrier.");
			return;
		}
		roomStartRequested = true;
		roomStartSubmitted = false;
		status("Room start requested. Waiting for the Nylocas entry confirmation.");
	}

	private void stop(String reason)
	{
		running = false;
		actions.cancelQueuedInput();
		if (entered && client.getGameState() == GameState.LOGGED_IN) actions.cancelAttack();
		reset();
		status(reason);
	}

	private void reset()
	{
		if (prayers != null) prayers.reset();
		tracked.clear();
		births.clear();
		room = new Room();
		strategy.reset();
		pending = null;
		entered = false;
		validated = false;
		cameraPrepared = false;
		roomStartRequested = false;
		roomStartSubmitted = false;
		lastTick = -1;
	}

	private void capture(Style style)
	{
		if (client.getGameState() != GameState.LOGGED_IN) { status("Log in to capture equipment."); return; }
		stop("Captured " + style.name().toLowerCase() + " equipment. Start when ready.");
		String key = style == Style.RANGE ? "rangedGear" : style == Style.MAGIC ? "magicGear" : "meleeGear";
		configManager.setConfiguration(RangerConfig.GROUP, key, actions.capture(style));
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING
			|| event.getGameState() == GameState.CONNECTION_LOST)
		{
			stop("Stopped after logout, disconnect, or world change. Start again at the barrier.");
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (running && regular(event.getNpc())) births.put(event.getNpc(), client.getTickCount());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		tracked.remove(event.getNpc());
		births.remove(event.getNpc());
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		Crab crab = tracked.get(event.getActor());
		if (crab != null && event.getHitsplat().isMine()) crab.ownHitUntil = 0;
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!running || !entered || !(event.getActor() instanceof Player)) return;
		Actor actor = event.getActor();
		if (!(actor.getInteracting() instanceof NPC)) return;
		Crab crab = tracked.get((NPC) actor.getInteracting());
		if (crab == null || !attackAnimation(actor.getAnimation())) return;
		int tick = client.getTickCount();
		if (actor == client.getLocalPlayer())
		{
			if (pending != null && pending.target == crab) confirm(tick);
		}
		else crab.claimedUntil = tick + (crab.style == Style.MELEE ? 2 : 4);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!running || client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null) return;
		int tick = client.getTickCount();
		if (lastTick == tick) return;
		lastTick = tick;
		Player player = client.getLocalPlayer();
		if (isPlayerDead()) { stop("Stopped: player died."); return; }
		WorldPoint point = actions.normalize(player.getWorldLocation());
		if (!nearRoom(point))
		{
			if (entered) stop("Stopped: left Nylocas.");
			else status("Armed. Move to the Nylocas entrance.");
			return;
		}
		if (!validated)
		{
			if (!actions.validate()) { stop(actions.error()); return; }
			validated = true;
		}
		if (point.getY() >= 4255)
		{
			if (entered) { stop("Stopped: left the room."); return; }
			if (!cameraPrepared)
			{
				actions.prepareRoomCamera();
				cameraPrepared = true;
			}
			if (!actions.prepareWaiting()) { stop(actions.error()); return; }
			actions.controls();
			if (roomStartRequested && !roomStartSubmitted && actions.confirmRoomStart()) roomStartSubmitted = true;
			if (roomStartSubmitted)
			{
				status("Room start confirmed. Waiting to enter Nylocas.");
				return;
			}
			status(actions.barrier() ? "Clicking barrier; waiting. Press Start room to enter."
				: "Waiting for Nylocas barrier to load.");
			return;
		}
		if (!entered)
		{
			entered = true;
			roomStartRequested = false;
			roomStartSubmitted = false;
			room.ready = tick;
		}
		room.player = tile(point);
		int previousWave = room.wave;
		int wave = client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL);
		if (wave >= 1 && wave <= 31 && wave > room.wave)
		{
			room.wave = wave;
			room.spawns[wave] = tick;
		}
		for (NPC npc : client.getNpcs())
		{
			if (npc.getId() >= 8354 && npc.getId() <= 8357)
			{
				stop("Wave strategy complete: Nylocas boss spawned.");
				return;
			}
			if (!regular(npc)) continue;
			// Server-tile anchors, not interpolated/model-center local coordinates.
			WorldPoint position = actions.normalize(npc.getWorldLocation());
			Crab crab = tracked.get(npc);
			if (crab == null)
			{
				// HUD wave is authoritative; do not advance by a timer through a stall.
				if (room.wave == 0) continue;
				Lane lane = lane(position);
				char slot = lane == Lane.SOUTH ? (position.getX() == 3295 ? 'W' : 'E')
					: lane == Lane.WEST || lane == Lane.EAST ? (position.getY() == 4249 ? 'N' : 'S') : 0;
				crab = new Crab(++serial, npc.getIndex(), births.getOrDefault(npc, tick), room.wave, lane,
					slot, big(npc), style(npc), tile(position));
				tracked.put(npc, crab);
			}
			crab.point = tile(position);
			crab.style = style(npc);
			int scale = npc.getHealthScale();
			if (scale > 0 && npc.getHealthRatio() >= 0)
				crab.hp = (int) Math.ceil((crab.big ? 16.0 : 8.0) * npc.getHealthRatio() / scale);
			crab.dying = dying(npc);
		}
		births.clear();
		room.crabs.clear();
		room.crabs.addAll(tracked.values());
		room.obstructed.clear();
		for (Map.Entry<NPC, Crab> entry : tracked.entrySet())
			if (!player.getWorldArea().hasLineOfSightTo(client.getTopLevelWorldView(), entry.getKey().getWorldArea()))
				room.obstructed.add(entry.getValue().key);
		room.specialEnergy = client.getVarpValue(300);
		prayers.tick(tracked, tick);
		actions.controls();
		if (pending != null && (room.wave >= 20 && previousWave < 20 && pending.target.big && pending.target.wave < 20
			|| room.wave == 31 && pending.target.wave == 29))
		{
			// Discard unacknowledged pre-cap work at the phase change; preserve the possible cooldown.
			actions.stopAttacking();
			pending = null;
		}
		if (pending != null)
		{
			// Blowpipe/Eye animations can be continuous, without AnimationChanged.
			if (!pending.pathToTarget && tick > requestedTick && player.getInteracting() instanceof NPC
				&& tracked.get((NPC) player.getInteracting()) == pending.target
				&& actions.equippedWeapon() == pending.weapon.itemId && weaponAnimation(pending.weapon, player.getAnimation()))
			{
				confirm(tick);
			}
			else
			{
				pending = null;
				// A request is not a launch. Reconsider it on the next tick,
				// without sending movement that could cancel an attack still in flight.
				room.ready = tick;
			}
		}
		Decision decision = strategy.decide(room, tick);
		if (decision.target == null)
		{
			if (!actions.prepare(decision.weapon)) { stop(actions.error()); return; }
			NPC approach = decision.approach == null ? null : npc(decision.approach);
			if (approach != null && (decision.approach.distance(tile(point)) > decision.weapon.range
				|| room.obstructed.contains(decision.approach.key)))
			{
				// An incoming lane crab may not have a reachable attack tile yet.
				if (!actions.approach(approach, decision.weapon.range, world(decision.tile))) actions.walk(world(decision.tile));
			}
			else if (!point.equals(world(decision.tile)) || player.getInteracting() != null) actions.walk(world(decision.tile));
		}
		else
		{
			NPC target = npc(decision.target);
			if (target == null) return;
			int incomingStep = decision.target.lane != Lane.SPLIT && tick - decision.target.born < 9 ? 1 : 0;
			if (!decision.pathToTarget && (decision.target.distance(tile(point)) > decision.weapon.range + incomingStep
				|| !player.getWorldArea().hasLineOfSightTo(client.getTopLevelWorldView(), target.getWorldArea())))
			{
				if (!actions.prepare(decision.weapon)) { stop(actions.error()); return; }
				actions.approach(target, decision.weapon.range, world(decision.tile));
				status("Wave " + room.wave + "\nApproach: " + decision.reason);
				return;
			}
			prayers.forAttack(decision.weapon.style, tick);
			if (!actions.attack(target, decision.weapon, decision.special))
			{
				if (actions.error() != null) stop(actions.error());
				return;
			}
			pending = decision;
			requestedTick = tick;
			room.ready = tick + decision.weapon.cooldown;
		}
		status("Wave " + room.wave + (room.cleanup ? " · final cleanup" : "") + "\n" + decision.reason
			+ (actions.error() == null ? "" : "\n" + actions.error()));
	}

	private boolean isPlayerDead()
	{
		// Actor.isDead() records a past death, and the health bar may still be empty
		// after respawn. The local player's current stat is authoritative here.
		return client.getBoostedSkillLevel(Skill.HITPOINTS) <= 0;
	}

	private void confirm(int tick)
	{
		strategy.attacked(pending, tick);
		room.ready = Math.max(room.ready, tick + pending.weapon.cooldown - 1);
		pending = null;
	}

	private NPC npc(Crab crab)
	{
		for (Map.Entry<NPC, Crab> entry : tracked.entrySet()) if (entry.getValue() == crab) return entry.getKey();
		return null;
	}

	private void status(String text)
	{
		RangerPanel current = panel;
		String warning = prayers == null ? null : prayers.warning();
		String displayed = text + (warning == null ? "" : "\n" + warning);
		SwingUtilities.invokeLater(() -> { if (panel == current && current != null) current.status(displayed); });
	}

	public static Lane lane(WorldPoint point)
	{
		if (point.getX() == 3281 && (point.getY() == 4248 || point.getY() == 4249)) return Lane.WEST;
		if ((point.getX() == 3309 || point.getX() == 3310) && (point.getY() == 4248 || point.getY() == 4249)) return Lane.EAST;
		if (point.getY() == 4233 && (point.getX() == 3295 || point.getX() == 3296)) return Lane.SOUTH;
		return Lane.SPLIT;
	}

	private static boolean nearRoom(WorldPoint point)
	{
		return point.getPlane() == 0 && point.getX() >= 3280 && point.getX() <= 3311
			&& point.getY() >= 4232 && point.getY() <= 4265;
	}

	private static boolean inArena(WorldPoint point) { return nearRoom(point) && point.getY() < 4255; }
	private static boolean regular(NPC npc) { return npc.getId() >= 8342 && npc.getId() <= 8353; }
	private static boolean big(NPC npc) { return (npc.getId() - 8342) % 6 >= 3; }
	private static Style style(NPC npc) { return Style.values()[(npc.getId() - 8342) % 3]; }

	private static boolean weaponAnimation(Weapon weapon, int animation)
	{
		switch (weapon)
		{
			case BOW: return animation == 426;
			case PIPE: return animation == 5061 || animation == 10656;
			case CHIN: return animation == 7618;
			case EYE: return animation == 12397;
			case CLAWS: return animation == 393 || animation == 1067 || animation == 7514;
			default: return false;
		}
	}

	private static boolean attackAnimation(int animation)
	{
		for (Weapon weapon : Weapon.values()) if (weaponAnimation(weapon, animation)) return true;
		return animation == 1167 || animation == 1162 || animation == 1979 || animation == 8056 || animation == 9493
			|| animation == 10092 || animation == 2068 || animation == 7511 || animation == 7554;
	}

	static boolean dying(NPC npc)
	{
		return npc.isDead() || npc.getHealthRatio() == 0 || deathAnimation(npc.getAnimation());
	}

	private static boolean deathAnimation(int animation)
	{
		return animation == AnimationID.TOP_SPIDER_MAGIC_DEATH || animation == AnimationID.TOP_SPIDER_MAGIC_DEATH_DETONATE
			|| animation == AnimationID.TOP_SPIDER_RANGED_DEATH || animation == AnimationID.TOP_SPIDER_RANGED_DEATH_DETONATE
			|| animation == AnimationID.TOP_SPIDER_MELEE_DEATH || animation == AnimationID.TOP_SPIDER_MELEE_DEATH_DETONATE;
	}

	private static Tile tile(WorldPoint point) { return new Tile(point.getX(), point.getY(), point.getPlane()); }
	private static WorldPoint world(Tile point) { return new WorldPoint(point.getX(), point.getY(), point.getPlane()); }
}
