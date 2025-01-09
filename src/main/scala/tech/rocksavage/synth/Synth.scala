package tech.rocksavage.synth

import chisel3.RawModule
import circt.stage.ChiselStage
import tech.rocksavage.util.Util.{runCommand, which}

import scala.sys.exit

object Synth {
  var nand2Area: Double = 0.798 // Nangate 45nm
  def setNand2Area(area: Double): Unit = {
    nand2Area = area
  }

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

  def synthesizeFromVerilogString(config: SynthConfig, topName: String, verilogString: String): SynthResult = {
    val tempTop = java.io.File.createTempFile(s"$topName", ".sv")
    val topPath = tempTop.getAbsolutePath
    val topFile = new java.io.PrintWriter(topPath)
    topFile.write(verilogString)
    topFile.close()

    val tempSynthOut = java.io.File.createTempFile(s"$topName"+"_net", ".v")
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

    println(stdout)
    println(stderr)
    println(exitCode)

    if (exitCode != 0) {
      println(s"Error running yosys command: $command, stdout: $stdout, stderr: $stderr")
      exit(1)
    }

    val synthFile = scala.io.Source.fromFile(synthOutPath)
    val synthString = synthFile.mkString
    synthFile.close()

    // Extract area and calculate gates

    val areaLine = stdout.split("\n").find(_.contains("Chip area"))
    val gates = areaLine match {
      case Some(line) => {
        val floatArea = line.split(":")(1).trim.toDouble
        val intArea = floatArea.toInt
        Some((intArea / nand2Area).toInt)
      }
      case None => None
    }

    new SynthResult(synthString, stdout, gates)
  }
}
