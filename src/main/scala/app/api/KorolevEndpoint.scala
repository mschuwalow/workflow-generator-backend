package app.api

import cats.effect.ConcurrentEffect
import cats.syntax.functor._
import fs2.{Chunk, Pipe, Stream => FS2Stream}
import korolev.data.Bytes
import korolev.effect.{Effect, Queue, Stream => KStream}
import korolev.fs2._
import korolev.scodec._
import korolev.server.{HttpRequest => KorolevHttpRequest, KorolevService}
import korolev.web.{PathAndQuery => PQ, Request => KorolevRequest, Response => KorolevResponse}
import korolev.zio.zioEffectInstance
import org.http4s.headers.Cookie
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.{Close, Text}
import org.http4s.{Header, Headers, Request, Response, Status}
import scodec.bits.ByteVector
import tsec.authentication._
import zio.interop.catz._
import zio.{Chunk => _, _}

import scala.concurrent.ExecutionContext

abstract class KorolevEndpoint[R <: KorolevEndpoint.Env] extends Endpoint[R] {
  import dsl._

  def makeService(implicit effect: Effect[RTask], ec: ExecutionContext): KorolevService[RTask]

  def authedRoutes =
    ZIO.runtime[R].flatMap { implicit runtime =>
      Auth.getTSecAuthenticator[R].map { auth =>
        implicit val ec: ExecutionContext            = runtime.platform.executor.asEC
        implicit val effect                          = zioEffectInstance[R, Throwable](runtime)(identity)(identity)
        val korolevServer: KorolevService[RIO[R, *]] = makeService

        SecuredRequestHandler(auth).liftService {
          TSecAuthService {
            case req @ GET -> Root / "bridge" / "web-socket" / _ asAuthed _ =>
              val (fromClient, kStream) = makeSinkAndSubscriber()

              val korolevRequest = mkKorolevRequest[RTask, KStream[RTask, String]](req.request, kStream)

              for {
                response <- korolevServer.ws(korolevRequest)
                toClient  = response match {
                              case KorolevResponse(_, outStream, _, _) =>
                                outStream
                                  .map(out => Text(out))
                                  .toFs2
                              case _                                   =>
                                throw new RuntimeException
                            }
                route    <- WebSocketBuilder[RTask].build(toClient, fromClient)
              } yield route

            case req @ GET -> _ asAuthed _                                  =>
              val body           = KStream.empty[RTask, Bytes]
              val korolevRequest = mkKorolevRequest(req.request, body)
              handleHttpResponse(korolevServer, korolevRequest)

            case req                                                        =>
              for {
                stream        <- req.request.body.chunks.map(ch => Bytes.wrap(ch.toByteVector)).toKorolev()
                korolevRequest = mkKorolevRequest(req.request, stream)
                response      <- handleHttpResponse(korolevServer, korolevRequest)
              } yield response
          }
        }
      }
    }

  private[this] def handleHttpResponse[F[_]: Effect: ConcurrentEffect](
    korolevServer: KorolevService[F],
    korolevRequest: KorolevHttpRequest[F]
  ) =
    korolevServer.http(korolevRequest).map {
      case KorolevResponse(status, stream, responseHeaders, _) =>
        val headers = getContentTypeAndResponseHeaders(responseHeaders)
        val body    = stream.toFs2.flatMap { bytes =>
          val bv    = bytes.as[ByteVector]
          val chunk = Chunk.byteVector(bv)
          FS2Stream.chunk(chunk)
        }
        Response[F](
          status = Status(status.code),
          headers = Headers.of(headers: _*),
          body = body
        )
    }

  private[this] def getContentTypeAndResponseHeaders(responseHeaders: Seq[(String, String)]) =
    responseHeaders.map { case (name, value) => Header(name, value) }

  private[this] def makeSinkAndSubscriber[F[_]: Effect: ConcurrentEffect]() = {
    val queue                               = Queue[F, String]()
    val sink: Pipe[F, WebSocketFrame, Unit] = (in: FS2Stream[F, WebSocketFrame]) => {
      in.evalMap {
        case Text(t, _) if t != null =>
          queue.enqueue(t)
        case _: Close                =>
          ConcurrentEffect[F].unit
        case f                       =>
          throw new Exception(s"Invalid frame type ${f.getClass.getName}")
      }.onFinalizeCase(_ => queue.close())
    }
    (sink, queue.stream)
  }

  private[this] def mkKorolevRequest[F[_], A](request: Request[F], body: A): KorolevRequest[A] = {
    val cookies = request.headers.get(Cookie).map(x => x.value)
    KorolevRequest(
      pq = PQ.fromString(request.pathInfo).withParams(request.params),
      method = KorolevRequest.Method.fromString(request.method.name),
      renderedCookie = cookies.orNull,
      contentLength = request.headers.find(_.name == "content-length").map(_.value.toLong),
      headers = {
        val contentType        = request.contentType
        val contentTypeHeaders =
          contentType.map { ct =>
            if (ct.mediaType.isMultipart) Seq("content-type" -> contentType.toString) else Seq.empty
          }.getOrElse(Seq.empty)
        request.headers.toList.map(h => (h.name.value, h.value)) ++ contentTypeHeaders
      },
      body = body
    )
  }

}

object KorolevEndpoint {
  type Env = Auth

}
