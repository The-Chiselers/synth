package tech.rocksavage.args

import org.rogach.scallop.ScallopConf



class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val valid_modes = List("verilog", "synth", "sta")
  val mode = opt[String](default = Some("verilog"), validate = valid_modes.contains(_))
  val moduleName = trailArg[String]()
  verify()
}