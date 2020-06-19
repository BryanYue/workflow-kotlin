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

/**
 * Optional interface that [Workflow]s should implement if they need the runtime to consider their
 * identity to include a child workflow's identity. This is intended to be used for helper workflows
 * that implement things like operators by wrapping and delegating to other workflows, and need to
 * be able to express that the identity of the operator workflow is derived from the identity of the
 * wrapped workflow.
 */
@ExperimentalWorkflow
interface ImpostorWorkflow {
  /**
   * The [WorkflowIdentifier] of the underlying [Workflow] that this workflow wraps, as obtained
   * by [Workflow.identifier].
   */
  val realIdentifier: WorkflowIdentifier
}
