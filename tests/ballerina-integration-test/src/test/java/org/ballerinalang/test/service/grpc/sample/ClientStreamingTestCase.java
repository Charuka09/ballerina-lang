/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.test.service.grpc.sample;

import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.BRunUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.types.BTypes;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.model.values.BValueArray;
import org.ballerinalang.test.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test class for gRPC client streaming service with non-blocking client.
 */
@Test(groups = "grpc-test")
public class ClientStreamingTestCase extends GrpcBaseTest {

    @BeforeClass
    private void setup() throws Exception {
        TestUtils.prepareBalo(this);
    }

    @Test
    public void testNonBlockingBallerinaClient() {

        Path balFilePath = Paths.get("src", "test", "resources", "grpc", "clients", "client_streaming_client.bal");
        CompileResult result = BCompileUtil.compile(balFilePath.toAbsolutePath().toString());
        BValueArray stringArray = new BValueArray(BTypes.typeString);
        stringArray.add(0, "Hi Sam");
        stringArray.add(1, "Hey Sam");
        stringArray.add(1, "GM Sam");
        final String serverMsg = "Ack";

        BValue[] responses = BRunUtil.invoke(result, "testClientStreaming", new BValue[]{stringArray});
        Assert.assertEquals(responses.length, 1);
        Assert.assertTrue(responses[0] instanceof BString);
        BString response = (BString) responses[0];
        Assert.assertEquals(response.stringValue(), serverMsg);
    }
}
