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
    val data   = Input(UInt(p.dataWidth.W))
    val en     = Input(Bool())
    val sel    = Output(Vec(n, Bool()))
    val regs   = Output(Vec(numRegs, UInt(p.dataWidth.W)))
  })

  val regs = RegInit(VecInit(Seq.fill(numRegs)(0.U(p.dataWidth.W))))

  // assign regs to io.regs
  io.regs := regs

  // Curried function which accepts a tuple and an input addr
  // Use map to apply it to inputs
  def inside(range: (UInt, UInt))(addr: UInt): Bool = {
    addr >= range._1 && addr < range._1 + range._2
  }
  // MUX output
  for (i <- 0 until n) {
    io.sel(i) := false.B
  }
  val effectiveRange0 = io.range0 map (_ + numRegs.U)
  val effectiveRange1 = io.range1 map (_ + numRegs.U)

  // io.regs := io.regs

  when(io.en) {

    when(inside((0.U, numRegs.U))(io.addr)) {
      // enumerate the registers
      for (i <- 0 until numRegs) {
        when(i.U === io.addr) {
          regs(i) := io.data
        }.otherwise {
          regs(i) := regs(i)
        }
      }
    }.otherwise {
      for (i <- 0 until n) {
        io.sel(i) := inside(io.range0(i), io.range1(i))(io.addr)
      }
    }
  }
  // $onehot0 output encoding check
  assert(PopCount(io.sel) <= 1.U, "Invalid addr decoding")
}
