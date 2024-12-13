package tech.rocksavage

import tech.rocksavage.args.Conf

object Main {
  def main(args_array: Array[String]) {
    val conf = new Conf(args_array)
  }

  def genVerilog(moduleName: String): Unit = {
    println("Generating Verilog for " + moduleName)
  }
}

