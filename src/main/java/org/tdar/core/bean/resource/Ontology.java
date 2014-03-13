package org.tdar.core.bean.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringUtils;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.springframework.util.CollectionUtils;
import org.tdar.core.bean.SupportsResource;

/**
 * $Id$
 * 
 * Represents an Ontology in tDAR. There are two ways to access an Ontology, via the URL (getUrl()) and
 * via the filesystem via getFilename()).
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision $
 */
@Entity
@Indexed
@Table(name = "ontology", indexes = {@Index(name="ontology_catvar_id", columnList="category_variable_id")})
@XmlRootElement(name = "ontology")
public class Ontology extends InformationResource implements SupportsResource {

    private static final long serialVersionUID = -5871337600253105652L;

    @ManyToOne
    @JoinColumn(name = "category_variable_id")
    @IndexedEmbedded(depth = 1)
    private CategoryVariable categoryVariable;

    @OneToMany(mappedBy = "ontology", cascade = CascadeType.ALL)
    private List<OntologyNode> ontologyNodes = new ArrayList<OntologyNode>();

    private transient Map<Long, OntologyNode> idMap = new WeakHashMap<Long, OntologyNode>();
    private transient Map<String, OntologyNode> iriMap = new WeakHashMap<String, OntologyNode>();
    private transient Map<String, OntologyNode> nameMap = new WeakHashMap<String, OntologyNode>();

    private final static Comparator<OntologyNode> IMPORT_ORDER_COMPARATOR = new Comparator<OntologyNode>() {
        @Override
        public int compare(OntologyNode o1, OntologyNode o2) {
            int comparison = o1.getImportOrder().compareTo(o2.getImportOrder());
            if (comparison == 0) {
                // use default comparison by index
                return o1.compareTo(o2);
            }
            return comparison;
        }
    };

    public Ontology() {
        setResourceType(ResourceType.ONTOLOGY);
    }

    @Override
    public CategoryVariable getCategoryVariable() {
        return categoryVariable;
    }

    @Override
    public void setCategoryVariable(CategoryVariable categoryVariable) {
        this.categoryVariable = categoryVariable;
    }

    public Map<Long, OntologyNode> getIdToNodeMap() {
        HashMap<Long, OntologyNode> idToNodeMap = new HashMap<Long, OntologyNode>();
        for (OntologyNode node : ontologyNodes) {
            idToNodeMap.put(node.getId(), node);
        }
        return idToNodeMap;
    }

    /**
     * Returns a list of internal IDs (not database IDs) mapped to list of child ontology nodes.
     * 
     * @return
     */
    public SortedMap<Integer, List<OntologyNode>> toOntologyNodeMap() {
        List<OntologyNode> sortedOntologyNodes = getSortedOntologyNodes();
        TreeMap<Integer, List<OntologyNode>> map = new TreeMap<Integer, List<OntologyNode>>();
        for (OntologyNode node : sortedOntologyNodes) {
            Integer intervalStart = node.getIntervalStart();
            String index = node.getIndex();
            for (String indexId : StringUtils.split(index, '.')) {
                Integer parentId = Integer.valueOf(indexId);
                // don't include this node if the parent id is the same as this node's interval start
                if (parentId.equals(intervalStart))
                    continue;
                List<OntologyNode> children = map.get(parentId);
                if (children == null) {
                    children = new ArrayList<OntologyNode>();
                    map.put(parentId, children);
                }
                children.add(node);
            }
        }
        return map;
    }

    @Transient
    public OntologyNode getNodeByName(String name) {
        if (CollectionUtils.isEmpty(nameMap)) {
            initializeNameAndIriMaps();
        }
        return nameMap.get(name);
    }

    @Transient
    public OntologyNode getNodeByIri(String iri) {
        if (CollectionUtils.isEmpty(iriMap)) {
            initializeNameAndIriMaps();
        }
        return iriMap.get(iri);
    }

    private void initializeNameAndIriMaps() {
        for (OntologyNode node: getOntologyNodes()) {
            nameMap.put(node.getDisplayName(), node);
            iriMap.put(node.getNormalizedIri(), node);
        }
    }

    @Transient
    public OntologyNode getNodeByNameIgnoreCase(String name) {
        for (OntologyNode node : getOntologyNodes()) {
            if (StringUtils.equalsIgnoreCase(node.getDisplayName(),name)) {
                return node;
            }
        }
        return null;
    }

    public List<OntologyNode> getSortedOntologyNodes() {
        // return ontology nodes by natural order.
        return getSortedOntologyNodes(null);
    }

    public List<OntologyNode> getSortedOntologyNodesByImportOrder() {
        return getSortedOntologyNodes(IMPORT_ORDER_COMPARATOR);
    }

    public List<OntologyNode> getSortedOntologyNodes(Comparator<OntologyNode> comparator) {
        ArrayList<OntologyNode> sortedNodes = new ArrayList<OntologyNode>(getOntologyNodes());
        Collections.sort(sortedNodes, comparator);
        return sortedNodes;
    }

    @XmlElementWrapper(name = "ontologyNodes")
    @XmlElement(name = "ontologyNode")
    public List<OntologyNode> getOntologyNodes() {
        return ontologyNodes;
    }

    public void setOntologyNodes(List<OntologyNode> ontologyNodes) {
        this.ontologyNodes = ontologyNodes;
    }

    public OntologyNode getOntologyNodeById(Long id) {
        if (idMap.isEmpty()) {
            for (OntologyNode node : getOntologyNodes()) {
                idMap.put(node.getId(), node);
            }
        }
        return idMap.get(id);
    }

    @Override
    public String getAdditonalKeywords() {
        StringBuilder sb = new StringBuilder();
        if (getCategoryVariable() != null) {
            sb.append(getCategoryVariable().getLabel()).append(" ");
            if (getCategoryVariable().getParent() != null) {
                sb.append(getCategoryVariable().getParent().getLabel());
            }
        }
        return sb.toString();
    }

}
