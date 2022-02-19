package configuration

import zio.logging.LogAnnotation
import zio.logging.slf4j.Slf4jLogger

object LoggingConfiguration {

  private val logFormat = "[correlation-id = %s] %s"

  // FIXME This is how it's done in onedrop-api, but not sure it's right
  private val correleationId = LogAnnotation[String](
    name = "correlationId",
    initialValue = "undefined-correlation-id",
    combine = (_, newVal) => newVal,
    render = identity
  )
  val live = Slf4jLogger.makeWithAnnotationsAsMdc(List(correleationId))

//  val live = Slf4jLogger.make { (ctx, msg) =>
//    val correlationId = LogAnnotation.CorrelationId.render(ctx.get(LogAnnotation.CorrelationId))
//    logFormat.format(correlationId, msg)
//  }
}
