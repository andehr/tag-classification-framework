package uk.ac.susx.tag.classificationframework.datastructures;

/*
 * #%L
 * LogicalCollection.java - classificationframework - CASM Consulting - 2,013
 * %%
 * Copyright (C) 2013 - 2014 CASM Consulting
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.collect.Iterators;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A LogicalCollection contains no elements of its own, instead it maintains a list of
 * references to other collections all containing the same element type.
 *
 * When iterated over, this collection will return each of the elements of each of the
 * collections in the order in which the collections were added, skipping empty collections.
 * For example:
 *
 *  List a = Lists.newArrayList(1, 2, 3, 4, 5);
 *  List b = Lists.newArrayList(6,7,8);
 *  List c = Lists.newArrayList();
 *  List d = Lists.newArrayList(9,10,11);
 *  LogicalCollection allItems = new LogicalCollection(a).add(b).add(c).add(d);
 *  System.out.println("Items : " + allItems + "\nSize  : "+allItems.size());
 *
 *  Outputs --> Items : [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
 *              Size  : 11
 *
 * User: Andrew D. Robertson
 * Date: 06/09/2013
 * Time: 15:21
 */
public class LogicalCollection<E> extends AbstractCollection<E> {

    private List<Collection<? extends E>> collections = new LinkedList<>();
    private int size = 0;

    public LogicalCollection(){}

    public LogicalCollection(Collection<? extends E> collection) {
        add(collection);
    }

    public LogicalCollection(Collection<? extends E> collection1, Collection<? extends E> collection2) {
        this(collection1); add(collection2);
    }

    public LogicalCollection(Collection<? extends E> collection1, Collection<? extends E> collection2, Collection<? extends E> collection3){
        this(collection1, collection2); add(collection3);
    }

    public LogicalCollection(Collection<? extends E> collection1, Collection<? extends E> collection2, Collection<? extends E> collection3, Collection<? extends E> collection4){
        this(collection1, collection2, collection3); add(collection4);
    }

    public LogicalCollection(Collection<? extends E> collection1, Collection<? extends E> collection2, Collection<? extends E> collection3, Collection<? extends E> collection4, Collection<? extends E> collection5){
        this(collection1, collection2, collection3, collection4); add(collection5);
    }

    public LogicalCollection<E> add(Collection<? extends E> collection){
        collections.add(collection);
        size += collection.size();
        return this;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>(){
            Iterator<Collection<? extends E>> collectionIterator = collections.iterator();
            Iterator<? extends E> currentIterator = collectionIterator.hasNext()? collectionIterator.next().iterator()
                                                                      : Iterators.<E>emptyIterator();
            @Override
            public boolean hasNext() {
                if (currentIterator.hasNext()) return true;  // If there are still more elements in the current collection return true
                else {                                       // Otherwise move on to the next collection
                    while (collectionIterator.hasNext()) {   // And so on, until more elements are found, else return false.
                        currentIterator = collectionIterator.next().iterator();
                        if (currentIterator.hasNext()) return true;
                    } return false;
                }
            }

            @Override
            public E next() {
                if (hasNext()) return currentIterator.next();
                else throw new NoSuchElementException();
            }

            @Override
            public void remove() { throw new UnsupportedOperationException(); }
        };
    }

    @Override
    public int size() { return size; }
}
