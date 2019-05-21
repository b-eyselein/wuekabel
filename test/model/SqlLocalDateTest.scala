package model

import java.sql.{Date => SqlDate}
import java.time.LocalDate

import org.scalatest.{FlatSpec, Matchers}

class SqlLocalDateTest extends FlatSpec with Matchers {

  behavior of "SqlLocalDate"

  it should "be converted" in {
    val today = LocalDate.now()
    SqlDate.valueOf(today).toLocalDate shouldBe today

    val other = LocalDate.of(2019, 5, 21)
    SqlDate.valueOf(other) shouldBe SqlDate.valueOf("2019-05-21")

  }

}
