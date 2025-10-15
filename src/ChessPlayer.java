import java.util.concurrent.Semaphore;
import java.util.Random;


 //Клас, що представляє гравця в шахи.
 //Кожен гравець виконується у власному потоці.
 //Реалізує інтерфейс Runnable.

public class ChessPlayer implements Runnable {

    private final String name; // Ім'я гравця (наприклад, "Гравець Білими")
    private final Semaphore mySemaphore; // Семафор для отримання дозволу на хід
    private final Semaphore opponentSemaphore; // Семафор суперника, якому потрібно передати хід
    private static final int MAX_MOVES = 5; // Кількість ходів для кожного гравця для симуляції
    private final Random random = new Random(); // Для імітації часу на роздуми

    public ChessPlayer(String name, Semaphore mySemaphore, Semaphore opponentSemaphore) {
        this.name = name;
        this.mySemaphore = mySemaphore;
        this.opponentSemaphore = opponentSemaphore;
    }


     //Основний метод, який виконується в потоці.
     //Містить логіку гри для одного гравця.

    @Override
    public void run() {
        // Гра триває, поки кожен гравець не зробить MAX_MOVES ходів
        for (int i = 1; i <= MAX_MOVES; i++) {
            try {
                // 1. Очікування своєї черги (спроба отримати дозвіл від семафора)
                System.out.println(name + " очікує на свій хід...");
                mySemaphore.acquire(); // Блокує потік, доки не буде отримано дозвіл

                // 2. Виконання ходу
                System.out.println("------------------------------------------");
                System.out.println("Зараз хід робить " + name + " (Хід #" + i + ")");

                // Імітація роздумів над ходом
                int thinkingTime = random.nextInt(3000) + 1000; // від 1 до 4 секунд
                System.out.println(name + " думає " + thinkingTime / 1000.0 + " секунд...");
                Thread.sleep(thinkingTime); // Призупиняємо потік, імітуючи роздуми

                System.out.println("✅ " + name + " зробив свій хід!");
                System.out.println("------------------------------------------\n");


            } catch (InterruptedException e) {
                // Обробка помилки, якщо потік буде перервано під час очікування (sleep або acquire)
                System.err.println(name + " був перерваний під час гри. Гра завершується.");
                // Відновлення статусу переривання потоку
                Thread.currentThread().interrupt();
                return; // Завершуємо виконання потоку
            } finally {
                // 3. Передача ходу супернику (звільнення семафора суперника)
                // Цей блок виконається завжди, навіть якщо виникла помилка,
                // щоб уникнути взаємного блокування (deadlock).
                opponentSemaphore.release();
            }
        }
        System.out.println("🏁 " + name + " закінчив свою партію.");
    }
}