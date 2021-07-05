package com.github.zhongl.webot.traversal

import org.scalatest.wordspec._

class packageTest extends AnyWordSpec {
  implicit val hInt: Handle[Int] = i => { println(i); Continue }

  "A node" when {
    "Leaf" should {
      "traverse anyway" in {
        Node.leaf(1).traverse
      }
    }
    "Empty Branch" should {
      "traverse anyway" in {
        Node.branch(List.empty[Node[Int]]).traverse
      }
    }
  }
}
