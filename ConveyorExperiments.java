import java.util.Locale;

public class ConveyorExperiments {
    static final int RUNS = 5;
    static final double TIME = 5000;

    // параметри за замовчуванням: інтервал надходження, обробка, перехід між пристроями, повернення, кількість пристроїв
    static final double[] BASE = {0.25, 1.0, 1.0, 5.0, 5};

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        System.out.printf("%-34s | %6s | %7s | %7s | %6s | %6s | %7s%n",
                "Змінений параметр", "sum rho", "очікув.", "rho_1", "rho_n", "tau", "D/T");
        run("базові значення", BASE);
        run("інтервал надходження 0,3", with(0, 0.3));
        run("інтервал надходження 0,5", with(0, 0.5));
        run("інтервал надходження 1,0", with(0, 1.0));
        run("час обробки 0,5", with(1, 0.5));
        run("час обробки 0,8", with(1, 0.8));
        run("час переходу між пристроями 0,5", with(2, 0.5));
        run("час переходу між пристроями 2", with(2, 2.0));
        run("час повернення 2", with(3, 2.0));
        run("час повернення 10", with(3, 10.0));
        run("кількість пристроїв 6", with(4, 6));
        run("кількість пристроїв 8", with(4, 8));
    }

    static double[] with(int idx, double v) {
        double[] p = BASE.clone();
        p[idx] = v;
        return p;
    }

    static void run(String label, double[] p) throws Exception {
        double busy = 0, rho1 = 0, rhoN = 0, tau = 0, thr = 0;
        for (int k = 0; k < RUNS; k++) {
            ConveyorModel.Result r = ConveyorModel.simulate(TIME, p[0], p[1], p[2], p[3], (int) p[4]);
            busy += r.busy; rho1 += r.rho[0]; rhoN += r.rho[r.rho.length - 1];
            tau += r.tau; thr += r.processed / TIME;
        }
        double expected = p[1] / p[0];   // lambda * середній час обробки
        System.out.printf("%-34s | %6.3f | %7.3f | %7.3f | %6.3f | %6.2f | %7.3f%n",
                label, busy / RUNS, expected, rho1 / RUNS, rhoN / RUNS, tau / RUNS, thr / RUNS);
        System.out.flush();
    }
}
