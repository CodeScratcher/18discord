package model

data class TrainEntry(val train: Train, var remaining: Int)

class GameState(
    val players: HashMap<String, Player>,
    val companies: HashMap<String, Company>,
    val shares: ShareRegistry,
    val map: BoardMap,
    val trainMarket: MutableMap<String, TrainEntry>) {

    fun runHour() {
        for (company in companies.values) {
            for (line in company.lines) {
                var sum = 0
                var pull = 1f

                for (edge in line.edges) {
                    sum += edge.source.population.toInt() / edge.source.stops
                    pull *= edge.dest.destFavorability
                }

                sum.coerceIn(0, line.trains.sumOf { (_, capacity, _) -> capacity })

                company.profit((sum * pull).toInt())
            }
        }
    }
}