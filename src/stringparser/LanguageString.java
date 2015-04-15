/*
 * Copyright (c) 2015 Julien Guerinet. All rights reserved.
 */

package stringparser;

import java.util.HashMap;

/**
 * One String with all of the translations
 * @author Julien Guerinet
 * @version 2.1
 * @since 1.0
 */
public class LanguageString {
    /**
     * The key to store the String under
     */
    private String key;
    /**
     * A HashMap of translations, with the key being the language Id and the value being the String
     */
    private HashMap<String, String> translations;

    /**
     * Default Constructor
     *
     * @param key The String key
     */
    public LanguageString(String key){
        this.key = key;
        this.translations = new HashMap<String, String>();
    }

    /* GETTERS */

    /**
     * @return The String Key
     */
    public String getKey(){
        return this.key;
    }

    /**
     * Get the String in a given language
     *
     * @param id The language Id
     * @return The String in that language
     */
    public String getString(String id){
        return this.translations.get(id);
    }

    /* SETTERS */

    /**
     * Add a translation
     *
     * @param id     The language Id
     * @param string The String
     */
    public void addTranslation(String id, String string){
        this.translations.put(id, string);
    }
}
