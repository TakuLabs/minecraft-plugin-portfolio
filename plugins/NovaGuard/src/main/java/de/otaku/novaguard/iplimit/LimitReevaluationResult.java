package de.otaku.novaguard.iplimit;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record LimitReevaluationResult(int affectedGroups, List<UUID> disconnects, Map<UUID, Integer> limitsByUuid) {
}
