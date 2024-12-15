package tech.rocksavage.util

import java.io.{ByteArrayOutputStream, PrintWriter}
import sys.process._

object Util {
  def which(program: String): Option[String] = {
    val envPath = sys.env("PATH")
    val pathList = envPath.split(java.io.File.pathSeparator).toList
    // search with and without .exe on Windows
    val resultUnix = pathList.find(dir => new java.io.File(dir, program).canExecute).map(_ + "/" + program)
    val resultWindows = pathList.find(dir => new java.io.File(dir, program + ".exe").canExecute).map(_ + "\\" + program + ".exe")
    resultUnix.orElse(resultWindows)
  }


  def runCommand(cmd: Seq[String]): (Int, String, String) = {
    val stdoutStream = new ByteArrayOutputStream
    val stderrStream = new ByteArrayOutputStream
    val stdoutWriter = new PrintWriter(stdoutStream)
    val stderrWriter = new PrintWriter(stderrStream)
    val exitValue = cmd.!(ProcessLogger(stdoutWriter.println, stderrWriter.println))
    stdoutWriter.close()
    stderrWriter.close()
    (exitValue, stdoutStream.toString, stderrStream.toString)
  }
}
