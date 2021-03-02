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

package org.springframework.scheduling.timer;

import junit.framework.TestCase;
import org.springframework.test.AssertThrows;

import java.util.Timer;

/**
 * Unit tests for the {@link org.springframework.scheduling.timer.TimerTaskExecutor} class.
 *
 * @author Rick Evans
 */
public final class TimerTaskExecutorTests extends TestCase {

    public void testExecuteChokesWithNullTimer() throws Exception {
        new AssertThrows(IllegalArgumentException.class) {
            public void test() throws Exception {
                TimerTaskExecutor executor = new TimerTaskExecutor();
                executor.execute(new NoOpRunnable());
            }
        }.runTest();
    }

    public void testExecuteChokesWithNullTask() throws Exception {
        new AssertThrows(IllegalArgumentException.class) {
            public void test() throws Exception {
                TimerTaskExecutor executor = new TimerTaskExecutor(new Timer());
                executor.execute(null);
            }
        }.runTest();
    }

    public void testExecuteChokesWithNegativeDelay() throws Exception {
        new AssertThrows(IllegalArgumentException.class) {
            public void test() throws Exception {
                TimerTaskExecutor executor = new TimerTaskExecutor(new Timer());
                executor.setDelay(-10);
                executor.execute(new NoOpRunnable());
            }
        }.runTest();
    }

    public void testExecuteReallyDoesScheduleTheSuppliedTask() throws Exception {
        final Object monitor = new Object();

        RunAwareRunnable task = new RunAwareRunnable(monitor);

        TimerTaskExecutor executor = new TimerTaskExecutor(new Timer());
        executor.execute(task);

        synchronized (monitor) {
            monitor.wait(5000);
        }

        assertTrue("Supplied task (a Runnable) is not being invoked.", task.isRunWasCalled());
    }

    public void testCtorWithNullTimer() throws Exception {
        new AssertThrows(IllegalArgumentException.class) {
            public void test() throws Exception {
                new TimerTaskExecutor(null);
            }
        }.runTest();
    }

    public void testCreateTimerMethodIsCalledIfNoTimerIsExplicitlySupplied() throws Exception {
        CreationAwareTimerTaskExecutor executor = new CreationAwareTimerTaskExecutor();
        executor.afterPropertiesSet();
        assertTrue("If no Timer is set explicitly, then the protected createTimer() " +
                "method must be called to create the Timer (it obviously isn't being called).",
                executor.isCreateTimerWasCalled());
    }

    public void testCreateTimerMethodIsNotCalledIfTimerIsExplicitlySupplied() throws Exception {
        CreationAwareTimerTaskExecutor executor = new CreationAwareTimerTaskExecutor();
        executor.setTimer(new Timer());
        executor.afterPropertiesSet();
        assertFalse("If a Timer is set explicitly, then the protected createTimer() " +
                "method must not be called to create the Timer (it obviously is being called, in error).",
                executor.isCreateTimerWasCalled());
    }

    public void testThatTheDestroyCallbackCancelsTheTimerIfNoTimerIsExplicitlySupplied() throws Exception {

        final CancelAwareTimer timer = new CancelAwareTimer();

        TimerTaskExecutor executor = new TimerTaskExecutor() {

            protected Timer createTimer() {
                return timer;
            }
        };
        executor.afterPropertiesSet();
        executor.destroy();
        assertTrue("When the Timer used is created by the TimerTaskExecutor because " +
                "no Timer was set explicitly, then the destroy() callback must cancel() said Timer (it obviously isn't doing this).",
                timer.isCancelWasCalled());
    }

    public void testThatTheDestroyCallbackDoesNotCancelTheTimerIfTheTimerWasSuppliedExplictly() throws Exception {
        TimerTaskExecutor executor = new TimerTaskExecutor();
        CancelAwareTimer timer = new CancelAwareTimer();
        executor.setTimer(timer);
        executor.afterPropertiesSet();
        executor.destroy();
        assertFalse("When the Timer used is not created by the TimerTaskExecutor because " +
                "it Timer was set explicitly, then the destroy() callback must NOT cancel() said Timer (it obviously is, in error).",
                timer.isCancelWasCalled());
    }


    private final static class CreationAwareTimerTaskExecutor extends TimerTaskExecutor {

        private boolean createTimerWasCalled = false;


        public boolean isCreateTimerWasCalled() {
            return this.createTimerWasCalled;
        }

        protected Timer createTimer() {
            this.createTimerWasCalled = true;
            return super.createTimer();
        }

    }

    private static class CancelAwareTimer extends Timer {

        private boolean cancelWasCalled;


        public boolean isCancelWasCalled() {
            return this.cancelWasCalled;
        }


        public void cancel() {
            this.cancelWasCalled = true;
            super.cancel();
        }
    }

    private static class RunAwareRunnable implements Runnable {
        private boolean runWasCalled;
        private final Object monitor;

        public RunAwareRunnable(Object monitor) {
            this.monitor = monitor;
        }


        public boolean isRunWasCalled() {
            return this.runWasCalled;
        }


        public void run() {
            this.runWasCalled = true;
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }
    }

    private static final class NoOpRunnable implements Runnable {

        public void run() {
            // explicit no-op
        }
    }

}
