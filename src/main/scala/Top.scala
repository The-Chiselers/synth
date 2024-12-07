// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.AddressDecoder

import chisel3._
import chisel3.util._

class Top(p: BaseParams) extends Module with Addressable {
  def memWidth(): Int = addressDecoder.total_size
  val io = IO(new Bundle {
    val in   = Input(UInt(p.dataWidth.W))
    val out1 = Output(UInt(p.dataWidth.W))

    val out2 = Output(UInt(p.dataWidth.W))

    val addr = Input(UInt(p.addressWidth.W))
    val data = Input(UInt(p.dataWidth.W))
    val re   = Input(Bool())
    val we   = Input(Bool())
  })
  val shiftReg = RegInit(VecInit(Seq.fill(p.max_delay - 1)(0.U(p.dataWidth.W))))
  val delay    = RegInit(0.U(p.dataWidth.W))

  // ###############
  // Initializations
  // ###############

  io.out2 := io.in

  // ####################
  // Module Functionality
  // ####################

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

  val delay_addr = 0x00

  // ##############
  // Decoding Stage
  // ##############

  val addressRanges: Seq[Int] = Seq(1)
  val addressDecoder          = Module(new AddrDecode(p, addressRanges))
  addressDecoder.io.addr        := io.addr
  addressDecoder.io.addr_offset := 0.U
  addressDecoder.io.en          := true.B

  val memory = Module(new Memory(p, addressDecoder.total_size))
  memory.io.read_enable   := addressDecoder.io.sel(delay_addr)
  memory.io.write_enable  := addressDecoder.io.sel(delay_addr) && io.we
  memory.io.read_address  := addressDecoder.io.addr_out
  memory.io.write_address := addressDecoder.io.addr_out
  memory.io.write_data    := io.data
  memory.io.clk           := clock

  // ##############
  // Memory Reading
  // ##############
  delay := 0.U
  when(memory.io.read_enable) {
    delay := memory.io.read_data
  }

}
