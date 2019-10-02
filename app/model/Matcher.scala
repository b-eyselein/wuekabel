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

  def doMatch(learnerSolutions: List[StringSolution], sampleSolutions: List[String]): MatchingResult = {

    def matchHead(learnerSol: StringSolution, sampleSolutions: List[String]): MatchHeadResult = {

      @annotation.tailrec
      def go(
        learnerSolution: StringSolution,
        remainingSampleSols: List[String],
        priorSamples: List[String],
        maybeBestAndDistanceToBest: Option[BestAndDistanceToBest],
        posteriorSamples: List[String]
      ): MatchHeadResult = remainingSampleSols match {
        case Nil          => MatchHeadResult(priorSamples ++ posteriorSamples, maybeBestAndDistanceToBest)
        case head :: tail =>

          val distanceToHead = LevenshteinDistance(learnerSolution, head)

          maybeBestAndDistanceToBest match {
            case None                        =>
              go(learnerSolution, tail, priorSamples ++ posteriorSamples, Some(BestAndDistanceToBest(head, distanceToHead)), List.empty[String])
            case Some(bestAndDistanceToBest) =>

              if (distanceToHead.distance < bestAndDistanceToBest.distanceToBest.distance) {
                go(learnerSolution, tail, priorSamples ++ (bestAndDistanceToBest.best :: posteriorSamples), Some(BestAndDistanceToBest(head, distanceToHead)), List.empty[String])
              } else {
                go(learnerSolution, tail, priorSamples, maybeBestAndDistanceToBest, posteriorSamples :+ head)
              }
          }
      }

      go(learnerSol, sampleSolutions, List.empty[String], None, List.empty[String])
    }

    @annotation.tailrec
    def go(learnerSols: List[StringSolution], sampleSols: List[String], matches: Seq[LevenshteinDistance]): MatchingResult = learnerSols match {
      case Nil          => MatchingResult(matches, sampleSols)
      case head :: tail =>

        matchHead(head, sampleSols) match {
          case MatchHeadResult(_, None)                                       =>
            // FIXME: sampleSolutions was empty ?!?
            ???
          case MatchHeadResult(remainingSamples, Some(bestAndDistanceToBest)) =>
            go(tail, remainingSamples, matches :+ bestAndDistanceToBest.distanceToBest)
        }

    }

    go(learnerSolutions, sampleSolutions, Seq.empty[LevenshteinDistance])
  }

}
