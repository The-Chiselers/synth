// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.AddrDecode

import chisel3._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec
import chiseltest.ChiselScalatestTester
import chiseltest.formal.{BoundedCheck, Formal, past}

// Source: https://github.com/chipsalliance/rocket-chip/issues/1668#issuecomment-433528365
class AddrDecode(
    p: BaseParams,
    sizes: Seq[Int],
    formal: Boolean = false
) extends Module
    with Addressable {

  val len_sel: Int            = sizes.length
  var ranges: Seq[(Int, Int)] = Seq()
  for (i <- 0 until len_sel) {
    if (i == 0) {
      ranges = ranges :+ (0, sizes(i) - 1)
    } else {
      ranges = ranges :+ (ranges(i - 1)._2 + 1, ranges(i - 1)._2 + sizes(i))
    }
  }

  val total_size: Int         = sizes.sum

  def memWidth(): Int = total_size

  val io = IO(new Bundle {
    val addr        = Input(UInt(p.addressWidth.W))
    val addr_offset = Input(UInt(p.addressWidth.W))
    val en          = Input(Bool())
    val sel_i       = Input(Bool())

    val sel           = Output(Vec(len_sel, Bool()))
    val addr_out      = Output(UInt(p.addressWidth.W))
    val error_code    = Output(AddrDecodeError())
    val error_address = Output(UInt(p.addressWidth.W))
  })

  def getSelect(
      range_addr: Seq[(Int, Int)],
      input_addr: UInt,
  ): Vec[Bool] = {
    // declare sel
    val sel_out = Wire(Vec(len_sel, Bool()))
    // check if input_addr is in range
    var index: Int = 0
    while (index < len_sel) {
      when(
        input_addr >= range_addr(
          index
        )._1.U && input_addr <= range_addr(
          index
        )._2.U
      ) {
        sel_out(index) := true.B
      }.otherwise {
        sel_out(index) := false.B
      }
      index += 1
    }
    return sel_out
  }

  def getAddrOut(
      range_addr: Seq[(Int, Int)],
      input_addr: UInt,
  ): UInt = {
    val addr_out = Wire(UInt(p.addressWidth.W))
    addr_out := 0.U

    for ((start_addr, end_addr) <- range_addr) {
      when(
        input_addr >= start_addr.U  && input_addr <= end_addr.U
      ) {
        addr_out := (input_addr - start_addr.U)
      }
    }
    return addr_out
  }

  def addrIsError(
      range_addr: Seq[(Int, Int)],
      input_addr: UInt
  ): Bool = {
    val isErr = Wire(Bool())

    val min_addr: Int = range_addr.head._1
    val max_addr: Int = range_addr.last._2

    isErr := false.B
    when(
      input_addr < min_addr.U || input_addr > max_addr.U
    ) {
      isErr := true.B
    }
    return isErr
  }

  // def getErrorCode(
  //     range_addr: Seq[(Int, Int)],
  //     input_addr: UInt,
  //     offset_addr: UInt
  // ): ChiselEnum = {
  //   for ((start_addr, end_addr) <- range_addr) {
  //     when(
  //       input_addr >= start_addr.U + offset_addr && input_addr <= end_addr.U + offset_addr
  //     ) {
  //       return AddressDecoderError.None
  //     }
  //   }
  //   return AddressDecoderError.AddressOutOfRange
  // }

  def getErrorAddress(
      range_addr: Seq[(Int, Int)],
      input_addr: UInt,
      offset_addr: UInt
  ): UInt = {
    val error_addr = Wire(UInt(p.addressWidth.W))
    error_addr := 0.U

    val min_addr: Int = range_addr.head._1
    val max_addr: Int = range_addr.last._2

    when(
      input_addr < min_addr.U || input_addr > max_addr.U
    ) {
      error_addr := input_addr + offset_addr
    }

    return error_addr
  }

  val isErr = Wire(Bool())

  val addr   = io.addr - io.addr_offset
//  val offset = io.addr_offset
  val en     = io.en

  io.sel           := VecInit(Seq.fill(len_sel)(false.B))
  io.addr_out      := 0.U
  io.error_code    := AddrDecodeError.None
  io.error_address := 0.U

  isErr := 0.U

  when(en && io.sel_i) {
    isErr := addrIsError(ranges, addr)
    when(isErr) {
      io.error_code := AddrDecodeError.AddressOutOfRange
    }.otherwise {
      io.error_code := AddrDecodeError.None
    }

    io.sel           := getSelect(ranges, addr)
    io.addr_out      := getAddrOut(ranges, addr)
    io.error_address := getErrorAddress(ranges, addr, io.addr_offset)
  }

  if (formal) {
    when(en && io.sel_i) {
      // ranges to
      for ((start_addr, end_addr) <- ranges) {
        when(
          addr >= start_addr.U && addr <= end_addr.U
        ) {
          assert(io.sel(ranges.indexOf((start_addr, end_addr))), "Invalid addr decoding")
          assert(io.addr_out === addr - start_addr.U, "Invalid addr output")
          assert(io.error_code === AddrDecodeError.None, "Invalid error code")
          assert(io.error_address === 0.U, "Invalid error address")
        }
      }
      val min_addr: Int = ranges.head._1
      val max_addr: Int = ranges.last._2
      when(
          addr < min_addr.U || addr > max_addr.U
      ) {
          // assert sel are all low
          assert(!io.sel.contains(true.B), "Invalid addr decoding")
          assert(io.addr_out === 0.U, "Invalid addr output")
          assert(io.error_code === AddrDecodeError.AddressOutOfRange, "Invalid error code")
          assert(io.error_address === io.addr, "Invalid error address")
      }

    }
  }

  assert(ranges.nonEmpty, "At least one range must be provided")
}
