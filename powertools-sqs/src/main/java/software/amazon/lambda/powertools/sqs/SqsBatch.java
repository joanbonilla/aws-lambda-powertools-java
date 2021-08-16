package software.amazon.lambda.powertools.sqs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.*;

/**
 * {@link SqsBatch} is used to process batch messages in {@link SQSEvent}
 *
 * <p>
 * When using the annotation, implementation of {@link SqsMessageHandler} is required. Annotation will take care of
 * calling {@link SqsMessageHandler#process(SQSMessage)} method for each {@link SQSMessage} in the received {@link SQSEvent}
 * </p>
 *
 * <p>
 * If any exception is thrown from {@link SqsMessageHandler#process(SQSMessage)} during processing of a messages, Utility
 * will take care of deleting all the successful messages from SQS. When one or more single message fails processing due
 * to exception thrown from {@link SqsMessageHandler#process(SQSMessage)}, Lambda execution will fail
 * with {@link SQSBatchProcessingException}.
 *
 * If all the messages are successfully processes, No SQS messages are deleted explicitly but is rather delegated to
 * Lambda execution context for deletion.
 * </p>
 *
 * <p>
 * If you want to suppress the exception even if any message in batch fails, set
 * {@link SqsBatch#suppressException()} to true. By default its value is false
 * </p>
 *
 * <p>
 * If you want certain exceptions to be treated as permanent failures, i.e. exceptions which are not worth retrying and
 * want such message should be moved to configured dead letter queue of the source SQS queue, you can use
 * {@link SqsBatch#nonRetryableExceptions()} to configure such exceptions.
 * If you want such messages to be deleted instead, set {@link SqsBatch#deleteNonRetryableMessageFromQueue()} to true.
 * By default its value is false.
 *
 * If there is no DLQ configured on source SQS queue and {@link SqsBatch#nonRetryableExceptions()} attribute is set, if
 * nonRetryableExceptions occurs from {@link SqsMessageHandler}, such exceptions will still be treated as temporary
 * exceptions and the message will be move back to source SQS queue for reprocessing. Same behaviour occurs if for some
 * reason utility is unable to move message to the DLQ. This can occur because of missing permissions.
 * </p>
 *
 * <pre>
 * public class SqsMessageHandler implements RequestHandler<SQSEvent, String> {
 *
 *    {@literal @}Override
 *    {@literal @}{@link SqsBatch (SqsMessageHandler)}
 *     public String handleRequest(SQSEvent sqsEvent, Context context) {
 *
 *         return "ok";
 *     }
 *
 *     public class DummySqsMessageHandler implements SqsMessageHandler<Object>{
 *     @Override
 *     public Object process(SQSEvent.SQSMessage message) {
 *         throw new UnsupportedOperationException();
 *     }
 * }
 *
 *     ...
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SqsBatch {

    Class<? extends SqsMessageHandler<Object>> value();

    boolean suppressException() default false;

    Class<? extends Exception>[] nonRetryableExceptions() default {};

    boolean deleteNonRetryableMessageFromQueue() default false;
}
