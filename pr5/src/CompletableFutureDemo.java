import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class CompletableFutureDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("===== ЗАДАЧА 1: Об'єднання двох асинхронних завдань =====");
        solveTask1();

        System.out.println("\n===== ЗАДАЧА 2: Аналіз ПЗ (ціна, функціонал, підтримка) =====");
        solveTask2();
    }

    // --------------------------------------------------------
    // РІШЕННЯ ЗАДАЧІ 1
    // --------------------------------------------------------
    private static void solveTask1() throws ExecutionException, InterruptedException {
        // 1. Створюємо перше асинхронне завдання (наприклад, отримуємо частину даних)
        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> {
            simulateDelay(1);
            System.out.println("Task 1: Завантаження даних користувача завершено.");
            return "Користувач: Іван;";
        });

        // 2. Створюємо друге асинхронне завдання
        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
            simulateDelay(2); // Це завдання довше
            System.out.println("Task 2: Завантаження налаштувань завершено.");
            return " Налаштування: Dark Mode";
        });

        // 3. Використовуємо thenCombine для об'єднання результатів, коли ОБИДВА завдання готові
        CompletableFuture<String> combinedTask = task1.thenCombine(task2, (user, settings) -> {
            return "РЕЗУЛЬТАТ ОБ'ЄДНАННЯ: " + user + settings;
        });

        // Очікуємо і виводимо результат
        System.out.println(combinedTask.get());
    }

    // --------------------------------------------------------
    // РІШЕННЯ ЗАДАЧІ 2
    // --------------------------------------------------------
    private static void solveTask2() throws ExecutionException, InterruptedException {

        // КРОК 1: Демонстрація anyOf()
        // Уявимо, що у нас є два джерела даних (сервери), і ми беремо дані з того, який відповість швидше.
        CompletableFuture<String> serverEU = CompletableFuture.supplyAsync(() -> {
            simulateDelay(ThreadLocalRandom.current().nextInt(1, 4));
            return "Server EU";
        });
        CompletableFuture<String> serverUS = CompletableFuture.supplyAsync(() -> {
            simulateDelay(ThreadLocalRandom.current().nextInt(1, 4));
            return "Server US";
        });

        CompletableFuture<Object> fastestServer = CompletableFuture.anyOf(serverEU, serverUS);

        System.out.println("Підключено до найшвидшого сервера: " + fastestServer.get());

        // КРОК 2: Демонстрація thenCompose()
        // Уявимо, що ми обрали програму "IntelliJ IDEA" і нам треба отримати її ID, а потім за ID отримати деталі.
        // Це залежні дії, тому thenCompose.

        String softwareName = "IntelliJ IDEA";

        CompletableFuture<SoftwareStats> analysisTask = CompletableFuture.supplyAsync(() -> getSoftwareId(softwareName))
                .thenCompose(id -> {
                    System.out.println("Знайдено ID програми (" + id + "). Починаємо паралельний збір критеріїв...");

                    // КРОК 3: Паралельне отримання критеріїв (Задача з картинки)
                    CompletableFuture<Integer> priceTask = CompletableFuture.supplyAsync(() -> getPrice(id));
                    CompletableFuture<Integer> functionalityTask = CompletableFuture.supplyAsync(() -> getFunctionalityScore(id));
                    CompletableFuture<String> supportTask = CompletableFuture.supplyAsync(() -> getSupportInfo(id));

                    // КРОК 4: Демонстрація allOf()
                    // Чекаємо поки ВСІ три метрики будуть отримані
                    return CompletableFuture.allOf(priceTask, functionalityTask, supportTask)
                            .thenApply(v -> {
                                // Коли всі завершились, збираємо результати в об'єкт
                                // join() тут безпечний, бо allOf гарантує завершення
                                int p = priceTask.join();
                                int f = functionalityTask.join();
                                String s = supportTask.join();
                                return new SoftwareStats(softwareName, p, f, s);
                            });
                });

        // Отримуємо фінальний результат
        SoftwareStats result = analysisTask.get();
        System.out.println("\n=== ФІНАЛЬНИЙ ЗВІТ ПО ЗАДАЧІ 2 ===");
        System.out.println(result);

        // Логіка вибору (спрощена)
        if (result.functionalityScore > 8 && result.price < 500) {
            System.out.println("ВИСНОВОК: Це чудовий варіант для вибору!");
        } else {
            System.out.println("ВИСНОВОК: Варто пошукати інші варіанти.");
        }
    }

    // --- Допоміжні методи для імітації роботи ---

    private static int getSoftwareId(String name) {
        simulateDelay(1);
        return Math.abs(name.hashCode() % 1000);
    }

    private static int getPrice(int id) {
        simulateDelay(1);
        System.out.println("-> Ціна отримана");
        return ThreadLocalRandom.current().nextInt(100, 600); // Випадкова ціна
    }

    private static int getFunctionalityScore(int id) {
        simulateDelay(2); // Функціонал аналізується довше
        System.out.println("-> Функціональність оцінена");
        return ThreadLocalRandom.current().nextInt(1, 11); // Оцінка 1-10
    }

    private static String getSupportInfo(int id) {
        simulateDelay(1);
        System.out.println("-> Дані про підтримку отримані");
        return ThreadLocalRandom.current().nextBoolean() ? "24/7 Support" : "Email Only";
    }

    private static void simulateDelay(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Клас для зберігання результатів (OOP)
    static class SoftwareStats {
        String name;
        int price;
        int functionalityScore;
        String support;

        public SoftwareStats(String name, int price, int functionalityScore, String support) {
            this.name = name;
            this.price = price;
            this.functionalityScore = functionalityScore;
            this.support = support;
        }

        @Override
        public String toString() {
            return String.format("Програма: %s | Ціна: $%d | Функціонал: %d/10 | Підтримка: %s",
                    name, price, functionalityScore, support);
        }
    }
}