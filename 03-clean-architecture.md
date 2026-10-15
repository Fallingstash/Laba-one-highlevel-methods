# Чистая архитектура: слои и план реструктуризации

## 1. Идея в одном абзаце

Чистая архитектура делит программу на концентрические слои. Главное правило (**правило зависимостей**): зависимости в исходном коде направлены только внутрь. Внутренние слои (предметная область, сценарии) ничего не знают о внешних (командная строка, библиотеки, хранилище данных). Благодаря этому бизнес-логику можно тестировать без окружения, а внешние детали (CLI, хранилище, алгоритм хэширования) заменять без изменения ядра.

```
┌───────────────────────────────────────────────┐
│ Frameworks & Drivers: Main, kotlinx-cli,      │
│ exitProcess, in-memory данные, java.security  │
│  ┌─────────────────────────────────────────┐  │
│  │ Interface Adapters: CLI-разбор, ExitCode │  │
│  │  ┌───────────────────────────────────┐  │  │
│  │  │ Use Cases: CheckAccessUseCase,    │  │  │
│  │  │ порты (интерфейсы)                │  │  │
│  │  │  ┌─────────────────────────────┐  │  │  │
│  │  │  │ Entities: Resource, Action, │  │  │  │
│  │  │  │ Grant, ResourcePath, права  │  │  │  │
│  │  │  └─────────────────────────────┘  │  │  │
│  │  └───────────────────────────────────┘  │  │
│  └─────────────────────────────────────────┘  │
└───────────────────────────────────────────────┘
        зависимости направлены внутрь →
```

## 2. Слои приложения

### 2.1. Domain (предметная область, сущности)

Что это: правила, которые верны независимо от способа ввода и хранения данных.

* действие над ресурсом (`Action`: чтение, запись, выполнение);
* ресурс и дерево ресурсов (`Resource`, максимальный объём);
* путь до ресурса (`ResourcePath`): формат имён (≤ 20 символов, `A–Z a–z 0–9 _`), разбор, проверка «является ли один путь префиксом другого»;
* право доступа (`Grant`) и политика наследования прав (`PermissionPolicy`): право на родителя действует на потомков, отсутствие права означает отказ;
* пользователь и его учётные данные (`UserAccount`, `Credentials`).

Не должен знать: `kotlinx-cli`, коды возврата, `println`, `exitProcess`, `java.security`.

### 2.2. Application (прикладной слой, сценарии)

Что это: сценарий «проверить запрос на доступ» и контракты, которые нужны ему от внешнего мира.

* `CheckAccessUseCase`: логин → пароль → действие → формат → существование → права → объём;
* `AccessRequest` (входные данные), `AccessResult` и `DenyReason` (результат);
* **порты** (интерфейсы, принадлежащие этому слою): `UserRepository`, `ResourceRepository`, `PasswordHasher`.

Зависит только от Domain. Реализации портов здесь не находятся.

### 2.3. Interface Adapters (адаптеры интерфейсов)

Что это: преобразование данных между внешним форматом и форматом сценариев.

* `CliArgumentsParser`: аргументы командной строки → `AccessRequest` (включая предварительную проверку формы аргументов, чтобы `kotlinx-cli` не завершал процесс сам);
* `ExitCode` и `ExitCodeMapper`: `DenyReason`/`Granted` → код возврата `0`–`8`;
* `HelpPrinter`: текст справки.

Зависит от Application и Domain.

### 2.4. Infrastructure / Frameworks & Drivers (внешний слой)

Что это: конкретные технологии и «клей».

* `Main.kt`: точка входа и **composition root** (единственное место, где создаются конкретные объекты и связываются зависимости);
* `InMemoryUserRepository`, `InMemoryResourceRepository`: данные в коде (требование задания);
* `Sha256PasswordHasher`, `SaltGenerator`, `HexCodec`: `java.security`;
* библиотека `kotlinx-cli`, `exitProcess`.

Зависит от всех внутренних слоёв, на неё не зависит никто.

## 3. Соответствие текущих компонентов слоям

| Текущий компонент | Что в нём | Куда относится | Что делать |
|-------------------|-----------|----------------|------------|
| `Models.kt`: `Action`, `Grant`, `Resource` | сущности | Domain | оставить, убрать из `Action` CLI-строку (`flag`) |
| `Models.kt`: `User` | учётные данные + права | Domain | разделить на `Credentials` и права |
| `Models.kt`: `ExitCode` | представление результата для CLI | Adapters | перенести в пакет CLI |
| `Access.kt`: `parsePath` | формат пути | Domain | превратить в `ResourcePath` |
| `Access.kt`: `find` | поиск в дереве | Infrastructure / порт `ResourceRepository` | вынести за интерфейс |
| `Access.kt`: `isAllowed` | политика прав | Domain | `PermissionPolicy` |
| `Auth.kt`: `verify`, `hashHex` | хэширование паролей | интерфейс — Application, реализация — Infrastructure | `PasswordHasher` + `Sha256PasswordHasher` |
| `Auth.kt`: `generateSaltHex`, `toHex`, `hexToBytes` | соль, кодирование | Infrastructure | `SaltGenerator`, `HexCodec` |
| `Data.kt` | данные | Infrastructure | `InMemory*Repository` |
| `Main.kt`: `execute` (проверки) | сценарий | Application | перенести в `CheckAccessUseCase` |
| `Main.kt`: `parseArgs`, справка | разбор CLI | Adapters | `CliArgumentsParser`, `HelpPrinter` |
| `Main.kt`: `main`, `exitProcess` | запуск | Infrastructure | оставить, превратить в composition root |
| `GenHash.kt` | инструмент | вне приложения | переместить в `tools/` |

## 4. Целевая структура каталогов и пакетов

Система сборки не используется, поэтому каталоги соответствуют пакетам, а скрипт сборки собирает файлы через `find src -name '*.kt'`.

```
repo/
├── src/
│   ├── domain/
│   │   ├── Action.kt
│   │   ├── Grant.kt
│   │   ├── Resource.kt
│   │   ├── ResourcePath.kt
│   │   ├── PermissionPolicy.kt
│   │   └── UserAccount.kt            (UserAccount, Credentials)
│   ├── application/
│   │   ├── CheckAccessUseCase.kt
│   │   ├── AccessRequest.kt
│   │   ├── AccessResult.kt           (AccessResult, DenyReason)
│   │   └── port/
│   │       ├── UserRepository.kt
│   │       ├── ResourceRepository.kt
│   │       └── PasswordHasher.kt
│   ├── adapter/
│   │   └── cli/
│   │       ├── CliArgumentsParser.kt
│   │       ├── ExitCode.kt
│   │       ├── ExitCodeMapper.kt
│   │       └── HelpPrinter.kt
│   ├── infrastructure/
│   │   ├── InMemoryUserRepository.kt
│   │   ├── InMemoryResourceRepository.kt
│   │   ├── Sha256PasswordHasher.kt
│   │   ├── SaltGenerator.kt
│   │   └── HexCodec.kt
│   └── Main.kt                       (composition root)
├── tools/GenHash.kt
├── scripts/  build.sh  run.sh  test.sh
├── lib/      kotlinx-cli-jvm-0.3.6.jar
├── docs/
├── readme.md
└── .gitignore                        (app.jar, build/, out/)
```

## 5. Новые абстракции

Контракты портов (набросок, не финальный код):

```kotlin
interface UserRepository     { fun findByLogin(login: String): UserAccount? }
interface ResourceRepository { fun find(path: ResourcePath): Resource? }
interface PasswordHasher     { fun verify(password: String, credentials: Credentials): Boolean }
```

Результат сценария вместо `null` и кодов возврата:

```kotlin
sealed interface AccessResult {
    data object Granted : AccessResult
    data class Denied(val reason: DenyReason) : AccessResult
}

enum class DenyReason {
    UNKNOWN_LOGIN, WRONG_PASSWORD, UNKNOWN_ACTION, INVALID_FORMAT,
    RESOURCE_NOT_FOUND, ACCESS_DENIED, VOLUME_EXCEEDED
}
```

Почему так:

* Домен не знает про коды `0`–`8`: отображение «причина отказа → код» живёт в адаптере CLI (`ExitCodeMapper`). Завтра можно заменить его на HTTP-статусы, не трогая ядро.
* Порты принадлежат прикладному слою, а реализации лежат снаружи. Это и есть инверсия зависимостей: ядро диктует, что ему нужно, внешний слой подстраивается.
* `AccessRequest` содержит необработанные строки, потому что проверка формата действия, пути и объёма — часть бизнес-правил (коды 4 и 7), а не забота CLI.

## 6. Как пойдёт запрос после реструктуризации

```
Main (composition root)
  создаёт: InMemoryUserRepository, InMemoryResourceRepository, Sha256PasswordHasher
  собирает: CheckAccessUseCase(users, resources, hasher)

args ──► CliArgumentsParser ──► AccessRequest
                                      │
                                      ▼
                              CheckAccessUseCase
                       (логин, пароль, действие, формат,
                        ресурс, права, объём)
                                      │
                                      ▼
                                AccessResult
                                      │
                                      ▼
                              ExitCodeMapper ──► ExitCode ──► exitProcess(code)
```

## 7. Правила зависимостей (проверяемый чек-лист)

| Слой | Может импортировать | Не может импортировать |
|------|---------------------|------------------------|
| Domain | только стандартную библиотеку Kotlin | `kotlinx.cli`, `java.security`, `kotlin.system`, любые другие слои |
| Application | Domain | `kotlinx.cli`, `java.security`, Adapters, Infrastructure |
| Adapters (CLI) | Application, Domain, `kotlinx.cli` | Infrastructure |
| Infrastructure / Main | все слои, `java.security`, `kotlin.system` | — |

Проверка выполняется поиском по импортам, например: `grep -rn "kotlinx.cli\|exitProcess\|java.security" src/domain src/application` не должен ничего находить.

## 8. Что даёт реструктуризация и чем за неё платим

**Выигрыш:**
* бизнес-правила (включая проверку объёма) не зависят от способа запуска;
* сценарий тестируется с поддельными репозиториями и хэшером, без реальных хэшей и без запуска процесса;
* смена хранилища, алгоритма хэширования или интерфейса (CLI → HTTP) затрагивает один внешний слой;
* каждый файл имеет одну причину для изменения.

**Цена:**
* файлов становится существенно больше при небольшом объёме логики;
* появляются преобразования между слоями (запрос → результат → код);
* без системы сборки нужно следить, чтобы скрипт собирал все исходники (используется `find`).

Для проекта такого размера архитектура избыточна. Цель работы — учебная: показать, как выглядит разделение слоёв на понятном примере.
