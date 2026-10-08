// Удобный конструктор дерева: vararg принимает любое число дочерних узлов
private fun res(name: String, max: Int, vararg kids: Resource) =
    Resource(name, max, kids.associateBy { it.name })

object Data {
    // Виртуальный корень: имя пустое, объём не используется
    val root = res("", 0,
        res("A", 100,
            res("B", 50,
                res("C", 20)
            ),
            res("D", 30)
        ),
        res("Docs", 200,
            res("Reports", 100)
        )
    )

    val users: Map<String, User> = listOf(
        User(
            login = "alice",
            saltHex = "1fda1d830b27c62aa536a07f792617f1",
            hashHex = "37cd06fd0ff7a59e8fe8b8a08cdfd26296a5f1a0f318e8f92909bbf3c73a3f71",          // пароль: qwerty
            grants = listOf(Grant(listOf("A"), setOf(Action.READ, Action.WRITE)))
        ),
        User(
            login = "bob",
            saltHex = "9d7b8ac85a9b45103c2dd81d80dbc61e",
            hashHex = "432ba59046ffd3b1b1b382d4b397b3bf861d0b81cf4593e922782d5f7046c0c0",          // пароль: hunter2
            grants = listOf(
                Grant(listOf("A", "B"), setOf(Action.READ)),
                Grant(listOf("Docs"), setOf(Action.EXECUTE))
            )
        )
    ).associateBy { it.login }
}