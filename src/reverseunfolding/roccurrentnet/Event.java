package reverseunfolding.roccurrentnet;

import java.util.*;

import petrinet.*;
import reverseunfolding.ReverseUnfolder;

// 事件
public class Event implements Comparable<Event> {
	private Transition map; // 映射到的变迁
	private int id;
	private int rextId; // 关于这个rextId参考ReverseUnfolder中对成员变量RExt的解释
	private List<Condition> preSet;
	private List<Condition> postSet;
	private List<Event> configuration; // [e]
	private Map<Place, Integer> mark; // Mark([e])
	private List<Transition> lexOrder; // 有点类似Esparza的全序，但进行了弱化
	private int f; // 排序依据
	private int coverable; // 0 or 1

	public Event(Transition map, int rextId) {
		this.map = map;
		id = -1;
		this.rextId = rextId;
		preSet = new ArrayList<>();
		postSet = new ArrayList<>();
		configuration = new ArrayList<>();
		mark = new HashMap<>();
		lexOrder = new ArrayList<> ();
		coverable = 0;
	}

	public Transition getMap() {
		return map;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}

	public int getRextId() {
		return rextId;
	}

	public void addPreSet(Condition c) {
		preSet.add(c);
	}

	public List<Condition> getPreSet() {
		return preSet;
	}

	public void addPostSet(Condition c) {
		postSet.add(c);
	}

	public List<Condition> getPostSet() {
		return postSet;
	}

	public void setConfiguration(List<Event> cfg) {
		configuration = cfg;
	}

	public List<Event> getConfiguration() {
		return configuration;
	}

	public void setMark(Map<Place, Integer> mk) {
		mark = mk;
	}

	public Map<Place, Integer> getMark() {
		return mark;
	}

	public void setLexOrder(List<Transition> lo) {
		lexOrder = lo;
	}
	
	public List<Transition> getLexOrder() {
		return lexOrder;
	}

	public void setF(int v) { f = v; }
	
	public void setCoverable() {
		coverable = 1;
	}
	
	public boolean isCoverable() {
		return (coverable == 1);
	}
	
	// Mark([e]) <= Mark([e'])
	public boolean isMarkSmallerThan(Event that) {
		for (Map.Entry<Place, Integer> entry : this.mark.entrySet()) {
			if (!that.mark.containsKey(entry.getKey())) {
				return false;
			}
			if (that.mark.get(entry.getKey()) < entry.getValue()) {
				return false;
			}
		}
		return true;
	}
	
	// adequate order
	// |[e]| < |[e']| || (|[e]| == |[e']| && Lex(φ([e])) < Lex(φ([e']))) 
	public boolean isAdeSmallerThan(Event that) {
		if (this.configuration.size() < that.configuration.size()) return true;
		else if (this.configuration.size() == that.configuration.size()) {
			for (int i = 0; i < this.lexOrder.size(); i++) {
				if (this.lexOrder.get(i).getId() < that.lexOrder.get(i).getId()) {
					return true;
				}
				else if (this.lexOrder.get(i).getId() > that.lexOrder.get(i).getId()) {
					return false;
				}
			}
			return false;
		}
		return false;
	}

	// heuristic strategy
	@Override
	public int compareTo(Event that) {
		// If a extension can make Mf coverable, we should consider it first
		if (this.coverable != that.coverable) {
			return that.coverable - this.coverable;
		}

		if (ReverseUnfolder.FLAG == 1) {
			// adequate order(bfs)
			if (this.configuration.size() != that.configuration.size()) {
				return this.configuration.size()-that.configuration.size();
			}
			for (int i = 0; i < this.lexOrder.size(); i++) {
				if (this.lexOrder.get(i).getId() < that.lexOrder.get(i).getId()) {
					return -1;
				}
				else if (this.lexOrder.get(i).getId() > that.lexOrder.get(i).getId()) {
					return 1;
				}
			}
			return 0;
		} else if (ReverseUnfolder.FLAG == 2) {
			// dfs
			return that.configuration.size()-this.configuration.size();
		} else if (ReverseUnfolder.FLAG == 3) {
			// block+dfs
			int t1 =  (this.postSet.size() == this.map.getPostSet().size() ? 1 : 0);
			int t2 =  (that.postSet.size() == that.map.getPostSet().size() ? 1 : 0);
			if (t1 != t2) return t2 - t1;
			return that.configuration.size()-this.configuration.size();
		} else if (ReverseUnfolder.FLAG == 4) {
			// hmax
			return this.f - that.f;
		} else if (ReverseUnfolder.FLAG == 5) {
			// hsum
			return this.f - that.f;
		} else if (ReverseUnfolder.FLAG == 6) {
			// block+hsum
			int t1 =  (this.postSet.size() == this.map.getPostSet().size() ? 1 : 0);
			int t2 =  (that.postSet.size() == that.map.getPostSet().size() ? 1 : 0);
			if (t1 != t2) {
				return t2 - t1;
			}
			return this.f - that.f;
		}
		return 0;
	}
	
	// 以下函数调试用
	@Override
	public String toString() {
		// 例如：e0<t0,{c0,c1,c2}>#0 or rext0<t0,{c0,c1,c2}>#0
		String s;
		if (id < 0) s =  "rext"+ rextId +"<t"+map.getId()+",{";
		else s = "e"+id+"<t"+map.getId()+",{";
		for (int i = 0; i < postSet.size(); i++) {
			s += "c"+postSet.get(i).getId()+(i==postSet.size()-1?"}>":",");
		}
		return (s+"#"+ rextId);
	}
	
	public void outputConfiguration() {
		for (Event e : configuration) {
			System.out.print("e"+e.getId() + " ");
		}
		System.out.println();
	}
	
	public void outputMark() {
		for (Map.Entry<Place, Integer> entry : mark.entrySet()) {
			System.out.print("p"+entry.getKey().getId()+"*"+entry.getValue()+" ");
		}
		System.out.println();
	}
	
	public void outputLex() {
		for (Transition t : lexOrder) {
			System.out.print("t"+t.getId()+" ");
		}
		System.out.println();
	}
}
