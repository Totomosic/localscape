package net.runelite.client.plugins.localgear;

final class GearPreset
{
	final String id;
	final String name;

	GearPreset(String id, String name)
	{
		this.id = id;
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
