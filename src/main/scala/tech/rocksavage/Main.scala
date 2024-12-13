package tech.rocksavage

import tech.rocksavage.args.Conf

object Main {
  def main(args_array: Array[String]) {
    val conf = new Conf(args_array)
    conf.subcommand match {
      case Some(conf.verilog) => {
        println("Generating Verilog for " + conf.verilog.module())
      }
      case Some(conf.synth) => {
        println("Synthesizing " + conf.synth.module())
      }
      case _ => {
        println("No subcommand given")
      }
    }
  }

  def genVerilog(moduleName: String): Unit = {
    println("Generating Verilog for " + moduleName)
  }
}

