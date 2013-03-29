package org.xivo.cti.message;

public interface UserUpdateListener {
    public void onUserConfigUpdate(UserConfigUpdate userConfigUpdate);

    public void onUserStatusUpdate(UserStatusUpdate userStatusUpdate);
}
