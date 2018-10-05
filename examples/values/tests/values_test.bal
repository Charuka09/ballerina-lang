import ballerina/test;
import ballerina/io;

any[] outputs = [];
int counter = 0;

// This is the mock function which will replace the real function
@test:Mock {
    packageName: "ballerina/io",
    functionName: "println"
}
public function mockPrint(any... s) {
    outputs[counter] = s[0];
    counter++;
}

@test:Config
function testFunc() {
    // Invoking the main function
    main();
    test:assertEquals(outputs[0], 10);
    test:assertEquals(outputs[1], 20.0);
    test:assertEquals(outputs[2], 23);
    test:assertEquals(outputs[3], "Ballerina");
    test:assertEquals(outputs[4], true);
}