package com.datasalt.pangolin.pangool;

import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.io.RawComparator;

import com.datasalt.pangolin.grouper.io.tuple.ITuple.InvalidFieldException;
import com.datasalt.pangolin.pangool.SortCriteria.SortOrder;

/**
 * Builds an inmutable {@link Sorting} instance.
 * May have childs {@link SortCriteriaBuilder}.
 * 
 * @author pere
 * 
 */
@SuppressWarnings("rawtypes")
public class SortingBuilder extends SortCriteriaBuilder {

	private Map<Integer, SortCriteriaBuilder> secondarySortBuilders;

	public SortingBuilder() {
		super(null);
		secondarySortBuilders = new HashMap<Integer, SortCriteriaBuilder>();
	}
	
	public SortingBuilder add(String fieldName, SortOrder order, Class<? extends RawComparator> customComparator) throws InvalidFieldException {
		super.add(fieldName, order, customComparator);
		return this;
	}
	
	@Override
	public SortingBuilder add(String fieldName, SortOrder order) throws InvalidFieldException {
		super.add(fieldName, order);
	  return this;
	}
	
	public SortCriteriaBuilder secondarySort(Integer sourceId) {
		SortCriteriaBuilder builder = new SortCriteriaBuilder(this);
		secondarySortBuilders.put(sourceId, builder);
		return builder;
	}

	public SortingBuilder addSourceId(SortOrder order) throws InvalidFieldException {
		add(Schema.Field.SOURCE_ID_FIELD, order, null);
		return this;
	}
	
	public Sorting buildSorting() {
		Map<Integer, SortCriteria> secondarySortCriterias = new HashMap<Integer, SortCriteria>();
		for(Map.Entry<Integer, SortCriteriaBuilder> builders: secondarySortBuilders.entrySet()) {
			secondarySortCriterias.put(builders.getKey(), builders.getValue().buildSortCriteria());
		}
		Sorting sorting = new Sorting(buildSortCriteria(), secondarySortCriterias);
		return sorting;
	}
}
