/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.ballerinalang.test.object;

import org.ballerinalang.launcher.util.BAssertUtil;
import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.BRunUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.values.BInteger;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.util.BLangConstants;
import org.ballerinalang.util.exceptions.BLangRuntimeException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test cases for user defined object types in ballerina.
 */
public class ObjectAccessWithIndexTest {

    private CompileResult compileResult;
    private CompileResult negativeResult;

    @BeforeClass
    public void setup() {
        compileResult = BCompileUtil.compile("test-src/object/object-access-with-index.bal");
        negativeResult = BCompileUtil.compile("test-src/object/object-access-with-index-negative.bal");
    }

    @Test(description = "Test Basic object operations")
    public void testBasicObject() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testCreateObject");

        Assert.assertTrue(returns[0] instanceof BString);
        Assert.assertEquals(returns[0].stringValue(), "Jack");

        Assert.assertTrue(returns[1] instanceof BMap);
        BMap<String, ?> adrsMap = ((BMap) returns[1]);
        Assert.assertEquals(adrsMap.get("country"), new BString("USA"));
        Assert.assertEquals(adrsMap.get("state"), new BString("CA"));

        Assert.assertTrue(returns[2] instanceof BInteger);
        Assert.assertEquals(((BInteger) returns[2]).intValue(), 25);
    }

    @Test(description = "Test using expressions as index for object arrays")
    public void testExpressionAsIndex() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testExpressionAsIndex");
        Assert.assertTrue(returns[0] instanceof BString);
        Assert.assertEquals(returns[0].stringValue(), "Jane");
    }

    @Test(description = "Test using objects inside objects")
    public void testObjectOfObjects() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testObjectOfObject");

        Assert.assertTrue(returns[0] instanceof BString);
        Assert.assertEquals(returns[0].stringValue(), "USA");
    }

    @Test(description = "Test returning fields of a object")
    public void testReturnObjectAttributes() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testReturnObjectAttributes");

        Assert.assertTrue(returns[0] instanceof BString);
        Assert.assertEquals(returns[0].stringValue(), "emily");
    }

    @Test(description = "Test using object expression as a index in another object expression")
    public void testObjectExpressionAsIndex() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testObjectExpressionAsIndex");
        Assert.assertTrue(returns[0] instanceof BString);
        Assert.assertEquals(returns[0].stringValue(), "emily");
    }

    @Test(description = "Test default value of a object field")
    public void testDefaultValue() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testDefaultVal");

        // Check default value of a field where the default value is set
        Assert.assertTrue(returns[0] instanceof BString);
        Assert.assertEquals(returns[0].stringValue(), "default first name");

        // Check the default value of a field where the default value is not set
        Assert.assertTrue(returns[1] instanceof BString);
        Assert.assertEquals(returns[1].stringValue(), BLangConstants.STRING_EMPTY_VALUE);

        Assert.assertTrue(returns[2] instanceof BInteger);
        Assert.assertEquals(((BInteger) returns[2]).intValue(), 999);
    }

    @Test(description = "Test default value of a nested object field")
    public void testNestedFieldDefaultValue() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testNestedFieldDefaultVal");

        Assert.assertTrue(returns[0] instanceof BString);
        Assert.assertEquals(returns[0].stringValue(), "default first name");

        Assert.assertTrue(returns[1] instanceof BString);
        Assert.assertEquals(returns[1].stringValue(), "Smith");

        Assert.assertTrue(returns[2] instanceof BInteger);
        Assert.assertEquals(((BInteger) returns[2]).intValue(), 999);
    }

    // Negative tests

    @Test(description = "Test accessing an undeclared object")
    public void testUndeclaredObjectAccess() {
        BAssertUtil.validateError(negativeResult, 0, "undefined symbol 'dpt1'", 3, 5);
    }

    @Test(description = "Test accessing an undeclared field of a object")
    public void testUndeclaredFieldAccess() {
        BAssertUtil.validateError(negativeResult, 2, "undefined field 'id' in object 'Department'", 9, 5);
    }

    @Test(description = "Test accesing a object with a dynamic index")
    public void testExpressionAsObjectIndex() {
        CompileResult compileResult = BCompileUtil.compile("test-src/object/object-access-dynamic-index-negative.bal");
        Assert.assertEquals(compileResult.getWarnCount(), 0);
        Assert.assertEquals(compileResult.getErrorCount(), 1);
        Assert.assertEquals(compileResult.getDiagnostics()[0].getMessage(),
                "invalid index expression: expected string literal");
    }

    @Test(description = "Test accessing an field of a noninitialized object",
            expectedExceptions = { BLangRuntimeException.class },
            expectedExceptionsMessageRegExp = "error:.*array index out of range: index: 0, size: 0.*")
    public void testGetNonInitField() {
        BRunUtil.invoke(compileResult, "testGetNonInitAttribute");
    }

    @Test(description = "Test accessing an arrays field of a noninitialized object",
            expectedExceptions = { BLangRuntimeException.class },
            expectedExceptionsMessageRegExp = "error:.*array index out of range: index: 0, size: 0.*")
    public void testGetNonInitArrayField() {
        BRunUtil.invoke(compileResult, "testGetNonInitArrayAttribute");
    }

    @Test(description = "Test accessing the field of a noninitialized object",
            expectedExceptions = { BLangRuntimeException.class },
            expectedExceptionsMessageRegExp = "error:.*array index out of range: index: 0, size: 0.*")
    public void testGetNonInitLastField() {
        BRunUtil.invoke(compileResult, "testGetNonInitLastAttribute");
    }

    @Test(description = "Test setting an field of a noninitialized child object")
    public void testSetNonInitField() {
        BRunUtil.invoke(compileResult, "testSetFieldOfNonInitChildObject");
    }

    @Test(description = "Test setting the field of a noninitialized root object")
    public void testSetNonInitLastField() {
        BRunUtil.invoke(compileResult, "testSetFieldOfNonInitObject");
    }
}
