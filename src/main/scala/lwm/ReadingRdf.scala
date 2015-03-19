package lwm

import java.io.File

import org.openrdf.repository.RepositoryConnection
import org.openrdf.repository.http.HTTPRepository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore
import org.openrdf.sail.nativerdf.NativeStore
import org.w3.banana._
import org.w3.banana.sesame._

import scala.util.{Success, Failure, Try}


class PrefixBuilder[Rdf <: RDF](val prefixName: String, val prefixIri: String)(implicit ops: RDFOps[Rdf]) extends Prefix[Rdf] {

  import ops._

  override def toString: String = "Prefix(" + prefixName + ")"

  def apply(value: String): Rdf#URI = makeUri(prefixIri + value)

  def unapply(iri: Rdf#URI): Option[String] = {
    val uriString = fromUri(iri)
    if (uriString.startsWith(prefixIri))
      Some(uriString.substring(prefixIri.length))
    else
      None
  }

  def getLocalName(iri: Rdf#URI): Try[String] =
    unapply(iri) match {
      case None => Failure(LocalNameException(this.toString + " couldn't extract localname for " + iri.toString))
      case Some(localname) => Success(localname)
    }

}

class LWMPrefix[Rdf <: RDF](ops: RDFOps[Rdf]) extends PrefixBuilder("lwm", "http://lwm.fh-koeln.de/ns/")(ops) {
  val name = apply("name")
  val Student = apply("Student")
}

object LWMPrefix {
  def apply[Rdf <: RDF : RDFOps](implicit ops: RDFOps[Rdf]) = new LWMPrefix(ops)
}

object ReadingRdf extends SesameModule with App {
  import scala.concurrent.duration._
  import ops._

  val dataDir = new File("sesame")
  val memStore = new MemoryStore(dataDir)
  memStore.setSyncDelay(5.seconds.toMillis)
  val repo = new SailRepository(memStore)

  repo.initialize()
  val connection = repo.getConnection



  val uri = makeUri("https://lwm.fh-koeln.de/ns")

  val lwm = LWMPrefix[Sesame]


  val g = rdfStore.getGraph(connection, uri)
  g.map { graph =>
    // val sub = makeUri(s"http://lwm.fh-koeln.de/person_${System.nanoTime}")
    // val pred = lwm.name
   // val lit = valueFactory.createLiteral("robert")
    // graph.add(sub, pred, lit)
    println(graph)
    // rdfStore.appendToGraph(connection, uri, graph)

    connection.close()
    repo.shutDown()
  }




  execBuilder[A](f: RepositoryConnection => A)(implicit repo: HTTPRepository) {
    val conn = repo.getConnection
    val res = f(conn)
    conn.close()
    res
  }
}