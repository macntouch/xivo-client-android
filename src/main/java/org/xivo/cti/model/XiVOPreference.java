package org.xivo.cti.model;

public class XiVOPreference {
    private final String parameter;
    private final String value;

    public XiVOPreference(String parameter, String value) {
        this.parameter = parameter;
        this.value = value;
    }

    public String getParameter() {
        return parameter;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        XiVOPreference preference = (XiVOPreference) obj;
        return preference.parameter.equals(parameter) && preference.value.equals(value);
    }
    @Override
    public String toString() {
        return parameter + " : <" + value +">";
    }
}
