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

package io.spine.tools.mc.java.comparable.tests;

import io.spine.tools.mc.java.comparable.tests.env.Joggings;
import io.spine.tools.mc.java.comparable.tests.env.LocalDateTimes;
import io.spine.tools.mc.java.comparable.tests.env.Students;
import io.spine.tools.mc.java.comparable.tests.env.Travelers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.tools.mc.java.comparable.tests.env.Joggings.fifthJogging;
import static io.spine.tools.mc.java.comparable.tests.env.Joggings.firstJogging;
import static io.spine.tools.mc.java.comparable.tests.env.Joggings.fourthJogging;
import static io.spine.tools.mc.java.comparable.tests.env.Joggings.secondJogging;
import static io.spine.tools.mc.java.comparable.tests.env.Joggings.thirdJogging;
import static io.spine.tools.mc.java.comparable.tests.env.LocalDateTimes.fifthStamp;
import static io.spine.tools.mc.java.comparable.tests.env.LocalDateTimes.firstStamp;
import static io.spine.tools.mc.java.comparable.tests.env.LocalDateTimes.fourthStamp;
import static io.spine.tools.mc.java.comparable.tests.env.LocalDateTimes.secondStamp;
import static io.spine.tools.mc.java.comparable.tests.env.LocalDateTimes.thirdStamp;
import static io.spine.tools.mc.java.comparable.tests.env.Students.fifthStudent;
import static io.spine.tools.mc.java.comparable.tests.env.Students.firstStudent;
import static io.spine.tools.mc.java.comparable.tests.env.Students.fourthStudent;
import static io.spine.tools.mc.java.comparable.tests.env.Students.secondStudent;
import static io.spine.tools.mc.java.comparable.tests.env.Students.thirdStudent;
import static io.spine.tools.mc.java.comparable.tests.env.Travelers.fifthTraveler;
import static io.spine.tools.mc.java.comparable.tests.env.Travelers.firstTraveler;
import static io.spine.tools.mc.java.comparable.tests.env.Travelers.fourthTraveler;
import static io.spine.tools.mc.java.comparable.tests.env.Travelers.secondTraveler;
import static io.spine.tools.mc.java.comparable.tests.env.Travelers.thirdTraveler;
import static java.util.Collections.sort;

@DisplayName("`ComparablePlugin` should")
class ComparablePluginTest {

    @Test
    @DisplayName("make messages comparable")
    void makeMessagesComparable() {
        var localDateTimes = LocalDateTimes.unsorted();
        var expected = newArrayList(firstStamp, secondStamp, thirdStamp,
                                    fourthStamp, fifthStamp);
        assertThat(localDateTimes).isNotEqualTo(expected);
        sort(localDateTimes);
        assertThat(localDateTimes).isEqualTo(expected);
    }

    @Test
    @DisplayName("make messages reversed-comparable")
    void makeMessagesReversedComparable() {
        var students = Students.unsorted();
        var expected = newArrayList(firstStudent, secondStudent, thirdStudent,
                                    fourthStudent, fifthStudent);
        assertThat(students).isNotEqualTo(expected);
        sort(students);
        assertThat(students).isEqualTo(expected);
    }

    @Test
    @DisplayName("support comparison by nested fields")
    void supportComparisonByNestedFields() {
        var travelers = Travelers.unsorted();
        var expected = newArrayList(firstTraveler, secondTraveler, thirdTraveler,
                                    fourthTraveler, fifthTraveler);
        assertThat(travelers).isNotEqualTo(expected);
        sort(travelers);
        assertThat(travelers).isEqualTo(expected);
    }

    @Test
    @DisplayName("support comparators from the 'ComparatorRegistry'")
    void supportComparatorsFromTheComparatorRegistry() {
        var joggings = Joggings.unsorted();
        var expected = newArrayList(firstJogging, secondJogging, thirdJogging,
                                    fourthJogging, fifthJogging);
        assertThat(joggings).isNotEqualTo(expected);
        sort(joggings);
        assertThat(joggings).isEqualTo(expected);
    }
}
