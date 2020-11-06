package reverseunfolding;

import java.lang.reflect.Array;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;

import petrinet.*;
import sun.management.counter.perf.PerfLongArrayCounter;

/***************************************************
 * 反向展开生成器
 * 描述：对输入的1-safe Petri网进行预处理，调用反向展开算法，处理算法结果并输出
 * 1. 对输入的1-safe Petri网(需要用户输入P,T,M0,Mf)做以下预处理：
 *  （1）根据M0和Mf添加源点Ps，汇点Pt
 *  （2）对后集>2的变迁进行拆分，保证每个变迁的后集大小小于等于2
 *  （3）计算每个节点的启发值
 * 2. 调用反向展开算法ReverseUnfoldingAlgorihtm
 * 3. 反向展开算法会返回一个触发序列，将这个序列映射回原网后输出,用户可通过这个触发序列验证覆盖路径的正确性
 * 这里触发序列的返回值和ReverseUnfoldingAlgorithm（简称RUnfA）不同，RUnfA由于添加了Ps和Pt触发序列一定不为空，所以可以通过判空检查可覆盖性
 * 而原网上的触发序列可能为空，即M0直接覆盖Mf的情况，所以这里需要通过null表示不可覆盖
 ***************************************************/
public class ReverseUnfolder {
    private List<Place> Po; // 原网中的库所
    private List<Transition> To; // 原网中的变迁
    private List<Place> M0, Mf; // 原网中的M0，Mf

    private List<Place> P; // 改造后的库所集
    private List<Transition> T; // 改造后的变迁集
    private Place Ps, Pt; // 改造后的源点，汇点
    // 改造前->改造后
    private Map<Place, Place> po_p;
    private Map<Transition, Transition> to_t;
    // 改造后->改造前
    private Map<Transition, Transition> t_to;

    private ReverseUnfoldingAlgorithm RUnfA; // 反向展开算法，设置为成员变量主要为了方便外部打印调试信息
    private long time; // 算法运行时间

    public static int FLAG = 1; // 启发式策略
    // 1.adequate order 2.dfs 3.block+dfs 4.hmax 5.hsum 6.block+hsum

    // 用户输入P,T,M0,Mf
    public final static boolean DEBUG = false; // 调试开关，设为true会显示各种调试信息

    public ReverseUnfolder(List<Place> Po, List<Transition> To, List<Place> M0, List<Place> Mf, int flag) {
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

    // 运行反向展开构造器，若Mf可覆盖则返回一个触发序列，否则返回一个空序列
    public List<Transition> start(int maxIter, long maxTime) {
        preProcess(); // 预处理
        // 调用反向展开算法
        time = System.currentTimeMillis();
        RUnfA = new ReverseUnfoldingAlgorithm(Ps, Pt);
        List<Transition> fr = RUnfA.start(maxIter, maxTime); // 设置最大扩展次数为1w步
        time = System.currentTimeMillis()-time;
        // 处理结果并返回
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

    // 对输入的1-safe Petri网做以下预处理：
    // 1.根据M0和Mf添加源点Ps，汇点Pt
    // 2.对后集>2的变迁进行拆分，保证每个变迁的后集大小小于等于2
    // 3.计算每个节点的启发值
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
        // 除了后集>2的变迁都拷贝原来的流关系
        for (Place po : Po) {
            Place p = po_p.get(po);
            for (Transition to : po.getPostSet()) {
                Transition t = to_t.get(to);
                addEdge(p, t);
            }
        }
        for (Transition to : To) {
            if (to.getPostSet().size() > 2) {
                continue;
            }
            Transition t = to_t.get(to);
            for (Place po : to.getPostSet()) {
                Place p = po_p.get(po);
                addEdge(t, p);
            }
        }

        // 接下来开始预处理，添加辅助节点
        int nodeId = Po.size()+To.size()+1; // 辅助节点id从nodeId开始递增
        // 拆分规则：
        //   x         x
        // x x x  -> x   x
        //              x x
        // 对后集>2的变迁进行拆分
        for (Transition to : To) {
            if (to.getPostSet().size() <= 2) continue;
            Transition preT = to_t.get(to);
            List<Place> postSet = to.getPostSet();
            for (int i = 0; i < postSet.size()-1; i++) {
                if (i != postSet.size()-2) {
                    Place p1 = po_p.get(postSet.get(i));
                    Place p2 = newPlace(nodeId++);
                    addEdge(preT, p1);
                    addEdge(preT, p2);
                    preT = newTransition(nodeId++);
                    addEdge(p2, preT);
                } else {
                    Place p1 = po_p.get(postSet.get(i));
                    Place p2 = po_p.get(postSet.get(i+1));
                    addEdge(preT, p1);
                    addEdge(preT, p2);
                }
            }
        }

        // 添加Ps
        Ps = newPlace(nodeId++);
        Place preP = Ps;
        if (M0.size() == 1) {
            Transition t = newTransition(nodeId++);
            addEdge(preP, t);
            addEdge(t, po_p.get(M0.get(0)));
        } else {
            for (int i = 0; i < M0.size()-1; i++) {
                Transition t = newTransition(nodeId++);
                addEdge(preP, t);
                if (i != M0.size()-2) {
                    Place p1 = po_p.get(M0.get(i));
                    Place p2 = newPlace(nodeId++);
                    addEdge(t, p1);
                    addEdge(t, p2);
                    preP = p2;
                } else {
                    Place p1 = po_p.get(M0.get(i));
                    Place p2 = po_p.get(M0.get(i+1));
                    addEdge(t, p1);
                    addEdge(t, p2);
                }
            }
        }
        // 添加Pt
        Transition Tt = newTransition(nodeId++);
        for (Place po : Mf) {
            Place p = po_p.get(po);
            addEdge(p, Tt);
        }
        Pt = newPlace(nodeId++);
        addEdge(Tt, Pt);

        // 计算启发值
        if (FLAG == 4) {
            // hmax
            Ps.setH(0);
            Queue<Place> queue = new LinkedList<> ();
            queue.offer(Ps);
            Map<Transition, Integer> map = new HashMap<>(); // 因为变迁要计算最大值，所以要类似拓扑排序那样等带所有前集reach
            for (Transition t : T) {
                map.put(t, t.getPreSet().size()); // 减为0才能触发
            }
            Set<Place> set = new HashSet<>();
            set.add(Ps);
            while (!queue.isEmpty()) {
                Place p = queue.poll();
                for (Transition t : p.getPostSet()) {
                    int temp = map.get(t);
                    map.put(t, temp-1);
                    if (t.getH() == Integer.MAX_VALUE) {
                        // 之前未设置
                        t.setH(p.getH());
                    } else {
                        // 取最大
                        t.setH(Integer.max(t.getH(), p.getH()));
                    }
                    // 减为0才能触发
                    if (temp-1 == 0) {
                        for (Place p1 : t.getPostSet()) {
                            if (set.contains(p1)) continue;
                            set.add(p1);
                            p1.setH(t.getH()+1);
                            queue.offer(p1);
                        }
                    }
                }
            }
        } else if (FLAG == 5 || FLAG == 6) {
            // hsum
            Ps.setH(0);
            Queue<Place> queue = new LinkedList<> ();
            queue.offer(Ps);
            Map<Transition, Integer> map = new HashMap<>(); // 因为变迁要计算总和，所以要类似拓扑排序那样等带所有前集reach
            for (Transition t : T) {
                map.put(t, t.getPreSet().size()); // 减为0才能触发
            }
            Set<Place> set = new HashSet<>();
            set.add(Ps);
            while (!queue.isEmpty()) {
                Place p = queue.poll();
                for (Transition t : p.getPostSet()) {
                    int temp = map.get(t);
                    map.put(t, temp-1);
                    if (t.getH() == Integer.MAX_VALUE) {
                        // 之前未设置
                        t.setH(p.getH());
                    } else {
                        // 累加
                        t.setH(t.getH()+p.getH());
                    }
                    // 减为0才能触发
                    if (temp-1 == 0) {
                        for (Place p1 : t.getPostSet()) {
                            if (set.contains(p1)) continue;
                            set.add(p1);
                            p1.setH(t.getH()+1);
                            queue.offer(p1);
                        }
                    }
                }
            }
        }

    }

    // 通过tMap将反向展开输出的触发序列映射到原网
    // 因为preProcess中添加的辅助节点并没有保存在tMap中，因此只要做简单的映射就能实现转换操作（要求preProcess在转换时仔细考虑变迁的触发顺序）
    // 这里不可覆盖时应返回null而不是空集，原因在最上面有解释
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
    // 获取原网库所数量
    public int getPOsz() {
        return Po.size();
    }

    // 获取原网变迁数量
    public int getTOsz() {
        return To.size();
    }

    // 获取改造后的库所数量
    public int getPsz() {
        return P.size();
    }

    // 获取改造后的变迁数量
    public int getTsz() {
        return T.size();
    }

    public int getCsSize() {
        return RUnfA.getCsSize();
    }

    public int getEsSize() {
        return RUnfA.getEsSize();
    }

    public int getRExtSize() {
        return RUnfA.getRExtSize();
    }

    public int getCutoffNumber() {
        return RUnfA.getCutoffNumber();
    }

    public int getRedundantNumber() {
        return RUnfA.getRedundantNumber();
    }

    public long getTime() {
        return time;
    }

}
