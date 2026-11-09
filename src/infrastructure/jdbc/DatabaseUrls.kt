package infrastructure.jdbc

import java.io.File

object DatabaseUrls {
    /**
     * URL файловой базы H2 по пути без расширения. Путь делается абсолютным (H2 не принимает
     * «неявно относительные» пути), IFEXISTS=TRUE не даёт молча создать пустую базу, если файла нет:
     * тогда это ошибка подключения, а не «таблицы не найдены».
     */
    fun h2File(path: String): String =
        "jdbc:h2:file:" + File(path).absolutePath.replace('\\', '/') + ";IFEXISTS=TRUE"
}
