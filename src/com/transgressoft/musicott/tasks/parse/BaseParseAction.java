/*
 * This file is part of Musicott software.
 *
 * Musicott software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Musicott library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Musicott. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2015 - 2017 Octavio Calleya
 */

package com.transgressoft.musicott.tasks.parse;

import com.google.common.collect.*;
import com.transgressoft.musicott.model.*;
import org.slf4j.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Extends from {@link RecursiveTask} so it can be used inside a
 * {@link ForkJoinPool} providing common methods and
 * collections used when parsing items to the music library.
 *
 * @param <I> The type of the item that will be parsed
 * @param <X> The type of the parsed result
 * @param <T> The type of the returned {@link BaseParseResult}
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.10-b
 */
public abstract class BaseParseAction<I, X, T extends BaseParseResult<X>> extends RecursiveTask<T> {

    protected final transient Logger LOG = LoggerFactory.getLogger(getClass().getName());

    protected final transient List<I> itemsToParse;
    protected final transient Collection<String> parseErrors;
    protected final transient BaseParseTask parentTask;
    protected final transient MusicLibrary musicLibrary = MusicLibrary.getInstance();

    public BaseParseAction(List<I> itemsToParse, BaseParseTask parentTask) {
        this.itemsToParse = itemsToParse;
        this.parentTask = parentTask;
        parseErrors = new ArrayList<>();
    }

    protected abstract void parseItem(I item);

    /**
     * Calls {@link RecursiveTask#fork} for each sub action and then joins the results
     */
    protected void forkIntoSubActions() {
        List<BaseParseAction<I, X, ? extends BaseParseResult<X>>> subActions = createSubActions();
        subActions.forEach(BaseParseAction::fork);
        subActions.forEach(action -> joinPartialResults(action.join()));
        LOG.debug("Forking parse of item into {} sub actions", subActions.size());
    }

    /**
     * Creates several {@link BaseParseAction} objects with partitions of
     * the {@link #itemsToParse} collection
     *
     * @return A {@link List} with the {@link BaseParseAction} objects
     */
    protected List<BaseParseAction<I, X, ? extends BaseParseResult<X>>> createSubActions() {
        int subListsSize = itemsToParse.size() / getNumberOfPartitions();
        return Lists.partition(itemsToParse, subListsSize)
                    .stream().map(this::parseActionMapper)
                    .collect(ImmutableList.toImmutableList());
    }

    protected abstract <T extends BaseParseResult<X>> void joinPartialResults(T partialResult);

    /**
     * Returns the number of partitions in which {@link #itemsToParse}
     * will be split, if its size is greater than a certain number
     *
     * @return The number of partitions
     */
    protected abstract int getNumberOfPartitions();

    /**
     * Returns a new instance of a {@link BaseParseAction} implementation
     * given a sub list of the items that should be processed independently
     *
     * @param subItems A {@link List} with the items
     *
     * @return A {@link BaseParseAction} instance with the items it has to process
     */
    protected abstract BaseParseAction<I, X, ? extends BaseParseResult<X>> parseActionMapper(List<I> subItems);
}