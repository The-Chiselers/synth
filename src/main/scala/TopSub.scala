// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.AddressDecoder

import chisel3._
import chisel3.util._

class TopSub extends Module(p: BaseParams) {
  val io = IO(new Bundle {
    val out = Output(UInt(p.dataWidth.W))

    val addrBundle = AddressBundle(p)
  })

  val addrMap: Map[]
  val addrDecoder = Module(new AddrDecoder(UInt(p.addressWidth.W), addrMap.size))

}
