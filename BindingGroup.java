package org.panther.easybindings;

import java.util.*;
import javafx.beans.value.*;

/**
 * The {@code BindingGroup} class can be used to create and manage lots of
 * bindings.
 *
 * @author henri
 */
public class BindingGroup {
    private LinkedList<EasyBinding> bindings;

    /**
     * Create a new empty binding group
     */
    public BindingGroup() {
        bindings = new LinkedList<>();
    }

    /**
     * Get all bindings contained by the group
     *
     * @return a copy of the internal bindings list
     */
    public List<EasyBinding> getBindings() {
        return new LinkedList<>(bindings);
    }

    /**
     * Create a new binding and add it to the group
     *
     * @param destinationObject See {@code EasyBinding} constructor for more information.
     * @param destinationKeyPath See {@code EasyBinding} constructor for more information.
     * @param sourceObject See {@code EasyBinding} constructor for more information.
     * @param sourceKeyPath See {@code EasyBinding} constructor for more information.
     * @return the newly created binding
     */
    public EasyBinding bind(ObservableValue destinationObject, String destinationKeyPath, ObservableValue sourceObject, String sourceKeyPath) {
        EasyBinding b = new EasyBinding(destinationObject, destinationKeyPath, sourceObject, sourceKeyPath);
        b.bind();
        bindings.add(b);

        return b;
    }

    /**
     * Unbind all bindings managed by this class
     */
    public void unbindAll() {
        bindings.forEach((b) -> {
            b.unbind();
        });
        bindings.clear();
    }
}
