# Diplom

Учебный Android-проект по полевой медицине (Kotlin + Jetpack Compose).

Коротко по тому, что уже есть:
- роли пользователей (фельдшер, эвакопункт, госпиталь, фонд);
- ручное заполнение формы и голосовой ввод;
- генерация PDF (форма 100) и сводки;
- локальная работа без сервера.

## Как запустить

1. Открыть проект в Android Studio.
2. В `Settings -> Build, Execution, Deployment -> Build Tools -> Gradle` выбрать `Gradle JDK = jbr-21` (или любой JDK 17+).
3. Сделать `Build -> Rebuild Project`.
4. Запустить на эмуляторе/телефоне.

## Оффлайн голосовой ввод (Vosk)

Для голосового режима нужна русская модель Vosk в `app/src/main/assets/model-ru`.

Самый простой вариант — запустить скрипт из корня проекта:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\download_vosk_model.ps1
```

После этого еще раз `Rebuild Project`.

## Если не собирается

Чаще всего проблема в Java. Если видишь ошибку про Java 8, переключи Gradle JDK на 17/21 и пересобери проект.
