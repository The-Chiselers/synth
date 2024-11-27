// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.AddressDecoder

import chisel3._
import chisel3.util._

// Source: https://github.com/chipsalliance/rocket-chip/issues/1668#issuecomment-433528365
class AddrDecoder[T <: Data with Num[T]](dType: T, n: Int) extends Module {
  val io = IO(new Bundle {
    val range0 = Input(Vec(n, dType))
    val range1 = Input(Vec(n, dType))
    val addr   = Input(dType)
    val en     = Input(Bool())
    val sel    = Output(Vec(n, Bool()))
  })
  // Curried function which accepts a tuple and an input addr
  // Use map to apply it to inputs
  def inside(range: (T, T))(addr: T): Bool = {
    addr >= range._1 && addr < range._1 + range._2
  }
  // MUX output
  for (i <- 0 until n) {
    io.sel(i) := false.B
  }
  when(io.en) {
    io.sel := io.range0 zip io.range1 map (inside(_)(io.addr))
  }
  // $onehot0 output encoding check
  assert(PopCount(io.sel) <= 1.U, "Invalid addr decoding")
}
