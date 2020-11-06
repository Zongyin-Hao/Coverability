package unfolding.occurrentnet;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import petrinet.*;

// 条件
public class Condition {
	private Place map; // 映射到的库所
	private int id;
	private Event preSet; // 出现网性质：只有一个前驱事件
	private List<Event> postSet;
	private Set<Condition> concurrentSet;  // 存储与当前条件并发的所有条件
	
	public Condition(Place map, int id) {
		this.map = map;
		this.id = id;
		postSet = new ArrayList<> ();
		concurrentSet = new HashSet<> ();
	}
	
	public Place getMap() {
		return map;
	}
	
	public int getId() {
		return id;
	}
	
	public void setPreSet(Event e) {
		preSet = e;
	}
	
	public Event getPreSet() {
		return preSet;
	}
	
	public void addPostSet(Event e) {
		postSet.add(e);
	}
	
	public List<Event> getPostSet() {
		return postSet;
	}
	
	public void addConcurrentSet(Condition c) {
		concurrentSet.add(c);
	}
	
	public void setConcurrentSet(Set<Condition> set) {
		concurrentSet = set;
	}

	public Set<Condition> getConcurrentSet() {
		return concurrentSet;
	}

	public boolean isConcurrentWith(Condition c) {
		return concurrentSet.contains(c);
	}

	@Override
	public String toString() {
		// 例如：c0<p0>
		return "c"+id+"<p"+map.getId()+">";
	}

}
