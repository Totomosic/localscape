package net.runelite.client.plugins.localdev;

import java.util.Locale;

final class LocalItem
{
	final int id;
	private final String name;
	private final boolean noted;
	private final String searchName;

	LocalItem(int id, String name, boolean noted)
	{
		this.id = id;
		this.name = name;
		this.noted = noted;
		this.searchName = name.toLowerCase(Locale.ROOT);
	}

	boolean matches(String query)
	{
		return Integer.toString(id).equals(query) || searchName.contains(query);
	}

	@Override
	public String toString()
	{
		return id + ": " + name + (noted ? " (noted)" : "");
	}
}
