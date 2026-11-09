# Тестопригодность проекта и модульные тесты

## 1. Анализ тестопригодности

Тестопригодность означает, что часть программы можно проверить отдельно: подать вход, получить результат и сравнить с ожидаемым, не запуская весь процесс и не трогая внешний мир (консоль, случайные числа, завершение процесса).

### 1.1. Что тестируется легко

| Класс / компонент | Слой | Почему легко | Как проверяем |
|-------------------|------|--------------|---------------|
| `ResourcePath` | domain | Чистые функции без внешних зависимостей | Разбор путей, граничные длины, запрещённые символы, `startsWith` по сегментам |
| `PermissionPolicy` | domain | Чистая функция от списка прав, пути и действия | Наследование прав вниз, отказ по умолчанию, разные ветки дерева |
| `CheckAccessUseCase` | application | Все зависимости (`UserRepository`, `ResourceRepository`, `PasswordHasher`) приходят через конструктор как интерфейсы | Подставляем поддельные реализации, проверяем результат и порядок проверок |
| `ExitCodeMapper`, `ExitCode` | adapter | Чистое отображение «результат → код» | Все причины отказа и таблица кодов из задания |
| `CliArgumentsParser` | adapter | Принимает массив строк, возвращает команду; опасные для `kotlinx-cli` аргументы отсекаются до вызова библиотеки | Корректные аргументы, справка, все виды неверного формата запуска |
| `HexCodec`, `Sha256PasswordHasher` | infrastructure | Детерминированные вычисления | Эталонные значения SHA-256, свойства хэша, `verify` |
| `InMemoryResourceRepository`, `InMemoryUserRepository` | infrastructure | Данные в коде, без ввода-вывода | Поиск, отсутствующие элементы, согласованность хэшей с документированными паролями |
| `SaltGenerator` | infrastructure | Случайность проверяется через свойства, а не значения | Длина, алфавит, различие значений |

### 1.2. Что было сложно протестировать и почему

| Место | Проблема | Решение |
|-------|----------|---------|
| `main` в `Main.kt` | Сразу вызывает `exitProcess`: тест, вызвавший `main`, завершил бы сам запуск тестов; результат (код выхода) нельзя получить как значение | Логику запуска вынесли в `CliRunner.run`, который возвращает `ExitCode`; `main` только создаёт объекты и вызывает `exitProcess` |
| `HelpPrinter.print()` | Печатал прямо в `System.out`: текст справки нельзя было проверить без перехвата потока | Вывод вынесен в `CliRunner` (принимает `PrintStream`), а `HelpPrinter` отдаёт текст методом `text()` |
| `kotlinx-cli` | Библиотека сама завершает процесс (код 127) при неверных аргументах | Форма аргументов проверяется до вызова библиотеки (сделано при исправлении дефекта лабы №1), поэтому `CliArgumentsParser` тестируется без риска завершить процесс |
| Случайная соль | Значение не повторяется, точное значение проверить нельзя | Проверяем свойства: длина, алфавит, различие между вызовами |
| Сквозное поведение процесса (реальный код возврата JVM) | Нужен запуск `app.jar` | Остаётся в `scripts/test.sh` (13 сквозных кейсов), модульные тесты его не заменяют |

Благодаря слоистой структуре из лаб №2–3 (порты и внедрение зависимостей через конструктор) основная логика уже была тестопригодной: именно поэтому потребовался минимальный рефакторинг, а не переписывание.

## 2. Что изменено в проекте

Изменения не меняют логику работы программы. Проверка: сквозные тесты `scripts/test.sh` дают те же 13 из 13 до и после правок.

1. **Новый класс `adapter/cli/CliRunner.kt`.** Принимает `CheckAccessUseCase` и `PrintStream` (по умолчанию `System.out`), метод `run(args): ExitCode` выполняет весь путь «аргументы → код ответа» и ничего не завершает.
2. **`Main.kt`.** Остались создание объектов и `exitProcess(CliRunner(useCase).run(args).code)`.
3. **`HelpPrinter`.** Вместо `print()` метод `text(): String`; вывод делает `CliRunner` в переданный поток.
4. **Скрипты.** Добавлены `scripts/build-tests.sh` и `scripts/run-tests.sh`; `config.sh` и `fetch-deps.sh` умеют скачивать JUnit (`junit-platform-console-standalone`) рядом с `kotlinx-cli`; разделитель путей в classpath выбирается автоматически (`;` в Windows, `:` в Linux). Прежний самодельный `UseCaseTest.kt` и `unit-test.sh` заменены тестами на JUnit 5.

## 3. Результаты выполнения тестов

Запуск: `bash scripts/run-tests.sh`. Тесты написаны на JUnit 5 (`org.junit.jupiter.api`) и запускаются консольным лаунчером `junit-platform-console-standalone` без системы сборки. Имена тестов описывают ожидаемое поведение (`shouldДействиеWhenУсловие`).

### 3.1. Итог

| Показатель | Значение |
|------------|----------|
| Тестовых классов | 12 |
| Тестов выполнено | **159** |
| Успешно | **159** |
| Упало / пропущено | 0 / 0 |

### 3.2. По классам

| Тестовый класс | Что проверяет | Тестов |
|----------------|---------------|--------|
| `domain.ResourcePathTest` | Разбор и формат пути, `startsWith` | 19 |
| `domain.PermissionPolicyTest` | Права, наследование, отказ по умолчанию | 10 |
| `application.CheckAccessUseCaseTest` | Весь сценарий проверки, порядок проверок | 36 |
| `adapter.cli.ExitCodeMapperTest` | Отображение причин отказа в коды, таблица кодов | 12 |
| `adapter.cli.HelpPrinterTest` | Содержимое справки | 3 |
| `adapter.cli.CliArgumentsParserTest` | Разбор аргументов, справка, неверный формат запуска | 19 |
| `adapter.cli.CliRunnerTest` | Аргументы → код ответа внутри процесса, «тихая» работа | 15 |
| `infrastructure.HexCodecTest` | Кодирование и декодирование hex | 7 |
| `infrastructure.Sha256PasswordHasherTest` | Эталонные хэши, свойства, `verify` | 14 |
| `infrastructure.SaltGeneratorTest` | Длина, алфавит, различие солей | 3 |
| `infrastructure.InMemoryResourceRepositoryTest` | Поиск ресурсов и их максимальный объём | 8 |
| `infrastructure.InMemoryUserRepositoryTest` | Пользователи, права, согласованность хэшей | 13 |
| **Итого** | | **159** |

### 3.3. Примеры проверяемых сценариев

| Тест | Описание |
|------|----------|
| `ResourcePathTest.shouldRejectSegmentLongerThanTwentyCharacters` | Имя ресурса из 21 символа недопустимо |
| `ResourcePathTest.shouldNotStartWithSiblingWhoseNameBeginsTheSame` | Путь `A.BX` не считается потомком `A.B` (сравнение по сегментам, а не по строкам) |
| `PermissionPolicyTest.shouldAllowChildWhenGrantIsOnParent` | Право на родителя действует на потомков |
| `PermissionPolicyTest.shouldDenyParentWhenGrantIsOnlyOnChild` | Право на потомка не даёт доступа к родителю |
| `CheckAccessUseCaseTest.shouldNotAskHasherWhenLoginIsUnknown` | Для неизвестного логина пароль не проверяется (поддельный хэшер считает вызовы) |
| `CheckAccessUseCaseTest.shouldReportAccessDeniedBeforeVolumeExceeded` | Порядок проверок: отказ в правах приоритетнее превышения объёма |
| `CheckAccessUseCaseTest.shouldGrantWhenVolumeEqualsMaximum` | Граница: объём, равный максимуму, допустим |
| `CheckAccessUseCaseTest.shouldDenyWithInvalidFormatWhenVolumeDoesNotFitInInt` | Слишком большое число даёт ошибку формата, а не исключение |
| `ExitCodeMapperTest.shouldKeepNumericCodesFromTaskTable` | Коды 0–8 совпадают с таблицей из задания |
| `CliArgumentsParserTest.shouldReturnHelpWhenFlagIsUnknown` | `--bogus` даёт справку приложения (раньше код 127 из библиотеки) |
| `CliArgumentsParserTest.shouldAcceptNegativeVolumeAsValue` | `--volume -5` доходит до сценария и даёт код 7, а не ошибку парсера |
| `CliRunnerTest.shouldPrintNothingWhenAccessIsGranted` | При успехе программа ничего не выводит |
| `CliRunnerTest.shouldReturnVolumeExceededWhenVolumeIsAboveMaximum` | Сквозной сценарий с реальными данными даёт код 8 |
| `Sha256PasswordHasherTest.shouldMatchKnownSha256VectorForAbcWhenSaltIsEmpty` | Хэш совпадает с эталонным значением SHA-256 |
| `Sha256PasswordHasherTest.shouldPutSaltBytesBeforePasswordWhenHashing` | Порядок «соль, затем пароль» зафиксирован эталонным значением |
| `Sha256PasswordHasherTest.shouldNotVerifyWhenAlgorithmIsNotSha256` | Учётные данные с другим алгоритмом не проходят проверку |
| `InMemoryUserRepositoryTest.shouldAcceptDocumentedPasswordForBob` | Записанные в коде соль и хэш соответствуют паролю из документации |
| `SaltGeneratorTest.shouldGenerateDifferentSaltsOnEachCall` | 50 сгенерированных солей все различны |

### 3.4. Проверка качества самих тестов

Чтобы убедиться, что тесты не «зелёные просто так», в копию проекта намеренно вносились ошибки. Все они были обнаружены:

| Внесённая ошибка | Упало тестов |
|------------------|--------------|
| В проверке объёма `>` заменено на `>=` | 2 |
| Проверка префикса пути по строкам вместо сегментов | 3 |
| Коды «неверный пароль» и «неверный логин» перепутаны | 3 |
| Пароль проверяется до проверки существования логина | 5 |
| Соль подмешивается после пароля, а не перед ним | 10 |

### 3.5. Какие классы пока не тестируются и почему

| Компонент | Причина |
|-----------|---------|
| `Main.kt` (`main`) | Вызывает `exitProcess` и создаёт объекты; логика вынесена в `CliRunner`. Реальный код возврата процесса проверяют сквозные тесты `scripts/test.sh` (13 кейсов) |
| `tools/GenHash.kt` | Вспомогательный инструмент генерации хэшей, в приложение не входит |
| `Action`, `Resource`, `Grant`, `UserAccount`, `Credentials`, `AccessRequest`, `AccessResult`, `CliCommand` | Классы-контейнеры данных без собственной логики; их поведение проверяется косвенно через тесты сценария и парсера |
| Значение по умолчанию `System.out` в `CliRunner` | Тесты подставляют свой поток; сама подстановка тривиальна |
| Сама библиотека `kotlinx-cli` | Чужой код; проверяется только то, что наш код не передаёт ей некорректные аргументы |
| Криптостойкость `SecureRandom` | Проверяются формат и различие значений, а не качество случайности |

## 4. Как запустить

```bash
bash scripts/run-tests.sh                                   # собрать и запустить все модульные тесты
bash scripts/run-tests.sh --select-class domain.ResourcePathTest   # только один класс
bash scripts/test.sh                                        # сквозные тесты app.jar (после scripts/build.sh)
```

Для запуска в IDE достаточно добавить `lib/junit-platform-console-standalone-*.jar` в библиотеки проекта и пометить каталог `tests` как тестовый (в этом jar уже есть `junit-jupiter-api` и движок).

## 5. Актуализация после перехода на базу данных

Отчёт выше описывает состояние проекта на момент лабораторной по тестированию. После перехода на хранение данных в H2 (`doc/lab6/sql-plan.md`) изменились следующие места:

* классы `InMemoryUserRepository` и `InMemoryResourceRepository` и их тесты удалены, данные теперь лежат в `scripts/fill.sql`;
* репозитории на JDBC проверяются тестами с настоящей H2 в памяти (`infrastructure.jdbc.*Test`), схема и данные проверяются `SchemaTest`;
* `CliRunnerTest` работает с H2 в памяти и проверяет в том числе коды 9 и 10;
* общее число модульных тестов 217 (все успешны), сквозных тестов `scripts/test.sh` 16.
