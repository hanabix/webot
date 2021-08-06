package webot

sealed trait Locator
trait HasDescription {
  def description: String
}
final private[webot] case object Self                       extends Locator
final private[webot] case class Local(description: String)  extends Locator with HasDescription
final private[webot] case class Global(description: String) extends Locator with HasDescription

private[webot] final class LocatorOps(val sc: StringContext) extends AnyVal {
  def g(args: Any*): Global = {
    val strings     = sc.parts.iterator
    val expressions = args.iterator
    var buf         = new StringBuilder(strings.next())
    while (strings.hasNext) {
      buf.append(expressions.next())
      buf.append(strings.next())
    }
    Global(buf.toString())
  }
}
