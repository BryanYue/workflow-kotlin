/*
 * Copyright 2019 Square Inc.
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

import com.squareup.workflow.internal.WorkflowNodeId
import com.squareup.workflow.internal.id
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class TreeSnapshotTest {

  @Test fun `overrides equals`() {
    val snapshot1 = TreeSnapshot(
        workflowSnapshot = Snapshot.of("foo"),
        childTreeSnapshots = mapOf(Workflow1.id("bar") to Snapshot.of("baz"))
    )
    val snapshot2 = TreeSnapshot(
        workflowSnapshot = Snapshot.of("foo"),
        childTreeSnapshots = mapOf(Workflow1.id("bar") to Snapshot.of("baz"))
    )
    assertEquals(snapshot1, snapshot2)
  }

  @Test fun `serialize and deserialize`() {
    val rootSnapshot = Snapshot.of("roo")
    val id1 = WorkflowNodeId(Workflow1)
    val id2 = WorkflowNodeId(Workflow2)
    val id3 = WorkflowNodeId(Workflow2, name = "b")
    val childSnapshots = mapOf(
        id1 to Snapshot.of("one"),
        id2 to Snapshot.of("two"),
        id3 to Snapshot.of("three")
    )

    val bytes = TreeSnapshot(rootSnapshot, childSnapshots).toByteString()
    val treeSnapshot = TreeSnapshot.parse(bytes)

    assertEquals(rootSnapshot.bytes, treeSnapshot.workflowSnapshot?.bytes)
    assertTrue(id1 in treeSnapshot.childTreeSnapshots)
    assertTrue(id2 in treeSnapshot.childTreeSnapshots)
    assertTrue(id3 in treeSnapshot.childTreeSnapshots)

    assertEquals("one", treeSnapshot.childTreeSnapshots.getValue(id1).bytes.utf8())
    assertEquals("two", treeSnapshot.childTreeSnapshots.getValue(id2).bytes.utf8())
    assertEquals("three", treeSnapshot.childTreeSnapshots.getValue(id3).bytes.utf8())
  }

  @Test fun `empty root is null`() {
    val rootSnapshot = Snapshot.EMPTY
    val treeSnapshot = TreeSnapshot(rootSnapshot, emptyMap())

    assertNull(treeSnapshot.workflowSnapshot)
  }
}

private object Workflow1 : Workflow<Unit, Nothing, Unit> {
  override fun asStatefulWorkflow(): StatefulWorkflow<Unit, *, Nothing, Unit> = fail()
}

private object Workflow2 : Workflow<Unit, Nothing, Unit> {
  override fun asStatefulWorkflow(): StatefulWorkflow<Unit, *, Nothing, Unit> = fail()
}
