package bot

import model.Company
import model.Edge
import model.GameState
import model.Node
import model.Train
import model.TrainEntry
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import java.io.File
import java.util.TimerTask
import kotlin.concurrent.timer
import kotlin.concurrent.timerTask

const val STARTING_WEALTH = 100

data class Game(val guild: Guild,
                val mainChannel: MessageChannel,
                val state: GameState,
                val companyChannels: MutableMap<Company, TextChannel>, val votes: MutableMap<String, Vote>) {
    var tick = 0
    fun setup() {
        guild.updateCommands().addCommands(
            Commands.slash("join", "join the game as a player"),
            Commands.slash("buyshare", "buy a share on the market or on IPO")
                .addOption(OptionType.STRING, "share", "the share to buy", true, true),
            Commands.slash("sellshare", "put a share onto the market")
                .addOption(OptionType.STRING, "share", "the share to sell", true, true)
                .addOption(OptionType.INTEGER, "cost", "the cost of the share", true),
            Commands.slash("opencompany", "open a company")
                .addOption(OptionType.STRING, "mark", "the recording mark of a company", true)
                .addOption(OptionType.STRING, "name", "the name of a company", true)
                .addOption(OptionType.INTEGER, "cost", "the cost of a share", true),
            Commands.slash("status", "show player status"),
            Commands.slash("market", "show the market"),
            Commands.slash("map", "show the map"),
            Commands.slash("trains", "show what trains are available"),
            Commands.slash("vote", "call for a vote among a company you own")
                .addSubcommands(SubcommandData("buytrain", "buy a train as a company")
                    .addOption(OptionType.STRING, "company", "the company you own", true, true)
                    .addOption(OptionType.STRING, "train", "the train to buy", true, true),
                    SubcommandData("buyrail", "Buy track")
                        .addOption(OptionType.STRING, "company", "the company you own", true, true)
                        .addOption(OptionType.STRING, "rail", "the rail to buy", true, true),
                    SubcommandData("line", "Open a line")
                        .addOption(OptionType.STRING, "company", "the company you own", true, true))
        ).
        queue()

        this::class.java.getResourceAsStream("/trains.txt")!!.bufferedReader().forEachLine {
            val line = it.split(",");
            state.trainMarket[line[0]] =
                TrainEntry(Train(line[0], line[1].toInt(), line[2].toInt()), line[3].toInt())
        }

        val graph = this::class.java.getResourceAsStream("/graph.txt")!!.bufferedReader().readLines()
        val index = graph.indexOf("EDGES")
        val nodes = graph.subList(0, index)
        val edges = graph.subList(index+1, graph.size)

        nodes.forEach {
            val fields = it.split(",")
            state.map.nodes.add(Node(fields[0],fields[2].toFloat(), fields[1].toInt(), 0))
        }

        edges.forEach {
            val fields = it.split(",")
            val name = "${fields[0]}->${fields[1]}"
            val node0 = state.map.nodes.find { node -> node.name == fields[0] }
            val node1 = state.map.nodes.find { node -> node.name == fields[1] }
            if (node0 == null) println(fields[0])
            if (node1 == null) println(fields[1])
            state.map.edges[name] = Edge(node0!!, node1!!, fields[2].toFloat())
        }

        timer("ticks", false, 1000 * 60 * 30, 1000 * 60 * 30) {
            mainChannel.sendMessage("Tick ${++tick}").queue()

            state.runHour()
            for ((k, v) in companyChannels) {
                v.sendMessage("${k.name} earned ${k.lastRecordedProfit}, paying out ${k.lastPayout} per share").queue()
            }

            for ((_, v) in votes) {
                v.timesAround++
                if (v.timesAround >= 2) {
                    val yesVotes = v.votes.filter { (_, b) -> b }
                        .keys.sumOf { player -> state.shares.shares.filterValues { (c, p) -> c == v.company && player == p}.size }
                    val noVotes  = v.votes.filter { (_, b) -> !b }
                        .keys.sumOf { player -> state.shares.shares.filterValues {
                            (c, p) -> c == v.company && player == p }.size }
                    if (yesVotes > noVotes) {
                        when (v) {
                            is Vote.BuyRail -> {
                                if (v.company.buyEdge(v.edge)) {
                                    v.message.editMessage("Successfully bought rail").setReplace(true).queue()
                                    companyChannels[v.company]!!.sendMessage("We bought ${v.edge}").queue()
                                }
                            }
                            is Vote.BuyTrain -> {
                                if (v.company.buyTrain(state.trainMarket[v.train]!!.train)) {
                                    state.trainMarket[v.train]!!.remaining--
                                    v.message.editMessage("Successfully bought train").setReplace(true).queue()
                                    companyChannels[v.company]!!.sendMessage("We bought ${v.train}").queue()
                                }
                                else {
                                    v.message.editMessage("Failed to buy").setReplace(true).queue()
                                    companyChannels[v.company]!!.sendMessage("We couldn't buy ${v.train}").queue()
                                }
                            }
                            is Vote.MakeLine -> {
                                val actualEdges = v.edges.map { state.map.edges[it]!! }
                                val actualTrains = v.trains.map { state.trainMarket[it]!!.train }
                                if (v.company.makeLine(actualEdges, actualTrains)) {
                                    v.message.editMessage("Successfully made line").setReplace(true).queue()
                                    companyChannels[v.company]!!.sendMessage("A new line is now operating")
                                }
                                else {
                                    v.message.editMessage("Failed to open line").setReplace(true).queue()
                                    companyChannels[v.company]!!.sendMessage("We couldn't open this line")

                                }
                            }
                        }
                    }
                    else {
                        v.message.editMessage("This vote failed to pass").setReplace(true).queue();
                        v.message.reply("We failed to pass this vote")
                    }
                }
            }

            for (k in HashSet(votes.keys)) if (votes[k]!!.timesAround >= 2) votes.remove(k)
        }
    }
}