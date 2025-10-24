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

package io.spine.tools.core.jvm.gradle

import org.gradle.api.logging.Logger
import org.gradle.internal.cc.base.logger

/**
 * The prefix for logging messages produced by CoreJvm Compiler Gradle plugins.
 */
public const val LOG_PREFIX: String = "[CoreJvm Compiler] "

/**
 * Logs the [message] at the `INFO` level with the [prefix][LOG_PREFIX].
 */
public fun Logger.info(message: () -> String) {
    if (logger.isInfoEnabled) {
        info(LOG_PREFIX + message())
    }
}

/**
 * Logs the [message] at the `WARN` level with the [prefix][LOG_PREFIX].
 */
public fun Logger.warn(message: () -> String) {
    if (logger.isWarnEnabled) {
        warn(LOG_PREFIX + message())
    }
}

/**
 * Logs the [message] at the `DEBUG` level with the [prefix][LOG_PREFIX].
 */
public fun Logger.debug(message: () -> String) {
    if (logger.isDebugEnabled) {
        debug(LOG_PREFIX + message())
    }
}
