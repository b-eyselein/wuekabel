package controllers

import javax.inject.{Inject, Singleton}
import model._
import model.persistence.TableDefs
import play.api.data.Form
import play.api.libs.Files.TemporaryFile
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdminController @Inject()(cc: ControllerComponents, protected val tableDefs: TableDefs)(implicit protected val ec: ExecutionContext)
  extends AbstractController(cc) with ControllerHelpers with play.api.i18n.I18nSupport {

  override protected val adminRightsRequired: Boolean = true

  def index: EssentialAction = futureWithUser { admin =>
    implicit request =>
      tableDefs.futureAllCourses map {
        allCourses => Ok(views.html.admin.adminIndex(admin, allCourses))
      }
  }

  // Courses

  def courseAdmin(courseId: Int): EssentialAction = futureWithUserAndCourse(courseId) { (admin, course) =>
    implicit request =>
      tableDefs.futureAllCollectionsInCourse(course.id) map {
        collections => Ok(views.html.admin.courseAdmin(admin, course, collections))
      }
  }

  def newCourseForm: EssentialAction = futureWithUser { admin =>
    implicit request =>
      tableDefs.futureNextCourseId.map { nextCourseId =>
        Ok(views.html.forms.newCourseForm(admin, FormMappings.newCourseForm.fill(Course(nextCourseId, "", ""))))
      }
  }

  def newCourse: EssentialAction = futureWithUser { admin =>
    implicit request =>
      def onError: Form[Course] => Future[Result] = { formWithErrors =>
        Future.successful(BadRequest(views.html.forms.newCourseForm(admin, formWithErrors)))
      }

      def onRead: Course => Future[Result] = { newCourse =>
        tableDefs.futureInsertCourse(newCourse) map {
          _ => Redirect(routes.AdminController.index())
        }
      }

      FormMappings.newCourseForm.bindFromRequest.fold(onError, onRead)
  }

  // Collections

  def collectionAdmin(courseId: Int, collId: Int): EssentialAction = futureWithUserAndCollection(courseId, collId) { (admin, course, collection) =>
    implicit request =>
      tableDefs.futureFlashcardsForCollection(collection) map {
        flashcards => Ok(views.html.admin.collectionAdmin(admin, courseId, collection, flashcards))
      }
  }


  def newCollectionForm(courseId: Int): EssentialAction = futureWithUser { admin =>
    implicit request =>
      for {
        allLanguages <- tableDefs.futureAllLanguages
        nextCollectionId <- tableDefs.futureNextCollectionIdInCourse(courseId)
      } yield {
        val filledForm = FormMappings.newCollectionForm.fill(
          CollectionBasics(nextCollectionId, courseId, allLanguages.head.id, allLanguages.head.id, "")
        )

        Ok(views.html.forms.newCollectionForm(admin, courseId, filledForm))
      }
  }

  def newCollection(courseId: Int): EssentialAction = futureWithUser { admin =>
    implicit request =>
      def onError: Form[CollectionBasics] => Future[Result] = { formWithErrors =>
        Future.successful(BadRequest(views.html.forms.newCollectionForm(admin, courseId, formWithErrors)))
      }

      def onRead: CollectionBasics => Future[Result] = { newCollection =>
        tableDefs.futureInsertCollection(newCollection) map {
          _ => Redirect(routes.AdminController.courseAdmin(courseId))
        }
      }

      FormMappings.newCollectionForm.bindFromRequest.fold(onError, onRead)
  }

  // Flashcards

  def uploadCardsFileForm(courseId: Int, collId: Int): EssentialAction = withUser { user =>
    implicit request => Ok(views.html.forms.uploadCardsForm(user, courseId, collId))
  }

  def uploadCardsFile(courseId: Int, collId: Int): EssentialAction = futureWithUserAndCollection(courseId, collId) { (admin, _, collection) =>
    implicit request =>

      request.body.asMultipartFormData flatMap (_.file(Consts.excelFileName)) match {
        case None => Future(Redirect(routes.AdminController.collectionAdmin(courseId, collId)))

        case Some(filePart: MultipartFormData.FilePart[TemporaryFile]) =>
          val (failureStrings, importedFlashcards) = Importer.importFlashcards(courseId, collId, filePart.ref.path)

          failureStrings.foreach(println)

          val futureImportedFlashcardsSaved = Future.sequence(importedFlashcards.map(tableDefs.futureInsertCompleteFlashcard))

          futureImportedFlashcardsSaved map { importedFlashcardsSaved =>
            Ok(views.html.cardPreview(admin, courseId, collection, importedFlashcards, failureStrings))
          }
      }

  }

}
