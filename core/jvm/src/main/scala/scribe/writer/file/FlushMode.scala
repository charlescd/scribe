package scribe.writer.file

import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

import scribe.util.Time

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

trait FlushMode {
  def dataWritten(logFile: LogFile, writer: LogFileWriter): Unit
}

object FlushMode {
  object NeverFlush extends FlushMode {
    override def dataWritten(logFile: LogFile, writer: LogFileWriter): Unit = {}
  }

  object AlwaysFlush extends FlushMode {
    override def dataWritten(logFile: LogFile, writer: LogFileWriter): Unit = writer.flush()
  }

  case class AsynchronousFlush(delay: FiniteDuration = 1.second) extends FlushMode {
    private lazy val delayMillis = delay.toMillis
    private lazy val flushing = new AtomicBoolean(false)
    private lazy val dirty = new AtomicBoolean(false)
    private lazy val lastFlush = new AtomicLong(0L)
    private var logFile: LogFile = _

    override def dataWritten(logFile: LogFile, writer: LogFileWriter): Unit = {
      this.logFile = logFile
      if (flushing.compareAndSet(false, true)) {
        flush()
      } else {
        dirty.set(true)
      }
    }

    private def flush(): Unit = Future {
      try {
        val delay = this.delayMillis - (Time() - lastFlush.get())
        if (delay > 0L) {
          Thread.sleep(delay)
        }
        logFile.flush()
      } finally {
        lastFlush.set(Time())
        if (dirty.compareAndSet(true, false)) {
          flush()
        } else {
          flushing.set(false)
        }
      }
    }
  }
}