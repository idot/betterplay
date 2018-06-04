package controllers

import scala.concurrent.Future

import play.api._
import play.api.mvc._
import play.api.cache.SyncCacheApi
import play.api.mvc.AbstractController
import play.api.mvc.Action
import play.api.libs.json.Json

import akka.pattern.AskTimeoutException

import models.{AccessViolationException,ItemNotFoundException,ValidationException}
import models.BetterDb
import models.User

/***
 * security based on 
 * http://www.jamesward.com/2013/05/13/securing-single-page-apps-and-rest-services
 * and especially
 * http://www.mariussoutier.com/blog/2013/07/14/272/
 *
 * It is important to not trust the client.
 * Every action that requires a certain user should first fetch the user from the database
 * The User object should not get to the UI there is the UserNoPw for that 
 * which does not have the fields passwordHash and e-mail
 * 
 * actions that reveal sensitive information only fetch from db the user that is logged in by token 
 *
 **/
class TokenRequest[A](val token: String, val userId: Long, request: Request[A]) extends WrappedRequest[A](request)
class UserRequest[A](val user: User, val request: TokenRequest[A]) extends WrappedRequest[A](request)
class AdminRequest[A](val admin: User, val request: TokenRequest[A]) extends WrappedRequest[A](request)
class OptionalUserRequest[A](val optUser: Option[User], val request: Request[A]) extends WrappedRequest[A](request) {
      def getIdOrN(): Long = {
         optUser.flatMap{ u => u.id }.getOrElse(-1)
      }
}

trait Security { self: AbstractController =>
  val betterDb: BetterDb
  val cache: SyncCacheApi //TODO switch to AsyncCacheApi
  
  val AuthTokenHeader = "X-AUTH-TOKEN"   //TODO: change to X-XSRF-TOKEN
  val AuthTokenCookieKey = "AUTH-TOKEN"   //TODO: change to XSRF-TOKEN
  val AuthTokenUrlKey = "auth"

  val securityLogger = Logger("security")
  
  implicit val ec = defaultExecutionContext  
  
  //TODO: check if cookie is necessary  
  def hasToken[A] = new ActionRefiner[Request,TokenRequest] with ActionBuilder[TokenRequest, AnyContent]{
      def refine[A](input: Request[A]) = Future.successful{
          val maybeToken = input.headers.get(AuthTokenHeader)
          securityLogger.trace(s"hasToken: ${input.uri} $AuthTokenHeader $maybeToken")
          maybeToken.flatMap{ token =>
              cache.get[Long](token).map{  userId =>  
                       securityLogger.trace(s"hasToken: ${input.uri} found token $AuthTokenHeader $maybeToken userId: $userId")
                       new TokenRequest(token, userId, input)
              }
          }.toRight{
            securityLogger.trace(s"hasToken: didn't find token: ${input.uri} $AuthTokenHeader $maybeToken UNAUTHORIZED")
            Unauthorized
          }
      }
      override def parser = controllerComponents.parsers.defaultBodyParser
      override def executionContext = controllerComponents.executionContext
  }
   
  
  def withOptUser = new ActionRefiner[Request, OptionalUserRequest] with ActionBuilder[OptionalUserRequest, AnyContent]{
      def refine[A](input: Request[A]) = {
         securityLogger.trace(s"optUserToken enter: ${input.uri} $AuthTokenHeader ${input.headers.get(AuthTokenHeader)}")
         val result = (for{
            token <- input.headers.get(AuthTokenHeader)
            userId <- cache.get[Long](token)
          } yield {
            securityLogger.trace(s"optUserToken from cache: $userId ${input.uri} $AuthTokenHeader ${input.headers.get(AuthTokenHeader)}")
            betterDb.userById(userId).map{ user =>
                securityLogger.trace(s"optUserToken from db: $userId ${user.username} ${input.uri} $AuthTokenHeader ${input.headers.get(AuthTokenHeader)}")
                Right(new OptionalUserRequest(Some(user), input))
            }.recoverWith{ case e: AccessViolationException =>  
                securityLogger.trace(s"optUserToken no db match: $userId ${input.uri} $AuthTokenHeader ${input.headers.get(AuthTokenHeader)}")
                Future.successful(Right(new OptionalUserRequest(None, input)))
            }
          }).getOrElse{ 
               securityLogger.trace(s"optUserToken cache: ${input.uri} $AuthTokenHeader ${input.headers.get(AuthTokenHeader)}")
               Future.successful(Right(new OptionalUserRequest(None, input)))
          }
          result
      }          
      override def parser = controllerComponents.parsers.defaultBodyParser
      override def executionContext = controllerComponents.executionContext
  }
  
  
  def withUserA = new ActionRefiner[TokenRequest,UserRequest]{ 
      def refine[A](input: TokenRequest[A]) = {
         betterDb.userById(input.userId)
            .map{ user => Right{
              securityLogger.trace(s"withUser ${input.uri} could find user in db: ${input.userId}")
              new UserRequest(user, input) 
            }}
            .recoverWith{ case e: AccessViolationException =>
                 securityLogger.trace(s"withUser ${input.uri} could not find user in db: ${input.userId} ${e.getMessage}")
                 Future.successful(Left(Unauthorized(e.getMessage))) 
            }
      }
      override def executionContext = controllerComponents.executionContext
  }
  
  def withAdminA = new ActionRefiner[TokenRequest,AdminRequest]{
      def refine[A](input: TokenRequest[A]) = {
         betterDb.userById(input.userId)
            .map{ user =>
                  if(user.isAdmin){
                      securityLogger.trace(s"withAdmin ${input.uri} could find admin user in db: ${input.userId}")
                      Right(new AdminRequest(user, input))
                  } else {
                      securityLogger.trace(s"withAdmin ${input.uri} could find user in db: ${input.userId} but noadmin")
                      Left(Unauthorized("you must be admin"))
                  }
            }.recoverWith{ case e: AccessViolationException =>
                 securityLogger.trace(s"withAdmin ${input.uri} could not find user in db: ${input.userId} ${e.getMessage}")
                 Future.successful(Left(Unauthorized(e.getMessage))) 
            }
      }
      override def executionContext = controllerComponents.executionContext
  }
  
  def withUser = { hasToken andThen withUserA }
    
  def withAdmin = { hasToken andThen withAdminA }
 
  def betterException(future: Future[Result]): Future[Result] = {
      def toJs(e: Exception) = Json.obj("error" -> e.getMessage)
      future.recoverWith {
        case e: AccessViolationException => Logger.debug(e.getMessage); Future.successful(Unauthorized(toJs(e)))
        case e: ItemNotFoundException => Logger.debug(e.getMessage); Future.successful(NotFound(toJs(e)))
        case e: ValidationException => Logger.debug(e.getMessage); Future.successful(NotAcceptable(toJs(e)))
        case e: java.sql.SQLException => Logger.debug(e.getMessage); Future.successful(InternalServerError(toJs(e)))
        case e: AskTimeoutException => Logger.debug(e.getMessage); Future.successful(InternalServerError(toJs(e)))
        case e: org.apache.commons.mail.EmailException => Logger.debug(e.getMessage); Future.successful(InternalServerError(toJs(e)))
      }
  }

  
}