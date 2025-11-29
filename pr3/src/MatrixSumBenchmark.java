import java.util.*;
import java.util.concurrent.*;

public class MatrixSumBenchmark {

    // Поріг для ForkJoin (коли зупинити поділ задачі)
    private static final int THRESHOLD = 10;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 1. Введення даних та валідація
        System.out.println("=== Генерація матриці ===");
        int rows = getValidInput(scanner, "Введіть кількість рядків: ");
        int cols = getValidInput(scanner, "Введіть кількість стовпців: ");
        int minVal = getValidInput(scanner, "Введіть мінімальне значення елемента: ");
        int maxVal = getValidInput(scanner, "Введіть максимальне значення елемента: ");

        if (maxVal < minVal) {
            System.out.println("Помилка: Максимальне значення менше мінімального. Міняємо місцями.");
            int temp = minVal; minVal = maxVal; maxVal = temp;
        }

        // 2. Генерація матриці
        int[][] matrix = generateMatrix(rows, cols, minVal, maxVal);
        if (rows <= 20 && cols <= 20) {
            printMatrix(matrix);
        } else {
            System.out.println("Матриця занадто велика для виводу.");
        }

        // 3. Запуск Work Stealing (Fork/Join)
        System.out.println("\n--- Підхід Work Stealing (Fork/Join) ---");
        long startStealing = System.nanoTime();

        ForkJoinPool fjPool = new ForkJoinPool();
        long[] resultsStealing = new long[cols];
        // Запускаємо рекурсивну задачу, яка заповнює масив resultsStealing
        fjPool.invoke(new ColumnSumRecursiveAction(matrix, 0, cols, resultsStealing));

        long endStealing = System.nanoTime();
        printResults(resultsStealing);
        System.out.printf("Час виконання (Work Stealing): %.4f мс%n", (endStealing - startStealing) / 1_000_000.0);


        // 4. Запуск Work Dealing (ExecutorService)
        System.out.println("\n--- Підхід Work Dealing (ExecutorService) ---");
        long startDealing = System.nanoTime();

        // Кількість потоків = кількість ядер
        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores);
        long[] resultsDealing = new long[cols];
        List<Future<?>> futures = new ArrayList<>();

        // Роздаємо задачі: кожен стовпець - окрема задача (Work Dealing у простому вигляді)
        for (int j = 0; j < cols; j++) {
            final int colIndex = j;
            futures.add(executor.submit(() -> {
                long sum = 0;
                for (int i = 0; i < matrix.length; i++) {
                    sum += matrix[i][colIndex];
                }
                resultsDealing[colIndex] = sum;
            }));
        }

        // Чекаємо виконання всіх задач
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();

        long endDealing = System.nanoTime();
        printResults(resultsDealing);
        System.out.printf("Час виконання (Work Dealing): %.4f мс%n", (endDealing - startDealing) / 1_000_000.0);
    }

    // --- Допоміжні класи та методи ---

    // Клас для Fork/Join (Work Stealing)
    static class ColumnSumRecursiveAction extends RecursiveAction {
        private final int[][] matrix;
        private final int startCol;
        private final int endCol;
        private final long[] results;

        public ColumnSumRecursiveAction(int[][] matrix, int startCol, int endCol, long[] results) {
            this.matrix = matrix;
            this.startCol = startCol;
            this.endCol = endCol;
            this.results = results;
        }

        @Override
        protected void compute() {
            // Якщо діапазон малий - обчислюємо прямо
            if (endCol - startCol <= THRESHOLD) {
                for (int j = startCol; j < endCol; j++) {
                    long sum = 0;
                    for (int i = 0; i < matrix.length; i++) {
                        sum += matrix[i][j];
                    }
                    results[j] = sum;
                }
            } else {
                // Інакше ділимо навпіл
                int mid = startCol + (endCol - startCol) / 2;
                invokeAll(
                        new ColumnSumRecursiveAction(matrix, startCol, mid, results),
                        new ColumnSumRecursiveAction(matrix, mid, endCol, results)
                );
            }
        }
    }

    private static int[][] generateMatrix(int rows, int cols, int min, int max) {
        Random random = new Random();
        int[][] matrix = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = random.nextInt(max - min + 1) + min;
            }
        }
        return matrix;
    }

    private static void printMatrix(int[][] matrix) {
        System.out.println("Згенерована матриця:");
        for (int[] row : matrix) {
            System.out.println(Arrays.toString(row));
        }
    }

    private static void printResults(long[] results) {
        if (results.length <= 20) {
            System.out.println("Суми стовпців: " + Arrays.toString(results));
        } else {
            System.out.println("Суми стовпців розраховано (приховано через великий розмір).");
        }
    }

    private static int getValidInput(Scanner s, String prompt) {
        int val;
        while (true) {
            System.out.print(prompt);
            if (s.hasNextInt()) {
                val = s.nextInt();
                if (val > 0 || prompt.contains("значення")) break; // Для розмірів > 0, для значень можна < 0
                System.out.println("Число має бути позитивним (для розмірів).");
            } else {
                System.out.println("Будь ласка, введіть ціле число.");
                s.next();
            }
        }
        return val;
    }
}