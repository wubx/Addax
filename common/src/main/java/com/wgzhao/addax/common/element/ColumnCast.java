/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.wgzhao.addax.common.element;

import com.wgzhao.addax.common.exception.CommonErrorCode;
import com.wgzhao.addax.common.exception.AddaxException;
import com.wgzhao.addax.common.util.Configuration;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public final class ColumnCast
{

    private ColumnCast() {}

    public static void bind(final Configuration configuration)
    {
        StringCast.init(configuration);
        DateCast.init(configuration);
        BytesCast.init(configuration);
    }

    public static Date string2Date(final StringColumn column)
            throws ParseException
    {
        return StringCast.asDate(column);
    }

    public static byte[] string2Bytes(final StringColumn column)
            throws UnsupportedEncodingException
    {
        return StringCast.asBytes(column);
    }

    public static String date2String(final DateColumn column)
    {
        return DateCast.asString(column);
    }

    public static String bytes2String(final BytesColumn column)
            throws UnsupportedEncodingException
    {
        return BytesCast.asString(column);
    }
}

class StringCast
{

    static String datetimeFormat = "yyyy-MM-dd HH:mm:ss";
    static String dateFormat = "yyyy-MM-dd";
    static String timeFormat = "HH:mm:ss";
    static List<String> extraFormats = Collections.emptyList();
    static String timeZone = "GMT+8";
    static FastDateFormat dateFormatter;
    static FastDateFormat timeFormatter;
    static FastDateFormat datetimeFormatter;
    static TimeZone timeZoner;
    static String encoding = "UTF-8";

    private StringCast() {}

    static void init(final Configuration configuration)
    {
        StringCast.datetimeFormat = configuration.getString(
                "common.column.datetimeFormat", StringCast.datetimeFormat);
        StringCast.dateFormat = configuration.getString(
                "common.column.dateFormat", StringCast.dateFormat);
        StringCast.timeFormat = configuration.getString(
                "common.column.timeFormat", StringCast.timeFormat);
        StringCast.extraFormats = configuration.getList(
                "common.column.extraFormats", Collections.emptyList(), String.class);

        StringCast.timeZone = configuration.getString("common.column.timeZone",
                StringCast.timeZone);
        StringCast.timeZoner = TimeZone.getTimeZone(StringCast.timeZone);

        StringCast.datetimeFormatter = FastDateFormat.getInstance(
                StringCast.datetimeFormat, StringCast.timeZoner);
        StringCast.dateFormatter = FastDateFormat.getInstance(
                StringCast.dateFormat, StringCast.timeZoner);
        StringCast.timeFormatter = FastDateFormat.getInstance(
                StringCast.timeFormat, StringCast.timeZoner);

        StringCast.encoding = configuration.getString("common.column.encoding",
                StringCast.encoding);
    }

    static Date asDate(final StringColumn column)
            throws ParseException
    {
        if (null == column.asString()) {
            return null;
        }

        try {
            return StringCast.datetimeFormatter.parse(column.asString());
        }
        catch (ParseException ignored) {
            //
        }

        try {
            return StringCast.dateFormatter.parse(column.asString());
        }
        catch (ParseException ignored) {
            //
        }

        ParseException e;
        try {
            return StringCast.timeFormatter.parse(column.asString());
        }
        catch (ParseException pe) {
            e = pe;
        }

        for (String format : StringCast.extraFormats) {
            try {
                return FastDateFormat.getInstance(format, StringCast.timeZoner).parse(column.asString());
            }
            catch (ParseException pe) {
                e = pe;
            }
        }
        throw e;
    }

    static byte[] asBytes(final StringColumn column)
            throws UnsupportedEncodingException
    {
        if (null == column.asString()) {
            return new byte[0];
        }

        return column.asString().getBytes(StringCast.encoding);
    }
}

/**
 * 后续为了可维护性，可以考虑直接使用 apache 的DateFormatUtils.
 * <p>
 * 迟南已经修复了该问题，但是为了维护性，还是直接使用apache的内置函数
 */
class DateCast
{
    static String datetimeFormat = "yyyy-MM-dd HH:mm:ss";
    static String dateFormat = "yyyy-MM-dd";
    static String timeFormat = "HH:mm:ss";
    static String timeZone = "GMT+8";
    static TimeZone timeZoner = TimeZone.getTimeZone(DateCast.timeZone);

    private DateCast() {}

    static void init(final Configuration configuration)
    {
        DateCast.datetimeFormat = configuration.getString(
                "common.column.datetimeFormat", datetimeFormat);
        DateCast.timeFormat = configuration.getString(
                "common.column.timeFormat", timeFormat);
        DateCast.dateFormat = configuration.getString(
                "common.column.dateFormat", dateFormat);
        DateCast.timeZone = configuration.getString("common.column.timeZone",
                DateCast.timeZone);
        DateCast.timeZoner = TimeZone.getTimeZone(DateCast.timeZone);
    }

    static String asString(final DateColumn column)
    {
        if (null == column.asDate()) {
            return null;
        }

        switch (column.getSubType()) {
            case DATE:
                return DateFormatUtils.format(column.asDate(), DateCast.dateFormat,
                        DateCast.timeZoner);
            case TIME:
                return DateFormatUtils.format(column.asDate(), DateCast.timeFormat,
                        DateCast.timeZoner);
            case DATETIME:
                return DateFormatUtils.format(column.asDate(),
                        DateCast.datetimeFormat, DateCast.timeZoner);
            default:
                throw AddaxException
                        .asAddaxException(CommonErrorCode.CONVERT_NOT_SUPPORT,
                                "时间类型出现不支持类型，目前仅支持DATE/TIME/DATETIME。该类型属于编程错误.");
        }
    }
}

class BytesCast
{
    static String encoding = "utf-8";

    private BytesCast() {}

    static void init(final Configuration configuration)
    {
        BytesCast.encoding = configuration.getString("common.column.encoding",
                BytesCast.encoding);
    }

    static String asString(final BytesColumn column)
            throws UnsupportedEncodingException
    {
        if (null == column.asBytes()) {
            return null;
        }

        return new String(column.asBytes(), encoding);
    }
}