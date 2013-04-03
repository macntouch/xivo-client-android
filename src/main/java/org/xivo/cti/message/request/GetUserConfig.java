package org.xivo.cti.message.request;


public class GetUserConfig extends GetConfig {

    private static final String UPDATECONFIG = "updateconfig";

    public GetUserConfig(Integer userId) {
        super(UPDATECONFIG);
    }

}
