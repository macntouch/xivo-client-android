package org.xivo.ctiold.message;

public interface CtiEvent <L> {
    public void notify(final L listener);
}
