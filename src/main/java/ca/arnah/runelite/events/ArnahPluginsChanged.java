package ca.arnah.runelite.events;

import ca.arnah.runelite.plugin.ArnahPluginManifest;
import lombok.Value;

import java.util.List;

@Value
public class ArnahPluginsChanged {
	List<ArnahPluginManifest> loadedManifest;
}