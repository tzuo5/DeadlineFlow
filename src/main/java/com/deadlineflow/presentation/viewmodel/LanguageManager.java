package com.deadlineflow.presentation.viewmodel;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class LanguageManager {
    public enum Language {
        ENGLISH("English", Locale.ENGLISH),
        CHINESE("中文", Locale.SIMPLIFIED_CHINESE);

        private final String displayName;
        private final Locale locale;

        Language(String displayName, Locale locale) {
            this.displayName = displayName;
            this.locale = locale;
        }

        public Locale locale() {
            return locale;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final String PREF_KEY_LANGUAGE = "ui_language";
    private static final String BUNDLE_BASE = "com.deadlineflow.i18n.messages";
    private static final ResourceBundle.Control UTF8_CONTROL = new Utf8Control();

    private final Preferences preferences = Preferences.userNodeForPackage(LanguageManager.class);
    private final ObjectProperty<Language> language = new SimpleObjectProperty<>(loadLanguagePreference());

    private ResourceBundle bundle = loadBundle(language.get());

    public LanguageManager() {
        language.addListener((obs, oldValue, newValue) -> {
            preferences.put(PREF_KEY_LANGUAGE, newValue.name());
            bundle = loadBundle(newValue);
        });
    }

    public ObjectProperty<Language> languageProperty() {
        return language;
    }

    public Language language() {
        return language.get();
    }

    public void setLanguage(Language newLanguage) {
        if (newLanguage != null) {
            language.set(newLanguage);
        }
    }

    public String t(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException ex) {
            return key;
        }
    }

    private Language loadLanguagePreference() {
        String saved = preferences.get(PREF_KEY_LANGUAGE, Language.ENGLISH.name());
        try {
            return Language.valueOf(saved);
        } catch (IllegalArgumentException ex) {
            return Language.ENGLISH;
        }
    }

    private ResourceBundle loadBundle(Language language) {
        Locale locale = language == null ? Locale.ENGLISH : language.locale();
        return ResourceBundle.getBundle(BUNDLE_BASE, locale, UTF8_CONTROL);
    }

    private static final class Utf8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(
                String baseName,
                Locale locale,
                String format,
                ClassLoader loader,
                boolean reload
        ) throws IllegalAccessException, InstantiationException, IOException {
            if (!"java.properties".equals(format)) {
                return super.newBundle(baseName, locale, format, loader, reload);
            }

            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");

            try (InputStream stream = loader.getResourceAsStream(resourceName)) {
                if (stream == null) {
                    return null;
                }
                return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
            }
        }
    }
}
