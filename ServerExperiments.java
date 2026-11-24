import java.util.Locale;

public class ServerExperiments {
    static final int RUNS = 5;
    static final double TIME = 20000;   // секунд

    // інтервал надходження, час обробки головним, час обробки допоміжним, імовірність звернення до допоміжного
    static final double[] BASE = {1.0, 0.6, 1.0, 0.3};

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        System.out.printf("%-36s | %6s | %6s | %6s | %6s | %7s | %7s%n",
                "Змінений параметр", "rho_m", "rho_a", "Q_m", "Q_a", "час", "теорія");
        run("базові значення", BASE);
        run("інтервал надходження 0,8", with(0, 0.8));
        run("інтервал надходження 1,5", with(0, 1.5));
        run("час обробки головним 0,4", with(1, 0.4));
        run("час обробки головним 0,8", with(1, 0.8));
        run("час обробки допоміжним 0,5", with(2, 0.5));
        run("час обробки допоміжним 1,5", with(2, 1.5));
        run("імовірність звернення 0,1", with(3, 0.1));
        run("імовірність звернення 0,6", with(3, 0.6));
    }

    static double[] with(int idx, double v) {
        double[] p = BASE.clone();
        p[idx] = v;
        return p;
    }

    static void run(String label, double[] p) throws Exception {
        double[] sum = new double[5];
        for (int k = 0; k < RUNS; k++) {
            double[] r = ServerModel.simulate(TIME, p[0], p[1], p[2], p[3]);
            for (int i = 0; i < 5; i++) sum[i] += r[i];
        }
        double lam = 1.0 / p[0], lamA = lam * p[3];
        double theory = 1.0 / (1.0 / p[1] - lam) + p[3] / (1.0 / p[2] - lamA);   // M/M/1 + p * M/M/1
        System.out.printf("%-36s | %6.3f | %6.3f | %6.3f | %6.3f | %7.3f | %7.3f%n",
                label, sum[0] / RUNS, sum[1] / RUNS, sum[2] / RUNS, sum[3] / RUNS, sum[4] / RUNS, theory);
    }
}
