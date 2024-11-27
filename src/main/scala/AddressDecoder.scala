package tech.rocksavage.chiselware.AddressDecoder

import chisel3._
import chisel3.util._

class AddressDecoder(decodeMap: Vector[Addressable]) {

  // val decodeMap: Vec[Addressable]

  def decode(address: UInt, data: UInt): Unit = {
    var count: Int = 0;
    // iterate through addressable and increment count each time, if address - count is less than or equal to size, then call closure and break
    for (addressable <- decodeMap) {
      when(address - count.U <= addressable.size.U) {
        addressable.closure(address - count.U, data)
        return
      }
      count += addressable.size
    }
  }
}
