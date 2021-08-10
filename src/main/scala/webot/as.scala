package webot

trait As[A] {
  def from(value: String): A
}
object As {
  def apply[A](implicit ins: As[A]): As[A] = ins

  implicit val asBoolean = new As[Boolean] {
    def from(value: String) = value.toBoolean
  }
  implicit val asByte = new As[Byte] {
    def from(value: String) = value.toByte
  }
  implicit val asInt = new As[Int] {
    def from(value: String) = value.toInt
  }
  implicit val asDouble = new As[Double] {
    def from(value: String) = value.toDouble
  }

  trait Dsl {
    trait AsOps {

      /** Convert a string to type `A`.
        *
        * @param f
        * @return
        */
      def as[A: As]: A
    }

    implicit def asSyntax(value: String): AsOps = new AsOps {
      def as[A: As] = As[A].from(value)
    }
  }

}
