package domain

/** Право на [actions] для ресурса [path] и всего его поддерева. */
class Grant(val path: ResourcePath, val actions: Set<Action>)
