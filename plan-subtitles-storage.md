# Plan: Хранение субтитров в БД

## 1. Миграция БД

- [ ] Создать Liquibase миграцию `create_table_subtitle.sql` — таблица `subtitle` с полями:
  - `id` UUID PK
  - `video_url` VARCHAR(2048) NOT NULL UNIQUE
  - `content` TEXT NOT NULL
  - `create_time` TIMESTAMP NOT NULL
  - `last_update_time` TIMESTAMP NOT NULL

## 2. Сущность и репозиторий

- [ ] Создать JPA-сущность `Subtitle` (таблица `subtitle`): поля `id`, `videoUrl`, `content`, extends `AuditableEntity`
- [ ] Создать `SubtitleRepository extends JpaRepository<Subtitle, UUID>` с методом `findByVideoUrl(String videoUrl): Optional<Subtitle>`

## 3. Сервис кэширования субтитров

- [ ] Создать интерфейс `SubtitleCacheService` с методами:
  - `findByVideoUrl(String videoUrl): Optional<String>`
  - `save(String videoUrl, String content): void`
- [ ] Создать `SubtitleCacheServiceImpl` в пакете `serivce/domain/impl`
- [ ] В `save()` — upsert: если запись существует, обновить `content` и `lastUpdateTime`; иначе создать новую

## 4. Интеграция в пайплайн

- [ ] В `YtSubtitlesTool.loadSubtitles()`:
  - Перед загрузкой через yt-dlp проверить наличие субтитров в БД через `SubtitleCacheService.findByVideoUrl()`
  - Если найдены — вернуть из кэша, не вызывать внешний процесс
  - Если не найдены — загрузить через yt-dlp, сохранить в БД через `SubtitleCacheService.save()`

## 5. Тесты

- [ ] `SubtitleCacheServiceImplTest`:
  - `when_findByVideoUrl_withExistingUrl_then_subtitlesReturned`
  - `when_findByVideoUrl_withUnknownUrl_then_emptyReturned`
  - `when_save_withNewUrl_then_subtitleCreated`
  - `when_save_withExistingUrl_then_subtitleUpdated`
- [ ] `YtSubtitlesTool` интеграционный тест:
  - `when_loadSubtitles_withCachedUrl_then_ytdlpNotCalled`
  - `when_loadSubtitles_withNewUrl_then_subtitleSavedToDb`
