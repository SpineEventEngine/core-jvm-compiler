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

package io.spine.tools.mc.java.routing.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSDeclarationContainer
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Variance
import java.io.File
import java.io.OutputStream

/**
 * A stub instance of [Environment] which can be used only as a parameter for no-op visitors.
 */
internal val stubEnvironment = Environment(NotResolver(), NotLogger(), NotCodeGenerator())

private fun notImplemented(): Nothing = TODO("Not implemented.")

/**
 * The stub implementation of [Resolver] which throws [NotImplementedError] from all functions.
 */
@Suppress("TooManyFunctions")
private class NotResolver : Resolver {

    override val builtIns: KSBuiltIns
        get() = notImplemented()

    override fun createKSTypeReferenceFromKSType(type: KSType): KSTypeReference = notImplemented()

    @KspExperimental
    override fun effectiveJavaModifiers(declaration: KSDeclaration): Set<Modifier> =
        notImplemented()

    override fun getAllFiles(): Sequence<KSFile> = notImplemented()

    override fun getClassDeclarationByName(name: KSName): KSClassDeclaration? = notImplemented()

    @KspExperimental
    override fun getDeclarationsFromPackage(packageName: String): Sequence<KSDeclaration> =
        notImplemented()

    @KspExperimental
    override fun getDeclarationsInSourceOrder(
        container: KSDeclarationContainer
    ): Sequence<KSDeclaration> = notImplemented()

    override fun getFunctionDeclarationsByName(
        name: KSName,
        includeTopLevel: Boolean
    ): Sequence<KSFunctionDeclaration> = notImplemented()

    @KspExperimental
    override fun getJavaWildcard(reference: KSTypeReference): KSTypeReference = notImplemented()

    @KspExperimental
    override fun getJvmCheckedException(function: KSFunctionDeclaration): Sequence<KSType> =
        notImplemented()

    @KspExperimental
    override fun getJvmCheckedException(accessor: KSPropertyAccessor): Sequence<KSType> =
        notImplemented()

    @KspExperimental
    override fun getJvmName(declaration: KSFunctionDeclaration): String? = notImplemented()

    @KspExperimental
    override fun getJvmName(accessor: KSPropertyAccessor): String? = notImplemented()

    override fun getKSNameFromString(name: String): KSName = notImplemented()

    @KspExperimental
    override fun getModuleName(): KSName = notImplemented()

    override fun getNewFiles(): Sequence<KSFile> = notImplemented()

    @KspExperimental
    override fun getOwnerJvmClassName(declaration: KSFunctionDeclaration): String? =
        notImplemented()

    @KspExperimental
    override fun getOwnerJvmClassName(declaration: KSPropertyDeclaration): String? =
        notImplemented()

    @KspExperimental
    override fun getPackageAnnotations(packageName: String): Sequence<KSAnnotation> =
        notImplemented()

    @KspExperimental
    override fun getPackagesWithAnnotation(annotationName: String): Sequence<String> =
        notImplemented()

    override fun getPropertyDeclarationByName(
        name: KSName,
        includeTopLevel: Boolean
    ): KSPropertyDeclaration? = notImplemented()

    override fun getSymbolsWithAnnotation(
        annotationName: String,
        inDepth: Boolean
    ): Sequence<KSAnnotated> = notImplemented()

    override fun getTypeArgument(typeRef: KSTypeReference, variance: Variance): KSTypeArgument =
        notImplemented()

    @KspExperimental
    override fun isJavaRawType(type: KSType): Boolean = notImplemented()

    @KspExperimental
    override fun mapJavaNameToKotlin(javaName: KSName): KSName? = notImplemented()

    @KspExperimental
    override fun mapKotlinNameToJava(kotlinName: KSName): KSName? = notImplemented()

    @KspExperimental
    override fun mapToJvmSignature(declaration: KSDeclaration): String? = notImplemented()

    override fun overrides(overrider: KSDeclaration, overridee: KSDeclaration): Boolean =
        notImplemented()

    override fun overrides(
        overrider: KSDeclaration,
        overridee: KSDeclaration,
        containingClass: KSClassDeclaration
    ): Boolean = notImplemented()
}

/**
 * The stub implementation of [KSPLogger] which throws [NotImplementedError] from all functions.
 */
private class NotLogger : KSPLogger {
    override fun error(message: String, symbol: KSNode?): Unit = notImplemented()
    override fun exception(e: Throwable): Unit = notImplemented()
    override fun info(message: String, symbol: KSNode?): Unit = notImplemented()
    override fun logging(message: String, symbol: KSNode?): Unit = notImplemented()
    override fun warn(message: String, symbol: KSNode?): Unit = notImplemented()
}

/**
 * The stub implementation of [CodeGenerator] which throws [NotImplementedError] from all functions.
 */
private class NotCodeGenerator : CodeGenerator {

    override val generatedFile: Collection<File>
        get() = notImplemented()

    override fun associate(
        sources: List<KSFile>,
        packageName: String,
        fileName: String,
        extensionName: String
    ): Unit = notImplemented()

    override fun associateByPath(sources: List<KSFile>, path: String, extensionName: String): Unit =
        notImplemented()

    override fun associateWithClasses(
        classes: List<KSClassDeclaration>,
        packageName: String,
        fileName: String,
        extensionName: String
    ): Unit = notImplemented()

    override fun createNewFile(
        dependencies: Dependencies,
        packageName: String,
        fileName: String,
        extensionName: String
    ): OutputStream = notImplemented()

    override fun createNewFileByPath(
        dependencies: Dependencies,
        path: String,
        extensionName: String
    ): OutputStream = notImplemented()
}
