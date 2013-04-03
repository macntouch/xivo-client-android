package org.xivo.cti.listener;

import org.xivo.cti.message.UserIdsList;

public interface UserIdsListener {

    void onUserIdsLoaded(UserIdsList userIdsList);

}
