// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.AddrDecode

import chisel3._
import chisel3.util._

trait Addressable {
  def memWidth(): Int
}
