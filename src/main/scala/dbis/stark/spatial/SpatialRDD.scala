package dbis.stark.spatial

import dbis.stark.spatial.indexed.RTree
import dbis.stark.spatial.indexed.persistent.PersistedIndexedSpatialRDDFunctions
import dbis.stark.{Distance, STObject}
import org.apache.spark.{Dependency, OneToOneDependency, Partition, SparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.util.collection.ExternalAppendOnlyMap

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag


/**
 * A base class for spatial RDD without indexing
 *
 */
abstract class SpatialRDD[G <: STObject : ClassTag, V: ClassTag](
    @transient private val _sc: SparkContext,
    @transient private val _deps: Seq[Dependency[_]]
  ) extends RDD[(G,V)](_sc, _deps) {

  def this(@transient oneParent: RDD[(G,V)]) =
    this(oneParent.context , List(new OneToOneDependency(oneParent)))

  override val partitioner = firstParent[(G,V)].partitioner  
    
  /**
   * We do not repartition our data.
   */
  override protected def getPartitions: Array[Partition] = firstParent[(G,V)].partitions

  /**
   * Find all elements that are within a given radius around the given object
   */
  def withinDistance(
      qry: G, 
      maxDist: Distance,
      distFunc: (STObject,STObject) => Distance
    ) = new PlainSpatialRDDFunctions(this).withinDistance(qry, maxDist, distFunc)

  /**
   * Compute an intersection of the elements in this RDD with the given geometry
   */
  def intersects(qry: G) = new PlainSpatialRDDFunctions(this).intersects(qry) 

  /**
   * Find all elements that are contained by the given geometry
   *
   * @param qry The Geometry that should contains elements of this RDD
   * @return Returns an RDD containing the elements of this RDD that are completely contained by qry
   */
  def containedby(qry: G) = new PlainSpatialRDDFunctions(this).containedby(qry)

  /**
   * Find all elements that contain the given geometry.
   *
   * @param g The geometry that must be contained by other geometries in this RDD
   * @return Returns an RDD consisting of all elements in this RDD that contain the given geometry g
   */
  def contains(g: G) = new PlainSpatialRDDFunctions(this).contains(g)

  /**
   * Find the k nearest neighbors of the given geometry in this RDD
   *
   * @param qry The geometry to find the nearest neighbors of
   * @param k The number of nearest neighbors to find
   * @return Returns an RDD containing the k nearest neighbors of qry
   */
  def kNN(qry: G, k: Int, distFunc: (STObject, STObject) => Distance): RDD[(G,(Distance,V))] = new PlainSpatialRDDFunctions(this).kNN(qry, k,distFunc)

}

/**
 * A helper companion object that contains implicit conversion methods to convert
 * simple RDDs to SpatialRDDs
 */
object SpatialRDD {

  protected[stark] def createExternalMap[G : ClassTag, V : ClassTag, V2: ClassTag](): ExternalAppendOnlyMap[G, (V,V2), ListBuffer[(V,V2)]] = {
    
    type ValuePair = (V,V2)
    type Combiner = ListBuffer[ValuePair]
    
    val createCombiner: ( ValuePair => Combiner) = pair => ListBuffer(pair)
    
    val mergeValue: (Combiner, ValuePair) => Combiner = (list, value) => list += value
    
    val mergeCombiners: ( Combiner, Combiner ) => Combiner = (c1, c2) => c1 ++= c2
    
    new ExternalAppendOnlyMap[G, ValuePair, Combiner](createCombiner, mergeValue, mergeCombiners)
    
  }  
  
  
  /**
   * Convert an RDD to a "plain" spatial RDD which uses no indexing.
   *
   * @param rdd The RDD to convert
   * @return Returns a SpatialRDDFunctions object that contains spatial methods
   */
	implicit def convertSpatialPlain[G <: STObject : ClassTag, V: ClassTag](rdd: RDD[(G, V)]): PlainSpatialRDDFunctions[G, V]
    = new PlainSpatialRDDFunctions[G,V](rdd)

	/**
	 * Convert an RDD to a SpatialRDD which uses persisted indexing.
	 *
	 * @param rdd The RDD to convert
	 * @return Returns a IndexedSpatialRDDFunctions object that contains spatial methods that use indexing
	 */
	implicit def convertSpatialPersistedIndexing[G <: STObject : ClassTag, V: ClassTag](rdd: RDD[RTree[G,(G,V)]]): PersistedIndexedSpatialRDDFunctions[G, V]
    = new PersistedIndexedSpatialRDDFunctions(rdd)

}


