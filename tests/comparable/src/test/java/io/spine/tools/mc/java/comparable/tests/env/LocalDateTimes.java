/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.tools.mc.java.comparable.tests.env;

import io.spine.test.tools.mc.java.comparable.tests.LocalDate;
import io.spine.test.tools.mc.java.comparable.tests.LocalDateTime;
import io.spine.test.tools.mc.java.comparable.tests.LocalTime;
import io.spine.test.tools.mc.java.comparable.tests.Zone;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Provides {@link LocalDateTime} instances for
 * {@link io.spine.tools.mc.java.comparable.ComparablePluginTest ComparablePluginTest}.
 *
 * <p>Please note, the used {@link LocalDateTime} is not a message from Spine Time library.
 * It is a locally-declared Protobuf message. See {@code local_date_time.proto}
 * file in the Protobyf sources of this module.
 *
 * <p>Field names reflect the expected position of the corresponding message
 * when they all are sorted. The test case is supposed to sort them, so we don't provide
 * a sorted collection on our own. Otherwise, it would break the test essence, in which
 * we would compare two collections sorted by {@link java.util.Collections#sort(List)}.
 */
public class LocalDateTimes {

    public static final LocalDateTime firstStamp = dateTime(
            date(2015, 9, 1),
            time(12, 12, 12),
            Zone.ZONE_LONDON
    );
    public static final LocalDateTime secondStamp = firstStamp.toBuilder()
            .setDate(date(2016, 10, 15))
            .setZone(Zone.ZONE_LISBON)
            .build();
    public static final LocalDateTime thirdStamp = secondStamp.toBuilder()
            .setZone(Zone.ZONE_WARSAW)
            .build();
    public static final LocalDateTime fourthStamp = thirdStamp.toBuilder()
            .setZone(Zone.ZONE_KYIV)
            .build();
    public static final LocalDateTime fifthStamp = fourthStamp.toBuilder()
            .setTime(time(18, 30, 0))
            .setZone(Zone.ZONE_KYIV)
            .build();

    /**
     * Prevents instantiation of this utility class.
     */
    private LocalDateTimes() {
    }

    /**
     * Returns an unsorted collection of the five timestamps.
     */
    public static List<LocalDateTime> unsorted() {
        return newArrayList(fourthStamp, thirdStamp, fifthStamp, firstStamp, secondStamp);
    }

    private static LocalDate date(int year, int month, int day) {
        return LocalDate.newBuilder()
                .setYear(year)
                .setMonth(month)
                .setDay(day)
                .build();
    }

    private static LocalTime time(int hours, int minutes, int seconds) {
        return LocalTime.newBuilder()
                .setHours(hours)
                .setMinutes(minutes)
                .setSeconds(seconds)
                .build();
    }

    private static LocalDateTime dateTime(LocalDate date, LocalTime time, Zone zone) {
        return LocalDateTime.newBuilder()
                .setDate(date)
                .setTime(time)
                .setZone(zone)
                .build();
    }
}
