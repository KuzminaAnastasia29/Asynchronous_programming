import java.util.concurrent.Semaphore;


 //Головний клас для запуску симуляції шахової гри.
 //Створює гравців та координує початок і кінець гри.

public class ChessGame {

    public static void main(String[] args) {
        System.out.println("👑 Починаємо шахову партію між двома гравцями! 👑\n");

        // Створюємо два семафори.
        // Одночасно існує лише один ключ.

        // whiteSemaphore(1) - Гравець білими має ключ на початку гри (1 дозвіл).
        Semaphore whiteSemaphore = new Semaphore(1);

        // blackSemaphore(0) - Гравець чорними чекає, поки білі передадуть йому ключ (0 дозволів).
        Semaphore blackSemaphore = new Semaphore(0);

        // Створюємо двох гравців.
        // Гравець Білими чекає на "білий" семафор і після ходу звільняє "чорний".
        ChessPlayer whitePlayer = new ChessPlayer("Гравець Білими", whiteSemaphore, blackSemaphore);

        // Гравець Чорними чекає на "чорний" семафор і після ходу звільняє "білий".
        ChessPlayer blackPlayer = new ChessPlayer("Гравець Чорними", blackSemaphore, whiteSemaphore);

        // Створюємо потоки на основі наших гравців
        Thread whiteThread = new Thread(whitePlayer);
        Thread blackThread = new Thread(blackPlayer);

        // Зміна стану потоків: з NEW на RUNNABLE
        System.out.println("Гравці сідають за стіл. Починаємо гру...\n");
        whiteThread.start();
        blackThread.start();

        try {
            // Головний потік чекає, поки обидва гравці завершать гру (стан потоку - TERMINATED)
            whiteThread.join();
            blackThread.join();
        } catch (InterruptedException e) {
            System.err.println("Головний потік був перерваний.");
            Thread.currentThread().interrupt();
        }

        System.out.println("\n🎉🎉🎉 Гра завершена! 🎉🎉🎉");
    }
}