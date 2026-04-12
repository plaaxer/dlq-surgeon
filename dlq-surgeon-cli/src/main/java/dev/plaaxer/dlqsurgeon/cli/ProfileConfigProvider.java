package dev.plaaxer.dlqsurgeon.cli;

import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.OptionSpec;

public class ProfileConfigProvider implements IDefaultValueProvider {

    private final ConfigFile config;

    public ProfileConfigProvider(String profileName) {
        this.config = new ConfigFile(profileName);
    }

    @Override
    public String defaultValue(ArgSpec argSpec) {
        if (!(argSpec instanceof OptionSpec opt)) return null;

        String longestName = opt.longestName(); // e.g. "--host", "--management-port"
        if (!longestName.startsWith("--")) return null;

        String key = longestName.substring(2); // strip "--"
        return config.get(key);
    }
}