package tech.rocksavage.synth

import chisel3.RawModule
import circt.stage.ChiselStage
import tech.rocksavage.util.Util.{runCommand, which}
import scala.sys.exit

/**
 * The `Synth` object provides utilities for generating Verilog from Chisel modules and synthesizing them using Yosys.
 * It supports setting the area of a NAND2 gate for area calculations and provides methods to generate Verilog and
 * perform synthesis from a module name or Verilog string.
 */
object Synth {

  /**
   * The area of a NAND2 gate in the target technology library (default is Nangate 45nm).
   * This value is used to estimate the number of equivalent NAND2 gates in the synthesized design.
   */
  var nand2Area: Double = 0.798 // Nangate 45nm

  /**
   * Sets the area of a NAND2 gate for area calculations.
   *
   * @param area The area of a NAND2 gate in the target technology library.
   */
  def setNand2Area(area: Double): Unit = {
    nand2Area = area
  }

  /**
   * Generates Verilog from a Chisel module specified by its class name.
   *
   * @param moduleName The fully qualified class name of the Chisel module.
   * @param params     The parameters to pass to the module's constructor.
   * @return The generated Verilog as a string.
   */
  def genVerilogFromModuleName(moduleName: String, params: Any*): String = {
    val clazz = Class.forName(moduleName).asSubclass(classOf[RawModule])
    val constructors = clazz.getConstructors
    var verilog = ""
    for (c <- constructors) {
      try {
        verilog = ChiselStage.emitSystemVerilog(c.newInstance(params: _*).asInstanceOf[RawModule], firtoolOpts = Array(
          "--lowering-options=disallowLocalVariables,disallowPackedArrays",
          "--disable-all-randomization",
          "--strip-debug-info",
        ))
      } catch {
        case e: java.lang.IllegalArgumentException => {
          println("Constructor " + c + " failed: " + e)
        }
      }
    }
    verilog
  }

  /**
   * Synthesizes a Chisel module specified by its class name using Yosys.
   *
   * @param synthConfig The synthesis configuration specifying the technology library and commands.
   * @param moduleName  The fully qualified class name of the Chisel module.
   * @param params      The parameters to pass to the module's constructor.
   * @return A `SynthResult` object containing the synthesized netlist, Yosys output, and estimated gate count.
   */
  def synthesizeFromModuleName(synthConfig: SynthConfig, moduleName: String, params: Any*): SynthResult = {
    val clazz = Class.forName(moduleName).asSubclass(classOf[RawModule])
    val constructors = clazz.getConstructors
    val className = clazz.getName.split('.').last
    var verilog = ""
    for (c <- constructors) {
      try {
        verilog = ChiselStage.emitSystemVerilog(c.newInstance(params: _*).asInstanceOf[RawModule], firtoolOpts = Array(
          "--lowering-options=disallowLocalVariables,disallowPackedArrays",
          "--disable-all-randomization",
          "--strip-debug-info",
        ))
      } catch {
        case e: java.lang.IllegalArgumentException => {
          println("Constructor " + c + " failed: " + e)
        }
      }
    }
    synthesizeFromVerilogString(synthConfig, className, verilog)
  }

  /**
   * Synthesizes a design from a Verilog string using Yosys.
   *
   * @param config        The synthesis configuration specifying the technology library and commands.
   * @param topName       The name of the top-level module in the Verilog design.
   * @param verilogString The Verilog design as a string.
   * @return A `SynthResult` object containing the synthesized netlist, Yosys output, and estimated gate count.
   */
  def synthesizeFromVerilogString(config: SynthConfig, topName: String, verilogString: String): SynthResult = {
    val tempTop = java.io.File.createTempFile(s"$topName", ".sv")
    val topPath = tempTop.getAbsolutePath
    val topFile = new java.io.PrintWriter(topPath)
    topFile.write(verilogString)
    topFile.close()

    val tempSynthOut = java.io.File.createTempFile(s"$topName" + "_net", ".v")
    val synthOutPath = tempSynthOut.getAbsolutePath

    val configString = config.toString(topName, topPath, synthOutPath)
    val tempTcl = java.io.File.createTempFile("synth", ".tcl")
    val tempTclPath = tempTcl.getAbsolutePath
    val tempTclFile = new java.io.PrintWriter(tempTclPath)
    tempTclFile.write(configString)
    tempTclFile.close()

    val yosysPath = which("yosys") match {
      case Some(path) => path
      case None => println("Yosys not found in PATH"); exit(1)
    }

    val command = Seq(yosysPath, tempTclPath)
    val res = runCommand(command)
    val exitCode = res._1
    val stdout = res._2
    val stderr = res._3

    if (exitCode != 0) {
      println(s"Error running yosys command: $command, stdout: $stdout, stderr: $stderr")
      exit(1)
    }

    val synthFile = scala.io.Source.fromFile(synthOutPath)
    val synthString = synthFile.getLines.mkString("\n")
    synthFile.close()

    val areaLine = stdout.split("\n").find(_.contains("Chip area"))
    val gates = areaLine match {
      case Some(line) => {
        val floatArea = line.split(":")(1).trim.toDouble
        val intArea = floatArea.toInt
        Some((intArea / nand2Area).toFloat)
      }
      case None => None
    }

    new SynthResult(synthString, stdout, gates)
  }
}