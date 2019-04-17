package model

import model.levenshtein._
import model.levenshtein.OperationType._
import org.scalatest.{FlatSpec, Matchers}

class LevenshteinTest extends FlatSpec with Matchers {

  behavior of "Levenshtein"

  it should "calculate correct distances" in {

    Levenshtein.distance("test", "tert") shouldBe 1

    Levenshtein.distance("Jack", "John") shouldBe 3

  }

  it should "calculate backtraces correctly" in {
    Levenshtein.calculateBacktrace("tesst", "test") shouldBe Seq(EditOperation(Delete, 3))

    Levenshtein.calculateBacktrace("test", "tert") shouldBe Seq(EditOperation(Replace, 2, Some('r')))

    Levenshtein.calculateBacktrace("tet", "test") shouldBe Seq(EditOperation(Insert, 2, Some('s')))

    Levenshtein.calculateBacktrace("Jack", "John") shouldBe
      Seq(EditOperation(Replace, 1, Some('o')), EditOperation(Replace, 2, Some('h')), EditOperation(Replace, 3, Some('n')))

    Levenshtein.calculateBacktrace("Jack", "James") shouldBe
      Seq(EditOperation(Replace, 2, Some('m')), EditOperation(Replace, 3, Some('e')), EditOperation(Insert, 4, Some('s')))

  }

}
