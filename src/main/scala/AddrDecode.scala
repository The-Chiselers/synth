// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.AddressDecoder

import chisel3._
import chisel3.util._

// Source: https://github.com/chipsalliance/rocket-chip/issues/1668#issuecomment-433528365
class AddrDecode(
    p: BaseParams,
    sizes: Seq[Int]
) extends Module
    with Addressable {

  val len_sel: Int            = sizes.length
  val ranges: Seq[(Int, Int)] = sizes.zip(sizes.scanLeft(0)(_ + _).tail)
  val total_size: Int         = sizes.sum

  def memWidth(): Int = total_size

  val io = IO(new Bundle {
    val addr        = Input(UInt(p.addressWidth.W))
    val addr_offset = Input(UInt(p.addressWidth.W))
    val en          = Input(Bool())

    val sel           = Output(Vec(len_sel, Bool()))
    val addr_out      = Output(UInt(p.addressWidth.W))
    val error_code    = Output(AddrDecodeError())
    val error_address = Output(UInt(p.addressWidth.W))
  })

  def getSelect(
      range_addr: Seq[(Int, Int)],
      input_addr: UInt,
      offset_addr: UInt
  ): Vec[Bool] = {
    // declare sel
    val sel_out = Wire(Vec(len_sel, Bool()))
    // check if input_addr is in range
    var index: Int = 0
    while (index < len_sel) {
      when(
        input_addr >= range_addr(
          index
        )._1.U + offset_addr && input_addr <= range_addr(
          index
        )._2.U + offset_addr
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
      offset_addr: UInt
  ): UInt = {
    val addr_out = Wire(UInt(p.addressWidth.W))
    addr_out := 0.U

    for ((start_addr, end_addr) <- range_addr) {
      when(
        input_addr >= start_addr.U + offset_addr && input_addr <= end_addr.U + offset_addr
      ) {
        addr_out := input_addr - start_addr.U
      }
    }
    return addr_out
  }

  def addrIsError(
      range_addr: Seq[(Int, Int)],
      input_addr: UInt,
      offset_addr: UInt
  ): Bool = {
    val isErr = Wire(Bool())

    val min_addr: Int = range_addr(0)._1
    val max_addr: Int = range_addr.last._2

    isErr := false.B
    when(
      input_addr < min_addr.U + offset_addr || input_addr > max_addr.U + offset_addr
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

    val min_addr: Int = range_addr(0)._1
    val max_addr: Int = range_addr.last._2

    when(
      input_addr < min_addr.U + offset_addr || input_addr > max_addr.U + offset_addr
    ) {
      error_addr := input_addr
    }

    return error_addr
  }

  val isErr = Wire(Bool())

  val addr   = io.addr
  val offset = io.addr_offset
  val en     = io.en

  io.sel           := VecInit(Seq.fill(len_sel)(false.B))
  io.addr_out      := 0.U
  io.error_code    := AddrDecodeError.None
  io.error_address := 0.U

  isErr := 0.U

  when(en) {
    isErr := addrIsError(ranges, addr, offset)
    when(isErr) {
      io.error_code := AddrDecodeError.AddressOutOfRange
    }.otherwise {
      io.error_code := AddrDecodeError.None
    }

    io.sel           := getSelect(ranges, addr, offset)
    io.addr_out      := getAddrOut(ranges, addr, offset)
    io.error_address := getErrorAddress(ranges, addr, offset)
  }

  // $onehot0 output encoding check
  verification.assert(PopCount(io.sel) <= 1.U, "Invalid addr decoding")
  assert(ranges.length >= 1, "At least one range must be provided")
}
