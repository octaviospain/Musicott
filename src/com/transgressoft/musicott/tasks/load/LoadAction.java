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

package com.transgressoft.musicott.tasks.load;

import java.io.*;

/**
 * A {@code LoadAction} class performs the deserialization of a {@code JSON} formatted file
 * using {@code Json-IO} library.
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 * @since 0.10.1-b
 *
 * @see <a href="https://github.com/jdereg/json-io">Json-IO</a>
 */
public interface LoadAction {

    /**
     * Parses an {@code Object} of a previously serialized instance using Json-IO
     *
     * @param jsonFormattedFile A {@code JSON} formatted {@link File}
     *
     * @return The parsed {@code Object}
     *
     * @throws IOException If something went bad
     */
    Object parseJsonFile(File jsonFormattedFile) throws IOException;
}