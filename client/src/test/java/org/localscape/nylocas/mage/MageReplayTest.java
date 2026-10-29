package org.localscape.nylocas.mage;

import org.localscape.nylocas.mage.MageStrategy;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.localscape.nylocas.Tile;
import org.localscape.nylocas.mage.MageStrategy.Crab;
import org.localscape.nylocas.mage.MageStrategy.Lane;
import org.localscape.nylocas.mage.MageStrategy.Assignment;
import org.localscape.nylocas.mage.MageStrategy.Style;
import org.junit.Test;

import static org.junit.Assert.*;

/** Recording fixtures are retained in the repo; no network or game server is used. */
public class MageReplayTest
{
	@Test
	public void verifiesLaneFlickersAndEveryMageAssignmentAgainst62Recordings() throws Exception
	{
		Path root = Paths.get("").toAbsolutePath();
		while (root != null && !Files.exists(root.resolve("docs/research/nylocas/mage/manifest.json"))) root = root.getParent();
		assertNotNull("Run inside the LocalScape workspace containing the pinned corpus", root);
		Path corpus = root.resolve("docs/research/nylocas/mage");
		JsonObject manifest = new JsonParser().parse(Files.readString(corpus.resolve("manifest.json"))).getAsJsonObject();
		JsonArray recordings = manifest.getAsJsonArray("recordings");
		assertEquals(62, recordings.size());
		Map<String, Integer> support = new HashMap<>();
		int updates = 0;
		for (JsonElement recording : recordings)
		{
			JsonObject files = recording.getAsJsonObject().getAsJsonObject("files");
			JsonObject raid = checked(corpus, files.getAsJsonObject("raid")).getAsJsonObject();
			JsonArray events = checked(corpus, files.getAsJsonObject("events")).getAsJsonArray();
			String mage = null;
			for (JsonElement member : raid.getAsJsonArray("party"))
				if (member.getAsJsonObject().get("currentUsername").getAsString().equalsIgnoreCase("Drummie"))
					mage = member.getAsJsonObject().get("username").getAsString();
			assertNotNull(mage);
			Map<Integer, Crab> crabs = new HashMap<>();
			JsonObject npcs = raid.getAsJsonObject("tobRooms").getAsJsonObject("nylocas").getAsJsonObject("npcs");
			for (Map.Entry<String, JsonElement> entry : npcs.entrySet())
			{
				JsonObject npc = entry.getValue().getAsJsonObject();
				int id = npc.get("spawnNpcId").getAsInt();
				if (id < 8342 || id > 8353) continue;
				JsonObject nylo = npc.getAsJsonObject("nylo");
				JsonObject location = npc.getAsJsonObject("spawnPoint");
				int x = location.get("x").getAsInt(), y = location.get("y").getAsInt();
				int type = nylo.get("spawnType").getAsInt();
				Lane lane = type == 1 ? Lane.WEST : type == 2 ? Lane.SOUTH : type == 3 ? Lane.EAST : Lane.SPLIT;
				char slot = lane == Lane.SOUTH ? (x == 3295 ? 'W' : 'E') : (y == 4249 ? 'N' : 'S');
				int rid = Integer.parseInt(entry.getKey());
				crabs.put(rid, new Crab(rid, rid, npc.get("spawnTick").getAsInt(), nylo.get("wave").getAsInt(),
					lane, slot, nylo.get("big").getAsBoolean(), Style.values()[(id - 8342) % 3], new Tile(x, y, 0)));
			}
			for (JsonElement raw : events)
			{
				JsonObject event = raw.getAsJsonObject();
				int type = event.get("type").getAsInt(), tick = event.get("tick").getAsInt();
				if (type == 7 || type == 8)
				{
					JsonObject npc = event.getAsJsonObject("npc");
					Crab crab = crabs.get(npc.get("roomId").getAsInt());
					if (crab == null || crab.lane == Lane.SPLIT) continue;
					// Dying bodies remain in cap occupancy but their style stops changing.
					if (npc.get("hitpoints").getAsInt() >>> 16 == 0) continue;
					Style actual = Style.values()[(npc.get("id").getAsInt() - 8342) % 3];
					assertEquals(raid.get("uuid") + " wave " + crab.wave + " " + crab.lane + crab.slot
						+ " age " + (tick - crab.born), actual.ordinal(), org.localscape.nylocas.NylocasFlickers.at(crab.wave, crab.lane.ordinal(), crab.slot, crab.style.ordinal(), tick - crab.born));
					updates++;
				}
				if (type != 5 || !event.getAsJsonObject("player").get("name").getAsString().equals(mage)) continue;
				JsonObject attack = event.getAsJsonObject("attack");
				if (!attack.has("target") || !attack.has("weapon")) continue;
				Crab crab = crabs.get(attack.getAsJsonObject("target").get("roomId").getAsInt());
				if (crab == null) continue;
				// Recorded launch happens one tick after the decision/request.
				int age = tick - crab.born - 1;
				for (Assignment rule : MageStrategy.ASSIGNMENTS)
				{
					if (rule.matches(crab) && matches(rule.weapon, attack.get("type").getAsInt()) && age >= rule.first && age <= rule.last) support.merge(rule.name, 1, Integer::sum);
				}
			}
		}
		assertTrue("Substantial direct NPC observations were checked", updates > 100000);
		for (Assignment rule : MageStrategy.ASSIGNMENTS) assertTrue("No replay supports packet: " + rule.name, support.getOrDefault(rule.name, 0) > 0);
	}

	@Test
	public void recordedFastestRoomFramesDriveAllCriticalMagePackets() throws Exception
	{
		Path root = Paths.get("").toAbsolutePath();
		while (root != null && !Files.exists(root.resolve("docs/research/nylocas/mage/manifest.json"))) root = root.getParent();
		assertNotNull(root);
		Path corpus = root.resolve("docs/research/nylocas/mage");
		JsonObject manifest = new JsonParser().parse(Files.readString(corpus.resolve("manifest.json"))).getAsJsonObject();
		JsonObject recording = manifest.getAsJsonArray("recordings").get(0).getAsJsonObject();
		assertEquals("0c25e13d-045e-4362-a7e3-e4cf83f229a8", recording.get("uuid").getAsString());
		JsonObject files = recording.getAsJsonObject("files");
		JsonObject raid = checked(corpus, files.getAsJsonObject("raid")).getAsJsonObject();
		JsonArray events = checked(corpus, files.getAsJsonObject("events")).getAsJsonArray();
		// Supply an off-cooldown mage at each policy's staging tile. Other NPC
		// positions, colors, HP, birth ages, deaths and wave markers are raw facts.
		assertPacket(raid, events, 91, 3301, 4249, "12 east scythe");
		assertPacket(raid, events, 96, 3293, 4249, "12 west Dinh");
		assertPacket(raid, events, 172, 3290, 4249, "21 west barrage");
		assertPacket(raid, events, 177, 3292, 4252, "21 north sulphur");
		assertPacket(raid, events, 189, 3295, 4244, "22 south Dinh");
		assertPacket(raid, events, 236, 3293, 4245, "26 Dinh on 28");
		MageStrategy.Room after29 = frame(raid, events, 240, 3295, 4243);
		assertNull(new MageStrategy().decide(after29, 240).target);
	}

	private void assertPacket(JsonObject raid, JsonArray events, int tick, int x, int y, String name)
	{
		MageStrategy.Decision decision = new MageStrategy().decide(frame(raid, events, tick, x, y), tick);
		assertNotNull(name, decision.target);
		assertEquals(name, decision.rule);
		if (name.equals("12 east scythe")) assertEquals('S', decision.target.slot);
	}

	private MageStrategy.Room frame(JsonObject raid, JsonArray events, int tick, int x, int y)
	{
		MageStrategy.Room room = new MageStrategy.Room();
		room.player = new Tile(x, y, 0); room.specialEnergy = 1000;
		Map<Integer, JsonObject> states = new HashMap<>();
		for (JsonElement raw : events)
		{
			JsonObject e = raw.getAsJsonObject();
			if (e.get("tick").getAsInt() > tick) continue;
			int type = e.get("type").getAsInt();
			if (type == 120)
			{
				int wave = e.getAsJsonObject("nyloWave").get("wave").getAsInt();
				room.wave = Math.max(room.wave, wave); room.spawns[wave] = e.get("tick").getAsInt();
			}
			if (type == 7 || type == 8)
			{
				int id = e.getAsJsonObject("npc").get("roomId").getAsInt();
				JsonObject before = states.get(id);
				if (before == null || before.get("tick").getAsInt() <= e.get("tick").getAsInt()) states.put(id, e);
			}
		}
		JsonObject npcs = raid.getAsJsonObject("tobRooms").getAsJsonObject("nylocas").getAsJsonObject("npcs");
		for (Map.Entry<String, JsonElement> entry : npcs.entrySet())
		{
			JsonObject npc = entry.getValue().getAsJsonObject();
			int initial = npc.get("spawnNpcId").getAsInt();
			if (initial < 8342 || initial > 8353 || npc.get("spawnTick").getAsInt() > tick
				|| npc.has("deathTick") && npc.get("deathTick").getAsInt() <= tick) continue;
			int id = Integer.parseInt(entry.getKey());
			JsonObject state = states.get(id), nylo = npc.getAsJsonObject("nylo"), point = npc.getAsJsonObject("spawnPoint");
			int type = nylo.get("spawnType").getAsInt();
			Lane lane = type == 1 ? Lane.WEST : type == 2 ? Lane.SOUTH : type == 3 ? Lane.EAST : Lane.SPLIT;
			char slot = lane == Lane.SOUTH ? (point.get("x").getAsInt() == 3295 ? 'W' : 'E') : (point.get("y").getAsInt() == 4249 ? 'N' : 'S');
			JsonObject live = state.getAsJsonObject("npc");
			int nativeId = live.get("id").getAsInt();
			Crab crab = new Crab(id, id, npc.get("spawnTick").getAsInt(), nylo.get("wave").getAsInt(), lane, slot,
				nylo.get("big").getAsBoolean(), Style.values()[(nativeId - 8342) % 3],
				new Tile(state.get("xCoord").getAsInt(), state.get("yCoord").getAsInt(), 0));
			crab.hp = live.get("hitpoints").getAsInt() >>> 16; crab.dying = crab.hp == 0; crab.fighting = nativeId >= 8348;
			room.crabs.add(crab);
		}
		return room;
	}

	private static boolean matches(MageStrategy.Weapon weapon, int attack)
	{
		switch (weapon)
		{
			case EYE: return attack == 104;
			case SHADOW: return attack == 40;
			case SCYTHE: return attack == 38;
			case SULPHUR: return attack == 101;
			case BARRAGE: return attack == 1 || attack == 37;
			default: return false;
		}
	}

	private JsonElement checked(Path corpus, JsonObject entry) throws Exception
	{
		byte[] compressed = Files.readAllBytes(corpus.resolve(entry.get("path").getAsString()));
		byte[] raw;
		try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(compressed))) { raw = input.readAllBytes(); }
		StringBuilder hash = new StringBuilder();
		for (byte value : MessageDigest.getInstance("SHA-256").digest(raw)) hash.append(String.format("%02x", value & 255));
		assertEquals(entry.get("sha256").getAsString(), hash.toString());
		return new JsonParser().parse(new String(raw, StandardCharsets.UTF_8));
	}
}
