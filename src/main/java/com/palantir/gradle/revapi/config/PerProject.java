/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.gradle.revapi.config;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import one.util.streamex.EntryStream;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutablePerProject.class)
abstract class PerProject<T> {
    @JsonValue
    protected abstract Map<GroupAndName, Set<T>> items();

    public Set<T> forGroupAndName(GroupAndName groupAndName) {
        return items().getOrDefault(groupAndName, Collections.emptySet());
    }

    public PerProject<T> merge(GroupAndName groupAndName, Set<T> acceptedBreaks) {
        Map<GroupAndName, Set<T>> newAcceptedBreaks = new HashMap<>(items());
        newAcceptedBreaks.put(groupAndName, Sets.union(
                acceptedBreaks,
                this.items().getOrDefault(groupAndName, ImmutableSet.of())));

        return PerProject.<T>builder()
                .putAllAcceptedBreaks(newAcceptedBreaks)
                .build();
    }

    public <R> Stream<R> flatten(BiFunction<GroupAndName, T, R> flattener) {
        return EntryStream.of(items())
                .flatMapKeyValue((groupAndName, items) -> items.stream()
                        .map(item -> flattener.apply(groupAndName, item)));
    }

    static final class Builder<T> extends ImmutablePerProject.Builder<T> { }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static <T> PerProject<T> empty() {
        return PerProject.<T>builder().build();
    }
}
