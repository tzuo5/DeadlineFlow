package com.deadlineflow.presentation.viewmodel;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class LanguageManager {
    private static final String BUNDLE_BASE = "com.deadlineflow.i18n.messages";
    private static final Locale APP_LOCALE = Locale.ENGLISH;
    private static final ResourceBundle.Control UTF8_CONTROL = new Utf8Control();
    private final ResourceBundle bundle = loadBundle();

    public String t(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException ex) {
            return key;
        }
    }

    public Locale locale() {
        return APP_LOCALE;
    }

    private ResourceBundle loadBundle() {
        return ResourceBundle.getBundle(BUNDLE_BASE, APP_LOCALE, UTF8_CONTROL);
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
