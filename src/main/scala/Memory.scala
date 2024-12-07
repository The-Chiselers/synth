// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.AddrDecode

import chisel3._
import chisel3.util._

/** Blackbox to hold Verilog simulation model */
class Memory(p: BaseParams, totalSize: Int) extends Module {
  val io = IO(new Bundle {
    val clk           = Input(Clock())
    val read_enable   = Input(Bool())
    val write_enable  = Input(Bool())
    val read_address  = Input(UInt(p.addressWidth.W))
    val write_address = Input(UInt(p.addressWidth.W))
    val write_data    = Input(UInt(p.dataWidth.W))
    val read_data     = Output(UInt(p.dataWidth.W))
  })

    val memory = SyncReadMem(totalSize, UInt(p.dataWidth.W))

    when(io.read_enable) {
      io.read_data := memory.read(io.read_address)
    }

    when(io.write_enable) {
      memory.write(io.write_address, io.write_data)
    }

}
