package model

import kotlin.math.floor

class Company(val mark: String, val name: String, var funds: Int, val gameState: GameState) {
    val purchasedEdges: MutableList<Edge> = ArrayList()
    val lines: MutableList<RailwayLine> = ArrayList()
    val trains: MutableList<Train> = ArrayList()
    var lastRecordedProfit = 0
    var lastPayout = 0

    fun createIpo(player: Player, price: Int) {
        player.wealth -= 2 * price
        gameState.shares.shares["$mark-1"] = Share(this, player)
        gameState.shares.shares["$mark-2"] = Share(this, player)

        for (i in 3..10) {
            gameState.shares.IPO["$mark-$i"] = IPO(company = this, price = price)
        }
    }

    fun buyEdge(edgeId: String) {
        if (edgeId in gameState.map.edges) {
            val edge = gameState.map.edges[edgeId]!!
            if (funds >= floor(edge.difficulty * COST)) {
                purchasedEdges += edge
                edge.difficulty *= 1.2f
                funds -= floor(edge.difficulty * COST).toInt()
            }
        }
    }

    fun validateLine(edges: List<Edge>): Boolean {
        val edgeChecker: MutableMap<Node, Pair<Boolean, Boolean>> = HashMap()

        for (edge in edges) {
            val source = edge.source
            val dest = edge.dest
            if (source in edgeChecker) {
                if (edgeChecker[source]!!.first) return false
                edgeChecker[source] = Pair(true, edgeChecker[source]!!.second)
            }
            else {
                edgeChecker[source] = Pair(true, false)
            }

            if (dest in edgeChecker) {
                if (edgeChecker[dest]!!.second) return false
                edgeChecker[dest] = Pair(edgeChecker[source]!!.first, true)
            }
            else {
                edgeChecker[source] = Pair(true, false)
            }
        }

        var hasSource = false
        var hasDest = false

        for (v in edgeChecker.values) {
            if (!v.first) {
                if (hasSource) return false
                else hasSource = true
            }
            if (!v.second) {
                if (hasDest) return false
                else hasDest = true
            }
        }

        return true
    }

    fun makeLine(edges: MutableList<Edge>, trains: MutableList<Train>) {
        for (edge in edges) {
            edge.source.stops++
        }
        lines.add(RailwayLine(edges, trains))
    }

    fun buyTrain(train: Train): Boolean {
        if (train.cost > funds && gameState.trainMarket[train.name]!!.remaining > 0) {
            funds -= train.cost
            gameState.trainMarket[train.name]!!.remaining - 1
            return true
        }
        return false
    }

    fun profit(amount: Int) {
        funds += amount / 10 * 9
        val companyShares = gameState.shares.shares.values.filter { (company, _) -> equals(company) }
        for (share in companyShares) {
            share.player.wealth += amount / 10 / companyShares.size
            lastPayout = amount / 10 / companyShares.size
        }

        lastRecordedProfit = amount
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Company -> other.mark == mark
            else -> false
        }
    }

    override fun hashCode(): Int {
        return mark.hashCode()
    }

    override fun toString(): String {
        return "Company(mark='$mark', name='$name', funds=$funds)"
    }


}