package model

import org.scalatest.{FlatSpec, Matchers}

class CorrectorTest extends FlatSpec with Matchers {

  behavior of "Corrector"

  it should "match integers" in {
    Corrector.matchAnswerIds(Seq(1, 2, 3), Seq(3, 4, 5)) shouldBe AnswerSelectionResult(List(1, 2), List(3), List(4, 5))
  }

}
