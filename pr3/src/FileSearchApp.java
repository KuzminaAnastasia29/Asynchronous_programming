import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class FileSearchApp {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 1. Введення шляху та розміру
        System.out.println("=== Пошук файлів ===");
        System.out.print("Введіть шлях до стартової директорії: ");
        String path = scanner.nextLine();

        System.out.print("Введіть мінімальний розмір файлу (у байтах): ");
        long sizeThreshold;
        while (!scanner.hasNextLong()) {
            System.out.println("Введіть коректне число.");
            scanner.next();
        }
        sizeThreshold = scanner.nextLong();

        File startDir = new File(path);
        if (!startDir.exists() || !startDir.isDirectory()) {
            System.out.println("Помилка: Вказаний шлях не існує або це не директорія.");
            return;
        }

        // 2. Запуск Fork/Join задачі
        // Використовуємо commonPool або створюємо свій
        ForkJoinPool pool = new ForkJoinPool();
        FileCounterTask task = new FileCounterTask(startDir, sizeThreshold);

        System.out.println("Пошук розпочато...");
        long startTime = System.nanoTime();

        Integer count = pool.invoke(task);

        long endTime = System.nanoTime();

        // 3. Результати
        System.out.println("---------------------------");
        System.out.println("Знайдено файлів: " + count);
        System.out.printf("Час виконання: %.2f мс%n", (endTime - startTime) / 1_000_000.0);
    }

    /**
     * Рекурсивна задача для підрахунку файлів.
     * Повертає Integer - кількість знайдених файлів.
     */
    static class FileCounterTask extends RecursiveTask<Integer> {
        private final File directory;
        private final long sizeThreshold;

        public FileCounterTask(File directory, long sizeThreshold) {
            this.directory = directory;
            this.sizeThreshold = sizeThreshold;
        }

        @Override
        protected Integer compute() {
            int count = 0;
            List<FileCounterTask> subTasks = new ArrayList<>();

            File[] files = directory.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // Для директорій створюємо нову підзадачу (fork)
                        FileCounterTask subTask = new FileCounterTask(file, sizeThreshold);
                        subTask.fork(); // Асинхронний запуск (кладемо в чергу потоку)
                        subTasks.add(subTask);
                    } else {
                        // Перевірка розміру файлу
                        if (file.length() > sizeThreshold) {
                            count++;
                        }
                    }
                }
            }

            // Збираємо результати від усіх підзадач (join - work stealing happens here if needed)
            for (FileCounterTask subTask : subTasks) {
                count += subTask.join();
            }

            return count;
        }
    }
}