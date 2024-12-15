// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.synth

import chisel3.{RawModule, getVerilogString}
import circt.stage.ChiselStage
import tech.rocksavage.util.Util.{runCommand, which}

import scala.sys.exit
import scala.sys.process._

object Synth {
  def genVerilog(moduleName: String, params: Any*): String = {
    val clazz = Class.forName(moduleName).asSubclass(classOf[RawModule])
    val constructors = clazz.getConstructors
    var verilog = ""
    for (c <- constructors) {
      try {
        verilog = getVerilogString(c.newInstance(params: _*).asInstanceOf[RawModule])
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
        verilog = getVerilogString(c.newInstance(params: _*).asInstanceOf[RawModule])
      } catch {
        case e: java.lang.IllegalArgumentException => {
          println("Constructor " + c + " failed: " + e)
        }
      }
    }
    synthesize(className, verilog, synthConfig)
  }

  def synthesize(topName: String, verilogString: String, config: SynthConfig): SynthResult = {
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
      case None =>
        println("Yosys not found in PATH")
        exit(1)
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
    val synthString = synthFile.mkString
    synthFile.close()

    new SynthResult(synthString, stdout)
  }
}
