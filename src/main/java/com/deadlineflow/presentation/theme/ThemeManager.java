package com.deadlineflow.presentation.theme;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
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
    private static final String BASE_CSS_URL = requireStylesheet(BASE_CSS);
    private static final String LIGHT_CSS_URL = requireStylesheet(LIGHT_CSS);
    private static final String DARK_CSS_URL = requireStylesheet(DARK_CSS);
    private static final PseudoClass DARK_PSEUDO_CLASS = PseudoClass.getPseudoClass("dark");

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
        List<String> stylesheets = scene.getStylesheets();
        if (!stylesheets.contains(BASE_CSS_URL)) {
            stylesheets.add(BASE_CSS_URL);
        }
        if (effective == ThemeMode.DARK) {
            stylesheets.remove(LIGHT_CSS_URL);
            if (!stylesheets.contains(DARK_CSS_URL)) {
                stylesheets.add(DARK_CSS_URL);
            }
        } else {
            stylesheets.remove(DARK_CSS_URL);
            if (!stylesheets.contains(LIGHT_CSS_URL)) {
                stylesheets.add(LIGHT_CSS_URL);
            }
        }

        scene.getRoot().pseudoClassStateChanged(DARK_PSEUDO_CLASS, effective == ThemeMode.DARK);
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

    private static String requireStylesheet(String path) {
        var resource = ThemeManager.class.getResource(path);
        if (resource == null) {
            throw new IllegalStateException("Missing stylesheet resource: " + path);
        }
        return resource.toExternalForm();
    }
}
