package main;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import petrinet.*;
import sun.awt.image.IntegerComponentRaster;
import sun.management.counter.perf.PerfLongArrayCounter;

// 读取册时用例，生成Petri网
// 格式：
// 第1行     ：|P| |T|：库所数量和变迁数量，库所和变迁的id都从1开始
// 第2行     ：m1     ：P->T的边数
// 接下来m1行 ：p t    ：库所p到变迁t连一条有向边
// 3+m1行    ：m2     ：T->P的边数
// 接下来m2行 ：t p    ：变迁t到库所p连一条有向边
// 4+m1+m2行 ：M0     ：若干个空格分离的库所，如1 10 31 78...，代表Petri网的初始标识
// 5+m1+m2行 ：Mfs    ：实验中一个网可能需要验证多个Mf的可覆盖性，Mfs代表需要验证的Mf的数量
// 接下来Mfs行：Mf     ：格式同M0
public class PetriNetReader {
    private String filename;
    private List<Place> P;
    private List<Transition> T;
    private List<Place> M0;
    private List<List<Place>> Mfs; // 一个测试用例中可能有多个需要验证的Mf

    public PetriNetReader(String filename) {
        this.filename = filename;
        P = new ArrayList<>();
        T = new ArrayList<>();
        M0 = new ArrayList<>();
        Mfs = new ArrayList<>();
    }

    public void start() throws IOException {
        FileInputStream fileInputStream = new FileInputStream(filename);
        InputStreamReader isr = new InputStreamReader(fileInputStream, "UTF-8");
        BufferedReader reader = new BufferedReader(isr, 1024);

        String[] pair = reader.readLine().split(" ");
        // |P| |T|
        int Psz = Integer.parseInt(pair[0]);
        int Tsz = Integer.parseInt(pair[1]);
        for (int i = 1; i <= Psz; i++) {
            newPlace(i);
        }
        for (int i = 1; i <= Tsz; i++) {
            newTransition(i);
        }
        // p->t
        int n = Integer.parseInt(reader.readLine());
        for (int i = 1; i <= n; i++) {
            pair = reader.readLine().split(" ");
            int p = Integer.parseInt(pair[0]);
            int t = Integer.parseInt(pair[1]);
            addEdge(P.get(p-1), T.get(t-1));
        }
        // t->p
        n = Integer.parseInt(reader.readLine());
        for (int i = 1; i <= n; i++) {
            pair = reader.readLine().split(" ");
            int t = Integer.parseInt(pair[0]);
            int p = Integer.parseInt(pair[1]);
            addEdge(T.get(t-1), P.get(p-1));
        }
        // M0
        String[] m0 = reader.readLine().split(" ");
        for (int i = 0; i < m0.length; i++) {
            M0.add(P.get(Integer.parseInt(m0[i]) -1));
        }
        // Mfs
        n = Integer.parseInt(reader.readLine());
        for (int i = 1; i <= n; i++) {
            String[] mf = reader.readLine().split(" ");
            List<Place> Mf = new ArrayList<>();
            for (int j = 0; j < mf.length; j++) {
                Mf.add(P.get(Integer.parseInt(mf[j])-1));
            }
            Mfs.add(Mf);
        }

        reader.close();
        isr.close();
        fileInputStream.close();
    }

    public Place newPlace(int id) {
        Place p = new Place(id);
        P.add(p);
        return p;
    }

    public Transition newTransition(int id) {
        Transition t = new Transition(id);
        T.add(t);
        return t;
    }

    // p->t
    public static void addEdge(Place p, Transition t) {
        p.addPostSet(t);
        t.addPreSet(p);
    }

    // t->p
    public static void addEdge(Transition t, Place p) {
        t.addPostSet(p);
        p.addPreSet(t);
    }
    public List<Place> getP() { return P; }

    public List<Transition> getT() { return T; }

    public List<Place> getM0() { return M0; }

    public List<List<Place>> getMfs() { return Mfs; }
}
