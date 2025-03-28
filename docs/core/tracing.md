---
title: Tracing
description: Core utility
---

Powertools tracing is an opinionated thin wrapper for [AWS X-Ray Java SDK](https://github.com/aws/aws-xray-sdk-java/)
a provides functionality to reduce the overhead of performing common tracing tasks.

![Tracing showcase](../media/tracing_utility_showcase.png)

 **Key Features**

 * Capture cold start as annotation, and responses as well as full exceptions as metadata
 * Helper methods to improve the developer experience of creating new X-Ray subsegments.
 * Better developer experience when developing with multiple threads.
 * Auto patch supported modules by AWS X-Ray

Initialization

Before your use this utility, your AWS Lambda function [must have permissions](https://docs.aws.amazon.com/lambda/latest/dg/services-xray.html#services-xray-permissions) to send traces to AWS X-Ray.

> Example using AWS Serverless Application Model (SAM)

=== "template.yaml"

    ```yaml hl_lines="8 11"
    Resources:
        HelloWorldFunction:
            Type: AWS::Serverless::Function
            Properties:
            ...
            Runtime: java8
    
            Tracing: Active
            Environment:
                Variables:
                    POWERTOOLS_SERVICE_NAME: example
    ```

The Powertools service name is used as the X-Ray namespace. This can be set using the environment variable
`POWERTOOLS_SERVICE_NAME`

### Lambda handler

To enable Powertools tracing to your function add the `@Tracing` annotation to your `handleRequest` method or on
any method will capture the method as a separate subsegment automatically. You can optionally choose to customize 
segment name that appears in traces.

=== "Tracing annotation"

    ```java hl_lines="3 10 15"
    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        @Tracing
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            businessLogic1();
    
            businessLogic2();
        }
    
        @Tracing
        public void businessLogic1(){
    
        }
    
        @Tracing
        public void businessLogic2(){
    
        }
    }
    ```

=== "Custom Segment names"

    ```java hl_lines="3"
    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        @Tracing(segmentName="yourCustomName")
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        ...
        }
    ```

When using this `@Tracing` annotation, Utility performs these additional tasks to ease operations:

  * Creates a `ColdStart` annotation to easily filter traces that have had an initialization overhead.
  * Creates a `Service` annotation if service parameter or `POWERTOOLS_SERVICE_NAME` is set.
  * Captures any response, or full exceptions generated by the handler, and include as tracing metadata.


By default, this annotation will automatically record method responses and exceptions. You can change the default behavior by setting
the environment variables `POWERTOOLS_TRACER_CAPTURE_RESPONSE` and `POWERTOOLS_TRACER_CAPTURE_ERROR` as needed. Optionally, you can override behavior by
different supported `captureMode` to record response, exception or both.

!!! warning "Returning sensitive information from your Lambda handler or functions, where `Tracing` is used?"
    You can disable annotation from capturing their responses and exception as tracing metadata with **`captureMode=DISABLED`**
    or globally by setting environment variables **`POWERTOOLS_TRACER_CAPTURE_RESPONSE`** and **`POWERTOOLS_TRACER_CAPTURE_ERROR`** to **`false`**

=== "Disable on annotation"

    ```java hl_lines="3"
    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        @Tracing(captureMode=CaptureMode.DISABLED)
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        ...
        }
    ```

=== "Disable Globally"

    ```yaml hl_lines="11 12"
    Resources:
        HelloWorldFunction:
            Type: AWS::Serverless::Function
            Properties:
            ...
            Runtime: java8
    
            Tracing: Active
            Environment:
                Variables:
                    POWERTOOLS_TRACER_CAPTURE_RESPONSE: false
                    POWERTOOLS_TRACER_CAPTURE_ERROR: false
    ```

### Annotations & Metadata

**Annotations** are key-values associated with traces and indexed by AWS X-Ray. You can use them to filter traces and to
create [Trace Groups](https://aws.amazon.com/about-aws/whats-new/2018/11/aws-xray-adds-the-ability-to-group-traces/) to slice and dice your transactions.

**Metadata** are key-values also associated with traces but not indexed by AWS X-Ray. You can use them to add additional 
context for an operation using any native object.

=== "Annotations"

    You can add annotations using `putAnnotation()` method from TracingUtils
    ```java hl_lines="8"
    import software.amazon.lambda.powertools.tracing.Tracing;
    import software.amazon.lambda.powertools.tracing.TracingUtils;

    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        @Tracing
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            TracingUtils.putAnnotation("annotation", "value");
        }
    }
    ```

=== "Metadata"

    You can add metadata using `putMetadata()` method from TracingUtils
    ```java hl_lines="8"
    import software.amazon.lambda.powertools.tracing.Tracing;
    import software.amazon.lambda.powertools.tracing.TracingUtils;

    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        @Tracing
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            TracingUtils.putMetadata("content", "value");
        }
    }
    ```

## Utilities

Tracing modules comes with certain utility method when you don't want to use annotation for capturing a code block
under a subsegment, or you are doing multithreaded programming. Refer examples below.

=== "Functional Api"

    ```java hl_lines="7 8 9 11 12 13"
    import software.amazon.lambda.powertools.tracing.Tracing;
    import software.amazon.lambda.powertools.tracing.TracingUtils;
    
    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
             TracingUtils.withSubsegment("loggingResponse", subsegment -> {
                // Some business logic
             });
    
             TracingUtils.withSubsegment("localNamespace", "loggingResponse", subsegment -> {
                // Some business logic
             });
        }
    }
    ```

=== "Multi Threaded Programming"

    ```java hl_lines="7 9 10 11"
    import static software.amazon.lambda.powertools.tracing.TracingUtils.withEntitySubsegment;

    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            // Extract existing trace data
            Entity traceEntity = AWSXRay.getTraceEntity();
    
            Thread anotherThread = new Thread(() -> withEntitySubsegment("inlineLog", traceEntity, subsegment -> {
                // Business logic in separate thread
            }));
        }
    }
    ```

## Instrumenting SDK clients and HTTP calls

User should make sure to instrument the SDK clients explicitly based on the function dependency. Refer details on
[how to instrument SDK client with Xray](https://docs.aws.amazon.com/xray/latest/devguide/xray-sdk-java-awssdkclients.html) and [outgoing http calls](https://docs.aws.amazon.com/xray/latest/devguide/xray-sdk-java-httpclients.html).
