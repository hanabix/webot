package zhongl

import ammonite.MainRunner
import ammonite.compiler.iface.Parser
import ammonite.main.Config
import mainargs.Flag
import org.openqa.selenium.chrome.ChromeDriverService

import java.io.PrintStream

object Main {

  def main(args: Array[String]): Unit = {
    val service = ChromeDriverService.createDefaultService()
    try {
      service.start()
      val url = service.getUrl;
      println(url)
      val config = Config.parser.constructOrExit(args.toList)
      val predefCode =
        s"""
           |import $$ivy.`org.seleniumhq.selenium:selenium-java:4.0.0-beta-3`
           |import org.openqa.selenium._
           |implicit val d = remote.RemoteWebDriver.builder().address("$url").oneOf(new chrome.ChromeOptions()).build()
           |""".stripMargin

      val runner = new MainRunner(
        Config(
          Config.Core(
            Flag(false),
            Flag(false),
            config.core.watch,
            Flag(false),
            thin = Flag(true),
            help = Flag(false)
          ),
          Config.Predef(predefCode, Flag(true)),
          Config.Repl(noRemoteLogging = Flag(false), classBased = Flag(false)),
          config.rest: _*
        ),
        new PrintStream(System.out),
        new PrintStream(System.err),
        System.in,
        System.out,
        System.err,
        os.pwd
      )
      config.rest.toList match {
        case head :: rest =>
          runner.runScript(os.Path(head, os.pwd), rest)
      }
    } finally {
      service.stop()
      println("exit")
    }
  }
}
