import java.util.*;
import java.util.concurrent.*;

/**
 * Клас задачі, який імплементує Callable.
 * Приймає масив чисел і повертає масив їх квадратів.
 */
class SquareCalculator implements Callable<Double[]> {
    private final Double[] inputChunk;
    private final int chunkId;

    public SquareCalculator(Double[] inputChunk, int chunkId) {
        this.inputChunk = inputChunk;
        this.chunkId = chunkId;
    }

    @Override
    public Double[] call() throws Exception {
        // Емуляція невеликої затримки для наочності асинхронності
        Thread.sleep(200);

        Double[] results = new Double[inputChunk.length];
        for (int i = 0; i < inputChunk.length; i++) {
            // Піднесення до квадрату
            results[i] = Math.pow(inputChunk[i], 2);
        }

        System.out.println("Потік [" + Thread.currentThread().getName() + "] обробив частину #" + chunkId);
        return results;
    }
}

public class AsyncSquaresDemo {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();

        // 1. Ввід діапазону користувачем (за варіантом 0,5 - 99,5)
        System.out.println("Введіть мінімальне значення діапазону (наприклад, 0,5): ");
        double minRange = scanner.nextDouble();

        System.out.println("Введіть максимальне значення діапазону (наприклад, 99,5): ");
        double maxRange = scanner.nextDouble();

        // Фіксація часу початку роботи
        long startTime = System.nanoTime();

        // 2. Генерація масиву (40-60 елементів)
        int arraySize = 40 + random.nextInt(21); // генерує від 40 до 60
        Double[] mainArray = new Double[arraySize];

        System.out.println("--- Генерируємо масив з " + arraySize + " елементів ---");
        for (int i = 0; i < arraySize; i++) {
            double val = minRange + (maxRange - minRange) * random.nextDouble();
            mainArray[i] = Math.round(val * 100.0) / 100.0; // Округлення до 2 знаків для краси
        }
        System.out.println("Вхідні дані: " + Arrays.toString(mainArray));
        System.out.println("----------------------------------------------------");

        // 3. Підготовка ExecutorService та колекції CopyOnWriteArraySet
        // Використовуємо пул потоків. Кількість потоків залежить від доступних ядер процесора
        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores > 1 ? cores : 2);

        // Потокобезпечна колекція згідно варіанту
        CopyOnWriteArraySet<Double> resultSet = new CopyOnWriteArraySet<>();
        List<Future<Double[]>> futureList = new ArrayList<>();

        // 4. Розбиття на частини (chunks) та запуск задач
        int chunkSize = 10; // Розмір однієї частини
        int chunkCount = 0;

        for (int i = 0; i < mainArray.length; i += chunkSize) {
            int end = Math.min(mainArray.length, i + chunkSize);
            // Копіюємо частину масиву
            Double[] chunk = Arrays.copyOfRange(mainArray, i, end);

            // Створюємо Callable та передаємо на виконання
            Callable<Double[]> task = new SquareCalculator(chunk, ++chunkCount);
            Future<Double[]> future = executor.submit(task);
            futureList.add(future);
        }

        System.out.println("Задачі відправлені на виконання...");

        // 5. Збір результатів через Future
        for (int i = 0; i < futureList.size(); i++) {
            Future<Double[]> future = futureList.get(i);

            try {
                // Демонстрація методів isDone() та isCancelled()
                // Оскільки ми викликаємо .get(), потік заблокується, доки задача не виконається,
                // тому isDone буде true одразу після get(), але перевіримо логіку:

                if (!future.isCancelled()) {
                    // get() блокує виконання до отримання результату
                    Double[] resultChunk = future.get();

                    // Перевірка isDone()
                    if (future.isDone()) {
                        // Додаємо результати у CopyOnWriteArraySet
                        resultSet.addAll(Arrays.asList(resultChunk));
                    }
                } else {
                    System.out.println("Задача #" + (i + 1) + " була скасована.");
                }

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // Завершуємо роботу екзек'ютора
        executor.shutdown();

        // 6. Вивід результатів
        System.out.println("----------------------------------------------------");
        System.out.println("Результат (Квадрати чисел у CopyOnWriteArraySet):");
        // Set не гарантує порядок вставки, але гарантує унікальність
        System.out.println(resultSet);
        System.out.println("Кількість елементів у результаті: " + resultSet.size());

        // 7. Вивід часу роботи
        long endTime = System.nanoTime();
        double durationInMs = (endTime - startTime) / 1_000_000.0;

        System.out.println("----------------------------------------------------");
        System.out.printf("Час роботи програми: %.2f мс%n", durationInMs);
    }
}