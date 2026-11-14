package bot

import model.BoardMap
import model.GameState
import model.Player
import model.ShareRegistry
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.EnumSet
import java.util.UUID
import kotlin.uuid.Uuid

class CommandListener : ListenerAdapter() {
    val games: MutableMap<String, Game> = HashMap()

    fun playerInGameCheck(event: SlashCommandInteractionEvent): Boolean {
        if (event.member!!.id !in games[event.guild!!.id]!!.state.players) {
            event.hook.sendMessage("You're not in this game, /join to join").queue()
            return true
        }
        return false
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "start" && event.guild!!.id !in games) {
            event.reply("Please wait for the game to start!").queue()
            return
        }
        when (event.name) {
            "start" -> {
                event.deferReply(true).queue()
                if (event.guild?.id !in games) {
                    val shareRegistry = ShareRegistry(HashMap(), HashMap(), HashMap())
                    val map = BoardMap(ArrayList(), HashMap())
                    val gameState = GameState(HashMap(), HashMap(), shareRegistry, map, HashMap())

                    games[event.guild!!.id] = Game(event.guild!!, event.messageChannel, gameState, HashMap(), HashMap())
                    games[event.guild!!.id]?.setup()
                    event.hook.sendMessage("Started game!").queue()
                } else event.hook.sendMessage("Already started").queue()
            }

            "join" -> {
                event.deferReply().queue()
                if (!games[event.guild!!.id]!!.state.players.any { (k, _) -> k == event.member!!.id }) {
                    games[event.guild!!.id]?.state?.players[event.member!!.id] = Player(
                        event.member!!.id,
                        event.member!!.user.name,
                        STARTING_WEALTH,
                        games[event.guild!!.id]!!.state
                    )
                    event.hook.sendMessage("Added player: " + event.member!!.user.name).queue()
                } else event.hook.sendMessage("Player already joined").queue()
            }

            "buyshare" -> {
                event.deferReply().queue()
                if (playerInGameCheck(event)) return

                val id = event.getOption("share")!!.asString
                val gameState = games[event.guild!!.id]!!.state
                var owner: Player? = null
                if (id in gameState.shares.shares) owner = gameState.shares.shares[id]!!.player
                if (gameState.players[event.member!!.id]!!.buyShare(id)) {
                    val channel = games[event.guild!!.id]!!.companyChannels[gameState.shares.shares[id]!!.company]!!

                    if (owner != null) {
                        println(owner.uuid)

                        if (!gameState.shares.shares.any { (_, v) -> v.player == owner })
                            event.guild!!.retrieveMember(UserSnowflake.fromId(owner.uuid)).queue { member ->
                                channel.upsertPermissionOverride(member)
                                    .setDenied(Permission.VIEW_CHANNEL).queue()
                            }
                    }

                    channel
                        .upsertPermissionOverride(event.member!!)
                        .setAllowed(Permission.VIEW_CHANNEL).queue()
                    event.hook.sendMessage("Bought share $id successfully").queue()
                } else event.hook.sendMessage("Couldn't buy share").queue()
            }

            "sellshare" -> {
                event.deferReply().queue()
                if (playerInGameCheck(event)) return

                val id = event.getOption("share")!!.asString
                val cost = event.getOption("cost")!!.asInt
                games[event.guild!!.id]!!.state.players[event.member!!.id]!!.sellShare(id, cost)
                event.hook.sendMessage("Share $id is up for sale at cost $cost!").queue()
            }

            "opencompany" -> {
                event.deferReply().queue()
                if (playerInGameCheck(event)) return

                val mark = event.getOption("mark")!!.asString
                val name = event.getOption("name")!!.asString
                val price = event.getOption("cost")!!.asInt
                if (price < 10)
                    event.hook.sendMessage("Price too low!").queue()
                else if (mark.length !in 3..5 ||
                    !mark.all { it in 'A'..'Z' || it == '&' } || mark.first() == '&'
                )
                    event.hook.sendMessage("Invalid mark!").queue()
                else if (games[event.guild!!.id]!!.state.players[event.member!!.id]!!.openCompany(mark, name, price)) {
                    val company = games[event.guild!!.id]!!.state.companies[mark]!!
                    event.channel.asTextChannel().parentCategory?.createTextChannel("${company.name}-board")
                        ?.addPermissionOverride(event.member!!, EnumSet.of(Permission.VIEW_CHANNEL), null)
                        ?.addPermissionOverride(event.guild!!.publicRole, null, EnumSet.of(Permission.VIEW_CHANNEL))
                        ?.queue { channel -> games[event.guild!!.id]!!.companyChannels[company] = channel }
                    event.hook.sendMessage("Company $name listed, IPO up!").queue()
                } else event.hook.sendMessage("Either you don't have funds, or there are companies with this mark already")
                    .queue()
            }

            "status" -> {
                event.deferReply(true).queue()
                if (playerInGameCheck(event)) return
                val gameState = games[event.guild!!.id]!!.state
                val player = gameState.players[event.member!!.id]!!
                val str: StringBuilder = StringBuilder()

                str.append("${player.username}: $${player.wealth}\nShares:\n")
                gameState.shares.shares
                    .filterValues { (_, player) -> player.uuid == event.member!!.id }
                    .forEach { (string, _) -> str.append("\t- $string\n") }
                event.hook.sendMessage(str.toString()).queue()
            }

            "market" -> {
                event.deferReply(true).queue()
                val message = StringBuilder()
                for (company in games[event.guild!!.id]!!.state.companies.values) {
                    message.append(
                        "${company.name} (${company.mark}): Has $${company.funds}, made $${company.lastRecordedProfit} last tick\n"
                    )
                }

                message.append("Shares\n------\n")
                for ((name, share) in games[event.guild!!.id]!!.state.shares.shares) {
                    message.append("$name - owned by ${share.player.username}\n")
                }

                message.append("\nMarket\n------\n")
                for ((shareName, offering) in games[event.guild!!.id]!!.state.shares.offering) {
                    message.append("$shareName ($${offering.price}), sold by ${offering.share.player.username}\n")
                }

                message.append("\nIPO shares\n----------")
                for ((shareName, ipo) in games[event.guild!!.id]!!.state.shares.IPO) {
                    message.append("\n$shareName ($${ipo.price})")
                }

                event.hook.sendMessage(message.toString()).queue()
            }

            "vote" -> {
                event.deferReply(true).queue()
                when (event.subcommandName) {
                    "buytrain" -> {
                        val game = games[event.guild!!.id]!!
                        val company = game.state.companies[event.getOption("company")!!.asString]
                        if (company != null && event.getOption("train")!!.asString in
                            game.state.trainMarket.keys
                        ) {
                            val name = UUID.randomUUID().toString()
                            val channel = game.companyChannels[company]!!

                            event.hook.sendMessage("Vote being called").queue()
                            channel.sendMessage("Buy train?").addComponents(
                                ActionRow.of(
                                    Button.primary("y!$name", "Yes"),
                                    Button.danger("n!$name", "No")
                                )
                            ).queue { m -> game.votes[name] =
                                Vote.BuyTrain(company, 0, event.getOption("train")!!.asString, m)
                            }
                        } else {
                            event.hook.sendMessage("Train or company doesn't exist").queue()
                        }
                    }
                }
            }

            else -> {

            }
        }
    }

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        if (event.name == "buyshare" && event.focusedOption.name == "share") {
            val gameState = games[event.guild!!.id]!!.state
            val shareNames = gameState.shares.IPO.keys + gameState.shares.offering.keys
            event.replyChoiceStrings(shareNames.filter {
                it.startsWith(event.focusedOption.value)
            }).queue()
        }
        if (event.name == "sellshare" && event.focusedOption.name == "share") {
            val gameState = games[event.guild!!.id]!!.state
            val shareNames = gameState.shares.shares.filterValues { (_, player) -> player.uuid == event.member!!.id }
            event.replyChoiceStrings(shareNames.keys.filter {
                it.startsWith(event.focusedOption.value)
            }).queue()
        }
        if (event.name == "vote" && event.focusedOption.name == "company") {
            val gameState = games[event.guild!!.id]!!.state
            val shareNames = gameState.shares.shares.filterValues { (_, player) -> player.uuid == event.member!!.id }
            event.replyChoiceStrings(shareNames.values.map { x -> x.company.mark }.filter {
                it.startsWith(event.focusedOption.value)
            }.distinct()).queue()
        }

        if (event.name == "vote" && event.focusedOption.name == "edge") {
            val gameState = games[event.guild!!.id]!!.state
            val edges = gameState.map.edges;
            event.replyChoiceStrings(edges.keys.filter {
                it.startsWith(event.focusedOption.value)
            }.distinct()).queue()
        }

        if (event.name == "vote" && event.focusedOption.name == "train") {
            val gameState = games[event.guild!!.id]!!.state
            val train = gameState.trainMarket.keys
            event.replyChoiceStrings(train.filter {
                it.startsWith(event.focusedOption.value)
            }.distinct()).queue()
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val elements = event.componentId.split("!")
        val game = games[event.guild!!.id]!!

        val vote = game.votes[elements[1]]

        if (vote != null) {
            vote.votes[game.state.players[event.member!!.id]!!] = elements[0] == "y"
            event.reply("Vote recorded").setEphemeral(true).queue()
        }
        else {
            event.reply("This vote is unavailable").setEphemeral(true).queue()
        }

    }
}