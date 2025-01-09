// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.synth

import chisel3.RawModule
import circt.stage.ChiselStage

class SynthConfig(val techlibPath: String, val commands: List[SynthCommand]) {
  require(new java.io.File(techlibPath).exists, s"Technology library $techlibPath does not exist")

  def toString(top: String, topPath: String, outPath: String): String = {

    // make sure \ paths are escaped for windows

    val techlibPathEscaped = techlibPath.replace("\\", "\\\\")
    val topPathEscaped = topPath.replace("\\", "\\\\")
    val outPathEscaped = outPath.replace("\\", "\\\\")

    val commandsStrList = List(
      s"set top $top",
      s"set techLib $techlibPathEscaped",
      "yosys -import",
      s"read_verilog -sv $topPathEscaped",
      s"hierarchy -check -top $top"
    ) ++ commands.map(c => c.toCmdString(top, topPathEscaped, techlibPathEscaped, outPathEscaped))
    //      s"write_verilog -noattr $outPathEscaped" :+
    //      s"stat -liberty synth/stdcells.lib"

    commandsStrList.mkString("\n")
  }
}

sealed trait SynthCommand {
  def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String
}

object SynthCommand {
  case object Synth extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String = s"synth -top $top"
  }
  case object Flatten extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String = "flatten"
  }
  case object Dfflibmap extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String = s"dfflibmap -liberty $techlib"
  }
  case object Abc extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String = s"abc -liberty $techlib"
  }
  case object Opt extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String = "opt"
  }
  case object Clean extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String = "clean"
  }
  case object OptCleanPurge extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String = "opt_clean -purge"
  }
  case object Stat extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String = s"stat -liberty $techlib"
  }
  case object Write extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String = s"write_verilog -noattr $output_path"
  }
}