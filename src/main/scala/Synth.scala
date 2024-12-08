// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.addrdecode

import chisel3._

import sys.process._

// run instructions:
// sbt "runMain tech.rocksavage.chiselware.addrdecode."
class Synth(moduleName: String, moduleVerilogString: String) {
  def requirements(): Unit = {
    // require yosys is installed, send stdout to /dev/null
    val yosys = "yosys -h".!(ProcessLogger(stdout => ()))
    require(yosys == 0, "Yosys is not installed")
    // test if moduleVerilogString is valid
    require(moduleVerilogString.nonEmpty, "Module verilog string is empty")
  }

  // run synthesis with this config, capture the generated file and the stdout,
  // use temporary files to store the generated file

  // want to basically do what this script would do:
//   # Setting up synth.tcl
// echo "set top ${TOP}" >> ${BUILD_ROOT}/synth/synth.tcl
// echo "set techLib ${PROJECT_ROOT}/synth/stdcells.lib" >> ${BUILD_ROOT}/synth/synth.tcl
// echo "yosys -import" >> ${BUILD_ROOT}/synth/synth.tcl
// echo "set f [open ${BUILD_ROOT}/verilog/filelist.f]" >> ${BUILD_ROOT}/synth/synth.tcl
// echo "while {[gets \$f line] > -1} {" >> ${BUILD_ROOT}/synth/synth.tcl
// echo "  read_verilog -sv ${BUILD_ROOT}/verilog/\$line" >> ${BUILD_ROOT}/synth/synth.tcl
// echo "}" >> ${BUILD_ROOT}/synth/synth.tcl
// echo "close \$f" >> ${BUILD_ROOT}/synth/synth.tcl
// echo "hierarchy -check -top \$top" >> ${BUILD_ROOT}/synth/synth.tcl
// echo "synth -top \$top" >> ${BUILD_ROOT}/synth/synth.tcl
// echo "flatten" >> ${BUILD_ROOT}/synth/synth.tcl
// echo "dfflibmap -liberty \$techLib" >> ${BUILD_ROOT}/synth/synth.tcl
// echo "abc -liberty \$techLib" >> ${BUILD_ROOT}/synth/synth.tcl
// echo "opt_clean -purge" >> ${BUILD_ROOT}/synth/synth.tcl
// echo "write_verilog -noattr \$top\_net.v" >> ${BUILD_ROOT}/synth/synth.tcl
// echo "stat -liberty \$techLib" >> ${BUILD_ROOT}/synth/synth.tcl

// # Running Synthesis
// cd ${BUILD_ROOT}/synth
// mkdir -p ${BUILD_ROOT}/synth/
// yosys -Qv 1 ${BUILD_ROOT}/synth/synth.tcl -p "tee -o ${BUILD_ROOT}/synth/synth.rpt stat"

  def synth(config: SynthConfig): SynthResult = {
    val tempModule     = java.io.File.createTempFile("module", ".v")
    val tempModulePath = tempModule.getAbsolutePath
    val tempModuleFile = new java.io.PrintWriter(tempModulePath)
    tempModuleFile.write(moduleVerilogString)
    tempModuleFile.close()

    val techLibPath = config.techlibPath

    val tempSynthFile = java.io.File.createTempFile("module_net", ".v")
    val tempSynthPath = tempSynthFile.getAbsolutePath

    val tempStdoutFile = java.io.File.createTempFile("stdout", ".txt")
    val tempStdoutPath = tempStdoutFile.getAbsolutePath

    val tempTcl     = java.io.File.createTempFile("synth", ".tcl")
    val tempTclPath = tempTcl.getAbsolutePath
    val tempTclFile = new java.io.PrintWriter(tempTclPath)
    tempTclFile.write(s"set top $moduleName\n")
    tempTclFile.write(s"set techLib $techLibPath\n")
    tempTclFile.write("yosys -import\n")
    tempTclFile.write(s"read_verilog -sv $tempModulePath\n")
    tempTclFile.write("hierarchy -check -top $top\n")
    tempTclFile.write("synth -top $top\n")
    tempTclFile.write("flatten\n")
    tempTclFile.write("dfflibmap -liberty $techLib\n")
    tempTclFile.write("abc -liberty $techLib\n")
    tempTclFile.write("opt_clean -purge\n")

    tempTclFile.write(s"write_verilog -noattr $tempSynthPath\n")
    tempTclFile.write("stat -liberty $techLib\n")
    tempTclFile.close()

    // run the synthesis and capture the stdout
    val cmd = s"yosys -Qv 1 $tempTclPath -p 'tee -o $tempStdoutPath stat'"
    // print(cmd)
    val exitCode = cmd.!(ProcessLogger(stdout => ()))

    // check if the synthesis was successful
    if (exitCode != 0) {
      throw new Exception(s"Yosys failed with exit code $exitCode")
    }

    // read the generated file
    val synthFile   = scala.io.Source.fromFile(tempSynthPath)
    val synthString = synthFile.mkString
    synthFile.close()

    val stdoutFile = scala.io.Source.fromFile(tempStdoutPath)
    val stdout     = stdoutFile.mkString
    stdoutFile.close()

    // return the result
    new SynthResult(synthString, stdout)
  }

}
