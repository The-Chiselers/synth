package tech.rocksavage.synth

import chisel3._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TestModule extends Module {
  val in = IO(Input(Bool()))
  val out = IO(Output(Bool()))
  out := in
}

class SynthSpec extends AnyFlatSpec with Matchers {

  behavior of "Synth"

  it should "generate Verilog from module name" in {
    val moduleName = "tech.rocksavage.synth.TestModule"
    val expectedVerilog =
      """module TestModule(
        |  input  clock,
        |         reset,
        |         in,
        |  output out
        |);
        |
        |  assign out = in;
        |endmodule""".stripMargin

    val result = Synth.genVerilogFromModuleName(moduleName)

    result should include (expectedVerilog)
  }

  it should "synthesize from module name" in {
    val moduleName = "tech.rocksavage.synth.TestModule"
    val expectedModuleDec = "module TestModule(clock, reset, in, out);"
    val expectedAssign = "assign out = in;"

    val commands = List(
      tech.rocksavage.synth.SynthCommand.Synth,
      tech.rocksavage.synth.SynthCommand.Flatten,
      tech.rocksavage.synth.SynthCommand.Dfflibmap,
      tech.rocksavage.synth.SynthCommand.Abc,
      tech.rocksavage.synth.SynthCommand.Opt,
      tech.rocksavage.synth.SynthCommand.Clean,
      tech.rocksavage.synth.SynthCommand.Stat
    )
    val config = new SynthConfig("synth/stdcells.lib", commands)
    val result = Synth.synthesizeFromModuleName(config, moduleName).getSynthString

    result should include (expectedModuleDec)
    result should include (expectedAssign)
  }

  it should "synthesize from verilog string" in {
    val moduleName = "TestModule"
    val verilogString =
      """module TestModule(
        |  input  clock,
        |         reset,
        |         in,
        |  output out
        |);
        |
        |  assign out = in;
        |endmodule
        |
        |""".stripMargin

    val expectedModuleDec = "module TestModule(clock, reset, in, out);"
    val expectedAssign = "assign out = in;"

    val commands = List(
        tech.rocksavage.synth.SynthCommand.Synth,
        tech.rocksavage.synth.SynthCommand.Flatten,
        tech.rocksavage.synth.SynthCommand.Dfflibmap,
        tech.rocksavage.synth.SynthCommand.Abc,
        tech.rocksavage.synth.SynthCommand.Opt,
        tech.rocksavage.synth.SynthCommand.Clean,
        tech.rocksavage.synth.SynthCommand.Stat
        )
    val config = new SynthConfig("synth/stdcells.lib", commands)
    val result = Synth.synthesizeFromVerilogString(config, moduleName, verilogString).getSynthString

    result should include (expectedModuleDec)
    result should include (expectedAssign)

  }
}