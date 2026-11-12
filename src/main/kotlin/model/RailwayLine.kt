package model

data class Train(val name: String, val capacity: Int, val speed: Float, val cost: Int)
data class RailwayLine(val edges: List<Edge>, val trains: List<Train>)