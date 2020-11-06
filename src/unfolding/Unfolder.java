package unfolding;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import petrinet.*;

// 类似反向展开，不过不需要对后集>2的变迁进行拆分
// 关于计算启发值，我们认为正向展开无法预处理每个库所到Pt的启发值，因此选择动态计算（这一点上反向展开具备优势）
public class Unfolder {
    private List<Place> Po;
    private List<Transition> To;
    private List<Place> M0, Mf;

    private List<Place> P;
    private List<Transition> T;
    private Place Ps, Pt;

    private Map<Place, Place> po_p;
    private Map<Transition, Transition> to_t;

    private Map<Transition, Transition> t_to;

    private UnfoldingAlgorithm UnfA;
    private long time;

    public static int FLAG = 3;
    // 1.adequate order 2.dfs 3.hmax 4.hsum

    public final static boolean DEBUG = false;

    public Unfolder(List<Place> Po, List<Transition> To, List<Place> M0, List<Place> Mf, int flag) {
        this.Po = Po;
        this.To = To;
        this.M0 = M0;
        this.Mf = Mf;

        P = new ArrayList<>();
        T = new ArrayList<>();
        po_p = new HashMap<>();
        to_t = new HashMap<>();
        t_to = new HashMap<>();

        FLAG = flag;
    }

    public List<Transition> start(int maxIter, long maxTime) {
        preProcess();
        time = System.currentTimeMillis();
        UnfA = new UnfoldingAlgorithm(Ps, Pt);
        List<Transition> fr = UnfA.start(maxIter, maxTime);
        time = System.currentTimeMillis()-time;
        return postProcess(fr);
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

    // 对输入的1-safe Petri网做以下预处理， 根据M0和Mf添加源点Ps，汇点Pt
    public void preProcess() {
        // 先将节点拷贝一份，建立映射
        for (Place po : Po) {
            Place p = newPlace(po.getId());
            po_p.put(po, p);
        }
        for (Transition to : To) {
            Transition t = newTransition(to.getId());
            to_t.put(to, t);
            t_to.put(t, to);
        }
        // 完全拷贝原来的流关系
        for (Place po : Po) {
            Place p = po_p.get(po);
            for (Transition to : po.getPostSet()) {
                Transition t = to_t.get(to);
                addEdge(p, t);
            }
        }
        for (Transition to : To) {
            Transition t = to_t.get(to);
            for (Place po : to.getPostSet()) {
                Place p = po_p.get(po);
                addEdge(t, p);
            }
        }

        // 接下来开始预处理，添加Ps,Pt
        int nodeId = Po.size()+To.size()+1; // 辅助节点id从nodeId开始递增
        // 添加Ps
        Ps = newPlace(nodeId++);
        Transition Ts = newTransition(nodeId++);
        addEdge(Ps, Ts);
        for (Place po : M0) {
            Place p = po_p.get(po);
            addEdge(Ts, p);
        }
        // 添加Pt
        Transition Tt = newTransition(nodeId++);
        for (Place po : Mf) {
            Place p = po_p.get(po);
            addEdge(p, Tt);
        }
        Pt = newPlace(nodeId++);
        addEdge(Tt, Pt);
    }

    public List<Transition> postProcess(List<Transition> fr) {
        if (fr.size() == 0) return null;
        List<Transition> fr_o = new ArrayList<> ();
        for (Transition t : fr) {
            if (t_to.containsKey(t)) {
                fr_o.add(t_to.get(t));
            }
        }
        return fr_o;
    }

    // 打印调试信息
    public int getPOsz() {
        return Po.size();
    }

    public int getTOsz() {
        return To.size();
    }

    public int getPsz() {
        return P.size();
    }

    public int getTsz() {
        return T.size();
    }

    public int getCsSize() {
        return UnfA.getCsSize();
    }

    public int getEsSize() {
        return UnfA.getEsSize();
    }

    public int getRExtSize() {
        return UnfA.getExtSize();
    }

    public int getCutoffNumber() {
        return UnfA.getCutoffNumber();
    }

    public long getTime() {
        return time;
    }

}
