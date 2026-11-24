import ua.stetsenkoinna.petriobj.*;
import java.util.ArrayList;

public class ConveyorModel {

    /** Мережа Петрі конвеєра. n - кількість пристроїв; час у хвилинах. */
    public static PetriNet buildNet(double interArrival, double service, double transit, double ret, int n)
            throws ExceptionInvalidTimeDelay {
        // номери позицій і переходів присвоюються за порядком створення, тому скидаємо лічильники
        PetriP.initNext(); PetriT.initNext(); ArcIn.initNext(); ArcOut.initNext();

        ArrayList<PetriP> P = new ArrayList<>();
        ArrayList<PetriT> T = new ArrayList<>();
        ArrayList<ArcIn> in = new ArrayList<>();
        ArrayList<ArcOut> out = new ArrayList<>();

        PetriP S = new PetriP("S", 1);
        PetriP D = new PetriP("D", 0);
        PetriP[] Q = new PetriP[n], B = new PetriP[n], F = new PetriP[n];
        for (int i = 0; i < n; i++) {
            Q[i] = new PetriP("Q" + (i + 1), 0);
            B[i] = new PetriP("B" + (i + 1), 0);
            F[i] = new PetriP("F" + (i + 1), 1);
        }
        P.add(S); P.add(D);
        for (int i = 0; i < n; i++) { P.add(Q[i]); P.add(B[i]); P.add(F[i]); }

        // надходження деталей
        PetriT Tin = new PetriT("Tin", interArrival);
        T.add(Tin);
        in.add(new ArcIn(S, Tin));
        out.add(new ArcOut(Tin, S, 1));
        out.add(new ArcOut(Tin, Q[0], 1));

        for (int i = 0; i < n; i++) {
            // Tk_i: пристрій вільний -> деталь береться на обробку (пріоритет 1, затримка 0)
            PetriT Tk = new PetriT("Tk" + (i + 1), 0.0, 1);
            T.add(Tk);
            in.add(new ArcIn(Q[i], Tk)); in.add(new ArcIn(F[i], Tk));
            out.add(new ArcOut(Tk, B[i], 1));

            // Ts_i: обробка, експоненціальний розподіл
            PetriT Ts = new PetriT("Ts" + (i + 1), service);
            Ts.setDistribution("exp", service);
            T.add(Ts);
            in.add(new ArcIn(B[i], Ts));
            out.add(new ArcOut(Ts, F[i], 1)); out.add(new ArcOut(Ts, D, 1));

            if (i < n - 1) {
                // Tm_i: деталь їде до наступного пристрою (пріоритет 0)
                PetriT Tm = new PetriT("Tm" + (i + 1), transit, 0);
                T.add(Tm);
                in.add(new ArcIn(Q[i], Tm));
                out.add(new ArcOut(Tm, Q[i + 1], 1));
            }
        }
        // Tr: повернення з останньої черги до першої (пріоритет 0)
        PetriT Tr = new PetriT("Tr", ret, 0);
        T.add(Tr);
        in.add(new ArcIn(Q[n - 1], Tr));
        out.add(new ArcOut(Tr, Q[0], 1));

        return new PetriNet("Conveyor", P, T, in, out);
    }

    /** Один прогін моделі. Повертає: завантаження пристроїв, L, середній час у системі, кількість оброблених. */
    public static Result simulate(double time, double interArrival, double service, double transit, double ret, int n)
            throws ExceptionInvalidTimeDelay {
        PetriSim sim = new PetriSim(buildNet(interArrival, service, transit, ret, n));
        ArrayList<PetriSim> list = new ArrayList<>();
        list.add(sim);
        PetriObjModel model = new PetriObjModel(list);
        model.setIsProtokol(false);
        model.go(time);

        PetriNet net = sim.getNet();
        Result r = new Result();
        r.rho = new double[n];
        for (int i = 1; i <= n; i++) {
            r.rho[i - 1] = mean(net, "Ts" + i);
            r.busy += r.rho[i - 1];
        }
        double L = 0;
        for (PetriP p : net.getListP())
            if (p.getName().startsWith("Q") || p.getName().startsWith("B")) L += p.getMean();
        for (PetriT t : net.getListT())
            if (t.getName().startsWith("Tm") || t.getName().startsWith("Ts") || t.getName().equals("Tr")) L += t.getMean();
        r.L = L;
        r.tau = L * interArrival;                 // формула Літтла, lambda = 1 / interArrival
        r.processed = net.getListP()[1].getMark();
        return r;
    }

    public static class Result {
        public double[] rho;
        public double busy, L, tau;
        public int processed;
    }

    static double mean(PetriNet net, String name) {
        for (PetriP p : net.getListP()) if (p.getName().equals(name)) return p.getMean();
        for (PetriT t : net.getListT()) if (t.getName().equals(name)) return t.getMean();
        throw new IllegalArgumentException(name);
    }

    public static void main(String[] args) throws Exception {
        double time = args.length > 0 ? Double.parseDouble(args[0]) : 10000;
        double interArrival = args.length > 1 ? Double.parseDouble(args[1]) : 0.25;
        Result r = simulate(time, interArrival, 1.0, 1.0, 5.0, 5);
        System.out.println("Час моделювання: " + time);
        for (int i = 0; i < r.rho.length; i++)
            System.out.printf("Завантаження пристрою %d: %.4f%n", i + 1, r.rho[i]);
        System.out.printf("Середня кількість зайнятих пристроїв: %.4f%n", r.busy);
        System.out.printf("Середня кількість деталей у системі L = %.4f%n", r.L);
        System.out.printf("Середній час у системі (Літтл) = %.4f хв%n", r.tau);
        System.out.println("Оброблено деталей (D): " + r.processed);
    }
}
