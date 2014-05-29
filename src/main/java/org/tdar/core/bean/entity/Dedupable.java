package org.tdar.core.bean.entity;

import java.util.Set;

import org.tdar.core.bean.HasStatus;
import org.tdar.core.bean.Persistable;

/**
 * An interface used to indicate classes whose instances are potentially 'de-dupable' under authority management service.
 * 
 * @author jimdevos
 * 
 */
@SuppressWarnings("hiding")
public interface Dedupable<Dedupable> extends Persistable, HasStatus {

    public boolean isDedupable();

    public Set<Dedupable> getSynonyms();

}
