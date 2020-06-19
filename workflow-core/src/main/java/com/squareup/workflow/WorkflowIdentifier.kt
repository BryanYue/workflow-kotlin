/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.workflow

import okio.BufferedSink
import okio.BufferedSource
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KClass

/**
 * Represents a [Workflow]'s "identity" and is used by the runtime to determine whether a workflow
 * is the same as a workflow as was rendered in a previous render pass, in which case its state
 * should be re-used, or if it's a new workflow and needs to be started.
 */
@ExperimentalWorkflow
class WorkflowIdentifier internal constructor(
  private val type: KClass<out Workflow<*, *, *>>? = null,
  typeString: String? = null,
  private val proxiedIdentifier: WorkflowIdentifier?
) {
  init {
    require((type == null) xor (typeString == null)) {
      "Either type or type string must be passed, not both."
    }
  }

  private val typeString: String by lazy(PUBLICATION) { typeString ?: type!!.java.name }

  override fun toString(): String =
    generateSequence(this) { it.proxiedIdentifier }
        .joinToString { it.typeString }
        .let { "WorkflowIdentifier($it)" }

  fun write(sink: BufferedSink) {
    sink.writeUtf8WithLength(typeString)
    if (proxiedIdentifier != null) {
      sink.writeByte(PROXY_IDENTIFIER_TAG.toInt())
      proxiedIdentifier.write(sink)
    } else {
      sink.writeByte(NO_PROXY_IDENTIFIER_TAG.toInt())
    }
  }

  override fun equals(other: Any?): Boolean = when {
    this === other -> true
    other !is WorkflowIdentifier -> false
    else -> {
      // If either identifier only has a string type, then only string types can be compared.
      val typesMatch = if (type == null || other.type == null) {
        typeString == other.typeString
      } else {
        type == other.type
      }
      typesMatch && proxiedIdentifier == other.proxiedIdentifier
    }
  }

  override fun hashCode(): Int {
    var result = typeString.hashCode()
    result = 31 * result + (proxiedIdentifier?.hashCode() ?: 0)
    return result
  }

  companion object {
    private const val NO_PROXY_IDENTIFIER_TAG = 0.toByte()
    private const val PROXY_IDENTIFIER_TAG = 1.toByte()

    /** Represents a parse error in [read]. */
    private val INVALID_IDENTIFIER = Any()

    /**
     * Reads a [WorkflowIdentifier] from [source]. Returns null if the source does not contain a
     * valid [WorkflowIdentifier] as written by [write].
     */
    fun read(source: BufferedSource): WorkflowIdentifier? {
      fun readIdentifierOrInvalid(): Any? {
        val typeString = source.readUtf8WithLength()
        val proxiedIdentifier = when (source.readByte()) {
          NO_PROXY_IDENTIFIER_TAG -> null
          PROXY_IDENTIFIER_TAG -> readIdentifierOrInvalid()
          else -> return INVALID_IDENTIFIER
        }
        if (proxiedIdentifier !is WorkflowIdentifier?) return INVALID_IDENTIFIER
        return WorkflowIdentifier(typeString = typeString, proxiedIdentifier = proxiedIdentifier)
      }
      return readIdentifierOrInvalid() as? WorkflowIdentifier
    }
  }
}

/**
 * The [WorkflowIdentifier] that identifies this [Workflow].
 */
@ExperimentalWorkflow
val Workflow<*, *, *>.identifier: WorkflowIdentifier
  get() {
    val proxiedIdentifier = (this as? ImpostorWorkflow)?.realIdentifier
    return WorkflowIdentifier(type = this::class, proxiedIdentifier = proxiedIdentifier)
  }
