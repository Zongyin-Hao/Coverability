package unfolding;

import java.util.*;

import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import petrinet.*;
import unfolding.occurrentnet.*;

// 正向展开算法，思路源自directed unfolding和Recent advances in unfolding technology
// 和反向展开类似用到了扩展顺序与截断偏序分离的思想，没有使用semi-adequated order
// 启发式技术用到了hmax，hsum，hff以及最基础的adequated order（也就是单纯的bfs）
// 主要用来和反向展开做对比，很多技术和思想和反向展开相似，相同点我们就不再详细介绍了，只介绍不同点。（推荐先阅读反向展开代码）
// 输入，输出和反向展开相同， 不过不需要“每个变迁的后集大小小于等于2”这个条件
public class UnfoldingAlgorithm {
    private Place Ps, Pt;
    private List<Condition> C;
    private List<Event> E;
    private int extId = 0;
    private PriorityQueue<Event> Ext;
    private int cutoffNumber;

    private Condition Cs; // 方便打印解

    public UnfoldingAlgorithm(Place ps, Place pt) {
        Ps = ps;
        Pt = pt;
        C = new ArrayList<> ();
        E = new ArrayList<> ();
        extId = 0;
        Ext = new PriorityQueue<> ();
        cutoffNumber = 0;
    }

    public List<Transition> start(int maxIter, long maxTime) {
        initialize();
        int iter = 0;
        List<Transition> result = new ArrayList<> ();
        long startTime = System.currentTimeMillis();
        while (!Ext.isEmpty() && iter < maxIter && System.currentTimeMillis()-startTime <= maxTime) {
            if (!Unfolder.DEBUG) {
                if (iter != 0) {
                    for (int i = 0; i < 17; i++) System.out.print("\b");
                }
                System.out.printf("[%6d - %6d]", (iter+1), maxIter);
            }
            if (Unfolder.DEBUG) {
                System.out.println("[Unfolder] --------------------Iter"+iter+"--------------------");
            }
            Event ext = Ext.poll();
            if (isCutoff(ext)) {
                if (Unfolder.DEBUG) {
                    System.out.println("[Unfolder] "+ext+" cut off!");
                }
                cutoffNumber++;
                continue;
            }
            Event e = ext;
            e.setId(E.size());
            E.add(e);
            for (Condition c : e.getPreSet()) {
                c.addPostSet(e);
            }
            if (Unfolder.DEBUG) {
                System.out.println("[Unfolder] Add "+e+" to Unf");
            }
            for (Place p : e.getMap().getPostSet()) {
                Condition c = new Condition(p, C.size());
                C.add(c);
                e.addPostSet(c);
                c.setPreSet(e);
                if (Unfolder.DEBUG) {
                    System.out.println("[Unfolder] Add "+c+" to Unf");
                }
            }
            if (e.isCoverable()) {
                if (Unfolder.DEBUG) {
                    System.out.println("[Unfolder] Coverable!");
                }
                result = getFiringSequence(e);
                break;
            }
            for (Condition c : e.getPostSet()) {
                updateConcurrentSet(c);
                updateExt(c);
            }
            iter++;
        }
        if (!Unfolder.DEBUG) {
            System.out.println();
        }
        return result;
    }

    public void initialize() {
        Condition c = new Condition(Ps, C.size());
        C.add(c);
        Cs = c;
        Event ext = new Event(Ps.getPostSet().get(0), extId++);
        ext.addPreSet(c);
        List<Event> cfg = new ArrayList<> ();
        cfg.add(ext);
        Map<Place, Integer> mark = new HashMap<> ();
        for (Place p : ext.getMap().getPostSet()) {
            mark.put(p, 1);
        }
        List<Transition> lex = new ArrayList<> ();
        lex.add(ext.getMap());
        ext.setConfiguration(cfg);
        ext.setMark(mark);
        ext.setLexOrder(lex);
        Ext.offer(ext);
    }

    public void updateConcurrentSet(Condition c0) {
        Event e = c0.getPreSet();
        if (e != null) {
            List<Condition> preSet = e.getPreSet();
            Set<Condition> concurrentSet = new HashSet<> ();
            concurrentSet.addAll(preSet.get(0).getConcurrentSet());
            for (int i = 1; i < preSet.size(); i++) {
                concurrentSet.retainAll(preSet.get(i).getConcurrentSet());
            }
            for (Condition c : e.getPostSet()) {
                if (!c.equals(c0)) {
                    concurrentSet.add(c);
                }
            }
            c0.setConcurrentSet(concurrentSet);
            for (Condition c : concurrentSet) {
                c.addConcurrentSet(c0);
            }
        }
    }

    public void updateExt(Condition c) {
        Set<Condition> concurrentSet = c.getConcurrentSet();
        for (Transition t : c.getMap().getPostSet()) {
            // 我们要找<t, {c, ...}>这样的扩展，{c, ...}是一个coset，能满足t的前驱库所
            // 既然和t的前驱库所有关，那我们拿出c的concurrentSet，将其中的条件按照t的前驱库所分类
            // 这样以来搜索策略就比较好理解了，举个例子：
            // t的前驱库所为{p1, p2, p3, p4}, c对应了p1，现在还剩p2, p3, p4需要有条件来对应
            // c的concurrentSet为{c1:p1, c2:p2, c3:p2, c4:p3, c5:p4, c6:p4, c7:p4}
            // 那么分类后长这个样子：
            // p2: c2, c3
            // p3: c4
            // p4: c5, c6, c7
            // 这样就可以dfs了，搜出所有两两并发的条件集（coset），使其满足{p2, p3, p4}，从而进行扩展

            // 因为分类用到了数组，所以先把Place映射到数组下标
            Map<Integer, Integer> map = new HashMap<> ();
            int count = 0;
            for (Place p : t.getPreSet()) {
                if (c.getMap().getId() == p.getId()) {
                    continue;
                }
                map.put(p.getId(), count++);
            }
            // 将concurrentSet中的条件分类
            List<List<Condition>> list = new ArrayList<> ();
            for (int i = 0; i < count; i++) {
                list.add(new ArrayList<Condition> ());
            }
            for (Condition c1 : concurrentSet) {
                int id = c1.getMap().getId();
                if (!map.containsKey(id)) {
                    continue;
                }
                list.get(map.get(id)).add(c1);
            }
            // dfs
            // 层数， c和t用来构造扩展， list是整个dfs的基础，最后新建了一个临时数组负责在搜索过程中保存当前搜到的coset
            updateExt_dfs(count-1, c, t, list, new ArrayList<Condition>());
        }
    }

    // dfs
    public void updateExt_dfs(int d, Condition c, Transition t, List<List<Condition>> list, List<Condition> res) {
        if (d < 0) {
            Event ext = new Event(t, extId++);
            ext.addPreSet(c);
            for (Condition c1 : res) {
                ext.addPreSet(c1);
            }
            calculate_ext(ext);
            Ext.offer(ext);
            if (Unfolder.DEBUG) {
                System.out.println("[Unfolder] -- Add "+ext+" to Ext");
            }
            return;
        }
        for (Condition c1 : list.get(d)) {
            boolean ok = true;
            // 判断是否和当前res中的条件两两并发
            for (Condition c2 : res) {
                if (!c1.isConcurrentWith(c2)) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                res.add(c1);
                updateExt_dfs(d-1, c, t, list, res);
                res.remove(res.size() - 1); // 别忘了回溯时处理一下
            }
        }
    }

    public void calculate_ext(Event ext) {
        Set<Event> unionSet = new HashSet<> ();
        unionSet.add(ext);
        for (Condition c : ext.getPreSet()) {
            Event e = c.getPreSet();
            if (e == null) continue;
            unionSet.addAll(e.getConfiguration());
        }
        List<Event> cfg = new ArrayList<> ();
        cfg.addAll(unionSet);
        Set<Condition> preSet = new HashSet<> ();
        Set<Condition> postSet = new HashSet<> ();
        for (Event e : cfg) {
            preSet.addAll(e.getPreSet());
            postSet.addAll(e.getPostSet());
        }
        postSet.removeAll(preSet);
        Map<Place, Integer> mark = new HashMap<> ();
        for (Condition c : postSet) {
            Place p = c.getMap();
            if (mark.containsKey(p)) {
                mark.put(p, mark.get(p)+1);
            }
            else {
                mark.put(p, 1);
            }
        }
        for (Place p : ext.getMap().getPostSet()) {
            if (mark.containsKey(p)) {
                mark.put(p, mark.get(p)+1);
            }
            else {
                mark.put(p, 1);
            }
        }
        List<Transition> lex = new ArrayList<> ();
        for (Event e : cfg) {
            lex.add(e.getMap());
        }
        Collections.sort(lex, new Comparator<Transition> () {
            @Override
            public int compare(Transition t1, Transition t2) {
                return t1.getId() - t2.getId();
            }
        });
        ext.setConfiguration(cfg);
        ext.setMark(mark);
        ext.setLexOrder(lex);
        calculate_f(ext);
        calculate_coverability(ext);
    }

    // 正向展开中需要动态计算扩展的启发值
    public void calculate_f(Event ext) {
        if (Unfolder.FLAG == 3) {
            // hmax
            Pt.setH(Integer.MAX_VALUE);
            Queue<Place> queue = new LinkedList<> ();
            for (Map.Entry<Place, Integer> entry : ext.getMark().entrySet()) {
                Place p = entry.getKey();
                p.setH(0);
                queue.offer(p);
            }
            Map<Transition, Integer> map = new HashMap<>();
            // 反向展开在这个地方将所有变迁的值都设为了0，其实有更好的做法
            // 这里我们只将遇到的变迁加入map并累加，通过判断value是否达到t.getPreSet().size()决定t是否被触发
            // 这样一是省空间，二是可以方便动态初始化

            Set<Place> set = new HashSet<>();
            for (Map.Entry<Place, Integer> entry : ext.getMark().entrySet()) {
                Place p = entry.getKey();
                set.add(p);
            }

            while (!queue.isEmpty()) {
                Place p = queue.poll();
                for (Transition t : p.getPostSet()) {
                    if (!map.containsKey(t)) {
                        map.put(t, 1);
                        t.setH(p.getH());
                    } else {
                        map.put(t, map.get(t)+1);
                        t.setH(Integer.max(t.getH(), p.getH()));
                    }
                    // 达到t.getPreSet().size()才能触发
                    if (map.get(t) == t.getPreSet().size()) {
                        for (Place p1 : t.getPostSet()) {
                            if (set.contains(p1)) continue;
                            set.add(p1);
                            p1.setH(t.getH()+1);
                            queue.offer(p1);
                        }
                    }
                }
            }
            ext.setF(ext.getConfiguration().size() + Pt.getH());
        } else if (Unfolder.FLAG == 4) {
            // hsum
            Pt.setH(Integer.MAX_VALUE);
            Queue<Place> queue = new LinkedList<> ();
            for (Map.Entry<Place, Integer> entry : ext.getMark().entrySet()) {
                Place p = entry.getKey();
                p.setH(0);
                queue.offer(p);
            }
            Map<Transition, Integer> map = new HashMap<>();
            // 反向展开在这个地方将所有变迁的值都设为了0，其实有更好的做法
            // 这里我们只将遇到的变迁加入map并累加，通过判断value是否达到t.getPreSet().size()决定t是否被触发
            // 这样一是省空间，二是可以方便动态初始化

            Set<Place> set = new HashSet<>();
            for (Map.Entry<Place, Integer> entry : ext.getMark().entrySet()) {
                Place p = entry.getKey();
                set.add(p);
            }

            while (!queue.isEmpty()) {
                Place p = queue.poll();
                for (Transition t : p.getPostSet()) {
                    if (!map.containsKey(t)) {
                        map.put(t, 1);
                        t.setH(p.getH());
                    } else {
                        map.put(t, map.get(t)+1);
                        t.setH(t.getH()+p.getH());
                    }
                    // 达到t.getPreSet().size()才能触发
                    if (map.get(t) == t.getPreSet().size()) {
                        for (Place p1 : t.getPostSet()) {
                            if (set.contains(p1)) continue;
                            set.add(p1);
                            p1.setH(t.getH()+1);
                            queue.offer(p1);
                        }
                    }
                }
            }
            ext.setF(ext.getConfiguration().size() + Pt.getH());
        } else {
            // 启用Event中的默认排序方法
        }
    }

    // 通过判断Mark([e0])是否包含Pt验证可覆盖性
    public void calculate_coverability(Event ext) {
        Map<Place, Integer> mark = ext.getMark();
        if (mark.containsKey(Pt)) {
            ext.setCoverable();
        }
    }


    // 截断事件判断
    public boolean isCutoff(Event ext) {
        for (Event e : E) {
            if (e.equals(ext)) continue;
            if (e.isMarkEqualWith(ext) && e.isAdeSmallerThan(ext)) {
                return true;
            }
        }
        return false;
    }

    // 通过Cs获取Es，类似反向展开那样进行拓扑排序
    // 这里和反向展开略有不同，正向展开可能会搜”歪“，因此要把搜索限制在e0的配置中
    public List<Transition> getFiringSequence(Event e0) {
        Event es = Cs.getPostSet().get(0);
        Set<Event> cfg = new HashSet<>(0);
        cfg.addAll(e0.getConfiguration());

        List<Transition> fs = new ArrayList<> ();
        Queue<Event> queue = new LinkedList<> ();
        if (cfg.contains(es)) queue.offer(es);
        Set<Condition> M = new HashSet<> ();
        while (!queue.isEmpty()) {
            Event e = queue.poll();
            fs.add(e.getMap());
            for (Condition c : e.getPostSet()) {
                M.add(c);
            }
            for (Condition c : e.getPostSet()) {
                for (Event e1 : c.getPostSet()) {
                    if (e1 == null || !cfg.contains(e1)) continue;

                    boolean fireable = true;
                    for (Condition c1 : e1.getPreSet()) {
                        if (!M.contains(c1)) {
                            fireable = false;
                            break;
                        }
                    }
                    if (!fireable) continue;
                    for (Condition c1 : e1.getPreSet()) {
                        M.remove(c1);
                    }
                    queue.offer(e1);
                }
            }
        }
        return fs;
    }

    public int getCsSize() {
        return C.size();
    }

    public int getEsSize() {
        return E.size();
    }

    public int getExtSize() {
        return Ext.size();
    }

    public int getCutoffNumber() {
        return cutoffNumber;
    }


}