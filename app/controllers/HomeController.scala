package controllers

import javax.inject.{Inject, Singleton}
import model._
import model.persistence.TableDefs
import play.api.libs.json.JsString
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(cc: ControllerComponents, protected val tableDefs: TableDefs)(implicit protected val ec: ExecutionContext)
  extends AbstractController(cc) with NewControllerHelpers with play.api.i18n.I18nSupport {

  //  private val logger = Logger(classOf[HomeController])

  override protected val adminRightsRequired: Boolean = false

  // Routes

  def index: EssentialAction = futureWithUser { user =>
    implicit request =>
      val pwSet = request.flash.get("no_pw_set").isEmpty

      for {
        courses <- tableDefs.futureCoursesForUser(user.username)
        repeatCount <- tableDefs.futureFlashcardsToRepeatCount(user)
      } yield Ok(views.html.index(user, courses, repeatCount, pwSet))
  }

  def acceptDPS: EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureUserAcceptedDps(user).map {
        _ => Redirect(routes.HomeController.index())
      }
  }

  def registerForCoursesForm: EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureAllCoursesWithRegisterState(user.username).map {
        allCoursesAndRegisterState => Ok(views.html.forms.registerForCoursesForm(user, allCoursesAndRegisterState))
      }
  }

  def registerForCourse(courseId: Int): EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureRegisterUserForCourse(user.username, courseId).map {
        _ => Redirect(controllers.routes.HomeController.registerForCoursesForm())
      }
  }

  def unregisterForCourse(courseId: Int): EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureUnregisterUserFromCourse(user.username, courseId).map {
        _ => Redirect(controllers.routes.HomeController.registerForCoursesForm())
      }
  }

  def userPage: EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureCoursesForUser(user.username) map {
        courses => Ok(views.html.user(user, courses))
      }
  }

  def course(courseId: Int): EssentialAction = futureWithUserAndCourse(courseId) { (user, course) =>
    implicit request =>
      tableDefs.futureAllCollectionsInCourse(course.id) map {
        collectionsForCourse => Ok(views.html.course(user, course, collectionsForCourse))
      }
  }

  def collection(courseId: Int, collId: Int): EssentialAction = futureWithUserAndCollection(courseId, collId) { (user, course, collection) =>
    implicit request =>
      for {
        flashcardCount <- tableDefs.futureFlashcardCountForCollection(collection)
        toLearnCount <- tableDefs.futureFlashcardsToLearnCount(user, collection)
      } yield Ok(views.html.collection(user, courseId, collection, flashcardCount, toLearnCount))
  }

  def learn(courseId: Int, collId: Int, frontToBack: Boolean = true): EssentialAction = futureWithUserAndCollection(courseId, collId) { (user, course, collection) =>
    implicit request =>
      tableDefs.futureFlashcardsToLearnCount(user, collection).map {
        cardsToLearnCount => Ok(views.html.learn(user, Math.min(cardsToLearnCount, 10), Some(course, collection)))
      }
  }

  def repeat: EssentialAction = futureWithUser { user =>
    implicit request =>
      tableDefs.futureFlashcardsToRepeatCount(user).map {
        cardsToRepeatCount => Ok(views.html.learn(user, Math.min(cardsToRepeatCount, 10), None))
      }
  }



}
