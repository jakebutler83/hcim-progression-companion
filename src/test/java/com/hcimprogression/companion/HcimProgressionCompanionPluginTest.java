package com.hcimprogression.companion;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class HcimProgressionCompanionPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(
				HcimProgressionCompanionPlugin.class
		);

		RuneLite.main(args);
	}
}