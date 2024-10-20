package com.minersstudios.wholib.paper.event;

import com.minersstudios.wholib.event.handle.HandlerExecutor;
import com.minersstudios.wholib.event.EventListener;
import com.minersstudios.wholib.event.handle.CancellableHandler;
import com.minersstudios.wholib.event.handle.CancellableHandlerParams;
import com.minersstudios.wholib.listener.ListenFor;
import com.minersstudios.wholib.throwable.ListenerException;
import com.minersstudios.wholib.paper.listener.PaperListenerManager;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * An abstract class that represents a paper event listener.
 * <table>
 *     <caption>Available optional overridable methods</caption>
 *     <tr>
 *         <th>Method</th>
 *         <th>Description</th>
 *     </tr>
 *     <tr>
 *         <td>{@link #onRegister()}</td>
 *         <td>
 *             Called when the listener is registered by a manager in the
 *             {@link PaperListenerManager#register(com.minersstudios.wholib.listener.Listener)} method
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@link #onUnregister()}</td>
 *         <td>
 *             Called when the listener is unregistered by a manager in the
 *             {@link PaperListenerManager#unregister(com.minersstudios.wholib.listener.Listener)} method
 *         </td>
 *     </tr>
 * </table>
 *
 * @see PaperEventContainer
 * @see HandlerExecutor
 */
@SuppressWarnings("unused")
public abstract class PaperEventListener
        extends EventListener<Class<? extends Event>, PaperEventContainer<? extends Event>>
        implements Listener {

    /**
     * Constructs a new event listener.
     * <p>
     * This constructor will automatically retrieve all event handlers from the
     * listener class and event class from the {@link ListenFor} annotation.
     *
     * @throws ClassCastException If the event class is not a subclass of
     *                            annotated event class
     * @throws ListenerException  If the listener has duplicate event handlers
     *                            for the same order, or if the listener does
     *                            not have a {@link ListenFor} annotation
     */
    protected PaperEventListener() throws ClassCastException, ListenerException {
        super(CancellableHandler.class, CancellableHandlerParams::of);
    }

    /**
     * Constructs a new event listener with the specified event class.
     * <p>
     * This constructor will automatically retrieve all event handlers from the
     * listener class.
     *
     * @param key The key of the event listener
     * @throws ListenerException If the listener has duplicate event handlers
     *                           for the same order
     */
    protected PaperEventListener(final @NotNull Class<? extends Event> key) throws ListenerException {
        super(key, CancellableHandler.class, CancellableHandlerParams::of);
    }
}
