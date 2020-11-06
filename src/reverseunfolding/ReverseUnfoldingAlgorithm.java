package reverseunfolding;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Queue;
import java.util.LinkedList;
import java.util.PriorityQueue;


import petrinet.*;
import reverseunfolding.roccurrentnet.Condition;
import reverseunfolding.roccurrentnet.Event;

/***************************************************
 * 反向展开算法
 * 描述：在1-safe Petri网上验证目标标识的可覆盖性
 * 输入：Ps、Pt,分别表示1-safe Petri网和目标标识：
 * ----Ps: 源库所，连接M0，方便可覆盖性判断（只要判断Mark([e])=={Ps}即可）
 * ----Pt: 汇库所，连接Mf，便于代码处理
 * 输出：若目标标识可覆盖输出一条触发序列，否则输出空序列
 * 特殊说明：1-safe Petri网需要特殊处理一下，保证每个变迁的后集大小小于等于2
 ***************************************************/
public class ReverseUnfoldingAlgorithm {
	private Place Ps, Pt; // 源汇库所
	private List<Condition> C; // 条件集合
	private List<Event> E; // 事件集合
	private int rextId; // 维护全局递增的扩展id
	private PriorityQueue<Event> RExt; // 扩展队列，按照启发式函数进行排序，具体参考Event类
	//   这里需要说明一下，扩展其实是一种特殊的事件，与事件具备相同的信息（除了事件id，作为代替有一个扩展id。其余的比如配置，mark什么的都有），
	// 可以理解为一个没有实际加入RUnf的候选事件。当一个扩展从RExt中被选中弹出时，我们会赋予它一个事件id并加入RUnf，此时扩展就变成了一个实际的事件
	//   再具体一点，一个扩展只把RUnf中的一个coset设为了postSet，而这个coset并没有把这个扩展设为preSet，从反向展开的角度看这个扩展并没有加入
	// RUnf，反向遍历也遍历不到。只有当这个扩展从RExt中被选中弹出时才会设置其对应coset的preSet，将其实际加入RUnf
	//   这样概念上好区分，也好写，而且RUnf中的事件id都是连续的也好调试
	private Map<String, Event> extensionHash; // 将通过字符串"t#c"获取RExt中的扩展<t, {c}>，具体用途参考updateExtension函数
	private int cutoffNumber; // 截断事件数量，调试用
	private int redundantNumber; // 减少的冗余扩展数量

	// 构造函数
	// Ps、Pt,分别表示1- safe Petri网和目标标识：
	// ----Ps: 源库所，连接M0，方便可覆盖性判断（只要判断Mark([e])=={Ps}即可）
	// ----Pt: 汇库所，连接Mf，便于代码处理
	public ReverseUnfoldingAlgorithm(Place ps, Place pt) {
		Ps = ps;
		Pt = pt;
		C = new ArrayList<> ();
		E = new ArrayList<> ();
		rextId = 0;
		RExt = new PriorityQueue<> ();
		extensionHash = new HashMap<> ();
		cutoffNumber = 0;
		redundantNumber = 0;
	}

	// 算法入口
	// 若目标标识可覆盖输出一条触发序列，否则输出空序列
	// 参数maxIter用来限制算法的扩展次数（实际扩展次数，截断不算）
	public List<Transition> start(int maxIter, long maxTime) {
		// 初始化C，E，RExt
		initialize();
		int iter = 0;
		List<Transition> result = new ArrayList<> (); // 触发序列
		long startTime = System.currentTimeMillis();
		// 逐步扩展
		while (!RExt.isEmpty() && iter < maxIter && System.currentTimeMillis()-startTime <= maxTime) {
			if (!ReverseUnfolder.DEBUG) {
				// 非DEBUG模式下显示进度条，以便告诉用户当前进度
				if (iter != 0) {
					for (int i = 0; i < 17; i++) System.out.print("\b");
				}
				System.out.printf("[%6d - %6d]", (iter+1), maxIter);
			}
			if (ReverseUnfolder.DEBUG) {
				System.out.println("[ReverseUnfolder] --------------------Iter"+iter+"--------------------");
			}
			Event rext = RExt.poll();
			// 截断事件判断
			if (isCutoff(rext)) {
				if (ReverseUnfolder.DEBUG) {
					System.out.println("[ReverseUnfolder] "+rext+" cut off!");
				}
				cutoffNumber++;
				continue; // 注意这里continue不计入iter
			}
			// 现在可以赋予事件id，实际加入RUnf了
			Event e = rext;
			e.setId(E.size());
			E.add(e);
			for (Condition c : e.getPostSet()) {
				c.addPreSet(e);
			}
			if (ReverseUnfolder.DEBUG) {
				System.out.println("[ReverseUnfolder] Add "+e+" to RUnf");
			}
			// 设e对应的变迁为t，这一步是对t前集中的每个库所p，在RUnf中建一个对应的条件c
			for (Place p : e.getMap().getPreSet()) {
				Condition c = new Condition(p, C.size());
				C.add(c);
				e.addPreSet(c);
				c.setPostSet(e);
				if (ReverseUnfolder.DEBUG) {
					System.out.println("[ReverseUnfolder] Add "+c+" to RUnf");
				}
			}
			// 可覆盖性判断
			if (e.isCoverable()) {
				if (ReverseUnfolder.DEBUG) {
					System.out.println("[ReverseUnfolder] Coverable!");
				}
				result = getFiringSequence(e);
				break;
			}
			// 遍历刚刚添加的条件，以每个条件为基准更新扩展集
			for (Condition c : e.getPreSet()) {
				// 更新concurrentSet，concurrentSet的具体定义参考Condition类（主要用来辅助计算coset）
				// 由于我们一边遍历一边更新concurrentSet，而不是一次性更新，因此以每个条件为基准更新扩展集时不会有重复扩展（不好理解的话
				// 可以继续阅读我们的concurrentSet更新方案和扩展方案）
				updateConcurrentSet(c);
				// 更新扩展集
				updateRExt(c);
			}
			iter++;
		}
		if (!ReverseUnfolder.DEBUG) {
			// 配合上面的进度
			System.out.println();
		}
		return result;
	}

	// 按照Pt初始化C，E，RExt
	public void initialize() {
		// 第一个条件
		Condition c = new Condition(Pt, C.size());
		C.add(c);
		// 第一个扩展
		Event rext = new Event(Pt.getPreSet().get(0), rextId++);
		rext.addPostSet(c);
		// 计算一下这个扩展的配置，mark，字典序等信息
		// Cfg
		List<Event> cfg = new ArrayList<> ();
		cfg.add(rext);
		// Mark
		Map<Place, Integer> mark = new HashMap<> ();
		for (Place p : rext.getMap().getPreSet()) {
			mark.put(p, 1);
		}
		// Lex
		List<Transition> lex = new ArrayList<> ();
		lex.add(rext.getMap());

		rext.setConfiguration(cfg);
		rext.setMark(mark);
		rext.setLexOrder(lex);
		RExt.offer(rext);
	}

	// 更新c0以及C中相关条件的concurrentSet
	// 一交一并
	public void updateConcurrentSet(Condition c0) {
		Event e = c0.getPostSet();
		if (e != null) {
			List<Condition> postSet = e.getPostSet();
			// 一交：计算后集条件concurrentSet的交集
			Set<Condition> concurrentSet = new HashSet<> ();
			concurrentSet.addAll(postSet.get(0).getConcurrentSet());
			for (int i = 1; i < postSet.size(); i++) {
				concurrentSet.retainAll(postSet.get(i).getConcurrentSet());
			}
			// 一并：把同层条件加入到c0的concurrentSet中
			for (Condition c : e.getPreSet()) {
				if (!c.equals(c0)) {
					concurrentSet.add(c);
				}
			}
			// 双向更新
			c0.setConcurrentSet(concurrentSet);
			for (Condition c : concurrentSet) {
				c.addConcurrentSet(c0);
			}
		}
	}

	// 算法的核心：以c0为基础更新扩展集
	// 由于输入的1-safe Petri网中任意变迁最多只有两个后继库所，更新扩展集变得十分简单，都不需要搜索
	public void updateRExt(Condition c0) {
		Set<Condition> concurrentSet = c0.getConcurrentSet();
		for (Transition t : c0.getMap().getPreSet()) {
			// 现在要把<t, c0>以及形如<t, {c0, cx}>的变迁加入扩展集
			// 根据扩展的定义，若有扩展rext=<t, {c0, cx}>，则应舍弃扩展rext1：rext1={t, {c}}, c=c0|c1, Mark([rext])<=Mark([rext])
			// 这样做是否会破坏完备性我们没有严格证明，但这个舍弃操作在直观上很容易解释，且我们在人工验证和实验验证时也没找到反例，因此我们最终还是采用了这个方案
			// 事实上，Parosh的反向展开没有加Mark([rext])<=Mark([rext])这个限制，遗憾的是这样做展开的完备性会被破坏（我们在论文中指出了其反例）

			{ // 新建一个作用域，防止和下面重名
				// 添加扩展<t, c0>（可能会被后续操作删除）
				Event rext = new Event(t, rextId++);
				rext.addPostSet(c0);
				calculate_rext(rext);
				if (ReverseUnfolder.DEBUG) {
					System.out.println("[ReverseUnfolder] -- Add "+rext+" to RExt");
				}
				// 在1-safe net中任意一个标识中的库所数量不得大于1，这里添加一个剪枝
				boolean ok = true;
				for (Map.Entry<Place, Integer> entry : rext.getMark().entrySet()) {
					if (entry.getValue() > 1) {
						ok = false;
						break;
					}
				}
				if (ok) {
					extensionHash.put(t.getId()+"#"+ c0.getId(), rext); // 保存后集大小为1的扩展以便去除冗余
					RExt.offer(rext);
				}
			}
			// 下面我们先将形如<t, {c0, cx}>的变迁加入扩展集（这里就用到了c0.concurrentSet）
			// 添加过程中会去除RExt中的一些冗余，具体如下：
			// 对于新生成的一个扩展rext=<t, {c0, cx}>， 通过extensionHash获取两个扩展rext1=<t, c0>以及rext2=<t, cx>(为了方便假设都存在)
			// 若rext1因rext而截断，则从RExt中删除rext1，rext2同理
			if (t.getPostSet().size() == 2) {
				for (Condition cx : concurrentSet) {
					// {c0, cx}是一个coset，只需验证cx对应库所是否在t的后继库所中（c0一定在），即可构造扩展<t, {c0, cx}>
					if (t.getPostSet().contains(cx.getMap()) && !cx.getMap().equals(c0.getMap())) {
						Event rext = new Event(t, rextId++);
						rext.addPostSet(c0);
						rext.addPostSet(cx);
						calculate_rext(rext);
						if (ReverseUnfolder.DEBUG) {
							System.out.println("[ReverseUnfolder] -- Add "+rext+" to RExt");
						}

						// 去除RExt中的冗余
//						 1.rext1=<t, c0>
						String s1 = t.getId()+"#"+c0.getId();
						if (extensionHash.containsKey(s1)) {
							Event rext1 = extensionHash.get(s1);
							// rext1因rext截断
							if (rext.isMarkSmallerThan(rext1)) {
								extensionHash.remove(s1);
								RExt.remove(rext1);
								redundantNumber++;
								if (ReverseUnfolder.DEBUG) {
									System.out.println("[ReverseUnfolder] -- remove "+rext1+" from RExt");
								}
							}
						}
						// 2.rext2=<t, cx>
						String s2 = t.getId()+"#"+cx.getId();
						if (extensionHash.containsKey(s2)) {
							Event rext2 = extensionHash.get(s2);
							// rext2因rext截断
							if (rext.isMarkSmallerThan(rext2)) {
								extensionHash.remove(s2);
								RExt.remove(rext2);
								redundantNumber++;
								if (ReverseUnfolder.DEBUG) {
									System.out.println("[ReverseUnfolder] -- remove "+rext2+" from RExt");
								}
							}
						}

						// 在1-safe net中任意一个标识中的库所数量不得大于1，这里添加一个剪枝
						boolean ok = true;
						for (Map.Entry<Place, Integer> entry : rext.getMark().entrySet()) {
							if (entry.getValue() > 1) {
								ok = false;
								break;
							}
						}
						if (ok) {
							RExt.offer(rext);
						}
					}
				}
			}
		}
	}
	
	// 计算配置，Mark，Lex等信息
	public void calculate_rext(Event rext) {
		// 1.calculate [rext]
		Set<Event> unionSet = new HashSet<> ();
		unionSet.add(rext);
		// 求子节点配置的并集
		for (Condition c : rext.getPostSet()) {
			Event e = c.getPostSet();
			if (e == null) continue;
			unionSet.addAll(e.getConfiguration());
		}
		List<Event> cfg = new ArrayList<> ();
		cfg.addAll(unionSet);
		// 2.calculate Mark([rext])
		// 计算公式参考论文
		Set<Condition> preSet = new HashSet<> ();
		Set<Condition> postSet = new HashSet<> ();
		for (Event e : cfg) {
			preSet.addAll(e.getPreSet());
			postSet.addAll(e.getPostSet());
		}
		preSet.removeAll(postSet);

		Map<Place, Integer> mark = new HashMap<> ();
		for (Condition c : preSet) {
			Place p = c.getMap();
			if (mark.containsKey(p)) {
				mark.put(p, mark.get(p)+1);
			}
			else {
				mark.put(p, 1);
			}
		}
		// 这里rext的前驱条件还没创建，所以要单独处理一下
		for (Place p : rext.getMap().getPreSet()) {
			if (mark.containsKey(p)) {
				mark.put(p, mark.get(p)+1);
			}
			else {
				mark.put(p, 1);
			}
		}
		// 3.calculate Lex
		List<Transition> lex = new ArrayList<> ();
		for (Event e : cfg) {
			lex.add(e.getMap());
		}
		// 根据变迁id大小排序
		Collections.sort(lex, new Comparator<Transition> () {
			@Override
			public int compare(Transition t1, Transition t2) {
				return t1.getId() - t2.getId();
			}
		});

		rext.setConfiguration(cfg);
		rext.setMark(mark);
		rext.setLexOrder(lex);
		calculate_f(rext); // 计算启发值
		calculate_coverablility(rext); // 最后别忘了计算一下可覆盖性
	}

	// 设置rext的启发值，将以此为基准进行排序
	public void calculate_f(Event rext) {
		if (ReverseUnfolder.FLAG == 4) {
			// hmax
			// 可接受的启发式技术
			int hmax = 0;
			for (Map.Entry<Place, Integer> entry : rext.getMark().entrySet()) {
				hmax = Integer.max(hmax, entry.getKey().getH());
			}
			rext.setF(rext.getConfiguration().size()+hmax);
		} else if (ReverseUnfolder.FLAG == 5 || ReverseUnfolder.FLAG == 6) {
			// hsum
			// 不可接受的启发式技术
			int hsum = 0;
			for (Map.Entry<Place, Integer> entry : rext.getMark().entrySet()) {
				hsum += entry.getKey().getH();
			}
			rext.setF(rext.getConfiguration().size()+hsum);
		} else {
			// 其余的不用计算启发值
		}
	}

	// 通过Mark([rext]) == {Ps}判断可覆盖性
	// 为什么这个函数返回的不是ture或false，而是调用了rext.setCoverable?
	// 我们想的在生成扩展时就进行可覆盖判断，一旦一个扩展使得Pt可覆盖，它将通过调用rext.setCoverable获得最高的优先级，下次扩展一定会从RExt中弹出
	// 当然也采用其他方法实现，比如设置全局flag之类的
	public void calculate_coverablility(Event rext) {
		Map<Place, Integer> mark = rext.getMark();
		if (mark.size() == 1 && mark.containsKey(Ps) && mark.get(Ps) == 1) {
			rext.setCoverable();
		}
	}

	// 截断事件判断(这里为了方便直接对扩展做截断)
	boolean isCutoff(Event rext) {
		for (Event e : E) {
			if (e.isMarkSmallerThan(rext) && e.isAdeSmallerThan(rext)) {
				return true;
			}
		}
		return false;
	}

	// 从e0开始输出一条触发序列
	// 如t0->t1->t2->...在1-safe Petri网（转化后的）上从Ps出发按照这个触发序列就能使Pt可覆盖
	public List<Transition> getFiringSequence(Event e0) {
		List<Transition> fs = new ArrayList<> ();
		// 这个过程有点类似拓扑序
		Queue<Event> queue = new LinkedList<> ();
		queue.offer(e0);
		Set<Condition> M = new HashSet<> (); // 当前搜索到的一个条件集
		while (!queue.isEmpty()) {
			Event e = queue.poll();
			fs.add(e.getMap());
			// 一个事件被触发后把它的后继条件加入M
			for (Condition c : e.getPostSet()) {
				M.add(c);
			}
			for (Condition c : e.getPostSet()) {
				Event e1 = c.getPostSet();
				if (e1 == null) continue;
				// 遍历后继事件，将第一个可触发事件加入队列
				boolean fireable = true;
				// 当前事件的前集条件都在M中则可触发
				for (Condition c1 : e1.getPreSet()) {
					if (!M.contains(c1)) {
						fireable = false;
						break;
					}
				}
				if (!fireable) continue;
				// 触发完以后将用到的前集条件从M中删除
				for (Condition c1 : e1.getPreSet()) {
					M.remove(c1);
				}
				queue.offer(e1);
			}
		}
		return fs;
	}

	// 下面都是一些用来打印调试信息的函数
	public int getCsSize() {
		return C.size();
	}
	
	public int getEsSize() {
		return E.size();
	}
	
	public int getRExtSize() {
		return RExt.size();
	}
	
	public int getCutoffNumber() {
		return cutoffNumber;
	}

	public int getRedundantNumber() {
		return redundantNumber;
	}

}