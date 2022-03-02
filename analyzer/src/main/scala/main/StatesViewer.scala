package main

import model.BooleanNetwork
import java.awt.image.BufferedImage

object StatesViewer extends App {

  val n = 200
  val k = 3
  val bn1 = BooleanNetwork.random(n, k, 0.1)
  val bn2 = BooleanNetwork.random(n, k, 0.5)
  val bn3 = BooleanNetwork.random(n, k, 0.2113248654051871)

  def generateImage(bn:BooleanNetwork):BufferedImage = {
    val length = 400
    val perturbationEvery = 100
    val perturbations = 100

    val res = (0 until length).grouped(perturbationEvery).scanLeft(Seq(bn))({
      case (bns, range) =>
        val nexts = range.scanLeft(bns.last)({
          case (bn, _) => bn.step()
        })
        nexts.drop(1).dropRight(1) :+ nexts.last.invertRandomStates(perturbations).step()
    }).drop(1).flatten.toList.map(_.states).toIndexedSeq
    val img = new BufferedImage(length, n, BufferedImage.TYPE_INT_RGB)
    val flattenedData = new Array[Int](length * n * 3)
    (0 until length).foreach(i => {
      (0 until n).foreach(j => {
        val index = j * length + i
        if((i+1) % perturbationEvery == 0) {
          flattenedData(index * 3 + 0) = if (res(i)(j)) 0 else 255
          flattenedData(index * 3 + 1) = if (res(i)(j)) 0 else 0
          flattenedData(index * 3 + 2) = if (res(i)(j)) 0 else 0
        } else {
          flattenedData(index * 3 + 0) = if (res(i)(j)) 0 else 255
          flattenedData(index * 3 + 1) = if (res(i)(j)) 0 else 255
          flattenedData(index * 3 + 2) = if (res(i)(j)) 0 else 255
        }

      })
    })
    img.getRaster.setPixels(0, 0, length, n, flattenedData)
    img
  }

  import javax.swing.ImageIcon
  import javax.swing.JFrame
  import javax.swing.JLabel
  import javax.swing.JPanel

  import javax.swing.ImageIcon
  import javax.swing.JFrame
  import javax.swing.JLabel
  import java.awt.FlowLayout

  val frame = new JFrame
  frame.getContentPane.setLayout(new FlowLayout)
  frame.getContentPane.add(new JLabel(new ImageIcon(generateImage(bn1))))
  frame.getContentPane.add(new JLabel(new ImageIcon(generateImage(bn2))))
  frame.getContentPane.add(new JLabel(new ImageIcon(generateImage(bn3))))
  frame.pack()
  frame.show()

  println("ok")
}
