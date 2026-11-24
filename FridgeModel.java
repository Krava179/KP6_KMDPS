import ua.stetsenkoinna.petriobj.*;
import java.util.ArrayList;

public class FridgeModel {

    static final int LEVEL = 72;       // контрольний рівень
    static final int POINT = 18;       // точка замовлення
    static final double REVIEW = 4;    // період перегляду, тижні
    static final double DELIVERY = 3;  // час доставки, тижні

    public static PetriNet buildNet(double interArrival, boolean det) throws ExceptionInvalidTimeDelay {
        return buildNet(interArrival, det, LEVEL, POINT, REVIEW, DELIVERY, 0.2);
    }

    /**
     * level - контрольний рівень, point - точка замовлення, review - період перегляду,
     * delivery - час доставки, pBack - імовірність повторного замовлення при відсутності запасу.
     */
    public static PetriNet buildNet(double interArrival, boolean det, int level, int point,
                                    double review, double delivery, double pBack) throws ExceptionInvalidTimeDelay {
        PetriP.initNext(); PetriT.initNext(); ArcIn.initNext(); ArcOut.initNext();
        ArrayList<PetriP> P = new ArrayList<>();
        ArrayList<PetriT> T = new ArrayList<>();
        ArrayList<ArcIn> in = new ArrayList<>();
        ArrayList<ArcOut> out = new ArrayList<>();

        PetriP Pc = new PetriP("Pc", 1);        // джерело покупців
        PetriP Pd = new PetriP("Pd", 0);        // покупець, що прийшов
        PetriP Ps = new PetriP("Ps", level);    // наявний запас
        PetriP Pg = new PetriP("Pg", 0);        // LEVEL - поточний стан запасу
        PetriP Pb = new PetriP("Pb", 0);        // невдоволений попит (очікують)
        PetriP Plost = new PetriP("Plost", 0);  // продажі, що не відбулися
        PetriP Psold = new PetriP("Psold", 0);  // продано зі складу
        PetriP Prev = new PetriP("Prev", 1);    // таймер перегляду
        PetriP Pchk = new PetriP("Pchk", 0);    // перегляд відбувся
        PetriP Pf = new PetriP("Pf", 0);        // оформлення замовлення
        PetriP Pw = new PetriP("Pw", 0);        // замовлено, у дорозі
        for (PetriP p : new PetriP[]{Pc, Pd, Ps, Pg, Pb, Plost, Psold, Prev, Pchk, Pf, Pw}) P.add(p);

        // надходження покупців: exp, M = 0,2 тижня
        PetriT Tarr = new PetriT("Tarr", interArrival);
        if (!det) Tarr.setDistribution("exp", interArrival);
        T.add(Tarr);
        in.add(new ArcIn(Pc, Tarr)); out.add(new ArcOut(Tarr, Pc, 1)); out.add(new ArcOut(Tarr, Pd, 1));

        // видача холодильника очікуючому покупцю (пріоритет 4)
        PetriT Tsb = new PetriT("Tsb", 0.0, 4);
        T.add(Tsb);
        in.add(new ArcIn(Pb, Tsb)); in.add(new ArcIn(Ps, Tsb));

        // продаж зі складу (пріоритет 3)
        PetriT Tsell = new PetriT("Tsell", 0.0, 3);
        T.add(Tsell);
        in.add(new ArcIn(Pd, Tsell)); in.add(new ArcIn(Ps, Tsell));
        out.add(new ArcOut(Tsell, Pg, 1)); out.add(new ArcOut(Tsell, Psold, 1));

        // запасу немає: 80% йде в інший магазин, 20% - повторне замовлення
        PetriT Tlost = new PetriT("Tlost", 0.0, 1 - pBack);
        Tlost.setPriority(2);
        T.add(Tlost);
        in.add(new ArcIn(Pd, Tlost)); out.add(new ArcOut(Tlost, Plost, 1));
        PetriT Tback = new PetriT("Tback", 0.0, pBack);
        Tback.setPriority(2);
        T.add(Tback);
        in.add(new ArcIn(Pd, Tback)); out.add(new ArcOut(Tback, Pb, 1)); out.add(new ArcOut(Tback, Pg, 1));

        // періодичний перегляд, кожні 4 тижні
        PetriT Trev = new PetriT("Trev", review);
        T.add(Trev);
        in.add(new ArcIn(Prev, Trev)); out.add(new ArcOut(Trev, Pchk, 1));

        // стан запасу <= 18, тобто Pg >= 72 - 18 = 54: замовляємо (пріоритет 1)
        PetriT Tord = new PetriT("Tord", 0.0, 1);
        T.add(Tord);
        in.add(new ArcIn(Pchk, Tord)); in.add(new ArcIn(Pg, Tord, level - point));
        out.add(new ArcOut(Tord, Pg, level - point)); out.add(new ArcOut(Tord, Pf, 1));
        // інакше замовлення не потрібне (пріоритет 0)
        PetriT Tskip = new PetriT("Tskip", 0.0, 0);
        T.add(Tskip);
        in.add(new ArcIn(Pchk, Tskip)); out.add(new ArcOut(Tskip, Prev, 1));

        // розміщення замовлення: по одному холодильнику з Pg у Pw, поки Pg не порожня
        PetriT Tmove = new PetriT("Tmove", 0.0, 1);
        T.add(Tmove);
        in.add(new ArcIn(Pg, Tmove)); in.add(new ArcIn(Pf, Tmove));
        out.add(new ArcOut(Tmove, Pf, 1)); out.add(new ArcOut(Tmove, Pw, 1));
        PetriT Tend = new PetriT("Tend", 0.0, 0);
        T.add(Tend);
        in.add(new ArcIn(Pf, Tend)); out.add(new ArcOut(Tend, Prev, 1));

        // доставка 3 тижні
        PetriT Tdel = new PetriT("Tdel", delivery);
        T.add(Tdel);
        in.add(new ArcIn(Pw, Tdel)); out.add(new ArcOut(Tdel, Ps, 1));

        return new PetriNet("Fridge", P, T, in, out);
    }

    static double meanOf(PetriNet net, String name) {
        for (PetriP p : net.getListP()) if (p.getName().equals(name)) return p.getMean();
        for (PetriT t : net.getListT()) if (t.getName().equals(name)) return t.getMean();
        throw new IllegalArgumentException(name);
    }
    static int markOf(PetriNet net, String name) {
        for (PetriP p : net.getListP()) if (p.getName().equals(name)) return p.getMark();
        throw new IllegalArgumentException(name);
    }

    /** Один прогін: повертає {середній запас, кількість втрачених продажів, середній невдоволений попит}. */
    public static double[] simulate(double time, double interArrival, int level, int point,
                                    double review, double delivery, double pBack) throws Exception {
        PetriSim sim = new PetriSim(buildNet(interArrival, false, level, point, review, delivery, pBack));
        ArrayList<PetriSim> list = new ArrayList<>();
        list.add(sim);
        PetriObjModel model = new PetriObjModel(list);
        model.setIsProtokol(false);
        model.go(time);
        PetriNet net = sim.getNet();
        return new double[]{meanOf(net, "Ps"), markOf(net, "Plost"), meanOf(net, "Pb")};
    }

    public static void main(String[] args) throws Exception {
        double time = args.length > 0 ? Double.parseDouble(args[0]) : 10000;
        double interArrival = args.length > 1 ? Double.parseDouble(args[1]) : 0.2;
        boolean det = args.length > 2 && args[2].equals("det");   // детермінований потік (для перевірки)
        PetriSim sim = new PetriSim(buildNet(interArrival, det));
        ArrayList<PetriSim> list = new ArrayList<>();
        list.add(sim);
        PetriObjModel model = new PetriObjModel(list);
        model.setIsProtokol(false);
        model.go(time);

        PetriNet net = sim.getNet();
        int lost = markOf(net, "Plost");
        System.out.println("Час моделювання, тижнів: " + time);
        System.out.printf("Середня кількість холодильників у запасі: %.4f%n", meanOf(net, "Ps"));
        System.out.println("Кількість продажів, що не відбулися: " + lost);
        System.out.printf("Середній час між продажами, що не відбулися: %.4f тижня%n", time / lost);
        System.out.println("Продано зі складу: " + markOf(net, "Psold"));
        System.out.printf("Середній невдоволений попит: %.4f%n", meanOf(net, "Pb"));
        // контроль інваріанта (у середньому): запас - очікуючі + замовлене + Pg = 72
        double inTransit = meanOf(net, "Pw") + meanOf(net, "Tdel");
        System.out.printf("Перевірка інваріанта: %.4f (має бути 72)%n", meanOf(net, "Ps") - meanOf(net, "Pb") + inTransit + meanOf(net, "Pg"));
        PetriT del = null;
        for (PetriT t : net.getListT()) if (t.getName().equals("Tdel")) del = t;
        System.out.println("Стан наприкінці: Ps=" + markOf(net, "Ps") + ", Pb=" + markOf(net, "Pb") + ", Pg=" + markOf(net, "Pg")
                + ", замовлено (Pw + у дорозі)=" + (markOf(net, "Pw") + del.getBuffer()));
    }
}
