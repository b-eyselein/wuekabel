package model

import better.files._
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy
import org.apache.poi.ss.usermodel.{CellType, Row => ExcelRow}
import org.apache.poi.xssf.usermodel.XSSFWorkbook

object Importer {

  private val stdFile: File = file"conf/vokabeln.xlsx"

  private val cardTypeCellIndex : Int = 0
  private val frontCellIndex    : Int = 1
  private val frontHintCellIndex: Int = frontCellIndex + 1
  private val backCellIndex     : Int = frontHintCellIndex + 1
  private val backHintCellIndex : Int = backCellIndex + 1

  private def cardTypeFromString(cardTypeStr: String): Either[String, CardType] = cardTypeStr match {
    case "Wort"    => Right(CardType.Word)
    case "Text"    => Right(CardType.Text)
    case "Auswahl" => Right(CardType.Choice)
    case "Lücke"   => Right(CardType.Blank)
    case other     => Left(s"Der Kartentype $other kann nicht verstanden werden!")
  }

  private def partitionEitherSeq[T, U](a: Seq[Either[T, U]]): (Seq[T], Seq[U]) =
    a.foldLeft[(Seq[T], Seq[U])]((Seq[T](), Seq[U]())) { (b, a) =>
      a match {
        case Left(t)  => (b._1 :+ t, b._2)
        case Right(u) => (b._1, b._2 :+ u)
      }
    }

  def importFlashcards(courseId: Int, collId: Int, file: File = stdFile): (Seq[String], Seq[Flashcard]) = {
    val workbook = new XSSFWorkbook(file.path.toAbsolutePath.toFile)

    val sheet = workbook.getSheetAt(workbook.getActiveSheetIndex)

    val firstRowWithoutHeaderIndex = sheet.getFirstRowNum + 1

    // Ignore header ExcelRow
    val readFlashcards = firstRowWithoutHeaderIndex.to(sheet.getLastRowNum).flatMap { rowIndex =>
      Option(sheet.getRow(rowIndex)) match {
        case None      => None
        case Some(row) => Some(readRow(row, courseId, collId))
      }
    }

    workbook.close()

    partitionEitherSeq(readFlashcards)
  }

  private def readChoiceRow(row: ExcelRow, courseId: Int, collId: Int, question: String): Either[String, Flashcard] = {

    val cardId = row.getRowNum

    val lastCellNum = row.getLastCellNum
    val maxCellIndex = if (lastCellNum % 2 == 1) lastCellNum + 1 else lastCellNum

    val (_, answers): (Seq[String], Seq[ChoiceAnswer]) = partitionEitherSeq(backCellIndex.to(maxCellIndex).by(2).map { cellIndex =>

      for {
        answer <- readStringCell(row, cellIndex)
        correct <- readOptionalStringCell(row, cellIndex + 1).map(_.isDefined)
      } yield {
        val id = (cellIndex - backCellIndex) / 2

        val correctness = if (correct) Correctness.Correct else Correctness.Wrong

        ChoiceAnswer(id, cardId, collId, courseId, answer, correctness)
      }
    })

    Right(Flashcard(cardId, collId, courseId, CardType.Choice, question, choiceAnswers = answers))

  }

  private def readTextualRow(row: ExcelRow, courseId: Int, collId: Int, cardType: CardType, question: String): Either[String, Flashcard] = for {
    frontHint <- readOptionalStringCell(row, frontHintCellIndex)
    back <- readStringCell(row, backCellIndex)
    backHint <- readOptionalStringCell(row, backHintCellIndex)
  } yield Flashcard(row.getRowNum, collId, courseId, cardType, question, frontHint, back, backHint)

  private def readBlankRow(row: ExcelRow, courseId: Int, collId: Int, question: String): Either[String, Flashcard] = {
    val cardId = row.getRowNum

    val (_, answers): (Seq[String], Seq[BlanksAnswerFragment]) = partitionEitherSeq(backCellIndex.to(row.getLastCellNum).map { cellIndex =>
      readStringCell(row, cellIndex) map {
        answer =>
          val isAnswer: Boolean = (cellIndex - backCellIndex) % 2 == 0
          BlanksAnswerFragment(cellIndex - backCellIndex, cardId, collId, courseId, answer, isAnswer)
      }
    })

    Right(Flashcard(cardId, collId, courseId, CardType.Blank, question, blanksAnswers = answers))
  }

  private def readRow(row: ExcelRow, courseId: Int, collId: Int): Either[String, Flashcard] = for {
    cardTypeString <- readStringCell(row, cardTypeCellIndex)
    cardType <- cardTypeFromString(cardTypeString)
    front <- readStringCell(row, frontCellIndex)
    flashcard <- cardType match {
      case CardType.Word | CardType.Text => readTextualRow(row, courseId, collId, cardType, front)
      case CardType.Blank                => readBlankRow(row, courseId, collId, front)
      case CardType.Choice               => readChoiceRow(row, courseId, collId, front)
    }
  } yield flashcard

  // Helper methods

  private def readStringCell(row: ExcelRow, index: Int): Either[String, String] = {
    val cell = row.getCell(index, MissingCellPolicy.CREATE_NULL_AS_BLANK)

    cell.getCellType match {
      case CellType.STRING => Right(cell.getStringCellValue.trim)
      case other           => Left(s"Column $index of row ${row.getRowNum} should be a string but was ${other.name()}")
    }
  }

  private def readOptionalStringCell(row: ExcelRow, index: Int): Either[String, Option[String]] = {
    val cell = row.getCell(index, MissingCellPolicy.CREATE_NULL_AS_BLANK)

    cell.getCellType match {
      case CellType.BLANK  => Right(None)
      case CellType.STRING => Right(Some(cell.getStringCellValue.trim))
      case other           => Left(s"Column $index of row ${row.getRowNum} should be a string but was ${other.name()}")
    }
  }

}
