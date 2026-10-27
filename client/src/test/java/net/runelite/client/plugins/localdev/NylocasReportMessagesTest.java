package net.runelite.client.plugins.localdev;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

public class NylocasReportMessagesTest
{
	private static final String ROOM = "62105b94-b7ee-47ba-bc30-b43e2586783b";
	private static final String REPORT = "LNW4 " + ROOM + " waves " + String.join(" ", Collections.nCopies(38, "0")) + " 0";

	private List<String> fragments(String report, long sequence)
	{
		List<String> result = new ArrayList<>();
		int count = (report.length() + 179) / 180;
		for (int index = 0; index < count; index++)
			result.add("LNF1 " + ROOM + " " + sequence + " " + index + " " + count + " "
				+ report.substring(index * 180, Math.min(report.length(), (index + 1) * 180)));
		return result;
	}

	@Test
	public void reassemblesFiveMemberSizedReportsOutOfOrderAndOnlyOnce()
	{
		String report = REPORT + String.join("", Collections.nCopies(20, " 1234567890123456789"));
		List<String> parts = fragments(report, 9);
		NylocasReportMessages messages = new NylocasReportMessages();
		for (int i = parts.size() - 1; i > 0; i--)
		{
			assertNull(messages.accept(parts.get(i), 0));
			assertNull(messages.accept(parts.get(i), 0));
		}
		assertEquals(report, messages.accept(parts.get(0), 0));
		for (String part : parts) assertNull(messages.accept(part, 1));
	}

	@Test
	public void neverCombinesDifferentSnapshotSequences()
	{
		String report = REPORT + String.join("", Collections.nCopies(40, " 0"));
		List<String> old = fragments(report, 1);
		List<String> next = fragments(report, 2);
		NylocasReportMessages messages = new NylocasReportMessages();
		assertNull(messages.accept(old.get(0), 0));
		assertNull(messages.accept(next.get(0), 0));
		assertNull(messages.accept(old.get(1), 0));
		assertEquals(report, messages.accept(next.get(1), 0));
	}

	@Test
	public void conflictingDuplicatesAndMissingPartsCannotReplaceTheReport()
	{
		List<String> parts = fragments(REPORT + String.join("", Collections.nCopies(40, " 0")), 1);
		NylocasReportMessages messages = new NylocasReportMessages();
		assertNull(messages.accept(parts.get(0), 0));
		String conflicting = parts.get(0).substring(0, parts.get(0).length() - 1) + "x";
		assertNull(messages.accept(conflicting, 0));
		assertNull(messages.accept(parts.get(1), 0));
		messages.reset();
		assertNull(messages.accept(parts.get(0), 0));
		messages.expire(10);
		assertNull(messages.accept(parts.get(1), 10));
	}

	@Test
	public void laterShortSnapshotsDiscardIncompleteLongOnes()
	{
		List<String> parts = fragments(REPORT + String.join("", Collections.nCopies(40, " 0")), 1);
		NylocasReportMessages messages = new NylocasReportMessages();
		assertNull(messages.accept(parts.get(0), 0));
		assertEquals(REPORT, messages.accept(REPORT, 0));
		assertNull(messages.accept(parts.get(1), 0));
		assertEquals(REPORT, messages.accept(fragments(REPORT, 2).get(0), 0));
	}

	@Test
	public void rejectsOversizedOrMalformedEnvelopesAndRoomMismatch()
	{
		NylocasReportMessages messages = new NylocasReportMessages();
		for (String suffix : new String[]{"0 0 2 x", "1 -1 2 x", "1 0 33 x", "x 0 1 x", "1 2 2 x", "1 0 1"})
			assertNull(messages.accept("LNF1 " + ROOM + " " + suffix, 0));
		assertNull(messages.accept("LNF1 invalid 1 0 1 x", 0));
		assertNull(messages.accept("LNF1 " + ROOM + " 1 0 1 " + String.join("", Collections.nCopies(300, "x")), 0));
		assertNull(messages.accept("LNF1 " + ROOM + " 2 0 1 LNW4 6a24f9d1-7ae2-4eb3-b916-f590ca2e4da2 waves", 0));
	}

	@Test
	public void resetDoesNotRetainPartsAcrossClientSessions()
	{
		String report = REPORT + String.join("", Collections.nCopies(40, " 0"));
		List<String> parts = fragments(report, 1);
		NylocasReportMessages messages = new NylocasReportMessages();
		assertNull(messages.accept(parts.get(0), 0));
		messages.reset();
		assertNull(messages.accept(parts.get(1), 1));
		assertEquals(report, messages.accept(parts.get(0), 1));
	}
}
