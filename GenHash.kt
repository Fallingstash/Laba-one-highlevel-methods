class GenHash {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            require(args.size == 2) { "usage: GenHash <login> <password>" }
            val salt = Auth.generateSaltHex()
            println("login=${args[0]}")
            println("salt=$salt")
            println("hash=${Auth.hashHex(args[1], salt)}")
        }
    }
}
