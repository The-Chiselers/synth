package tech.rocksavage

import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import tech.rocksavage.args.Conf

object Main {
  def main(args_array: Array[String]) {
    val conf = new Conf(args_array)

//    print(conf.subcommands)

    val newconf = new Conf(Seq("verilog", "--module", "foo"))
    newconf.verilog.module.get shouldBe Some("foo")



//    println("mode: " + conf.)
//    println("module name: " + conf.moduleName)
//
//    conf.mode() match {
//      case "verilog" => genVerilog(conf.moduleName())
//      case "synth" => println("Synthesizing")
//      case "sta" => println("Running Static Timing Analysis")
//      case _ => println("Unknown mode")
//    }
  }

  def genVerilog(moduleName: String): Unit = {
    println("Generating Verilog for " + moduleName)
  }
}

