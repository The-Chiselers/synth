// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.AddressDecoder

import chisel3._
import chisel3.util._

class Top(p: BaseParams) extends Module with Addressable {
  val io = IO(new Bundle {
    val in   = Input(UInt(p.dataWidth.W))
    val out1 = Output(UInt(p.dataWidth.W))

    val out2 = Output(UInt(p.dataWidth.W))

    val addr = Input(UInt(p.addressWidth.W))
    val data = Input(UInt(p.dataWidth.W))
  })

  // ####################
  // Module Functionality
  // ####################

  val shiftReg = RegInit(VecInit(Seq.fill(p.max_delay - 1)(0.U(p.dataWidth.W))))
  shiftReg(0) := io.in
  for (i <- 1 until p.max_delay - 1) {
    shiftReg(i) := shiftReg(i - 1)
  }
  when(delay === 0.U) {
    io.out1 := io.in
  }.otherwise {
    io.out1 := shiftReg(delay - 1.U)
  }

  // ##############
  // Memory Mapping
  // ##############

  val depth_addr = 0x00

  // #####################
  // Memory Implementation
  // #####################

  val memory = Module(new SramBb(p))
  memory.io.read_enable
  memory.io.write_enable
  memory.io.read_address
  memory.io.write_address
  memory.io.write_data
  memory.io.read_data

  val addressDecoder = Module(new AddrDecoder(p, 0, 1))
  // UInt<8>[0]
  addressDecoder.io.range0 := Wire(Vec(0, UInt(p.addressWidth.W)))
  addressDecoder.io.range1 := Wire(Vec(0, UInt(p.addressWidth.W)))
  addressDecoder.io.addr   := io.addr
  addressDecoder.io.en     := 1.B

  // ##
  // Decoding Stage
  // ##

  memory.io.read_enable

  // ##
  // Memory Reading
  // ##

  val delay = Wire(UInt(p.dataWidth.W))
  delay := memory.io.read_data

}
