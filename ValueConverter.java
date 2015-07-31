package org.panther.easybindings;

/**
 * Used to dynamically convert values into other values.
 *
 * Your converter should work in both ways.
 *
 * @author henri
 */
public interface ValueConverter {
    /**
     * Convert an object into another object.
     *
     * @param object Object to convert
     * @return the converted value
     */
    public Object convert(Object object);
}
