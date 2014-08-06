package uk.ac.susx.tag.classificationframework.geolocation;

import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Andrew D. Robertson on 06/08/2014.
 */
public class ResultsElement {

    public String type = null;
    public long id = 0;
    public double lat = 0;
    public double lon = 0;
    public double radius = 0;
    public Map<String, String> tags = null;
    public List<Long> nodes = null;

    public boolean hasTag(String tag){
        return tags != null && tags.containsKey(tag);
    }

    /**
     * return true if this element has ANY of the tags in *tags*
     */
    public boolean hasTag(Set<String> tags){
        return Sets.intersection(this.tags.keySet(), tags).size() > 0;
    }

    public boolean hasTagValue(String tag, String value){
        return tags != null && tags.containsKey(tag) && tags.get(tag).equals(value);
    }

    public String getTagValue(String tag){
        return tags.containsKey(tag)? tags.get(tag) : null;
    }
}
