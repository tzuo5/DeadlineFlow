package com.deadlineflow.presentation.theme;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;

import java.util.List;
import java.util.prefs.Preferences;

public class ThemeManager {
    public enum ThemeMode {
        LIGHT,
        DARK,
        SYSTEM
    }

    private static final String PREF_KEY_THEME_MODE = "ui_theme_mode";
    private static final String BASE_CSS = "/com/deadlineflow/presentation/styles/base.css";
    private static final String LIGHT_CSS = "/com/deadlineflow/presentation/styles/light.css";
    private static final String DARK_CSS = "/com/deadlineflow/presentation/styles/dark.css";

    private final Preferences preferences = Preferences.userNodeForPackage(ThemeManager.class);
    private final ObjectProperty<ThemeMode> themeMode = new SimpleObjectProperty<>(loadThemeMode());
    private final ObjectProperty<ThemeMode> effectiveTheme = new SimpleObjectProperty<>(resolveEffectiveTheme(themeMode.get()));

    private Scene attachedScene;

    public ThemeManager() {
        themeMode.addListener((obs, oldValue, newValue) -> {
            preferences.put(PREF_KEY_THEME_MODE, newValue.name());
            ThemeMode effective = resolveEffectiveTheme(newValue);
            effectiveTheme.set(effective);
            if (attachedScene != null) {
                applyToScene(attachedScene, effective);
            }
        });
    }

    public ObjectProperty<ThemeMode> themeModeProperty() {
        return themeMode;
    }

    public ThemeMode themeMode() {
        return themeMode.get();
    }

    public void setThemeMode(ThemeMode mode) {
        if (mode != null) {
            themeMode.set(mode);
        }
    }

    public ObjectProperty<ThemeMode> effectiveThemeProperty() {
        return effectiveTheme;
    }

    public ThemeMode effectiveTheme() {
        return effectiveTheme.get();
    }

    public void apply(Scene scene) {
        if (scene == null) {
            return;
        }
        attachedScene = scene;
        applyToScene(scene, effectiveTheme.get());
    }

    private void applyToScene(Scene scene, ThemeMode effective) {
        String base = getClass().getResource(BASE_CSS).toExternalForm();
        String light = getClass().getResource(LIGHT_CSS).toExternalForm();
        String dark = getClass().getResource(DARK_CSS).toExternalForm();

        List<String> stylesheets = scene.getStylesheets();
        stylesheets.removeIf(sheet -> sheet.equals(base) || sheet.equals(light) || sheet.equals(dark));
        stylesheets.add(base);
        stylesheets.add(effective == ThemeMode.DARK ? dark : light);

        scene.getRoot().pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("dark"), effective == ThemeMode.DARK);
    }

    private ThemeMode resolveEffectiveTheme(ThemeMode mode) {
        if (mode == ThemeMode.SYSTEM) {
            return systemPrefersDark() ? ThemeMode.DARK : ThemeMode.LIGHT;
        }
        return mode;
    }

    private ThemeMode loadThemeMode() {
        String raw = preferences.get(PREF_KEY_THEME_MODE, ThemeMode.SYSTEM.name());
        try {
            return ThemeMode.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return ThemeMode.SYSTEM;
        }
    }

    private boolean systemPrefersDark() {
        String macAppearance = System.getProperty("apple.awt.application.appearance", "");
        if (macAppearance == null || macAppearance.isBlank()) {
            // Follow System fallback: if OS theme cannot be detected, default to dark.
            return true;
        }
        return macAppearance.toLowerCase().contains("dark");
    }
}
