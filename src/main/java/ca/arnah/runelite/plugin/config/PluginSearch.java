package ca.arnah.runelite.plugin.config;

import com.google.common.base.Splitter;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PluginSearch {
    private static final Splitter SPLITTER = Splitter.on(" ").trimResults().omitEmptyStrings();

    public static <T extends SearchablePlugin> List<T> search(Collection<T> searchablePlugins, String query)
    {
        return searchablePlugins.stream()
                .filter(plugin -> Text.matchesSearchTerms(SPLITTER.split(query.toLowerCase()), plugin.getKeywords()))
                .sorted(comparator(query))
                .collect(Collectors.toList());
    }

    private static Comparator<SearchablePlugin> comparator(String query) {
        if (StringUtils.isBlank(query)) {
            return Comparator.comparing(SearchablePlugin::isPinned, Comparator.reverseOrder())
                    .thenComparing(SearchablePlugin::getSearchableName);
        }

        Iterable<String> queryPieces = SPLITTER.split(query.toLowerCase());
        return Comparator.comparing(SearchablePlugin::isPinned)
                .thenComparing(sp -> query.equalsIgnoreCase(sp.getSearchableName()))
                // any piece of the search string starting with any part of the plugin name
                .thenComparing(sp -> stream(queryPieces).anyMatch(queryPiece -> stream(SPLITTER.split(sp.getSearchableName().toLowerCase())).anyMatch(namePiece -> namePiece.startsWith(queryPiece))))
                // each piece of the search string in one part of the plugin name
                .thenComparing(sp -> stream(queryPieces).allMatch(queryPiece -> stream(SPLITTER.split(sp.getSearchableName().toLowerCase())).anyMatch(namePiece -> namePiece.contains(queryPiece))))
                .thenComparingInt(SearchablePlugin::installs)
                .reversed()
                .thenComparing(SearchablePlugin::getSearchableName);
    }

    private static Stream<String> stream(Iterable<String> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}