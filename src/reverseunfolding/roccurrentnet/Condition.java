package reverseunfolding.roccurrentnet;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import petrinet.*;

// 条件
public class Condition {
	private Place map; // 映射到的库所
	private int id;
	private List<Event> preSet;
	private Event postSet; // 反向出现网性质：只有一个后继事件
	private Set<Condition> concurrentSet; // 存储与当前条件并发的所有条件

	public Condition(Place map, int id) {
		this.map = map;
		this.id = id;
		preSet = new ArrayList<>();
		concurrentSet = new HashSet<>();
	}

	public Place getMap() {
		return map;
	}
	
	public int getId() {
		return id;
	}

	public void addPreSet(Event e) {
		preSet.add(e);
	}

	public List<Event> getPreSet() {
		return preSet;
	}

	public void setPostSet(Event e) {
		postSet = e;
	}

	public Event getPostSet() {
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

	@Override
	public String toString() {
		// 例如：c0<p0>
		return "c"+id+"<p"+map.getId()+">";
	}

}
