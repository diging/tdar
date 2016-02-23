package org.tdar.core.service;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.databind.util.ISO8601Utils;

import java.text.FieldPosition;
import java.util.Date;
import java.util.TimeZone;

/**
 * Extends ISO8601DateFormat's format() to provide date according to specified timezone instead of UTC.
 */
public class ISO8601LocalDateFormat extends ISO8601DateFormat {

    TimeZone timeZone = null;

    /**
     * Return DateFormat  instance that formats dates in the specified time zone.
     * @param timeZone
     */
    public ISO8601LocalDateFormat(TimeZone timeZone) {
        if(timeZone == null) {
            this.timeZone = TimeZone.getDefault();
        }
    }

    /**
     * Instantiate DateFormat that formats dates according to system default timezone
     */
    public ISO8601LocalDateFormat() {
        this(null);
    }


    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        String value = ISO8601Utils.format(date, false, timeZone);
        toAppendTo.append(value);
        return toAppendTo;
    }
}
