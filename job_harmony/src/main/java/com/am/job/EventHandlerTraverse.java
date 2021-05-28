/*
 * Copyright (C) 2021 AlexMofer
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
package com.am.job;

import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;
import ohos.eventhandler.InnerEvent;

import java.util.ArrayList;

/**
 * 任务线程通信
 * Created by Alex on 2021/5/28.
 */
class EventHandlerTraverse implements BaseJob.Traverse {

    private static final int MSG_PROGRESS = 1001;
    private static final int MSG_RESULT = 1002;
    private static final ArrayList<Tag> TAGS = new ArrayList<>();
    private static EventHandlerTraverse MAIN_TRAVERSE = null;
    private final InnerEventHandler mHandler;

    private EventHandlerTraverse(EventRunner runner) {
        mHandler = new InnerEventHandler(runner);
    }

    static EventHandlerTraverse getMainTraverse() {
        if (MAIN_TRAVERSE == null) {
            MAIN_TRAVERSE = new EventHandlerTraverse(EventRunner.getMainEventRunner());
        }
        return MAIN_TRAVERSE;
    }

    private static Tag getTag(Object callback, Object value) {
        final Tag progress;
        synchronized (TAGS) {
            if (TAGS.isEmpty()) {
                progress = new Tag();
            } else {
                progress = TAGS.remove(0);
            }
        }
        progress.put(callback, value);
        return progress;
    }

    private static void putTag(Tag tag) {
        tag.clear();
        synchronized (TAGS) {
            TAGS.add(tag);
        }
    }

    @Override
    public void publishResult(ResultCallback resultCallback, BaseJob.Result result) {
        mHandler.sendEvent(InnerEvent.get(MSG_RESULT, MSG_RESULT, getTag(resultCallback, result)));
    }

    @Override
    public void publishProgress(ProgressCallback progressCallback, BaseJob.Progress progress) {
        mHandler.sendEvent(InnerEvent.get(MSG_PROGRESS, MSG_PROGRESS, getTag(progressCallback, progress)));
    }

    private void handleEvent(InnerEvent event) {
        if (event.object instanceof Tag) {
            final Tag tag = (Tag) event.object;
            switch (event.eventId) {
                case MSG_RESULT:
                    tag.<ResultCallback>getCallback().dispatchResult(tag.getValue());
                    break;
                case MSG_PROGRESS:
                    tag.<ProgressCallback>getCallback().dispatchProgress(tag.getValue());
                    break;
            }
            putTag(tag);
        }
    }

    private static class Tag {
        private Object mCallback;
        private Object mValue;

        void put(Object callback, Object value) {
            mCallback = callback;
            mValue = value;
        }

        <V> V getCallback() {
            //noinspection unchecked
            return (V) mCallback;
        }

        <V> V getValue() {
            //noinspection unchecked
            return (V) mValue;
        }

        private void clear() {
            mCallback = null;
            mValue = null;
        }
    }

    private class InnerEventHandler extends EventHandler {

        InnerEventHandler(EventRunner runner) throws IllegalArgumentException {
            super(runner);
        }

        @Override
        protected void processEvent(InnerEvent event) {
            super.processEvent(event);
            if (event == null) {
                return;
            }
            handleEvent(event);
        }
    }
}
