package model

import model.levenshtein._
import model.levenshtein.OperationType._
import org.scalatest.{FlatSpec, Matchers}

class LevenshteinTest extends FlatSpec with Matchers {

  "The Levenshtein distance" should "calculate correct distances" in {

    assert(Levenshtein.distance("test", "tert") == 1)

    assert(Levenshtein.distance("Jack", "John") == 3)

  }

  it should "calculate backtraces correctly" in {
    assert(Levenshtein.calculateBacktrace("tesst", "test") == List(EditOperation(Delete, 3)))

    assert(Levenshtein.calculateBacktrace("test", "tert") == List(EditOperation(Replace, 2, Some('r'))))

    assert(Levenshtein.calculateBacktrace("tet", "test") == List(EditOperation(Insert, 2, Some('s'))))

    assert(Levenshtein.calculateBacktrace("Jack", "John") ==
      List(EditOperation(Replace, 1, Some('o')), EditOperation(Replace, 2, Some('h')), EditOperation(Replace, 3, Some('n'))))

    assert(Levenshtein.calculateBacktrace("Jack", "James") ==
      List(EditOperation(Replace, 2, Some('m')), EditOperation(Replace, 3, Some('e')), EditOperation(Insert, 4, Some('s'))))

  }

}
