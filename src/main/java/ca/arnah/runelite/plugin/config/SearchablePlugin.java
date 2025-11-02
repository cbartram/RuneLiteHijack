package ca.arnah.runelite.plugin.config;


import java.util.List;

public interface SearchablePlugin {
    String getSearchableName();
    List<String> getKeywords();
    default boolean isPinned() {
        return false;
    }
    default int installs() {
        return 0;
    }
}