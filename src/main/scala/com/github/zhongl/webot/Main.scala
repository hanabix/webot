package com.github.zhongl.webot

import org.openqa.selenium.chrome.ChromeDriverService

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
  }
}