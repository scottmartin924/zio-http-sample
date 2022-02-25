package repository

import doobie.LogHandler
import doobie.util.log.{ExecFailure, ProcessingFailure, Success}
import org.slf4j.LoggerFactory

// FIXME I feel like we (in onedrop-api) must have a different approach to logging every statement? Maybe a common jdbc logger?
// To get logging for sql statements need to have this implicit loghandler anywhere the connectionio is created...that's not my favorite...probably something smarter
trait DoobieLogging {

  // This is just a simple logger
  implicit val logHandler: LogHandler = {
    val logger = LoggerFactory.getLogger(getClass.getName)
    LogHandler {
      case Success(s, a, e1, e2) =>
        logger.info(s"""Statement success: ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString(" ")}
                          | arguments = [${a.mkString(", ")}]
                          | elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (${(e1 + e2).toMillis} ms total)""".stripMargin)

      case ProcessingFailure(s, a, e1, e2, t) =>
        logger.error(s"""Failed Resultset Processing: ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString(" ")}
                            | arguments = [${a.mkString(", ")}]
                            | elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (failed) (${(e1 + e2).toMillis} ms total)
                            | failure = ${t.getMessage}""".stripMargin)

      case ExecFailure(s, a, e1, t) =>
        logger.error(s"""Failed Statement Execution: ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString(" ")}
                            | arguments = [${a.mkString(", ")}]
                            |   elapsed = ${e1.toMillis} ms exec (failed)
                            |   failure = ${t.getMessage}""".stripMargin)
    }
  }
}
