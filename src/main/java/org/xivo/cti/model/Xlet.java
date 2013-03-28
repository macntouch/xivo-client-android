package org.xivo.cti.model;

public class Xlet {
	private final String name;
	private final String container;
	private final int order;

	public Xlet(String name, String container, int order) {
		this.name = name;
		this.container = container;
		this.order = order;
	}
	
	@Override
	public boolean equals(Object obj) {
		Xlet xlet = (Xlet) obj;
		return xlet.name.equals(name) 
				&& xlet.container.equals(container) && xlet.order == order;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return name + " " + container + " " + order;
	}
}
