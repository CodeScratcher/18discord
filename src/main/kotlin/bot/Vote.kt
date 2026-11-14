package bot

import model.Company
import model.Edge
import model.Player
import model.Train
import net.dv8tion.jda.api.entities.Message

sealed class Vote(val company: Company, var timesAround: Int, val message: Message) {
    val votes: MutableMap<Player, Boolean> = HashMap()
    class BuyTrain(company: Company, timesAround: Int, val train: String, message: Message) :
        Vote(company, timesAround, message)
    class BuyRail(company: Company, timesAround: Int, val edge: String, message: Message) :
        Vote(company, timesAround, message)
    class MakeLine(company: Company, timesAround: Int,
                   val edges: List<String>, val trains: List<String>, message: Message) :
        Vote(company, timesAround, message)
}