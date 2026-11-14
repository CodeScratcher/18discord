package bot

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.interactions.commands.build.Commands


fun main() {
    val api = JDABuilder.createDefault(System.getenv("TOKEN")).addEventListeners(CommandListener()).build()
    api.awaitReady()
    api.upsertCommand(Commands.slash("start", "Start a game in this server")).queue()
}