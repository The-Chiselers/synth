// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.AddressDecoder

import chisel3._
import chisel3.util._

class AddressBundle(p: BaseParams) extends Bundle {
  val address = Input(UInt(p.addressWidth.W))
  val data    = Input(UInt(p.dataWidth.W))
}
