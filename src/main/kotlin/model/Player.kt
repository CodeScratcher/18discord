package model

import java.util.UUID

class Player(val uuid: UUID, val username: String, var wealth: Int, val gameState: GameState) {
    fun buyShare(id: String) {
        if (id in gameState.shares.offering) {
            val offering = gameState.shares.offering[id]!!

            if (wealth >= offering.price) {
                wealth -= offering.price
                offering.share.player = this;
                gameState.shares.offering.remove(id)
            }
        }
        else if (id in gameState.shares.IPO) {
            val ipo = gameState.shares.IPO[id]!!

            if  (wealth >= ipo.price) {
                wealth -= ipo.price
                gameState.shares.IPO.remove(id)
                gameState.shares.shares[id] = Share(ipo.company, this)
                ipo.company.funds += ipo.price
            }
        }
    }

    fun sellShare(id: String, price: Int) {
        if (gameState.shares.shares[id]?.player == this) {
            gameState.shares.offering[id] = Offering(gameState.shares.shares[id]!!, price)
        }
    }

    fun openCompany(mark: String, name: String, price: Int) {
        if (wealth >= price * 2) {
            val company = Company(mark, name, price * 2, gameState)
            company.createIpo(this, price)
            gameState.companies.add(company)
        }

    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Player -> uuid == other.uuid
            else -> false;
        }
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    override fun toString(): String {
        return "Player(uuid=$uuid, username='$username', wealth=$wealth)"
    }


}