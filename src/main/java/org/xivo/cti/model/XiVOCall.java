package org.xivo.cti.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


public class XiVOCall {
    private final String callDate;
    private final long duration;
    private final String fullName;
    private final CallType callType;

    public XiVOCall(String date, long duration, String fullName, CallType callType) {
        this.callDate = date;
        this.duration = duration;
        this.fullName = fullName;
        this.callType = callType;
    }

    public String getCallDate() {
        return callDate;
    }


    public double getDuration() {
        return duration;
    }


    public String getFullName() {
        return fullName;
    }


    public CallType getCallType() {
        return callType;
    }


    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this,obj);
    }
    
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
