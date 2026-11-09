package application

import application.port.PasswordHasher
import application.port.ResourceRepository
import application.port.StorageException
import application.port.UserRepository
import domain.Action
import domain.PermissionPolicy
import domain.ResourcePath

/** Сценарий «можно ли пользователю выполнить действие над ресурсом в заданном объёме». */
class CheckAccessUseCase(
    private val users: UserRepository,
    private val resources: ResourceRepository,
    private val hasher: PasswordHasher
) {
    fun check(request: AccessRequest): AccessResult =
        try {
            evaluate(request)
        } catch (e: StorageException) {
            AccessResult.StorageFailure(e.kind)
        }

    private fun evaluate(request: AccessRequest): AccessResult {
        val user = users.findByLogin(request.login) ?: return deny(DenyReason.UNKNOWN_LOGIN)
        if (!hasher.verify(request.password, user.credentials)) return deny(DenyReason.WRONG_PASSWORD)

        val action = ACTIONS[request.action] ?: return deny(DenyReason.UNKNOWN_ACTION)
        val path = ResourcePath.parse(request.resource) ?: return deny(DenyReason.INVALID_FORMAT)
        val volume = request.volume.toIntOrNull()?.takeIf { it >= 0 }
            ?: return deny(DenyReason.INVALID_FORMAT)

        val resource = resources.find(path) ?: return deny(DenyReason.RESOURCE_NOT_FOUND)
        if (!PermissionPolicy.isAllowed(user.grants, path, action)) return deny(DenyReason.ACCESS_DENIED)
        if (volume > resource.maxVolume) return deny(DenyReason.VOLUME_EXCEEDED)

        return AccessResult.Granted
    }

    private fun deny(reason: DenyReason) = AccessResult.Denied(reason)

    private companion object {
        val ACTIONS = mapOf(
            "read" to Action.READ,
            "write" to Action.WRITE,
            "execute" to Action.EXECUTE
        )
    }
}
