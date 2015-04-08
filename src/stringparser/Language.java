/*
 * Copyright (c) 2015 Julien Guerinet. All rights reserved.
 */

package stringparser;

/**
 * One language that the Strings are in
 * @author Julien Guerinet
 * @version 1.0
 * @since 1.0
 */
public class Language {
    /**
     * The language Id
     */
    private String id;
    /**
     * The language path
     */
    private String path;

    /**
     * Default Constructor
     *
     * @param id   The language Id
     * @param path The language path
     */
    public Language(String id, String path){
        this.id = id;
        this.path = path;
    }

    /* GETTERS */

    /**
     * @return The language Id
     */
    public String getId(){
        return this.id;
    }

    /**
     * @return The language path
     */
    public String getPath(){
        return this.path;
    }
}
