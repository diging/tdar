package org.tdar.core.event;

import java.io.Serializable;

public class TdarEvent implements Serializable {

	private static final long serialVersionUID = -6141090148370581562L;
	private EventType type;
	private Object indexable;
	private Long extraId;

	public TdarEvent(Object irFile, EventType type, Long extraId) {
		this(irFile, type);
		this.setExtraId(extraId);
	}

	public TdarEvent(Object indexable, EventType type) {
		this.indexable = indexable;
		this.type = type;
	}

	public EventType getType() {
		return type;
	}

	public void setType(EventType type) {
		this.type = type;
	}

	public Object getRecord() {
		return indexable;
	}

	public void setIndexable(Object indexable) {
		this.indexable = indexable;
	}

	public Long getExtraId() {
		return extraId;
	}

	public void setExtraId(Long extraId) {
		this.extraId = extraId;
	}

}
