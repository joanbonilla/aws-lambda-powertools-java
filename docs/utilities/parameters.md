---
title: Parameters
description: Utility
---


The parameters utility provides a way to retrieve parameter values from
[AWS Systems Manager Parameter Store](https://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-parameter-store.html) or
[AWS Secrets Manager](https://aws.amazon.com/secrets-manager/). It also provides a base class to create your parameter provider implementation.

**Key features**

* Retrieve one or multiple parameters from the underlying provider
* Cache parameter values for a given amount of time (defaults to 5 seconds)
* Transform parameter values from JSON or base 64 encoded strings

## Install

To install this utility, add the following dependency to your project.

=== "Maven"

    ```xml
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-parameters</artifactId>
        <version>1.8.2</version>
    </dependency>
    ```
=== "Gradle"

    ```groovy
     dependencies {
        ...
        implementation 'software.amazon.lambda:powertools-parameters:1.8.2'
        aspectpath 'software.amazon.lambda:powertools-parameters:1.8.2'
    }
    ```

**IAM Permissions**

This utility requires additional permissions to work as expected. See the table below:

Provider | Function/Method | IAM Permission
------------------------------------------------- | ------------------------------------------------- | ---------------------------------------------------------------------------------
SSM Parameter Store | `SSMProvider.get(String)` `SSMProvider.get(String, Class)`  | `ssm:GetParameter`
SSM Parameter Store | `SSMProvider.getMultiple(String)` | `ssm:GetParametersByPath`
Secrets Manager | `SecretsProvider.get(String)` `SecretsProvider.get(String, Class)` | `secretsmanager:GetSecretValue`

## SSM Parameter Store

You can retrieve a single parameter using SSMProvider.get() and pass the key of the parameter.
For multiple parameters, you can use SSMProvider.getMultiple() and pass the path to retrieve them all.

Alternatively, you can retrieve an instance of a provider and configure its underlying SDK client,
in order to get data from other regions or use specific credentials.

=== "SSMProvider"

    ```java hl_lines="6"
    import software.amazon.lambda.powertools.parameters.SSMProvider;
    import software.amazon.lambda.powertools.parameters.ParamManager;

    public class AppWithSSM implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        // Get an instance of the SSM Provider
        SSMProvider ssmProvider = ParamManager.getSsmProvider();
    
        // Retrieve a single parameter
        String value = ssmProvider.get("/my/parameter");
    
        // Retrieve multiple parameters from a path prefix
        // This returns a Map with the parameter name as key
        Map<String, String> values = ssmProvider.getMultiple("/my/path/prefix");
    
    }
    ```

=== "SSMProvider with an explicit region"

    ```java hl_lines="5 7"
    import software.amazon.lambda.powertools.parameters.SSMProvider;
    import software.amazon.lambda.powertools.parameters.ParamManager;

    public class AppWithSSM implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        SsmClient client = SsmClient.builder().region(Region.EU_CENTRAL_1).build();
        // Get an instance of the SSM Provider
        SSMProvider ssmProvider = ParamManager.getSsmProvider(client);
    
        // Retrieve a single parameter
        String value = ssmProvider.get("/my/parameter");
    
        // Retrieve multiple parameters from a path prefix
        // This returns a Map with the parameter name as key
        Map<String, String> values = ssmProvider.getMultiple("/my/path/prefix");
    
    }
    ```

### Additional arguments

The AWS Systems Manager Parameter Store provider supports two additional arguments for the `get()` and `getMultiple()` methods:

| Option     | Default | Description |
|---------------|---------|-------------|
| **withDecryption()**   | `False` | Will automatically decrypt the parameter. |
| **recursive()** | `False`  | For `getMultiple()` only, will fetch all parameter values recursively based on a path prefix. |

**Example:**

=== "AppWithSSM.java"

    ```java hl_lines="9 12"
    import software.amazon.lambda.powertools.parameters.SSMProvider;
    import software.amazon.lambda.powertools.parameters.ParamManager;

    public class AppWithSSM implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        // Get an instance of the SSM Provider
        SSMProvider ssmProvider = ParamManager.getSsmProvider();
    
        // Retrieve a single parameter and decrypt it
        String value = ssmProvider.withDecryption().get("/my/parameter");
    
        // Retrieve multiple parameters recursively from a path prefix
        Map<String, String> values = ssmProvider.recursive().getMultiple("/my/path/prefix");
    
    }
    ```

## Secrets Manager

For secrets stored in Secrets Manager, use `getSecretsProvider`.

Alternatively, you can retrieve an instance of a provider and configure its underlying SDK client,
in order to get data from other regions or use specific credentials.


=== "SecretsProvider"

    ```java hl_lines="9"
    import software.amazon.lambda.powertools.parameters.SecretsProvider;
    import software.amazon.lambda.powertools.parameters.ParamManager;

    public class AppWithSecrets implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        // Get an instance of the Secrets Provider
        SecretsProvider secretsProvider = ParamManager.getSecretsProvider();
    
        // Retrieve a single secret
        String value = secretsProvider.get("/my/secret");
    
    }
    ```

=== "SecretsProvider with an explicit region"

    ```java hl_lines="5 7"
    import software.amazon.lambda.powertools.parameters.SecretsProvider;
    import software.amazon.lambda.powertools.parameters.ParamManager;

    public class AppWithSecrets implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        SecretsManagerClient client = SecretsManagerClient.builder().region(Region.EU_CENTRAL_1).build();
        // Get an instance of the Secrets Provider
        SecretsProvider secretsProvider = ParamManager.getSecretsProvider(client);
    
        // Retrieve a single secret
        String value = secretsProvider.get("/my/secret");
    
    }
    ```

## Advanced configuration

### Caching

By default, all parameters and their corresponding values are cached for 5 seconds.

You can customize this default value using `defaultMaxAge`. You can also customize this value for each parameter using 
`withMaxAge`.

=== "Provider with default Max age"

    ```java hl_lines="9"
    import software.amazon.lambda.powertools.parameters.SecretsProvider;
    import software.amazon.lambda.powertools.parameters.ParamManager;

    public class AppWithSecrets implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        // Get an instance of the Secrets Provider
        SecretsProvider secretsProvider = ParamManager.getSecretsProvider()
                                                      .defaultMaxAge(10, ChronoUnit.SECONDS);

        String value = secretsProvider.get("/my/secret");
    
    }
    ```

=== "Provider with age for each param"

    ```java hl_lines="8"
    import software.amazon.lambda.powertools.parameters.SecretsProvider;
    import software.amazon.lambda.powertools.parameters.ParamManager;

    public class AppWithSecrets implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        SecretsManagerClient client = SecretsManagerClient.builder().region(Region.EU_CENTRAL_1).build();
        
        SecretsProvider secretsProvider = ParamManager.getSecretsProvider(client);

        String value = secretsProvider.withMaxAge(10, ChronoUnit.SECONDS).get("/my/secret");
    
    }
    ```

### Transform values

Parameter values can be transformed using ```withTransformation(transformerClass)```.
Base64 and JSON transformations are provided. For more complex transformation, you need to specify how to deserialize-

!!! warning "`SSMProvider.getMultiple()` does not support transformation and will return simple Strings."

=== "Base64 Transformation"
    ```java
       String value = provider
                        .withTransformation(Transformer.base64)
                        .get("/my/parameter/b64");
    ```

=== "Complex Transformation"

    ```java
       MyObj object = provider
                        .withTransformation(Transformer.json)
                        .get("/my/parameter/json", MyObj.class);
    ```

## Write your own Transformer

You can write your own transformer, by implementing the `Transformer` interface and the `applyTransformation()` method.
For example, if you wish to deserialize XML into an object.

=== "XmlTransformer.java"

    ```java hl_lines="1"
    public class XmlTransformer<T> implements Transformer<T> {
    
        private final XmlMapper mapper = new XmlMapper();
    
        @Override
        public T applyTransformation(String value, Class<T> targetClass) throws TransformationException {
            try {
                return mapper.readValue(value, targetClass);
            } catch (IOException e) {
                throw new TransformationException(e);
            }
        }
    }
    ```

=== "Using XmlTransformer"

    ```java
        MyObj object = provider
                            .withTransformation(XmlTransformer.class)
                            .get("/my/parameter/xml", MyObj.class);
    ```

### Fluent API

To simplify the use of the library, you can chain all method calls before a get.

=== "Fluent API call"

    ```java
        ssmProvider
          .defaultMaxAge(10, SECONDS)     // will set 10 seconds as the default cache TTL
          .withMaxAge(1, MINUTES)         // will set the cache TTL for this value at 1 minute
          .withTransformation(json)       // json is a static import from Transformer.json
          .withDecryption()               // enable decryption of the parameter value
          .get("/my/param", MyObj.class); // finally get the value
    ```

## Create your own provider

You can create your own custom parameter store provider by inheriting the ```BaseProvider``` class and implementing the
```String getValue(String key)``` method to retrieve data from your underlying store. All transformation and caching logic is handled by the get() methods in the base class.

=== "Example implementation using S3 as a custom parameter"

    ```java
    public class S3Provider extends BaseProvider {
    
        private final S3Client client;
        private String bucket;
    
        S3Provider(CacheManager cacheManager) {
            this(cacheManager, S3Client.create());
        }
    
        S3Provider(CacheManager cacheManager, S3Client client) {
            super(cacheManager);
            this.client = client;
        }
    
        public S3Provider withBucket(String bucket) {
            this.bucket = bucket;
            return this;
        }
    
        @Override
        protected String getValue(String key) {
            if (bucket == null) {
                throw new IllegalStateException("A bucket must be specified, using withBucket() method");
            }
    
            GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key).build();
            ResponseBytes<GetObjectResponse> response = client.getObject(request, ResponseTransformer.toBytes());
            return response.asUtf8String();
        }
    
        @Override
        protected Map<String, String> getMultipleValues(String path) {
            if (bucket == null) {
                throw new IllegalStateException("A bucket must be specified, using withBucket() method");
            }
    
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder().bucket(bucket).prefix(path).build();
            List<S3Object> s3Objects = client.listObjectsV2(listRequest).contents();
    
            Map<String, String> result = new HashMap<>();
            s3Objects.forEach(s3Object -> {
                result.put(s3Object.key(), getValue(s3Object.key()));
            });
    
            return result;
        }
    
        @Override
        protected void resetToDefaults() {
            super.resetToDefaults();
            bucket = null;
        }
    
    }
    ```

=== "Using custom parameter store"

    ```java hl_lines="3"
        S3Provider provider = new S3Provider(ParamManager.getCacheManager());

        provider.setTransformationManager(ParamManager.getTransformationManager());

        String value = provider.withBucket("myBucket").get("myKey");
    ```

## Annotation

You can make use of the annotation `@Param` to inject a parameter value in a variable.

By default, it will use `SSMProvider` to retrieve the value from AWS System Manager Parameter Store.
You could specify a different provider as long as it extends `BaseProvider` and/or a `Transformer`.

=== "Param Annotation"

    ```java hl_lines="3"
    public class AppWithAnnotation implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        @Param(key = "/my/parameter/json")
        ObjectToDeserialize value;
    
    }
    ```

=== "Custom Provider Usage"
    
    ```java hl_lines="3"
    public class AppWithAnnotation implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        @Param(key = "/my/parameter/json" provider = SecretsProvider.class, transformer = JsonTransformer.class)
        ObjectToDeserialize value;
    
    }
    ```

    In this case ```SecretsProvider``` will be used to retrieve a raw value that is then trasformed into the target Object by using ```JsonTransformer```.
    To show the convenience of the annotation compare the following two code snippets.


### Install

If you want to use the ```@Param``` annotation in your project add configuration to compile-time weave (CTW) the powertools-parameters aspects into your project.

=== "Maven"

    ```xml
    <build>
        <plugins>
            ...
            <plugin>
                 <groupId>org.codehaus.mojo</groupId>
                 <artifactId>aspectj-maven-plugin</artifactId>
                 <version>1.14.0</version>
                 <configuration>
                     ...
                     <aspectLibraries>
                         ...
                         <aspectLibrary>
                             <groupId>software.amazon.lambda</groupId>
                             <artifactId>powertools-parameters</artifactId>
                         </aspectLibrary>
                     </aspectLibraries>
                 </configuration>
                 <executions>
                     <execution>
                         <goals>
                             <goal>compile</goal>
                         </goals>
                     </execution>
                 </executions>
            </plugin>
            ...
        </plugins>
    </build>
    ```
    
=== "Gradle"

    ```groovy
    plugins{
        id 'java'
        id 'aspectj.AspectjGradlePlugin' version '0.0.6'
    }
    repositories {
        jcenter()
    }
    dependencies {
        ...
        implementation 'software.amazon.lambda:powertools-parameters:1.8.2'
        aspectpath 'software.amazon.lambda:powertools-parameters:1.8.2'
    }
    ```

    **Note:**
    
    Please add `aspectjVersion = '1.9.6'` to the `gradle.properties` file. The aspectj plugin works at the moment with gradle 5.x only if
    you are using `java 8` as runtime. Please refer to [open issue](https://github.com/awslabs/aws-lambda-powertools-java/issues/146) for more details.