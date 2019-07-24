package model

import model.levenshtein.LevenshteinDistance

final case class MatchingResult(matches: Seq[LevenshteinDistance], nonMatchedSamples: Seq[String]) {

  def isCorrect: Boolean = nonMatchedSamples.isEmpty && matches.forall(_.distance == 0)

}

object Matcher {

  private final case class MatchHeadResult(
    remainingSamples: List[String],
    maybeBestAndDistanceToBest: Option[BestAndDistanceToBest]
  )

  private final case class BestAndDistanceToBest(
    best: String,
    distanceToBest: LevenshteinDistance
  )

  def doMatch(learnerSolutions: List[String], sampleSolutions: List[String]): MatchingResult = {

    def matchHead(learnerSolution: String, sampleSolutions: List[String]): MatchHeadResult = {

      @annotation.tailrec
      def go(
        learnerSolution: String,
        remainingSamples: List[String],
        priorSamples: List[String],
        maybeBestAndDistanceToBest: Option[BestAndDistanceToBest],
        posterior: List[String]
      ): MatchHeadResult = sampleSolutions match {
        case Nil          => MatchHeadResult(priorSamples ++ posterior, maybeBestAndDistanceToBest)
        case head :: tail =>

          val distanceToHead = LevenshteinDistance(learnerSolution, head)

          maybeBestAndDistanceToBest match {
            case None                        =>
              go(learnerSolution, tail, priorSamples ++ posterior, Some(BestAndDistanceToBest(head, distanceToHead)), List.empty[String])
            case Some(bestAndDistanceToBest) =>

              if (distanceToHead.distance < bestAndDistanceToBest.distanceToBest.distance) {
                go(learnerSolution, tail, priorSamples ++ (bestAndDistanceToBest.best :: posterior), Some(BestAndDistanceToBest(head, distanceToHead)), List.empty[String])
              } else {
                go(learnerSolution, tail, priorSamples, maybeBestAndDistanceToBest, posterior :+ head)
              }
          }
      }

      go(learnerSolution, sampleSolutions, List.empty[String], None, List.empty[String])
    }

    @annotation.tailrec
    def go(learnerSolutions: List[String], sampleSolutions: List[String], matches: Seq[LevenshteinDistance]): MatchingResult = learnerSolutions match {
      case Nil          => MatchingResult(matches, sampleSolutions)
      case head :: tail =>

        matchHead(head, sampleSolutions) match {
          case MatchHeadResult(remainingSamples, None)                        =>
            // FIXME: sampleSolutions was empty ?!?
            ???
          case MatchHeadResult(remainingSamples, Some(bestAndDistanceToBest)) =>
            go(tail, remainingSamples, matches :+ bestAndDistanceToBest.distanceToBest)
        }

    }

    go(learnerSolutions, sampleSolutions, Seq.empty[LevenshteinDistance])
  }

}
