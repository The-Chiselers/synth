package tech.rocksavage.chiselware.AddressDecoder

import chisel3._
import chisel3.util._

// trait AddressIO extends Bundle {
//   val addr        = Input
//   val data        = Input
// }

trait AddressableModule extends Module {
  val decoder: AddressDecoder

  def 
}
