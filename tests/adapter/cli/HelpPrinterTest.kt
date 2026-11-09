package adapter.cli

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HelpPrinterTest {

    private val text = HelpPrinter.text()

    @Test
    fun shouldDescribeEverySupportedFlag() {
        listOf("--login", "--password", "--action", "--resource", "--volume", "-h", "--help")
            .forEach { assertTrue(text.contains(it), "в справке нет $it") }
    }

    @Test
    fun shouldListAllSupportedActions() {
        listOf("read", "write", "execute").forEach { assertTrue(text.contains(it), "в справке нет $it") }
    }

    @Test
    fun shouldDescribeDatabaseErrorCodes() {
        assertTrue(text.contains("9 ошибка подключения к базе данных"))
        assertTrue(text.contains("10 ошибка SQL-запроса"))
    }

    @Test
    fun shouldExplainHowToChooseDatabaseFile() {
        assertTrue(text.contains("ACCESS_DB_PATH"))
        assertTrue(text.contains("init-db.sh"))
    }

    @Test
    fun shouldShowUsageExample() {
        assertTrue(text.contains("java -jar app.jar"))
    }
}
