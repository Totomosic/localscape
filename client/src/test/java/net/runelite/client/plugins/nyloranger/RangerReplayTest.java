package net.runelite.client.plugins.nyloranger;

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
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Crab;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Lane;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Rule;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Style;
import org.junit.Test;

import static org.junit.Assert.*;

/** Recording fixtures are retained in the repo; no network or game server is used. */
public class RangerReplayTest
{
	@Test
	public void verifiesAllLaneFlickersAndEveryScheduledPacketAgainstThe93Recordings() throws Exception
	{
		Path root = Paths.get("").toAbsolutePath();
		while (root != null && !Files.exists(root.resolve("docs/research/nylocas/ranger/manifest.json"))) root = root.getParent();
		assertNotNull("Run inside the LocalScape workspace containing the pinned corpus", root);
		Path corpus = root.resolve("docs/research/nylocas/ranger");
		JsonObject manifest = new JsonParser().parse(Files.readString(corpus.resolve("manifest.json"))).getAsJsonObject();
		JsonArray recordings = manifest.getAsJsonArray("recordings");
		assertEquals(93, recordings.size());
		Map<String, Integer> support = new HashMap<>();
		int updates = 0;
		for (JsonElement recording : recordings)
		{
			JsonObject files = recording.getAsJsonObject().getAsJsonObject("files");
			JsonObject raid = checked(corpus, files.getAsJsonObject("raid")).getAsJsonObject();
			JsonArray events = checked(corpus, files.getAsJsonObject("events")).getAsJsonArray();
			String ranger = null;
			for (JsonElement member : raid.getAsJsonArray("party"))
				if (member.getAsJsonObject().get("currentUsername").getAsString().equalsIgnoreCase("cutely meows"))
					ranger = member.getAsJsonObject().get("username").getAsString();
			assertNotNull(ranger);
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
					lane, slot, nylo.get("big").getAsBoolean(), Style.values()[(id - 8342) % 3], new WorldPoint(x, y, 0)));
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
						+ " age " + (tick - crab.born), actual, RangerFlickers.at(crab, tick - crab.born));
					updates++;
				}
				if (type != 5 || !event.getAsJsonObject("player").get("name").getAsString().equals(ranger)) continue;
				JsonObject attack = event.getAsJsonObject("attack");
				if (!attack.has("target") || !attack.has("weapon")) continue;
				Crab crab = crabs.get(attack.getAsJsonObject("target").get("roomId").getAsInt());
				if (crab == null) continue;
				int weapon = attack.getAsJsonObject("weapon").get("id").getAsInt();
				// Recorded launch happens one tick after the decision/request.
				int age = tick - crab.born - 1;
				for (Rule rule : RangerStrategy.RULES)
				{
					if (rule.matches(crab) && weapon == rule.weapon.itemId && age >= rule.first && age <= rule.last
						&& (!rule.special || attack.get("type").getAsInt() == 20)) support.merge(rule.name, 1, Integer::sum);
				}
			}
		}
		assertTrue("Substantial direct NPC observations were checked", updates > 100000);
		for (Rule rule : RangerStrategy.RULES) assertTrue("No replay supports packet: " + rule.name, support.getOrDefault(rule.name, 0) > 0);
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
