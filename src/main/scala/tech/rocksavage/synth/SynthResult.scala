// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.synth

class SynthResult(synthString: String, stdout: String) {
  def getSynthString: String = synthString
  def getStdout: String = stdout
}
