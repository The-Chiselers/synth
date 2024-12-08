// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.addrdecode

import chisel3._

class SynthConfig(val techlibPath: String) {

  require(
    new java.io.File(techlibPath).exists,
    s"Technology library $techlibPath does not exist"
  )
}
