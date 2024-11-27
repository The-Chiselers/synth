// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.AddressDecoder

import chisel3._
import chisel3.util._

class Top(p: BaseParams) extends Module {
  val io = IO(new Bundle {
    val in  = Input(UInt(p.dataWidth.W))
    val out = Output(UInt(p.dataWidth.W))
  })
  val io_address = Input(UInt(p.addressWidth.W))
  val io_data    = Input(UInt(p.dataWidth.W))

  when(io_address)

  val shiftReg = RegInit(VecInit(Seq.fill(p.max_delay - 1)(0.U(p.dataWidth.W))))
  val delay: UInt = RegInit(0.U(p.addressWidth.W))

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
