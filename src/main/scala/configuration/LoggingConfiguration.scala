package configuration

import zio.logging.LogAnnotation
import zio.logging.slf4j.Slf4jLogger

object LoggingConfiguration {

  private val logFormat = "[correlation-id = %s, user-id = %s] %s"

  // FIXME This is how it's done in onedrop-api, but not sure it's right
  val correleationId = LogAnnotation[String](
    name = "correlationId",
    initialValue = "undefined-correlation-id",
    combine = (_, newVal) => newVal,
    render = identity
  )
  val userIdAnnotation = LogAnnotation[String](
    name = "userId",
    initialValue = "undefined-user-id",
    combine = (_, newValue) => newValue,
    render = identity
  )
//  val live = Slf4jLogger.makeWithAnnotationsAsMdc(List(correleationId, userIdAnnotation))

  val live = Slf4jLogger.make { (ctx, msg) =>
    val correlationId = correleationId.render(ctx.get(correleationId))
    val userId = userIdAnnotation.render(ctx.get(userIdAnnotation))
    logFormat.format(correlationId, userId, msg)
  }
}
