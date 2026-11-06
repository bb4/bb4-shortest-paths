package com.barrybecker4.discreteoptimization.pathviewer

import com.barrybecker4.discreteoptimization.pathviewer.AlgorithmKind.*

import java.awt.FlowLayout
import javax.swing.*

class SolveBar(onSolve: SolveRequest => Unit, onCompare: Option[AlgorithmKind] => Unit)
  extends JPanel(new FlowLayout(FlowLayout.LEFT)) {

  private val algorithm = new JComboBox[AlgorithmKind](Array(Dijkstra, ModifiedDijkstra, Yen))
  private val source = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1))
  private val target = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1))
  private val k = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1))
  private val solveButton = new JButton("Solve")
  private val compareBox = new JCheckBox("Compare")
  private val compareAlgorithmBox = new JComboBox[AlgorithmKind](Array(Dijkstra, ModifiedDijkstra, Yen))
  private val status = new JLabel("Open a graph, then Solve")

  compareBox.setEnabled(false)
  compareAlgorithmBox.setEnabled(false)
  k.setEnabled(false)

  algorithm.addActionListener(_ => k.setEnabled(algorithm.getSelectedItem == Yen))
  solveButton.addActionListener(_ => onSolve(selectedRequest()))
  compareBox.addActionListener(_ => onCompare(this.compareAlgorithm()))

  add(new JLabel("Algorithm"))
  add(algorithm)
  add(new JLabel("Source"))
  add(source)
  add(new JLabel("Target"))
  add(target)
  add(new JLabel("k"))
  add(k)
  add(solveButton)
  add(compareBox)
  add(compareAlgorithmBox)
  add(status)

  def setVertexRange(maxId: Int): Unit = {
    source.setModel(new SpinnerNumberModel(0, 0, maxId.max(0), 1))
    val initialTarget = if (maxId > 0) maxId else 0
    target.setModel(new SpinnerNumberModel(initialTarget, 0, maxId.max(0), 1))
  }

  def setSource(vertex: Int): Unit = source.setValue(vertex)

  def setStatus(text: String): Unit = status.setText(text)

  def setBusy(busy: Boolean): Unit = solveButton.setEnabled(!busy)

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

  /** Task 6 calls this. Until then the checkbox stays disabled. */
  def setCompareEnabled(enabled: Boolean): Unit = {
    compareBox.setEnabled(enabled)
    compareAlgorithmBox.setEnabled(enabled && compareBox.isSelected)
  }
}
