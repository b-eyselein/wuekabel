package model

import model.levenshtein._
import model.levenshtein.OperationType._
import org.scalatest.{FlatSpec, Matchers}

class LevenshteinDistanceTest extends FlatSpec with Matchers {

  behavior of "Levenshtein"

  it should "calculate correct distances" in {

    LevenshteinDistance.distance("test", "tert") shouldBe 1

    LevenshteinDistance.distance("Jack", "John") shouldBe 3

  }

  it should "calculate backtraces correctly" in {
    LevenshteinDistance.calculateBacktrace("tesst", "test") shouldBe Seq(EditOperation(Delete, 3))

    LevenshteinDistance.calculateBacktrace("test", "tert") shouldBe Seq(EditOperation(Replace, 2, Some('r')))

    LevenshteinDistance.calculateBacktrace("tet", "test") shouldBe Seq(EditOperation(Insert, 2, Some('s')))

    LevenshteinDistance.calculateBacktrace("Jack", "John") shouldBe
      Seq(EditOperation(Replace, 1, Some('o')), EditOperation(Replace, 2, Some('h')), EditOperation(Replace, 3, Some('n')))

    LevenshteinDistance.calculateBacktrace("Jack", "James") shouldBe
      Seq(EditOperation(Replace, 2, Some('m')), EditOperation(Replace, 3, Some('e')), EditOperation(Insert, 4, Some('s')))

  }

}
