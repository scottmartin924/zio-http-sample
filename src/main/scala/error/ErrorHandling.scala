package error

import cats.implicits.catsSyntaxOptionId
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, parser}
import zhttp.http.{Http, HttpApp, Request, Response, Status}
import zio.{Has, Task, ZIO}
import zio.logging.{Logging, log}

import java.net.http.HttpResponse

object ErrorHandling {

  case class BusinessException(status: Status, code: String, details: String) extends Throwable

  // Consider if this really belongs in resource
  case class ErrorResponse(code: String, details: Option[String])
  object ErrorResponse {
    implicit val encoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
    implicit val decoder: Decoder[ErrorResponse] = deriveDecoder[ErrorResponse]
  }

  // FIXME This is awful...would like both to be able to ignore this and also to override it if want custom effect or response
  val defaultBodyParserErrorHandler: (String, String) => ZIO[Logging, Throwable, Response] = (body, err) => log.info(s"bodyParserErrorCatcher: Failed to parse body $body")
    .as(Response.json(ErrorResponse(code = "invalid-request", details = s"Failed to deserialize body: $err".some).asJson.noSpaces).setStatus(Status.BAD_REQUEST))

  // FIXME This should be middleware
  // FIXME I really don't think this should require an environment type parameter, but I can't seem to make it go away
  /**
   *
   * @param req the request
   * @param success the action to take on success. Input is the parsed bod, output
   *                should be effects to execute
   * @tparam A the type to parse the request to
   * @tparam R the environment type for success
   * @return
   */
  def bodyParser[A, R](req: Request, success: A => ZIO[R, Throwable, Response])(implicit decoder: Decoder[A]): ZIO[R with Logging, Throwable, Response] = {
    req.getBodyAsString.flatMap { body =>
      parser.decode[A](body) match {
        case Left(err) => defaultBodyParserErrorHandler(body, err.getMessage)
        case Right(value) => success(value)
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
