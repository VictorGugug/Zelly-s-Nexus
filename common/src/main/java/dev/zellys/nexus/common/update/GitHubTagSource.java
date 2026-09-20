/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.common.update;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GitHubTagSource implements UpdateChecker.TagSource {
    private static final Pattern TAG = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");

    private final String repository;

    public GitHubTagSource(String repository) {
        this.repository = repository;
    }

    @Override
    public List<String> tags() throws Exception {
        URI uri = URI.create("https://api.github.com/repos/" + repository + "/tags?per_page=100");
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(uri.toURL().openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line).append('\n');
            }
        }
        List<String> tags = new ArrayList<>();
        Matcher matcher = TAG.matcher(body);
        while (matcher.find()) {
            tags.add(matcher.group(1));
        }
        return tags;
    }
}
