package model

import better.files._
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy
import org.apache.poi.ss.usermodel.{CellType, Row => ExcelRow}
import org.apache.poi.xssf.usermodel.XSSFWorkbook

object Importer {

  private val stdFile: File = file"conf/vokabeln.xlsx"

  private val cardTypeCellIndex: Int = 0
  private val questionCellIndex: Int = 1
  private val meaningCellIndex : Int = 2

  private def cardTypeFromString(cardTypeStr: String): Either[String, CardType] = cardTypeStr match {
    case "Wort"      => Right(CardType.Vocable)
    case "Text"      => Right(CardType.Text)
    case "SC" | "MC" => Right(CardType.Choice)
    case "Lücke"     => Right(CardType.Blank)
    case other       => Left(s"Der Kartentype $other kann nicht verstanden werden!")
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
    val readFlashcards = (firstRowWithoutHeaderIndex to sheet.getLastRowNum) flatMap { rowIndex =>
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

    val (_, answers): (Seq[String], Seq[ChoiceAnswer]) = partitionEitherSeq((meaningCellIndex to maxCellIndex by 2).map { cellIndex =>
      readStringCell(row, cellIndex) map { answer =>
        val id = (cellIndex - meaningCellIndex) / 2

        val correctnessCellStringValue = row.getCell(cellIndex + 1, MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue

        val correctness = if (correctnessCellStringValue.nonEmpty) Correctness.Correct else Correctness.Wrong

        ChoiceAnswer(id, cardId, collId, courseId, answer, correctness)
      }
    })

    Right(Flashcard(cardId, collId, courseId, CardType.Choice, question, choiceAnswers = answers))

  }

  private def readTextualRow(row: ExcelRow, courseId: Int, collId: Int, cardType: CardType, question: String): Either[String, Flashcard] =
    readStringCell(row, meaningCellIndex) map { meaning =>
      Flashcard(row.getRowNum, collId, courseId, cardType, question, meaning)
    }

  private def readBlankRow(row: ExcelRow, courseId: Int, collId: Int, question: String): Either[String, Flashcard] = {
    val cardId = row.getRowNum

    val (_, answers): (Seq[String], Seq[BlanksAnswer]) = partitionEitherSeq((meaningCellIndex to row.getLastCellNum).map { cellIndex =>
      readStringCell(row, cellIndex) map {
        answer => BlanksAnswer(cellIndex - meaningCellIndex, cardId, collId, courseId, answer)
      }
    })

    Right(Flashcard(cardId, collId, courseId, CardType.Blank, question, meaning = "", answers))
  }

  private def readRow(row: ExcelRow, courseId: Int, collId: Int): Either[String, Flashcard] = for {
    cardTypeString <- readStringCell(row, cardTypeCellIndex)
    cardType <- cardTypeFromString(cardTypeString)
    question <- readStringCell(row, questionCellIndex)
    flashcard <- cardType match {
      case CardType.Vocable | CardType.Text => readTextualRow(row, courseId, collId, cardType, question)
      case CardType.Blank                   => readBlankRow(row, courseId, collId, question)
      case CardType.Choice                  => readChoiceRow(row, courseId, collId, question)
    }
  } yield flashcard


  private def readStringCell(row: ExcelRow, index: Int): Either[String, String] = {
    val cell = row.getCell(index, MissingCellPolicy.CREATE_NULL_AS_BLANK)

    cell.getCellType match {
      case CellType.STRING => Right(cell.getStringCellValue.trim)
      case other           => Left(s"Column $index of row ${row.getRowNum} should be a string but was ${other.name()}")
    }
  }

}
