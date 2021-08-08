package webot.selenium

import scala.util.Try
import scala.jdk.CollectionConverters._
import scala.jdk.FunctionConverters._

import org.openqa.selenium._
import org.openqa.selenium.chrome._
import org.openqa.selenium.remote._
import org.openqa.selenium.support.ui._

import cats._
import data._
import arrow._
import webot._

final case class Context(global: SearchContext, handle: Handle, currentUrl: String, find: SearchContext => Wait[SearchContext])
    extends webot.Context[Handle]
    with Control.Dsl { self =>
  type F[A] = List[A]
  type C    = Context

  implicit val toId: F ~> Id = new (F ~> Id) {
    def apply[A](fa: F[A]): Id[A] = fa.head
  }
  implicit val toOption: F ~> Option = new (F ~> Option) {
    def apply[A](fa: F[A]): Option[A] = fa.headOption
  }
  implicit val toNonEmptyList: F ~> NonEmptyList = new (F ~> NonEmptyList) {
    def apply[A](fa: F[A]): NonEmptyList[A] = NonEmptyList.fromList(fa).get
  }
  implicit val toList: F ~> List = FunctionK.id

  def get[G[_]](dp: Descriptor)(implicit nt: F ~> G): ControlOr[G[C]] = {
    def cond(descriptor: String): SearchContext => List[Context] = sc => {
      val result = sc.findElements(By.cssSelector(descriptor))
      if (result.isEmpty()) null
      else
        result.asScala.toList.map { e => self.copy(handle = handle.copy(element = e)) }
    }

    val r = dp match {
      case Self               => List(self)
      case Local(descriptor)  => find(handle.element).until(cond(descriptor).asJava)
      case Global(descriptor) => find(global).until(cond(descriptor).asJava)
    }

    Try(nt(r)).toOption.toRight(complain(s"Missing descriptor: $dp"))
  }
}
