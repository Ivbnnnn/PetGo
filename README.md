# Для участников команды
Необходимо установить maven, прописать путь к нему в Path переменных окружения (for win, для мака хз);
Установить bundle для Java (extension Pack for Java - расширение vs code);


# Запуск проекта
Для окна Hello World нужна Java 17 или новее и Maven. Из корня проекта выполните:

```powershell
mvn javafx:run
```

Также можно запустить метод `main` в `Main.java` кнопкой Run в редакторе.

# Подключение к базе данных
Настройки PostgreSQL находятся в `src/main/resources/application.properties`:
`db.url`, `db.user`, `db.password`. Их можно переопределить переменными
окружения `DB_URL`, `DB_USER`, `DB_PASSWORD`.
