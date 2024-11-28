// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.AddressDecoder

import chisel3._
import chisel3.util._

class Top(p: BaseParams) extends Module {
  val io = IO(new Bundle {
    val in  = Input(UInt(p.dataWidth.W))
    val out = Output(UInt(p.dataWidth.W))

    val addr = Input(UInt(p.addressWidth.W))
    val data = Input(UInt(p.dataWidth.W))
  })

  val addressDecoder = Module(new AddrDecoder(p, 0, 1))
  // UInt<8>[0]
  addressDecoder.io.range0 := Wire(Vec(0, UInt(p.addressWidth.W)))
  addressDecoder.io.range1 := Wire(Vec(0, UInt(p.addressWidth.W)))
  addressDecoder.io.addr   := io.addr
  addressDecoder.io.data   := io.data
  addressDecoder.io.en     := 1.B

  val delay = Wire(UInt(p.dataWidth.W))
  delay := addressDecoder.io.regs(0)

  val shiftReg = RegInit(VecInit(Seq.fill(p.max_delay - 1)(0.U(p.dataWidth.W))))
  shiftReg(0) := io.in
  for (i <- 1 until p.max_delay - 1) {
    shiftReg(i) := shiftReg(i - 1)
  }
  when(delay === 0.U) {
    io.out := io.in
  }.otherwise {
    io.out := shiftReg(delay - 1.U)
  }

}
