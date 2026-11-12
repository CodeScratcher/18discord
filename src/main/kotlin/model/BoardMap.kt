package model

const val COST = 10

data class Node(val name: String, var destFavorability: Float, var population: Float, var stops: Int)
data class Edge(val source: Node, val dest: Node, var difficulty: Float)

data class BoardMap (val nodes: List<Node>, val edges: Map<String, Edge>)
