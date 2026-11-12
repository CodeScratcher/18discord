package model

data class IPO(val company: Company, val price: Int)
data class Offering(val share: Share, val price: Int)

data class Share(val company: Company, var player: Player)

class ShareRegistry(val shares: MutableMap<String, Share>,
                    val IPO: MutableMap<String, IPO>, val offering: MutableMap<String, Offering>) {
    override fun toString(): String {
        return "ShareRegistry(shares=$shares, IPO=$IPO, offering=$offering)"
    }
}