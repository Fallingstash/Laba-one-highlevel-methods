package infrastructure

import application.port.ResourceRepository
import domain.Resource
import domain.ResourcePath

/** Дерево ресурсов, хранящееся в коде (требование задания). */
class InMemoryResourceRepository : ResourceRepository {

    private class Node(val maxVolume: Int, val children: Map<String, Node> = emptyMap())

    private fun node(max: Int, vararg children: Pair<String, Node>) = Node(max, children.toMap())

    // Виртуальный корень: объём не используется
    private val root = node(0,
        "A" to node(100,
            "B" to node(50,
                "C" to node(20)
            ),
            "D" to node(30)
        ),
        "Docs" to node(200,
            "Reports" to node(100)
        )
    )

    override fun find(path: ResourcePath): Resource? {
        var current = root
        for (segment in path.segments) {
            current = current.children[segment] ?: return null
        }
        return Resource(path, current.maxVolume)
    }
}
