import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class CompletableFutureLab {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("====== ЗАПУСК ВАРІАНТУ 1 (Текст і Файли) ======");
        Variant1.run();

        // Пауза між варіантами
        Thread.sleep(3000);

        System.out.println("\n\n====== ЗАПУСК ВАРІАНТУ 2 (Математика) ======");
        Variant2.run();

        // Затримка перед виходом, щоб асинхронні потоки встигли вивести все в консоль
        Thread.sleep(3000);
    }
}

// ---------------------------------------------------------------
// ВАРІАНТ 1: Робота з файлами та текстом
// ---------------------------------------------------------------
class Variant1 {

    private static final List<String> FILES = Arrays.asList("text1.txt", "text2.txt", "text3.txt");

    public static void run() {
        // Крок 0: Підготовка файлів
        // Використовуємо .join(), щоб гарантувати створення файлів ДО читання
        CompletableFuture.runAsync(() -> {
            createDummyFiles();
            System.out.println("[Task 0] Файли створено/оновлено успішно.");
        }).join();

        System.out.println("[Setup] Готовність 100%. Запускаємо асинхронний ланцюжок...");

        // Крок 1: Завантаження
        CompletableFuture.supplyAsync(() -> {
                    long start = System.nanoTime();
                    System.out.println("[Task 1] Починаю читання файлів...");

                    List<String> sentences = new ArrayList<>();
                    for (String fileName : FILES) {
                        try {
                            sentences.addAll(Files.readAllLines(Paths.get(fileName)));
                        } catch (IOException e) {
                            System.err.println("Помилка читання " + fileName);
                        }
                    }

                    printTime("Завантаження файлів", start);
                    return sentences;
                })
                // Крок 2: Обробка
                .thenApplyAsync(originalSentences -> {
                    long start = System.nanoTime();
                    System.out.println(">>> Оригінальні речення: " + originalSentences);

                    // Видаляємо всі літери
                    List<String> processed = originalSentences.stream()
                            .map(s -> s.replaceAll("[a-zA-Zа-яА-ЯіІїЇєЄ]", ""))
                            .collect(Collectors.toList());

                    printTime("Обробка тексту", start);
                    return processed;
                })
                // Крок 3: Вивід результату
                .thenAcceptAsync(processedSentences -> {
                    long start = System.nanoTime();
                    System.out.println(">>> Оброблений масив (без літер): " + processedSentences);
                    printTime("Вивід результату", start);
                })
                // Крок 4: Фінал
                .thenRunAsync(() -> {
                    System.out.println("--- Варіант 1 повністю завершено ---");
                });
    }

    private static void createDummyFiles() {
        try {
            Files.write(Paths.get("text1.txt"), Collections.singletonList("Hello World! Перше речення."), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.write(Paths.get("text2.txt"), Collections.singletonList("Java 21. Number: 12345."), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.write(Paths.get("text3.txt"), Collections.singletonList("Async task test +-*/"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printTime(String taskName, long startTime) {
        long duration = (System.nanoTime() - startTime) / 1000;
        System.out.printf("[Timer] %s: %d мкс%n", taskName, duration);
    }
}

// ---------------------------------------------------------------
// ВАРІАНТ 2: Математична послідовність
// ---------------------------------------------------------------
class Variant2 {

    public static void run() {
        long globalStart = System.nanoTime();

        // Крок 1: Генерація
        CompletableFuture<double[]> dataFuture = CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();
            System.out.println("[Task 1] Генерація чисел...");

            double[] sequence = new double[20];
            for (int i = 0; i < sequence.length; i++) {
                sequence[i] = Math.round(ThreadLocalRandom.current().nextDouble(1, 10) * 100.0) / 100.0;
            }

            printTime("Генерація даних", start);
            return sequence;
        });

        // Гілка А: Вивід
        CompletableFuture<Void> printInputTask = dataFuture.thenAcceptAsync(sequence -> {
            long start = System.nanoTime();
            System.out.println(">>> Вхідна послідовність: " + Arrays.toString(sequence));
            printTime("Вивід масиву", start);
        });

        // Гілка Б: Обчислення -> Вивід
        CompletableFuture<Void> calcTask = dataFuture.thenApplyAsync(sequence -> {
            long start = System.nanoTime();
            System.out.println("[Task 2] Обчислення формули...");

            double sum = 0;
            for (int i = 0; i < sequence.length - 1; i++) {
                sum += sequence[i] * sequence[i+1];
            }

            printTime("Математичні обчислення", start);
            return sum;
        }).thenAcceptAsync(result -> {
            long start = System.nanoTime();
            System.out.printf(">>> РЕЗУЛЬТАТ ОБЧИСЛЕННЯ: %.4f%n", result);
            printTime("Вивід результату", start);
        });

        // Фінал
        CompletableFuture.allOf(printInputTask, calcTask).thenRunAsync(() -> {
            long totalDuration = (System.nanoTime() - globalStart) / 1_000_000;
            System.out.println("------------------------------------------------");
            System.out.printf("Загальний час роботи Варіанту 2: %d мс%n", totalDuration);
            System.out.println("--- Варіант 2 повністю завершено ---");
        });
    }

    private static void printTime(String taskName, long startTime) {
        long duration = (System.nanoTime() - startTime) / 1000;
        System.out.printf("[Timer] %s: %d мкс%n", taskName, duration);
    }
}