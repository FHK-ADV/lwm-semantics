package lwm.store

import java.io.File

import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore
import org.w3.banana.sesame.{Sesame, SesameModule}
import org.w3.banana.{RDF, RDFModule, RDFOps}

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}


trait RepositoryLike[Id, G] {
  def add[T](entity: T)(implicit serialiser: RepositorySerialiser[T, Id, G, Sesame]): Try[Id]

  def get[T](implicit serialiser: RepositorySerialiser[T, Id, G, Sesame]): List[Try[T]]

  def get[T](id: Id)(implicit serialiser: RepositorySerialiser[T, Id, G, Sesame]): Try[T]

  def close(): Unit
}

trait SemanticRepository[Id <: RDF#URI, G <: RDF#Graph] extends RepositoryLike[Id, G] with RDFModule


class SesameRepository(folder: File = new File("store"), syncInterval: FiniteDuration = 10.seconds) extends SemanticRepository[Sesame#URI, Sesame#Graph] with SesameModule {

  import ops._

  val ns = makeUri("http://lwm.gm.fh-koeln.de/ns/")

  val memStore = new MemoryStore(folder)
  memStore.setSyncDelay(syncInterval.toMillis)

  val repo = new SailRepository(memStore)
  repo.initialize()


  override def add[T](entity: T)(implicit serialiser: RepositorySerialiser[T, Sesame#URI, Sesame#Graph, Sesame]): Try[Sesame#URI] = {
    val connection = repo.getConnection
    val (uri, graph) = serialiser.serialise(entity)

    rdfStore.appendToGraph(connection, ns, graph).flatMap(_ => Success(uri))
  }


  override def close() = {
    repo.shutDown()
  }

  override def get[T](implicit serialiser: RepositorySerialiser[T, Rdf#URI, Rdf#Graph, Rdf]): List[Try[T]] = {
    val connection = repo.getConnection
    """
      |select ?student where {
      | ?student rdf:type lwm:Student
      |}
    """.stripMargin
    ???
  }

  override def get[T](id: Rdf#URI)(implicit serialiser: RepositorySerialiser[T, Rdf#URI, Rdf#Graph, Rdf]): Try[T] = {
    val connection = repo.getConnection

    rdfStore.getGraph(connection, id).map(graph => serialiser.deserialise(graph))
  }
}


object SesameRepository {
  def apply(folder: File, syncInterval: FiniteDuration) = new SesameRepository(folder, syncInterval)

  def apply(folder: File) = new SesameRepository(folder)

  def apply(syncInterval: FiniteDuration) = new SesameRepository(syncInterval = syncInterval)

  def apply() = new SesameRepository()
}


case class Student(name: String)

sealed trait ValidationResult

case class ValidationError(errors: List[String]) extends ValidationResult
case class ValidationSuccess(graph: Sesame#Graph) extends ValidationResult

object Implicits {

  implicit object StudentSerialiser extends RepositorySerialiser[Student, Sesame#URI, Sesame#Graph, Sesame] {
    object StudentModelValidator{
      def validate(graph: Sesame#Graph): ValidationResult = ???
    }

    override def serialise(entity: Student)(implicit ops: RDFOps[Sesame]): (Sesame#URI, Sesame#Graph) = {
      val g = ops.makeEmptyMGraph()
      val u = ops.makeUri(s"http://lwm.gm-koeln.de/students/student_${System.nanoTime()}")

      g.add(
        u,
        ops.makeUri("http://lwm.gm-koeln.de/ns/name"),
        ops.makeLiteral(entity.name, ops.makeUri("http://xsd.com/string"))
      )

      (u, g)
    }

    override def deserialise(graph: Sesame#Graph)(implicit ops: RDFOps[Sesame]): Try[Student] = {
      import scala.collection.JavaConversions._
      StudentModelValidator.validate(graph) match {
        case ValidationError(errors) =>
          Failure(new IllegalArgumentException(errors.mkString(",")))
        case ValidationSuccess(validatedGraph) =>
          val names = validatedGraph.filter(null, ops.makeUri("http://lwm.gm-koeln.de/ns/name"), null).iterator().toList.map { statement =>
            statement.getObject.stringValue()
          }

          Success(Student(names.head))
      }
    }
  }

}

object bla {

  import Implicits._

  val repo = SesameRepository()
  val ich = Student("robert")

  val maybeURI = repo.add(ich)

  maybeURI.map { uri =>
    val student = repo.get(uri)
  }

  val students = repo.get[Student]

}