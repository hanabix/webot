package com.github.zhongl.webot

import org.openqa.selenium.chrome.ChromeDriverService
import mainargs._
import ammonite._
import ammonite.main._
import java.io.PrintStream

object Main {
  def main(args: Array[String]): Unit = {
    val service = ChromeDriverService.createDefaultService()
    service.start()
    sys.addShutdownHook(Console.err.println("Chrome Service Stopping..."))
    sys.addShutdownHook(service.stop())
    val url = service.getUrl.toString
    sys.props.addOne("webdriver.remote.server" -> url)
    Console.err.println(s"Chrome Service Started At: $url")
    ammonite.Main.main(args)

    val printErr = new PrintStream(System.err)
    val printOut = new PrintStream(System.out)

    parser.constructEither(
      args.toList,
      customName = "Webot REPL & Script-Runner",
      customDoc = "usage: webot [webot-options] [script-file [script-options]]"
    ) match {
      case Left(msg) =>
        printErr.println(msg)
        sys.exit(1)

      case Right(cfg) =>
        val core = Config.Core(
          noDefaultPredef = Flag(true),
          silent = cfg.opts.silent,
          watch = cfg.opts.watch,
          bsp = Flag(false),
          home = os.Path(System.getProperty("user.home"))/".webot",
          color = cfg.opts.color,
          thin = Flag(true),
          help = Flag(false)
        )
        val predef = Config.Predef(
          "import $ivy.`com.github.zhongl:webot_2.13:0.1.0`, com.github.zhongl._, webot._ , webot.selenium._",
          Flag(true)
        )
        val repl = Config.Repl(
          "Welcome to Webot REPL",
          Flag(true),
          Flag(false)
        )

        val runner = new MainRunner(
          Config(core, predef, repl, cfg.rest: _*),
          printOut,
          printErr,
          System.in,
          System.out,
          System.err,
          os.pwd
        )

        cfg.rest match {
          case Nil =>
            runner.printInfo("Loading...")
            runner.runRepl()
            sys.exit(0)

          case head :: rest if head.startsWith("-") =>
            runner.printError(s"Unknown Webot option: $head ${ammonite.util.Util.newLine} Use --help to list possible options")
            sys.exit(1)

          case head :: rest =>
            val b = (runner.runScript(os.Path(head, os.pwd), rest))
            sys.exit(if (b) 0 else 1)
        }
    }

  }

  @main
  case class Opts(
      @arg(
        short = 's',
        doc = """Make ivy logs go silent instead of printing though failures will
        still throw exception"""
      )
      silent: Flag,
      @arg(short = 'w', doc = "Watch and re-run your scripts when they change")
      watch: Flag,
      @arg(doc = """Enable or disable colored output; by default colors are enabled
    in both REPL and scripts if the console is interactive, and disabled
    otherwise""")
      color: Option[Boolean] = None,
      @arg(doc = "Print this message")
      help: Flag
  )

  implicit val optsParser = ParserForClass[Opts]

  @main
  case class Cfg(opts: Opts, rest: String*)

  val parser = ParserForClass[Cfg]
}
