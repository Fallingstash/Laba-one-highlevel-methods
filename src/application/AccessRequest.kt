package application

/** Запрос «как пришёл»: все значения необработанные строки, проверку формата делает сценарий. */
data class AccessRequest(
    val login: String,
    val password: String,
    val action: String,
    val resource: String,
    val volume: String
)
