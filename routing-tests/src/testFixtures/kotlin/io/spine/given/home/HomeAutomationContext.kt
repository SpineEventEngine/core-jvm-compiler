/*
 * Copyright 2025, TeamDev. All rights reserved.
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

package io.spine.given.home

import com.google.common.annotations.VisibleForTesting
import io.spine.core.Subscribe
import io.spine.given.home.commands.AddDevice
import io.spine.given.home.commands.SetState
import io.spine.given.home.events.DeviceAdded
import io.spine.given.home.events.DeviceMoved
import io.spine.given.home.events.RoomAdded
import io.spine.given.home.events.RoomEvent
import io.spine.given.home.events.RoomRenamed
import io.spine.given.home.events.StateChanged
import io.spine.given.home.events.deviceAdded
import io.spine.given.home.events.stateChanged
import io.spine.protobuf.isDefault
import io.spine.server.BoundedContext
import io.spine.server.aggregate.Aggregate
import io.spine.server.aggregate.Apply
import io.spine.server.command.Assign
import io.spine.server.entity.alter
import io.spine.server.projection.Projection
import io.spine.server.route.Route

fun homeAutomation(): BoundedContext = BoundedContext.singleTenant("HomeAutomation")
    .add(RoomProjection::class.java)
    .add(DeviceAggregate::class.java)
    .add(HomeProjection::class.java)
    .build()

@VisibleForTesting
class RoomProjection : Projection<RoomId, Room, Room.Builder>() {

    @Subscribe
    internal fun on(e: RoomAdded) = alter {
        name = e.name
    }

    @Subscribe
    internal fun on(e: RoomRenamed) = alter {
        name = e.name
    }

    @Subscribe
    internal fun on(e: DeviceMoved) = alter {
        if (id == e.prevRoom) {
            val toRemove = deviceBuilderList.find { b -> b.uuid == e.device.uuid }
            if (toRemove != null) {
                deviceBuilderList.remove(toRemove)
            }
        }
        if (id == e.room) {
            addDevice(e.device)
        }
    }

    companion object {

        /**
         * The routing function accepting the interface.
         */
        @Route
        fun route(e: RoomEvent): RoomId = e.room

        /**
         * The routing function accepting the event class.
         */
        @Route
        fun routeMoved(e: DeviceMoved): Set<RoomId> =
            if (e.prevRoom.isDefault()) setOf(e.room) else setOf(e.prevRoom, e.room)
    }
}

@VisibleForTesting
class DeviceAggregate : Aggregate<DeviceId, Device, Device.Builder>() {

    @Assign
    internal fun handle(c: AddDevice): DeviceAdded =
        deviceAdded { device = c.device; name = c.name }

    @Assign
    internal fun handle(c: SetState): StateChanged =
        stateChanged { device = id(); current = c.state }

    @Apply
    private fun event(e: DeviceAdded) = alter {
        name = e.name
        state = State.OFF
    }

    @Apply
    private fun event(e: StateChanged) = alter {
        state = e.current
    }

    companion object {

        @Route
        fun command(c: SetState): DeviceId = c.device
    }
}

@VisibleForTesting
class HomeProjection : Projection<String, Home, Home.Builder>() {

    @Subscribe
    internal fun on(updated: Room) = alter {
        val builder = roomBuilderList.find { b -> b.id == updated.id }
        if (builder != null) {
            builder.clear().mergeFrom(updated)
        } else {
            addRoom(updated)
        }
    }

    companion object {
        const val SINGLETON_ID = "OurHome"

        @Route
        internal fun room(@Suppress("UNUSED_PARAMETER") r: Room): String = SINGLETON_ID
    }
}
