package com.barrybecker4.graph.algorithms.kshortestpaths

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.algorithms.shortestpaths.DijkstraHeap
import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}

import scala.collection.mutable

/**
 * Eppstein's algorithm for the k shortest s–t walks in a directed graph with
 * non-negative edge weights. Walks may repeat vertices (unlike [[YensAlgorithm]]).
 *
 * D. Eppstein, "Finding the k Shortest Paths," SIAM J. Comput. 28(2):652–673, 1998.
 *
 * After one backward Dijkstra and an `O(m + n log n)` leftist-heap build, the
 * next walks are selected in `O(k log k)` plus time proportional to the total
 * length of the emitted walks.
 */
class EppsteinsAlgorithm(graph: DirectedGraph) extends KShortestPathsFinder {

  private val edges: IndexedSeq[DirectedEdge] = graph.edges
  private val n: Int = {
    var maxId = graph.numVertices - 1
    for (e <- edges) {
      if (e.source > maxId) maxId = e.source
      if (e.destination > maxId) maxId = e.destination
    }
    if (maxId < 0) 0 else maxId + 1
  }

  /** Incoming edge indices for each vertex, preserving parallel edges. */
  private val incomingEdgeIndices: Array[List[Int]] = {
    val arr = Array.fill(n)(List.empty[Int])
    var i = 0
    while (i < edges.length) {
      arr(edges(i).destination) = i :: arr(edges(i).destination)
      i += 1
    }
    arr
  }

  def findKShortestPaths(source: Int, destination: Int, k: Int): Seq[Path] = {
    require(k > 0, "The number of paths should be positive")
    edges.foreach { e =>
      require(e.weight >= 0.0, s"Edge weights must be non-negative: $e")
    }

    val tree = buildShortestPathTree(destination)
    if (!tree.dist.contains(source)) return Nil

    val outHeaps = buildOutHeaps(tree)
    val heaps = buildSharedHeaps(tree, outHeaps)
    search(source, destination, k, tree, heaps)
  }

  private final case class Tree(
      dist: Map[Int, Double],
      /** Index of the tree edge leaving `v` toward the destination, if any. */
      treeEdgeIndex: Map[Int, Int],
      treeSuccessor: Map[Int, Int]
  )

  /** Leftist heap node holding one sidetrack edge. */
  private final class Leftist(
      val edgeIndex: Int,
      val delta: Double,
      val left: Leftist | Null,
      val right: Leftist | Null,
      val npl: Int
  )

  private object Leftist {
    def nplOf(node: Leftist | Null): Int = if (node == null) -1 else node.npl

    def less(a: Leftist, b: Leftist): Boolean =
      a.delta < b.delta || (a.delta == b.delta && a.edgeIndex < b.edgeIndex)

    def singleton(edgeIndex: Int, delta: Double): Leftist =
      new Leftist(edgeIndex, delta, null, null, 0)

    def merge(a: Leftist | Null, b: Leftist | Null): Leftist | Null = {
      if (a == null) return b
      if (b == null) return a
      val (x, y) = if (less(a, b)) (a, b) else (b, a)
      val mergedRight = merge(x.right, y)
      val (left, right) =
        if (nplOf(x.left) < nplOf(mergedRight)) (mergedRight, x.left)
        else (x.left, mergedRight)
      new Leftist(x.edgeIndex, x.delta, left, right, nplOf(right) + 1)
    }

    def fromSidetracks(items: Seq[(Int, Double)]): Leftist | Null =
      items.foldLeft(null: Leftist | Null) { case (heap, (idx, delta)) =>
        merge(heap, singleton(idx, delta))
      }
  }

  /** Backward Dijkstra from `destination`: distances are path lengths to the destination. */
  private def buildShortestPathTree(destination: Int): Tree = {
    val dist = mutable.Map[Int, Double]()
    val treeEdgeIndex = mutable.Map[Int, Int]()
    val treeSuccessor = mutable.Map[Int, Int]()
    val settled = mutable.Set[Int]()
    val heap = DijkstraHeap()

    dist(destination) = 0.0
    heap.enqueue(destination, 0.0)

    var done = false
    while (!done) {
      heap.extractMin { (node, distQueued) =>
        !settled.contains(node) &&
          DijkstraHeap.sameDistance(distQueued, dist.getOrElse(node, Double.MaxValue))
      } match {
        case None => done = true
        case Some(node) =>
          settled += node
          for (idx <- incomingEdgeIndices(node)) {
            val e = edges(idx)
            val prev = e.source
            val candidate = dist(node) + e.weight
            val best = dist.getOrElse(prev, Double.PositiveInfinity)
            if (candidate < best || (candidate == best && treeEdgeIndex.get(prev).forall(_ > idx))) {
              dist(prev) = candidate
              treeEdgeIndex(prev) = idx
              treeSuccessor(prev) = node
              heap.enqueue(prev, candidate)
            }
          }
      }
    }

    Tree(dist.toMap, treeEdgeIndex.toMap, treeSuccessor.toMap)
  }

  private def buildOutHeaps(tree: Tree): Array[Leftist | Null] = {
    val buckets = Array.fill(n)(List.empty[(Int, Double)])
    var i = 0
    while (i < edges.length) {
      val e = edges(i)
      val u = e.source
      val v = e.destination
      if (tree.dist.contains(u) && tree.dist.contains(v) && tree.treeEdgeIndex.get(u) != Some(i)) {
        val delta = e.weight + tree.dist(v) - tree.dist(u)
        buckets(u) = (i, delta) :: buckets(u)
      }
      i += 1
    }
    val heaps = Array.fill[Leftist | Null](n)(null)
    var u = 0
    while (u < n) {
      heaps(u) = Leftist.fromSidetracks(buckets(u))
      u += 1
    }
    heaps
  }

  private def buildSharedHeaps(tree: Tree, outHeaps: Array[Leftist | Null]): Array[Leftist | Null] = {
    val heaps = Array.fill[Leftist | Null](n)(null)
    val ordered = tree.dist.keys.toSeq.sortBy(v => (tree.dist(v), v))
    for (v <- ordered) {
      tree.treeSuccessor.get(v) match {
        case None => heaps(v) = outHeaps(v)
        case Some(succ) => heaps(v) = Leftist.merge(outHeaps(v), heaps(succ))
      }
    }
    heaps
  }

  private final case class QueueEntry(
      weight: Double,
      prefix: Vector[Int],
      node: Leftist
  )

  private def search(
      source: Int,
      destination: Int,
      k: Int,
      tree: Tree,
      heaps: Array[Leftist | Null]
  ): Seq[Path] = {
    val results = mutable.ListBuffer.empty[Path]
    results += recover(source, destination, Vector.empty, tree)

    if (results.size == k) return results.toList

    val ordering: Ordering[QueueEntry] = Ordering.fromLessThan { (a, b) =>
      if (a.weight != b.weight) a.weight > b.weight
      else {
        val aSeq = a.prefix :+ a.node.edgeIndex
        val bSeq = b.prefix :+ b.node.edgeIndex
        compareSeq(aSeq, bSeq) > 0
      }
    }
    val queue = mutable.PriorityQueue.empty[QueueEntry](using ordering)

    if (source >= 0 && source < heaps.length && heaps(source) != null) {
      val root = heaps(source)
      queue.enqueue(QueueEntry(tree.dist(source) + root.delta, Vector.empty, root))
    }

    while (queue.nonEmpty && results.size < k) {
      val entry = queue.dequeue()
      val sequence = entry.prefix :+ entry.node.edgeIndex
      results += recover(source, destination, sequence, tree)

      val head = edges(entry.node.edgeIndex).destination
      if (head >= 0 && head < heaps.length && heaps(head) != null) {
        val cont = heaps(head)
        queue.enqueue(QueueEntry(entry.weight + cont.delta, sequence, cont))
      }

      def pushChild(child: Leftist | Null): Unit =
        if (child != null) {
          queue.enqueue(
            QueueEntry(entry.weight - entry.node.delta + child.delta, entry.prefix, child)
          )
        }

      pushChild(entry.node.left)
      pushChild(entry.node.right)
    }

    results.toList
  }

  private def compareSeq(a: Vector[Int], b: Vector[Int]): Int = {
    val len = math.min(a.length, b.length)
    var i = 0
    while (i < len) {
      if (a(i) != b(i)) return a(i).compareTo(b(i))
      i += 1
    }
    a.length.compareTo(b.length)
  }

  private def recover(
      source: Int,
      destination: Int,
      sidetracks: Vector[Int],
      tree: Tree
  ): Path = {
    if (sidetracks.isEmpty && source == destination)
      return Path(0.0, List(source))

    val nodes = mutable.ListBuffer[Int](source)
    var weight = 0.0
    var current = source
    var si = 0

    while (si < sidetracks.length || current != destination) {
      if (si < sidetracks.length) {
        val e = edges(sidetracks(si))
        if (current == e.source) {
          nodes += e.destination
          weight += e.weight
          current = e.destination
          si += 1
        } else {
          val idx = tree.treeEdgeIndex.getOrElse(
            current,
            throw new IllegalStateException(s"No tree edge from $current while recovering $sidetracks")
          )
          val te = edges(idx)
          nodes += te.destination
          weight += te.weight
          current = te.destination
        }
      } else {
        val idx = tree.treeEdgeIndex.getOrElse(
          current,
          throw new IllegalStateException(s"No tree edge from $current toward $destination")
        )
        val te = edges(idx)
        nodes += te.destination
        weight += te.weight
        current = te.destination
      }
    }

    Path(weight, nodes.toList)
  }
}
