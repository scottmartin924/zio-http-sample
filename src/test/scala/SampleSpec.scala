import zio.test._
import zio.test.Assertion._
import zio.ZIO

object SampleSpec extends DefaultRunnableSpec {
  override def spec = suite("SampleSpec")(
    testM("succeed") {
      val result = ZIO.succeed(19)
      assertM(result)(equalTo(19))
    }
  )
}
