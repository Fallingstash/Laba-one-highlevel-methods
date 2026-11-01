package domain

/** Правила прав: наследование вниз по дереву, по умолчанию доступ закрыт. */
object PermissionPolicy {
    fun isAllowed(grants: List<Grant>, path: ResourcePath, action: Action): Boolean =
        grants.any { grant -> action in grant.actions && path.startsWith(grant.path) }
}
