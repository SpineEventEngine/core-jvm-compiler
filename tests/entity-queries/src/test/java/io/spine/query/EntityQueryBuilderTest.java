/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.query;

import com.google.common.collect.ImmutableList;
import io.spine.tools.query.ProjectId;
import io.spine.tools.query.ProjectView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.query.ComparisonOperator.EQUALS;
import static io.spine.query.ComparisonOperator.GREATER_THAN;
import static io.spine.query.ComparisonOperator.LESS_THAN;
import static io.spine.query.Direction.ASC;
import static io.spine.query.Direction.DESC;
import static io.spine.query.LogicalOperator.AND;
import static io.spine.query.LogicalOperator.OR;
import static io.spine.query.given.EntityQueryBuilderTestEnv.assertHasParamValue;
import static io.spine.query.given.EntityQueryBuilderTestEnv.assertNoSortingMaskLimit;
import static io.spine.query.given.EntityQueryBuilderTestEnv.generateIds;
import static io.spine.query.given.EntityQueryBuilderTestEnv.projectId;
import static io.spine.query.given.EntityQueryBuilderTestEnv.subjectWithNoParameters;
import static io.spine.query.given.Lifecycle.DELETED;
import static io.spine.tools.query.Project.Status.CREATED;
import static io.spine.tools.query.Project.Status.DONE;
import static io.spine.tools.query.Project.Status.STARTED;
import static io.spine.tools.query.ProjectView.Column.daysSinceStarted;
import static io.spine.tools.query.ProjectView.Column.projectName;
import static io.spine.tools.query.ProjectView.Column.status;
import static io.spine.tools.query.ProjectView.Column.wasReassigned;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`EntityQueryBuilder` should")
class EntityQueryBuilderTest {

    @Nested
    @DisplayName("create `EntityQuery` instances")
    final class CreateQuery {

        @Test
        @DisplayName("without parameters")
        void empty() {
            var query = ProjectView.query().build();
            var subject = subjectWithNoParameters(query);
            assertThat(subject.id()
                              .values()).isEmpty();
            assertNoSortingMaskLimit(query);
        }

        @Test
        @DisplayName("which hold the types of the queried entity state and entity ID")
        void withRecordType() {
            var subject = ProjectView.query().build().subject();
            assertThat(subject.recordType()).isEqualTo(ProjectView.class);
            assertThat(subject.idType()).isEqualTo(ProjectId.class);
        }

        @Test
        @DisplayName("by a single ID value")
        void byId() {
            var expectedId = projectId();
            var query = ProjectView.query()
                                   .projectId().is(expectedId)
                                   .build();
            var subject = subjectWithNoParameters(query);
            var actualIdParam = subject.id();
            assertThat(actualIdParam.values()).containsExactly(expectedId);
        }

        @Test
        @DisplayName("by several ID values")
        void bySeveralIds() {
            var expectedValues = generateIds(24);
            var query = ProjectView.query()
                                   .projectId().in(expectedValues)
                                   .build();
            var subject = subjectWithNoParameters(query);
            var actualIdParam = subject.id();
            assertThat(actualIdParam.values()).isEqualTo(expectedValues);
        }

        @Test
        @DisplayName("by the values of several entity columns")
        void byColumnValues() {
            var statusValue = DONE;
            var daysSinceStarted = 15;
            var query = ProjectView.query()
                                   .status().is(statusValue)
                                   .daysSinceStarted().isLessThan(daysSinceStarted)
                                   .build();
            var rootPredicate = query.subject().predicate();
            assertThat(rootPredicate.operator()).isEqualTo(AND);

            var children = rootPredicate.children();
            assertThat(children).hasSize(0);
            assertThat(rootPredicate.parameters()).hasSize(2);

            assertHasParamValue(rootPredicate, status(), EQUALS, statusValue);
            assertHasParamValue(rootPredicate, daysSinceStarted(), LESS_THAN, daysSinceStarted);
        }

        @Test
        @DisplayName("by the value of either of the entity columns")
        @SuppressWarnings("RedundantExplicitVariableType") // Avoid extra casts for lambdas.
        void byEitherColumn() {
            var daysSinceStarted = 30;
            var statusValue = DONE;
            var deletedValue = true;

            Either<ProjectView.QueryBuilder> startedMoreThanMonthAgo =
                    project -> project.daysSinceStarted().isGreaterThan(daysSinceStarted);
            Either<ProjectView.QueryBuilder> isDone =
                    project -> project.status().is(statusValue);
            Either<ProjectView.QueryBuilder> isDeleted =
                    project -> project.where(DELETED.column(), deletedValue);
            var query = ProjectView.query()
                                   .either(startedMoreThanMonthAgo, isDone, isDeleted)
                                   .build();

            var rootPredicate = query.subject().predicate();
            var predicates = rootPredicate.children();
            assertThat(predicates).hasSize(0);
            assertThat(rootPredicate.operator()).isEqualTo(OR);
            assertThat(rootPredicate.allParams()).hasSize(3);

            assertHasParamValue(rootPredicate, daysSinceStarted(), GREATER_THAN, daysSinceStarted);
            assertHasParamValue(rootPredicate, status(), EQUALS, statusValue);
            assertHasParamValue(rootPredicate, DELETED.column(), EQUALS, deletedValue);
        }

        @Test
        @DisplayName("sorted by several entity columns")
        void withSorting() {
            var query = ProjectView.query()
                                   .sortAscendingBy(daysSinceStarted())
                                   .sortAscendingBy(projectName())
                                   .sortDescendingBy(wasReassigned())
                                   .build();
            var sorting = query.sorting();
            assertThat(sorting).hasSize(3);
            assertThat(sorting.get(0)).isEqualTo(new SortBy<>(daysSinceStarted(), ASC));
            assertThat(sorting.get(1)).isEqualTo(new SortBy<>(projectName(), ASC));
            assertThat(sorting.get(2)).isEqualTo(new SortBy<>(wasReassigned(), DESC));
        }

        @Test
        @DisplayName("sorted by an entity column values with the record limit")
        void withLimitAndSorting() {
            var dozenOfRecords = 10;
            var query = ProjectView.query()
                                   .sortDescendingBy(daysSinceStarted())
                                   .limit(dozenOfRecords)
                                   .build();
            var sortBy = query.sorting()
                              .get(0);
            assertThat(sortBy).isEqualTo(new SortBy<>(daysSinceStarted(), DESC));
            assertThat(query.limit()).isEqualTo(dozenOfRecords);
        }

        @Test
        @DisplayName("which return the same query builder instance if asked")
        void returnSameBuilder() {
            var builder = ProjectView.query()
                                     .status().is(STARTED)
                                     .daysSinceStarted().isGreaterOrEqualTo(5);
            var query = builder.build();
            var actualBuilder = query.toBuilder();
            assertThat(actualBuilder).isSameInstanceAs(builder);
        }
    }

    @Nested
    @DisplayName("prevent from")
    final class Prevent {

        @Test
        @DisplayName("building entity queries with the record limit set with no sorting specified")
        void fromUsingLimitWithoutSorting() {
            assertThrows(IllegalStateException.class,
                         () -> ProjectView.query()
                                          .limit(100)
                                          .build());
        }
    }

    @Nested
    @DisplayName("return previously specified entity column values")
    final class ReturnValues {

        @Test
        @DisplayName("of a single identifier parameter")
        void ofId() {
            var value = projectId();
            assertThat(ProjectView.query()
                                  .projectId().is(value)
                                  .whichIds().values()).containsExactly(value);
        }

        @Test
        @DisplayName("of several IDs")
        void ofSeveralIds() {
            var ids = generateIds(3);
            assertThat(ProjectView.query()
                                  .projectId().in(ids)
                                  .whichIds().values()).isEqualTo(ids);
        }

        @Test
        @DisplayName("of the column parameters")
        void ofParameterValues() {
            var statusValue = CREATED;
            var daysSinceStarted = 1;
            var query = ProjectView.query()
                                   .status().is(statusValue)
                                   .daysSinceStarted().isGreaterThan(daysSinceStarted)
                                   .build();
            var rootPredicate = query.subject()
                                     .predicate();
            assertThat(rootPredicate.operator()).isEqualTo(AND);

            assertHasParamValue(rootPredicate, status(), EQUALS, statusValue);
            assertHasParamValue(rootPredicate, daysSinceStarted(), GREATER_THAN, daysSinceStarted);
        }

        @Test
        @DisplayName("of the record limit")
        void ofLimit() {
            var limit = 92;
            assertThat(ProjectView.query()
                                  .limit(limit)
                                  .whichLimit()).isEqualTo(limit);

        }

        @Test
        @DisplayName("of the column sorting directives")
        void ofSorting() {
            assertThat(ProjectView.query()
                                  .sortDescendingBy(daysSinceStarted())
                                  .sortAscendingBy(projectName())
                                  .sorting())
                    .isEqualTo(ImmutableList.of(new SortBy<>(daysSinceStarted(), DESC),
                                                new SortBy<>(projectName(), ASC))
                    );
        }
    }

    @Test
    @DisplayName("allow transforming the built `EntityQuery` instance into an object of choice" +
            " in the same call chain")
    void transform() {
        int predicateSize = ProjectView.query()
                .status().is(CREATED)
                .build((q) -> q.subject()
                               .predicate()
                               .allParams()
                               .size());
        assertThat(predicateSize).isEqualTo(1);
    }
}
