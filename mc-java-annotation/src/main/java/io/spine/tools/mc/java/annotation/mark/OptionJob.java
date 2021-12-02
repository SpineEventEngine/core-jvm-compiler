/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.annotation.mark;

import com.google.common.flogger.FluentLogger;
import io.spine.code.java.ClassName;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An annotation {@link Job} which covers Java sources generated from Protobuf
 * marked with a certain {@link ApiOption}.
 */
final class OptionJob extends AnnotationJob {

    private final ApiOption protobufOption;

    OptionJob(ApiOption protobufOption, ClassName annotation) {
        super(annotation);
        this.protobufOption = checkNotNull(protobufOption);
    }

    @SuppressWarnings("FloggerSplitLogStatement")
    // See: https://github.com/SpineEventEngine/base/issues/612
    @Override
    public void execute(AnnotatorFactory factory) {
        FluentLogger.Api debug = _debug();
        ClassName annotation = annotation();
        ApiOption option = protobufOption;
        debug.log("Annotating sources marked as `%s` with `%s`.",
                  option, annotation);
        debug.log("Annotating by the file option.");
        factory.createFileAnnotator(annotation, option)
               .annotate();
        debug.log("Annotating by the message option.");
        factory.createMessageAnnotator(annotation, option)
               .annotate();
        if (option.supportsServices()) {
            debug.log("Annotating by the service option.");
            factory.createServiceAnnotator(annotation, option)
                   .annotate();
        }
        if (option.supportsFields()) {
            debug.log("Annotating by the field option.");
            factory.createFieldAnnotator(annotation, option)
                   .annotate();
        }
        debug.log("Option `%s` processed.", option);
    }
}
