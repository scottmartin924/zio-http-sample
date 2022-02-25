package error

import cats.implicits.catsSyntaxOptionId
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, parser}
import zhttp.http.{Http, HttpApp, Request, Response, Status}
import zio.ZIO
import zio.logging.{Logging, log}

object ErrorHandling {

  case class BusinessException(status: Status, code: String, details: String) extends Throwable

  // Consider if this really belongs in resource
  case class ErrorResponse(code: String, details: Option[String])
  object ErrorResponse {
    implicit val encoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
    implicit val decoder: Decoder[ErrorResponse] = deriveDecoder[ErrorResponse]
  }

  val defaultBodyParserErrorHandler: (String, String) => ZIO[Logging, Throwable, Nothing] = (body, err) =>
    log.info(s"bodyParserErrorCatcher: Failed to parse body $body") *> ZIO.fail(BusinessException(Status.BAD_REQUEST, "invalid-request", details = s"Failed to deserialize body: $err"))

  // TODO Consider allowing to pass in a bodyParserErrorHandler instead of defaulting...one tricky thing is we'd need to be
  //  somewhat strict on the environment that allows (or pass in an environment parameter...and know that the requirements on the generated httpapp might change)
  def bodyParser[A](req: Request)(implicit decoder: Decoder[A]): ZIO[Logging, Throwable, A] = {
    req.getBodyAsString.flatMap { body =>
      parser.decode[A](body) match {
        case Left(err) => defaultBodyParserErrorHandler(body, err.getMessage)
        case Right(value) => ZIO.succeed(value)
      }
    }
  }

  // TODO Consider if this is the correct approach to exception handling
  val exceptionHandler: Throwable => HttpApp[Any, Nothing] = {
    case exc: BusinessException => businessExceptionHandler(exc)
    case t: Throwable => unhandledError(t)
    case _ => Http.response(Response.text("wtf").setStatus(Status.INTERNAL_SERVER_ERROR))
  }

  private val businessExceptionHandler = (err: BusinessException) => {
    val errorResponse = ErrorResponse(err.code, details = s"Error: ${err.details}".some)
    Http.response(Response.json(errorResponse.asJson.toString()).setStatus(err.status))
  }

  private val unhandledError = (err: Throwable) => {
    val errorResponse = ErrorResponse("internal-error", err.getMessage.some)
    Http.response(Response.json(errorResponse.asJson.toString()).setStatus(Status.INTERNAL_SERVER_ERROR))
  }
}
