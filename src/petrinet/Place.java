package petrinet;

import java.util.List;
import java.util.ArrayList;

// 库所
public class Place {
	private int id;
	private List<Transition> preSet;
	private List<Transition> postSet;
	private int h; // 启发值
	
	public Place(int id) {
		this.id = id;
		preSet = new ArrayList<>();
		postSet = new ArrayList<>();
		h = Integer.MAX_VALUE;
	}

	public int getId() {
		return id;
	}

	public void addPreSet(Transition t) {
		preSet.add(t);
	}

	public List<Transition> getPreSet() {
		return preSet;
	}

	public void addPostSet(Transition t) {
		postSet.add(t);
	}

	public List<Transition> getPostSet() {
		return postSet;
	}

	public void setH(int v) { h = v; }

	public int getH() { return h; }
}
