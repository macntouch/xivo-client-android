package org.xivo.ctiold.listener;

import java.util.List;

import org.xivo.cti.model.XiVOCall;

public interface CallHistoryListener {

    void onCallHistoryUpdated(List<XiVOCall> callHistory);

}
