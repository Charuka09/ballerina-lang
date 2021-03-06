// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/time;

public function buildStreamEvent(any t, string streamName) returns StreamEvent?[] {
    EventType evntType = "CURRENT";
    var keyVals = <map<anydata>>map<anydata>.stamp(t.clone());
    StreamEvent event = new((streamName, keyVals), evntType, time:currentTime().time);
    StreamEvent?[] streamEvents = [event];
    return streamEvents;
}

public function createResetStreamEvent(StreamEvent event) returns StreamEvent {
    StreamEvent resetStreamEvent = new(event.data, "RESET", event.timestamp);
    return resetStreamEvent;
}

public function getStreamEvent(any? anyEvent) returns StreamEvent {
    return <StreamEvent>anyEvent;
}
