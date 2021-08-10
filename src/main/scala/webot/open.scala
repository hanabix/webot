package webot

trait Open {
  def apply(df: FExpression[Unit]): Unit = apply { case _ => df }
  def apply(df: PartialFunction[String, FExpression[Unit]]): Unit
}

object Open {
  trait Dsl {
    def open[A](start: String)(implicit runtime: Runtime[A], compiler: ContextCompiler[A], engine: Engine): Open = new Open {
      def apply(df: PartialFunction[String, FExpression[Unit]]) = {
        runtime { context =>
          engine { url =>
            val ctx  = context(url)
            val free = df(ctx.currentUrl)
            () => free.foldMap(compiler(ctx)).value.value
          } apply start
        }
      }
    }
  }
}
