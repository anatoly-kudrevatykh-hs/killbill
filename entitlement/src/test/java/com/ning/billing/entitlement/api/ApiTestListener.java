/*
 * Copyright 2010-2011 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.billing.entitlement.api;

import com.google.common.base.Joiner;
import com.google.common.eventbus.Subscribe;
import com.ning.billing.entitlement.api.user.SubscriptionTransition;
import com.ning.billing.util.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class ApiTestListener {

    private static final Logger log = LoggerFactory.getLogger(ApiTestListener.class);

    private final List<NextEvent> nextExpectedEvent;

    private volatile boolean completed;

    public enum NextEvent {
        MIGRATE_ENTITLEMENT,
        CREATE,
        CHANGE,
        CANCEL,
        PAUSE,
        RESUME,
        PHASE
    }

    public ApiTestListener(EventBus eventBus) {
        this.nextExpectedEvent = new Stack<NextEvent>();
        this.completed = false;
    }

    @Subscribe
    public void handleEntitlementEvent(SubscriptionTransition event) {
        switch (event.getTransitionType()) {
        case MIGRATE_ENTITLEMENT:
            subscriptionMigrated(event);
            break;
        case CREATE:
            subscriptionCreated(event);
            break;
        case CANCEL:
            subscriptionCancelled(event);
            break;
        case CHANGE:
            subscriptionChanged(event);
            break;
        case PAUSE:
            subscriptionPaused(event);
            break;
        case RESUME:
            subscriptionResumed(event);
            break;
        case UNCANCEL:
            break;
        case PHASE:
            subscriptionPhaseChanged(event);
            break;
        default:
            throw new RuntimeException("Unexpected event type " + event.getRequestedTransitionTime());
        }

    }

    public void pushExpectedEvent(NextEvent next) {
        synchronized (this) {
            nextExpectedEvent.add(next);
            completed = false;
        }
    }

    public boolean isCompleted(long timeout) {
        synchronized (this) {
            try {
                wait(timeout);
            } catch (Exception ignore) {
            }
        }
        if (!completed) {
            log.debug("ApiTestListener did not complete in " + timeout + " ms");
        }
        return completed;
    }

    public void reset() {
        nextExpectedEvent.clear();
    }

    private void notifyIfStackEmpty() {
        log.debug("notifyIfStackEmpty ENTER");
        synchronized (this) {
            if (nextExpectedEvent.isEmpty()) {
                log.debug("notifyIfStackEmpty EMPTY");
                completed = true;
                notify();
            }
        }
        log.debug("notifyIfStackEmpty EXIT");
    }

    private void assertEqualsNicely(NextEvent expected) {

        boolean foundIt = false;
        Iterator<NextEvent> it = nextExpectedEvent.iterator();
        while (it.hasNext()) {
            NextEvent ev = it.next();
            if (ev == expected) {
                it.remove();
                foundIt = true;
                break;
            }
        }

        if (!foundIt) {
            Joiner joiner = Joiner.on(" ");
            System.err.println("Expected event " + expected + " got " + joiner.join(nextExpectedEvent));
            System.exit(1);
        }
    }


    public void subscriptionMigrated(SubscriptionTransition migrated) {
        log.debug("-> Got event MIGRATED");
        assertEqualsNicely(NextEvent.MIGRATE_ENTITLEMENT);
        notifyIfStackEmpty();
    }

    public void subscriptionCreated(SubscriptionTransition created) {
        log.debug("-> Got event CREATED");
        assertEqualsNicely(NextEvent.CREATE);
        notifyIfStackEmpty();
    }


    public void subscriptionCancelled(SubscriptionTransition cancelled) {
        log.debug("-> Got event CANCEL");
        assertEqualsNicely(NextEvent.CANCEL);
        notifyIfStackEmpty();
    }


    public void subscriptionChanged(SubscriptionTransition changed) {
        log.debug("-> Got event CHANGE");
        assertEqualsNicely(NextEvent.CHANGE);
        notifyIfStackEmpty();
    }


    public void subscriptionPaused(SubscriptionTransition paused) {
        log.debug("-> Got event PAUSE");
        assertEqualsNicely(NextEvent.PAUSE);
        notifyIfStackEmpty();
    }


    public void subscriptionResumed(SubscriptionTransition resumed) {
        log.debug("-> Got event RESUME");
        assertEqualsNicely(NextEvent.RESUME);
        notifyIfStackEmpty();
    }


    public void subscriptionPhaseChanged(
            SubscriptionTransition phaseChanged) {
        log.debug("-> Got event PHASE");
        assertEqualsNicely(NextEvent.PHASE);
        notifyIfStackEmpty();
    }
}
