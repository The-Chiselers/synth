package tech.rocksavage.util

import java.io.{ByteArrayOutputStream, PrintWriter}
import scala.util.Using
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
    val stdout = new StringBuilder
    val stderr = new StringBuilder
    val logger = ProcessLogger(
      (line: String) => stdout.append(line + "\n"),  // Append newline to stdout
      (line: String) => stderr.append(line + "\n")   // Append newline to stderr
    )
    val status = cmd ! logger
    (status, stdout.toString, stderr.toString)
  }
}
