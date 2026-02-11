package com.deadlineflow.presentation.theme;

import com.deadlineflow.domain.model.Task;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class StatusColorManager {
    private static final Color TODO = Color.web("#3B82F6");
    private static final Color IN_PROGRESS = Color.web("#7C3AED");
    private static final Color BLOCKED = Color.web("#F59E0B");
    private static final Color OVERDUE = Color.web("#EF4444");
    private static final Color DONE = Color.web("#22C55E");
    private static final Color FALLBACK = Color.web("#4F46E5");
    private static final int STATUS_CACHE_LIMIT = 128;
    private static final StatusTone OVERDUE_TONE = new StatusTone("OVERDUE", OVERDUE, Color.web("#FEE2E2"), Color.web("#FCA5A5"), Color.web("#B91C1C"));

    private final Map<String, StatusTone> toneCache = new LinkedHashMap<>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, StatusTone> eldest) {
            return size() > STATUS_CACHE_LIMIT;
        }
    };
    private final Map<StatusTone, String> chipStyleCache = new LinkedHashMap<>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<StatusTone, String> eldest) {
            return size() > STATUS_CACHE_LIMIT;
        }
    };

    public StatusTone toneForTask(Task task) {
        if (task == null) {
            return toneForStatus(Task.DEFAULT_STATUS, false);
        }
        return toneForStatus(task.status(), isOverdue(task));
    }

    public StatusTone toneForStatus(String status, boolean overdue) {
        if (overdue) {
            return OVERDUE_TONE;
        }

        String cacheKey = status == null ? "" : status.trim();
        StatusTone cached = toneCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        StatusTone resolved = resolveTone(status);
        toneCache.put(cacheKey, resolved);
        return resolved;
    }

    public String barColorHex(Task task) {
        return toHex(toneForTask(task).base());
    }

    public String chipStyle(StatusTone tone) {
        if (tone == null) {
            return "";
        }
        String cached = chipStyleCache.get(tone);
        if (cached != null) {
            return cached;
        }
        String style = "-fx-background-color: " + toRgba(tone.chipFill()) + ";"
                + "-fx-border-color: " + toRgba(tone.chipBorder()) + ";"
                + "-fx-border-width: 1;"
                + "-fx-background-radius: 999;"
                + "-fx-border-radius: 999;"
                + "-fx-text-fill: " + toHex(tone.chipText()) + ";";
        chipStyleCache.put(tone, style);
        return style;
    }

    public String displayStatus(String status) {
        String normalized = normalize(status);
        return switch (normalized) {
            case "TODO" -> "TODO";
            case "IN_PROGRESS", "INPROGRESS", "DOING" -> "IN PROGRESS";
            case "BLOCKED" -> "BLOCKED";
            case "DONE" -> "DONE";
            default -> humanize(status);
        };
    }

    public boolean isOverdue(Task task) {
        return task != null
                && task.dueDate().isBefore(LocalDate.now())
                && !"DONE".equals(normalize(task.status()));
    }

    private StatusTone resolveTone(String status) {
        String normalized = normalize(status);
        return switch (normalized) {
            case "TODO" -> new StatusTone("TODO", TODO, Color.web("#DBEAFE"), Color.web("#93C5FD"), Color.web("#1E3A8A"));
            case "IN_PROGRESS", "INPROGRESS", "DOING" ->
                    new StatusTone("IN PROGRESS", IN_PROGRESS, Color.web("#E9D5FF"), Color.web("#C4B5FD"), Color.web("#5B21B6"));
            case "BLOCKED" -> new StatusTone("BLOCKED", BLOCKED, Color.web("#FFEDD5"), Color.web("#FDBA74"), Color.web("#9A3412"));
            case "DONE" -> new StatusTone("DONE", DONE, Color.web("#DCFCE7"), Color.web("#86EFAC"), Color.web("#166534"));
            default -> new StatusTone(humanize(status), FALLBACK, Color.web("#E0E7FF"), Color.web("#A5B4FC"), Color.web("#312E81"));
        };
    }

    private String normalize(String status) {
        if (status == null || status.isBlank()) {
            return Task.DEFAULT_STATUS;
        }
        return status.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_')
                .replaceAll("_+", "_");
    }

    private String humanize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "TODO";
        }
        return raw.trim().replace('_', ' ').toUpperCase(Locale.ROOT);
    }

    private String toHex(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private String toRgba(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format(Locale.ROOT, "rgba(%d,%d,%d,%.3f)", r, g, b, color.getOpacity());
    }

    public record StatusTone(String label, Color base, Color chipFill, Color chipBorder, Color chipText) {
    }
}
