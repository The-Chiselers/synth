package tech.rocksavage

import tech.rocksavage.args.Conf

object Main {
  def main(args_array: Array[String]) {
    val conf = new Conf(args_array)
    println("mode: " + conf.mode)
    println("module name: " + conf.moduleName)
  }
}