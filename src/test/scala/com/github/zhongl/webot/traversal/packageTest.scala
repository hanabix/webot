package com.github.zhongl.webot.traversal

import org.scalatest.wordspec.AnyWordSpec
import org.scalamock.scalatest.MockFactory
import Node._

class packageTest extends AnyWordSpec with MockFactory {
  implicit val handle = mockFunction[Int, React[Int]]

  "A leaf" should {
    "be traversed" in {
      handle.expects(0).returns(Continue).once()

      leaf(0).traverse
    }
  }

  "A branch" when {
    "empty" should {
      "not be traversed" in {
        handle.expects(*).never()

        branch[Int]().traverse
      }
    }

    "one leaf" should {
      "be traversed" in {
        handle.expects(0).returns(Continue).once()

        branch(leaf(0)).traverse
      }
    }

    "more leafs" should {
      "be traversed" in {
        handle.expects(*).returns(Continue).twice()

        branch(leaf(0), branch(leaf(1))).traverse
      }

      "be skipped" in {
        handle.expects(0).returns(Skip).once()
        handle.expects(1).never()
        handle.expects(2).returns(Continue).once()

        branch(
          branch(leaf(0), leaf(1)),
          leaf(2)
        ).traverse
      }

      "be aborted" in {
        handle.expects(0).returns(Abort).once()
        handle.expects(1).never()
        handle.expects(2).never()

        branch(
          branch(leaf(0), leaf(1)),
          leaf(2)
        ).traverse
      }

      "be retried" in {
        handle.expects(0).returns(Retry(leaf(1))).once()
        handle.expects(0).returns(Continue).once()
        handle.expects(1).returns(Continue).twice()

        branch(leaf(0), leaf(1)).traverse
      }
    }
  }

}
