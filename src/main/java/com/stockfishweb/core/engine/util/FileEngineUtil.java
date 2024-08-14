package com.stockfishweb.core.engine.util;

import com.stockfishweb.core.engine.enums.Variant;
import com.stockfishweb.core.engine.exception.StockfishEngineException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileEngineUtil {
    private static final Log log = LogFactory.getLog(FileEngineUtil.class);

    public static final String ASSETS_LOCATION = "assets/engines/";
    public static final String ENGINE_FILE_NAME_PREFIX = "stockfish_";
    public static final String ENGINE_FILE_NAME_SUFFIX = "_x64";
    public static final String FILE_MASK = ENGINE_FILE_NAME_PREFIX + "??" + ENGINE_FILE_NAME_SUFFIX + "*";

    public static Set<Integer> SUPPORTED_VERSIONS = new TreeSet<>(Comparator.reverseOrder());

    static {
        try {
            try (DirectoryStream<Path> assetsDir = Files.newDirectoryStream(
                    Paths.get(ASSETS_LOCATION), FILE_MASK)) {
                Pattern pattern = Pattern.compile("[1-9][0-9]");
                log.debug("Supported engines:");
                for (Path executable : assetsDir) {
                    log.debug(executable.toString());

                    String entry = executable.toString();
                    Matcher matcher = pattern.matcher(entry);
                    if (matcher.find()) {
                        SUPPORTED_VERSIONS.add(Integer.parseInt(matcher.group(0)));
                    }
                }
            }
            if (SUPPORTED_VERSIONS.isEmpty()) {
                log.fatal("No engines found.");
                throw new StockfishEngineException("No engines found.");
            }
        } catch (IOException e) {
            log.fatal("Error accessing assets location.", e);
            throw new StockfishEngineException("Error accessing assets location.", e);
        }
    }

    public static String getPath(Variant variant, String override) {
        return getPath(variant, override, null);
    }

    public static String getPath(Variant variant, String override, Integer requestedEngineVersion) {
        int engineVersion;
        if (requestedEngineVersion != null) {
            if (SUPPORTED_VERSIONS.contains(requestedEngineVersion)) {
                engineVersion = requestedEngineVersion;
            } else {
                engineVersion = SUPPORTED_VERSIONS.iterator().next();
                log.info("Version " + requestedEngineVersion + " not found. Defaulting to highest available: " + engineVersion);
            }
        } else {
            engineVersion = SUPPORTED_VERSIONS.iterator().next();
            log.info("No version was specified. Defaulting to highest available: " + engineVersion);
        }

        StringBuilder path = new StringBuilder(override == null || "".equals(override.trim()) ?
                ASSETS_LOCATION + ENGINE_FILE_NAME_PREFIX + engineVersion + ENGINE_FILE_NAME_SUFFIX :
                override +  ENGINE_FILE_NAME_PREFIX + engineVersion + ENGINE_FILE_NAME_SUFFIX);

        if (System.getProperty("os.name").toLowerCase().contains("win"))
            switch (variant) {
                case DEFAULT:
                    path.append(".exe");
                    break;
                case BMI2:
                    path.append("_bmi2.exe");
                    break;
                case POPCNT:
                    path.append("_popcnt.exe");
                    break;
                default:
                    throw new StockfishEngineException("Illegal variant provided.");
            }
        else
            switch (variant) {
                case DEFAULT:
                    break;
                case BMI2:
                    path.append("_bmi2");
                    break;
                case MODERN:
                    path.append("_modern");
                    break;
                default:
                    throw new StockfishEngineException("Illegal variant provided.");
            }

        return path.toString();
    }
}
