// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.addrdecode

import chisel3._
import circt.stage.ChiselStage
import tech.rocksavage.synth.{Synth, SynthConfig, SynthResult}

/** An address decoder that can be used to decode addresses into a set of ranges
  *
  * @constructor
  *   Create a new address decoder
  * @param params
  *   BaseParams object including dataWidth and addressWidth
  * @param formal
  *   A boolean value to enable formal verification
  * @author
  *   Warren Savage
  */
class AddrDecode(
  params: AddrDecodeParams,
  formal: Boolean = false
) extends Module
    with Addressable {

  // ###################
  // Default Constructor
  // ###################

  def this() = {
    this(AddrDecodeParams(), false)
  }

  // ###################
  // Parameter checking & calculation
  // ###################

  val lengthSel: Int          = params.memorySizes.length
  var ranges: Seq[(Int, Int)] = Seq()
  for (i <- 0 until lengthSel) {
    if (i == 0) {
      ranges = ranges :+ (0, params.memorySizes(i) - 1)
    } else {
      ranges =
        ranges :+ (ranges(i - 1)._2 + 1, ranges(i - 1)._2 + params.memorySizes(i))
    }
  }

  val totalMemorySize: Int = params.memorySizes.sum

  require(ranges.nonEmpty, "At least one range must be provided")
  require(
      totalMemorySize <= math.pow(2, params.addressWidth),
      "Address space is not large enough to hold all ranges"
  )

  /** Returns the number of memory addresses used by the module
    *
    * @return
    *   The width of the memory
    */
  def memWidth(): Int = totalMemorySize

  val io = IO(new Bundle {
    val addr       = Input(UInt(params.addressWidth.W))
    val addrOffset = Input(UInt(params.addressWidth.W))
    val en         = Input(Bool())
    val selInput   = Input(Bool())

    val sel       = Output(Vec(lengthSel, Bool()))
    val addrOut   = Output(UInt(params.addressWidth.W))
    val errorCode = Output(AddrDecodeError())
    val errorAddr = Output(UInt(params.addressWidth.W))
  })

  /** Returns a vector of booleans representing the selected range
    *
    * @param addrRanges
    *   A sequence of tuples representing the start and end of each range
    * @param inputAddr
    *   The address to be decoded
    * @return
    *   A vector of booleans representing the selected range
    */
  def getSelect(
      addrRanges: Seq[(Int, Int)],
      inputAddr: UInt
  ): Vec[Bool] = {
    // declare sel
    val selOut = Wire(Vec(lengthSel, Bool()))
    // check if input_addr is in range
    var index: Int = 0
    while (index < lengthSel) {
      when(
        inputAddr >= addrRanges(
          index
        )._1.U && inputAddr <= addrRanges(
          index
        )._2.U
      ) {
        selOut(index) := true.B
      }.otherwise {
        selOut(index) := false.B
      }
      index += 1
    }
    return selOut
  }

  /** Returns the address output
    *
    * @param addrRanges
    *   A sequence of tuples representing the start and end of each range
    * @param inputAddr
    *   The address to be decoded
    * @return
    *   The address output
    */

  def getAddrOut(
      addrRanges: Seq[(Int, Int)],
      inputAddr: UInt
  ): UInt = {
    val addrOut = Wire(UInt(params.addressWidth.W))
    addrOut := 0.U

    for ((startAddr, endAddr) <- addrRanges) {
      when(
        inputAddr >= startAddr.U && inputAddr <= endAddr.U
      ) {
        addrOut := (inputAddr - startAddr.U)
      }
    }
    return addrOut
  }

  /** Returns a boolean value indicating if the address is in error
    *
    * @param rangeAddr
    *   A sequence of tuples representing the start and end of each range
    * @param inputAddr
    *   The address to be decoded
    * @return
    *   A boolean value indicating if the address is in error
    */
  def addrIsError(
      rangeAddr: Seq[(Int, Int)],
      inputAddr: UInt
  ): Bool = {
    val isErr = Wire(Bool())

    val minAddr: Int = rangeAddr.head._1
    val maxAddr: Int = rangeAddr.last._2

    isErr := false.B
    when(
      inputAddr < minAddr.U || inputAddr > maxAddr.U
    ) {
      isErr := true.B
    }
    return isErr
  }

  /** Returns the error address
    *
    * @param addrRanges
    *   A sequence of tuples representing the start and end of each range
    * @param inputAddr
    *   The address to be decoded
    * @param offsetAddr
    *   The offset address
    * @return
    *   The error address
    */

  def getErrorAddress(
      addrRanges: Seq[(Int, Int)],
      inputAddr: UInt,
      offsetAddr: UInt
  ): UInt = {
    val errorAddr = Wire(UInt(params.addressWidth.W))
    errorAddr := 0.U

    val minAddr: Int = addrRanges.head._1
    val maxAddr: Int = addrRanges.last._2

    when(
      inputAddr < minAddr.U || inputAddr > maxAddr.U
    ) {
      errorAddr := inputAddr + offsetAddr
    }

    return errorAddr
  }

  // ##########
  // Main logic
  // ##########
  /**
    * in this section, we take the results from the above functions and assign them to the output ports
    */
  private val isErr = Wire(Bool())
  private val addr  = io.addr - io.addrOffset
  private val en    = io.en

  io.sel       := VecInit(Seq.fill(lengthSel)(false.B))
  io.addrOut   := 0.U
  io.errorCode := AddrDecodeError.None
  io.errorAddr := 0.U

  isErr := 0.U

  when(en && io.selInput) {
    isErr := addrIsError(ranges, addr)
    when(isErr) {
      io.errorCode := AddrDecodeError.AddressOutOfRange
    }.otherwise {
      io.errorCode := AddrDecodeError.None
    }

    io.sel       := getSelect(ranges, addr)
    io.addrOut   := getAddrOut(ranges, addr)
    io.errorAddr := getErrorAddress(ranges, addr, io.addrOffset)
  }

  // ###################
  // Formal verification
  // ###################
  /**
    * The assertions being made here are:
    * - If the address is in range:
    *     - Exactly one of the sel vector is high
    *     - The address is decoded correctly from the relative start of the internal block
    *         - offset = 10, addr = 20, start_addr = 10, addr_out = 10
    *     - The error code is set to None
    *     - The error address is set to 0
    * - If the address is out of range:
    *     - The sel vector is all low
    *     - The address output is 0
    *     - The error code is set to AddressOutOfRange
    *     - The error address is set to the input address
    */
  if (formal) {
    when(en && io.selInput) {
      // ranges to
      for ((startAddr, endAddr) <- ranges) {
        when(
          addr >= startAddr.U && addr <= endAddr.U
        ) {
          assert(
            io.sel(ranges.indexOf((startAddr, endAddr))),
            "Invalid addr decoding"
          )
          assert(io.addrOut === addr - startAddr.U, "Invalid addr output")
          assert(io.errorCode === AddrDecodeError.None, "Invalid error code")
          assert(io.errorAddr === 0.U, "Invalid error address")
        }
      }
      val minAddr: Int = ranges.head._1
      val maxAddr: Int = ranges.last._2
      when(
        addr < minAddr.U || addr > maxAddr.U
      ) {
        // assert sel are all low
        assert(!io.sel.contains(true.B), "Invalid addr decoding")
        assert(io.addrOut === 0.U, "Invalid addr output")
        assert(
          io.errorCode === AddrDecodeError.AddressOutOfRange,
          "Invalid error code"
        )
        assert(io.errorAddr === io.addr, "Invalid error address")
      }

    }
  }

  assert(ranges.nonEmpty, "At least one range must be provided")
}

/** A main file to generate Verilog for an example address decoder
  */

object Main extends App {

  // ######### Getting Setup #########
  // get build root, if not set use null
  var output = sys.env.get("BUILD_ROOT")
  if (output == null || output.isEmpty) {
    println("BUILD_ROOT not set, please set and run again")
    System.exit(1)
  }
  // set output directory
  val outputUnwrapped = output.get
  val outputDir       = s"$outputUnwrapped/verilog"

  val dataWidth: Int = 32
  val addrWidth: Int = 32

  val configurations = Map(
    "8x8"   -> AddrDecodeParams(dataWidth, addrWidth, Seq.fill(8)(8)),
    "8x16"  -> AddrDecodeParams(dataWidth, addrWidth, Seq.fill(8)(16)),
    "8x32"  -> AddrDecodeParams(dataWidth, addrWidth, Seq.fill(8)(32)),
    "8x64"  -> AddrDecodeParams(dataWidth, addrWidth, Seq.fill(8)(64)),
    "16x8"  -> AddrDecodeParams(dataWidth, addrWidth, Seq.fill(16)(8)),
    "16x16" -> AddrDecodeParams(dataWidth, addrWidth, Seq.fill(16)(16)),
    "16x32" -> AddrDecodeParams(dataWidth, addrWidth, Seq.fill(16)(32)),
    "16x64" -> AddrDecodeParams(dataWidth, addrWidth, Seq.fill(16)(64)),
    "32x8"  -> AddrDecodeParams(dataWidth, addrWidth, Seq.fill(32)(8)),
    "32x16" -> AddrDecodeParams(dataWidth, addrWidth, Seq.fill(32)(16)),
    "32x32" -> AddrDecodeParams(dataWidth, addrWidth, Seq.fill(32)(32)),
    "32x64" -> AddrDecodeParams(dataWidth, addrWidth, Seq.fill(32)(64)),
    "64x8"  -> AddrDecodeParams(dataWidth, addrWidth, Seq.fill(64)(8)),
    "64x16" -> AddrDecodeParams(dataWidth, addrWidth, Seq.fill(64)(16)),
    "64x32" -> AddrDecodeParams(dataWidth, addrWidth, Seq.fill(64)(32)),
    "64x64" -> AddrDecodeParams(dataWidth, addrWidth, Seq.fill(64)(64))
  )

  // if output dir does not exist, make path
  val javaOutputDir = new java.io.File(outputDir)
  if (!javaOutputDir.exists) javaOutputDir.mkdirs

  // ######### Export to Files #########
  val files = scala.collection.mutable.Map[String, String]()
  for ((name, myParams) <- configurations) {
    ChiselStage.emitSystemVerilog(
      new AddrDecode(myParams),
      firtoolOpts = Array(
        "--lowering-options=disallowLocalVariables,disallowPackedArrays",
        "--disable-all-randomization",
        "--strip-debug-info",
        "--split-verilog",
        s"-o=$outputDir/$name/"
      )
    )
    val verilog =
      scala.io.Source.fromFile(s"$outputDir/$name/AddrDecode.sv").mkString
    files += (name -> verilog)
  }

  // Synth
  var SynthResults = scala.collection.mutable.Map[String, SynthResult]()
  for ((name, _) <- configurations) {
    val synth = new Synth(s"$outputDir/$name/", "AddrDecode")
    synth.requirements()
    val config = new SynthConfig("synth/stdcells.lib")

    SynthResults += (name -> synth.synth(config))

    // create directory and write file
    val synthDir = s"$outputUnwrapped/synth/$name"
    if (!new java.io.File(synthDir).exists) new java.io.File(synthDir).mkdirs
    val synthFile = new java.io.PrintWriter(s"$synthDir/AddrDecode_net.v")
    synthFile.write(SynthResults(name).file)
    synthFile.close()

    // write stdout
    val stdoutFile = new java.io.PrintWriter(s"$synthDir/stdout.txt")
    stdoutFile.write(SynthResults(name).stdout)
    stdoutFile.close()
  }

  // ##########################################
  System.exit(0)
}
