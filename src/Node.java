import java.util.ArrayList;

public class Node {
	String attribute;
	int attributeIndex;
	ArrayList<Pointer> pointerArray;
	public Node(String attribute, int attributeIndex, ArrayList<Pointer> pointerArray) {
		this.attribute = attribute;
		this.attributeIndex = attributeIndex;
		this.pointerArray = pointerArray;
	}
}
