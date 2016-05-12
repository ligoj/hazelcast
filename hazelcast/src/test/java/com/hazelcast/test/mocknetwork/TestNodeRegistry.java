/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hazelcast.test.mocknetwork;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.LifecycleService;
import com.hazelcast.instance.Node;
import com.hazelcast.instance.NodeContext;
import com.hazelcast.instance.NodeState;
import com.hazelcast.nio.Address;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.assertEquals;

public final class TestNodeRegistry {

    private final ConcurrentMap<Address, Node> nodes = new ConcurrentHashMap<Address, Node>(10);
    private final Collection<Address> joinAddresses;

    public TestNodeRegistry(Collection<Address> addresses) {
        this.joinAddresses = addresses;
    }

    public NodeContext createNodeContext(Address address) {
        Node node;
        if ((node = nodes.get(address)) != null) {
            assertEquals("This address is already in registry! " + address, NodeState.SHUT_DOWN, node.getState());
            nodes.remove(address, node);
        }
        return new MockNodeContext(this, address);
    }

    public HazelcastInstance getInstance(Address address) {
        Node node = nodes.get(address);
        return node != null && node.isRunning() ? node.hazelcastInstance : null;
    }

    public boolean removeInstance(Address address) {
        return nodes.remove(address) != null;
    }

    public Collection<HazelcastInstance> getAllHazelcastInstances() {
        Collection<HazelcastInstance> all = new LinkedList<HazelcastInstance>();
        for (Node node : nodes.values()) {
            if (node.isRunning()) {
                all.add(node.hazelcastInstance);
            }
        }
        return all;
    }

    public void shutdown() {
        shutdown(false);
    }

    public void terminate() {
        shutdown(true);
    }

    private void shutdown(boolean terminate) {
        Iterator<Node> iterator = nodes.values().iterator();
        while (iterator.hasNext()) {
            Node node = iterator.next();
            HazelcastInstance hz = node.hazelcastInstance;
            LifecycleService lifecycleService = hz.getLifecycleService();
            if (terminate) {
                lifecycleService.terminate();
            } else {
                lifecycleService.shutdown();
            }
            iterator.remove();
        }
    }

    Map<Address, Node> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    Node getNode(Address address) {
        return nodes.get(address);
    }

    Collection<Address> getJoinAddresses() {
        return Collections.unmodifiableCollection(joinAddresses);
    }

    void registerNode(Node node) {
        Address address = node.getThisAddress();
        Node currentNode = nodes.putIfAbsent(address, node);
        if (currentNode != null) {
            assertEquals("This address is already in registry! " + address, currentNode, node);
        }
    }

    Collection<Address> getAddresses() {
        return Collections.unmodifiableCollection(nodes.keySet());
    }
}