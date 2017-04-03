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

import java.util.*;

/**
 * Class that isolates results and errors of a {@link BaseParseTask}
 *
 * @param <P> The type of the parsed results
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.10-b
 */
public abstract class BaseParseResult<P> {

    private P parsedResults;
    private Collection<String> parseErrors;

    public BaseParseResult(P parsedResults, Collection<String> parseErrors) {
        this(parsedResults);
        this.parseErrors = parseErrors;
    }

    public BaseParseResult(P parsedResults) {
        this.parsedResults = parsedResults;
    }

    public P getParsedResults() {
        return parsedResults;
    }

    public Collection<String> getParseErrors() {
        return parseErrors;
    }
}