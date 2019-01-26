package model

import play.api.data.Form
import play.api.data.Forms._
import Consts._

final case class LoginFormValues(username: String, password: String)

final case class RegisterFormValues(username: String, name: String, pw: String, pwRepeat: String)

object FormMappings {

  val loginValuesForm: Form[LoginFormValues] = Form(
    mapping(
      usernameName -> nonEmptyText,
      pwName -> nonEmptyText
    )(LoginFormValues.apply)(LoginFormValues.unapply)
  )

  val registerValuesForm: Form[RegisterFormValues] = Form(
    mapping(
      usernameName -> nonEmptyText,
      nameName -> nonEmptyText,
      pwName -> nonEmptyText,
      repeatPwName -> nonEmptyText
    )(RegisterFormValues.apply)(RegisterFormValues.unapply)
  )

}
