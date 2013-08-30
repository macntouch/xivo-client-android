package org.xivo.ctiold.message;

import org.xivo.ctiold.message.PhoneConfigUpdate;
import org.xivo.ctiold.message.UserConfigUpdate;
import org.xivo.ctiold.message.UserStatusUpdate;
import org.xivo.ctiold.message.request.PhoneStatusUpdate;

public interface UserUpdateListener {
    public void onUserConfigUpdate(UserConfigUpdate userConfigUpdate);

    public void onUserStatusUpdate(UserStatusUpdate userStatusUpdate);

    public void onPhoneConfigUpdate(PhoneConfigUpdate phoneConfigUpdate);

    public void onUserStatusUpdate(PhoneStatusUpdate phoneStatusUpdate);
}
