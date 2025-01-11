package tech.rocksavage.synth

import chisel3.RawModule
import circt.stage.ChiselStage

/**
 * The `SynthConfig` class represents a synthesis configuration, including the path to the technology library
 * and a list of synthesis commands to be executed by Yosys.
 *
 * @param techlibPath The path to the technology library file.
 * @param commands    A list of synthesis commands to be executed by Yosys.
 */
class SynthConfig(val techlibPath: String, val commands: List[SynthCommand]) {
  require(new java.io.File(techlibPath).exists, s"Technology library $techlibPath does not exist")

  /**
   * Converts the synthesis configuration into a TCL script string for Yosys.
   *
   * @param top     The name of the top-level module.
   * @param topPath The path to the top-level Verilog file.
   * @param outPath The path to the output netlist file.
   * @return A TCL script string that can be executed by Yosys.
   */
  def toString(top: String, topPath: String, outPath: String): String = {
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

    commandsStrList.mkString("\n")
  }
}

/**
 * A sealed trait representing a synthesis command that can be executed by Yosys.
 */
sealed trait SynthCommand {

  /**
   * Converts the synthesis command into a Yosys command string.
   *
   * @param top         The name of the top-level module.
   * @param topPath     The path to the top-level Verilog file.
   * @param techlib     The path to the technology library file.
   * @param output_path The path to the output netlist file.
   * @return A Yosys command string.
   */
  def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String
}

/**
 * Companion object for `SynthCommand` containing predefined synthesis commands.
 */
object SynthCommand {

  /**
   * Represents the `synth` command in Yosys, which performs synthesis.
   */
  case object Synth extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String =
      s"synth -top $top"
  }

  /**
   * Represents the `flatten` command in Yosys, which flattens the design hierarchy.
   */
  case object Flatten extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String =
      "flatten"
  }

  /**
   * Represents the `dfflibmap` command in Yosys, which maps flip-flops to the target technology library.
   */
  case object Dfflibmap extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String =
      s"dfflibmap -liberty $techlib"
  }

  /**
   * Represents the `abc` command in Yosys, which performs technology mapping using ABC.
   */
  case object Abc extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String =
      s"abc -liberty $techlib"
  }

  /**
   * Represents the `opt` command in Yosys, which performs optimization.
   */
  case object Opt extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String =
      "opt"
  }

  /**
   * Represents the `clean` command in Yosys, which removes unused cells and wires.
   */
  case object Clean extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String =
      "clean"
  }

  /**
   * Represents the `opt_clean -purge` command in Yosys, which performs aggressive optimization and cleanup.
   */
  case object OptCleanPurge extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String =
      "opt_clean -purge"
  }

  /**
   * Represents the `stat` command in Yosys, which prints statistics about the design.
   */
  case object Stat extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String =
      s"stat -liberty $techlib"
  }

  /**
   * Represents the `write_verilog` command in Yosys, which writes the synthesized netlist to a file.
   */
  case object Write extends SynthCommand {
    override def toCmdString(top: String, topPath: String, techlib: String, output_path: String): String =
      s"write_verilog -noattr $output_path"
  }
}