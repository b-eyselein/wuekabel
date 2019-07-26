package model.levenshtein

import model.StringSolution
import model.levenshtein.OperationType._


final case class LevenshteinDistance(start: StringSolution, target: String) {

  @inline
  private def minimum(i: Int*): Int = i.min

  private def calculateTable(s1: String, s2: String): Array[Array[Int]] = {
    val dist = Array.tabulate(s1.length + 1, s2.length + 1) { (j, i) => if (j == 0) i else if (i == 0) j else 0 }

    for {
      i <- dist.indices.tail
      j <- dist(0).indices.tail
    } dist(i)(j) =
      if (s2(j - 1) == s1(i - 1)) dist(i - 1)(j - 1)
      else minimum(dist(i)(j - 1) + 1, dist(i - 1)(j) + 1, dist(i - 1)(j - 1) + 1)

    dist
  }

  private def calculateBacktrace(dist: Array[Array[Int]]): Seq[EditOperation] = {

    @annotation.tailrec
    def go(dist: Array[Array[Int]], i: Int, j: Int, operations: List[EditOperation]): List[EditOperation] =
      if (i > 0 && (dist(i - 1)(j) + 1) == dist(i)(j)) {

        val op = EditOperation(Delete, i - 1)
        go(dist, i - 1, j, op :: operations)

      } else if (j > 0 && (dist(i)(j - 1) + 1) == dist(i)(j)) {

        val op = EditOperation(Insert, i, Some(target(j - 1)))
        go(dist, i, j - 1, op :: operations)

      } else if (i > 0 && j > 0 && dist(i - 1)(j - 1) + 1 == dist(i)(j)) {

        val op = EditOperation(Replace, i - 1, Some(target(j - 1)))
        go(dist, i - 1, j - 1, op :: operations)

      } else if (i > 0 && j > 0 && dist(i - 1)(j - 1) == dist(i)(j)) {

        go(dist, i - 1, j - 1, operations)

      } else operations

    go(dist, start.solution.length, target.length, List[EditOperation]())
  }

  private val dist = calculateTable(start.solution, target)

  val distance: Int = dist(start.solution.length)(target.length)

  lazy val backtrace: Seq[EditOperation] = calculateBacktrace(dist)


}
