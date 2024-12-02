// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.AddressDecoder

import chisel3._
import chisel3.util._

/** Default parameter settings for the AddressDecoder
  *
  * @constructor
  *   default parameter settings
  * @param dataWidth
  *   specifies the width of the data bus
  * @param addressWidth
  *   specifies the width of the address bus
  * @author
  *   Warren Savage
  * @version 1.0
  *
  * @see
  *   [[http://www.rocksavage.tech]] for more information
  */
case class BaseParams(
    dataWidth: Int = 8,
    addressWidth: Int = 8,

    // module params
    max_delay: Int = 8,

    // Verilog Blackbox files
    bbFiles: List[String] = List("dual_port_sync_sram.v")
) {

  require(dataWidth >= 1, "Data Width must be greater than or equal 1")
  require(addressWidth >= 1, "Address Width must be greater than or equal 1")

  // module params
  require(max_delay >= 1, "Max Delay must be greater than or equal 1")

}
