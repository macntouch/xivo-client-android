package org.xivo.cti.message;

import java.util.ArrayList;
import java.util.List;

import org.xivo.cti.listener.UserIdsListener;

public class UserIdsList extends CtiMessage implements CtiEvent<UserIdsListener> {

    List<Integer> userIds;

    public UserIdsList() {
        userIds = new ArrayList<Integer>();
    }

    public List<Integer> getUserIds() {
        return userIds;
    }

    public void add(Integer userId) {
        userIds.add(userId);
    }

    @Override
    public void notify(UserIdsListener listener) {
        listener.onUserIdsLoaded(this);
    }

}
