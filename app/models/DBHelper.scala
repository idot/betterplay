package models

import play.api.db.slick.Config.driver.simple._
 
trait Profile {
	val profile: scala.slick.driver.JdbcProfile
	val simple:profile.simple.type = profile.simple
}
 
trait CrudComponent { this: Profile =>

 
abstract class Crud[T <: Table[E] with IdentifiableTable[PK], E <: Entity[PK], PK: BaseColumnType](implicit session: Session){
	val query: TableQuery[T]
	def count: Int = query.length.run
	def findAll: List[E] = query.list()
	def queryById(id: PK) = query.filter(_.id === id)
	def findOne(id: PK): Option[E] = queryById(id).firstOption
	def add(m: E): PK = (query returning query.map(_.id)) += m
	def withId(model: E, id: PK): E
	def extractId(m: E): Option[PK] = m.id
	def save(m: E): E = {
	  extractId(m) match {
	   case Some(id) =>
		 queryById(id).update(m)
		 m
	    case None => withId(m, add(m))
	   }
	}
	def saveAll(ms: E*): Option[Int] = query ++= ms
	def deleteById(id: PK): Int = queryById(id).delete
	def delete(m: E): Int = {
	   extractId(m) match {
	      case Some(id) => deleteById(id)
          case None => 0
       }
    }
}
}

trait Entity[PK] {
   def id: Option[PK]
}
 
trait IdentifiableTable[I] {
   def id: scala.slick.lifted.Column[I]
}