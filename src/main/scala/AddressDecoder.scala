// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.AddressDecoder

import chisel3._
import chisel3.util._

// Source: https://github.com/chipsalliance/rocket-chip/issues/1668#issuecomment-433528365
class AddrDecoder(
    p: BaseParams,
    n: Int,
    numRegs: Int
) extends Module {
  val io = IO(new Bundle {
    val range0 = Input(Vec(n, UInt(p.addressWidth.W)))
    val range1 = Input(Vec(n, UInt(p.addressWidth.W)))
    val addr   = Input(UInt(p.addressWidth.W))
    val en     = Input(Bool())
    val sel    = Output(Vec(n, Bool()))
  })

  // Curried function which accepts a tuple and an input addr
  // Use map to apply it to inputs
  def inside(range: (UInt, UInt))(addr: UInt): Bool = {
    addr >= range._1 && addr < range._1 + range._2
  }
  // MUX output
  for (i <- 0 until n) {
    io.sel(i) := false.B
  }

  // io.regs := io.regs

  when(io.en) {
    for (i <- 0 until n) {
      io.sel(i) := inside(io.range0(i), io.range1(i))(io.addr)
    }
  }
  // $onehot0 output encoding check
  assert(PopCount(io.sel) <= 1.U, "Invalid addr decoding")
}
