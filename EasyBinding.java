package org.panther.easybindings;

import java.lang.reflect.*;
import java.util.function.*;

import javafx.beans.value.*;


/**
 * The {@code EasyBinding} class provides some key-value binding based on the
 * excellent Cocoa key-value framework.
 *
 * <p>Bindings are bidirectional. Objects participating in a value are specified
 * using a key path. All key paths must start with the prefix {@code root},
 * e.g. {@code root.house.door.color}.
 *
 * <p>Every component in the key path, including the root, must point to an
 * instance implementing the {@code ObservableValue} interface, which is used
 * for doing the actual observing. The value encapsulated by the
 * {@code ObservableValue} must have a public field with the same name as
 * the path component.
 *
 * <p>The last component of the key path must implement the {@code ObservableValue}
 * and the {@code WriteableValue} interface.
 *
 * <p>If one of the intermediate values changed to null, the other value will
 * also be set to null.
 *
 * <p>If a value changes, but the other path doesn't fully exists due to a null
 * object, no action will be taken.
 */
public class EasyBinding {
    private final ObservableValue destinationObject;
    private final String[] destinationKeyPath;
    private final ObservableValue sourceObject;
    private final String[] sourceKeyPath;

    protected Listener destinationListener;
    protected Listener sourceListener;

    /**
     * Create a new EasyBinding.
     *
     * <p>Bindings are (for now) bidirectional. So the destination/source
     * symantics don't actually mean anything.
     *
     * <p>See the class description for more information about the parameters.
     *
     * @param destinationObject The destination object.
     * @param destinationKeyPath The destination key path.
     * @param sourceObject The source object.
     * @param sourceKeyPath The source key path.
     */
    public EasyBinding(ObservableValue destinationObject, String destinationKeyPath, ObservableValue sourceObject, String sourceKeyPath) {
        this.destinationObject = destinationObject;
        this.destinationKeyPath = splitKeyPath(destinationKeyPath);
        this.sourceObject = sourceObject;
        this.sourceKeyPath = splitKeyPath(sourceKeyPath);

        if(!this.destinationKeyPath[0].equals("root")) {
            throw new IllegalArgumentException("The destination key path should have the 'root' prefix");
        } else if(!this.sourceKeyPath[0].equals("root")) {
            throw new IllegalArgumentException("The source key path should have the 'root' prefix");
        }
    }

    /**
     * Returns object being updated.
     *
     * @return the object passed to the constructor
     */
    public Object getDestinationObject() {
        return destinationObject;
    }

    /**
     * Returns the key path being updated
     *
     * @return the key path passed to the constructor
     */
    public String getDestinationKeyPath() {
        return String.join(".", destinationKeyPath);
    }

    /**
     * Returns the source object
     *
     * @return the object passed to the constructor
     */
    public Object getSourceObject() {
        return sourceObject;
    }

    /**
     * Returns the source key path
     *
     * @return the key path passed to the constructor
     */
    public Object getSourceKeyPath() {
        return String.join(".", sourceKeyPath);
    }

    /**
     * Actually binds the destination key path to the source key path.
     *
     * <p>The method will setup listeners for all path components in the
     * destination path except for the last one. If one of them changes, it
     * will stop listening for the old values and start listening for the new
     * values. It will also update the bound value.
     *
     * <p>It will also setup listeners for all path components in the source path.
     * If one of them changes, it will stop listening for the old values and
     * start listening for the new values. It will also update the bound value.
     */
    public void bind() {
        Consumer<Object> changeConsumer = (value) -> {
            WritableValue sourceValue = getFinalWritableValue(sourceObject, sourceKeyPath);
            if(sourceValue != null) {
                if(sourceValue.getValue() != value) {
                    sourceValue.setValue(value);
                }
            }

            WritableValue destinationValue = getFinalWritableValue(destinationObject, destinationKeyPath);
            if(destinationValue != null) {
                if(destinationValue.getValue() != value) {
                    destinationValue.setValue(value);
                }
            }
        };

        destinationListener = Listener.createListenerChain(destinationObject, destinationKeyPath, changeConsumer);
        sourceListener = Listener.createListenerChain(sourceObject, sourceKeyPath, changeConsumer);
    }

    public void unbind() {
        Listener.destroyListenerChain(destinationListener);
        Listener.destroyListenerChain(sourceListener);
    }

    /**
     * Split the key path into path components.
     *
     * @param keyPath The key path specified by the user.
     *
     * @return the path components
     */
    static protected String[] splitKeyPath(String keyPath) {
        return keyPath.split("\\.");
    }

    /**
     * Join key path components to a string
     *
     * @param keyPath All components of the key path.
     *
     * @return a string containing the path components joined with a dot
     */
    static protected String joinKeyPath(String[] keyPath) {
        return String.join(".", keyPath);
    }

    static protected ObservableValue getFinalObservableValue(ObservableValue rootObject, String[] keyPath) {
        ObservableValue lastValue = rootObject;
        Object object = rootObject.getValue();

        String currentPath = keyPath[0];
        int i;
        for(i = 1; i < keyPath.length && object != null; i++) {
            String pathComponent = keyPath[i];
            currentPath = currentPath + "." + pathComponent;

            try {
                Field field = object.getClass().getField(keyPath[i]);
                if(!(field.get(object) instanceof ObservableValue)) {
                    throw new RuntimeException("Could not bind " + joinKeyPath(keyPath) + " of object "
                            + rootObject + " because the field " + currentPath + " is not an "
                            + "ObservableValue instance");
                }

                lastValue = (ObservableValue)field.get(object);
                object = lastValue.getValue();
            } catch(NoSuchFieldException exception) {
                    throw new RuntimeException("Could not bind " + joinKeyPath(keyPath) + " of object "
                        + rootObject + " because the field " + currentPath + " could not be found",
                        exception);
            } catch(IllegalAccessException exception) {
                throw new RuntimeException("Could not bind " + joinKeyPath(keyPath) + " of object "
                        + rootObject + " because the field " + currentPath + " could not be accessed",
                        exception);
            }
        }

        if(i != keyPath.length) {
            // We could not get to the final path component without getting a null value
            return null;
        }

        return lastValue;
    }

    static protected WritableValue getFinalWritableValue(ObservableValue rootObject, String[] keyPath) {
        ObservableValue observableValue = getFinalObservableValue(rootObject, keyPath);
        if(observableValue == null) {
            return null;
        }

        if(!(observableValue instanceof WritableValue)) {
            throw new RuntimeException("Could not bind " + joinKeyPath(keyPath) + " of object " +
                    rootObject + " because the field " + joinKeyPath(keyPath) + " is not a " +
                    "WritableValue instance");
        }

        return (WritableValue)observableValue;
    }

    static protected class Listener {
        protected ObservableValue rootObject;
        protected String[] fullKeyPath;
        protected Listener[] allListeners;
        protected ObservableValue value;
        protected ChangeListener listener;

        protected Consumer<Object> changeConsumer;

        protected Listener(ObservableValue rootObject, String[] fullKeyPath, Listener[] allListeners, ObservableValue value, Consumer<Object> changeConsumer) {
            this.rootObject = rootObject;
            this.fullKeyPath = fullKeyPath;
            this.allListeners = allListeners;
            this.value = value;

            this.changeConsumer = changeConsumer;
        }

        static protected Listener createListener(ObservableValue rootObject, String[] fullKeypath, Listener[] allListeners, ObservableValue value, Consumer<Object> changeConsumer) {
            Listener listener = new Listener(rootObject, fullKeypath, allListeners, value, changeConsumer);
            listener.listener = (observable, oldValue, newValue) -> {
                listener.onChange();
            };

            return listener;
        }

        static public Listener createListenerChain(ObservableValue rootObject, String[] fullKeyPath, Consumer<Object> changeConsumer) {
            Listener[] allListeners = new Listener[fullKeyPath.length];

            Listener rootListener = createListener(rootObject, fullKeyPath, allListeners, rootObject, changeConsumer);
            rootObject.addListener(rootListener.getListener());
            allListeners[0] = rootListener;

            rootListener.onChange();

            return allListeners[0];
        }

        static public void destroyListenerChain(Listener chain) {
            for(Listener listener : chain.allListeners) {
                if(listener != null) {
                    listener.unbind();
                }
            }
        }

        public ObservableValue getValue() {
            return value;
        }

        public ChangeListener getListener() {
            return listener;
        }

        /**
         * Called when the change listener is invoked.
         *
         * <p>Rebind all path components coming after the path component of this
         * listener and update destination value.
         *
         * <p>Must be called when the actual value of this listener changes, as
         * from that moment on all listeners of subsequent listeners are
         * listening for changes on the wrong object.
         */
        protected void onChange() {
            int i;
            String currentPath = null;
            for(i = 0; i < fullKeyPath.length; i++) {
                String pathComponent = fullKeyPath[i];
                currentPath = currentPath == null ? pathComponent : currentPath + "." + pathComponent;

                if(allListeners[i] == this) {
                    // we have found where we are in the key path
                    break;
                }
            }

            if(i == fullKeyPath.length) {
                // This cannot happen, the listener was not found in the key
                // path
                throw new RuntimeException("Programming error");
            }

            // Remove previous listeners
            for(int j = i + 1; j < fullKeyPath.length; j++) {
                if(allListeners[j] != null) {
                    allListeners[j].unbind();
                }
            }

            // Add new listeners
            for(i += 1; i < fullKeyPath.length; i++) {
                String pathComponent = fullKeyPath[i];
                currentPath = currentPath == null ? pathComponent : currentPath + "." + pathComponent;
                Object object = allListeners[i-1].getValue().getValue();
                try {
                    if(object != null) {
                        Field field = object.getClass().getField(pathComponent);
                        if(!(field.get(object) instanceof ObservableValue)) {
                            throw new RuntimeException("Could not bind " + joinKeyPath(fullKeyPath) + " of object " +
                                    rootObject + " because the field " + currentPath + " is not an " +
                                    "ObservableValue instance");
                        }

                        ObservableValue subvalue = (ObservableValue)field.get(object);
                        Listener sublistener = Listener.createListener(rootObject, fullKeyPath, allListeners, subvalue, changeConsumer);
                        subvalue.addListener(sublistener.getListener());
                        allListeners[i] = sublistener;
                    }

                } catch(NoSuchFieldException exception) {
                    throw new RuntimeException("Could not bind " + joinKeyPath(fullKeyPath) + " of object " +
                            rootObject + " because the field " + currentPath + " could not be found",
                            exception);
                } catch(IllegalAccessException exception) {
                    throw new RuntimeException("Could not bind " + joinKeyPath(fullKeyPath) + " of object " +
                            rootObject + " because the field " + currentPath + " could not be accessed",
                            exception);
                }
            }

            // Notify that the value has changed
            ObservableValue finalValue = getFinalObservableValue(rootObject, fullKeyPath);
            if(finalValue == null) {
                changeConsumer.accept(null);
            } else {
                changeConsumer.accept(finalValue.getValue());
            }
        }

        /**
         * Unbind the listener from its value.
         */
        protected void unbind() {
            value.removeListener(listener);
        }
    }
}
