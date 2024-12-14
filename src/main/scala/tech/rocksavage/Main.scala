package tech.rocksavage

import chisel3._

import java.io.File
import tech.rocksavage.args.Conf
import tech.rocksavage.chiselware.addrdecode.AddrDecode

import scala.sys.exit

object Main {
  def main(args_array: Array[String]) {
    val conf = new Conf(args_array)
    conf.subcommand match {
      case Some(conf.verilog) => {
        val verilogString = genVerilog(conf.verilog.module())
        conf.verilog.mode() match {
          case "print" => {
            println(verilogString)
          }
          case "write" => {
            val filename = conf.verilog.module() + ".sv"
            // write to file
            def f = new File(filename)
            val bw = new java.io.BufferedWriter(new java.io.FileWriter(f))
            bw.write(verilogString)
          }
        }
      }
      case Some(conf.synth) => {
        println("Synthesizing " + conf.synth.module())
      }
      case _ => {
        println("No subcommand given")
      }
    }
  }

  def genVerilog(moduleName: String, params: Any*): String = {

    val clazz = Class.forName(moduleName).asSubclass(classOf[RawModule])
    val constructors = clazz.getConstructors
    for (c <- constructors) {
      println(params)
      println(s"constructor: $c")
    }
    // Generate Verilog
    val verilog = emitVerilog(constructors(1).newInstance().asInstanceOf[RawModule])

    verilog
  }
}

