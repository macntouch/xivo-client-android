package org.xivo.cti.message;

public interface CtiEvent <L> {
    public void notify(final L listener);
}
