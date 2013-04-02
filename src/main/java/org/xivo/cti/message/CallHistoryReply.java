package org.xivo.cti.message;

import java.util.ArrayList;
import java.util.List;

import org.xivo.cti.listener.CallHistoryListener;
import org.xivo.cti.model.XiVOCall;

public class CallHistoryReply extends CtiMessage implements CtiEvent<CallHistoryListener>{
    
    List<XiVOCall> callHistory;

    public CallHistoryReply() {
        callHistory = new ArrayList<XiVOCall>();
    }
    public List<XiVOCall> getCallHistory() {
        return callHistory;
    }

    public void addCall(XiVOCall xiVOCall) {
        this.callHistory.add(xiVOCall);
        
    }
    @Override
    public void notify(CallHistoryListener listener) {
        listener.onCallHistoryUpdated(this.getCallHistory());
        
    }

}
