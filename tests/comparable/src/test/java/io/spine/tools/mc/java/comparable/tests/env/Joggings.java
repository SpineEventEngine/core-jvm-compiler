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

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.test.tools.mc.java.comparable.tests.Jogging;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Provides {@link Jogging} instances for
 * {@link io.spine.tools.mc.java.comparable.ComparablePluginTest ComparablePluginTest}.
 *
 * <p>Field names reflect the expected position of the corresponding message
 * when they all are sorted. The test case is supposed to sort them, so we don't provide
 * a sorted collection on our own. Otherwise, it would break the test essence, in which
 * we would compare two collections sorted by {@link java.util.Collections#sort(List)}.
 */
public class Joggings {

    public static final Jogging firstJogging = jogging(start(12, 15), duration(30));
    public static final Jogging secondJogging = jogging(start(12, 15), duration(40));
    public static final Jogging thirdJogging = jogging(start(13, 15), duration(20));
    public static final Jogging fourthJogging = jogging(start(17, 30), duration(20));
    public static final Jogging fifthJogging = jogging(start(18, 0), duration(20));

    /**
     * Prevents instantiation of this utility class.
     */
    private Joggings() {
    }

    /**
     * Returns an unsorted collection of the five joggings.
     */
    public static List<Jogging> unsorted() {
        return newArrayList(fourthJogging, secondJogging, fifthJogging, firstJogging, thirdJogging);
    }

    private static Jogging jogging(Timestamp start, Duration duration) {
        return Jogging.newBuilder()
                .setStart(start)
                .setDuration(duration)
                .build();
    }

    private static Timestamp start(long hours, long minutes) {
        var instant = Instant.EPOCH
                .plus(hours, ChronoUnit.HOURS)
                .plus(minutes, ChronoUnit.MINUTES);
        var date = Date.from(instant);
        return Timestamps.fromDate(date);
    }

    private static Duration duration(long minutes) {
        return Duration.newBuilder()
                .setSeconds(minutes * 60)
                .build();
    }
}
