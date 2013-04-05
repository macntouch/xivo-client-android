package org.xivo.cti.message;

import org.xivo.cti.message.request.PhoneStatusUpdate;

public interface UserUpdateListener {
    public void onUserConfigUpdate(UserConfigUpdate userConfigUpdate);

    public void onUserStatusUpdate(UserStatusUpdate userStatusUpdate);

    public void onPhoneConfigUpdate(PhoneConfigUpdate phoneConfigUpdate);

    public void onUserStatusUpdate(PhoneStatusUpdate phoneStatusUpdate);
}
