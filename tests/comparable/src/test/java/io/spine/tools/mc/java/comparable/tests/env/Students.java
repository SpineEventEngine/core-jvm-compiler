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

import io.spine.test.tools.mc.java.comparable.tests.Student;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Provides {@link Student} instances for
 * {@link io.spine.tools.mc.java.comparable.ComparablePluginTest ComparablePluginTest}.
 *
 * <p>Field names reflect the expected position of the corresponding message
 * when they all are sorted. The test case is supposed to sort them, so we don't provide
 * a sorted collection on our own. Otherwise, it would break the test essence, in which
 * we would compare two collections sorted by {@link java.util.Collections#sort(List)}.
 */
public class Students {

    public static final Student firstStudent = student(2022, "Jack");
    public static final Student secondStudent = student(2022, "Danial");
    public static final Student thirdStudent = student(2022, "Alex");
    public static final Student fourthStudent = student(2018, "Richard");
    public static final Student fifthStudent = student(2015, "Jessica");

    /**
     * Prevents instantiation of this utility class.
     */
    private Students() {
    }

    /**
     * Returns an unsorted collection of the five students.
     */
    public static List<Student> unsorted() {
        return newArrayList(fourthStudent, secondStudent, fifthStudent, firstStudent, thirdStudent);
    }

    private static Student student(int year, String name) {
        return Student.newBuilder()
                .setYear(year)
                .setName(name)
                .build();
    }
}
