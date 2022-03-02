package main

import main.BooleanNetworkViewer.bn
import model.BooleanNetwork

import java.awt.{BasicStroke, Color, Dimension, Graphics2D}
import javax.swing.Timer
import scala.swing.{MainFrame, Panel, SimpleSwingApplication}

object BooleanNetworkViewer extends App {

  val n = 100
  val k = 2
  val bn = BooleanNetwork.random(n, k, 0.5).copy(inputs = IndexedSeq(1, 2), outputs = IndexedSeq(1, 50))
  //.copy(connections = (0 until n).map(i => (0 until k).map(j =>(i + j)%n)))


  val viewer = BnViewer(bn)

  var i = 0
  viewer.startup(Array())
  Iterator.continually(()).foldLeft(bn)({
    case (bn, _) =>
      viewer.update(bn)
      Thread.sleep(500)
      i = i + 1
      if (i % 50 == 0) {
        bn.invertRandomStates(1)
      } else {
        bn.step()
      }

  })
}

case class BnViewer(bn: BooleanNetwork) extends SimpleSwingApplication {
  private val panel: PointAndLinesPanel = new PointAndLinesPanel(bn) {
    preferredSize = new Dimension(800, 800)
  }
  private val frame = new MainFrame {
    contents = panel
  }

  def top: MainFrame = frame

  def update(bn: BooleanNetwork): Unit = {
    panel.bn = bn
    frame.repaint()
  }

  private class PointAndLinesPanel(var bn: BooleanNetwork) extends Panel {

    private def points = (0 until bn.n).map(i => {
      val angle = Math.PI * 2 / bn.n * i
      val x = Math.cos(angle)
      val y = Math.sin(angle)
      (x, y)
    })

    override def paintComponent(g: Graphics2D) {
      println("Repainting")
      val margin = 0.05
      val minX = points.map(_._1).min - margin
      val maxX = points.map(_._1).max + margin
      val minY = points.map(_._2).min - margin
      val maxY = points.map(_._2).max + margin


      val width = g.getClipBounds.width.toFloat
      val height = g.getClipBounds.height.toFloat

      def t(p: (Double, Double), pWidth: Int = 0): (Int, Int) = ((width * (p._1 - minX) / (maxX - minX) + 0.5 - pWidth / 2.0).toInt,
        (height * (p._2 - minY) / (maxY - minY) + 0.5 - pWidth / 2.0).toInt)

      g.setColor(Color.BLACK)
      g.setStroke(new BasicStroke(1))
      bn.connections.indices.foreach(i => {
        val nodeConnections = bn.connections(i)
        val (x1, y1) = t(points(i))
        nodeConnections.indices.foreach(j => {
          g.setColor(if (bn.states(nodeConnections(j))) Color.RED else Color.BLACK)
          val (x2, y2) = t(points(nodeConnections(j)))
          g.drawLine(x1, y1, x2, y2)
        })
      })

      val pointWidth = (Math.min(width, height) / bn.n * 2).toInt
      g.setStroke(new BasicStroke())
      points.zipWithIndex.foreach({
        case (p, i) =>
          val color = if (bn.states(i)) {
            Color.RED
          } else {
            Color.BLACK
          }
          g.setColor(color)
          val (x, y) = t(p, pointWidth)
          g.fillOval(x, y, pointWidth, pointWidth)
      })

      val iPointWidth = (Math.min(width, height) / bn.n * 1.5).toInt
      g.setStroke(new BasicStroke())
      g.setColor(Color.GREEN)
      bn.inputs.foreach({ i =>
        val (x, y) = t(points(i), iPointWidth)
        g.fillOval(x, y, iPointWidth, iPointWidth)
      })
      val oPointWidth = (Math.min(width, height) / bn.n * 1).toInt
      g.setColor(Color.BLUE)
      bn.outputs.foreach({ i =>
        val (x, y) = t(points(i), oPointWidth)
        g.fillOval(x, y, oPointWidth, oPointWidth)
      })
    }
  }

}