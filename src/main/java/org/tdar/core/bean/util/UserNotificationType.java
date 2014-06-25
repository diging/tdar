package org.tdar.core.bean.util;

import org.tdar.core.bean.Localizable;
import org.tdar.utils.MessageHelper;

/**
 * Enum for user notification message types:
 * 
 * SYSTEM_BROADCAST messages are sent to every user and can be dismissed via TdarUser.updateDismissedNotificationsDate()
 * ERROR and WARNING messages are targeted to specific users and cannot be dismissed
 * 
 * INFO messages can be dismissed.
 * 
 */
public enum UserNotificationType implements Comparable<UserNotificationType>, Localizable {
    
    SYSTEM_BROADCAST,
    ERROR,
    WARNING,
    INFO;
    
    @Override
    public String getLocaleKey() {
        return MessageHelper.formatLocalizableKey(this);
    }

    
}
