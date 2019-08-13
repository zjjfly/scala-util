package io.wasted.util.test

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference

import com.twitter.conversions.time._
import com.twitter.util.Await
import io.netty.handler.codec.http._
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate
import io.netty.util.CharsetUtil
import io.wasted.util.http._
import org.scalatest._
import org.scalatest.concurrent._

class HttpsSpec
  extends WordSpec
  with ScalaFutures
  with AsyncAssertions
  with BeforeAndAfter {

  val responder = new HttpResponder("wasted-http")

  val cert = new SelfSignedCertificate()
  val sslCtx =
    SslContextBuilder.forServer(cert.certificate(), cert.privateKey()).build()
  var server =
    new AtomicReference[HttpServer[FullHttpRequest, HttpResponse]](null)

  before {
    server.set(
      HttpServer[FullHttpRequest, HttpResponse](NettyHttpCodec()
        .withTls(sslCtx))
        .handler {
          case (ctx, req) =>
            req.map { req =>
              val resp =
                if (req.uri() == "/bad_gw") HttpResponseStatus.BAD_GATEWAY
                else HttpResponseStatus.ACCEPTED
              responder(resp)
            }
        }
        .bind(new InetSocketAddress(8889)))
  }

  val client1 = HttpClient(
    NettyHttpCodec[HttpRequest, FullHttpResponse]().withInsecureTls())

  "2 GET Request to https://spring.io/" should {
    "contain the phrase \"Spring\" somewhere" in {
      val resp1: FullHttpResponse =
        Await.result(
          client1.get(new java.net.URI("https://spring.io/")),
          5.seconds)
      assert(resp1.content.toString(CharsetUtil.UTF_8).contains("Spring"))
      resp1.content.release()
    }
    "another phrase of \"Spring\" somewhere" in {
      val resp2: FullHttpResponse =
        Await.result(
          client1.get(new java.net.URI("https://spring.io/")),
          5.seconds)
      assert(resp2.content.toString(CharsetUtil.UTF_8).contains("Spring"))
      resp2.content.release()
    }
  }

  "GET Request to embedded Http Server" should {
    "returns status code ACCEPTED" in {
      val resp3: FullHttpResponse =
        Await.result(
          client1.get(new java.net.URI("https://localhost:8889/")),
          5.seconds)
      assert(resp3.status() equals HttpResponseStatus.ACCEPTED)
      resp3.content.release()
    }
    "returns status code BAD_GATEWAY" in {
      val resp4: FullHttpResponse = Await.result(
        client1.get(new java.net.URI("https://localhost:8889/bad_gw")),
        5.seconds)
      assert(resp4.status() equals HttpResponseStatus.BAD_GATEWAY)
      resp4.content.release()
    }
  }

  after(server.get.shutdown())
}
