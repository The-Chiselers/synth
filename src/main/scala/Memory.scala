// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.addrdecode

import chisel3._

/** Blackbox to hold Verilog simulation model */
class Memory(p: BaseParams, totalSize: Int) extends Module {
  val io = IO(new Bundle {
    val readEnable: Bool = Input(Bool())
    val writeEnable: Bool = Input(Bool())
    val readAddress: UInt = Input(UInt(p.addressWidth.W))
    val writeAddress: UInt = Input(UInt(p.addressWidth.W))
    val writeData: UInt = Input(UInt(p.dataWidth.W))
    val readData: UInt = Output(UInt(p.dataWidth.W))
  })

  private val memory = SyncReadMem(totalSize, UInt(p.dataWidth.W))

  when(io.readEnable) {
    io.readData := memory.read(io.readAddress)
  }

  when(io.writeEnable) {
    memory.write(io.writeAddress, io.writeData)
  }

}
