package org.xivo.ctiold.listener;

import org.xivo.ctiold.message.UserIdsList;

public interface UserIdsListener {

    void onUserIdsLoaded(UserIdsList userIdsList);

}
