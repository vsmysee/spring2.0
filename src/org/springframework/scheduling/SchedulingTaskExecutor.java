/*
 * Copyright 2002-2006 the original author or authors.
 *
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
 */

package org.springframework.scheduling;

import org.springframework.core.task.TaskExecutor;

/**
 * Extension of the core TaskExecutor interface, exposing scheduling
 * characteristics that are relevant to potential task submitters.
 *
 * <p>Scheduling clients are encouraged to submit Runnables that match
 * the exposed preferences of the TaskExecutor implementation in use.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.scheduling.SchedulingAwareRunnable
 * @see org.springframework.core.task.TaskExecutor
 * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
 */
public interface SchedulingTaskExecutor extends TaskExecutor {

	/**
	 * Return whether this TaskExecutor prefers short-lived tasks
	 * (<code>true</code>) over long-lived ones (<code>false</code>).
	 * <p>A SchedulingTaskExecutor implementation can indicate whether it prefers
	 * submitted tasks to perform as little work as they can within a single task
	 * execution. For example, submitted tasks might break a repeated loop into
	 * individual subtasks which submit a follow-up task afterwards (if feasible).
	 * <p>This should be considered a hint. Of course TaskExecutor clients are
	 * free to ignore this flag and hence the SchedulingTaskExecutor interface overall.
	 * However, thread pools will usually indicated a preference for short-lived
	 * tasks, to be able to perform more fine-grained scheduling.
	 */
	boolean prefersShortLivedTasks();

}
