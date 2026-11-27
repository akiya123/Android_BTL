package com.fittrack.app.data.cloud;

import org.junit.Test;
import static com.fittrack.app.data.cloud.CloudModels.*;
import static org.junit.Assert.*;

public class SnapshotMapperTest {
    @Test public void repeatedBackupCountsIdenticalMealsAndIgnoresServerTime() {
        Data local = empty();
        Food meal = new Food();
        meal.foodName = "Rice";
        meal.loggedAt = "2026-11-27T00:00:00";
        local.foodLogs.add(meal);
        local.foodLogs.add(meal);
        Data remote = empty();
        Food existing = new Food();
        existing.foodName = "Rice";
        existing.loggedAt = "2026-11-27T12:30:00";
        remote.foodLogs.add(existing);
        Data delta = SnapshotMapper.difference(local, remote);
        assertEquals(1, delta.foodLogs.size());
        remote.foodLogs.addAll(delta.foodLogs);
        assertTrue(SnapshotMapper.difference(local, remote).foodLogs.isEmpty());
    }

    @Test public void latestLocalWeightPerDayIsSentOnce() {
        Data local = empty();
        for (float kg : new float[]{70, 69}) {
            Weight w = new Weight();
            w.recordedDate = "2026-11-27T00:00:00";
            w.weight = kg;
            local.weightEntries.add(w);
        }
        Data remote = empty();
        Data delta = SnapshotMapper.difference(local, remote);
        assertEquals(1, delta.weightEntries.size());
        assertEquals(69f, delta.weightEntries.get(0).weight, 0f);
        remote.weightEntries.addAll(delta.weightEntries);
        assertTrue(SnapshotMapper.difference(local, remote).weightEntries.isEmpty());
    }
}
