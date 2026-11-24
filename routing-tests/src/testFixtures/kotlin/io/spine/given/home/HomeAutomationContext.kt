/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.given.home

import io.spine.annotation.VisibleForTesting
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
import io.spine.server.BoundedContextBuilder
import io.spine.server.aggregate.Aggregate
import io.spine.server.command.Assign
import io.spine.server.projection.Projection
import io.spine.server.route.Route

fun homeAutomation(): BoundedContextBuilder = BoundedContext.singleTenant("HomeAutomation")
    .add(RoomProjection::class.java)
    .add(DeviceAggregate::class.java)
    .add(HomeProjection::class.java)

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
    internal fun handle(c: AddDevice): DeviceAdded {
        alter {
            name = c.name
            state = State.OFF
        }
        return deviceAdded { device = c.device; name = c.name }
    }

    @Assign
    internal fun handle(c: SetState): StateChanged {
        alter {
            state = c.state
        }
        return stateChanged { device = id(); current = c.state }
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
