import java.util.Locale;

/**
 * Верифікація моделі запасів: на кожному етапі змінюється лише один параметр,
 * для кожного набору виконується RUNS прогонів, у таблицю записуються усереднені результати.
 */
public class FridgeExperiments {
    static final int RUNS = 5;
    static final double TIME = 20000;   // тижнів

    // середній інтервал попиту, контрольний рівень, точка замовлення, період перегляду, час доставки, імовірність повторного замовлення
    static final double[] BASE = {0.2, 72, 18, 4, 3, 0.2};

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        System.out.printf("%-40s | %9s | %12s | %13s | %10s%n",
                "Змінений параметр", "серед.запас", "втрачено/тиж", "інтервал, тиж", "невдов.поп");
        run("базові значення", BASE);
        run("інтервал між покупцями 0,1", with(0, 0.1));
        run("інтервал між покупцями 0,4", with(0, 0.4));
        run("контрольний рівень 48", with(1, 48));
        run("контрольний рівень 96", with(1, 96));
        run("точка замовлення 9", with(2, 9));
        run("точка замовлення 36", with(2, 36));
        run("період перегляду 2", with(3, 2));
        run("період перегляду 8", with(3, 8));
        run("час доставки 1", with(4, 1));
        run("час доставки 6", with(4, 6));
        run("імовірність повторного замовлення 0", with(5, 0.0));
        run("імовірність повторного замовлення 0,5", with(5, 0.5));
    }

    static double[] with(int idx, double v) {
        double[] p = BASE.clone();
        p[idx] = v;
        return p;
    }

    static void run(String label, double[] p) throws Exception {
        double stock = 0, lost = 0, back = 0;
        for (int k = 0; k < RUNS; k++) {
            double[] r = FridgeModel.simulate(TIME, p[0], (int) p[1], (int) p[2], p[3], p[4], p[5]);
            stock += r[0]; lost += r[1]; back += r[2];
        }
        stock /= RUNS; lost /= RUNS; back /= RUNS;
        System.out.printf("%-40s | %9.2f | %12.3f | %13.3f | %10.3f%n",
                label, stock, lost / TIME, lost > 0 ? TIME / lost : Double.POSITIVE_INFINITY, back);
    }
}
