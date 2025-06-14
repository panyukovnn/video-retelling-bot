package ru.panyukovnn.videoretellingbot.serivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import ru.panyukovnn.videoretellingbot.dto.Lang;
import ru.panyukovnn.videoretellingbot.dto.YoutubeSubtitles;
import ru.panyukovnn.videoretellingbot.exception.RetellingException;
import ru.panyukovnn.videoretellingbot.util.SubtitlesFileNameGenerator;
import ru.panyukovnn.videoretellingbot.util.YtDlpProcessBuilderCreator;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeSubtitlesLoader {

    private static final String SUBTITLES_LANG_RU = "ru";
    private static final String SUBTITLES_LANG_EN = "en";
    private static final String SUBTITLES_DIRECTORY = "./subtitles/";

    private final YtDlpProcessBuilderCreator ytDlpProcessBuilderCreator;
    private final SubtitlesFileNameGenerator subtitlesFileNameGenerator;

    public YoutubeSubtitles load(String link) {
        log.info("Начинаю загрузку субтитров из youtube видео по ссылке: {}", link);

        cleanSubtitlesFolder();

        try {
            Pair<String, Lang> subtitlesFilenameAndLangPair = loadSubtitles(link);
            String subtitlesFilename = subtitlesFilenameAndLangPair.getFirst();
            Lang lang = subtitlesFilenameAndLangPair.getSecond();

            File subtitlesFile = new File(SUBTITLES_DIRECTORY + subtitlesFilename);

            List<String> strings = Files.readAllLines(subtitlesFile.toPath());
            Set<String> cleanedFileLines = cleanSubtitles(strings);

            String subtitles = String.join("\n", cleanedFileLines);

            return YoutubeSubtitles.builder()
                .link(link)
                .lang(lang)
                .subtitles(subtitles)
                .build();
        } catch (RetellingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка загрузки субтитров из видео: {}", e.getMessage(), e);

            throw new RetellingException("63e9", "Не удалось извлечь субтитры из видео", e);
        }
    }

    /**
     * Пытаемся загрузить субтитры с разными языками, при этом проверяем наличие как встроенных, так и автогенерируемых
     *
     * @param videoUrl ссылка на видео
     * @return пара, где первый параметр - имя файла с загруженными субтитрами, второй параметр - язык
     */
    private Pair<String, Lang> loadSubtitles(String videoUrl) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            CompletableFuture<Optional<String>> ruSubtitles = CompletableFuture.supplyAsync(() -> tryDownloadSubtitles(videoUrl, SUBTITLES_LANG_RU, false), executorService);
            CompletableFuture<Optional<String>> autoGenRuSubtitles = CompletableFuture.supplyAsync(() -> tryDownloadSubtitles(videoUrl, SUBTITLES_LANG_RU, true), executorService);

            Pair<String, Lang> ruSubs = CompletableFuture.allOf(ruSubtitles, autoGenRuSubtitles)
                .thenApply(action -> {
                    Optional<String> optionalRuSubs = ruSubtitles.getNow(Optional.empty());
                    Optional<String> optionalRuAutoGenSubs = autoGenRuSubtitles.getNow(Optional.empty());

                    Optional<Pair<String, Lang>> stringLangPair = optionalRuSubs.map(it -> Pair.of(it, Lang.RU));
                    return stringLangPair
                        .orElseGet(() -> optionalRuAutoGenSubs.map(it -> Pair.of(it, Lang.RU))
                            .orElse(null));
                })
                .get();

            if (ruSubs != null) {
                return ruSubs;
            }

            CompletableFuture<Optional<String>> enSubtitles = CompletableFuture.supplyAsync(() -> tryDownloadSubtitles(videoUrl, SUBTITLES_LANG_EN, false), executorService);
            CompletableFuture<Optional<String>> autoGenEnSubtitles = CompletableFuture.supplyAsync(() -> tryDownloadSubtitles(videoUrl, SUBTITLES_LANG_EN, true), executorService);

            return CompletableFuture.allOf(enSubtitles, autoGenEnSubtitles)
                .thenApply(action -> {
                    Optional<String> optionalEnSubs = enSubtitles.getNow(Optional.empty());
                    Optional<String> optionalEnAutoGenSubs = autoGenEnSubtitles.getNow(Optional.empty());

                    return optionalEnSubs.map(it -> Pair.of(it, Lang.EN))
                                .orElseGet(() -> optionalEnAutoGenSubs.map(it -> Pair.of(it, Lang.EN))
                                    .orElseThrow(() -> new RetellingException("48ae", "Не удалось загрузить субтитры для указанного видео")));
                })
                .get();
        } catch (ExecutionException | InterruptedException e) {
            if (e.getCause() instanceof RetellingException re) {
                throw re;
            }

            log.error("Ошибка выгрузки файлов субтитров: {}", e.getMessage(), e);

            throw new RetellingException("3db2", "Ошибка выгрузки файлов субтитров", e);
        } finally {
            executorService.shutdown();
        }
    }

    private void cleanSubtitlesFolder() {
        long cutoffMillis = Instant.now().minusSeconds(3600).toEpochMilli();

        File folder = new File(SUBTITLES_DIRECTORY);

        File[] files = folder.listFiles();
        if (files == null) {
            log.warn("Не удалось прочитать содержимое папки с файлами субтитров, для очистки.");
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                if (file.lastModified() < cutoffMillis) {
                    boolean deleted = file.delete();

                    log.info("Файл: {} {}", file.getName(), (deleted ? "удалён" : "не удалось удалить"));
                }
            }
        }
    }

    /**
     * @param videoUrl ссылка на видео на youtube
     * @return имя файла с субтитрами, куда произошла выгрузка
     */
    private Optional<String> tryDownloadSubtitles(String videoUrl, String lang, boolean isAutoSubs) {
        String outputFileName = subtitlesFileNameGenerator.generateFileName();

        ProcessBuilder builder = ytDlpProcessBuilderCreator.createProcessBuilder(videoUrl, lang, isAutoSubs, SUBTITLES_DIRECTORY + outputFileName);

        try {
            log.info("Начало загрузки субтитров для видео: {}", videoUrl);

            Process process = builder.start();

            // Чтение вывода yt-dlp
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);

                if (line.contains("There are no subtitles for the requested languages")) {
                    return Optional.empty();
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("12d7 Ошибка выгрузки субтитров с помощью yt-dlp, exitCode: " + exitCode);

                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("45bb Ошибка выгрузки субтитров с помощью yt-dlp: " + e.getMessage(), e);

            return Optional.empty();
        }

        log.info("Субтитры успешно загружены");

        return Optional.of(outputFileName + "." + lang + ".vtt");
    }

    private Set<String> cleanSubtitles(List<String> lines) {
        Set<String> uniqueLines = new LinkedHashSet<>();

        for (String line : lines) {
            // Убираем теги и метки
            String cleanedLine = line
                .replaceAll("<[^>]+>", "")                         // Удаляет все теги вида <...>
                .replaceAll("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}", "")   // Удаляет временные метки
                .replaceAll("-->.*", "")                           // Удаляет строки с временными интервалами
                .replaceAll("align:\\w+ position:\\d+%", "")       // Удаляет служебные параметры
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")         // Удаляет управляющие символы
                .replaceAll("\\s{2,}", " ")                        // Заменяет множественные пробелы на один
                .trim();

            if (!cleanedLine.isEmpty()) {
                uniqueLines.add(cleanedLine);
            }
        }

        return uniqueLines;
    }


}
