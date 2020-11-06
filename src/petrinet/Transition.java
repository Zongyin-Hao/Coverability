package petrinet;

import java.util.List;
import java.util.ArrayList;

// 变迁
public class Transition {
	private int id;
	private List<Place> preSet;
	private List<Place> postSet;
	private int h; // 启发值（主要用于辅助计算Place的启发值）

	public Transition(int id) {
		this.id = id;
		preSet = new ArrayList<>();
		postSet = new ArrayList<>();
		h = Integer.MAX_VALUE;
	}

	public int getId() {
		return id;
	}

	public void addPreSet(Place p) {
		preSet.add(p);
	}

	public List<Place> getPreSet() {
		return preSet;
	}

	public void addPostSet(Place p) {
		postSet.add(p);
	}

	public List<Place> getPostSet() {
		return postSet;
	}

	public void setH(int v) { h = v; }

	public int getH() { return h; }
}
