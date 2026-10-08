object Access {
    private val NAME = Regex("^[A-Za-z0-9_]{1,20}$")

    // null = неверный формат (код 7)
    fun parsePath(raw: String): List<String>? {
        val parts = raw.split('.')
        return if (parts.all { NAME.matches(it) }) parts else null
    }

    // null = такого ресурса нет (код 6)
    fun find(root: Resource, path: List<String>): Resource? =
        path.fold<String, Resource?>(root) { node, name -> node?.children?.get(name) }

    // Есть ли у пользователя право на действие на этом пути или у любого предка
    fun isAllowed(user: User, path: List<String>, action: Action): Boolean =
        user.grants.any { g ->
            action in g.actions &&
            path.size >= g.path.size &&
                path.subList(0, g.path.size) == g.path
            }
    }   