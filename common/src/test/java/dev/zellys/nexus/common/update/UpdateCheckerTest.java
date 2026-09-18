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

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;

class UpdateCheckerTest {

    private static UpdateChecker.TagSource fixed(String... tags) {
        return () -> List.of(tags);
    }

    @Test
    void stableNewerNotifies() {
        UpdateChecker.UpdateResult result =
                UpdateChecker.check("0.1.0-alpha", "VictorGugug/Zelly-s-Nexus", fixed("0.2.0", "0.1.0-alpha"));
        assertEquals(UpdateChecker.Status.UPDATE_AVAILABLE, result.status());
        assertEquals("0.2.0", result.latestVersion());
    }

    @Test
    void runningAlphaSeesStable() {
        UpdateChecker.UpdateResult result =
                UpdateChecker.check("0.1.0-alpha", "VictorGugug/Zelly-s-Nexus", fixed("0.1.0", "0.0.9"));
        assertEquals(UpdateChecker.Status.UPDATE_AVAILABLE, result.status());
        assertEquals("0.1.0", result.latestVersion());
    }

    @Test
    void preReleaseOnlyNotifiesPreReleaseRuns() {
        UpdateChecker.UpdateResult result =
                UpdateChecker.check("0.1.0-alpha", "VictorGugug/Zelly-s-Nexus", fixed("0.1.1-alpha"));
        assertEquals(UpdateChecker.Status.UPDATE_AVAILABLE, result.status());
        assertEquals("0.1.1-alpha", result.latestVersion());
    }

    @Test
    void stableRunIgnoresPreRelease() {
        UpdateChecker.UpdateResult result =
                UpdateChecker.check("0.1.0", "VictorGugug/Zelly-s-Nexus", fixed("0.1.1-alpha"));
        assertEquals(UpdateChecker.Status.UP_TO_DATE, result.status());
    }

    @Test
    void upToDateWhenNothingNewer() {
        UpdateChecker.UpdateResult result =
                UpdateChecker.check("0.2.0", "VictorGugug/Zelly-s-Nexus", fixed("0.2.0", "0.1.0"));
        assertEquals(UpdateChecker.Status.UP_TO_DATE, result.status());
    }

    @Test
    void sourceFailureReportsCheckFailed() {
        UpdateChecker.TagSource broken = () -> {
            throw new java.io.IOException("offline");
        };
        UpdateChecker.UpdateResult result =
                UpdateChecker.check("0.1.0-alpha", "VictorGugug/Zelly-s-Nexus", broken);
        assertEquals(UpdateChecker.Status.CHECK_FAILED, result.status());
    }

    @Test
    void badTagsAreIgnored() {
        UpdateChecker.UpdateResult result =
                UpdateChecker.check("0.1.0-alpha", "VictorGugug/Zelly-s-Nexus", fixed("hello", "0.2.0"));
        assertEquals(UpdateChecker.Status.UPDATE_AVAILABLE, result.status());
        assertEquals("0.2.0", result.latestVersion());
    }
}
