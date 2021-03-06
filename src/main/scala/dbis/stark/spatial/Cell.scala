package dbis.stark.spatial

/**
 * This class represents a cell or Partition
 * 
 * @param range The computed bounds of the cell
 * @param extent The theoretical bounds of the cell with the minimum and maximum extent of the contained geometries
 */
case class Cell(var id: Int, range: NRectRange, var extent: NRectRange) extends Cloneable {
  override def hashCode() = range.hashCode()
  override def equals(other: Any) = other match {
    case Cell(_,otherRange, _) => range.equals(otherRange)
    case _ => false
  }

  /**
    * Updates this cell's extent in place!
    * @param r The range to extent by
    */
  def extendBy(r: NRectRange): Unit = { extent = extent.extend(r) }
  
  override def clone(): Cell = Cell(id, range.clone(), extent.clone())
}

object Cell {
  
  def apply(r: NRectRange): Cell = Cell(-1, r,r)
  def apply(id: Int, r: NRectRange): Cell = Cell(id, r,r)
  def apply(r: NRectRange, e: NRectRange):Cell = Cell(-1, r, e)
}
