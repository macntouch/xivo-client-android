package org.xivo.cti;

import java.util.ArrayList;
import java.util.HashMap;

import org.xivo.cti.message.CtiEvent;

public class MessageDispatcher {

    private final HashMap<Class, ArrayList> activeListeners = new HashMap<Class, ArrayList>(10);

    public <L> void addListener(Class<? extends CtiEvent<L>> evtClass, L listener) {
        final ArrayList<L> listeners = listenersOf(evtClass);
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /** Gets listeners for a given event class **/
    private <L> ArrayList<L> listenersOf(Class<? extends CtiEvent<L>> evtClass) {
        @SuppressWarnings("unchecked")
        final ArrayList<L> existing = activeListeners.get(evtClass);
        if (existing != null) {
            return existing;
        }

        final ArrayList<L> emptyList = new ArrayList<L>(5);
        activeListeners.put(evtClass, emptyList);
        return emptyList;
    }

    public <L> void dispatch(final CtiEvent<L> evt) {
        @SuppressWarnings("unchecked")
        Class<CtiEvent<L>> evtClass = (Class<CtiEvent<L>>) evt.getClass();

        for (L listener : listenersOf(evtClass)) {
            evt.notify(listener);
        }
    }
}
