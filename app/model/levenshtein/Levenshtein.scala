package model.levenshtein

import model.levenshtein.OperationType._

object Levenshtein {

  @inline
  private def minimum(i: Int*): Int = i.min

  private def calcuateTable(s1: String, s2: String): Array[Array[Int]] = {
    val dist = Array.tabulate(s1.length + 1, s2.length + 1) { (j, i) => if (j == 0) i else if (i == 0) j else 0 }

    for {
      i <- dist.indices.tail
      j <- dist(0).indices.tail
    } dist(i)(j) =
      if (s2(j - 1) == s1(i - 1)) dist(i - 1)(j - 1)
      else minimum(dist(i)(j - 1) + 1, dist(i - 1)(j) + 1, dist(i - 1)(j - 1) + 1)

    dist
  }

  def distance(s1: String, s2: String): Int = {
    val dist = calcuateTable(s1, s2)

    dist(s2.length)(s1.length)
  }


  def calculateBacktrace(start: String, target: String): Seq[EditOperation] = {

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

    val dist = calcuateTable(start, target)

    go(dist, start.length, target.length, List[EditOperation]())
  }

}
