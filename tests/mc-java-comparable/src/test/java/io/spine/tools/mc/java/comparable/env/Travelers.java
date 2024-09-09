/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.tools.mc.java.comparable.env;

import io.spine.tools.mc.java.comparable.given.Address;
import io.spine.tools.mc.java.comparable.given.Destination;
import io.spine.tools.mc.java.comparable.given.Traveler;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Provides {@link Traveler} instances for
 * {@link io.spine.tools.mc.java.comparable.ComparablePluginTest ComparablePluginTest}.
 *
 * <p>Field names reflect the expected position of the corresponding message
 * when they all are sorted. The test case is supposed to sort them, so we don't provide
 * a sorted collection on our own. Otherwise, it would break the test essence, in which
 * we would compare two collections sorted by {@link java.util.Collections#sort(List)}.
 */
public class Travelers {

    public static final Traveler firstTraveler = traveler("America", "Washington", "Pennsylvania Avenue 5", true);
    public static final Traveler secondTraveler = traveler("America", "Washington", "U Street", false);
    public static final Traveler thirdTraveler = traveler("Germany", "Berlin", "Potsdamer Platz", false);
    public static final Traveler fourthTraveler = traveler("Germany", "Munich", "Sendlinger Strasse", false);
    public static final Traveler fifthTraveler = traveler("Germany", "Munich", "Sendlinger Strasse", true);

    /**
     * Prevents instantiation of this utility class.
     */
    private Travelers() {
    }

    /**
     * Returns a non-sorted collection of the five travelers.
     */
    public static List<Traveler> notSorted() {
        return newArrayList(fourthTraveler, secondTraveler, fifthTraveler, firstTraveler,
                            thirdTraveler);
    }

    private static Traveler traveler(String country, String city, String street, boolean fool) {
        var address = Address.newBuilder()
                .setStreet(street)
                .setFull(fool)
                .build();
        var destination = Destination.newBuilder()
                .setCountry(country)
                .setCity(city)
                .setAddress(address)
                .build();
        return Traveler.newBuilder()
                .setDestination(destination)
                .build();
    }
}
