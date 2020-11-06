package unfolding.occurrentnet;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import petrinet.*;
import unfolding.Unfolder;

// 事件
public class Event implements Comparable<Event> {
	private Transition map; // 映射到的变迁
	private int id;
	private int extId;
	private List<Condition> preSet;
	private List<Condition> postSet;
	private List<Event> configuration;
	private Map<Place, Integer> mark;
	private List<Transition> lexOrder;
	private int f; // 排序依据
	private int coverable; // 0 or 1

	public Event(Transition map, int extId) {
		this.map = map;
		this.id = -1;
		this.extId = extId;
		preSet = new ArrayList<> ();
		postSet = new ArrayList<> ();
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

	public int getExtId() {
		return extId;
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

	// Mark([e]) == Mark([e'])
	public boolean isMarkEqualWith(Event that) {
		if (this.mark.size() != that.mark.size()) {
			return false;
		}
		for (Map.Entry<Place, Integer> entry : this.mark.entrySet()) {
			if (!that.mark.containsKey(entry.getKey())) {
				return false;
			}
			if (that.mark.get(entry.getKey()) != entry.getValue()) {
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
	
	@Override
	// sort with adequate order
	public int compareTo(Event that) {
		// If a extension can make Mf coverable, we should consider it first
		if (this.coverable != that.coverable) {
			return that.coverable - this.coverable;
		}

		if (Unfolder.FLAG == 1) {
			// adequate order (bfs)
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
		} else if (Unfolder.FLAG == 2) {
			// dfs
			return that.configuration.size() - this.configuration.size();
		} else if (Unfolder.FLAG == 3) {
			// hmax
			return this.f - that.f;
		} else if (Unfolder.FLAG == 4) {
			// hsum
			return this.f - that.f;
		}
		return 0;
	}

	// 以下函数调试用
	@Override
	public String toString() {
		// 例如：e0<t0,{c0,c1,c2}>#0 or rext0<t0,{c0,c1,c2}>#0
		String s;
		if (id < 0) s =  "ext"+ extId +"<t"+map.getId()+",{";
		else s = "e"+id+"<t"+map.getId()+",{";
		for (int i = 0; i < preSet.size(); i++) {
			s += "c"+preSet.get(i).getId()+(i==preSet.size()-1?"}>":",");
		}
		return (s+"#"+ extId);
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
