import model.BoardMap
import model.GameState
import model.Player
import model.ShareRegistry
import java.util.UUID

val shareRegistry = ShareRegistry(HashMap(), HashMap(), HashMap())
val map = BoardMap(ArrayList(), HashMap())
val gameState = GameState(HashMap(), HashMap(), shareRegistry, map, HashMap())
val player1 = Player("ASDFASDF", "skiscratcher", 100, gameState)
val player2 = Player("asdfkjhahsjdhgfak", "moon483", 100, gameState)

fun main() {
    player1.openCompany("B&O", "Baltimore & Ohio", 20)
    player2.openCompany("B&M", "Boston & Maine", 10)
    player1.buyShare("B&M-3")
    player1.buyShare("B&O-3")
    player1.sellShare("B&O-2", 15)
    player2.buyShare("B&0-2")
    println(shareRegistry)
}