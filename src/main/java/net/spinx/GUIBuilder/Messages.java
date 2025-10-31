package net.spinx.GUIBuilder;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class Messages {

    public enum Language {
        ENGLISH("en", Locale.ENGLISH),
        GERMAN("de", Locale.GERMAN);

        private final String code;
        private final Locale locale;

        Language(String code, Locale locale) {
            this.code = code;
            this.locale = locale;
        }

        public Locale locale() {
            return locale;
        }

        public static Language fromCode(String code) {
            if (code == null) {
                return null;
            }
            String normalized = code.trim().toLowerCase(Locale.ROOT);
            for (Language language : values()) {
                if (language.code.equals(normalized) || language.name().equalsIgnoreCase(normalized)) {
                    return language;
                }
            }
            return null;
        }
    }

    private final Language language;
    private final ResourceBundle bundle;

    private Messages(Language language, ResourceBundle bundle) {
        this.language = language;
        this.bundle = bundle;
    }

    public static Messages load(String code, Logger logger) {
        Language language = Language.fromCode(code);
        if (language == null) {
            if (code != null && logger != null) {
                logger.warning("Unknown language '" + code + "'. Falling back to English.");
            }
            language = Language.ENGLISH;
        }

        ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle("messages", language.locale());
        } catch (MissingResourceException ex) {
            if (logger != null) {
                logger.warning("Could not load language bundle for '" + language + "'. Falling back to English.");
            }
            language = Language.ENGLISH;
            bundle = ResourceBundle.getBundle("messages", language.locale());
        }
        return new Messages(language, bundle);
    }

    public Language language() {
        return language;
    }

    public String get(String key) {
        return bundle.getString(key);
    }

    public String format(String key, Object... args) {
        MessageFormat format = new MessageFormat(get(key), bundle.getLocale());
        return format.format(args);
    }
}
