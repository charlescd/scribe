package scribe.writer

import scribe.Platform._
import scribe._
import scribe.output._
import scribe.output.format.OutputFormat

import scala.collection.mutable.ListBuffer
import scala.scalajs.js

object BrowserConsoleWriter extends Writer {
  val args: ListBuffer[String] = ListBuffer.empty

  override def write[M](record: LogRecord[M], output: LogOutput, outputFormat: OutputFormat): Unit = {
    val b = new StringBuilder
    args.clear()
    outputFormat(output, b.append(_))

    val jsArgs = args.map(js.Any.fromString).toList

    if (record.level >= Level.Error) {
      console.error(b.toString(), jsArgs: _*)
    } else if (record.level >= Level.Warn) {
      console.warn(b.toString(), jsArgs: _*)
    } else {
      console.log(b.toString(), jsArgs: _*)
    }
  }
}