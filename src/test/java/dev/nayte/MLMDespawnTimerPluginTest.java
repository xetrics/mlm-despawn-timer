package dev.nayte;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class MLMDespawnTimerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MLMDespawnTimerPlugin.class);
		RuneLite.main(args);
	}
}