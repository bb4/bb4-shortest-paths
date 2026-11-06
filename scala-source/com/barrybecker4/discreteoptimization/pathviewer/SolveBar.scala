package com.barrybecker4.discreteoptimization.pathviewer

import com.barrybecker4.discreteoptimization.pathviewer.AlgorithmKind.*
import com.barrybecker4.discreteoptimization.pathviewer.render.PlaybackControl

import java.awt.FlowLayout
import javax.swing.*

class SolveBar(onSolve: SolveRequest => Unit, onCompare: Option[AlgorithmKind] => Unit)
  extends JPanel(new FlowLayout(FlowLayout.LEFT)) {

  private val algorithm = new JComboBox[AlgorithmKind](Array(Dijkstra, ModifiedDijkstra, Yen))
  private val source = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1))
  private val target = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1))
  private val k = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1))
  private val solveButton = new JButton("Solve")
  private val animate = new JCheckBox("Animate")
  private val playButton = new JButton("Play")
  private val pauseButton = new JButton("Pause")
  private val stepButton = new JButton("Step")
  private val skipButton = new JButton("Skip")
  private val compareBox = new JCheckBox("Compare")
  private val compareAlgorithmBox = new JComboBox[AlgorithmKind](Array(Dijkstra, ModifiedDijkstra, Yen))
  private val showOther = new JButton("Show other")
  private var showOtherAction: () => Unit = () => ()
  private val status = new JLabel("Open a graph, then Solve")
  private val legend = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0))
  private var playback: PlaybackControl = new PlaybackControl

  compareBox.setEnabled(false)
  compareAlgorithmBox.setEnabled(false)
  k.setEnabled(false)

  algorithm.addActionListener(_ => k.setEnabled(algorithm.getSelectedItem == Yen))
  solveButton.addActionListener(_ => onSolve(selectedRequest()))
  playButton.addActionListener(_ => playback.play())
  pauseButton.addActionListener(_ => playback.pause())
  stepButton.addActionListener(_ => playback.step())
  skipButton.addActionListener(_ => playback.skip())
  compareBox.addActionListener(_ => onCompare(this.compareAlgorithm()))
  showOther.setEnabled(false)
  showOther.addActionListener(_ => showOtherAction())

  add(new JLabel("Algorithm"))
  add(algorithm)
  add(new JLabel("Source"))
  add(source)
  add(new JLabel("Target"))
  add(target)
  add(new JLabel("k"))
  add(k)
  add(solveButton)
  add(animate)
  add(playButton)
  add(pauseButton)
  add(stepButton)
  add(skipButton)
  add(compareBox)
  add(compareAlgorithmBox)
  add(showOther)
  add(status)

  def setVertexRange(maxId: Int): Unit = {
    source.setModel(new SpinnerNumberModel(0, 0, maxId.max(0), 1))
    val initialTarget = if (maxId > 0) maxId else 0
    target.setModel(new SpinnerNumberModel(initialTarget, 0, maxId.max(0), 1))
  }

  def setSource(vertex: Int): Unit = source.setValue(vertex)

  def setStatus(text: String): Unit = status.setText(text)

  def setLegend(entries: Seq[(Int, java.awt.Color)]): Unit = {
    legend.removeAll()
    entries.foreach { (rank, color) =>
      val swatch = new JLabel(rank.toString)
      swatch.setOpaque(true)
      swatch.setBackground(color)
      legend.add(swatch)
    }
    if (legend.getParent == null) add(legend)
    revalidate()
    repaint()
  }

  def setBusy(busy: Boolean): Unit = solveButton.setEnabled(!busy)

  def bindPlayback(control: PlaybackControl): Unit = playback = control

  def animateSelected: Boolean = animate.isSelected

  def setAnimateDefault(on: Boolean): Unit = animate.setSelected(on)

  private[pathviewer] def selectAlgorithm(kind: AlgorithmKind): Unit = algorithm.setSelectedItem(kind)

  private[pathviewer] def kSpec: (Int, Int, Int, Boolean) = {
    val m = k.getModel.asInstanceOf[SpinnerNumberModel]
    (
      m.getNumber.intValue(),
      m.getMinimum.asInstanceOf[Number].intValue(),
      m.getMaximum.asInstanceOf[Number].intValue(),
      k.isEnabled
    )
  }

  private[pathviewer] def compareBoxEnabled: Boolean = compareBox.isEnabled

  def selectedRequest(): SolveRequest =
    SolveRequest(
      algorithm.getSelectedItem.asInstanceOf[AlgorithmKind],
      source.getValue.asInstanceOf[Int],
      target.getValue.asInstanceOf[Int],
      k.getValue.asInstanceOf[Int]
    )

  def compareAlgorithm(): Option[AlgorithmKind] =
    if (compareBox.isSelected) Some(compareAlgorithmBox.getSelectedItem.asInstanceOf[AlgorithmKind])
    else None

  /** The frame enables Compare once a graph is loaded. */
  def setCompareEnabled(enabled: Boolean): Unit = {
    compareBox.setEnabled(enabled)
    compareAlgorithmBox.setEnabled(enabled && compareBox.isSelected)
  }

  def setShowOtherEnabled(enabled: Boolean): Unit = showOther.setEnabled(enabled)

  def onShowOther(action: () => Unit): Unit = showOtherAction = action

  private[pathviewer] def selectCompare(kind: Option[AlgorithmKind]): Unit = {
    kind.foreach(compareAlgorithmBox.setSelectedItem)
    compareBox.setSelected(kind.isDefined)
  }
  private[pathviewer] def statusText: String = status.getText
  private[pathviewer] def showOtherEnabled: Boolean = showOther.isEnabled
  private[pathviewer] def clickShowOther(): Unit = showOther.doClick()
}
