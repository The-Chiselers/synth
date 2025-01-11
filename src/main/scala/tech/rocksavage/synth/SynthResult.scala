package tech.rocksavage.synth

/**
 * The `SynthResult` class represents the result of a synthesis run, including the synthesized netlist,
 * Yosys output, and estimated gate count.
 *
 * @param synthString The synthesized netlist as a string.
 * @param stdout      The standard output from Yosys.
 * @param gates       The estimated number of equivalent NAND2 gates in the synthesized design.
 */
class SynthResult(synthString: String, stdout: String, gates: Option[Float]) {

  /**
   * Returns the synthesized netlist as a string.
   *
   * @return The synthesized netlist.
   */
  def getSynthString: String = synthString

  /**
   * Returns the standard output from Yosys.
   *
   * @return The Yosys output.
   */
  def getStdout: String = stdout

  /**
   * Returns the estimated number of equivalent NAND2 gates in the synthesized design.
   *
   * @return An optional float representing the gate count, or `None` if the gate count could not be estimated.
   */
  def getGates: Option[Float] = gates
}