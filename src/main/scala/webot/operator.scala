package webot

sealed trait Operator[+A]
object Operator {
  final private[webot] case class Attribute(name: String) extends Operator[String]
  final private[webot] case object Text                   extends Operator[String]
  final private[webot] case class Input(value: String)    extends Operator[Unit]
  final private[webot] case object Hover                  extends Operator[Unit]

  trait Dsl {

    /** An [[Operator]] to get attribute by name from a [[Subject]].
      *
      * {{{a("a.title") get attr("href")}}}
      *
      * @param name
      * @return
      */
    def attr(name: String): Operator[String] = Attribute(name)

    /** An [[Operator]] to get text from a [[Subject]].
      *
      * {{{a("span.content") get text}}}
      *
      * @return
      */
    def text: Operator[String] = Text

    /** An [[Operator]] to input a value to a [[Subject]].
      *
      * {{{a("input.username") apply input("rock")}}}
      *
      * @param value
      * @return
      */
    def input(value: String): Operator[Unit] = Input(value)

    /** An [[Operator]] to hover mouse on a [[Subject]].
      *
      * {{{a("a.link") apply hover}}}
      *
      * @return
      */
    def hover: Operator[Unit] = Hover

  }
}
