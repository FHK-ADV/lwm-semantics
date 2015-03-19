package lwm.store

import org.w3.banana.sesame.Sesame
import org.w3.banana.{RDFOps, RDF}

trait RepositorySerialiser[T, U <: RDF#URI, G <: RDF#Graph, Rdf <: RDF] {
  def serialise(entity: T)(implicit ops: RDFOps[Rdf]): (U, G)
  def deserialise(graph: G)(implicit ops: RDFOps[Rdf]): T
}


