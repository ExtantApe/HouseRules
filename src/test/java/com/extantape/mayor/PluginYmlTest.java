package com.extantape.mayor;

import com.extantape.mayor.rules.Reach;
import com.extantape.mayor.rules.WordFilter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginYmlTest {
    private static YamlConfiguration pluginYml() throws Exception {
        YamlConfiguration yml = new YamlConfiguration();
        // Permission names contain dots, so use another path separator.
        yml.options().pathSeparator('/');
        try (var in = PluginYmlTest.class.getResourceAsStream("/paper-plugin.yml")) {
            yml.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return yml;
    }

    @Test
    void bypassesAreOffForEveryoneOpsIncluded() throws Exception {
        YamlConfiguration yml = pluginYml();
        for (String permission : new String[] {Reach.BYPASS, WordFilter.BYPASS}) {
            assertEquals("false", String.valueOf(yml.get("permissions/" + permission + "/default")), permission);
        }
    }
}
