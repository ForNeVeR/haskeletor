/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.util.index

import com.intellij.util.indexing.impl.forward.AbstractForwardIndexAccessor
import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}

@RunWith(classOf[JUnitRunner])
class ScalaScalarIndexExtensionSpec extends AnyFlatSpec with Matchers {

  private val externalizer = ScalaScalarIndexExtension.VoidDataExternalizer

  "serializeValueToByteSeq" should "return non-null for Unit value" in {
    // This is the exact code path that fails at runtime in ValueSerializationChecker:
    // when save() writes 0 bytes, serializeToByteSeq returns null,
    // and the checker compares original "()" against deserialized "null".
    val result = AbstractForwardIndexAccessor.serializeValueToByteSeq((), externalizer, 4)
    result should not be null
  }

  it should "produce a value that deserializes back to Unit" in {
    val sequence = AbstractForwardIndexAccessor.serializeValueToByteSeq((), externalizer, 4)
    sequence should not be null
    val deserialized = externalizer.read(sequence.toInputStream)
    deserialized shouldEqual (())
  }

  "VoidDataExternalizer" should "roundtrip Unit through save and read" in {
    val byteOut = new ByteArrayOutputStream()
    val dataOut = new DataOutputStream(byteOut)
    externalizer.save(dataOut, ())
    dataOut.flush()

    byteOut.size() should be > 0

    val dataIn = new DataInputStream(new ByteArrayInputStream(byteOut.toByteArray))
    val result = externalizer.read(dataIn)
    result shouldEqual (())
  }
}
