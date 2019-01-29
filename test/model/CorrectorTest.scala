package model

import org.scalatest.{FlatSpec, Matchers}

class CorrectorTest extends FlatSpec with Matchers {

  "A Corrector" should "match integers" in {
    assert(Corrector.matchAnswerIds(Seq(1, 2, 3), Seq(3, 4, 5)) == AnswerSelectionResult(List(1, 2), List(3), List(4, 5)))
  }

}
