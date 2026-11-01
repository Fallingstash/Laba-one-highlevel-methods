package domain

/** Путь до ресурса: последовательность имён. Объект можно создать только через [parse]. */
class ResourcePath private constructor(val segments: List<String>) {

    /** true, если [prefix] является началом этого пути (сравнение по сегментам, не по строкам). */
    fun startsWith(prefix: ResourcePath): Boolean =
        segments.size >= prefix.segments.size &&
            segments.subList(0, prefix.segments.size) == prefix.segments

    override fun toString(): String = segments.joinToString(".")

    companion object {
        private val NAME = Regex("^[A-Za-z0-9_]{1,20}$")

        /** null, если формат пути неверный. */
        fun parse(raw: String): ResourcePath? {
            val parts = raw.split('.')
            return if (parts.all { NAME.matches(it) }) ResourcePath(parts) else null
        }
    }
}
