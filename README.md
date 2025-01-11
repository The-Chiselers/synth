# Chisel Synthesis Library

## Overview

The **Chisel Synthesis Library** is a Scala-based library designed to simplify the process of running synthesis for Chisel hardware designs directly from within Scala. It provides a seamless integration between Chisel hardware description language (HDL) and synthesis tools like Yosys, enabling users to generate Verilog, synthesize designs, and analyze synthesis results programmatically. This library is particularly useful for hardware designers who want to automate synthesis workflows, evaluate design metrics (e.g., area, gate count), and integrate synthesis into their Scala-based design pipelines.

---

## Features

- **Chisel to Verilog Conversion**: Automatically generate Verilog from Chisel modules using the `ChiselStage` API.
- **Synthesis Workflow Automation**: Define and execute synthesis workflows using a configurable `SynthConfig` object.
- **Yosys Integration**: Seamlessly integrate with Yosys for synthesis, optimization, and area estimation.
- **Temporary File Management**: Handle temporary Verilog files, TCL scripts, and synthesis outputs without manual intervention.
- **Area and Gate Count Estimation**: Extract chip area and estimate gate count based on a configurable NAND2 equivalent area.
- **Scalable and Extensible**: Easily extend the library to support additional synthesis tools or custom synthesis commands.

---

## Usage

### 1. **Defining a Synthesis Configuration**

To define a synthesis configuration, create an instance of `SynthConfig` with the path to your technology library and a list of synthesis commands. The library provides a set of predefined synthesis commands (e.g., `SynthCommand.Synth`, `SynthCommand.Flatten`, `SynthCommand.Abc`).

```scala
val synthCommands = List(
  SynthCommand.Synth,
  SynthCommand.Flatten,
  SynthCommand.Dfflibmap,
  SynthCommand.Abc,
  SynthCommand.OptCleanPurge,
  SynthCommand.Write,
  SynthCommand.Stat
)

val synthConfig = new SynthConfig("/path/to/techlib.lib", synthCommands)
```

### 2. **Running Synthesis**

Use the `synthesizeFromModuleName` method to synthesize a Chisel module. Provide the module name, synthesis configuration, and any module parameters.

```scala
val synthResult = Synth.synthesizeFromModuleName(
  synthConfig,
  "tech.rocksavage.chiselware.timer.Timer",
  TimerParams()
)
```

### 3. **Accessing Synthesis Results**

The `SynthResult` object contains the synthesized Verilog, synthesis logs, and estimated gate count.

```scala
val synthesizedVerilog = synthResult.getSynthString
val synthesisLogs = synthResult.getStdout
val gateCount = synthResult.getGates.getOrElse("No gate count available")
```

### 4. **Writing Results to Files**

The library provides utility methods to write synthesis results to files for further analysis or integration into larger workflows.

```scala
writeFile("output/netlist.v", synthesizedVerilog)
writeFile("output/log.txt", synthesisLogs)
writeFile("output/gates.txt", gateCount.toString)
```

---

## Configuration Options

### `SynthConfig`

- **`techlibPath`**: Path to the technology library file (e.g., `.lib`).
- **`commands`**: List of synthesis commands to execute (e.g., `SynthCommand.Synth`, `SynthCommand.Flatten`).

### `SynthCommand`

Predefined synthesis commands include:
- `Synth`: Run synthesis.
- `Flatten`: Flatten the design hierarchy.
- `Dfflibmap`: Map flip-flops to the technology library.
- `Abc`: Run ABC for technology mapping.
- `OptCleanPurge`: Optimize and clean the design.
- `Write`: Write the synthesized netlist to a file.
- `Stat`: Generate statistics (e.g., area, gate count).

---

## Dependencies

- **Chisel**: The library relies on Chisel for hardware design and Verilog generation.
- **Yosys**: Yosys is used for synthesis and optimization. Ensure Yosys is installed and available in your system's `PATH`.
- **Scala**: The library is written in Scala and requires a compatible Scala environment.

---

## License

This code is licensed under the **Apache Software License 2.0**. See the [LICENSE](LICENSE) file for details.
