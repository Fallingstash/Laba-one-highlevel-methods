package application.port

import domain.Resource
import domain.ResourcePath

interface ResourceRepository {
    /** null, если ресурса с таким путём нет. */
    fun find(path: ResourcePath): Resource?
}
