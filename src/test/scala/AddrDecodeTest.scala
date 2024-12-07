package tech.rocksavage.chiselware.AddressDecoder

import chisel3._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec
import chiseltest.ChiselScalatestTester
import chiseltest.formal.{BoundedCheck, Formal}

class AddrDecodeTest extends AnyFlatSpec
  with ChiselScalatestTester with Formal {

  "AddrDecode" should "pass" in {
    val p: BaseParams = new BaseParams(32, 32, 32)
    val sizes: Seq[Int] = Seq(1, 2, 3, 4, 5)
    verify(new AddrDecode(p, sizes), Seq(BoundedCheck(5)))
  }
}