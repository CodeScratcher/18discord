package model

class GameState(
    val players: MutableList<Player>,
    val companies: MutableList<Company>,
    val shares: ShareRegistry,
    val map: BoardMap,
    val trainMarket: MutableMap<Train, Int>) {

    fun runHour() {
        for (company in companies) {
            for (line in company.lines) {
                var sum = 0
                var pull = 1f

                for (edge in line.edges) {
                    sum += edge.source.population.toInt() / edge.source.stops
                    pull *= edge.dest.destFavorability
                }

                company.profit((sum * pull).toInt())
            }
        }
    }
}