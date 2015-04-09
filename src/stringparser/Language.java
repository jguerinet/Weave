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
     * The column index of the language in the CSV file
     */
    private int columnIndex;

    /**
     * Default Constructor
     *
     * @param id   The language Id
     * @param path The language path
     */
    public Language(String id, String path){
        this.id = id;
        this.path = path;
        this.columnIndex = -1;
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

    /**
     * @return The column index of this language
     */
    public int getColumnIndex(){
        return this.columnIndex;
    }

    /* SETTERS */

    /**
     * @param columnIndex The column index of this language
     */
    public void setColumnIndex(int columnIndex){
        this.columnIndex = columnIndex;
    }
}
