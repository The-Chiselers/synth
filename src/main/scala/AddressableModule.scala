package tech.rocksavage.chiselware.AddressDecoder

import chisel3._
import chisel3.util._

trait AddressableModule extends Module {
  val addressDecoder: AddressDecoder
}
