package tech.rocksavage.chiselware.addrdecode

import chiseltest.formal.{BoundedCheck, Formal}
import chiseltest.ChiselScalatestTester
import org.scalatest.flatspec.AnyFlatSpec

class AddrDecodeTest
    extends AnyFlatSpec
    with ChiselScalatestTester
    with Formal {

  "AddrDecode" should "pass" in {

    val addrWidth: Int  = 32
    val dataWidth: Int  = 32
    val sizes: Seq[Int] = Seq.fill(64)(64)

    val p = BaseParams(dataWidth, addrWidth, sizes)

    verify(new AddrDecode(p, true), Seq(BoundedCheck(30)))

  }
}
