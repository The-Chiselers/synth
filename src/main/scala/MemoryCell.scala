// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.AddressDecoder

import chisel3._
import chisel3.util._
import firrtl.PrimOps.Add

class MemoryCell(p: BaseParams, n: Int) extends Module with Addressable {
  val io = IO(new Bundle {
    val read_enable   = Input(Bool())
    val write_enable  = Input(Bool())
    val read_address  = Input(UInt(p.addressWidth.W))
    val write_address = Input(UInt(p.addressWidth.W))
    val write_data    = Input(UInt(p.dataWidth.W))
    val read_data     = Output(UInt(p.dataWidth.W))
  })

  def memWidth(): Int = n

  // ####################
  // Module Functionality
  // ####################

  val sram = Module(new SramBb(p))

  val sram_write_valid = io.write_address < n.U
  val sram_read_valid  = io.read_address < n.U

  // Map Sram Inputs
  sram.io.clk           := clock
  sram.io.read_enable   := io.read_enable && sram_read_valid
  sram.io.write_enable  := io.write_enable && sram_write_valid
  sram.io.read_address  := io.read_address
  sram.io.write_address := io.write_address
  sram.io.write_data    := io.write_data
  sram.io.read_data     := io.read_data
  // sram
}
