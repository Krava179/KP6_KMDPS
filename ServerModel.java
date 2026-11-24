import ua.stetsenkoinna.petriobj.*;
import java.util.ArrayList;

public class ServerModel {

    static final double P_AUX = 0.3;   // імовірність звернення до допоміжного сервера (за замовчуванням)

    public static PetriNet buildNet(double interArrival, double mainTime, double auxTime) throws ExceptionInvalidTimeDelay {
        return buildNet(interArrival, mainTime, auxTime, P_AUX);
    }

    public static PetriNet buildNet(double interArrival, double mainTime, double auxTime, double pAux) throws ExceptionInvalidTimeDelay {
        PetriP.initNext(); PetriT.initNext(); ArcIn.initNext(); ArcOut.initNext();
        ArrayList<PetriP> P = new ArrayList<>();
        ArrayList<PetriT> T = new ArrayList<>();
        ArrayList<ArcIn> in = new ArrayList<>();
        ArrayList<ArcOut> out = new ArrayList<>();

        PetriP Pgen = new PetriP("Pgen", 1);   // джерело запитів
        PetriP Qm = new PetriP("Qm", 0);       // черга до головного сервера
        PetriP Fm = new PetriP("Fm", 1);       // головний сервер вільний
        PetriP Pmid = new PetriP("Pmid", 0);   // запит оброблено головним сервером
        PetriP Qa = new PetriP("Qa", 0);       // черга до допоміжного сервера
        PetriP Fa = new PetriP("Fa", 1);       // допоміжний сервер вільний
        PetriP Paux = new PetriP("Paux", 0);   // лічильник: запити, що звернулися до допоміжного сервера
        PetriP Pdone = new PetriP("Pdone", 0); // оброблені запити
        for (PetriP p : new PetriP[]{Pgen, Qm, Fm, Pmid, Qa, Fa, Paux, Pdone}) P.add(p);

        PetriT Tgen = new PetriT("Tgen", interArrival); Tgen.setDistribution("exp", interArrival);
        T.add(Tgen);
        in.add(new ArcIn(Pgen, Tgen)); out.add(new ArcOut(Tgen, Pgen, 1)); out.add(new ArcOut(Tgen, Qm, 1));

        PetriT Tm = new PetriT("Tm", mainTime); Tm.setDistribution("exp", mainTime);
        T.add(Tm);
        in.add(new ArcIn(Qm, Tm)); in.add(new ArcIn(Fm, Tm));
        out.add(new ArcOut(Tm, Fm, 1)); out.add(new ArcOut(Tm, Pmid, 1));

        // маршрутизація після головного сервера (однаковий пріоритет 1, ймовірності 0,3 і 0,7)
        PetriT Tto = new PetriT("Tto", 0.0, pAux);
        Tto.setPriority(1);
        T.add(Tto);
        in.add(new ArcIn(Pmid, Tto)); out.add(new ArcOut(Tto, Qa, 1)); out.add(new ArcOut(Tto, Paux, 1));
        PetriT Tout = new PetriT("Tout", 0.0, 1 - pAux);
        Tout.setPriority(1);
        T.add(Tout);
        in.add(new ArcIn(Pmid, Tout)); out.add(new ArcOut(Tout, Pdone, 1));

        PetriT Ta = new PetriT("Ta", auxTime); Ta.setDistribution("exp", auxTime);
        T.add(Ta);
        in.add(new ArcIn(Qa, Ta)); in.add(new ArcIn(Fa, Ta));
        out.add(new ArcOut(Ta, Fa, 1)); out.add(new ArcOut(Ta, Pdone, 1));

        return new PetriNet("Servers", P, T, in, out);
    }

    static double mean(PetriNet net, String name) {
        for (PetriP p : net.getListP()) if (p.getName().equals(name)) return p.getMean();
        for (PetriT t : net.getListT()) if (t.getName().equals(name)) return t.getMean();
        throw new IllegalArgumentException(name);
    }
    static int mark(PetriNet net, String name) {
        for (PetriP p : net.getListP()) if (p.getName().equals(name)) return p.getMark();
        throw new IllegalArgumentException(name);
    }

    /** Один прогін: {завантаження головного, завантаження допоміжного, черга головного, черга допоміжного, час у системі}. */
    public static double[] simulate(double time, double inter, double tMain, double tAux, double pAux) throws Exception {
        PetriSim sim = new PetriSim(buildNet(inter, tMain, tAux, pAux));
        ArrayList<PetriSim> list = new ArrayList<>();
        list.add(sim);
        PetriObjModel model = new PetriObjModel(list);
        model.setIsProtokol(false);
        model.go(time);
        PetriNet net = sim.getNet();
        double L = mean(net, "Qm") + mean(net, "Tm") + mean(net, "Qa") + mean(net, "Ta");
        return new double[]{mean(net, "Tm"), mean(net, "Ta"), mean(net, "Qm"), mean(net, "Qa"), L * inter};
    }

    public static void main(String[] args) throws Exception {
        double time = args.length > 0 ? Double.parseDouble(args[0]) : 100000;
        double inter = args.length > 1 ? Double.parseDouble(args[1]) : 1.0;
        double tMain = args.length > 2 ? Double.parseDouble(args[2]) : 0.6;
        double tAux = args.length > 3 ? Double.parseDouble(args[3]) : 1.0;

        PetriSim sim = new PetriSim(buildNet(inter, tMain, tAux));
        ArrayList<PetriSim> list = new ArrayList<>();
        list.add(sim);
        PetriObjModel model = new PetriObjModel(list);
        model.setIsProtokol(false);
        model.go(time);
        PetriNet net = sim.getNet();

        double lambda = 1.0 / inter;
        double lMain = mean(net, "Qm") + mean(net, "Tm");
        double lAux = mean(net, "Qa") + mean(net, "Ta");
        System.out.println("Час моделювання, с: " + time);
        System.out.printf("Завантаження головного сервера: %.4f%n", mean(net, "Tm"));
        System.out.printf("Завантаження допоміжного сервера: %.4f%n", mean(net, "Ta"));
        System.out.printf("Середня довжина черги до головного: %.4f%n", mean(net, "Qm"));
        System.out.printf("Середня довжина черги до допоміжного: %.4f%n", mean(net, "Qa"));
        System.out.printf("Середній час у головному сервері (з чергою): %.4f с%n", lMain / lambda);
        System.out.printf("Середній час у допоміжному сервері (з чергою, на запит до нього): %.4f с%n", lAux / (lambda * P_AUX));
        System.out.printf("Середній час запиту в системі (Літтл): %.4f с%n", (lMain + lAux) / lambda);
        int done = mark(net, "Pdone"), aux = mark(net, "Paux");
        System.out.printf("Оброблено запитів: %d, частка звернень до допоміжного: %.4f%n", done, (double) aux / (done));
    }
}
